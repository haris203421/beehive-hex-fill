package com.beehivehex.fill.data.repository

import com.beehivehex.fill.data.local.HivePreferences
import com.beehivehex.fill.data.sample.SampleData
import com.beehivehex.fill.domain.model.*
import com.beehivehex.fill.domain.repository.HiveRepository
import com.beehivehex.fill.domain.usecase.HiveEngine
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class HiveRepositoryImpl(private val preferences: HivePreferences, private val engine: HiveEngine) : HiveRepository {
    private val _state = MutableStateFlow(HiveStore())
    override val state = _state.asStateFlow()
    override val bees = SampleData.bees
    override val frames = SampleData.frames
    override val framePreview = SampleData.framePreview
    private val mutex = Mutex()

    override suspend fun load() = mutex.withLock {
        if (!_state.value.loading) return@withLock
        readLocked()
    }

    private suspend fun readLocked() {
        val loaded = withContext(Dispatchers.IO) { runCatching { preferences.read() } }
        _state.update { it.copy(progress = loaded.getOrNull()?.progress ?: HiveProgress(), loading = false,
            recovered = loaded.getOrNull()?.recovered ?: true, readFailed = loaded.isFailure) }
    }

    override suspend fun retry() = mutex.withLock {
        if (_state.value.readFailed && _state.value.progress.revision == 0L) readLocked() else saveLocked()
    }

    private suspend fun saveLocked() {
        val snapshot = _state.value.progress
        val success = withContext(Dispatchers.IO) { runCatching { preferences.write(snapshot) }.getOrDefault(false) }
        _state.update { it.copy(saveFailed = !success, readFailed = false) }
    }

    private suspend fun change(transform: (HiveProgress) -> HiveProgress) = mutex.withLock {
        val before = _state.value.progress
        val after = transform(before)
        if (before != after) {
            _state.update { it.copy(progress = after.copy(revision = before.revision + 1)) }
            saveLocked()
        }
    }

    override suspend fun startRound(mode: HiveMode): String = mutex.withLock {
        val progress = _state.value.progress
        val id = UUID.randomUUID().toString()
        val round = engine.create(id, mode, System.nanoTime(), progress.best(mode))
        _state.update { it.copy(progress = progress.copy(round = round, mode = mode, revision = progress.revision + 1)) }
        saveLocked()
        id
    }

    override suspend fun chooseMode(mode: HiveMode) = change { it.copy(mode = mode) }
    override suspend fun selectBee(id: String) = change { progress ->
        if (SampleData.bees.any { it.id == id && it.threshold <= progress.total }) progress.copy(bee = id) else progress
    }
    override suspend fun selectFrame(id: String) = change { progress ->
        if (SampleData.frames.any { it.id == id && it.threshold <= progress.total }) progress.copy(frame = id) else progress
    }
    override suspend fun setSound(enabled: Boolean) = change { it.copy(sound = enabled) }
    override suspend fun setHaptics(enabled: Boolean) = change { it.copy(haptics = enabled) }

    private fun finalized(progress: HiveProgress, round: Round): HiveProgress {
        val summary = if (round.finished) RoundSummary(round.id, round.mode, round.score, round.previousBest,
            round.moves, round.lines, round.highestStreak, round.unlocked) else progress.result
        return progress.copy(round = round, result = summary)
    }

    override suspend fun place(roundId: String, slot: Int, anchor: Hex): MoveResult? = mutex.withLock {
        val progress = _state.value.progress
        val round = progress.round?.takeIf { it.id == roundId && !it.finished } ?: return@withLock null
        val move = engine.place(round, slot, anchor) ?: return@withLock null
        val total = progress.total + move.pollen
        val unlocks = SampleData.bees.filter { it.threshold > progress.total && it.threshold <= total }.map { "bee:${it.id}" } +
            SampleData.frames.filter { it.threshold > progress.total && it.threshold <= total }.map { "frame:${it.id}" }
        val updatedRound = move.round.copy(unlocked = round.unlocked + unlocks)
        val updatedProgress = finalized(progress.copy(total = total,
            classicBest = if (round.mode == HiveMode.CLASSIC) maxOf(progress.classicBest, updatedRound.score) else progress.classicBest,
            pocketBest = if (round.mode == HiveMode.POCKET) maxOf(progress.pocketBest, updatedRound.score) else progress.pocketBest,
            announced = progress.announced + unlocks, revision = progress.revision + 1), updatedRound)
        _state.update { it.copy(progress = updatedProgress) }
        saveLocked()
        move.copy(round = updatedRound)
    }

    override suspend fun checkFinished(roundId: String): Boolean = mutex.withLock {
        val progress = _state.value.progress
        val round = progress.round?.takeIf { it.id == roundId } ?: return@withLock false
        if (round.finished) return@withLock true
        if (engine.hasMoves(round)) return@withLock false
        _state.update { it.copy(progress = finalized(progress, round.copy(finished = true)).copy(revision = progress.revision + 1)) }
        saveLocked()
        true
    }
}
