package com.beehivehex.fill.presentation.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beehivehex.fill.core.di.ServiceLocator
import com.beehivehex.fill.core.navigation.Screen
import com.beehivehex.fill.presentation.beefriends.BeeFriendsRoute
import com.beehivehex.fill.presentation.frames.FramesRoute
import com.beehivehex.fill.presentation.game.GameRoute
import com.beehivehex.fill.presentation.gameover.GameOverRoute
import com.beehivehex.fill.presentation.menu.MenuRoute
import com.beehivehex.fill.presentation.settings.SettingsRoute
import com.beehivehex.fill.presentation.splash.SplashRoute

@Composable
fun AppRoot() {
    val model: AppViewModel = viewModel(factory = ServiceLocator.factory())
    val screen by model.screen.collectAsStateWithLifecycle()
    val state by model.uiState.collectAsStateWithLifecycle()
    BackHandler(enabled = screen != Screen.Menu) { if (state.backStack.isNotEmpty()) model.back() }
    AnimatedContent(targetState = screen, transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(220)) }, label = "hive_routes") { current ->
        when (current) {
            Screen.Splash -> SplashRoute(model::splashFinished)
            Screen.Menu -> MenuRoute(model::navigate)
            is Screen.Game -> GameRoute(current.roundId, model::navigate, model::menu)
            is Screen.GameOver -> GameOverRoute(current.roundId, model::replay, model::menu, { model.navigate(Screen.BeeFriends) })
            Screen.BeeFriends -> BeeFriendsRoute(model::back)
            Screen.Frames -> FramesRoute(model::back)
            Screen.Settings -> SettingsRoute(model::back, model::menu)
        }
    }
}
