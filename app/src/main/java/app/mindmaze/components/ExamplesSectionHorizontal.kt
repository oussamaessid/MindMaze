package app.mindmaze.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun ExamplesSectionHorizontal() {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val exampleWidth = screenWidth * 0.85f

    val listState = rememberLazyListState()
    val totalItems = 4

    val currentPage by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.width / 2

            layoutInfo.visibleItemsInfo
                .minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }
                ?.index ?: 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Text(
            "Examples",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            item {
                ExampleGrid(
                    initialQueens = listOf(0 to 0),
                    initialXs = listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5),
                    colorMatrix = ExampleColorMatrix,
                    message = "Each row can only have one 💣",
                    modifier = Modifier.width(exampleWidth)
                )
            }
            item {
                ExampleGrid(
                    initialQueens = listOf(0 to 0),
                    initialXs = listOf(1 to 0, 2 to 0, 3 to 0, 4 to 0),
                    colorMatrix = ExampleColorMatrix,
                    message = "Each column can also only have one 💣",
                    modifier = Modifier.width(exampleWidth)
                )
            }
            item {
                ExampleGrid(
                    initialQueens = listOf(2 to 5, 3 to 2),
                    colorMatrix = ExampleColorMatrix,
                    violatedCells = listOf(
                        2 to 2, 2 to 3, 2 to 4, 2 to 5,
                        3 to 2, 3 to 3, 3 to 4, 3 to 5,
                        4 to 2, 4 to 3, 4 to 4, 4 to 5
                    ).toSet(),
                    violatedRegionQueens = setOf(2 to 5, 3 to 2),
                    message = "Each color region can also only have one 💣",
                    modifier = Modifier.width(exampleWidth)
                )
            }
            item {
                ExampleGrid(
                    initialQueens = listOf(1 to 5, 2 to 4),
                    colorMatrix = ExampleColorMatrix,
                    violatedQueens = setOf(1 to 5, 2 to 4),
                    violatedCells = setOf(1 to 5, 2 to 4),
                    violatedDiagonal = setOf(1 to 5, 2 to 4),
                    message = "Two 💣 cannot touch each other, not even diagonally",
                    modifier = Modifier.width(exampleWidth)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalItems) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == currentPage) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == currentPage)
                                Color(0xFF4CAF50)
                            else
                                Color.Gray.copy(alpha = 0.4f)
                        )
                )
                if (index < totalItems - 1) {
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}

val ExampleColorMatrix = arrayOf(
    intArrayOf(4, 0, 0, 1, 1, 1),
    intArrayOf(0, 0, 0, 1, 1, 1),
    intArrayOf(2, 2, 3, 3, 3, 3),
    intArrayOf(2, 2, 3, 3, 3, 3),
    intArrayOf(2, 2, 3, 3, 3, 3)
)

val ExampleColors = listOf(
    Color(0xFFE8C1FF),
    Color(0xFFFFD4A3),
    Color(0xFFB4E7B4),
    Color(0xFFADD8E6),
    Color(0xFFFFB3BA)
)