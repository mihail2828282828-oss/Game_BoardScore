package com.example.boardgamescore

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
internal fun StatsPage(
    games: List<SavedGame>
) {
    val statistics = remember(games) {
        calculateStats(games)
    }

    Page {
        Heading(
            title = tr("Статистика", "Stats"),
            subtitle = tr(
                "Личные и командные результаты",
                "Individual and team results"
            )
        )

        if (games.isEmpty()) {
            GlowPanel {
                Text(
                    text = tr("Пока нет данных", "No data yet"),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = tr(
                        "Завершите партию, чтобы увидеть статистику.",
                        "Finish a game to see your statistics."
                    )
                )
            }
        } else {
            GlowPanel {
                Text(
                    text = tr(
                        "Всего партий: ${games.size}",
                        "Total games: ${games.size}"
                    ),
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = tr(
                        "Участников: ${statistics.size}",
                        "Players: ${statistics.size}"
                    )
                )

                Text(
                    text = tr(
                        "Командная победа учитывается каждому игроку команды. " +
                            "Ничья — только участникам с лучшим равным результатом. " +
                            "Очки разных игр не сравниваются.",
                        "A team win counts for every teammate. " +
                            "Only tied leaders receive a draw. " +
                            "Scores from different games are not compared."
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            statistics.forEach { player ->
                key(normalizedPlayerName(player.name)) {
                    val losses = (
                        player.played - player.wins - player.ties
                    ).coerceAtLeast(0)

                    val winRate = if (player.played > 0) {
                        player.wins.toLong() * 100 / player.played
                    } else {
                        0L
                    }

                    GlowPanel {
                        Text(
                            text = player.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = tr(
                                "Игр: ${player.played}",
                                "Games: ${player.played}"
                            )
                        )

                        Text(
                            text = tr(
                                "Побед: ${player.wins} · Ничьих: ${player.ties}",
                                "Wins: ${player.wins} · Draws: ${player.ties}"
                            )
                        )

                        Text(
                            text = tr(
                                "Поражений: $losses",
                                "Losses: $losses"
                            )
                        )

                        Text(
                            text = tr(
                                "Доля побед: $winRate%",
                                "Win rate: $winRate%"
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}