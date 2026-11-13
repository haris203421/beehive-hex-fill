package com.beehivehex.fill.presentation.gameover

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beehivehex.fill.R
import com.beehivehex.fill.core.di.ServiceLocator
import com.beehivehex.fill.core.navigation.RouteEvent
import com.beehivehex.fill.core.theme.*
import com.beehivehex.fill.core.ui.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameOverRoute(roundId: String, onReplay: (String) -> Unit, onMenu: () -> Unit, onFriends: () -> Unit) {
    val model: GameOverViewModel = viewModel(key = "result:$roundId", factory = ServiceLocator.factory(roundId))
    val state by model.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { model.events.collect { if (it is RouteEvent.Replay) onReplay(it.roundId) } }
    GameOverScreen(state, model::replay, onMenu, onFriends, model::retry)
}

@Composable
fun GameOverScreen(state: GameOverUiState, onReplay: () -> Unit, onMenu: () -> Unit, onFriends: () -> Unit, onRetry: () -> Unit) {
    var appeared by rememberSaveable(state.result?.id) { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val entrance by animateFloatAsState(if (appeared) 1f else 0.94f, tween(280), label = "result")
    val burst by animateFloatAsState(if (appeared) 1f else 0f, tween(650), label = "bloom")
    val result = state.result
    HiveBackdrop(BackdropStyle.RESULT) {
        if (result != null && (result.score > result.previousBest || result.unlocked.isNotEmpty())) Canvas(Modifier.fillMaxSize()) {
            repeat(18) { index ->
                val angle = index * Math.PI * 2 / 18
                val distance = (40 + 100 * burst).dp.toPx()
                val point = Offset(size.width / 2 + cos(angle).toFloat() * distance, size.height * 0.24f + sin(angle).toFloat() * distance)
                drawCircle(Honey.copy(alpha = (1f - burst) * 0.7f), (2 + index % 3).dp.toPx(), point)
            }
        }
        Column(Modifier.align(Alignment.TopCenter).widthIn(max = 480.dp).fillMaxSize().safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SymbolArt(state.bee, Modifier.size(96.dp), stringResource(R.string.bee_portrait, stringResource(beeLabel(state.bee))))
            Text(stringResource(R.string.round_over), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.round_over_subtitle), color = TextSecondary, textAlign = TextAlign.Center)
            if (result != null) {
                GlassPanel(Modifier.fillMaxWidth().graphicsLayer { scaleX = entrance; scaleY = entrance }, Honey.copy(alpha = 0.42f)) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.number, result.score), style = MaterialTheme.typography.displaySmall.copy(fontSize = 48.sp, lineHeight = 56.sp, fontWeight = FontWeight.Bold),
                            color = HoneyLight, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        Text(stringResource(R.string.pollen_collected), color = TextSecondary)
                        if (result.score > result.previousBest) Surface(shape = RoundedCornerShape(16.dp), color = Honey.copy(alpha = 0.12f)) {
                            Text(stringResource(R.string.new_best), Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Honey, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Text(stringResource(R.string.another_hive), textAlign = TextAlign.Center, color = TextSecondary)
                PrimaryButton(R.string.play_again, onReplay, enabled = !state.busy)
                SecondaryButton(R.string.menu, onMenu, Modifier.fillMaxWidth())
                if (result.moves > 0 && result.lines > 0) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(R.string.moves, result.moves.toLong(), Modifier.weight(1f))
                    StatCard(R.string.lines_cleared, result.lines.toLong(), Modifier.weight(1f))
                } else Text(stringResource(R.string.round_summary, result.moves, result.lines), color = TextSecondary)
                if (result.highestStreak > 0) Text(stringResource(R.string.highest_streak, result.highestStreak), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                GlassPanel(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SymbolArt("pollen", Modifier.size(52.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (result.unlocked.isNotEmpty()) {
                                Text(stringResource(R.string.hive_grew), color = Honey, style = MaterialTheme.typography.labelSmall)
                                result.unlocked.forEach { Text(stringResource(rewardLabel(it)), style = MaterialTheme.typography.bodyLarge) }
                            } else if (state.nextId != null) Text(stringResource(R.string.next_friend, state.total, state.nextThreshold, stringResource(rewardLabel(state.nextId))),
                                style = MaterialTheme.typography.bodyMedium)
                            else Text(stringResource(R.string.all_unlocked), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                SaveWarning(state.saveFailed, onRetry)
            } else {
                Text(stringResource(R.string.result_unavailable), color = TextSecondary, textAlign = TextAlign.Center)
                SecondaryButton(R.string.menu, onMenu, Modifier.fillMaxWidth())
            }
            TextButton(onFriends, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.bee_friends)) }
        }
    }
}
