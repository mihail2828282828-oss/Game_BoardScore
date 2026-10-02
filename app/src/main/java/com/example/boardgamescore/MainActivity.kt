package com.example.boardgamescore

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import kotlinx.coroutines.delay
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

private val LocalEnglish = staticCompositionLocalOf { false }
private val LocalMotion = staticCompositionLocalOf { true }

@Composable
private fun tr(ru: String, en: String): String =
    if (LocalEnglish.current) en else ru

private class Storage(context: Context) {
    private val prefs = context.getSharedPreferences(
        "board_game_score",
        Context.MODE_PRIVATE
    )

    var theme: String
        get() = prefs.getString("theme", "system") ?: "system"
        set(value) { prefs.edit().putString("theme", value).apply() }

    var accent: Int
        get() = prefs.getInt("accent", 0).coerceIn(0, 2)
        set(value) { prefs.edit().putInt("accent", value).apply() }

    var animations: Boolean
        get() = prefs.getBoolean("animations", true)
        set(value) { prefs.edit().putBoolean("animations", value).apply() }

    var english: Boolean
        get() = prefs.getBoolean("english", false)
        set(value) { prefs.edit().putBoolean("english", value).apply() }

    fun loadHistory(): List<SavedGame> = runCatching {
        val array = JSONArray(prefs.getString("history", "[]") ?: "[]")
        buildList {
            for (index in 0 until array.length()) {
                // Повреждение одной записи не скрывает остальные.
                runCatching {
                    val item = array.getJSONObject(index)
                    val names = item.getJSONArray("players")
                    val rows = item.getJSONArray("rounds")
                    val players = List(names.length()) { names.getString(it) }
                    val rounds = List(rows.length()) { rowIndex ->
                        val row = rows.getJSONArray(rowIndex)
                        List(row.length()) { row.getInt(it) }
                    }
                    require(players.size in 2..8)
                    require(rounds.isNotEmpty())
                    require(rounds.all { row ->
                        row.size == players.size &&
                            row.all { it in -9999..9999 }
                    })
                    SavedGame(
                        item.getString("id"),
                        item.getString("name"),
                        item.getLong("date"),
                        players,
                        rounds,
                        WinRule.valueOf(item.getString("rule"))
                    )
                }.getOrNull()?.let { add(it) }
            }
        }
    }.getOrDefault(emptyList())

    fun saveHistory(games: List<SavedGame>) {
        val array = JSONArray()
        games.forEach { game ->
            val names = JSONArray()
            game.players.forEach { names.put(it) }
            val rows = JSONArray()
            game.rounds.forEach { row ->
                val values = JSONArray()
                row.forEach { values.put(it) }
                rows.put(values)
            }
            array.put(JSONObject().apply {
                put("id", game.id)
                put("name", game.name)
                put("date", game.date)
                put("players", names)
                put("rounds", rows)
                put("rule", game.rule.name)
            })
        }
        prefs.edit().putString("history", array.toString()).apply()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val storage = Storage(this)
        setContent { ScoreApp(storage) }
    }
}

private fun parseScore(text: String): Int? =
    text.trim().toIntOrNull()?.takeIf { it in -9999..9999 }

private fun newRound(size: Int) =
    mutableStateListOf<String>().apply {
        repeat(size) { add("0") }
    }

private fun totals(
    rounds: List<List<String>>,
    count: Int
): List<Long?> = List(count) { column ->
    val values = rounds.map { parseScore(it[column]) }
    if (values.any { it == null }) null
    else values.sumOf { it!!.toLong() }
}

private fun savedTotals(game: SavedGame): List<Long> =
    List(game.players.size) { column ->
        game.rounds.sumOf { it[column].toLong() }
    }

private fun winner(
    players: List<String>,
    values: List<Long>,
    rule: WinRule,
    english: Boolean
): String {
    if (values.isEmpty()) return if (english) "No results" else "Нет результатов"
    val best = if (rule == WinRule.MAX) values.maxOrNull()!!
    else values.minOrNull()!!
    val names = players.filterIndexed { index, _ -> values[index] == best }
    val title = if (names.size == 1) {
        if (english) "Winner" else "Победитель"
    } else {
        if (english) "Tie" else "Ничья"
    }
    return "$title: ${names.joinToString(", ")}"
}

