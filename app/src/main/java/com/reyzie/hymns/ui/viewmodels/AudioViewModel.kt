package com.reyzie.hymns.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.reyzie.hymns.data.AppConfigRepository

data class AudioState(
    val isPlaying: Boolean = false,
    val currentSongTitle: String? = null,
    val currentSongNumber: Int? = null,
    val isKeerthane: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val isLooping: Boolean = false,
    val isVisible: Boolean = false,
    val position: Long = 0,
    val duration: Long = 0,
    val error: String? = null,
    val isLoading: Boolean = false,
    val currentAudioUrl: String? = null,
    val midiTranspose: Int = 0,
    val isSopranoEnabled: Boolean = true,
    val isAltoEnabled: Boolean = true,
    val isTenorEnabled: Boolean = true,
    val isBassEnabled: Boolean = true,
    val sopranoInstrument: Int = 19,
    val altoInstrument: Int = 19,
    val tenorInstrument: Int = 19,
    val bassInstrument: Int = 19
)

class AudioViewModel(application: Application) : AndroidViewModel(application) {
    private val appConfigRepository = AppConfigRepository(context = application)
    private val exoPlayer = ExoPlayer.Builder(application).build()
    private var mediaPlayer: android.media.MediaPlayer? = null
    private var isUsingMediaPlayer = false
    private var rawMidiCache: ByteArray? = null
    
    private val _audioState = MutableStateFlow(AudioState())
    val audioState: StateFlow<AudioState> = _audioState.asStateFlow()

    private var progressJob: Job? = null
    private var downloadJob: Job? = null

