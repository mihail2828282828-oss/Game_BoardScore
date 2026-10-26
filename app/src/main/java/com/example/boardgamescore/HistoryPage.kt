package com.example.boardgamescore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun HistoryPage(
    games: List<SavedGame>,
    onOpen: (SavedGame) -> Unit,
    onDelete: (SavedGame) -> Unit
) {
    val english = LocalEnglish.current

    Page {
        Heading(
            title = tr("История", "History"),
            subtitle = tr(
                "Завершённые партии",
                "Completed games"
            )
        )

        if (games.isEmpty()) {
            GlowPanel {
                Text(
                    text = tr(
                        "Здесь появятся ваши игры",
                        "Your games will appear here"
                    ),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = tr(
                        "Завершите первую партию.",
                        "Finish your first game."
                    )
                )
            }
        }

        games.forEach { game ->
            key(game.id) {
                GlowPanel {
                    Text(
                        text = dateText(game.date),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = game.name,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (game.teamMode) {
                        Text(
                            text = tr(
                                "Командная партия",
                                "Team game"
                            ),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = savedWinnerText(game, english),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = tr(
                            "Игроков: ${game.players.size} · Раундов: ${game.rounds.size}",
                            "Players: ${game.players.size} · Rounds: ${game.rounds.size}"
                        )
                    )

                    if (game.notes.isNotBlank()) {
                        Text(
                            text = "📝 ${game.notes}",
                            fontSize = 13.sp,
                            maxLines = 3,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = { onOpen(game) }
                        ) {
                            Text(tr("Подробнее", "Details"))
                        }

                        TextButton(
                            onClick = { onDelete(game) }
                        ) {
                            Text(
                                text = tr("Удалить", "Delete"),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun HistoryDetails(
    game: SavedGame,
    onDismiss: () -> Unit
) {
    val english = LocalEnglish.current
    val scores = savedTotals(game)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(game.name)
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = dateText(game.date),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = savedWinnerText(game, english),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (game.rule == WinRule.MAX) {
                        tr("Больше очков — лучше", "Higher score wins")
                    } else {
                        tr("Меньше очков — лучше", "Lower score wins")
                    }
                )

                if (game.target > 0) {
                    Text(
                        text = tr(
                            "Цель / лимит: ${game.target}",
                            "Target / limit: ${game.target}"
                        )
                    )
                }

                if (game.notes.isNotBlank()) {
                    Text("📝 ${game.notes}")
                }

                if (game.teamMode) {
                    HorizontalDivider()

                    Text(
                        text = tr("Команды", "Teams"),
                        fontWeight = FontWeight.Bold
                    )

                    savedTeamTotals(game).forEachIndexed { index, score ->
                        val teamName = game.teamNames.getOrElse(index) {
                            if (index == 0) "A" else "B"
                        }

                        Text(
                            text = "$teamName: $score",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                HorizontalDivider()

                Text(
                    text = tr("Участники", "Players"),
                    fontWeight = FontWeight.Bold
                )

                game.players.forEachIndexed { index, player ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "$player: ${scores[index]}",
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = tr(
                                "Стартовые очки: ${game.starts.getOrElse(index) { 0 }}",
                                "Starting score: ${game.starts.getOrElse(index) { 0 }}"
                            ),
                            fontSize = 12.sp
                        )

                        if (game.teamMode) {
                            val team = game.teamAssign.getOrElse(index) { 0 }

                            Text(
                                text = game.teamNames.getOrElse(team) { "—" },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    text = tr("Раунды", "Rounds"),
                    fontWeight = FontWeight.Bold
                )

                game.rounds.forEachIndexed { roundIndex, round ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = tr(
                                "Раунд ${roundIndex + 1}",
                                "Round ${roundIndex + 1}"
                            ),
                            fontWeight = FontWeight.SemiBold
                        )

                        game.players.forEachIndexed { playerIndex, player ->
                            Text(
                                text = "$player: ${round[playerIndex]}",
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Закрыть", "Close"))
            }
        }
    )
}