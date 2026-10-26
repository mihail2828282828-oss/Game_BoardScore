package com.example.boardgamescore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Главное окно приложения.
 *
 * Сначала показывает заставку с ФИО разработчика,
 * затем открывает существующее приложение ScoreApp.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val storage = remember {
                Storage(applicationContext)
            }

            val systemDark = isSystemInDarkTheme()

            val useDark = storage.style == 3 || when (storage.theme) {
                "light" -> false
                "dark", "amoled" -> true
                else -> systemDark
            }

            val useAmoled = storage.theme == "amoled" || storage.style == 3

            val colors = if (useDark) {
                darkColorScheme(
                    primary = Color(0xFFC6A8FF),
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
                    primary = Color(0xFF7040B0),
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

/**
 * Переключение между заставкой и основным приложением.
 */
@Composable
private fun AppRouter(storage: Storage) {
    var showSplash by rememberSaveable {
        mutableStateOf(true)
    }

    val motion = storage.animations

    AnimatedContent(
        targetState = showSplash,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            fadeIn(
                animationSpec = tween(
                    durationMillis = if (motion) 350 else 0
                )
            ) togetherWith fadeOut(
                animationSpec = tween(
                    durationMillis = if (motion) 200 else 0
                )
            )
        },
        label = "splash_to_main"
    ) { splashVisible ->
        if (splashVisible) {
            SplashScreen(
                onFinished = {
                    showSplash = false
                }
            )
        } else {
            ScoreApp(storage = storage)
        }
    }
}

/**
 * Начальная заставка.
 *
 * Показывается 2,5 секунды.
 * Индикатор декоративный и не отражает реальный прогресс загрузки.
 */
@Composable
internal fun SplashScreen(onFinished: () -> Unit) {
    val currentOnFinished by rememberUpdatedState(onFinished)
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        delay(2500L)
        currentOnFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colors.primary.copy(alpha = 0.18f),
                        colors.background,
                        colors.background
                    )
                )
            )
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🎲",
                fontSize = 64.sp
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = "SCORE CLUB",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = colors.primary,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "Настольные игры • Очки • Турниры",
                fontSize = 14.sp,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(36.dp)
            )

            CircularProgressIndicator(
                color = colors.primary,
                strokeWidth = 3.dp
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                text = "Подготовка игрового стола…",
                fontSize = 13.sp,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(40.dp)
            )

            Text(
                text = "Разработал Венедиктов М. Р.",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onBackground,
                textAlign = TextAlign.Center
            )
        }
    }
}
