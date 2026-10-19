package com.example.boardgamescore

import androidx.compose.ui.graphics.Color

internal val Templates = listOf(
    Template(
        ru = "Свободная игра",
        en = "Free game",
        rule = WinRule.MAX,
        target = 0
    ),
    Template(
        ru = "Уно — до 500",
        en = "UNO — to 500",
        rule = WinRule.MAX,
        target = 500
    ),
    Template(
        ru = "Манчкин — до 10",
        en = "Munchkin — to 10",
        rule = WinRule.MAX,
        target = 10
    ),
    Template(
        ru = "Скрэббл / Эрудит",
        en = "Scrabble",
        rule = WinRule.MAX,
        target = 0
    ),
    Template(
        ru = "Рамми",
        en = "Rummy",
        rule = WinRule.MAX,
        target = 0
    ),
    Template(
        ru = "Домино — штрафы",
        en = "Dominoes — penalties",
        rule = WinRule.MIN,
        target = 0
    ),
    Template(
        ru = "Ятцы — кости",
        en = "Yahtzee — dice",
        rule = WinRule.MAX,
        target = 0
    ),
    Template(
        ru = "Викторина",
        en = "Quiz",
        rule = WinRule.MAX,
        target = 0
    )
)

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