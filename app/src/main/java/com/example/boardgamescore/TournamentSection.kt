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

    val totalWins =
