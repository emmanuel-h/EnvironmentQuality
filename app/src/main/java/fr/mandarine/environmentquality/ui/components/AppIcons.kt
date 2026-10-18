package fr.mandarine.environmentquality.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** The mockup's stroke icons, drawn from the same 24×24 path data. */
object AppIcons {

    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

    private fun icon(name: String, width: Float, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { data ->
                addPath(
                    pathData = addPathNodes(data),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = width,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val Back = icon("back", 2.2f, "M15 5l-7 7 7 7")
    val ChevronRight = icon("chevron_right", 2.2f, "M9 5l7 7-7 7")
    val ChevronDown = icon("chevron_down", 2.2f, "M6 9l6 6 6-6")
    val Search = icon("search", 2f, circle(11f, 11f, 6.5f), "M16 16l4.5 4.5")
    val Info = icon("info", 2f, circle(12f, 12f, 9f), "M12 11v6M12 7.5v.5")
    val Map = icon("map", 2f, "M3 6l6-2 6 2 6-2v14l-6 2-6-2-6 2z", "M9 4v14M15 6v14")
    val Star = icon("star", 2f, "M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8-5.2-2.7-5.2 2.7 1-5.8-4.3-4.1 5.9-.9z")
    val Bell = icon("bell", 2f, "M6 9a6 6 0 0 1 12 0c0 6 2.5 7.5 2.5 7.5h-17S6 15 6 9z", "M10 20a2 2 0 0 0 4 0")
    val Check = icon("check", 3f, "M5 12.5l4.5 4.5L19 7.5")
    val Close = icon("close", 2.6f, "M6.5 6.5l11 11M17.5 6.5l-11 11")
    val Exclamation = icon("exclamation", 3.5f, "M12 5v9M12 19v.5")
    val Minus = icon("minus", 3f, "M6 12h12")
    val Waves = icon("waves", 2f, "M3 10c3-3 6 3 9 0s6 3 9 0M3 16c3-3 6 3 9 0s6 3 9 0")
    val Drop = icon("drop", 2f, "M12 3c3.5 4.2 6 7.6 6 10.6A6 6 0 0 1 6 13.6C6 10.6 8.5 7.2 12 3z", "M9.2 14.2a2.9 2.9 0 0 0 2.8 2.6")
    val Drought = icon("drought", 2.2f, "M12 3c3.5 4.2 6 7.6 6 10.6A6 6 0 0 1 6 13.6C6 10.6 8.5 7.2 12 3z", "M4 4l16 16")
    val Warning = icon("warning", 2.2f, "M12 3l9 16H3z", "M12 10v4M12 17v.5")
    val Trend = icon("trend", 2f, "M3 17l5-5 4 3 8-8", "M15 7h5v5")
    val Compare = icon("compare", 2f, "M8 4v16M16 4v16M4 8h4M16 16h4")
    val Download = icon("download", 2f, "M12 3v12M7 10l5 5 5-5M5 20h14")
    val Locate = icon("locate", 2f, circle(12f, 12f, 3f), "M12 2v3M12 19v3M2 12h3M19 12h3", circle(12f, 12f, 7.5f))
    val Clock = icon("clock", 2.2f, circle(12f, 12f, 9f), "M12 7v5l3 2")
    val Plus = icon("plus", 2.2f, "M12 5v14M5 12h14")
    val External = icon("external", 2f, "M14 4h6v6M20 4l-9 9", "M18 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h5")
    val Flood = icon("flood", 2f, "M3 15c3-3 6 3 9 0s6 3 9 0M3 20c3-3 6 3 9 0s6 3 9 0M12 3v8M8.5 7.5L12 11l3.5-3.5")
    val Terrain = icon("terrain", 2f, "M3 20l6-9 4 5 3-4 5 8z")
    val Factory = icon("factory", 2f, "M3 21V10l6 4V10l6 4V5h6v16z")
    val Truck = icon("truck", 2f, "M3 7h11v9H3zM14 10h4l3 3v3h-7", circle(7f, 18f, 1.6f), circle(17f, 18f, 1.6f))
    val Fire = icon("fire", 2f, "M12 3c1 3 4 5 4 9a4 4 0 0 1-8 0c0-2 1-3 2-4 0 2 1 3 2 3 0-3-1-5 0-8z")
    val Radon = icon("radon", 2f, circle(12f, 12f, 2f), "M12 3a9 9 0 0 1 7.8 4.5L14 11M4.2 7.5A9 9 0 0 1 12 3M12 14l3.9 6.7A9 9 0 0 1 8.1 20.7z")
    val Quake = icon("quake", 2f, "M2 12h4l2-5 3 10 3-8 2 3h6")
    val Thermometer = icon("thermometer", 2f, "M10 14V5a2 2 0 0 1 4 0v9a4 4 0 1 1-4 0z")
}
