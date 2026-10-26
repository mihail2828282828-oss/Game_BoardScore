package com.example.boardgamescore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import java.util.Locale
import java.util.UUID

@Composable
internal fun TournamentPage(
    tournaments: List<Tournament>,
    onTournamentsChange: (List<Tournament>) -> Unit,
    selectedTournamentId: String?,
    onSelectTournament: (String?) -> Unit,
    matchTournamentId: String?,
    onPlayMatch: (Tournament) -> Unit
) {
    val selected = tournaments.firstOrNull {
        it.id == selectedTournamentId
    }

    if (selected == null) {
        TournamentList(
            tournaments = tournaments,
            onTournamentsChange = onTournamentsChange,
            onSelectTournament = onSelectTournament,
            matchTournamentId = matchTournamentId
        )
    } else {
        TournamentDetails(
            tournament = selected,
            tournaments = tournaments,
            onTournamentsChange = onTournamentsChange,
            onClose = { onSelectTournament(null) },
            matchTournamentId = matchTournamentId,
            onPlayMatch = onPlayMatch
        )
    }
}

@Composable
private fun TournamentList(
    tournaments: List<Tournament>,
    onTournamentsChange: (List<Tournament>) -> Unit,
    onSelectTournament: (String?) -> Unit,
    matchTournamentId: String?
) {
    var tournamentName by remember { mutableStateOf("") }
    var playersText by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("3") }

    val playerNames = playersText
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val targetWins = targetText.trim().toIntOrNull()

    val namesAreUnique = playerNames
        .map { it.lowercase(Locale.ROOT) }
        .distinct()
        .size == playerNames.size

    val canCreate =
        tournamentName.trim().length in 1..40 &&
            playerNames.size in 2..8 &&
            playerNames.all { it.length in 1..15 } &&
            namesAreUnique &&
            targetWins != null &&
            targetWins in 1..20

    Page {
        Heading(
            title = tr("Турниры", "Tournaments"),
            subtitle = tr(
                "Серия партий до заданного числа побед",
                "A series of games to a set number of wins"
            )
        )

        GlowPanel(highlighted = true) {
            Text(
                text = tr("Новый турнир", "New tournament"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = tournamentName,
                onValueChange = { tournamentName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(tr("Название", "Name")) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                isError = tournamentName.isNotEmpty() &&
                    tournamentName.trim().length !in 1..40
            )

            OutlinedTextField(
                value = playersText,
                onValueChange = { playersText = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(tr("Игроки через запятую", "Players, comma separated"))
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                isError = playerNames.any { it.length > 15 } ||
                    !namesAreUnique
            )

            OutlinedTextField(
                value = targetText,
                onValueChange = { targetText = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(tr("Побед для титула", "Wins for title"))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                shape = RoundedCornerShape(14.dp),
                isError = targetWins == null || targetWins !in 1..20
            )

            if (playerNames.size !in 2..8) {
                Text(
                    text = tr(
                        "Введите от 2 до 8 имён через запятую.",
                        "Enter 2–8 player names separated by commas."
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (!namesAreUnique) {
                Text(
                    text = tr(
                        "Имена игроков должны быть уникальными.",
                        "Player names must be unique."
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            MainButton(
                text = tr("Создать турнир", "Create tournament"),
                enabled = canCreate
            ) {
                val newTournament = Tournament(
                    id = UUID.randomUUID().toString(),
                    name = tournamentName.trim(),
                    date = System.currentTimeMillis(),
                    players = playerNames,
                    targetWins = targetWins ?: 3,
                    wins = List(playerNames.size) { 0 }
                )

                onTournamentsChange(
                    listOf(newTournament) + tournaments
                )

                onSelectTournament(newTournament.id)

                tournamentName = ""
                playersText = ""
                targetText = "3"
            }
        }

        if (tournaments.isEmpty()) {
            GlowPanel {
                Text(
                    text = tr(
                        "Создайте турнир — после создания сразу откроется его таблица.",
                        "Create a tournament — its standings open right after creation."
                    )
                )
            }
        } else {
            tournaments.forEach { tournament ->
                key(tournament.id) {
                    val finished = tournament.wins.any {
                        it >= tournament.targetWins
                    }

                    val leaderWins = tournament.wins.maxOrNull() ?: 0

                    GlowPanel(highlighted = !finished) {
                        Text(
                            text = tournament.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = tr(
                                "Цель: ${tournament.targetWins} побед · " +
                                    "Лидер: $leaderWins",
                                "Target: ${tournament.targetWins} wins · " +
                                    "Leader: $leaderWins"
                            ),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (finished) {
                            Text(
                                text = "🏆 " + tr("Завершён", "Finished"),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (matchTournamentId == tournament.id) {
                            Text(
                                text = tr(
                                    "Матч этого турнира настроен на вкладке «Игра».",
                                    "A match for this tournament is prepared on the “Game” tab."
                                ),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                onSelectTournament(tournament.id)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                tr(
                                    "Открыть турнир",
                                    "Open tournament"
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentDetails(
    tournament: Tournament,
    tournaments: List<Tournament>,
    onTournamentsChange: (List<Tournament>) -> Unit,
    onClose: () -> Unit,
    matchTournamentId: String?,
    onPlayMatch: (Tournament) -> Unit
) {
    var pendingDelete by remember { mutableStateOf(false) }

    val tournamentFinished = tournament.wins.any {
        it >= tournament.targetWins
    }

    val championIndexes = tournament.players.indices.filter {
        tournament.wins.getOrElse(it) { 0 } >= tournament.targetWins
    }

    val totalWins = tournament.wins.sum()
    val english = LocalEnglish.current

    Page {
        Heading(
            title = tournament.name,
            subtitle = tr(
                "Цель: ${tournament.targetWins} побед · Матчей сыграно: $totalWins",
                "Target: ${tournament.targetWins} wins · Matches played: $totalWins"
            )
        )

        if (tournamentFinished) {
            GlowPanel(highlighted = true) {
                Text(
                    text = "🏆 " + tr(
                        "Турнир завершён",
                        "Tournament finished"
                    ),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = tr(
                        "Чемпион: " + championIndexes.joinToString(", ") {
                            tournament.players[it]
                        },
                        "Champion: " + championIndexes.joinToString(", ") {
                            tournament.players[it]
                        }
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        GlowPanel {
            Text(
                text = tr("Таблица побед", "Wins table"),
                fontWeight = FontWeight.Bold
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                tournament.players.forEachIndexed { playerIndex, player ->
                    val wins = tournament.wins
                        .getOrElse(playerIndex) { 0 }
                        .coerceAtLeast(0)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = if (wins >= tournament.targetWins) {
                                    "$player 🏆"
                                } else {
                                    player
                                },
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = tr(
                                    "Побед: $wins из ${tournament.targetWins}",
                                    "Wins: $wins of ${tournament.targetWins}"
                                ),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        TextButton(
                            enabled = !tournamentFinished &&
                                playerIndex in tournament.wins.indices,
                            onClick = {
                                if (tournament.wins.any {
                                        it >= tournament.targetWins
                                    }
                                ) {
                                    return@TextButton
                                }

                                changeWins(
                                    tournaments = tournaments,
                                    tournament = tournament,
                                    onChange = onTournamentsChange,
                                    playerIndex = playerIndex,
                                    delta = 1
                                )
                            }
                        ) {
                            Text("+1")
                        }

                        TextButton(
                            enabled = wins > 0,
                            onClick = {
                                changeWins(
                                    tournaments = tournaments,
                                    tournament = tournament,
                                    onChange = onTournamentsChange,
                                    playerIndex = playerIndex,
                                    delta = -1
                                )
                            }
                        ) {
                            Text("−1")
                        }
                    }
                }
            }
        }

        if (!tournamentFinished) {
            MainButton(
                text = tr("Играть матч", "Play match"),
                onClick = { onPlayMatch(tournament) }
            )
        }

        if (matchTournamentId == tournament.id) {
            Text(
                text = tr(
                    "Матч этого турнира настроен на вкладке «Игра». Завершите партию — победа зачтётся автоматически.",
                    "A match for this tournament is prepared on the “Game” tab. Finish the game and the win is credited automatically."
                ),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        GlowPanel {
            Text(
                text = tr("Как ведётся счёт", "How wins are counted"),
                fontWeight = FontWeight.Bold
            )

            Text(
                text = tr(
                    "Автозачёт работает, когда этот турнир — единственный подходящий: " +
                        "тот же состав игроков, один победитель, не командная партия. " +
                        "При ничьей или нескольких подходящих турнирах используйте +1 вручную.",
                    "Automatic credit works when this is the only matching tournament: " +
                        "the same players, a single winner, not a team game. " +
                        "For a draw or several matching tournaments, use +1 manually."
                ),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = fairPlayNote(english),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TextButton(
            onClick = { pendingDelete = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = tr("Удалить турнир", "Delete tournament"),
                color = MaterialTheme.colorScheme.error
            )
        }

        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                tr(
                    "← К списку турниров",
                    "← Back to tournaments"
                )
            )
        }
    }

    if (pendingDelete) {
        AlertDialog(
            onDismissRequest = { pendingDelete = false },
            title = {
                Text(
                    tr("Удалить турнир?", "Delete tournament?")
                )
            },
            text = {
                Text(
                    tr(
                        "Турнир «${tournament.name}» и его счёт побед будут удалены. Это действие нельзя отменить.",
                        "The tournament “${tournament.name}” and its win count will be removed. This action cannot be undone."
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTournamentsChange(
                            tournaments.filterNot {
                                it.id == tournament.id
                            }
                        )

                        pendingDelete = false
                        onClose()
                    }
                ) {
                    Text(tr("Удалить", "Delete"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDelete = false }
                ) {
                    Text(tr("Отмена", "Cancel"))
                }
            }
        )
    }
}

private fun changeWins(
    tournaments: List<Tournament>,
    tournament: Tournament,
    onChange: (List<Tournament>) -> Unit,
    playerIndex: Int,
    delta: Int
) {
    onChange(
        tournaments.map { current ->
            if (current.id == tournament.id) {
                current.copy(
                    wins = current.wins.mapIndexed { index, value ->
                        if (index == playerIndex) {
                            (value + delta).coerceIn(
                                0,
                                current.targetWins
                            )
                        } else {
                            value
                        }
                    }
                )
            } else {
                current
            }
        }
    )
}