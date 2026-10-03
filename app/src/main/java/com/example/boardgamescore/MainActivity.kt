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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
data class Template(val ru:String,val en:String,val rule:WinRule,val target:Int)
private val Templates=listOf(
Template("Свободная игра","Free game",WinRule.MAX,0),
Template("Уно — до 500","UNO — to 500",WinRule.MAX,500),
Template("Манчкин — до 10","Munchkin — to 10",WinRule.MAX,10),
Template("Скрэббл / Эрудит","Scrabble",WinRule.MAX,0),
Template("Рамми","Rummy",WinRule.MAX,0),
Template("Домино — штрафы","Dominoes — penalties",WinRule.MIN,0),
Template("Ятцы — кости","Yahtzee — dice",WinRule.MAX,0),
Template("Викторина","Quiz",WinRule.MAX,0)
)
data class SavedGame(val id:String,val name:String,val date:Long,val players:List<String>,val rounds:List<List<Int>>,val rule:WinRule,val target:Int=0,val notes:String="",val starts:List<Int>=emptyList(),val teamMode:Boolean=false,val teamAssign:List<Int>=emptyList(),val teamNames:List<String>=emptyList())
data class Tournament(val id:String,val name:String,val date:Long,val players:List<String>,val targetWins:Int,val wins:List<Int>)
private val LocalEnglish=staticCompositionLocalOf{false}
private val LocalMotion=staticCompositionLocalOf{true}
@Composable private fun tr(ru:String,en:String)=if(LocalEnglish.current)en else ru
private val Emojis=listOf("🦊","🐼","🦁","🐸","🐯","🦄","⚡","🎲","🌟","🍀")
private val ChartColors=listOf(Color(0xFFB79CFF),Color(0xFF7ADBCB),Color(0xFFFFB87A),Color(0xFF8ECFFF),Color(0xFFFF9EB5),Color(0xFFE2D58C),Color(0xFF9FAEFF),Color(0xFFD7A3E5))

private class Storage(c:Context){
private val p=c.getSharedPreferences("board_game_score",Context.MODE_PRIVATE)
var theme:String get()=p.getString("theme","system")?:"system" set(v){p.edit().putString("theme",v).apply()}
var accent:Int get()=p.getInt("accent",0).coerceIn(0,2) set(v){p.edit().putInt("accent",v).apply()}
var style:Int get()=p.getInt("style",0).coerceIn(0,3) set(v){p.edit().putInt("style",v).apply()}
var animations:Boolean get()=p.getBoolean("animations",true) set(v){p.edit().putBoolean("animations",v).apply()}
var english:Boolean get()=p.getBoolean("english",false) set(v){p.edit().putBoolean("english",v).apply()}
var sound:Boolean get()=p.getBoolean("sound",true) set(v){p.edit().putBoolean("sound",v).apply()}
fun loadHistory():List<SavedGame>=runCatching{
val a=JSONArray(p.getString("history","[]")?:"[]");buildList{
for(i in 0 until a.length()){runCatching{
val o=a.getJSONObject(i);val np=o.getJSONArray("players");val nr=o.getJSONArray("rounds")
val pl=List(np.length()){np.getString(it)};val ro=List(nr.length()){r->val row=nr.getJSONArray(r);List(row.length()){row.getInt(it)}}
require(pl.size in 2..8&&ro.isNotEmpty())
SavedGame(o.getString("id"),o.getString("name"),o.getLong("date"),pl,ro,WinRule.valueOf(o.getString("rule")),o.optInt("target",0),o.optString("notes",""),
if(o.has("starts"))List(o.getJSONArray("starts").length()){o.getJSONArray("starts").getInt(it)}else List(pl.size){0},
o.optBoolean("teamMode",false),
if(o.has("teamAssign"))List(o.getJSONArray("teamAssign").length()){o.getJSONArray("teamAssign").getInt(it)}else List(pl.size){0},
if(o.has("teamNames"))List(o.getJSONArray("teamNames").length()){o.getJSONArray("teamNames").getString(it)}else listOf("A","B"))
}.getOrNull()?.let{add(it)}}}}.getOrDefault(emptyList())
fun saveHistory(g:List<SavedGame>){val a=JSONArray();g.forEach{gm->
val np=JSONArray();gm.players.forEach{np.put(it)};val nr=JSONArray();gm.rounds.forEach{r->val v=JSONArray();r.forEach{v.put(it)};nr.put(v)}
val ns=JSONArray();gm.starts.forEach{ns.put(it)};val ta=JSONArray();gm.teamAssign.forEach{ta.put(it)};val tn=JSONArray();gm.teamNames.forEach{tn.put(it)}
a.put(JSONObject().apply{put("id",gm.id);put("name",gm.name);put("date",gm.date);put("players",np);put("rounds",nr);put("rule",gm.rule.name);put("target",gm.target);put("notes",gm.notes);put("starts",ns);put("teamMode",gm.teamMode);put("teamAssign",ta);put("teamNames",tn)})}
p.edit().putString("history",a.toString()).apply()}
fun loadTournaments():List<Tournament>=runCatching{
val a=JSONArray(p.getString("tournaments","[]")?:"[]");List(a.length()){i->val o=a.getJSONObject(i);val np=o.getJSONArray("players");val nw=o.getJSONArray("wins")
Tournament(o.getString("id"),o.getString("name"),o.getLong("date"),List(np.length()){np.getString(it)},o.optInt("targetWins",3),List(nw.length()){nw.getInt(it)})}}.getOrDefault(emptyList())
fun saveTournaments(t:List<Tournament>){val a=JSONArray();t.forEach{x->
val np=JSONArray();x.players.forEach{np.put(it)};val nw=JSONArray();x.wins.forEach{nw.put(it)}
a.put(JSONObject().apply{put("id",x.id);put("name",x.name);put("date",x.date);put("players",np);put("targetWins",x.targetWins);put("wins",nw)})}
p.edit().putString("tournaments",a.toString()).apply()}
}
class MainActivity:ComponentActivity(){
override fun onCreate(s:Bundle?){super.onCreate(s);val st=Storage(this);setContent{ScoreApp(st)}}}
private fun parseScore(t:String)=t.trim().toIntOrNull()?.takeIf{it in -9999..9999}
private fun newRound(n:Int)=mutableStateListOf<String>().apply{repeat(n){add("0")}}
private fun totals(rounds:List<List<String>>,starts:List<Int>):List<Long?>{
if(rounds.isEmpty())return starts.map{it.toLong()};val n=rounds[0].size
return List(n){c->val vs=rounds.map{parseScore(it[c])};if(vs.any{it==null})null else starts.getOrElse(c){0}.toLong()+vs.sumOf{it!!.toLong()}}}
private fun savedTotals(g:SavedGame)=List(g.players.size){c->g.starts.getOrElse(c){0}.toLong()+g.rounds.sumOf{it[c].toLong()}}
private fun winnerText(pl:List<String>,v:List<Long>,rule:WinRule,en:Boolean):String{
if(v.isEmpty())return if(en)"No results" else "Нет результатов"
val best=if(rule==WinRule.MAX)v.maxOrNull()!! else v.minOrNull()!!
val w=pl.filterIndexed{i,_ ->v[i]==best}
return (if(w.size==1)if(en)"Winner: "else"Победитель: "else if(en)"Tie: "else"Ничья: ")+w.joinToString(", ")}
private fun dateText(t:Long)=SimpleDateFormat("dd.MM.yyyy · HH:mm",Locale.getDefault()).format(Date(t))
private fun vibrateNow(ctx:Context){runCatching{
val v=if(Build.VERSION.SDK_INT>=31)(ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
else @Suppress("DEPRECATION")ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createOneShot(450,VibrationEffect.DEFAULT_AMPLITUDE)) else @Suppress("DEPRECATION")v.vibrate(450)}}
private suspend fun fanfare(on:Boolean){if(!on)return;runCatching{
val g=ToneGenerator(AudioManager.STREAM_MUSIC,70)
listOf(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,ToneGenerator.TONE_CDMA_ABBR_ALERT,ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE).forEach{g.startTone(it,220);delay(260)};g.release()}}

