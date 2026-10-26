package com.example.boardgamescore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun HelpDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(tr("Помощь", "Help"))
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = tr(
                        "Приложение складывает стартовые очки и результаты " +
                            "раундов. Особые правила игр нужно учитывать самостоятельно.",
                        "The app adds starting scores and round results. " +
                            "Special game rules must be handled manually."
                    )
                )

                HelpItem(
                    title = tr("Как начать", "Getting started"),
                    description = tr(
                        "1. Выберите шаблон или свободную игру.\n" +
                            "2. Добавьте от 2 до 8 участников с разными именами.\n" +
                            "3. При необходимости укажите стартовые очки.\n" +
                            "4. Выберите правило победы.\n" +
                            "5. Начните партию и записывайте очки по раундам.",
                        "1. Select a template or a free game.\n" +
                            "2. Add 2–8 players with distinct names.\n" +
                            "3. Set starting scores if needed.\n" +
                            "4. Choose the win rule.\n" +
                            "5. Start the game and enter round scores."
                    )
                )

                HelpItem(
                    title = tr("Ввод очков", "Entering scores"),
                    description = tr(
                        "Допустимы целые числа от −9999 до 9999 в каждой ячейке. " +
                            "Быстрые кнопки меняют последний раунд. " +
                            "Кнопка ± меняет знак. Ошибки ввода нужно исправить " +
                            "перед завершением партии.",
                        "Each cell accepts whole numbers from −9999 to 9999. " +
                            "Quick buttons change the last round. " +
                            "The ± button changes the sign. Fix invalid entries " +
                            "before finishing the game."
                    )
                )

                HelpItem(
                    title = tr("Цель и лимит", "Target and limit"),
                    description = tr(
                        "Пустое значение или 0 отключает цель. " +
                            "Для правила «Больше очков» это целевая сумма, " +
                            "для правила «Меньше очков» — лимит штрафов. " +
                            "Достижение цели показывает подсказку. " +
                            "Партия завершается только по вашей команде.",
                        "An empty value or 0 disables the target. " +
                            "For “Higher wins”, it is a target score; " +
                            "for “Lower wins”, it is a penalty limit. " +
                            "Reaching it displays a hint. " +
                            "You decide when to finish the game."
                    )
                )

                HelpItem(
                    title = tr("Команды", "Teams"),
                    description = tr(
                        "В каждой команде нужен хотя бы один игрок. " +
                            "Переключайте команду кнопкой рядом со стартовыми очками. " +
                            "Командный итог — сумма результатов участников. " +
                            "Победа команды учитывается каждому её игроку.",
                        "Each team needs at least one player. " +
                            "Switch teams using the button next to starting scores. " +
                            "A team's result is the sum of its players' scores. " +
                            "A team win counts for each teammate."
                    )
                )

                HelpItem(
                    title = tr("Турниры", "Tournaments"),
                    description = tr(
                        "Создайте турнир до заданного числа побед. " +
                            "Автозачёт предназначен для единственного подходящего " +
                            "незавершённого турнира с теми же игроками. " +
                            "Порядок имён не важен. Ничья и командная партия " +
                            "не начисляют победу автоматически. " +
                            "При нескольких подходящих турнирах используйте +1 вручную. " +
                            "Удаление партии из истории не меняет турнирный счёт.",
                        "Create a tournament with a target number of wins. " +
                            "Automatic credit is intended for exactly one matching " +
                            "unfinished tournament with the same players. " +
                            "Name order does not matter. Draws and team games " +
                            "do not automatically award a win. " +
                            "If several tournaments match, use +1 manually. " +
                            "Deleting a game from history does not change tournament wins."
                    )
                )

                HorizontalDivider()

                HelpItem(
                    title = "UNO",
                    description = tr(
                        "Записывайте очки победителя каждой раздачи. " +
                            "Выберите «Больше очков» и цель 500.",
                        "Record the winner's points for each hand. " +
                            "Choose “Higher wins” and a target of 500."
                    )
                )

                HelpItem(
                    title = tr("Манчкин", "Munchkin"),
                    description = tr(
                        "Укажите начальный уровень в стартовых очках " +
                            "и записывайте изменения уровня. Особые условия " +
                            "получения победного уровня проверяйте самостоятельно.",
                        "Set the initial level as the starting score " +
                            "and record level changes. Check the special conditions " +
                            "for reaching the winning level yourself."
                    )
                )

                HelpItem(
                    title = tr("Скрэббл / Эрудит", "Scrabble"),
                    description = tr(
                        "Записывайте очки за ходы. Побеждает большая сумма. " +
                            "Штрафы можно внести отдельным раундом.",
                        "Record points for turns. The highest total wins. " +
                            "Penalties can be entered as a separate round."
                    )
                )

                HelpItem(
                    title = tr("Рамми и домино", "Rummy and dominoes"),
                    description = tr(
                        "Выбирайте правило победы по вашей версии игры. " +
                            "Для штрафных очков обычно подходит «Меньше очков».",
                        "Choose the win rule for your version of the game. " +
                            "“Lower wins” usually suits penalty scoring."
                    )
                )

                HelpItem(
                    title = tr("Ятцы", "Yahtzee"),
                    description = tr(
                        "Записывайте очки категорий по раундам. " +
                            "Бонусы вносите отдельно. Проверка категорий " +
                            "и комбинаций не автоматизирована.",
                        "Record category scores by round. " +
                            "Enter bonuses separately. Categories and combinations " +
                            "are not validated automatically."
                    )
                )

                HelpItem(
                    title = tr("Викторины", "Quizzes"),
                    description = tr(
                        "Начисляйте очки за ответы. " +
                            "Для командной викторины включите командный режим.",
                        "Award points for answers. " +
                            "Enable team mode for a team quiz."
                    )
                )

                HorizontalDivider()

                HelpItem(
                    title = tr("Таймер", "Timer"),
                    description = tr(
                        "Таймер — вспомогательный инструмент текущего экрана, " +
                            "а не фоновый будильник. При смене экрана или " +
                            "пересоздании приложения его состояние может сброситься.",
                        "The timer is a tool for the current screen, " +
                            "not a background alarm. Its state may reset " +
                            "when changing screens or recreating the app."
                    )
                )

                HelpItem(
                    title = tr("Сохранение", "Saving"),
                    description = tr(
                        "История, турниры и настройки сохраняются на устройстве. " +
                            "Незавершённая партия пока хранится в памяти: " +
                            "она может потеряться при пересоздании Activity, " +
                            "например при повороте экрана, или завершении процесса. " +
                            "Облачной синхронизации нет.",
                        "History, tournaments and settings are saved on the device. " +
                            "An unfinished game is currently kept in memory: " +
                            "it may be lost when the Activity is recreated, " +
                            "for example on rotation, or when the process ends. " +
                            "There is no cloud synchronization."
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
            text = description,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}