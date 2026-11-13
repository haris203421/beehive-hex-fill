package com.beehivehex.fill.domain.repository

import com.beehivehex.fill.domain.model.*
import kotlinx.coroutines.flow.StateFlow

interface HiveRepository {
    val state: StateFlow<HiveStore>
    val bees: List<Cosmetic>
    val frames: List<Cosmetic>
    val framePreview: Set<Hex>
    suspend fun load()
    suspend fun retry()
    suspend fun startRound(mode: HiveMode): String
    suspend fun chooseMode(mode: HiveMode)
    suspend fun selectBee(id: String)
    suspend fun selectFrame(id: String)
    suspend fun setSound(enabled: Boolean)
    suspend fun setHaptics(enabled: Boolean)
    suspend fun place(roundId: String, slot: Int, anchor: Hex): MoveResult?
    suspend fun checkFinished(roundId: String): Boolean
}
