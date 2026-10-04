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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
import android.media.session.MediaSession
import android.media.session.PlaybackState as SessionPlaybackState
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.MusicApplication
import com.example.R
import com.example.data.model.Song
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

    // Artwork bitmap cache
    private var cachedArtSongId: Long = -1L
    private var cachedArtBitmap: Bitmap? = null

    // Notification update throttling
    private var lastNotifSongId: Long = -1L
    private var lastNotifPlaying: Boolean? = null
    private var lastNotifFavorite: Boolean? = null
    private var lastNotifPosSec: Long = -1L

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
                val song = state.currentSong
                if (song != null) {
                    updateMediaSessionState(state)
                    // Update App Widgets
                    MusicWidgetProvider.updateAllWidgets(this@MusicService, state)

                    val currentPosSec = state.currentPositionMs / 1000L
                    val shouldUpdateNotification = song.id != lastNotifSongId ||
                            state.isPlaying != lastNotifPlaying ||
                            song.isFavorite != lastNotifFavorite ||
                            currentPosSec != lastNotifPosSec

                    if (shouldUpdateNotification) {
                        lastNotifSongId = song.id
                        lastNotifPlaying = state.isPlaying
                        lastNotifFavorite = song.isFavorite
                        lastNotifPosSec = currentPosSec

                        val notification = buildNotification(state)
                        startForeground(NOTIFICATION_ID, notification)
                    }
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
                    SessionPlaybackState.ACTION_REWIND or
                    SessionPlaybackState.ACTION_STOP

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
                val artBitmap = loadArtworkBitmap(song)
                val metadata = MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, song.title)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, song.artist)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, song.album)
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, state.durationMs)
                    .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artBitmap)
                    .putBitmap(MediaMetadata.METADATA_KEY_ART, artBitmap)
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
            ACTION_TOGGLE_FAVORITE -> playbackManager.toggleFavoriteCurrent()
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
        val isFavorite = song?.isFavorite == true

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val favIntent = Intent(this, MusicService::class.java).apply { action = ACTION_TOGGLE_FAVORITE }
        val favPendingIntent = PendingIntent.getService(
            this,
            1,
            favIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(this, MusicService::class.java).apply { action = ACTION_PREVIOUS }
        val prevPendingIntent = PendingIntent.getService(
            this,
            2,
            prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = Intent(this, MusicService::class.java).apply { action = ACTION_PLAY_PAUSE }
        val playPausePendingIntent = PendingIntent.getService(
            this,
            3,
            playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent(this, MusicService::class.java).apply { action = ACTION_NEXT }
        val nextPendingIntent = PendingIntent.getService(
            this,
            4,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MusicService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this,
            5,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val artBitmap = if (song != null) loadArtworkBitmap(song) else createDefaultArtBitmap("Music")

        // Formatted times
        val curMs = state.currentPositionMs.coerceAtLeast(0L)
        val totMs = state.durationMs.coerceAtLeast(1L)
        val curSec = curMs / 1000L
        val totSec = totMs / 1000L
        val curTimeStr = "%02d:%02d".format(curSec / 60, curSec % 60)
        val totTimeStr = "%02d:%02d".format(totSec / 60, totSec % 60)
        val progress1000 = ((curMs.toFloat() / totMs.toFloat()) * 1000).toInt().coerceIn(0, 1000)

        val favIconRes = if (isFavorite) R.drawable.ic_notif_heart_filled else R.drawable.ic_notif_heart_outline
        val playPauseIconRes = if (isPlaying) R.drawable.ic_notif_pause else R.drawable.ic_notif_play

        val title = song?.title ?: "Music Player"
        val artist = song?.artist?.ifBlank { "<unknown>" } ?: "<unknown>"

        // 1. Expanded RemoteViews matching the user screenshot layout
        val expandedViews = RemoteViews(packageName, R.layout.notification_player_expanded).apply {
            setImageViewBitmap(R.id.notif_album_art, artBitmap)
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_artist, artist)
            setImageViewResource(R.id.notif_btn_fav, favIconRes)
            setImageViewResource(R.id.notif_btn_prev, R.drawable.ic_notif_prev)
            setImageViewResource(R.id.notif_btn_play_pause, playPauseIconRes)
            setImageViewResource(R.id.notif_btn_next, R.drawable.ic_notif_next)
            setImageViewResource(R.id.notif_btn_close, R.drawable.ic_notif_close)

            setTextViewText(R.id.notif_time_current, curTimeStr)
            setTextViewText(R.id.notif_time_total, totTimeStr)
            setProgressBar(R.id.notif_progress_bar, 1000, progress1000, false)

            setOnClickPendingIntent(R.id.notif_header, openAppPendingIntent)
            setOnClickPendingIntent(R.id.notif_album_art, openAppPendingIntent)
            setOnClickPendingIntent(R.id.notif_title, openAppPendingIntent)
            setOnClickPendingIntent(R.id.notif_artist, openAppPendingIntent)
            setOnClickPendingIntent(R.id.notif_btn_fav, favPendingIntent)
            setOnClickPendingIntent(R.id.notif_btn_prev, prevPendingIntent)
            setOnClickPendingIntent(R.id.notif_btn_play_pause, playPausePendingIntent)
            setOnClickPendingIntent(R.id.notif_btn_next, nextPendingIntent)
            setOnClickPendingIntent(R.id.notif_btn_close, stopPendingIntent)
            setOnClickPendingIntent(R.id.notif_btn_cast, openAppPendingIntent)
        }

        // 2. Collapsed RemoteViews for compact notification tray
        val collapsedViews = RemoteViews(packageName, R.layout.notification_player_collapsed).apply {
            setImageViewBitmap(R.id.notif_collapsed_art, artBitmap)
            setTextViewText(R.id.notif_collapsed_title, title)
            setTextViewText(R.id.notif_collapsed_artist, artist)
            setImageViewResource(R.id.notif_collapsed_fav, favIconRes)
            setImageViewResource(R.id.notif_collapsed_prev, R.drawable.ic_notif_prev)
            setImageViewResource(R.id.notif_collapsed_play_pause, playPauseIconRes)
            setImageViewResource(R.id.notif_collapsed_next, R.drawable.ic_notif_next)
            setImageViewResource(R.id.notif_collapsed_close, R.drawable.ic_notif_close)

            setOnClickPendingIntent(R.id.notif_collapsed_container, openAppPendingIntent)
            setOnClickPendingIntent(R.id.notif_collapsed_art, openAppPendingIntent)
            setOnClickPendingIntent(R.id.notif_collapsed_fav, favPendingIntent)
            setOnClickPendingIntent(R.id.notif_collapsed_prev, prevPendingIntent)
            setOnClickPendingIntent(R.id.notif_collapsed_play_pause, playPausePendingIntent)
            setOnClickPendingIntent(R.id.notif_collapsed_next, nextPendingIntent)
            setOnClickPendingIntent(R.id.notif_collapsed_close, stopPendingIntent)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_play)
            .setContentIntent(openAppPendingIntent)
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
    }

    private fun loadArtworkBitmap(song: Song): Bitmap {
        if (song.id == cachedArtSongId && cachedArtBitmap != null) {
            return cachedArtBitmap!!
        }

        var rawBmp: Bitmap? = null

        // 1. Try album art URI
        try {
            if (!song.albumArtUriString.isNullOrBlank()) {
                val uri = Uri.parse(song.albumArtUriString)
                contentResolver.openInputStream(uri)?.use { stream ->
                    rawBmp = BitmapFactory.decodeStream(stream)
                }
            }
        } catch (e: Exception) {
            // Fallback
        }

        // 2. Try embedded picture via MediaMetadataRetriever
        if (rawBmp == null) {
            try {
                if (song.contentUriString.isNotBlank()) {
                    val mmr = MediaMetadataRetriever()
                    mmr.setDataSource(this, Uri.parse(song.contentUriString))
                    val embedded = mmr.embeddedPicture
                    mmr.release()
                    if (embedded != null) {
                        rawBmp = BitmapFactory.decodeByteArray(embedded, 0, embedded.size)
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        val finalBmp = if (rawBmp != null) {
            getRoundedCornerBitmap(rawBmp!!, 24f)
        } else {
            createDefaultArtBitmap(song.title)
        }

        cachedArtSongId = song.id
        cachedArtBitmap = finalBmp
        return finalBmp
    }

    private fun getRoundedCornerBitmap(bitmap: Bitmap, cornerRadiusPx: Float): Bitmap {
        val size = 160
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply { isAntiAlias = true }
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, 0f, 0f, paint)

        // Draw translucent badge in bottom right corner with musical note (matching user screenshot)
        paint.xfermode = null
        paint.color = Color.parseColor("#99FFFFFF")
        canvas.drawCircle(size - 22f, size - 22f, 16f, paint)
        paint.color = Color.parseColor("#121316")
        paint.textSize = 20f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("♫", size - 22f, size - 15f, paint)

        return output
    }

    private fun createDefaultArtBitmap(title: String): Bitmap {
        val size = 160
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply { isAntiAlias = true }

        // Draw modern dark gradient background with rounded corners
        val shader = LinearGradient(
            0f, 0f, size.toFloat(), size.toFloat(),
            Color.parseColor("#E50914"), Color.parseColor("#800000"),
            Shader.TileMode.CLAMP
        )
        paint.shader = shader
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawRoundRect(rect, 24f, 24f, paint)

        // Draw center music note symbol
        paint.shader = null
        paint.color = Color.WHITE
        paint.textSize = 68f
        paint.textAlign = Paint.Align.CENTER
        val fontMetrics = paint.fontMetrics
        val y = (size / 2f) - ((fontMetrics.descent + fontMetrics.ascent) / 2f)
        canvas.drawText("♫", size / 2f, y, paint)

        return output
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
        const val ACTION_TOGGLE_FAVORITE = "com.example.action.TOGGLE_FAVORITE"
        const val ACTION_FAST_FORWARD = "com.example.action.FAST_FORWARD"
        const val ACTION_REWIND = "com.example.action.REWIND"
        const val ACTION_STOP = "com.example.action.STOP"
        const val ACTION_UPDATE_NOTIFICATION = "com.example.action.UPDATE_NOTIFICATION"
    }
}
