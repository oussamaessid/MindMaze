package app.mindmaze

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
import app.mindmaze.components.BannerAdView
import app.mindmaze.components.NoInternetDialog
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun HomeScreen(
    onPlayClicked: () -> Unit
) {
    val context = LocalContext.current
    var showNoInternetDialog by remember { mutableStateOf(false) }
    var showGameCompletedDialog by remember { mutableStateOf(false) }

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.bomb))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    // Vérifier au démarrage si tous les niveaux sont complétés
    LaunchedEffect(Unit) {
        if (LevelPreferences.isAllLevelsCompleted(context)) {
            showGameCompletedDialog = true
        }
    }

    // Dialog - Pas d'internet
    if (showNoInternetDialog) {
        NoInternetDialog(
            onDismiss = { showNoInternetDialog = false },
            onRetry = {
                if (NetworkUtils.isInternetAvailable(context)) {
                    showNoInternetDialog = false
                    onPlayClicked()
                }
            }
        )
    }

    // ✅ Dialog "Jeu complété"
    if (showGameCompletedDialog) {
        AlertDialog(
            onDismissRequest = { showGameCompletedDialog = false },
            title = {
                Text(
                    text = "🏆 Félicitations!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )
            },
            text = {
                Text(
                    text = "Vous avez complété tous les niveaux disponibles!\n\nAttendez les prochains niveaux dans la mise à jour suivante 🚀",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF64748B),
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 24.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showGameCompletedDialog = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        "D'accord",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
            .background(Color(0xFFFFFFFF))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header - Logo et titre
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 60.dp)
                ) {
                    Text(
                        text = "MindMaze",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Bomb Puzzle Challenge",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.Black.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Animation
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        modifier = Modifier.size(250.dp)
                    )
                }

                // Bouton START
                Button(
                    onClick = {
                        when {
                            // Pas d'internet
                            !NetworkUtils.isInternetAvailable(context) -> {
                                showNoInternetDialog = true
                            }
                            // ✅ Tous les niveaux terminés
                            LevelPreferences.isAllLevelsCompleted(context) -> {
                                showGameCompletedDialog = true
                            }
                            // Navigation normale
                            else -> {
                                onPlayClicked()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(bottom = 20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(30.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "START PLAYING",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Banner Ad
            BannerAdView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .navigationBarsPadding()
            )
        }
    }
}