private fun dateText(time: Long): String =
    SimpleDateFormat("dd.MM.yyyy · HH:mm", Locale.getDefault()).format(Date(time))

@Composable
private fun ScoreApp(storage: Storage) {
    var theme by remember { mutableStateOf(storage.theme) }
    var accent by remember { mutableStateOf(storage.accent) }
    var motion by remember { mutableStateOf(storage.animations) }
    var english by remember { mutableStateOf(storage.english) }
    var history by remember { mutableStateOf(storage.loadHistory()) }
    var tab by remember { mutableStateOf(0) }

    var draftName by remember { mutableStateOf("") }
    val draftPlayers = remember { mutableStateListOf("", "") }
    var draftRule by remember { mutableStateOf(WinRule.MAX) }

    var active by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var players by remember { mutableStateOf(emptyList<String>()) }
    var rule by remember { mutableStateOf(WinRule.MAX) }
    val rounds = remember {
        mutableStateListOf<SnapshotStateList<String>>()
    }

    // finish / discard / clear / round / delete
    var confirmation by remember { mutableStateOf<String?>(null) }
    var deleteId by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<SavedGame?>(null) }
    var help by remember { mutableStateOf(false) }

    fun reset() {
        active = false
        finished = false
        players = emptyList()
        rounds.clear()
        draftName = ""
        draftPlayers.clear()
        draftPlayers.addAll(listOf("", ""))
        draftRule = WinRule.MAX
    }

    fun updateHistory(value: List<SavedGame>) {
        history = value
        storage.saveHistory(value)
    }

    val dark = when (theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val darkAccents = listOf(
        Color(0xFFC6A8FF), Color(0xFF8ECDFF), Color(0xFF88E3BF)
    )
    val lightAccents = listOf(
        Color(0xFF7040B0), Color(0xFF175DA6), Color(0xFF176B4C)
    )
    val primary by animateColorAsState(
        targetValue = if (dark) darkAccents[accent] else lightAccents[accent],
        animationSpec = tween(if (motion) 350 else 0),
        label = "themeAccent"
    )

    val scheme = if (dark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF171021),
            secondary = Color(0xFF7ADBCB),
            background = Color(0xFF101019),
            surface = Color(0xFF1B1B29),
            surfaceVariant = Color(0xFF292A3A),
            onBackground = Color(0xFFF5F1FF),
            onSurface = Color(0xFFF5F1FF)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            secondary = Color(0xFF167D80),
            background = Color(0xFFF5F3FC),
            surface = Color(0xFFFFFBFF),
            surfaceVariant = Color(0xFFEAE6F2),
            onBackground = Color(0xFF231D30),
            onSurface = Color(0xFF231D30)
        )
    }

    BackHandler(enabled = active || tab != 0) {
        if (tab != 0) tab = 0
        else confirmation = "discard"
    }

    CompositionLocalProvider(
        LocalEnglish provides english,
        LocalMotion provides motion
    ) {
        MaterialTheme(colorScheme = scheme) {
            Scaffold(
                containerColor = scheme.background,
                topBar = {
                    Surface(color = scheme.background.copy(alpha = 0.95f)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "SCORE CLUB",
                                color = scheme.primary,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp,
                                fontSize = 14.sp
                            )
                            TextButton(onClick = { help = true }) {
                                Text(tr("?  Помощь", "?  Help"))
                            }
                        }
                    }
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = scheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        val titles = listOf(
                            tr("Игра", "Game"),
                            tr("История", "History"),
                            tr("Настройки", "Settings")
                        )
                        val symbols = listOf("▶", "≡", "⚙")
                        titles.forEachIndexed { index, title ->
                            NavigationBarItem(
                                selected = tab == index,
                                onClick = { tab = index },
                                icon = { Text(symbols[index], fontSize = 22.sp) },
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
                            Brush.linearGradient(
                                listOf(
                                    scheme.primary.copy(alpha = 0.20f),
                                    scheme.background,
                                    scheme.secondary.copy(alpha = 0.14f)
                                )
                            )
                        )
                ) {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            (fadeIn(tween(if (motion) 250 else 0)) +
                                slideInVertically(
                                    tween(if (motion) 250 else 0)
                                ) { it / 18 }) togetherWith
                                fadeOut(tween(if (motion) 120 else 0))
                        },
                        label = "tabs"
                    ) { current ->
                        when (current) {
                            0 -> if (!active) {
                                SetupPage(
                                    draftName,
                                    { draftName = it },
                                    draftPlayers,
                                    draftRule,
                                    { draftRule = it }
                                ) {
                                    name = draftName.trim()
                                    players = draftPlayers.map { it.trim() }
                                    rule = draftRule
                                    rounds.clear()
                                    rounds.add(newRound(players.size))
                                    finished = false
                                    active = true
                                }
                            } else {
                                GamePage(
                                    name, players, rule, rounds, finished,
                                    onAdd = { rounds.add(newRound(players.size)) },
                                    onRemove = { confirmation = "round" },
                                    onFinish = { confirmation = "finish" },
                                    onReset = {
                                        if (finished) reset()
                                        else confirmation = "discard"
                                    }
                                )
                            }

                            1 -> HistoryPage(
                                history,
                                onOpen = { selected = it },
                                onDelete = {
                                    deleteId = it.id
                                    confirmation = "delete"
                                }
                            )

                            2 -> SettingsPage(
                                theme, {
                                    theme = it
                                    storage.theme = it
                                },
                                accent, {
                                    accent = it
                                    storage.accent = it
                                },
                                english, {
                                    english = it
                                    storage.english = it
                                },
                                motion, {
                                    motion = it
                                    storage.animations = it
                                },
                                history.size,
                                { confirmation = "clear" }
                            )
                        }
                    }
                }
            }

            if (help) HelpDialog { help = false }

            selected?.let { game ->
                HistoryDetails(game) { selected = null }
            }

            confirmation?.let { action ->
                val title = when (action) {
                    "finish" -> tr("Завершить партию?", "Finish game?")
                    "discard" -> tr("Закрыть партию?", "Close game?")
                    "round" -> tr("Удалить последний раунд?", "Delete last round?")
                    "clear" -> tr("Очистить историю?", "Clear history?")
                    else -> tr("Удалить запись?", "Delete entry?")
                }
                val message = when (action) {
                    "finish" -> tr(
                        "Результат сохранится в истории. Изменить очки после завершения нельзя.",
                        "The result will be saved to history. Scores cannot be edited afterwards."
                    )
                    "discard" -> if (finished) tr(
                        "Результат уже сохранён в истории.",
                        "The result is already saved to history."
                    ) else tr(
                        "Незавершённая партия будет удалена.",
                        "The unfinished game will be discarded."
                    )
                    "round" -> tr(
                        "Очки последнего раунда будут удалены, итоги пересчитаются.",
                        "The last round will be deleted and totals recalculated."
                    )
                    else -> tr(
                        "Это действие нельзя отменить.",
                        "This action cannot be undone."
                    )
                }

                AlertDialog(
                    onDismissRequest = { confirmation = null },
                    title = { Text(title) },
                    text = { Text(message) },
                    confirmButton = {
                        TextButton(onClick = {
                            when (action) {
                                "finish" -> {
                                    val valid = rounds.isNotEmpty() &&
                                        rounds.all { row ->
                                            row.all { parseScore(it) != null }
                                        }
                                    if (valid && !finished) {
                                        val game = SavedGame(
                                            UUID.randomUUID().toString(),
                                            name,
                                            System.currentTimeMillis(),
                                            players.toList(),
                                            rounds.map { row ->
                                                row.map { parseScore(it)!! }
                                            },
                                            rule
                                        )
                                        updateHistory(listOf(game) + history)
                                        finished = true
                                    }
                                }
                                "discard" -> reset()
                                "round" -> if (!finished && rounds.size > 1) {
                                    rounds.removeAt(rounds.lastIndex)
                                }
                                "clear" -> updateHistory(emptyList())
                                "delete" -> updateHistory(
                                    history.filterNot { it.id == deleteId }
                                )
                            }
                            confirmation = null
                        }) { Text(tr("Подтвердить", "Confirm")) }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmation = null }) {
                            Text(tr("Отмена", "Cancel"))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        content = content
    )
}

