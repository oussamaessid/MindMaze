package app.mindmaze.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.mindmaze.R
import app.mindmaze.audio.SoundManager
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Three-second BOOMDUKU victory celebration that always continues, even if audio fails. */
@Composable
fun LevelSuccessAnimation(levelNumber: Int, onContinue: () -> Unit) {
    val context = LocalContext.current
    val soundManager = remember(context) { SoundManager.get(context) }
    val particles = remember { Animatable(0f) }
    val entrance = remember { Animatable(0.65f) }
    var continued by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            coroutineScope {
                launch { entrance.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
                launch { particles.animateTo(1f, tween(2800, easing = LinearEasing)) }
                launch {
                    delay(180)
                    runCatching { soundManager.playDrumHit() }
                }
            }
            delay(200)
        } finally {
            if (!continued) {
                continued = true
                onContinue()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xE6233A67)),
        contentAlignment = Alignment.Center
    ) {
        ExplosionRings(progress = particles.value)
        CelebrationParticles(progress = particles.value)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 28.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.boomduku_victory_mascot),
                contentDescription = "BOOMDUKU mascot celebrating the completed level",
                modifier = Modifier
                    .size(310.dp)
                    .graphicsLayer {
                        scaleX = entrance.value * (1f + 0.035f * sin(particles.value * PI * 6).toFloat())
                        scaleY = entrance.value * (1f - 0.025f * sin(particles.value * PI * 6).toFloat())
                        rotationZ = 3.5f * sin(particles.value * PI * 4).toFloat()
                        translationY = -18f * sin(particles.value * PI * 3).toFloat()
                    }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "LEVEL $levelNumber COMPLETE!",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "BOOM!  🏆🎉",
                color = Color(0xFFFFD54F),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ExplosionRings(progress: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height * .43f)
        repeat(3) { index ->
            val local = ((progress * 2.2f) - index * .22f).coerceIn(0f, 1f)
            if (local > 0f && local < 1f) {
                drawCircle(
                    color = listOf(Color(0xFFFFF176), Color(0xFFFF8A22), Color(0xFFFF3D18))[index]
                        .copy(alpha = (1f - local) * .72f),
                    radius = size.minDimension * (.08f + local * .58f),
                    center = center,
                    style = Stroke(width = (10f - local * 7f) * density)
                )
            }
        }
    }
}

@Composable
private fun AnimatedDrum(hit: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bodyLeft = w * 0.17f
        val bodyTop = h * 0.35f
        val bodyWidth = w * 0.66f
        val bodyHeight = h * 0.42f

        drawCircle(
            Color(0xFFFFD54F).copy(alpha = 0.45f * (1f - hit)),
            w * (0.30f + hit * 0.22f),
            Offset(w / 2f, h * 0.48f)
        )
        drawRoundRect(
            Color(0xFFE84A3C), Offset(bodyLeft, bodyTop), Size(bodyWidth, bodyHeight),
            CornerRadius(w * 0.08f)
        )
        drawOval(
            Color(0xFFFFE0A3), Offset(bodyLeft, bodyTop - h * 0.08f),
            Size(bodyWidth, h * 0.20f)
        )
        drawOval(
            Color(0xFFFFB52E), Offset(bodyLeft, bodyTop - h * 0.08f),
            Size(bodyWidth, h * 0.20f), style = Stroke(w * 0.035f)
        )
        drawLine(
            Color(0xFFFFC247), Offset(bodyLeft, bodyTop + bodyHeight),
            Offset(bodyLeft + bodyWidth, bodyTop + bodyHeight), w * 0.045f
        )
        repeat(4) { i ->
            val x = bodyLeft + bodyWidth * (i + 0.5f) / 4f
            drawLine(
                Color(0xFFFFD166), Offset(x - w * 0.07f, bodyTop + h * 0.08f),
                Offset(x + w * 0.07f, bodyTop + bodyHeight), w * 0.016f
            )
        }

        val strikeY = h * (0.28f + hit * 0.17f)
        val leftTip = Offset(w * 0.43f, strikeY)
        val rightTip = Offset(w * 0.57f, strikeY)
        drawLine(Color(0xFF8D5A2B), Offset(w * 0.14f, h * 0.10f), leftTip, w * 0.035f, StrokeCap.Round)
        drawLine(Color(0xFF8D5A2B), Offset(w * 0.86f, h * 0.10f), rightTip, w * 0.035f, StrokeCap.Round)
        drawCircle(Color(0xFFFFC247), w * 0.035f, leftTip)
        drawCircle(Color(0xFFFFC247), w * 0.035f, rightTip)
    }
}

@Composable
private fun CelebrationParticles(progress: Float) {
    val colors = listOf(
        Color(0xFFFFD54F), Color(0xFFFF6B35), Color(0xFF55D6BE),
        Color(0xFF7DB7FF), Color(0xFFE879F9), Color.White
    )
    Canvas(Modifier.fillMaxSize()) {
        val origin = Offset(size.width / 2f, size.height * 0.43f)
        repeat(32) { i ->
            val angle = (i * 137.5f + 12f) * PI.toFloat() / 180f
            val speed = size.minDimension * (0.22f + (i % 7) * 0.018f)
            val x = origin.x + cos(angle) * speed * progress
            val y = origin.y + sin(angle) * speed * progress + size.height * 0.16f * progress * progress
            val alpha = (1f - progress).coerceIn(0f, 1f)
            val radius = (4f + i % 4) * density
            if (i % 3 == 0) {
                drawRect(colors[i % colors.size].copy(alpha = alpha), Offset(x, y), Size(radius * 1.8f, radius))
            } else {
                drawCircle(colors[i % colors.size].copy(alpha = alpha), radius, Offset(x, y))
            }
        }
    }
}
