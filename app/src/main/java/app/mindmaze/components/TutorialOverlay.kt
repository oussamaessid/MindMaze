package app.mindmaze.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun TutorialOverlay(
    onSkip: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val isSmallScreen = screenHeight < 700.dp
    val isMediumScreen = screenHeight < 800.dp

    val titleSize = when {
        isSmallScreen -> 20.sp
        isMediumScreen -> 22.sp
        else -> 26.sp
    }

    val subtitleSize = when {
        isSmallScreen -> 12.sp
        isMediumScreen -> 13.sp
        else -> 15.sp
    }

    val buttonTextSize = when {
        isSmallScreen -> 14.sp
        isMediumScreen -> 15.sp
        else -> 17.sp
    }

    val buttonHeight = when {
        isSmallScreen -> 44.dp
        isMediumScreen -> 48.dp
        else -> 54.dp
    }

    val cardPadding = when {
        isSmallScreen -> 8.dp
        isMediumScreen -> 10.dp
        else -> 14.dp
    }

    val verticalSpacing = when {
        isSmallScreen -> 6.dp
        isMediumScreen -> 8.dp
        else -> 12.dp
    }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.9f))
                .systemBarsPadding()
        ) {

            IconButton(
                onClick = onSkip,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }

            // MAIN CONTENT - Adaptatif à toutes les tailles d'écran
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 56.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Section du titre
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.wrapContentHeight()
                ) {
                    Text(
                        text = "👋 Welcome to MindMaze!",
                        fontSize = titleSize,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Learn how to play",
                        fontSize = subtitleSize,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(verticalSpacing))

                // Contenu principal - utilise exactement l'espace disponible
                Column(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(verticalSpacing, Alignment.CenterVertically)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(cardPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            HowToPlaySection()
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(cardPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            ExamplesSectionHorizontal()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(verticalSpacing))

                // Bouton
                Button(
                    onClick = onSkip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black
                    ),
                    shape = RoundedCornerShape(buttonHeight / 2),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp
                    )
                ) {
                    Text(
                        text = "GOT IT! START PLAYING 🚀",
                        fontSize = buttonTextSize,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}