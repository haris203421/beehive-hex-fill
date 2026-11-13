package com.beehivehex.fill.data.local

import android.content.Context
import com.beehivehex.fill.core.config.GameConfig
import com.beehivehex.fill.data.sample.SampleData
import com.beehivehex.fill.domain.model.*
import org.json.JSONArray
import org.json.JSONObject

data class LoadedHive(val progress: HiveProgress, val recovered: Boolean)

class HivePreferences(private val context: Context) {
    private val preferences get() = context.getSharedPreferences("woodland_hive", Context.MODE_PRIVATE)

    fun read(): LoadedHive {
        val raw = preferences.getString("snapshot", null) ?: return LoadedHive(HiveProgress(), false)
        val root = JSONObject(raw)
        var recovered = root.optInt("schema", -1) != GameConfig.SCHEMA_VERSION
        fun nonnegative(name: String) = root.optLong(name, 0).coerceIn(0, Long.MAX_VALUE / 4)
        val total = nonnegative("total")
        fun selection(name: String, choices: List<Cosmetic>): String {
            val id = root.optString(name, choices.first().id)
            return choices.firstOrNull { it.id == id && it.threshold <= total }?.id ?: choices.first().id
        }
        var round: Round? = null
        if (root.has("round") && !root.isNull("round")) {
            round = runCatching { decodeRound(root.getJSONObject("round")) }.getOrElse { recovered = true; null }
        }
        var result: RoundSummary? = null
        if (root.has("result") && !root.isNull("result")) {
            result = runCatching { decodeSummary(root.getJSONObject("result")) }.getOrElse { recovered = true; null }
        }
        if (root.optInt("schema", -1) != GameConfig.SCHEMA_VERSION) {
            round = null
            result = null
        }
        if (round?.let { it.score > total || it.score > nonnegative(if (it.mode == HiveMode.CLASSIC) "classicBest" else "pocketBest") } == true) {
            round = null
            recovered = true
        }
        if (result?.let { it.score > total } == true) {
            result = null
            recovered = true
        }
        val finishedRound = round?.takeIf { it.finished }
        if (finishedRound != null) {
            result = RoundSummary(finishedRound.id, finishedRound.mode, finishedRound.score, finishedRound.previousBest,
                finishedRound.moves, finishedRound.lines, finishedRound.highestStreak, finishedRound.unlocked)
        }
        return LoadedHive(
            HiveProgress(
                revision = nonnegative("revision"), total = total,
                classicBest = nonnegative("classicBest").coerceAtMost(total), pocketBest = nonnegative("pocketBest").coerceAtMost(total),
                mode = runCatching { HiveMode.valueOf(root.optString("mode", "CLASSIC")) }.getOrDefault(HiveMode.CLASSIC),
                bee = selection("bee", SampleData.bees), frame = selection("frame", SampleData.frames),
                sound = root.optBoolean("sound", true), haptics = root.optBoolean("haptics", true),
                announced = strings(root.optJSONArray("announced")), round = round, result = result
            ), recovered
        )
    }

    fun write(progress: HiveProgress): Boolean {
        val root = JSONObject().put("schema", GameConfig.SCHEMA_VERSION)
            .put("revision", progress.revision).put("total", progress.total)
            .put("classicBest", progress.classicBest).put("pocketBest", progress.pocketBest)
            .put("mode", progress.mode.name).put("bee", progress.bee).put("frame", progress.frame)
            .put("sound", progress.sound).put("haptics", progress.haptics)
            .put("announced", JSONArray(progress.announced.toList()))
            .put("round", progress.round?.let(::encodeRound) ?: JSONObject.NULL)
            .put("result", progress.result?.let(::encodeSummary) ?: JSONObject.NULL)
        return preferences.edit().putString("snapshot", root.toString()).commit()
    }

    private fun encodeCells(cells: Collection<Hex>) = JSONArray().apply {
        cells.sortedWith(compareBy<Hex> { it.r }.thenBy { it.q }).forEach { put(JSONArray().put(it.q).put(it.r)) }
    }

