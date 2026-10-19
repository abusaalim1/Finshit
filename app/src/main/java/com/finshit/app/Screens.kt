package com.finshit.app

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle as DTS
import java.util.Locale

val TST = listOf("To do", "Doing", "Done")
val PRN = listOf("None", "Low", "Med", "High")
fun fmt(b: Long) = if (b > 1_000_000) "%.1f MB".format(b / 1e6) else "${maxOf(1, (b / 1000).toInt())} KB"
fun fmtDate(e: Long) = LocalDate.ofEpochDay(e).format(java.time.format.DateTimeFormatter.ofPattern("d MMM"))

@Composable
fun Page(content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(20.dp, 10.dp, 20.dp, 130.dp), content = content)

@Composable
fun TopRow(title: String, right: @Composable () -> Unit = {}) =
    Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) { H(title, 34); right() }

@Composable
fun Hdr(s: Store, title: String, right: @Composable () -> Unit = {}) =
    Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { IBtn(Icons.Outlined.ArrowBack) { s.back() }; H(title, 26) }
        right()
    }

@Composable fun Sec(t: String) = H(t, 19, m = Modifier.padding(top = 26.dp, bottom = 12.dp))

@Composable
fun GlassBox(m: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) = Column(m.fillMaxWidth().glass().padding(16.dp), content = content)

@Composable
fun ProjRow(s: Store, p: Proj, m: Modifier = Modifier) = RowCard(Icons.Outlined.Folder, p.name, "${p.type} · ${s.stages.getOrNull(p.s) ?: ""}", m = m, onClick = { s.go(Scr.Project(p.id)) }) {
    Box(Modifier.clickable { p.fav = !p.fav }) { Ico(starIcon(p.fav), 20.dp, if (p.fav) Ac else LocalPal.current.mu) }
}

