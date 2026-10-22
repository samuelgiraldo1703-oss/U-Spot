package data.spotty

import android.content.Context
import android.location.Geocoder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Ubicación para las búsquedas de Spotty:
 *  - si el usuario nombra una zona ("estoy en Chapinero"), se ubica con el Geocoder de Android
 *    (gratis, sin clave) dentro de Bogotá,
 *  - si no, [gps] (Fase 4) o, sin GPS, la Universidad Jorge Tadeo Lozano.
 */
class AndroidSpottyLocator(
    private val context: Context,
    private val gps: (suspend () -> GeoPoint?)? = null
) : SpottyLocator {

    private val cache = mutableMapOf<String, GeoPoint?>()

    override suspend fun origin(zone: String?, nearMe: Boolean): SpottyOrigin {
        if (zone != null) {
            geocode(zone)?.let { return SpottyOrigin(it, zone, precise = true) }
        }
        if (nearMe) {
            gps?.invoke()?.let { return SpottyOrigin(it, "tu ubicación actual", precise = true) }
        }
        return SpottyOrigin(SpottyGeo.UTADEO, "la Universidad Jorge Tadeo Lozano", precise = !nearMe)
    }

    @Suppress("DEPRECATION")
    private suspend fun geocode(zone: String): GeoPoint? = cache.getOrPut(zone.lowercase()) {
        if (!Geocoder.isPresent()) return@getOrPut null
        withContext(Dispatchers.IO) {
            runCatching {
                Geocoder(context, Locale.forLanguageTag("es-CO"))
                    .getFromLocationName("$zone, Bogotá, Colombia", 1, 4.45, -74.25, 4.85, -73.98)
                    ?.firstOrNull()
                    ?.let { GeoPoint(it.latitude, it.longitude) }
            }.onFailure { Log.w("SpottyLocator", "No se pudo ubicar '$zone': ${it.message}") }.getOrNull()
        }
    }
}
