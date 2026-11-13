package com.beehivehex.fill.domain.usecase

import com.beehivehex.fill.data.sample.SampleData
import com.beehivehex.fill.domain.model.Hex
import com.beehivehex.fill.domain.model.HiveMode
import com.beehivehex.fill.domain.model.Piece
import com.beehivehex.fill.domain.model.Round
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HiveEngineTest {
    private val engine = HiveEngine(SampleData.shapes)
    private fun drop(slot: Int, consumed: Boolean = false) = Piece(slot, "drop", 0, listOf(Hex(0, 0)), consumed)
    private fun round(piece: Piece, occupied: Set<Hex> = emptySet()) = Round(
        id = "woodland-test", mode = HiveMode.CLASSIC, occupied = occupied,
        tray = listOf(piece, drop(1), drop(2)), randomState = 12345L
    )

    @Test
    fun boardsHaveTheirRealCellCountsAndStableOrder() {
        assertEquals(61, engine.board(HiveMode.CLASSIC).size)
        assertEquals(37, engine.board(HiveMode.POCKET).size)
        HiveMode.entries.forEach { mode ->
            val cells = engine.board(mode)
            assertEquals(cells.sortedWith(compareBy<Hex> { it.r }.thenBy { it.q }), cells)
            assertTrue(cells.all { it.inside(mode.radius) })
        }
    }

    @Test
    fun everyFixedOrientationFitsEitherEmptyHive() {
        assertEquals(100, SampleData.shapes.sumOf { it.weight })
        HiveMode.entries.forEach { mode ->
            SampleData.shapes.forEach { shape ->
                var cells = shape.cells
                repeat(6) { orientation ->
                    val piece = Piece(0, shape.id, orientation, cells)
                    val snapshot = round(piece).copy(mode = mode)
                    assertTrue(engine.board(mode).any { engine.fits(snapshot, piece, it) })
                    cells = cells.map(Hex::rotate)
                }
                assertEquals(shape.cells, cells)
            }
        }
    }

    @Test
    fun invalidPlacementIsAtomic() {
        val piece = Piece(0, "pair", 0, listOf(Hex(0, 0), Hex(1, 0)))
        val snapshot = round(piece, setOf(Hex(0, 0))).copy(score = 27)
        assertNull(engine.place(snapshot, 0, Hex(0, 0)))
        assertNull(engine.place(snapshot, 0, Hex(4, 0)))
        assertEquals(setOf(Hex(0, 0)), snapshot.occupied)
        assertEquals(27L, snapshot.score)
        assertEquals(12345L, snapshot.randomState)
        assertFalse(snapshot.tray[0].consumed)
    }

    @Test
    fun oneFiveCellLineAwardsTwentyThreePollen() {
        val piece = Piece(0, "trio_line", 1, listOf(Hex(0, 0), Hex(0, 1), Hex(0, 2)))
        val snapshot = round(piece, setOf(Hex(-4, 3), Hex(-4, 4)))
        val result = checkNotNull(engine.place(snapshot, 0, Hex(-4, 0)))
        assertEquals(1, result.lineCount)
        assertEquals(5, result.cleared.size)
        assertEquals(23L, result.pollen)
        assertEquals(23L, result.round.score)
        assertTrue(result.round.occupied.isEmpty())
        assertEquals(1, result.round.streak)
    }

    @Test
    fun intersectingLinesRemoveCrossingCellOnceAndAwardStreak() {
        val board = engine.board(HiveMode.CLASSIC)
        val union = board.filter { it.q == -4 || it.r == 4 }.toSet()
        val crossing = Hex(-4, 4)
        val piece = Piece(0, "pair", 5, listOf(Hex(0, 0), Hex(1, -1)))
        val snapshot = round(piece, union - crossing).copy(streak = 1, highestStreak = 1, moves = 1, score = 10L)
        val result = checkNotNull(engine.place(snapshot, 0, crossing))
        assertEquals(2, result.lineCount)
        assertEquals(9, result.cleared.size)
        assertEquals(48L, result.pollen)
        assertEquals(setOf(Hex(-3, 3)), result.round.occupied)
        assertEquals(2, result.round.streak)
        assertEquals(2, result.round.highestStreak)
    }

    @Test
    fun nonClearingMoveResetsOnlyTheCurrentStreak() {
        val snapshot = round(drop(0)).copy(streak = 3, highestStreak = 3, moves = 3, score = 15L)
        val result = checkNotNull(engine.place(snapshot, 0, Hex(0, 0)))
        assertEquals(1L, result.pollen)
        assertEquals(0, result.round.streak)
        assertEquals(3, result.round.highestStreak)
    }

    @Test
    fun trayRefillsExactlyWhenAllThreePiecesAreUsed() {
        val initial = round(drop(0))
        val first = checkNotNull(engine.place(initial, 0, Hex(0, 0))).round
        assertEquals(12345L, first.randomState)
        assertTrue(first.tray[0].consumed)
        assertFalse(first.tray[1].consumed)
        val second = checkNotNull(engine.place(first, 1, Hex(1, 0))).round
        assertEquals(12345L, second.randomState)
        val third = checkNotNull(engine.place(second, 2, Hex(0, 1))).round
        assertEquals(3, third.tray.size)
        assertTrue(third.tray.none { it.consumed })
        assertTrue(third.randomState != initial.randomState)
        assertEquals(3L, third.score)
    }

    @Test
    fun fitSearchConsidersEveryRemainingPiece() {
        val pair = Piece(0, "pair", 0, listOf(Hex(0, 0), Hex(1, 0)))
        val snapshot = round(pair, engine.board(HiveMode.CLASSIC).toSet() - Hex(0, 0))
            .copy(tray = listOf(pair, drop(1, true), drop(2, true)))
        assertFalse(engine.hasMoves(snapshot))
        assertTrue(engine.hasMoves(snapshot.copy(tray = listOf(pair, drop(1), drop(2, true)))))
        assertFalse(engine.hasMoves(snapshot.copy(tray = snapshot.tray.map { it.copy(consumed = true) })))
    }

    @Test
    fun savedRandomStateReproducesTheSameNextTray() {
        val initial = engine.create("seeded", HiveMode.POCKET, 84711L, 90L)
        assertEquals(initial, engine.create("seeded", HiveMode.POCKET, 84711L, 90L))
        val snapshot = round(drop(0)).copy(tray = listOf(drop(0), drop(1, true), drop(2, true)))
        val first = checkNotNull(engine.place(snapshot, 0, Hex(0, 0)))
        val restored = snapshot.copy(occupied = snapshot.occupied.toSet(), tray = snapshot.tray.toList())
        assertEquals(first, engine.place(restored, 0, Hex(0, 0)))
    }

    @Test
    fun finishedRoundsRejectFurtherMoves() {
        val snapshot = round(drop(0)).copy(finished = true, score = 42L)
        assertNull(engine.place(snapshot, 0, Hex(0, 0)))
        assertEquals(42L, snapshot.score)
    }
}
