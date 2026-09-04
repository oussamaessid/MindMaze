package app.mindmaze.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.mindmaze.lives.LivesManager

@Composable
fun LivesComponent(
    lives: Int,
    modifier: Modifier = Modifier
) {
    val flamePulse by rememberInfiniteTransition(label = "lifeFuse").animateFloat(
        initialValue = .82f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "lifeFusePulse"
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(LivesManager.MAX_LIVES) { index ->
            val isAlive = index < lives
            val scale by animateFloatAsState(
                targetValue = if (isAlive) 1f else 0.78f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "bomb_life_scale_$index"
            )
            BombLifeIcon(
                lit = isAlive,
                flamePulse = flamePulse * (1f - index * .025f),
                modifier = Modifier.size(27.dp).scale(scale)
            )
        }
    }
}

@Composable
private fun BombLifeIcon(lit: Boolean, flamePulse: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val center = Offset(size.width * .43f, size.height * .62f)
        val radius = size.minDimension * .29f
        val bombColor = if (lit) Color(0xFF20232B) else Color(0xFF777985)

        drawCircle(if (lit) Color(0xFFFF6A2A).copy(alpha = .18f) else Color.Transparent, radius * 1.28f, center)
        drawCircle(bombColor, radius, center)
        drawCircle(Color.White.copy(alpha = if (lit) .55f else .18f), radius * .20f, center - Offset(radius * .38f, radius * .35f))
        drawArc(
            color = if (lit) Color(0xFFD39447) else Color(0xFF9698A0),
            startAngle = 205f,
            sweepAngle = 112f,
            useCenter = false,
            topLeft = Offset(size.width * .42f, size.height * .12f),
            size = androidx.compose.ui.geometry.Size(size.width * .39f, size.height * .43f),
            style = Stroke(width = size.width * .075f, cap = StrokeCap.Round)
        )

        val fuseTip = Offset(size.width * .80f, size.height * .18f)
        if (lit) {
            drawCircle(Color(0xFFFF3D18).copy(alpha = .30f), radius * .43f * flamePulse, fuseTip)
            drawCircle(Color(0xFFFF8A20), radius * .27f * flamePulse, fuseTip)
            drawCircle(Color(0xFFFFE066), radius * .13f * flamePulse, fuseTip)
        } else {
            drawCircle(Color(0xFF9CA3AF).copy(alpha = .35f), radius * .17f, fuseTip)
            drawCircle(Color(0xFF9CA3AF).copy(alpha = .18f), radius * .12f, fuseTip + Offset(radius * .24f, -radius * .26f))
        }
    }
}
