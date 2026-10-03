package com.example.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState as SessionPlaybackState
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.MusicApplication
import com.example.R
import com.example.widget.MusicWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MusicService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateObserverJob: Job? = null
    private lateinit var playbackManager: PlaybackManager
    private lateinit var notificationManager: NotificationManager
    private var mediaSession: MediaSession? = null

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                if (::playbackManager.isInitialized) {
                    playbackManager.pause()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val app = application as MusicApplication
        playbackManager = app.playbackManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()
        setupMediaSession()

        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(noisyReceiver, filter)

        observePlaybackState()
    }

    private fun setupMediaSession() {
        try {
            val session = MediaSession(this, "MusicServiceMediaSession")
            session.setCallback(object : MediaSession.Callback() {
                override fun onPlay() { playbackManager.resume() }
                override fun onPause() { playbackManager.pause() }
                override fun onSkipToNext() { playbackManager.playNext() }
                override fun onSkipToPrevious() { playbackManager.playPrevious() }
                override fun onSeekTo(pos: Long) { playbackManager.seekTo(pos) }
                override fun onFastForward() { playbackManager.fastForward(10) }
                override fun onRewind() { playbackManager.rewind(10) }
                override fun onStop() { playbackManager.stopPlayback() }
            })
            session.isActive = true
            mediaSession = session
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun observePlaybackState() {
        stateObserverJob?.cancel()
        stateObserverJob = serviceScope.launch {
            playbackManager.state.collectLatest { state ->
                updateMediaSessionState(state)
                // Update App Widgets
                MusicWidgetProvider.updateAllWidgets(this@MusicService, state)

                if (state.currentSong != null) {
                    val notification = buildNotification(state)
                    startForeground(NOTIFICATION_ID, notification)
                } else {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    private fun updateMediaSessionState(state: PlaybackState) {
        val session = mediaSession ?: return
        val song = state.currentSong
        try {
            val actions = SessionPlaybackState.ACTION_PLAY or
                    SessionPlaybackState.ACTION_PAUSE or
                    SessionPlaybackState.ACTION_SKIP_TO_NEXT or
                    SessionPlaybackState.ACTION_SKIP_TO_PREVIOUS or
                    SessionPlaybackState.ACTION_SEEK_TO or
                    SessionPlaybackState.ACTION_FAST_FORWARD or
                    SessionPlaybackState.ACTION_REWIND

            val pbState = SessionPlaybackState.Builder()
                .setActions(actions)
                .setState(
                    if (state.isPlaying) SessionPlaybackState.STATE_PLAYING else SessionPlaybackState.STATE_PAUSED,
                    state.currentPositionMs,
                    state.playbackSpeed
                )
                .build()
            session.setPlaybackState(pbState)

            if (song != null) {
                val metadata = MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, song.title)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, song.artist)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, song.album)
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, state.durationMs)
                    .build()
                session.setMetadata(metadata)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> playbackManager.togglePlayPause()
            ACTION_NEXT -> playbackManager.playNext()
            ACTION_PREVIOUS -> playbackManager.playPrevious()
            ACTION_FAST_FORWARD -> playbackManager.fastForward(10)
            ACTION_REWIND -> playbackManager.rewind(10)
            ACTION_STOP -> {
                playbackManager.stopPlayback()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_UPDATE_NOTIFICATION -> {
                val state = playbackManager.state.value
                if (state.currentSong != null) {
                    startForeground(NOTIFICATION_ID, buildNotification(state))
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(state: PlaybackState): Notification {
        val song = state.currentSong
        val isPlaying = state.isPlaying

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(this, MusicService::class.java).apply { action = ACTION_PREVIOUS }
        val prevPendingIntent = PendingIntent.getService(
            this,
            1,
            prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = Intent(this, MusicService::class.java).apply { action = ACTION_PLAY_PAUSE }
        val playPausePendingIntent = PendingIntent.getService(
            this,
            2,
            playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent(this, MusicService::class.java).apply { action = ACTION_NEXT }
        val nextPendingIntent = PendingIntent.getService(
            this,
            3,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MusicService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this,
            4,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }

        val title = song?.title ?: "Music"
        val artist = song?.artist ?: "Unknown Artist"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText(song?.album)
            .setContentIntent(openAppPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(playPauseIcon, if (isPlaying) "Pause" else "Play", playPausePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close", stopPendingIntent)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${song?.artist ?: "Unknown Artist"} • ${song?.album ?: ""}")
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls and status for music playback"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stateObserverJob?.cancel()
        try {
            mediaSession?.release()
            mediaSession = null
            unregisterReceiver(noisyReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "music_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY_PAUSE = "com.example.action.PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.action.NEXT"
        const val ACTION_PREVIOUS = "com.example.action.PREVIOUS"
        const val ACTION_FAST_FORWARD = "com.example.action.FAST_FORWARD"
        const val ACTION_REWIND = "com.example.action.REWIND"
        const val ACTION_STOP = "com.example.action.STOP"
        const val ACTION_UPDATE_NOTIFICATION = "com.example.action.UPDATE_NOTIFICATION"
    }
}
