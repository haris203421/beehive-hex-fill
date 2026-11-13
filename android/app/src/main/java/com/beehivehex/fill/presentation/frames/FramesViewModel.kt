package com.beehivehex.fill.presentation.frames

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beehivehex.fill.domain.repository.HiveRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FramesViewModel(private val repository: HiveRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(FramesUiState(entries = repository.frames, preview = repository.framePreview))
    val uiState = _uiState.asStateFlow()
    init {
        viewModelScope.launch {
            repository.state.collect { store -> _uiState.update { it.copy(selected = store.progress.frame,
                total = store.progress.total, saveFailed = store.saveFailed) } }
        }
    }
    fun select(id: String) { viewModelScope.launch { repository.selectFrame(id) } }
    fun retry() { viewModelScope.launch { repository.retry() } }
}
