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
    data class Folder(val id: Int, val i: Int) : Scr()
    data class Editor(val id: Int) : Scr()
}

sealed class Sh {
    object Add : Sh(); object NewProj : Sh(); object NewTask : Sh()
    data class Import(val pid: Int?, val fo: Int) : Sh()
}

class Proj(val id: Int, val name: String, val type: String) { var s by mutableIntStateOf(0); var fav by mutableStateOf(false) }
class Task(val id: Int, val t: String) { var d by mutableStateOf(false) }
class FileItem(val id: Int, val name: String, val size: Long, val kind: String, val pid: Int?, val fo: Int, val uri: Uri) { var fav by mutableStateOf(false) }
class Note(val id: Int, val pid: Int?) { var title by mutableStateOf(""); var body by mutableStateOf("") }

class Store : ViewModel() {
    private var uid = 1
    val projects = mutableStateListOf<Proj>()
    val tasks = mutableStateListOf<Task>()
    val files = mutableStateListOf<FileItem>()
    val notes = mutableStateListOf<Note>()
    val events = mutableStateMapOf<String, List<String>>()
    val brandColors = mutableStateListOf<Color>()
    var brandFont by mutableStateOf("")
    var dark by mutableStateOf<Boolean?>(null)
    var lock by mutableStateOf(false)
    var signedIn by mutableStateOf(runCatching { FirebaseAuth.getInstance().currentUser != null }.getOrDefault(false))
    var authLoading by mutableStateOf(false)
    var linkOnly by mutableStateOf(false)
    val stack = mutableStateListOf<Scr>(Scr.Home)
    var sheet by mutableStateOf<Sh?>(null)
    var tele by mutableStateOf<Int?>(null)
    var launcher: ((Int?, Int) -> Unit)? = null

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
                o.put("uid", uid)
                o.put("projects", JSONArray(projects.map { p ->
                    JSONObject().put("id", p.id).put("name", p.name).put("type", p.type)
                        .put("s", p.s).put("fav", p.fav)
                }))
                o.put("tasks", JSONArray(tasks.map { t ->
                    JSONObject().put("id", t.id).put("t", t.t).put("d", t.d)
                }))
                o.put("notes", JSONArray(notes.map { n ->
                    JSONObject().put("id", n.id).put("pid", if (n.pid == null) JSONObject.NULL else n.pid)
                        .put("title", n.title).put("body", n.body)
                }))
                o.put("files", JSONArray(files.map { f ->
                    JSONObject().put("id", f.id).put("name", f.name).put("size", f.size)
                        .put("kind", f.kind).put("pid", if (f.pid == null) JSONObject.NULL else f.pid)
                        .put("fo", f.fo).put("uri", f.uri.toString()).put("fav", f.fav)
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
            o.optJSONArray("projects")?.let { a ->
                for (i in 0 until a.length()) { val p = a.getJSONObject(i)
                    projects.add(Proj(p.getInt("id"), p.getString("name"), p.getString("type")).also {
                        it.s = p.optInt("s", 0); it.fav = p.optBoolean("fav", false)
                    }) }
            }
            o.optJSONArray("tasks")?.let { a ->
                for (i in 0 until a.length()) { val t = a.getJSONObject(i)
                    tasks.add(Task(t.getInt("id"), t.getString("t")).also { it.d = t.optBoolean("d", false) }) }
            }
            o.optJSONArray("notes")?.let { a ->
                for (i in 0 until a.length()) { val n = a.getJSONObject(i)
                    notes.add(Note(n.getInt("id"), if (n.isNull("pid")) null else n.getInt("pid")).also {
                        it.title = n.optString("title", ""); it.body = n.optString("body", "")
                    }) }
            }
            o.optJSONArray("files")?.let { a ->
                for (i in 0 until a.length()) { val fi = a.getJSONObject(i)
                    files.add(FileItem(fi.getInt("id"), fi.getString("name"), fi.getLong("size"), fi.getString("kind"),
                        if (fi.isNull("pid")) null else fi.getInt("pid"), fi.optInt("fo", 0),
                        Uri.parse(fi.getString("uri"))).also { it.fav = fi.optBoolean("fav", false) }) }
            }
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
            // keep uid ahead of every restored id
            val maxId = listOf(
                projects.maxOfOrNull { it.id }, tasks.maxOfOrNull { it.id },
                notes.maxOfOrNull { it.id }, files.maxOfOrNull { it.id }
            ).filterNotNull().maxOrNull() ?: 0
            if (uid <= maxId) uid = maxId + 1
        }
    }
    fun go(s: Scr) { stack.add(s) }
    fun back() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    fun root(s: Scr) { stack.clear(); stack.add(s) }

    fun proj(id: Int) = projects.firstOrNull { it.id == id }
    fun addProject(n: String, type: String) { projects.add(Proj(uid++, n, type)); sheet = null }
    fun delProject(id: Int) { projects.removeAll { it.id == id }; back() }
    fun addTask(t: String) { tasks.add(Task(uid++, t)); sheet = null }
    fun newNote(pid: Int? = projects.lastOrNull()?.id) { val n = Note(uid++, pid); notes.add(n); sheet = null; go(Scr.Editor(n.id)) }
    fun pick(pid: Int?, fo: Int) { sheet = null; launcher?.invoke(pid, fo) }

    fun addFile(ctx: Context, uri: Uri, pid: Int?, fo: Int) {
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
            files.add(FileItem(id, name, size, kind, pid, fo, final))
        }
    }
}
