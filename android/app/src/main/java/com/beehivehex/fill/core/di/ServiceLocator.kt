package com.beehivehex.fill.core.di

import android.content.Context
import com.beehivehex.fill.core.ui.FeedbackPlayer
import com.beehivehex.fill.data.local.HivePreferences
import com.beehivehex.fill.data.repository.HiveRepositoryImpl
import com.beehivehex.fill.data.sample.SampleData
import com.beehivehex.fill.domain.repository.HiveRepository
import com.beehivehex.fill.domain.usecase.HiveEngine

object ServiceLocator {
    private var dependencies: Dependencies? = null
    fun initialize(context: Context) {
        if (dependencies == null) {
            val engine = HiveEngine(SampleData.shapes)
            dependencies = Dependencies(HiveRepositoryImpl(HivePreferences(context.applicationContext), engine),
                engine, FeedbackPlayer(context.applicationContext))
        }
    }
    fun factory(roundId: String = ""): AppViewModelFactory = AppViewModelFactory(requireNotNull(dependencies), roundId)
}

data class Dependencies(val repository: HiveRepository, val engine: HiveEngine, val feedback: FeedbackPlayer)
