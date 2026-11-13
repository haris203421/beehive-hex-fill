package com.beehivehex.fill.presentation.gameover

import com.beehivehex.fill.domain.model.RoundSummary

data class GameOverUiState(
    val result: RoundSummary? = null,
    val bee: String = "sunny",
    val total: Long = 0,
    val nextId: String? = null,
    val nextThreshold: Long = 0,
    val saveFailed: Boolean = false,
    val busy: Boolean = false
)
