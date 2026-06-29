package app.mindmaze.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun BrokenHeartOverlay(
    message: String,
    livesRemaining: Int,
    onDismiss: () -> Unit
) {
    val heartScale = remember { Animatable(0f) }
    val overlayAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        overlayAlpha.animateTo(1f, tween(180))
        heartScale.animateTo(1.55f, tween(260, easing = FastOutSlowInEasing))
        heartScale.animateTo(0.85f, tween(110))
        heartScale.animateTo(1.15f, tween(90))
        heartScale.animateTo(1.0f, tween(90))
        delay(1300L)
        overlayAlpha.animateTo(0f, tween(280))
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(overlayAlpha.value)
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(40.dp)
        ) {
            Text(
                text = "💔",
                fontSize = 100.sp,
                modifier = Modifier.scale(heartScale.value)
            )

            Spacer(Modifier.height(16.dp))

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
