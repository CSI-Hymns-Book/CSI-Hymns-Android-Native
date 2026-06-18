package com.reyzie.hymns.cast

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import com.reyzie.hymns.data.RemoteAppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SongCastRequest(
    val streamUrl: String,
    val title: String,
    val subtitle: String,
    val artworkUrl: String? = null,
    val contentType: String = "audio/ogg"
)

class CastService private constructor() {
    private var castContext: CastContext? = null
    private var featureEnabled = false

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    val featureEnabledFlag: Boolean
        get() = featureEnabled

    fun applyRemoteConfig(context: Context, config: RemoteAppConfig) {
        val enabled = config.castEnabled == true
        featureEnabled = enabled
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putString(KEY_APP_ID, config.castAppId?.trim()?.takeIf { it.isNotEmpty() })
            .apply()
        if (enabled) {
            try {
                castContext = CastContext.getSharedInstance(context)
                castContext?.sessionManager?.addSessionManagerListener(sessionListener, CastSession::class.java)
            } catch (e: Exception) {
                Log.w(TAG, "Cast init failed", e)
            }
        }
    }

    fun disconnect() {
        castContext?.sessionManager?.endCurrentSession(true)
        _isConnected.value = false
    }

    fun castAudio(request: SongCastRequest) {
        val client = castContext?.sessionManager?.currentCastSession?.remoteMediaClient ?: return
        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
            putString(MediaMetadata.KEY_TITLE, request.title)
            putString(MediaMetadata.KEY_SUBTITLE, request.subtitle)
            request.artworkUrl?.let { url ->
                addImage(WebImage(Uri.parse(url)))
            }
        }
        val mediaInfo = MediaInfo.Builder(request.streamUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(request.contentType)
            .setMetadata(metadata)
            .build()
        client.load(
            com.google.android.gms.cast.MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(true)
                .build()
        )
    }

    fun currentSession(): CastSession? = castContext?.sessionManager?.currentCastSession

    fun remoteMediaClient(): RemoteMediaClient? = currentSession()?.remoteMediaClient

    private val sessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            _isConnected.value = true
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            _isConnected.value = false
        }

        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionStartFailed(session: CastSession, error: Int) {
            _isConnected.value = false
        }
        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            _isConnected.value = true
        }
        override fun onSessionResumeFailed(session: CastSession, error: Int) {}
        override fun onSessionSuspended(session: CastSession, reason: Int) {
            _isConnected.value = false
        }
    }

    companion object {
        private const val TAG = "CastService"
        private const val PREFS = "cast_prefs"
        private const val KEY_ENABLED = "cast_enabled"
        private const val KEY_APP_ID = "cast_app_id"
        private const val DEFAULT_APP_ID = "CC1AD845"

        @Volatile
        private var instance: CastService? = null

        fun getInstance(): CastService =
            instance ?: synchronized(this) {
                instance ?: CastService().also { instance = it }
            }

        fun getCachedAppId(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return prefs.getString(KEY_APP_ID, null)?.takeIf { it.isNotEmpty() } ?: DEFAULT_APP_ID
        }

        fun isFeatureEnabledCached(context: Context): Boolean =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, false)
    }
}
