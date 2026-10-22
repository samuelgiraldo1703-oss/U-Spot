package data.spotty

import data.HomeCategory
import data.Place
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

/** Desde dónde se miden las distancias en una búsqueda. */
data class SpottyOrigin(val point: GeoPoint, val label: String, val precise: Boolean)

/** Resuelve la ubicación del usuario (GPS o zona escrita). Fase 4 agrega el GPS. */
fun interface SpottyLocator {
    suspend fun origin(zone: String?, nearMe: Boolean): SpottyOrigin
}

/** Lugar devuelto por una búsqueda, con lo calculado para esta pregunta (la tarjeta lo usa tal cual). */
data class SpottyHit(val place: Place, val meters: Int?, val match: Int, val people: Int)

/**
 * Funciones que Gemini puede pedir. Todas se ejecutan aquí, sobre el catálogo real: el modelo
 * solo decide qué buscar y redacta; los lugares, precios, distancias y cuentas salen de Kotlin.
 */
class SpottyTools(private val places: List<Place>, private val locator: SpottyLocator) {

    /** Todo lo que devolvieron las búsquedas de esta pregunta, por id (las tarjetas salen de aquí). */
    val hits = linkedMapOf<String, SpottyHit>()
    var analyzed = 0
        private set
    var lastPeople = 1
        private set

    companion object {
        const val RESPONDER = "responder"
        private val CATEGORIES = HomeCategory.entries.map { it.key }

        private fun prop(type: String, description: String) = JSONObject().put("type", type).put("description", description)

        val declarations: JSONArray = JSONArray().put(JSONObject().put("functionDeclarations", JSONArray()
            .put(JSONObject()
                .put("name", "buscar_lugares")
                .put("description", "Busca lugares del catálogo de U-Spot (centro de Bogotá) y devuelve los mejores con precios por persona, cuenta del grupo y distancia reales. Úsala siempre antes de recomendar.")
                .put("parameters", JSONObject().put("type", "object").put("properties", JSONObject()
                    .put("categoria", prop("string", "Tipo de lugar. Omitir si el usuario no lo dice.").put("enum", JSONArray(CATEGORIES)))
                    .put("personas", prop("integer", "Cuántas personas van, contando al usuario. 1 si va solo."))
                    .put("presupuesto_max_por_persona", prop("integer", "Máximo que puede gastar cada persona, en pesos colombianos (40 lucas = 40000)."))
                    .put("acepta_nequi", prop("boolean", "true si debe aceptar pago con Nequi."))
                    .put("para_estudiar", prop("boolean", "true si es para estudiar o trabajar con calma."))
                    .put("plan_solo", prop("boolean", "true si la persona va sola."))
                    .put("zona", prop("string", "Barrio o lugar de Bogotá donde está el usuario, si lo menciona (p. ej. 'Chapinero', 'la Javeriana')."))
                    .put("cerca_de_mi", prop("boolean", "true si pide algo cercano a su ubicación actual."))
                    .put("distancia_max_metros", prop("integer", "Distancia máxima si el usuario la pone."))
                    .put("nombre", prop("string", "Parte del nombre si busca un lugar concreto."))
                    .put("orden", prop("string", "Cómo ordenar: precio (más barato primero), cercania o coincidencia.").put("enum", JSONArray(listOf("precio", "cercania", "coincidencia"))))
                )))
            .put(JSONObject()
                .put("name", "detalle_lugar")
                .put("description", "Horario completo, notas y datos de un lugar por su id.")
                .put("parameters", JSONObject().put("type", "object")
                    .put("properties", JSONObject().put("id", prop("string", "id del lugar devuelto por buscar_lugares")))
                    .put("required", JSONArray().put("id"))))
            .put(JSONObject()
                .put("name", "presupuesto_grupo")
                .put("description", "Cuánto cuesta un lugar para un grupo: total y aporte sugerido por persona.")
                .put("parameters", JSONObject().put("type", "object")
                    .put("properties", JSONObject()
                        .put("id", prop("string", "id del lugar"))
                        .put("personas", prop("integer", "número de personas")))
                    .put("required", JSONArray().put("id").put("personas"))))
            .put(JSONObject()
                .put("name", RESPONDER)
                .put("description", "Respuesta final al usuario. Llamarla SIEMPRE para terminar, una sola vez.")
                .put("parameters", JSONObject().put("type", "object")
                    .put("properties", JSONObject()
                        .put("texto", prop("string", "Mensaje corto (máximo 3 frases) en español colombiano cercano."))
                        .put("lugares", JSONObject().put("type", "array").put("description", "Lugares recomendados, en orden, SOLO con ids devueltos por buscar_lugares. Vacío si no aplica.")
                            .put("items", JSONObject().put("type", "object").put("properties", JSONObject()
                                .put("id", prop("string", "id exacto"))
                                .put("motivo", prop("string", "Por qué encaja, usando solo datos devueltos por las funciones.")))
                                .put("required", JSONArray().put("id"))))
                        .put("seguimiento", prop("string", "Pregunta corta para seguir ayudando."))
                        .put("sugerencias", JSONObject().put("type", "array").put("description", "2 o 3 respuestas rápidas que el usuario podría tocar (máximo 30 caracteres cada una, pueden empezar con un emoji).")
                            .put("items", JSONObject().put("type", "string"))))
                    .put("required", JSONArray().put("texto").put("lugares"))))
        ))
    }