@Composable
fun TaskRow(t: Task, onOpen: (() -> Unit)? = null, m: Modifier = Modifier) {
    val on = t.d
    val meta = buildString {
        if (t.status != 0) append(TST.getOrElse(t.status) { "" })
        t.dueDay?.let { if (isNotEmpty()) append(" · "); append("Due ${fmtDate(it)}") }
        if (t.priority > 0) { if (isNotEmpty()) append(" · "); append(PRN.getOrElse(t.priority) { "" }) }
    }
    Row(m.fillMaxWidth().padding(bottom = 10.dp).tap { onOpen?.invoke() ?: run { t.d = !t.d } }.glass().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(if (on) Ac else Color.Transparent).border(2.dp, Ac, CircleShape).tap { t.d = !t.d }, Alignment.Center) { if (on) Ico(Icons.Outlined.Check, 14.dp, Color.White) }
        Column(Modifier.weight(1f)) {
            Text(t.t, Modifier.fillMaxWidth(), color = LocalPal.current.tx.copy(if (on) .5f else 1f), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = BodyF,
                textDecoration = if (on) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
            if (meta.isNotEmpty()) Mu(meta, 12, m = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
fun NoteRow(s: Store, n: Note) = Row(Modifier.fillMaxWidth().padding(bottom = 10.dp).tap { s.go(Scr.Editor(n.id)) }.glass().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    T(n.icon, 20)
    Column(Modifier.weight(1f)) { T(n.title.ifBlank { "Untitled script" }, 15, FontWeight.Bold, lines = 1); Mu("Page") }
}

@Composable
fun FileCard(f: FileItem) {
    val p = LocalPal.current
    Column(Modifier.fillMaxWidth().glass()) {
        Box(Modifier.fillMaxWidth().height(92.dp).background(Ac.copy(.10f)), Alignment.Center) {
            if (f.kind == "Image") AsyncImage(f.uri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Ico(when (f.kind) { "Video" -> Icons.Outlined.Videocam; "Audio" -> Icons.Outlined.MusicNote; else -> Icons.Outlined.InsertDriveFile }, 26.dp, Ac)
        }
        Column(Modifier.padding(12.dp, 10.dp)) {
            T(f.name, 13, FontWeight.Bold, lines = 1)
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Mu(fmt(f.size)); Box(Modifier.clickable { f.fav = !f.fav }) { Ico(starIcon(f.fav), 17.dp, if (f.fav) Ac else p.mu) }
            }
        }
    }
}

@Composable
fun HomeS(s: Store) {
    val h = LocalTime.now().hour
    val g = if (h < 12) "Good morning" else if (h < 18) "Good afternoon" else "Good evening"
    val lp = s.projects.lastOrNull()
    val ot = s.tasks.filter { !it.d }
    Page {
        Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column { Mu(g); H("Finshit", 34) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { IBtn(Icons.Outlined.AutoAwesome) { s.go(Scr.AI) }; IBtn(Icons.Outlined.GridView) { s.go(Scr.Explore) } }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp).tap { s.go(Scr.Search) }.glass(18.dp).padding(16.dp, 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Ico(Icons.Outlined.Search, 18.dp, LocalPal.current.mu); Mu("Search projects, files, scripts", 15)
        }
        Box(Modifier.fillMaxWidth().glass().background(Brush.linearGradient(listOf(Ac2.copy(.35f), Color.Transparent))).padding(16.dp)) {
            if (lp != null) Column {
                Mu("Resume where you left"); H(lp.name, 22, m = Modifier.padding(top = 4.dp)); Mu("Stage · ${s.stages.getOrNull(lp.s) ?: ""}")
                StageBar(lp.s, s.stages); Bt("Continue", Modifier.padding(top = 12.dp)) { s.go(Scr.Project(lp.id)) }
            } else Empty(Icons.Outlined.Folder, "No active project", "Create a project to keep scripts, footage and thumbnails together.") { Bt("New project") { s.sheet = Sh.NewProj } }
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), Arrangement.spacedBy(10.dp)) {
            listOf("Projects" to s.projects.size, "Open tasks" to ot.size, "Files" to s.files.size).forEachIndexed { i, (l, n) ->
                Column(Modifier.weight(1f).enterAnim(i).glass().padding(16.dp)) { H("$n", 26); Mu(l) }
            }
        }
        Sec("Quick actions")
        Row(Modifier.fillMaxWidth().enterAnim(0), Arrangement.spacedBy(12.dp)) {
            RowCard(Icons.Outlined.Add, "Project", m = Modifier.weight(1f), onClick = { s.sheet = Sh.NewProj })
            RowCard(Icons.Outlined.FileUpload, "Import", m = Modifier.weight(1f), onClick = { s.sheet = Sh.Import(s.projects.lastOrNull()?.id, null) })
        }
        Row(Modifier.fillMaxWidth().enterAnim(1), Arrangement.spacedBy(12.dp)) {
            RowCard(Icons.Outlined.Edit, "Script", m = Modifier.weight(1f), onClick = { s.sheet = Sh.NewNote(null) })
            RowCard(Icons.Outlined.AutoAwesome, "AI", m = Modifier.weight(1f), onClick = { s.go(Scr.AI) })
        }
        Sec("Today’s tasks")
        if (ot.isEmpty()) GlassBox { Empty(Icons.Outlined.TaskAlt, "All clear", "Tasks you add will show up here.") { Bt("Add task", ghost = true) { s.sheet = Sh.NewTask } } }
        else ot.take(4).forEach { TaskRow(it) }
    }
}

@Composable
fun ProjectsS(s: Store) = Page {
    TopRow("Projects") { IBtn(Icons.Outlined.Add) { s.sheet = Sh.NewProj } }
    if (s.projects.isEmpty()) GlassBox { Empty(Icons.Outlined.Folder, "No projects yet", "Each project holds its scripts, footage, edits and thumbnails.") { Bt("Create project") { s.sheet = Sh.NewProj } } }
    else s.projects.forEachIndexed { i, p -> ProjRow(s, p, m = Modifier.enterAnim(i)) }
}

@Composable
fun ProjectS(s: Store, id: Int) {
    val p = s.proj(id) ?: return
    Page {
        Hdr(s, p.name) { IBtn(Icons.Outlined.Star, if (p.fav) Ac else null) { p.fav = !p.fav } }
        GlassBox {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Mu("${p.type} · tap a stage to update"); IBtn(Icons.Outlined.Tune, size = 36.dp) { s.sheet = Sh.Stages }
            }
            StageBar(p.s, s.stages, 10.dp) { p.s = it }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Mu(s.stages.firstOrNull() ?: ""); T(s.stages.getOrNull(p.s) ?: "", 15, FontWeight.Bold, Ac); Mu(s.stages.lastOrNull() ?: "") }
        }
        Sec("Folders")
        val tiles: List<FolderItem?> = p.folders.toList() + listOf(null)
        Grid2(tiles) { f ->
            if (f == null) Column(Modifier.fillMaxWidth().tap { s.sheet = Sh.FolderEdit(id, null) }.glass().padding(16.dp)) {
                IcoBox(Icons.Outlined.Add); Spacer(Modifier.height(12.dp)); T("+ Folder", 15, FontWeight.Bold); Mu("Create new")
            } else {
                val n = s.files.count { it.pid == id && it.folderId == f.id }
                Column(Modifier.fillMaxWidth().tap { s.go(Scr.Folder(id, f.id)) }.glass().padding(16.dp)) {
                    IcoBox(Icons.Outlined.Folder); Spacer(Modifier.height(12.dp)); T(f.name, 15, FontWeight.Bold); Mu("$n items")
                }
            }
        }
        val unfiled = s.files.filter { it.pid == id && it.folderId == null }
        if (unfiled.isNotEmpty()) { Sec("Unfiled"); Grid2(unfiled) { FileCard(it) } }
        Sec("Notes")
        s.notes.filter { it.pid == id && it.parentId == null }.forEach { NoteRow(s, it) }
        Bt("+ New note", Modifier.fillMaxWidth().padding(top = 4.dp), ghost = true) { s.sheet = Sh.NewNote(id) }
        Bt("Delete project", Modifier.fillMaxWidth().padding(top = 18.dp), ghost = true) { s.delProject(id) }
    }
}

@Composable
fun FolderS(s: Store, id: Int, folderId: Int) {
    val f = s.foldersOf(id).firstOrNull { it.id == folderId }
    val fs = s.files.filter { it.pid == id && it.folderId == folderId }
    Page {
        Hdr(s, f?.name ?: "Folder") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IBtn(Icons.Outlined.Edit) { s.sheet = Sh.FolderEdit(id, folderId) }
                IBtn(Icons.Outlined.FileUpload) { s.sheet = Sh.Import(id, folderId) }
            }
        }
        if (fs.isNotEmpty()) Grid2(fs) { FileCard(it) }
        else GlassBox { Empty(Icons.Outlined.Folder, "Nothing here yet", "Import files from your gallery or file picker.") { Bt("Import files") { s.sheet = Sh.Import(id, folderId) } } }
    }
}

