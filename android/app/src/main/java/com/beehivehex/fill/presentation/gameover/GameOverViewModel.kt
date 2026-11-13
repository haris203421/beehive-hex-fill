package com.beehivehex.fill.presentation.gameover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beehivehex.fill.core.navigation.RouteEvent
import com.beehivehex.fill.domain.repository.HiveRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GameOverViewModel(private val roundId: String, private val repository: HiveRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(GameOverUiState())
    val uiState = _uiState.asStateFlow()
    private val channel = Channel<RouteEvent>(Channel.BUFFERED)
    val events = channel.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.state.collect { store ->
                val progress = store.progress
                val next = (repository.bees.map { "bee:${it.id}" to it.threshold } +
                    repository.frames.map { "frame:${it.id}" to it.threshold })
                    .filter { it.second > progress.total }.minByOrNull { it.second }
                _uiState.update { it.copy(result = progress.result?.takeIf { summary -> summary.id == roundId },
                    bee = progress.bee, total = progress.total, nextId = next?.first, nextThreshold = next?.second ?: 0,
                    saveFailed = store.saveFailed) }
            }
        }
    }

    fun retry() { viewModelScope.launch { repository.retry() } }
    fun replay() {
        val result = _uiState.value.result ?: return
        if (_uiState.value.busy) return
        _uiState.update { it.copy(busy = true) }
        viewModelScope.launch {
            val id = repository.startRound(result.mode)
            _uiState.update { it.copy(busy = false) }
            channel.send(RouteEvent.Replay(id))
        }
    }
}
