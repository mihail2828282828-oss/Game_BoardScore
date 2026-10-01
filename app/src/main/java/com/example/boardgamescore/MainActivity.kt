package com.example.boardgamescore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF101019)
private val Panel = Color(0xFF1B1B29)
private val Accent = Color(0xFFB9A0FF)
private val Muted = Color(0xFFA6A4BB)
private val Mint = Color(0xFF88E0C2)

private val PlayerColors = listOf(
    Color(0xFFB9A0FF),
    Color(0xFF88E0C2),
    Color(0xFFFFBF87),
    Color(0xFF8ECFFF),
    Color(0xFFFF9EB5),
    Color(0xFFE2D58C),
    Color(0xFF9FAEFF),
    Color(0xFFD7A3E5)
)

enum class WinRule { MAX, MIN }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Accent,
                    onPrimary = Color(0xFF231344),
                    secondary = Mint,
                    background = Background,
                    surface = Panel,
                    onBackground = Color(0xFFF4F1FF),
                    onSurface = Color(0xFFF4F1FF),
                    onSurfaceVariant = Muted,
                    outline = Color(0xFF494459),
                    error = Color(0xFFFFB4AB)
                )
            ) {
                BoardGameScoreApp()
            }
        }
    }
}

@Composable
fun BoardGameScoreApp() {
    var inGame by remember { mutableStateOf(false) }
    var gameName by remember { mutableStateOf("") }
    var rule by remember { mutableStateOf(WinRule.MAX) }
    var players by remember { mutableStateOf(emptyList<String>()) }

    val rounds = remember {
        mutableStateListOf<SnapshotStateList<String>>()
    }

    var finished by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }

    fun resetGame() {
        inGame = false
        finished = false
        confirmFinish = false
        confirmExit = false
        rounds.clear()
        players = emptyList()
    }

    BackHandler(enabled = inGame) {
        confirmExit = true
    }

    Scaffold(containerColor = Background) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF211A36), Background),
                        endY = 1000f
                    )
                )
                .imePadding()
        ) {
            if (!inGame) {
                SetupScreen { name, selectedRule, names ->
                    gameName = name
                    rule = selectedRule
                    players = names
                    rounds.clear()
                    rounds.add(newRound(names.size))
                    finished = false
                    inGame = true
                }
            } else {
                GameScreen(
                    gameName = gameName,
                    rule = rule,
                    players = players,
                    rounds = rounds,
                    finished = finished,
                    onBack = { confirmExit = true },
                    onAddRound = {
                        rounds.add(newRound(players.size))
                    },
                    onFinish = { confirmFinish = true },
                    onNewGame = { resetGame() }
                )
            }
        }
    }

    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text("Подвести итоги?") },
            text = {
                Text("После завершения изменить очки будет нельзя.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmFinish = false
                        finished = true
                    }
                ) {
                    Text("Завершить")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmFinish = false }) {
                    Text("Продолжить игру")
                }
            }
        )
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Выйти из партии?") },
            text = {
                Text("Текущий счёт будет удалён. История не сохраняется.")
            },
            confirmButton = {
                TextButton(onClick = { resetGame() }) {
                    Text("Выйти")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) {
                    Text("Остаться")
                }
            }
        )
    }
}

private fun newRound(size: Int): SnapshotStateList<String> {
    return mutableStateListOf<String>().apply {
        repeat(size) { add("0") }
    }
}

private fun parseScore(text: String): Int? {
    return text.trim().toIntOrNull()?.takeIf {
        it in -9999..9999
    }
}

private fun totals(
    rounds: List<List<String>>,
    playerCount: Int
): List<Long?> {
    return List(playerCount) { column ->
        val values = rounds.map { parseScore(it[column]) }

        if (values.any { it == null }) {
            null
        } else {
            values.sumOf { it!!.toLong() }
        }
    }
}