    init {
        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isUsingMediaPlayer) {
                    _audioState.value = _audioState.value.copy(isPlaying = isPlaying)
                    if (isPlaying) {
                        startProgressUpdate()
                    } else {
                        stopProgressUpdate()
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (!isUsingMediaPlayer) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            _audioState.value = _audioState.value.copy(
                                duration = exoPlayer.duration.coerceAtLeast(0),
                                isLoading = false,
                                error = null
                            )
                        }
                        Player.STATE_BUFFERING -> {
                            _audioState.value = _audioState.value.copy(isLoading = true, error = null)
                        }
                        Player.STATE_ENDED -> {
                            exoPlayer.pause()
                            exoPlayer.seekTo(0)
                            _audioState.value = _audioState.value.copy(
                                isPlaying = false,
                                position = 0
                            )
                            stopProgressUpdate()
                        }
                        else -> {}
                    }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (!isUsingMediaPlayer) {
                    val is404 = error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
                            || error.message?.contains("404") == true
                            || error.cause?.message?.contains("404") == true
                    val errorMsg = if (is404) {
                        "AUDIO_NOT_FOUND"
                    } else {
                        com.reyzie.hymns.data.ContentErrorMessages.AUDIO_OFFLINE
                    }
                    _audioState.value = _audioState.value.copy(
                        error = errorMsg,
                        isLoading = false,
                        isPlaying = false
                    )
                    stopProgressUpdate()
                }
            }
        })
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                val currentPos = if (isUsingMediaPlayer) {
                    mediaPlayer?.currentPosition?.toLong() ?: 0L
                } else {
                    exoPlayer.currentPosition
                }
                val totalDuration = if (isUsingMediaPlayer) {
                    mediaPlayer?.duration?.toLong() ?: 0L
                } else {
                    exoPlayer.duration
                }
                _audioState.value = _audioState.value.copy(
                    position = currentPos.coerceAtLeast(0),
                    duration = totalDuration.coerceAtLeast(0)
                )
                delay(100)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
    }
    fun playSong(number: Int, title: String, isKeerthane: Boolean, signature: String? = null, customAudioUrl: String? = null) {
        val config = appConfigRepository.getCachedRemoteConfig()
        
        val defaultOption = if (!isKeerthane && !signature.isNullOrBlank()) {
            if (signature.contains("/")) signature.split("/").firstOrNull()?.trim() ?: "" else signature.trim()
        } else ""

        val isMidiMigrated = if (isKeerthane) {
            config.parsedMidiKeerthanes.contains(number)
        } else {
            val isMtRef = defaultOption.contains("M.T.", ignoreCase = true) || 
                          defaultOption.contains("Mang.T.B.", ignoreCase = true) || 
                          defaultOption.lowercase().startsWith("mt")
            if (isMtRef) {
                true
            } else {
                val baseMeter = if (defaultOption.contains("_")) defaultOption.substringBefore("_") else defaultOption
                val normalized = com.reyzie.hymns.utils.MeterUtils.getNormalizedMeter(baseMeter)
                config.parsedMidiHymns.contains(normalized)
            }
        }

        val audioUrl = customAudioUrl ?: if (isKeerthane) {
            if (isMidiMigrated) {
                "https://raw.githubusercontent.com/reynold29/midi-files/main/Keerthane/midi/Keerthane_$number.mid"
            } else {
                "https://raw.githubusercontent.com/reynold29/midi-files/main/Keerthane/Keerthane_$number.ogg"
            }
        } else {
            if (isMidiMigrated) {
                val isMtRef = defaultOption.contains("M.T.", ignoreCase = true) || 
                              defaultOption.contains("Mang.T.B.", ignoreCase = true) || 
                              defaultOption.lowercase().startsWith("mt")
                if (isMtRef) {
                    val mtNumber = defaultOption.filter { it.isDigit() || it == 'b' || it == 'c' || it == 'd' || it == 'e' }
                    "https://raw.githubusercontent.com/Reynold29/midi-files/main/Mangalore%20Tunes/mt${mtNumber}.mid"
                } else {
                    val meterName = com.reyzie.hymns.utils.MeterUtils.getMeterMidiFileName(defaultOption)
                    "https://raw.githubusercontent.com/reynold29/midi-files/main/Hymns/midi/${meterName}.mid"
                }
            } else {
                "https://raw.githubusercontent.com/reynold29/midi-files/main/Hymns/Hymn_$number.ogg"
            }
        }

        val isMidi = audioUrl.endsWith(".mid", ignoreCase = true)
        
        if (_audioState.value.currentAudioUrl == audioUrl) {
            _audioState.value = _audioState.value.copy(
                isVisible = true,
                currentSongTitle = title,
                error = null
            )
            if (isMidi) {
                val mp = mediaPlayer
                if (mp == null) {
                    rawMidiCache = null
                    playMidi(audioUrl, number, title, isKeerthane)
                } else {
                    mp.seekTo(0)
                    _audioState.value = _audioState.value.copy(position = 0, isPlaying = true)
                    mp.start()
                    startProgressUpdate()
                }
            } else {
                if (exoPlayer.mediaItemCount == 0) {
                    _audioState.value = _audioState.value.copy(isLoading = true, error = null)
                    exoPlayer.setMediaItem(MediaItem.fromUri(audioUrl))
                    exoPlayer.prepare()
                }
                exoPlayer.seekTo(0)
                _audioState.value = _audioState.value.copy(position = 0, isPlaying = false)
                exoPlayer.play()
            }
            return
        }

        rawMidiCache = null
        stopProgressUpdate()
        isUsingMediaPlayer = isMidi
        _audioState.value = _audioState.value.copy(
            currentSongTitle = title,
            currentSongNumber = number,
            isKeerthane = isKeerthane,
            isVisible = true,
            isPlaying = false,
            isLoading = true,
            error = null,
            position = 0,
            duration = 0,
            currentAudioUrl = audioUrl
        )

        // Reset ExoPlayer
        exoPlayer.stop()
        exoPlayer.clearMediaItems()

        // Reset MediaPlayer fallback
        mediaPlayer?.release()
        mediaPlayer = null

        if (isMidi) {
            playMidi(audioUrl, number, title, isKeerthane)
        } else {
            exoPlayer.setMediaItem(MediaItem.fromUri(audioUrl))
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    private fun playMidi(audioUrl: String, number: Int, title: String, isKeerthane: Boolean) {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Download MIDI bytes
                val connection = java.net.URL(audioUrl).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                val midiBytes = connection.inputStream.use { it.readBytes() }
                rawMidiCache = midiBytes
                
                // 2. Patch to chosen MIDI Instrument from preferences
                val prefs = getApplication<Application>().getSharedPreferences("settings_prefs", android.content.Context.MODE_PRIVATE)
                val instrument = prefs.getInt("midi_instrument", 19)
                val state = _audioState.value
                val patchedBytes = patchMidiInstrument(
                    midiBytes = midiBytes,
                    instrumentProgram = instrument,
                    transposeSemitones = state.midiTranspose,
                    isSopranoEnabled = state.isSopranoEnabled,
                    isAltoEnabled = state.isAltoEnabled,
                    isTenorEnabled = state.isTenorEnabled,
                    isBassEnabled = state.isBassEnabled,
                    sopranoInstrument = state.sopranoInstrument,
                    altoInstrument = state.altoInstrument,
                    tenorInstrument = state.tenorInstrument,
                    bassInstrument = state.bassInstrument,
                    speed = state.playbackSpeed
                )
                
                // 3. Write to temp file
                val tempFile = File(getApplication<Application>().cacheDir, "temp_patched_${number}.mid")
                tempFile.writeBytes(patchedBytes)
                
                // 4. Set datasource on Main thread
                withContext(Dispatchers.Main) {
                    val mp = android.media.MediaPlayer().apply {
                        setAudioAttributes(
                            android.media.AudioAttributes.Builder()
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                .build()
                        )
                        setDataSource(tempFile.absolutePath)
                        setOnPreparedListener { preparedMp ->
                            _audioState.value = _audioState.value.copy(
                                duration = preparedMp.duration.toLong().coerceAtLeast(0),
                                isLoading = false,
                                isPlaying = true,
                                error = null
                            )
                            preparedMp.start()
                            startProgressUpdate()
                        }
                        setOnErrorListener { _, _, _ ->
                            _audioState.value = _audioState.value.copy(
                                error = com.reyzie.hymns.data.ContentErrorMessages.AUDIO_OFFLINE,
                                isLoading = false,
                                isPlaying = false
                            )
                            stopProgressUpdate()
                            true
                        }
                        setOnCompletionListener { completedMp ->
                            completedMp.seekTo(0)
                            if (_audioState.value.isLooping) {
                                completedMp.start()
                            } else {
                                _audioState.value = _audioState.value.copy(
                                    isPlaying = false,
                                    position = 0
                                )
                                stopProgressUpdate()
                            }
                        }
                        prepareAsync()
                    }
                    mediaPlayer = mp
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val is404 = e is java.io.FileNotFoundException || e.message?.contains("404") == true
                    val errorMsg = if (is404) {
                        "AUDIO_NOT_FOUND"
                    } else {
                        com.reyzie.hymns.data.ContentErrorMessages.AUDIO_OFFLINE
                    }
                    _audioState.value = _audioState.value.copy(
                        error = errorMsg,
                        isLoading = false,
                        isPlaying = false
                    )
                    stopProgressUpdate()
                }
            }
        }
    }

    private fun patchMidiInstrument(
        midiBytes: ByteArray,
        instrumentProgram: Int,
        transposeSemitones: Int,
        isSopranoEnabled: Boolean,
        isAltoEnabled: Boolean,
        isTenorEnabled: Boolean,
        isBassEnabled: Boolean,
        sopranoInstrument: Int = 19,
        altoInstrument: Int = 19,
        tenorInstrument: Int = 19,
        bassInstrument: Int = 19,
        speed: Float = 1.0f
    ): ByteArray {
        val result = midiBytes.clone()
        var i = 0
        val length = result.size

        // Verify it starts with \"MThd\" header
        if (length < 14 || result[0] != 'M'.code.toByte() || result[1] != 'T'.code.toByte() || result[2] != 'h'.code.toByte() || result[3] != 'd'.code.toByte()) {
            return midiBytes
        }

        // Header size is always 6 bytes, so track data starts after 8 + 6 = 14 bytes
        i = 14
        while (i < length - 8) {
            // Read chunk ID
            val c0 = result[i]
            val c1 = result[i+1]
            val c2 = result[i+2]
            val c3 = result[i+3]
            
            // Read chunk length (4 bytes big-endian)
            val chunkLen = ((result[i+4].toInt() and 0xFF) shl 24) or
                           ((result[i+5].toInt() and 0xFF) shl 16) or
                           ((result[i+6].toInt() and 0xFF) shl 8) or
                           (result[i+7].toInt() and 0xFF)
            
            i += 8
            if (c0 == 'M'.code.toByte() && c1 == 'T'.code.toByte() && c2 == 'r'.code.toByte() && c3 == 'k'.code.toByte()) {
                val trackEnd = i + chunkLen
                var trackPtr = i
                var runningStatus = 0
                
                while (trackPtr < trackEnd && trackPtr < length) {
                    // 1. Read delta time (variable length quantity)
                    var byte = result[trackPtr].toInt() and 0xFF
                    trackPtr++
                    while (byte and 0x80 != 0) {
                        if (trackPtr >= trackEnd) break
                        byte = result[trackPtr].toInt() and 0xFF
                        trackPtr++
                    }
                    if (trackPtr >= trackEnd) break
                    
                    // 2. Read status byte
                    var status = result[trackPtr].toInt() and 0xFF
                    if (status and 0x80 != 0) {
                        trackPtr++
                        runningStatus = status
                    } else {
                        status = runningStatus
                    }
                    
                    val statusType = status and 0xF0
                    val channel = status and 0x0F
                    
                    if (status == 0xFF) {
                        // Meta Event: 0xFF [type] [length (VLQ)] [data...]
                        if (trackPtr >= trackEnd) break
                        val metaType = result[trackPtr].toInt() and 0xFF
                        trackPtr++
                        
                        // Read length of meta event (VLQ)
                        var metaLen = 0
                        if (trackPtr < trackEnd) {
                            var lenByte = result[trackPtr].toInt() and 0xFF
                            trackPtr++
                            metaLen = lenByte and 0x7F
                            while (lenByte and 0x80 != 0 && trackPtr < trackEnd) {
                                lenByte = result[trackPtr].toInt() and 0xFF
                                trackPtr++
                                metaLen = (metaLen shl 7) or (lenByte and 0x7F)
                            }
                        }
                        if (metaType == 0x51 && metaLen == 3 && trackPtr + 2 < trackEnd) {
                            val oldTempo = ((result[trackPtr].toInt() and 0xFF) shl 16) or
                                           ((result[trackPtr + 1].toInt() and 0xFF) shl 8) or
                                           (result[trackPtr + 2].toInt() and 0xFF)
                            if (speed != 1.0f && speed > 0f) {
                                val newTempo = (oldTempo / speed).toInt().coerceIn(10000, 10000000)
                                result[trackPtr] = ((newTempo shr 16) and 0xFF).toByte()
                                result[trackPtr + 1] = ((newTempo shr 8) and 0xFF).toByte()
                                result[trackPtr + 2] = (newTempo and 0xFF).toByte()
                            }
                        }
                        trackPtr += metaLen
                    } else if (statusType == 0xF0) {
                        // SysEx Event: 0xF0 or 0xF7 [length (VLQ)] [data...]
                        var sysexLen = 0
                        if (trackPtr < trackEnd) {
                            var lenByte = result[trackPtr].toInt() and 0xFF
                            trackPtr++
                            sysexLen = lenByte and 0x7F
                            while (lenByte and 0x80 != 0 && trackPtr < trackEnd) {
                                lenByte = result[trackPtr].toInt() and 0xFF
                                trackPtr++
                                sysexLen = (sysexLen shl 7) or (lenByte and 0x7F)
                            }
                        }
                        trackPtr += sysexLen
                    } else {
                        // Channel Voice Message
                        when (statusType) {
                            0x90 -> {
                                // Note On: data1 = note, data2 = velocity
                                if (trackPtr + 1 < trackEnd) {
                                    val isMuted = when (channel) {
                                        0 -> !isSopranoEnabled
                                        1 -> !isAltoEnabled
                                        2 -> !isTenorEnabled
                                        3 -> !isBassEnabled
                                        else -> false
                                    }
                                    if (isMuted) {
                                        result[trackPtr + 1] = 0.toByte()
                                    }
                                }
                                if (trackPtr < trackEnd && channel != 9) {
                                    var note = result[trackPtr].toInt() and 0xFF
                                    note = (note + transposeSemitones).coerceIn(0, 127)
                                    result[trackPtr] = note.toByte()
                                }
                                trackPtr += 2
                            }
                            0x80, 0xA0 -> {
                                // Note Off, Key Pressure: transpose note
                                if (trackPtr < trackEnd && channel != 9) {
                                    var note = result[trackPtr].toInt() and 0xFF
                                    note = (note + transposeSemitones).coerceIn(0, 127)
                                    result[trackPtr] = note.toByte()
                                }
                                trackPtr += 2
                            }
                            0xB0, 0xE0 -> {
                                // 2 data bytes
                                trackPtr += 2
                            }
                            0xC0 -> {
                                // Program Change: 1 data byte (program number)
                                if (trackPtr < trackEnd) {
                                    // Channel 9 is standard MIDI drums. Skip it.
                                    if (channel != 9) {
                                        val instr = when (channel) {
                                            0 -> sopranoInstrument
                                            1 -> altoInstrument
                                            2 -> tenorInstrument
                                            3 -> bassInstrument
                                            else -> instrumentProgram
                                        }
                                        result[trackPtr] = instr.toByte()
                                    }
                                    trackPtr++
                                }
                            }
                            0xD0 -> {
                                // 1 data byte
                                trackPtr++
                            }
                            else -> {
                                // Unknown/fallback
                                trackPtr++
                            }
                        }
                    }
                }
                i = trackEnd
            } else {
                i += chunkLen
            }
        }
        return result
    }

    fun seekTo(position: Long) {
        if (isUsingMediaPlayer) {
            mediaPlayer?.seekTo(position.toInt())
        } else {
            exoPlayer.seekTo(position)
        }
        _audioState.value = _audioState.value.copy(position = position)
    }

    fun togglePlayback() {
        val state = _audioState.value
        if (state.error != null) {
            val num = state.currentSongNumber
            val title = state.currentSongTitle
            val isKeerthane = state.isKeerthane
            val url = state.currentAudioUrl
            if (num != null && title != null) {
                playSong(num, title, isKeerthane, url)
                return
            }
        }

        if (isUsingMediaPlayer) {
            val mp = mediaPlayer ?: return
            if (mp.isPlaying) {
                mp.pause()
                _audioState.value = _audioState.value.copy(isPlaying = false)
                stopProgressUpdate()
            } else {
                mp.start()
                _audioState.value = _audioState.value.copy(isPlaying = true)
                startProgressUpdate()
            }
        } else {
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.seekTo(0)
                _audioState.value = _audioState.value.copy(position = 0)
            }
            if (exoPlayer.playbackState == Player.STATE_IDLE) {
                _audioState.value = _audioState.value.copy(isLoading = true, error = null)
            }
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            } else {
                exoPlayer.play()
            }
        }
    }

    fun setMidiInstrument(instrument: Int) {
        val prefs = getApplication<Application>().getSharedPreferences("settings_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putInt("midi_instrument", instrument).apply()
        
        val state = _audioState.value
        val url = state.currentAudioUrl
        if (url != null && url.endsWith(".mid", ignoreCase = true)) {
            applyRealtimeMidiChanges()
        }
    }

    fun setMidiTranspose(semitones: Int) {
        _audioState.value = _audioState.value.copy(midiTranspose = semitones)
        
        val state = _audioState.value
        val url = state.currentAudioUrl
        if (url != null && url.endsWith(".mid", ignoreCase = true)) {
            applyRealtimeMidiChanges()
        }
    }

    fun setSatbRoute(soprano: Boolean, alto: Boolean, tenor: Boolean, bass: Boolean) {
        _audioState.value = _audioState.value.copy(
            isSopranoEnabled = soprano,
            isAltoEnabled = alto,
            isTenorEnabled = tenor,
            isBassEnabled = bass
        )
        
        val state = _audioState.value
        val url = state.currentAudioUrl
        if (url != null && url.endsWith(".mid", ignoreCase = true)) {
            applyRealtimeMidiChanges()
        }
    }

    fun setSatbInstruments(soprano: Int, alto: Int, tenor: Int, bass: Int) {
        _audioState.value = _audioState.value.copy(
            sopranoInstrument = soprano,
            altoInstrument = alto,
            tenorInstrument = tenor,
            bassInstrument = bass
        )
        
        val state = _audioState.value
        val url = state.currentAudioUrl
        if (url != null && url.endsWith(".mid", ignoreCase = true)) {
            applyRealtimeMidiChanges()
        }
    }

    private fun applyRealtimeMidiChanges() {
        val mp = mediaPlayer ?: return
        val isPlaying = mp.isPlaying
        val currentPos = mp.currentPosition
        
        stopProgressUpdate()
        if (isPlaying) {
            mp.pause()
        }
        
        val midiBytes = rawMidiCache ?: return
        val state = _audioState.value
        val num = state.currentSongNumber ?: return
        
        val prefs = getApplication<Application>().getSharedPreferences("settings_prefs", android.content.Context.MODE_PRIVATE)
        val instrument = prefs.getInt("midi_instrument", 19)
        
        val patchedBytes = patchMidiInstrument(
            midiBytes = midiBytes,
            instrumentProgram = instrument,
            transposeSemitones = state.midiTranspose,
            isSopranoEnabled = state.isSopranoEnabled,
            isAltoEnabled = state.isAltoEnabled,
            isTenorEnabled = state.isTenorEnabled,
            isBassEnabled = state.isBassEnabled,
            sopranoInstrument = state.sopranoInstrument,
            altoInstrument = state.altoInstrument,
            tenorInstrument = state.tenorInstrument,
            bassInstrument = state.bassInstrument,
            speed = state.playbackSpeed
        )
        
        val tempFile = File(getApplication<Application>().cacheDir, "temp_patched_${num}.mid")
        tempFile.writeBytes(patchedBytes)
        
        mp.reset()
        mp.setDataSource(tempFile.absolutePath)
        mp.prepare()
        mp.seekTo(currentPos)
        
        if (isPlaying) {
            mp.start()
            _audioState.value = _audioState.value.copy(isPlaying = true)
            startProgressUpdate()
        } else {
            _audioState.value = _audioState.value.copy(isPlaying = false)
        }
    }

    fun toggleVisibility() {
        val becomingVisible = !_audioState.value.isVisible
        if (!becomingVisible) {
            if (isUsingMediaPlayer) {
                mediaPlayer?.pause()
                mediaPlayer?.seekTo(0)
            } else {
                exoPlayer.pause()
                exoPlayer.seekTo(0)
            }
            stopProgressUpdate()
            _audioState.value = _audioState.value.copy(
                isVisible = false,
                isPlaying = false,
                position = 0
            )
        } else {
            _audioState.value = _audioState.value.copy(isVisible = true)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _audioState.value = _audioState.value.copy(playbackSpeed = speed)
        if (isUsingMediaPlayer) {
            val mp = mediaPlayer
            if (mp != null) {
                applyRealtimeMidiChanges()
            }
        } else {
            exoPlayer.setPlaybackSpeed(speed)
        }
    }

    fun toggleLoop() {
        val nextLoop = !_audioState.value.isLooping
        if (isUsingMediaPlayer) {
            mediaPlayer?.isLooping = nextLoop
        } else {
            exoPlayer.repeatMode = if (nextLoop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        }
        _audioState.value = _audioState.value.copy(isLooping = nextLoop)
    }

    fun stopAndReset() {
        stopProgressUpdate()
        if (isUsingMediaPlayer) {
            mediaPlayer?.pause()
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } else {
            exoPlayer.pause()
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.seekTo(0)
            exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
            exoPlayer.setPlaybackSpeed(1.0f)
        }
        isUsingMediaPlayer = false
        _audioState.value = AudioState()
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        exoPlayer.release()
    }
}