@Composable private fun ScoreApp(s:Storage){
val ctx=LocalContext.current;val scope=rememberCoroutineScope()
var theme by remember{mutableStateOf(s.theme)};var accent by remember{mutableStateOf(s.accent)}
var style by remember{mutableStateOf(s.style)};var motion by remember{mutableStateOf(s.animations)}
var english by remember{mutableStateOf(s.english)};var sound by remember{mutableStateOf(s.sound)}
var history by remember{mutableStateOf(s.loadHistory())};var tournaments by remember{mutableStateOf(s.loadTournaments())}
var tab by remember{mutableStateOf(0)}
var draftTpl by remember{mutableStateOf(0)};var draftName by remember{mutableStateOf("")}
val draftPlayers=remember{mutableStateListOf("","")};val draftStarts=remember{mutableStateListOf("0","0")}
val draftEmojis=remember{mutableStateListOf("🦊","🐼")};var draftRule by remember{mutableStateOf(WinRule.MAX)}
var draftTarget by remember{mutableStateOf("0")};var draftTeamMode by remember{mutableStateOf(false)}
val draftTeams=remember{mutableStateListOf(0,0)};var draftTeamA by remember{mutableStateOf("Команда А")}
var draftTeamB by remember{mutableStateOf("Команда Б")};var draftNotes by remember{mutableStateOf("")}
var active by remember{mutableStateOf(false)};var finished by remember{mutableStateOf(false)}
var name by remember{mutableStateOf("")};var players by remember{mutableStateOf(emptyList<String>())}
var emojis by remember{mutableStateOf(emptyList<String>())};var starts by remember{mutableStateOf(emptyList<Int>())}
var rule by remember{mutableStateOf(WinRule.MAX)};var target by remember{mutableStateOf(0)}
var notes by remember{mutableStateOf("")};var teamMode by remember{mutableStateOf(false)}
var teamAssign by remember{mutableStateOf(emptyList<Int>())};var teamNames by remember{mutableStateOf(listOf("A","B"))}
val rounds=remember{mutableStateListOf<SnapshotStateList<String>>()}
val undoStack=remember{mutableStateListOf<List<List<String>>>()}
var turn by remember{mutableStateOf(0)};var big by remember{mutableStateOf(false)}
var confirm by remember{mutableStateOf<String?>(null)};var deleteId by remember{mutableStateOf<String?>(null)}
var selected by remember{mutableStateOf<SavedGame?>(null)};var help by remember{mutableStateOf(false)}
var tourName by remember{mutableStateOf("")};var tourPlayers by remember{mutableStateOf("")};var tourTarget by remember{mutableStateOf("3")}
fun syncDraft(){while(draftStarts.size<draftPlayers.size)draftStarts.add("0");while(draftEmojis.size<draftPlayers.size)draftEmojis.add(Emojis[draftEmojis.size%Emojis.size]);while(draftTeams.size<draftPlayers.size)draftTeams.add(0)
while(draftStarts.size>draftPlayers.size)draftStarts.removeAt(draftStarts.lastIndex);while(draftEmojis.size>draftPlayers.size)draftEmojis.removeAt(draftEmojis.lastIndex);while(draftTeams.size>draftPlayers.size)draftTeams.removeAt(draftTeams.lastIndex)}
fun pushUndo(){undoStack.add(rounds.map{it.toList()});if(undoStack.size>30)undoStack.removeAt(0)}
fun reset(){active=false;finished=false;players=emptyList();rounds.clear();undoStack.clear();turn=0;big=false
draftName="";draftPlayers.clear();draftPlayers.addAll(listOf("",""));draftStarts.clear();draftStarts.addAll(listOf("0","0"))
draftEmojis.clear();draftEmojis.addAll(listOf("🦊","🐼"));draftTeams.clear();draftTeams.addAll(listOf(0,0));draftRule=WinRule.MAX;draftTarget="0";draftTeamMode=false;draftNotes=""}
fun doFinish(){if(finished||!active)return;val sc=totals(rounds,starts);if(sc.any{it==null})return
val g=SavedGame(UUID.randomUUID().toString(),name,System.currentTimeMillis(),players.toList(),rounds.map{r->r.map{parseScore(it)!!}},rule,target,notes,starts.toList(),teamMode,teamAssign.toList(),teamNames.toList())
history=listOf(g)+history;s.saveHistory(history);finished=true;vibrateNow(ctx);scope.launch{fanfare(sound)}
if(tournaments.isNotEmpty()&&!teamMode){val vs=sc.filterNotNull();val best=if(rule==WinRule.MAX)vs.maxOrNull()!! else vs.minOrNull()!!
val wi=players.indices.filter{vs[it]==best}
tournaments=tournaments.map{t->if(t.players.size==players.size&&t.players.zip(players).all{(a,b)->a.equals(b,true)}){val nw=t.wins.toMutableList();wi.forEach{if(it<nw.size)nw[it]++};t.copy(wins=nw)}else t};s.saveTournaments(tournaments)}}
val useDark=when(theme){"light"->false;"dark","amoled"->true;else->isSystemInDarkTheme()}
val amoled=theme=="amoled"||style==3
val darkAcc=listOf(Color(0xFFC6A8FF),Color(0xFF8ECDFF),Color(0xFF88E3BF))
val lightAcc=listOf(Color(0xFF7040B0),Color(0xFF175DA6),Color(0xFF176B4C))
val primary by animateColorAsState(if(useDark)darkAcc[accent]else lightAcc[accent],tween(if(motion)350 else 0),label="ac")
val scheme=if(useDark)darkColorScheme(primary=primary,onPrimary=Color(0xFF171021),secondary=Color(0xFF7ADBCB),
background=if(amoled)Color.Black else Color(0xFF101019),surface=if(amoled)Color(0xFF0D0D12)else Color(0xFF1B1B29),
surfaceVariant=Color(0xFF292A3A),onBackground=Color(0xFFF5F1FF),onSurface=Color(0xFFF5F1FF))
else lightColorScheme(primary=primary,onPrimary=Color.White,secondary=Color(0xFF167D80),
background=Color(0xFFF5F3FC),surface=Color.White,surfaceVariant=Color(0xFFEAE6F2),onBackground=Color(0xFF231D30),onSurface=Color(0xFF231D30))
val bg=when(style){1->Brush.linearGradient(listOf(primary.copy(.30f),scheme.background,Color(0xFFFF9E7A).copy(.18f)))
2->Brush.linearGradient(listOf(Color(0xFF4ADE80).copy(.22f),scheme.background,primary.copy(.16f)))
else->Brush.linearGradient(listOf(primary.copy(.22f),scheme.background,scheme.secondary.copy(.14f)))}
BackHandler(enabled=active||tab!=0){if(tab!=0)tab=0 else confirm="discard"}
CompositionLocalProvider(LocalEnglish provides english,LocalMotion provides motion){
MaterialTheme(colorScheme=scheme){
Scaffold(containerColor=scheme.background,
topBar={Surface(color=scheme.background.copy(.95f)){Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=20.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
Text("SCORE CLUB",color=scheme.primary,fontWeight=FontWeight.ExtraBold,letterSpacing=2.sp,fontSize=14.sp)
TextButton(onClick={help=true}){Text(tr("?  Помощь","?  Help"))}}}},
bottomBar={NavigationBar(containerColor=scheme.surface,tonalElevation=0.dp){
listOf(tr("Игра","Game"),tr("История","History"),tr("Статы","Stats"),tr("Настройки","Settings")).forEachIndexed{i,t->
NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Text(listOf("▶","≡","★","⚙")[i],fontSize=20.sp)},label={Text(t)})}}}){pad->
Box(Modifier.fillMaxSize().padding(pad).imePadding().background(bg)){
AnimatedContent(tab,transitionSpec={fadeIn(tween(if(motion)220 else 0)) togetherWith fadeOut(tween(if(motion)120 else 0))},label="tabs"){c->
when(c){
0->if(!active){syncDraft()
SetupPage(draftTpl,{draftTpl=it;draftRule=Templates[it].rule;draftTarget=Templates[it].target.toString()},draftName,{draftName=it},draftPlayers,draftStarts,draftEmojis,draftTeams,draftRule,{draftRule=it},draftTarget,{draftTarget=it},draftTeamMode,{draftTeamMode=it},draftTeamA,{draftTeamA=it},draftTeamB,{draftTeamB=it},draftNotes,{draftNotes=it}){
name=draftName.trim();players=draftPlayers.map{it.trim()};starts=draftStarts.map{it.trim().toIntOrNull()?.coerceIn(-9999,9999)?:0}
emojis=draftEmojis.toList();rule=draftRule;target=draftTarget.trim().toIntOrNull()?.coerceIn(0,9999)?:0;notes=draftNotes.trim();teamMode=draftTeamMode
teamAssign=draftTeams.toList();teamNames=listOf(draftTeamA.trim().ifEmpty{"A"},draftTeamB.trim().ifEmpty{"B"})
rounds.clear();rounds.add(newRound(players.size));undoStack.clear();finished=false;active=true;turn=0;big=false}}
else{val sc=totals(rounds,starts);val valid=rounds.isNotEmpty()&&sc.all{it!=null}
LaunchedEffect(sc,valid){if(valid&&!finished&&target>0){val vs=sc.filterNotNull();val hit=if(rule==WinRule.MAX)vs.any{it>=target}else vs.any{it<=target};if(hit)doFinish()}}
if(big)BigScreen(name,players,emojis,sc,finished,rule,{big=false},{confirm="finish"},{reset()})
else GamePage(name,players,emojis,starts,rule,target,teamMode,teamAssign,teamNames,rounds,finished,turn,{turn=it},{big=true},{pushUndo();rounds.add(newRound(players.size))},{confirm="round"},
{if(undoStack.isNotEmpty()){val l=undoStack.removeAt(undoStack.lastIndex);rounds.clear();l.forEach{r->rounds.add(mutableStateListOf<String>().apply{addAll(r)})}}},undoStack.isNotEmpty(),{pushUndo()},{confirm="finish"},{if(finished)reset() else confirm="discard"})}
1->HistoryPage(history,{selected=it},{deleteId=it.id;confirm="delete"},tournaments,{tournaments=it;s.saveTournaments(it)},tourName,{tourName=it},tourPlayers,{tourPlayers=it},tourTarget,{tourTarget=it})
2->StatsPage(history)
3->SettingsPage(theme,{theme=it;s.theme=it},accent,{accent=it;s.accent=it},style,{style=it;s.style=it},english,{english=it;s.english=it},motion,{motion=it;s.animations=it},sound,{sound=it;s.sound=it},history.size,{confirm="clear"},tournaments.size)
}}}}
if(help)HelpDialog{help=false}
selected?.let{HistoryDetails(it){selected=null}}
confirm?.let{a->AlertDialog(onDismissRequest={confirm=null},
title={Text(when(a){"round"->tr("Удалить последний раунд?","Delete last round?");"clear"->tr("Очистить историю?","Clear history?");"delete"->tr("Удалить запись?","Delete entry?");"discard"->tr("Закрыть партию?","Close game?");else->tr("Завершить партию?","Finish game?")})},
text={Text(if(a=="discard"&&!finished)tr("Незавершённая партия будет удалена.","Unfinished game will be discarded.")else tr("Это действие нельзя отменить.","This cannot be undone."))},
confirmButton={TextButton(onClick={when(a){"finish"->doFinish();"discard"->reset();"round"->if(rounds.size>1){pushUndo();rounds.removeAt(rounds.lastIndex)};"clear"->{history=emptyList();s.saveHistory(history)};"delete"->{history=history.filterNot{it.id==deleteId};s.saveHistory(history)}};confirm=null}){Text(tr("Подтвердить","Confirm"))}},
dismissButton={TextButton(onClick={confirm=null}){Text(tr("Отмена","Cancel"))}})}
}}}}

