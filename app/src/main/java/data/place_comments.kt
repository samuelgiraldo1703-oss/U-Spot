package data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * Comentario de un lugar del Home, guardado en Firestore en places/{lugar}/comments/{id}:
 * uid, authorName ("María L."), authorPhoto (miniatura de la foto de perfil, base64), text,
 * photo (foto opcional de la galería, base64 JPEG), createdAt (hora del servidor), likeCount y
 * replyCount (no existen en los comentarios antiguos: cuentan como 0).
 *
 * Las respuestas van en .../comments/{id}/replies/{id} con los mismos campos (sin foto) y [parentId]
 * apunta a su comentario. Cada "me gusta" es un documento likes/{uid} dentro del comentario o respuesta.
 */
data class PlaceComment(
    val id: String,
    val uid: String,
    val authorName: String,
    val authorPhotoBase64: String?,
    val text: String,
    val photoBase64: String?,
    val createdAt: Long,
    val likeCount: Int = 0,
    val replyCount: Int = 0,
    val parentId: String? = null
)

/** Largo máximo de un comentario. */
const val COMMENT_MAX_LENGTH = 500

/** Lado mayor en píxeles de la foto adjunta (JPEG). */
private const val COMMENT_PHOTO_SIZE = 1080

/** Lado de la miniatura de la foto de perfil que se guarda con cada comentario. */
private const val AUTHOR_PHOTO_SIZE = 96

private fun commentsOf(placeKey: String) =
    FirebaseFirestore.getInstance().collection("places").document(placeKey).collection("comments")

private fun DocumentSnapshot.toComment(parentId: String? = null): PlaceComment? {
    val text = getString("text") ?: return null
    return PlaceComment(
        id = id,
        uid = getString("uid").orEmpty(),
        authorName = getString("authorName").orEmpty(),
        authorPhotoBase64 = getString("authorPhoto"),
        text = text,
        photoBase64 = getString("photo"),
        // Mientras el servidor no confirma la hora, se usa la estimada (comentario recién enviado)
        createdAt = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: 0L,
        likeCount = (getLong("likeCount") ?: 0L).toInt().coerceAtLeast(0),
        replyCount = (getLong("replyCount") ?: 0L).toInt().coerceAtLeast(0),
        parentId = parentId
    )
}

private fun repliesOf(placeKey: String, commentId: String) =
    commentsOf(placeKey).document(commentId).collection("replies")

/** Documento de un comentario o, si tiene [PlaceComment.parentId], de una respuesta. */
private fun PlaceComment.reference(placeKey: String): DocumentReference =
    parentId?.let { repliesOf(placeKey, it).document(id) } ?: commentsOf(placeKey).document(id)

/** Una "página" de comentarios escuchada en vivo; [loaded] es falso hasta la primera respuesta. */
data class CommentsFeed(val items: List<PlaceComment>, val loaded: Boolean, val hasMore: Boolean)

/**
 * Los [limit] comentarios más recientes del lugar [placeKey], actualizados en vivo. Para ver más
 * basta con subir [limit]: mientras llega la nueva página se conserva la lista actual.
 */
@Composable
fun rememberPlaceComments(placeKey: String, limit: Int): CommentsFeed {
    var feed by remember(placeKey) { mutableStateOf(CommentsFeed(emptyList(), loaded = false, hasMore = false)) }
    DisposableEffect(placeKey, limit) {
        val registration = commentsOf(placeKey)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PlaceComments", "Error leyendo comentarios de $placeKey", error)
                    feed = feed.copy(loaded = true, hasMore = false)
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents.orEmpty()
                feed = CommentsFeed(docs.mapNotNull { it.toComment() }, loaded = true, hasMore = docs.size >= limit)
            }
        onDispose { registration.remove() }
    }
    return feed
}

/** Las primeras [limit] respuestas de un comentario, de la más antigua a la más reciente. */
@Composable
fun rememberCommentReplies(placeKey: String, commentId: String, limit: Int): List<PlaceComment> {
    var replies by remember(placeKey, commentId) { mutableStateOf(emptyList<PlaceComment>()) }
    DisposableEffect(placeKey, commentId, limit) {
        val registration = repliesOf(placeKey, commentId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PlaceComments", "Error leyendo respuestas de $commentId", error)
                    return@addSnapshotListener
                }
                replies = snapshot?.documents?.mapNotNull { it.toComment(parentId = commentId) }.orEmpty()
            }
        onDispose { registration.remove() }
    }
    return replies
}

