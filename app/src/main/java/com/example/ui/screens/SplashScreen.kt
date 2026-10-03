package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onDismiss() }

    // Entrance animations
    val iconScale = remember { Animatable(0.5f) }
    val iconAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(40f) }
    val waveAlpha = remember { Animatable(0f) }
    val bottomAlpha = remember { Animatable(0f) }

    // Continuous ambient animations
    val infiniteTransition = rememberInfiniteTransition(label = "splash_infinite")
    
    // Glowing pulse scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    // Secondary pulse for depth
    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale_2"
    )
    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha_2"
    )

    // Equalizer animation bars
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(360, delayMillis = 80, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(500, delayMillis = 150, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar3"
    )
    val bar4 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(390, delayMillis = 50, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar4"
    )
    val bar5 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(460, delayMillis = 120, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar5"
    )

    LaunchedEffect(Unit) {
        // Step 1: Icon entrance with snappy spring
        launch {
            iconAlpha.animateTo(1f, tween(400))
        }
        launch {
            iconScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        }
        delay(250)

        // Step 2: Title and badge fade in & slide up
        launch {
            titleAlpha.animateTo(1f, tween(500))
        }
        launch {
            titleOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
        }

        // Step 3: Equalizer & bottom text
        delay(200)
        launch {
            waveAlpha.animateTo(1f, tween(400))
        }
        launch {
            bottomAlpha.animateTo(1f, tween(500))
        }

        // Keep visible for a pleasant moment, then dismiss smoothly
        delay(1600)
        onDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF282A36),
                        Color(0xFF13141A),
                        Color(0xFF090A0D)
                    ),
                    radius = 1200f
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Tap to skip
                onDismiss()
            }
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Central Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // Icon with glowing ripple rings
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(190.dp)
            ) {
                // Ripple ring 2
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(pulseScale2)
                        .alpha(pulseAlpha2)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xFF6C5CE7), CircleShape)
                )

                // Ripple ring 1
                Box(
                    modifier = Modifier
                        .size(125.dp)
                        .scale(pulseScale)
                        .alpha(pulseAlpha)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFF00CEC9), CircleShape)
                )

                // Ambient glow backdrop
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(iconScale.value * 1.05f)
                        .alpha(iconAlpha.value * 0.45f)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF6C5CE7), Color(0xFF00CEC9))
                            )
                        )
                )

                // Main App Logo Icon
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(iconScale.value)
                        .alpha(iconAlpha.value)
                        .shadow(16.dp, RoundedCornerShape(26.dp), spotColor = Color(0xFF00CEC9))
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFF1A1B22))
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)
                            ),
                            RoundedCornerShape(26.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_pro_icon_1791006651263),
                        contentDescription = "Music Pro Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Animated Title: "MUSIC" + "PRO" Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .offset { IntOffset(0, titleOffsetY.value.toInt()) }
                    .alpha(titleAlpha.value)
            ) {
                Text(
                    text = "MUSIC",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        letterSpacing = 2.sp
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFF39C12), Color(0xFFE67E22))
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PRO",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        ),
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle Tagline
            Text(
                text = "Premium Offline Audio Experience",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    letterSpacing = 0.4.sp
                ),
                color = Color(0xFF8E90A2),
                modifier = Modifier
                    .offset { IntOffset(0, (titleOffsetY.value * 0.6f).toInt()) }
                    .alpha(titleAlpha.value)
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Animated Equalizer Wave Bars
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(28.dp)
                    .alpha(waveAlpha.value)
            ) {
                val bars = listOf(bar1, bar2, bar3, bar4, bar5)
                val colors = listOf(
                    Color(0xFF00CEC9),
                    Color(0xFF6C5CE7),
                    Color(0xFFE84393),
                    Color(0xFF6C5CE7),
                    Color(0xFF00CEC9)
                )

                bars.forEachIndexed { index, fraction ->
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height((28 * fraction).coerceAtLeast(5f).dp)
                            .clip(CircleShape)
                            .background(colors[index])
                    )
                }
            }
        }

        // Bottom Footer
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .alpha(bottomAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Hi-Res Audio Engine • Version 1.0",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp
                ),
                color = Color(0xFF5E6070)
            )
        }
    }
}
