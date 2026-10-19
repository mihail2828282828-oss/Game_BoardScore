package com.example.boardgamescore

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

internal class Storage(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        "board_game_score",
        Context.MODE_PRIVATE
    )

    var theme: String
        get() {
            val value = prefs.getString("theme", "system")
            return value?.takeIf {
                it in setOf("system", "light", "dark", "amoled")
            } ?: "system"
        }
        set(value) {
            val safeValue = value.takeIf {
                it in setOf("system", "light", "dark", "amoled")
            } ?: "system"

            prefs.edit()
                .putString("theme", safeValue)
                .apply()
        }

    var accent: Int
        get() = prefs.getInt("accent", 0).coerceIn(0, 2)
        set(value) {
            prefs.edit()
                .putInt("accent", value.coerceIn(0, 2))
                .apply()
        }

    var style: Int
        get() = prefs.getInt("style", 0).coerceIn(0, 3)
        set(value) {
            prefs.edit()
                .putInt("style", value.coerceIn(0, 3))
                .apply()
        }

    var animations: Boolean
        get() = prefs.getBoolean("animations", true)
        set(value) {
            prefs.edit()
                .putBoolean("animations", value)
                .apply()
        }

    var english: Boolean
        get() = prefs.getBoolean("english", false)
        set(value) {
            prefs.edit()
                .putBoolean("english", value)
                .apply()
        }

    var sound: Boolean
        get() = prefs.getBoolean("sound", true)
        set(value) {
            prefs.edit()
                .putBoolean("sound", value)
                .apply()
        }

    fun loadHistory(): List<SavedGame> {
        val array = readArray("history")

        return buildList {
            for (index in 0 until array.length()) {
                val game = runCatching {
                    readGame(array.getJSONObject(index))
                }.getOrNull()

                if (game != null) {
                    add(game)
                }
            }
        }.distinctBy { it.id }
    }

    fun saveHistory(games: List<SavedGame>) {
        val array = JSONArray()

        games.forEach { game ->
            val rounds = JSONArray()

            game.rounds.forEach { round ->
                rounds.put(intArray(round))
            }

            val item = JSONObject().apply {
                put("id", game.id)
                put("name", game.name)
                put("date", game.date)

                put("players", stringArray(game.players))
                put("rounds", rounds)

                put("rule", game.rule.name)
                put("target", game.target)
                put("notes", game.notes)

                put("starts", intArray(game.starts))
                put("teamMode", game.teamMode)
                put("teamAssign", intArray(game.teamAssign))
                put("teamNames", stringArray(game.teamNames))
            }

            array.put(item)
        }

        prefs.edit()
            .putString("history", array.toString())
            .apply()
    }

    fun loadTournaments(): List<Tournament> {
        val array = readArray("tournaments")

        return buildList {
            for (index in 0 until array.length()) {
                val tournament = runCatching {
                    readTournament(array.getJSONObject(index))
                }.getOrNull()

                if (tournament != null) {
                    add(tournament)
                }
            }
        }.distinctBy { it.id }
    }

    fun saveTournaments(tournaments: List<Tournament>) {
        val array = JSONArray()

        tournaments.forEach { tournament ->
            val item = JSONObject().apply {
                put("id", tournament.id)
                put("name", tournament.name)
                put("date", tournament.date)

                put("players", stringArray(tournament.players))
                put("targetWins", tournament.targetWins)
                put("wins", intArray(tournament.wins))
            }

            array.put(item)
        }

        prefs.edit()
            .putString("tournaments", array.toString())
            .apply()
    }

    private fun readGame(item: JSONObject): SavedGame {
        val id = item.getString("id")
        val name = item.getString("name")
        val date = item.getLong("date")

        val players = readStrings(item.getJSONArray("players"))
            .map { it.trim() }

        require(id.isNotBlank())
        require(name.isNotBlank())
        require(validPlayers(players))

        val roundsArray = item.getJSONArray("rounds")

        val rounds = List(roundsArray.length()) { index ->
            readInts(roundsArray.getJSONArray(index))
        }

        require(rounds.isNotEmpty())
        require(
            rounds.all { round ->
                round.size == players.size &&
                    round.all { it in -9999..9999 }
            }
        )

        val storedStarts = item.optJSONArray("starts")
            ?.let(::readInts)
            .orEmpty()

        // Старые записи могли не содержать стартовые очки.
        val starts = if (storedStarts.isEmpty()) {
            List(players.size) { 0 }
        } else {
            storedStarts
        }

        require(starts.size == players.size)
        require(starts.all { it in -9999..9999 })

        val teamMode = item.optBoolean("teamMode", false)

        val storedAssignments = item.optJSONArray("teamAssign")
            ?.let(::readInts)
            .orEmpty()

        val storedNames = item.optJSONArray("teamNames")
            ?.let(::readStrings)
            .orEmpty()

        val teamAssign: List<Int>
        val teamNames: List<String>

        if (teamMode) {
            require(storedAssignments.size == players.size)
            require(storedAssignments.all { it in 0..1 })
            require(storedAssignments.contains(0))
            require(storedAssignments.contains(1))

            teamAssign = storedAssignments

            teamNames = List(2) { index ->
                storedNames.getOrNull(index)
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: if (index == 0) "A" else "B"
            }
        } else {
            teamAssign = List(players.size) { 0 }
            teamNames = listOf("A", "B")
        }

        val rule = WinRule.valueOf(item.getString("rule"))
        val target = item.optInt("target", 0)

        require(target in 0..9999)

        return SavedGame(
            id = id,
            name = name,
            date = date,
            players = players,
            rounds = rounds,
            rule = rule,
            target = target,
            notes = item.optString("notes", ""),
            starts = starts,
            teamMode = teamMode,
            teamAssign = teamAssign,
            teamNames = teamNames
        )
    }

    private fun readTournament(item: JSONObject): Tournament {
        val id = item.getString("id")
        val name = item.getString("name")

        val players = readStrings(item.getJSONArray("players"))
            .map { it.trim() }

        val wins = readInts(item.getJSONArray("wins"))
        val targetWins = item.optInt("targetWins", 3)

        require(id.isNotBlank())
        require(name.isNotBlank())
        require(validPlayers(players))
        require(wins.size == players.size)
        require(wins.all { it >= 0 })
        require(targetWins in 1..20)

        return Tournament(
            id = id,
            name = name,
            date = item.getLong("date"),
            players = players,
            targetWins = targetWins,
            wins = wins
        )
    }

    private fun validPlayers(players: List<String>): Boolean {
        if (players.size !in 2..8) return false
        if (players.any { it.isBlank() }) return false

        val normalized = players.map {
            it.trim().lowercase(Locale.ROOT)
        }

        return normalized.distinct().size == players.size
    }

    private fun readArray(key: String): JSONArray {
        return runCatching {
            JSONArray(prefs.getString(key, "[]") ?: "[]")
        }.getOrElse {
            JSONArray()
        }
    }

    private fun readInts(array: JSONArray): List<Int> {
        return List(array.length()) { index ->
            array.getInt(index)
        }
    }

    private fun readStrings(array: JSONArray): List<String> {
        return List(array.length()) { index ->
            array.getString(index)
        }
    }

    private fun intArray(values: List<Int>): JSONArray {
        return JSONArray().apply {
            values.forEach { value ->
                put(value)
            }
        }
    }

    private fun stringArray(values: List<String>): JSONArray {
        return JSONArray().apply {
            values.forEach { value ->
                put(value)
            }
        }
    }
}