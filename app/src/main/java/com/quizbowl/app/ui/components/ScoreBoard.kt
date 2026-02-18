package com.quizbowl.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.quizbowl.app.ui.theme.qbColors
import com.quizbowl.app.util.TossupScore

@Composable
fun ScoreBoard(
    score: TossupScore,
    modifier: Modifier = Modifier,
) {
    val qbColors = MaterialTheme.qbColors

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = qbColors.surface),
        border = BorderStroke(1.dp, qbColors.border),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScoreItem(label = "Score", value = score.total.toString(), highlight = true)
            ScoreItem(label = "Correct", value = score.correct.toString())
            ScoreItem(label = "Negs", value = score.neg.toString())
            ScoreItem(label = "Questions", value = score.questions.toString())
        }
    }
}

@Composable
private fun ScoreItem(
    label: String,
    value: String,
    highlight: Boolean = false,
) {
    val qbColors = MaterialTheme.qbColors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            fontSize = if (highlight) 20.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) qbColors.primary else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = qbColors.textMuted,
        )
    }
}