/**
 * Total de comentarios del lugar (null hasta saberlo o sin conexión). El conteo de Firestore no
 * llega en vivo: se vuelve a pedir cada vez que cambia [refresh].
 */
@Composable
fun rememberCommentCount(placeKey: String, refresh: Any?): Int? {
    var count by remember(placeKey) { mutableStateOf<Int?>(null) }
    LaunchedEffect(placeKey, refresh) {
        try {
            count = commentsOf(placeKey).count().get(AggregateSource.SERVER).await().count.toInt()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("PlaceComments", "No se pudo contar los comentarios de $placeKey", e)
        }
    }
    return count
}

/** Si el usuario con sesión le dio "me gusta" a [comment], en vivo. */
@Composable
fun rememberIsLiked(placeKey: String, comment: PlaceComment): Boolean {
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid }
    var liked by remember(placeKey, comment.id) { mutableStateOf(false) }
    DisposableEffect(placeKey, comment.id, uid) {
        val registration = uid?.let {
            comment.reference(placeKey).collection("likes").document(it)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("PlaceComments", "Error leyendo el me gusta de ${comment.id}", error)
                        return@addSnapshotListener
                    }
                    liked = snapshot?.exists() == true
                }
        }
        onDispose { registration?.remove() }
    }
    return liked
}

/**
 * Pone ([like] verdadero) o quita el "me gusta" del usuario con sesión. El documento likes/{uid} y
 * el contador cambian en la misma transacción, así un doble toque no cuenta dos veces.
 */
suspend fun setCommentLiked(placeKey: String, comment: PlaceComment, like: Boolean): Boolean {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
    val target = comment.reference(placeKey)
    val likeDoc = target.collection("likes").document(uid)
    return try {
        FirebaseFirestore.getInstance().runTransaction { tx ->
            if (tx.get(likeDoc).exists() != like) {
                if (like) tx.set(likeDoc, mapOf("createdAt" to FieldValue.serverTimestamp()))
                else tx.delete(likeDoc)
                tx.update(target, "likeCount", FieldValue.increment(if (like) 1 else -1))
            }
        }.await()
        true
    } catch (e: Exception) {
        Log.e("PlaceComments", "Error guardando el me gusta de ${comment.id}", e)
        false
    }
}

/** Qué foto le corresponde a quien comentó, según su documento users/{uid}. */
sealed interface AuthorPicture {
    /** Aún no se sabe o no hay conexión: se usa la miniatura guardada en el comentario. */
    data object Unknown : AuthorPicture

    /** El perfil existe y tiene foto. */
    data class Profile(val base64: String) : AuthorPicture

    /** El perfil existe pero no tiene foto: se deja el icono genérico. */
    data object Empty : AuthorPicture

    /** El perfil no existe: se le asigna una imagen de relleno. */
    data object NoProfile : AuthorPicture
}

/** Foto del autor [uid] en vivo. Un "no existe" solo se cree si lo confirma el servidor, no la caché. */
@Composable
fun rememberAuthorPicture(uid: String): AuthorPicture {
    var picture by remember(uid) { mutableStateOf(if (uid.isEmpty()) AuthorPicture.NoProfile else AuthorPicture.Unknown) }
    DisposableEffect(uid) {
        val registration = uid.takeIf { it.isNotEmpty() }?.let {
            FirebaseFirestore.getInstance().collection("users").document(it)
                .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                    if (error != null || snapshot == null) {
                        Log.w("PlaceComments", "No se pudo leer el perfil de $uid", error)
                        return@addSnapshotListener
                    }
                    val photo = snapshot.getString("photoBase64")?.takeIf { p -> p.isNotBlank() }
                    when {
                        photo != null -> picture = AuthorPicture.Profile(photo)
                        snapshot.exists() -> picture = AuthorPicture.Empty
                        !snapshot.metadata.isFromCache -> picture = AuthorPicture.NoProfile
                    }
                }
        }
        onDispose { registration?.remove() }
    }
    return picture
}

