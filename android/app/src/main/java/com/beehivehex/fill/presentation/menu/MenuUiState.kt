package com.beehivehex.fill.presentation.menu

import com.beehivehex.fill.domain.model.HiveMode

data class MenuUiState(
    val mode: HiveMode = HiveMode.CLASSIC,
    val best: Long = 0,
    val total: Long = 0,
    val bee: String = "sunny",
    val activeId: String? = null,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val recovered: Boolean = false,
    val readFailed: Boolean = false,
    val saveFailed: Boolean = false,
    val help: Boolean = false,
    val replace: Boolean = false
)
