package com.example.boardgamescore

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

enum class WinRule { MAX, MIN }

data class Template(val ru: String, val en: String, val rule: WinRule, val target: Int)

private val Templates = listOf(
  Template("Свободная игра", "Free game", WinRule.MAX, 0),
  Template("Уно — до 500", "UNO — to 500", WinRule.MAX, 500),
  Template("Манчкин — до 10", "Munchkin — to 10", WinRule.MAX, 10),
  Template("Скрэббл / Эрудит", "Scrabble", WinRule.MAX, 0),
  Template("Рамми", "Rummy", WinRule.MAX, 0),
  Template("Домино — штрафы", "Dominoes — penalties", WinRule.MIN, 0),
  Template("Ятцы — кости", "Yahtzee — dice", WinRule.MAX, 0),
  Template("Викторина", "Quiz", WinRule.MAX, 0)
)

data class SavedGame(
  val id: String,
  val name: String,
  val date: Long,
  val players: List<String>,
  val rounds: List<List<Int>>,
  val rule: WinRule,
  val target: Int = 0,
  val notes: String = "",
  val starts: List<Int> = emptyList(),
  val teamMode: Boolean = false,
  val teamAssign: List<Int> = emptyList(),
  val teamNames: List<String> = emptyList()
)

data class Tournament(
  val id: String,
  val name: String,
  val date: Long,
  val players: List<String>,
  val targetWins: Int,
  val wins: List<Int>
)

private val LocalEnglish = staticCompositionLocalOf { false }
private val LocalMotion = staticCompositionLocalOf { true }

@Composable
private fun tr(ru: String, en: String): String {
  return if (LocalEnglish.current) en else ru
}

private val Emojis = listOf("🦊", "🐼", "🦁", "🐸", "🐯", "🦄", "⚡", "🎲", "🌟", "🍀")
private val ChartColors = listOf(
  Color(0xFFB79CFF), Color(0xFF7ADBCB), Color(0xFFFFB87A), Color(0xFF8ECFFF),
  Color(0xFFFF9EB5), Color(0xFFE2D58C), Color(0xFF9FAEFF), Color(0xFFD7A3E5)
)

private class Storage(context: Context) {
  private val prefs = context.getSharedPreferences("board_game_score", Context.MODE_PRIVATE)
  var theme: String
    get() = prefs.getString("theme", "system") ?: "system"
    set(v) { prefs.edit().putString("theme", v).apply() }
  var accent: Int
    get() = prefs.getInt("accent", 0).coerceIn(0, 2)
    set(v) { prefs.edit().putInt("accent", v).apply() }
  var style: Int
    get() = prefs.getInt("style", 0).coerceIn(0, 3)
    set(v) { prefs.edit().putInt("style", v).apply() }
  var animations: Boolean
    get() = prefs.getBoolean("animations", true)
    set(v) { prefs.edit().putBoolean("animations", v).apply() }
  var english: Boolean
    get() = prefs.getBoolean("english", false)
    set(v) { prefs.edit().putBoolean("english", v).apply() }
  var sound: Boolean
    get() = prefs.getBoolean("sound", true)
    set(v) { prefs.edit().putBoolean("sound", v).apply() }

  fun loadHistory(): List<SavedGame> {
    return runCatching {
      val a = JSONArray(prefs.getString("history", "[]") ?: "[]")
      buildList {
        for (i in 0 until a.length()) {
          runCatching {
            val o = a.getJSONObject(i)
            val np = o.getJSONArray("players")
            val nr = o.getJSONArray("rounds")
            val pl = List(np.length()) { np.getString(it) }
            val ro = List(nr.length()) { r ->
              val row = nr.getJSONArray(r)
              List(row.length()) { row.getInt(it) }
            }
            require(pl.size in 2..8 && ro.isNotEmpty())
            val st = if (o.has("starts")) List(o.getJSONArray("starts").length()) { o.getJSONArray("starts").getInt(it) } else List(pl.size) { 0 }
            val ta = if (o.has("teamAssign")) List(o.getJSONArray("teamAssign").length()) { o.getJSONArray("teamAssign").getInt(it) } else List(pl.size) { 0 }
            val tn = if (o.has("teamNames")) List(o.getJSONArray("teamNames").length()) { o.getJSONArray("teamNames").getString(it) } else listOf("A", "B")
            SavedGame(o.getString("id"), o.getString("name"), o.getLong("date"), pl, ro, WinRule.valueOf(o.getString("rule")), o.optInt("target", 0), o.optString("notes", ""), st, o.optBoolean("teamMode", false), ta, tn)
          }.getOrNull()?.let { add(it) }
        }
      }
    }.getOrDefault(emptyList())
  }

  fun saveHistory(g: List<SavedGame>) {
    val a = JSONArray()
    g.forEach { gm ->
      val np = JSONArray()
      gm.players.forEach { np.put(it) }
      val nr = JSONArray()
      gm.rounds.forEach { r ->
        val v = JSONArray()
        r.forEach { v.put(it) }
        nr.put(v)
      }
      val ns = JSONArray()
      gm.starts.forEach { ns.put(it) }
      val ta = JSONArray()
      gm.teamAssign.forEach { ta.put(it) }
      val tn = JSONArray()
      gm.teamNames.forEach { tn.put(it) }
      a.put(JSONObject().apply {
        put("id", gm.id); put("name", gm.name); put("date", gm.date)
        put("players", np); put("rounds", nr); put("rule", gm.rule.name)
        put("target", gm.target); put("notes", gm.notes); put("starts", ns)
        put("teamMode", gm.teamMode); put("teamAssign", ta); put("teamNames", tn)
      })
    }
    prefs.edit().putString("history", a.toString()).apply()
  }

  fun loadTournaments(): List<Tournament> {
    return runCatching {
      val a = JSONArray(prefs.getString("tournaments", "[]") ?: "[]")
      List(a.length()) { i ->
        val o = a.getJSONObject(i)
        val np = o.getJSONArray("players")
        val nw = o.getJSONArray("wins")
        Tournament(o.getString("id"), o.getString("name"), o.getLong("date"), List(np.length()) { np.getString(it) }, o.optInt("targetWins", 3), List(nw.length()) { nw.getInt(it) })
      }
    }.getOrDefault(emptyList())
  }

  fun saveTournaments(t: List<Tournament>) {
    val a = JSONArray()
    t.forEach { x ->
      val np = JSONArray()
      x.players.forEach { np.put(it) }
      val nw = JSONArray()
      x.wins.forEach { nw.put(it) }
      a.put(JSONObject().apply {
        put("id", x.id); put("name", x.name); put("date", x.date)
        put("players", np); put("targetWins", x.targetWins); put("wins", nw)
      })
    }
    prefs.edit().putString("tournaments", a.toString()).apply()
  }
}

class MainActivity : ComponentActivity() {
  override fun onCreate(s: Bundle?) {
    super.onCreate(s)
    val st = Storage(this)
    setContent { ScoreApp(st) }
  }
}

private fun parseScore(t: String): Int? {
  return t.trim().toIntOrNull()?.takeIf { it in -9999..9999 }
}

