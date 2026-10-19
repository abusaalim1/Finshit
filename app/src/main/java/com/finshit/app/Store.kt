package com.finshit.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

sealed class Scr {
    object Home : Scr(); object Projects : Scr(); object Tasks : Scr(); object Library : Scr()
    object Search : Scr(); object Favs : Scr(); object Explore : Scr(); object Cal : Scr()
    object Board : Scr(); object Brand : Scr(); object Storage : Scr(); object Settings : Scr(); object AI : Scr()
    data class Project(val id: Int) : Scr()
    data class Folder(val id: Int, val folderId: Int) : Scr()
    data class LibraryFolder(val folderId: Int) : Scr()
    data class Editor(val id: Int) : Scr()
}

sealed class Sh {
    object Add : Sh(); object NewProj : Sh(); object NewTask : Sh(); object Stages : Sh()
    data class Import(val pid: Int?, val folderId: Int?) : Sh()
    data class FolderEdit(val pid: Int?, val folderId: Int?) : Sh()
    data class NewNote(val pid: Int?) : Sh()
    data class TaskDetail(val id: Int) : Sh()
    data class IconPick(val noteId: Int) : Sh()
    data class AiNote(val noteId: Int) : Sh()
    data class LinkPage(val noteId: Int) : Sh()
}

class FolderItem(val id: Int, var name: String)

class Proj(val id: Int, val name: String, val type: String) {
    var s by mutableIntStateOf(0); var fav by mutableStateOf(false)
    val folders = mutableStateListOf<FolderItem>()
}

class SubTask(var t: String, var d: Boolean)

// Obsidian-style page links: [[Page Title]] in a note body. Derived, not stored.
// Title match is case-insensitive; same-project notes win over global ones.
private val LINK_RX = Regex("\\[\\[([^\\]]+)\\]\\]")

fun Store.noteByTitle(title: String, preferPid: Int?): Note? {
    val t = title.trim().lowercase()
    if (t.isEmpty()) return null
    val same = notes.filter { it.title.trim().lowercase() == t }
    return same.firstOrNull { it.pid == preferPid } ?: same.firstOrNull()
}

fun Store.outgoingLinks(n: Note): List<Note> =
    LINK_RX.findAll(n.body).map { it.groupValues[1] }
        .mapNotNull { noteByTitle(it, n.pid) }
        .filter { it.id != n.id }
        .distinctBy { it.id }
        .toList()

fun Store.backlinks(n: Note): List<Note> {
    val t = n.title.trim().lowercase()
    if (t.isEmpty()) return emptyList()
    return notes.filter { it.id != n.id &&
        LINK_RX.findAll(it.body).any { m -> m.groupValues[1].trim().lowercase() == t } }
}

class Task(val id: Int, title: String) {
    var t by mutableStateOf(title)
    var status by mutableIntStateOf(0) // 0 To do, 1 Doing, 2 Done
    var dueDay by mutableStateOf<Long?>(null) // LocalDate.toEpochDay
    var priority by mutableIntStateOf(0) // 0 none, 1 low, 2 medium, 3 high
    val tags = mutableStateListOf<String>()
    val subs = mutableStateListOf<SubTask>()
    var pid by mutableStateOf<Int?>(null)
    /** compat with the old done-flag: every existing toggle/filter keeps compiling */
    var d: Boolean get() = status == 2; set(v) { status = if (v) 2 else 0 }
}

class FileItem(val id: Int, val name: String, val size: Long, val kind: String, val pid: Int?, var folderId: Int?, val uri: Uri) { var fav by mutableStateOf(false) }
class Note(val id: Int, val pid: Int?) {
    var title by mutableStateOf(""); var body by mutableStateOf("")
    var icon by mutableStateOf("📄"); var parentId by mutableStateOf<Int?>(null)
}

