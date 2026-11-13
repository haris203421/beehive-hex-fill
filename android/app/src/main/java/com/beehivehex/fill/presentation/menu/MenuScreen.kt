package com.beehivehex.fill.presentation.menu

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beehivehex.fill.R
import com.beehivehex.fill.core.di.ServiceLocator
import com.beehivehex.fill.core.navigation.RouteEvent
import com.beehivehex.fill.core.navigation.Screen
import com.beehivehex.fill.core.theme.*
import com.beehivehex.fill.core.ui.*
import com.beehivehex.fill.domain.model.HiveMode
import com.beehivehex.fill.presentation.dialog.HowToPlayDialog
import com.beehivehex.fill.presentation.dialog.ReplaceRoundDialog

@Composable
fun MenuRoute(onNavigate: (Screen) -> Unit) {
    val model: MenuViewModel = viewModel(factory = ServiceLocator.factory())
    val state by model.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { model.events.collect { if (it is RouteEvent.Open) onNavigate(it.screen) } }
    MenuScreen(state, model::play, model::requestNew, model::chooseMode, { model.help(true) }, model::retry, onNavigate)
    if (state.help) HowToPlayDialog { model.help(false) }
    if (state.replace) ReplaceRoundDialog(model::newRound, { model.replace(false) })
}

@Composable
fun MenuScreen(state: MenuUiState, onPlay: () -> Unit, onNew: () -> Unit, onMode: (HiveMode) -> Unit,
    onHelp: () -> Unit, onRetry: () -> Unit, onNavigate: (Screen) -> Unit) {
    val decoration = rememberInfiniteTransition(label = "friend")
    val bob by decoration.animateFloat(-3f, 3f, infiniteRepeatable(tween(2400), RepeatMode.Reverse), label = "friend_bob")
    HiveBackdrop(BackdropStyle.MENU) {
        Column(Modifier.align(Alignment.TopCenter).widthIn(max = 480.dp).fillMaxSize().safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.logo), null, Modifier.size(42.dp), contentScale = ContentScale.Fit)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.app_name), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                TextButton({ onNavigate(Screen.Settings) }, Modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(4.dp)) {
                    Text(stringResource(R.string.settings), style = MaterialTheme.typography.labelSmall)
                }
            }
            GlassPanel(Modifier.fillMaxWidth(), Honey.copy(alpha = 0.2f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.hero_title), style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(R.string.hero_subtitle), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                    SymbolArt(state.bee, Modifier.offset(y = bob.dp).size(112.dp), stringResource(R.string.bee_portrait, stringResource(beeLabel(state.bee))))
                }
            }
            if (state.best > 0 && state.total > 0) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(R.string.best_pollen, state.best, Modifier.weight(1f))
                StatCard(R.string.total_pollen, state.total, Modifier.weight(1f))
            } else Text(stringResource(R.string.first_hive), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            SaveWarning(state.saveFailed, onRetry)
            if (state.recovered || state.readFailed) GlassPanel(Modifier.fillMaxWidth()) {
                Text(stringResource(if (state.readFailed) R.string.read_warning else R.string.recovery_notice), style = MaterialTheme.typography.bodyMedium)
                if (state.readFailed) SecondaryButton(R.string.retry, onRetry)
            }
            Text(stringResource(R.string.choose_hive), style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModeCard(HiveMode.CLASSIC, state.mode == HiveMode.CLASSIC, { onMode(HiveMode.CLASSIC) }, Modifier.weight(1f))
                ModeCard(HiveMode.POCKET, state.mode == HiveMode.POCKET, { onMode(HiveMode.POCKET) }, Modifier.weight(1f))
            }
            Text(stringResource(if (state.activeId != null) R.string.resume_hint else R.string.placement_hint),
                color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            if (state.loading || state.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Honey)
            PrimaryButton(if (state.activeId != null) R.string.resume else R.string.play, onPlay, enabled = !state.loading && !state.busy)
            if (state.activeId != null) SecondaryButton(R.string.new_round, onNew, Modifier.fillMaxWidth(), enabled = !state.busy)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(R.string.bee_friends, { onNavigate(Screen.BeeFriends) }, Modifier.weight(1f))
                SecondaryButton(R.string.frames, { onNavigate(Screen.Frames) }, Modifier.weight(1f))
            }
            TextButton(onHelp, Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp)) { Text(stringResource(R.string.how_to_play)) }
        }
    }
}

@Composable
private fun ModeCard(mode: HiveMode, selected: Boolean, onSelect: () -> Unit, modifier: Modifier) {
    val border by animateColorAsState(if (selected) ActionBlue else TextSecondary.copy(alpha = 0.18f), label = "mode")
    val description = stringResource(if (selected) R.string.selected_state else R.string.available_state)
    Surface(modifier.semantics { stateDescription = description; this.selected = selected },
        shape = RoundedCornerShape(16.dp), color = if (selected) SecondaryForest.copy(alpha = 0.9f) else RaisedForest.copy(alpha = 0.9f),
        border = BorderStroke(if (selected) 2.dp else 1.dp, border), shadowElevation = 3.dp) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(if (mode == HiveMode.CLASSIC) R.string.classic_detail else R.string.pocket_detail),
                style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            OutlinedButton(onSelect, Modifier.fillMaxWidth().heightIn(min = 48.dp), contentPadding = PaddingValues(6.dp),
                border = BorderStroke(1.dp, if (selected) Honey else TextSecondary.copy(alpha = 0.3f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)) {
                Text(stringResource(modeLabel(mode)), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
