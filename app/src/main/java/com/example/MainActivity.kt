package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MusicViewModel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.ui.Screen
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.DeletePlaylistConfirmDialog
import com.example.ui.components.EqualizerDialog
import com.example.ui.components.QueueDialog
import com.example.ui.components.RenamePlaylistDialog
import com.example.ui.components.SaveQueueAsPlaylistDialog
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.SongDetailsDialog
import com.example.ui.screens.AlbumDetailScreen
import com.example.ui.screens.ArtistDetailScreen
import com.example.ui.screens.FolderDetailScreen
import com.example.ui.screens.MainMusicScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MusicPlayerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MusicViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            var showSplash by remember { mutableStateOf(true) }

            MusicPlayerTheme(
                themeMode = themeMode
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    MainAppContent(viewModel = viewModel)

                    AnimatedVisibility(
                        visible = showSplash,
                        enter = fadeIn(),
                        exit = fadeOut(animationSpec = tween(400))
                    ) {
                        SplashScreen(
                            onDismiss = { showSplash = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MusicViewModel) {
    val context = LocalContext.current
    val currentScreen by viewModel.screen.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    val allSongs by viewModel.allSongs.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val hiddenFolders by viewModel.hiddenFolders.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val mostPlayed by viewModel.mostPlayed.collectAsState()
    val recentlyAdded by viewModel.recentlyAdded.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val genres by viewModel.genres.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val selectedMainTab by viewModel.selectedMainTab.collectAsState()

    val showNowPlaying by viewModel.showNowPlaying.collectAsState()
    val showEqualizer by viewModel.showEqualizer.collectAsState()
    val showSleepTimer by viewModel.showSleepTimer.collectAsState()
    val showQueue by viewModel.showQueue.collectAsState()
    val showCreatePlaylist by viewModel.showCreatePlaylist.collectAsState()
    val showSaveQueueAsPlaylist by viewModel.showSaveQueueAsPlaylist.collectAsState()
    val playlistToRename by viewModel.playlistToRename.collectAsState()
    val playlistToDelete by viewModel.playlistToDelete.collectAsState()
    val songToAddToPlaylist by viewModel.songToAddToPlaylist.collectAsState()
    val songForDetails by viewModel.songForDetails.collectAsState()

    val themeMode by viewModel.themeMode.collectAsState()
    val pauseOnUnplug by viewModel.pauseOnUnplug.collectAsState()
    val resumePosition by viewModel.resumePlayback.collectAsState()
    val gaplessPlayback by viewModel.gaplessPlayback.collectAsState()
    val filterShortAudios by viewModel.filterShortAudios.collectAsState()
    val crossfadeSeconds by viewModel.crossfadeSeconds.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val lastScanTime by viewModel.lastScanTime.collectAsState()

    // Permissions check
    val permissionsToRequest = remember {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.READ_MEDIA_AUDIO)
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        list.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { it }) {
            viewModel.rescanLibrary()
        }
    }

    LaunchedEffect(Unit) {
        val needsRequest = permissionsToRequest.any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needsRequest) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    // Hardware back navigation
    BackHandler {
        if (!viewModel.navigateBack()) {
            val activity = context as? ComponentActivity
            activity?.finish()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val screen = currentScreen) {
            is Screen.Main -> {
                MainMusicScreen(
                    selectedTab = selectedMainTab,
                    onTabSelected = { viewModel.setSelectedMainTab(it) },
                    allSongs = allSongs,
                    folders = folders,
                    hiddenFolders = hiddenFolders,
                    playlists = playlists,
                    favoriteSongs = favoriteSongs,
                    recentlyAdded = recentlyAdded,
                    mostPlayed = mostPlayed,
                    albums = albums,
                    artists = artists,
                    playbackState = playbackState,
                    onSongClick = { song, queue -> viewModel.playSong(song, queue) },
                    onFolderClick = { folder -> viewModel.navigateTo(Screen.FolderDetail(folder)) },
                    onPlayFolder = { viewModel.playFolder(it) },
                    onAddFolderToQueue = { viewModel.addFolderToQueue(it) },
                    onHideFolder = { viewModel.hideFolder(it) },
                    onUnhideFolder = { viewModel.unhideFolder(it) },
                    onRescanFolders = { viewModel.rescanLibrary() },
                    onAlbumClick = { album -> viewModel.navigateTo(Screen.AlbumDetail(album)) },
                    onArtistClick = { artist -> viewModel.navigateTo(Screen.ArtistDetail(artist)) },
                    onPlaylistClick = { playlist -> viewModel.navigateTo(Screen.PlaylistDetail(playlist)) },
                    onOpenFavorites = {
                        viewModel.navigateTo(
                            Screen.SmartPlaylistDetail("Favorites", Screen.SmartPlaylistDetail.SmartType.FAVORITES)
                        )
                    },
                    onOpenRecentlyAdded = {
                        viewModel.navigateTo(
                            Screen.SmartPlaylistDetail("Recently Added", Screen.SmartPlaylistDetail.SmartType.RECENTLY_ADDED)
                        )
                    },
                    onOpenMostPlayed = {
                        viewModel.navigateTo(
                            Screen.SmartPlaylistDetail("Most Played", Screen.SmartPlaylistDetail.SmartType.MOST_PLAYED)
                        )
                    },
                    onCreatePlaylist = { viewModel.setCreatePlaylistVisible(true) },
                    onRenamePlaylist = { viewModel.setPlaylistToRename(it) },
                    onDeletePlaylist = { viewModel.setPlaylistToDelete(it) },
                    onDuplicatePlaylist = { viewModel.duplicatePlaylist(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onPlayNext = { viewModel.playNextInQueue(it) },
                    onAddToQueue = { viewModel.addToQueue(it) },
                    onAddToPlaylist = { viewModel.setSongToAddToPlaylist(it) },
                    onShowSongDetails = { viewModel.setSongForDetails(it) },
                    onNavigateToSearch = { viewModel.navigateTo(Screen.Search) },
                    onNavigateToSettings = { viewModel.navigateTo(Screen.Settings) },
                    onPlayPause = { viewModel.togglePlayPause() },
                    onNextTrack = { viewModel.playNext() },
                    onOpenNowPlaying = { viewModel.setNowPlayingVisible(true) },
                    onLoadSamplePack = { viewModel.loadSamplePack() }
                )
            }

            is Screen.FolderDetail -> {
                val folderSongs by viewModel.repository.getSongsForFolder(screen.folder.path)
                    .collectAsState(initial = emptyList())
                FolderDetailScreen(
                    folderName = screen.folder.name,
                    folderPath = screen.folder.path,
                    songs = folderSongs,
                    playbackState = playbackState,
                    onBack = { viewModel.navigateBack() },
                    onSongClick = { song, queue -> viewModel.playSong(song, queue) },
                    onPlayAll = {
                        if (folderSongs.isNotEmpty()) viewModel.playSong(folderSongs[0], folderSongs, 0)
                    },
                    onShuffleAll = {
                        if (folderSongs.isNotEmpty()) {
                            val shuffled = folderSongs.shuffled()
                            viewModel.playSong(shuffled[0], shuffled, 0)
                        }
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onPlayNext = { viewModel.playNextInQueue(it) },
                    onAddToQueue = { viewModel.addToQueue(it) },
                    onAddToPlaylist = { viewModel.setSongToAddToPlaylist(it) },
                    onShowDetails = { viewModel.setSongForDetails(it) }
                )
            }

            is Screen.Search -> {
                SearchScreen(
                    allSongs = allSongs,
                    allAlbums = albums,
                    allArtists = artists,
                    allPlaylists = playlists,
                    searchHistory = searchHistory,
                    playbackState = playbackState,
                    onSongClick = { song, queue -> viewModel.playSong(song, queue) },
                    onAlbumClick = { album -> viewModel.navigateTo(Screen.AlbumDetail(album)) },
                    onArtistClick = { artist -> viewModel.navigateTo(Screen.ArtistDetail(artist)) },
                    onPlaylistClick = { playlist -> viewModel.navigateTo(Screen.PlaylistDetail(playlist)) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onPlayNext = { viewModel.playNextInQueue(it) },
                    onAddToQueue = { viewModel.addToQueue(it) },
                    onAddToPlaylist = { viewModel.setSongToAddToPlaylist(it) },
                    onSearchCommitted = { viewModel.addSearchQuery(it) },
                    onDeleteHistoryItem = { viewModel.deleteSearchHistoryItem(it) },
                    onClearHistory = { viewModel.clearSearchHistory() },
                    allFolders = folders,
                    allGenres = genres,
                    onFolderClick = { viewModel.navigateTo(Screen.FolderDetail(it)) },
                    onGenreClick = { viewModel.navigateTo(Screen.GenreDetail(it)) },
                    onShowSongDetails = { viewModel.setSongForDetails(it) }
                )
            }

            is Screen.Settings -> {
                SettingsScreen(
                    currentThemeMode = themeMode,
                    onThemeModeChange = { viewModel.setThemeMode(it) },
                    onOpenEqualizer = { viewModel.setEqualizerVisible(true) },
                    onOpenSleepTimer = { viewModel.setSleepTimerVisible(true) },
                    onRescanLibrary = { viewModel.rescanLibrary() },
                    onBack = { viewModel.navigateBack() },
                    pauseOnUnplug = pauseOnUnplug,
                    onPauseOnUnplugChange = { viewModel.setPauseOnUnplug(it) },
                    resumePosition = resumePosition,
                    onResumePositionChange = { viewModel.setResumePlayback(it) },
                    gaplessPlayback = gaplessPlayback,
                    onGaplessPlaybackChange = { viewModel.setGaplessPlayback(it) },
                    filterShortAudios = filterShortAudios,
                    onFilterShortAudiosChange = { viewModel.setFilterShortAudios(it) },
                    crossfadeSeconds = crossfadeSeconds,
                    onCrossfadeSecondsChange = { viewModel.setCrossfadeSeconds(it) },
                    isScanning = isScanning,
                    lastScanTime = lastScanTime,
                    onClearCache = { viewModel.clearAppCache() },
                    songsCount = allSongs.size,
                    albumsCount = albums.size,
                    artistsCount = artists.size,
                    foldersCount = folders.size,
                    totalStorageBytes = allSongs.sumOf { it.fileSize }
                )
            }

            is Screen.PlaylistDetail -> {
                val playlistSongs by viewModel.repository.getSongsForPlaylist(screen.playlist.id)
                    .collectAsState(initial = emptyList())
                PlaylistDetailScreen(
                    playlistName = screen.playlist.name,
                    songs = playlistSongs,
                    playbackState = playbackState,
                    onBack = { viewModel.navigateBack() },
                    onSongClick = { song, queue -> viewModel.playSong(song, queue) },
                    onPlayAll = {
                        if (playlistSongs.isNotEmpty()) viewModel.playSong(playlistSongs[0], playlistSongs, 0)
                    },
                    onShuffleAll = {
                        if (playlistSongs.isNotEmpty()) {
                            val shuffled = playlistSongs.shuffled()
                            viewModel.playSong(shuffled[0], shuffled, 0)
                        }
                    },
                    onRemoveSong = { viewModel.removeSongFromPlaylist(screen.playlist.id, it.id) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onPlayNext = { viewModel.playNextInQueue(it) },
                    onAddToQueue = { viewModel.addToQueue(it) },
                    onAddToPlaylist = { viewModel.setSongToAddToPlaylist(it) },
                    onShowDetails = { viewModel.setSongForDetails(it) }
                )
            }

            is Screen.SmartPlaylistDetail -> {
                val songs = when (screen.type) {
                    Screen.SmartPlaylistDetail.SmartType.FAVORITES -> favoriteSongs
                    Screen.SmartPlaylistDetail.SmartType.RECENTLY_ADDED -> recentlyAdded
                    Screen.SmartPlaylistDetail.SmartType.MOST_PLAYED -> mostPlayed
                }
                PlaylistDetailScreen(
                    playlistName = screen.title,
                    songs = songs,
                    playbackState = playbackState,
                    onBack = { viewModel.navigateBack() },
                    onSongClick = { song, queue -> viewModel.playSong(song, queue) },
                    onPlayAll = {
                        if (songs.isNotEmpty()) viewModel.playSong(songs[0], songs, 0)
                    },
                    onShuffleAll = {
                        if (songs.isNotEmpty()) {
                            val shuffled = songs.shuffled()
                            viewModel.playSong(shuffled[0], shuffled, 0)
                        }
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onPlayNext = { viewModel.playNextInQueue(it) },
                    onAddToQueue = { viewModel.addToQueue(it) },
                    onAddToPlaylist = { viewModel.setSongToAddToPlaylist(it) },
                    onShowDetails = { viewModel.setSongForDetails(it) }
                )
            }

            is Screen.AlbumDetail -> {
                val albumSongs by viewModel.repository.getSongsForAlbum(screen.album.name)
                    .collectAsState(initial = emptyList())
                AlbumDetailScreen(
                    album = screen.album,
                    songs = albumSongs,
                    playbackState = playbackState,
                    onBack = { viewModel.navigateBack() },
                    onSongClick = { song, queue -> viewModel.playSong(song, queue) },
                    onPlayAll = {
                        if (albumSongs.isNotEmpty()) viewModel.playSong(albumSongs[0], albumSongs, 0)
                    },
                    onShuffleAll = {
                        if (albumSongs.isNotEmpty()) {
                            val shuffled = albumSongs.shuffled()
                            viewModel.playSong(shuffled[0], shuffled, 0)
                        }
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onPlayNext = { viewModel.playNextInQueue(it) },
                    onAddToQueue = { viewModel.addToQueue(it) },
                    onAddToPlaylist = { viewModel.setSongToAddToPlaylist(it) },
                    onShowDetails = { viewModel.setSongForDetails(it) }
                )
            }

            is Screen.ArtistDetail -> {
                val artistSongs by viewModel.repository.getSongsForArtist(screen.artist.name)
                    .collectAsState(initial = emptyList())
                ArtistDetailScreen(
                    artist = screen.artist,
                    songs = artistSongs,
                    playbackState = playbackState,
                    onBack = { viewModel.navigateBack() },
                    onSongClick = { song, queue -> viewModel.playSong(song, queue) },
                    onPlayAll = {
                        if (artistSongs.isNotEmpty()) viewModel.playSong(artistSongs[0], artistSongs, 0)
                    },
                    onShuffleAll = {
                        if (artistSongs.isNotEmpty()) {
                            val shuffled = artistSongs.shuffled()
                            viewModel.playSong(shuffled[0], shuffled, 0)
                        }
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onPlayNext = { viewModel.playNextInQueue(it) },
                    onAddToQueue = { viewModel.addToQueue(it) },
                    onAddToPlaylist = { viewModel.setSongToAddToPlaylist(it) },
                    onShowDetails = { viewModel.setSongForDetails(it) }
                )
            }

            is Screen.GenreDetail -> {
                val genreSongs by viewModel.repository.getSongsForGenre(screen.genre.name)
                    .collectAsState(initial = emptyList())
                PlaylistDetailScreen(
                    playlistName = screen.genre.name,
                    songs = genreSongs,
                    playbackState = playbackState,
                    onBack = { viewModel.navigateBack() },
                    onSongClick = { song, queue -> viewModel.playSong(song, queue) },
                    onPlayAll = {
                        if (genreSongs.isNotEmpty()) viewModel.playSong(genreSongs[0], genreSongs, 0)
                    },
                    onShuffleAll = {
                        if (genreSongs.isNotEmpty()) {
                            val shuffled = genreSongs.shuffled()
                            viewModel.playSong(shuffled[0], shuffled, 0)
                        }
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onPlayNext = { viewModel.playNextInQueue(it) },
                    onAddToQueue = { viewModel.addToQueue(it) },
                    onAddToPlaylist = { viewModel.setSongToAddToPlaylist(it) },
                    onShowDetails = { viewModel.setSongForDetails(it) }
                )
            }
        }

        // Full Screen Animated Now Playing
        AnimatedVisibility(
            visible = showNowPlaying && playbackState.currentSong != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            NowPlayingScreen(
                state = playbackState,
                onBack = { viewModel.setNowPlayingVisible(false) },
                onPlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.playNext() },
                onPrevious = { viewModel.playPrevious() },
                onSeek = { viewModel.seekTo(it) },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleRepeat = { viewModel.toggleRepeat() },
                onToggleFavorite = { playbackState.currentSong?.let { viewModel.toggleFavorite(it) } },
                onOpenQueue = { viewModel.setQueueVisible(true) },
                onOpenEqualizer = { viewModel.setEqualizerVisible(true) },
                onOpenSleepTimer = { viewModel.setSleepTimerVisible(true) },
                onCycleSpeed = { viewModel.cyclePlaybackSpeed() },
                onFastForward = { viewModel.fastForward() },
                onRewind = { viewModel.rewind() },
                onShowSongDetails = { viewModel.setSongForDetails(it) },
                onAddToPlaylist = { viewModel.setSongToAddToPlaylist(it) },
                onArtworkChangeUri = { songId, uri -> viewModel.updateSongArtwork(songId, uri) }
            )
        }

        // Equalizer Dialog
        if (showEqualizer) {
            EqualizerDialog(
                controller = viewModel.playbackManager.equalizerController,
                onDismiss = { viewModel.setEqualizerVisible(false) }
            )
        }

        // Sleep Timer Dialog
        if (showSleepTimer) {
            SleepTimerDialog(
                currentRemainingSeconds = playbackState.sleepTimerRemainingSeconds,
                onSetTimerMinutes = { viewModel.setSleepTimerMinutes(it) },
                onCancelTimer = { viewModel.cancelSleepTimer() },
                onDismiss = { viewModel.setSleepTimerVisible(false) }
            )
        }

        // Queue Dialog
        if (showQueue) {
            QueueDialog(
                queue = playbackState.queue,
                currentIndex = playbackState.queueIndex,
                onPlayTrackAt = { index ->
                    if (index in playbackState.queue.indices) {
                        viewModel.playSong(playbackState.queue[index], playbackState.queue, index)
                    }
                },
                onRemoveFromQueue = { viewModel.removeFromQueue(it) },
                onClearQueue = { viewModel.clearQueue() },
                onMoveQueueItem = { from, to -> viewModel.moveQueueItem(from, to) },
                onSaveQueueAsPlaylist = { viewModel.setShowSaveQueueAsPlaylist(true) },
                onDismiss = { viewModel.setQueueVisible(false) }
            )
        }

        // Create Playlist Dialog
        if (showCreatePlaylist) {
            CreatePlaylistDialog(
                onConfirm = { viewModel.createPlaylist(it) },
                onDismiss = { viewModel.setCreatePlaylistVisible(false) }
            )
        }

        // Save Queue as Playlist Dialog
        if (showSaveQueueAsPlaylist) {
            SaveQueueAsPlaylistDialog(
                onConfirm = { viewModel.saveQueueAsPlaylist(it) },
                onDismiss = { viewModel.setShowSaveQueueAsPlaylist(false) }
            )
        }

        // Rename Playlist Dialog
        playlistToRename?.let { playlist ->
            RenamePlaylistDialog(
                playlist = playlist,
                onConfirm = { newName -> viewModel.renamePlaylist(playlist, newName) },
                onDismiss = { viewModel.setPlaylistToRename(null) }
            )
        }

        // Delete Playlist Dialog
        playlistToDelete?.let { playlist ->
            DeletePlaylistConfirmDialog(
                playlist = playlist,
                onConfirm = { viewModel.deletePlaylist(playlist) },
                onDismiss = { viewModel.setPlaylistToDelete(null) }
            )
        }

        // Add To Playlist Dialog
        songToAddToPlaylist?.let { song ->
            AddToPlaylistDialog(
                song = song,
                playlists = playlists,
                onSelectPlaylist = { playlist -> viewModel.addSongToPlaylist(playlist.id, song.id) },
                onCreateNewPlaylist = { viewModel.setCreatePlaylistVisible(true) },
                onDismiss = { viewModel.setSongToAddToPlaylist(null) }
            )
        }

        // Song Details Dialog
        songForDetails?.let { song ->
            SongDetailsDialog(
                song = song,
                onDismiss = { viewModel.setSongForDetails(null) }
            )
        }
    }
}