class Store : ViewModel() {
    private var uid = 1
    val projects = mutableStateListOf<Proj>()
    val tasks = mutableStateListOf<Task>()
    val files = mutableStateListOf<FileItem>()
    val notes = mutableStateListOf<Note>()
    val events = mutableStateMapOf<String, List<String>>()
    val brandColors = mutableStateListOf<Color>()
    val stages = mutableStateListOf<String>()
    val libraryFolders = mutableStateListOf<FolderItem>()
    var brandFont by mutableStateOf("")
    var dark by mutableStateOf<Boolean?>(null)
    var lock by mutableStateOf(false)
    var signedIn by mutableStateOf(runCatching { FirebaseAuth.getInstance().currentUser != null }.getOrDefault(false))
    var authLoading by mutableStateOf(false)
    var linkOnly by mutableStateOf(false)
    val stack = mutableStateListOf<Scr>(Scr.Home)
    var sheet by mutableStateOf<Sh?>(null)
    var tele by mutableStateOf<Int?>(null)
    var launcher: ((Int?, Int?) -> Unit)? = null

    val cur get() = stack.last()

    // ---------------- local persistence (finshit-data.json in filesDir) ----------------
    private var appCtx: Context? = null
    private val dataFile get() = File(appCtx!!.filesDir, "finshit-data.json")

    /** Call once from MainActivity. Loads saved state (defensive: corrupt file -> start fresh). */
    fun attach(ctx: Context) {
        if (appCtx != null) return
        appCtx = ctx.applicationContext
        load()
    }

