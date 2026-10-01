package com.example.boardgamescore
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

enum class WinRule { MAX, MIN }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                BoardGameScoreApp()
            }
        }
    }
}

@Composable
fun BoardGameScoreApp() {
    var inGame by remember { mutableStateOf(false) }
    var gameName by remember { mutableStateOf("") }
    var winRule by remember { mutableStateOf(WinRule.MAX) }
    var players by remember { mutableStateOf(listOf<String>()) }
    val rounds = remember { mutableStateListOf<SnapshotStateList<String>>() }
    var isFinished by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            if (!inGame) {
                SetupScreen(
                    onStart = { name, rule, names ->
                        gameName = name
                        winRule = rule
                        players = names
                        rounds.clear()
                        rounds.add(createEmptyRound(names.size))
                        isFinished = false
                        inGame = true
                    }
                )
            } else {
                GameScreen(
                    gameName = gameName,
                    winRule = winRule,
                    players = players,
                    rounds = rounds,
                    isFinished = isFinished,
                    onAddRound = { rounds.add(createEmptyRound(players.size)) },
                    onFinishClick = { showConfirm = true },
                    onNewGame = {
                        inGame = false
                        isFinished = false
                        showResult = false
                    }
                )
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Завершить партию?") },
            text = { Text("После завершения изменить очки будет нельзя.") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    isFinished = true
                    showResult = true
                }) { Text("Да") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Нет") }
            }
        )
    }

    if (showResult && isFinished) {
        val totals = calcTotals(rounds)
        val text = buildResultText(winRule, players, totals)
        AlertDialog(
            onDismissRequest = { showResult = false },
            title = { Text("Результат") },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = { showResult = false }) { Text("OK") }
            }
        )
    }
}

private fun createEmptyRound(size: Int): SnapshotStateList<String> {
    val list = SnapshotStateList<String>()
    repeat(size) { list.add("0") }
    return list
}

private fun parseScore(s: String): Int? {
    val v = s.trim().toIntOrNull() ?: return null
    if (v < -9999 || v > 9999) return null
    return v
}

private fun calcTotals(rounds: List<List<String>>): List<Int> {
    if (rounds.isEmpty()) return emptyList()
    val n = rounds[0].size
    return List(n) { col ->
        rounds.sumOf { parseScore(it[col]) ?: 0 }
    }
}

private fun buildResultText(rule: WinRule, players: List<String>, totals: List<Int>): String {
    val best = if (rule == WinRule.MAX) totals.max() else totals.min()
    val winners = players.filterIndexed { i, _ -> totals[i] == best }
    return if (winners.size == 1) {
        "Победитель: ${winners[0]} ($best)"
    } else {
        "Ничья: ${winners.joinToString(", ")} ($best)"
    }
}

@Composable
fun SetupScreen(onStart: (String, WinRule, List<String>) -> Unit) {
    var name by remember { mutableStateOf("") }
    val nameInputs = remember { mutableStateListOf("", "") }
    var rule by remember { mutableStateOf(WinRule.MAX) }

    val trimmedName = name.trim()
    val nameError = when {
        trimmedName.isEmpty() -> "Введи название"
        trimmedName.length > 30 -> "Максимум 30 символов"
        else -> null
    }

    val trimmedPlayers = nameInputs.map { it.trim() }
    val playersError = when {
        trimmedPlayers.any { it.isEmpty() } -> "Все имена должны быть заполнены"
        trimmedPlayers.any { it.length > 15 } -> "Имя — максимум 15 символов"
        trimmedPlayers.size != trimmedPlayers.distinct().size -> "Имена не должны повторяться"
        trimmedPlayers.size < 2 -> "Нужно от 2 игроков"
        else -> null
    }

    val canStart = nameError == null && playersError == null

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Новая партия", style = MaterialTheme.typography.headlineSmall)
        Text("1. Название")
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Например: Уно") },
            singleLine = true,
            isError = nameError != null,
            modifier = Modifier.fillMaxWidth()
        )
        if (nameError != null) Text(nameError, color = MaterialTheme.colorScheme.error)

        Text("2. Игроки: ${nameInputs.size} / 8")
        nameInputs.forEachIndexed { i, v ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = v,
                    onValueChange = { nameInputs[i] = it },
                    label = { Text("Игрок ${i + 1}") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                if (nameInputs.size > 2) {
                    TextButton(onClick = { nameInputs.removeAt(i) }) { Text("x") }
                }
            }
        }
        if (playersError != null) Text(playersError, color = MaterialTheme.colorScheme.error)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { if (nameInputs.size < 8) nameInputs.add("") },
                enabled = nameInputs.size < 8,
                modifier = Modifier.heightIn(min = 48.dp).weight(1f)
            ) { Text("+ Игрок") }
        }

        Text("3. Победа")
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = rule == WinRule.MAX, onClick = { rule = WinRule.MAX })
            Text("Больше", modifier = Modifier.padding(end = 16.dp))
            RadioButton(selected = rule == WinRule.MIN, onClick = { rule = WinRule.MIN })
            Text("Меньше")
        }

        Button(
            onClick = { onStart(trimmedName, rule, trimmedPlayers) },
            enabled = canStart,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) { Text("Начать игру") }
    }
}

@Composable
fun GameScreen(
    gameName: String,
    winRule: WinRule,
    players: List<String>,
    rounds: SnapshotStateList<SnapshotStateList<String>>,
    isFinished: Boolean,
    onAddRound: () -> Unit,
    onFinishClick: () -> Unit,
    onNewGame: () -> Unit
) {
    val allValid = rounds.isNotEmpty() && rounds.all { r -> r.all { parseScore(it) != null } }
    val totals = calcTotals(rounds)

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(gameName, style = MaterialTheme.typography.headlineSmall)
        Text("Победа: ${if (winRule == WinRule.MAX) "больше" else "меньше"}")

        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Spacer(Modifier.width(70.dp))
                    players.forEach { Text(it, modifier = Modifier.width(80.dp)) }
                }
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rounds.forEachIndexed { r, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Р${r + 1}", modifier = Modifier.width(70.dp))
                            row.forEachIndexed { c, value ->
                                val ok = parseScore(value) != null
                                OutlinedTextField(
                                    value = value,
                                    onValueChange = { row[c] = it },
                                    readOnly = isFinished,
                                    isError = !ok,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(80.dp)
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Итого", modifier = Modifier.width(70.dp))
                        totals.forEach { Text(it.toString(), modifier = Modifier.width(80.dp)) }
                    }
                }
            }
        }

        if (!allValid) Text("Исправь подсвеченные поля: только числа -9999..9999", color = MaterialTheme.colorScheme.error)

        if (!isFinished) {
            Button(onClick = onAddRound, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Добавить раунд")
            }
            Button(
                onClick = onFinishClick,
                enabled = allValid,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("Завершить игру") }
        } else {
            Button(onClick = onNewGame, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Новая партия")
            }
        }
    }
}
