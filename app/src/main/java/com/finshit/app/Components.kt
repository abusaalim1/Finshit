package com.finshit.app

import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Spring press-scale + click, no ripple (iOS feel). */
fun Modifier.tap(onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pr by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pr) .96f else 1f, spring(.6f, 800f), label = "tap")
    this.graphicsLayer { scaleX = sc; scaleY = sc }.clickable(src, null, onClick = onClick)
}

@Composable
fun T(t: String, size: Int = 15, w: FontWeight = FontWeight.Normal, color: Color? = null, center: Boolean = false, lines: Int = Int.MAX_VALUE, m: Modifier = Modifier) =
    Text(t, m, color = color ?: LocalPal.current.tx, fontSize = size.sp, fontWeight = w, fontFamily = BodyF, maxLines = lines,
        overflow = TextOverflow.Ellipsis, textAlign = if (center) TextAlign.Center else null)

@Composable
fun Mu(t: String, size: Int = 13, center: Boolean = false, m: Modifier = Modifier) = T(t, size, color = LocalPal.current.mu, center = center, m = m)

@Composable
fun H(t: String, size: Int = 26, color: Color? = null, m: Modifier = Modifier, center: Boolean = false) =
    Text(t, m, textAlign = if (center) TextAlign.Center else null, color = color ?: LocalPal.current.tx, fontSize = size.sp, fontFamily = HeadF, fontWeight = FontWeight.Bold, letterSpacing = (-0.03).em)

@Composable
fun Ico(i: ImageVector, size: Dp = 22.dp, tint: Color? = null) = Icon(i, null, Modifier.size(size), tint ?: LocalPal.current.tx)

@Composable
fun IcoBox(i: ImageVector, box: Dp = 42.dp, ic: Dp = 22.dp, r: Dp = 14.dp) =
    Box(Modifier.size(box).clip(RoundedCornerShape(r)).background(Ac.copy(.12f)), Alignment.Center) { Ico(i, ic, Ac) }

@Composable
fun IBtn(i: ImageVector, tint: Color? = null, size: Dp = 42.dp, onClick: () -> Unit) =
    Box(Modifier.size(size).tap(onClick).glass(size / 2), Alignment.Center) { Ico(i, 20.dp, tint) }

@Composable
fun Bt(t: String, m: Modifier = Modifier, ghost: Boolean = false, onClick: () -> Unit) {
    val p = LocalPal.current
    val sh = RoundedCornerShape(16.dp)
    Box(
        m.tap(onClick).clip(sh).then(
            if (ghost) Modifier.background(p.gl).border(1.dp, p.gb, sh)
            else Modifier.background(Brush.linearGradient(listOf(Ac2, Ac)))
        ).padding(horizontal = 20.dp, vertical = 13.dp), Alignment.Center
    ) { T(t, 15, FontWeight.Bold, if (ghost) p.tx else Color.White) }
}

@Composable
fun Chip(t: String, on: Boolean, onClick: () -> Unit) {
    val p = LocalPal.current
    Box(
        Modifier.padding(end = 6.dp, bottom = 8.dp).tap(onClick).clip(CircleShape)
            .background(if (on) Ac else p.gl).border(1.dp, if (on) Color.Transparent else p.gb, CircleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) { T(t, 13, FontWeight.Bold, if (on) Color.White else p.tx) }
}

@Composable
fun Seg(items: List<String>, sel: Int, onSel: (Int) -> Unit) {
    val p = LocalPal.current
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(14.dp)).background(p.gl).border(1.dp, p.gb, RoundedCornerShape(14.dp)).padding(4.dp)) {
        items.forEachIndexed { i, s ->
            Box(Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(if (i == sel) Ac else Color.Transparent).clickable { onSel(i) }.padding(9.dp), Alignment.Center) {
                T(s, 13, FontWeight.Bold, if (i == sel) Color.White else p.tx)
            }
        }
    }
}

@Composable
fun Sw(on: Boolean, onToggle: () -> Unit) {
    val x by animateDpAsState(if (on) 20.dp else 0.dp, spring(.55f, 600f), label = "sw")
    Box(Modifier.width(50.dp).height(30.dp).clip(CircleShape).background(if (on) Ac else LocalPal.current.mu.copy(.3f)).clickable { onToggle() }.padding(3.dp)) {
        Box(Modifier.offset(x).size(24.dp).clip(CircleShape).background(Color.White))
    }
}

@Composable
fun Inp(v: String, c: (String) -> Unit, hint: String, m: Modifier = Modifier, onDone: (() -> Unit)? = null) {
    val p = LocalPal.current
    val sh = RoundedCornerShape(16.dp)
    BasicTextField(
        v, c, m.fillMaxWidth().padding(bottom = 12.dp).clip(sh).background(p.gl).border(1.dp, p.gb, sh).padding(16.dp, 14.dp),
        textStyle = TextStyle(color = p.tx, fontSize = 15.sp, fontFamily = BodyF), cursorBrush = SolidColor(Ac), singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        decorationBox = { inner -> if (v.isEmpty()) Mu(hint, 15); inner() }
    )
}

@Composable
fun Empty(i: ImageVector, t: String, d: String, act: (@Composable () -> Unit)? = null) =
    Column(Modifier.fillMaxWidth().padding(vertical = 26.dp, horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        IcoBox(i, 56.dp, 26.dp, 20.dp); T(t, 15, FontWeight.Bold); Mu(d, center = true); act?.invoke()
    }

@Composable
fun RowCard(icon: ImageVector, title: String, sub: String? = null, m: Modifier = Modifier, onClick: () -> Unit, end: @Composable () -> Unit = {}) =
    Row(m.fillMaxWidth().padding(bottom = 10.dp).tap(onClick).glass().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IcoBox(icon)
        Column(Modifier.weight(1f)) { T(title, 15, FontWeight.Bold, lines = 1); sub?.let { Mu(it) } }
        end()
    }

@Composable
fun StageBar(cur: Int, h: Dp = 6.dp, onTap: ((Int) -> Unit)? = null) =
    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp), Arrangement.spacedBy(6.dp)) {
        repeat(6) { i ->
            val c by animateColorAsState(if (i <= cur) Ac else LocalPal.current.mu.copy(.25f), label = "st")
            Box(Modifier.weight(1f).height(h).clip(CircleShape).background(c).then(if (onTap != null) Modifier.clickable { onTap(i) } else Modifier))
        }
    }

@Composable
fun starIcon(fav: Boolean) = if (fav) Icons.Filled.Star else Icons.Outlined.StarBorder

@Composable
fun <X> Grid2(items: List<X>, c: @Composable (X) -> Unit) =
    items.chunked(2).forEach { r ->
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), Arrangement.spacedBy(12.dp)) {
            r.forEach { Box(Modifier.weight(1f)) { c(it) } }
            if (r.size == 1) Spacer(Modifier.weight(1f))
        }
    }