    fun save() {
        val ctx = appCtx ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val o = JSONObject()
                o.put("v", 2)
                o.put("uid", uid)
                o.put("stages", JSONArray(stages))
                o.put("libraryFolders", JSONArray(libraryFolders.map { f ->
                    JSONObject().put("id", f.id).put("name", f.name)
                }))
                o.put("projects", JSONArray(projects.map { p ->
                    JSONObject().put("id", p.id).put("name", p.name).put("type", p.type)
                        .put("s", p.s).put("fav", p.fav)
                        .put("folders", JSONArray(p.folders.map { f ->
                            JSONObject().put("id", f.id).put("name", f.name)
                        }))
                }))
                o.put("tasks", JSONArray(tasks.map { t ->
                    JSONObject().put("id", t.id).put("t", t.t).put("status", t.status)
                        .put("dueDay", if (t.dueDay == null) JSONObject.NULL else t.dueDay)
                        .put("priority", t.priority)
                        .put("pid", if (t.pid == null) JSONObject.NULL else t.pid)
                        .put("tags", JSONArray(t.tags))
                        .put("subs", JSONArray(t.subs.map { s -> JSONObject().put("t", s.t).put("d", s.d) }))
                }))
                o.put("notes", JSONArray(notes.map { n ->
                    JSONObject().put("id", n.id).put("pid", if (n.pid == null) JSONObject.NULL else n.pid)
                        .put("title", n.title).put("body", n.body)
                        .put("icon", n.icon).put("parentId", if (n.parentId == null) JSONObject.NULL else n.parentId)
                }))
                o.put("files", JSONArray(files.map { f ->
                    JSONObject().put("id", f.id).put("name", f.name).put("size", f.size)
                        .put("kind", f.kind).put("pid", if (f.pid == null) JSONObject.NULL else f.pid)
                        .put("folderId", if (f.folderId == null) JSONObject.NULL else f.folderId)
                        .put("uri", f.uri.toString()).put("fav", f.fav)
                }))
                val ev = JSONObject()
                events.forEach { (k, v) -> ev.put(k, JSONArray(v)) }
                o.put("events", ev)
                o.put("brandColors", JSONArray(brandColors.map { it.value.toLong() }))
                o.put("brandFont", brandFont)
                o.put("dark", if (dark == null) JSONObject.NULL else dark)
                o.put("lock", lock)
                o.put("linkOnly", linkOnly)
                dataFile.writeText(o.toString())
            }
        }
    }

    private fun load() {
        runCatching {
            val f = dataFile
            if (!f.exists()) return
            val o = JSONObject(f.readText())
            uid = o.optInt("uid", 1)
            val v = o.optInt("v", 1)
            o.optJSONArray("projects")?.let { a ->
                for (i in 0 until a.length()) { val p = a.getJSONObject(i)
                    val pr = Proj(p.getInt("id"), p.getString("name"), p.getString("type"))
                    pr.s = p.optInt("s", 0); pr.fav = p.optBoolean("fav", false)
                    p.optJSONArray("folders")?.let { fa ->
                        for (j in 0 until fa.length()) { val fj = fa.getJSONObject(j)
                            pr.folders.add(FolderItem(fj.getInt("id"), fj.optString("name", "Folder"))) }
                    }
                    projects.add(pr)
                }
            }
            o.optJSONArray("stages")?.let { a ->
                for (i in 0 until a.length()) stages.add(a.getString(i))
            }
            if (stages.isEmpty()) stages.addAll(listOf("Idea", "Script", "Shoot", "Edit", "Thumb", "Live"))
            o.optJSONArray("libraryFolders")?.let { a ->
                for (i in 0 until a.length()) { val fj = a.getJSONObject(i)
                    libraryFolders.add(FolderItem(fj.getInt("id"), fj.optString("name", "Folder"))) }
            }
            o.optJSONArray("tasks")?.let { a ->
                for (i in 0 until a.length()) { val t = a.getJSONObject(i)
                    val tk = Task(t.getInt("id"), t.getString("t"))
                    tk.status = t.optInt("status", if (t.optBoolean("d", false)) 2 else 0)
                    tk.dueDay = if (t.isNull("dueDay")) null else t.optLong("dueDay")
                    tk.priority = t.optInt("priority", 0)
                    tk.pid = if (t.isNull("pid")) null else t.optInt("pid")
                    t.optJSONArray("tags")?.let { ta ->
                        for (j in 0 until ta.length()) tk.tags.add(ta.getString(j)) }
                    t.optJSONArray("subs")?.let { sa ->
                        for (j in 0 until sa.length()) { val sj = sa.getJSONObject(j)
                            tk.subs.add(SubTask(sj.optString("t", ""), sj.optBoolean("d", false))) } }
                    tasks.add(tk)
                }
            }
            o.optJSONArray("notes")?.let { a ->
                for (i in 0 until a.length()) { val n = a.getJSONObject(i)
                    val nt = Note(n.getInt("id"), if (n.isNull("pid")) null else n.getInt("pid"))
                    nt.title = n.optString("title", ""); nt.body = n.optString("body", "")
                    nt.icon = n.optString("icon", "📄")
                    nt.parentId = if (n.isNull("parentId")) null else n.optInt("parentId")
                    notes.add(nt)
                }
            }
            val oldFo = mutableMapOf<Int, Int>()
            o.optJSONArray("files")?.let { a ->
                for (i in 0 until a.length()) { val fi = a.getJSONObject(i)
                    val fid = fi.getInt("id")
                    if (v < 2 && fi.has("fo") && !fi.isNull("fo")) oldFo[fid] = fi.optInt("fo", 0)
                    files.add(FileItem(fid, fi.getString("name"), fi.getLong("size"), fi.getString("kind"),
                        if (fi.isNull("pid")) null else fi.getInt("pid"),
                        if (fi.isNull("folderId")) null else fi.getInt("folderId"),
                        Uri.parse(fi.getString("uri"))).also { it.fav = fi.optBoolean("fav", false) }) }
            }
            if (v < 2 && oldFo.isNotEmpty()) migrateOldFolders(oldFo)
            o.optJSONObject("events")?.let { ev ->
                ev.keys().forEach { k ->
                    val a = ev.getJSONArray(k)
                    events[k] = List(a.length()) { i -> a.getString(i) }
                }
            }
            o.optJSONArray("brandColors")?.let { a ->
                for (i in 0 until a.length()) brandColors.add(Color(a.getLong(i).toULong()))
            }
            brandFont = o.optString("brandFont", "")
            dark = if (o.isNull("dark")) null else o.optBoolean("dark", false)
            lock = o.optBoolean("lock", false)
            linkOnly = o.optBoolean("linkOnly", false)
            clampStages()
            // keep uid ahead of every restored id
            val maxId = listOf(
                projects.maxOfOrNull { it.id }, tasks.maxOfOrNull { it.id },
                notes.maxOfOrNull { it.id }, files.maxOfOrNull { it.id },
                projects.flatMap { it.folders }.maxOfOrNull { it.id },
                libraryFolders.maxOfOrNull { it.id }
            ).filterNotNull().maxOrNull() ?: 0
            if (uid <= maxId) uid = maxId + 1
        }
    }

    /** v1 -> v2: old files stored a fixed-taxonomy index ("fo"); create matching user folders and remap. */
    private fun migrateOldFolders(oldFo: Map<Int, Int>) {
        val old = listOf("Scripts", "Footage", "Edits", "Thumbnails", "Notes", "Published")
        val cache = mutableMapOf<Pair<Int?, Int>, Int>()
        files.forEach { fl ->
            val oi = oldFo[fl.id] ?: return@forEach
            val folderId = cache.getOrPut(fl.pid to oi) {
                val nf = FolderItem(uid++, old.getOrElse(oi) { "Folder ${oi + 1}" })
                (if (fl.pid == null) libraryFolders else proj(fl.pid)?.folders ?: libraryFolders).add(nf)
                nf.id
            }
            fl.folderId = folderId
        }
    }

    fun go(s: Scr) { stack.add(s) }
    fun back() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    fun root(s: Scr) { stack.clear(); stack.add(s) }

    fun proj(id: Int) = projects.firstOrNull { it.id == id }
    fun foldersOf(pid: Int?): List<FolderItem> = if (pid == null) libraryFolders else proj(pid)?.folders ?: emptyList()
    fun addFolder(pid: Int?, name: String): FolderItem {
        val f = FolderItem(uid++, name)
        (if (pid == null) libraryFolders else proj(pid)?.folders ?: libraryFolders).add(f)
        return f
    }
    fun delFolder(pid: Int?, folderId: Int) {
        val list = if (pid == null) libraryFolders else proj(pid)?.folders ?: return
        list.removeAll { it.id == folderId }
        files.forEach { if (it.folderId == folderId && it.pid == pid) it.folderId = null }
    }

    fun clampStages() {
        val m = (stages.size - 1).coerceAtLeast(0)
        projects.forEach { it.s = it.s.coerceIn(0, m) }
    }

    fun addProject(n: String, type: String, template: String = "Blank") {
        val p = Proj(uid++, n, type)
        when (template) {
            "Video Project" -> listOf("Script", "Footage", "Thumbnails")
            "Podcast" -> listOf("Audio", "Notes")
            else -> emptyList()
        }.forEach { p.folders.add(FolderItem(uid++, it)) }
        projects.add(p); sheet = null
    }
    fun delProject(id: Int) { projects.removeAll { it.id == id }; back() }
    fun addTask(t: String, status: Int = 0): Int {
        val k = Task(uid++, t); k.status = status; tasks.add(k); sheet = null; return k.id
    }
    fun newNote(pid: Int? = null, template: String = "Blank") {
        val n = Note(uid++, pid ?: projects.lastOrNull()?.id)
        when (template) {
            "Video Script" -> { n.title = "New video script"; n.body = "Hook\n\n\nIntro\n\n\nMain\n\n\nCTA\n" }
            "Ideas" -> { n.title = "Ideas"; n.body = "• \n" }
            "Meeting" -> { n.title = "Meeting notes"; n.body = "Attendees:\n\nNotes:\n\nActions:\n" }
        }
        notes.add(n); sheet = null; go(Scr.Editor(n.id))
    }
    fun addSubPage(n: Note) {
        val c = Note(uid++, n.pid); c.parentId = n.id; notes.add(c); go(Scr.Editor(c.id))
    }
    fun pick(pid: Int?, folderId: Int?) { sheet = null; launcher?.invoke(pid, folderId) }

    fun addFile(ctx: Context, uri: Uri, pid: Int?, folderId: Int?) {
        var name = "file"; var size = 0L
        ctx.contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                it.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { i -> i >= 0 }?.let { i -> name = it.getString(i) }
                it.getColumnIndex(OpenableColumns.SIZE).takeIf { i -> i >= 0 }?.let { i -> size = it.getLong(i) }
            }
        }
        val mime = ctx.contentResolver.getType(uri) ?: ""
        val kind = when { mime.startsWith("video") -> "Video"; mime.startsWith("image") -> "Image"; mime.startsWith("audio") -> "Audio"; else -> "Docs" }
        val id = uid++
        viewModelScope.launch {
            val final = withContext(Dispatchers.IO) {
                if (linkOnly) {
                    runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                    uri
                } else {
                    val out = File(ctx.filesDir, "${id}_$name")
                    ctx.contentResolver.openInputStream(uri)?.use { i -> out.outputStream().use { o -> i.copyTo(o) } }
                    Uri.fromFile(out)
                }
            }
            files.add(FileItem(id, name, size, kind, pid, folderId, final))
        }
    }
}