private fun resultText(
    rule: WinRule,
    players: List<String>,
    scores: List<Long?>
): String {
    if (scores.any { it == null } || scores.isEmpty()) {
        return "Проверьте введённые очки"
    }

    val values = scores.filterNotNull()
    val best = if (rule == WinRule.MAX) {
        values.maxOrNull()!!
    } else {
        values.minOrNull()!!
    }

    val winners = players.filterIndexed { index, _ ->
        values[index] == best
    }

    return if (winners.size == 1) {
        "Победитель — ${winners.first()}\nИтог: $best"
    } else {
        "Ничья — ${winners.joinToString(", ")}\nИтог: $best"
    }
}

@Composable
private fun SectionCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Panel
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun PlayerAvatar(index: Int) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                PlayerColors[index % PlayerColors.size].copy(alpha = 0.16f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${index + 1}",
            color = PlayerColors[index % PlayerColors.size],
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SectionTitle(number: String, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = number,
            color = Accent,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SetupScreen(
    onStart: (String, WinRule, List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var rule by remember { mutableStateOf(WinRule.MAX) }
    val inputs = remember { mutableStateListOf("", "") }

    val cleanName = name.trim()
    val names = inputs.map { it.trim() }

    val nameValid = cleanName.length in 1..30

    val playerErrors = names.map { playerName ->
        when {
            playerName.isEmpty() -> "Введите имя"
            playerName.length > 15 -> "Не более 15 символов"
            names.count {
                it.equals(playerName, ignoreCase = true)
            } > 1 -> "Имя уже используется"
            else -> null
        }
    }

    val canStart =
        nameValid &&
        names.size in 2..8 &&
        playerErrors.all { it == null }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Text(
            text = "BOARD GAME SCORE",
            color = Accent,
            fontSize = 12.sp,
            letterSpacing = 3.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Играйте.\nСчёт — на нас.",
            fontSize = 34.sp,
            lineHeight = 39.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Соберите компанию и начните новую партию.",
            color = Muted,
            style = MaterialTheme.typography.bodyLarge
        )

        SectionCard {
            SectionTitle("01", "Во что играем?")

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Название игры") },
                placeholder = { Text("Например, Уно") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                isError = name.isNotEmpty() && !nameValid,
                supportingText = {
                    Text(
                        if (name.isNotEmpty() && !nameValid) {
                            "Название должно содержать 1–30 символов"
                        } else {
                            "${cleanName.length}/30"
                        }
                    )
                }
            )
        }

        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle("02", "Кто за столом?")
                Text("${inputs.size}/8", color = Muted)
            }

            inputs.forEachIndexed { index, value ->
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PlayerAvatar(index)

                        OutlinedTextField(
                            value = value,
                            onValueChange = { inputs[index] = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Игрок ${index + 1}") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            isError =
                                value.isNotEmpty() &&
                                playerErrors[index] != null
                        )

                        if (inputs.size > 2) {
                            TextButton(
                                onClick = { inputs.removeAt(index) },
                                modifier = Modifier.size(48.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("×", fontSize = 24.sp)
                            }
                        }
                    }

                    if (value.isNotEmpty() && playerErrors[index] != null) {
                        Text(
                            text = playerErrors[index]!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(
                                start = 50.dp,
                                top = 4.dp
                            )
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { inputs.add("") },
                enabled = inputs.size < 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("+  Добавить игрока")
            }
        }

        SectionCard {
            SectionTitle("03", "Как определить победителя?")

            RuleOption(
                title = "Больше очков",
                subtitle = "Побеждает максимальная сумма",
                selected = rule == WinRule.MAX,
                onClick = { rule = WinRule.MAX }
            )

            RuleOption(
                title = "Меньше очков",
                subtitle = "Побеждает минимальная сумма",
                selected = rule == WinRule.MIN,
                onClick = { rule = WinRule.MIN }
            )
        }

        Button(
            onClick = { onStart(cleanName, rule, names) },
            enabled = canStart,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                "Начать партию",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "Без аккаунтов и интернета. Только ваша игра.",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = Muted,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RuleOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            Accent.copy(alpha = 0.14f)
        } else {
            Background.copy(alpha = 0.5f)
        }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = null
            )

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    subtitle,
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun GameScreen(
    gameName: String,
    rule: WinRule,
    players: List<String>,
    rounds: SnapshotStateList<SnapshotStateList<String>>,
    finished: Boolean,
    onBack: () -> Unit,
    onAddRound: () -> Unit,
    onFinish: () -> Unit,
    onNewGame: () -> Unit
) {
    val scores = totals(rounds, players.size)
    val allValid = rounds.isNotEmpty() &&
        rounds.all { row -> row.all { parseScore(it) != null } }

    val best = if (allValid) {
        if (rule == WinRule.MAX) {
            scores.filterNotNull().maxOrNull()
        } else {
            scores.filterNotNull().minOrNull()
        }
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) {
                Text("‹  Выйти")
            }

            Surface(
                shape = RoundedCornerShape(50.dp),
                color = if (finished) {
                    Mint.copy(alpha = 0.15f)
                } else {
                    Accent.copy(alpha = 0.15f)
                }
            ) {
                Text(
                    text = if (finished) "ЗАВЕРШЕНА" else "ИДЁТ ИГРА",
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 9.dp
                    ),
                    color = if (finished) Mint else Accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        Text(
            text = gameName,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "${players.size} игроков • " +
                "${rounds.size} раундов • " +
                if (rule == WinRule.MAX) {
                    "Побеждает большая сумма"
                } else {
                    "Побеждает меньшая сумма"
                },
            color = Muted
        )

        if (finished) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Mint.copy(alpha = 0.13f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "ИТОГИ ПАРТИИ",
                        color = Mint,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        resultText(rule, players, scores),
                        fontSize = 23.sp,
                        lineHeight = 31.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            text = "Общий счёт",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            players.forEachIndexed { index, player ->
                val leading = best != null && scores[index] == best

                Card(
                    modifier = Modifier.width(150.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (leading) {
                            Color(0xFF302541)
                        } else {
                            Panel
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PlayerAvatar(index)

                        Text(
                            text = player,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = scores[index]?.toString() ?: "—",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlayerColors[index]
                        )

                        Text(
                            text = when {
                                scores[index] == null -> "Ошибка ввода"
                                finished && leading -> "Лучший результат"
                                leading -> "В лидерах"
                                else -> "Общий итог"
                            },
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        SectionCard {
            Text(
                text = "Очки по раундам",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (finished) {
                    "Партия завершена. Счёт доступен только для просмотра."
                } else {
                    "Нажмите на число, чтобы изменить его. Таблица листается вправо."
                },
                color = Muted,
                style = MaterialTheme.typography.bodySmall
            )

            Column(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Раунд",
                        modifier = Modifier.width(54.dp),
                        color = Muted,
                        fontSize = 12.sp
                    )

                    players.forEachIndexed { index, name ->
                        Text(
                            text = name,
                            modifier = Modifier.width(100.dp),
                            color = PlayerColors[index],
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }

                rounds.forEachIndexed { roundIndex, row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${roundIndex + 1}".padStart(2, '0'),
                            modifier = Modifier.width(54.dp),
                            color = Muted,
                            fontWeight = FontWeight.Bold
                        )

                        row.forEachIndexed { playerIndex, value ->
                            OutlinedTextField(
                                value = value,
                                onValueChange = {
                                    row[playerIndex] = it
                                },
                                modifier = Modifier.width(100.dp),
                                enabled = !finished,
                                singleLine = true,
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

            if (!allValid) {
                Text(
                    text = "Введите целые числа от −9999 до 9999 во все поля.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (!finished) {
            OutlinedButton(
                onClick = onAddRound,
                enabled = allValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("+  Добавить раунд", fontSize = 16.sp)
            }

            Button(
                onClick = onFinish,
                enabled = allValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    "Завершить партию",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Button(
                onClick = onNewGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    "Начать новую партию",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}
