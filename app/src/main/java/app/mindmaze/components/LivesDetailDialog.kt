package app.mindmaze.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.mindmaze.AdConfig
import app.mindmaze.lives.LivesManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

@Composable
fun LivesDetailDialog(
    lives: Int,
    timeToNextLife: Long,
    onWatchVideo: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var rewardedAd by remember { mutableStateOf<RewardedAd?>(null) }

    LaunchedEffect(Unit) {
        RewardedAd.load(
            context,
            AdConfig.getRewardedAdId(),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { rewardedAd = null }
            }
        )
    }

    fun showRewardedAd() {
        val ad = rewardedAd
        val activity = context as? Activity
        if (ad == null || activity == null) {
            Toast.makeText(context, "Ad loading, please try again.", Toast.LENGTH_SHORT).show()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { rewardedAd = null }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { rewardedAd = null }
        }
        ad.show(activity) { onWatchVideo() }
    }

    val totalSeconds = timeToNextLife / 1_000L
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val formattedTime = "%02d:%02d:%02d".format(hours, minutes, seconds)
    val livesIsFull = lives >= LivesManager.MAX_LIVES
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text("Watch a video?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
            },
            text = {
                Text(
                    "Watch a short video to instantly earn +1 life.",
                    fontSize = 15.sp,
                    color = Color(0xFF374151),
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showConfirmDialog = false; showRewardedAd() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED), contentColor = Color.White),
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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Your Lives",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )

                Spacer(Modifier.height(20.dp))

                LivesComponent(lives = lives)

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "$lives / ${LivesManager.MAX_LIVES} lives",
                    fontSize = 15.sp,
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(20.dp))

                if (livesIsFull) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0FDF4), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✅  All lives available!",
                            fontSize = 15.sp,
                            color = Color(0xFF166534),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    if (timeToNextLife > 0) {
                        Text(
                            text = "Next life in:",
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280)
                        )

                        Spacer(Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1E293B), RoundedCornerShape(14.dp))
                                .padding(horizontal = 28.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = formattedTime,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF6B6B)
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "Lives restore automatically.",
                            fontSize = 12.sp,
                            color = Color(0xFF9CA3AF),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = "Restoring...",
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280)
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    HorizontalDivider(color = Color(0xFFE5E7EB))

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { showConfirmDialog = true  },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7C3AED),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Watch a video for +1 life",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Close",
                        fontSize = 15.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }
    }
}
