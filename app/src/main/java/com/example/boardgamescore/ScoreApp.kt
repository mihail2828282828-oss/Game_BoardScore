package com.example.boardgamescore

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
internal fun ScoreApp(
    storage: Storage
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var theme by remember { mutableStateOf(storage.theme) }
    var accent by remember { mutableStateOf(storage.accent) }
    var style by remember { mutableStateOf(storage.style) }
    var motion by remember { mutableStateOf(storage.animations) }
    var english by remember { mutableStateOf(storage.english) }
    var sound by remember { mutableStateOf(storage.sound) }

    var history by remember { mutableStateOf(storage.loadHistory()) }
    var tournaments by remember {
        mutableStateOf(storage.loadTournaments())
    }

    var tab by remember { mutableStateOf(0) }

    // Черновик новой партии.
    var draftTemplate by remember { mutableStateOf(0) }
    var draftName by remember { mutableStateOf("") }

    val draftPlayers = remember {
        mutableStateListOf("", "")
    }

    val draftStarts = remember {
        mutableStateListOf("0", "0")
    }

    val draftEmojis = remember {
        mutableStateListOf(
            Emojis[0],
            Emojis[1]
        )
    }

    val draftTeams = remember {
        mutableStateListOf(0, 0)
    }

    var draftRule by remember { mutableStateOf(WinRule.MAX) }
    var draftTarget by remember { mutableStateOf("0") }
    var draftTeamMode by remember { mutableStateOf(false) }
    var draftTeamA by remember { mutableStateOf("Команда А") }
    var draftTeamB by remember { mutableStateOf("Команда Б") }
    var draftNotes by remember { mutableStateOf("") }

    // Текущая партия.
    var active by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var gameName by remember { mutableStateOf("") }
    var players by remember { mutableStateOf(emptyList<String>()) }
    var emojis by remember { mutableStateOf(emptyList<String>()) }
    var starts by remember { mutableStateOf(emptyList<Int>()) }
    var rule by remember { mutableStateOf(WinRule.MAX) }
    var target by remember { mutableStateOf(0) }
    var notes by remember { mutableStateOf("") }
    var teamMode by remember { mutableStateOf(false) }
    var teamAssign by remember { mutableStateOf(emptyList<Int>()) }
    var teamNames by remember { mutableStateOf(listOf("A", "B")) }

    val rounds = remember {
        mutableStateListOf<androidx.compose.runtime.snapshots.SnapshotStateList<String>>()
    }

    val undoStack = remember {
        mutableStateListOf<List<List<String>>>()
    }

    var turn by remember { mutableStateOf(0) }
    var bigScreen by remember { mutableStateOf(false) }

    // Диалоги.
    var confirmAction by remember { mutableStateOf<String?>(null) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var selectedGame by remember { mutableStateOf<SavedGame?>(null) }
    var showHelp by remember { mutableStateOf(false) }

    fun pushUndo() {
        undoStack.add(rounds.map { it.toList() })

        if (undoStack.size > 40) {
            undoStack.removeAt(0)
        }
    }

    fun restoreUndo() {
        if (undoStack.isEmpty()) return

        val previous = undoStack.removeAt(undoStack.lastIndex)

        rounds.clear()

        previous.forEach { snapshot ->
            rounds.add(newRound(snapshot.size).apply {
                addAll(snapshot)
            })
        }
    }

    fun resetGame() {
        active = false
        finished = false
        bigScreen = false

        gameName = ""
        players = emptyList()
        emojis = emptyList()
        starts = emptyList()
        rule = WinRule.MAX
        target = 0
        notes = ""
        teamMode = false
        teamAssign = emptyList()
        teamNames = listOf("A", "B")

        rounds.clear()
        undoStack.clear()
        turn = 0

        draftTemplate = 0
        draftName = ""
        draftRule = WinRule.MAX
        draftTarget = "0"
        draftTeamMode = false
        draftNotes = ""

        draftPlayers.clear()
        draftPlayers.addAll(listOf("", ""))

        draftStarts.clear()
        draftStarts.addAll(listOf("0", "0"))

        draftEmojis.clear()
        draftEmojis.addAll(listOf(Emojis[0], Emojis[1]))

        draftTeams.clear()
        draftTeams.addAll(listOf(0, 0))

        draftTeamA = if (english) "Team A" else "Команда А"
        draftTeamB = if (english) "Team B" else "Команда Б"
    }

    fun finishGame() {
        if (!active || finished) return

        val playerCount = players.size

        if (
            playerCount !in 2..8 ||
            rounds.isEmpty() ||
            rounds.any { it.size != playerCount }
        ) {
            return
        }

        val parsedRounds = rounds.map { row ->
            row.map { parseScore(it) ?: return }
        }

        val game = SavedGame(
            id = UUID.randomUUID().toString(),
            name = gameName.ifBlank {
                if (english) "Game" else "Партия"
            },
            date = System.currentTimeMillis(),
            players = players.toList(),
            rounds = parsedRounds,
            rule = rule,
            target = target,
            notes = notes,
            starts = starts.toList(),
            teamMode = teamMode,
            teamAssign = teamAssign.toList(),
            teamNames = teamNames.toList()
        )

        history = listOf(game) + history
        storage.saveHistory(history)

        val credited = creditTournamentWin(
            tournaments = tournaments,
            game = game
        )

        if (credited != tournaments) {
            tournaments = credited
            storage.saveTournaments(tournaments)
        }

        finished = true
        bigScreen = false

        if (sound) {
            vibrateNow(context)
            scope.launch {
                fanfare(true)
            }
        }
    }

    val useDark = style == 3 || when (theme) {
        "light" -> false
        "dark", "amoled" -> true
        else -> isSystemInDarkTheme()
    }

    val useAmoled = theme == "amoled" || style == 3

    val safeAccent = accent.coerceIn(0, 2)

    val darkAccents = listOf(
        Color(0xFFC6A8FF),
        Color(0xFF8ECDFF),
        Color(0xFF88E3BF)
    )

    val lightAccents = listOf(
        Color(0xFF7040B0),
        Color(0xFF175DA6),
        Color(0xFF176B4C)
    )

    val colors = if (useDark) {
        darkColorScheme(
            primary = darkAccents[safeAccent],
            onPrimary = Color(0xFF171021),
            secondary = Color(0xFF7ADBCB),
            background = if (useAmoled) {
                Color.Black
            } else {
                Color(0xFF101019)
            },
            surface = if (useAmoled) {
                Color(0xFF0B0B0F)
            } else {
                Color(0xFF1B1B29)
            },
            surfaceVariant = Color(0xFF292A3A),
            onBackground = Color(0xFFF5F1FF),
            onSurface = Color(0xFFF5F1FF)
        )
    } else {
        lightColorScheme(
            primary = lightAccents[safeAccent],
            onPrimary = Color.White,
            secondary = Color(0xFF167D80),
            background = Color(0xFFF5F3FC),
            surface = Color.White,
            surfaceVariant = Color(0xFFEAE6F2),
            onBackground = Color(0xFF231D30),
            onSurface = Color(0xFF231D30)
        )
    }

    val backgroundBrush = if (useAmoled) {
        null
    } else {
        when (style) {
            1 -> Brush.linearGradient(
                listOf(
                    colors.primary.copy(alpha = 0.24f),
                    colors.background,
                    Color(0xFFFF9E7A).copy(alpha = 0.16f)
                )
            )

            2 -> Brush.linearGradient(
                listOf(
                    Color(0xFF4ADE80).copy(alpha = 0.20f),
                    colors.background,
                    colors.primary.copy(alpha = 0.14f)
                )
            )

            else -> Brush.linearGradient(
                listOf(
                    colors.primary.copy(alpha = 0.18f),
                    colors.background,
                    colors.secondary.copy(alpha = 0.12f)
                )
            )
        }
    }

    BackHandler(
        enabled = active || tab != 0
    ) {
        when {
            tab != 0 -> tab = 0
            bigScreen -> bigScreen = false
            finished -> resetGame()
            else -> confirmAction = "discard"
        }
    }

    CompositionLocalProvider(
        LocalEnglish provides english,
        LocalMotion provides motion
    ) {
        MaterialTheme(colorScheme = colors) {
            Scaffold(
                containerColor = colors.background,
                topBar = {
                    Surface(
                        color = colors.background.copy(alpha = 0.92f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(
                                    horizontal = 20.dp,
                                    vertical = 6.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "SCORE CLUB",
                                color = colors.primary,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                fontSize = 14.sp
                            )

                            TextButton(
                                onClick = { showHelp = true }
                            ) {
                                Text(
                                    tr("?  Помощь", "?  Help")
                                )
                            }
                        }
                    }
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = colors.surface,
                        tonalElevation = 0.dp
                    ) {
                        val titles = listOf(
                            tr("Игра", "Game"),
                            tr("История", "History"),
                            tr("Статы", "Stats"),
                            tr("Настройки", "Settings")
                        )

                        val icons = listOf("▶", "≡", "★", "⚙")

                        titles.forEachIndexed { index, title ->
                            NavigationBarItem(
                                selected = tab == index,
                                onClick = { tab = index },
                                icon = {
                                    Text(
                                        icons[index],
                                        fontSize = 18.sp
                                    )
                                },
                                label = {
                                    Text(title)
                                }
                            )
                        }
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .imePadding()
                        .background(
                            backgroundBrush
                                ?: SolidBackground(colors.background)
                        )
                ) {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            fadeIn(
                                tween(if (motion) 200 else 0)
                            ) togetherWith fadeOut(
                                tween(if (motion) 100 else 0)
                            )
                        },
                        label = "tabs"
                    ) { currentTab ->
                        when (currentTab) {
                            0 -> {
                                when {
                                    !active -> {
                                        SetupPage(
                                            templateIndex = draftTemplate,
                                            onTemplateChange = { index ->
                                                draftTemplate = index

                                                val template =
                                                    Templates.getOrNull(index)

                                                if (template != null) {
                                                    draftRule = template.rule
                                                    draftTarget =
                                                        template.target
 .toString()

                                                    if (index != 0) {
                                                        draftName = ""
                                                    }
                                                }
                                            },
                                            gameName = draftName,
                                            onGameNameChange = {
                                                draftName = it
                                            },
                                            playerNames = draftPlayers,
                                            startingScores = draftStarts,
                                            playerEmojis = draftEmojis,
                                            teamAssignments = draftTeams,
                                            rule = draftRule,
                                            onRuleChange = {
                                                draftRule = it
                                            },
                                            target = draftTarget,
                                            onTargetChange = {
                                                draftTarget = it
                                            },
                                            teamMode = draftTeamMode,
                                            onTeamModeChange = {
                                                draftTeamMode = it
                                            },
                                            teamA = draftTeamA,
                                            onTeamAChange = {
                                                draftTeamA = it
                                            },
                                            teamB = draftTeamB,
                                            onTeamBChange = {
                                                draftTeamB = it
                                            },
                                            notes = draftNotes,
                                            onNotesChange = {
                                                draftNotes = it
                                            },
                                            tournaments = tournaments,
                                            onTournamentsChange = {
                                                tournaments = it
                                                storage.saveTournaments(it)
                                            },
                                            onStart = {
                                                val count =
                                                    draftPlayers.size

                                                if (
                                                    count !in 2..8 ||
                                                    rounds.isNotEmpty()
                                                ) {
                                                    return@SetupPage
                                                }

                                                gameName =
                                                    gameNameForTemplate(
                                                        templateIndex =
                                                            draftTemplate,
                                                        customName =
                                                            draftName,
                                                        english =
                                                            english
                                                    )

                                                players = draftPlayers
                                                    .map { it.trim() }

                                                starts = draftStarts.map {
                                                    it.trim()
                                                        .toIntOrNull()
                                                        ?.coerceIn(
                                                            -9999,
                                                            9999
                                                        )
                                                        ?: 0
                                                }

                                                emojis = draftEmojis
                                                    .toList()

                                                rule = draftRule

                                                target =
                                                    parseTarget(draftTarget)
                                                        ?: 0

                                                notes =
                                                    draftNotes.trim()

                                                teamMode =
                                                    draftTeamMode

                                                teamAssign =
                                                    draftTeams.toList()

                                                teamNames = listOf(
                                                    draftTeamA.trim()
                                                        .ifEmpty {
                                                            if (english) {
                                                                "Team A"
                                                            } else {
                                                                "Команда А"
                                                            }
                                                        },
                                                    draftTeamB.trim()
                                                        .ifEmpty {
                                                            if (english) {
                                                                "Team B"
                                                            } else {
                                                                "Команда Б"
                                                            }
                                                        }
                                                )

                                                rounds.clear()
                                                rounds.add(
                                                    newRound(count)
                                                )

                                                undoStack.clear()

                                                finished = false
                                                bigScreen = false
                                                turn = 0
                                                active = true
                                            }
                                        )
                                    }

                                    bigScreen -> {
                                        BigScreen(
                                            name = gameName,
                                            players = players,
                                            emojis = emojis,
                                            scores = totals(
                                                rounds,
                                                starts
                                            ),
                                            finished = finished,
                                            rule = rule,
                                            teamMode = teamMode,
                                            teamAssign = teamAssign,
                                            teamNames = teamNames,
                                            onNormal = {
                                                bigScreen = false
                                            },
                                            onFinish = {
                                                confirmAction =
                                                    "finish"
                                            },
                                            onNew = {
                                                resetGame()
                                            }
                                        )
                                    }

                                    else -> {
                                        GamePage(
                                            name = gameName,
                                            players = players,
                                            emojis = emojis,
                                            starts = starts,
                                            rule = rule,
                                            target = target,
                                            teamMode = teamMode,
                                            teamAssign = teamAssign,
                                            teamNames = teamNames,
                                            rounds = rounds,
                                            finished = finished,
                                            turn = turn,
                                            onTurn = { turn = it },
                                            onBig = {
                                                bigScreen = true
                                            },
                                            onAdd = {
                                                pushUndo()
                                                rounds.add(
                                                    newRound(
                                                        players.size
                                                    )
                                                )
                                            },
                                            onRemoveAsk = {
                                                confirmAction = "round"
                                            },
                                            onUndo = {
                                                restoreUndo()
                                            },
                                            canUndo =
                                                undoStack.isNotEmpty(),
                                            pushUndo = {
                                                pushUndo()
                                            },
                                            onFinishAsk = {
                                                confirmAction =
                                                    "finish"
                                            },
                                            onResetAsk = {
                                                confirmAction =
                                                    "discard"
                                            }
                                        )
                                    }
                                }
                            }

                            1 -> {
                                HistoryPage(
                                    games = history,
                                    onOpen = {
                                        selectedGame = it
                                    },
                                    onDelete = {
                                        pendingDeleteId = it.id
                                        confirmAction = "delete"
                                    }
                                )
                            }

                            2 -> {
                                StatsPage(games = history)
                            }

                            else -> {
                                SettingsPage(
                                    theme = theme,
                                    onTheme = {
                                        theme = it
                                        storage.theme = it
                                    },
                                    accent = accent,
                                    onAccent = {
                                        accent = it
                                        storage.accent = it
                                    },
                                    style = style,
                                    onStyle = {
                                        style = it
                                        storage.style = it
                                    },
                                    english = english,
                                    onEnglish = {
                                        english = it
                                        storage.english = it
                                    },
                                    motion = motion,
                                    onMotion = {
                                        motion = it
                                        storage.animations = it
                                    },
                                    sound = sound,
                                    onSound = {
                                        sound = it
                                        storage.sound = it
                                    },
                                    historyCount = history.size,
                                    onClear = {
                                        confirmAction = "clear"
                                    },
                                    tourCount = tournaments.size
                                )
                            }
                        }
                    }
                }
            }

            if (showHelp) {
                HelpDialog(
                    onDismiss = { showHelp = false }
                )
            }

            selectedGame?.let { game ->
                HistoryDetails(
                    game = game,
                    onDismiss = { selectedGame = null }
                )
            }

            confirmAction?.let { action ->
                val title = when (action) {
                    "round" -> tr(
                        "Удалить последний раунд?",
                        "Delete the last round?"
                    )

                    "clear" -> tr(
                        "Очистить историю?",
                        "Clear history?"
                    )

                    "delete" -> tr(
                        "Удалить запись?",
                        "Delete entry?"
                    )

                    "discard" -> tr(
                        "Отменить партию?",
                        "Discard game?"
                    )

                    else -> tr(
                        "Завершить партию?",
                        "Finish game?"
                    )
                }

                val message = when (action) {
                    "round" -> tr(
                        "Это действие можно отменить кнопкой отмены последнего действия.",
                        "You can undo this with Undo last action."
                    )

                    "clear" -> tr(
                        "История и статистика будут удалены. Турниры сохранятся.",
                        "History and statistics will be removed. Tournaments are kept."
                    )

                    "delete" -> tr(
                        "Это действие нельзя отменить.",
                        "This action cannot be undone."
                    )

                    "discard" -> tr(
                        "Незавершённая партия будет удалена.",
                        "The unfinished game will be discarded."
                    )

                    else -> tr(
                        "Партия будет сохранена в истории, а её счёт станет результатом.",
                        "The game will be saved to history and its scores become final."
                    )
                }

                AlertDialog(
                    onDismissRequest = {
                        confirmAction = null
                    },
                    title = {
                        Text(title)
                    },
                    text = {
                        Text(message)
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                when (action) {
                                    "finish" -> finishGame()

                                    "discard" -> resetGame()

                                    "round" -> {
                                        if (rounds.size > 1) {
                                            pushUndo()
                                            rounds.removeAt(
                                                rounds.lastIndex
                                            )
                                        }
                                    }

                                    "clear" -> {
                                        history = emptyList()
                                        storage.saveHistory(history)
                                    }

                                    "delete" -> {
                                        val id = pendingDeleteId

                                        if (id != null) {
                                            history =
                                                history.filterNot {
                                                    it.id == id
                                                }

                                            storage.saveHistory(
                                                history
                                            )
                                        }
                                    }
                                }

                                pendingDeleteId = null
                                confirmAction = null
                            }
                        ) {
                            Text(
                                if (action == "round") {
                                    tr("Удалить", "Delete")
                                } else {
                                    tr("Подтвердить", "Confirm")
                                }
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                pendingDeleteId = null
                                confirmAction = null
                            }
                        ) {
                            Text(tr("Отмена", "Cancel"))
                        }
                    }
                )
            }
        }
    }
}

private fun SolidBackground(
    color: Color
): Brush {
    return Brush.linearGradient(listOf(color, color))
}
