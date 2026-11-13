package com.beehivehex.fill.core.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.beehivehex.fill.presentation.app.AppViewModel
import com.beehivehex.fill.presentation.beefriends.BeeFriendsViewModel
import com.beehivehex.fill.presentation.frames.FramesViewModel
import com.beehivehex.fill.presentation.game.GameViewModel
import com.beehivehex.fill.presentation.gameover.GameOverViewModel
import com.beehivehex.fill.presentation.menu.MenuViewModel
import com.beehivehex.fill.presentation.settings.SettingsViewModel
import com.beehivehex.fill.presentation.splash.SplashViewModel

class AppViewModelFactory(private val dependencies: Dependencies, private val roundId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val instance: ViewModel = when (modelClass) {
            AppViewModel::class.java -> AppViewModel()
            SplashViewModel::class.java -> SplashViewModel(dependencies.repository)
            MenuViewModel::class.java -> MenuViewModel(dependencies.repository)
            GameViewModel::class.java -> GameViewModel(roundId, dependencies.repository, dependencies.engine, dependencies.feedback)
            GameOverViewModel::class.java -> GameOverViewModel(roundId, dependencies.repository)
            BeeFriendsViewModel::class.java -> BeeFriendsViewModel(dependencies.repository)
            FramesViewModel::class.java -> FramesViewModel(dependencies.repository)
            SettingsViewModel::class.java -> SettingsViewModel(dependencies.repository, dependencies.feedback)
            else -> error(modelClass.name)
        }
        return modelClass.cast(instance) ?: error(modelClass.name)
    }
}
