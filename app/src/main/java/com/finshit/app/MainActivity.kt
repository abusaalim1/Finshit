package com.finshit.app

import android.os.Bundle
import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme

class MainActivity : ComponentActivity() {
    private val store by viewModels<Store>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val ctx = LocalContext.current
            var pend by remember { mutableStateOf<Pair<Int?, Int>>(null to 0) }
            val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
                uris.forEach { store.addFile(ctx, it, pend.first, pend.second) }
            }
            store.launcher = { pid, fo -> pend = pid to fo; picker.launch(arrayOf("*/*")) }
            val scope = rememberCoroutineScope()
            val onGoogle: () -> Unit = {
                scope.launch {
                    store.authLoading = true
                    val r = if (Config.PREVIEW_UI_ONLY) { delay(1200); Result.success(Unit) } else AuthManager().signIn(this@MainActivity).map { }
                    r.onSuccess { store.signedIn = true }.onFailure { Toast.makeText(ctx, "Sign-in failed: ${it.message}", Toast.LENGTH_LONG).show() }
                    store.authLoading = false
                }
            }
            App(store, onGoogle)
        }
    }
}

@Composable
fun App(s: Store, onGoogle: () -> Unit) {
    val sys = isSystemInDarkTheme()
    FinshitTheme(s.dark ?: sys) {
        if (!s.signedIn) LoginScreen(s.authLoading, onGoogle) else {
        val p = LocalPal.current
        Box(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(p.b1, p.b2))).drawBehind {
                val r = 380f
                drawCircle(Brush.radialGradient(listOf(Color(0x80FFB48F), Color.Transparent), Offset(size.width, 0f), r), r, Offset(size.width, 0f))
                drawCircle(Brush.radialGradient(listOf(Color(0x66FFD9C4), Color.Transparent), Offset(0f, size.height * .85f), r), r, Offset(0f, size.height * .85f))
            }
        ) {
            BackHandler(s.stack.size > 1 || s.tele != null) { if (s.tele != null) s.tele = null else s.back() }
            AnimatedContent(s.cur, transitionSpec = {
                (fadeIn(tween(420)) + slideInVertically(tween(420)) { it / 24 }) togetherWith fadeOut(tween(140))
            }, label = "screen") { c ->
                when (c) {
                    Scr.Home -> HomeS(s); Scr.Projects -> ProjectsS(s); Scr.Tasks -> TasksS(s); Scr.Library -> LibraryS(s)
                    Scr.Search -> SearchS(s); Scr.Favs -> FavsS(s); Scr.Explore -> ExploreS(s); Scr.Cal -> CalS(s)
                    Scr.Board -> BoardS(s); Scr.Brand -> BrandS(s); Scr.Storage -> StorageS(s); Scr.Settings -> SettingsS(s, sys); Scr.AI -> AiS(s)
                    is Scr.Project -> ProjectS(s, c.id); is Scr.Folder -> FolderS(s, c.id, c.i); is Scr.Editor -> EditorS(s, c.id)
                }
            }
            if (s.cur !is Scr.Editor) Tabs(s, Modifier.align(Alignment.BottomCenter))
            Sheets(s)
            s.tele?.let { Tele(s, it) }
        }
        }
    }
}

@Composable
fun Tabs(s: Store, m: Modifier) {
    val root = s.stack.first()
    @Composable fun Tab(sc: Scr, l: String, i: ImageVector) {
        val on = root == sc
        Column(Modifier.width(58.dp).tap { s.root(sc) }, verticalArrangement = Arrangement.spacedBy(3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Ico(i, 22.dp, if (on) Ac else LocalPal.current.mu); T(l, 10, FontWeight.Bold, if (on) Ac else LocalPal.current.mu)
        }
    }
    Row(m.navigationBarsPadding().padding(14.dp).fillMaxWidth().height(68.dp).glass(34.dp), Arrangement.SpaceAround, Alignment.CenterVertically) {
        Tab(Scr.Home, "Home", Icons.Outlined.Home); Tab(Scr.Projects, "Projects", Icons.Outlined.Folder)
        Box(Modifier.size(52.dp).tap { s.sheet = Sh.Add }.clip(CircleShape).background(Brush.linearGradient(listOf(Ac2, Ac))), Alignment.Center) { Ico(Icons.Outlined.Add, 26.dp, Color.White) }
        Tab(Scr.Tasks, "Tasks", Icons.Outlined.TaskAlt); Tab(Scr.Library, "Library", Icons.Outlined.GridView)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sheets(s: Store) {
    val sh = s.sheet ?: return
    val p = LocalPal.current
    ModalBottomSheet({ s.sheet = null }, containerColor = p.b1, contentColor = p.tx, shape = RoundedCornerShape(32.dp)) {
        Column(Modifier.padding(20.dp, 0.dp, 20.dp, 28.dp).navigationBarsPadding().imePadding()) {
            when (sh) {
                Sh.Add -> {
                    H("Create", 19, m = Modifier.padding(bottom = 14.dp))
                    RowCard(Icons.Outlined.Folder, "New project", onClick = { s.sheet = Sh.NewProj })
                    RowCard(Icons.Outlined.FileUpload, "Import files", onClick = { s.sheet = Sh.Import(s.projects.lastOrNull()?.id, 0) })
                    RowCard(Icons.Outlined.Edit, "New script", onClick = { s.newNote() })
                    RowCard(Icons.Outlined.TaskAlt, "New task", onClick = { s.sheet = Sh.NewTask })
                }
                Sh.NewProj -> {
                    var n by remember { mutableStateOf("") }
                    var t by remember { mutableIntStateOf(0) }
                    val types = listOf("YouTube Video", "Reel", "Short", "Post")
                    H("New project", 19, m = Modifier.padding(bottom = 14.dp))
                    Inp(n, { n = it }, "Project name")
                    Row(Modifier.horizontalScroll(rememberScrollState())) { types.forEachIndexed { i, x -> Chip(x, t == i) { t = i } } }
                    Bt("Create project", Modifier.fillMaxWidth().padding(top = 8.dp)) { if (n.isNotBlank()) s.addProject(n.trim(), types[t]) }
                }
                Sh.NewTask -> {
                    var n by remember { mutableStateOf("") }
                    H("New task", 19, m = Modifier.padding(bottom = 14.dp))
                    Inp(n, { n = it }, "What needs to be done?", onDone = { if (n.isNotBlank()) s.addTask(n.trim()) })
                    Bt("Add task", Modifier.fillMaxWidth()) { if (n.isNotBlank()) s.addTask(n.trim()) }
                }
                is Sh.Import -> {
                    var pid by remember { mutableStateOf(sh.pid) }
                    var fo by remember { mutableIntStateOf(sh.fo) }
                    H("Import files", 19, m = Modifier.padding(bottom = 14.dp))
                    Mu("Save to project", m = Modifier.padding(bottom = 6.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        Chip("Library only", pid == null) { pid = null }
                        s.projects.forEach { Chip(it.name, pid == it.id) { pid = it.id } }
                    }
                    Mu("Folder", m = Modifier.padding(bottom = 6.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState())) { FOL.forEachIndexed { i, x -> Chip(x, fo == i) { fo = i } } }
                    Seg(listOf("Copy", "Link only"), if (s.linkOnly) 1 else 0) { s.linkOnly = it == 1 }
                    Bt("Choose from Gallery or Files", Modifier.fillMaxWidth()) { s.pick(pid, fo) }
                }
            }
        }
    }
}