@Composable private fun Page(c:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp),content=c)}
@Composable private fun GlowPanel(mod:Modifier=Modifier,hi:Boolean=false,c:@Composable ColumnScope.()->Unit){
val cs=MaterialTheme.colorScheme;val mo=LocalMotion.current
val st by animateFloatAsState(if(hi).28f else .08f,tween(if(mo)350 else 0),label="gl");val sh=RoundedCornerShape(24.dp)
Surface(mod.fillMaxWidth().shadow(if(hi)14.dp else 3.dp,sh,ambientColor=cs.primary,spotColor=cs.primary),shape=sh,color=cs.surface,
border=BorderStroke(1.dp,Brush.linearGradient(listOf(cs.primary.copy(if(hi).7f else .3f),cs.secondary.copy(.1f),cs.primary.copy(.15f))))){
Column(Modifier.background(Brush.linearGradient(listOf(cs.primary.copy(st),Color.Transparent,cs.secondary.copy(st*.6f)))).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=c)}}
@Composable private fun Heading(t:String,s:String){Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text(t,fontSize=30.sp,lineHeight=36.sp,fontWeight=FontWeight.ExtraBold);Text(s,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
@Composable private fun MainButton(t:String,e:Boolean=true,onClick:()->Unit){Button(onClick=onClick,enabled=e,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp),shape=RoundedCornerShape(18.dp)){Text(t,fontSize=16.sp,fontWeight=FontWeight.Bold)}}
@Composable private fun Choice(t:String,sel:Boolean,onClick:()->Unit){Surface(onClick=onClick,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),
color=if(sel)MaterialTheme.colorScheme.primary.copy(.15f)else MaterialTheme.colorScheme.surfaceVariant.copy(.35f)){
Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){RadioButton(selected=sel,onClick=null);Text(t,modifier=Modifier.weight(1f))}}}