private fun newRound(n: Int): SnapshotStateList<String> {
  return mutableStateListOf<String>().apply { repeat(n) { add("0") } }
}

private fun totals(rounds: List<List<String>>, starts: List<Int>): List<Long?> {
  if (rounds.isEmpty()) return starts.map { it.toLong() }
  val n = rounds[0].size
  return List(n) { c ->
    val vs = rounds.map { parseScore(it[c]) }
    if (vs.any { it == null }) null else starts.getOrElse(c) { 0 }.toLong() + vs.sumOf { it!!.toLong() }
  }
}

private fun savedTotals(g: SavedGame): List<Long> {
  return List(g.players.size) { c -> g.starts.getOrElse(c) { 0 }.toLong() + g.rounds.sumOf { it[c].toLong() } }
}

private fun winnerText(pl: List<String>, v: List<Long>, rule: WinRule, en: Boolean): String {
  if (v.isEmpty()) return if (en) "No results" else "Нет результатов"
  val best = if (rule == WinRule.MAX) v.maxOrNull()!! else v.minOrNull()!!
  val w = pl.filterIndexed { i, _ -> v[i] == best }
  val t = if (w.size == 1) { if (en) "Winner: " else "Победитель: " } else { if (en) "Tie: " else "Ничья: " }
  return t + w.joinToString(", ")
}

private fun teamWinnerText(names: List<String>, v: List<Long>, rule: WinRule, en: Boolean): String {
  if (v.size < 2) return winnerText(names, v, rule, en)
  return if (v[0] == v[1]) {
    if (en) "Tie: ${names[0]} — ${names[1]}" else "Ничья: ${names[0]} — ${names[1]}"
  } else {
    val winIdx = if (rule == WinRule.MAX) { if (v[0] > v[1]) 0 else 1 } else { if (v[0] < v[1]) 0 else 1 }
    (if (en) "Winner: " else "Победитель: ") + names.getOrElse(winIdx) { "" }
  }
}

private fun dateText(t: Long): String {
  return SimpleDateFormat("dd.MM.yyyy · HH:mm", Locale.getDefault()).format(Date(t))
}

private fun vibrateNow(ctx: Context) {
  runCatching {
    val v = if (Build.VERSION.SDK_INT >= 31) {
      (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
      @Suppress("DEPRECATION") ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(450, VibrationEffect.DEFAULT_AMPLITUDE))
    else { @Suppress("DEPRECATION") v.vibrate(450) }
  }
}

private suspend fun fanfare(on: Boolean) {
  if (!on) return
  runCatching {
    val g = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
    g.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 220); delay(260)
    g.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 220); delay(260)
    g.startTone(ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE, 220); delay(260)
    g.release()
  }
}

