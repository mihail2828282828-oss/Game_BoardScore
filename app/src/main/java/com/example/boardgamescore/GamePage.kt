package com.example.boardgamescore

import android.os.SystemClock
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.key
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
internal fun GamePage(
    name: String,
    players: List<String>,
    emojis: List<String>,
    starts: List<Int>,
    rule: WinRule,
    target: Int,
    teamMode: Boolean,
    teamAssign: List<Int>,
    teamNames: List<String>,
    rounds: SnapshotStateList<SnapshotStateList<String>>,
    finished: Boolean,
    turn: Int,
    onTurn: (Int) -> Unit,
    onBig: () -> Unit,
    onAdd: () -> Unit,
    onRemoveAsk: () -> Unit,
    onUndo: () -> Unit,
    canUndo: Boolean,
    pushUndo: () -> Unit,
    onFinishAsk: () -> Unit,
    onResetAsk: () -> Unit
) {
    val context = LocalContext.current
    val motion = LocalMotion.current
    val english = LocalEnglish.current

    val scores = totals(rounds, starts)
    val valid =
    players.size in 2..8 &&
        rounds.isNotEmpty() &&
        starts.size == players.size &&
        rounds.all { it.size == players.size } &&
        scores.size == players.size &&
        scores.all { it != null }

    val bestScore = if (!valid) {
        null
    } else if (rule == WinRule.MAX) {
        scores.filterNotNull().maxOrNull()
    } else {
        scores.filterNotNull().minOrNull()
    }

    val cumulative = cumulativeScores(
        rounds = rounds,
        starts = starts,
        playerCount = players.size
    )

    val safeTurn = if (players.isNotEmpty()) {
        turn.mod(players.size)
    } else {
        0
    }

    val teamTotals = if (teamMode) {
        teamScores(scores, teamAssign)
    } else {
        emptyList()
    }

    val resultsValid =
    valid &&
        (
            !teamMode ||
                (
                    teamNames.size == 2 &&
                        teamTotals.size == 2 &&
                        teamTotals.all { it != null }
                )
        )

    var die by remember { mutableIntStateOf(0) }
    var coinHeads by remember { mutableStateOf<Boolean?>(null) }

    var timerLength by remember { mutableIntStateOf(0) }
    var secondsLeft by remember { mutableIntStateOf(0) }
    var timerRunning by remember { mutableStateOf(false) }

	var timerDeadline by remember {
    mutableLongStateOf(0L)
}

var timerRevision by remember {
    mutableIntStateOf(0)
}

    LaunchedEffect(timerRunning, timerRevision, finished) {
    if (finished) {
        timerRunning = false
        return@LaunchedEffect
    }

    if (!timerRunning) {
        return@LaunchedEffect
    }

    while (true) {
        val remainingMillis =
            timerDeadline - SystemClock.elapsedRealtime()

        if (remainingMillis <= 0L) {
            secondsLeft = 0
            timerRunning = false
            vibrateNow(context)
            break
        }

        secondsLeft = (
            (remainingMillis + 999L) / 1000L
        ).toInt()

        delay(100)
    }
}

    Page {
        Heading(
            title = name,
            subtitle = tr(
                "Игроков: ${players.size} · Раундов: ${rounds.size}",
                "Players: ${players.size} · Rounds: ${rounds.size}"
            )
        )

        Text(
            text = buildString {
                append(
                    if (rule == WinRule.MAX) {
                        tr("Больше — лучше", "Higher wins")
                    } else {
                        tr("Меньше — лучше", "Lower wins")
                    }
                )

                if (target > 0) {
                    append(
                        tr(
                            " · Цель/лимит: $target",
                            " · Target/limit: $target"
                        )
                    )
                }
            },
            color = androidx.compose.material3.MaterialTheme
                .colorScheme
                .primary
        )

        if (
    !finished &&
    valid &&
    targetReached(
        scores = if (teamMode) teamTotals else scores,
        target = target
    )
) {
            GlowPanel(highlighted = true) {
                Text(
                    text = tr(
                        "Достигнута цель или лимит.",
                        "Target or limit reached."
                    ),
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = tr(
                        "Проверьте счёт и завершите партию вручную.",
                        "Check the scores and finish the game manually."
                    )
                )
            }
        }

        AnimatedVisibility(
            visible = finished,
            enter = fadeIn(
                tween(if (motion) 220 else 0)
            ) + expandVertically(
                tween(if (motion) 220 else 0)
            ),
            exit = fadeOut(
                tween(if (motion) 120 else 0)
            )
        ) {
            GlowPanel(highlighted = true) {
                Text(
                    text = tr("ИТОГИ", "RESULTS"),
                    color = androidx.compose.material3.MaterialTheme
                        .colorScheme
                        .primary,
                    letterSpacing = 2.sp
                )

                if (teamMode && teamTotals.all { it != null }) {
                    Text(
                        text = teamWinnerText(
                            names = teamNames,
                            scores = teamTotals.filterNotNull(),
                            rule = rule,
                            english = english
                        ),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else if (!teamMode && valid) {
                    Text(
                        text = winnerText(
                            players = players,
                            scores = scores.filterNotNull(),
                            rule = rule,
                            english = english
                        ),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (teamMode && teamTotals.isNotEmpty()) {
            GlowPanel(highlighted = true) {
                Text(
                    text = tr("Команды", "Teams"),
                    fontWeight = FontWeight.Bold
                )

                teamTotals.forEachIndexed { index, total ->
                    Text(
                        text = "${teamNames.getOrElse(index) { "T${index + 1}" }}: " +
                            (total?.toString() ?: "—"),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        GlowPanel {
            Text(
                text = tr(
                    "Инструменты партии",
                    "Game tools"
                ),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            if (players.isNotEmpty()) {
                Text(
                    text = tr(
                        "Ходит: ${emojis.getOrElse(safeTurn) { "" }} ${players.getOrElse(safeTurn) { "" }}",
                        "To move: ${emojis.getOrElse(safeTurn) { "" }} ${players.getOrElse(safeTurn) { "" }}"
                    )
                )
            }

            Row(
                modifier = Modifier.horizontalScroll(
                    rememberScrollState()
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (players.isNotEmpty()) {
                            onTurn((safeTurn + 1) % players.size)
                        }
                    },
                    enabled = !finished && players.size > 1
                ) {
                    Text(tr("Дальше ›", "Next ›"))
                }

                OutlinedButton(
                    onClick = {
                        die = Random.nextInt(1, 7)
                    }
                ) {
                    Text("🎲 ${if (die == 0) "–" else die}")
                }

                OutlinedButton(
    			onClick = {
        			coinHeads = Random.nextBoolean()
    			},
   			 enabled = !finished
			) {
    			val result = when (coinHeads) {
       				true -> tr("Орёл", "Heads")
        			false -> tr("Решка", "Tails")
        			null -> "—"
    			}

    			Text("🪙 $result")
			}

                OutlinedButton(
                    onClick = {
                        if (players.isNotEmpty()) {
                            onTurn(Random.nextInt(players.size))
                        }
                    },
                    enabled = !finished && players.isNotEmpty()
                ) {
                    Text(
                        tr(
                            "🎲 Первый ход",
                            "🎲 First turn"
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(
                    rememberScrollState()
                ),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(0, 30, 60, 90).forEach { seconds ->
                    FilterChip(
                        selected = timerLength == seconds,
                        onClick = {
    timerRunning = false
    timerLength = seconds
    secondsLeft = seconds
    timerDeadline = 0L
    timerRevision++
},
                        label = {
                            Text(
                                if (seconds == 0) {
                                    tr("Таймер выкл.", "Timer off")
                                } else {
                                    tr("${seconds}с", "${seconds}s")
                                }
                            )
                        }
                    )
                }
            }

            if (timerLength > 0) {
                Text(
                    text = tr(
                        "Осталось: ${secondsLeft}с",
                        "Time left: ${secondsLeft}s"
                    ),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (secondsLeft == 0) {
                        androidx.compose.material3.MaterialTheme
                            .colorScheme
                            .error
                    } else {
                        androidx.compose.material3.MaterialTheme
                            .colorScheme
                            .onSurface
                    }
                )

                if (secondsLeft == 0) {
                    Text(
                        text = tr(
                            "⏰ Время вышло!",
                            "⏰ Time's up!"
                        ),
                        color = androidx.compose.material3.MaterialTheme
                            .colorScheme
                            .error,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.horizontalScroll(
                        rememberScrollState()
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.material3.Button(
    onClick = {
        if (secondsLeft <= 0) {
            secondsLeft = timerLength
        }

        timerDeadline =
            SystemClock.elapsedRealtime() +
                secondsLeft * 1000L

        timerRevision++
        timerRunning = true
    },
    enabled = !timerRunning && !finished
) {
    Text(
        tr(
            "Старт",
            "Start"
        )
    )
}

                    OutlinedButton(
                        onClick = {
                            timerRunning = false
                        },
                        enabled = timerRunning
                    ) {
                        Text(tr("Пауза", "Pause"))
                    }

                    OutlinedButton(
                        onClick = {
    timerRunning = false
    secondsLeft = timerLength
    timerDeadline = 0L
    timerRevision++
}
                    ) {
                        Text(tr("Сброс", "Reset"))
                    }
                }
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(
                rememberScrollState()
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            players.forEachIndexed { playerIndex, player ->
                val leading =
                    bestScore != null &&
                        scores.getOrNull(playerIndex) == bestScore

                GlowPanel(
                    modifier = Modifier.width(260.dp),
                    highlighted = leading
                ) {
                    Text(
                        text = "${emojis.getOrElse(playerIndex) { "" }}  $player",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    if (playerIndex == safeTurn && !finished) {
                        Text(
                            text = tr("● ходит", "● to move"),
                            color = androidx.compose.material3.MaterialTheme
                                .colorScheme
                                .primary,
                            fontSize = 12.sp
                        )
                    }

                    ScoreNumber(
                        scores.getOrNull(playerIndex)
                    )

                    if (!finished && valid && rounds.isNotEmpty()) {
                        Row(
                            modifier = Modifier.horizontalScroll(
                                rememberScrollState()
                            ),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1, 5, 10, -1).forEach { delta ->
                                OutlinedButton(
                                    onClick = {
                                        val currentRound = rounds.lastOrNull()
                                            ?: return@OutlinedButton

                                        val currentValue = parseScore(
                                            currentRound.getOrNull(playerIndex)
                                                ?: return@OutlinedButton
                                        ) ?: return@OutlinedButton

                                        val updatedValue = (
                                            currentValue + delta
                                        ).coerceIn(-9999, 9999)

                                        if (updatedValue != currentValue) {
    						pushUndo()
    						currentRound[playerIndex] = updatedValue.toString()
					},
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Text(
                                        text = if (delta > 0) {
                                            "+$delta"
                                        } else {
                                            delta.toString()
                                        },
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (valid && cumulative != null) {
            ScoreChart(
                cumulative = cumulative,
                players = players
            )
        } else {
            Text(
                text = tr(
                    "Исправьте очки, чтобы увидеть график.",
                    "Correct the scores to view the chart."
                ),
                color = androidx.compose.material3.MaterialTheme
                    .colorScheme
                    .error
            )
        }

        GlowPanel {
            Text(
                text = tr(
                    "Очки по раундам",
                    "Round scores"
                ),
                fontWeight = FontWeight.Bold
            )

            Column(
                modifier = Modifier.horizontalScroll(
                    rememberScrollState()
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "№",
                        modifier = Modifier.width(36.dp)
                    )

                    players.forEach { player ->
                        Text(
                            text = player,
                            modifier = Modifier.width(108.dp),
                            color = androidx.compose.material3.MaterialTheme
                                .colorScheme
                                .primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                rounds.forEachIndexed { roundIndex, round ->
                    key(roundIndex) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${roundIndex + 1}".padStart(2, '0'),
                                modifier = Modifier.width(36.dp)
                            )

                            players.forEachIndexed { playerIndex, _ ->
                                val currentValue =
                                    round.getOrNull(playerIndex).orEmpty()

                                Column(
                                    modifier = Modifier.width(108.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    OutlinedTextField(
                                        value = currentValue,
					onValueChange = { newValue ->
    						if (
        						!finished &&
        						playerIndex in round.indices &&
        						round[playerIndex] != newValue
    						) {
        						pushUndo()
        						round[playerIndex] = newValue
    						}
					},
                                        modifier = Modifier.fillMaxWidth(),
                                        readOnly = finished,
                                        singleLine = true,
                                        isError = parseScore(currentValue) == null,
                                        shape = RoundedCornerShape(14.dp),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number
                                        )
                                    )

if (!finished) {
    TextButton(
        onClick = {
            if (playerIndex in round.indices) {
                val updated = when {
                    currentValue.startsWith("-") ->
                        																					currentValue.removePrefix("-")

                    								currentValue.startsWith("+") ->
                        								"-" + currentValue.removePrefix("+")

                   	 							else -> "-$currentValue"
                							}

                							if (updated != currentValue) {
                    								pushUndo()
                    								round[playerIndex] = updated
                							}
            							}
        						},
        						modifier = Modifier.heightIn(
            							min = 48.dp
        						)
    						) {
        						Text(
            							text = "±",
            							fontSize = 18.sp
        						)
    						}
					}
                                }
                            }
                        }
                    }
                }
            }

            if (!valid) {
                Text(
                    text = tr(
                        "Введите целые числа от −9999 до 9999 во всех ячейках.",
                        "Enter whole numbers from −9999 to 9999 in every cell."
                    ),
                    color = androidx.compose.material3.MaterialTheme
                        .colorScheme
                        .error
                )
            }
        }

        if (!finished) {
            OutlinedButton(
                onClick = onAdd,
                enabled = valid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    tr(
                        "+ Добавить раунд",
                        "+ Add round"
                    )
                )
            }

            OutlinedButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    tr(
                        "Отменить последнее действие",
                        "Undo last action"
                    )
                )
            }

            OutlinedButton(
                onClick = onRemoveAsk,
                enabled = rounds.size > 1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    tr(
                        "Удалить последний раунд",
                        "Delete last round"
                    )
                )
            }

            OutlinedButton(
                onClick = onBig,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    tr(
                        "Большой экран",
                        "Large scoreboard"
                    )
                )
            }

		MainButton(
    		text = tr(
        		"Завершить партию",
        		"Finish game"
    		),
    		enabled = resultsValid,
    		onClick = onFinishAsk
		)

            TextButton(
                onClick = onResetAsk,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    tr(
                        "Отменить партию",
                        "Discard game"
                    )
                )
            }
        } else {
            MainButton(
                text = tr(
                    "Новая партия",
                    "New game"
                ),
                onClick = onResetAsk
            )
        }
    }
}