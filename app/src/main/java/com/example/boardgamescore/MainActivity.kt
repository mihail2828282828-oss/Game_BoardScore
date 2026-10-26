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

            val darkMode = when (storage.theme) {
                "light" -> false
                "dark", "amoled" -> true
                else -> isSystemInDarkTheme()
            }

            val colors = if (darkMode) {
                darkColorScheme(
                    primary = Color(0xFFC6A8FF),
                    secondary = Color(0xFF7ADBCB),
                    background = if (storage.theme == "amoled") {
                        Color.Black
                    } else {
                        Color(0xFF101019)
                    },
                    surface = if (storage.theme == "amoled") {
                        Color(0xFF08080B)
                    } else {
                        Color(0xFF1B1B29)
                    },
                    onBackground = Color(0xFFF5F1FF),
                    onSurface = Color(0xFFF5F1FF)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF7040B0),
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
    val splashState = rememberSaveable {
        androidx.compose.runtime.mutableStateOf(true)
    }

    if (splashState.value) {
        SplashScreen(
            onFinished = {
                splashState.value = false
            }
        )
    } else {
        ScoreApp(storage = storage)
    }
}