/** Elige una de las [count] imágenes de relleno: siempre la misma para la misma persona. */
fun fillerAvatarIndex(comment: PlaceComment, count: Int): Int =
    Math.floorMod(comment.uid.ifEmpty { comment.authorName.ifEmpty { comment.id } }.hashCode(), count)

/** Datos públicos que muestra la tarjeta de perfil de quien comentó (nunca el correo). */
data class CommentAuthor(val name: String?, val bio: String, val commentCount: Int?)

/** Perfil público del usuario [uid]. Lo que no se pueda leer queda vacío: la tarjeta sigue mostrando el nombre del comentario. */
suspend fun loadCommentAuthor(uid: String): CommentAuthor {
    val firestore = FirebaseFirestore.getInstance()
    val doc = try {
        firestore.collection("users").document(uid).get().await()
    } catch (e: Exception) {
        Log.w("PlaceComments", "No se pudo leer el perfil de $uid", e)
        null
    }
    // Necesita el índice de grupo de colecciones de firestore.indexes.json; sin él no se muestra el total
    val commentCount = try {
        firestore.collectionGroup("comments").whereEqualTo("uid", uid)
            .count().get(AggregateSource.SERVER).await().count.toInt()
    } catch (e: Exception) {
        Log.w("PlaceComments", "No se pudo contar los comentarios de $uid", e)
        null
    }
    return CommentAuthor(
        name = doc?.getString("username")?.trim()?.takeIf { it.isNotEmpty() },
        bio = doc?.getString("bio").orEmpty(),
        commentCount = commentCount
    )
}

/** Hace cuánto se publicó: "ahora", "5 min", "2 h", "3 d", "2 sem" o la fecha si pasó más de un mes. */
fun commentAgeLabel(createdAt: Long, now: Long = System.currentTimeMillis()): String {
    if (createdAt <= 0L) return ""
    val minutes = (now - createdAt) / 60_000L
    return when {
        minutes < 1 -> "ahora"
        minutes < 60 -> "$minutes min"
        minutes < 60 * 24 -> "${minutes / 60} h"
        minutes < 60 * 24 * 7 -> "${minutes / (60 * 24)} d"
        minutes < 60 * 24 * 30 -> "${minutes / (60 * 24 * 7)} sem"
        else -> SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es-CO")).format(Date(createdAt))
    }
}

/** Texto del botón que abre el panel: deja claro que ahí se leen y también se escriben comentarios. */
fun commentsButtonLabel(hasComments: Boolean): String =
    if (hasComments) "Ver y escribir comentarios" else "Escribir un comentario"

/** "Juan Pablo Torres" -> "Juan T."; un solo nombre queda igual. */
fun shortAuthorName(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.isEmpty()) return "Usuario"
    if (parts.size == 1) return parts[0]
    return "${parts[0]} ${parts.last().first().uppercaseChar()}."
}

/** Decodifica una imagen guardada en base64 (null si no hay o está dañada). */
fun decodeBase64Image(base64: String?): ImageBitmap? {
    if (base64.isNullOrBlank()) return null
    return try {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (e: IllegalArgumentException) {
        null
    }
}

private fun Bitmap.toBase64Jpeg(quality: Int): String {
    val out = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, quality, out)
    return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}

/** Foto de la galería reducida a [COMMENT_PHOTO_SIZE] px de lado mayor, en base64. */
private fun readCommentPhoto(context: Context, uri: Uri): String? = try {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        val scale = COMMENT_PHOTO_SIZE.toFloat() / max(info.size.width, info.size.height)
        if (scale < 1f) decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
    bitmap.toBase64Jpeg(72)
} catch (e: Exception) {
    Log.w("PlaceComments", "ImageDecoder no pudo leer la foto, intentando con decodeStream", e)
    try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val bm = BitmapFactory.decodeStream(stream)
            bm?.let {
                val scale = COMMENT_PHOTO_SIZE.toFloat() / max(it.width, it.height)
                val scaled = if (scale < 1f) Bitmap.createScaledBitmap(it, (it.width * scale).toInt(), (it.height * scale).toInt(), true) else it
                scaled.toBase64Jpeg(72)
            }
        }
    } catch (e2: Exception) {
        Log.e("PlaceComments", "No se pudo leer la foto de la galería", e2)
        null
    }
}

