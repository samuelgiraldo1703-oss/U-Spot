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
import androidx.compose.ui.graphics.asImageBitmap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.min

/**
 * Datos del usuario con sesión iniciada que muestra el módulo de perfil.
 * [name] es el "Nombre De Usuario" del registro (campo "username" de users/{uid}).
 * [hasPassword] es falso para cuentas que solo entran con Google.
 * [passwordUpdatedAt] viene del campo "passwordUpdatedAt"; si no existe se usa la fecha de
 * creación de la cuenta (la contraseña no se ha cambiado desde entonces).
 * [bio] y [photo] los edita el propio usuario desde su perfil (campos "bio" y "photoBase64").
 */
data class UserProfile(
    val name: String,
    val email: String,
    val hasPassword: Boolean,
    val passwordUpdatedAt: Long?,
    val bio: String = "",
    val photo: ImageBitmap? = null
) {
    companion object {
        /** Datos de ejemplo para las vistas previas de Android Studio. */
        val Preview = UserProfile("Usuario", "usuario@gmail.com", hasPassword = true, passwordUpdatedAt = null)
    }
}

/** Largo máximo de la descripción: una frase corta que quepa en las dos líneas del perfil. */
const val BIO_MAX_LENGTH = 70

/** Lado en píxeles de la foto de perfil guardada (cuadrada, JPEG). */
private const val PHOTO_SIZE = 512

private fun fromAuth(user: FirebaseUser?): UserProfile {
    val email = user?.email.orEmpty()
    // Sin documento en Firestore: nombre de Google o la parte del correo antes de la @ ("uwu" -> "Uwu")
    val name = user?.displayName?.takeIf { it.isNotBlank() }
        ?: email.substringBefore("@").replaceFirstChar { it.uppercaseChar() }
    return UserProfile(
        name = name,
        email = email,
        hasPassword = user?.providerData?.any { it.providerId == "password" } ?: false,
        passwordUpdatedAt = user?.metadata?.creationTimestamp
    )
}

private fun decodePhoto(base64: String?): ImageBitmap? {
    if (base64.isNullOrBlank()) return null
    return try {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (e: IllegalArgumentException) {
        Log.e("UserProfile", "Foto de perfil con formato inválido", e)
        null
    }
}

/**
 * Perfil del usuario actual. Se actualiza solo cuando cambia la sesión (otro usuario inicia
 * sesión) o cuando cambia su documento users/{uid} en Firestore.
 */
@Composable
fun rememberUserProfile(): UserProfile {
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    var profile by remember { mutableStateOf(fromAuth(auth.currentUser)) }

    DisposableEffect(Unit) {
        var docListener: ListenerRegistration? = null
        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            docListener?.remove()
            val user = firebaseAuth.currentUser
            val base = fromAuth(user)
            profile = base
            if (user != null) {
                docListener = firestore.collection("users").document(user.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("UserProfile", "Error leyendo el perfil en Firestore", error)
                            return@addSnapshotListener
                        }
                        if (snapshot == null || !snapshot.exists()) return@addSnapshotListener
                        profile = base.copy(
                            // El nombre tal como lo escribió al registrarse
                            name = snapshot.getString("username")?.trim()?.takeIf { it.isNotEmpty() } ?: base.name,
                            email = snapshot.getString("email")?.takeIf { it.isNotBlank() } ?: base.email,
                            passwordUpdatedAt = snapshot.getLong("passwordUpdatedAt") ?: base.passwordUpdatedAt,
                            bio = snapshot.getString("bio").orEmpty(),
                            photo = decodePhoto(snapshot.getString("photoBase64"))
                        )
                    }
            }
        }
        auth.addAuthStateListener(authListener)
        onDispose {
            auth.removeAuthStateListener(authListener)
            docListener?.remove()
        }
    }
    return profile
}

/** Escribe campos en el documento del usuario con sesión (las reglas solo permiten el propio). */
private suspend fun updateCurrentUser(fields: Map<String, Any>): Boolean {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
    return try {
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .set(fields, SetOptions.merge()).await()
        true
    } catch (e: Exception) {
        Log.e("UserProfile", "Error guardando el perfil", e)
        false
    }
}

/** Guarda la descripción (máximo [BIO_MAX_LENGTH] caracteres, sin saltos de línea). */
suspend fun saveBio(text: String): Boolean =
    updateCurrentUser(mapOf("bio" to text.replace('\n', ' ').trim().take(BIO_MAX_LENGTH)))

/**
 * Lee la foto tomada con la cámara, la recorta al centro en un cuadrado de [PHOTO_SIZE] px
 * (respetando la orientación de la cámara) y la guarda comprimida en el perfil.
 */
suspend fun saveProfilePhoto(context: Context, uri: Uri): Boolean {
    val base64 = withContext(Dispatchers.Default) {
        try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                // Reducir al decodificar: el lado menor queda en PHOTO_SIZE
                val scale = PHOTO_SIZE.toFloat() / min(info.size.width, info.size.height)
                if (scale < 1f) decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            val side = min(bitmap.width, bitmap.height)
            val square = Bitmap.createBitmap(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side)
            val out = ByteArrayOutputStream()
            square.compress(Bitmap.CompressFormat.JPEG, 80, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("UserProfile", "No se pudo leer la foto de la cámara", e)
            null
        }
    } ?: return false
    return updateCurrentUser(mapOf("photoBase64" to base64))
}

/** "Borrar imagen de perfil": quita la foto del documento del usuario. */
suspend fun deleteProfilePhoto(): Boolean = updateCurrentUser(mapOf("photoBase64" to FieldValue.delete()))

/** "hace 3 meses", "hace 2 días", "hoy"... para la fila de contraseña. */
fun timeAgo(millis: Long?, now: Long = System.currentTimeMillis()): String {
    if (millis == null || millis <= 0) return ""
    val days = (now - millis) / 86_400_000L
    return when {
        days < 1 -> "hoy"
        days == 1L -> "hace 1 día"
        days < 30 -> "hace $days días"
        days < 60 -> "hace 1 mes"
        days < 365 -> "hace ${days / 30} meses"
        days < 730 -> "hace 1 año"
        else -> "hace ${days / 365} años"
    }
}
