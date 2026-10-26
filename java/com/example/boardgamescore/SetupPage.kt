package com.example.boardgamescore

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SetupPage(
    templateIndex: Int,
    onTemplateChange: (Int) -> Unit,
    gameName: String,
    onGameNameChange: (String) -> Unit,
    playerNames: SnapshotStateList<String>,
    startingScores: SnapshotStateList<String>,
    playerEmojis: SnapshotStateList<String>,
    teamAssignments: SnapshotStateList<Int>,
    rule: WinRule,
    onRuleChange: (WinRule) -> Unit,
    target: String,
    onTargetChange: (String) -> Unit,
    teamMode: Boolean,
    onTeamModeChange: (Boolean) -> Unit,
    teamA: String,
    onTeamAChange: (String) -> Unit,
    teamB: String,
    onTeamBChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    tournaments: List<Tournament>,
    onTournamentsChange: (List<Tournament>) -> Unit,
    onStart: () -> Unit
) {
    val isFreeGame = templateIndex == 0

    val cleanNames = playerNames.map { it.trim() }

    val nameErrors = cleanNames.map { value ->
        when {
            value.isEmpty() -> {
                tr("Введите имя", "Enter a name")
            }

            value.length > 15 -> {
                tr("Максимум 15 символов", "Maximum 15 characters")
            }

            cleanNames.count {
                it.equals(value, ignoreCase = true)
            } > 1 -> {
                tr("Имя уже используется", "Name already used")
            }

            else -> null
        }
    }

    val startingScoresValid = startingScores.all { value ->
        value.trim()
            .toIntOrNull()
            ?.let { it in -9999..9999 }
            ?: false
    }

    val targetValue = parseTarget(target)
    val targetValid = targetValue != null

    val gameNameValid = if (isFreeGame) {
        gameName.trim().length in 1..30
    } else {
        true
    }

    val playerListsValid =
        playerNames.size == startingScores.size &&
            playerNames.size == playerEmojis.size &&
            playerNames.size == teamAssignments.size

    val teamsValid = if (!teamMode) {
        true
    } else {
        val teamAValue = teamA.trim()
        val teamBValue = teamB.trim()

        teamAssignments.size == playerNames.size &&
            teamAssignments.all { it == 0 || it == 1 } &&
            teamAssignments.contains(0) &&
            teamAssignments.contains(1) &&
            teamAValue.length in 1..30 &&
            teamBValue.length in 1..30 &&
            !teamAValue.equals(teamBValue, ignoreCase = true)
    }

    val canStart =
        playerListsValid &&
            playerNames.size in 2..8 &&
            gameNameValid &&
            nameErrors.all { it == null } &&
            startingScoresValid &&
            targetValid &&
            teamsValid

    Page {
        Heading(
            title = tr("Время играть", "Time to play"),
            subtitle = tr(
                "Выберите шаблон и настройте участников.",
                "Choose a template and configure the players."
            )
        )

        TournamentSection(
            tournaments = tournaments,
            onTournamentsChange = onTournamentsChange
        )

        GlowPanel(highlighted = true) {
            Text(
                text = tr("Шаблон игры", "Game template"),
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.horizontalScroll(
                    rememberScrollState()
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Templates.forEachIndexed { index, template ->
                    FilterChip(
                        selected = templateIndex == index,
                        onClick = { onTemplateChange(index) },
                        label = {
                            Text(
                                text = if (LocalEnglish.current) {
                                    template.en
                                } else {
                                    template.ru
                                }
                            )
                        }
                    )
                }
            }
        }

        if (isFreeGame) {
            GlowPanel {
                Text(
                    text = tr("Название игры", "Game name"),
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = gameName,
                    onValueChange = onGameNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(tr("Название", "Name"))
                    },
                    placeholder = {
                        Text(
                            tr(
                                "Например: Вечер настольных игр",
                                "For example: Board game night"
                            )
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    isError = gameName.isNotEmpty() &&
                        gameName.trim().length > 30
                )

                Text(
                    text = tr(
                        "От 1 до 30 символов",
                        "From 1 to 30 characters"
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (gameName.isNotEmpty() && gameName.trim().length > 30) {
                    Text(
                        text = tr(
                            "Максимум 30 символов",
                            "Maximum 30 characters"
                        ),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        GlowPanel {
            Text(
                text = tr(
                    "Участники · ${playerNames.size}/8",
                    "Players · ${playerNames.size}/8"
                ),
                fontWeight = FontWeight.Bold
            )

            playerNames.forEachIndexed { index, value ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            val currentIndex = Emojis
                                .indexOf(playerEmojis[index])
                                .coerceAtLeast(0)

                            playerEmojis[index] =
                                Emojis[(currentIndex + 1) % Emojis.size]
                        },
                        modifier = Modifier.size(48.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = playerEmojis[index],
                            fontSize = 22.sp
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = value,
                            onValueChange = {
                                playerNames[index] = it
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    tr(
                                        "Игрок ${index + 1}",
                                        "Player ${index + 1}"
                                    )
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            isError = value.isNotEmpty() &&
                                nameErrors[index] != null
                        )

                        if (value.isNotEmpty() && nameErrors[index] != null) {
                            Text(
                                text = nameErrors[index]!!,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = startingScores[index],
                                onValueChange = {
                                    startingScores[index] = it
                                },
                                modifier = Modifier.width(96.dp),
                                label = {
                                    Text(tr("Старт", "Start"))
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                isError = startingScores[index]
                                    .trim()
                                    .toIntOrNull()
                                    ?.let { it !in -9999..9999 }
                                    ?: true
                            )

                            if (teamMode) {
                                TextButton(
                                    onClick = {
                                        teamAssignments[index] =
                                            1 - teamAssignments[index]
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (teamAssignments[index] == 0) {
                                            teamA.ifBlank {
                                                tr("Команда A", "Team A")
                                            }
                                        } else {
                                            teamB.ifBlank {
                                                tr("Команда B", "Team B")
                                            }
                                        },
                                        maxLines = 1
                                    )
                                }
                            }

                            if (playerNames.size > 2) {
                                TextButton(
                                    onClick = {
                                        playerNames.removeAt(index)
                                        startingScores.removeAt(index)
                                        playerEmojis.removeAt(index)
                                        teamAssignments.removeAt(index)
                                    },
                                    modifier = Modifier.size(48.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = "×",
                                        fontSize = 24.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    playerNames.add("")
                    startingScores.add("0")
                    playerEmojis.add(
                        Emojis[(playerNames.size - 1) % Emojis.size]
                    )
                    teamAssignments.add(0)
                },
                enabled = playerNames.size < 8,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    tr("+ Добавить игрока", "+ Add player")
                )
            }
        }

        GlowPanel {
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
                        text = tr("Командный режим", "Team mode"),
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = tr("Счёт по командам", "Scores by teams"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = teamMode,
                    onCheckedChange = onTeamModeChange
                )
            }

            if (teamMode) {
                OutlinedTextField(
                    value = teamA,
                    onValueChange = onTeamAChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(tr("Команда A", "Team A"))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    isError = teamA.trim().length !in 1..30
                )

                OutlinedTextField(
                    value = teamB,
                    onValueChange = onTeamBChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(tr("Команда B", "Team B"))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    isError = teamB.trim().length !in 1..30
                )

                if (!teamsValid) {
                    Text(
                        text = tr(
                            "В каждой команде должен быть хотя бы один игрок. Названия команд должны отличаться.",
                            "Each team must have at least one player. Team names must be different."
                        ),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Text(
                text = tr("Победа", "Win rule"),
                fontWeight = FontWeight.Bold
            )

            Choice(
                text = tr("Больше очков", "Higher score wins"),
                selected = rule == WinRule.MAX,
                onClick = { onRuleChange(WinRule.MAX) }
            )

            Choice(
                text = tr("Меньше очков", "Lower score wins"),
                selected = rule == WinRule.MIN,
                onClick = { onRuleChange(WinRule.MIN) }
            )

            OutlinedTextField(
                value = target,
                onValueChange = onTargetChange,
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(tr("Цель, необязательно", "Target, optional"))
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                isError = target.isNotEmpty() && !targetValid
            )

            Text(
                text = if (rule == WinRule.MAX) {
                    tr(
                        "Целевая сумма очков. Пустое значение или 0 — без цели. Завершение партии вручную.",
                        "Target score. Leave empty or enter 0 to disable. Finish the game manually."
                    )
                } else {
                    tr(
                        "Лимит штрафных очков. Пустое значение или 0 — без лимита. Побеждает меньшая сумма.",
                        "Penalty limit. Leave empty or enter 0 to disable. The lowest total wins."
                    )
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        GlowPanel {
            Text(
                text = tr("Заметка", "Note"),
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(tr("Где играли", "Where you played"))
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        MainButton(
            text = tr("Начать партию", "Start game"),
            enabled = canStart,
            onClick = onStart
        )
    }
}