package com.reyzie.hymns.presentation

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.InetAddress
import java.util.concurrent.TimeUnit

data class FreeShowDevice(
    val name: String,
    val host: String,
    val port: Int
)

sealed class FreeShowConnectionState {
    object Disconnected : FreeShowConnectionState()
    object Discovering : FreeShowConnectionState()
    data class Connecting(val host: String) : FreeShowConnectionState()
    data class Connected(val host: String, val port: Int) : FreeShowConnectionState()
    data class Error(val message: String) : FreeShowConnectionState()
}

/**
 * Manages zero-configuration mDNS discovery and real-time Socket.io 4 WebSocket control
 * with FreeShow projection software (port 5510) on the local Wi-Fi network.
 */
object FreeShowRemoteManager {

    private const val TAG = "FreeShowRemoteManager"
    private const val SERVICE_TYPE = "_freeshow._udp."
    private const val PREFS_NAME = "freeshow_remote_prefs"
    private const val PREF_LAST_HOST = "last_host"
    private const val PREF_LAST_PORT = "last_port"
    private const val PREF_LAST_PASSWORD = "last_password"
    private const val DEFAULT_PASSWORD = "1542"
    private const val DEFAULT_PORT = 5510

    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Keep alive for WebSocket
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    // State Flows
    private val _connectionState = MutableStateFlow<FreeShowConnectionState>(FreeShowConnectionState.Disconnected)
    val connectionState: StateFlow<FreeShowConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<FreeShowDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<FreeShowDevice>> = _discoveredDevices.asStateFlow()

    private val _currentLiveSlideIndex = MutableStateFlow<Int?>(null)
    val currentLiveSlideIndex: StateFlow<Int?> = _currentLiveSlideIndex.asStateFlow()

    private val _currentLiveShowId = MutableStateFlow<String?>(null)
    val currentLiveShowId: StateFlow<String?> = _currentLiveShowId.asStateFlow()

    private val _isScreenCleared = MutableStateFlow(false)
    val isScreenCleared: StateFlow<Boolean> = _isScreenCleared.asStateFlow()

    private val _connectedHost = MutableStateFlow<String?>(null)
    val connectedHost: StateFlow<String?> = _connectedHost.asStateFlow()

    private var activeWebSocket: WebSocket? = null
    private var pendingPassword: String = DEFAULT_PASSWORD
    private var pendingShowOnConnect: Pair<String, Map<String, Any>>? = null

    // NSD Discovery
    private var nsdManager: NsdManager? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var isDiscovering = false

