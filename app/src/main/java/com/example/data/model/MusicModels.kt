package com.example.data.model

import android.net.Uri

data class Song(
    val id: Long,
    val mediaStoreId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long = 0,
    val durationMs: Long,
    val contentUriString: String,
    val albumArtUriString: String? = null,
    val genre: String = "Unknown",
    val year: Int = 0,
    val trackNumber: Int = 0,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedTimestamp: Long = 0L,
    val dateAdded: Long = 0L,
    val isSample: Boolean = false,
    val folderName: String = "Music",
    val folderPath: String = "/storage/emulated/0/Music",
    val lyrics: String? = null,
    val fileSize: Long = 0L,
    val bitrate: Int = 320,
    val sampleRate: Int = 44100
) {
    val contentUri: Uri get() = Uri.parse(contentUriString)
    val albumArtUri: Uri? get() = albumArtUriString?.let { Uri.parse(it) }

    fun formattedDuration(): String {
        val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    fun formattedFileSize(): String {
        if (fileSize <= 0) return "Unknown"
        val mb = fileSize.toDouble() / (1024 * 1024)
        return "%.1f MB".format(mb)
    }

    val parsedLyrics: List<LyricLine>
        get() = LyricsParser.parse(lyrics)
}

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

object LyricsParser {
    private val LRC_REGEX = Regex("""\[(\d{2}):(\d{2})(?:\.(\d{2,3}))?](.*)""")

    fun parse(lrcText: String?): List<LyricLine> {
        if (lrcText.isNullOrBlank()) return emptyList()
        val lines = mutableListOf<LyricLine>()
        var fallbackTime = 0L
        lrcText.lines().forEach { rawLine ->
            val trimmed = rawLine.trim()
            val match = LRC_REGEX.find(trimmed)
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val millisPart = match.groupValues[3]
                val ms = when (millisPart.length) {
                    2 -> (millisPart.toLongOrNull() ?: 0L) * 10
                    3 -> millisPart.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val totalMs = (min * 60 + sec) * 1000 + ms
                val text = match.groupValues[4].trim()
                if (text.isNotEmpty()) {
                    lines.add(LyricLine(totalMs, text))
                }
            } else if (trimmed.isNotBlank() && !trimmed.startsWith("[")) {
                lines.add(LyricLine(fallbackTime, trimmed))
                fallbackTime += 4000L
            }
        }
        return lines.sortedBy { it.timestampMs }
    }
}

data class MusicFolder(
    val name: String,
    val path: String,
    val songCount: Int,
    val isHidden: Boolean = false
)

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val songCount: Int,
    val artworkUriString: String? = null,
    val year: Int = 0
)

data class Artist(
    val name: String,
    val songCount: Int,
    val albumCount: Int = 1
)

data class Genre(
    val name: String,
    val songCount: Int
)

data class Playlist(
    val id: Long,
    val name: String,
    val songCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class PlaylistBackup(
    val name: String,
    val songs: List<String>
)

enum class MainTab {
    SONGS,
    PLAYLIST,
    FOLDERS,
    ALBUMS,
    ARTISTS
}

enum class LibraryTab {
    SONGS,
    ALBUMS,
    ARTISTS,
    GENRES
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class SortOrder {
    TITLE_ASC,
    TITLE_DESC,
    ARTIST_ASC,
    DURATION_DESC,
    DATE_ADDED_DESC
}

enum class FolderSortOrder {
    NAME_ASC,
    NAME_DESC,
    SONG_COUNT_DESC,
    PATH_ASC
}
