package com.example.boardgamescore

import androidx.compose.ui.graphics.Color

internal val Templates = listOf(
    Template(
        ru = "Свободная игра",
        en = "Free game",
        rule = WinRule.MAX,
        target = 0,
        descRu = "Очки записываются вручную по раундам по любым правилам. " +
            "Победа определяется выбранным правилом: больше или меньше очков.",
        descEn = "Scores are entered manually by rounds using any rules. " +
            "The winner is decided by the chosen rule: higher or lower total."
    ),
    Template(
        ru = "Уно — до 500",
        en = "UNO — to 500",
        rule = WinRule.MAX,
        target = 500,
        descRu = "После каждой раздачи запишите очки её победителя. " +
            "Выигрыш происходит при 500 очках: кто первым набрал сумму — тот победил.",
        descEn = "After each hand, record the hand winner's points. " +
            "The win occurs at 500 points: whoever reaches the total first wins."
    ),
    Template(
        ru = "Манчкин — до 10",
        en = "Munchkin — to 10",
        rule = WinRule.MAX,
        target = 10,
        descRu = "Записывайте уровень игрока по раундам. " +
            "Выигрыш происходит на 10 уровне: первый достигший — победитель.",
        descEn = "Track each player's level by rounds. " +
            "The win occurs at level 10: the first to reach it wins."
    ),
    Template(
        ru = "Скрэббл / Эрудит",
        en = "Scrabble",
        rule = WinRule.MAX,
        target = 0,
        descRu = "Записывайте очки за выложенные слова по ходам. " +
            "Игра заканчивается, когда фишки закончились; победа — у наибольшей суммы очков.",
        descEn = "Record points for words placed each turn. " +
            "The game ends when the tiles run out; the highest total wins."
    ),
    Template(
        ru = "Рамми",
        en = "Rummy",
        rule = WinRule.MAX,
        target = 0,
        descRu = "Раунд заканчивается, когда один игрок избавился от карт. " +
            "Очки считаются по картам, оставшимся у соперников; победа — у наибольшей суммы.",
        descEn = "A round ends when a player goes out. " +
            "Points are counted from cards left with opponents; the highest total wins."
    ),
    Template(
        ru = "Домино — штрафы",
        en = "Dominoes — penalties",
        rule = WinRule.MIN,
        target = 0,
        descRu = "В конце раунда игрок получает штрафные очки за оставшиеся кости. " +
            "Победа — у наименьшей суммы штрафов после всех раундов.",
        descEn = "At the end of a round, a player earns penalty points for remaining tiles. " +
            "The lowest penalty total after all rounds wins."
    ),
    Template(
        ru = "Ятцы — кости",
        en = "Yahtzee — dice",
        rule = WinRule.MAX,
        target = 0,
        descRu = "15 категорий, каждая заполняется один раз. " +
            "Бонус 35 очков — за 63 и больше в верхней части. " +
            "Победа — у наибольшей суммы после заполнения всех категорий.",
        descEn = "15 categories, each filled once. " +
            "A 35-point bonus is granted for 63 or more in the upper section. " +
            "The highest total after all categories wins."
    ),
    Template(
        ru = "Викторина",
        en = "Quiz",
        rule = WinRule.MAX,
        target = 0,
        descRu = "Очки начисляются за верные ответы: один раунд — один вопрос или тур. " +
            "Победа — у наибольшей суммы; для команд включите командный режим.",
        descEn = "Points are awarded for correct answers: one round is one question or stage. " +
            "The highest total wins; enable team mode for team play."
    )
)

internal fun fairPlayNote(english: Boolean): String {
    return if (english) {
        "SCORE CLUB is a scoreboard for friendly board games. " +
            "Betting, wagers and any form of gambling are prohibited."
    } else {
        "SCORE CLUB — счётчик очков для дружеских настольных игр. " +
            "Ставки, денежные пари и любые азартные игры запрещены."
    }
}

internal val Emojis = listOf(
    "🦊",
    "🐼",
    "🦁",
    "🐸",
    "🐯",
    "🦄",
    "⚡",
    "🎲",
    "🌟",
    "🍀"
)

internal val ChartColors = listOf(
    Color(0xFFB79CFF),
    Color(0xFF7ADBCB),
    Color(0xFFFFB87A),
    Color(0xFF8ECFFF),
    Color(0xFFFF9EB5),
    Color(0xFFE2D58C),
    Color(0xFF9FAEFF),
    Color(0xFFD7A3E5)
)