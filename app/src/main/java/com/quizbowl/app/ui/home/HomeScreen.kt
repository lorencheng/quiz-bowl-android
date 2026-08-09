package com.quizbowl.app.ui.home

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.play.core.review.ReviewManagerFactory
import com.quizbowl.app.R
import com.quizbowl.app.data.LocalFeedbackManager
import com.quizbowl.app.data.RotatingTextRepository
import com.quizbowl.app.navigation.Screen
import com.quizbowl.app.ui.theme.AccentAmber // re-enable with Multiplayer card
import com.quizbowl.app.ui.theme.AccentRose
import com.quizbowl.app.ui.theme.AccentTeal
import com.quizbowl.app.ui.theme.Primary
import com.quizbowl.app.ui.theme.qbColors

private const val PLAY_STORE_APP_ID = "com.quizbowl.app"         // TODO: replace with real app ID once published

@Composable
fun HomeScreen(navController: NavController) {
    val colors = MaterialTheme.qbColors
    val context = LocalContext.current
    val feedbackManager = LocalFeedbackManager.current
    var menuExpanded by remember { mutableStateOf(false) }
    var tossupSubtitle by remember { mutableStateOf("") }
    var bonusSubtitle by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        tossupSubtitle = RotatingTextRepository.nextTossupSubtitle(context)
        bonusSubtitle = RotatingTextRepository.nextBonusSubtitle(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Branded header — brand on left, overflow menu on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AppIcon(size = 44)
                Column {
                    Text(
                        text = "PowerMark",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Primary,
                    )
                    Text(
                        text = "Buzz faster. Score higher.",
                        fontSize = 13.sp,
                        color = colors.textMuted,
                    )
                }
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        tint = colors.textMuted,
                        modifier = Modifier.size(22.dp),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("What's New") },
                        leadingIcon = { Icon(Icons.Filled.NewReleases, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            navController.navigate(Screen.WhatsNew.route)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Rate this App") },
                        leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            triggerInAppReview(context)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Send Feedback") },
                        leadingIcon = { Icon(Icons.Filled.Feedback, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            feedbackManager.open()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("About") },
                        leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            navController.navigate(Screen.About.route)
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        NavCard(
            title = "Tossup Practice",
            subtitle = tossupSubtitle,
            accentColor = AccentTeal,
            icon = Icons.Filled.Bolt,
            onClick = { navController.navigate(Screen.TossupPractice.route) },
        )
        NavCard(
            title = "Bonus Practice",
            subtitle = bonusSubtitle,
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

// ─────────────────────────────────────────────────────────────────────────────
// App icon — the asterisk on its gradient background, used in the header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AppIcon(size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.22f).dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF9D8FFF), Color(0xFF4A52D4)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "PowerMark icon",
            modifier = Modifier.fillMaxSize(),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun triggerInAppReview(context: Context) {
    val activity = context as? Activity ?: return
    val manager = ReviewManagerFactory.create(context)
    manager.requestReviewFlow().addOnCompleteListener { task ->
        if (task.isSuccessful) {
            manager.launchReviewFlow(activity, task.result)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Nav card
// ─────────────────────────────────────────────────────────────────────────────

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
