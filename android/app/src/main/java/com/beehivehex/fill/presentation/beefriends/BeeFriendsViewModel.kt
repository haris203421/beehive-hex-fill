package com.beehivehex.fill.presentation.beefriends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beehivehex.fill.domain.repository.HiveRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BeeFriendsViewModel(private val repository: HiveRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(BeeFriendsUiState(entries = repository.bees))
    val uiState = _uiState.asStateFlow()
    init {
        viewModelScope.launch {
            repository.state.collect { store -> _uiState.update { it.copy(selected = store.progress.bee,
                total = store.progress.total, saveFailed = store.saveFailed) } }
        }
    }
    fun select(id: String) { viewModelScope.launch { repository.selectBee(id) } }
    fun retry() { viewModelScope.launch { repository.retry() } }
}
