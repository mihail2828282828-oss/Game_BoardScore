package com.example.boardgamescore

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun parseScore(text: String): Int? {
    return text.trim()
        .toIntOrNull()
        ?.takeIf { it in -9999..9999 }
}

internal fun parseTarget(text: String): Int? {
    if (text.isBlank()) return 0

    return text.trim()
        .toIntOrNull()
        ?.takeIf { it in 0..9999 }
}

internal fun normalizedPlayerName(name: String): String {
    return name.trim().lowercase(Locale.ROOT)
}

internal fun gameNameForTemplate(
    templateIndex: Int,
    customName: String,
    english: Boolean
): String {
    if (templateIndex == 0) {
        return customName.trim()
    }

    val template = Templates.getOrNull(templateIndex)
        ?: return customName.trim()

    return if (english) template.en else template.ru
}

internal fun newRound(
    playerCount: Int
): SnapshotStateList<String> {
    return mutableStateListOf<String>().apply {
        repeat(playerCount) {
            add("0")
        }
    }
}

internal fun totals(
    rounds: List<List<String>>,
    starts: List<Int>
): List<Long?> {
    val playerCount = rounds.firstOrNull()?.size ?: starts.size

    if (rounds.any { it.size != playerCount }) {
        return List(playerCount) { null }
    }

    return List(playerCount) { column ->
        var total = starts.getOrElse(column) { 0 }.toLong()
        var valid = true

        rounds.forEach { round ->
            val value = parseScore(round[column])

            if (value == null) {
                valid = false
            } else {
                total += value
            }
        }

        if (valid) total else null
    }
}

internal fun savedTotals(game: SavedGame): List<Long> {
    require(
        game.rounds.all { it.size == game.players.size }
    ) {
        "Количество очков в раунде не соответствует количеству игроков."
    }

    return game.players.indices.map { column ->
        game.starts.getOrElse(column) { 0 }.toLong() +
            game.rounds.sumOf { round ->
                round[column].toLong()
            }
    }
}

internal fun teamScores(
    scores: List<Long?>,
    assignments: List<Int>
): List<Long?> {
    if (
        scores.size != assignments.size ||
        assignments.any { it !in 0..1 } ||
        !assignments.contains(0) ||
        !assignments.contains(1)
    ) {
        return listOf(null, null)
    }

    return List(2) { team ->
        val indexes = assignments.indices.filter {
            assignments[it] == team
        }

        if (indexes.any { scores[it] == null }) {
            null
        } else {
            indexes.sumOf { scores[it]!! }
        }
    }
}

internal fun savedTeamTotals(game: SavedGame): List<Long> {
    require(game.teamMode) {
        "Партия не является командной."
    }

    val results = teamScores(
        scores = savedTotals(game),
        assignments = game.teamAssign
    )

    require(results.all { it != null }) {
        "Некорректное распределение игроков по командам."
    }

    return results.filterNotNull()
}

internal fun winnerIndexes(
    scores: List<Long>,
    rule: WinRule
): List<Int> {
    if (scores.isEmpty()) return emptyList()

    val best = when (rule) {
        WinRule.MAX -> scores.maxOrNull()!!
        WinRule.MIN -> scores.minOrNull()!!
    }

    return scores.indices.filter { index ->
        scores[index] == best
    }
}

internal fun winnerText(
    players: List<String>,
    scores: List<Long>,
    rule: WinRule,
    english: Boolean
): String {
    if (scores.isEmpty() || players.size != scores.size) {
        return if (english) {
            "No valid results"
        } else {
            "Нет корректных результатов"
        }
    }

    val indexes = winnerIndexes(scores, rule)
    val names = indexes.map { players[it] }

    val prefix = if (names.size == 1) {
        if (english) "Winner: " else "Победитель: "
    } else {
        if (english) "Tie: " else "Ничья: "
    }

    return prefix + names.joinToString(", ")
}

internal fun teamWinnerText(
    names: List<String>,
    scores: List<Long>,
    rule: WinRule,
    english: Boolean
): String {
    return winnerText(
        players = names,
        scores = scores,
        rule = rule,
        english = english
    )
}

internal fun savedWinnerText(
    game: SavedGame,
    english: Boolean
): String {
    return if (game.teamMode) {
        teamWinnerText(
            names = game.teamNames,
            scores = savedTeamTotals(game),
            rule = game.rule,
            english = english
        )
    } else {
        winnerText(
            players = game.players,
            scores = savedTotals(game),
            rule = game.rule,
            english = english
        )
    }
}