@Composable
private fun GlowPanel(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val strength by animateFloatAsState(
        targetValue = if (highlighted) 0.28f else 0.08f,
        animationSpec = tween(if (motion) 350 else 0),
        label = "glowStrength"
    )
    val shape = RoundedCornerShape(24.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (highlighted) 14.dp else 3.dp,
                shape = shape,
                ambientColor = colors.primary,
                spotColor = colors.primary
            ),
        shape = shape,
        color = colors.surface,
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    colors.primary.copy(alpha = if (highlighted) 0.7f else 0.3f),
                    colors.secondary.copy(alpha = 0.1f),
                    colors.primary.copy(alpha = 0.15f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            colors.primary.copy(alpha = strength),
                            Color.Transparent,
                            colors.secondary.copy(alpha = strength * 0.6f)
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun Heading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.ExtraBold
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
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Choice(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        }
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RadioButton(selected = selected, onClick = null)
            Text(text, modifier = Modifier.weight(1f))
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
    val cleanNames = names.map { it.trim() }
    val nameValid = name.trim().length in 1..30
    val errors = cleanNames.map { player ->
        when {
            player.isEmpty() -> tr("Введите имя", "Enter a name")
            player.length > 15 -> tr("Максимум 15 символов", "Maximum 15 characters")
            cleanNames.count { it.equals(player, true) } > 1 ->
                tr("Имя уже используется", "Name already used")
            else -> null
        }
    }

    Page {
        Heading(
            tr("Время играть", "Time to play"),
            tr("Соберите компанию. Мы посчитаем очки.",
                "Gather your friends. We'll keep score.")
        )

        GlowPanel(highlighted = true) {
            Text(tr("01  Название игры", "01  Game name"), fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = name,
                onValueChange = onName,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(tr("Например, Уно", "For example, UNO")) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                isError = name.isNotEmpty() && !nameValid,
                supportingText = {
                    Text(tr("От 1 до 30 символов", "1–30 characters"))
                }
            )
        }

        GlowPanel {
            Text(
                tr("02  Участники", "02  Players") + " · ${names.size}/8",
                fontWeight = FontWeight.Bold
            )

            names.forEachIndexed { index, value ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier.size(32.dp).background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            CircleShape
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${index + 1}", color = MaterialTheme.colorScheme.primary)
                    }

                    OutlinedTextField(
                        value = value,
                        onValueChange = { names[index] = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(tr("Игрок", "Player") + " ${index + 1}") },
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
                            modifier = Modifier.size(48.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("×", fontSize = 24.sp) }
                    }
                }
            }

            OutlinedButton(
                onClick = { names.add("") },
                enabled = names.size < 8,
                modifier = Modifier.fillMaxWidth()
            ) { Text(tr("+ Добавить игрока", "+ Add player")) }
        }

        GlowPanel {
            Text(tr("03  Условие победы", "03  Winning rule"), fontWeight = FontWeight.Bold)
            Choice(
                tr("Больше очков — лучше", "Higher score wins"),
                rule == WinRule.MAX
            ) { onRule(WinRule.MAX) }
            Choice(
                tr("Меньше очков — лучше", "Lower score wins"),
                rule == WinRule.MIN
            ) { onRule(WinRule.MIN) }
        }

        MainButton(
            tr("Начать партию", "Start game"),
            nameValid && names.size in 2..8 && errors.all { it == null },
            onStart
        )
    }
}

