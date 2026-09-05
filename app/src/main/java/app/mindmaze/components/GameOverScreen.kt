package app.mindmaze.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.mindmaze.AdConfig
import app.mindmaze.lives.LivesManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

@Composable
fun GameOverScreen(
    timeToNextLife: Long,
    onLifeEarned: () -> Unit,
    onGoHome: () -> Unit
) {
    val context = LocalContext.current
    var rewardedAd by remember { mutableStateOf<RewardedAd?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Traverse context chain to find the Activity (ContextThemeWrapper → Activity)
    fun Context.findActivity(): Activity? {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun loadAd() {
        RewardedAd.load(
            context,
            AdConfig.getRewardedAdId(),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    // Retry after 4 seconds
                    android.os.Handler(android.os.Looper.getMainLooper())
                        .postDelayed({ loadAd() }, 4_000L)
                }
            }
        )
    }

    // Load rewarded ad as soon as this screen appears
    LaunchedEffect(Unit) { loadAd() }

    fun showRewardedAd() {
        val ad = rewardedAd
        val activity = context.findActivity()   // safe traversal instead of cast
        if (ad == null || activity == null) {
            Toast.makeText(context, "Ad is loading, please try again.", Toast.LENGTH_SHORT).show()
            loadAd()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadAd()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                Toast.makeText(context, "Ad unavailable, please try again.", Toast.LENGTH_SHORT).show()
                loadAd()
            }
        }
        ad.show(activity) {
            onLifeEarned()
        }
    }

    // Confirmation dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text("Watch a video?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
            },
            text = {
                Text(
                    "Watch a short video to instantly earn +1 life and keep playing.",
                    fontSize = 15.sp,
                    color = Color(0xFF374151),
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showConfirmDialog = false; showRewardedAd() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7C3AED),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Watch ▶", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    val totalSeconds = timeToNextLife / 1_000L
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val formattedTime = "%02d:%02d:%02d".format(hours, minutes, seconds)

    val infiniteTransition = rememberInfiniteTransition(label = "heart_pulse")
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heart_scale"
    )
    val sadSway by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sad_sway"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Image(
                painter = painterResource(app.mindmaze.R.drawable.boomdoko_defeat_mascot),
                contentDescription = "BOOMDOKU mascot sad and frustrated after losing",
                modifier = Modifier
                    .size(210.dp)
                    .graphicsLayer {
                        rotationZ = sadSway
                        scaleX = heartScale * 0.92f
                        scaleY = heartScale * 0.92f
                    }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "OH NO!",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "The bomb is sad… but your comeback can be explosive!",
                fontSize = 16.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Spacer(modifier = Modifier.height(12.dp))

            if (timeToNextLife > 0) {
                Text(
                    text = "Next life in",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFF1E293B), RoundedCornerShape(16.dp))
                        .padding(horizontal = 28.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B6B),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Lives restore automatically every ${LivesManager.RECOVERY_TIME_MS / 3_600_000L}h",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            } else {
                Text(
                    text = "A life is being restored...",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            HorizontalDivider(color = Color(0xFF1E293B))

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C3AED),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Watch a video for +1 life",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onGoHome,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Home", fontSize = 15.sp, color = Color(0xFF64748B))
            }
        }
    }
}
