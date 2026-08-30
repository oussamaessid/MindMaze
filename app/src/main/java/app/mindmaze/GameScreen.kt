package app.mindmaze

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.mindmaze.audio.SoundManager
import app.mindmaze.components.*
import app.mindmaze.data.model.PuzzleLevel
import app.mindmaze.data.repositoryImp.GuideLevel
import app.mindmaze.data.repositoryImp.PuzzleLevels
import app.mindmaze.lives.LivesViewModel
import app.mindmaze.rules.RulesValidator
import app.mindmaze.vm.GameViewModel
import kotlinx.coroutines.delay

@Composable
private fun GameRuleCards(modifier: Modifier = Modifier) {
    val rules = listOf(
        "ONE PER\nCOLOR",
        "ROW +\nCOLUMN",
        "NEVER\nTOUCH"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rules.forEach { label ->
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(76.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD9E3F4)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.fire_boom_character),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                    Text(
                        text = label,
                        color = Color(0xFF2778C9),
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    onBack: () -> Unit,
    viewModel: GameViewModel = viewModel(),
    livesViewModel: LivesViewModel = viewModel()
) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager.get(context) }
    val haptic = LocalHapticFeedback.current

    var levels by remember { mutableStateOf<List<PuzzleLevel>?>(null) }
    // The "?" button opens this static reference overlay on demand, any time.
    var showTutorial by remember { mutableStateOf(false) }
    // First-time-only interactive level 0. It uses the first puzzle's real rules, then
    // resets the board before level 1 starts so guide marks never leak into normal play.
    var guideActive by remember { mutableStateOf(!TutorialPreferences.isTutorialShown(context)) }
    var showWelcome by remember { mutableStateOf(guideActive) }
    // 0 = opening X swipe, 1..4 = the four BOOMS, 5 = complete.
    var guideStep by remember { mutableStateOf(0) }
    var guideFinishing by remember { mutableStateOf(false) }
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
    var boomEvent by remember { mutableStateOf<CellEvent?>(null) }
    var errorCellEvent by remember { mutableStateOf<CellEvent?>(null) }
    var successCellEvent by remember { mutableStateOf<CellEvent?>(null) }

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
            val lastSavedIndex = if (guideActive) 0 else {
                LevelPreferences.loadLastLevel(context).coerceIn(0, loadedLevels.lastIndex)
            }
            viewModel.currentLevelIndex.value = lastSavedIndex
            val initialLevel = if (guideActive) GuideLevel.level else loadedLevels[lastSavedIndex]
            val size = PuzzleLevels.getBoardSize(initialLevel)
            viewModel.initBoard(size, initialLevel)
            if (!guideActive) {
                val savedBoard = LevelPreferences.loadBoardState(context, lastSavedIndex, size)
                if (savedBoard != null && savedBoard.size == size) viewModel.restoreBoardState(savedBoard)
            }
            isLoadingLevels = false
        } catch (e: Exception) {
            loadError = "Loading error: ${e.message}"
            isLoadingLevels = false
            if (!NetworkUtils.isInternetAvailable(context)) showNoInternetDialog = true
        }
    }

    val currentLevel = if (guideActive) GuideLevel.level else levels?.getOrNull(currentIndex)
    val isFullyLoaded =
        levels != null && currentLevel != null && viewModel.isBoardReady && boardState.isNotEmpty()

    fun finishGuide() {
        guideActive = false
        showWelcome = false
        guideFinishing = false
        TutorialPreferences.setTutorialShown(context)
        levels?.firstOrNull()?.let { firstLevel ->
            viewModel.currentLevelIndex.value = 0
            viewModel.initBoard(PuzzleLevels.getBoardSize(firstLevel), firstLevel)
        }
        prevBombCount = -1
        lastPlacedBombCell = null
    }

    LaunchedEffect(guideFinishing) {
        if (guideFinishing) {
            delay(900)
            finishGuide()
        }
    }

    LaunchedEffect(currentIndex) {
        levels?.let { if (currentIndex in it.indices) LevelPreferences.saveLastLevel(context, currentIndex) }
    }
    LaunchedEffect(boardState) {
        if (isFullyLoaded && !hasWon && !isTransitioning && !guideActive) {
            LevelPreferences.saveBoardState(context, currentIndex, boardState)
        }
    }

    // ── Auto-victory detection ─────────────────────────────────────────────────
    LaunchedEffect(boardState, currentLevel) {
        if (isFullyLoaded && !hasWon && !isTransitioning && !showLevelSuccess && !guideActive) {
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
        if (guideActive) {
            prevBombCount = bombCount
            return@LaunchedEffect
        }
        if (prevBombCount == -1) { prevBombCount = bombCount; return@LaunchedEffect }
        if (bombCount > prevBombCount) {
            val size = boardState.size
            val matrix = PuzzleLevels.buildMatrix(currentLevel!!, size)
            val result = RulesValidator.validate(boardState, matrix, size)
            val bombCell = lastPlacedBombCell
            if (result.hasViolation) {
                // Turn the offending bomb into an X (tried-and-wrong marker) instead of
                // clearing it, right as the red error flash plays over it.
                bombCell?.let { (r, c) ->
                    if (boardState.getOrNull(r)?.getOrNull(c) == 2) {
                        viewModel.markInvalidBombAsErrorX(r, c)
                    }
                    errorCellEvent = CellEvent(r, c, System.nanoTime())
                }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                soundManager.playError()
                livesViewModel.loseLife()
                shakeKey++
                violationMessage = result.message
                showBrokenHeart = true
            } else {
                bombCell?.let { (r, c) -> successCellEvent = CellEvent(r, c, System.nanoTime()) }
                soundManager.playSuccess()
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
                                text = if (guideActive) "Guide • Level 0" else "Level ${currentIndex + 1}",
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

                // Keep the three core rules visible during normal gameplay.
                if (isFullyLoaded && !guideActive) {
                    GameRuleCards()
                }

                // ── First-launch guide banner ───────────────────────────────────
                // Guide sequence: complete a dedicated four-BOOM puzzle, including X swipe.
                if (guideActive && !showWelcome && isFullyLoaded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .background(Color(0xFF111827), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.fire_boom_character),
                                contentDescription = "Fire guide",
                                modifier = Modifier.size(58.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (guideStep) {
                                    0 -> "Start here: swipe across the three glowing cells to place several ✕ marks at once."
                                    1 -> "All BOOM positions are now indicated. Double-tap or press and hold any glowing BOOM cell."
                                    2 -> "Great! Three BOOMS remain. Choose another indicated cell."
                                    3 -> "Good! Two BOOMS remain. Keep using double-tap or press and hold."
                                    4 -> "One final BOOM remains. Place it to complete Level 0."
                                    else -> "Excellent! Level 0 is complete. Level 1 is starting…"
                                },
                                color = Color.White,
                                fontSize = 14.sp,
                                lineHeight = 19.sp
                            )
                            if (guideStep == 0) {
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(R.drawable.point),
                                        contentDescription = "Swipe finger",
                                        modifier = Modifier.size(30.dp)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text("SWIPE  →", color = Color(0xFFFFB74D), fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(5.dp))
                                    repeat(3) {
                                        Text("✕", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.width(3.dp))
                                    }
                                }
                            }
                            }
                        }
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
                                onBoomLongPress = { r, c ->
                                    val alreadyPlacedGuideBoom = guideActive &&
                                        boardState.getOrNull(r)?.getOrNull(c) == 2
                                    val placed = if (alreadyPlacedGuideBoom) false else viewModel.placeBomb(r, c)
                                    if (placed) {
                                        lastPlacedBombCell = r to c
                                        boomEvent = CellEvent(r, c, System.nanoTime())
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        soundManager.playBoom()
                                        if (guideActive) {
                                            val solutionIndex = if (guideStep in 1..4) guideStep - 1 else -1
                                            val isSolutionCell = (r to c) in GuideLevel.solution
                                            if (solutionIndex >= 0 && isSolutionCell) {
                                                if (guideStep == 1) viewModel.autoMarkAroundBomb(r, c)
                                                successCellEvent = CellEvent(r, c, System.nanoTime())
                                                soundManager.playSuccess()
                                                val guideSolved = GuideLevel.solution.all { (sr, sc) ->
                                                    viewModel.boardState.getOrNull(sr)?.getOrNull(sc) == 2
                                                }
                                                if (guideSolved) {
                                                    guideStep = 5
                                                    guideFinishing = true
                                                } else {
                                                    guideStep++
                                                }
                                            } else {
                                                viewModel.markInvalidBombAsErrorX(r, c)
                                                errorCellEvent = CellEvent(r, c, System.nanoTime())
                                                soundManager.playError()
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                shakeKey++
                                            }
                                        }
                                    }
                                },
                                onXPlace = { r, c, isSwipe ->
                                    viewModel.placeX(r, c, isSwipe)
                                    if (guideActive && guideStep == 0 && isSwipe && (r to c) in GuideLevel.swipeCells) {
                                        guideStep = 1
                                    }
                                },
                                boomEvent = boomEvent,
                                errorEvent = errorCellEvent,
                                successEvent = successCellEvent,
                                highlightCells = if (guideActive && !showWelcome && !guideFinishing) {
                                    if (guideStep == 0) GuideLevel.swipeCells
                                    else GuideLevel.solution.filterTo(mutableSetOf()) { (sr, sc) ->
                                        boardState.getOrNull(sr)?.getOrNull(sc) != 2
                                    }
                                } else emptySet(),
                                guidePointerCell = if (guideActive && !showWelcome && !guideFinishing) {
                                    if (guideStep == 0) GuideLevel.swipeCells.first()
                                    else GuideLevel.solution.firstOrNull { (sr, sc) ->
                                        boardState.getOrNull(sr)?.getOrNull(sc) != 2
                                    }
                                } else null,
                                maxCellSize = if (guideActive) 84.dp else 72.dp
                            )
                        }
                    }
                }
            }
        }

        // ── First-install welcome screen ──────────────────────────────────────
        if (showWelcome && guideActive) {
            AlertDialog(
                onDismissRequest = { },
                icon = {
                    Image(
                        painter = painterResource(R.drawable.fire_boom_character),
                        contentDescription = "KABOOM fire character",
                        modifier = Modifier.size(128.dp)
                    )
                },
                title = {
                    Text(
                        text = "Welcome to KABOOM!",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Find one BOOM in every row, column, and colored zone. BOOMS can never touch — not even diagonally.",
                            fontSize = 16.sp,
                            lineHeight = 23.sp,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF475569)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "A short Level 0 guide will show you each move.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Color(0xFFFF6B35)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showWelcome = false },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B35))
                    ) {
                        Text("START THE GUIDE", fontWeight = FontWeight.ExtraBold)
                    }
                },
                shape = RoundedCornerShape(28.dp),
                containerColor = Color.White
            )
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
