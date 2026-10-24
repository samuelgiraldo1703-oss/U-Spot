package data.spotty

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Historial de Spotty en Firestore (reglas en firestore.rules: solo el dueño lee y escribe):
 *
 *   users/{uid}/chats/{chatId}                 title, createdAt, updatedAt, lastMessage
 *   users/{uid}/chats/{chatId}/messages/{id}   role, text, time, analysis, cards, followUp, suggestions
 *
 * Firestore guarda una copia local, así que el historial se ve sin internet y las escrituras
 * hechas sin conexión se suben solas al volver.
 */
class SpottyChatRepository(private val uid: String) {
    private val db = FirebaseFirestore.getInstance()
    private val chats = db.collection("users").document(uid).collection("chats")

    companion object {
        private const val TAG = "SpottyChats"
        /** Repositorio del usuario con sesión; null si no hay sesión (Spotty no guarda nada). */
        fun forCurrentUser(): SpottyChatRepository? = FirebaseAuth.getInstance().currentUser?.uid?.let(::SpottyChatRepository)
    }

    fun newChatId(): String = chats.document().id

    /** Chats del más reciente al más antiguo (máximo 50). */
    fun observeChats(onError: () -> Unit, onChange: (List<SpottyChatSummary>) -> Unit): ListenerRegistration =
        chats.orderBy("updatedAt", Query.Direction.DESCENDING).limit(50).addSnapshotListener { snap, e ->
            if (e != null) { Log.w(TAG, "No se pudo leer el historial: ${e.message}"); onError(); return@addSnapshotListener }
            onChange(snap?.documents?.mapNotNull { d ->
                SpottyChatSummary(d.id, d.getString("title") ?: return@mapNotNull null, d.getLong("updatedAt") ?: 0L)
            }.orEmpty())
        }

    fun observeMessages(chatId: String, onChange: (List<SpottyMessage>) -> Unit): ListenerRegistration =
        chats.document(chatId).collection("messages").orderBy("time").addSnapshotListener { snap, e ->
            if (e != null) { Log.w(TAG, "No se pudieron leer los mensajes: ${e.message}"); return@addSnapshotListener }
            onChange(snap?.documents?.mapNotNull { it.toMessage() }.orEmpty())
        }

    /** Guarda un mensaje y actualiza el chat (lo crea con [title] si es el primero). */
    fun add(chatId: String, msg: SpottyMessage, title: String? = null) {
        val chatRef = chats.document(chatId)
        val chat = mutableMapOf<String, Any>(
            "updatedAt" to msg.time,
            "lastMessage" to msg.text.take(120)
        )
        if (title != null) { chat["title"] = title; chat["createdAt"] = msg.time }
        db.batch()
            .set(chatRef.collection("messages").document(msg.id), msg.toMap())
            .set(chatRef, chat, com.google.firebase.firestore.SetOptions.merge())
            .commit()
            .addOnFailureListener { Log.w(TAG, "No se pudo guardar el mensaje: ${it.message}") }
    }

    /** Borra el chat con todos sus mensajes. */
    suspend fun delete(chatId: String) {
        val chatRef = chats.document(chatId)
        runCatching {
            val messages = chatRef.collection("messages").get().await().documents
            messages.chunked(400).forEach { part ->
                db.batch().apply { part.forEach { delete(it.reference) } }.commit().await()
            }
            chatRef.delete().await()
        }.onFailure { Log.w(TAG, "No se pudo borrar el chat: ${it.message}") }
    }

    private fun SpottyMessage.toMap(): Map<String, Any?> = mapOf(
        "role" to role.name,
        "text" to text,
        "time" to time,
        "analysis" to analysis,
        "followUp" to followUp,
        "suggestions" to suggestions,
        "cards" to cards.map {
            mapOf("placeId" to it.placeId, "match" to it.match, "distance" to it.distance,
                "priceLabel" to it.priceLabel, "priceValue" to it.priceValue)
        }
    )

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toMessage(): SpottyMessage? {
        val role = runCatching { SpottyRole.valueOf(getString("role") ?: return null) }.getOrNull() ?: return null
        val cards = (get("cards") as? List<Map<String, Any?>>).orEmpty().mapNotNull { c ->
            SpottyCard(
                placeId = c["placeId"] as? String ?: return@mapNotNull null,
                match = (c["match"] as? Number)?.toInt() ?: 0,
                distance = c["distance"] as? String,
                priceLabel = c["priceLabel"] as? String ?: "Por persona",
                priceValue = c["priceValue"] as? String ?: ""
            )
        }
        return SpottyMessage(
            id = id,
            role = role,
            text = getString("text").orEmpty(),
            time = getLong("time") ?: 0L,
            analysis = getString("analysis"),
            cards = cards,
            followUp = getString("followUp"),
            suggestions = (get("suggestions") as? List<String>).orEmpty()
        )
    }
}
