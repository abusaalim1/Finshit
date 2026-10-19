package com.finshit.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal OpenRouter chat client (no new dependencies).
 * The API key is injected at build time via BuildConfig.OPENROUTER_KEY (from the
 * OPENROUTER_API_KEY env var in CI, which reads the GitHub Actions secret). The key
 * is never stored in the repo.
 */
object AiClient {
    const val MODEL = "nvidia/nemotron-3.5-super-vl"

    private val KEY get() = try { BuildConfig.OPENROUTER_KEY } catch (e: Throwable) { "" }

    suspend fun chat(messages: List<Pair<String, String>>): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val key = KEY
            if (key.isBlank()) throw IllegalStateException("AI key missing in this build")
            val url = URL("https://openrouter.ai/api/v1/chat/completions")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 30000
                readTimeout = 60000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $key")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("HTTP-Referer", "https://github.com/abusaalim1/Finshit")
                setRequestProperty("X-Title", "Finshit")
            }
            val body = JSONObject().apply {
                put("model", MODEL)
                put("messages", JSONArray(messages.map { (role, content) ->
                    JSONObject().put("role", role).put("content", content)
                }))
            }.toString()
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val text = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                val err = runCatching { conn.errorStream?.bufferedReader()?.use { it.readText() } }.getOrNull() ?: ""
                throw IllegalStateException("AI request failed (HTTP $code)${if (err.isNotBlank()) ": ${err.take(200)}" else ""}")
            }
            val content = runCatching {
                JSONObject(text).getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").getString("content")
            }.getOrNull()?.trim()
            if (content.isNullOrBlank()) throw IllegalStateException("Empty AI reply")
            content
        }
    }

    fun friendlyError(e: Throwable): String = when {
        e.message?.contains("AI key missing") == true -> "AI key missing in this build — reinstall the latest release."
        e.message?.contains("HTTP 401") == true -> "AI service refused the key (401). The key may be invalid or revoked."
        e.message?.contains("HTTP 402") == true -> "AI credits exhausted (402). Top up the OpenRouter account."
        e.message?.contains("HTTP 429") == true -> "AI is rate-limited right now (429). Try again in a minute."
        else -> "AI error: ${e.message?.take(160) ?: "unknown"}"
    }
}
