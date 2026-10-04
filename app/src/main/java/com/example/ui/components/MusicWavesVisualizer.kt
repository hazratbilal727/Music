package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Visualizer modes that users can tap to switch between.
 */
enum class WaveVisualizerMode(val label: String) {
    MAGENTA_SUNSET("Sunset Pink Waves"),
    FLUID_OCEAN("Emerald Green Waves"),
    CYBER_RIBBONS("Neon Cyan Waves"),
    RADIAL_PULSES("Electric Purple Waves"),
    WAVE_SPECTRUM("Golden Spectrum Waves"),
    CRIMSON_PULSE("Crimson Red Waves")
}

/**
 * A rich, interactive multi-colored wave animation that synchronizes with music playback.
 * Replaces static album artwork with fluid, colorful waves for high user engagement.
 */
@Composable
fun MusicWavesVisualizer(
    isPlaying: Boolean,
    currentPositionMs: Long,
    playbackSpeed: Float = 1.0f,
    modifier: Modifier = Modifier,
    diameter: Dp = 240.dp,
    mode: WaveVisualizerMode = WaveVisualizerMode.MAGENTA_SUNSET,
    onTap: (() -> Unit)? = null
) {
    // Infinite transitions for continuous fluid wave phase progression
    val infiniteTransition = rememberInfiniteTransition(label = "wave_motion")

    val durationMs = (2800 / playbackSpeed.coerceIn(0.5f, 2.5f)).toInt()

    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween((durationMs * 1.35f).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )

    val phase3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween((durationMs * 0.75f).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase3"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween((durationMs * 4).coerceAtLeast(6000), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_rotation"
    )

    // Smooth amplitude scale based on playing state (damps when paused, energetic when playing)
    val amplitudeScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.22f,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "amplitude_scale"
    )

    // Music sync: beat rhythm pulse derived from current playback position (simulated 110 BPM groove)
    val beatInterval = (545L / playbackSpeed.coerceIn(0.5f, 2.0f)).toLong()
    val beatProgress = ((currentPositionMs % beatInterval).toFloat() / beatInterval)
    val beatPulse = if (isPlaying) {
        val raw = sin(beatProgress * PI).toFloat()
        (raw * raw).coerceIn(0f, 1f)
    } else {
        0f
    }

    // Color Palette for Multi-Colored Waves
    val waveMagentaSunset = Color(0xFFEC4899) // Screenshot Sunset Pink/Magenta
    val waveGreen = Color(0xFF00E676)   // Vibrant Emerald Green
    val waveCyan = Color(0xFF00E5FF)    // Neon Cyan
    val waveMagenta = Color(0xFFFF1744) // Electric Sunset Magenta
    val waveAmber = Color(0xFFFFD600)   // Golden Amber
    val wavePurple = Color(0xFF9D4EDD)  // Deep Electric Purple

    val activeModeColor = when (mode) {
        WaveVisualizerMode.MAGENTA_SUNSET -> waveMagentaSunset
        WaveVisualizerMode.FLUID_OCEAN -> waveGreen
        WaveVisualizerMode.CYBER_RIBBONS -> waveCyan
        WaveVisualizerMode.RADIAL_PULSES -> wavePurple
        WaveVisualizerMode.WAVE_SPECTRUM -> waveAmber
        WaveVisualizerMode.CRIMSON_PULSE -> waveMagenta
    }

    Box(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .background(Color(0xFF0A0B0E))
            .border(2.dp, Color(0xFF22242D), CircleShape)
            .clickable {
                onTap?.invoke()
            }
            .testTag("music_waves_visualizer"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height
            val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
            val radius = canvasWidth / 2f

            if (mode == WaveVisualizerMode.MAGENTA_SUNSET) {
                // Screenshot exact design: Pink/Magenta to Purple gradient with animated wave layers
                drawSunsetMagentaWaves(
                    width = canvasWidth,
                    height = canvasHeight,
                    phase1 = phase1,
                    phase2 = phase2,
                    phase3 = phase3,
                    ampScale = amplitudeScale,
                    beatPulse = beatPulse
                )
            } else {
                // 1. Background dark vinyl grooves with subtle circular sheen
                drawVinylBackdrop(center, radius, rotationAngle, isPlaying)

                // 2. Render chosen wave visualizer style with vibrant different colors
                when (mode) {
                    WaveVisualizerMode.FLUID_OCEAN -> {
                        drawFluidOceanWaves(
                            width = canvasWidth,
                            height = canvasHeight,
                            phase1 = phase1,
                            phase2 = phase2,
                            phase3 = phase3,
                            ampScale = amplitudeScale,
                            beatPulse = beatPulse,
                            colors = listOf(wavePurple, waveMagenta, waveCyan, waveGreen, waveAmber)
                        )
                    }
                    WaveVisualizerMode.CYBER_RIBBONS -> {
                        drawCyberRibbonWaves(
                            width = canvasWidth,
                            height = canvasHeight,
                            phase1 = phase1,
                            phase2 = phase2,
                            phase3 = phase3,
                            ampScale = amplitudeScale,
                            beatPulse = beatPulse,
                            colors = listOf(waveGreen, waveCyan, waveMagenta, waveAmber)
                        )
                    }
                    WaveVisualizerMode.RADIAL_PULSES -> {
                        drawRadialRipples(
                            center = center,
                            maxRadius = radius,
                            phase = phase1,
                            ampScale = amplitudeScale,
                            beatPulse = beatPulse,
                            colors = listOf(waveGreen, waveCyan, waveMagenta, waveAmber, wavePurple)
                        )
                    }
                    WaveVisualizerMode.WAVE_SPECTRUM -> {
                        drawWaveSpectrumBars(
                            center = center,
                            width = canvasWidth,
                            height = canvasHeight,
                            phase = phase1,
                            ampScale = amplitudeScale,
                            beatPulse = beatPulse,
                            colors = listOf(waveGreen, waveCyan, waveAmber, waveMagenta)
                        )
                    }
                    WaveVisualizerMode.CRIMSON_PULSE -> {
                        drawCrimsonPulseWaves(
                            width = canvasWidth,
                            height = canvasHeight,
                            phase1 = phase1,
                            phase2 = phase2,
                            phase3 = phase3,
                            ampScale = amplitudeScale,
                            beatPulse = beatPulse,
                            colors = listOf(waveMagenta, Color(0xFFFF3D00), Color(0xFFE50914), Color(0xFFFF8A80))
                        )
                    }
                    WaveVisualizerMode.MAGENTA_SUNSET -> {}
                }

                // 3. Central Vinyl Audio Spindle / Pulsing Core
                drawAudioCore(
                    center = center,
                    radius = 26.dp.toPx(),
                    beatPulse = beatPulse,
                    isPlaying = isPlaying,
                    accentColor = activeModeColor
                )
            }
        }

        // Overlay Icon Badge at center (pulsing white musical note matching screenshot)
        val noteScale = if (isPlaying) 1f + (0.10f * beatPulse) else 1f
        Icon(
            imageVector = if (mode == WaveVisualizerMode.WAVE_SPECTRUM) Icons.Default.GraphicEq else Icons.Default.MusicNote,
            contentDescription = "Wave Visualizer",
            tint = Color.White.copy(alpha = if (isPlaying) 0.98f else 0.88f),
            modifier = Modifier
                .size(if (mode == WaveVisualizerMode.MAGENTA_SUNSET) 64.dp else 26.dp)
                .graphicsLayer(scaleX = noteScale, scaleY = noteScale)
        )
    }
}

