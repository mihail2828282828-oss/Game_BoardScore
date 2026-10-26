package com.example.boardgamescore

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun BigScreen(
    name: String,
    players: List<String>,
    emojis: List<String>,
    scores: List<Long?>,
    finished: Boolean,
    rule: WinRule,
    teamMode: Boolean,
    teamAssign: List<Int>,
    teamNames: List<String>,
    onNormal: () -> Unit,
    onFinish: () -> Unit,
    onNew: () -> Unit
) {
    val english = LocalEnglish.current

    val individualValid =
        players.size in 2..8 &&
            scores.size == players.size &&
            scores.all { it != null }

    val teams = if (teamMode) {
        teamScores(
            scores = scores,
            assignments = teamAssign
        )
    } else {
        emptyList()
    }

    val valid =
        individualValid &&
            (
                !teamMode ||
                    (
                        teamNames.size == 2 &&
                            teams.size == 2 &&
                            teams.all { it != null }
                    )
                )

    val displayedNames = if (teamMode) {
        List(2) { index ->
            teamNames.getOrElse(index) {
                if (index == 0) "A" else "B"
            }
        }
    } else {
        players.mapIndexed { index, player ->
            "${emojis.getOrElse(index) { "" }} $player".trim()
        }
    }

    val displayedScores: List<Long?> = if (teamMode) {
        teams
    } else {
        scores
    }

    val leaders = if (valid) {
        winnerIndexes(
            scores = displayedScores.filterNotNull(),
            rule = rule
        )
    } else {
        emptyList()
    }

    Page {
        Heading(
            title = name,
            subtitle = if (finished) {
                tr("Итоговое табло", "Final scoreboard")
            } else {
                tr("Текущий счёт", "Live scoreboard")
            }
        )

        if (finished && valid) {
            GlowPanel(highlighted = true) {
                Text(
                    text = winnerText(
                        players = if (teamMode) teamNames else players,
                        scores = displayedScores.filterNotNull(),
                        rule = rule,
                        english = english
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (!valid) {
            Text(
                text = tr(
                    "Вернитесь к таблице и исправьте некорректные значения.",
                    "Return to the table and correct invalid values."
                ),
                color = MaterialTheme.colorScheme.error
            )
        }

        displayedNames.forEachIndexed { index, participant ->
            GlowPanel(
                highlighted = index in leaders
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = participant,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = displayedScores
                            .getOrNull(index)
                            ?.toString()
                            ?: "—",
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        fontSize = 56.sp,
                        lineHeight = 64.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        OutlinedButton(
            onClick = onNormal,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(tr("Обычный вид", "Normal view"))
        }

        if (finished) {
            MainButton(
                text = tr("Новая партия", "New game"),
                onClick = onNew
            )
        } else {
            MainButton(
                text = tr("Завершить партию", "Finish game"),
                enabled = valid,
                onClick = onFinish
            )
        }
    }
}