    private fun decodeCells(array: JSONArray): List<Hex> = (0 until array.length()).map {
        val pair = array.getJSONArray(it)
        require(pair.length() == 2)
        Hex(pair.getInt(0), pair.getInt(1))
    }

    private fun strings(array: JSONArray?): Set<String> = if (array == null) emptySet() else
        (0 until array.length()).mapNotNull { array.opt(it) as? String }.toSet()

    private fun encodeRound(round: Round): JSONObject = JSONObject()
        .put("id", round.id).put("mode", round.mode.name).put("occupied", encodeCells(round.occupied))
        .put("random", round.randomState).put("score", round.score).put("moves", round.moves)
        .put("lines", round.lines).put("streak", round.streak).put("highestStreak", round.highestStreak)
        .put("previousBest", round.previousBest).put("finished", round.finished)
        .put("unlocked", JSONArray(round.unlocked.toList()))
        .put("tray", JSONArray().apply {
            round.tray.forEach { piece -> put(JSONObject().put("slot", piece.slot).put("shape", piece.shapeId)
                .put("rotation", piece.rotation).put("cells", encodeCells(piece.cells)).put("consumed", piece.consumed)) }
        })

    private fun decodeRound(root: JSONObject): Round {
        val mode = HiveMode.valueOf(root.getString("mode"))
        val cells = decodeCells(root.getJSONArray("occupied"))
        require(cells.size == cells.toSet().size && cells.all {
            it.q in -mode.radius..mode.radius && it.r in -mode.radius..mode.radius && it.inside(mode.radius)
        })
        val trayJson = root.getJSONArray("tray")
        require(trayJson.length() == 3)
        val tray = (0..2).map { index ->
            val item = trayJson.getJSONObject(index)
            val shape = SampleData.shapes.first { it.id == item.getString("shape") }
            val rotation = item.getInt("rotation")
            require(rotation in 0..5 && item.getInt("slot") == index)
            var expected = shape.cells
            repeat(rotation) { expected = expected.map(Hex::rotate) }
            val offsets = decodeCells(item.getJSONArray("cells"))
            require(offsets.toSet() == expected.toSet() && offsets.size == expected.size)
            Piece(index, shape.id, rotation, expected, item.getBoolean("consumed"))
        }
        val round = Round(
            id = root.getString("id"), mode = mode, occupied = cells.toSet(), tray = tray,
            randomState = root.getLong("random"), score = root.getLong("score"),
            moves = root.getInt("moves"), lines = root.getInt("lines"), streak = root.getInt("streak"),
            highestStreak = root.getInt("highestStreak"), previousBest = root.getLong("previousBest"),
            finished = root.getBoolean("finished"), unlocked = strings(root.optJSONArray("unlocked"))
        )
        require(round.id.isNotBlank() && round.id.length < 100)
        require(round.randomState in 0..0x7FFFFFFFL && round.score in 0..Long.MAX_VALUE / 4 && round.previousBest in 0..Long.MAX_VALUE / 4)
        require(round.moves in 0..1000000000 && round.lines in 0..1000000000 && round.streak in 0..round.moves && round.highestStreak in round.streak..round.moves)
        require(round.tray.any { !it.consumed })
        return round
    }

    private fun encodeSummary(result: RoundSummary): JSONObject = JSONObject()
        .put("id", result.id).put("mode", result.mode.name).put("score", result.score)
        .put("previousBest", result.previousBest).put("moves", result.moves).put("lines", result.lines)
        .put("highestStreak", result.highestStreak).put("unlocked", JSONArray(result.unlocked.toList()))

    private fun decodeSummary(root: JSONObject): RoundSummary {
        val result = RoundSummary(root.getString("id"), HiveMode.valueOf(root.getString("mode")),
            root.getLong("score"), root.getLong("previousBest"), root.getInt("moves"),
            root.getInt("lines"), root.getInt("highestStreak"), strings(root.optJSONArray("unlocked")))
        require(result.id.isNotBlank() && result.score in 0..Long.MAX_VALUE / 4 && result.previousBest in 0..Long.MAX_VALUE / 4 &&
            result.moves in 0..1000000000 && result.lines in 0..1000000000 && result.highestStreak in 0..result.moves)
        return result
    }
}