@Composable
private fun ScoreApp(s: Storage) {
  val ctx = LocalContext.current
  val scope = rememberCoroutineScope()
  var theme by remember { mutableStateOf(s.theme) }
  var accent by remember { mutableStateOf(s.accent) }
  var style by remember { mutableStateOf(s.style) }
  var motion by remember { mutableStateOf(s.animations) }
  var english by remember { mutableStateOf(s.english) }
  var sound by remember { mutableStateOf(s.sound) }
  var history by remember { mutableStateOf(s.loadHistory()) }
  var tournaments by remember { mutableStateOf(s.loadTournaments()) }
  var tab by remember { mutableStateOf(0) }
  var draftTpl by remember { mutableStateOf(0) }
  var draftName by remember { mutableStateOf("") }
  val draftPlayers = remember { mutableStateListOf("", "") }
  val draftStarts = remember { mutableStateListOf("0", "0") }
  val draftEmojis = remember { mutableStateListOf("🦊", "🐼") }
  var draftRule by remember { mutableStateOf(WinRule.MAX) }
  var draftTarget by remember { mutableStateOf("0") }
  var draftTeamMode by remember { mutableStateOf(false) }
  val draftTeams = remember { mutableStateListOf(0, 0) }
  var draftTeamA by remember { mutableStateOf("Команда А") }
  var draftTeamB by remember { mutableStateOf("Команда Б") }
  var draftNotes by remember { mutableStateOf("") }
  // примитивы:
var active by rememberSaveable { mutableStateOf(false) }
var finished by rememberSaveable { mutableStateOf(false) }
var turn by rememberSaveable { mutableStateOf(0) }
// списки rounds/players/starts — в GameViewModel : ViewModel,
// иначе пересоздание Activity сбросит партию в ноль
  var name by remember { mutableStateOf("") }
  var players by remember { mutableStateOf(emptyList<String>()) }
  var emojis by remember { mutableStateOf(emptyList<String>()) }
  var starts by remember { mutableStateOf(emptyList<Int>()) }
  var rule by remember { mutableStateOf(WinRule.MAX) }
  var target by remember { mutableStateOf(0) }
  var notes by remember { mutableStateOf("") }
  var teamMode by remember { mutableStateOf(false) }
  var teamAssign by remember { mutableStateOf(emptyList<Int>()) }
  var teamNames by remember { mutableStateOf(listOf("A", "B")) }
  val undoStack = remember { mutableStateListOf<List<List<String>>>() }
  var big by remember { mutableStateOf(false) }
  var confirm by remember { mutableStateOf<String?>(null) }
  var deleteId by remember { mutableStateOf<String?>(null) }
  var selected by remember { mutableStateOf<SavedGame?>(null) }
  var help by remember { mutableStateOf(false) }

  fun pushUndo() {
    undoStack.add(rounds.map { it.toList() })
    if (undoStack.size > 40) undoStack.removeAt(0)
  }

  fun restoreUndo() {
    if (undoStack.isEmpty()) return
    val last = undoStack.removeAt(undoStack.lastIndex)
    rounds.clear()
    last.forEach { r -> rounds.add(mutableStateListOf<String>().apply { addAll(r) }) }
  }

  fun resetGame() {
    active = false; finished = false; players = emptyList(); rounds.clear()
    undoStack.clear(); turn = 0; big = false
    draftName = ""; draftPlayers.clear(); draftPlayers.addAll(listOf("", ""))
    draftStarts.clear(); draftStarts.addAll(listOf("0", "0"))
    draftEmojis.clear(); draftEmojis.addAll(listOf("🦊", "🐼"))
    draftTeams.clear(); draftTeams.addAll(listOf(0, 0))
    draftRule = WinRule.MAX; draftTarget = "0"; draftTeamMode = false; draftNotes = ""
  }

  fun doFinish() {
    if (finished || !active) return
    val sc = totals(rounds, starts)
    if (sc.any { it == null }) return
    val g = SavedGame(UUID.randomUUID().toString(), name, System.currentTimeMillis(), players.toList(), rounds.map { r -> r.map { parseScore(it)!! } }, rule, target, notes, starts.toList(), teamMode, teamAssign.toList(), teamNames.toList())
    history = listOf(g) + history
    s.saveHistory(history)
    finished = true
    vibrateNow(ctx)
    scope.launch { fanfare(sound) }
    if (tournaments.isNotEmpty() && !teamMode) {
      val vs = sc.filterNotNull()
      val best = if (rule == WinRule.MAX) vs.maxOrNull()!! else vs.minOrNull()!!
      val wi = players.indices.filter { vs[it] == best }
      tournaments = tournaments.map { t ->
        val same = t.players.size == players.size && t.players.zip(players).all { (a, b) -> a.equals(b, true) }
        if (same) {
          val nw = t.wins.toMutableList()
          wi.forEach { if (it < nw.size) nw[it]++ }
          t.copy(wins = nw)
        } else t
      }
      s.saveTournaments(tournaments)
    }
  }

  val useDark = when (theme) {
    "light" -> false
    "dark" -> true
    "amoled" -> true
    else -> isSystemInDarkTheme()
  }
  val useAmoled = theme == "amoled"
  val darkAcc = listOf(Color(0xFFC6A8FF), Color(0xFF8ECDFF), Color(0xFF88E3BF))
  val lightAcc = listOf(Color(0xFF7040B0), Color(0xFF175DA6), Color(0xFF176B4C))
  val primary by animateColorAsState(
    targetValue = if (useDark) darkAcc[accent] else lightAcc[accent],
    animationSpec = tween(if (motion) 350 else 0),
    label = "ac"
  )
  val scheme = if (useDark) {
    darkColorScheme(primary = primary, onPrimary = Color(0xFF171021), secondary = Color(0xFF7ADBCB), background = if (useAmoled) Color.Black else Color(0xFF101019), surface = if (useAmoled) Color(0xFF0D0D12) else Color(0xFF1B1B29), surfaceVariant = Color(0xFF292A3A), onBackground = Color(0xFFF5F1FF), onSurface = Color(0xFFF5F1FF))
  } else {
    lightColorScheme(primary = primary, onPrimary = Color.White, secondary = Color(0xFF167D80), background = Color(0xFFF5F3FC), surface = Color.White, surfaceVariant = Color(0xFFEAE6F2), onBackground = Color(0xFF231D30), onSurface = Color(0xFF231D30))
  }
  val bg = when (style) {
    1 -> Brush.linearGradient(listOf(primary.copy(0.30f), scheme.background, Color(0xFFFF9E7A).copy(0.18f)))
    2 -> Brush.linearGradient(listOf(Color(0xFF4ADE80).copy(0.22f), scheme.background, primary.copy(0.16f)))
    else -> Brush.linearGradient(listOf(primary.copy(0.22f), scheme.background, scheme.secondary.copy(0.14f)))
  }

  BackHandler(enabled = big || active || tab != 0) {
  if (big) big = false
  else if (tab != 0) tab = 0
  else confirm = "discard"
  }

  CompositionLocalProvider(LocalEnglish provides english, LocalMotion provides motion) {
    MaterialTheme(colorScheme = scheme) {
      Scaffold(
        containerColor = scheme.background,
        topBar = {
          Surface(color = scheme.background.copy(0.95f)) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
              Text("SCORE CLUB", color = scheme.primary, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, fontSize = 14.sp)
              TextButton(onClick = { help = true }) { Text(tr("?  Помощь", "?  Help")) }
            }
          }
        },
        bottomBar = {
          NavigationBar(containerColor = scheme.surface, tonalElevation = 0.dp) {
            val titles = listOf(tr("Игра", "Game"), tr("История", "History"), tr("Статы", "Stats"), tr("Настройки", "Settings"))
            val icons = listOf("▶", "≡", "★", "⚙")
            titles.forEachIndexed { i, t ->
              NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Text(icons[i], fontSize = 20.sp) }, label = { Text(t) })
            }
          }
        }
      ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad).imePadding().background(bg)) {
          AnimatedContent(targetState = tab, transitionSpec = { fadeIn(tween(if (motion) 220 else 0)) togetherWith fadeOut(tween(if (motion) 120 else 0)) }, label = "tabs") { c ->
            when (c) {
              0 -> {
                if (!active) {
                  SetupPage(draftTpl, { draftTpl = it; draftRule = Templates[it].rule; draftTarget = Templates[it].target.toString() }, draftName, { draftName = it }, draftPlayers, draftStarts, draftEmojis, draftTeams, draftRule, { draftRule = it }, draftTarget, { draftTarget = it }, draftTeamMode, { draftTeamMode = it }, draftTeamA, { draftTeamA = it }, draftTeamB, { draftTeamB = it }, draftNotes, { draftNotes = it }) {
                    name = draftName.trim()
                    players = draftPlayers.map { it.trim() }
                    starts = draftStarts.map { it.trim().toIntOrNull()?.coerceIn(-9999, 9999) ?: 0 }
                    emojis = draftEmojis.toList()
                    rule = draftRule
                    target = draftTarget.trim().toIntOrNull()?.coerceIn(0, 9999) ?: 0
                    notes = draftNotes.trim()
                    teamMode = draftTeamMode
                    teamAssign = draftTeams.toList()
                    teamNames = listOf(draftTeamA.trim().ifEmpty { "A" }, draftTeamB.trim().ifEmpty { "B" })
                    rounds.clear()
                    rounds.add(newRound(players.size))
                    undoStack.clear(); finished = false; active = true; turn = 0; big = false
                  }
                } else {
                  if (big) {
                    BigScreen(name, players, emojis, totals(rounds, starts), finished, rule, teamMode, teamAssign, teamNames, { big = false }, { confirm = "finish" }, { resetGame() })
                  } else {
                    GamePage(name, players, emojis, starts, rule, target, teamMode, teamAssign, teamNames, rounds, finished, turn, { turn = it }, { big = true }, { pushUndo(); rounds.add(newRound(players.size)) }, { confirm = "round" }, { restoreUndo() }, undoStack.isNotEmpty(), { pushUndo() }, { confirm = "finish" }, { doFinish() }, { if (finished) resetGame() else confirm = "discard" })
                  }
                }
              }
              1 -> {
                HistoryPage(history, { selected = it }, { deleteId = it.id; confirm = "delete" }, tournaments, { tournaments = it; s.saveTournaments(it) })
              }
              2 -> StatsPage(history)
              else -> {
                SettingsPage(theme, { theme = it; s.theme = it }, accent, { accent = it; s.accent = it }, style, { style = it; s.style = it }, english, { english = it; s.english = it }, motion, { motion = it; s.animations = it }, sound, { sound = it; s.sound = it }, history.size, { confirm = "clear" }, tournaments.size)
              }
            }
          }
        }
      }
      if (help) HelpDialog { help = false }
      selected?.let { HistoryDetails(it) { selected = null } }
      confirm?.let { a ->
        val title = when (a) {
          "round" -> tr("Удалить последний раунд?", "Delete last round?")
          "clear" -> tr("Очистить историю?", "Clear history?")
          "delete" -> tr("Удалить запись?", "Delete entry?")
          "discard" -> tr("Закрыть партию?", "Close game?")
          else -> tr("Завершить партию?", "Finish game?")
        }
        val msg = if (a == "discard" && !finished) tr("Незавершённая партия будет удалена.", "Unfinished game will be discarded.") else tr("Это действие нельзя отменить.", "This cannot be undone.")
        AlertDialog(
          onDismissRequest = { confirm = null },
          title = { Text(title) },
          text = { Text(msg) },
          confirmButton = {
            TextButton(onClick = {
              when (a) {
                "finish" -> doFinish()
                "discard" -> resetGame()
                "round" -> if (rounds.size > 1) { pushUndo(); rounds.removeAt(rounds.lastIndex) }
                "clear" -> { history = emptyList(); s.saveHistory(history) }
                "delete" -> { history = history.filterNot { it.id == deleteId }; s.saveHistory(history) }
              }
              confirm = null
            }) { Text(tr("Подтвердить", "Confirm")) }
          },
          dismissButton = { TextButton(onClick = { confirm = null }) { Text(tr("Отмена", "Cancel")) } }
        )
      }
    }
  }
}