/**
 * Накопленные результаты игроков по раундам.
 * null означает, что таблица содержит некорректные значения.
 */
internal fun cumulativeScores(
    rounds: List<List<String>>,
    starts: List<Int>,
    playerCount: Int
): List<List<Long>>? {
    if (rounds.any { it.size != playerCount }) return null

    val parsedRounds = mutableListOf<List<Int>>()

    for (round in rounds) {
        val parsed = round.map { parseScore(it) }

        if (parsed.any { it == null }) return null

        parsedRounds.add(parsed.filterNotNull())
    }

    return List(playerCount) { player ->
        var score = starts.getOrElse(player) { 0 }.toLong()

        parsedRounds.map { round ->
            score += round[player]
            score
        }
    }
}

/**
 * Для MAX — достижение целевого количества очков.
 * Для MIN — достижение лимита штрафов.
 *
 * Эта проверка должна показывать подсказку, а не автоматически
 * сохранять партию во время редактирования поля.
 */
internal fun targetReached(
    scores: List<Long?>,
    target: Int
): Boolean {
    return target > 0 &&
        scores.isNotEmpty() &&
        scores.all { it != null } &&
        scores.filterNotNull().any { it >= target }
}

internal fun dateText(timestamp: Long): String {
    return SimpleDateFormat(
        "dd.MM.yyyy · HH:mm",
        Locale.getDefault()
    ).format(Date(timestamp))
}

internal fun calculateStats(
    games: List<SavedGame>
): List<PlayerStats> {
    val names = games
        .flatMap { it.players }
        .distinctBy(::normalizedPlayerName)
        .sortedBy(::normalizedPlayerName)

    return names.map { name ->
        var played = 0
        var wins = 0
        var ties = 0

        games.forEach { game ->
            val playerIndex = game.players.indexOfFirst {
                normalizedPlayerName(it) == normalizedPlayerName(name)
            }

            if (playerIndex >= 0) {
                val scores = if (game.teamMode) {
                    savedTeamTotals(game)
                } else {
                    savedTotals(game)
                }

                val participantIndex = if (game.teamMode) {
                    game.teamAssign[playerIndex]
                } else {
                    playerIndex
                }

                val winners = winnerIndexes(scores, game.rule)

                played++

                if (participantIndex in winners) {
                    if (winners.size == 1) {
                        wins++
                    } else {
                        ties++
                    }
                }
            }
        }

        PlayerStats(
            name = name,
            played = played,
            wins = wins,
            ties = ties
        )
    }
}

/**
 * Автоматически начисляет победу, только когда:
 * - партия не командная;
 * - победитель один;
 * - существует ровно один подходящий незавершённый турнир.
 *
 * При нескольких подходящих турнирах список не меняется:
 * пользователь должен начислить победу вручную.
 */
internal fun creditTournamentWin(
    tournaments: List<Tournament>,
    game: SavedGame
): List<Tournament> {
    if (game.teamMode) return tournaments

    val winners = winnerIndexes(
        scores = savedTotals(game),
        rule = game.rule
    )

    if (winners.size != 1) return tournaments

    val gameNames = game.players
        .map(::normalizedPlayerName)
        .toSet()

    val eligible = tournaments.filter { tournament ->
        tournament.players.size == game.players.size &&
            tournament.wins.size == tournament.players.size &&
            tournament.wins.none { it >= tournament.targetWins } &&
            tournament.players
                .map(::normalizedPlayerName)
                .toSet() == gameNames
    }

    if (eligible.size != 1) return tournaments

    val selected = eligible.single()
    val winnerName = normalizedPlayerName(
        game.players[winners.single()]
    )

    val tournamentPlayerIndex = selected.players.indexOfFirst {
        normalizedPlayerName(it) == winnerName
    }

    if (tournamentPlayerIndex < 0) return tournaments

    return tournaments.map { tournament ->
        if (tournament.id != selected.id) {
            tournament
        } else {
            tournament.copy(
                wins = tournament.wins.mapIndexed { index, value ->
                    if (index == tournamentPlayerIndex) {
                        (value + 1).coerceAtMost(tournament.targetWins)
                    } else {
                        value
                    }
                }
            )
        }
    }
}