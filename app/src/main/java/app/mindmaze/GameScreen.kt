package app.mindmaze

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.mindmaze.components.BannerAdView
import app.mindmaze.components.NoInternetDialog
import app.mindmaze.components.PuzzleGame
import app.mindmaze.components.TutorialOverlay
import app.mindmaze.components.checkVictory
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

    // État pour masquer le jeu pendant la transition
    var isTransitioning by remember { mutableStateOf(false) }

    val currentIndex by viewModel.currentLevelIndex
    val boardState by derivedStateOf { viewModel.boardState }
    val hasWon by viewModel.hasWon

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

    // Victoire - Passage automatique au niveau suivant
    LaunchedEffect(boardState, currentLevel) {
        if (isFullyLoaded && !hasWon && !isTransitioning) {
            val size = boardState.size
            val matrix = PuzzleLevels.buildMatrix(currentLevel!!, size)
            if (checkVictory(boardState, size, matrix)) {
                viewModel.hasWon.value = true
                LevelPreferences.clearBoardState(context, currentIndex)

                // Masquer le jeu immédiatement pour éviter de voir le niveau précédent
                isTransitioning = true

                // Fonction pour passer au niveau suivant
                val goToNextLevel = {
                    val next = currentIndex + 1
                    viewModel.currentLevelIndex.value = next
                    val nextLevel = levels!![next]
                    val nextSize = PuzzleLevels.getBoardSize(nextLevel)
                    viewModel.initBoard(nextSize, nextLevel)
                    val savedNext = LevelPreferences.loadBoardState(context, next, nextSize)
                    if (savedNext != null) viewModel.restoreBoardState(savedNext)

                    // Réinitialiser les états après avoir chargé le nouveau niveau
                    viewModel.hasWon.value = false
                    isTransitioning = false
                }

                // Vérifier s'il y a un niveau suivant
                if (currentIndex < (levels?.lastIndex ?: 0)) {
                    // Afficher la publicité puis passer automatiquement au niveau suivant
                    interstitialAdManager.showAd(
                        onAdDismissed = {
                            // Pub terminée -> Passer au niveau suivant
                            goToNextLevel()
                        },
                        onAdFailed = {
                            // Pub échouée -> Passer quand même au niveau suivant
                            goToNextLevel()
                        }
                    )
                } else {
                    // Dernier niveau terminé, retour à l'écran précédent
                    isTransitioning = false
                    onBack()
                }
            }
        }
    }

    // Show no internet dialog
    if (showNoInternetDialog) {
        NoInternetDialog(
            onDismiss = {
                showNoInternetDialog = false
                onBack() // Retourner à l'écran d'accueil si pas d'Internet
            },
            onRetry = {
                if (NetworkUtils.isInternetAvailable(context)) {
                    showNoInternetDialog = false
                    isLoadingLevels = true
                    loadError = null
                    // Relancer le chargement
                    viewModel.currentLevelIndex.value = 0
                }
            }
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
                                fontSize = 24.sp,
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
                                contentDescription = "Back",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showTutorial = true }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Help",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(bottom = 60.dp) // Marge pour la bannière publicitaire
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                when {
                    // Afficher loader pendant la transition entre niveaux
                    isTransitioning -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.Black, strokeWidth = 8.dp)
                            Spacer(Modifier.height(32.dp))
                            Text(
                                "Loading next level...",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                    // Afficher erreur si échec de chargement
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
                    // Afficher loader pendant le chargement
                    isLoadingLevels -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.Black, strokeWidth = 8.dp)
                            Spacer(Modifier.height(32.dp))
                            Text(
                                "Loading level...",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                    // Afficher le jeu une fois chargé
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

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White) // Background opaque pour éviter la transparence
                .padding(bottom = 8.dp) // Marge en bas pour les téléphones avec barre de navigation
        ) {
            BannerAdView(
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (showTutorial) {
            TutorialOverlay(onSkip = {
                TutorialPreferences.setTutorialShown(context)
                showTutorial = false
            })
        }
    }
}