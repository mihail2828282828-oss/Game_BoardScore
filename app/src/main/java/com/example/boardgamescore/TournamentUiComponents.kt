package com.example.boardgamescore

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Заголовок раздела на экране турнира.
 */
@Composable
internal fun TournamentSectionTitle(
    text: String,
    accent: Boolean = false
) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = if (accent) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    )
}

/**
 * Пояснение или сообщение об ошибке на экране турнира.
 */
@Composable
internal fun TournamentHint(
    text: String,
    error: Boolean = false
) {
    Text(
        text = text,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        color = if (error) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
}
