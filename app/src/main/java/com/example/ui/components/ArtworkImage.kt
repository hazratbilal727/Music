package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

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
            listOf(Color(0xFF00E676), Color(0xFF00B0FF)), // Emerald Green & Cyan Waves
            listOf(Color(0xFF10B981), Color(0xFF059669)), // Vibrant Green
            listOf(Color(0xFF00E5FF), Color(0xFF3B82F6)), // Neon Cyan & Electric Blue
            listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)), // Magenta & Purple
            listOf(Color(0xFFFFD600), Color(0xFFFF5252)), // Amber & Coral
            listOf(Color(0xFF8B5CF6), Color(0xFF00E5FF))  // Violet & Cyan
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
    val infiniteTransition = rememberInfiniteTransition(label = "badge_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors = gradient)),
        contentAlignment = Alignment.Center
    ) {
        // Subtle animated flowing wave curves in background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = this.size.width
            val height = this.size.height

            // Wave 1
            val path1 = Path()
            path1.moveTo(0f, height)
            path1.lineTo(0f, height * 0.6f)
            val steps = 24
            for (i in 0..steps) {
                val x = (width / steps) * i
                val y = height * 0.62f + sin((x / width) * 2 * PI.toFloat() + phase).toFloat() * (height * 0.12f)
                path1.lineTo(x, y)
            }
            path1.lineTo(width, height)
            path1.close()
            drawPath(path = path1, color = Color.White.copy(alpha = 0.18f))

            // Wave 2
            val path2 = Path()
            path2.moveTo(0f, height)
            path2.lineTo(0f, height * 0.72f)
            for (i in 0..steps) {
                val x = (width / steps) * i
                val y = height * 0.72f + sin((x / width) * 2 * PI.toFloat() * 1.5f - phase).toFloat() * (height * 0.08f)
                path2.lineTo(x, y)
            }
            path2.lineTo(width, height)
            path2.close()
            drawPath(path = path2, color = Color.White.copy(alpha = 0.22f))
        }

        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.95f),
            modifier = Modifier.size(size * 0.45f)
        )
    }
}
