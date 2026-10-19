package com.finshit.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
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

/** Glass surface: translucent fill + hairline border + soft warm shadow. */
@Composable
fun Modifier.glass(r: Dp = 24.dp): Modifier {
    val p = LocalPal.current
    val s = RoundedCornerShape(r)
    return this.shadow(14.dp, s, ambientColor = Ac.copy(.15f), spotColor = Ac.copy(.22f))
        .clip(s).background(p.gl).border(1.dp, p.gb, s)
}
