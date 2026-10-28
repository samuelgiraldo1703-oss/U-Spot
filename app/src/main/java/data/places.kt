package data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject

/** Horario de un día o rango de días ("Mar-Sáb" → "8am-8pm"). */
data class PlaceSchedule(val days: String, val hours: String)

/**
 * Lugar del Home con los datos reales de la base del equipo (data_import/lugares.xlsx, convertida
 * con data_import/excel_to_json.ps1 a assets/places/places.json). [figma] es la pantalla de Figma
 * que se mezcla con estos datos; los lugares sin ella usan la tarjeta y el detalle genéricos.
 * [id] es también la clave de sus comentarios (places/{id}/comments).
 */
data class Place(
    val id: String,
    val figma: HomePlace?,
    val name: String,
    val category: HomeCategory,
    val address: String,
    /** Coordenadas verificadas (data_import/geocodificar.ps1); null si el lugar aún no las tiene. */
    val lat: Double? = null,
    val lng: Double? = null,
    val distanceM: Int,
    val distance: String,
    val walkMin: Int,
    val nequi: Boolean,
    val group: Boolean,
    val solo: Boolean,
    val study: Boolean,
    val quiet: Boolean,
    /** Rango de precios tal como está en el Excel; si [priceVaries], es un rango aproximado de la categoría. */
    val price: String,
    val priceVaries: Boolean,
    /** Horario resumido; si [hoursVaries], es el horario habitual de la categoría. */
    val hours: String,
    val hoursVaries: Boolean,
    val schedule: List<PlaceSchedule>,
    val notes: List<String>,
    /** Descripción breve del lugar (30-40 palabras) para el detalle y el buscador de Spotty. */
    val description: String = "",
    /** Etiquetas de búsqueda (platos, tipo de cocina, actividades, ambiente) para Spotty. */
    val tags: List<String> = emptyList(),
    /** Foto propia del lugar en assets/figma/img (null: se usa la de su categoría). */
    val photoFile: String? = null
) {
    /** "Más info" solo aparece en los lugares que tienen algo que contar. */
    val hasMoreInfo: Boolean get() = description.isNotBlank() || schedule.isNotEmpty() || notes.isNotEmpty()

    /** "A 400 m de UTADEO" */
    val distanceLabel: String get() = "A $distance de UTADEO"
}

private fun JSONObject.strings(name: String): List<String> =
    optJSONArray(name)?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()

private fun placeOf(o: JSONObject): Place? {
    val category = HomeCategory.fromKey(o.optString("category")) ?: return null
    val schedule = o.optJSONArray("schedule") ?: JSONArray()
    return Place(
        id = o.getString("id"),
        figma = HomePlace.fromKey(o.optString("figma").takeIf { it.isNotEmpty() && it != "null" }),
        name = o.getString("name"),
        category = category,
        address = o.optString("address"),
        lat = o.optDouble("lat").takeUnless { it.isNaN() },
        lng = o.optDouble("lng").takeUnless { it.isNaN() },
        distanceM = o.optInt("distanceM"),
        distance = o.optString("distance"),
        walkMin = o.optInt("walkMin", 1),
        nequi = o.optBoolean("nequi"),
        group = o.optBoolean("group"),
        solo = o.optBoolean("solo"),
        study = o.optBoolean("study"),
        quiet = o.optBoolean("quiet"),
        price = o.optString("price"),
        priceVaries = o.optBoolean("priceVaries"),
        hours = o.optString("hours"),
        hoursVaries = o.optBoolean("hoursVaries"),
        schedule = (0 until schedule.length()).map {
            val s = schedule.getJSONObject(it)
            PlaceSchedule(s.optString("days"), s.optString("hours"))
        },
        notes = o.strings("notes"),
        description = o.optString("description"),
        tags = o.strings("tags"),
        photoFile = o.optString("photo").takeIf { it.isNotEmpty() && it != "null" }
    )
}

/** Mismo formato que places.json, desde un documento de Firestore (colección "places"). */
private fun DocumentSnapshot.toPlace(): Place? {
    val map = data ?: return null
    val o = JSONObject(map)
    if (!o.has("id")) o.put("id", id)
    return placeOf(o)
}

/**
 * Lugares del Home. Arranca con los que vienen en la app (assets/places/places.json, así se ven
 * sin internet) y, si la colección "places" de Firestore tiene datos, usa esos (así se pueden
 * corregir sin publicar otra versión).
 */
object PlaceStore {
    private const val TAG = "PlaceStore"
    private var bundled: List<Place>? = null
    private var remote: List<Place>? = null
    private var listening = false
    private val observers = mutableSetOf<(List<Place>) -> Unit>()

    fun all(context: Context): List<Place> = remote ?: bundled ?: loadBundled(context)

    private fun loadBundled(context: Context): List<Place> {
        val list = runCatching {
            val json = context.assets.open("places/places.json").bufferedReader().use { it.readText() }
            val arr = JSONObject(json).getJSONArray("places")
            (0 until arr.length()).mapNotNull { placeOf(arr.getJSONObject(it)) }
        }.onFailure { Log.e(TAG, "No se pudo leer places.json", it) }.getOrDefault(emptyList())
        bundled = list
        return list
    }

    fun observe(context: Context, observer: (List<Place>) -> Unit): () -> Unit {
        observers += observer
        if (!listening) {
            listening = true
            FirebaseFirestore.getInstance().collection("places").addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "Lugares de Firestore no disponibles, se usan los de la app: ${error.message}")
                    return@addSnapshotListener
                }
                // Los documentos de Firestore se subieron antes de geocodificar y sin el enlace a la
                // pantalla de Figma ni la foto propia: lo que les falte se toma de places.json (mismo
                // id), sin escribir nada en la base.
                val local = (bundled ?: loadBundled(context)).associateBy { it.id }
                val raw = snap?.documents?.mapNotNull { it.toPlace() }.orEmpty()
                val list = raw.map { p ->
                    val l = local[p.id] ?: return@map p
                    p.copy(
                        lat = p.lat ?: l.lat,
                        lng = p.lng ?: l.lng,
                        figma = p.figma ?: l.figma,
                        photoFile = p.photoFile ?: l.photoFile,
                        description = p.description.ifBlank { l.description },
                        tags = p.tags.ifEmpty { l.tags }
                    )
                }
                if (list.isNotEmpty()) {
                    remote = list.sortedWith(compareBy({ it.category.ordinal }, { it.distanceM }, { it.name }))
                    observers.forEach { it(remote!!) }
                }
            }
        }
        return { observers -= observer }
    }
}

/** Todos los lugares (se actualiza si cambian en Firestore). */
@Composable
fun rememberPlaces(): List<Place> {
    val context = LocalContext.current.applicationContext
    var places by remember { mutableStateOf(PlaceStore.all(context)) }
    DisposableEffect(Unit) {
        val stop = PlaceStore.observe(context) { places = it }
        onDispose { stop() }
    }
    return places
}

/** Lugar del Excel mezclado con una pantalla de Figma, si lo hay. */
fun List<Place>.forFigma(place: HomePlace): Place? = firstOrNull { it.figma == place }

/** Lugares de una categoría que no tienen pantalla de Figma (van debajo de las tarjetas de Figma). */
fun List<Place>.extraIn(category: HomeCategory): List<Place> = filter { it.category == category && it.figma == null }
