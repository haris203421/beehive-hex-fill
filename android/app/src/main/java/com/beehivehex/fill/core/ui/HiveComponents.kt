package com.beehivehex.fill.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beehivehex.fill.R
import com.beehivehex.fill.core.theme.*

enum class BackdropStyle { SPLASH, MENU, GAME, RESULT, COLLECTION, SETTINGS }

@Composable
fun HiveBackdrop(style: BackdropStyle, content: @Composable BoxScope.() -> Unit) {
    val colors = when (style) {
        BackdropStyle.SPLASH -> listOf(Background.copy(alpha = 0.94f), SecondaryForest.copy(alpha = 0.88f), DeepForest.copy(alpha = 0.97f))
        BackdropStyle.MENU -> listOf(Background.copy(alpha = 0.72f), RaisedForest.copy(alpha = 0.83f), Background.copy(alpha = 0.96f))
        BackdropStyle.GAME -> listOf(Background.copy(alpha = 0.91f), DeepForest.copy(alpha = 0.97f))
        BackdropStyle.RESULT -> listOf(SecondaryForest.copy(alpha = 0.88f), Background.copy(alpha = 0.94f), DeepForest.copy(alpha = 0.98f))
        BackdropStyle.COLLECTION -> listOf(RaisedForest.copy(alpha = 0.91f), Background.copy(alpha = 0.96f))
        BackdropStyle.SETTINGS -> listOf(Background.copy(alpha = 0.94f), DeepForest.copy(alpha = 0.98f))
    }
    Box(Modifier.fillMaxSize().background(Background)) {
        Image(painterResource(R.drawable.bg_game), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colors)))
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(Honey.copy(alpha = 0.07f), Color.Transparent),
                center = Offset(size.width * 0.75f, size.height * 0.25f), radius = size.width * 0.8f),
                radius = size.width * 0.8f, center = Offset(size.width * 0.75f, size.height * 0.25f))
        }
        content()
    }
}

@Composable
fun PrimaryButton(label: Int, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, spring(dampingRatio = 0.75f, stiffness = 450f), label = "press")
    val shape = RoundedCornerShape(16.dp)
    Button(onClick = onClick, enabled = enabled, interactionSource = interaction,
        modifier = modifier.fillMaxWidth().heightIn(min = 60.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (enabled) 6.dp else 0.dp, shape, ambientColor = ActionBlueDeep, spotColor = ActionBlueDeep)
            .background(Brush.verticalGradient(if (enabled) listOf(ActionBlue, ActionBlueDeep) else listOf(SecondaryForest, RaisedForest)), shape),
        shape = shape, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextPrimary,
            disabledContainerColor = Color.Transparent, disabledContentColor = TextSecondary.copy(alpha = 0.55f)),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(label: Int, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, selected: Boolean = false) {
    val description = stringResource(if (selected) R.string.selected_state else if (!enabled) R.string.unavailable_state else R.string.available_state)
    OutlinedButton(onClick, modifier.heightIn(min = 48.dp).semantics { stateDescription = description; this.selected = selected },
        enabled = enabled, shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Honey else TextSecondary.copy(alpha = if (enabled) 0.35f else 0.13f)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selected) SecondaryForest else RaisedForest.copy(alpha = 0.8f),
            contentColor = TextPrimary, disabledContentColor = if (selected) Honey else TextSecondary.copy(alpha = 0.45f))) {
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun GlassPanel(modifier: Modifier = Modifier, borderColor: Color = TextPrimary.copy(alpha = 0.11f), content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(18.dp), color = RaisedForest.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, borderColor), shadowElevation = 4.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
fun StatCard(label: Int, value: Long, modifier: Modifier = Modifier) {
    if (value <= 0) return
    GlassPanel(modifier) {
        Text(stringResource(R.string.number, value), style = MaterialTheme.typography.titleLarge, color = Honey)
        Text(stringResource(label), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
fun SymbolArt(id: String, modifier: Modifier = Modifier, description: String? = null) {
    when (id) {
        "clover" -> Image(painterResource(R.drawable.sprite_2), description, modifier.size(96.dp), contentScale = ContentScale.Fit)
        "bluebell" -> Image(painterResource(R.drawable.sprite_3), description, modifier.size(96.dp), contentScale = ContentScale.Fit)
        "amber" -> Image(painterResource(R.drawable.sprite_4), description, modifier.size(96.dp), contentScale = ContentScale.Fit)
        "pollen" -> Image(painterResource(R.drawable.sprite_5), description, modifier.size(96.dp), contentScale = ContentScale.Fit)
        else -> Image(painterResource(R.drawable.sprite_1), description, modifier.size(96.dp), contentScale = ContentScale.Fit)
    }
}

@Composable
fun PageHeader(title: Int, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onBack, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.back)) }
        Text(stringResource(title), Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun SaveWarning(visible: Boolean, onRetry: () -> Unit) {
    if (visible) GlassPanel(Modifier.fillMaxWidth(), GentleWarning.copy(alpha = 0.5f)) {
        Text(stringResource(R.string.save_warning), color = GentleWarning,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        SecondaryButton(R.string.try_again, onRetry)
    }
}