@Composable private fun SetupPage(tpl:Int,onTpl:(Int)->Unit,name:String,onName:(String)->Unit,names:SnapshotStateList<String>,starts:SnapshotStateList<String>,emojis:SnapshotStateList<String>,teams:SnapshotStateList<Int>,rule:WinRule,onRule:(WinRule)->Unit,target:String,onTarget:(String)->Unit,teamMode:Boolean,onTeamMode:(Boolean)->Unit,teamA:String,onTeamA:(String)->Unit,teamB:String,onTeamB:(String)->Unit,notes:String,onNotes:(String)->Unit,onStart:()->Unit){
val clean=names.map{it.trim()}
val errs=clean.map{v->when{ v.isEmpty()->tr("Введите имя","Enter a name");v.length>15->tr("Максимум 15","Max 15");clean.count{it.equals(v,true)}>1->tr("Имя уже есть","Name used");else->null}}
val startsOk=starts.all{it.trim().toIntOrNull() in -9999..9999};val targetOk=target.trim().toIntOrNull() in 0..9999
val ok=name.trim().length in 1..30&&names.size in 2..8&&errs.all{it==null}&&startsOk&&targetOk
Page{
Heading(tr("Время играть","Time to play"),tr("Шаблон подставит цель и правило.","A template sets goal and rule."))
GlowPanel(hi=true){Text(tr("Шаблон игры","Game template"),fontWeight=FontWeight.Bold)
Templates.forEachIndexed{i,t->Choice(if(LocalEnglish.current)t.en else t.ru,tpl==i){onTpl(i)}}}
GlowPanel{Text(tr("Название и цель","Name and target"),fontWeight=FontWeight.Bold)
OutlinedTextField(name,onName,Modifier.fillMaxWidth(),placeholder={Text(tr("Например, Уно","For example, UNO"))},singleLine=true,shape=RoundedCornerShape(16.dp))
OutlinedTextField(target,onTarget,Modifier.fillMaxWidth(),label={Text(tr("Цель (0 — без автофиниша)","Target (0 — off)"))},singleLine=true,shape=RoundedCornerShape(16.dp),isError=!targetOk,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))}
GlowPanel{Text(tr("Участники","Players")+" · ${names.size}/8",fontWeight=FontWeight.Bold)
names.forEachIndexed{i,v->Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
TextButton(onClick={emojis[i]=Emojis[(Emojis.indexOf(emojis[i])+1)%Emojis.size]}){Text(emojis[i],fontSize=22.sp)}
Column(Modifier.weight(1f)){OutlinedTextField(v,{names[i]=it},Modifier.fillMaxWidth(),label={Text(tr("Игрок","Player")+" ${i+1}")},singleLine=true,shape=RoundedCornerShape(16.dp),isError=v.isNotEmpty()&&errs[i]!=null)
Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
OutlinedTextField(starts[i],{starts[i]=it},Modifier.width(96.dp),label={Text(tr("Старт","Start"))},singleLine=true,shape=RoundedCornerShape(12.dp),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
if(teamMode)TextButton(onClick={teams[i]=1-teams[i]}){Text(if(teams[i]==0)teamA.ifEmpty{"A"}else teamB.ifEmpty{"B"})}
if(names.size>2)TextButton(onClick={names.removeAt(i);starts.removeAt(i);emojis.removeAt(i);teams.removeAt(i)},modifier=Modifier.size(48.dp),contentPadding=PaddingValues(0.dp)){Text("×",fontSize=24.sp)}}}}}
OutlinedButton(onClick={names.add("");starts.add("0");emojis.add(Emojis[names.size%Emojis.size]);teams.add(0)},enabled=names.size<8,modifier=Modifier.fillMaxWidth()){Text(tr("+ Добавить игрока","+ Add player"))}}
GlowPanel{Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
Column(Modifier.weight(1f)){Text(tr("Командный режим","Team mode"),fontWeight=FontWeight.Bold);Text(tr("Счёт по командам","Scores by teams"),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
Switch(teamMode,onTeamMode)}
if(teamMode){OutlinedTextField(teamA,onTeamA,Modifier.fillMaxWidth(),label={Text("A")},singleLine=true,shape=RoundedCornerShape(14.dp))
OutlinedTextField(teamB,onTeamB,Modifier.fillMaxWidth(),label={Text("B")},singleLine=true,shape=RoundedCornerShape(14.dp))}
Text(tr("Победа","Win rule"),fontWeight=FontWeight.Bold)
Choice(tr("Больше очков","Higher wins"),rule==WinRule.MAX){onRule(WinRule.MAX)}
Choice(tr("Меньше очков","Lower wins"),rule==WinRule.MIN){onRule(WinRule.MIN)}}
GlowPanel{Text(tr("Заметка","Note"),fontWeight=FontWeight.Bold)
OutlinedTextField(notes,onNotes,Modifier.fillMaxWidth(),placeholder={Text(tr("Где играли","Where you played"))},shape=RoundedCornerShape(16.dp))}
MainButton(tr("Начать партию","Start game"),ok,onStart)}}

