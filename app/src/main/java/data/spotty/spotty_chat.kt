package data.spotty

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.firestore.ListenerRegistration
import data.PlaceStore
import data.isOnline
import java.util.UUID

/**
 * Gestor del estado de conversación y del historial de chats con Spotty.
 *
 * Mantiene la lista de mensajes en memoria y sincroniza con Firestore
 * (users/{uid}/chats y users/{uid}/chats/{chatId}/messages).
 *
 * Se conserva durante la sesión para que cuando el usuario entra a ver el detalle de un lugar
 * y presiona el botón de atrás (o navega a otra pestaña y regresa), la conversación actual
 * no se pierda y pueda seguir explorando el resto de propuestas sugeridas por Spotty.
 */
class SpottyChat(
    private val engine: SpottyEngine,
    repo: SpottyChatRepository?,
    private val isOnline: () -> Boolean,
    val gps: SpottyGps
) {
    /** Se vuelve null si Firestore rechaza el acceso: desde ahí el historial vive en memoria. */
    var repo: SpottyChatRepository? = repo
        private set
    val chats = mutableStateListOf<SpottyChatSummary>()
    val messages = mutableStateListOf<SpottyMessage>()
    var currentChatId by mutableStateOf<String?>(null)
    var typing by mutableStateOf(false)
    var problem by mutableStateOf<SpottyProblem?>(null)
    var startedAt by mutableStateOf(System.currentTimeMillis())

    private var chatsListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null

    /** Mensajes de cada chat en memoria (para navegación instantánea sin parpadeo). */
    private val memory = mutableMapOf<String, List<SpottyMessage>>()

    fun start() {
        val cloud = repo ?: SpottyChatRepository.forCurrentUser().also { repo = it }
        chatsListener?.remove()
        chatsListener = cloud?.observeChats(onError = { goLocal() }) { list ->
            // Mantiene chats locales en caso de que aún no se hayan subido a Firestore
            val localOnly = chats.filter { c -> list.none { it.id == c.id } }
            chats.clear()
            chats.addAll(localOnly)
            chats.addAll(list)
            chats.sortByDescending { it.updatedAt }
        }
        currentChatId?.let { id ->
            if (cloud != null && messagesListener == null) {
                listen(id)
            }
        }
    }

    /** Firestore no disponible (p. ej. reglas sin publicar): se sigue en memoria sin perder lo que hay en pantalla. */
    private fun goLocal() {
        repo = null
        messagesListener?.remove()
        messagesListener = null
        currentChatId?.let { memory[it] = messages.toList() }
    }

    fun stop() {
        // Pausar observadores al salir de la pantalla pero conservar mensajes y chat activo
        chatsListener?.remove()
        chatsListener = null
        messagesListener?.remove()
        messagesListener = null
    }

    fun newChat() {
        messagesListener?.remove()
        messagesListener = null
        currentChatId = null
        messages.clear()
        problem = null
        typing = false
        startedAt = System.currentTimeMillis()
    }

    fun open(id: String) {
        messagesListener?.remove()
        messagesListener = null
        currentChatId = id
        messages.clear()
        problem = null
        typing = false
        startedAt = System.currentTimeMillis()

        // Carga inmediata desde memoria local para que la UI responda instantáneamente
        val cached = memory[id].orEmpty()
        if (cached.isNotEmpty()) {
            messages.addAll(cached)
            startedAt = cached.firstOrNull()?.time ?: startedAt
        }

        val cloud = repo ?: SpottyChatRepository.forCurrentUser().also { repo = it }
        if (cloud != null) {
            listen(id)
        }
    }

    private fun listen(id: String) {
        messagesListener?.remove()
        val cloud = repo ?: SpottyChatRepository.forCurrentUser().also { repo = it }
        messagesListener = cloud?.observeMessages(id) { list ->
            if (currentChatId != id) return@observeMessages
            if (list.isNotEmpty()) {
                messages.clear()
                messages.addAll(list)
                memory[id] = list
                list.firstOrNull()?.let { startedAt = it.time }
            }
        }
    }

    suspend fun delete(id: String) {
        if (currentChatId == id) newChat()
        chats.removeAll { it.id == id }
        memory.remove(id)
        val cloud = repo ?: SpottyChatRepository.forCurrentUser()
        cloud?.delete(id)
    }

    private fun save(chatId: String, msg: SpottyMessage, title: String? = null) {
        if (currentChatId == chatId && messages.none { it.id == msg.id }) {
            messages.add(msg)
        }
        memory[chatId] = memory[chatId].orEmpty() + msg
        val i = chats.indexOfFirst { it.id == chatId }
        if (i >= 0) {
            chats[i] = chats[i].copy(updatedAt = msg.time)
        } else {
            chats.add(0, SpottyChatSummary(chatId, title ?: msg.text.take(60), msg.time))
        }
        chats.sortByDescending { it.updatedAt }

        val cloud = repo ?: SpottyChatRepository.forCurrentUser().also { repo = it }
        cloud?.add(chatId, msg, title)
    }

    suspend fun send(text: String, userName: String) {
        val now = System.currentTimeMillis()
        var title: String? = null
        val cloud = repo ?: SpottyChatRepository.forCurrentUser().also { repo = it }
        val chatId = currentChatId ?: (cloud?.newChatId() ?: UUID.randomUUID().toString()).also { id ->
            // El título sale de la primera pregunta (sin gastar una llamada a Gemini)
            title = text.take(60)
            currentChatId = id
            startedAt = now
        }
        problem = null
        save(chatId, SpottyMessage(UUID.randomUUID().toString(), SpottyRole.USER, text, now), title)
        if (title != null && cloud != null) {
            listen(chatId)
        }
        answer(chatId, userName)
    }

    /** Vuelve a pedir la respuesta de la última pregunta (después de un error). */
    suspend fun retry(userName: String) {
        val chatId = currentChatId ?: return
        if (messages.lastOrNull()?.role != SpottyRole.USER) return
        problem = null
        answer(chatId, userName)
    }

    private suspend fun answer(chatId: String, userName: String) {
        if (!isOnline()) {
            problem = SpottyProblem.OFFLINE
            return
        }
        typing = true
        try {
            val reply = engine.reply(messages.toList(), userName)
            save(chatId, reply)
        } catch (e: SpottyException) {
            if (currentChatId == chatId) problem = e.problem
        } catch (e: Exception) {
            Log.e("Spotty", "Error inesperado", e)
            if (currentChatId == chatId) problem = SpottyProblem.GENERIC
        } finally {
            typing = false
        }
    }

    companion object {
        @Volatile
        private var instance: SpottyChat? = null

        /**
         * Retorna la instancia de chat compartida durante la sesión del usuario.
         */
        fun getInstance(context: Context): SpottyChat {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val appCtx = context.applicationContext
                    val gps = SpottyGps(appCtx)
                    val engine = SpottyEngine({ PlaceStore.all(appCtx) }, AndroidSpottyLocator(appCtx, gps::current))
                    val repo = SpottyChatRepository.forCurrentUser()
                    SpottyChat(engine, repo, { isOnline(appCtx) }, gps).also {
                        instance = it
                    }
                }
            }
        }

        /**
         * Limpia la instancia actual (p. ej. al cerrar sesión).
         */
        fun reset() {
            instance?.stop()
            instance = null
        }
    }
}
