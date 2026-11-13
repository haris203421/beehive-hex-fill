package com.beehivehex.fill.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beehivehex.fill.R
import com.beehivehex.fill.core.di.ServiceLocator
import com.beehivehex.fill.core.theme.*
import com.beehivehex.fill.core.ui.*
import com.beehivehex.fill.presentation.dialog.HowToPlayDialog

@Composable
fun SettingsRoute(onBack: () -> Unit, onMenu: () -> Unit) {
    val model: SettingsViewModel = viewModel(factory = ServiceLocator.factory())
    val state by model.uiState.collectAsStateWithLifecycle()
    SettingsScreen(state, onBack, onMenu, model::sound, model::haptics, { model.help(true) }, model::retry)
    if (state.help) HowToPlayDialog { model.help(false) }
}

@Composable
fun SettingsScreen(state: SettingsUiState, onBack: () -> Unit, onMenu: () -> Unit, onSound: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit, onHelp: () -> Unit, onRetry: () -> Unit) {
    HiveBackdrop(BackdropStyle.SETTINGS) {
        Column(Modifier.align(Alignment.TopCenter).widthIn(max = 480.dp).fillMaxSize().safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            PageHeader(R.string.settings_title, onBack)
            GlassPanel(Modifier.fillMaxWidth()) {
                SettingRow(R.string.sound_title, R.string.sound_hint, state.sound, onSound)
                HorizontalDivider(color = TextSecondary.copy(alpha = 0.12f))
                SettingRow(R.string.haptics_title, R.string.haptics_hint, state.haptics, onHaptics)
            }
            GlassPanel(Modifier.fillMaxWidth()) {
                SymbolArt("pollen", Modifier.size(64.dp))
                Text(stringResource(R.string.privacy_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.privacy_message), style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
            }
            SaveWarning(state.saveFailed, onRetry)
            SecondaryButton(R.string.how_to_play, onHelp, Modifier.fillMaxWidth())
            PrimaryButton(R.string.menu, onMenu)
        }
    }
}

@Composable
private fun SettingRow(title: Int, hint: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    val state = stringResource(if (checked) R.string.on_state else R.string.off_state)
    val label = stringResource(title)
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(hint), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked, onChange, Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics {
            contentDescription = label; stateDescription = state; liveRegion = LiveRegionMode.Polite
        })
    }
}