@Composable private fun ScoreNumber(v:Long?){val mo=LocalMotion.current
AnimatedContent(v,transitionSpec={val d=if(mo)260 else 0;val up=(targetState?:0L)>=(initialState?:0L)
(slideInVertically(tween(d)){if(up)it else -it}+fadeIn(tween(d))) togetherWith (slideOutVertically(tween(d)){if(up)-it else it}+fadeOut(tween(d)))},label="sc"){
Text(it?.toString()?:"—",fontSize=32.sp,fontWeight=FontWeight.ExtraBold,color=MaterialTheme.colorScheme.primary)}}

@Composable private fun ScoreChart(cum:List<List<Long>>,players:List<String>){
Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant.copy(.35f))){
Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
Text(tr("График по раундам","Round chart"),fontWeight=FontWeight.Bold)
if(cum.isEmpty()||cum[0].isEmpty()){Text(tr("Добавьте раунды","Add rounds"));return@Column}
val all=cum.flatten();val mn=all.minOrNull()!!;val mx=all.maxOrNull()!!;val span=(mx-mn).coerceAtLeast(1L)
Canvas(Modifier.fillMaxWidth().height(160.dp)){val w=size.width;val h=size.height;val n=cum[0].size
cum.forEachIndexed{p,line->val path=Path();line.forEachIndexed{r,v->
val x=if(n==1)w/2 else r*w/(n-1).coerceAtLeast(1);val y=h-((v-mn).toFloat()/span.toFloat())*(h-16)-8
if(r==0)path.moveTo(x,y) else path.lineTo(x,y)};drawPath(path,ChartColors[p%ChartColors.size],style=Stroke(7f))}}
Text(players.mapIndexed{i,n->"● $n"}.joinToString("   "),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}

@Composable private fun BigScreen(name:String,players:List<String>,emojis:List<String>,sc:List<Long?>,finished:Boolean,rule:WinRule,onNormal:()->Unit,onFinish:()->Unit,onNew:()->Unit){
val en=LocalEnglish.current
Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp),horizontalAlignment=Alignment.CenterHorizontally){
Text(name,fontSize=26.sp,fontWeight=FontWeight.ExtraBold)
players.forEachIndexed{i,p->Text("${emojis.getOrElse(i){""}} $p",fontSize=22.sp)
Text(sc[i]?.toString()?:"—",fontSize=64.sp,lineHeight=68.sp,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary)}
if(finished&&sc.all{it!=null})Text(winnerText(players,sc.filterNotNull(),rule,en),fontSize=22.sp,fontWeight=FontWeight.Bold)
OutlinedButton(onClick=onNormal,modifier=Modifier.fillMaxWidth()){Text(tr("Обычный вид","Normal view"))}
if(!finished)MainButton(tr("Завершить","Finish"),sc.all{it!=null},onFinish) else MainButton(tr("Новая партия","New game"),onClick=onNew)}}