@Composable
fun TasksS(s: Store) {
    var tf by remember { mutableIntStateOf(0) }
    val today = LocalDate.now().toEpochDay()
    val names = listOf("All", "Today", "Overdue", "High", "Done")
    val l = s.tasks.filter { t -> when (tf) {
        1 -> t.dueDay == today && t.status != 2
        2 -> t.dueDay != null && t.dueDay!! < today && t.status != 2
        3 -> t.priority >= 2 && t.status != 2
        4 -> t.status == 2
        else -> true
    } }
    Page {
        TopRow("Tasks") { IBtn(Icons.Outlined.Add) { s.sheet = Sh.NewTask } }
        Row(Modifier.horizontalScroll(rememberScrollState())) { names.forEachIndexed { i, x -> Chip(x, tf == i) { tf = i } } }
        if (l.isEmpty()) GlassBox { Empty(Icons.Outlined.TaskAlt, "Nothing here", "Plan your day, shoots and uploads here.") { Bt("Add task") { s.sheet = Sh.NewTask } } }
        else l.forEachIndexed { i, t -> TaskRow(t, onOpen = { s.sheet = Sh.TaskDetail(t.id) }, m = Modifier.enterAnim(i)) }
    }
}

@Composable
fun LibraryS(s: Store) {
    var flt by remember { mutableStateOf("All") }
    var nf by remember { mutableStateOf(false) }
    var nn by remember { mutableStateOf("") }
    val fs = s.files.filter { (flt == "All" || it.kind == flt) && it.pid == null }
    Page {
        TopRow("Library") { IBtn(Icons.Outlined.FileUpload) { s.sheet = Sh.Import(null, null) } }
        Row(Modifier.horizontalScroll(rememberScrollState())) { listOf("All", "Video", "Image", "Audio", "Docs").forEach { Chip(it, flt == it) { flt = it } } }
        Sec("Folders")
        Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            s.libraryFolders.forEach { Chip(it.name, false) { s.go(Scr.LibraryFolder(it.id)) } }
            Chip("+ New", false) { nf = true }
        }
        if (nf) Inp(nn, { nn = it }, "Folder name", onDone = { if (nn.isNotBlank()) s.addFolder(null, nn.trim()); nn = ""; nf = false })
        Sec("All files")
        if (fs.isEmpty()) GlassBox { Empty(Icons.Outlined.Image, "Library is empty", "Everything you import appears here, ready to search.") { Bt("Import files") { s.sheet = Sh.Import(null, null) } } }
        else Grid2(fs) { FileCard(it) }
    }
}

