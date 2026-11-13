package com.beehivehex.fill.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beehivehex.fill.R
import com.beehivehex.fill.core.di.ServiceLocator
import com.beehivehex.fill.core.navigation.RouteEvent
import com.beehivehex.fill.core.theme.*
import com.beehivehex.fill.core.ui.*

@Composable
fun SplashRoute(onFinished: () -> Unit) {
    val model: SplashViewModel = viewModel(factory = ServiceLocator.factory())
    val state by model.uiState.collectAsStateWithLifecycle()
    val finished by model.finished.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { model.events.collect { if (it == RouteEvent.Finished) onFinished() } }
    SplashScreen(state.copy(finished = finished))
}

@Composable
fun SplashScreen(state: SplashUiState) {
    val transition = rememberInfiniteTransition(label = "hive")
    val bob by transition.animateFloat(-6f, 6f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    val orbit by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "pollen")
    val loading = stringResource(R.string.loading_hive)
    HiveBackdrop(BackdropStyle.SPLASH) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(painterResource(R.drawable.logo), null, Modifier.size(48.dp), contentScale = ContentScale.Fit)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.woodland_puzzle), style = MaterialTheme.typography.labelSmall, color = Honey)
            Spacer(Modifier.height(16.dp))
            Box(Modifier.size(200.dp).background(Brush.radialGradient(listOf(Honey.copy(alpha = 0.18f), Color.Transparent)), CircleShape), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(190.dp).rotate(orbit)) {
                    drawCircle(Honey.copy(alpha = 0.28f), style = Stroke(1.dp.toPx()))
                    drawCircle(HoneyLight, 4.dp.toPx(), Offset(size.width * 0.5f, 0f))
                    drawCircle(PrimaryLeaf.copy(alpha = 0.8f), 3.dp.toPx(), Offset(size.width * 0.5f, size.height))
                }
                SymbolArt("sunny", Modifier.offset(y = bob.dp).size(152.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center, color = TextPrimary)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.splash_tagline), style = MaterialTheme.typography.bodyLarge, color = TextSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            CircularProgressIndicator(Modifier.size(32.dp).semantics { stateDescription = loading }, color = Honey, strokeWidth = 3.dp)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(if (state.finished) R.string.hive_ready else R.string.loading_hive), style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite; stateDescription = loading })
            Spacer(Modifier.height(12.dp))
        }
    }
}
