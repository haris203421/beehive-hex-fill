package com.beehivehex.fill.presentation.frames

import com.beehivehex.fill.domain.model.Cosmetic
import com.beehivehex.fill.domain.model.Hex

data class FramesUiState(
    val entries: List<Cosmetic> = emptyList(),
    val selected: String = "meadow",
    val total: Long = 0,
    val preview: Set<Hex> = emptySet(),
    val saveFailed: Boolean = false
)
