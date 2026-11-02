package com.example.boardgamescore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

/**
 * Вкладка «Турнир»: создание турнира, таблица побед и запуск матча.
 * Файл самодостаточен и не требует TournamentRules.kt.
 */
@Composable
internal fun TournamentPage(
    tournaments: List<Tournament>,
    onTournamentsChange: (List<Tournament>) -> Unit,
    selectedTournamentId: String?,
    onSelectTournament: (String?) -> Unit,
    matchTournamentId: String?,
    onPlayMatch: (Tournament) -> Unit
) {
    val selected = tournaments.firstOrNull { it.id == selectedTournamentId }

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
    var name by remember { mutableStateOf("") }
    var playersText by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("3") }

    val playerNames = playersText
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val targetWins = targetText.trim().toIntOrNull()

    val namesUnique = playerNames
        .map { it.trim().lowercase() }
        .distinct()
        .size == playerNames.size

    val canCreate =
        name.trim().length in 1..40 &&
            playerNames.size in 2..8 &&
            playerNames.all { it.length in 1..15 } &&
            namesUnique &&
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
            TournamentTournamentSectionTitle(tr("Новый турнир", "New tournament"))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(tr("Название", "Name")) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                isError = name.isNotEmpty() && name.trim().length !in 1..40
            )

            OutlinedTextField(
                value = playersText,
                onValueChange = { playersText = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(tr("Игроки через запятую", "Players, comma separated"))
                },
                placeholder = { Text(tr("Анна, Борис, Вика", "Ann, Bob, Vera")) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                isError = playerNames.any { it.length > 15 } || !namesUnique
            )

            OutlinedTextField(
                value = targetText,
                onValueChange = { targetText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(tr("Побед для титула", "Wins for the title")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                shape = RoundedCornerShape(14.dp),
                isError = targetText.isNotEmpty() &&
                    (targetWins == null || targetWins !in 1..20)
            )

            if (playerNames.isNotEmpty() && playerNames.size !in 2..8) {
                TournamentHint(
                    tr(
                        "Введите от 2 до 8 имён через запятую.",
                        "Enter 2–8 player names separated by commas."
                    ),
                    error = true
                )
            }

            if (!namesUnique) {
                TournamentHint(
                    tr(
                        "Имена игроков должны быть уникальными.",
                        "Player names must be unique."
                    ),
                    error = true
                )
            }

            MainButton(
                text = tr("Создать турнир", "Create tournament"),
                enabled = canCreate,
                onClick = {
                    val created = Tournament(
                        id = UUID.randomUUID().toString(),
                        name = name.trim(),
                        date = System.currentTimeMillis(),
                        players = playerNames,
                        targetWins = targetWins ?: 3,
                        wins = List(playerNames.size) { 0 }
                    )

                    onTournamentsChange(listOf(created) + tournaments)
                    onSelectTournament(created.id)

                    name = ""
                    playersText = ""
                    targetText = "3"
                }
            )

            TournamentHint(
                tr(
                    "SCORE CLUB — счётчик очков для дружеских настольных игр. " +
                        "Ставки и азартные игры запрещены.",
                    "SCORE CLUB is a scoreboard for friendly board games. " +
                        "Betting and gambling are prohibited."
                )
            )
        }

        if (tournaments.isEmpty()) {
            GlowPanel {
                TournamentSectionTitle(tr("Пока пусто", "Nothing yet"))

                TournamentHint(
                    tr(
                        "Создайте первый турнир — его таблица откроется сразу.",
                        "Create your first tournament — its table opens right away."
                    )
                )
            }
        } else {
            tournaments.forEach { tournament ->
                key(tournament.id) {
                    val finished = tpIsFinished(tournament)
                    val champions = tpChampions(tournament)

                    GlowPanel(highlighted = !finished) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = tournament.name,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                maxLines = 2
                            )

                            if (finished) {
                                Text(text = "🏆", fontSize = 22.sp)
                            }
                        }

                        TournamentHint(
                            tr(
                                "Цель: ${tournament.targetWins} побед · " +
                                    "Матчей: ${tpMatches(tournament)}",
                                "Target: ${tournament.targetWins} wins · " +
                                    "Matches: ${tpMatches(tournament)}"
                            )
                        )

                        if (finished && champions.isNotEmpty()) {
                            Text(
                                text = tr(
                                    "Чемпион: ${champions.joinToString(", ")}",
                                    "Champion: ${champions.joinToString(", ")}"
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        if (matchTournamentId == tournament.id) {
                            Text(
                                text = tr(
                                    "● Матч настроен на вкладке «Игра»",
                                    "● Match prepared on the “Game” tab"
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { onSelectTournament(tournament.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(tr("Открыть турнир", "Open tournament"))
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

    val finished = tpIsFinished(tournament)
    val champions = tpChampions(tournament)

    Page {
        Heading(
            title = tournament.name,
            subtitle = tr(
                "Цель: ${tournament.targetWins} побед · " +
                    "Матчей: ${tpMatches(tournament)}",
                "Target: ${tournament.targetWins} wins · " +
                    "Matches: ${tpMatches(tournament)}"
            )
        )

        if (finished) {
            GlowPanel(highlighted = true) {
                TournamentSectionTitle(
                    text = "🏆 " + tr("Турнир завершён", "Tournament finished"),
                    accent = true
                )

                Text(
                    text = tr(
                        "Чемпион: ${champions.joinToString(", ")}",
                        "Champion: ${champions.joinToString(", ")}"
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        GlowPanel {
            TournamentSectionTitle(tr("Таблица побед", "Wins table"))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

                            TournamentHint(
                                tr(
                                    "Побед: $wins из ${tournament.targetWins}",
                                    "Wins: $wins of ${tournament.targetWins}"
                                )
                            )
                        }

                        TextButton(
                            enabled = !finished &&
                                playerIndex in tournament.wins.indices,
                            onClick = {
                                tpChangeWins(
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
                                tpChangeWins(
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

        if (!finished) {
            MainButton(
                text = tr("▶ Играть матч", "▶ Play match"),
                onClick = { onPlayMatch(tournament) }
            )

            TournamentHint(
                tr(
                    "Откроется вкладка «Игра» с составом этого турнира. " +
                        "Вернуться сюда можно в любой момент.",
                    "The “Game” tab opens with this tournament's roster. " +
                        "You can come back here at any time."
                )
            )
        }

        if (matchTournamentId == tournament.id) {
            GlowPanel(highlighted = true) {
                TournamentSectionTitle(
                    tr("Матч готов", "Match is ready"),
                    accent = true
                )

                TournamentHint(
                    tr(
                        "Партия настроена на вкладке «Игра». Завершите её — " +
                            "победа попадёт в эту таблицу.",
                        "The game is prepared on the “Game” tab. Finish it and " +
                            "the win goes to this table."
                    )
                )
            }
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
            Text(tr("← К списку турниров", "← Back to tournaments"))
        }
    }

    if (pendingDelete) {
        AlertDialog(
            onDismissRequest = { pendingDelete = false },
            title = { Text(tr("Удалить турнир?", "Delete tournament?")) },
            text = {
                Text(
                    tr(
                        "Турнир «${tournament.name}» и его счёт побед " +
                            "будут удалены. Это действие нельзя отменить.",
                        "The tournament “${tournament.name}” and its win count " +
                            "will be removed. This action cannot be undone."
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTournamentsChange(
                            tournaments.filterNot { it.id == tournament.id }
                        )

                        pendingDelete = false
                        onClose()
                    }
                ) {
                    Text(tr("Удалить", "Delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = false }) {
                    Text(tr("Отмена", "Cancel"))
                }
            }
        )
    }
}

/** Изменение счёта побед: от 0 до цели турнира. */
private fun tpChangeWins(
    tournaments: List<Tournament>,
    tournament: Tournament,
    onChange: (List<Tournament>) -> Unit,
    playerIndex: Int,
    delta: Int
) {
    onChange(
        tournaments.map { current ->
            if (current.id != tournament.id) {
                current
            } else {
                current.copy(
                    wins = current.wins.mapIndexed { index, value ->
                        if (index == playerIndex) {
                            (value + delta).coerceIn(0, current.targetWins)
                        } else {
                            value
                        }
                    }
                )
            }
        }
    )
}

private fun tpIsFinished(tournament: Tournament): Boolean {
    return tournament.wins.any { it >= tournament.targetWins }
}

private fun tpChampions(tournament: Tournament): List<String> {
    return tournament.players.indices
        .filter {
            tournament.wins.getOrElse(it) { 0 } >= tournament.targetWins
        }
        .map { tournament.players[it] }
}

private fun tpMatches(tournament: Tournament): Int {
    return tournament.wins.sum()
}
