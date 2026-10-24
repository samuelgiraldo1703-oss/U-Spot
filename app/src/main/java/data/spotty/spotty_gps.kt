package data.spotty

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Ubicación real del usuario para "cerca de mí". El permiso se pide la primera vez que Spotty lo
 * necesita (no al abrir la pantalla): el motor llama a [current], que pide a la pantalla mostrar
 * el diálogo de Android ([pendingRequest]) y espera la respuesta. Si el usuario lo niega no se
 * vuelve a pedir en la sesión; Spotty mide desde la Tadeo y le pregunta en qué zona está.
 */
class SpottyGps(private val context: Context) {
    /** La pantalla observa esto y lanza el diálogo de permisos cuando no es null. */
    var pendingRequest by mutableStateOf<CompletableDeferred<Boolean>?>(null)
        private set
    private var askedThisSession = false

    private val granted: Boolean
        get() = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    /** Respuesta del diálogo de permisos (la llama la pantalla). */
    fun onPermissionResult(ok: Boolean) {
        pendingRequest?.complete(ok)
        pendingRequest = null
    }

    suspend fun current(): GeoPoint? {
        if (!granted) {
            if (askedThisSession) return null
            askedThisSession = true
            val answer = CompletableDeferred<Boolean>()
            pendingRequest = answer
            if (!answer.await()) return null
        }
        return locate()
    }

    @SuppressLint("MissingPermission") // se comprueba en current()
    private suspend fun locate(): GeoPoint? = runCatching {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cancel = CancellationTokenSource()
        val fresh = withTimeoutOrNull(6000) {
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancel.token).await()
        }
        if (fresh == null) cancel.cancel()
        (fresh ?: client.lastLocation.await())?.let { GeoPoint(it.latitude, it.longitude) }
    }.onFailure { Log.w("SpottyGps", "Sin ubicación: ${it.message}") }.getOrNull()
}
