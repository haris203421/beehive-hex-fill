package com.beehivehex.fill.presentation.beefriends

import com.beehivehex.fill.domain.model.Cosmetic

data class BeeFriendsUiState(
    val entries: List<Cosmetic> = emptyList(),
    val selected: String = "sunny",
    val total: Long = 0,
    val saveFailed: Boolean = false
)
