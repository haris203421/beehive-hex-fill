package com.beehivehex.fill.presentation.game

import com.beehivehex.fill.domain.model.Hex
import com.beehivehex.fill.domain.model.Round

enum class GamePhase { LOADING, READY, RESOLVING, PAUSED, FINISHED_REVIEW }
enum class PlacementStatus { CHOOSE, VALID, INVALID, PLACED, CLEARED, FINISHED }

data class GameUiState(
    val round: Round? = null,
    val board: List<Hex> = emptyList(),
    val best: Long = 0,
    val frame: String = "meadow",
    val selectedSlot: Int? = null,
    val anchor: Hex = Hex(0, 0),
    val valid: Boolean = false,
    val phase: GamePhase = GamePhase.LOADING,
    val status: PlacementStatus = PlacementStatus.CHOOSE,
    val cleared: Set<Hex> = emptySet(),
    val clearFading: Boolean = false,
    val gained: Long = 0,
    val help: Boolean = false,
    val adjust: Boolean = false,
    val saveFailed: Boolean = false,
    val dragging: Boolean = false,
    val pauseDialog: Boolean = false,
    val cursorRow: Int = 1,
    val cursorCell: Int = 1
)
