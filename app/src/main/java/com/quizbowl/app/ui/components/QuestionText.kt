package com.quizbowl.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.quizbowl.app.ui.theme.qbColors
import com.quizbowl.app.ui.tossup.TossupPhase

/**
 * Renders question text word-by-word, revealing words as TTS reads them.
 *
 * Visibility rules (matching web app):
 *  - RESULT : all words visible
 *  - ttsDone : all words visible (buzz countdown active, waiting for player)
 *  - BUZZING : words 0..buzzIndex visible
 *  - READING : words 0..wordIndex visible; current word (wordIndex) highlighted
 *  - IDLE    : nothing visible
 */
@Composable
fun QuestionText(
    words: List<String>,
    wordIndex: Int,
    buzzIndex: Int,
    phase: TossupPhase,
    ttsDone: Boolean,
    modifier: Modifier = Modifier,
) {
    val qbColors = MaterialTheme.qbColors

    val visibleUpTo = when {
        phase == TossupPhase.RESULT -> words.size - 1
        ttsDone -> words.size - 1
        phase == TossupPhase.BUZZING -> buzzIndex
        phase == TossupPhase.READING -> wordIndex
        else -> -1
    }

    val annotated = buildAnnotatedString {
        words.forEachIndexed { i, word ->
            if (i <= visibleUpTo) {
                val isHighlighted = i == wordIndex && phase == TossupPhase.READING && !ttsDone
                if (isHighlighted) {
                    withStyle(
                        SpanStyle(
                            background = qbColors.primaryGlow,
                            color = qbColors.primary,
                        )
                    ) {
                        append(word)
                    }
                } else {
                    append(word)
                }
                append(' ')
            }
        }
    }

    Text(
        text = annotated,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        textAlign = TextAlign.Start,
        modifier = modifier,
    )
}
