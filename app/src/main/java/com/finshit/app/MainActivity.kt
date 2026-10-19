package com.finshit.app

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val store by viewModels<Store>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val ctx = LocalContext.current
            var pend by remember { mutableStateOf<Pair<Int?, Int?>>(null to null) }
            val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
                uris.forEach { store.addFile(ctx, it, pend.first, pend.second) }
            }
            store.launcher = { pid, fid -> pend = pid to fid; picker.launch(arrayOf("*/*")) }
            // local persistence: load once, then autosave on any data change (debounced)
            LaunchedEffect(Unit) {
                store.attach(ctx)
                snapshotFlow {
                    buildString {
                        append(store.projects.joinToString { "${it.id}|${it.name}|${it.type}|${it.s}|${it.fav}|${it.folders.joinToString(";") { f -> "${f.id},${f.name}" }}" }); append('#')
                        append(store.stages.joinToString(",")); append('#')
                        append(store.libraryFolders.joinToString { "${it.id}|${it.name}" }); append('#')
                        append(store.tasks.joinToString { "${it.id}|${it.t}|${it.status}|${it.dueDay}|${it.priority}|${it.pid}|${it.tags.joinToString(",")}|${it.subs.joinToString(",") { x -> "${x.t}=${x.d}" }}" }); append('#')
                        append(store.notes.joinToString { "${it.id}|${it.pid}|${it.icon}|${it.parentId}|${it.title}|${it.body}" }); append('#')
                        append(store.files.joinToString { "${it.id}|${it.name}|${it.size}|${it.kind}|${it.pid}|${it.folderId}|${it.uri}|${it.fav}" }); append('#')
                        append(store.events.entries.joinToString { e -> e.key + "=" + e.value.joinToString(",") }); append('#')
                        append(store.brandColors.joinToString { it.value.toString() }); append('#')
                        append("${store.brandFont}|${store.dark}|${store.lock}|${store.linkOnly}")
                    }
                }.debounce(400).collect { store.save() }
            }
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
                val ease = CubicBezierEasing(.22f, 1f, .36f, 1f)
                (fadeIn(tween(360, easing = ease)) + slideInVertically(tween(360, easing = ease)) { it / 28 }) togetherWith fadeOut(tween(160))
            }, label = "screen") { c ->
                when (c) {
                    Scr.Home -> HomeS(s); Scr.Projects -> ProjectsS(s); Scr.Tasks -> TasksS(s); Scr.Library -> LibraryS(s)
                    Scr.Search -> SearchS(s); Scr.Favs -> FavsS(s); Scr.Explore -> ExploreS(s); Scr.Cal -> CalS(s)
                    Scr.Board -> BoardS(s); Scr.Brand -> BrandS(s); Scr.Storage -> StorageS(s); Scr.Settings -> SettingsS(s, sys); Scr.AI -> AiS(s)
                    is Scr.Project -> ProjectS(s, c.id); is Scr.Folder -> FolderS(s, c.id, c.folderId)
                    is Scr.LibraryFolder -> LibraryFolderS(s, c.folderId); is Scr.Editor -> EditorS(s, c.id)
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
    val p = LocalPal.current
    val items = listOf(
        Triple(Scr.Home, "Home", Icons.Outlined.Home),
        Triple(Scr.Projects, "Projects", Icons.Outlined.Folder),
        Triple(Scr.Tasks, "Tasks", Icons.Outlined.TaskAlt),
        Triple(Scr.Library, "Library", Icons.Outlined.GridView),
    )
    val active = items.indexOfFirst { it.first == root }.coerceAtLeast(0)
    val slotIdx = if (active < 2) active else active + 1 // center slot is the + button
    val step = 62.dp
    val ox by animateDpAsState(((slotIdx - 2) * step.value).dp, spring(.55f, 500f), label = "pill")
    @Composable fun Tab(sc: Scr, l: String, i: ImageVector) {
        val on = root == sc
        val pop by animateFloatAsState(if (on) 1.12f else 1f, spring(.5f, 600f), label = "pop")
        val lc by animateColorAsState(if (on) Ac else p.mu, label = "tcol")
        Box(Modifier.width(58.dp), Alignment.Center) {
            Column(Modifier.tap { s.root(sc) }, verticalArrangement = Arrangement.spacedBy(3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.graphicsLayer { scaleX = pop; scaleY = pop }) { Ico(i, 22.dp, lc) }
                T(l, 10, FontWeight.Bold, lc)
            }
        }
    }
    Box(m.navigationBarsPadding().padding(14.dp).fillMaxWidth().height(68.dp).glass(34.dp), Alignment.Center) {
        Box(Modifier.offset(x = ox).size(width = 58.dp, height = 52.dp).clip(RoundedCornerShape(18.dp)).background(Ac.copy(.14f)))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Tab(Scr.Home, "Home", Icons.Outlined.Home); Tab(Scr.Projects, "Projects", Icons.Outlined.Folder)
            Box(Modifier.width(58.dp), Alignment.Center) {
                Box(Modifier.size(52.dp).tap { s.sheet = Sh.Add }.clip(CircleShape).background(Brush.linearGradient(listOf(Ac2, Ac))), Alignment.Center) { Ico(Icons.Outlined.Add, 26.dp, Color.White) }
            }
            Tab(Scr.Tasks, "Tasks", Icons.Outlined.TaskAlt); Tab(Scr.Library, "Library", Icons.Outlined.GridView)
        }
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
                    RowCard(Icons.Outlined.FileUpload, "Import files", onClick = { s.sheet = Sh.Import(s.projects.lastOrNull()?.id, null) })
                    RowCard(Icons.Outlined.Edit, "New script", onClick = { s.sheet = Sh.NewNote(null) })
                    RowCard(Icons.Outlined.TaskAlt, "New task", onClick = { s.sheet = Sh.NewTask })
                }
                Sh.NewProj -> {
                    var n by remember { mutableStateOf("") }
                    var t by remember { mutableIntStateOf(0) }
                    var pt by remember { mutableIntStateOf(0) }
                    val types = listOf("YouTube Video", "Reel", "Short", "Post")
                    val pts = listOf("Blank", "Video Project", "Podcast", "Minimal")
                    H("New project", 19, m = Modifier.padding(bottom = 14.dp))
                    Inp(n, { n = it }, "Project name")
                    Mu("Type", m = Modifier.padding(bottom = 6.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState())) { types.forEachIndexed { i, x -> Chip(x, t == i) { t = i } } }
                    Mu("Template", m = Modifier.padding(bottom = 6.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState())) { pts.forEachIndexed { i, x -> Chip(x, pt == i) { pt = i } } }
                    Bt("Create project", Modifier.fillMaxWidth().padding(top = 8.dp)) { if (n.isNotBlank()) s.addProject(n.trim(), types[t], pts[pt]) }
                }
                Sh.NewTask -> {
                    var n by remember { mutableStateOf("") }
                    H("New task", 19, m = Modifier.padding(bottom = 14.dp))
                    Inp(n, { n = it }, "What needs to be done?", onDone = { if (n.isNotBlank()) s.addTask(n.trim()) })
                    Bt("Add task", Modifier.fillMaxWidth()) { if (n.isNotBlank()) s.addTask(n.trim()) }
                }
                is Sh.Import -> {
                    var pid by remember { mutableStateOf(sh.pid) }
                    var fid by remember { mutableStateOf(sh.folderId) }
                    var nf by remember { mutableStateOf(false) }
                    var nn by remember { mutableStateOf("") }
                    H("Import files", 19, m = Modifier.padding(bottom = 14.dp))
                    Mu("Save to project", m = Modifier.padding(bottom = 6.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        Chip("Library only", pid == null) { pid = null; fid = null }
                        s.projects.forEach { Chip(it.name, pid == it.id) { pid = it.id; fid = null } }
                    }
                    Mu("Folder", m = Modifier.padding(bottom = 6.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        Chip("No folder", fid == null) { fid = null }
                        s.foldersOf(pid).forEach { Chip(it.name, fid == it.id) { fid = it.id } }
                        Chip("+ New folder", false) { nf = true }
                    }
                    if (nf) Inp(nn, { nn = it }, "Folder name", onDone = { if (nn.isNotBlank()) fid = s.addFolder(pid, nn.trim()).id; nn = ""; nf = false })
                    Seg(listOf("Copy", "Link only"), if (s.linkOnly) 1 else 0) { s.linkOnly = it == 1 }
                    Bt("Choose from Gallery or Files", Modifier.fillMaxWidth()) { s.pick(pid, fid) }
                }
                is Sh.NewNote -> {
                    var tp by remember { mutableIntStateOf(0) }
                    val tps = listOf("Blank", "Video Script", "Ideas", "Meeting")
                    H("New note", 19, m = Modifier.padding(bottom = 14.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState())) { tps.forEachIndexed { i, x -> Chip(x, tp == i) { tp = i } } }
                    Bt("Create", Modifier.fillMaxWidth().padding(top = 8.dp)) { s.newNote(sh.pid, tps[tp]) }
                }
                is Sh.FolderEdit -> {
                    val list = s.foldersOf(sh.pid)
                    val ex = list.firstOrNull { it.id == sh.folderId }
                    var n by remember { mutableStateOf(ex?.name ?: "") }
                    H(if (ex == null) "New folder" else "Folder", 19, m = Modifier.padding(bottom = 14.dp))
                    Inp(n, { n = it }, "Folder name")
                    Bt(if (ex == null) "Create folder" else "Save", Modifier.fillMaxWidth()) {
                        if (n.isNotBlank()) { if (ex == null) s.addFolder(sh.pid, n.trim()) else ex.name = n.trim(); s.sheet = null }
                    }
                    if (ex != null) Bt("Delete folder", Modifier.fillMaxWidth().padding(top = 8.dp), ghost = true) { s.delFolder(sh.pid, ex.id); s.sheet = null }
                }
                is Sh.Stages -> {
                    H("Stages", 19, m = Modifier.padding(bottom = 14.dp))
                    var nv by remember { mutableStateOf("") }
                    s.stages.toList().forEachIndexed { i, st ->
                        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Inp(st, { s.stages[i] = it }, "Stage name", m = Modifier.weight(1f))
                            Bt("↑", m = Modifier.width(52.dp), ghost = true) { if (i > 0) { s.stages.add(i - 1, s.stages.removeAt(i)); s.clampStages() } }
                            Bt("↓", m = Modifier.width(52.dp), ghost = true) { if (i < s.stages.size - 1) { s.stages.add(i + 1, s.stages.removeAt(i)); s.clampStages() } }
                            Bt("×", m = Modifier.width(52.dp), ghost = true) { if (s.stages.size > 1) { s.stages.removeAt(i); s.clampStages() } }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Inp(nv, { nv = it }, "New stage", m = Modifier.weight(1f))
                        Bt("Add") { if (nv.isNotBlank()) { s.stages.add(nv.trim()); nv = "" } }
                    }
                }
                is Sh.IconPick -> {
                    val n = s.notes.firstOrNull { it.id == sh.noteId }
                    H("Pick an icon", 19, m = Modifier.padding(bottom = 14.dp))
                    Grid2(listOf("📄", "🎬", "✍️", "💡", "📝", "🎧", "🖼️", "📅", "🔥", "⭐", "🎯", "🚀")) { e ->
                        Box(Modifier.fillMaxWidth().tap { n?.icon = e; s.sheet = null }.glass(18.dp).padding(14.dp), Alignment.Center) { T(e, 30, center = true) }
                    }
                }
                is Sh.TaskDetail -> {
                    val t = s.tasks.firstOrNull { it.id == sh.id }
                    if (t != null) Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                        val ctx = LocalContext.current
                        var tagv by remember { mutableStateOf("") }
                        var subv by remember { mutableStateOf("") }
                        fun pickDate() {
                            val cur = t.dueDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now()
                            DatePickerDialog(ctx, { _, y, mo, d -> t.dueDay = LocalDate.of(y, mo + 1, d).toEpochDay() }, cur.year, cur.monthValue - 1, cur.dayOfMonth).show()
                        }
                        H("Task", 19, m = Modifier.padding(bottom = 14.dp))
                        Inp(t.t, { t.t = it }, "Task name")
                        Mu("Status", m = Modifier.padding(bottom = 6.dp))
                        Seg(TST, t.status) { t.status = it }
                        Mu("Priority", m = Modifier.padding(bottom = 6.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState())) { PRN.forEachIndexed { i, x -> Chip(x, t.priority == i) { t.priority = i } } }
                        Mu("Due date", m = Modifier.padding(bottom = 6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Bt(t.dueDay?.let { "Due ${fmtDate(it)}" } ?: "Set date", ghost = true) { pickDate() }
                            if (t.dueDay != null) Bt("Clear", ghost = true) { t.dueDay = null }
                        }
                        Spacer(Modifier.height(12.dp))
                        Mu("Tags", m = Modifier.padding(bottom = 6.dp))
                        if (t.tags.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            t.tags.toList().forEach { tag ->
                                Box(Modifier.padding(end = 6.dp).clip(CircleShape).background(p.gl).border(1.dp, p.gb, CircleShape).padding(horizontal = 12.dp, vertical = 7.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        T(tag, 13, FontWeight.Bold)
                                        Box(Modifier.tap { t.tags.remove(tag) }) { T("×", 15, color = p.mu) }
                                    }
                                }
                            }
                        }
                        Inp(tagv, { tagv = it }, "Add tag…", onDone = { if (tagv.isNotBlank()) { t.tags.add(tagv.trim()); tagv = "" } })
                        Mu("Subtasks", m = Modifier.padding(bottom = 6.dp))
                        t.subs.toList().forEach { sub ->
                            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.size(24.dp).clip(CircleShape).background(if (sub.d) Ac else Color.Transparent).border(2.dp, Ac, CircleShape).tap { sub.d = !sub.d }, Alignment.Center) { if (sub.d) Ico(Icons.Outlined.Check, 13.dp, Color.White) }
                                T(sub.t, 14, FontWeight.Bold, m = Modifier.weight(1f), color = if (sub.d) p.mu else null)
                                IBtn(Icons.Outlined.Close, size = 34.dp) { t.subs.remove(sub) }
                            }
                        }
                        Inp(subv, { subv = it }, "Add subtask…", onDone = { if (subv.isNotBlank()) { t.subs.add(SubTask(subv.trim(), false)); subv = "" } })
                        Bt("Delete task", Modifier.fillMaxWidth().padding(top = 8.dp), ghost = true) { s.tasks.remove(t); s.sheet = null }
                    }
                }
                is Sh.AiNote -> {
                    val n = s.notes.firstOrNull { it.id == sh.noteId }
                    if (n != null) Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                        val scope = rememberCoroutineScope()
                        var out by remember { mutableStateOf("") }
                        var busy by remember { mutableStateOf(false) }
                        fun runAi(prompt: String) {
                            if (busy) return
                            busy = true; out = ""
                            scope.launch {
                                val ctx = "Title: ${n.title}\n\n${n.body}"
                                val r = AiClient.chat(listOf("system" to AI_SYSTEM, "user" to "$prompt\n\n$ctx"))
                                out = r.getOrElse { AiClient.friendlyError(it) }
                                busy = false
                            }
                        }
                        H("AI writing help", 19, m = Modifier.padding(bottom = 14.dp))
                        Bt("Hook ideas", Modifier.fillMaxWidth()) { runAi("Suggest 5 strong hook ideas for this script:") }
                        Bt("Improve writing", Modifier.fillMaxWidth().padding(top = 8.dp)) { runAi("Improve this writing (clearer, punchier), keeping the same structure:") }
                        Bt("Summarize", Modifier.fillMaxWidth().padding(top = 8.dp)) { runAi("Summarize this in 3 short bullets:") }
                        Spacer(Modifier.height(12.dp))
                        when {
                            busy -> Mu("Thinking…")
                            out.isNotEmpty() -> {
                                GlassBox { T(out, 14) }
                                Spacer(Modifier.height(8.dp))
                                Bt("Insert at end", Modifier.fillMaxWidth(), ghost = true) {
                                    n.body = (n.body.trimEnd() + "\n\n" + out).trimStart(); s.sheet = null
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
