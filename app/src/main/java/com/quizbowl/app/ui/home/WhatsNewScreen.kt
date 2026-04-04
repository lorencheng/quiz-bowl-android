package com.quizbowl.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.quizbowl.app.BuildConfig
import com.quizbowl.app.ui.theme.qbColors

private data class ChangelogEntry(
    val version: String,
    val date: String,
    val bullets: List<String>,
)

private val CHANGELOG = listOf(
    ChangelogEntry(
        version = "v1.0",
        date = "April 2026",
        bullets = listOf(
            "Tossup practice with word-by-word TTS",
            "Bonus practice with 3-part questions",
            "Voice answer recognition — speak your answer hands-free",
            "Answer timer with animated countdown",
            "Settings: TTS speed, buzz timer, answer timer, categories, difficulty",
            "Dark game-show aesthetic throughout",
        ),
    ),
)

@Composable
fun WhatsNewScreen(navController: NavController) {
    val qbColors = MaterialTheme.qbColors

    Scaffold(containerColor = qbColors.bg) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = qbColors.textMuted,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = "WHAT'S NEW",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp,
                    color = qbColors.primary,
                )
                // Spacer to balance the back button and keep title centered
                Box(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Current version badge
            Text(
                text = "Current version: v${BuildConfig.VERSION_NAME}",
                fontSize = 12.sp,
                color = qbColors.textMuted,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CHANGELOG.forEach { entry ->
                    ChangelogCard(entry = entry)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ChangelogCard(entry: ChangelogEntry) {
    val qbColors = MaterialTheme.qbColors

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = qbColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Version + date row
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.version,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = qbColors.primary,
                )
                Text(
                    text = "·",
                    fontSize = 12.sp,
                    color = qbColors.textMuted,
                )
                Text(
                    text = entry.date,
                    fontSize = 12.sp,
                    color = qbColors.textMuted,
                )
            }

            // Bullet items
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                entry.bullets.forEach { bullet ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "•",
                            fontSize = 14.sp,
                            color = qbColors.primary,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                        Text(
                            text = bullet,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp,
                        )
                    }
                }
            }
        }
    }
}