/**
 * Draws the subtle textured vinyl backdrop inside the circular artwork container.
 */
private fun DrawScope.drawVinylBackdrop(
    center: Offset,
    radius: Float,
    rotation: Float,
    isPlaying: Boolean
) {
    // Subtle circular grooves
    val grooveSteps = 5
    for (i in 1..grooveSteps) {
        val r = radius * (0.35f + (i * 0.11f))
        drawCircle(
            color = Color(0xFF1B1D25).copy(alpha = 0.45f),
            radius = r,
            center = center,
            style = Stroke(width = 1.2.dp.toPx())
        )
    }

    // Dynamic subtle radial sheen
    val sheenBrush = Brush.sweepGradient(
        listOf(
            Color.Transparent,
            Color.White.copy(alpha = if (isPlaying) 0.05f else 0.02f),
            Color.Transparent,
            Color.White.copy(alpha = if (isPlaying) 0.04f else 0.01f),
            Color.Transparent
        ),
        center = center
    )
    drawCircle(
        brush = sheenBrush,
        radius = radius,
        center = center
    )
}

/**
 * Style 1: Layered Fluid Multi-Color Waves rising from the bottom.
 */
private fun DrawScope.drawFluidOceanWaves(
    width: Float,
    height: Float,
    phase1: Float,
    phase2: Float,
    phase3: Float,
    ampScale: Float,
    beatPulse: Float,
    colors: List<Color>
) {
    val green = colors[3]
    val cyan = colors[2]
    val magenta = colors[1]
    val purple = colors[0]

    // Layer 1: Deep Violet / Purple Base Wave
    drawSingleWaveLayer(
        width = width,
        height = height,
        baseY = height * 0.65f,
        amplitude = (16.dp.toPx() + 10.dp.toPx() * beatPulse) * ampScale,
        frequency = 1.2f,
        phase = phase2,
        brush = Brush.verticalGradient(
            colors = listOf(purple.copy(alpha = 0.55f), purple.copy(alpha = 0.15f)),
            startY = height * 0.5f,
            endY = height
        )
    )

    // Layer 2: Electric Magenta Wave
    drawSingleWaveLayer(
        width = width,
        height = height,
        baseY = height * 0.60f,
        amplitude = (20.dp.toPx() + 14.dp.toPx() * beatPulse) * ampScale,
        frequency = 1.6f,
        phase = -phase3,
        brush = Brush.verticalGradient(
            colors = listOf(magenta.copy(alpha = 0.6f), magenta.copy(alpha = 0.15f)),
            startY = height * 0.45f,
            endY = height
        )
    )

    // Layer 3: Neon Cyan Wave
    drawSingleWaveLayer(
        width = width,
        height = height,
        baseY = height * 0.55f,
        amplitude = (22.dp.toPx() + 16.dp.toPx() * beatPulse) * ampScale,
        frequency = 1.9f,
        phase = phase1,
        brush = Brush.verticalGradient(
            colors = listOf(cyan.copy(alpha = 0.65f), cyan.copy(alpha = 0.2f)),
            startY = height * 0.4f,
            endY = height
        )
    )

    // Layer 4: Vibrant Green Wave (Dominant on top)
    drawSingleWaveLayer(
        width = width,
        height = height,
        baseY = height * 0.50f,
        amplitude = (24.dp.toPx() + 18.dp.toPx() * beatPulse) * ampScale,
        frequency = 2.2f,
        phase = phase2 * 1.2f,
        brush = Brush.verticalGradient(
            colors = listOf(green.copy(alpha = 0.75f), green.copy(alpha = 0.25f)),
            startY = height * 0.35f,
            endY = height
        ),
        crestStrokeColor = green,
        crestStrokeWidth = 2.5.dp.toPx()
    )

    // Floating audio particles along crests
    val particleCount = 6
    for (i in 0 until particleCount) {
        val px = (width / (particleCount + 1)) * (i + 1)
        val py = height * 0.48f + sin(px * 0.02f + phase1).toFloat() * 15.dp.toPx() * ampScale - (beatPulse * 12.dp.toPx())
        drawCircle(
            color = if (i % 2 == 0) green else cyan,
            radius = (3.dp.toPx() + beatPulse * 2.dp.toPx()),
            center = Offset(px, py)
        )
    }
}

