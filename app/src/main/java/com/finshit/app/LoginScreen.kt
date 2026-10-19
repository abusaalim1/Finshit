package com.finshit.app

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val EaseX = CubicBezierEasing(.22f, 1f, .36f, 1f)
private val Bold = FontWeight.Bold

/** 0 -> 1 after [delay] ms. spring = overshoot (pop). */
@Composable
private fun rememberIn(delay: Int, ms: Int = 550, spring: Boolean = false): Float {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delay.toLong())
        if (spring) a.animateTo(1f, spring(.55f, 320f)) else a.animateTo(1f, tween(ms, easing = EaseX))
    }
    return a.value
}

private fun Modifier.enter(p: Float, dy: Float = 0f, s0: Float = 1f, dx: Float = 0f) = graphicsLayer {
    alpha = p.coerceIn(0f, 1f); translationY = (1 - p) * dy * density; translationX = (1 - p) * dx * density
    val k = s0 + (1 - s0) * p; scaleX = k; scaleY = k
}

private data class Fl(val label: String, val ic: ImageVector, val x: Int, val y: Int, val r: Float)

private val FLS get() = listOf(
    Fl("final_v3.mp4", Icons.Outlined.Videocam, -62, -78, -8f), Fl("thumb(2).png", Icons.Outlined.Image, 66, -50, 7f),
    Fl("script_old.txt", Icons.Outlined.Edit, -70, -8, 5f), Fl("raw_clip.mov", Icons.Outlined.Videocam, 58, 28, -6f),
    Fl("voice.wav", Icons.Outlined.Mic, -38, 66, 9f), Fl("notes.pdf", Icons.Outlined.InsertDriveFile, 70, 90, -4f)
)

/* ---------- Scene 1: the problem ---------- */
@Composable
private fun Scene1() {
    val inf = rememberInfiniteTransition(label = "shake")
    Box(Modifier.size(300.dp, 250.dp)) {
        FLS.forEachIndexed { n, f ->
            val p = rememberIn(n * 70, 600)
            val sh by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(450), RepeatMode.Reverse, initialStartOffset = StartOffset(n * 60)), label = "s$n")
            val k = p.coerceIn(0f, 1f) * sh
            Row(
                Modifier.align(Alignment.Center).offset((f.x + 4 * k).dp, (f.y - 3 * k).dp)
                    .graphicsLayer { alpha = p.coerceIn(0f, 1f); translationY = -(1 - p) * 190 * density; rotationZ = f.r + (1 - p) * 50 + 3 * k }
                    .glass(16.dp).padding(12.dp, 9.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) { Ico(f.ic, 16.dp, Ac); T(f.label, 12, Bold, lines = 1) }
        }
        val q = rememberIn(1000, spring = true)
        Box(Modifier.align(Alignment.TopCenter).enter(q, s0 = .5f).glass(99.dp).padding(14.dp, 7.dp)) { T("where is it?", 12, Bold, LocalPal.current.mu) }
    }
}

/* ---------- Scene 2: one project ---------- */
@Composable
private fun Scene2() {
    val p = LocalPal.current
    val c = rememberIn(0, spring = true)
    Box(Modifier.size(300.dp, 250.dp), Alignment.TopCenter) {
        Column(Modifier.padding(top = 10.dp).width(250.dp).enter(c, 30f, .7f).glass(26.dp).padding(16.dp)) {
            Row(Modifier.padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IcoBox(Icons.Outlined.Folder, 38.dp, 20.dp, 13.dp); Column { H("My First Vlog", 15); Mu("Stage · Script", 11) }
            }
            listOf(Triple(Icons.Outlined.Edit, "Scripts", 1), Triple(Icons.Outlined.Videocam, "Footage", 4), Triple(Icons.Outlined.Image, "Thumbnails", 3)).forEachIndexed { n, (ic, t, cnt) ->
                val a = rememberIn(350 + n * 120); val b = rememberIn(700 + n * 120, spring = true)
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).enter(a, dx = -24f).clip(RoundedCornerShape(15.dp)).background(p.gl).padding(12.dp, 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Ico(ic, 18.dp, Ac); T(t, 13, Bold); Spacer(Modifier.weight(1f))
                    Box(Modifier.enter(b, s0 = 0f).clip(CircleShape).background(Ac).padding(9.dp, 2.dp)) { T("$cnt", 11, Bold, Color.White) }
                }
            }
        }
    }
}

