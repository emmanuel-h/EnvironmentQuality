package fr.mandarine.environmentquality.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.mandarine.environmentquality.domain.model.GeoPoint
import fr.mandarine.environmentquality.domain.model.WaterStatus
import fr.mandarine.environmentquality.ui.theme.Palette
import kotlin.math.cos
import kotlin.math.hypot

/** Something drawn on the map: a commune with its status, or a station. */
data class MapPin(val id: String, val location: GeoPoint, val status: WaterStatus?, val selected: Boolean)

/**
 * France drawn on a Canvas in an equirectangular projection corrected for
 * latitude. Pinch to zoom, drag to pan, tap a pin to select it.
 */
@Composable
fun FranceMap(
    pins: List<MapPin>,
    onPinTap: (String) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    bottomInset: Dp = 0.dp,
) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    // Written while drawing and read on tap: plain storage, not snapshot state.
    val projection = remember { arrayOfNulls<Projection>(1) }
    val currentPins by androidx.compose.runtime.rememberUpdatedState(pins)

    Canvas(
        modifier = modifier
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, panChange, zoomChange, _ ->
                    val newZoom = (zoom * zoomChange).coerceIn(1f, 12f)
                    // Keep the point under the fingers still while zooming.
                    pan = (pan - centroid) * (newZoom / zoom) + centroid + panChange
                    zoom = newZoom
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { tap ->
                    val proj = projection[0] ?: return@detectTapGestures
                    val hit = currentPins
                        .map { it to proj.toScreen(it.location, zoom, pan) }
                        .minByOrNull { (_, at) -> hypot(at.x - tap.x, at.y - tap.y) }
                    if (hit != null && hypot(hit.second.x - tap.x, hit.second.y - tap.y) < 28.dp.toPx()) {
                        onPinTap(hit.first.id)
                    }
                }
            },
    ) {
        val proj = Projection.fit(size, topInset.toPx(), bottomInset.toPx(), 16.dp.toPx())
        projection[0] = proj
        drawRect(Palette.MapSea)
        FranceOutline.all.forEach { ring ->
            val path = Path()
            ring.forEachIndexed { index, point ->
                val at = proj.toScreen(point, zoom, pan)
                if (index == 0) path.moveTo(at.x, at.y) else path.lineTo(at.x, at.y)
            }
            path.close()
            drawPath(path, Palette.MapLand)
            drawPath(path, Palette.MapStroke, style = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round))
        }
        pins.sortedBy { it.selected }.forEach { pin ->
            drawPin(pin, proj.toScreen(pin.location, zoom, pan))
        }
    }
}

private class Projection(private val scale: Float, private val origin: Offset) {

    fun toScreen(point: GeoPoint, zoom: Float, pan: Offset): Offset {
        val base = Offset(origin.x + x(point.longitude) * scale, origin.y - point.latitude.toFloat() * scale)
        return base * zoom + pan
    }

    companion object {
        private const val MIN_LAT = 41.3f
        private const val MAX_LAT = 51.1f
        private const val MIN_LON = -5.0f
        private const val MAX_LON = 9.6f
        private val K = cos(Math.toRadians(46.5)).toFloat()

        fun x(longitude: Double) = longitude.toFloat() * K

        fun fit(size: Size, top: Float, bottom: Float, padding: Float): Projection {
            val width = (MAX_LON - MIN_LON) * K
            val height = MAX_LAT - MIN_LAT
            val availableW = size.width - 2 * padding
            val availableH = (size.height - top - bottom - 2 * padding).coerceAtLeast(size.height * 0.3f)
            val scale = minOf(availableW / width, availableH / height)
            val left = padding + (availableW - width * scale) / 2 - MIN_LON * K * scale
            val topY = top + padding + (availableH - height * scale) / 2 + MAX_LAT * scale
            return Projection(scale, Offset(left, topY))
        }
    }
}

private fun DrawScope.drawPin(pin: MapPin, at: Offset) {
    val status = pin.status
    val size = (if (pin.selected) 34.dp else 24.dp).toPx()
    val half = size / 2
    if (pin.selected) drawCircle(Palette.Water.copy(alpha = 0.3f), radius = half + 6.dp.toPx(), center = at)
    val color = status?.let {
        when (it) {
            WaterStatus.CONFORM -> Palette.Conform
            WaterStatus.RESERVES -> Palette.Reserves
            WaterStatus.NON_CONFORM -> Palette.NonConform
            WaterStatus.UNKNOWN -> Palette.Muted
        }
    } ?: Palette.Water
    val border = 2.5.dp.toPx()
    val topLeft = Offset(at.x - half, at.y - half)
    val glyph = Stroke(width = size * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val u = size / 24f
    when (status) {
        WaterStatus.RESERVES -> {
            drawRoundRect(Color.White, topLeft, Size(size, size), CornerRadius(7.dp.toPx()))
            drawRoundRect(color, topLeft + Offset(border, border), Size(size - 2 * border, size - 2 * border), CornerRadius(5.dp.toPx()))
            drawLine(Color.White, at + Offset(0f, -5 * u), at + Offset(0f, 2 * u), glyph.width, StrokeCap.Round)
            drawCircle(Color.White, glyph.width / 2, at + Offset(0f, 6 * u))
        }
        WaterStatus.NON_CONFORM -> rotate(45f, at) {
            drawRoundRect(Color.White, topLeft, Size(size, size), CornerRadius(5.dp.toPx()))
            drawRoundRect(color, topLeft + Offset(border, border), Size(size - 2 * border, size - 2 * border), CornerRadius(3.dp.toPx()))
            drawLine(Color.White, at + Offset(-6 * u, 0f), at + Offset(6 * u, 0f), glyph.width, StrokeCap.Round)
            drawLine(Color.White, at + Offset(0f, -6 * u), at + Offset(0f, 6 * u), glyph.width, StrokeCap.Round)
        }
        else -> {
            drawCircle(Color.White, half, at)
            drawCircle(color, half - border, at)
            when (status) {
                WaterStatus.CONFORM -> {
                    val tick = Path().apply {
                        moveTo(at.x - 6 * u, at.y + 0.5f * u)
                        lineTo(at.x - 2 * u, at.y + 4.5f * u)
                        lineTo(at.x + 6 * u, at.y - 4.5f * u)
                    }
                    drawPath(tick, Color.White, style = glyph)
                }
                WaterStatus.UNKNOWN -> drawLine(Color.White, at + Offset(-5 * u, 0f), at + Offset(5 * u, 0f), glyph.width, StrokeCap.Round)
                else -> {
                    val wave = Path().apply {
                        moveTo(at.x - 7 * u, at.y)
                        cubicTo(at.x - 4 * u, at.y - 3 * u, at.x - 2 * u, at.y + 3 * u, at.x, at.y)
                        cubicTo(at.x + 2 * u, at.y - 3 * u, at.x + 4 * u, at.y + 3 * u, at.x + 7 * u, at.y)
                    }
                    drawPath(wave, Color.White, style = glyph)
                }
            }
        }
    }
}
