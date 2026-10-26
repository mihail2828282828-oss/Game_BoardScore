package com.example.boardgamescore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalEnglish = staticCompositionLocalOf {
    false
}

internal val LocalMotion = staticCompositionLocalOf {
    true
}

@Composable
internal fun tr(
    ru: String,
    en: String
): String {
    return if (LocalEnglish.current) en else ru
}