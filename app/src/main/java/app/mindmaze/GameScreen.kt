package app.mindmaze

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
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
import androidx.lifecycle.viewmodel.compose.viewModel
import app.mindmaze.components.*
import app.mindmaze.data.model.PuzzleLevel
import app.mindmaze.data.repositoryImp.PuzzleLevels
import app.mindmaze.vm.GameViewModel

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    onBack: () -> Unit,
    viewModel: GameViewModel = viewModel()
) {
    val context = LocalContext.current
    val interstitialAdManager = remember { InterstitialAdManager(context) }

    var levels by remember { mutableStateOf<List<PuzzleLevel>?>(null) }
    var showTutorial by remember { mutableStateOf(!TutorialPreferences.isTutorialShown(context)) }
    var showNoInternetDialog by remember { mutableStateOf(false) }
    var isLoadingLevels by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var isTransitioning by remember { mutableStateOf(false) }

    // ✅ NOUVEAU - Dialog "Jeu terminé"
    var showGameCompletedDialog by remember { mutableStateOf(false) }

    val currentIndex by viewModel.currentLevelIndex
    val boardState by derivedStateOf { viewModel.boardState }
    val hasWon by viewModel.hasWon

    // Gestion du bouton retour système
    BackHandler(enabled = true) {
        onBack()
    }

    LaunchedEffect(Unit) {
        if (!NetworkUtils.isInternetAvailable(context)) {
            showNoInternetDialog = true
            isLoadingLevels = false
            return@LaunchedEffect
        }

        try {
            val loadedLevels = PuzzleLevels.loadLevelsFromRemote(context)
            if (loadedLevels.isEmpty()) {
                levels = loadedLevels
                viewModel.currentLevelIndex.value = 0
                loadError = "Aucun niveau disponible"
                isLoadingLevels = false
                return@LaunchedEffect
            }

            levels = loadedLevels

            val lastSavedIndex = LevelPreferences.loadLastLevel(context)
                .coerceIn(0, loadedLevels.lastIndex)

            viewModel.currentLevelIndex.value = lastSavedIndex

            val currentLevel = loadedLevels[lastSavedIndex]
            val size = PuzzleLevels.getBoardSize(currentLevel)
            viewModel.initBoard(size, currentLevel)

            val savedBoard = LevelPreferences.loadBoardState(context, lastSavedIndex, size)
            if (savedBoard != null && savedBoard.size == size) {
                viewModel.restoreBoardState(savedBoard)
            }

            isLoadingLevels = false
        } catch (e: Exception) {
            loadError = "Erreur de chargement: ${e.message}"
            isLoadingLevels = false
            if (!NetworkUtils.isInternetAvailable(context)) {
                showNoInternetDialog = true
            }
        }
    }

    val currentLevel = levels?.getOrNull(currentIndex)
    val isFullyLoaded =
        levels != null && currentLevel != null && viewModel.isBoardReady && boardState.isNotEmpty()

    // Sauvegarde index
    LaunchedEffect(currentIndex) {
        levels?.let {
            if (currentIndex in it.indices) {
                LevelPreferences.saveLastLevel(context, currentIndex)
            }
        }
    }

    // Sauvegarde plateau
    LaunchedEffect(boardState) {
        if (isFullyLoaded && !hasWon && !isTransitioning) {
            LevelPreferences.saveBoardState(context, currentIndex, boardState)
        }
    }

    // ✅ Victoire - Passage automatique au niveau suivant
    LaunchedEffect(boardState, currentLevel) {
        if (isFullyLoaded && !hasWon && !isTransitioning) {
            val size = boardState.size
            val matrix = PuzzleLevels.buildMatrix(currentLevel!!, size)
            if (checkVictory(boardState, size, matrix)) {
                viewModel.hasWon.value = true
                LevelPreferences.clearBoardState(context, currentIndex)

                isTransitioning = true

                val goToNextLevel = {
                    val next = currentIndex + 1

                    // ✅ Vérifier s'il y a un niveau suivant
                    if (next <= (levels?.lastIndex ?: 0)) {
                        viewModel.currentLevelIndex.value = next
                        val nextLevel = levels!![next]
                        val nextSize = PuzzleLevels.getBoardSize(nextLevel)
                        viewModel.initBoard(nextSize, nextLevel)
                        val savedNext = LevelPreferences.loadBoardState(context, next, nextSize)
                        if (savedNext != null) viewModel.restoreBoardState(savedNext)

                        viewModel.hasWon.value = false
                        isTransitioning = false
                    } else {
                        // ✅ C'était le dernier niveau!
                        LevelPreferences.setAllLevelsCompleted(context)
                        isTransitioning = false
                        // Afficher la dialog avant de revenir
                        showGameCompletedDialog = true
                    }
                }

                // Afficher pub puis passer au niveau suivant
                interstitialAdManager.showAd(
                    onAdDismissed = { goToNextLevel() },
                    onAdFailed = { goToNextLevel() }
                )
            }
        }
    }

    // Dialog pas d'internet
    if (showNoInternetDialog) {
        NoInternetDialog(
            onDismiss = {
                showNoInternetDialog = false
                onBack()
            },
            onRetry = {
                if (NetworkUtils.isInternetAvailable(context)) {
                    showNoInternetDialog = false
                    isLoadingLevels = true
                    loadError = null
                    viewModel.currentLevelIndex.value = 0
                }
            }
        )
    }

    // ✅ Dialog "Jeu terminé"
    if (showGameCompletedDialog) {
        AlertDialog(
            onDismissRequest = {
                showGameCompletedDialog = false
                onBack()
            },
            title = {
                Text(
                    text = "🏆 Félicitations!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )
            },
            text = {
                Text(
                    text = "Vous avez complété tous les niveaux!\n\nAttendez les prochains niveaux dans la mise à jour suivante 🚀",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF64748B),
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 24.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGameCompletedDialog = false
                        onBack()
                    },
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
                        "Retour à l'accueil",
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (isFullyLoaded && !isTransitioning) {
                            Text(
                                text = "Level ${currentIndex + 1}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxWidth())
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Retour",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showTutorial = true }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Aide",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
            },
            // Banner placée dans bottomBar pour qu'elle ne chevauche jamais le contenu du jeu
            bottomBar = {
                Column {
                    Spacer(modifier = Modifier.height(4.dp))
                    BannerAdView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isTransitioning -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.Black, strokeWidth = 8.dp)
                            Spacer(Modifier.height(32.dp))
                            Text(
                                "Chargement du prochain niveau...",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }

                    loadError != null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "❌",
                                fontSize = 48.sp
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = loadError!!,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    isLoadingLevels -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.Black, strokeWidth = 8.dp)
                            Spacer(Modifier.height(32.dp))
                            Text(
                                "Chargement du niveau...",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }

                    isFullyLoaded -> {
                        PuzzleGame(
                            level = currentLevel!!,
                            boardState = boardState,
                            onCellToggle = { r, c -> viewModel.toggleCell(r, c) }
                        )
                    }
                }
            }
        }

        if (showTutorial) {
            TutorialOverlay(onSkip = {
                TutorialPreferences.setTutorialShown(context)
                showTutorial = false
            })
        }
    }
}