@Composable
private fun Page(c: @Composable ColumnScope.() -> Unit) {
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp), content = c)
}

@Composable
private fun GlowPanel(
  mod: Modifier = Modifier,
  hi: Boolean = false,
  c: @Composable ColumnScope.() -> Unit
) {
  val cs = MaterialTheme.colorScheme
  val mo = LocalMotion.current
  val glow by animateFloatAsState(
    targetValue = if (hi) 0.18f else 0.05f,
    animationSpec = tween(if (mo) 300 else 0),
    label = "glow"
  )
  val shape = RoundedCornerShape(24.dp)

  Surface(
    modifier = mod
      .fillMaxWidth()
      .shadow(
        elevation = if (hi) 8.dp else 2.dp,
        shape = shape,
        ambientColor = cs.primary.copy(alpha = 0.18f),
        spotColor = cs.primary.copy(alpha = 0.18f)
      ),
    shape = shape,
    color = cs.surface,
    border = BorderStroke(
      1.dp,
      cs.primary.copy(alpha = if (hi) 0.30f else 0.10f)
    )
  ) {
    Column(
      modifier = Modifier
        .background(
          Brush.linearGradient(
            listOf(
              cs.primary.copy(alpha = glow),
              Color.Transparent,
              cs.secondary.copy(alpha = glow * 0.45f)
            )
          )
        )
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      content = c
    )
  }
}