/**
 * Draws a filled sinusoidal wave path with optional glowing crest stroke.
 */
private fun DrawScope.drawSingleWaveLayer(
    width: Float,
    height: Float,
    baseY: Float,
    amplitude: Float,
    frequency: Float,
    phase: Float,
    brush: Brush,
    crestStrokeColor: Color? = null,
    crestStrokeWidth: Float = 1.5f
) {
    val path = Path()
    path.moveTo(0f, height)
    path.lineTo(0f, baseY)

    val steps = 36
    val dx = width / steps
    for (i in 0..steps) {
        val x = i * dx
        val normalizedX = (x / width) * 2 * PI.toFloat() * frequency
        val y = baseY + sin(normalizedX + phase).toFloat() * amplitude
        path.lineTo(x, y)
    }

    path.lineTo(width, height)
    path.close()

    drawPath(path = path, brush = brush)

    // Draw illuminated crest line if specified
    if (crestStrokeColor != null) {
        val crestPath = Path()
        for (i in 0..steps) {
            val x = i * dx
            val normalizedX = (x / width) * 2 * PI.toFloat() * frequency
            val y = baseY + sin(normalizedX + phase).toFloat() * amplitude
            if (i == 0) crestPath.moveTo(x, y) else crestPath.lineTo(x, y)
        }
        drawPath(
            path = crestPath,
            color = crestStrokeColor,
            style = Stroke(width = crestStrokeWidth, cap = StrokeCap.Round)
        )
    }
}

