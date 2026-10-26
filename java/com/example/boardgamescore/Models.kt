package com.example.boardgamescore

enum class WinRule {
    MAX,
    MIN
}

data class Template(
    val ru: String,
    val en: String,
    val rule: WinRule,
    val target: Int,
    val descRu: String = "",
    val descEn: String = ""
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

internal data class PlayerStats(
    val name: String,
    val played: Int,
    val wins: Int,
    val ties: Int
)