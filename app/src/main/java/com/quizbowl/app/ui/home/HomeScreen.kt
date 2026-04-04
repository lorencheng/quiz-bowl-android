package com.quizbowl.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.quizbowl.app.navigation.Screen
import com.quizbowl.app.ui.theme.AccentAmber // re-enable with Multiplayer card
import com.quizbowl.app.ui.theme.AccentRose
import com.quizbowl.app.ui.theme.AccentTeal
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
        // Branded header — icon + title/subtitle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(40.dp),
            )
            Column {
                Text(
                    text = "QuizBowl",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Primary,
                )
                Text(
                    text = "Text-to-speech practice",
                    fontSize = 13.sp,
                    color = colors.textMuted,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        NavCard(
            title = "Tossup Practice",
            subtitle = "Practice tossups with questions read aloud",
            accentColor = AccentTeal,
            icon = Icons.Filled.Bolt,
            onClick = { navController.navigate(Screen.TossupPractice.route) },
        )
        NavCard(
            title = "Bonus Practice",
            subtitle = "Practice bonuses with questions read aloud",
            accentColor = AccentRose,
            icon = Icons.Filled.GridView,
            onClick = { navController.navigate(Screen.BonusPractice.route) },
        )
        // Multiplayer hidden — re-enable when ready:
        // NavCard(
        //     title = "Multiplayer",
        //     subtitle = "Join existing qbreader rooms",
        //     accentColor = AccentAmber,
        //     icon = Icons.Filled.Groups,
        //     onClick = { navController.navigate(Screen.Multiplayer.route) },
        // )
    }
}

@Composable
private fun NavCard(
    title: String,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(80.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = accentColor,
                ) {}
            }

            // Mode icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(28.dp),
            )

            // Title + subtitle
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = colors.textMuted,
                )
            }
        }
    }
}
