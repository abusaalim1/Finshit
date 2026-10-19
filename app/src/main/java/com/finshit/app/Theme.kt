package com.finshit.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val Ac = Color(0xFFE8714A)
val Ac2 = Color(0xFFF59A6F)

data class Pal(val b1: Color, val b2: Color, val tx: Color, val mu: Color, val gl: Color, val gb: Color)

val LightPal = Pal(Color(0xFFFFF9F4), Color(0xFFFFE2D2), Color(0xFF3A1515), Color(0xFF9A7B72), Color(0x94FFFFFF), Color(0xD9FFFFFF))
val DarkPal = Pal(Color(0xFF241713), Color(0xFF3A2218), Color(0xFFFBEBE2), Color(0xFFB99A8E), Color(0x12FFFFFF), Color(0x21FFFFFF))
val LocalPal = staticCompositionLocalOf { LightPal }

val HeadF = FontFamily(Font(R.font.bricolage_bold, FontWeight.Bold))
val BodyF = FontFamily(Font(R.font.dm_sans_regular, FontWeight.Normal), Font(R.font.dm_sans_bold, FontWeight.Bold))

@Composable
fun FinshitTheme(dark: Boolean, content: @Composable () -> Unit) {
    val pal = if (dark) DarkPal else LightPal
    CompositionLocalProvider(LocalPal provides pal) {
        MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Ac) else lightColorScheme(primary = Ac), content = content)
    }
}

/**
 * Glass surface: translucent fill + hairline border + soft warm shadow.
 *
 * The shadow is painted as a native-canvas draw layer BEHIND the element with the
 * element's own rounded-rect path clipped out (ClipOp.Difference), so the shadow can
 * never bleed inside the translucent background (Android elevation shadows used to
 * pool as black patches at the bottom of the cards in dark theme). Matches the
 * prototype's outside-only CSS box-shadow: light 0 1px 1px rgba(120,50,20,.04),
 * 0 8px 24px rgba(190,100,70,.10); dark 0 8px 24px rgba(0,0,0,.35); plus the
 * inset 0 1px top highlight (light white .9 / dark white .08).
 */
@Composable
fun Modifier.glass(r: Dp = 24.dp): Modifier {
    val p = LocalPal.current
    val s = RoundedCornerShape(r)
    val isDark = LocalPal.current === DarkPal
    val shColor = if (isDark) Color(0x59000000) else Color(0x19BE6446)
    val shColor2 = if (!isDark) Color(0x0A783214) else Color.Transparent
    val topHi = if (isDark) Color(0x14FFFFFF) else Color(0xE6FFFFFF)
    return this.drawBehind {
        val rr = r.toPx()
        val interior = Path().apply {
            addRoundRect(RoundRect(Rect(0f, 0f, size.width, size.height), CornerRadius(rr)))
        }
        drawIntoCanvas { canvas ->
            // --- outside-only soft shadow: main pass ---
            // NOTE: body must be opaque for setShadowLayer to cast; the interior is
            // clipped out (ClipOp.Difference) so only the outside shadow remains visible.
            val rectF = android.graphics.RectF(0f, 0f, size.width, size.height)
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                setShadowLayer(24.dp.toPx(), 0f, 8.dp.toPx(), shColor.toArgb())
            }
            canvas.save()
            canvas.clipPath(interior, ClipOp.Difference)
            canvas.nativeCanvas.drawRoundRect(rectF, rr, rr, paint)
            canvas.restore()
            // --- light-theme second tiny pass: 0 1px 1px rgba(120,50,20,.04) ---
            if (shColor2.alpha > 0f) {
                val paint2 = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    setShadowLayer(2.dp.toPx(), 0f, 1.dp.toPx(), shColor2.toArgb())
                }
                canvas.save()
                canvas.clipPath(interior, ClipOp.Difference)
                canvas.nativeCanvas.drawRoundRect(rectF, rr, rr, paint2)
                canvas.restore()
            }
        }
    }
        .clip(s).background(p.gl).border(1.dp, p.gb, s)
        .drawBehind {
            // --- inset top highlight: 1dp inner line along the top edge, drawn OVER the fill ---
            val rr = r.toPx()
            val topOnly = Path().apply {
                addRect(Rect(0f, 0f, size.width, size.height * 0.6f))
            }
            drawIntoCanvas { canvas ->
                canvas.save()
                canvas.clipPath(topOnly)
                val stroke = 1.dp.toPx()
                val hiPaint = Paint().apply {
                    color = topHi
                    style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                    strokeWidth = stroke
                }
                val inset = stroke / 2f
                canvas.drawRoundRect(
                    inset, inset, size.width - inset, size.height - inset,
                    rr, rr, hiPaint
                )
                canvas.restore()
            }
        }
}