/* ---------- Scene 3: instant search ---------- */
@Composable
private fun Scene3() {
    var typed by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { delay(350); "thumbnail".forEach { typed += it; delay(75) } }
    val a = rememberIn(0, 500); val r = rememberIn(1200, 600); val bd = rememberIn(1600, spring = true)
    Box(Modifier.size(300.dp, 250.dp)) {
        Row(Modifier.align(Alignment.TopCenter).padding(top = 20.dp).width(250.dp).enter(a, 24f).glass(20.dp).padding(16.dp, 15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Ico(Icons.Outlined.Search, 18.dp, Ac); T(typed, 15); Box(Modifier.width(2.dp).height(18.dp).background(Ac))
        }
        Row(Modifier.align(Alignment.TopCenter).padding(top = 96.dp).width(250.dp).enter(r, 30f).glass(22.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(54.dp).clip(RoundedCornerShape(15.dp)).background(Brush.linearGradient(listOf(Ac2, Ac))), Alignment.Center) { Ico(Icons.Outlined.Image, 24.dp, Color.White) }
            Column { T("thumb_final.png", 14, Bold); Mu("Thumbnails · My First Vlog", 11) }
        }
        Box(Modifier.align(Alignment.TopEnd).padding(top = 74.dp, end = 30.dp).enter(bd, s0 = 0f).clip(CircleShape).background(Ac).padding(11.dp, 4.dp)) { T("0.2s", 11, Bold, Color.White) }
    }
}

/* ---------- Scene 4: start to finish ---------- */
@Composable
private fun Scene4() {
    val mu = LocalPal.current.mu
    val c = rememberIn(0, 600); val dn = rememberIn(1700, spring = true); val off = rememberIn(2000, 500)
    Box(Modifier.size(300.dp, 250.dp)) {
        Row(Modifier.align(Alignment.TopCenter).padding(top = 30.dp).width(270.dp).enter(c, 24f).glass(26.dp).padding(16.dp, 18.dp, 16.dp, 14.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("Idea", "Script", "Shoot", "Edit", "Thumb", "Live").forEachIndexed { n, l ->
                val f = remember { Animatable(0f) }
                LaunchedEffect(Unit) { delay(400L + n * 200); f.animateTo(1f, tween(300, easing = EaseX)) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(mu.copy(.22f))) {
                        Box(Modifier.fillMaxWidth(f.value).fillMaxHeight().background(Brush.horizontalGradient(listOf(Ac2, Ac))))
                    }
                    Spacer(Modifier.height(7.dp)); T(l, 10, Bold, mu)
                }
            }
        }
        Row(Modifier.align(Alignment.TopCenter).padding(top = 128.dp).enter(dn, s0 = .5f).clip(CircleShape).background(Brush.linearGradient(listOf(Ac2, Ac))).padding(20.dp, 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Ico(Icons.Outlined.Check, 18.dp, Color.White); T("Published", 15, Bold, Color.White)
        }
        Row(Modifier.align(Alignment.TopCenter).padding(top = 184.dp).enter(off, 12f).glass(99.dp).padding(14.dp, 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Ico(Icons.Outlined.WifiOff, 14.dp, mu); T("Works fully offline", 12, Bold, mu)
        }
    }
}

@Composable
private fun Cap(tag: String, problem: Boolean, h: String, p: String) {
    val mu = LocalPal.current.mu
    Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.enter(rememberIn(100), 12f).clip(CircleShape).background(if (problem) mu.copy(.2f) else Ac.copy(.15f)).padding(12.dp, 5.dp)) { T(tag.uppercase(), 11, Bold, if (problem) mu else Ac) }
        Box(Modifier.enter(rememberIn(200), 12f)) { H(h, 27, center = true, m = Modifier.widthIn(max = 290.dp)) }
        Box(Modifier.enter(rememberIn(300), 12f)) { Mu(p, 14, center = true) }
    }
}

@Composable
private fun Story(scene: Int) = Crossfade(scene, animationSpec = tween(300), label = "story") { s ->
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        when (s) { 0 -> Scene1(); 1 -> Scene2(); 2 -> Scene3(); else -> Scene4() }
        when (s) {
            0 -> Cap("The problem", true, "Your content is scattered.", "Scripts, footage, thumbnails — everywhere.")
            1 -> Cap("With Finshit", false, "One project. Everything inside.", "Scripts, footage and thumbnails together.")
            2 -> Cap("Instant search", false, "Find anything in a second.", "Files, scripts, notes and tasks.")
            else -> Cap("Start to finish", false, "Idea to published. Offline.", "Track every video, stage by stage.")
        }
    }
}

