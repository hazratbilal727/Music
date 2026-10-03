package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.local.HiddenFolderEntity
import com.example.data.local.MusicDatabase
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSongCrossRef
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.SongEntity
import com.example.data.model.Album
import com.example.data.model.Artist
import com.example.data.model.Genre
import com.example.data.model.MusicFolder
import com.example.data.model.Playlist
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(
    private val context: Context,
    private val database: MusicDatabase
) {
    private val songDao = database.songDao()
    private val playlistDao = database.playlistDao()
    private val searchHistoryDao = database.searchHistoryDao()
    private val hiddenFolderDao = database.hiddenFolderDao()

    val allSongs: Flow<List<Song>> = songDao.getAllSongs().map { list ->
        list.map { it.toSong() }
    }.flowOn(Dispatchers.IO)

    val favoriteSongs: Flow<List<Song>> = songDao.getFavoriteSongs().map { list ->
        list.map { it.toSong() }
    }.flowOn(Dispatchers.IO)

    val recentlyPlayed: Flow<List<Song>> = songDao.getRecentlyPlayedSongs().map { list ->
        list.map { it.toSong() }
    }.flowOn(Dispatchers.IO)

    val mostPlayed: Flow<List<Song>> = songDao.getMostPlayedSongs().map { list ->
        list.map { it.toSong() }
    }.flowOn(Dispatchers.IO)

    val recentlyAdded: Flow<List<Song>> = songDao.getRecentlyAddedSongs().map { list ->
        list.map { it.toSong() }
    }.flowOn(Dispatchers.IO)

    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists().map { list ->
        list.map { Playlist(id = it.id, name = it.name, createdAt = it.createdAt) }
    }.flowOn(Dispatchers.IO)

    val searchHistory: Flow<List<SearchHistoryEntity>> = searchHistoryDao.getRecentSearches()

    val hiddenFolders: Flow<List<HiddenFolderEntity>> = hiddenFolderDao.getAllHiddenFolders()

    val folders: Flow<List<MusicFolder>> = combine(allSongs, hiddenFolders) { songs, hiddenList ->
        val hiddenPaths = hiddenList.map { it.folderPath }.toSet()
        songs.groupBy { it.folderPath }.map { (path, folderSongs) ->
            val name = folderSongs.firstOrNull()?.folderName?.ifBlank { File(path).name } ?: File(path).name
            MusicFolder(
                name = name,
                path = path,
                songCount = folderSongs.size,
                isHidden = hiddenPaths.contains(path)
            )
        }.sortedBy { it.name.lowercase() }
    }.flowOn(Dispatchers.IO)

    fun getSongsForFolder(folderPath: String): Flow<List<Song>> =
        songDao.getSongsByFolder(folderPath).map { list ->
            list.map { it.toSong() }
        }.flowOn(Dispatchers.IO)

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> =
        playlistDao.getSongsForPlaylist(playlistId).map { list ->
            list.map { it.toSong() }
        }.flowOn(Dispatchers.IO)

    fun getSongsForAlbum(albumName: String): Flow<List<Song>> =
        songDao.getSongsByAlbum(albumName).map { list ->
            list.map { it.toSong() }
        }.flowOn(Dispatchers.IO)

    fun getSongsForArtist(artistName: String): Flow<List<Song>> =
        songDao.getSongsByArtist(artistName).map { list ->
            list.map { it.toSong() }
        }.flowOn(Dispatchers.IO)

    fun getSongsForGenre(genreName: String): Flow<List<Song>> =
        songDao.getSongsByGenre(genreName).map { list ->
            list.map { it.toSong() }
        }.flowOn(Dispatchers.IO)

    suspend fun getSongById(songId: Long): Song? = withContext(Dispatchers.IO) {
        songDao.getSongById(songId)?.toSong()
    }

    val albums: Flow<List<Album>> = allSongs.map { songs ->
        songs.groupBy { it.album }.map { (albumName, albumSongs) ->
            val first = albumSongs.first()
            Album(
                id = first.albumId,
                name = albumName,
                artist = first.artist,
                songCount = albumSongs.size,
                artworkUriString = first.albumArtUriString,
                year = albumSongs.map { it.year }.filter { it > 0 }.maxOrNull() ?: 0
            )
        }.sortedBy { it.name.lowercase() }
    }.flowOn(Dispatchers.IO)

    val artists: Flow<List<Artist>> = allSongs.map { songs ->
        songs.groupBy { it.artist }.map { (artistName, artistSongs) ->
            val albumCount = artistSongs.map { it.album }.distinct().size
            Artist(
                name = artistName,
                songCount = artistSongs.size,
                albumCount = albumCount
            )
        }.sortedBy { it.name.lowercase() }
    }.flowOn(Dispatchers.IO)

    val genres: Flow<List<Genre>> = allSongs.map { songs ->
        songs.groupBy { it.genre.ifBlank { "Unknown" } }.map { (genreName, genreSongs) ->
            Genre(
                name = genreName,
                songCount = genreSongs.size
            )
        }.sortedByDescending { it.songCount }
    }.flowOn(Dispatchers.IO)

    suspend fun clearSampleSongs() = withContext(Dispatchers.IO) {
        try {
            songDao.clearSampleSongs()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun scanMediaStore(minDurationMs: Long = 3000L): Int = withContext(Dispatchers.IO) {
        val songsList = mutableListOf<SongEntity>()
        val mediaStoreIds = mutableListOf<Long>()

        // Ensure legacy mock/sample songs are pruned so only authentic device media is preserved
        songDao.clearSampleSongs()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE
        )

        val selection = "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%') AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(minDurationMs.toString())
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val trackCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val dateAddedCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dataCol = c.getColumnIndex(MediaStore.Audio.Media.DATA)
                val sizeCol = c.getColumnIndex(MediaStore.Audio.Media.SIZE)

                while (c.moveToNext()) {
                    val mediaStoreId = c.getLong(idCol)
                    val rawTitle = c.getString(titleCol)
                    val rawArtist = c.getString(artistCol)
                    val rawAlbum = c.getString(albumCol)
                    val albumId = c.getLong(albumIdCol)
                    val duration = c.getLong(durationCol)
                    val track = c.getInt(trackCol)
                    val year = c.getInt(yearCol)
                    val dateAdded = c.getLong(dateAddedCol) * 1000L
                    val filePath = if (dataCol != -1) c.getString(dataCol) ?: "" else ""
                    val fileSize = if (sizeCol != -1) c.getLong(sizeCol) else 0L

                    // Clean and refine titles and artists
                    val fallbackTitle = if (filePath.isNotBlank()) File(filePath).nameWithoutExtension else "Track $mediaStoreId"
                    val title = if (rawTitle.isNullOrBlank() || rawTitle == "<unknown>") fallbackTitle else rawTitle.trim()
                    val artist = if (rawArtist.isNullOrBlank() || rawArtist == "<unknown>") "Unknown Artist" else rawArtist.trim()
                    val album = if (rawAlbum.isNullOrBlank() || rawAlbum == "<unknown>") "Unknown Album" else rawAlbum.trim()

                    val folderPath = if (filePath.isNotBlank()) {
                        File(filePath).parent ?: "/storage/emulated/0/Music"
                    } else {
                        "/storage/emulated/0/Music"
                    }
                    val folderName = File(folderPath).name.ifBlank { "Music" }

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        mediaStoreId
                    )
                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )

                    mediaStoreIds.add(mediaStoreId)

                    val existing = songDao.getSongByMediaStoreId(mediaStoreId)

                    val entity = SongEntity(
                        id = existing?.id ?: mediaStoreId,
                        mediaStoreId = mediaStoreId,
                        title = existing?.title ?: title,
                        artist = existing?.artist ?: artist,
                        album = existing?.album ?: album,
                        albumId = albumId,
                        durationMs = duration,
                        contentUriString = contentUri.toString(),
                        albumArtUriString = albumArtUri.toString(),
                        genre = existing?.genre ?: "Music",
                        year = if ((existing?.year ?: 0) > 0) existing!!.year else year,
                        trackNumber = track,
                        isFavorite = existing?.isFavorite ?: false,
                        playCount = existing?.playCount ?: 0,
                        lastPlayedTimestamp = existing?.lastPlayedTimestamp ?: 0L,
                        dateAdded = dateAdded,
                        isSample = false,
                        folderName = folderName,
                        folderPath = folderPath,
                        lyrics = existing?.lyrics,
                        fileSize = if (fileSize > 0) fileSize else (existing?.fileSize ?: 0L),
                        bitrate = if (duration > 0 && fileSize > 0) ((fileSize * 8) / (duration)).toInt().coerceIn(64, 320) else 320,
                        sampleRate = 44100
                    )
                    songsList.add(entity)
                }
            }

            if (songsList.isNotEmpty()) {
                songsList.forEach { songDao.insertOrUpdateSong(it) }
                songDao.removeDeletedSongs(mediaStoreIds)
            } else {
                songDao.removeDeletedSongs(emptyList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        songsList.size
    }

    suspend fun toggleFavorite(song: Song) = withContext(Dispatchers.IO) {
        val newFav = !song.isFavorite
        songDao.updateFavorite(song.id, newFav)
    }

    suspend fun recordPlayback(songId: Long) = withContext(Dispatchers.IO) {
        songDao.recordPlayback(songId, System.currentTimeMillis())
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name.trim()))
    }

    suspend fun duplicatePlaylist(playlistId: Long, newName: String): Long = withContext(Dispatchers.IO) {
        val existingSongs = playlistDao.getSongsForPlaylist(playlistId).first()
        val newId = playlistDao.insertPlaylist(PlaylistEntity(name = newName.trim()))
        val refs = existingSongs.mapIndexed { idx, s ->
            PlaylistSongCrossRef(playlistId = newId, songId = s.id, position = idx)
        }
        playlistDao.insertPlaylistSongs(refs)
        newId
    }

    suspend fun saveQueueAsPlaylist(name: String, songs: List<Song>): Long = withContext(Dispatchers.IO) {
        val newId = playlistDao.insertPlaylist(PlaylistEntity(name = name.trim()))
        val refs = songs.mapIndexed { idx, s ->
            PlaylistSongCrossRef(playlistId = newId, songId = s.id, position = idx)
        }
        playlistDao.insertPlaylistSongs(refs)
        newId
    }

    suspend fun reorderSongInPlaylist(playlistId: Long, fromPos: Int, toPos: Int) = withContext(Dispatchers.IO) {
        val currentSongs = playlistDao.getSongsForPlaylist(playlistId).first().toMutableList()
        if (fromPos in currentSongs.indices && toPos in currentSongs.indices) {
            val moved = currentSongs.removeAt(fromPos)
            currentSongs.add(toPos, moved)
            playlistDao.clearPlaylistSongs(playlistId)
            val refs = currentSongs.mapIndexed { idx, s ->
                PlaylistSongCrossRef(playlistId = playlistId, songId = s.id, position = idx)
            }
            playlistDao.insertPlaylistSongs(refs)
        }
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) = withContext(Dispatchers.IO) {
        playlistDao.renamePlaylist(playlistId, newName.trim())
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        val count = playlistDao.getSongCountForPlaylist(playlistId).first()
        playlistDao.addSongToPlaylist(
            PlaylistSongCrossRef(playlistId = playlistId, songId = songId, position = count)
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    suspend fun hideFolder(folderPath: String, folderName: String) = withContext(Dispatchers.IO) {
        hiddenFolderDao.hideFolder(HiddenFolderEntity(folderPath, folderName))
    }

    suspend fun unhideFolder(folderPath: String) = withContext(Dispatchers.IO) {
        hiddenFolderDao.unhideFolder(folderPath)
    }

    suspend fun addSearchQuery(query: String) = withContext(Dispatchers.IO) {
        if (query.isNotBlank()) {
            searchHistoryDao.insertSearch(SearchHistoryEntity(query = query.trim()))
        }
    }

    suspend fun deleteSearchQuery(id: Long) = withContext(Dispatchers.IO) {
        searchHistoryDao.deleteSearch(id)
    }

    suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        searchHistoryDao.clearAll()
    }

    suspend fun updateSongMetadata(
        songId: Long,
        title: String,
        artist: String,
        album: String,
        genre: String,
        year: Int,
        lyrics: String?
    ) = withContext(Dispatchers.IO) {
        songDao.updateSongMetadata(songId, title, artist, album, genre, year, lyrics)
    }

    suspend fun deleteSong(songId: Long) = withContext(Dispatchers.IO) {
        songDao.deleteSong(songId)
    }

    suspend fun exportPlaylistsToJson(): String = withContext(Dispatchers.IO) {
        val playlists = playlistDao.getAllPlaylists().first()
        val builder = StringBuilder("[\n")
        playlists.forEachIndexed { i, p ->
            val songs = playlistDao.getSongsForPlaylist(p.id).first()
            builder.append("  {\n")
            builder.append("    \"name\": \"${p.name.replace("\"", "\\\"")}\",\n")
            builder.append("    \"songs\": [")
            songs.forEachIndexed { j, s ->
                builder.append("\"${s.title.replace("\"", "\\\"")}\"")
                if (j < songs.size - 1) builder.append(", ")
            }
            builder.append("]\n")
            builder.append("  }")
            if (i < playlists.size - 1) builder.append(",")
            builder.append("\n")
        }
        builder.append("]")
        builder.toString()
    }

    suspend fun restorePlaylistsFromJson(json: String): Int = withContext(Dispatchers.IO) {
        var restoredCount = 0
        try {
            val trimmed = json.trim()
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                val array = org.json.JSONArray(trimmed)
                val allSongsList = songDao.getAllSongs().first()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val name = obj.optString("name", "Restored Playlist")
                    val songsArray = obj.optJSONArray("songs")
                    val playlistId = playlistDao.insertPlaylist(PlaylistEntity(name = name))
                    var pos = 0
                    if (songsArray != null) {
                        for (j in 0 until songsArray.length()) {
                            val songTitle = songsArray.getString(j)
                            val found = allSongsList.find { it.title.equals(songTitle, ignoreCase = true) }
                            if (found != null) {
                                playlistDao.addSongToPlaylist(
                                    PlaylistSongCrossRef(playlistId = playlistId, songId = found.id, position = pos++)
                                )
                            }
                        }
                    }
                    restoredCount++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        restoredCount
    }
}
