package app.mindmaze.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.mindmaze.R
import app.mindmaze.lives.LivesManager

@Composable
fun LivesComponent(
    lives: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(LivesManager.MAX_LIVES) { index ->
            val isAlive = index < lives
            val scale by animateFloatAsState(
                targetValue = if (isAlive) 1f else 0.75f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "heart_scale_$index"
            )
            Icon(
                painter = painterResource(
                    id = if (isAlive) R.drawable.ic_heart else R.drawable.ic_heart1
                ),
                contentDescription = if (isAlive) "Vie" else "Vie perdue",
                tint = if (isAlive) Color(0xFFEF4444) else Color(0xFFD1D5DB),
                modifier = Modifier
                    .size(22.dp)
                    .scale(scale)
            )
        }
    }
}