    fun getSavedHost(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_LAST_HOST, "") ?: ""
    }

    fun getSavedPort(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(PREF_LAST_PORT, DEFAULT_PORT)
    }

    fun getSavedPassword(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_LAST_PASSWORD, DEFAULT_PASSWORD) ?: DEFAULT_PASSWORD
    }

    private fun saveConnectionDetails(context: Context, host: String, port: Int, password: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(PREF_LAST_HOST, host)
            .putInt(PREF_LAST_PORT, port)
            .putString(PREF_LAST_PASSWORD, password)
            .apply()
    }

    /**
     * Starts mDNS service discovery for FreeShow projectors on the local network.
     */
    fun startDiscovery(context: Context) {
        if (isDiscovering) return

        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifiManager?.createMulticastLock("FreeShowMDNSLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }

            nsdManager = context.applicationContext.getSystemService(Context.NSD_SERVICE) as? NsdManager
            _discoveredDevices.value = emptyList()

            discoveryListener = object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                    Log.w(TAG, "Discovery failed to start: $errorCode")
                    isDiscovering = false
                }

                override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                    isDiscovering = false
                }

                override fun onDiscoveryStarted(serviceType: String?) {
                    isDiscovering = true
                    Log.i(TAG, "Discovery started for $serviceType")
                }

                override fun onDiscoveryStopped(serviceType: String?) {
                    isDiscovering = false
                }

                override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                    serviceInfo ?: return
                    Log.i(TAG, "FreeShow Service found: ${serviceInfo.serviceName}")
                    resolveService(serviceInfo)
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                    serviceInfo ?: return
                    val current = _discoveredDevices.value.toMutableList()
                    current.removeAll { it.name == serviceInfo.serviceName }
                    _discoveredDevices.value = current
                }
            }

            nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting NSD discovery", e)
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val resolver = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.w(TAG, "Resolve failed for ${serviceInfo?.serviceName}: $errorCode")
            }

            override fun onServiceResolved(resolved: NsdServiceInfo?) {
                resolved ?: return
                val host = resolved.host?.hostAddress ?: return
                val port = resolved.port
                val name = resolved.serviceName ?: "FreeShow Projector"

                // We target port 5510 (FreeShow Remote server with Socket.io)
                if (port == 5510 || name.contains("REMOTE", ignoreCase = true)) {
                    val device = FreeShowDevice(
                        name = if (name.contains("freeshow", ignoreCase = true)) "FreeShow Projector ($host)" else name,
                        host = host,
                        port = port
                    )
                    val current = _discoveredDevices.value.toMutableList()
                    if (!current.any { it.host == host && it.port == port }) {
                        current.add(device)
                        _discoveredDevices.value = current
                    }
                }
            }
        }

        try {
            nsdManager?.resolveService(serviceInfo, resolver)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve service ${serviceInfo.serviceName}", e)
        }
    }

    /**
     * Stops mDNS discovery and releases the multicast lock.
     */
    fun stopDiscovery() {
        if (!isDiscovering) return
        try {
            discoveryListener?.let { nsdManager?.stopServiceDiscovery(it) }
            multicastLock?.let { if (it.isHeld) it.release() }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping discovery", e)
        } finally {
            isDiscovering = false
            discoveryListener = null
        }
    }

    /**
     * Connects to FreeShow via Socket.io 4 WebSocket endpoint.
     */
    fun connect(
        context: Context,
        host: String,
        port: Int = DEFAULT_PORT,
        password: String = DEFAULT_PASSWORD,
        pendingShow: Pair<String, Map<String, Any>>? = null
    ) {
        disconnect()

        val cleanHost = host.trim().removePrefix("http://").removePrefix("https://").removePrefix("ws://").removePrefix("wss://")
        if (cleanHost.isEmpty()) {
            _connectionState.value = FreeShowConnectionState.Error("Please enter a valid IP address")
            return
        }

        pendingPassword = password.trim().ifEmpty { DEFAULT_PASSWORD }
        pendingShowOnConnect = pendingShow
        _connectionState.value = FreeShowConnectionState.Connecting(cleanHost)

        val wsUrl = "ws://$cleanHost:$port/socket.io/?EIO=4&transport=websocket"
        Log.i(TAG, "Connecting to FreeShow at $wsUrl")

        val request = Request.Builder().url(wsUrl).build()
        activeWebSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket connection opened to $cleanHost:$port")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(context, webSocket, cleanHost, port, text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket closed: $code / $reason")
                handleDisconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}", t)
                _connectionState.value = FreeShowConnectionState.Error(
                    t.message ?: "Failed to connect to FreeShow at $cleanHost:$port"
                )
                handleDisconnect()
            }
        })
    }

    private fun handleIncomingMessage(
        context: Context,
        webSocket: WebSocket,
        host: String,
        port: Int,
        message: String
    ) {
        when {
            // Engine.IO Handshake response (0{...})
            message.startsWith("0") -> {
                Log.d(TAG, "Received EIO handshake, sending Socket.io connect (40)")
                webSocket.send("40")
            }

            // Socket.io Connect ACK (40) -> Send ACCESS auth packet
            message.startsWith("40") -> {
                Log.i(TAG, "Socket.io connected, sending ACCESS passcode ($pendingPassword)")
                val authPacket = createRemotePacket("ACCESS", pendingPassword)
                webSocket.send("42$authPacket")
            }

            // Engine.IO Ping (2) -> Respond Pong (3)
            message == "2" -> {
                webSocket.send("3")
            }

            // Socket.io Message Event (42[...])
            message.startsWith("42") -> {
                val jsonPayload = message.substring(2)
                try {
                    val array = JsonParser.parseString(jsonPayload).asJsonArray
                    if (array.size() >= 2 && array[0].asString == "REMOTE") {
                        val packetObj = array[1].asJsonObject
                        val channel = packetObj.get("channel")?.asString ?: ""
                        val data = packetObj.get("data")

                        handleRemoteChannel(context, webSocket, host, port, channel, data)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing packet: $message", e)
                }
            }
        }
    }

    private fun handleRemoteChannel(
        context: Context,
        webSocket: WebSocket,
        host: String,
        port: Int,
        channel: String,
        data: JsonElement?
    ) {
        when (channel) {
            "ACCESS" -> {
                Log.i(TAG, "Authentication accepted by FreeShow on $host:$port")
                saveConnectionDetails(context, host, port, pendingPassword)
                _connectedHost.value = host
                _connectionState.value = FreeShowConnectionState.Connected(host, port)

                // If a pending show was queued, beam it right away!
                pendingShowOnConnect?.let { (showId, showData) ->
                    pendingShowOnConnect = null
                    beamShow(showId, showData, autoSelectSlide = 0)
                }
            }

            "OUT" -> {
                try {
                    if (data != null && data.isJsonObject) {
                        val outObj = data.asJsonObject
                        val slideElement = outObj.get("slide")
                        val slideIndex = if (slideElement != null && !slideElement.isJsonNull) {
                            slideElement.asInt
                        } else null

                        val showElement = outObj.get("show")
                        val showId = if (showElement != null && showElement.isJsonObject) {
                            showElement.asJsonObject.get("id")?.asString
                        } else null

                        _currentLiveSlideIndex.value = slideIndex
                        _currentLiveShowId.value = showId
                        _isScreenCleared.value = (slideIndex == null)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing OUT state", e)
                }
            }

            "OUT_DATA" -> {
                try {
                    if (data != null && data.isJsonObject) {
                        val outDataObj = data.asJsonObject
                        val slideElement = outDataObj.get("slide")
                        if (slideElement != null) {
                            val slideIndex = if (!slideElement.isJsonNull) slideElement.asInt else null
                            _currentLiveSlideIndex.value = slideIndex
                            _isScreenCleared.value = (slideIndex == null)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun handleDisconnect() {
        activeWebSocket = null
        _connectedHost.value = null
        if (_connectionState.value is FreeShowConnectionState.Connected ||
            _connectionState.value is FreeShowConnectionState.Connecting) {
            _connectionState.value = FreeShowConnectionState.Disconnected
        }
        _currentLiveSlideIndex.value = null
        _currentLiveShowId.value = null
        _isScreenCleared.value = false
    }

    fun disconnect() {
        try {
            activeWebSocket?.close(1000, "User disconnected")
        } catch (_: Exception) {}
        handleDisconnect()
    }

    /**
     * Emits a command packet to FreeShow's REMOTE event channel.
     */
    private fun sendRemote(channel: String, payload: Any): Boolean {
        val ws = activeWebSocket ?: return false
        val packet = createRemotePacket(channel, payload)
        return ws.send("42$packet")
    }

    private fun createRemotePacket(channel: String, payload: Any): String {
        val remoteMap = mapOf(
            "id" to "",
            "channel" to channel,
            "data" to payload
        )
        return gson.toJson(listOf("REMOTE", remoteMap))
    }

    // ==========================================
    // FreeShow Direct Control API Actions
    // ==========================================

    /**
     * Injects the show into FreeShow and immediately projects slide 0.
     */
    fun beamShow(showId: String, showData: Map<String, Any>, autoSelectSlide: Int? = 0) {
        scope.launch {
            // 1. Inject Show data
            sendRemote("API:set_show", mapOf("id" to showId, "value" to gson.toJson(showData)))

            // Brief delay to allow FreeShow internal state to register the show
            kotlinx.coroutines.delay(150)

            // 2. Select show as active
            sendRemote("API:id_select_show", mapOf("id" to showId))

            // 3. Select target slide
            if (autoSelectSlide != null) {
                kotlinx.coroutines.delay(100)
                selectSlide(showId, autoSelectSlide)
            }
        }
    }

    /**
     * Jumps directly to a specific slide index.
     */
    fun selectSlide(showId: String, index: Int) {
        _currentLiveSlideIndex.value = index
        _isScreenCleared.value = false
        sendRemote("API:index_select_slide", mapOf("index" to index, "showId" to showId))
    }

    /**
     * Advances to the next slide.
     */
    fun nextSlide() {
        val current = _currentLiveSlideIndex.value ?: -1
        _currentLiveSlideIndex.value = current + 1
        _isScreenCleared.value = false
        sendRemote("API:next_slide", emptyMap<String, Any>())
    }

    /**
     * Moves back to the previous slide.
     */
    fun previousSlide() {
        val current = _currentLiveSlideIndex.value ?: 1
        if (current > 0) {
            _currentLiveSlideIndex.value = current - 1
            _isScreenCleared.value = false
        }
        sendRemote("API:previous_slide", emptyMap<String, Any>())
    }

    /**
     * Blanks / Clears projector output immediately.
     */
    fun clearOutput() {
        _isScreenCleared.value = true
        sendRemote("API:clear_all", emptyMap<String, Any>())
    }

    /**
     * Restores cleared projector output.
     */
    fun restoreOutput() {
        _isScreenCleared.value = false
        sendRemote("API:restore_output", emptyMap<String, Any>())
    }

    fun toggleClear() {
        if (_isScreenCleared.value) {
            restoreOutput()
        } else {
            clearOutput()
        }
    }
}

