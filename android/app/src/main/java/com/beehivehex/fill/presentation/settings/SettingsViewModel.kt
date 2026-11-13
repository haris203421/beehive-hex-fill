package com.beehivehex.fill.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beehivehex.fill.core.ui.FeedbackPlayer
import com.beehivehex.fill.domain.repository.HiveRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: HiveRepository, private val feedback: FeedbackPlayer) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()
    init {
        viewModelScope.launch {
            repository.state.collect { store -> _uiState.update { it.copy(sound = store.progress.sound,
                haptics = store.progress.haptics, saveFailed = store.saveFailed) } }
        }
    }
    fun sound(enabled: Boolean) {
        _uiState.update { it.copy(sound = enabled) }
        if (!enabled) feedback.stop()
        viewModelScope.launch { repository.setSound(enabled) }
    }
    fun haptics(enabled: Boolean) {
        _uiState.update { it.copy(haptics = enabled) }
        viewModelScope.launch { repository.setHaptics(enabled) }
    }
    fun help(visible: Boolean) { _uiState.update { it.copy(help = visible) } }
    fun retry() { viewModelScope.launch { repository.retry() } }
}
