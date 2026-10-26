package com.example.boardgamescore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val storage = remember {
                Storage(applicationContext)
            }

            val systemDark = isSystemInDarkTheme()

            val useAmoled =
                storage.theme == "amoled" || storage.style == 3

            val useDark = useAmoled || when (storage.theme) {
                "light" -> false
                "dark" -> true
                else -> systemDark
            }

            val accentIndex = storage.accent.coerceIn(0, 2)

            val darkAccents = listOf(
                Color(0xFFC6A8FF),
                Color(0xFF8ECDFF),
                Color(0xFF88E3BF)
            )

            val lightAccents = listOf(
                Color(0xFF7040B0),
                Color(0xFF175DA6),
                Color(0xFF176B4C)
            )

            val colors = if (useDark) {
                darkColorScheme(
                    primary = darkAccents[accentIndex],
                    secondary = Color(0xFF7ADBCB),
                    background = if (useAmoled) {
                        Color.Black
                    } else {
                        Color(0xFF101019)
                    },
                    surface = if (useAmoled) {
                        Color(0xFF0B0B0F)
                    } else {
                        Color(0xFF1B1B29)
                    },
                    onBackground = Color(0xFFF5F1FF),
                    onSurface = Color(0xFFF5F1FF)
                )
            } else {
                lightColorScheme(
                    primary = lightAccents[accentIndex],
                    secondary = Color(0xFF167D80),
                    background = Color(0xFFF5F3FC),
                    surface = Color.White,
                    onBackground = Color(0xFF231D30),
                    onSurface = Color(0xFF231D30)
                )
            }

            MaterialTheme(colorScheme = colors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRouter(storage = storage)
                }
            }
        }
    }
}

@Composable
private fun AppRouter(storage: Storage) {
    val showSplash = rememberSaveable {
        mutableStateOf(true)
    }

    if (showSplash.value) {
        SplashScreen(
            onFinished = {
                showSplash.value = false
            }
        )
    } else {
        ScoreApp(storage = storage)
    }
}
