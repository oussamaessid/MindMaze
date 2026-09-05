package app.mindmaze

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.mindmaze.components.BannerAdView
import app.mindmaze.components.NoInternetDialog
import app.mindmaze.ui.theme.*

@Composable
fun HomeScreen(onPlayClicked: () -> Unit, onHelpClicked: () -> Unit = {}) {
    val context = LocalContext.current
    var showNoInternetDialog by remember { mutableStateOf(false) }
    var showGameCompletedDialog by remember { mutableStateOf(false) }
    val currentLevel = remember { LevelPreferences.loadLastLevel(context) + 1 }
    val pulse by rememberInfiniteTransition(label = "heroPulse").animateFloat(
        initialValue = .96f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "heroScale"
    )

    LaunchedEffect(Unit) { showGameCompletedDialog = LevelPreferences.isAllLevelsCompleted(context) }

    if (showNoInternetDialog) {
        NoInternetDialog(onDismiss = { showNoInternetDialog = false }, onRetry = {
            if (NetworkUtils.isInternetAvailable(context)) {
                showNoInternetDialog = false
                onPlayClicked()
            }
        })
    }

    if (showGameCompletedDialog) {
        AlertDialog(
            onDismissRequest = { showGameCompletedDialog = false },
            icon = { Text("🏆", fontSize = 46.sp) },
            title = { Text("Puzzle master!", fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center) },
            text = { Text("You completed every available level. New challenges are coming soon!", textAlign = TextAlign.Center, color = InkMuted, lineHeight = 22.sp) },
            confirmButton = {
                Button(
                    onClick = { showGameCompletedDialog = false },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Ember)
                ) { Text("AWESOME", fontWeight = FontWeight.ExtraBold) }
            },
            shape = RoundedCornerShape(28.dp), containerColor = Cloud
        )
    }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Box(Modifier.size(230.dp).offset(x = (-80).dp, y = 90.dp).blur(58.dp).background(Ember.copy(alpha = .13f), CircleShape))
        Box(Modifier.size(200.dp).align(Alignment.CenterEnd).offset(x = 85.dp, y = 110.dp).blur(54.dp).background(Sky.copy(alpha = .18f), CircleShape))

        Column(Modifier.fillMaxSize().statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = Ember.copy(alpha = .10f), shape = RoundedCornerShape(50), border = androidx.compose.foundation.BorderStroke(1.dp, Ember.copy(alpha = .18f))) {
                    Text(
                        "DAILY BOMB PUZZLE", Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                        color = Ember, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp
                    )
                }
                IconButton(
                    onClick = onHelpClicked,
                    modifier = Modifier.background(Color(0xFFF3F1F8), CircleShape).border(1.dp, Color(0xFFE7E3F0), CircleShape)
                ) { Icon(Icons.Default.Info, "How to play", tint = Ink) }
            }

            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.height(238.dp)) {
                    Box(Modifier.size(218.dp).scale(pulse).background(Brush.radialGradient(listOf(Ember.copy(.20f), Color.Transparent)), CircleShape))
                    Image(
                        painter = painterResource(R.drawable.boomdoko_app_icon),
                        contentDescription = "BOOMDOKU fire bomb icon",
                        modifier = Modifier.size(204.dp).scale(pulse).clip(RoundedCornerShape(46.dp))
                    )
                }
                Text("BOOMDOKU", color = Ink, fontSize = 46.sp, lineHeight = 50.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Black)
                Text("One bomb. One row. One brilliant move.", Modifier.padding(top = 6.dp), color = InkMuted, fontSize = 15.sp, fontWeight = FontWeight.Medium)

                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = Cloud,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE7E3F0))
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("NEXT BLAST", color = Color(0xFFE08A00), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
                            Text("LEVEL $currentLevel", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }
                        Text("READY", color = Color(0xFFE08A00), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Row(Modifier.padding(top = 12.dp, bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeaturePill("1", "per zone", Violet)
                    FeaturePill("+", "row + column", Sky)
                    FeaturePill("×", "never touch", Ember)
                }

                Button(
                    onClick = {
                        when {
                            !NetworkUtils.isInternetAvailable(context) -> showNoInternetDialog = true
                            LevelPreferences.isAllLevelsCompleted(context) -> showGameCompletedDialog = true
                            else -> onPlayClicked()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(68.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Ember, contentColor = Color.White),
                    shape = RoundedCornerShape(22.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp, pressedElevation = 2.dp)
                ) {
                    Box(Modifier.fillMaxWidth()) {
                        Column(Modifier.align(Alignment.CenterStart)) {
                            Text("START THE BLAST", fontSize = 17.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
                            Text("Continue from level $currentLevel", fontSize = 11.sp, color = Color.White.copy(.78f))
                        }
                        Surface(Modifier.align(Alignment.CenterEnd).size(40.dp), shape = CircleShape, color = Color.White.copy(alpha = .18f)) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
            BannerAdView(modifier = Modifier.fillMaxWidth().navigationBarsPadding())
        }
    }
}

@Composable
private fun FeaturePill(symbol: String, label: String, accent: Color) {
    Surface(shape = RoundedCornerShape(16.dp), color = accent.copy(alpha = .10f), border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = .22f))) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(symbol, color = accent, fontWeight = FontWeight.Black, fontSize = 15.sp)
            Spacer(Modifier.width(5.dp))
            Text(label.uppercase(), color = InkMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}
