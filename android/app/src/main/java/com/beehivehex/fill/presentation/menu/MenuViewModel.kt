package com.beehivehex.fill.presentation.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beehivehex.fill.core.navigation.RouteEvent
import com.beehivehex.fill.core.navigation.Screen
import com.beehivehex.fill.domain.model.HiveMode
import com.beehivehex.fill.domain.repository.HiveRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MenuViewModel(private val repository: HiveRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState = _uiState.asStateFlow()
    private val channel = Channel<RouteEvent>(Channel.BUFFERED)
    val events = channel.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.state.collect { store ->
                val progress = store.progress
                _uiState.update { it.copy(mode = progress.mode, best = progress.best(progress.mode), total = progress.total,
                    bee = progress.bee, activeId = progress.round?.takeUnless { round -> round.finished }?.id,
                    loading = store.loading, recovered = store.recovered, readFailed = store.readFailed, saveFailed = store.saveFailed) }
            }
        }
    }

    fun chooseMode(mode: HiveMode) { viewModelScope.launch { repository.chooseMode(mode) } }
    fun help(visible: Boolean) { _uiState.update { it.copy(help = visible) } }
    fun replace(visible: Boolean) { _uiState.update { it.copy(replace = visible) } }
    fun retry() { viewModelScope.launch { repository.retry() } }
    fun play() {
        if (_uiState.value.loading || _uiState.value.busy) return
        val activeId = _uiState.value.activeId
        if (activeId != null) viewModelScope.launch { channel.send(RouteEvent.Open(Screen.Game(activeId))) } else newRound()
    }
    fun requestNew() { if (_uiState.value.activeId != null) replace(true) else newRound() }
    fun newRound() {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(busy = true, replace = false) }
        viewModelScope.launch {
            val id = repository.startRound(_uiState.value.mode)
            _uiState.update { it.copy(busy = false) }
            channel.send(RouteEvent.Open(Screen.Game(id)))
        }
    }
}
