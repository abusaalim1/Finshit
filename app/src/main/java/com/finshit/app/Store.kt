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
