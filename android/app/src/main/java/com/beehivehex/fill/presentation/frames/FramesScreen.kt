package com.beehivehex.fill.presentation.frames

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

@Composable
fun FramesRoute(onBack: () -> Unit) {
    val model: FramesViewModel = viewModel(factory = ServiceLocator.factory())
    val state by model.uiState.collectAsStateWithLifecycle()
    FramesScreen(state, onBack, model::select, model::retry)
}

@Composable
fun FramesScreen(state: FramesUiState, onBack: () -> Unit, onSelect: (String) -> Unit, onRetry: () -> Unit) {
    HiveBackdrop(BackdropStyle.GAME) {
        Column(Modifier.align(Alignment.TopCenter).widthIn(max = 480.dp).fillMaxSize().safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PageHeader(R.string.frames_title, onBack)
            Text(stringResource(R.string.frames_hint), style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
            Text(stringResource(R.string.pollen_total, state.total), color = Honey, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            SaveWarning(state.saveFailed, onRetry)
            state.entries.forEach { frame ->
                val unlocked = state.total >= frame.threshold
                val selected = state.selected == frame.id
                val description = stringResource(R.string.frame_preview, stringResource(frameLabel(frame.id)))
                val status = stringResource(if (selected) R.string.selected_state else if (unlocked) R.string.unlocked else R.string.locked)
                GlassPanel(Modifier.fillMaxWidth().semantics { stateDescription = status; this.selected = selected },
                    if (selected) frameColor(frame.id) else TextPrimary.copy(alpha = 0.12f)) {
                    Row(Modifier.heightIn(min = 128.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        FrameArt(state.preview, frame.id, Modifier.size(96.dp).semantics { contentDescription = description })
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(frameLabel(frame.id)), style = MaterialTheme.typography.titleLarge)
                            Text(if (unlocked) stringResource(R.string.unlocked) else stringResource(R.string.unlock_requirement, frame.threshold),
                                color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                            SecondaryButton(if (selected) R.string.selected else if (unlocked) R.string.select else R.string.locked,
                                { onSelect(frame.id) }, Modifier.fillMaxWidth(), enabled = unlocked && !selected, selected = selected)
                        }
                    }
                }
            }
            Text(stringResource(R.string.cosmetic_hint), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
