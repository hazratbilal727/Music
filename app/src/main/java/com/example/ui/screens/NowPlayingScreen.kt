package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import com.example.playback.PlaybackState
import com.example.ui.components.ArtworkImage
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NowPlayingScreen(
    state: PlaybackState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onCycleSpeed: () -> Unit,
    onFastForward: (() -> Unit)? = null,
    onRewind: (() -> Unit)? = null,
    onShowSongDetails: ((Song) -> Unit)? = null,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onOpenThemePicker: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val song = state.currentSong ?: return

    var isUserScrubbing by remember { mutableStateOf(false) }
    var scrubProgress by remember { mutableFloatStateOf(0f) }
    var showLyrics by remember { mutableStateOf(false) }

    val currentMs = if (isUserScrubbing) {
        (scrubProgress * state.durationMs).toLong()
    } else {
        state.currentPositionMs
    }
    val formattedCurrent = "%02d:%02d".format((currentMs / 1000) / 60, (currentMs / 1000) % 60)
    val totalMs = state.durationMs.coerceAtLeast(0L)
    val formattedTotal = "%02d:%02d".format((totalMs / 1000) / 60, (totalMs / 1000) % 60)

    val currentProgress = if (isUserScrubbing) scrubProgress else state.progress.coerceIn(0f, 1f)
    val lyricsList = remember(song.lyrics) { song.parsedLyrics }

    // Pure black styling as required
    val backgroundColor = Color(0xFF000000) // Deep pure black
    val iconMutedColor = Color(0xFF90939F)
    val accentRedColor = Color(0xFFE53935)
    val circleTrackColor = Color(0xFF1E1F24)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .testTag("now_playing_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Bar: Collapse Button, Centered Header & Pill, Info Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Down Chevron to Collapse Player
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("now_playing_collapse")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse Player",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Centered Drag Pill + Header Label
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onBack() }
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF45474F))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp,
                            fontSize = 11.sp
                        ),
                        color = Color(0xFF9597A2)
                    )
                }

                // Song Info / Details Button
                IconButton(
                    onClick = { onShowSongDetails?.invoke(song) },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("now_playing_details")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Song Info",
                        tint = iconMutedColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 2. Header Row: Shuffle, Title/Artist, Repeat
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Shuffle Button
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("now_playing_shuffle")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (state.isShuffle) accentRedColor else iconMutedColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Song Title & Artist
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            letterSpacing = (-0.2).sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${song.artist} • ${song.album}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = Color(0xFFA5A7B2),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                // Repeat Button
                IconButton(
                    onClick = onToggleRepeat,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("now_playing_repeat")
                ) {
                    val (icon, tint) = when (state.repeatMode) {
                        RepeatMode.OFF -> Pair(Icons.Default.Repeat, iconMutedColor)
                        RepeatMode.ALL -> Pair(Icons.Default.Repeat, accentRedColor)
                        RepeatMode.ONE -> Pair(Icons.Default.RepeatOne, accentRedColor)
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Repeat",
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time Display: "00:00 / 03:30"
            Text(
                text = "$formattedCurrent / $formattedTotal",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                ),
                color = Color(0xFFC0C2CB)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Center Area: Circular Player with Circular Progress Arc OR Synchronized Lyrics
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentAlignment = Alignment.Center
            ) {
                if (!showLyrics) {
                    Box(
                        modifier = Modifier.size(286.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Canvas for circular track and progress scrubber
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(totalMs) {
                                    detectTapGestures { offset ->
                                        val center = Offset(size.width / 2f, size.height / 2f)
                                        val touchVector = offset - center
                                        val angle = Math.toDegrees(atan2(touchVector.y.toDouble(), touchVector.x.toDouble())).toFloat()
                                        val normalizedAngle = (angle + 90f + 360f) % 360f
                                        val newProgress = (normalizedAngle / 360f).coerceIn(0f, 1f)
                                        val targetMs = (newProgress * totalMs).toLong()
                                        onSeek(targetMs)
                                    }
                                }
                                .pointerInput(totalMs) {
                                    detectDragGestures(
                                        onDragStart = { isUserScrubbing = true },
                                        onDragEnd = {
                                            val targetMs = (scrubProgress * totalMs).toLong()
                                            onSeek(targetMs)
                                            isUserScrubbing = false
                                        },
                                        onDragCancel = { isUserScrubbing = false }
                                    ) { change, _ ->
                                        change.consume()
                                        val center = Offset(size.width / 2f, size.height / 2f)
                                        val touchVector = change.position - center
                                        val angle = Math.toDegrees(atan2(touchVector.y.toDouble(), touchVector.x.toDouble())).toFloat()
                                        val normalizedAngle = (angle + 90f + 360f) % 360f
                                        scrubProgress = (normalizedAngle / 360f).coerceIn(0f, 1f)
                                    }
                                }
                        ) {
                            val strokeWidth = 5.dp.toPx()
                            val radius = (size.minDimension - strokeWidth) / 2f
                            val center = Offset(size.width / 2f, size.height / 2f)

                            // Background circular track
                            drawCircle(
                                color = circleTrackColor,
                                radius = radius,
                                center = center,
                                style = Stroke(width = strokeWidth)
                            )

                            // Active progress arc
                            val sweepAngle = currentProgress * 360f
                            drawArc(
                                color = accentRedColor,
                                startAngle = -90f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = Offset(center.x - radius, center.y - radius),
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            // Solid white scrubber thumb
                            val thumbAngleRad = Math.toRadians((sweepAngle - 90.0))
                            val thumbX = center.x + (radius * cos(thumbAngleRad)).toFloat()
                            val thumbY = center.y + (radius * sin(thumbAngleRad)).toFloat()

                            // Outer subtle shadow for thumb
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.5f),
                                radius = 10.dp.toPx(),
                                center = Offset(thumbX, thumbY)
                            )
                            // Solid white thumb dot
                            drawCircle(
                                color = Color.White,
                                radius = 8.dp.toPx(),
                                center = Offset(thumbX, thumbY)
                            )
                        }

                        // Inner circular album artwork
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color(0xFF222328), CircleShape)
                                .clickable { showLyrics = true }
                        ) {
                            ArtworkImage(
                                artworkUri = song.albumArtUriString,
                                title = song.title,
                                size = 240.dp,
                                shape = CircleShape,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else {
                    // Synchronized / Formatted Lyrics Box
                    val lyricsState = rememberLazyListState()

                    // Auto-scroll to current lyric line
                    LaunchedEffect(currentMs) {
                        val activeIndex = lyricsList.indexOfLast { currentMs >= it.timestampMs }
                        if (activeIndex >= 0) {
                            lyricsState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.94f)
                            .height(286.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF111215))
                            .border(1.dp, Color(0xFF262730), RoundedCornerShape(22.dp))
                            .clickable { showLyrics = false }
                            .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (lyricsList.isEmpty()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Lyrics,
                                    contentDescription = null,
                                    tint = accentRedColor,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No Embedded Lyrics",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap anywhere to return to artwork",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB0B2BA)
                                )
                            }
                        } else {
                            LazyColumn(
                                state = lyricsState,
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                items(lyricsList) { line ->
                                    val isPassed = currentMs >= line.timestampMs
                                    Text(
                                        text = line.text,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isPassed) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = if (isPassed) 17.sp else 14.sp
                                        ),
                                        color = if (isPassed) accentRedColor else Color.White.copy(alpha = 0.5f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Utility Bar (5 icons: Playlist, Equalizer, Timer, Speed, More)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Add to Playlist
                IconButton(
                    onClick = { onAddToPlaylist?.invoke(song) },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("now_playing_add_to_playlist")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = "Add to playlist",
                        tint = iconMutedColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // 2. Equalizer
                IconButton(
                    onClick = onOpenEqualizer,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("now_playing_equalizer")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Equalizer",
                        tint = iconMutedColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 3. Sleep Timer
                IconButton(
                    onClick = onOpenSleepTimer,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("now_playing_sleep_timer")
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Sleep timer",
                        tint = if (state.sleepTimerRemainingSeconds != null) accentRedColor else iconMutedColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 4. Playback Speed Selector (e.g. 1.0x, 1.25x)
                IconButton(
                    onClick = onCycleSpeed,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("now_playing_speed")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${state.playbackSpeed}x",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = if (state.playbackSpeed != 1.0f) accentRedColor else iconMutedColor
                        )
                    }
                }

                // 5. More Options / Song Info
                IconButton(
                    onClick = { onShowSongDetails?.invoke(song) },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("now_playing_more_options")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "More options",
                        tint = iconMutedColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 6. Main Playback Controls: Rewind 10s, Prev, Big Red Play/Pause, Next, Forward 10s
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Rewind 10s
                IconButton(
                    onClick = { onRewind?.invoke() },
                    modifier = Modifier
                        .size(46.dp)
                        .testTag("now_playing_rewind")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind 10s",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Previous Track
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("now_playing_prev")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Center Vibrant Red Play/Pause Button
                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(70.dp)
                        .shadow(16.dp, CircleShape, spotColor = accentRedColor)
                        .testTag("now_playing_play_pause"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = accentRedColor,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Next Track
                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("now_playing_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Forward 10s
                IconButton(
                    onClick = { onFastForward?.invoke() },
                    modifier = Modifier
                        .size(46.dp)
                        .testTag("now_playing_forward")
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 7. Bottom Row: Favorite, Lyrics toggle, Queue
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Favorite Heart
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("now_playing_favorite")
                ) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (song.isFavorite) accentRedColor else iconMutedColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Lyrics Button (Chat Bubble + "Lyrics")
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showLyrics = !showLyrics }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("now_playing_lyrics_toggle"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Lyrics",
                        tint = if (showLyrics) accentRedColor else iconMutedColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lyrics",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        ),
                        color = if (showLyrics) accentRedColor else iconMutedColor
                    )
                }

                // Queue Button
                IconButton(
                    onClick = onOpenQueue,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("now_playing_queue")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Queue",
                        tint = iconMutedColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
