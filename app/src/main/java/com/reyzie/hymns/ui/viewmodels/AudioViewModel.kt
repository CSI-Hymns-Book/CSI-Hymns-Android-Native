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
    val currentAudioUrl: String? = null
)

class AudioViewModel(application: Application) : AndroidViewModel(application) {
    private val exoPlayer = ExoPlayer.Builder(application).build()
    private var mediaPlayer: android.media.MediaPlayer? = null
    private var isUsingMediaPlayer = false
    
    private val _audioState = MutableStateFlow(AudioState())
    val audioState: StateFlow<AudioState> = _audioState.asStateFlow()

    private var progressJob: Job? = null

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
                    _audioState.value = _audioState.value.copy(
                        error = com.reyzie.hymns.data.ContentErrorMessages.AUDIO_OFFLINE,
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

    fun playSong(number: Int, title: String, isKeerthane: Boolean, customAudioUrl: String? = null) {
        val audioUrl = customAudioUrl ?: if (isKeerthane) {
            "https://raw.githubusercontent.com/reynold29/midi-files/main/Keerthane/Keerthane_$number.ogg"
        } else {
            "https://raw.githubusercontent.com/reynold29/midi-files/main/Hymns/Hymn_$number.ogg"
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
        mediaPlayer = android.media.MediaPlayer().apply {
            setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(audioUrl)
            setOnPreparedListener { mp ->
                _audioState.value = _audioState.value.copy(
                    duration = mp.duration.toLong().coerceAtLeast(0),
                    isLoading = false,
                    isPlaying = true,
                    error = null
                )
                try {
                    mp.playbackParams = mp.playbackParams.setSpeed(_audioState.value.playbackSpeed)
                } catch (e: Exception) {
                    // Fallback if device speed settings fail
                }
                mp.start()
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
            setOnCompletionListener { mp ->
                mp.seekTo(0)
                if (_audioState.value.isLooping) {
                    mp.start()
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
        if (isUsingMediaPlayer) {
            val mp = mediaPlayer ?: return
            if (mp.isPlaying) {
                mp.pause()
                _audioState.value = _audioState.value.copy(isPlaying = false)
                stopProgressUpdate()
            } else {
                try {
                    mp.playbackParams = mp.playbackParams.setSpeed(_audioState.value.playbackSpeed)
                } catch (e: Exception) {}
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
        if (isUsingMediaPlayer) {
            mediaPlayer?.let { mp ->
                try {
                    mp.playbackParams = mp.playbackParams.setSpeed(speed)
                } catch (e: Exception) {}
            }
        } else {
            exoPlayer.setPlaybackSpeed(speed)
        }
        _audioState.value = _audioState.value.copy(playbackSpeed = speed)
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