@Composable
fun LibraryFolderS(s: Store, folderId: Int) {
    val f = s.libraryFolders.firstOrNull { it.id == folderId }
    val fs = s.files.filter { it.pid == null && it.folderId == folderId }
    Page {
        Hdr(s, f?.name ?: "Folder") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IBtn(Icons.Outlined.Edit) { s.sheet = Sh.FolderEdit(null, folderId) }
                IBtn(Icons.Outlined.FileUpload) { s.sheet = Sh.Import(null, folderId) }
            }
        }
        if (fs.isNotEmpty()) Grid2(fs) { FileCard(it) }
        else GlassBox { Empty(Icons.Outlined.Folder, "Nothing here yet", "Import files into this folder.") { Bt("Import files") { s.sheet = Sh.Import(null, folderId) } } }
    }
}

@Composable
fun SearchS(s: Store) {
    var q by remember { mutableStateOf("") }
    val t = q.trim().lowercase()
    val a = s.projects.filter { it.name.lowercase().contains(t) }
    val b = s.files.filter { it.name.lowercase().contains(t) }
    val c = s.notes.filter { (it.title + " " + it.body).lowercase().contains(t) }
    val d = s.tasks.filter { it.t.lowercase().contains(t) }
    Page {
        Hdr(s, "Search")
        Inp(q, { q = it }, "Type to search…")
        if (t.isEmpty()) Empty(Icons.Outlined.Search, "Search everything", "Projects, files, scripts and tasks in one place.")
        else if (a.size + b.size + c.size + d.size == 0) Empty(Icons.Outlined.Search, "No results", "Try a different word.")
        else { a.forEach { ProjRow(s, it) }; c.forEach { NoteRow(s, it) }; d.forEach { TaskRow(it) }; Grid2(b) { FileCard(it) } }
    }
}

@Composable
fun FavsS(s: Store) {
    val a = s.projects.filter { it.fav }; val b = s.files.filter { it.fav }
    Page {
        Hdr(s, "Favorites")
        if (a.isEmpty() && b.isEmpty()) GlassBox { Empty(Icons.Outlined.StarBorder, "No favorites", "Tap the star on any project or file to pin it here.") }
        else { a.forEach { ProjRow(s, it) }; Grid2(b) { FileCard(it) } }
    }
}

