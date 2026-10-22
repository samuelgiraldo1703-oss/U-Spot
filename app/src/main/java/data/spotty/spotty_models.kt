package data.spotty

/** Quién escribió el mensaje. */
enum class SpottyRole { USER, SPOTTY }

/**
 * Lugar recomendado por Spotty. Solo guarda el id del catálogo (places.json) y lo que la app
 * calculó para esta pregunta: los demás datos (nombre, foto, dirección, precios) se leen siempre
 * del catálogo, nunca del texto del modelo.
 */
data class SpottyCard(
    val placeId: String,
    /** Qué tanto cumple el lugar lo pedido (0-100), calculado en Kotlin. */
    val match: Int,
    /** "A 1.2 km" desde la ubicación del usuario, o null si no se sabe dónde está. */
    val distance: String?,
    /** Línea inferior de la tarjeta: "Por persona" o "Cada uno (6)" y su valor. */
    val priceLabel: String,
    val priceValue: String
)

/** Un mensaje del chat. Los de Spotty pueden traer el resumen del análisis, tarjetas y sugerencias. */
data class SpottyMessage(
    val id: String,
    val role: SpottyRole,
    val text: String,
    val time: Long,
    /** "Analicé 14 locales cerca de ti • 2 coincidencias ideales" */
    val analysis: String? = null,
    val cards: List<SpottyCard> = emptyList(),
    /** Pregunta de seguimiento debajo de las tarjetas. */
    val followUp: String? = null,
    /** Chips de respuesta rápida que se muestran sobre la barra de escritura. */
    val suggestions: List<String> = emptyList()
)

/** Fila del historial de chats. */
data class SpottyChatSummary(
    val id: String,
    val title: String,
    val updatedAt: Long
)

/** Estado del motor que la pantalla muestra en lugar de la respuesta (Fase 3 los produce de verdad). */
enum class SpottyProblem {
    /** No hay internet. */
    OFFLINE,
    /** Se acabó la cuota gratuita del día (error 429 de Gemini). */
    QUOTA,
    /** Falta GEMINI_API_KEY en local.properties. */
    NO_KEY,
    /** Cualquier otro error del servicio. */
    GENERIC
}
