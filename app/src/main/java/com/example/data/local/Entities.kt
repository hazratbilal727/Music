package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.Song

@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["mediaStoreId"], unique = true),
        Index(value = ["isFavorite"]),
        Index(value = ["lastPlayedTimestamp"]),
        Index(value = ["folderPath"])
    ]
)
data class SongEntity(
    @PrimaryKey val id: Long,
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
    fun toSong(): Song = Song(
        id = id,
        mediaStoreId = mediaStoreId,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        durationMs = durationMs,
        contentUriString = contentUriString,
        albumArtUriString = albumArtUriString,
        genre = genre,
        year = year,
        trackNumber = trackNumber,
        isFavorite = isFavorite,
        playCount = playCount,
        lastPlayedTimestamp = lastPlayedTimestamp,
        dateAdded = dateAdded,
        isSample = isSample,
        folderName = folderName,
        folderPath = folderPath,
        lyrics = lyrics,
        fileSize = fileSize,
        bitrate = bitrate,
        sampleRate = sampleRate
    )

    companion object {
        fun fromSong(song: Song): SongEntity = SongEntity(
            id = song.id,
            mediaStoreId = song.mediaStoreId,
            title = song.title,
            artist = song.artist,
            album = song.album,
            albumId = song.albumId,
            durationMs = song.durationMs,
            contentUriString = song.contentUriString,
            albumArtUriString = song.albumArtUriString,
            genre = song.genre,
            year = song.year,
            trackNumber = song.trackNumber,
            isFavorite = song.isFavorite,
            playCount = song.playCount,
            lastPlayedTimestamp = song.lastPlayedTimestamp,
            dateAdded = song.dateAdded,
            isSample = song.isSample,
            folderName = song.folderName,
            folderPath = song.folderPath,
            lyrics = song.lyrics,
            fileSize = song.fileSize,
            bitrate = song.bitrate,
            sampleRate = song.sampleRate
        )
    }
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["playlistId"]), Index(value = ["songId"])]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val position: Int = 0
)

@Entity(
    tableName = "search_history",
    indices = [Index(value = ["query"], unique = true)]
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "hidden_folders")
data class HiddenFolderEntity(
    @PrimaryKey val folderPath: String,
    val folderName: String
)
