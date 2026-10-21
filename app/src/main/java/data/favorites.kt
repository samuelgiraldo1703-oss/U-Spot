package data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

/**
 * Gestor de lugares favoritos del usuario, sincronizado en tiempo real con Firestore
 * en el documento users/{uid} (campo "favorites" como lista de IDs) y subcolección
 * users/{uid}/favorites/{placeId}.
 */
object FavoriteStore {
    private const val TAG = "FavoriteStore"
    private val memoryFavorites = mutableSetOf<String>()
    private val listeners = mutableSetOf<(Set<String>) -> Unit>()
    private var firestoreRegistration: ListenerRegistration? = null
    private var currentUid: String? = null
    private var authListener: FirebaseAuth.AuthStateListener? = null

    init {
        val auth = FirebaseAuth.getInstance()
        authListener = FirebaseAuth.AuthStateListener { fbAuth ->
            val uid = fbAuth.currentUser?.uid
            if (uid != currentUid) {
                currentUid = uid
                firestoreRegistration?.remove()
                firestoreRegistration = null
                memoryFavorites.clear()
                notifyListeners()
                if (uid != null) {
                    attachFirestoreListener(uid)
                }
            }
        }
        auth.addAuthStateListener(authListener!!)
        auth.currentUser?.uid?.let { attachFirestoreListener(it) }
    }

    private fun attachFirestoreListener(uid: String) {
        currentUid = uid
        val userRef = FirebaseFirestore.getInstance().collection("users").document(uid)
        firestoreRegistration = userRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Error escuchando favoritos de Firestore: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val list = (snapshot.get("favorites") as? List<*>)?.mapNotNull { it as? String }
                if (list != null) {
                    memoryFavorites.clear()
                    memoryFavorites.addAll(list)
                    notifyListeners()
                }
            }
        }
    }

    fun isFavorite(id: String): Boolean = id in memoryFavorites

    fun all(): Set<String> = memoryFavorites.toSet()

    fun toggle(context: Context, id: String) {
        val next = !isFavorite(id)
        if (next) memoryFavorites.add(id) else memoryFavorites.remove(id)
        notifyListeners()

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val userRef = FirebaseFirestore.getInstance().collection("users").document(uid)
            val favRef = userRef.collection("favorites").document(id)

            if (next) {
                userRef.set(
                    mapOf("favorites" to FieldValue.arrayUnion(id)),
                    SetOptions.merge()
                ).addOnFailureListener { Log.e(TAG, "Error agregando favorito", it) }

                favRef.set(
                    mapOf("placeId" to id, "createdAt" to FieldValue.serverTimestamp()),
                    SetOptions.merge()
                ).addOnFailureListener { Log.e(TAG, "Error en subcolección de favoritos", it) }
            } else {
                userRef.set(
                    mapOf("favorites" to FieldValue.arrayRemove(id)),
                    SetOptions.merge()
                ).addOnFailureListener { Log.e(TAG, "Error quitando favorito", it) }

                favRef.delete()
                    .addOnFailureListener { Log.e(TAG, "Error borrando de subcolección de favoritos", it) }
            }
        }
    }

    private fun notifyListeners() {
        val copy = memoryFavorites.toSet()
        listeners.forEach { it(copy) }
    }

    fun observe(observer: (Set<String>) -> Unit): () -> Unit {
        listeners.add(observer)
        observer(memoryFavorites.toSet())
        return { listeners.remove(observer) }
    }
}

/** Observa el conjunto de IDs de lugares favoritos en Compose. */
@Composable
fun rememberFavorites(): Set<String> {
    var favs by remember { mutableStateOf(FavoriteStore.all()) }
    DisposableEffect(Unit) {
        val stop = FavoriteStore.observe { favs = it }
        onDispose { stop() }
    }
    return favs
}