@Composable private fun GamePage(name:String,players:List<String>,emojis:List<String>,starts:List<Int>,rule:WinRule,target:Int,teamMode:Boolean,teamAssign:List<Int>,teamNames:List<String>,rounds:SnapshotStateList<SnapshotStateList<String>>,finished:Boolean,turn:Int,onTurn:(Int)->Unit,onBig:()->Unit,onAdd:()->Unit,onRemoveAsk:()->Unit,onUndo:()->Unit,canUndo:Boolean,pushUndo:()->Unit,onFinishAsk:()->Unit,onResetAsk:()->Unit){
val ctx=LocalContext.current;val en=LocalEnglish.current
val sc=totals(rounds,starts);val valid=rounds.isNotEmpty()&&sc.all{it!=null}
val best=if(!valid)null else if(rule==WinRule.MAX)sc.filterNotNull().maxOrNull() else sc.filterNotNull().minOrNull()
val cum=players.indices.map{c->var a=starts.getOrElse(c){0}.toLong();rounds.map{a+=parseScore(it[c])?:0;a}}
var dice by remember{mutableStateOf<Int?>(null)};var coin by remember{mutableStateOf<String?>(null)}
var timerLen by remember{mutableStateOf(0)};var left by remember{mutableStateOf(0)};var running by remember{mutableStateOf(false)}
LaunchedEffect(running,left){if(running&&left>0){delay(1000);left--}else if(running&&left<=0){running=false;if(left==0&&timerLen>0)vibrateNow(ctx)}}
val teamTotals=if(teamMode)listOf(players.indices.filter{teamAssign.getOrElse(it){0}==0}.sumOf{sc[it]?:0L},players.indices.filter{teamAssign.getOrElse(it){0}==1}.sumOf{sc[it]?:0L})else emptyList()
Page{
Heading(name,tr("Игроков: ${players.size} · Раундов: ${rounds.size}","Players: ${players.size} · Rounds: ${rounds.size}"))
Text((if(rule==WinRule.MAX)tr("Больше — лучше","Higher wins")else tr("Меньше — лучше","Lower wins"))+(if(target>0)tr(" · Цель: $target"," · Target: $target")else ""),color=MaterialTheme.colorScheme.primary)
AnimatedVisibility(finished,enter=fadeIn()+expandVertically()){GlowPanel(hi=true){
Text(tr("ИТОГИ","RESULTS"),color=MaterialTheme.colorScheme.primary,letterSpacing=2.sp)
Text(winnerText(players,sc.filterNotNull(),rule,en),fontSize=24.sp,fontWeight=FontWeight.Bold)}}
if(teamMode&&valid)GlowPanel(hi=true){Text(tr("Команды","Teams"),fontWeight=FontWeight.Bold)
teamTotals.forEachIndexed{i,v->Text("${teamNames.getOrElse(i){"T${i+1}"}}: $v",fontSize=20.sp,fontWeight=FontWeight.Bold)}}
GlowPanel{Text(tr("Ход и таймер","Turn and timer"),fontWeight=FontWeight.Bold)
Text(tr("Ходит: ${emojis.getOrElse(turn){""}} ${players.getOrElse(turn){""}}","To move: ${emojis.getOrElse(turn){""}} ${players.getOrElse(turn){""}}"))
Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={onTurn((turn+1)%players.size)},enabled=!finished){Text(tr("Дальше ›","Next ›"))}
OutlinedButton(onClick={dice=Random.nextInt(1,7)}){Text("🎲 ${dice?:"–"}")}
OutlinedButton(onClick={coin=if(Random.nextBoolean())tr("Орёл","Heads")else tr("Решка","Tails")}){Text("🪙 ${coin?:"–"}")}}
OutlinedButton(onClick={onTurn(Random.nextInt(players.size))}){Text("🎲")}
Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf(0,30,60,90).forEach{d->FilterChip(d==timerLen,{timerLen=d;left=d;running=false},label={Text(if(d==0)tr("Выкл","Off")else "${d}c")})}}
if(timerLen>0){Text(tr("Осталось: $left c","Left: ${left}s"),fontSize=20.sp,fontWeight=FontWeight.Bold,color=if(left==0)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
if(left==0)Text(tr("⏰ Время вышло!","⏰ Time's up!"),color=MaterialTheme.colorScheme.error,fontWeight=FontWeight.Bold)
Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={if(left<=0)left=timerLen;running=true},enabled=!running){Text(tr("Старт","Start"))}
OutlinedButton(onClick={running=false}){Text(tr("Пауза","Pause"))};OutlinedButton(onClick={running=false;left=timerLen}){Text(tr("Сброс","Reset"))}}}}
Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(12.dp)){
players.forEachIndexed{i,p->val lead=best!=null&&sc[i]==best
GlowPanel(modifier=Modifier.width(178.dp),hi=lead){
Text("${emojis.getOrElse(i){""}} $p",fontWeight=FontWeight.Bold)
if(i==turn&&!finished)Text(tr("● ходит","● to move"),color=MaterialTheme.colorScheme.primary,fontSize=12.sp)
ScoreNumber(sc[i])
if(!finished&&valid)Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
listOf(1,5,10,-1).forEach{d->OutlinedButton(onClick={pushUndo();val last=rounds.last();val cur=parseScore(last[i])?:0;last[i]=(cur+d).coerceIn(-9999,9999).toString()},contentPadding=PaddingValues(0.dp),modifier=Modifier.size(48.dp)){Text(if(d>0)"+$d" else "$d",fontSize=13.sp)}}}}}}
ScoreChart(cum,players)
GlowPanel{Text(tr("Очки по раундам","Round scores"),fontWeight=FontWeight.Bold)
Column(Modifier.horizontalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Text("№",modifier=Modifier.width(36.dp))
players.forEach{Text(it,modifier=Modifier.width(108.dp),color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)}}
rounds.forEachIndexed{ri,row->key(ri){var vis by remember{mutableStateOf(!LocalMotion.current)};LaunchedEffect(Unit){delay(16);vis=true}
AnimatedVisibility(vis||!LocalMotion.current,enter=fadeIn()+expandVertically()){
Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
Text("${ri+1}".padStart(2,'0'),modifier=Modifier.width(36.dp))
row.forEachIndexed{c,v->Column(Modifier.width(108.dp),horizontalAlignment=Alignment.CenterHorizontally){
OutlinedTextField(v,{row[c]=it},Modifier.fillMaxWidth(),readOnly=finished,singleLine=true,isError=parseScore(v)==null,shape=RoundedCornerShape(14.dp),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
if(!finished)TextButton(onClick={row[c]=if(v.startsWith("-"))v.removePrefix("-")else "-"+v.removePrefix("+")},modifier=Modifier.heightIn(min=48.dp)){Text("±",fontSize=18.sp)}}}}}}}}
if(!valid)Text(tr("Нужны целые числа −9999..9999.","Need whole numbers −9999..9999."),color=MaterialTheme.colorScheme.error)}
if(!finished){
OutlinedButton(onClick=onAdd,enabled=valid,modifier=Modifier.fillMaxWidth().heightIn(min=50.dp)){Text(tr("+ Добавить раунд","+ Add round"))}
Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
OutlinedButton(onClick=onUndo,enabled=canUndo,modifier=Modifier.weight(1f)){Text(tr("↩ Отмена","↩ Undo"))}
OutlinedButton(onClick=onRemoveAsk,enabled=rounds.size>1,modifier=Modifier.weight(1f)){Text(tr("Удалить раунд","Delete round"))}
OutlinedButton(onClick=onBig,modifier=Modifier.weight(1f)){Text(tr("⛶ Экран","⛶ Big"))}}
MainButton(tr("Завершить партию","Finish game"),valid,onFinishAsk)
TextButton(onClick=onResetAsk,modifier=Modifier.fillMaxWidth()){Text(tr("Отменить партию","Discard game"))}}
else MainButton(tr("Новая партия","New game"),onClick=onResetAsk)}}