    /** Ejecuta una función (menos [RESPONDER]) y devuelve el JSON que se le manda al modelo. */
    suspend fun run(name: String, args: JSONObject): JSONObject = when (name) {
        "buscar_lugares" -> search(args)
        "detalle_lugar" -> detail(args.optString("id"))
        "presupuesto_grupo" -> budget(args.optString("id"), args.optInt("personas", 1))
        else -> JSONObject().put("error", "Función desconocida: $name")
    }

    private suspend fun search(a: JSONObject): JSONObject {
        val category = HomeCategory.fromKey(a.optString("categoria").takeIf { it.isNotBlank() })
        val people = a.optInt("personas", 1).coerceIn(1, 50)
        val budgetMax = a.optInt("presupuesto_max_por_persona", 0).takeIf { it > 0 }
        val nequi = a.optBoolean("acepta_nequi", false)
        val study = a.optBoolean("para_estudiar", false)
        val solo = a.optBoolean("plan_solo", false)
        val zone = a.optString("zona").takeIf { it.isNotBlank() }
        val nearMe = a.optBoolean("cerca_de_mi", false) || zone != null
        val maxMeters = a.optInt("distancia_max_metros", 0).takeIf { it > 0 }
        val name = a.optString("nombre").takeIf { it.isNotBlank() }?.lowercase()
        val order = a.optString("orden").ifBlank { if (budgetMax != null) "precio" else if (nearMe) "cercania" else "coincidencia" }
        lastPeople = people

        val origin = locator.origin(zone, nearMe)
        var pool = places.filter { category == null || it.category == category }
        if (name != null) pool = pool.filter { name in it.name.lowercase() }
        analyzed = pool.size

        // Filtros: obligatorios primero; los opcionales se aflojan (y se avisa) si dejan la lista vacía
        val relaxed = mutableListOf<String>()
        fun narrow(label: String, keep: (Place) -> Boolean) {
            val next = pool.filter(keep)
            if (next.isNotEmpty()) pool = next else relaxed += label
        }
        if (people >= 4) narrow("apto para grupo") { it.group }
        if (budgetMax != null) narrow("presupuesto") { (SpottyBudget.pricePerPerson(it)?.first ?: Int.MAX_VALUE) <= budgetMax }
        if (nequi) narrow("acepta Nequi") { it.nequi }
        if (study) narrow("para estudiar") { it.study }
        if (solo) narrow("plan solo") { it.solo }
        if (maxMeters != null) narrow("distancia máxima") { p -> SpottyGeo.meters(origin.point, p)?.let { it <= maxMeters } ?: false }

        fun matchOf(p: Place, meters: Int?): Int {
            var total = 0.0; var ok = 0.0
            fun check(weight: Double, passed: Boolean) { total += weight; if (passed) ok += weight }
            if (category != null) check(2.0, p.category == category)
            if (people >= 4) check(2.0, p.group)
            if (budgetMax != null) check(2.0, (SpottyBudget.pricePerPerson(p)?.last ?: Int.MAX_VALUE) <= budgetMax)
            if (nequi) check(1.0, p.nequi)
            if (study) check(1.0, p.study)
            if (solo) check(1.0, p.solo)
            if (nearMe || maxMeters != null) check(1.5, meters != null && meters <= (maxMeters ?: 1200))
            // Cercanía como criterio suave: entre dos lugares que cumplen todo, gana el más cercano
            else if (meters != null) { total += 1.0; ok += (1.0 - meters / 2500.0).coerceIn(0.0, 1.0) }
            // Sin criterios explícitos: se premia lo cercano y lo económico
            if (total == 0.0) {
                check(1.0, meters != null && meters <= 1000)
                check(1.0, (SpottyBudget.pricePerPerson(p)?.first ?: Int.MAX_VALUE) <= 25000)
            }
            return (100 * ok / total).roundToInt().coerceIn(1, 99)
        }

        val scored = pool.map { p ->
            val m = SpottyGeo.meters(origin.point, p)
            SpottyHit(p, m, matchOf(p, m), people)
        }
        val price = { h: SpottyHit -> SpottyBudget.pricePerPerson(h.place)?.let { (it.first + it.last) / 2 } ?: Int.MAX_VALUE }
        val dist = { h: SpottyHit -> h.meters ?: Int.MAX_VALUE }
        val sorted = when (order) {
            "precio" -> scored.sortedWith(compareBy(price, dist))
            "cercania" -> scored.sortedWith(compareBy(dist, price))
            else -> scored.sortedWith(compareByDescending<SpottyHit> { it.match }.thenBy(price).thenBy(dist))
        }.take(5)
        sorted.forEach { hits[it.place.id] = it }

        val nearest = scored.mapNotNull { it.meters }.minOrNull()
        val warning = if (origin.precise && nearest != null && nearest > 2000)
            "Todos los lugares de U-Spot están en el centro de Bogotá; el más cercano a ${origin.label} está a ${SpottyGeo.label(nearest).removePrefix("A ")}. Díselo al usuario con honestidad."
        else null

        return JSONObject()
            .apply { warning?.let { put("aviso", it) } }
            .put("lugares_analizados", analyzed)
            .put("coincidencias", pool.size)
            .put("distancias_desde", origin.label + if (origin.precise) "" else " (aproximado)")
            .put("filtros_no_cumplidos", JSONArray(relaxed))
            .put("lugares", JSONArray(sorted.map { placeJson(it) }))
    }

