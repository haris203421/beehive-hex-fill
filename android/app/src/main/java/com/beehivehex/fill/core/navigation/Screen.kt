package com.beehivehex.fill.core.navigation

sealed interface Screen {
    data object Splash : Screen
    data object Menu : Screen
    data class Game(val roundId: String) : Screen
    data class GameOver(val roundId: String) : Screen
    data object BeeFriends : Screen
    data object Frames : Screen
    data object Settings : Screen
}

sealed interface RouteEvent {
    data class Open(val screen: Screen) : RouteEvent
    data class Replay(val roundId: String) : RouteEvent
    data object Finished : RouteEvent
}