@Composable private fun HistoryPage(games:List<SavedGame>,onOpen:(SavedGame)->Unit,onDelete:(SavedGame)->Unit,tours:List<Tournament>,onTours:(List<Tournament>)->Unit,tName:String,onTName:(String)->Unit,tPlayers:String,onTPlayers:(String)->Unit,tTarget:String,onTTarget:(String)->Unit){
val en=LocalEnglish.current
Page{
Heading(tr("История","History"),tr("Партии и турниры","Games and tournaments"))
if(games.isEmpty())GlowPanel{Text(tr("Здесь появятся ваши игры","Your games will appear here"),fontSize=22.sp,fontWeight=FontWeight.Bold)
Text(tr("Завершите первую партию.","Finish your first game."))}
games.forEach{g->key(g.id){GlowPanel{
Text(dateText(g.date),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
Text(g.name,fontSize=23.sp,fontWeight=FontWeight.Bold)
Text(winnerText(g.players,savedTotals(g),g.rule,en),color=MaterialTheme.colorScheme.primary)
Text(tr("Игроков: ${g.players.size} · Раундов: ${g.rounds.size}","Players: ${g.players.size} · Rounds: ${g.rounds.size}"))
if(g.notes.isNotEmpty())Text("📝 ${g.notes}",fontSize=13.sp)
Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
TextButton(onClick={onOpen(g)}){Text(tr("Подробнее","Details"))}
TextButton(onClick={onDelete(g)}){Text(tr("Удалить","Delete"),color=MaterialTheme.colorScheme.error)}}}}}
GlowPanel(hi=true){Text(tr("Турнир до N побед","Tournament to N wins"),fontWeight=FontWeight.Bold)
OutlinedTextField(tName,onTName,Modifier.fillMaxWidth(),label={Text(tr("Название турнира","Tournament name"))},singleLine=true,shape=RoundedCornerShape(14.dp))
OutlinedTextField(tPlayers,onTPlayers,Modifier.fillMaxWidth(),label={Text(tr("Игроки через запятую","Players, comma separated"))},singleLine=true,shape=RoundedCornerShape(14.dp))
OutlinedTextField(tTarget,onTTarget,Modifier.fillMaxWidth(),label={Text(tr("Побед для титула","Wins for title"))},singleLine=true,shape=RoundedCornerShape(14.dp),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
MainButton(tr("Создать турнир","Create tournament"),tName.trim().isNotEmpty()&&tPlayers.split(",").map{it.trim()}.filter{it.isNotEmpty()}.size>=2){
val pl=tPlayers.split(",").map{it.trim()}.filter{it.isNotEmpty()}.take(8)
val tw=tTarget.trim().toIntOrNull()?.coerceIn(1,20)?:3
onTours(listOf(Tournament(UUID.randomUUID().toString(),tName.trim(),System.currentTimeMillis(),pl,tw,List(pl.size){0}))+tours);onTName("");onTPlayers("")}
tours.forEach{t->key(t.id){Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
Text(t.name,fontWeight=FontWeight.Bold);Text(tr("Цель: ${t.targetWins} победы","Target: ${t.targetWins} wins"),fontSize=12.sp)
t.players.forEachIndexed{i,p->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
Text("$p — ${t.wins.getOrElse(i){0}}"+if((t.wins.getOrElse(i){0})>=t.targetWins)" 🏆" else "")
Row{TextButton(onClick={val nw=t.wins.toMutableList();nw[i]++;onTours(tours.map{if(it.id==t.id)it.copy(wins=nw)else it})}){Text("+1")}
TextButton(onClick={val nw=t.wins.toMutableList();if(nw[i]>0)nw[i]--;onTours(tours.map{if(it.id==t.id)it.copy(wins=nw)else it})}){Text("-1")}}}}
TextButton(onClick={onTours(tours.filterNot{it.id==t.id})}){Text(tr("Удалить турнир","Delete tournament"),color=MaterialTheme.colorScheme.error)}}}}}}

@Composable private fun StatsPage(games:List<SavedGame>){
val en=LocalEnglish.current
val names=games.flatMap{it.players}.distinct()
Page{
Heading(tr("Статистика","Stats"),tr("Кто чаще побеждает","Who wins most"))
if(games.isEmpty()){GlowPanel{Text(tr("Нет данных","No data"),fontWeight=FontWeight.Bold)};return@Page}
names.sorted().forEach{n->
val played=games.count{g->g.players.any{it.equals(n,true)}}
val res=games.mapNotNull{g->val idx=g.players.indexOfFirst{it.equals(n,true)};if(idx<0)null else{val tt=savedTotals(g);val best=if(g.rule==WinRule.MAX)tt.maxOrNull()!! else tt.minOrNull()!!;val win=tt[idx]==best;val tie=tt.count{it==best}>1;Triple(tt[idx],win,tie)}}
val wins=res.count{it.second&&!it.third};val ties=res.count{it.third}
val avg=res.map{it.first}.average();val bestScore=if(en||true)res.map{it.first}.maxOrNull()?:0 else 0
GlowPanel{Text(n,fontSize=20.sp,fontWeight=FontWeight.Bold)
Text(tr("Игр: $played · Побед: $wins · Ничьих: $ties","Games: $played · Wins: $wins · Ties: $ties"))
Text(tr("Винрейт: ${if(played>0)"${wins*100/played}%" else "—"} · Средний итог: ${"%.1f".format(avg)} · Лучший: $bestScore","Winrate: ${if(played>0)"${wins*100/played}%" else "—"} · Avg: ${"%.1f".format(avg)} · Best: $bestScore")}}}}

@Composable private fun HistoryDetails(g:SavedGame,onDismiss:()->Unit){
val scores=savedTotals(g);val en=LocalEnglish.current
AlertDialog(onDismissRequest=onDismiss,title={Text(g.name)},
text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)){
Text(dateText(g.date));Text(winnerText(g.players,scores,g.rule,en),color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)
Text(if(g.rule==WinRule.MAX)tr("Больше — лучше","Higher wins")else tr("Меньше — лучше","Lower wins"))
if(g.notes.isNotEmpty())Text("📝 ${g.notes}")
g.players.forEachIndexed{i,p->Text("$p: ${scores[i]}")}
HorizontalDivider();Text(tr("Раунды","Rounds"),fontWeight=FontWeight.Bold)
g.rounds.forEachIndexed{i,r->Text(tr("Раунд","Round")+" ${i+1}\n"+g.players.mapIndexed{c,p->"$p: ${r[c]}"}.joinToString(" · "))}}},
confirmButton={TextButton(onClick=onDismiss){Text(tr("Закрыть","Close"))}})}

