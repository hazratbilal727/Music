package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.playback.MusicService
import com.example.playback.PlaybackState

class MusicWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, null)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            state: PlaybackState?
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_music_player)

            val song = state?.currentSong
            val isPlaying = state?.isPlaying == true

            val title = song?.title ?: "Music Player"
            val artist = song?.artist ?: "Tap to play"

            views.setTextViewText(R.id.widget_song_title, title)
            views.setTextViewText(R.id.widget_song_artist, artist)

            val playPauseIcon = if (isPlaying) {
                android.R.drawable.ic_media_pause
            } else {
                android.R.drawable.ic_media_play
            }
            views.setImageViewResource(R.id.widget_btn_play_pause, playPauseIcon)

            // Open App
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, openPendingIntent)

            // Prev
            val prevIntent = Intent(context, MusicService::class.java).apply {
                action = MusicService.ACTION_PREVIOUS
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_prev,
                PendingIntent.getService(
                    context, 10, prevIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            // Play/Pause
            val playIntent = Intent(context, MusicService::class.java).apply {
                action = MusicService.ACTION_PLAY_PAUSE
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_play_pause,
                PendingIntent.getService(
                    context, 11, playIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            // Next
            val nextIntent = Intent(context, MusicService::class.java).apply {
                action = MusicService.ACTION_NEXT
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_next,
                PendingIntent.getService(
                    context, 12, nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context, state: PlaybackState) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, MusicWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (widgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId, state)
            }
        }
    }
}
