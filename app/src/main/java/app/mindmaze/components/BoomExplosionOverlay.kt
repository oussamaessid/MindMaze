package app.mindmaze.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.mindmaze.R
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Plays a real BOOM explosion (a jagged flash + smoke puffs) when a rule is broken and a life is lost. */
@Composable
fun BoomExplosionOverlay(
    message: String,
    livesRemaining: Int,
    onDismiss: () -> Unit
) {
    val overlayAlpha = remember { Animatable(0f) }
    val burstScale = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    val explosionComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.boom_explosion))
    var continued by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            coroutineScope {
                launch { overlayAlpha.animateTo(1f, tween(140)) }
                launch {
                    burstScale.animateTo(1.25f, tween(180, easing = FastOutSlowInEasing))
                    burstScale.animateTo(1f, tween(140))
                }
                launch {
                    shake.animateTo(1f, tween(60))
                    shake.animateTo(-1f, tween(90))
                    shake.animateTo(0.5f, tween(90))
                    shake.animateTo(0f, tween(80))
                }
            }
            delay(1200L)
            overlayAlpha.animateTo(0f, tween(260))
        } finally {
            if (!continued) {
                continued = true
                onDismiss()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(overlayAlpha.value)
            .background(Color.Black.copy(alpha = 0.84f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(40.dp)
                .graphicsLayer { translationX = shake.value * 14f }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                LottieAnimation(
                    composition = explosionComposition,
                    iterations = 1,
                    modifier = Modifier.fillMaxSize().scale(burstScale.value)
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = "−1 Life",
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEF4444)
            )

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .background(Color(0xFF1E293B), RoundedCornerShape(14.dp))
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Text(
                    text = message,
                    fontSize = 15.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lives remaining:",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8)
                )
                LivesComponent(lives = livesRemaining)
            }
        }
    }
}

/** A jagged starburst flash — the visual core of the BOOM. */
@Composable
private fun BoomBurst(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxR = size.minDimension / 2f
        val spikes = 9
        val path = Path()
        for (i in 0 until spikes * 2) {
            val angle = (PI * 2 * i / (spikes * 2)).toFloat()
            val r = if (i % 2 == 0) maxR else maxR * 0.42f
            val x = center.x + cos(angle) * r
            val y = center.y + sin(angle) * r
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(
            path,
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFF176), Color(0xFFFF7A1A), Color(0xFFD32F2F)),
                center = center,
                radius = maxR + 1f
            )
        )
        drawCircle(Color.White.copy(alpha = .85f), maxR * .18f, center)
    }
}

/** A handful of dark smoke puffs drifting away from the blast. */
@Composable
private fun SmokePuffs(progress: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val origin = Offset(size.width / 2f, size.height / 2f)
        repeat(10) { i ->
            val angle = (i * 137.5f) * PI.toFloat() / 180f
            val distance = size.minDimension * (0.34f + (i % 4) * 0.10f) * progress
            val x = origin.x + cos(angle) * distance
            val y = origin.y + sin(angle) * distance
            val alpha = (1f - progress * .7f).coerceIn(0f, 1f)
            val radius = size.minDimension * (0.07f + (i % 3) * 0.02f)
            drawCircle(Color(0xFF3A3A3A).copy(alpha = alpha * .55f), radius, Offset(x, y))
        }
    }
}
