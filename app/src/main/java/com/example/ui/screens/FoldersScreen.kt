package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.HiddenFolderEntity
import com.example.data.model.FolderSortOrder
import com.example.data.model.MusicFolder
import com.example.data.model.Song
import kotlinx.coroutines.launch

@Composable
fun FoldersScreen(
    folders: List<MusicFolder>,
    hiddenFolders: List<HiddenFolderEntity>,
    onFolderClick: (MusicFolder) -> Unit,
    onPlayFolder: (MusicFolder) -> Unit,
    onAddFolderToQueue: (MusicFolder) -> Unit,
    onHideFolder: (MusicFolder) -> Unit,
    onUnhideFolder: (String) -> Unit,
    onRescanFolders: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sortOrder by remember { mutableStateOf(FolderSortOrder.NAME_ASC) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showHiddenFoldersDialog by remember { mutableStateOf(false) }
    var showDirectoriesDialog by remember { mutableStateOf(false) }

    val visibleFolders = remember(folders, sortOrder) {
        val nonHidden = folders.filter { !it.isHidden }
        when (sortOrder) {
            FolderSortOrder.NAME_ASC -> nonHidden.sortedBy { it.name.lowercase() }
            FolderSortOrder.NAME_DESC -> nonHidden.sortedByDescending { it.name.lowercase() }
            FolderSortOrder.SONG_COUNT_DESC -> nonHidden.sortedByDescending { it.songCount }
            FolderSortOrder.PATH_ASC -> nonHidden.sortedBy { it.path.lowercase() }
        }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Distinct alphabetical letters for index bar
    val alphabetIndex = remember(visibleFolders) {
        visibleFolders.map { it.name.take(1).uppercase() }
            .filter { it.matches(Regex("[A-Z]")) }
            .distinct()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("folders_screen")
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Header count and sort icon
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${visibleFolders.size} folders",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        ),
                        color = Color(0xFF90939F)
                    )

                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort Folders",
                                tint = Color(0xFFC0C2CB),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Name (A-Z)") },
                                onClick = {
                                    sortOrder = FolderSortOrder.NAME_ASC
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (Z-A)") },
                                onClick = {
                                    sortOrder = FolderSortOrder.NAME_DESC
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Most Songs") },
                                onClick = {
                                    sortOrder = FolderSortOrder.SONG_COUNT_DESC
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("File Path") },
                                onClick = {
                                    sortOrder = FolderSortOrder.PATH_ASC
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Directories & Hidden folders cards row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Directories card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showDirectoriesDialog = true },
                        color = Color(0xFF1B1C22),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = Color(0xFFE24A4A),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Directories",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                ),
                                color = Color(0xFFE2E4EC)
                            )
                        }
                    }

                    // Hidden folders card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showHiddenFoldersDialog = true },
                        color = Color(0xFF1B1C22),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = Color(0xFFE24A4A),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Hidden folders",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                ),
                                color = Color(0xFFE2E4EC)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Folder items list
            if (visibleFolders.isEmpty()) {
                item {
                    com.example.ui.components.EmptyMediaView(
                        icon = Icons.Default.FolderOpen,
                        title = "No Audio Folders Found",
                        subtitle = "No folders containing audio tracks were detected on your device.",
                        actionText = "Rescan Folders",
                        onAction = onRescanFolders
                    )
                }
            } else {
                items(visibleFolders, key = { it.path }) { folder ->
                    FolderItemRow(
                        folder = folder,
                        onClick = { onFolderClick(folder) },
                        onPlay = { onPlayFolder(folder) },
                        onAddToQueue = { onAddFolderToQueue(folder) },
                        onHide = { onHideFolder(folder) }
                    )
                }
            }
        }

        // Alphabet Fast Scroll Scrubber on right edge (matches screenshot)
        if (alphabetIndex.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp, top = 80.dp, bottom = 120.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF15161B).copy(alpha = 0.8f))
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.SpaceAround,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                alphabetIndex.take(12).forEach { letter ->
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF888B97),
                        modifier = Modifier
                            .clickable {
                                val target = visibleFolders.indexOfFirst {
                                    it.name.uppercase().startsWith(letter)
                                }
                                if (target != -1) {
                                    scope.launch {
                                        listState.scrollToItem(target + 2) // offset by header & cards
                                    }
                                }
                            }
                            .padding(vertical = 2.dp)
                    )
                }
            }
        }
    }

    // Hidden Folders Dialog
    if (showHiddenFoldersDialog) {
        AlertDialog(
            onDismissRequest = { showHiddenFoldersDialog = false },
            title = { Text("Hidden Folders (${hiddenFolders.size})") },
            text = {
                if (hiddenFolders.isEmpty()) {
                    Text("No hidden folders. You can hide any folder using its three-dot menu.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(hiddenFolders) { hidden ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(hidden.folderName, fontWeight = FontWeight.SemiBold)
                                    Text(hidden.folderPath, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                TextButton(onClick = { onUnhideFolder(hidden.folderPath) }) {
                                    Text("Unhide")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHiddenFoldersDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Directories Dialog
    if (showDirectoriesDialog) {
        AlertDialog(
            onDismissRequest = { showDirectoriesDialog = false },
            title = { Text("Music Directories") },
            text = {
                Column {
                    Text("All scanned music locations on your device:")
                    Spacer(modifier = Modifier.height(8.dp))
                    folders.take(6).forEach { f ->
                        Text("• ${f.path}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onRescanFolders()
                    showDirectoriesDialog = false
                }) {
                    Text("Rescan All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDirectoriesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun FolderItemRow(
    folder: MusicFolder,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .testTag("folder_item_${folder.name}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Yellow/orange folder icon (matching screenshot)
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = "Folder",
            tint = Color(0xFFF79A1A), // vibrant amber-orange folder
            modifier = Modifier.size(46.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = Color(0xFFEDEDF2),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Subtitle: e.g. "36 songs · /storage/emulated/0/Download"
            Text(
                text = "${folder.songCount} songs · ${folder.path}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp
                ),
                color = Color(0xFF7A7D8A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "Options",
                    tint = Color(0xFF707380),
                    modifier = Modifier.size(24.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Play All") },
                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onPlay()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add to Queue") },
                    leadingIcon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onAddToQueue()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Hide Folder") },
                    leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onHide()
                    }
                )
            }
        }
    }
}
