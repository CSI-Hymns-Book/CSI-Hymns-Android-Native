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
    val isLoading: Boolean = false
)

class AudioViewModel(application: Application) : AndroidViewModel(application) {
    private val exoPlayer = ExoPlayer.Builder(application).build()
    
    private val _audioState = MutableStateFlow(AudioState())
    val audioState: StateFlow<AudioState> = _audioState.asStateFlow()

    private var progressJob: Job? = null

    init {
        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _audioState.value = _audioState.value.copy(isPlaying = isPlaying)
                if (isPlaying) {
                    startProgressUpdate()
                } else {
                    stopProgressUpdate()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
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

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _audioState.value = _audioState.value.copy(
                    error = com.reyzie.hymns.data.ContentErrorMessages.AUDIO_OFFLINE,
                    isLoading = false,
                    isPlaying = false
                )
                stopProgressUpdate()
            }
        })
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                _audioState.value = _audioState.value.copy(
                    position = exoPlayer.currentPosition.coerceAtLeast(0),
                    duration = exoPlayer.duration.coerceAtLeast(0)
                )
                delay(100)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
    }

    fun playSong(number: Int, title: String, isKeerthane: Boolean) {
        val audioUrl = if (isKeerthane) {
            "https://raw.githubusercontent.com/reynold29/midi-files/main/Keerthane/Keerthane_$number.ogg"
        } else {
            "https://raw.githubusercontent.com/reynold29/midi-files/main/Hymns/Hymn_$number.ogg"
        }

        if (_audioState.value.currentSongNumber == number && _audioState.value.isKeerthane == isKeerthane) {
            _audioState.value = _audioState.value.copy(
                isVisible = true,
                currentSongTitle = title,
                error = null
            )
            if (exoPlayer.mediaItemCount == 0) {
                _audioState.value = _audioState.value.copy(isLoading = true, error = null)
                exoPlayer.setMediaItem(MediaItem.fromUri(audioUrl))
                exoPlayer.prepare()
            }
            exoPlayer.seekTo(0)
            _audioState.value = _audioState.value.copy(position = 0, isPlaying = false)
            exoPlayer.play()
            return
        }

        stopProgressUpdate()
        _audioState.value = _audioState.value.copy(
            currentSongTitle = title,
            currentSongNumber = number,
            isKeerthane = isKeerthane,
            isVisible = true,
            isPlaying = false,
            isLoading = true,
            error = null,
            position = 0,
            duration = 0
        )

        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.setMediaItem(MediaItem.fromUri(audioUrl))
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
        _audioState.value = _audioState.value.copy(position = position)
    }

    fun togglePlayback() {
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

    fun toggleVisibility() {
        val becomingVisible = !_audioState.value.isVisible
        if (!becomingVisible) {
            exoPlayer.pause()
            exoPlayer.seekTo(0)
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
        exoPlayer.setPlaybackSpeed(speed)
        _audioState.value = _audioState.value.copy(playbackSpeed = speed)
    }

    fun toggleLoop() {
        val nextLoop = !_audioState.value.isLooping
        exoPlayer.repeatMode = if (nextLoop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        _audioState.value = _audioState.value.copy(isLooping = nextLoop)
    }

    fun stopAndReset() {
        stopProgressUpdate()
        exoPlayer.pause()
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.seekTo(0)
        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
        exoPlayer.setPlaybackSpeed(1.0f)
        _audioState.value = AudioState()
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer.release()
    }
}
