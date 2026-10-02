package com.example.boardgamescore

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class WinRule { MAX, MIN }

data class SavedGame(
    val id: String,
    val name: String,
    val date: Long,
    val players: List<String>,
    val rounds: List<List<Int>>,
    val rule: WinRule
)

private val AccentNames = listOf("Фиолетовый", "Синий", "Зелёный")
private val DarkAccents = listOf(
    Color(0xFFC2AAFF),
    Color(0xFF9CCAFF),
    Color(0xFF87DBB6)
)
private val LightAccents = listOf(
    Color(0xFF7045B5),
    Color(0xFF245FA6),
    Color(0xFF176B4C)
)

private class AppStorage(context: Context) {
    private val prefs = context.getSharedPreferences(
        "board_game_score",
        Context.MODE_PRIVATE
    )

    var theme: String
        get() = prefs.getString("theme", "system") ?: "system"
        set(value) {
            prefs.edit().putString("theme", value).apply()
        }

    var accent: Int
        get() = prefs.getInt("accent", 0).coerceIn(0, 2)
        set(value) {
            prefs.edit().putInt("accent", value).apply()
        }

    var animations: Boolean
        get() = prefs.getBoolean("animations", true)
        set(value) {
            prefs.edit().putBoolean("animations", value).apply()
        }