@Composable
fun ExploreS(s: Store) = Page {
    Hdr(s, "Explore")
    listOf(
        Triple(Scr.Favs, "Favorites", Icons.Outlined.StarBorder), Triple(Scr.Cal, "Publish calendar", Icons.Outlined.CalendarMonth),
        Triple(Scr.Board, "Pipeline board", Icons.Outlined.ViewKanban), Triple(Scr.Brand, "Brand kit", Icons.Outlined.Palette),
        Triple(Scr.Storage, "Storage", Icons.Outlined.Storage), Triple(Scr.AI, "AI assistant", Icons.Outlined.AutoAwesome),
        Triple(Scr.Settings, "Settings", Icons.Outlined.Settings)
    ).forEach { (sc, t, i) -> RowCard(i, t, onClick = { s.go(sc) }) { Ico(Icons.Outlined.ChevronRight, 18.dp, LocalPal.current.mu) } }
}

@Composable
fun CalS(s: Store) {
    var ym by remember { mutableStateOf(YearMonth.now()) }
    var sel by remember { mutableStateOf<LocalDate?>(null) }
    var txt by remember { mutableStateOf("") }
    val today = LocalDate.now()
    val lead = ym.atDay(1).dayOfWeek.value % 7
    Page {
        Hdr(s, "Calendar")
        GlassBox {
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                IBtn(Icons.Outlined.ChevronLeft, size = 36.dp) { ym = ym.minusMonths(1) }
                H("${ym.month.getDisplayName(DTS.FULL, Locale.ENGLISH)} ${ym.year}", 17)
                IBtn(Icons.Outlined.ChevronRight, size = 36.dp) { ym = ym.plusMonths(1) }
            }
            Row(Modifier.fillMaxWidth()) { "SMTWTFS".forEach { Box(Modifier.weight(1f), Alignment.Center) { Mu("$it", 11) } } }
            val cells = List(lead) { null } + (1..ym.lengthOfMonth()).map { ym.atDay(it) }
            cells.chunked(7).forEach { w ->
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    (w + List(7 - w.size) { null }).forEach { d ->
                        Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp)).background(if (d != null && d == sel) Ac else Color.Transparent)
                            .then(if (d != null && d == today) Modifier.border(1.5.dp, Ac, RoundedCornerShape(12.dp)) else Modifier)
                            .clickable(enabled = d != null) { sel = d }, Alignment.Center) {
                            if (d != null) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                T("${d.dayOfMonth}", 14, FontWeight.Bold, if (d == sel) Color.White else null)
                                if (s.events[d.toString()]?.isNotEmpty() == true) Box(Modifier.size(4.dp).clip(CircleShape).background(if (d == sel) Color.White else Ac))
                            }
                        }
                    }
                }
            }
        }
        val d = sel
        if (d == null) Mu("Select a date to plan an upload.", center = true, m = Modifier.fillMaxWidth().padding(top = 16.dp))
        else {
            Sec("Plan for this day")
            Inp(txt, { txt = it }, "e.g. Upload video", onDone = { if (txt.isNotBlank()) { s.events[d.toString()] = (s.events[d.toString()] ?: emptyList()) + txt.trim(); txt = "" } })
            val ev = s.events[d.toString()] ?: emptyList()
            if (ev.isEmpty()) Mu("Nothing planned.") else ev.forEach { RowCard(Icons.Outlined.CalendarMonth, it, onClick = {}) }
        }
    }
}

