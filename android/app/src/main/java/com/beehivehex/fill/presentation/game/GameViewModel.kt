package com.beehivehex.fill.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beehivehex.fill.core.navigation.RouteEvent
import com.beehivehex.fill.core.navigation.Screen
import com.beehivehex.fill.core.ui.FeedbackPlayer
import com.beehivehex.fill.domain.model.Hex
import com.beehivehex.fill.domain.repository.HiveRepository
import com.beehivehex.fill.domain.usecase.HiveEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GameViewModel(
    private val roundId: String,
    private val repository: HiveRepository,
    private val engine: HiveEngine,
    private val feedback: FeedbackPlayer
) : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState = _uiState.asStateFlow()
    private val channel = Channel<RouteEvent>(Channel.BUFFERED)
    val events = channel.receiveAsFlow()
    private var visible = false
    private var resultSent = false
    private var feedbackJob: Job? = null
    private var committing = false

    init {
        viewModelScope.launch {
            repository.state.collect { store ->
                val round = store.progress.round?.takeIf { it.id == roundId }
                _uiState.update { state -> state.copy(round = round, board = round?.let { engine.board(it.mode) }.orEmpty(),
                    frame = store.progress.frame, best = round?.let { store.progress.best(it.mode) } ?: 0,
                    saveFailed = store.saveFailed,
                    phase = when { round == null -> GamePhase.LOADING; round.finished -> GamePhase.FINISHED_REVIEW
                        state.phase == GamePhase.LOADING && visible -> GamePhase.READY; else -> state.phase }) }
                updateValidity()
                if (round?.finished == true && visible && !resultSent) showResult()
            }
        }
    }

    fun resume() {
        visible = true
        _uiState.update { it.copy(cleared = emptySet(), clearFading = false,
            phase = when { it.round?.finished == true -> GamePhase.FINISHED_REVIEW; it.pauseDialog -> GamePhase.PAUSED; else -> GamePhase.READY }) }
        updateValidity()
        if (!resultSent) viewModelScope.launch {
            if (repository.checkFinished(roundId)) showResult()
        }
    }

    fun pause() {
        visible = false
        feedbackJob?.cancel()
        feedback.stop()
        _uiState.update { it.copy(dragging = false, selectedSlot = null, valid = false, cleared = emptySet(), clearFading = false, adjust = false,
            phase = if (it.round?.finished == true) GamePhase.FINISHED_REVIEW else GamePhase.PAUSED) }
    }

    private fun updateValidity() {
        _uiState.update { state ->
            val round = state.round
            val piece = round?.tray?.firstOrNull { it.slot == state.selectedSlot }
            val valid = round != null && piece != null && !round.finished && engine.fits(round, piece, state.anchor)
            val radius = round?.mode?.radius ?: 0
            state.copy(valid = valid, cursorRow = state.anchor.r + radius + 1,
                cursorCell = state.anchor.q - maxOf(-radius, -state.anchor.r - radius) + 1)
        }
    }

    fun select(slot: Int) {
        val state = _uiState.value
        if (state.phase != GamePhase.READY || state.round?.tray?.getOrNull(slot)?.consumed != false) return
        _uiState.update { it.copy(selectedSlot = slot, status = PlacementStatus.VALID) }
        updateValidity()
    }

    fun anchor(hex: Hex) {
        if (_uiState.value.phase != GamePhase.READY) return
        _uiState.update { it.copy(anchor = hex) }
        updateValidity()
    }

    fun move(direction: Hex) {
        val next = _uiState.value.anchor + direction
        val round = _uiState.value.round ?: return
        if (next.inside(round.mode.radius)) anchor(next)
    }

    fun drag(slot: Int) { select(slot); _uiState.update { it.copy(dragging = it.selectedSlot == slot) } }
    fun cancelDrag() { _uiState.update { it.copy(dragging = false) } }
    fun drop(onBoard: Boolean) { cancelDrag(); if (onBoard) place() else invalid() }
    fun help(visible: Boolean) { _uiState.update { it.copy(help = visible) } }
    fun adjust(visible: Boolean) { _uiState.update { it.copy(adjust = visible) } }
    fun retry() { viewModelScope.launch { repository.retry() } }

    fun setPaused(paused: Boolean) {
        feedbackJob?.cancel()
        if (paused) feedback.stop()
        _uiState.update { it.copy(pauseDialog = paused, dragging = false, cleared = emptySet(), clearFading = false,
            phase = if (paused) GamePhase.PAUSED else if (it.round?.finished == true) GamePhase.FINISHED_REVIEW else GamePhase.READY) }
    }

    private fun invalid() {
        _uiState.update { it.copy(status = PlacementStatus.INVALID) }
        viewModelScope.launch { if (repository.checkFinished(roundId) && !resultSent) showResult() }
        feedbackJob?.cancel()
        feedbackJob = viewModelScope.launch {
            delay(1200)
            _uiState.update { it.copy(status = PlacementStatus.CHOOSE) }
        }
    }

    fun place() {
        val state = _uiState.value
        if (committing || state.phase != GamePhase.READY) return
        val slot = state.selectedSlot ?: return
        if (!state.valid) { invalid(); return }
        committing = true
        _uiState.update { it.copy(phase = GamePhase.RESOLVING, adjust = false) }
        viewModelScope.launch {
            try {
                val move = repository.place(roundId, slot, state.anchor)
                if (move != null) {
                    val settings = repository.state.value.progress
                    if (visible && !_uiState.value.pauseDialog) feedback.play(move.lineCount > 0, settings.sound, settings.haptics)
                    _uiState.update { it.copy(selectedSlot = null, valid = false, cleared = move.cleared, clearFading = false,
                        gained = move.pollen, status = if (move.lineCount > 0) PlacementStatus.CLEARED else PlacementStatus.PLACED) }
                    if (move.round.finished) {
                        if (!resultSent) showResult()
                    } else if (visible && !_uiState.value.pauseDialog) {
                        feedbackJob = viewModelScope.launch {
                            delay(180)
                            if (move.cleared.isNotEmpty()) {
                                _uiState.update { it.copy(clearFading = true) }
                                delay(220)
                            }
                            _uiState.update { it.copy(phase = GamePhase.READY, cleared = emptySet(), clearFading = false) }
                        }
                    }
                } else if (visible) _uiState.update { it.copy(phase = GamePhase.READY) }
            } finally { committing = false }
        }
    }

    fun showResult() {
        if (!visible || repository.state.value.progress.round?.takeIf { it.id == roundId }?.finished != true) return
        resultSent = true
        _uiState.update { it.copy(phase = GamePhase.FINISHED_REVIEW, status = PlacementStatus.FINISHED) }
        viewModelScope.launch { channel.send(RouteEvent.Open(Screen.GameOver(roundId))) }
    }
}
