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
data class SpottyHit(val place: Place, val meters: Int?, val match: Int, val people: Int, val relevance: Int = 0)

/**
 * Funciones que Gemini puede pedir. Todas se ejecutan aquí, sobre el catálogo real: el modelo
 * solo decide qué buscar y redacta; los lugares, precios, distancias y cuentas salen de Kotlin.
 */
class SpottyTools(
    private val places: List<Place>,
    private val locator: SpottyLocator,
    /** Última pregunta del usuario: permite respaldar el filtro semántico con lo que pidió literalmente. */
    private val userText: String = ""
) {

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

        private val STOP_WORDS = setOf(
            "quiero", "quisiera", "busco", "buscar", "necesito", "recomienda", "recomendacion", "recomiendame",
            "un", "una", "unos", "unas", "el", "la", "los", "las", "de", "del", "al", "en", "por", "para",
            "con", "sin", "que", "como", "donde", "cerca", "cercano", "cercana", "cercanos", "alrededor",
            "tadeo", "utadeo", "universidad", "jorge", "lozano", "bogota", "centro", "lugar", "lugares",
            "sitio", "sitios", "local", "locales", "plan", "planes", "ir", "comer", "tomar", "hacer",
            "algo", "algun", "alguna", "restaurante", "restaurantes", "comida", "comidas", "bar", "bares",
            "cafeteria", "cafeterias", "personas", "persona", "amigos", "grupo", "barato", "economico",
            "bueno", "bonito", "rico", "delicioso", "hoy", "noche", "tarde", "dia", "abierto", "ver", "hay"
        )

        val declarations: JSONArray by lazy {
            JSONArray().put(JSONObject().put("functionDeclarations", JSONArray()
                .put(JSONObject()
                    .put("name", "buscar_lugares")
                    .put("description", "Busca lugares del catálogo de U-Spot (centro de Bogotá) usando sus nombres, descripciones y etiquetas (tags), y devuelve los mejores con precios por persona, cuenta del grupo y distancia reales. Úsala siempre antes de recomendar.")
                    .put("parameters", JSONObject().put("type", "object").put("properties", JSONObject()
                        .put("categoria", prop("string", "Categoría del catálogo. teatros = teatros Y CINES (Cine Colombia CityU, Cinemateca de Bogotá); ocio = billar, tejo, bolos, parques, Monserrate, centros culturales; museos = museos y galerías; restaurantes = comida; cafes = cafés y panaderías; librerias = librerías. Omitir si el usuario no lo dice o no estás seguro.").put("enum", JSONArray(CATEGORIES)))
                        .put("tipo_lugar", prop("string", "Plato, tipo de cocina o actividad específica que pide el usuario, en una a tres palabras: p. ej. 'comida italiana', 'salchipapas', 'comida peruana', 'mexicana', 'ajiaco', 'alitas', 'pizza', 'sushi', 'ramen', 'hamburguesas', 'empanadas', 'arepas', 'cine', 'billar', 'tejo', 'bolos', 'salsa', 'rock', 'cerveza artesanal', 'brunch'… Se busca en nombre, etiquetas (tags) y descripción del lugar."))
                        .put("personas", prop("integer", "Cuántas personas van, contando al usuario. 1 si va solo."))
                        .put("presupuesto_max_por_persona", prop("integer", "Máximo que puede gastar cada persona, en pesos colombianos (40 lucas = 40000; 0 = gratis). Omitir si no lo dice."))
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
                    .put("description", "Horario completo, descripción, etiquetas, notas y datos de un lugar por su id.")
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
    }

    /** Minúsculas y sin tildes, para comparar nombres, tags y descripciones. */
    private fun plain(s: String): String =
        java.text.Normalizer.normalize(s.lowercase().trim(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    /** Raíz léxica simple en español (quita plurales y género final para empatar italiana/italiano/italia o salchipapas/salchipapa). */
    private fun stem(word: String): String {
        var s = plain(word)
        if (s.length > 4 && s.endsWith("es") && s[s.length - 3] !in "aeiou") s = s.dropLast(2)
        else if (s.length > 3 && s.endsWith("s")) s = s.dropLast(1)
        if (s.length > 5 && (s.endsWith("a") || s.endsWith("o"))) s = s.dropLast(1)
        return s
    }

    private fun stemsMatch(a: String, b: String): Boolean {
        if (a.isEmpty() || b.isEmpty()) return false
        if (a == b) return true
        val minLen = minOf(a.length, b.length)
        return minLen >= 5 && (a.startsWith(b) || b.startsWith(a))
    }

    private fun wordsOf(text: String): List<String> =
        plain(text).split(Regex("[^a-z0-9]+")).filter { it.length >= 3 }

    /** Familias semánticas de platos, cocinas y planes para expandir búsquedas por sinónimos. */
    private val kinds = listOf(
        "Comida italiana" to listOf("italiana", "italiano", "italia", "pasta", "pastas", "lasana", "raviolis", "fetuccini", "espagueti", "risotto", "trattoria", "bruschetta", "noquis", "canelones"),
        "Comida peruana" to listOf("peruana", "peruano", "ceviche", "lomo saltado", "chaufa", "tiradito", "nikkei", "causa limena", "pisco"),
        "Comida mexicana" to listOf("mexicana", "mexicano", "tacos", "taco", "nachos", "quesadillas", "burrito", "guacamole", "mezcal", "mole"),
        "Comida típica colombiana" to listOf("tipica", "colombiana", "ajiaco", "bandeja paisa", "frijoles", "sancocho", "tamal", "changua", "sobrebarriga", "piqueteadero", "fritanga", "corrientazo", "ejecutivo"),
        "Comida de mar y pacífico" to listOf("pacifico", "mariscos", "pescaderia", "pescado frito", "mojarra", "camarones", "encocado", "cazuela de mariscos", "comida de mar"),
        "Comida asiática y sushi" to listOf("sushi", "asiatica", "asiatico", "japonesa", "japones", "ramen", "wok", "pad thai", "curry", "baos", "gyozas", "teriyaki", "oriental", "tailandesa"),
        "Comida española" to listOf("espanola", "espanol", "paella", "tapas", "montaditos", "sangria", "jamon serrano"),
        "Parrilla y carnes" to listOf("parrilla", "asado", "carnes", "steakhouse", "t-bone", "bife", "punta de anca", "costillas", "argentina", "argentino"),
        "Salchipapas y papas" to listOf("salchipapa", "salchipapas", "papas locas", "papas cargadas"),
        "Hamburguesas" to listOf("hamburguesa", "hamburguesas", "burger", "smash burger"),
        "Alitas" to listOf("alitas", "wings", "boneless"),
        "Pizzería" to listOf("pizza", "pizzas", "pizzeria"),
        "Empanadas y arepas" to listOf("empanada", "empanadas", "arepa", "arepas", "cachapa", "cachapas", "tequenos"),
        "Crepes, waffles y postres" to listOf("crepe", "crepes", "waffle", "waffles", "helado", "helados", "postre", "postres", "tortas", "hojaldre"),
        "Brunch y desayunos" to listOf("brunch", "desayuno", "pancakes"),
        "Cervecería" to listOf("cerveza", "cerveceria", "pola", "beer", "pub", "brew", "bbc"),
        "Salsa" to listOf("salsa", "son cubano"),
        "Rock" to listOf("rock", "punk", "metal"),
        "Jazz y tango" to listOf("jazz", "tango", "boleros"),
        "Cine" to listOf("cine", "cinema", "pelicula", "peliculas", "cinemateca"),
        "Billar" to listOf("billar", "pool"),
        "Tejo y bolirana" to listOf("tejo", "turmeque", "bolirana", "rana"),
        "Bolos" to listOf("bolos", "bolera", "boliche"),
        "Karaoke" to listOf("karaoke"),
        "Galería" to listOf("galeria", "gallery", "exposicion", "exposiciones")
    )

    /** Extrae términos de búsqueda relevantes combinando [rawKind] y palabras específicas de [userText]. */
    private fun extractQueryTerms(rawKind: String?): List<String> {
        val fromKind = rawKind?.let(::wordsOf).orEmpty().filter { it !in STOP_WORDS }
        if (fromKind.isNotEmpty()) return fromKind
        val rawWords = rawKind?.let(::wordsOf).orEmpty()
        if (rawWords.isNotEmpty()) return rawWords
        // Respaldo desde el texto del usuario si el modelo omitió tipo_lugar
        val userWords = wordsOf(userText).filter { it !in STOP_WORDS }
        return userWords.filter { uw ->
            val uStem = stem(uw)
            kinds.any { (_, groupWords) ->
                groupWords.any { gw -> wordsOf(gw).any { stemsMatch(uStem, stem(it)) } }
            }
        }
    }

    /**
     * Puntaje de coincidencia semántica de un lugar frente a lo pedido (0 = no coincide).
     * Revisa: nombre del lugar (peso alto), tags del lugar (peso alto), descripción/notas (peso medio)
     * y familias de sinónimos en [kinds].
     */
    private fun relevanceOf(p: Place, rawKind: String?, queryTerms: List<String>): Int {
        if (rawKind.isNullOrBlank() && queryTerms.isEmpty()) return 0
        val namePlain = plain(p.name)
        val nameStems = wordsOf(namePlain).map(::stem)
        val tagPlains = p.tags.map(::plain)
        val tagStems = tagPlains.flatMap(::wordsOf).map(::stem)
        val descPlain = plain(p.description + " " + p.notes.joinToString(" "))
        val descStems = wordsOf(descPlain).map(::stem)

        var score = 0
        val kindPlain = rawKind?.let(::plain)?.trim().orEmpty()

        // 1. Frase completa en tags o nombre (p. ej. "comida italiana", "cerveza artesanal", "papas cargadas")
        if (kindPlain.length >= 4) {
            if (kindPlain in namePlain) score += 15
            if (tagPlains.any { it == kindPlain || kindPlain in it }) score += 14
            else if (kindPlain in descPlain) score += 6
        }

        // 2. Coincidencia por cada término clave (con raíz morfológica)
        for (term in queryTerms) {
            val tStem = stem(term)
            val inName = nameStems.any { stemsMatch(it, tStem) }
            val inTags = tagStems.any { stemsMatch(it, tStem) }
            val inDesc = descStems.any { stemsMatch(it, tStem) }

            if (inName) score += 10
            if (inTags) score += 9
            if (inDesc) score += 4

            // 3. Expansión por familia de sinónimos (p. ej. pide "italiana" y el tag tiene "pasta" o "trattoria")
            val group = kinds.firstOrNull { (_, words) ->
                words.any { gw -> wordsOf(gw).any { stemsMatch(stem(it), tStem) } }
            }
            if (group != null) {
                val synStems = group.second.flatMap(::wordsOf).map(::stem).toSet()
                if (!inName && nameStems.any { ns -> synStems.any { stemsMatch(ns, it) } }) score += 5
                if (!inTags && tagStems.any { ts -> synStems.any { stemsMatch(ts, it) } }) score += 4
            }
        }
        return score
    }

    /** Búsqueda directa en memoria sobre el catálogo (usada por pruebas unitarias y diagnóstico). */
    fun findMatching(
        category: HomeCategory? = null,
        rawKind: String? = null,
        originPoint: GeoPoint = SpottyGeo.UTADEO
    ): List<SpottyHit> {
        val queryTerms = extractQueryTerms(rawKind)
        val hasKindQuery = !rawKind.isNullOrBlank() || queryTerms.isNotEmpty()
        var pool = places.filter { category == null || it.category == category }
        val relevanceMap = mutableMapOf<String, Int>()
        if (hasKindQuery) {
            for (p in places) {
                val r = relevanceOf(p, rawKind, queryTerms)
                if (r > 0) relevanceMap[p.id] = r
            }
            val byKindAll = places.filter { (relevanceMap[it.id] ?: 0) > 0 }
            val inCategory = byKindAll.filter { category == null || it.category == category }
            if (inCategory.isNotEmpty()) pool = inCategory
            else if (byKindAll.isNotEmpty()) pool = byKindAll
        }
        return pool.map { p ->
            val m = SpottyGeo.meters(originPoint, p)
            SpottyHit(p, m, 90, 1, relevanceMap[p.id] ?: 0)
        }.sortedWith(
            compareByDescending<SpottyHit> { it.relevance }
                .thenBy { it.meters ?: Int.MAX_VALUE }
        )
    }

    /** Etiqueta descriptiva principal del lugar según sus tags o su nombre. */
    private fun kindOf(p: Place): String? {
        val name = plain(p.name)
        val tagsText = p.tags.joinToString(" ") { plain(it) }
        return kinds.firstOrNull { (_, words) ->
            words.any { Regex("\\b$it\\b").containsMatchIn(name) || Regex("\\b$it\\b").containsMatchIn(tagsText) }
        }?.first
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
        // 0 es válido: "gratis"
        val budgetMax = if (a.has("presupuesto_max_por_persona")) a.optInt("presupuesto_max_por_persona", 0).coerceAtLeast(0) else null
        val nequi = a.optBoolean("acepta_nequi", false)
        val study = a.optBoolean("para_estudiar", false)
        val solo = a.optBoolean("plan_solo", false)
        val zone = a.optString("zona").takeIf { it.isNotBlank() }
        val nearMe = a.optBoolean("cerca_de_mi", false) || zone != null
        val maxMeters = a.optInt("distancia_max_metros", 0).takeIf { it > 0 }
        val name = a.optString("nombre").takeIf { it.isNotBlank() }?.let(::plain)
        val rawKind = a.optString("tipo_lugar").takeIf { it.isNotBlank() }
        val queryTerms = extractQueryTerms(rawKind)
        val hasKindQuery = !rawKind.isNullOrBlank() || queryTerms.isNotEmpty()
        val order = a.optString("orden").ifBlank { if (budgetMax != null) "precio" else if (nearMe) "cercania" else "coincidencia" }
        lastPeople = people

        val origin = locator.origin(zone, nearMe)
        var pool = places.filter { category == null || it.category == category }
        if (name != null) pool = pool.filter { name in plain(it.name) }

        val relaxed = mutableListOf<String>()
        val relevanceMap = mutableMapOf<String, Int>()
        if (hasKindQuery) {
            for (p in places) {
                if (name == null || name in plain(p.name)) {
                    val r = relevanceOf(p, rawKind, queryTerms)
                    if (r > 0) relevanceMap[p.id] = r
                }
            }
            val byKindAll = places.filter { (relevanceMap[it.id] ?: 0) > 0 }
            val inCategory = byKindAll.filter { category == null || it.category == category }
            when {
                inCategory.isNotEmpty() -> pool = inCategory
                byKindAll.isNotEmpty() -> pool = byKindAll
                else -> relaxed += (rawKind ?: queryTerms.joinToString(" "))
            }
        }
        analyzed = pool.size

        // Filtros adicionales: se aflojan (y se avisa) si dejan la lista vacía
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

        val maxRelevance = pool.maxOfOrNull { relevanceMap[it.id] ?: 0 }?.coerceAtLeast(1) ?: 1

        fun matchOf(p: Place, meters: Int?): Int {
            var total = 0.0; var ok = 0.0
            fun check(weight: Double, passed: Boolean) { total += weight; if (passed) ok += weight }
            if (category != null) check(2.0, p.category == category)
            if (hasKindQuery) {
                val rel = relevanceMap[p.id] ?: 0
                total += 3.0
                if (rel > 0) ok += 3.0 * (0.65 + 0.35 * (rel.toDouble() / maxRelevance)).coerceIn(0.0, 1.0)
            }
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
            val rel = relevanceMap[p.id] ?: 0
            SpottyHit(p, m, matchOf(p, m), people, rel)
        }
        val price = { h: SpottyHit -> SpottyBudget.pricePerPerson(h.place)?.let { (it.first + it.last) / 2 } ?: Int.MAX_VALUE }
        val dist = { h: SpottyHit -> h.meters ?: Int.MAX_VALUE }
        val sorted = when (order) {
            "precio" -> scored.sortedWith(compareBy(price, dist))
            "cercania" -> scored.sortedWith(compareBy(dist, price))
            else -> scored.sortedWith(compareByDescending<SpottyHit> { it.relevance }.thenByDescending { it.match }.thenBy(dist).thenBy(price))
        }.take(5)
        sorted.forEach { hits[it.place.id] = it }

        val nearest = scored.mapNotNull { it.meters }.minOrNull()
        val warning = if (origin.precise && nearest != null && nearest > 2000)
            "Todos los lugares de U-Spot están en el centro de Bogotá; el más cercano a ${origin.label} está a ${SpottyGeo.label(nearest).removePrefix("A ")}. Díselo al usuario con honestidad."
        else null

        val locationHint = if (nearMe && !origin.precise)
            "No tengo la ubicación del usuario (no dio permiso o no hay GPS): las distancias son desde la Universidad Jorge Tadeo Lozano. Díselo y pregúntale en qué barrio o lugar está."
        else null

        return JSONObject()
            .apply { warning?.let { put("aviso", it) } }
            .apply { locationHint?.let { put("aviso_ubicacion", it) } }
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
            .apply { kindOf(p)?.let { put("tipo", it) } }
            .apply { if (p.description.isNotBlank()) put("descripcion", p.description) }
            .apply { if (p.tags.isNotEmpty()) put("tags", JSONArray(p.tags.take(12))) }
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
