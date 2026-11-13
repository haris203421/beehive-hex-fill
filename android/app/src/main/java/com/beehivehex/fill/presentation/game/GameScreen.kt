package com.beehivehex.fill.presentation.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beehivehex.fill.R
import com.beehivehex.fill.core.di.ServiceLocator
import com.beehivehex.fill.core.navigation.RouteEvent
import com.beehivehex.fill.core.navigation.Screen
import com.beehivehex.fill.core.theme.*
import com.beehivehex.fill.core.ui.*
import com.beehivehex.fill.domain.model.Hex
import com.beehivehex.fill.presentation.dialog.HowToPlayDialog
import com.beehivehex.fill.presentation.dialog.PlacementDialog
import kotlin.math.roundToInt

data class GameActions(
    val select: (Int) -> Unit,
    val anchor: (Hex) -> Unit,
    val place: () -> Unit,
    val adjust: () -> Unit,
    val help: () -> Unit,
    val pause: () -> Unit,
    val retry: () -> Unit,
    val menu: () -> Unit,
    val result: () -> Unit,
    val drag: (Int) -> Unit,
    val drop: (Boolean) -> Unit,
    val cancelDrag: () -> Unit
)

@Composable
fun GameRoute(roundId: String, onNavigate: (Screen) -> Unit, onMenu: () -> Unit) {
    val model: GameViewModel = viewModel(key = "game:$roundId", factory = ServiceLocator.factory(roundId))
    val state by model.uiState.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) { model.events.collect { if (it is RouteEvent.Open) onNavigate(it.screen) } }
    DisposableEffect(model, owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) { Lifecycle.Event.ON_START -> model.resume(); Lifecycle.Event.ON_STOP -> model.pause(); else -> Unit }
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) model.resume()
        onDispose { owner.lifecycle.removeObserver(observer); model.pause() }
    }
    val actions = remember(model, onMenu) { GameActions(model::select, model::anchor, model::place,
        { model.adjust(true) }, { model.help(true) }, { model.setPaused(true) }, model::retry, onMenu,
        model::showResult, model::drag, model::drop, model::cancelDrag) }
    GameScreen(state, actions)
    if (state.help) HowToPlayDialog { model.help(false) }
    if (state.adjust) PlacementDialog(cursorDescription(state), state.valid, model::move, model::place, { model.adjust(false) })
    if (state.pauseDialog) AlertDialog(onDismissRequest = { model.setPaused(false) },
        title = { Text(stringResource(R.string.paused_title)) }, text = { Text(stringResource(R.string.paused_message)) },
        confirmButton = { TextButton({ model.setPaused(false) }, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.resume)) } },
        dismissButton = { TextButton(onMenu, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.menu)) } })
}

@Composable
private fun cursorDescription(state: GameUiState): String {
    val piece = state.round?.tray?.firstOrNull { it.slot == state.selectedSlot }
    val name = if (piece != null) stringResource(shapeLabel(piece.shapeId)) else stringResource(R.string.choose_piece)
    return stringResource(R.string.cursor_description, name, state.cursorRow, state.cursorCell,
        stringResource(if (state.valid) R.string.valid_placement else R.string.invalid_placement))
}

