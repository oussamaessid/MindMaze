package app.mindmaze.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.mindmaze.R
import app.mindmaze.lives.LivesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "How to Play",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HelpSection(icon = "🎯", title = "Objective", color = Color(0xFF3B82F6)) {
                Text(
                    text = "Place exactly one bomb (💣) in each row, column, and colored region of the board.",
                    fontSize = 15.sp,
                    color = Color(0xFF374151),
                    lineHeight = 22.sp
                )
            }

            HelpSection(icon = "👆", title = "Controls", color = Color(0xFF10B981)) {
                TapRuleRow(tap = "1 tap", result = "Mark a forbidden cell  ✕")
                Spacer(Modifier.height(6.dp))
                TapRuleRow(tap = "2 taps", result = "Place a bomb  💣")
                Spacer(Modifier.height(6.dp))
                TapRuleRow(tap = "3 taps", result = "Clear the cell")
            }

            HelpSection(icon = "📋", title = "Game Rules", color = Color(0xFFF59E0B)) {
                RuleRow("1", "One bomb per row", "Each row must contain exactly one bomb.", "↔️")
                Spacer(Modifier.height(12.dp))
                RuleRow("2", "One bomb per column", "Each column must contain exactly one bomb.", "↕️")
                Spacer(Modifier.height(12.dp))
                RuleRow("3", "One bomb per region", "Each colored area must contain exactly one bomb.", "🎨")
                Spacer(Modifier.height(12.dp))
                RuleRow("4", "No touching", "Bombs cannot touch each other, not even diagonally.", "❌")
            }

            HelpSection(icon = "⚠️", title = "Violations", color = Color(0xFFEF4444)) {
                Text(
                    text = "When you place a bomb that breaks a rule:",
                    fontSize = 14.sp,
                    color = Color(0xFF374151),
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(8.dp))
                BulletPoint("A broken heart animation plays 💔")
                BulletPoint("The violated rule is displayed")
                BulletPoint("You lose 1 life")
                BulletPoint("You stay on the same level")
            }

            HelpSection(icon = "❤️", title = "Lives System", color = Color(0xFFEC4899)) {
                Text(
                    text = "You have ${LivesManager.MAX_LIVES} lives maximum.",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151)
                )
                Spacer(Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(LivesManager.MAX_LIVES) { i ->
                        val alive = i < 3
                        Icon(
                            painter = painterResource(if (alive) R.drawable.ic_heart else R.drawable.ic_heart1),
                            contentDescription = null,
                            tint = if (alive) Color(0xFFEF4444) else Color(0xFFD1D5DB),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text("= 3 lives", fontSize = 13.sp, color = Color(0xFF6B7280))
                }

                Spacer(Modifier.height(10.dp))
                BulletPoint("Filled hearts ❤️ = available lives")
                BulletPoint("Empty hearts 🤍 = lives recovering")
            }

            HelpSection(icon = "⏱️", title = "Life Recovery", color = Color(0xFF8B5CF6)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3E8FF))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "1h",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C3AED),
                            modifier = Modifier.width(60.dp),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Each lost life is automatically restored after 1 hour.",
                            fontSize = 14.sp,
                            color = Color(0xFF374151),
                            lineHeight = 20.sp
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                BulletPoint("Timer runs even when the app is closed")
                BulletPoint("Maximum ${LivesManager.MAX_LIVES} lives at a time")
                BulletPoint("Watch a video ad to instantly get +1 life")
            }

            HelpSection(icon = "💀", title = "Game Over", color = Color(0xFF6B7280)) {
                Text(text = "When you run out of lives:", fontSize = 14.sp, color = Color(0xFF374151))
                Spacer(Modifier.height(8.dp))
                BulletPoint("The Game Over screen appears with a countdown")
                BulletPoint("Watch a video ad to immediately get +1 life")
                BulletPoint("Or wait — lives restore automatically")
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF0FDF4))
                    .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text("💡 Pro Tip", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Use ✕ marks to eliminate impossible positions. " +
                                "By ruling out where bombs can't go, you'll find the correct placements faster!",
                        fontSize = 14.sp,
                        color = Color(0xFF166534),
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HelpSection(
    icon: String,
    title: String,
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8F9FA))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun TapRuleRow(tap: String, result: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFDCFCE7))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(tap, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
        }
        Spacer(Modifier.width(12.dp))
        Text(result, fontSize = 14.sp, color = Color(0xFF374151))
    }
}

@Composable
private fun RuleRow(number: String, title: String, description: String, icon: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFFEF3C7)),
            contentAlignment = Alignment.Center
        ) {
            Text(number, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
                Spacer(Modifier.width(6.dp))
                Text(icon, fontSize = 16.sp)
            }
            Spacer(Modifier.height(2.dp))
            Text(description, fontSize = 13.sp, color = Color(0xFF6B7280), lineHeight = 18.sp)
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("• ", fontSize = 14.sp, color = Color(0xFF6B7280))
        Text(text, fontSize = 14.sp, color = Color(0xFF374151), lineHeight = 20.sp)
    }
}