    fun loadHistory(): List<SavedGame> {
        return runCatching {
            val array = JSONArray(prefs.getString("history", "[]"))

            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                val players = item.getJSONArray("players")
                val rounds = item.getJSONArray("rounds")

                SavedGame(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    date = item.getLong("date"),
                    players = List(players.length()) {
                        players.getString(it)
                    },
                    rounds = List(rounds.length()) { rowIndex ->
                        val row = rounds.getJSONArray(rowIndex)
                        List(row.length()) { row.getInt(it) }
                    },
                    rule = WinRule.valueOf(item.getString("rule"))
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveHistory(games: List<SavedGame>) {
        val array = JSONArray()

        games.forEach { game ->
            val playerArray = JSONArray()
            game.players.forEach { playerArray.put(it) }

            val roundArray = JSONArray()
            game.rounds.forEach { row ->
                val values = JSONArray()
                row.forEach { values.put(it) }
                roundArray.put(values)
            }

            array.put(
                JSONObject().apply {
                    put("id", game.id)
                    put("name", game.name)
                    put("date", game.date)
                    put("players", playerArray)
                    put("rounds", roundArray)
                    put("rule", game.rule.name)
                }
            )
        }

        prefs.edit().putString("history", array.toString()).apply()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val storage = AppStorage(this)

        setContent {
            ScoreApp(storage)
        }
    }
}

@Composable
private fun ScoreApp(storage: AppStorage) {
    var theme by remember { mutableStateOf(storage.theme) }
    var accent by remember { mutableStateOf(storage.accent) }
    var animations by remember { mutableStateOf(storage.animations) }

    var history by remember { mutableStateOf(storage.loadHistory()) }
    var tab by remember { mutableStateOf(0) }

    var gameName by remember { mutableStateOf("") }
    var players by remember { mutableStateOf(emptyList<String>()) }
    var rule by remember { mutableStateOf(WinRule.MAX) }
    val rounds = remember {
        mutableStateListOf<SnapshotStateList<String>>()
    }

    // Форма создания не сбрасывается при переключении вкладок.
    var draftName by remember { mutableStateOf("") }
    val draftPlayers = remember { mutableStateListOf("", "") }
    var draftRule by remember { mutableStateOf(WinRule.MAX) }

    var active by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    var confirmFinish by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf<String?>(null) }
    var selectedGame by remember { mutableStateOf<SavedGame?>(null) }

    val systemDark = isSystemInDarkTheme()
    val dark = when (theme) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    val targetAccent = if (dark) DarkAccents[accent] else LightAccents[accent]
    val primary by animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(if (animations) 300 else 0),
        label = "accent"
    )

    val colors = if (dark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF181222),
            primaryContainer = primary.copy(alpha = 0.18f),
            background = Color(0xFF101117),
            surface = Color(0xFF1B1D26),
            surfaceVariant = Color(0xFF262936),
            onSurface = Color(0xFFF2F0F8),
            onBackground = Color(0xFFF2F0F8)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.12f),
            background = Color(0xFFF5F5FA),
            surface = Color.White,
            surfaceVariant = Color(0xFFEBEDF5),
            onSurface = Color(0xFF20212C),
            onBackground = Color(0xFF20212C)
        )
    }

    fun resetGame() {
        active = false
        finished = false
        players = emptyList()
        rounds.clear()
        confirmDiscard = false
        draftName = ""
        draftPlayers.clear()
        draftPlayers.addAll(listOf("", ""))
        draftRule = WinRule.MAX
    }

    BackHandler(enabled = active || tab != 0) {
        if (tab != 0) {
            tab = 0
        } else {
            confirmDiscard = true
        }
    }

    MaterialTheme(colorScheme = colors) {
        Scaffold(
            containerColor = colors.background,
            bottomBar = {
                NavigationBar(
                    containerColor = colors.surface,
                    tonalElevation = 0.dp
                ) {
                    val titles = listOf("Игра", "История", "Настройки")
                    val symbols = listOf("▶", "≡", "⚙")

                    titles.forEachIndexed { index, title ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = {
                                Text(
                                    symbols[index],
                                    fontSize = 23.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            label = { Text(title) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                colors.primary.copy(alpha = 0.10f),
                                colors.background
                            )
                        )
                    )
            ) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        fadeIn(tween(if (animations) 220 else 0)) togetherWith
                            fadeOut(tween(if (animations) 120 else 0))
                    },
                    label = "tabs"
                ) { currentTab ->
                    when (currentTab) {
                        0 -> {
                            if (!active) {
                                SetupPage(
                                    name = draftName,
                                    onName = { draftName = it },
                                    names = draftPlayers,
                                    rule = draftRule,
                                    onRule = { draftRule = it },
                                    onStart = {
                                        gameName = draftName.trim()
                                        players = draftPlayers.map { it.trim() }
                                        rule = draftRule

                                        rounds.clear()
                                        rounds.add(
                                            mutableStateListOf<String>().apply {
                                                repeat(players.size) { add("0") }
                                            }
                                        )

                                        active = true
                                        finished = false
                                    }
                                )
                            } else {
                                GamePage(
                                    name = gameName,
                                    players = players,
                                    rule = rule,
                                    rounds = rounds,
                                    finished = finished,
                                    animations = animations,
                                    onAddRound = {
                                        rounds.add(
                                            mutableStateListOf<String>().apply {
                                                repeat(players.size) { add("0") }
                                            }
                                        )
                                    },
                                    onFinish = { confirmFinish = true },
                                    onReset = {
                                        if (finished) resetGame()
                                        else confirmDiscard = true
                                    }
                                )
                            }
                        }

                        1 -> HistoryPage(
                            games = history,
                            onOpen = { selectedGame = it },
                            onDelete = { deleteId = it.id }
                        )

                        2 -> SettingsPage(
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
                            animations = animations,
                            onAnimations = {
                                animations = it
                                storage.animations = it
                            },
                            historySize = history.size,
                            onClear = { confirmClear = true }
                        )
                    }
                }
            }
        }

        if (confirmFinish) {
            ConfirmDialog(
                title = "Завершить партию?",
                message = "Результат сохранится в истории. Изменить очки после завершения нельзя.",
                action = "Завершить",
                onDismiss = { confirmFinish = false },
                onConfirm = {
                    val valid = rounds.all { row ->
                        row.all { parseScore(it) != null }
                    }

                    if (valid && !finished) {
                        val saved = SavedGame(
                            id = UUID.randomUUID().toString(),
                            name = gameName,
                            date = System.currentTimeMillis(),
                            players = players.toList(),
                            rounds = rounds.map { row ->
                                row.map { parseScore(it)!! }
                            },
                            rule = rule
                        )

                        history = listOf(saved) + history
                        storage.saveHistory(history)
                        finished = true
                    }

                    confirmFinish = false
                }
            )
        }

        if (confirmDiscard) {
            ConfirmDialog(
                title = "Закрыть текущую партию?",
                message = if (finished) {
                    "Результат уже сохранён в истории."
                } else {
                    "Незавершённая партия будет удалена без сохранения."
                },
                action = "Закрыть",
                onDismiss = { confirmDiscard = false },
                onConfirm = { resetGame() }
            )
        }

        if (confirmClear) {
            ConfirmDialog(
                title = "Очистить историю?",
                message = "Все завершённые партии будут удалены. Отменить это действие нельзя.",
                action = "Удалить всё",
                onDismiss = { confirmClear = false },
                onConfirm = {
                    history = emptyList()
                    storage.saveHistory(history)
                    confirmClear = false
                }
            )
        }

        deleteId?.let { id ->
            ConfirmDialog(
                title = "Удалить партию?",
                message = "Эта запись исчезнет из истории.",
                action = "Удалить",
                onDismiss = { deleteId = null },
                onConfirm = {
                    history = history.filterNot { it.id == id }
                    storage.saveHistory(history)
                    deleteId = null
                }
            )
        }

        selectedGame?.let { game ->
            HistoryDetails(
                game = game,
                onDismiss = { selectedGame = null }
            )
        }
    }
}

