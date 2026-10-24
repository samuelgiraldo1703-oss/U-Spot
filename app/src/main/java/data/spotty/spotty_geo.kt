package data.spotty

import data.Place
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Punto en el mapa (grados). */
data class GeoPoint(val lat: Double, val lng: Double)

object SpottyGeo {
    /** Universidad Jorge Tadeo Lozano según OpenStreetMap: origen cuando no se sabe dónde está el usuario. */
    val UTADEO = GeoPoint(4.6069421, -74.0679263)

    /** Distancia en línea recta (metros). */
    fun meters(a: GeoPoint, b: GeoPoint): Int {
        val k = Math.PI / 180
        val dLat = (b.lat - a.lat) * k
        val dLng = (b.lng - a.lng) * k
        val h = sin(dLat / 2) * sin(dLat / 2) + cos(a.lat * k) * cos(b.lat * k) * sin(dLng / 2) * sin(dLng / 2)
        return (2 * 6371000 * asin(sqrt(h))).roundToInt()
    }

    /** Distancia del lugar al punto, o null si el lugar no tiene coordenadas. */
    fun meters(from: GeoPoint, place: Place): Int? {
        val lat = place.lat ?: return null
        val lng = place.lng ?: return null
        return meters(from, GeoPoint(lat, lng))
    }

    /** "A 350 m" / "A 1.2 km" (estilo de las tarjetas de Figma). */
    fun label(meters: Int): String =
        if (meters < 1000) "A ${((meters + 25) / 50) * 50} m"
        else if (meters < 10000) "A ${"%.1f".format(java.util.Locale.US, meters / 1000.0)} km"
        else "A ${"%,d".format(java.util.Locale.US, meters / 1000).replace(',', '.')} km"
}