/**
 * Style 2: Cyber Neon Oscilloscope Ribbons weaving across the center.
 */
private fun DrawScope.drawCyberRibbonWaves(
    width: Float,
    height: Float,
    phase1: Float,
    phase2: Float,
    phase3: Float,
    ampScale: Float,
    beatPulse: Float,
    colors: List<Color>
) {
    val green = colors[0]
    val cyan = colors[1]
    val magenta = colors[2]
    val amber = colors[3]

    val centerY = height / 2f
    val steps = 48
    val dx = width / steps

    val ribbonConfigs = listOf(
        Triple(green, phase1, 32.dp.toPx() + 20.dp.toPx() * beatPulse),
        Triple(cyan, -phase2, 28.dp.toPx() + 16.dp.toPx() * beatPulse),
        Triple(magenta, phase3, 24.dp.toPx() + 14.dp.toPx() * beatPulse),
        Triple(amber, phase1 * 0.8f, 20.dp.toPx() + 10.dp.toPx() * beatPulse)
    )

    ribbonConfigs.forEachIndexed { index, (color, phase, maxAmp) ->
        val ribbonPath = Path()
        val freq = 1.5f + (index * 0.4f)
        val amp = maxAmp * ampScale

        for (i in 0..steps) {
            val x = i * dx
            val normX = (x / width) * 2 * PI.toFloat() * freq
            val y = centerY + sin(normX + phase).toFloat() * amp + cos(normX * 0.5f + phase).toFloat() * (amp * 0.35f)
            if (i == 0) ribbonPath.moveTo(x, y) else ribbonPath.lineTo(x, y)
        }

        // Draw glowing laser ribbon
        drawPath(
            path = ribbonPath,
            color = color.copy(alpha = 0.85f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Soft glow halo
        drawPath(
            path = ribbonPath,
            color = color.copy(alpha = 0.25f),
            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * Style 3: Concentric Radial Sonic Ripples expanding from the center.
 */
private fun DrawScope.drawRadialRipples(
    center: Offset,
    maxRadius: Float,
    phase: Float,
    ampScale: Float,
    beatPulse: Float,
    colors: List<Color>
) {
    val ringCount = 6
    val effectiveMax = maxRadius * 0.88f

    for (i in 0 until ringCount) {
        val color = colors[i % colors.size]
        val progress = ((phase / (2 * PI).toFloat() + (i.toFloat() / ringCount)) % 1f)
        val r = 24.dp.toPx() + progress * (effectiveMax - 24.dp.toPx())
        val alpha = ((1f - progress) * 0.85f * ampScale).coerceIn(0f, 1f)

        // Undulating wavy circle perimeter
        val waveCirclePath = Path()
        val vertexCount = 40
        for (j in 0..vertexCount) {
            val theta = (j.toFloat() / vertexCount) * 2 * PI.toFloat()
            val waveDisplacement = sin(theta * 6f + phase * 2f).toFloat() * (5.dp.toPx() * beatPulse * ampScale)
            val currentR = r + waveDisplacement
            val px = center.x + currentR * cos(theta)
            val py = center.y + currentR * sin(theta)

            if (j == 0) waveCirclePath.moveTo(px, py) else waveCirclePath.lineTo(px, py)
        }
        waveCirclePath.close()

        drawPath(
            path = waveCirclePath,
            color = color.copy(alpha = alpha),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * Style 4: Multi-Color Spectrum Wave Bars dancing across the disc.
 */
private fun DrawScope.drawWaveSpectrumBars(
    center: Offset,
    width: Float,
    height: Float,
    phase: Float,
    ampScale: Float,
    beatPulse: Float,
    colors: List<Color>
) {
    val barCount = 18
    val barWidth = 5.dp.toPx()
    val spacing = (width * 0.76f) / barCount
    val startX = center.x - ((barCount * spacing) / 2f) + (spacing / 2f)

    for (i in 0 until barCount) {
        val x = startX + (i * spacing)
        val normIndex = i.toFloat() / barCount
        val harmonic = sin(normIndex * PI.toFloat() * 2f + phase).toFloat()
        val harmonic2 = cos(normIndex * PI.toFloat() * 3f - phase * 1.5f).toFloat()

        val color = colors[i % colors.size]
        val baseH = 14.dp.toPx()
        val dynamicH = ((harmonic + 1.2f) * 24.dp.toPx() + harmonic2 * 12.dp.toPx() + beatPulse * 30.dp.toPx()) * ampScale
        val totalH = (baseH + dynamicH).coerceAtMost(height * 0.42f)

        val top = center.y - (totalH / 2f)
        val bottom = center.y + (totalH / 2f)

        // Draw vertical wave bar
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(color, color.copy(alpha = 0.3f)),
                startY = top,
                endY = bottom
            ),
            start = Offset(x, top),
            end = Offset(x, bottom),
            strokeWidth = barWidth,
            cap = StrokeCap.Round
        )

        // Peak dot
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = 2.dp.toPx(),
            center = Offset(x, top - 4.dp.toPx())
        )
    }
}

/**
 * Draws the illuminated center vinyl audio hub with pulse ring.
 */
private fun DrawScope.drawAudioCore(
    center: Offset,
    radius: Float,
    beatPulse: Float,
    isPlaying: Boolean,
    accentColor: Color
) {
    val pulsedRadius = radius + (4.dp.toPx() * beatPulse)

    // Glowing outer pulse aura
    if (isPlaying) {
        drawCircle(
            color = accentColor.copy(alpha = 0.28f + (0.22f * beatPulse)),
            radius = pulsedRadius + 8.dp.toPx(),
            center = center
        )
    }

    // Inner dark disc core
    drawCircle(
        color = Color(0xFF13141A),
        radius = pulsedRadius,
        center = center
    )

    // Center metallic ring border
    drawCircle(
        color = if (isPlaying) accentColor else Color(0xFF383A46),
        radius = pulsedRadius,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
}

/**
 * Style 5: Crimson Red Harmonic Waves pulsing with rhythm and music sync.
 */
private fun DrawScope.drawCrimsonPulseWaves(
    width: Float,
    height: Float,
    phase1: Float,
    phase2: Float,
    phase3: Float,
    ampScale: Float,
    beatPulse: Float,
    colors: List<Color>
) {
    val wavePath = androidx.compose.ui.graphics.Path()
    val steps = 40
    val centerY = height * 0.52f
    val baseAmp = (height * 0.16f) * ampScale

    for (waveIdx in 0..3) {
        wavePath.reset()
        val phaseOffset = waveIdx * 0.95f + phase1
        val color = colors[waveIdx % colors.size]

        wavePath.moveTo(0f, height)
        wavePath.lineTo(0f, centerY)

        for (i in 0..steps) {
            val normX = i.toFloat() / steps
            val x = normX * width
            val s1 = sin(normX * 2 * PI.toFloat() * 1.8f + phaseOffset).toFloat()
            val s2 = cos(normX * 2 * PI.toFloat() * 3.4f - phase2 * 1.2f).toFloat()
            val s3 = sin(normX * PI.toFloat() * 5f + phase3).toFloat()
            val y = centerY + (s1 * baseAmp * (1f + 0.38f * beatPulse)) + (s2 * baseAmp * 0.35f) + (s3 * baseAmp * 0.2f)
            wavePath.lineTo(x, y)
        }

        wavePath.lineTo(width, height)
        wavePath.close()

        drawPath(
            path = wavePath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = 0.55f),
                    color.copy(alpha = 0.10f)
                ),
                startY = centerY - baseAmp,
                endY = height
            )
        )
    }
}

/**
 * Style 1 (DEFAULT): Sunset Pink / Magenta to Purple Waves exactly matching user screenshot design.
 */
private fun DrawScope.drawSunsetMagentaWaves(
    width: Float,
    height: Float,
    phase1: Float,
    phase2: Float,
    phase3: Float,
    ampScale: Float,
    beatPulse: Float
) {
    // 1. Draw rich pink-magenta to purple gradient background
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFEC4899), // Bright Pink / Magenta
            Color(0xFFD946EF), // Vibrant Fuchsia
            Color(0xFF8B5CF6)  // Deep Purple / Violet
        ),
        start = Offset(0f, 0f),
        end = Offset(width, height)
    )
    drawCircle(
        brush = gradientBrush,
        radius = width / 2f,
        center = Offset(width / 2f, height / 2f)
    )

    // 2. Animated Flowing Wave Curves (3 layers matching screenshot)
    val wavePath = androidx.compose.ui.graphics.Path()
    val steps = 36

    // Layer 1 - Deep soft bottom wave
    wavePath.reset()
    val centerY1 = height * 0.56f
    val baseAmp1 = (height * 0.13f) * ampScale * (1f + 0.28f * beatPulse)
    wavePath.moveTo(0f, height)
    wavePath.lineTo(0f, centerY1)
    for (i in 0..steps) {
        val normX = i.toFloat() / steps
        val x = normX * width
        val s = sin(normX * 2 * PI.toFloat() * 1.5f + phase1).toFloat()
        val y = centerY1 + s * baseAmp1
        wavePath.lineTo(x, y)
    }
    wavePath.lineTo(width, height)
    wavePath.close()
    drawPath(path = wavePath, color = Color.White.copy(alpha = 0.18f))

    // Layer 2 - Middle harmonic wave
    wavePath.reset()
    val centerY2 = height * 0.66f
    val baseAmp2 = (height * 0.11f) * ampScale * (1f + 0.35f * beatPulse)
    wavePath.moveTo(0f, height)
    wavePath.lineTo(0f, centerY2)
    for (i in 0..steps) {
        val normX = i.toFloat() / steps
        val x = normX * width
        val s1 = sin(normX * 2 * PI.toFloat() * 2.1f - phase2).toFloat()
        val s2 = cos(normX * PI.toFloat() * 1.4f + phase3).toFloat()
        val y = centerY2 + s1 * baseAmp2 + s2 * (baseAmp2 * 0.3f)
        wavePath.lineTo(x, y)
    }
    wavePath.lineTo(width, height)
    wavePath.close()
    drawPath(path = wavePath, color = Color.White.copy(alpha = 0.26f))

    // Layer 3 - Foreground crest wave
    wavePath.reset()
    val centerY3 = height * 0.76f
    val baseAmp3 = (height * 0.08f) * ampScale * (1f + 0.40f * beatPulse)
    wavePath.moveTo(0f, height)
    wavePath.lineTo(0f, centerY3)
    for (i in 0..steps) {
        val normX = i.toFloat() / steps
        val x = normX * width
        val s = sin(normX * 2 * PI.toFloat() * 1.8f + phase1 * 1.25f).toFloat()
        val y = centerY3 + s * baseAmp3
        wavePath.lineTo(x, y)
    }
    wavePath.lineTo(width, height)
    wavePath.close()
    drawPath(path = wavePath, color = Color.White.copy(alpha = 0.38f))
}
