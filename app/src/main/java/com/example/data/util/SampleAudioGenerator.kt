package com.example.data.util

import android.content.Context
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

data class SampleTrackSpec(
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val durationSeconds: Int,
    val year: Int,
    val lyrics: String,
    val chordFrequencies: List<List<Double>>
)

object SampleAudioGenerator {

    val SAMPLE_TRACKS = listOf(
        SampleTrackSpec(
            title = "Midnight Horizon",
            artist = "Aura Sound",
            album = "Neon Drift",
            genre = "Synthwave",
            durationSeconds = 24,
            year = 2026,
            lyrics = """
                [00:00.00]Cruising down the empty highway
                [00:04.00]Neon lights illuminate the skyline
                [00:08.00]Basslines pulse through the dashboard
                [00:12.00]Electric dreams in midnight twilight
                [00:16.00]Lost in the rhythm of the city
                [00:20.00]Chasing the dawn into eternity
            """.trimIndent(),
            chordFrequencies = listOf(
                listOf(220.0, 261.63, 329.63), // Am
                listOf(174.61, 220.0, 261.63), // F
                listOf(261.63, 329.63, 392.0), // C
                listOf(196.0, 246.94, 293.66)  // G
            )
        ),
        SampleTrackSpec(
            title = "Acoustic Sunset",
            artist = "Luna Grove",
            album = "Whispering Pines",
            genre = "Acoustic",
            durationSeconds = 20,
            year = 2025,
            lyrics = """
                [00:00.00]Sunlight filtering through the trees
                [00:04.00]Carried away by the gentle breeze
                [00:08.00]Strumming chords of golden peace
                [00:12.00]Watching all the troubles cease
                [00:16.00]Peaceful river flows into twilight
            """.trimIndent(),
            chordFrequencies = listOf(
                listOf(261.63, 329.63, 392.0), // C
                listOf(196.0, 246.94, 293.66), // G
                listOf(220.0, 261.63, 329.63), // Am
                listOf(174.61, 220.0, 261.63)  // F
            )
        ),
        SampleTrackSpec(
            title = "Lo-Fi Coffee Shop",
            artist = "Velvet Beats",
            album = "Midnight Brew",
            genre = "Lo-Fi",
            durationSeconds = 22,
            year = 2026,
            lyrics = """
                [00:00.00]Steam rises from the porcelain cup
                [00:04.00]Raindrops tapping on the windowsill
                [00:08.00]Mellow keys and vinyl texture
                [00:13.00]Time stands quiet and still
                [00:17.00]Finding serenity in simple moments
            """.trimIndent(),
            chordFrequencies = listOf(
                listOf(146.83, 220.0, 261.63, 349.23), // Dm7
                listOf(196.0, 246.94, 293.66, 349.23), // G7
                listOf(130.81, 196.0, 246.94, 329.63), // Cmaj7
                listOf(220.0, 261.63, 329.63, 392.0)   // Am7
            )
        ),
        SampleTrackSpec(
            title = "Cosmic Odyssey",
            artist = "Solaris",
            album = "Starlight Voyage",
            genre = "Electronic",
            durationSeconds = 25,
            year = 2026,
            lyrics = """
                [00:00.00]Drifting beyond gravitational pull
                [00:05.00]Nebula colors shimmering brightly
                [00:10.00]A symphony echoing through space
                [00:15.00]Stars aligning in cosmic harmony
                [00:20.00]Infinite wonders waiting beyond
            """.trimIndent(),
            chordFrequencies = listOf(
                listOf(130.81, 196.0, 261.63, 392.0), // Csus2
                listOf(174.61, 261.63, 349.23, 523.25), // Fsus2
                listOf(196.0, 293.66, 392.0, 587.33), // Gsus2
                listOf(220.0, 329.63, 440.0, 659.25)  // Asus2
            )
        )
    )

    fun ensureSampleFilesExist(context: Context): List<File> {
        val dir = File(context.filesDir, "sample_audio")
        if (!dir.exists()) dir.mkdirs()

        return SAMPLE_TRACKS.mapIndexed { index, spec ->
            val file = File(dir, "sample_track_${index + 1}.wav")
            if (!file.exists() || file.length() < 1000L) {
                generateWav(file, spec)
            }
            file
        }
    }

    private fun generateWav(outputFile: File, spec: SampleTrackSpec) {
        val sampleRate = 44100
        val duration = spec.durationSeconds
        val totalSamples = sampleRate * duration
        val numChannels = 1
        val bitsPerSample = 16
        val dataSize = totalSamples * numChannels * (bitsPerSample / 8)
        val totalFileSize = 36 + dataSize

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(totalFileSize)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // Subchunk1Size for PCM
            putShort(1.toShort()) // AudioFormat (1 = PCM)
            putShort(numChannels.toShort())
            putInt(sampleRate)
            putInt(sampleRate * numChannels * (bitsPerSample / 8)) // ByteRate
            putShort((numChannels * (bitsPerSample / 8)).toShort()) // BlockAlign
            putShort(bitsPerSample.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
        }

        BufferedOutputStream(FileOutputStream(outputFile), 32768).use { stream ->
            stream.write(header.array())

            val chords = spec.chordFrequencies
            val chordDurationSeconds = 3.0
            val samplesPerChord = (sampleRate * chordDurationSeconds).toInt()

            val sampleBuffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                val chordIndex = ((i / samplesPerChord) % chords.size)
                val currentChord = chords[chordIndex]

                val chordTime = (i % samplesPerChord).toDouble() / sampleRate
                val attack = (chordTime / 0.15).coerceIn(0.0, 1.0)
                val decay = (1.0 - (chordTime / chordDurationSeconds).coerceIn(0.0, 1.0)).coerceIn(0.2, 1.0)
                val chordEnv = attack * decay

                val trackFadeIn = (t / 1.0).coerceIn(0.0, 1.0)
                val trackFadeOut = ((duration - t) / 1.5).coerceIn(0.0, 1.0)
                val masterEnv = trackFadeIn * trackFadeOut

                var sampleSum = 0.0
                for (freq in currentChord) {
                    val phase = 2.0 * PI * freq * t
                    val tone = sin(phase) + 0.3 * sin(2.0 * phase)
                    sampleSum += tone
                }
                sampleSum = (sampleSum / currentChord.size) * chordEnv * masterEnv

                val intVal = (sampleSum * 24000.0).toInt().coerceIn(-32767, 32767)
                sampleBuffer.clear()
                sampleBuffer.putShort(intVal.toShort())
                stream.write(sampleBuffer.array())
            }
            stream.flush()
        }
    }
}
