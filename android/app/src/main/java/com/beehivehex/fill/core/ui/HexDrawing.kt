package com.beehivehex.fill.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.beehivehex.fill.core.theme.*
import com.beehivehex.fill.domain.model.Hex
import com.beehivehex.fill.domain.model.HiveMode
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private val RootThree = sqrt(3f)

fun cellCenter(hex: Hex, radius: Float, center: Offset) = Offset(
    center.x + RootThree * radius * (hex.q + hex.r / 2f), center.y + 1.5f * radius * hex.r
)

fun boardHit(position: Offset, size: Size, mode: HiveMode, board: List<Hex>): Hex? {
    if (size.width <= 0 || size.height <= 0) return null
    val radius = size.width / (RootThree * (2 * mode.radius + 1))
    val center = Offset(size.width / 2, size.height / 2)
    val nearest = board.minByOrNull { (cellCenter(it, radius, center) - position).getDistanceSquared() } ?: return null
    return nearest.takeIf { (cellCenter(it, radius, center) - position).getDistance() <= radius * 1.15f }
}

private fun hexPath(center: Offset, radius: Float) = Path().apply {
    repeat(6) { index ->
        val angle = Math.toRadians((index * 60 - 90).toDouble())
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y + radius * sin(angle).toFloat()
        if (index == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

fun frameColor(id: String): Color = when (id) {
    "golden" -> Honey
    "bluebell" -> ActionBlue
    "ivory" -> TextPrimary
    else -> PrimaryLeaf
}

private fun DrawScope.honeyCell(center: Offset, radius: Float, alpha: Float = 1f) {
    val path = hexPath(center, radius)
    drawPath(path, Brush.verticalGradient(listOf(HoneyLight.copy(alpha = alpha), HoneyDeep.copy(alpha = alpha)),
        startY = center.y - radius, endY = center.y + radius))
    drawPath(path, HoneyLight.copy(alpha = 0.7f * alpha), style = Stroke(1.5.dp.toPx()))
    drawLine(TextPrimary.copy(alpha = 0.45f * alpha), center + Offset(-radius * 0.5f, -radius * 0.52f),
        center + Offset(radius * 0.5f, -radius * 0.52f), 1.5.dp.toPx())
}

@Composable
fun HexBoard(
    mode: HiveMode,
    board: List<Hex>,
    occupied: Set<Hex>,
    frame: String,
    modifier: Modifier = Modifier,
    preview: List<Hex> = emptyList(),
    valid: Boolean = false,
    cleared: Set<Hex> = emptySet(),
    clearOpacity: Float = 1f,
    description: String,
    enabled: Boolean,
    onAnchor: (Hex) -> Unit
) {
    val ratio = RootThree * (2 * mode.radius + 1) / (3 * mode.radius + 2)
    Canvas(modifier.fillMaxWidth().aspectRatio(ratio).semantics {
        contentDescription = description
        stateDescription = description
    }.pointerInput(mode, board, enabled) {
        if (enabled) detectTapGestures { point ->
            boardHit(point, Size(size.width.toFloat(), size.height.toFloat()), mode, board)?.let(onAnchor)
        }
    }) {
        val a = size.width / (RootThree * (2 * mode.radius + 1))
        val center = Offset(size.width / 2, size.height / 2)
        board.forEach { hex ->
            val point = cellCenter(hex, a, center)
            val path = hexPath(point, a * 0.92f)
            drawPath(path, SecondaryForest.copy(alpha = 0.7f))
            drawPath(path, frameColor(frame).copy(alpha = 0.34f), style = Stroke(1.dp.toPx()))
            if (hex in occupied) honeyCell(point, a * 0.89f)
            if (hex in cleared) {
                drawPath(path, Honey.copy(alpha = 0.35f * clearOpacity))
                drawPath(path, TextPrimary.copy(alpha = 0.8f * clearOpacity), style = Stroke(2.dp.toPx()))
            }
        }
        preview.forEach { hex ->
            val point = cellCenter(hex, a, center)
            val path = hexPath(point, a * 0.9f)
            if (valid) {
                honeyCell(point, a * 0.88f, 0.65f)
                drawPath(path, TextPrimary, style = Stroke(2.dp.toPx()))
                drawLine(TextPrimary, point + Offset(-a * 0.23f, 0f), point + Offset(-a * 0.03f, a * 0.2f), 2.dp.toPx())
                drawLine(TextPrimary, point + Offset(-a * 0.03f, a * 0.2f), point + Offset(a * 0.27f, -a * 0.2f), 2.dp.toPx())
            } else {
                drawPath(path, GentleWarning.copy(alpha = 0.18f))
                drawPath(path, GentleWarning, style = Stroke(2.dp.toPx()))
                clipPath(path) {
                    for (index in -3..3) {
                        val shift = index * a * 0.45f
                        drawLine(GentleWarning.copy(alpha = 0.7f), point + Offset(-a, a + shift), point + Offset(a, -a + shift), 1.dp.toPx())
                    }
                }
            }
        }
    }
}

@Composable
fun PieceArt(cells: List<Hex>, modifier: Modifier = Modifier, dimmed: Boolean = false) {
    Canvas(modifier.size(width = 92.dp, height = 66.dp)) {
        if (cells.isEmpty()) {
            val path = hexPath(center, size.minDimension * 0.24f)
            drawPath(path, TextSecondary.copy(alpha = 0.25f), style = Stroke(1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f))))
        } else {
            val raw = cells.map { cellCenter(it, 1f, Offset.Zero) }
            val left = raw.minOf { it.x } - RootThree / 2
            val right = raw.maxOf { it.x } + RootThree / 2
            val top = raw.minOf { it.y } - 1
            val bottom = raw.maxOf { it.y } + 1
            val a = minOf(size.width / (right - left + 0.4f), size.height / (bottom - top + 0.4f), 19.dp.toPx())
            val offset = Offset(size.width / 2 - (left + right) * a / 2, size.height / 2 - (top + bottom) * a / 2)
            cells.forEach { honeyCell(cellCenter(it, a, offset), a * 0.9f, if (dimmed) 0.3f else 1f) }
        }
    }
}

@Composable
fun FrameArt(occupied: Set<Hex>, frame: String, modifier: Modifier = Modifier) {
    Canvas(modifier.size(96.dp)) {
        val a = size.minDimension / 8f
        for (r in -2..2) for (q in -2..2) {
            val hex = Hex(q, r)
            if (hex.inside(2)) {
                val point = cellCenter(hex, a, center)
                val path = hexPath(point, a * 0.9f)
                drawPath(path, SecondaryForest)
                drawPath(path, frameColor(frame).copy(alpha = 0.75f), style = Stroke(1.dp.toPx()))
                if (hex in occupied) honeyCell(point, a * 0.86f)
            }
        }
    }
}