@Composable
fun BoardS(s: Store) = Page {
    Hdr(s, "Board")
    Row(Modifier.horizontalScroll(rememberScrollState()), Arrangement.spacedBy(12.dp)) {
        TST.forEachIndexed { st, name ->
            val l = s.tasks.filter { it.status == st }
            Column(Modifier.width(220.dp)) {
                Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) { H(name, 14); Mu("${l.size}", 14) }
                l.forEach { t ->
                    val done = t.subs.count { it.d }
                    val meta = buildString {
                        t.dueDay?.let { append("Due ${fmtDate(it)}") }
                        if (t.priority > 0) { if (isNotEmpty()) append(" · "); append(PRN.getOrElse(t.priority) { "" }) }
                        if (t.subs.isNotEmpty()) { if (isNotEmpty()) append(" · "); append("$done/${t.subs.size} subtasks") }
                    }
                    GlassBox(Modifier.padding(bottom = 10.dp).tap { s.sheet = Sh.TaskDetail(t.id) }) {
                        T(t.t.ifBlank { "Untitled" }, 14, FontWeight.Bold, lines = 2)
                        if (meta.isNotEmpty()) Mu(meta, 12, m = Modifier.padding(top = 4.dp))
                    }
                }
                if (l.isEmpty()) Box(Modifier.fillMaxWidth().border(1.5.dp, LocalPal.current.mu.copy(.6f), RoundedCornerShape(18.dp)).padding(18.dp), Alignment.Center) { Mu("Empty") }
                Bt("+ Add", Modifier.fillMaxWidth().padding(top = 6.dp), ghost = true) {
                    val nid = s.addTask("New task", st); s.sheet = Sh.TaskDetail(nid)
                }
            }
        }
    }
}

@Composable
fun BrandS(s: Store) {
    var add by remember { mutableStateOf(false) }
    var hex by remember { mutableStateOf("") }
    Page {
        Hdr(s, "Brand kit")
        GlassBox {
            H("Colors", 17)
            Row(Modifier.padding(top = 12.dp).horizontalScroll(rememberScrollState()), Arrangement.spacedBy(10.dp)) {
                s.brandColors.forEach { Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(it)) }
                IBtn(Icons.Outlined.Add, size = 46.dp) { add = !add }
            }
            if (add) { Spacer(Modifier.height(12.dp)); Inp(hex, { hex = it }, "Hex, e.g. E8714A", onDone = {
                runCatching { Color(android.graphics.Color.parseColor("#" + hex.trim().removePrefix("#"))) }.getOrNull()?.let { s.brandColors.add(it); hex = ""; add = false }
            }) }
            else if (s.brandColors.isEmpty()) Mu("Add your channel colors.", m = Modifier.padding(top = 10.dp))
        }
        Spacer(Modifier.height(12.dp))
        GlassBox { H("Fonts", 17); Spacer(Modifier.height(12.dp)); Inp(s.brandFont, { s.brandFont = it }, "Heading and body fonts") }
        Spacer(Modifier.height(12.dp))
        GlassBox { Empty(Icons.Outlined.Image, "No logo yet", "Import your logo and default intro or outro text.") { Bt("Import logo", ghost = true) { s.sheet = Sh.Import(null, null) } } }
    }
}

@Composable
fun StorageS(s: Store) {
    val t = s.files.sumOf { it.size }
    Page {
        Hdr(s, "Storage")
        GlassBox { Mu("Used by Finshit"); H(fmt(t), 40) }
        Sec("By folder")
        val rows = buildList {
            s.projects.forEach { p ->
                p.folders.forEach { f -> add("${p.name} / ${f.name}" to s.files.filter { it.pid == p.id && it.folderId == f.id }.sumOf { it.size }) }
                val u = s.files.filter { it.pid == p.id && it.folderId == null }.sumOf { it.size }
                if (u > 0 || p.folders.isEmpty()) add("${p.name} / Unfiled" to u)
            }
            s.libraryFolders.forEach { f -> add("Library / ${f.name}" to s.files.filter { it.pid == null && it.folderId == f.id }.sumOf { it.size }) }
            add("Unfiled" to s.files.filter { it.pid == null && it.folderId == null }.sumOf { it.size })
        }
        rows.forEach { (f, z) ->
            val frac by androidx.compose.animation.core.animateFloatAsState(if (t > 0) z.toFloat() / t else 0f, label = "bar")
            Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).glass().padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { T(f, 15, FontWeight.Bold); Mu(fmt(z)) }
                Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(8.dp).clip(CircleShape).background(LocalPal.current.mu.copy(.2f))) {
                    Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(CircleShape).background(Brush.horizontalGradient(listOf(Ac2, Ac))))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        GlassBox { Empty(Icons.Outlined.Close, "Trash is empty", "Deleted files stay here for 30 days.") }
    }
}

