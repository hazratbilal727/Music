package com.example

import android.app.Application
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.example.data.local.MusicDatabase
import com.example.data.repository.MusicRepository
import com.example.playback.PlaybackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MusicApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mediaObserver: ContentObserver? = null

    val database by lazy { MusicDatabase.getDatabase(this) }
    val repository by lazy { MusicRepository(this, database) }
    val playbackManager by lazy { PlaybackManager.getInstance(this, repository) }

    override fun onCreate() {
        super.onCreate()

        // Real-time automatic MediaStore monitoring for library updates
        registerMediaStoreObserver()

        // Initial scan of real device media
        applicationScope.launch {
            try {
                repository.clearSampleSongs()
                repository.scanMediaStore()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun registerMediaStoreObserver() {
        try {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                private var debounceJob: Job? = null

                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    debounceJob?.cancel()
                    debounceJob = applicationScope.launch {
                        delay(1000)
                        try {
                            repository.scanMediaStore()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
            mediaObserver = observer
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
