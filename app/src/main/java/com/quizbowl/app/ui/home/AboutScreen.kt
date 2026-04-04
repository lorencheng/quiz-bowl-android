package com.quizbowl.app.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.quizbowl.app.BuildConfig
import com.quizbowl.app.ui.theme.qbColors

private const val QBREADER_URL = "https://www.qbreader.org"

@Composable
fun AboutScreen(navController: NavController) {
    val qbColors = MaterialTheme.qbColors
    val context = LocalContext.current

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
                    text = "ABOUT",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp,
                    color = qbColors.primary,
                )
                // Spacer to balance the back button and keep title centered
                Box(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = qbColors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // App name + version
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "QuizBowl TTS",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            fontSize = 13.sp,
                            color = qbColors.textMuted,
                        )
                    }

                    HorizontalDivider(color = qbColors.border)

                    // QBReader attribution
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Questions powered by",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            color = qbColors.textMuted,
                        )
                        Row(
                            modifier = Modifier
                                .clickable { openUrl(context, QBREADER_URL) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "QBReader.org",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = qbColors.primary,
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open QBReader.org",
                                tint = qbColors.primary,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Text(
                            text = "Free, open-source question database for competitive quiz bowl.",
                            fontSize = 13.sp,
                            color = qbColors.textMuted,
                            lineHeight = 20.sp,
                        )
                        Text(
                            text = "Used under MIT License",
                            fontSize = 11.sp,
                            color = qbColors.textMuted.copy(alpha = 0.6f),
                        )
                    }

                    HorizontalDivider(color = qbColors.border)

                    // App description
                    Text(
                        text = "QuizBowl TTS is a practice app for competitive quiz bowl players. " +
                            "Questions are read aloud word-by-word using text-to-speech, and " +
                            "answers can be spoken or typed.",
                        fontSize = 14.sp,
                        color = qbColors.textMuted,
                        lineHeight = 22.sp,
                    )
                }
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
