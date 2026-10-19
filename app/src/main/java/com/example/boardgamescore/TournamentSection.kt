package com.example.boardgamescore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import java.util.UUID

@Composable
internal fun TournamentSection(
    tournaments: List<Tournament>,
    onTournamentsChange: (List<Tournament>) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    var tournamentName by remember {
        mutableStateOf("")
    }

    var playersText by remember {
        mutableStateOf("")
    }

    var targetText by remember {
        mutableStateOf("3")
    }

    var pendingDeleteId by remember {
        mutableStateOf<String?>(null)
    }

    val playerNames = playersText
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val targetWins = targetText
        .trim()
        .toIntOrNull()

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

    pendingDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = {
                pendingDeleteId = null
            },
            title = {
                Text(
                    tr(
                        "Удалить турнир?",
                        "Delete tournament?"
                    )
                )
            },
            text = {
                Text(
                    tr(
                        "Это действие нельзя отменить.",
                        "This action cannot be undone."
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTournamentsChange(
                            tournaments.filterNot {
                                it.id == id
                            }
                        )
                        pendingDeleteId = null
                    }
                ) {
                    Text(
                        tr(
                            "Удалить",
                            "Delete"
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingDeleteId = null
                    }
                ) {
                    Text(
                        tr(
                            "Отмена",
                            "Cancel"
                        )
                    )
                }
            }
        )
    }

    GlowPanel(highlighted = tournaments.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = tr(
                        "🏆 Турниры",
                        "🏆 Tournaments"
                    ),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = tr(
                        "До заданного числа побед",
                        "Play to a chosen number of wins"
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(
                onClick = {
                    expanded = !expanded
                }
            ) {
                Text(
                    if (expanded) {
                        tr("Свернуть", "Hide")
                    } else {
                        tr("Открыть", "Open")
                    }
                )
            }
        }

        if (!expanded) {
            return@GlowPanel
        }

        if (tournaments.isEmpty()) {
            Text(
                tr(
                    "Пока нет турниров. Создайте первый!",
                    "No tournaments yet. Create your first one!"
                )
            )
        }

        tournaments.forEach { tournament ->
            HorizontalDivider()

	val tournamentFinished = tournament.wins.any {
    it >= tournament.targetWins
}

            Text(
                text = tournament.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = tr(
                    "Цель: ${tournament.targetWins} побед",
                    "Target: ${tournament.targetWins} wins"
                ),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            tournament.players.forEachIndexed { playerIndex, player ->
                val wins = tournament.wins
                    .getOrElse(playerIndex) { 0 }
                    .coerceAtLeast(0)

                Column(
    modifier = Modifier.weight(1f),
    verticalArrangement = Arrangement.spacedBy(4.dp)
) {
                    Text(
                        text = "$player — $wins" +
                            if (wins >= tournament.targetWins) {
                                " 🏆"
                            } else {
                                ""
                            }
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
    enabled = !tournamentFinished &&
        playerIndex in tournament.wins.indices,
    onClick = {
        if (tournament.wins.any { it >= tournament.targetWins }) {
            return@TextButton
        }

        updateTournament(
            tournaments = tournaments,
            tournament = tournament,
            onChange = onTournamentsChange
        ) { current ->
            current.copy(
                wins = current.wins.mapIndexed { index, value ->
                    if (index == playerIndex) {
                        (value + 1).coerceAtMost(current.targetWins)
                    } else {
                        value
                    }
                }
            )
        }
    }
) {
    Text("+1")
}

                        TextButton(
                            enabled = wins > 0,
                            onClick = {
                                updateTournament(
                                    tournaments = tournaments,
                                    tournament = tournament,
                                    onChange = onTournamentsChange
                                ) { current ->
                                    current.copy(
                                        wins = current.wins.mapIndexed {
                                                index,
                                                value ->
                                            if (index == playerIndex) {
                                                (value - 1).coerceAtLeast(0)
                                            } else {
                                                value
                                            }
                                        }
                                    )
                                }
                            }
                        ) {
                            Text("−1")
                        }
                    }
                }
            }

            TextButton(
                onClick = {
                    pendingDeleteId = tournament.id
                }
            ) {
                Text(
                    text = tr(
                        "Удалить турнир",
                        "Delete tournament"
                    ),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        HorizontalDivider()

        Text(
            text = tr(
                "Автоматический зачёт срабатывает только для единственного подходящего турнира и победителя без ничьей. При нескольких турнирах добавьте победу вручную.",
                "Automatic credit applies only when exactly one tournament matches and there is one winner. If multiple tournaments match, add the win manually."
            ),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = tr(
                "Новый турнир",
                "New tournament"
            ),
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = tournamentName,
            onValueChange = {
                tournamentName = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    tr(
                        "Название",
                        "Name"
                    )
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            isError = tournamentName.isNotEmpty() &&
                tournamentName.trim().length !in 1..40
        )

        OutlinedTextField(
            value = playersText,
            onValueChange = {
                playersText = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    tr(
                        "Игроки через запятую",
                        "Players, comma separated"
                    )
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            isError = playerNames.any { it.length > 15 } ||
                !namesAreUnique
        )

        OutlinedTextField(
            value = targetText,
            onValueChange = {
                targetText = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    tr(
                        "Побед для титула",
                        "Wins for title"
                    )
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            shape = RoundedCornerShape(14.dp),
            isError = targetWins == null ||
                targetWins !in 1..20
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
            text = tr(
                "Создать турнир",
                "Create tournament"
            ),
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

            tournamentName = ""
            playersText = ""
            targetText = "3"
        }
    }
}

private fun updateTournament(
    tournaments: List<Tournament>,
    tournament: Tournament,
    onChange: (List<Tournament>) -> Unit,
    transform: (Tournament) -> Tournament
) {
    onChange(
        tournaments.map { current ->
            if (current.id == tournament.id) {
                transform(current)
            } else {
                current
            }
        }
    )
}