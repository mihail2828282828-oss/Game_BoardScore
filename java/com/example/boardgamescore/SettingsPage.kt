package com.example.boardgamescore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SettingsPage(
    theme: String,
    onTheme: (String) -> Unit,
    accent: Int,
    onAccent: (Int) -> Unit,
    style: Int,
    onStyle: (Int) -> Unit,
    english: Boolean,
    onEnglish: (Boolean) -> Unit,
    motion: Boolean,
    onMotion: (Boolean) -> Unit,
    sound: Boolean,
    onSound: (Boolean) -> Unit,
    historyCount: Int,
    onClear: () -> Unit,
    tourCount: Int
) {
    Page {
        Heading(
            title = tr("Настройки", "Settings"),
            subtitle = tr(
                "Ваш стиль. Ваша игра.",
                "Your style. Your game."
            )
        )

        GlowPanel(highlighted = true) {
            Text(
                text = tr("Язык интерфейса", "Interface language"),
                fontWeight = FontWeight.Bold
            )

            Choice(
                text = "Русский",
                selected = !english,
                onClick = { onEnglish(false) }
            )

            Choice(
                text = "English",
                selected = english,
                onClick = { onEnglish(true) }
            )
        }

        GlowPanel {
            Text(
                text = tr("Тема", "Theme"),
                fontWeight = FontWeight.Bold
            )

            Choice(
                text = tr("Как на устройстве", "System"),
                selected = theme == "system",
                onClick = { onTheme("system") }
            )

            Choice(
                text = tr("Светлая", "Light"),
                selected = theme == "light",
                onClick = { onTheme("light") }
            )

            Choice(
                text = tr("Тёмная", "Dark"),
                selected = theme == "dark",
                onClick = { onTheme("dark") }
            )

            Choice(
                text = "AMOLED",
                selected = theme == "amoled",
                onClick = { onTheme("amoled") }
            )
        }

        GlowPanel {
            Text(
                text = tr("Стиль фона", "Background style"),
                fontWeight = FontWeight.Bold
            )

            Choice(
                text = tr("Стандарт", "Standard"),
                selected = style == 0,
                onClick = { onStyle(0) }
            )

            Choice(
                text = tr("Закат", "Sunset"),
                selected = style == 1,
                onClick = { onStyle(1) }
            )

            Choice(
                text = tr("Лес", "Forest"),
                selected = style == 2,
                onClick = { onStyle(2) }
            )

            Choice(
                text = "AMOLED",
                selected = style == 3,
                onClick = { onStyle(3) }
            )

            Text(
                text = tr(
                    "AMOLED использует тёмную палитру и чёрный фон. " +
                        "Для градиентного фона выберите другую тему и другой стиль.",
                    "AMOLED uses a dark palette and a black background. " +
                        "For a gradient background, select another theme and style."
                ),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        GlowPanel {
            Text(
                text = tr("Акцентный цвет", "Accent color"),
                fontWeight = FontWeight.Bold
            )

            Choice(
                text = tr("Аметист", "Amethyst"),
                selected = accent == 0,
                onClick = { onAccent(0) }
            )

            Choice(
                text = tr("Океан", "Ocean"),
                selected = accent == 1,
                onClick = { onAccent(1) }
            )

            Choice(
                text = tr("Изумруд", "Emerald"),
                selected = accent == 2,
                onClick = { onAccent(2) }
            )
        }

        GlowPanel {
            SettingSwitch(
                title = tr("Анимации", "Animations"),
                description = tr(
                    "Переходы и изменение счёта.",
                    "Transitions and score changes."
                ),
                checked = motion,
                onCheckedChange = onMotion
            )

            SettingSwitch(
                title = tr("Звук победы", "Victory sound"),
                description = tr(
                    "Фанфара и вибрация после завершения партии. " +
                        "Вибрация таймера работает отдельно.",
                    "Fanfare and vibration when a game finishes. " +
                        "Timer vibration works independently."
                ),
                checked = sound,
                onCheckedChange = onSound
            )
        }

        GlowPanel {
            Text(
                text = tr("Данные", "Data"),
                fontWeight = FontWeight.Bold
            )

            Text(
                text = tr(
                    "Завершённых партий: $historyCount",
                    "Completed games: $historyCount"
                )
            )

            Text(
                text = tr(
                    "Турниров: $tourCount",
                    "Tournaments: $tourCount"
                )
            )

            Text(
                text = tr(
                    "Очистка истории удалит статистику партий, " +
                        "но не изменит турниры и их счёт.",
                    "Clearing history removes game statistics, " +
                        "but does not change tournaments or their wins."
                ),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = onClear,
                enabled = historyCount > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(tr("Очистить историю", "Clear history"))
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}