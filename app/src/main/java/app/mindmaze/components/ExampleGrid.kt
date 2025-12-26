package app.mindmaze.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun ExampleGrid(
    rows: Int = 5,
    cols: Int = 6,
    initialQueens: List<Pair<Int, Int>> = emptyList(),
    initialXs: List<Pair<Int, Int>> = emptyList(),
    colorMatrix: Array<IntArray> = Array(rows) { IntArray(cols) { 0 } },
    violatedCells: Set<Pair<Int, Int>> = emptySet(),
    violatedQueens: Set<Pair<Int, Int>> = emptySet(),
    violatedDiagonal: Set<Pair<Int, Int>> = emptySet(),
    violatedRegionQueens: Set<Pair<Int, Int>> = emptySet(),
    message: String,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val availableWidth = (screenWidth * 0.85f) - 32.dp
    val cellSize = (availableWidth / cols).coerceAtMost(50.dp)

    val fontSize = (cellSize.value * 0.45f).sp
    val queenSize = (cellSize.value * 0.5f).sp

    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in 0 until rows) {
            Row {
                for (col in 0 until cols) {
                    val bgColor = ExampleColors[colorMatrix[row][col]]
                    val isQueen = initialQueens.contains(row to col)
                    val isX = initialXs.contains(row to col)
                    val isViolatedCell = violatedCells.contains(row to col)
                    val isViolatedQueen = violatedQueens.contains(row to col)
                    val isViolatedDiagonalQueen = violatedDiagonal.contains(row to col)
                    val isViolatedRegionQ = violatedRegionQueens.contains(row to col)

                    Box(
                        modifier = Modifier.size(cellSize),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val w = size.width
                            val h = size.height

                            drawRect(color = bgColor, size = size)

                            if (isViolatedCell) {
                                val lineCount = 5
                                val strokeWidth = 2.5f
                                val spacing = w / (lineCount + 1)
                                clipRect {
                                    for (i in 1..lineCount) {
                                        val xf = spacing * i
                                        drawLine(
                                            color = Color.Red.copy(alpha = 0.5f),
                                            start = Offset(xf - h / 2, 0f),
                                            end = Offset(xf + h / 2, h),
                                            strokeWidth = strokeWidth
                                        )
                                    }
                                }
                            }

                            val strokeW =
                                if (col < cols - 1 && colorMatrix[row][col] != colorMatrix[row][col + 1]) 3f else 1f
                            val strokeH =
                                if (row < rows - 1 && colorMatrix[row][col] != colorMatrix[row + 1][col]) 3f else 1f

                            drawLine(
                                color = Color.Black,
                                start = Offset(w, 0f),
                                end = Offset(w, h),
                                strokeWidth = strokeW
                            )
                            drawLine(
                                color = Color.Black,
                                start = Offset(0f, h),
                                end = Offset(w, h),
                                strokeWidth = strokeH
                            )
                            drawRect(color = Color.Black, style = Stroke(width = 1f))
                        }

                        if (isX) {
                            Text(
                                "X",
                                fontSize = fontSize,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        } else if (isQueen) {
                            Text(
                                "💣",
                                fontSize = queenSize,
                                fontWeight = FontWeight.Bold,
                                color = if (isViolatedQueen || isViolatedDiagonalQueen || isViolatedRegionQ)
                                    Color.Red else Color.Black
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = message,
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}