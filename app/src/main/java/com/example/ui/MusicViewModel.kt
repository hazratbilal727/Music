package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MusicApplication
import com.example.data.local.HiddenFolderEntity
import com.example.data.model.Album
import com.example.data.model.Artist
import com.example.data.model.Genre
import com.example.data.model.MainTab
import com.example.data.model.MusicFolder
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.model.SortOrder
import com.example.playback.PlaybackState
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface Screen {
    object Main : Screen
    object Search : Screen
    object Settings : Screen
    data class PlaylistDetail(val playlist: Playlist) : Screen
    data class SmartPlaylistDetail(val title: String, val type: SmartType) : Screen {
        enum class SmartType { FAVORITES, RECENTLY_ADDED, MOST_PLAYED }
    }
    data class AlbumDetail(val album: Album) : Screen
    data class ArtistDetail(val artist: Artist) : Screen
    data class GenreDetail(val genre: Genre) : Screen
    data class FolderDetail(val folder: MusicFolder) : Screen
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MusicApplication
    val repository = app.repository
    val playbackManager = app.playbackManager

    val playbackState: StateFlow<PlaybackState> = playbackManager.state

    val allSongs: StateFlow<List<Song>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<Song>> = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<Song>> = repository.recentlyPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayed: StateFlow<List<Song>> = repository.mostPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAdded: StateFlow<List<Song>> = repository.recentlyAdded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<Album>> = repository.albums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<Artist>> = repository.artists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val genres: StateFlow<List<Genre>> = repository.genres
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchHistory = repository.searchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<MusicFolder>> = repository.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hiddenFolders: StateFlow<List<HiddenFolderEntity>> = repository.hiddenFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Main Tab Selection (Defaults to FOLDERS to match screenshot)
    private val _selectedMainTab = MutableStateFlow(MainTab.FOLDERS)
    val selectedMainTab: StateFlow<MainTab> = _selectedMainTab.asStateFlow()

    // Navigation & Screen Stack
    private val _screenStack = MutableStateFlow<List<Screen>>(listOf(Screen.Main))
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Main)
    val screen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Dialog & Sheet States
    private val _showNowPlaying = MutableStateFlow(false)
    val showNowPlaying: StateFlow<Boolean> = _showNowPlaying.asStateFlow()

    private val _showEqualizer = MutableStateFlow(false)
    val showEqualizer: StateFlow<Boolean> = _showEqualizer.asStateFlow()

    private val _showSleepTimer = MutableStateFlow(false)
    val showSleepTimer: StateFlow<Boolean> = _showSleepTimer.asStateFlow()

    private val _showQueue = MutableStateFlow(false)
    val showQueue: StateFlow<Boolean> = _showQueue.asStateFlow()

    private val _showCreatePlaylist = MutableStateFlow(false)
    val showCreatePlaylist: StateFlow<Boolean> = _showCreatePlaylist.asStateFlow()

    private val _showSaveQueueAsPlaylist = MutableStateFlow(false)
    val showSaveQueueAsPlaylist: StateFlow<Boolean> = _showSaveQueueAsPlaylist.asStateFlow()

    private val _playlistToRename = MutableStateFlow<Playlist?>(null)
    val playlistToRename: StateFlow<Playlist?> = _playlistToRename.asStateFlow()

    private val _playlistToDelete = MutableStateFlow<Playlist?>(null)
    val playlistToDelete: StateFlow<Playlist?> = _playlistToDelete.asStateFlow()

    private val _songToAddToPlaylist = MutableStateFlow<Song?>(null)
    val songToAddToPlaylist: StateFlow<Song?> = _songToAddToPlaylist.asStateFlow()

    private val _songForDetails = MutableStateFlow<Song?>(null)
    val songForDetails: StateFlow<Song?> = _songForDetails.asStateFlow()

    // Preferences
    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _pauseOnUnplug = MutableStateFlow(true)
    val pauseOnUnplug: StateFlow<Boolean> = _pauseOnUnplug.asStateFlow()

    private val _resumePlayback = MutableStateFlow(true)
    val resumePlayback: StateFlow<Boolean> = _resumePlayback.asStateFlow()

    private val _gaplessPlayback = MutableStateFlow(true)
    val gaplessPlayback: StateFlow<Boolean> = _gaplessPlayback.asStateFlow()

    private val _filterShortAudios = MutableStateFlow(true)
    val filterShortAudios: StateFlow<Boolean> = _filterShortAudios.asStateFlow()

    private val _crossfadeSeconds = MutableStateFlow(0)
    val crossfadeSeconds: StateFlow<Int> = _crossfadeSeconds.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _lastScanTime = MutableStateFlow("Just now")
    val lastScanTime: StateFlow<String> = _lastScanTime.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.TITLE_ASC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    fun setSelectedMainTab(tab: MainTab) {
        _selectedMainTab.value = tab
    }

    fun navigateTo(newScreen: Screen) {
        val stack = _screenStack.value.toMutableList()
        stack.add(newScreen)
        _screenStack.value = stack
        _currentScreen.value = newScreen
    }

    fun navigateBack(): Boolean {
        if (_showNowPlaying.value) {
            _showNowPlaying.value = false
            return true
        }
        val stack = _screenStack.value.toMutableList()
        if (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
            _screenStack.value = stack
            _currentScreen.value = stack.last()
            return true
        }
        return false
    }

    // Playback Controls
    fun playSong(song: Song, queue: List<Song> = listOf(song), index: Int = queue.indexOf(song).coerceAtLeast(0)) {
        playbackManager.playSong(song, queue, index)
        _showNowPlaying.value = true
    }

    fun loadSamplePack() {
        viewModelScope.launch {
            repository.seedSampleSongs()
        }
    }

    fun togglePlayPause() = playbackManager.togglePlayPause()
    fun playNext() = playbackManager.playNext()
    fun playPrevious() = playbackManager.playPrevious()
    fun seekTo(positionMs: Long) = playbackManager.seekTo(positionMs)
    fun toggleShuffle() = playbackManager.toggleShuffle()
    fun toggleRepeat() = playbackManager.toggleRepeatMode()

    fun cyclePlaybackSpeed() {
        val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        val currentSpeed = playbackState.value.playbackSpeed
        val nextIndex = (speeds.indexOf(currentSpeed) + 1) % speeds.size
        playbackManager.setPlaybackSpeed(speeds[nextIndex])
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song)
        }
    }

    fun addToQueue(song: Song) = playbackManager.addToQueue(song)
    fun playNextInQueue(song: Song) = playbackManager.playNextInQueue(song)
    fun removeFromQueue(index: Int) = playbackManager.removeFromQueue(index)
    fun moveQueueItem(fromIndex: Int, toIndex: Int) = playbackManager.moveQueueItem(fromIndex, toIndex)
    fun clearQueue() = playbackManager.clearQueue()
    fun fastForward(seconds: Int = 10) = playbackManager.fastForward(seconds)
    fun rewind(seconds: Int = 10) = playbackManager.rewind(seconds)
    fun setStopAfterCurrent(enabled: Boolean) = playbackManager.setStopAfterCurrent(enabled)

    // Folder Actions
    fun playFolder(folder: MusicFolder) {
        viewModelScope.launch {
            val folderSongs = repository.getSongsForFolder(folder.path).first()
            if (folderSongs.isNotEmpty()) {
                playSong(folderSongs[0], folderSongs, 0)
            }
        }
    }

    fun addFolderToQueue(folder: MusicFolder) {
        viewModelScope.launch {
            val folderSongs = repository.getSongsForFolder(folder.path).first()
            folderSongs.forEach { addToQueue(it) }
        }
    }

    fun hideFolder(folder: MusicFolder) {
        viewModelScope.launch {
            repository.hideFolder(folder.path, folder.name)
        }
    }

    fun unhideFolder(folderPath: String) {
        viewModelScope.launch {
            repository.unhideFolder(folderPath)
        }
    }

    // Dialog & Visibility Controls
    fun setNowPlayingVisible(visible: Boolean) {
        _showNowPlaying.value = visible
    }

    fun setEqualizerVisible(visible: Boolean) {
        _showEqualizer.value = visible
    }

    fun setSleepTimerVisible(visible: Boolean) {
        _showSleepTimer.value = visible
    }

    fun setQueueVisible(visible: Boolean) {
        _showQueue.value = visible
    }

    fun setCreatePlaylistVisible(visible: Boolean) {
        _showCreatePlaylist.value = visible
    }

    fun setSongToAddToPlaylist(song: Song?) {
        _songToAddToPlaylist.value = song
    }

    fun setPlaylistToRename(playlist: Playlist?) {
        _playlistToRename.value = playlist
    }

    fun setPlaylistToDelete(playlist: Playlist?) {
        _playlistToDelete.value = playlist
    }

    fun setShowSaveQueueAsPlaylist(visible: Boolean) {
        _showSaveQueueAsPlaylist.value = visible
    }

    fun setSongForDetails(song: Song?) {
        _songForDetails.value = song
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun toggleThemeMode() {
        _themeMode.value = if (_themeMode.value == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
    }

    fun setPauseOnUnplug(enabled: Boolean) {
        _pauseOnUnplug.value = enabled
    }

    fun setResumePlayback(enabled: Boolean) {
        _resumePlayback.value = enabled
    }

    fun setGaplessPlayback(enabled: Boolean) {
        _gaplessPlayback.value = enabled
    }

    fun setFilterShortAudios(enabled: Boolean) {
        _filterShortAudios.value = enabled
    }

    fun setCrossfadeSeconds(seconds: Int) {
        _crossfadeSeconds.value = seconds
    }

    fun setSleepTimerMinutes(minutes: Int) {
        playbackManager.setSleepTimer(minutes)
    }

    fun cancelSleepTimer() {
        playbackManager.cancelSleepTimer()
    }

    // Playlist actions
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
            _showCreatePlaylist.value = false
        }
    }

    fun duplicatePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.duplicatePlaylist(playlist.id, "${playlist.name} (Copy)")
        }
    }

    fun saveQueueAsPlaylist(name: String) {
        val currentQueue = playbackState.value.queue
        if (currentQueue.isNotEmpty()) {
            viewModelScope.launch {
                repository.saveQueueAsPlaylist(name, currentQueue)
                _showSaveQueueAsPlaylist.value = false
            }
        }
    }

    fun reorderSongInPlaylist(playlistId: Long, fromPos: Int, toPos: Int) {
        viewModelScope.launch {
            repository.reorderSongInPlaylist(playlistId, fromPos, toPos)
        }
    }

    fun renamePlaylist(playlist: Playlist, newName: String) {
        viewModelScope.launch {
            repository.renamePlaylist(playlist.id, newName)
            _playlistToRename.value = null
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist.id)
            _playlistToDelete.value = null
            if (_currentScreen.value is Screen.PlaylistDetail) {
                navigateBack()
            }
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
            _songToAddToPlaylist.value = null
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun updateSongArtwork(songId: Long, artworkUri: String?) {
        viewModelScope.launch {
            repository.updateSongArtwork(songId, artworkUri)
            playbackManager.updateCurrentSongArtwork(artworkUri)
        }
    }

    // Media & Library
    fun rescanLibrary() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                repository.scanMediaStore()
                val now = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                _lastScanTime.value = "Today at $now"
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun clearAppCache() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }

    // Search History
    fun addSearchQuery(query: String) {
        viewModelScope.launch {
            repository.addSearchQuery(query)
        }
    }

    fun deleteSearchHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteSearchQuery(id)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }
}