@Composable private fun SettingsPage(theme:String,onTheme:(String)->Unit,accent:Int,onAccent:(Int)->Unit,style:Int,onStyle:(Int)->Unit,english:Boolean,onEnglish:(Boolean)->Unit,motion:Boolean,onMotion:(Boolean)->Unit,sound:Boolean,onSound:(Boolean)->Unit,historyCount:Int,onClear:()->Unit,tourCount:Int){
Page{
Heading(tr("Настройки","Settings"),tr("Ваш стиль. Ваша игра.","Your style. Your game."))
GlowPanel(hi=true){Text(tr("Язык / Language","Language"),fontWeight=FontWeight.Bold)
Choice("Русский",!english){onEnglish(false)};Choice("English",english){onEnglish(true)}}
GlowPanel{Text(tr("Тема","Theme"),fontWeight=FontWeight.Bold)
Choice(tr("Как на устройстве","System"),theme=="system"){onTheme("system")}
Choice(tr("Светлая","Light"),theme=="light"){onTheme("light")}
Choice(tr("Тёмная","Dark"),theme=="dark"){onTheme("dark")}
Choice("AMOLED",theme=="amoled"){onTheme("amoled")}}
GlowPanel{Text(tr("Стиль фона","Background style"),fontWeight=FontWeight.Bold)
listOf(tr("Стандарт","Standard"),tr("Закат","Sunset"),tr("Лес","Forest"),"AMOLED").forEachIndexed{i,t->Choice(t,style==i){onStyle(i)}}}
GlowPanel{Text(tr("Акцент","Accent"),fontWeight=FontWeight.Bold)
listOf(tr("Аметист","Amethyst"),tr("Океан","Ocean"),tr("Изумруд","Emerald")).forEachIndexed{i,t->Choice(t,accent==i){onAccent(i)}}}
GlowPanel{Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
Column(Modifier.weight(1f)){Text(tr("Анимации","Animations"),fontWeight=FontWeight.Bold);Text(tr("Переходы и счёт","Transitions and score"),fontSize=13.sp)}
Switch(motion,onMotion)}
Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
Column(Modifier.weight(1f)){Text(tr("Звук победы","Victory sound"),fontWeight=FontWeight.Bold);Text(tr("Фанфары + вибрация","Fanfare + vibration"),fontSize=13.sp)}
Switch(sound,onSound)}}
GlowPanel{Text(tr("Данные","Data"),fontWeight=FontWeight.Bold)
Text(tr("Партий: $historyCount · Турниров: $tourCount","Games: $historyCount · Tournaments: $tourCount"))
OutlinedButton(onClick=onClear,enabled=historyCount>0,modifier=Modifier.fillMaxWidth()){Text(tr("Очистить историю","Clear history"))}}}}

@Composable private fun HelpDialog(onDismiss:()->Unit){
AlertDialog(onDismissRequest=onDismiss,title={Text(tr("Во что поиграть?","What to play?"))},
text={Column(Modifier.heightIn(max=440.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)){
Text(tr("Приложение складывает очки и сравнивает суммы. Правила конкретных игр проверяйте сами.","The app adds scores and compares totals. Check specific game rules yourself."))
HelpItem("UNO",tr("Записывайте очки победителя раздачи. Правило «Больше». Завершите вручную на цели 500.","Record hand winner points. Rule Higher. Finish manually at 500."))
HelpItem(tr("Скрэббл / Эрудит","Scrabble"),tr("Очки за ходы. Большая сумма. Штрафы — отдельным раундом.","Turn scores. Highest total. Penalties as separate round."))
HelpItem(tr("Рамми","Rummy"),tr("Результаты раздач. Правило зависит от вашего варианта.","Hand results. Rule depends on your variant."))
HelpItem(tr("Домино","Dominoes"),tr("Для штрафных очков — правило «Меньше».","For penalty points use Lower rule."))
HelpItem(tr("Ятцы","Yahtzee"),tr("Категории — отдельными раундами. Проверки категорий нет.","Categories as rounds. No validation."))
HelpItem(tr("Викторины","Quizzes"),tr("Очки за ответы. Для команд используйте названия команд.","Points for answers. Use team names for teams."))
HorizontalDivider()
HelpItem(tr("Как пользоваться","How to use"),tr("1. Создайте партию 2–8 участников.\n2. Выберите правило.\n3. Нет очков — оставьте 0.\n4. ± меняет знак.\n5. Быстрые кнопки добавляют к последнему раунду.\n6. Завершите — результат уйдёт в историю и турниры.","1. Create 2–8 players.\n2. Pick rule.\n3. No points — leave 0.\n4. ± flips sign.\n5. Quick buttons add to last round.\n6. Finish — result goes to history and tournaments."))
HelpItem(tr("Сохранение","Saving"),tr("История и настройки на устройстве. Незавершённая партия в памяти и может потеряться. Удаление приложения удаляет данные.","History and settings on device. Unfinished game is in memory. Uninstall removes data."))}},
confirmButton={TextButton(onClick=onDismiss){Text(tr("Понятно","Got it"))}})}
@Composable private fun HelpItem(t:String,d:String){Column(verticalArrangement=Arrangement.spacedBy(6.dp)){Text(t,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold,fontSize=16.sp);Text(d)}}
