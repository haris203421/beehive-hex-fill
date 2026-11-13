package com.beehivehex.fill.domain.usecase

import com.beehivehex.fill.domain.model.*

class HiveEngine(private val shapes: List<Shape>) {
    fun board(mode: HiveMode): List<Hex> = (-mode.radius..mode.radius).flatMap { r ->
        (-mode.radius..mode.radius).map { q -> Hex(q, r) }.filter { it.inside(mode.radius) }
    }

    fun create(id: String, mode: HiveMode, seed: Long, previousBest: Long): Round =
        refill(Round(id = id, mode = mode, randomState = seed and 0x7FFFFFFFL, previousBest = previousBest))

    private fun refill(round: Round): Round {
        var state = round.randomState
        fun next(bound: Int): Int {
            state = (state * 1103515245L + 12345L) and 0x7FFFFFFFL
            return ((state ushr 8) % bound).toInt()
        }
        val tray = (0..2).map { slot ->
            var ticket = next(100)
            val shape = shapes.first { shape -> ticket -= shape.weight; ticket < 0 }
            val rotation = next(6)
            var cells = shape.cells
            repeat(rotation) { cells = cells.map(Hex::rotate) }
            Piece(slot, shape.id, rotation, cells)
        }
        return round.copy(tray = tray, randomState = state)
    }

    fun fits(round: Round, piece: Piece, anchor: Hex): Boolean = !piece.consumed && piece.cells.all {
        val target = it + anchor
        target.inside(round.mode.radius) && target !in round.occupied
    }

    fun hasMoves(round: Round): Boolean = round.tray.filterNot { it.consumed }.any { piece ->
        board(round.mode).any { fits(round, piece, it) }
    }

    fun place(round: Round, slot: Int, anchor: Hex): MoveResult? {
        if (round.finished) return null
        val piece = round.tray.firstOrNull { it.slot == slot } ?: return null
        if (!fits(round, piece, anchor)) return null
        val occupied = round.occupied + piece.cells.map { it + anchor }
        val board = board(round.mode)
        val complete = (0..2).flatMap { axis ->
            (-round.mode.radius..round.mode.radius).map { k ->
                board.filter { when (axis) { 0 -> it.q == k; 1 -> it.r == k; else -> it.s == k } }
            }
        }.filter { line -> line.all { it in occupied } }
        val cleared = complete.flatten().toSet()
        val count = complete.size
        val streak = if (count > 0) round.streak + 1 else 0
        val pollen = piece.cells.size.toLong() + if (count == 0) 0L else
            10L * count + 2L * cleared.size + 5L * maxOf(0, count - 1) + 3L * (streak - 1)
        var updated = round.copy(
            occupied = occupied - cleared,
            tray = round.tray.map { if (it.slot == slot) it.copy(consumed = true) else it },
            score = round.score + pollen,
            moves = round.moves + 1,
            lines = round.lines + count,
            streak = streak,
            highestStreak = maxOf(round.highestStreak, streak)
        )
        if (updated.tray.all { it.consumed }) updated = refill(updated)
        updated = updated.copy(finished = !hasMoves(updated))
        return MoveResult(updated, pollen, cleared, count)
    }
}
