package com.beehivehex.fill.presentation.settings

data class SettingsUiState(
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val saveFailed: Boolean = false,
    val help: Boolean = false
)
