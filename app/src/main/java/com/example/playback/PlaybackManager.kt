package com.example.playback

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackManager private constructor(
    private val context: Context,
    private val repository: MusicRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaPlayer: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    val equalizerController = EqualizerController()

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private val prefs = context.getSharedPreferences("music_playback_prefs", Context.MODE_PRIVATE)

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer?.setVolume(0.2f, 0.2f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.setVolume(1.0f, 1.0f)
                // Resume if was playing
            }
        }
    }

    init {
        initMediaPlayer()
        restoreLastPlaybackIfAny()
    }

    private fun restoreLastPlaybackIfAny() {
        scope.launch {
            try {
                val lastSongId = prefs.getLong("last_song_id", -1L)
                val lastPos = prefs.getLong("last_pos_ms", 0L)
                if (lastSongId != -1L) {
                    val song = repository.getSongById(lastSongId)
                    if (song != null && _state.value.currentSong == null) {
                        _state.value = _state.value.copy(
                            currentSong = song,
                            currentPositionMs = lastPos,
                            durationMs = song.durationMs,
                            queue = listOf(song),
                            queueIndex = 0,
                            isPlaying = false,
                            isPrepared = false
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun initMediaPlayer() {
        releaseMediaPlayer()
        val player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnCompletionListener {
                handleSongCompletion()
            }
            setOnErrorListener { _, what, extra ->
                _state.value = _state.value.copy(isPlaying = false)
                true
            }
        }
        mediaPlayer = player
        equalizerController.attachToAudioSession(player.audioSessionId)
        _state.value = _state.value.copy(audioSessionId = player.audioSessionId)
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song), startIndex: Int = queue.indexOf(song).coerceAtLeast(0)) {
        val player = mediaPlayer ?: run {
            initMediaPlayer()
            mediaPlayer!!
        }

        try {
            player.reset()
            val uri = Uri.parse(song.contentUriString)
            player.setDataSource(context, uri)
            player.prepare()

            applyPlaybackSpeed(_state.value.playbackSpeed)
            requestAudioFocus()
            player.start()
            val duration = player.duration.toLong().coerceAtLeast(song.durationMs)
            _state.value = _state.value.copy(
                currentSong = song,
                isPlaying = true,
                isPrepared = true,
                currentPositionMs = 0L,
                durationMs = duration,
                queue = queue,
                queueIndex = startIndex,
                audioSessionId = player.audioSessionId
            )
            equalizerController.attachToAudioSession(player.audioSessionId)
            startProgressTracker()

            // Record play in repository
            scope.launch {
                repository.recordPlayback(song.id)
            }

            startService()
        } catch (e: Exception) {
            e.printStackTrace()
            // If local playback failed (e.g. mock file URI issue), update state gracefully
            _state.value = _state.value.copy(
                currentSong = song,
                isPlaying = false,
                isPrepared = false,
                durationMs = song.durationMs,
                queue = queue,
                queueIndex = startIndex
            )
        }
    }

    fun togglePlayPause() {
        val current = _state.value
        if (current.currentSong == null && current.queue.isNotEmpty()) {
            playSong(current.queue[0], current.queue, 0)
            return
        }
        val song = current.currentSong ?: return

        if (!current.isPrepared || mediaPlayer == null) {
            playSong(song, current.queue.ifEmpty { listOf(song) }, current.queueIndex.coerceAtLeast(0))
            if (current.currentPositionMs > 0) {
                seekTo(current.currentPositionMs)
            }
            return
        }

        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            }
        }
        stopProgressTracker()
        _state.value = _state.value.copy(isPlaying = false)
        _state.value.currentSong?.let { savePlaybackProgress(it.id, _state.value.currentPositionMs) }
        updateService()
    }

    private fun savePlaybackProgress(songId: Long, positionMs: Long) {
        try {
            prefs.edit()
                .putLong("last_song_id", songId)
                .putLong("last_pos_ms", positionMs)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resume() {
        val song = _state.value.currentSong ?: return
        if (!_state.value.isPrepared || mediaPlayer == null) {
            playSong(song, _state.value.queue.ifEmpty { listOf(song) }, _state.value.queueIndex.coerceAtLeast(0))
            if (_state.value.currentPositionMs > 0) {
                seekTo(_state.value.currentPositionMs)
            }
            return
        }
        val player = mediaPlayer ?: return
        requestAudioFocus()
        player.start()
        _state.value = _state.value.copy(isPlaying = true)
        startProgressTracker()
        startService()
    }

    fun playNext() {
        val current = _state.value
        if (current.queue.isEmpty()) return

        val nextIndex = if (current.isShuffle) {
            current.queue.indices.random()
        } else {
            if (current.queueIndex + 1 < current.queue.size) {
                current.queueIndex + 1
            } else if (current.repeatMode == RepeatMode.ALL) {
                0
            } else {
                null
            }
        }

        if (nextIndex != null) {
            val nextSong = current.queue[nextIndex]
            playSong(nextSong, current.queue, nextIndex)
        } else {
            pause()
            seekTo(0L)
        }
    }

    fun playPrevious() {
        val current = _state.value
        if (current.queue.isEmpty()) return

        // If played more than 3 seconds, seek to start of current song
        if (current.currentPositionMs > 3000L) {
            seekTo(0L)
            return
        }

        val prevIndex = if (current.isShuffle) {
            current.queue.indices.random()
        } else {
            if (current.queueIndex - 1 >= 0) {
                current.queueIndex - 1
            } else if (current.repeatMode == RepeatMode.ALL) {
                current.queue.size - 1
            } else {
                0
            }
        }

        val prevSong = current.queue[prevIndex]
        playSong(prevSong, current.queue, prevIndex)
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _state.value.durationMs.coerceAtLeast(0L))
        try {
            mediaPlayer?.seekTo(clamped.toInt())
            _state.value = _state.value.copy(currentPositionMs = clamped)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun fastForward(seconds: Int = 10) {
        seekTo(_state.value.currentPositionMs + seconds * 1000L)
    }

    fun rewind(seconds: Int = 10) {
        seekTo(_state.value.currentPositionMs - seconds * 1000L)
    }

    fun setAudioBalance(balance: Float) {
        val clamped = balance.coerceIn(-1.0f, 1.0f)
        _state.value = _state.value.copy(audioBalance = clamped)
        applyVolume()
    }

    fun setStopAfterCurrent(enabled: Boolean) {
        _state.value = _state.value.copy(stopAfterCurrent = enabled)
    }

    fun setGapless(enabled: Boolean) {
        _state.value = _state.value.copy(isGapless = enabled)
    }

    fun setReplayGain(enabled: Boolean) {
        _state.value = _state.value.copy(isReplayGain = enabled)
        applyVolume()
    }

    private fun applyVolume() {
        val bal = _state.value.audioBalance.coerceIn(-1f, 1f)
        val left = if (bal > 0f) (1f - bal) else 1f
        val right = if (bal < 0f) (1f + bal) else 1f
        val gain = if (_state.value.isReplayGain) 0.85f else 1.0f
        try {
            mediaPlayer?.setVolume(left * gain, right * gain)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleShuffle() {
        val newShuffle = !_state.value.isShuffle
        _state.value = _state.value.copy(isShuffle = newShuffle)
    }

    fun toggleRepeatMode() {
        val nextMode = when (_state.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _state.value = _state.value.copy(repeatMode = nextMode)
    }

    fun setPlaybackSpeed(speed: Float) {
        _state.value = _state.value.copy(playbackSpeed = speed)
        applyPlaybackSpeed(speed)
    }

    private fun applyPlaybackSpeed(speed: Float) {
        try {
            mediaPlayer?.let { player ->
                val params = player.playbackParams
                params.speed = speed
                player.playbackParams = params
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addToQueue(song: Song) {
        val currentQueue = _state.value.queue.toMutableList()
        currentQueue.add(song)
        _state.value = _state.value.copy(queue = currentQueue)
    }

    fun playNextInQueue(song: Song) {
        val currentQueue = _state.value.queue.toMutableList()
        val insertIndex = (_state.value.queueIndex + 1).coerceIn(0, currentQueue.size)
        currentQueue.add(insertIndex, song)
        _state.value = _state.value.copy(queue = currentQueue)
    }

    fun removeFromQueue(index: Int) {
        val currentQueue = _state.value.queue.toMutableList()
        if (index in currentQueue.indices) {
            val removedCurrent = index == _state.value.queueIndex
            currentQueue.removeAt(index)
            val newIndex = when {
                index < _state.value.queueIndex -> _state.value.queueIndex - 1
                removedCurrent -> _state.value.queueIndex.coerceAtMost(currentQueue.size - 1)
                else -> _state.value.queueIndex
            }
            _state.value = _state.value.copy(queue = currentQueue, queueIndex = newIndex)
            if (removedCurrent && currentQueue.isNotEmpty() && newIndex >= 0) {
                playSong(currentQueue[newIndex], currentQueue, newIndex)
            } else if (currentQueue.isEmpty()) {
                pause()
                _state.value = _state.value.copy(currentSong = null)
            }
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val currentQueue = _state.value.queue.toMutableList()
        if (fromIndex in currentQueue.indices && toIndex in currentQueue.indices && fromIndex != toIndex) {
            val item = currentQueue.removeAt(fromIndex)
            currentQueue.add(toIndex, item)
            val currentIdx = _state.value.queueIndex
            val newCurrentIdx = when (currentIdx) {
                fromIndex -> toIndex
                in (minOf(fromIndex, toIndex)..maxOf(fromIndex, toIndex)) -> {
                    if (fromIndex < toIndex) currentIdx - 1 else currentIdx + 1
                }
                else -> currentIdx
            }
            _state.value = _state.value.copy(queue = currentQueue, queueIndex = newCurrentIdx)
        }
    }

    fun clearQueue() {
        pause()
        _state.value = _state.value.copy(queue = emptyList(), queueIndex = -1, currentSong = null)
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _state.value = _state.value.copy(sleepTimerRemainingSeconds = null)
            return
        }

        var remaining = minutes * 60
        _state.value = _state.value.copy(sleepTimerRemainingSeconds = remaining)

        sleepTimerJob = scope.launch {
            while (isActive && remaining > 0) {
                delay(1000L)
                remaining--
                _state.value = _state.value.copy(sleepTimerRemainingSeconds = remaining)

                // Smooth fade out in the last 5 seconds
                if (remaining in 1..5) {
                    val fadeFactor = remaining / 5.0f
                    try {
                        mediaPlayer?.setVolume(fadeFactor, fadeFactor)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            if (remaining <= 0) {
                pause()
                applyVolume() // restore regular configured volume
                _state.value = _state.value.copy(sleepTimerRemainingSeconds = null)
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _state.value = _state.value.copy(sleepTimerRemainingSeconds = null)
    }

    private fun handleSongCompletion() {
        val current = _state.value
        if (current.stopAfterCurrent) {
            pause()
            _state.value = _state.value.copy(stopAfterCurrent = false)
            return
        }
        when (current.repeatMode) {
            RepeatMode.ONE -> {
                seekTo(0L)
                resume()
            }
            RepeatMode.ALL, RepeatMode.OFF -> {
                playNext()
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                try {
                    mediaPlayer?.let { player ->
                        if (player.isPlaying) {
                            val pos = player.currentPosition.toLong()
                            val dur = player.duration.toLong().coerceAtLeast(_state.value.durationMs)
                            _state.value = _state.value.copy(
                                currentPositionMs = pos,
                                durationMs = dur
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Ignore transient exceptions during seek or state switch
                }
                delay(250L)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun startService() {
        val intent = Intent(context, MusicService::class.java).apply {
            action = MusicService.ACTION_UPDATE_NOTIFICATION
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateService() {
        startService()
    }

    fun stopPlayback() {
        pause()
        stopProgressTracker()
        releaseMediaPlayer()
    }

    private fun releaseMediaPlayer() {
        try {
            equalizerController.release()
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: PlaybackManager? = null

        fun getInstance(context: Context, repository: MusicRepository): PlaybackManager {
            return INSTANCE ?: synchronized(this) {
                val instance = PlaybackManager(context.applicationContext, repository)
                INSTANCE = instance
                instance
            }
        }
    }
}