@Composable
private fun Heading(t: String, s: String) {
  Column(
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Text(
      text = t,
      fontSize = 30.sp,
      lineHeight = 36.sp,
      fontWeight = FontWeight.ExtraBold,
      color = MaterialTheme.colorScheme.onBackground
    )
    Text(
      text = s,
      fontSize = 14.sp,
      lineHeight = 20.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
private fun MainButton(t: String, e: Boolean = true, onClick: () -> Unit) {
  Button(onClick = onClick, enabled = e, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(18.dp)) {
    Text(t, fontSize = 16.sp, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun Choice(
  text: String,
  sel: Boolean,
  onClick: () -> Unit
) {
  val colors = MaterialTheme.colorScheme

  Surface(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = if (sel) {
      colors.primary.copy(alpha = 0.10f)
    } else {
      colors.surfaceVariant.copy(alpha = 0.25f)
    },
    border = BorderStroke(
      width = 1.dp,
      color = if (sel) {
        colors.primary.copy(alpha = 0.55f)
      } else {
        colors.outline.copy(alpha = 0.15f)
      }
    )
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      RadioButton(
        selected = sel,
        onClick = onClick
      )
      Text(
        text = text,
        modifier = Modifier.weight(1f),
        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal
      )
    }
  }
}

@Composable
private fun SetupPage(tpl: Int, onTpl: (Int) -> Unit, name: String, onName: (String) -> Unit, names: SnapshotStateList<String>, starts: SnapshotStateList<String>, emojis: SnapshotStateList<String>, teams: SnapshotStateList<Int>, rule: WinRule, onRule: (WinRule) -> Unit, target: String, onTarget: (String) -> Unit, teamMode: Boolean, onTeamMode: (Boolean) -> Unit, teamA: String, onTeamA: (String) -> Unit, teamB: String, onTeamB: (String) -> Unit, notes: String, onNotes: (String) -> Unit, onStart: () -> Unit) {
  val clean = names.map { it.trim() }
  val errs = clean.map { v ->
    when {
      v.isEmpty() -> tr("Введите имя", "Enter a name")
      v.length > 15 -> tr("Максимум 15", "Max 15")
      clean.count { it.equals(v, true) } > 1 -> tr("Имя уже есть", "Name used")
      else -> null
    }
  }
  val startsOk = starts.all { it.trim().toIntOrNull() in -9999..9999 }
  val targetOk = target.trim().toIntOrNull() in 0..9999
  val ok = name.trim().length in 1..30 && names.size in 2..8 && errs.all { it == null } && startsOk && targetOk
  Page {
    Heading(tr("Время играть", "Time to play"), tr("Шаблон подставит цель и правило.", "A template sets goal and rule."))
    GlowPanel(hi = true) {
      Text(tr("Шаблон игры", "Game template"), fontWeight = FontWeight.Bold)
      Templates.forEachIndexed { i, t -> Choice(if (LocalEnglish.current) t.en else t.ru, tpl == i) { onTpl(i) } }
    }
    GlowPanel(hi = true) {
  Text(
    tr("Шаблон игры", "Game template"),
    fontWeight = FontWeight.Bold
  )

  Row(
    modifier = Modifier.horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Templates.forEachIndexed { i, template ->
      FilterChip(
        selected = tpl == i,
        onClick = { onTpl(i) },
        label = {
          Text(
            if (LocalEnglish.current) template.en else template.ru
          )
        }
      )
    }
  }
}
    GlowPanel {
      Text(tr("Участники", "Players") + " · ${names.size}/8", fontWeight = FontWeight.Bold)
      names.forEachIndexed { i, v ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          TextButton(onClick = { emojis[i] = Emojis[(Emojis.indexOf(emojis[i]) + 1).mod(Emojis.size)] }) { Text(emojis[i], fontSize = 22.sp) }
          Column(Modifier.weight(1f)) {
            OutlinedTextField(v, { names[i] = it }, Modifier.fillMaxWidth(), label = { Text(tr("Игрок", "Player") + " ${i + 1}") }, singleLine = true, shape = RoundedCornerShape(16.dp), isError = v.isNotEmpty() && errs[i] != null)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
              OutlinedTextField(starts[i], { starts[i] = it }, Modifier.width(96.dp), label = { Text(tr("Старт", "Start")) }, singleLine = true, shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
              if (teamMode) TextButton(onClick = { teams[i] = 1 - teams[i] }) { Text(if (teams[i] == 0) teamA.ifEmpty { "A" } else teamB.ifEmpty { "B" }) }
              if (names.size > 2) TextButton(onClick = { names.removeAt(i); starts.removeAt(i); emojis.removeAt(i); teams.removeAt(i) }, modifier = Modifier.size(48.dp), contentPadding = PaddingValues(0.dp)) { Text("×", fontSize = 24.sp) }
            }
          }
        }
      }
      OutlinedButton(onClick = { names.add(""); starts.add("0"); emojis.add(Emojis[names.size % Emojis.size]); teams.add(0) }, enabled = names.size < 8, modifier = Modifier.fillMaxWidth()) { Text(tr("+ Добавить игрока", "+ Add player")) }
    }
    GlowPanel {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
          Text(tr("Командный режим", "Team mode"), fontWeight = FontWeight.Bold)
          Text(tr("Счёт по командам", "Scores by teams"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(teamMode, onTeamMode)
      }
      if (teamMode) {
        OutlinedTextField(teamA, onTeamA, Modifier.fillMaxWidth(), label = { Text("A") }, singleLine = true, shape = RoundedCornerShape(14.dp))
        OutlinedTextField(teamB, onTeamB, Modifier.fillMaxWidth(), label = { Text("B") }, singleLine = true, shape = RoundedCornerShape(14.dp))
      }
      Text(tr("Победа", "Win rule"), fontWeight = FontWeight.Bold)
      Choice(tr("Больше очков", "Higher wins"), rule == WinRule.MAX) { onRule(WinRule.MAX) }
      Choice(tr("Меньше очков", "Lower wins"), rule == WinRule.MIN) { onRule(WinRule.MIN) }
    }
    GlowPanel {
      Text(tr("Заметка", "Note"), fontWeight = FontWeight.Bold)
      OutlinedTextField(notes, onNotes, Modifier.fillMaxWidth(), placeholder = { Text(tr("Где играли", "Where you played")) }, shape = RoundedCornerShape(16.dp))
    }
    MainButton(tr("Начать партию", "Start game"), ok, onStart)
  }
}

@Composable
private fun ScoreNumber(v: Long?) {
  val mo = LocalMotion.current
  AnimatedContent(targetState = v, transitionSpec = {
    val d = if (mo) 260 else 0
    val up = (targetState ?: 0L) >= (initialState ?: 0L)
    (slideInVertically(tween(d)) { if (up) it else -it } + fadeIn(tween(d))) togetherWith (slideOutVertically(tween(d)) { if (up) -it else it } + fadeOut(tween(d)))
  }, label = "sc") {
    Text(it?.toString() ?: "—", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
  }
}

@Composable
private fun ScoreChart(cum: List<List<Long>>, players: List<String>) {
  Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.35f))) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(tr("График по раундам", "Round chart"), fontWeight = FontWeight.Bold)
      if (cum.isEmpty() || cum[0].isEmpty()) {
        Text(tr("Добавьте раунды", "Add rounds"))
        return@Column
      }
      val all = cum.flatten()
      val mn = all.minOrNull() ?: 0L
      val mx = all.maxOrNull() ?: 0L
      val span = (mx - mn).coerceAtLeast(1L)
      Canvas(Modifier.fillMaxWidth().height(160.dp)) {
        val w = size.width
        val h = size.height
        val n = cum[0].size
        cum.forEachIndexed { p, line ->
          val path = Path()
          line.forEachIndexed { r, v ->
            val x = if (n == 1) w / 2 else r * w / (n - 1).coerceAtLeast(1)
            val y = h - ((v - mn).toFloat() / span.toFloat()) * (h - 16) - 8
            if (r == 0) path.moveTo(x, y) else path.lineTo(x, y)
          }
          drawPath(path, ChartColors[p % ChartColors.size], style = Stroke(7f))
        }
      }
      Text(players.joinToString("   ") { "● $it" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
private fun BigScreen(name: String, players: List<String>, emojis: List<String>, sc: List<Long?>, finished: Boolean, rule: WinRule, teamMode: Boolean, teamAssign: List<Int>, teamNames: List<String>, onNormal: () -> Unit, onFinish: () -> Unit, onNew: () -> Unit) {
  val en = LocalEnglish.current
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Text(name, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
    if (teamMode) {
      val t0 = players.indices.filter { teamAssign.getOrElse(it) { 0 } == 0 }.sumOf { sc[it] ?: 0L }
      val t1 = players.indices.filter { teamAssign.getOrElse(it) { 0 } == 1 }.sumOf { sc[it] ?: 0L }
      teamNames.forEachIndexed { i, tn -> Text(tn, fontSize = 22.sp); Text(if (i == 0) t0.toString() else t1.toString(), fontSize = 64.sp, lineHeight = 68.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary) }
      if (finished) Text(teamWinnerText(teamNames, listOf(t0, t1), rule, en), fontSize = 22.sp, fontWeight = FontWeight.Bold)
    } else {
      players.forEachIndexed { i, p ->
        Text("${emojis.getOrElse(i) { "" }} $p", fontSize = 22.sp)
        Text(sc[i]?.toString() ?: "—", fontSize = 64.sp, lineHeight = 68.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
      }
      if (finished && sc.all { it != null }) Text(winnerText(players, sc.filterNotNull(), rule, en), fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
    OutlinedButton(onClick = onNormal, modifier = Modifier.fillMaxWidth()) { Text(tr("Обычный вид", "Normal view")) }
    if (!finished) MainButton(tr("Завершить", "Finish"), sc.all { it != null }, onFinish) else MainButton(tr("Новая партия", "New game"), onClick = onNew)
  }
}

@Composable
private fun GamePage(name: String, players: List<String>, emojis: List<String>, starts: List<Int>, rule: WinRule, target: Int, teamMode: Boolean, teamAssign: List<Int>, teamNames: List<String>, rounds: SnapshotStateList<SnapshotStateList<String>>, finished: Boolean, turn: Int, onTurn: (Int) -> Unit, onBig: () -> Unit, onAdd: () -> Unit, onRemoveAsk: () -> Unit, onUndo: () -> Unit, canUndo: Boolean, pushUndo: () -> Unit, onFinishAsk: () -> Unit, onAutoFinish: () -> Unit, onResetAsk: () -> Unit) {
  val ctx = LocalContext.current
  val en = LocalEnglish.current
  val mo = LocalMotion.current
  val sc = totals(rounds, starts)
  val valid = rounds.isNotEmpty() && sc.all { it != null }
  val best = if (!valid) null else if (rule == WinRule.MAX) sc.filterNotNull().maxOrNull() else sc.filterNotNull().minOrNull()
  val cum = players.indices.map { c ->
    var a = starts.getOrElse(c) { 0 }.toLong()
    rounds.map { a += parseScore(it[c]) ?: 0; a }
  }
  var dice by remember { mutableStateOf<Int?>(null) }
  var coin by remember { mutableStateOf<String?>(null) }
  var timerLen by remember { mutableStateOf(0) }
  var left by remember { mutableStateOf(0) }
  var running by remember { mutableStateOf(false) }
  val headsWord = if (en) "Heads" else "Орёл"
  val tailsWord = if (en) "Tails" else "Решка"
  val safeTurn = if (players.isNotEmpty()) turn % players.size else 0
var autoAsked by remember(target) { mutableStateOf(false) }
LaunchedEffect(valid, sc) {
  if (valid && !finished && target > 0 && !autoAsked) {
    val vs = sc.filterNotNull()
    val hit = if (rule == WinRule.MAX) vs.any { it >= target } else vs.any { it <= target }
    if (hit) { autoAsked = true; onFinishAsk() } // только диалог "Завершить?"
  }
}
  LaunchedEffect(running, left) {
    if (running && left > 0) { delay(1000); left -= 1 }
    else if (running && left <= 0) { running = false; if (timerLen > 0) vibrateNow(ctx) }
  }
  val teamTotals = if (teamMode) listOf(players.indices.filter { teamAssign.getOrElse(it) { 0 } == 0 }.sumOf { sc[it] ?: 0L }, players.indices.filter { teamAssign.getOrElse(it) { 0 } == 1 }.sumOf { sc[it] ?: 0L }) else emptyList()
  Page {
    Heading(name, tr("Игроков: ${players.size} · Раундов: ${rounds.size}", "Players: ${players.size} · Rounds: ${rounds.size}"))
    val ruleWord = if (rule == WinRule.MAX) tr("Больше — лучше", "Higher wins") else tr("Меньше — лучше", "Lower wins")
    val targetWord = if (target > 0) tr(" · Цель: $target", " · Target: $target") else ""
    Text(ruleWord + targetWord, color = MaterialTheme.colorScheme.primary)
    AnimatedVisibility(finished, enter = fadeIn() + expandVertically()) {
      GlowPanel(hi = true) {
        Text(tr("ИТОГИ", "RESULTS"), color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
        if (teamMode && valid) Text(teamWinnerText(teamNames, teamTotals, rule, en), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        else Text(winnerText(players, sc.filterNotNull(), rule, en), fontSize = 24.sp, fontWeight = FontWeight.Bold)
      }
    }
    if (teamMode && valid) {
      GlowPanel(hi = true) {
        Text(tr("Команды", "Teams"), fontWeight = FontWeight.Bold)
        teamTotals.forEachIndexed { i, v -> Text("${teamNames.getOrElse(i) { "T${i + 1}" }}: $v", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
      }
    }
    GlowPanel {
      Text(
  tr("Инструменты партии", "Game tools"),
  fontSize = 17.sp,
  fontWeight = FontWeight.Bold
)
      Text(tr("Ходит: ${emojis.getOrElse(safeTurn) { "" }} ${players.getOrElse(safeTurn) { "" }}", "To move: ${emojis.getOrElse(safeTurn) { "" }} ${players.getOrElse(safeTurn) { "" }}"))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onTurn((safeTurn + 1) % players.size) }, enabled = !finished) { Text(tr("Дальше ›", "Next ›")) }
        OutlinedButton(onClick = { dice = Random.nextInt(1, 7) }) { Text("🎲 ${dice ?: "–"}") }
        OutlinedButton(onClick = { coin = if (Random.nextBoolean()) headsWord else tailsWord }) { Text("🪙 ${coin ?: "–"}") }
      }
      OutlinedButton(onClick = { onTurn(Random.nextInt(players.size)) }){ Text(tr("🎲 Первый ход","🎲 First")) }
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(0, 30, 60, 90).forEach { d ->
          FilterChip(d == timerLen, { timerLen = d; left = d; running = false }, label = { Text(if (d == 0) tr("Выкл", "Off") else "${d}c") })
        }
      }
      if (timerLen > 0) {
        Text(tr("Осталось: $left c", "Left: ${left}s"), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (left == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        if (left == 0) Text(tr("⏰ Время вышло!", "⏰ Time's up!"), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(onClick = { if (left <= 0) left = timerLen; running = true }, enabled = !running) { Text(tr("Старт", "Start")) }
          OutlinedButton(onClick = { running = false }) { Text(tr("Пауза", "Pause")) }
          OutlinedButton(onClick = { running = false; left = timerLen }) { Text(tr("Сброс", "Reset")) }
        }
      }
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      players.forEachIndexed { i, p ->
        val lead = best != null && sc[i] == best
        GlowPanel(
          mod = Modifier.width(178.dp),
          hi = lead
        ) {
          Text(
  text = "${emojis.getOrElse(i) { "" }}  $p",
  fontSize = 16.sp,
  fontWeight = FontWeight.Bold,
  maxLines = 1
)
          if (i == safeTurn && !finished) Text(tr("● ходит", "● to move"), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
          ScoreNumber(sc[i])
          if (!finished && valid) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf(1, 5, 10, -1).forEach { d ->
                OutlinedButton(onClick = { pushUndo(); val last = rounds.last(); val cur = parseScore(last[i]) ?: 0; last[i] = (cur + d).coerceIn(-9999, 9999).toString() }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(48.dp)) { Text(if (d > 0) "+$d" else "$d", fontSize = 13.sp) }
              }
            }
          }
        }
      }
    }
    ScoreChart(cum, players)
    GlowPanel {
      Text(tr("Очки по раундам", "Round scores"), fontWeight = FontWeight.Bold)
      Column(Modifier.horizontalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("№", modifier = Modifier.width(36.dp))
          players.forEach { Text(it, modifier = Modifier.width(108.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        }
        rounds.forEachIndexed { ri, row ->
          key(ri) {
            var vis by remember { mutableStateOf(!mo) }
            LaunchedEffect(Unit) { delay(16); vis = true }
            AnimatedVisibility(vis || !mo, enter = fadeIn() + expandVertically()) {
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${ri + 1}".padStart(2, '0'), modifier = Modifier.width(36.dp))
                row.forEachIndexed { c, _ ->
                  Column(Modifier.width(108.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val cur = row[c]
                    OutlinedTextField(
                      value = cur,
                      Modifier.onFocusChanged {
                        if (it.isFocused && !finished) pushUndo()
                          },
// ...
                          onValueChange = { newValue ->
                        if (!finished) row[c] = newValue
                          },
                      modifier = Modifier.fillMaxWidth(),
                      readOnly = finished,
                      singleLine = true,
                      isError = parseScore(cur) == null,
                      shape = RoundedCornerShape(14.dp),
                      keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                      )
                    )
                    if (!finished) TextButton(onClick = { row[c] = if (cur.startsWith("-")) cur.removePrefix("-") else "-" + cur.removePrefix("+") }, modifier = Modifier.heightIn(min = 48.dp)) { Text("±", fontSize = 18.sp) }
                  }
                }
              }
            }
          }
        }
      }
      if (!valid) Text(tr("Нужны целые числа −9999..9999.", "Need whole numbers −9999..9999."), color = MaterialTheme.colorScheme.error)
    }
    if (!finished) {
      OutlinedButton(onClick = onAdd, enabled = valid, modifier = Modifier.fillMaxWidth()) { Text(tr("+ Добавить раунд", "+ Add round")) }
      OutlinedButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.fillMaxWidth()) { Text(tr("Отменить последнее действие", "Undo last action")) }
      OutlinedButton(onClick = onRemoveAsk, enabled = rounds.size > 1, modifier = Modifier.fillMaxWidth()) { Text(tr("Удалить последний раунд", "Delete last round")) }
      OutlinedButton(onClick = onBig, modifier = Modifier.fillMaxWidth()) { Text(tr("Большой экран", "Large scoreboard")) }
      MainButton(tr("Завершить партию", "Finish game"), valid, onFinishAsk)
      TextButton(onClick = onResetAsk, modifier = Modifier.fillMaxWidth()) { Text(tr("Отменить партию", "Discard game")) }
    } else {
      MainButton(tr("Новая партия", "New game"), onClick = onResetAsk)
    }
  }
}

@Composable
private fun HistoryPage(games: List<SavedGame>, onOpen: (SavedGame) -> Unit, onDelete: (SavedGame) -> Unit, tours: List<Tournament>, onTours: (List<Tournament>) -> Unit) {
  val en = LocalEnglish.current
  var tName by remember { mutableStateOf("") }
  var tPlayers by remember { mutableStateOf("") }
  var tTarget by remember { mutableStateOf("3") }
  val plist = tPlayers.split(",").map { it.trim() }.filter { it.isNotEmpty() }
  val tw = tTarget.toIntOrNull()
  val okT = tName.trim().length in 1..40 && plist.size in 2..8 && tw in 1..20
  Page {
    Heading(tr("История", "History"), tr("Партии и турниры", "Games and tournaments"))
    if (games.isEmpty()) {
      GlowPanel { Text(tr("Здесь появятся ваши игры", "Your games will appear here"), fontSize = 22.sp, fontWeight = FontWeight.Bold); Text(tr("Завершите первую партию.", "Finish your first game.")) }
    }
    games.forEach { g ->
      key(g.id) {
        GlowPanel {
          Text(dateText(g.date), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(g.name, fontSize = 23.sp, fontWeight = FontWeight.Bold)
          Text(winnerText(g.players, savedTotals(g), g.rule, en), color = MaterialTheme.colorScheme.primary)
          Text(tr("Игроков: ${g.players.size} · Раундов: ${g.rounds.size}", "Players: ${g.players.size} · Rounds: ${g.rounds.size}"))
          if (g.notes.isNotEmpty()) Text("📝 ${g.notes}", fontSize = 13.sp)
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { onOpen(g) }) { Text(tr("Подробнее", "Details")) }
            TextButton(onClick = { onDelete(g) }) { Text(tr("Удалить", "Delete"), color = MaterialTheme.colorScheme.error) }
          }
        }
      }
    }
    GlowPanel(hi = true) {
      Text(tr("Турнир до N побед", "Tournament to N wins"), fontWeight = FontWeight.Bold)
      OutlinedTextField(tName, { tName = it }, Modifier.fillMaxWidth(), label = { Text(tr("Название турнира", "Tournament name")) }, singleLine = true, shape = RoundedCornerShape(14.dp))
      OutlinedTextField(tPlayers, { tPlayers = it }, Modifier.fillMaxWidth(), label = { Text(tr("Игроки через запятую", "Players, comma separated")) }, singleLine = true, shape = RoundedCornerShape(14.dp))
      OutlinedTextField(tTarget, { tTarget = it }, Modifier.fillMaxWidth(), label = { Text(tr("Побед для титула", "Wins for title")) }, singleLine = true, shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
      MainButton(tr("Создать турнир", "Create tournament"), okT) {
        onTours(listOf(Tournament(UUID.randomUUID().toString(), tName.trim(), System.currentTimeMillis(), plist.take(8), tw ?: 3, List(plist.take(8).size) { 0 })) + tours)
        tName = ""; tPlayers = ""
      }
      tours.forEach { t ->
        key(t.id) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(t.name, fontWeight = FontWeight.Bold)
            Text(tr("Цель: ${t.targetWins} победы", "Target: ${t.targetWins} wins"), fontSize = 12.sp)
            t.players.forEachIndexed { i, p ->
              Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("$p — ${t.wins.getOrElse(i) { 0 }}" + (if (t.wins.getOrElse(i) { 0 } >= t.targetWins) " 🏆" else ""))
                Row {
                  TextButton(onClick = { val nw = t.wins.toMutableList(); nw[i]++; onTours(tours.map { if (it.id == t.id) it.copy(wins = nw) else it }) }) { Text("+1") }
                  TextButton(onClick = { val nw = t.wins.toMutableList(); if (nw[i] > 0) nw[i]--; onTours(tours.map { if (it.id == t.id) it.copy(wins = nw) else it }) }) { Text("-1") }
                }
              }
            }
            TextButton(onClick = { onTours(tours.filterNot { it.id == t.id }) }) { Text(tr("Удалить турнир", "Delete tournament"), color = MaterialTheme.colorScheme.error) }
          }
        }
      }
    }
  }
}

@Composable
private fun StatsPage(games: List<SavedGame>) {
  Page {
    Heading(tr("Статистика", "Stats"), tr("Кто чаще побеждает", "Who wins most"))
    if (games.isEmpty()) {
      GlowPanel { Text(tr("Нет данных", "No data"), fontWeight = FontWeight.Bold) }
      return@Page
    }
    val names = games.flatMap { it.players }.distinct().sorted()
    names.forEach { n ->
      val played = games.count { g -> g.players.any { it.equals(n, true) } }
      val res = games.mapNotNull { g ->
        val idx = g.players.indexOfFirst { it.equals(n, true) }
        if (idx < 0) null else {
          val tt = savedTotals(g)
          val best = if (g.rule == WinRule.MAX) tt.maxOrNull()!! else tt.minOrNull()!!
          Triple(tt[idx], tt[idx] == best, tt.count { it == best } > 1)
        }
      }
      // res надо хранить с правилом: Triple(score, isBest, isTie) + rule из SavedGame
val wins = res.count { it.second && !it.third }
val ties = res.count { it.second && it.third } // только если сам в дележе 1 места
val bestMax = res.filter { it.thirdRule == WinRule.MAX }.map { it.first }.maxOrNull()
val bestMin = res.filter { it.thirdRule == WinRule.MIN }.map { it.first }.minOrNull()
val avg = if (res.isEmpty()) 0.0 else res.map { it.first.toDouble() }.average()
      val rate = if (played > 0) "${wins * 100 / played}%" else "—"
      GlowPanel {
        Text(n, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(tr("Игр: $played · Побед: $wins · Ничьих: $ties", "Games: $played · Wins: $wins · Ties: $ties"))
        Text(tr("Винрейт: $rate · Средний итог: ${"%.1f".format(avg)} · Лучший: $bestScore", "Winrate: $rate · Avg: ${"%.1f".format(avg)} · Best: $bestScore"))
      }
    }
  }
}

@Composable
private fun HistoryDetails(g: SavedGame, onDismiss: () -> Unit) {
  val scores = savedTotals(g)
  val en = LocalEnglish.current
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(g.name) },
    text = {
      Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(dateText(g.date))
        Text(winnerText(g.players, scores, g.rule, en), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(if (g.rule == WinRule.MAX) tr("Больше — лучше", "Higher wins") else tr("Меньше — лучше", "Lower wins"))
        if (g.notes.isNotEmpty()) Text("📝 ${g.notes}")
        g.players.forEachIndexed { i, p -> Text("$p: ${scores[i]}") }
        HorizontalDivider()
        Text(tr("Раунды", "Rounds"), fontWeight = FontWeight.Bold)
        g.rounds.forEachIndexed { i, r -> Text((tr("Раунд", "Round") + " ${i + 1}\n") + g.players.mapIndexed { c, p -> "$p: ${r[c]}" }.joinToString(" · ")) }
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Закрыть", "Close")) } }
  )
}

@Composable
private fun SettingsPage(theme: String, onTheme: (String) -> Unit, accent: Int, onAccent: (Int) -> Unit, style: Int, onStyle: (Int) -> Unit, english: Boolean, onEnglish: (Boolean) -> Unit, motion: Boolean, onMotion: (Boolean) -> Unit, sound: Boolean, onSound: (Boolean) -> Unit, historyCount: Int, onClear: () -> Unit, tourCount: Int) {
  Page {
    Heading(tr("Настройки", "Settings"), tr("Ваш стиль. Ваша игра.", "Your style. Your game."))
    GlowPanel(hi = true) {
      Text(
        tr("Язык интерфейса", "Interface language"),
        fontWeight = FontWeight.Bold
      )

      Choice(
        text = "Русский",
        sel = !english,
        onClick = { onEnglish(false) }
      )

      Choice(
        text = "English",
        sel = english,
        onClick = { onEnglish(true) }
      )
    }

    GlowPanel {
      Text(
        tr("Тема", "Theme"),
        fontWeight = FontWeight.Bold
      )

      Choice(
        text = tr("Как на устройстве", "System"),
        sel = theme == "system",
        onClick = { onTheme("system") }
      )

      Choice(
        text = tr("Светлая", "Light"),
        sel = theme == "light",
        onClick = { onTheme("light") }
      )

      Choice(
        text = tr("Тёмная", "Dark"),
        sel = theme == "dark",
        onClick = { onTheme("dark") }
      )

      Choice(
        text = "AMOLED",
        sel = theme == "amoled",
        onClick = { onTheme("amoled") }
      )
    }

    GlowPanel {
      Text(
        tr("Стиль фона", "Background style"),
        fontWeight = FontWeight.Bold
      )

      Choice(
        text = tr("Стандарт", "Standard"),
        sel = style == 0,
        onClick = { onStyle(0) }
      )

      Choice(
        text = tr("Закат", "Sunset"),
        sel = style == 1,
        onClick = { onStyle(1) }
      )

      Choice(
        text = tr("Лес", "Forest"),
        sel = style == 2,
        onClick = { onStyle(2) }
      )

      Choice(
        text = "AMOLED",
        sel = style == 3,
        onClick = { onStyle(3) }
      )
    }

    GlowPanel {
      Text(
        tr("Акцентный цвет", "Accent color"),
        fontWeight = FontWeight.Bold
      )

      Choice(
        text = tr("Аметист", "Amethyst"),
        sel = accent == 0,
        onClick = { onAccent(0) }
      )

      Choice(
        text = tr("Океан", "Ocean"),
        sel = accent == 1,
        onClick = { onAccent(1) }
      )

      Choice(
        text = tr("Изумруд", "Emerald"),
        sel = accent == 2,
        onClick = { onAccent(2) }
      )
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
            tr("Анимации", "Animations"),
            fontWeight = FontWeight.Bold
          )

          Text(
            tr(
              "Переходы, изменение счёта и появление раундов",
              "Transitions, score changes and round appearance"
            ),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Switch(
          checked = motion,
          onCheckedChange = onMotion
        )
      }

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
            tr("Звук победы", "Victory sound"),
            fontWeight = FontWeight.Bold
          )

          Text(
            tr(
              "Фанфара и вибрация после завершения",
              "Fanfare and vibration after finishing"
            ),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Switch(
          checked = sound,
          onCheckedChange = onSound
        )
      }
    }

    GlowPanel {
      Text(
        tr("Данные", "Data"),
        fontWeight = FontWeight.Bold
      )

      Text(
        tr(
          "Завершённых партий: $historyCount",
          "Completed games: $historyCount"
        )
      )

      Text(
        tr(
          "Турниров: $tourCount",
          "Tournaments: $tourCount"
        )
      )

      OutlinedButton(
        onClick = onClear,
        enabled = historyCount > 0,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          tr("Очистить историю", "Clear history")
        )
      }
    }
  }
}

@Composable
private fun HelpDialog(
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        tr("Во что поиграть?", "What can you play?")
      )
    },
    text = {
      Column(
        modifier = Modifier
          .heightIn(max = 480.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text(
          tr(
            "Приложение складывает очки по раундам и сравнивает итоговые суммы. Особые правила конкретной игры нужно учитывать самостоятельно.",
            "The app adds scores by round and compares the final totals. Special rules of a particular game must be handled manually."
          )
        )

        HelpItem(
          title = "UNO",
          description = tr(
            "Записывайте очки победителя каждой раздачи. Выберите правило «Больше очков» и цель 500.",
            "Enter the points earned by the winner of each hand. Select “Higher wins” and set the target to 500."
          )
        )

        HelpItem(
          title = tr("Скрэббл / Эрудит", "Scrabble"),
          description = tr(
            "Записывайте очки за ходы. Побеждает игрок с большей суммой. Штрафы можно внести отдельным раундом.",
            "Enter points for turns. The player with the highest total wins. Enter penalties as a separate round."
          )
        )

        HelpItem(
          title = tr("Рамми", "Rummy"),
          description = tr(
            "Используйте раунды для записи результатов раздач. Выберите правило в зависимости от вашей версии игры.",
            "Use rounds to record hand results. Select the rule according to your version of the game."
          )
        )

        HelpItem(
          title = tr("Домино", "Dominoes"),
          description = tr(
            "Для варианта со штрафными очками выберите «Меньше очков».",
            "For a penalty-scoring variant, select “Lower wins”."
          )
        )

        HelpItem(
          title = tr("Ятцы", "Yahtzee"),
          description = tr(
            "Записывайте очки за категории отдельными раундами. Бонусы можно внести отдельным раундом.",
            "Record category scores as separate rounds. Bonuses can be entered as a separate round."
          )
        )

        HelpItem(
          title = tr("Викторины", "Quizzes"),
          description = tr(
            "Начисляйте очки за правильные ответы. Для команд используйте командный режим.",
            "Award points for correct answers. Use team mode for team quizzes."
          )
        )

        HorizontalDivider()

        HelpItem(
          title = tr("Как пользоваться", "How to use"),
          description = tr(
            "1. Создайте партию и добавьте от 2 до 8 участников.\n" +
              "2. Выберите шаблон или настройте правило вручную.\n" +
              "3. Введите стартовые очки при необходимости.\n" +
              "4. Добавляйте раунды и вводите очки.\n" +
              "5. Используйте быстрые кнопки или клавишу ±.\n" +
              "6. Завершите партию — результат появится в истории.",
            "1. Create a game with 2–8 players.\n" +
              "2. Select a template or configure the rule manually.\n" +
              "3. Add starting scores if needed.\n" +
              "4. Add rounds and enter scores.\n" +
              "5. Use quick buttons or the ± button.\n" +
              "6. Finish the game to save it to history."
          )
        )

        HelpItem(
          title = tr("Сохранение", "Saving"),
          description = tr(
            "История и настройки сохраняются на этом устройстве. Незавершённая партия хранится только во время работы приложения.",
            "History and settings are saved on this device. An unfinished game is kept only while the app is running."
          )
        )
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(
          tr("Понятно", "Got it")
        )
      }
    }
  )
}

@Composable
private fun HelpItem(
  title: String,
  description: String
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Text(
      text = title,
      color = MaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.Bold,
      fontSize = 16.sp
    )

    Text(
      text = description
    )
  }
}
