package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.HiddenFolderEntity
import com.example.data.model.Album
import com.example.data.model.Artist
import com.example.data.model.MainTab
import com.example.data.model.MusicFolder
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.playback.PlaybackState
import com.example.ui.components.AlbumCard
import com.example.ui.components.ArtistCard
import com.example.ui.components.EmptyMediaView
import com.example.ui.components.MiniPlayer
import com.example.ui.components.PlaylistCard
import com.example.ui.components.SongItemRow

@Composable
fun MainMusicScreen(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    // Data
    allSongs: List<Song>,
    folders: List<MusicFolder>,
    hiddenFolders: List<HiddenFolderEntity>,
    playlists: List<Playlist>,
    favoriteSongs: List<Song>,
    recentlyAdded: List<Song>,
    mostPlayed: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    playbackState: PlaybackState,
    // Handlers
    onSongClick: (Song, List<Song>) -> Unit,
    onFolderClick: (MusicFolder) -> Unit,
    onPlayFolder: (MusicFolder) -> Unit,
    onAddFolderToQueue: (MusicFolder) -> Unit,
    onHideFolder: (MusicFolder) -> Unit,
    onUnhideFolder: (String) -> Unit,
    onRescanFolders: () -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenRecentlyAdded: () -> Unit,
    onOpenMostPlayed: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onRenamePlaylist: (Playlist) -> Unit = {},
    onDeletePlaylist: (Playlist) -> Unit = {},
    onDuplicatePlaylist: (Playlist) -> Unit = {},
    onToggleFavorite: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onShowSongDetails: ((Song) -> Unit)? = null,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onPlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14))
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar matching screenshot
            TopBrandHeader(
                onSearchClick = onNavigateToSearch,
                onVideoClick = onNavigateToSearch,
                onSettingsClick = onNavigateToSettings
            )

            // Horizontal Pill Tabs Row (Songs, Playlist, Folders, Albums, Artists)
            PillNavigationTabs(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    MainTab.FOLDERS -> {
                        FoldersScreen(
                            folders = folders,
                            hiddenFolders = hiddenFolders,
                            onFolderClick = onFolderClick,
                            onPlayFolder = onPlayFolder,
                            onAddFolderToQueue = onAddFolderToQueue,
                            onHideFolder = onHideFolder,
                            onUnhideFolder = onUnhideFolder,
                            onRescanFolders = onRescanFolders
                        )
                    }

                    MainTab.SONGS -> {
                        if (allSongs.isEmpty()) {
                            EmptyMediaView(
                                icon = Icons.Default.MusicNote,
                                title = "No Music Found",
                                subtitle = "No audio files detected on your device. Add music (.mp3, .m4a, .flac, .wav) to storage, then rescan.",
                                actionText = "Rescan Library",
                                onAction = onRescanFolders
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 120.dp)
                            ) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${allSongs.size} songs",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 15.sp
                                            ),
                                            color = Color(0xFF90939F)
                                        )
                                    }
                                }

                                items(allSongs, key = { it.id }) { song ->
                                    SongItemRow(
                                        song = song,
                                        isPlayingThis = playbackState.currentSong?.id == song.id,
                                        onClick = { onSongClick(song, allSongs) },
                                        onToggleFavorite = { onToggleFavorite(song) },
                                        onPlayNext = { onPlayNext(song) },
                                        onAddToQueue = { onAddToQueue(song) },
                                        onAddToPlaylist = { onAddToPlaylist(song) },
                                        onShowDetails = { onShowSongDetails?.invoke(song) }
                                    )
                                }
                            }
                        }
                    }

                    MainTab.PLAYLIST -> {
                        PlaylistsScreen(
                            playlists = playlists,
                            favoriteSongs = favoriteSongs,
                            recentlyAdded = recentlyAdded,
                            mostPlayed = mostPlayed,
                            onOpenPlaylist = onPlaylistClick,
                            onOpenFavorites = onOpenFavorites,
                            onOpenRecentlyAdded = onOpenRecentlyAdded,
                            onOpenMostPlayed = onOpenMostPlayed,
                            onCreatePlaylistClick = onCreatePlaylist,
                            onRenamePlaylist = onRenamePlaylist,
                            onDeletePlaylist = onDeletePlaylist,
                            onDuplicatePlaylist = onDuplicatePlaylist
                        )
                    }

                    MainTab.ALBUMS -> {
                        if (albums.isEmpty()) {
                            EmptyMediaView(
                                icon = Icons.Default.Album,
                                title = "No Albums Found",
                                subtitle = "Albums will appear here once audio files are detected on your device.",
                                actionText = "Rescan Library",
                                onAction = onRescanFolders
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 140.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 120.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(albums, key = { it.id }) { album ->
                                    AlbumCard(
                                        album = album,
                                        onClick = { onAlbumClick(album) }
                                    )
                                }
                            }
                        }
                    }

                    MainTab.ARTISTS -> {
                        if (artists.isEmpty()) {
                            EmptyMediaView(
                                icon = Icons.Default.Person,
                                title = "No Artists Found",
                                subtitle = "Artists will appear here once audio files are detected on your device.",
                                actionText = "Rescan Library",
                                onAction = onRescanFolders
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 110.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 120.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(artists, key = { it.name }) { artist ->
                                    ArtistCard(
                                        artist = artist,
                                        onClick = { onArtistClick(artist) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Mini Player Docked
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            MiniPlayer(
                state = playbackState,
                onPlayPause = onPlayPause,
                onNext = onNextTrack,
                onClick = onOpenNowPlaying
            )
        }
    }
}

@Composable
private fun TopBrandHeader(
    onSearchClick: () -> Unit,
    onVideoClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Brand Title with PRO badge
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MUSIC",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    letterSpacing = 0.5.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.width(6.dp))

            // PRO Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE5A910)) // Warm Golden yellow
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PRO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp
                    ),
                    color = Color.Black
                )
            }
        }

        // Top Right Icons: Search, Video, Hexagon Settings
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFFD6D8E2),
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = onVideoClick,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SmartDisplay,
                    contentDescription = "Video",
                    tint = Color(0xFFD6D8E2),
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Hexagon,
                    contentDescription = "Settings",
                    tint = Color(0xFFD6D8E2),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun PillNavigationTabs(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    val tabs = listOf(
        MainTab.SONGS to "Songs",
        MainTab.PLAYLIST to "Playlist",
        MainTab.FOLDERS to "Folders",
        MainTab.ALBUMS to "Albums",
        MainTab.ARTISTS to "Artists"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { (tab, label) ->
            val isSelected = selectedTab == tab
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color.White else Color(0xFF1E2028)
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 18.dp, vertical = 9.dp)
                    .testTag("tab_${label.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = if (isSelected) Color.Black else Color(0xFFA5A8B6)
                )
            }
        }
    }
}