@Composable
fun SettingsS(s: Store, sysDark: Boolean) {
    val isDark = s.dark ?: sysDark
    @Composable fun Tg(t: String, d: String, on: Boolean, f: () -> Unit) =
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp).glass().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { T(t, 15, FontWeight.Bold); Mu(d) }; Sw(on, f)
        }
    Page {
        Hdr(s, "Settings")
        Tg("Dark mode", "Switch appearance", isDark) { s.dark = !isDark }
        Tg("App lock", "Biometric for private drafts", s.lock) { s.lock = !s.lock }
        GlassBox(Modifier.padding(bottom = 10.dp)) { T("Import mode", 15, FontWeight.Bold); Spacer(Modifier.height(10.dp)); Seg(listOf("Copy into app", "Link only"), if (s.linkOnly) 1 else 0) { s.linkOnly = it == 1 } }
        RowCard(Icons.Outlined.Storage, "Encrypted backup", "Export to Drive or USB", onClick = {})
        RowCard(Icons.Outlined.Settings, "AI model", "On-device or your own API key", onClick = {})
        RowCard(Icons.Outlined.Close, "Sign out", "Back to the login screen", onClick = { AuthManager().signOut(); s.signedIn = false; s.root(Scr.Home) })
        Mu("Finshit · everything stays on your device", center = true, m = Modifier.fillMaxWidth().padding(top = 18.dp))
    }
}

internal val AI_SYSTEM = "You are the in-app AI assistant inside Finshit, a workspace app for video creators. Help with scripts, hooks, titles, thumbnails, planning. Be concise and practical."

@Composable
fun AiS(s: Store) {
    val msgs = remember { mutableStateListOf<Pair<Boolean, String>>() }
    var txt by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun ask(t: String) {
        if (sending) return
        msgs.add(true to t); txt = ""; sending = true
        scope.launch {
            val r = AiClient.chat(listOf("system" to AI_SYSTEM, "user" to t))
            msgs.add(false to r.getOrElse { AiClient.friendlyError(it) })
            sending = false
        }
    }
    val starters = mapOf(
        "Hook ideas" to "Suggest 5 strong hook ideas for a video",
        "Video titles" to "Suggest 5 clickable video titles",
        "Script outline" to "Write a short script outline"
    )
    Page {
        Hdr(s, "AI assistant")
        if (msgs.isEmpty()) Empty(Icons.Outlined.AutoAwesome, "Ask anything", "Titles, hooks, outlines and descriptions for your next video.")
        msgs.forEach { (u, t) ->
            Box(Modifier.fillMaxWidth().padding(bottom = 10.dp), contentAlignment = if (u) Alignment.CenterEnd else Alignment.CenterStart) {
                Box(Modifier.fillMaxWidth(.85f).then(if (u) Modifier.clip(RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)).background(Ac) else Modifier.glass(20.dp)).padding(16.dp, 12.dp)) { T(t, 14, color = if (u) Color.White else null) }
            }
        }
        if (sending) Mu("Thinking…", m = Modifier.padding(bottom = 10.dp))
        Row(Modifier.padding(vertical = 10.dp).horizontalScroll(rememberScrollState())) { starters.forEach { (l, p) -> Chip(l, false) { ask(p) } } }
        Inp(txt, { txt = it }, "Message AI…", onDone = { if (txt.isNotBlank()) ask(txt.trim()) })
    }
}

