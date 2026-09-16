package com.talha.music.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.Player
import androidx.media3.common.MediaItem
import android.util.Log
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

@Singleton
class PlayerConnection @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val _player = MutableStateFlow<Player?>(null)
    val player = _player.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode = _repeatMode.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed = _playbackSpeed.asStateFlow()

    private val _queue = MutableStateFlow<List<MediaItem>>(emptyList())
    val queue = _queue.asStateFlow()

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            val controller = controllerFuture?.get()
            _player.value = controller
            controller?.addListener(object : Player.Listener {
                init {
                    updateQueue(controller)
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e("PlayerConnection", "Playback failed", error)
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    _shuffleEnabled.value = shuffleModeEnabled
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    _repeatMode.value = repeatMode
                }

                override fun onPlaybackStateChanged(state: Int) {
                    _duration.value = controller.duration.coerceAtLeast(0L)
                }
                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                    _duration.value = controller.duration.coerceAtLeast(0L)
                    updateQueue(controller)
                }

                override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                    updateQueue(controller)
                }
            })
        }, MoreExecutors.directExecutor())
    }

    suspend fun pollPosition() {
        while (kotlinx.coroutines.currentCoroutineContext().isActive) {
            _player.value?.let {
                if (it.isPlaying) {
                    _currentPosition.value = it.currentPosition
                    _duration.value = it.duration.coerceAtLeast(0L)
                }
            }
            delay(500)
        }
    }

    fun seekTo(position: Long) {
        _player.value?.seekTo(position)
    }

    fun toggleShuffle() {
        _player.value?.let { player ->
            player.shuffleModeEnabled = !player.shuffleModeEnabled
        }
    }

    fun cycleRepeatMode() {
        _player.value?.let { player ->
            player.repeatMode = when (player.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun cyclePlaybackSpeed() {
        _player.value?.let { player ->
            val nextSpeed = when (player.playbackParameters.speed) {
                1f -> 1.25f
                1.25f -> 1.5f
                1.5f -> 2f
                else -> 1f
            }
            player.setPlaybackSpeed(nextSpeed)
            _playbackSpeed.value = nextSpeed
        }
    }

    fun playQueueItem(index: Int) {
        _player.value?.seekToDefaultPosition(index)
    }

    fun removeQueueItem(index: Int) {
        _player.value?.removeMediaItem(index)
        _player.value?.let(::updateQueue)
    }

    private fun updateQueue(player: Player) {
        _queue.value = (0 until player.mediaItemCount).map { index -> player.getMediaItemAt(index) }
    }

    fun release() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
