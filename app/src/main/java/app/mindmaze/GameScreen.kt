package app.mindmaze

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import app.mindmaze.lives.LivesViewModel
import app.mindmaze.rules.RulesValidator
import app.mindmaze.vm.GameViewModel

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    onBack: () -> Unit,
    viewModel: GameViewModel = viewModel(),
    livesViewModel: LivesViewModel = viewModel()
) {
    val context = LocalContext.current

    var levels by remember { mutableStateOf<List<PuzzleLevel>?>(null) }
    var showTutorial by remember { mutableStateOf(!TutorialPreferences.isTutorialShown(context)) }
    var showNoInternetDialog by remember { mutableStateOf(false) }
    var isLoadingLevels by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var isTransitioning by remember { mutableStateOf(false) }
    var showGameCompletedDialog by remember { mutableStateOf(false) }

    val lives by livesViewModel.lives
    val timeToNextLife by livesViewModel.timeToNextLife
    var showLevelSuccess by remember { mutableStateOf(false) }
    var showBrokenHeart by remember { mutableStateOf(false) }
    var violationMessage by remember { mutableStateOf("") }
    var showLivesDialog by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }
    var shakeKey by remember { mutableStateOf(0) }
    LaunchedEffect(shakeKey) {
        if (shakeKey > 0) {
            repeat(4) { i -> shakeOffset.animateTo(if (i % 2 == 0) -18f else 18f, tween(60)) }
            shakeOffset.animateTo(0f, tween(60))
        }
    }

    var prevBombCount by remember { mutableStateOf(-1) }
    var lastPlacedBombCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val currentIndex by viewModel.currentLevelIndex
    val boardState by derivedStateOf { viewModel.boardState }
    val hasWon by viewModel.hasWon

    BackHandler(enabled = true) { onBack() }

    // ── Load levels ────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        livesViewModel.refresh()
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
                loadError = "No levels available"
                isLoadingLevels = false
                return@LaunchedEffect
            }
            levels = loadedLevels
            val lastSavedIndex = LevelPreferences.loadLastLevel(context).coerceIn(0, loadedLevels.lastIndex)
            viewModel.currentLevelIndex.value = lastSavedIndex
            val currentLevel = loadedLevels[lastSavedIndex]
            val size = PuzzleLevels.getBoardSize(currentLevel)
            viewModel.initBoard(size, currentLevel)
            val savedBoard = LevelPreferences.loadBoardState(context, lastSavedIndex, size)
            if (savedBoard != null && savedBoard.size == size) viewModel.restoreBoardState(savedBoard)
            isLoadingLevels = false
        } catch (e: Exception) {
            loadError = "Loading error: ${e.message}"
            isLoadingLevels = false
            if (!NetworkUtils.isInternetAvailable(context)) showNoInternetDialog = true
        }
    }

    val currentLevel = levels?.getOrNull(currentIndex)
    val isFullyLoaded =
        levels != null && currentLevel != null && viewModel.isBoardReady && boardState.isNotEmpty()

    LaunchedEffect(currentIndex) {
        levels?.let { if (currentIndex in it.indices) LevelPreferences.saveLastLevel(context, currentIndex) }
    }
    LaunchedEffect(boardState) {
        if (isFullyLoaded && !hasWon && !isTransitioning) {
            LevelPreferences.saveBoardState(context, currentIndex, boardState)
        }
    }

    // ── Auto-victory detection ─────────────────────────────────────────────────
    LaunchedEffect(boardState, currentLevel) {
        if (isFullyLoaded && !hasWon && !isTransitioning && !showLevelSuccess) {
            val size = boardState.size
            val matrix = PuzzleLevels.buildMatrix(currentLevel!!, size)
            if (checkVictory(boardState, size, matrix)) {
                viewModel.hasWon.value = true
                LevelPreferences.clearBoardState(context, currentIndex)
                showLevelSuccess = true
            }
        }
    }

    // ── Auto-violation: fires when a new bomb is placed ────────────────────────
    LaunchedEffect(boardState) {
        if (!isFullyLoaded || isTransitioning || hasWon || showLevelSuccess) return@LaunchedEffect
        val bombCount = boardState.sumOf { row -> row.count { it == 2 } }
        if (prevBombCount == -1) { prevBombCount = bombCount; return@LaunchedEffect }
        if (bombCount > prevBombCount) {
            val size = boardState.size
            val matrix = PuzzleLevels.buildMatrix(currentLevel!!, size)
            val result = RulesValidator.validate(boardState, matrix, size)
            if (result.hasViolation) {
                // Remove the offending bomb immediately so the board is clean before the animation
                lastPlacedBombCell?.let { (r, c) ->
                    if (boardState.getOrNull(r)?.getOrNull(c) == 2) {
                        viewModel.clearCell(r, c)
                    }
                }
                livesViewModel.loseLife()
                shakeKey++
                violationMessage = result.message
                showBrokenHeart = true
            }
        }
        prevBombCount = bombCount
    }

    // ── Advance to next level ─────────────────────────────────────────────────
    val goToNextLevel: () -> Unit = {
        prevBombCount = -1
        val next = currentIndex + 1
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
            LevelPreferences.setAllLevelsCompleted(context)
            isTransitioning = false
            showGameCompletedDialog = true
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────
    if (showNoInternetDialog) {
        NoInternetDialog(
            onDismiss = { showNoInternetDialog = false; onBack() },
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

    if (showGameCompletedDialog) {
        AlertDialog(
            onDismissRequest = { showGameCompletedDialog = false; onBack() },
            title = {
                Text(
                    text = "🏆 Congratulations!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )
            },
            text = {
                Text(
                    text = "You completed all levels!\n\nNew levels are coming in the next update 🚀",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF64748B),
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 24.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showGameCompletedDialog = false; onBack() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
                ) {
                    Text("Back to Home", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    // ── Main UI ───────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.White,
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
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black, modifier = Modifier.size(24.dp))
                        }
                    },
                    actions = {
                        IconButton(onClick = { showTutorial = true }) {
                            Icon(Icons.Default.Info, contentDescription = "Help", tint = Color.Black, modifier = Modifier.size(24.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            bottomBar = {
                Column {
                    Spacer(modifier = Modifier.height(4.dp))
                    BannerAdView(modifier = Modifier.fillMaxWidth().navigationBarsPadding())
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.White)
            ) {
                // ── Lives row (tappable) ──────────────────────────────────────
                if (isFullyLoaded) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { showLivesDialog = true }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LivesComponent(lives = lives)
                    }
                }

                // ── Game content ──────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = shakeOffset.value.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isTransitioning -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color.Black, strokeWidth = 8.dp)
                                Spacer(Modifier.height(32.dp))
                                Text("Loading next level...", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                        loadError != null -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                Text("❌", fontSize = 48.sp)
                                Spacer(Modifier.height(16.dp))
                                Text(loadError!!, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.Black, textAlign = TextAlign.Center)
                            }
                        }
                        isLoadingLevels -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color.Black, strokeWidth = 8.dp)
                                Spacer(Modifier.height(32.dp))
                                Text("Loading level...", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                        isFullyLoaded -> {
                            PuzzleGame(
                                level = currentLevel!!,
                                boardState = boardState,
                                onCellToggle = { r, c ->
                                    // State cycle: 0→1→2→0. Track when a bomb (2) is about to be placed.
                                    val cur = boardState.getOrNull(r)?.getOrNull(c) ?: 0
                                    if (cur == 1) lastPlacedBombCell = r to c
                                    viewModel.toggleCell(r, c)
                                }
                            )
                        }
                    }
                }
            }
        }

        // ── Tutorial overlay ──────────────────────────────────────────────────
        if (showTutorial) {
            TutorialOverlay(onSkip = {
                TutorialPreferences.setTutorialShown(context)
                showTutorial = false
            })
        }

        // ── Broken heart (violation) ──────────────────────────────────────────
        if (showBrokenHeart) {
            BrokenHeartOverlay(
                message = violationMessage,
                livesRemaining = lives,
                onDismiss = { showBrokenHeart = false }
            )
        }

        // ── Lives detail dialog ───────────────────────────────────────────────
        if (showLivesDialog) {
            LivesDetailDialog(
                lives = lives,
                timeToNextLife = timeToNextLife,
                onWatchVideo = {
                    livesViewModel.addLife()
                    showLivesDialog = false
                },
                onDismiss = { showLivesDialog = false }
            )
        }

        // ── Level success animation ───────────────────────────────────────────
        if (showLevelSuccess) {
            LevelSuccessAnimation(
                levelNumber = currentIndex + 1,
                onContinue = {
                    showLevelSuccess = false
                    isTransitioning = true
                    goToNextLevel()   // No interstitial — banner only
                }
            )
        }

        // ── Game Over (no lives left) ─────────────────────────────────────────
        if (lives == 0 && isFullyLoaded && !showLevelSuccess) {
            GameOverScreen(
                timeToNextLife = timeToNextLife,
                onLifeEarned = { livesViewModel.addLife() },
                onGoHome = onBack
            )
        }
    }
}