@Composable
private fun Dots(scene: Int) = Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    repeat(4) { i ->
        val f = remember(scene) { Animatable(if (i < scene) 1f else 0f) }
        LaunchedEffect(scene) { if (i == scene) f.animateTo(1f, tween(2500, easing = LinearEasing)) }
        Box(Modifier.width(34.dp).height(4.dp).clip(CircleShape).background(LocalPal.current.mu.copy(.25f))) { Box(Modifier.fillMaxWidth(f.value).fillMaxHeight().background(Ac)) }
    }
}

@Composable
private fun GoogleBtn(loading: Boolean, onClick: () -> Unit) {
    val sh = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().tap { if (!loading) onClick() }.shadow(12.dp, sh, ambientColor = Ac.copy(.2f), spotColor = Ac.copy(.3f)).clip(sh).background(Color.White).padding(16.dp),
        Arrangement.Center, Alignment.CenterVertically
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = Ac, strokeWidth = 3.dp)
        else { Image(painterResource(R.drawable.ic_google), null, Modifier.size(22.dp)); Spacer(Modifier.width(12.dp)); T("Continue with Google", 16, Bold, Color(0xFF3A1515)) }
    }
}

/** Drop-in login / sign-up screen. Call inside FinshitTheme. */
@Composable
fun LoginScreen(loading: Boolean, onGoogle: () -> Unit) {
    val p = LocalPal.current
    var scene by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(2500); scene = (scene + 1) % 4 } }
    val up = rememberIn(0, 900)
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(p.b1, p.b2))).drawBehind {
            val r = 380f
            drawCircle(Brush.radialGradient(listOf(Color(0x80FFB48F), Color.Transparent), Offset(size.width, 0f), r), r, Offset(size.width, 0f))
            drawCircle(Brush.radialGradient(listOf(Color(0x66FFD9C4), Color.Transparent), Offset(0f, size.height * .45f), r), r, Offset(0f, size.height * .45f))
        }
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                Story(scene)
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)) { Dots(scene) }
            }
            Column(Modifier.navigationBarsPadding().padding(8.dp).enter(up, 60f).fillMaxWidth().glass(34.dp).padding(22.dp, 26.dp, 22.dp, 20.dp)) {
                H("Welcome to Finshit", 30); Spacer(Modifier.height(8.dp))
                Mu("Your offline workspace for scripts, footage and everything you publish.", 15, m = Modifier.padding(bottom = 22.dp))
                GoogleBtn(loading, onGoogle)
                Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp), Arrangement.Center, Alignment.CenterVertically) {
                    listOf("Works offline", "Stays on device").forEachIndexed { i, t ->
                        if (i > 0) Spacer(Modifier.width(16.dp))
                        Box(Modifier.size(6.dp).clip(CircleShape).background(Ac)); Spacer(Modifier.width(6.dp)); Mu(t, 12)
                    }
                }
                Mu("By continuing you agree to our Terms and Privacy Policy", 11, center = true, m = Modifier.fillMaxWidth().padding(top = 10.dp))
            }
        }
    }
}
