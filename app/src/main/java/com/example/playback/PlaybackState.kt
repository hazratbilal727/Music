package com.example.playback

import com.example.data.model.RepeatMode
import com.example.data.model.Song

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isPrepared: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isShuffle: Boolean = false,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerRemainingSeconds: Int? = null,
    val audioSessionId: Int = 0,
    val audioBalance: Float = 0f, // -1f (Full Left) to +1f (Full Right)
    val stopAfterCurrent: Boolean = false,
    val isGapless: Boolean = true,
    val isReplayGain: Boolean = false
) {
    val progress: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}
