package com.quizbowl.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

    val animatedTotal by animateIntAsState(
        targetValue = score.total,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy,
        ),
        label = "scoreTotal",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Score — the hero number
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = animatedTotal.toString(),
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = qbColors.accentAmber,
                lineHeight = 30.sp,
            )
            Text(
                text = "pts",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = qbColors.accentAmber.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }

        // Stats — right side
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatColumn(value = score.correct.toString(), label = "correct", color = qbColors.green)
            StatColumn(value = score.neg.toString(), label = "neg", color = qbColors.red)
            StatColumn(value = score.questions.toString(), label = "played", color = qbColors.textMuted)
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = color.copy(alpha = 0.6f),
            letterSpacing = 0.5.sp,
        )
    }
}
