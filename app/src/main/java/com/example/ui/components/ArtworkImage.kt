package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlin.math.abs

@Composable
fun ArtworkImage(
    artworkUri: String?,
    title: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        val hash = abs((title + (artworkUri ?: "")).hashCode())
        val gradients = listOf(
            listOf(Color(0xFF6366F1), Color(0xFFA855F7)),
            listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)),
            listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)),
            listOf(Color(0xFF10B981), Color(0xFF059669)),
            listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
            listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4))
        )
        val selectedGradient = gradients[hash % gradients.size]

        if (!artworkUri.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artworkUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Album Artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    DefaultArtBadge(selectedGradient, size)
                },
                error = {
                    DefaultArtBadge(selectedGradient, size)
                }
            )
        } else {
            DefaultArtBadge(selectedGradient, size)
        }
    }
}

@Composable
private fun DefaultArtBadge(gradient: List<Color>, size: Dp) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors = gradient)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(size * 0.45f)
        )
    }
}
