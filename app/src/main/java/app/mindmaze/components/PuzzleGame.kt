package app.mindmaze.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.mindmaze.R
import app.mindmaze.data.model.PuzzleLevel
import app.mindmaze.data.repositoryImp.PuzzleLevels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** A one-shot signal targeting a single cell (placement / error / success). [id] must be unique per event so repeated hits on the same cell re-trigger the animation. */
data class CellEvent(val row: Int, val col: Int, val id: Long)

@Composable
fun PuzzleGame(
    level: PuzzleLevel,
    boardState: List<List<Int>>,
    onBoomLongPress: (Int, Int) -> Unit,
    onXPlace: (row: Int, col: Int, isSwipe: Boolean) -> Unit,
    isLoading: Boolean = false,
    boomEvent: CellEvent? = null,
    errorEvent: CellEvent? = null,
    successEvent: CellEvent? = null,
    highlightCells: Set<Pair<Int, Int>> = emptySet(),
    guidePointerCell: Pair<Int, Int>? = null,
    maxCellSize: Dp = 72.dp
) {
    val colors = PuzzleLevels.colors
    val boardSize = boardState.size

    if (boardSize == 0 || isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.Black, strokeWidth = 6.dp)
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Loading puzzle...",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
        return
    }

    val matrix = remember(level, boardSize) { PuzzleLevels.buildMatrix(level, boardSize) }
    val configuration = LocalConfiguration.current
    val cellSpacing = 4.dp
    val totalSpacing = cellSpacing * (boardSize - 1)
    val cellSize = ((configuration.screenWidthDp.dp - 16.dp - totalSpacing) / boardSize).coerceAtMost(maxCellSize)

    // Position des Bomb
    val queens = remember(boardState) {
        boardState.mapIndexed { r, row ->
            row.mapIndexedNotNull { c, value -> if (value == 2) r to c else null }
        }.flatten()
    }

    // Détection erreurs lignes/colonnes/régions
    val (badRows, badCols, badRegions) = remember(queens) {
        val rows = mutableSetOf<Int>()
        val cols = mutableSetOf<Int>()
        val regions = mutableSetOf<Int>()

        queens.groupBy { it.first }.forEach { (r, list) -> if (list.size > 1) rows += r }
        queens.groupBy { it.second }.forEach { (c, list) -> if (list.size > 1) cols += c }
        queens.groupBy { matrix[it.first][it.second] }.forEach { (_, list) ->
            if (list.size > 1) list.forEach { regions += matrix[it.first][it.second] }
        }
        Triple(rows, cols, regions)
    }

    // Bomb qui se touchent (y compris diagonale)
    val adjacentErrors = remember(queens) {
        val set = mutableSetOf<Pair<Int, Int>>()
        for (i in queens.indices) {
            for (j in i + 1 until queens.size) {
                val (r1, c1) = queens[i]
                val (r2, c2) = queens[j]
                if (abs(r1 - r2) <= 1 && abs(c1 - c2) <= 1 && (r1 != r2 || c1 != c2)) {
                    set += queens[i]
                    set += queens[j]
                }
            }
        }
        set
    }

    // Toutes les cellules en erreur → fond rouge + hachures
    val violatedCells = remember(badRows, badCols, badRegions, adjacentErrors) {
        val cells = mutableSetOf<Pair<Int, Int>>()
        badRows.forEach { r -> repeat(boardSize) { c -> cells += (r to c) } }
        badCols.forEach { c -> repeat(boardSize) { r -> cells += (r to c) } }
        for (r in 0 until boardSize) for (c in 0 until boardSize) {
            if (badRegions.contains(matrix[r][c])) cells += (r to c)
        }
        cells += adjacentErrors
        cells
    }

    val density = LocalDensity.current
    val cellSizePx = with(density) { cellSize.toPx() }
    val cellSpacingPx = with(density) { cellSpacing.toPx() }
    val longPressTimeoutMillis = LocalViewConfiguration.current.longPressTimeoutMillis
    var pressedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(cellSpacing),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Grille — a single gesture region spanning every cell so a drag can be tracked
        // continuously across cell boundaries (a per-cell clickable can't do this).
        Column(
            verticalArrangement = Arrangement.spacedBy(cellSpacing),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .pointerInput(boardSize, cellSizePx, cellSpacingPx) {
                    val pitch = cellSizePx + cellSpacingPx

                    fun cellAt(offset: Offset): Pair<Int, Int>? {
                        val col = (offset.x / pitch).toInt()
                        val row = (offset.y / pitch).toInt()
                        if (row !in 0 until boardSize || col !in 0 until boardSize) return null
                        // Ignore touches that land in the spacing gap between cells.
                        if (offset.x - col * pitch > cellSizePx) return null
                        if (offset.y - row * pitch > cellSizePx) return null
                        return row to col
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startCell = cellAt(down.position)
                        val pointerId = down.id
                        val visited = mutableSetOf<Pair<Int, Int>>()
                        val touchSlop = viewConfiguration.touchSlop
                        pressedCell = startCell

                        // Race the finger against the long-press timeout: it either lifts
                        // (short click → X), moves past the slop (swipe → X per cell), or
                        // the timeout wins outright (long press → boom).
                        var outcome = "released"
                        try {
                            withTimeout(viewConfiguration.longPressTimeoutMillis) {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    // An event pass with no change for our pointer (extra
                                    // hover/other-pointer noise) is NOT a release — only an
                                    // explicit "not pressed" on a matched change is.
                                    val change = event.changes.firstOrNull { it.id == pointerId } ?: continue
                                    if (!change.pressed) {
                                        outcome = "released"
                                        return@withTimeout
                                    }
                                    if ((change.position - down.position).getDistance() > touchSlop) {
                                        outcome = "dragging"
                                        return@withTimeout
                                    }
                                }
                                @Suppress("UNREACHABLE_CODE") Unit
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            outcome = "longpress"
                        } finally {
                            pressedCell = null
                        }

                        when (outcome) {
                            "longpress" -> {
                                startCell?.let { onBoomLongPress(it.first, it.second) }
                                // Drain the rest of the gesture; the boom already fired.
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == pointerId } ?: continue
                                    if (!change.pressed) break
                                    change.consume()
                                }
                            }
                            "dragging" -> {
                                startCell?.let { if (visited.add(it)) onXPlace(it.first, it.second, true) }
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == pointerId } ?: continue
                                    if (!change.pressed) break
                                    cellAt(change.position)?.let { cell ->
                                        if (visited.add(cell)) onXPlace(cell.first, cell.second, true)
                                    }
                                    change.consume()
                                }
                            }
                            else -> { // "released": could be a single click, or the first
                                // half of a double click — wait briefly for a second tap on
                                // the same cell before committing to X.
                                if (startCell != null) {
                                    val secondDown = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
                                        awaitFirstDown(requireUnconsumed = false)
                                    }
                                    if (secondDown != null && cellAt(secondDown.position) == startCell) {
                                        onBoomLongPress(startCell.first, startCell.second)
                                        val secondId = secondDown.id
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == secondId } ?: continue
                                            if (!change.pressed) break
                                            change.consume()
                                        }
                                    } else {
                                        onXPlace(startCell.first, startCell.second, false)
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            repeat(boardSize) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(cellSpacing)) {
                    repeat(boardSize) { col ->
                        GridCell(
                            row = row,
                            col = col,
                            bgColor = colors[matrix[row][col]],
                            state = boardState[row][col],
                            hasViolation = violatedCells.contains(row to col),
                            queenInError = adjacentErrors.contains(row to col),
                            thickRight = col < boardSize - 1 && matrix[row][col] != matrix[row][col + 1],
                            thickBottom = row < boardSize - 1 && matrix[row][col] != matrix[row + 1][col],
                            cellSize = cellSize,
                            isPressed = pressedCell == row to col,
                            longPressTimeoutMillis = longPressTimeoutMillis,
                            isHighlighted = (row to col) in highlightCells,
                            showGuidePointer = guidePointerCell == row to col,
                            boomEvent = boomEvent,
                            errorEvent = errorEvent,
                            successEvent = successEvent
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        val placed = queens.size
        Text(
            text = "$placed / $boardSize 💣",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = if (violatedCells.isNotEmpty()) Color.Red else Color.Black
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Tap / swipe → ✕  •  Double tap / long press → 💣",
            fontSize = 15.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun GridCell(
    row: Int,
    col: Int,
    bgColor: Color,
    state: Int,
    hasViolation: Boolean,
    queenInError: Boolean,
    thickRight: Boolean,
    thickBottom: Boolean,
    cellSize: Dp,
    isPressed: Boolean,
    longPressTimeoutMillis: Long,
    isHighlighted: Boolean = false,
    showGuidePointer: Boolean = false,
    boomEvent: CellEvent?,
    errorEvent: CellEvent?,
    successEvent: CellEvent?
) {
    // Tutorial pointer: a slow pulsing ring marking exactly which cell to interact with.
    val highlightPulse = rememberInfiniteTransition(label = "highlight")
    val highlightScale by highlightPulse.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "highlightScale"
    )

    // Immediate feedback the instant a finger goes down: a ring that fills up over the
    // long-press duration, so holding never "feels like nothing is happening".
    val pressProgress = remember { Animatable(0f) }
    LaunchedEffect(isPressed) {
        if (isPressed) {
            pressProgress.snapTo(0f)
            pressProgress.animateTo(1f, tween(longPressTimeoutMillis.toInt(), easing = LinearEasing))
        } else {
            pressProgress.snapTo(0f)
        }
    }

    // Boom placement: scale 0 → 1.15 → 1.0 with a fade-in, matching the game's existing
    // cell-cycle so a bomb restored from saved state simply appears at rest (no event).
    val boomScale = remember { Animatable(if (state == 2) 1f else 0f) }
    val boomAlpha = remember { Animatable(if (state == 2) 1f else 0f) }
    var showBoomParticles by remember { mutableStateOf(false) }

    // Single effect owning boomScale/boomAlpha: keying separately on `state` AND on
    // `boomEvent` (the old approach) let two coroutines fight over the same Animatables —
    // Animatable's internal mutex lets a later snapTo/animateTo call pre-empt an
    // in-progress one, so the pop-in animation could get cancelled mid-flight and leave
    // the boom stuck at alpha/scale 0 (invisible) after a long press. Keying on `state`
    // alone and reading `boomEvent` inside removes the race entirely.
    LaunchedEffect(state) {
        if (state == 2) {
            val isFreshPlacement = boomEvent != null && boomEvent.row == row && boomEvent.col == col
            if (isFreshPlacement) {
                showBoomParticles = true
                boomAlpha.snapTo(0f)
                boomScale.snapTo(0f)
                launch { boomAlpha.animateTo(1f, tween(120)) }
                boomScale.animateTo(1.15f, tween(160, easing = FastOutSlowInEasing))
                boomScale.animateTo(1f, tween(90))
                delay(220)
                showBoomParticles = false
            } else {
                // Silent restore (level load / saved board / level transition) — show at rest.
                boomScale.snapTo(1f)
                boomAlpha.snapTo(1f)
            }
        } else {
            boomScale.snapTo(0f)
            boomAlpha.snapTo(0f)
        }
    }

    // X placement: quick scale-in. Covers both a normal X (1) and a red "error X" (3) —
    // a bomb that was placed in an invalid spot and got turned into a lasting mistake marker.
    val xScale = remember { Animatable(if (state == 1 || state == 3) 1f else 0f) }
    LaunchedEffect(state) {
        if (state == 1 || state == 3) {
            xScale.snapTo(0f)
            xScale.animateTo(1f, tween(140, easing = FastOutSlowInEasing))
        } else {
            xScale.snapTo(0f)
        }
    }

    // Incorrect placement: red flash + local left/right shake.
    val errorShake = remember { Animatable(0f) }
    var errorFlash by remember { mutableStateOf(false) }
    LaunchedEffectSafe(errorEvent, row, col) {
        errorFlash = true
        repeat(3) { i -> errorShake.animateTo(if (i % 2 == 0) 9f else -9f, tween(45)) }
        errorShake.animateTo(0f, tween(45))
        delay(180)
        errorFlash = false
    }

    // Correct placement: a real multi-frame reaction (jump + tilt + two quick winks),
    // synchronized by GameScreen with the success sound.
    val successPulse = remember { Animatable(1f) }
    val successLift = remember { Animatable(0f) }
    val successTilt = remember { Animatable(0f) }
    var showSuccessParticles by remember { mutableStateOf(false) }
    var showWinkFrame by remember { mutableStateOf(false) }
    LaunchedEffectSafe(successEvent, row, col) {
        showSuccessParticles = true
        launch {
            successPulse.animateTo(1.28f, tween(120, easing = FastOutSlowInEasing))
            successPulse.animateTo(1f, tween(170))
        }
        launch {
            successLift.animateTo(1f, tween(150, easing = FastOutSlowInEasing))
            successLift.animateTo(0f, tween(210, easing = LinearOutSlowInEasing))
        }
        launch {
            successTilt.animateTo(-8f, tween(90))
            successTilt.animateTo(8f, tween(120))
            successTilt.animateTo(0f, tween(120))
        }
        showWinkFrame = true
        delay(130)
        showWinkFrame = false
        delay(90)
        showWinkFrame = true
        delay(130)
        showWinkFrame = false
        delay(180)
        showSuccessParticles = false
    }

    Box(
        modifier = Modifier
            .size(cellSize)
            .offset(x = errorShake.value.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            drawRect(color = bgColor, size = size)

            if (hasViolation) {
                drawRect(Color.Red.copy(alpha = 0.25f), size = size)

                val lineCount = 6
                val spacing = w / (lineCount + 1f)
                clipRect {
                    for (i in 1..lineCount) {
                        val xf = spacing * i
                        drawLine(
                            color = Color.Red.copy(alpha = 0.6f),
                            start = Offset(xf - h / 2, 0f),
                            end = Offset(xf + h / 2, h),
                            strokeWidth = 3f
                        )
                    }
                }
            }

            if (queenInError && state == 2) {
                drawRect(Color.Red, style = Stroke(width = 5f))
            }

            drawLine(Color.Black, Offset(w, 0f), Offset(w, h), strokeWidth = if (thickRight) 6f else 2f)
            drawLine(Color.Black, Offset(0f, h), Offset(w, h), strokeWidth = if (thickBottom) 6f else 2f)

            if (!queenInError || state != 2) {
                drawRect(Color.Black, style = Stroke(1.5f))
            }

            if (pressProgress.value > 0f) {
                val strokeW = w * 0.07f
                drawArc(
                    color = Color(0xFFFFA000),
                    startAngle = -90f,
                    sweepAngle = 360f * pressProgress.value,
                    useCenter = false,
                    topLeft = Offset(strokeW / 2, strokeW / 2),
                    size = Size(w - strokeW, h - strokeW),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }

            if (isHighlighted) {
                val strokeW = w * 0.08f * highlightScale
                val inset = strokeW / 2f + 2f
                drawRect(
                    color = Color(0xFF2196F3),
                    topLeft = Offset(inset, inset),
                    size = Size((w - inset * 2).coerceAtLeast(1f), (h - inset * 2).coerceAtLeast(1f)),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }
        }

        if (errorFlash) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Red.copy(alpha = 0.35f))
            )
            Image(
                painter = painterResource(R.drawable.boom_wrong),
                contentDescription = "Sad BOOM",
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.colorMatrix(
                    ColorMatrix().apply { setToSaturation(0.22f) }
                ),
                modifier = Modifier
                    .fillMaxSize(0.88f)
                    .graphicsLayer {
                        rotationZ = errorShake.value * 1.4f
                        translationY = size.height * 0.10f
                        alpha = 0.88f
                    }
            )
        }

        when (state) {
            1 -> Text(
                "X",
                fontSize = (cellSize.value * 0.65f).sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.scale(xScale.value)
            )
            3 -> Text(
                "X",
                fontSize = (cellSize.value * 0.65f).sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD32F2F),
                modifier = Modifier.scale(xScale.value)
            )
            2 -> Image(
                painter = painterResource(
                    if (showWinkFrame) R.drawable.boom_wink
                    else R.drawable.boom_happy
                ),
                contentDescription = "BOOM",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize(0.92f)
                    .graphicsLayer {
                        scaleX = boomScale.value * successPulse.value
                        scaleY = boomScale.value * successPulse.value
                        translationY = -successLift.value * size.height * 0.26f
                        rotationZ = successTilt.value
                    }
                    .alpha(boomAlpha.value)
            )
        }

        if (showGuidePointer && state != 2) {
            Image(
                painter = painterResource(R.drawable.point),
                contentDescription = "Place BOOM here",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxSize(0.52f)
                    .offset(y = (4f * highlightScale).dp)
                    .alpha(0.88f)
            )
        }

        if (showBoomParticles) ParticleBurst(color = Color(0xFFFF7043))
        if (showSuccessParticles) ParticleBurst(color = Color(0xFF43A047), particleCount = 10)
    }
}

/** Runs [block] once whenever [event] newly targets ([row], [col]); a no-op for every other cell. */
@Composable
private fun LaunchedEffectSafe(event: CellEvent?, row: Int, col: Int, block: suspend CoroutineScope.() -> Unit) {
    LaunchedEffect(event?.id) {
        if (event != null && event.row == row && event.col == col) {
            block()
        }
    }
}

@Composable
private fun BoxScope.ParticleBurst(color: Color, particleCount: Int = 8) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(380, easing = LinearOutSlowInEasing))
    }
    Canvas(modifier = Modifier.matchParentSize()) {
        val p = progress.value
        val radius = size.minDimension / 2f * p
        val center = Offset(size.width / 2f, size.height / 2f)
        val alpha = (1f - p).coerceIn(0f, 1f)
        for (i in 0 until particleCount) {
            val angle = (2 * Math.PI * i / particleCount).toFloat()
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = size.minDimension * 0.06f,
                center = Offset(center.x + radius * cos(angle), center.y + radius * sin(angle))
            )
        }
    }
}
