package com.quizbowl.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.quizbowl.app.navigation.Screen
import com.quizbowl.app.ui.theme.AccentAmber
import com.quizbowl.app.ui.theme.AccentRose
import com.quizbowl.app.ui.theme.AccentTeal
import androidx.compose.material3.MaterialTheme
import com.quizbowl.app.ui.theme.Primary
import com.quizbowl.app.ui.theme.qbColors

@Composable
fun HomeScreen(navController: NavController) {
    val colors = MaterialTheme.qbColors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "QuizBowl TTS",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Primary,
        )
        Text(
            text = "Quiz bowl practice with text-to-speech",
            fontSize = 14.sp,
            color = colors.textMuted,
        )

        Spacer(modifier = Modifier.height(8.dp))

        NavCard(
            title = "Tossup Practice",
            subtitle = "Practice tossups with questions read aloud",
            accentColor = AccentTeal,
            onClick = { navController.navigate(Screen.TossupPractice.route) },
        )
        NavCard(
            title = "Bonus Practice",
            subtitle = "Practice bonuses with questions read aloud",
            accentColor = AccentRose,
            onClick = { navController.navigate(Screen.BonusPractice.route) },
        )
        NavCard(
            title = "Multiplayer",
            subtitle = "Join existing qbreader rooms",
            accentColor = AccentAmber,
            onClick = { navController.navigate(Screen.Multiplayer.route) },
        )
    }
}

@Composable
private fun NavCard(
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.qbColors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(
            width = 1.dp,
            color = colors.border,
        ),
    ) {
        // Left accent bar
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(80.dp)
                    .padding(vertical = 0.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = accentColor,
                ) {}
            }
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = colors.textMuted,
                )
            }
        }
    }
}
