package app.mindmaze.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.mindmaze.data.model.PuzzleLevel
import app.mindmaze.data.repositoryImp.PuzzleLevels
import kotlin.math.abs

@Composable
fun PuzzleGame(
    level: PuzzleLevel,
    boardState: List<List<Int>>,
    onCellToggle: (Int, Int) -> Unit,
    isLoading: Boolean = false
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
    val cellSize = ((configuration.screenWidthDp.dp - 32.dp) / boardSize).coerceAtMost(60.dp)

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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Grille
        repeat(boardSize) { row ->
            Row {
                repeat(boardSize) { col ->
                    val bgColor = colors[matrix[row][col]]
                    val state = boardState[row][col]
                    val hasViolation = violatedCells.contains(row to col)
                    val queenInError = adjacentErrors.contains(row to col)

                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .clickable { onCellToggle(row, col) },
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

                            val thickRight = col < boardSize - 1 && matrix[row][col] != matrix[row][col + 1]
                            val thickBottom = row < boardSize - 1 && matrix[row][col] != matrix[row + 1][col]

                            drawLine(Color.Black, Offset(w, 0f), Offset(w, h), strokeWidth = if (thickRight) 6f else 2f)
                            drawLine(Color.Black, Offset(0f, h), Offset(w, h), strokeWidth = if (thickBottom) 6f else 2f)

                            if (!queenInError || state != 2) {
                                drawRect(Color.Black, style = Stroke(1.5f))
                            }
                        }

                        when (state) {
                            1 -> Text(
                                "X",
                                fontSize = (cellSize.value * 0.65f).sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            2 -> Text(
                                "\uD83D\uDCA3",
                                fontSize = (cellSize.value * 0.8f).sp,
                                color = if (queenInError) Color.Red else Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        val placed = queens.size
        Text(
            text = "$placed / $boardSize Bomb",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = if (violatedCells.isNotEmpty()) Color.Red else Color.Black
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Tap → X  •  Double tap → \uD83D\uDCA3  • ",
            fontSize = 15.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
    }
}