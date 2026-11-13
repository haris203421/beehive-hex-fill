package com.beehivehex.fill.domain.model

import kotlin.math.abs

data class Hex(val q: Int, val r: Int) {
    val s: Int get() = -q - r
    operator fun plus(other: Hex) = Hex(q + other.q, r + other.r)
    fun rotate() = Hex(-r, q + r)
    fun inside(radius: Int) = maxOf(abs(q), abs(r), abs(s)) <= radius
}

enum class HiveMode(val radius: Int) { CLASSIC(4), POCKET(3) }

data class Shape(val id: String, val cells: List<Hex>, val weight: Int)
data class Piece(val slot: Int, val shapeId: String, val rotation: Int, val cells: List<Hex>, val consumed: Boolean = false)
data class Cosmetic(val id: String, val threshold: Long)

data class Round(
    val id: String,
    val mode: HiveMode,
    val occupied: Set<Hex> = emptySet(),
    val tray: List<Piece> = emptyList(),
    val randomState: Long,
    val score: Long = 0,
    val moves: Int = 0,
    val lines: Int = 0,
    val streak: Int = 0,
    val highestStreak: Int = 0,
    val previousBest: Long = 0,
    val finished: Boolean = false,
    val unlocked: Set<String> = emptySet()
)

data class RoundSummary(
    val id: String,
    val mode: HiveMode,
    val score: Long,
    val previousBest: Long,
    val moves: Int,
    val lines: Int,
    val highestStreak: Int,
    val unlocked: Set<String>
)

data class HiveProgress(
    val revision: Long = 0,
    val total: Long = 0,
    val classicBest: Long = 0,
    val pocketBest: Long = 0,
    val mode: HiveMode = HiveMode.CLASSIC,
    val bee: String = "sunny",
    val frame: String = "meadow",
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val announced: Set<String> = emptySet(),
    val round: Round? = null,
    val result: RoundSummary? = null
) {
    fun best(mode: HiveMode) = if (mode == HiveMode.CLASSIC) classicBest else pocketBest
}

data class HiveStore(
    val progress: HiveProgress = HiveProgress(),
    val loading: Boolean = true,
    val recovered: Boolean = false,
    val readFailed: Boolean = false,
    val saveFailed: Boolean = false
)

data class MoveResult(val round: Round, val pollen: Long, val cleared: Set<Hex>, val lineCount: Int)