    private fun placeJson(h: SpottyHit): JSONObject {
        val p = h.place
        val o = JSONObject()
            .put("id", p.id)
            .put("nombre", p.name)
            .put("categoria", p.category.label)
            .put("direccion", p.address)
            .put("horario", p.hours + if (p.hoursVaries) " (aproximado)" else "")
            .put("precio_por_persona", p.price + if (p.priceVaries) " (varía según el evento)" else "")
            .put("acepta_nequi", p.nequi)
            .put("apto_grupo", p.group)
            .put("para_estudiar", p.study)
            .put("coincidencia_pct", h.match)
        h.meters?.let { o.put("distancia", SpottyGeo.label(it).removePrefix("A ")) }
        if (h.people > 1) SpottyBudget.group(p, h.people)?.let { g ->
            o.put("personas", g.people)
                .put("aporte_sugerido_por_persona", SpottyBudget.pesos(g.suggestedEach))
                .put("total_grupo", "${SpottyBudget.pesos(g.total.first)} - ${SpottyBudget.pesos(g.total.last)}")
        }
        if (p.notes.isNotEmpty()) o.put("notas", JSONArray(p.notes))
        return o
    }

    private fun detail(id: String): JSONObject {
        val p = places.firstOrNull { it.id == id } ?: return JSONObject().put("error", "No existe un lugar con id '$id' en U-Spot")
        val hit = hits[id] ?: SpottyHit(p, null, 0, lastPeople)
        return placeJson(hit)
            .put("horario_detallado", JSONArray(p.schedule.map { "${it.days}: ${it.hours}" }))
    }

    private fun budget(id: String, people: Int): JSONObject {
        val p = places.firstOrNull { it.id == id } ?: return JSONObject().put("error", "No existe un lugar con id '$id' en U-Spot")
        val g = SpottyBudget.group(p, people.coerceIn(1, 50))
            ?: return JSONObject().put("lugar", p.name).put("precio", p.price).put("nota", "El precio de este lugar no tiene un rango fijo")
        lastPeople = g.people
        hits[id]?.let { hits[id] = it.copy(people = g.people) }
        return JSONObject()
            .put("lugar", p.name)
            .put("personas", g.people)
            .put("precio_por_persona", p.price)
            .put("total_grupo", "${SpottyBudget.pesos(g.total.first)} - ${SpottyBudget.pesos(g.total.last)}")
            .put("aporte_sugerido_por_persona", SpottyBudget.pesos(g.suggestedEach))
    }
}
