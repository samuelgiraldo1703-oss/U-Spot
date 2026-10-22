package data.spotty

import android.util.Log
import com.example.u_spot.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Error de Gemini traducido a lo que la pantalla sabe mostrar. */
class SpottyException(val problem: SpottyProblem, message: String) : Exception(message)

/**
 * Llamadas REST a la Gemini Developer API (generateContent) con la clave de AI Studio guardada en
 * local.properties → BuildConfig.GEMINI_API_KEY. El proyecto de la clave no tiene facturación:
 * cuando se acaba la cuota gratuita la API responde 429 y nunca cobra.
 */
object GeminiClient {
    private const val TAG = "GeminiClient"

    /** Flash-Lite más reciente y estable disponible para la clave (verificado el 22-oct-2026). */
    const val MODEL = "gemini-3.5-flash-lite"
    const val MODEL_LABEL = "Gemini 3.5 Flash-Lite"

    /** Si Google retira el modelo fijo, se usa el alias que siempre apunta al Flash-Lite vigente. */
    private const val FALLBACK_MODEL = "gemini-flash-lite-latest"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    val hasKey: Boolean get() = BuildConfig.GEMINI_API_KEY.isNotBlank()

    /**
     * Una llamada a generateContent. [contents] es la conversación en formato de Gemini (las
     * respuestas del modelo se reenvían tal cual, con su thoughtSignature, como pide la API).
     */
    suspend fun generate(
        system: String,
        contents: JSONArray,
        tools: JSONArray,
        forceFunction: String? = null
    ): JSONObject {
        if (!hasKey) throw SpottyException(SpottyProblem.NO_KEY, "Falta GEMINI_API_KEY en local.properties")
        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            .put("contents", contents)
            .put("tools", tools)
            .put("generationConfig", JSONObject().put("temperature", 0.2))
        if (forceFunction != null) {
            body.put("toolConfig", JSONObject().put("functionCallingConfig", JSONObject()
                .put("mode", "ANY")
                .put("allowedFunctionNames", JSONArray().put(forceFunction))))
        }
        return withContext(Dispatchers.IO) {
            try {
                post(MODEL, body)
            } catch (e: ModelGone) {
                Log.w(TAG, "$MODEL no disponible, se usa $FALLBACK_MODEL")
                post(FALLBACK_MODEL, body)
            }
        }
    }

    private class ModelGone : Exception()

    private suspend fun post(model: String, body: JSONObject, attempt: Int = 0): JSONObject {
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
            .header("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
            .post(body.toString().toRequestBody(JSON))
            .build()
        val (code, text) = try {
            http.newCall(request).execute().use { it.code to it.body?.string().orEmpty() }
        } catch (e: IOException) {
            throw SpottyException(SpottyProblem.OFFLINE, e.message ?: "Sin conexión")
        }
        if (code == 200) return JSONObject(text)
        Log.w(TAG, "Gemini $code: ${text.take(300)}")
        return when {
            code == 429 -> throw SpottyException(SpottyProblem.QUOTA, "Cuota gratuita agotada")
            code == 404 && model == MODEL -> throw ModelGone()
            code == 400 && "API_KEY" in text || code == 401 || code == 403 ->
                throw SpottyException(SpottyProblem.NO_KEY, "Clave de Gemini inválida o restringida")
            code >= 500 && attempt < 2 -> {
                delay(1200L * (attempt + 1))
                post(model, body, attempt + 1)
            }
            else -> throw SpottyException(SpottyProblem.GENERIC, "Gemini respondió $code")
        }
    }
}
