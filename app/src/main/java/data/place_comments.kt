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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Comentario de un lugar del Home, guardado en Firestore en places/{lugar}/comments/{id}:
 * uid, authorName ("María L."), authorPhoto (miniatura de la foto de perfil, base64), text,
 * photo (foto opcional de la galería, base64 JPEG) y createdAt (hora del servidor).
 */
data class PlaceComment(
    val id: String,
    val uid: String,
    val authorName: String,
    val authorPhotoBase64: String?,
    val text: String,
    val photoBase64: String?,
    val createdAt: Long
)

/** Largo máximo de un comentario. */
const val COMMENT_MAX_LENGTH = 500

/** Lado mayor en píxeles de la foto adjunta (JPEG). */
private const val COMMENT_PHOTO_SIZE = 1080

/** Lado de la miniatura de la foto de perfil que se guarda con cada comentario. */
private const val AUTHOR_PHOTO_SIZE = 96

private fun commentsOf(placeKey: String) =
    FirebaseFirestore.getInstance().collection("places").document(placeKey).collection("comments")

private fun DocumentSnapshot.toComment(): PlaceComment? {
    val text = getString("text") ?: return null
    return PlaceComment(
        id = id,
        uid = getString("uid").orEmpty(),
        authorName = getString("authorName").orEmpty(),
        authorPhotoBase64 = getString("authorPhoto"),
        text = text,
        photoBase64 = getString("photo"),
        // Mientras el servidor no confirma la hora, se usa la estimada (comentario recién enviado)
        createdAt = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: 0L
    )
}

/** Comentarios del lugar [placeKey], del más antiguo al más reciente, actualizados en vivo. */
@Composable
fun rememberPlaceComments(placeKey: String): List<PlaceComment> {
    var comments by remember(placeKey) { mutableStateOf(emptyList<PlaceComment>()) }
    DisposableEffect(placeKey) {
        val registration = commentsOf(placeKey)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PlaceComments", "Error leyendo comentarios de $placeKey", error)
                    return@addSnapshotListener
                }
                comments = snapshot?.documents?.mapNotNull { it.toComment() }.orEmpty()
            }
        onDispose { registration.remove() }
    }
    return comments
}

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
