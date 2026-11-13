package com.beehivehex.fill.presentation.beefriends

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
fun BeeFriendsRoute(onBack: () -> Unit) {
    val model: BeeFriendsViewModel = viewModel(factory = ServiceLocator.factory())
    val state by model.uiState.collectAsStateWithLifecycle()
    BeeFriendsScreen(state, onBack, model::select, model::retry)
}

@Composable
fun BeeFriendsScreen(state: BeeFriendsUiState, onBack: () -> Unit, onSelect: (String) -> Unit, onRetry: () -> Unit) {
    HiveBackdrop(BackdropStyle.COLLECTION) {
        Column(Modifier.align(Alignment.TopCenter).widthIn(max = 480.dp).fillMaxSize().safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PageHeader(R.string.friends_title, onBack)
            Text(stringResource(R.string.meet_friends), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.friends_hint), color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
            GlassPanel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SymbolArt(state.selected, Modifier.size(64.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.pollen_total, state.total), style = MaterialTheme.typography.titleLarge, color = Honey,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            }
            SaveWarning(state.saveFailed, onRetry)
            state.entries.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { bee ->
                        val unlocked = state.total >= bee.threshold
                        val selected = state.selected == bee.id
                        val status = stringResource(if (selected) R.string.selected_state else if (unlocked) R.string.unlocked else R.string.locked)
                        GlassPanel(Modifier.weight(1f).semantics { stateDescription = status; this.selected = selected },
                            if (selected) Honey.copy(alpha = 0.75f) else TextPrimary.copy(alpha = 0.12f)) {
                            SymbolArt(bee.id, Modifier.align(Alignment.CenterHorizontally).size(92.dp).graphicsLayer { alpha = if (unlocked) 1f else 0.45f })
                            Text(stringResource(beeLabel(bee.id)), style = MaterialTheme.typography.titleLarge)
                            Text(stringResource(beeDescription(bee.id)), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text(if (unlocked) stringResource(R.string.unlocked) else stringResource(R.string.unlock_requirement, bee.threshold),
                                style = MaterialTheme.typography.labelSmall, color = if (unlocked) Honey else TextSecondary)
                            SecondaryButton(if (selected) R.string.selected else if (unlocked) R.string.select else R.string.locked,
                                { onSelect(bee.id) }, Modifier.fillMaxWidth(), enabled = unlocked && !selected, selected = selected)
                        }
                    }
                }
            }
            Text(stringResource(R.string.cosmetic_hint), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}
