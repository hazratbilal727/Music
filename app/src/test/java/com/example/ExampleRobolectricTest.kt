package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Music", appName)
    }

    @Test
    fun `song formatted duration test`() {
        val song = Song(
            id = 1L,
            mediaStoreId = 1L,
            title = "Test Song",
            artist = "Artist",
            album = "Album",
            durationMs = 215000L, // 3 min 35 sec
            contentUriString = "content://media/external/audio/media/1"
        )
        assertEquals("3:35", song.formattedDuration())
    }
}
