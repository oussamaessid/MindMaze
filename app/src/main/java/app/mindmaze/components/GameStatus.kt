package app.mindmaze.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameStatus(
    totalQueens: Int,
    requiredQueens: Int,
    hasViolations: Boolean,
    isComplete: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Afficher toujours le compteur
        Text(
            text = "Bombs placed: $totalQueens/$requiredQueens",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Afficher le statut
        Text(
            text = when {
                isComplete -> "✅ Perfect! All conditions met!"
                hasViolations -> "❌ Violations detected"
                totalQueens > requiredQueens -> "⚠️ Too many bombs!"
                else -> "Place all bombs following the rules"
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = when {
                isComplete -> Color(0xFF4CAF50)
                hasViolations || totalQueens > requiredQueens -> Color.Red
                else -> Color.Gray
            }
        )
    }
}