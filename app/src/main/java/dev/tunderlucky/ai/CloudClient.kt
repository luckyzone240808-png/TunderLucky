package dev.tunderlucky.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class ChatMsg(val role: String, val text: String)

/** Provider-independent client for OpenAI-compatible /chat/completions endpoints (HTTPS only). */
object CloudClient {
    data class Msg(val role: String, val content: String)

    suspend fun chat(
        endpoint: String, apiKey: String, model: String, system: String,
        history: List<Msg>, onPartial: (String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(endpoint.startsWith("https://")) { "Endpoint must start with https://" }
            require(model.isNotBlank()) { "Model name is empty" }
            val arr = JSONArray()
            arr.put(JSONObject().put("role", "system").put("content", system))
            history.forEach { arr.put(JSONObject().put("role", it.role).put("content", it.content)) }
            val body = JSONObject().put("model", model).put("messages", arr).put("stream", true).toString()

            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; connectTimeout = 15000; readTimeout = 90000; doOutput = true
                setRequestProperty("Content-Type", "application/json")
                if (apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer $apiKey")
            }
            try {
                conn.outputStream.use { it.write(body.toByteArray()) }
                val code = conn.responseCode
                if (code !in 200..299) {
                    val err = conn.errorStream?.bufferedReader()?.readText()?.take(300) ?: ""
                    throw IOException("HTTP $code ${if (code == 401 || code == 403) "(check API key) " else ""}$err")
                }
                val sb = StringBuilder()
                if ((conn.contentType ?: "").contains("event-stream")) {
                    conn.inputStream.bufferedReader().useLines { lines ->
                        for (l in lines) {
                            if (!l.startsWith("data:")) continue
                            val d = l.removePrefix("data:").trim()
                            if (d == "[DONE]") break
                            val t = runCatching {
                                JSONObject(d).getJSONArray("choices").getJSONObject(0)
                                    .optJSONObject("delta")?.optString("content", "")
                            }.getOrNull().orEmpty()
                            if (t.isNotEmpty()) { sb.append(t); onPartial(sb.toString()) }
                        }
                    }
                } else {
                    val txt = conn.inputStream.bufferedReader().readText()
                    val m = JSONObject(txt).getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").getString("content")
                    sb.append(m); onPartial(m)
                }
                if (sb.isEmpty()) throw IOException("Empty response from provider")
                sb.toString()
            } finally { conn.disconnect() }
        }
    }
}