@Composable
private fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        content = content
    )
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun PageTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            fontSize = 30.sp,
            lineHeight = 35.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MainButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Choice(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(selected = selected, onClick = null)
            Text(title, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SetupPage(
    name: String,
    onName: (String) -> Unit,
    names: SnapshotStateList<String>,
    rule: WinRule,
    onRule: (WinRule) -> Unit,
    onStart: () -> Unit
) {
    val trimmed = names.map { it.trim() }
    val validName = name.trim().length in 1..30

    val errors = trimmed.map { player ->
        when {
            player.isEmpty() -> "Введите имя"
            player.length > 15 -> "Максимум 15 символов"
            trimmed.count { it.equals(player, true) } > 1 ->
                "Имя уже используется"
            else -> null
        }
    }

    val valid = validName &&
        names.size in 2..8 &&
        errors.all { it == null }

    Page {
        Text(
            "BOARD GAME SCORE",
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 3.sp,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        PageTitle(
            "Время играть",
            "Новая компания. Новая партия. Новый победитель."
        )

        Panel {
            Text("Название игры", fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = name,
                onValueChange = onName,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Например, Уно") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                isError = name.isNotEmpty() && !validName,
                supportingText = {
                    Text(
                        if (name.isNotEmpty() && !validName) {
                            "Введите от 1 до 30 символов"
                        } else {
                            "${name.trim().length}/30"
                        }
                    )
                }
            )
        }

        Panel {
            Text(
                "Участники · ${names.size}/8",
                fontWeight = FontWeight.Bold
            )

            names.forEachIndexed { index, value ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${index + 1}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = value,
                        onValueChange = { names[index] = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Игрок ${index + 1}") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        isError = value.isNotEmpty() && errors[index] != null,
                        supportingText = {
                            if (value.isNotEmpty() && errors[index] != null) {
                                Text(errors[index]!!)
                            }
                        }
                    )

                    if (names.size > 2) {
                        TextButton(
                            onClick = { names.removeAt(index) },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Text("×", fontSize = 24.sp)
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { names.add("") },
                enabled = names.size < 8,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Добавить игрока")
            }
        }

        Panel {
            Text("Условие победы", fontWeight = FontWeight.Bold)

            Choice(
                "Больше очков — лучше",
                rule == WinRule.MAX
            ) { onRule(WinRule.MAX) }

            Choice(
                "Меньше очков — лучше",
                rule == WinRule.MIN
            ) { onRule(WinRule.MIN) }
        }

        MainButton("Начать партию", valid, onStart)
    }
}

private fun parseScore(text: String): Int? {
    return text.trim().toIntOrNull()?.takeIf { it in -9999..9999 }
}

private fun calculateTotals(
    rounds: List<List<String>>,
    count: Int
): List<Long?> {
    return List(count) { column ->
        val values = rounds.map { parseScore(it[column]) }

        if (values.any { it == null }) null
        else values.sumOf { it!!.toLong() }
    }
}

private fun savedTotals(game: SavedGame): List<Long> {
    return List(game.players.size) { column ->
        game.rounds.sumOf { it[column].toLong() }
    }
}

private fun winnerText(
    players: List<String>,
    totals: List<Long>,
    rule: WinRule
): String {
    if (totals.isEmpty()) return "Нет результатов"

    val best = if (rule == WinRule.MAX) {
        totals.maxOrNull()!!
    } else {
        totals.minOrNull()!!
    }

    val winners = players.filterIndexed { index, _ ->
        totals[index] == best
    }

    return if (winners.size == 1) {
        "Победитель: ${winners.first()}"
    } else {
        "Ничья: ${winners.joinToString(", ")}"
    }
}

@Composable
private fun GamePage(
    name: String,
    players: List<String>,
    rule: WinRule,
    rounds: SnapshotStateList<SnapshotStateList<String>>,
    finished: Boolean,
    animations: Boolean,
    onAddRound: () -> Unit,
    onFinish: () -> Unit,
    onReset: () -> Unit
) {
    val totals = calculateTotals(rounds, players.size)
    val valid = rounds.isNotEmpty() && totals.all { it != null }
    val best = if (valid) {
        if (rule == WinRule.MAX) totals.filterNotNull().maxOrNull()
        else totals.filterNotNull().minOrNull()
    } else null

    Page {
        PageTitle(
            name,
            if (finished) {
                "Партия завершена · сохранено в истории"
            } else {
                "${players.size} игроков · ${rounds.size} раундов"
            }
        )

        Text(
            if (rule == WinRule.MAX) {
                "Побеждает максимальная сумма"
            } else {
                "Побеждает минимальная сумма"
            },
            color = MaterialTheme.colorScheme.primary
        )

        if (finished) {
            Panel {
                Text(
                    "ИТОГИ ПАРТИИ",
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    winnerText(players, totals.filterNotNull(), rule),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            players.forEachIndexed { index, player ->
                val leading = best != null && totals[index] == best

                val cardColor by animateColorAsState(
                    targetValue = if (leading) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    animationSpec = tween(if (animations) 250 else 0),
                    label = "leader"
                )

                Card(
                    modifier = Modifier.width(156.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cardColor
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(player, fontWeight = FontWeight.Bold)

                        AnimatedContent(
                            targetState = totals[index]?.toString() ?: "—",
                            transitionSpec = {
                                fadeIn(tween(if (animations) 180 else 0)) togetherWith
                                    fadeOut(tween(if (animations) 100 else 0))
                            },
                            label = "score"
                        ) { score ->
                            Text(
                                score,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            when {
                                totals[index] == null -> "Ошибка ввода"
                                leading && finished -> "Лучший результат"
                                leading -> "В лидерах"
                                else -> "Общий счёт"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Panel {
            Text("Очки по раундам", fontWeight = FontWeight.Bold)

            Text(
                "Таблица прокручивается вправо",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("№", modifier = Modifier.width(32.dp))

                    players.forEach {
                        Text(
                            it,
                            modifier = Modifier.width(100.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                rounds.forEachIndexed { roundIndex, row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${roundIndex + 1}",
                            modifier = Modifier.width(32.dp)
                        )

                        row.forEachIndexed { column, value ->
                            OutlinedTextField(
                                value = value,
                                onValueChange = { row[column] = it },
                                modifier = Modifier.width(100.dp),
                                singleLine = true,
                                readOnly = finished,
                                isError = parseScore(value) == null,
                                shape = RoundedCornerShape(14.dp),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                )
                            )
                        }
                    }
                }
            }

            if (!valid) {
                Text(
                    "Во всех полях нужны целые числа от −9999 до 9999.",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (!finished) {
            OutlinedButton(
                onClick = onAddRound,
                enabled = valid,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
            ) {
                Text("+ Добавить раунд")
            }

            MainButton("Завершить партию", valid, onFinish)

            TextButton(
                onClick = onReset,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Отменить партию")
            }
        } else {
            MainButton("Новая партия", onClick = onReset)
        }
    }
}

private fun formattedDate(time: Long): String {
    return SimpleDateFormat(
        "dd.MM.yyyy · HH:mm",
        Locale.getDefault()
    ).format(Date(time))
}

@Composable
private fun HistoryPage(
    games: List<SavedGame>,
    onOpen: (SavedGame) -> Unit,
    onDelete: (SavedGame) -> Unit
) {
    Page {
        PageTitle(
            "История",
            "Завершённые партии и ваши результаты"
        )

        if (games.isEmpty()) {
            Panel {
                Text(
                    "Пока ни одной партии",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Завершите игру — её результат появится здесь.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        games.forEach { game ->
            key(game.id) {
                Panel {
                    Text(
                        formattedDate(game.date),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )

                    Text(
                        game.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        winnerText(game.players, savedTotals(game), game.rule),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        "${game.players.size} игроков · ${game.rounds.size} раундов",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { onOpen(game) }) {
                            Text("Подробнее")
                        }
                        TextButton(onClick = { onDelete(game) }) {
                            Text(
                                "Удалить",
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
private fun HistoryDetails(
    game: SavedGame,
    onDismiss: () -> Unit
) {
    val totals = savedTotals(game)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(game.name) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(formattedDate(game.date))

                Text(
                    winnerText(game.players, totals, game.rule),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    if (game.rule == WinRule.MAX) {
                        "Условие: больше очков — лучше"
                    } else {
                        "Условие: меньше очков — лучше"
                    }
                )

                game.players.forEachIndexed { index, player ->
                    Text("$player: ${totals[index]}")
                }

                HorizontalDivider()

                Text("Раунды", fontWeight = FontWeight.Bold)

                game.rounds.forEachIndexed { index, row ->
                    Text(
                        "Раунд ${index + 1}\n" +
                            game.players.mapIndexed { playerIndex, player ->
                                "$player: ${row[playerIndex]}"
                            }.joinToString(" · ")
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
private fun SettingsPage(
    theme: String,
    onTheme: (String) -> Unit,
    accent: Int,
    onAccent: (Int) -> Unit,
    animations: Boolean,
    onAnimations: (Boolean) -> Unit,
    historySize: Int,
    onClear: () -> Unit
) {
    Page {
        PageTitle("Настройки", "Оформление под ваше настроение")

        Panel {
            Text("Тема приложения", fontWeight = FontWeight.Bold)

            Choice("Как на устройстве", theme == "system") {
                onTheme("system")
            }
            Choice("Светлая", theme == "light") {
                onTheme("light")
            }
            Choice("Тёмная", theme == "dark") {
                onTheme("dark")
            }
        }

        Panel {
            Text("Акцентный цвет", fontWeight = FontWeight.Bold)

            AccentNames.forEachIndexed { index, name ->
                Choice(name, accent == index) {
                    onAccent(index)
                }
            }
        }

        Panel {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Анимации", fontWeight = FontWeight.Bold)
                    Text(
                        "Плавные переходы, смена счёта и подсветка лидера",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = animations,
                    onCheckedChange = onAnimations
                )
            }
        }

        Panel {
            Text("История игр", fontWeight = FontWeight.Bold)
            Text("Сохранено партий: $historySize")

            OutlinedButton(
                onClick = onClear,
                enabled = historySize > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Очистить историю")
            }
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    action: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(action)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