@Composable
fun EditorS(s: Store, id: Int) {
    val n = s.notes.firstOrNull { it.id == id } ?: return
    val p = LocalPal.current
    var tv by remember { mutableStateOf(TextFieldValue(n.body, TextRange(n.body.length))) }
    fun ins(x: String) {
        val a = tv.selection.min; val b = tv.selection.max
        val t = tv.text.substring(0, a) + x + tv.text.substring(b)
        tv = TextFieldValue(t, TextRange(a + x.length)); n.body = t
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(20.dp, 10.dp, 20.dp, 130.dp)) {
            Hdr(s, "Script") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IBtn(Icons.Outlined.AutoAwesome) { s.sheet = Sh.AiNote(n.id) }
                    IBtn(Icons.Outlined.Tv) { s.tele = id }
                }
            }
            n.parentId?.let { pidv ->
                val par = s.notes.firstOrNull { it.id == pidv }
                Mu("‹ " + (par?.title?.ifBlank { "Untitled" } ?: "Parent page"), m = Modifier.padding(bottom = 4.dp).tap { s.go(Scr.Editor(pidv)) })
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                T(n.icon, 28, m = Modifier.tap { s.sheet = Sh.IconPick(n.id) })
                BasicTextField(n.title, { n.title = it }, Modifier.weight(1f).padding(vertical = 6.dp), textStyle = TextStyle(color = p.tx, fontSize = 28.sp, fontFamily = HeadF, fontWeight = FontWeight.Bold), cursorBrush = SolidColor(Ac),
                    decorationBox = { i -> if (n.title.isEmpty()) H("Untitled script", 28, p.mu); i() })
            }
            BasicTextField(tv, { tv = it; n.body = it.text }, Modifier.fillMaxWidth().heightIn(min = 400.dp), textStyle = TextStyle(color = p.tx, fontSize = 16.sp, lineHeight = 27.sp, fontFamily = BodyF), cursorBrush = SolidColor(Ac),
                decorationBox = { i -> if (tv.text.isEmpty()) Mu("Start writing your script…", 16); i() })
            val kids = s.notes.filter { it.parentId == n.id }
            if (kids.isNotEmpty()) { Sec("Sub-pages"); kids.forEach { NoteRow(s, it) } }
            Bt("+ Add sub-page", Modifier.fillMaxWidth().padding(top = 14.dp), ghost = true) { s.addSubPage(n) }
        }
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding().padding(14.dp, 0.dp, 14.dp, 16.dp).fillMaxWidth().glass().horizontalScroll(rememberScrollState()).padding(8.dp)) {
            listOf("H1" to "# ", "H2" to "## ", "Text" to "", "Quote" to "> ", "List" to "• ", "To-do" to "☐ ", "Divider" to "\n———\n").forEach { (l, x) -> Chip(l, false) { ins(x) } }
        }
    }
}

@Composable
fun Tele(s: Store, id: Int) {
    val n = s.notes.firstOrNull { it.id == id }
    val sc = rememberScrollState()
    var on by remember { mutableStateOf(false) }
    var sp by remember { mutableFloatStateOf(2f) }
    LaunchedEffect(on) { while (on) { androidx.compose.runtime.withFrameNanos { }; sc.scrollBy(sp * 1.2f) } }
    Box(Modifier.fillMaxSize().background(Color(0xFF1D1210))) {
        Column(Modifier.fillMaxSize().verticalScroll(sc).statusBarsPadding().padding(26.dp, 80.dp, 26.dp, 220.dp)) {
            Text(n?.body?.ifBlank { null } ?: "Your script is empty.", color = Color(0xFFFFF3EC), fontSize = 34.sp, lineHeight = 51.sp, fontFamily = HeadF, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.statusBarsPadding().padding(16.dp).size(42.dp).clip(CircleShape).background(Color.White.copy(.12f)).clickable { s.tele = null }, Alignment.Center) { Ico(Icons.Outlined.Close, 20.dp, Color.White) }
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(14.dp, 0.dp, 14.dp, 16.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Color.White.copy(.10f)).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(Ac).clickable { on = !on }, Alignment.Center) { Ico(if (on) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, 20.dp, Color.White) }
            androidx.compose.material3.Slider(sp, { sp = it }, Modifier.weight(1f), valueRange = 1f..8f, colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = Ac, activeTrackColor = Ac))
        }
    }
}
