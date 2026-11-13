package com.beehivehex.fill.presentation.app

import androidx.lifecycle.ViewModel
import com.beehivehex.fill.core.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AppUiState(val backStack: List<Screen> = emptyList())

class AppViewModel : ViewModel() {
    private val _screen = MutableStateFlow<Screen>(Screen.Splash)
    val screen = _screen.asStateFlow()
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState = _uiState.asStateFlow()

    fun splashFinished() {
        if (_screen.value == Screen.Splash) _screen.value = Screen.Menu
    }

    fun navigate(target: Screen) {
        if (target == _screen.value || target == Screen.Splash) return
        val current = _screen.value
        if (current != Screen.Splash) _uiState.update { it.copy(backStack = it.backStack + current) }
        _screen.value = target
    }

    fun back() {
        val stack = _uiState.value.backStack
        if (stack.isNotEmpty()) {
            _screen.value = stack.last()
            _uiState.update { it.copy(backStack = stack.dropLast(1)) }
        }
    }

    fun menu() {
        _uiState.update { it.copy(backStack = emptyList()) }
        _screen.value = Screen.Menu
    }

    fun replay(roundId: String) {
        menu()
        navigate(Screen.Game(roundId))
    }
}