@Composable
private fun ScoreNumber(value: Long?) {
    val motion = LocalMotion.current
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            val duration = if (motion) 260 else 0
            val increasing = (targetState ?: 0L) >= (initialState ?: 0L)

            (slideInVertically(tween(duration)) {
                if (increasing) it else -it
            } + fadeIn(tween(duration)) + scaleIn(
                tween(duration), initialScale = 0.9f
            )) togetherWith (
                slideOutVertically(tween(duration)) {
                    if (increasing) -it else it
                } + fadeOut(tween(duration))
            )
        },
        label = "scoreCounter"
    ) { score ->
        Text(
            score?.toString() ?: "—",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun RoundRow(
    index: Int,
    row: SnapshotStateList<String>,
    finished: Boolean
) {
    val motion = LocalMotion.current
    var visible by remember { mutableStateOf(!motion) }
    LaunchedEffect(Unit) {
        delay(16)
        visible = true
    }

    AnimatedVisibility(
        visible = visible || !motion,
        enter = fadeIn(tween(240)) + expandVertically(tween(280))
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${index + 1}".padStart(2, '0'),
                modifier = Modifier.width(36.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            row.forEachIndexed { column, value ->
                Column(
                    modifier = Modifier.width(108.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { row[column] = it },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = finished,
                        singleLine = true,
                        isError = parseScore(value) == null,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )

                    if (!finished) {
                        TextButton(
                            onClick = {
                                row[column] = if (value.startsWith("-")) {
                                    value.removePrefix("-")
                                } else {
                                    "-" + value.removePrefix("+")
                                }
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("±", fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GamePage(
    name: String,
    players: List<String>,
    rule: WinRule,
    rounds: SnapshotStateList<SnapshotStateList<String>>,
    finished: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onFinish: () -> Unit,
    onReset: () -> Unit
) {
    val english = LocalEnglish.current
    val scores = totals(rounds, players.size)
    val valid = rounds.isNotEmpty() && scores.all { it != null }
    val best = if (!valid) null else if (rule == WinRule.MAX) {
        scores.filterNotNull().maxOrNull()
    } else {
        scores.filterNotNull().minOrNull()
    }

    Page {
        Heading(
            name,
            if (finished) tr(
                "Завершено · сохранено в истории",
                "Finished · saved to history"
            ) else tr(
                "Игроков: ${players.size} · Раундов: ${rounds.size}",
                "Players: ${players.size} · Rounds: ${rounds.size}"
            )
        )

        Text(
            if (rule == WinRule.MAX) {
                tr("Побеждает большая сумма", "Highest total wins")
            } else {
                tr("Побеждает меньшая сумма", "Lowest total wins")
            },
            color = MaterialTheme.colorScheme.primary
        )

        AnimatedVisibility(
            visible = finished,
            enter = fadeIn(tween(if (LocalMotion.current) 350 else 0)) +
                expandVertically(tween(if (LocalMotion.current) 350 else 0))
        ) {
            GlowPanel(highlighted = true) {
                Text(
                    tr("ИТОГИ ПАРТИИ", "GAME RESULTS"),
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
                Text(
                    winner(players, scores.filterNotNull(), rule, english),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            players.forEachIndexed { index, player ->
                val leading = best != null && scores[index] == best
                GlowPanel(
                    modifier = Modifier.width(166.dp),
                    highlighted = leading
                ) {
                    Text(player, fontWeight = FontWeight.Bold)
                    ScoreNumber(scores[index])
                    Text(
                        when {
                            scores[index] == null -> tr("Ошибка ввода", "Invalid score")
                            leading && finished -> tr("Лучший результат", "Best result")
                            leading -> tr("В лидерах", "Leading")
                            else -> tr("Общий счёт", "Total score")
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        GlowPanel {
            Text(tr("Очки по раундам", "Round scores"), fontWeight = FontWeight.Bold)
            Text(
                tr("Листайте таблицу вправо", "Swipe the table horizontally"),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                Modifier.horizontalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("№", modifier = Modifier.width(36.dp))
                    players.forEach {
                        Text(
                            it,
                            modifier = Modifier.width(108.dp),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                rounds.forEachIndexed { index, row ->
                    // Удаление поддерживается только с конца списка.
                    key(index) {
                        RoundRow(index, row, finished)
                    }
                }
            }

            if (!valid) {
                Text(
                    tr(
                        "Заполните все поля целыми числами от −9999 до 9999.",
                        "Fill every field with a whole number from −9999 to 9999."
                    ),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (!finished) {
            OutlinedButton(
                onClick = onAdd,
                enabled = valid,
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
            ) { Text(tr("+ Добавить раунд", "+ Add round")) }

            if (rounds.size > 1) {
                TextButton(
                    onClick = onRemove,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(tr("Удалить последний раунд", "Delete last round"))
                }
            }

            MainButton(tr("Завершить партию", "Finish game"), valid, onFinish)

            TextButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Отменить партию", "Discard game"))
            }
        } else {
            MainButton(tr("Новая партия", "New game"), onClick = onReset)
        }
    }
}

@Composable
private fun HistoryPage(
    games: List<SavedGame>,
    onOpen: (SavedGame) -> Unit,
    onDelete: (SavedGame) -> Unit
) {
    val english = LocalEnglish.current

    Page {
        Heading(
            tr("История", "History"),
            tr("Ваши партии и победы", "Your games and victories")
        )

        if (games.isEmpty()) {
            GlowPanel {
                Text(
                    tr("Здесь появятся ваши игры", "Your games will appear here"),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(tr(
                    "Завершите первую партию, чтобы сохранить результат.",
                    "Finish your first game to save its result."
                ))
            }
        }

        games.forEach { game ->
            key(game.id) {
                GlowPanel {
                    Text(
                        dateText(game.date),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(game.name, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text(
                        winner(game.players, savedTotals(game), game.rule, english),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(tr(
                        "Игроков: ${game.players.size} · Раундов: ${game.rounds.size}",
                        "Players: ${game.players.size} · Rounds: ${game.rounds.size}"
                    ))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { onOpen(game) }) {
                            Text(tr("Подробнее", "Details"))
                        }
                        TextButton(onClick = { onDelete(game) }) {
                            Text(
                                tr("Удалить", "Delete"),
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
private fun HistoryDetails(game: SavedGame, onDismiss: () -> Unit) {
    val scores = savedTotals(game)
    val english = LocalEnglish.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(game.name) },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(dateText(game.date))
                Text(
                    winner(game.players, scores, game.rule, english),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (game.rule == WinRule.MAX) {
                        tr("Больше очков — лучше", "Higher score wins")
                    } else {
                        tr("Меньше очков — лучше", "Lower score wins")
                    }
                )
                game.players.forEachIndexed { index, player ->
                    Text("$player: ${scores[index]}")
                }
                HorizontalDivider()
                Text(tr("Раунды", "Rounds"), fontWeight = FontWeight.Bold)
                game.rounds.forEachIndexed { index, row ->
                    Text(
                        tr("Раунд", "Round") + " ${index + 1}\n" +
                            game.players.mapIndexed { column, player ->
                                "$player: ${row[column]}"
                            }.joinToString(" · ")
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(tr("Закрыть", "Close")) }
        }
    )
}

@Composable
private fun SettingsPage(
    theme: String,
    onTheme: (String) -> Unit,
    accent: Int,
    onAccent: (Int) -> Unit,
    english: Boolean,
    onEnglish: (Boolean) -> Unit,
    motion: Boolean,
    onMotion: (Boolean) -> Unit,
    historyCount: Int,
    onClear: () -> Unit
) {
    Page {
        Heading(
            tr("Настройки", "Settings"),
            tr("Ваш стиль. Ваша игра.", "Your style. Your game.")
        )

        GlowPanel(highlighted = true) {
            Text(tr("Язык интерфейса", "Interface language"), fontWeight = FontWeight.Bold)
            Choice("Русский", !english) { onEnglish(false) }
            Choice("English", english) { onEnglish(true) }
        }

        GlowPanel {
            Text(tr("Тема", "Theme"), fontWeight = FontWeight.Bold)
            Choice(tr("Как на устройстве", "System"), theme == "system") {
                onTheme("system")
            }
            Choice(tr("Светлая", "Light"), theme == "light") {
                onTheme("light")
            }
            Choice(tr("Тёмная", "Dark"), theme == "dark") {
                onTheme("dark")
            }
        }

        GlowPanel {
            Text(tr("Акцентный цвет", "Accent color"), fontWeight = FontWeight.Bold)
            listOf(
                tr("Аметист", "Amethyst"),
                tr("Океан", "Ocean"),
                tr("Изумруд", "Emerald")
            ).forEachIndexed { index, title ->
                Choice(title, accent == index) { onAccent(index) }
            }
        }

        GlowPanel {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(tr("Анимации", "Animations"), fontWeight = FontWeight.Bold)
                    Text(
                        tr(
                            "Переходы, изменение счёта и появление раундов",
                            "Transitions, score changes and round appearance"
                        ),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = motion, onCheckedChange = onMotion)
            }
        }

        GlowPanel {
            Text(tr("История игр", "Game history"), fontWeight = FontWeight.Bold)
            Text(tr("Сохранено партий: ", "Saved games: ") + historyCount)
            OutlinedButton(
                onClick = onClear,
                enabled = historyCount > 0,
                modifier = Modifier.fillMaxWidth()
            ) { Text(tr("Очистить историю", "Clear history")) }
        }
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Во что поиграть?", "What can you play?")) },
        text = {
            Column(
                Modifier.heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    tr(
                        "Приложение складывает очки по раундам и сравнивает итоговые суммы. Правила игр и особые условия победы нужно проверять самостоятельно.",
                        "The app adds round scores and compares totals. You must check game rules and special winning conditions yourself."
                    )
                )

                HelpItem(
                    "UNO",
                    tr(
                        "При классическом подсчёте записывайте очки, полученные победителем раздачи. Выберите «Больше очков — лучше». Когда достигнут нужный порог, завершите партию вручную.",
                        "With classic scoring, enter the points earned by the hand winner. Select “Higher score wins”. Finish manually when the target is reached."
                    )
                )

                HelpItem(
                    tr("Скрэббл / Эрудит", "Scrabble"),
                    tr(
                        "Записывайте очки за ходы. Выберите большую сумму. Штрафы и финальные поправки внесите отдельным раундом.",
                        "Record turn scores. Select the highest total. Enter penalties and final adjustments as a separate round."
                    )
                )

                HelpItem(
                    tr("Домино", "Dominoes"),
                    tr(
                        "Для варианта со штрафными очками за оставшиеся кости выберите меньшую сумму. Другие варианты могут использовать иной подсчёт.",
                        "For variants that assign penalty points for remaining tiles, select the lowest total. Other variants may use different scoring."
                    )
                )

                HelpItem(
                    tr("Рамми", "Rummy"),
                    tr(
                        "Подходит для записи результатов раздач. Выберите большую или меньшую сумму в зависимости от правил вашего варианта.",
                        "Track hand results. Choose the highest or lowest total according to the rules of your variant."
                    )
                )

                HelpItem(
                    tr("Ятцы / покер на костях", "Yahtzee / dice poker"),
                    tr(
                        "Записывайте очки за категории отдельными раундами, бонусы — отдельно. Выберите большую сумму. Проверки категорий в приложении нет.",
                        "Record category scores as rounds and add bonuses separately. Select the highest total. The app does not validate categories."
                    )
                )

                HelpItem(
                    tr("Викторины и домашние турниры", "Quizzes and home tournaments"),
                    tr(
                        "Начисляйте очки за ответы или задания. Для командной игры используйте названия команд вместо имён.",
                        "Award points for answers or challenges. For team games, enter team names instead of player names."
                    )
                )

                HorizontalDivider()

                HelpItem(
                    tr("Как пользоваться", "How to use"),
                    tr(
                        "1. Создайте партию и добавьте 2–8 участников.\n" +
                            "2. Выберите условие победы.\n" +
                            "3. Введите очки для каждого участника. Если очков нет — оставьте 0.\n" +
                            "4. Кнопка ± меняет знак числа.\n" +
                            "5. Добавляйте раунды и исправляйте очки до завершения.\n" +
                            "6. Завершите партию — результат появится в истории.",
                        "1. Create a game with 2–8 players.\n" +
                            "2. Select the winning rule.\n" +
                            "3. Enter each player's score. Leave 0 if they earned no points.\n" +
                            "4. Use ± to change a number's sign.\n" +
                            "5. Add rounds and edit scores before finishing.\n" +
                            "6. Finish the game to save it to history."
                    )
                )

                HelpItem(
                    tr("Сохранение", "Saving"),
                    tr(
                        "История и настройки сохраняются на этом устройстве. Незавершённая партия хранится в памяти и может потеряться при закрытии процесса Android. Удаление приложения удаляет его данные.",
                        "History and settings are saved on this device. An unfinished game is kept in memory and may be lost if Android closes the process. Uninstalling the app removes its data."
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Понятно", "Got it"))
            }
        }
    )
}

@Composable
private fun HelpItem(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(description, style = MaterialTheme.typography.bodyMedium)
    }
}
