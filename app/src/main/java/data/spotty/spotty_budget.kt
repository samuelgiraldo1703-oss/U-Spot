package data.spotty

import data.Place
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Cuentas de dinero de Spotty, siempre en Kotlin (nunca las hace el modelo). El rango de precio
 * del Excel ("$15k - $50k") es lo que gasta UNA persona (decisión del equipo, 21-oct-2026).
 */
object SpottyBudget {

    /** "$15k - $50k" → 15000..50000; "$8.5k - $20k" → 8500..20000; null si no se entiende. */
    fun pricePerPerson(price: String): IntRange? {
        if (price.trim().equals("gratis", ignoreCase = true)) return 0..0
        // "$60k+" (sin tope): se toma el mínimo como referencia
        val values = Regex("""\$\s*([\d.,]+)\s*(k|mil)?""", RegexOption.IGNORE_CASE).findAll(price).mapNotNull { m ->
            val number = m.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@mapNotNull null
            val pesos = if (m.groupValues[2].isNotEmpty()) number * 1000 else number
            pesos.roundToInt()
        }.toList()
        return when (values.size) {
            0 -> null
            1 -> values[0]..values[0]
            else -> minOf(values[0], values[1])..maxOf(values[0], values[1])
        }
    }

    fun pricePerPerson(place: Place): IntRange? = pricePerPerson(place.price)

    /** Total del grupo y aporte sugerido por persona (punto medio del rango). */
    data class GroupBudget(val people: Int, val perPerson: IntRange, val total: IntRange, val suggestedEach: Int)

    fun group(place: Place, people: Int): GroupBudget? {
        val each = pricePerPerson(place) ?: return null
        val n = people.coerceAtLeast(1)
        return GroupBudget(
            people = n,
            perPerson = each,
            total = (each.first * n)..(each.last * n),
            suggestedEach = roundTo500((each.first + each.last) / 2.0)
        )
    }

    /** $32.500 (formato colombiano, sin decimales). */
    fun pesos(value: Int): String = "$" + "%,d".format(Locale.US, value).replace(',', '.')

    /** "$15k - $50k" con el mismo estilo corto del Excel. */
    fun shortRange(range: IntRange): String =
        if (range.first == range.last) short(range.first) else "${short(range.first)} - ${short(range.last)}"

    private fun short(value: Int): String {
        if (value < 1000) return "$$value"
        val k = value / 1000.0
        return if (k == k.toInt().toDouble()) "$${k.toInt()}k" else "$${"%.1f".format(Locale.US, k).replace(',', '.')}k"
    }

    private fun roundTo500(value: Double): Int = ((value / 500.0).roundToInt() * 500)
}