@Composable
fun GameScreen(state: GameUiState, actions: GameActions) {
    var boardBounds by remember { mutableStateOf(Rect.Zero) }
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }
    val lift = with(androidx.compose.ui.platform.LocalDensity.current) { 48.dp.toPx() }
    val ghostWidth = with(androidx.compose.ui.platform.LocalDensity.current) { 110.dp.toPx() }
    val round = state.round
    val selected = round?.tray?.firstOrNull { it.slot == state.selectedSlot }
    val status = when {
        round?.finished == true -> stringResource(R.string.finished_review)
        state.status == PlacementStatus.INVALID -> stringResource(R.string.invalid_placement)
        state.status == PlacementStatus.CLEARED -> stringResource(R.string.clear_done, state.gained)
        state.status == PlacementStatus.PLACED -> stringResource(R.string.placement_done, state.gained)
        selected != null -> stringResource(if (state.valid) R.string.valid_placement else R.string.invalid_placement)
        else -> stringResource(R.string.choose_piece)
    }
    val pulse by animateFloatAsState(if (state.phase == GamePhase.RESOLVING) 1.06f else 1f, tween(220), label = "pollen")
    val clearOpacity by animateFloatAsState(if (state.clearFading) 0f else 1f, tween(220), label = "clear")
    val currentState by rememberUpdatedState(state)
    fun dragAnchor(position: Offset): Hex? {
        val model = currentState.round ?: return null
        val point = position - Offset(0f, lift)
        return boardHit(point - boardBounds.topLeft, Size(boardBounds.width, boardBounds.height), model.mode, currentState.board)
    }
    HiveBackdrop(BackdropStyle.GAME) {
        Box(Modifier.fillMaxSize().onGloballyPositioned { rootOrigin = it.positionInRoot() }) {
            Column(Modifier.align(Alignment.TopCenter).widthIn(max = 480.dp).fillMaxSize().safeDrawingPadding()
                .verticalScroll(rememberScrollState(), enabled = !state.dragging).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = RaisedForest.copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, TextPrimary.copy(alpha = 0.12f)), shadowElevation = 3.dp) {
                    Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(actions.menu, Modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(8.dp)) {
                            Image(painterResource(R.drawable.icon_home), null, Modifier.size(18.dp), contentScale = ContentScale.Fit)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.menu), style = MaterialTheme.typography.labelSmall)
                        }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(round?.let { modeLabel(it.mode) } ?: R.string.loading_hive), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                            Text(stringResource(frameLabel(state.frame)), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        TextButton(actions.help, Modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(8.dp)) { Text(stringResource(R.string.help), style = MaterialTheme.typography.labelSmall) }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite }) {
                        Text(stringResource(R.string.pollen), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(stringResource(R.string.number, round?.score ?: 0L), style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold),
                            color = HoneyLight, modifier = Modifier.graphicsLayer { scaleX = pulse; scaleY = pulse })
                    }
                    if (state.best > 0) Text(stringResource(R.string.best_value, state.best), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    if (round?.finished != true) TextButton(actions.pause, Modifier.heightIn(min = 48.dp)) {
                        Image(painterResource(R.drawable.icon_pause), null, Modifier.size(18.dp), contentScale = ContentScale.Fit)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.pause), style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (round != null) {
                    val boardDescription = stringResource(R.string.board_description, stringResource(modeLabel(round.mode)), round.occupied.size,
                        if (selected != null) cursorDescription(state) else status)
                    Surface(Modifier.align(Alignment.CenterHorizontally).widthIn(max = 420.dp).fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = DeepForest.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, frameColor(state.frame).copy(alpha = 0.6f)), shadowElevation = 6.dp) {
                        HexBoard(round.mode, state.board, round.occupied, state.frame,
                            modifier = Modifier.padding(12.dp).onGloballyPositioned { boardBounds = it.boundsInRoot() },
                            preview = if (state.phase == GamePhase.READY) selected?.cells?.map { it + state.anchor }.orEmpty() else emptyList(),
                            valid = state.valid, cleared = state.cleared, clearOpacity = clearOpacity, description = boardDescription,
                            enabled = state.phase == GamePhase.READY, onAnchor = actions.anchor)
                    }
                    Text(status, Modifier.fillMaxWidth().heightIn(min = 40.dp).semantics { liveRegion = LiveRegionMode.Polite; stateDescription = status },
                        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                        color = if (state.status == PlacementStatus.INVALID) GentleWarning else TextSecondary)
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = RaisedForest.copy(alpha = 0.96f),
                        border = BorderStroke(1.dp, Honey.copy(alpha = 0.16f)), shadowElevation = 4.dp) {
                        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            round.tray.forEach { piece ->
                                var coordinates by remember(piece.slot) { mutableStateOf<LayoutCoordinates?>(null) }
                                val used = piece.consumed
                                val pieceDescription = if (used) stringResource(R.string.used_piece) else stringResource(R.string.piece_description,
                                    stringResource(shapeLabel(piece.shapeId)), piece.cells.size, piece.rotation + 1)
                                val slotDescription = stringResource(R.string.piece_slot, piece.slot + 1, pieceDescription)
                                val selectionDescription = stringResource(if (piece.slot == state.selectedSlot) R.string.selected_state else if (used) R.string.unavailable_state else R.string.available_state)
                                val canDrag = !used && state.phase == GamePhase.READY
                                val dragModifier = Modifier.onGloballyPositioned { coordinates = it }
                                    .pointerInput(piece, canDrag) {
                                        if (canDrag) detectDragGestures(
                                            onDragStart = { local ->
                                                dragPosition = coordinates?.takeIf { it.isAttached }?.localToRoot(local)
                                                actions.drag(piece.slot)
                                                dragPosition?.let { point -> dragAnchor(point)?.let(actions.anchor) }
                                            },
                                            onDrag = { change, delta ->
                                                change.consume()
                                                dragPosition = dragPosition?.plus(delta)
                                                dragPosition?.let { point -> dragAnchor(point)?.let(actions.anchor) }
                                            },
                                            onDragEnd = {
                                                val anchor = dragPosition?.let(::dragAnchor)
                                                if (anchor != null) actions.anchor(anchor)
                                                actions.drop(anchor != null)
                                                dragPosition = null
                                            },
                                            onDragCancel = { dragPosition = null; actions.cancelDrag() }
                                        )
                                    }
                                OutlinedButton({ actions.select(piece.slot) }, Modifier.weight(1f).heightIn(min = 104.dp).then(dragModifier)
                                    .semantics { contentDescription = slotDescription; stateDescription = selectionDescription; this.selected = piece.slot == state.selectedSlot }
                                    .graphicsLayer { alpha = if (state.dragging && state.selectedSlot == piece.slot) 0.4f else 1f },
                                    enabled = canDrag, shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(4.dp),
                                    border = BorderStroke(if (state.selectedSlot == piece.slot) 2.dp else 1.dp,
                                        if (state.selectedSlot == piece.slot) Honey else TextSecondary.copy(alpha = if (used) 0.14f else 0.28f)),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = SecondaryForest.copy(alpha = if (used) 0.1f else 0.4f),
                                        disabledContentColor = TextSecondary.copy(alpha = 0.4f))) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        PieceArt(if (used) emptyList() else piece.cells, Modifier.fillMaxWidth().height(64.dp), dimmed = !canDrag)
                                        Text(if (used) stringResource(R.string.used_piece) else stringResource(R.string.cell_count, piece.cells.size),
                                            style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                    if (round.finished) PrimaryButton(R.string.view_result, actions.result)
                    else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        SecondaryButton(R.string.adjust, actions.adjust, Modifier.weight(1f), enabled = selected != null && state.phase == GamePhase.READY)
                        PrimaryButton(R.string.place, actions.place, Modifier.weight(1.5f), enabled = state.valid && state.phase == GamePhase.READY)
                    }
                } else {
                    CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally), color = Honey)
                    SecondaryButton(R.string.menu, actions.menu, Modifier.fillMaxWidth())
                }
                SaveWarning(state.saveFailed, actions.retry)
                Spacer(Modifier.height(8.dp))
            }
            val ghost = dragPosition
            if (state.dragging && ghost != null && selected != null) PieceArt(selected.cells,
                Modifier.offset { IntOffset((ghost.x - rootOrigin.x - ghostWidth / 2).roundToInt(), (ghost.y - rootOrigin.y - lift - ghostWidth / 3).roundToInt()) }
                    .size(110.dp, 80.dp).graphicsLayer { scaleX = 1.08f; scaleY = 1.08f; alpha = 0.85f })
        }
    }
}