/**
 * Guarda un comentario del usuario con sesión en el lugar [placeKey]. [photoUri] es la foto
 * elegida de la galería (opcional). Devuelve false si no hay sesión o falla Firestore.
 */
suspend fun addPlaceComment(
    context: Context,
    placeKey: String,
    text: String,
    photoUri: Uri?,
    author: UserProfile
): Boolean {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
    val clean = text.trim().take(COMMENT_MAX_LENGTH)
    if (clean.isEmpty() && photoUri == null) return false
    val (photo, authorPhoto) = withContext(Dispatchers.Default) {
        val photo = photoUri?.let { readCommentPhoto(context, it) }
        val authorPhoto = author.photo?.asAndroidBitmap()?.let {
            Bitmap.createScaledBitmap(it, AUTHOR_PHOTO_SIZE, AUTHOR_PHOTO_SIZE, true).toBase64Jpeg(80)
        }
        photo to authorPhoto
    }
    if (photoUri != null && photo == null) return false
    val fields = buildMap<String, Any> {
        put("uid", uid)
        put("authorName", shortAuthorName(author.name))
        put("text", clean)
        put("createdAt", FieldValue.serverTimestamp())
        if (photo != null) put("photo", photo)
        if (authorPhoto != null) put("authorPhoto", authorPhoto)
    }
    return try {
        commentsOf(placeKey).add(fields).await()
        true
    } catch (e: Exception) {
        Log.e("PlaceComments", "Error guardando el comentario", e)
        false
    }
}

/**
 * Elimina un comentario de Firestore en places/{placeKey}/comments/{commentId}.
 */
suspend fun deletePlaceComment(placeKey: String, commentId: String): Boolean {
    return try {
        commentsOf(placeKey).document(commentId).delete().await()
        true
    } catch (e: Exception) {
        Log.e("PlaceComments", "Error eliminando el comentario $commentId", e)
        false
    }
}

/**
 * Elimina únicamente la foto adjunta de un comentario, conservando el texto.
 */
suspend fun deletePlaceCommentPhoto(placeKey: String, commentId: String): Boolean {
    return try {
        commentsOf(placeKey).document(commentId).update("photo", FieldValue.delete()).await()
        true
    } catch (e: Exception) {
        Log.e("PlaceComments", "Error eliminando la foto del comentario $commentId", e)
        false
    }
}

/** Publica una respuesta al comentario [commentId] y suma 1 a su contador en la misma operación. */
suspend fun addCommentReply(placeKey: String, commentId: String, text: String, author: UserProfile): Boolean {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
    val clean = text.trim().take(COMMENT_MAX_LENGTH)
    if (clean.isEmpty()) return false
    val authorPhoto = withContext(Dispatchers.Default) {
        author.photo?.asAndroidBitmap()?.let {
            Bitmap.createScaledBitmap(it, AUTHOR_PHOTO_SIZE, AUTHOR_PHOTO_SIZE, true).toBase64Jpeg(80)
        }
    }
    val fields = buildMap<String, Any> {
        put("uid", uid)
        put("authorName", shortAuthorName(author.name))
        put("text", clean)
        put("createdAt", FieldValue.serverTimestamp())
        if (authorPhoto != null) put("authorPhoto", authorPhoto)
    }
    return try {
        FirebaseFirestore.getInstance().batch()
            .set(repliesOf(placeKey, commentId).document(), fields)
            .update(commentsOf(placeKey).document(commentId), "replyCount", FieldValue.increment(1))
            .commit().await()
        true
    } catch (e: Exception) {
        Log.e("PlaceComments", "Error guardando la respuesta a $commentId", e)
        false
    }
}

/** Elimina una respuesta propia y resta 1 al contador de su comentario. */
suspend fun deleteCommentReply(placeKey: String, reply: PlaceComment): Boolean {
    val parent = reply.parentId ?: return false
    return try {
        FirebaseFirestore.getInstance().batch()
            .delete(reply.reference(placeKey))
            .update(commentsOf(placeKey).document(parent), "replyCount", FieldValue.increment(-1))
            .commit().await()
        true
    } catch (e: Exception) {
        Log.e("PlaceComments", "Error eliminando la respuesta ${reply.id}", e)
        false
    }
}
