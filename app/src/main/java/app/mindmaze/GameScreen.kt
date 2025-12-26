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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    var showVictoryDialog by remember { mutableStateOf(false) }
    var showTutorial by remember { mutableStateOf(!TutorialPreferences.isTutorialShown(context)) }

    val currentIndex by viewModel.currentLevelIndex
    val boardState by derivedStateOf { viewModel.boardState }
    val hasWon by viewModel.hasWon

    // Chargement complet des niveaux + restauration
    LaunchedEffect(Unit) {
        val loadedLevels = PuzzleLevels.loadLevelsFromRemote(context)
        if (loadedLevels.isEmpty()) {
            levels = loadedLevels
            viewModel.currentLevelIndex.value = 0
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
        if (isFullyLoaded && !hasWon) {
            LevelPreferences.saveBoardState(context, currentIndex, boardState)
        }
    }

    // Victoire
    LaunchedEffect(boardState, currentLevel) {
        if (isFullyLoaded && !hasWon) {
            val size = boardState.size
            val matrix = PuzzleLevels.buildMatrix(currentLevel!!, size)
            if (checkVictory(boardState, size, matrix)) {
                viewModel.hasWon.value = true
                showVictoryDialog = true
                LevelPreferences.clearBoardState(context, currentIndex)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (isFullyLoaded) {
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
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (isFullyLoaded) {
                    PuzzleGame(
                        level = currentLevel!!,
                        boardState = boardState,
                        onCellToggle = { r, c -> viewModel.toggleCell(r, c) }
                    )
                } else {
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
            }
        }

        BannerAdView(modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth())

        if (showVictoryDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(
                        if (currentIndex < (levels?.lastIndex ?: 0)) "Level Completed!"
                        else "You Won Everything!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                text = { Text("Ready for the next challenge?") },
                confirmButton = {
                    Button(
                        onClick = {
                            if (currentIndex < (levels?.lastIndex ?: 0)) {
                                interstitialAdManager.showAd(
                                    onAdDismissed = {
                                        val next = currentIndex + 1
                                        viewModel.currentLevelIndex.value = next
                                        val nextLevel = levels!![next]
                                        val size = PuzzleLevels.getBoardSize(nextLevel)
                                        viewModel.initBoard(size, nextLevel)
                                        val savedNext =
                                            LevelPreferences.loadBoardState(context, next, size)
                                        if (savedNext != null) viewModel.restoreBoardState(savedNext)
                                        showVictoryDialog = false
                                    },
                                    onAdFailed = {
                                        val next = currentIndex + 1
                                        viewModel.currentLevelIndex.value = next
                                        val nextLevel = levels!![next]
                                        val size = PuzzleLevels.getBoardSize(nextLevel)
                                        viewModel.initBoard(size, nextLevel)
                                        val savedNext =
                                            LevelPreferences.loadBoardState(context, next, size)
                                        if (savedNext != null) viewModel.restoreBoardState(savedNext)
                                        showVictoryDialog = false
                                    }
                                )
                            } else {
                                showVictoryDialog = false
                                onBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text(
                            if (currentIndex < (levels?.lastIndex ?: 0)) "Next Level" else "Finish",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = if (currentIndex < (levels?.lastIndex ?: 0)) {
                    { TextButton(onClick = { showVictoryDialog = false }) { Text("Stay") } }
                } else null,
                containerColor = Color.White
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