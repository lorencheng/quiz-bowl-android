package com.quizbowl.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizbowl.app.ui.theme.qbColors
import com.quizbowl.app.util.TossupScore

@Composable
fun ScoreBoard(
    score: TossupScore,
    modifier: Modifier = Modifier,
) {
    val qbColors = MaterialTheme.qbColors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Score — prominent, amber
        Text(
            text = "${score.total} pts",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = qbColors.accentAmber,
        )

        // Spacer pushes the rest to the right
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScorePill(label = "✓", value = score.correct.toString(), color = qbColors.green)
            Text("  ", fontSize = 12.sp)
            ScorePill(label = "✗", value = score.neg.toString(), color = qbColors.red)
            Text("  ", fontSize = 12.sp)
            ScorePill(label = "Q", value = score.questions.toString(), color = qbColors.textMuted)
        }
    }
}

@Composable
private fun ScorePill(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}
