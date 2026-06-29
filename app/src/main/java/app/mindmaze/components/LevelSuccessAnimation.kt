package app.mindmaze.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import app.mindmaze.R
import kotlinx.coroutines.delay

@Composable
fun LevelSuccessAnimation(
    levelNumber: Int,
    onContinue: () -> Unit
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.trophy))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        speed = 1.2f
    )

    val infiniteTransition = rememberInfiniteTransition(label = "success_pulse")
    val textScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "text_scale"
    )

    val star1Y by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -18f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "s1"
    )
    val star2Y by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -24f,
        animationSpec = infiniteRepeatable(tween(1000, delayMillis = 200), RepeatMode.Reverse),
        label = "s2"
    )
    val star3Y by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -14f,
        animationSpec = infiniteRepeatable(tween(650, delayMillis = 400), RepeatMode.Reverse),
        label = "s3"
    )

    var continued by remember { mutableStateOf(false) }
    LaunchedEffect(progress) {
        if (progress >= 0.98f && !continued) {
            delay(1200L)
            if (!continued) {
                continued = true
                onContinue()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("⭐", fontSize = 28.sp, modifier = Modifier.offset(y = star1Y.dp))
                Text("🌟", fontSize = 36.sp, modifier = Modifier.offset(y = star2Y.dp))
                Text("⭐", fontSize = 28.sp, modifier = Modifier.offset(y = star3Y.dp))
            }

            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(220.dp)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Level $levelNumber Complete!",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.scale(textScale)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Excellent work! 🎉",
                fontSize = 18.sp,
                color = Color(0xFFFFD700),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    if (!continued) {
                        continued = true
                        onContinue()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Continue →",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
