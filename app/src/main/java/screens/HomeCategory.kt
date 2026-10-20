package screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.HomeCategory
import data.HomePlace
import data.Place
import data.extraIn
import data.forFigma
import data.rememberPlaces
import decorations.PlaceCard
import decorations.Scene
import decorations.SceneImage
import decorations.SceneNode
import decorations.SceneScreen
import decorations.allNodes
import decorations.descendants
import decorations.app_colors
import decorations.firstNode
import decorations.jakartaText
import decorations.placeAt
import decorations.rememberScene

/**
 * Pantalla de una categoría: Figma "Home App (Pantalla Principal)" (442:1658 Restaurantes,
 * 442:1860 Cafés, 445:2268 Bares, ...). Las tarjetas abren el detalle del lugar; la cuadrícula de
 * iconos de arriba a la derecha despliega el menú para cambiar de categoría.
 */
@Composable
fun HomeCategoryScreen(
    category: HomeCategory = HomeCategory.RESTAURANTES,
    onBack: () -> Unit = {},
    onPlace: (HomePlace) -> Unit = {},
    onExtraPlace: (Place) -> Unit = {},
    onCategory: (HomeCategory) -> Unit = {}
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    if (menuOpen) BackHandler { menuOpen = false }
    val places = rememberPlaces()
    val extra = remember(places, category) { places.extraIn(category) }

    SceneScreen(
        sceneKey = category.sceneKey,
        statusBarColor = app_colors.white,
        onClick = { scene -> categoryClicks(scene, category, onBack, onPlace) { menuOpen = !menuOpen } },
        hide = { scene -> figmaCards(scene, category, places).flatMap { it.hidden() }.toSet() },
        texts = { scene -> figmaCards(scene, category, places).flatMap { it.texts().entries }.associate { it.key to it.value } },
        overlay = { scene ->
            val cards = figmaCards(scene, category, places)
            // Precio y horario reales en las tarjetas de Figma
            for (card in cards) card.pricing?.let { PricingRow(card.info, it, card.card) }
            // Debajo de las tarjetas de Figma, el resto de lugares de la categoría (más cercanos primero)
            val bottom = scene.allNodes { it is SceneNode.Frame && it.name.startsWith("Article") }.maxOfOrNull { it.y + it.h } ?: scene.height
            Column(
                modifier = Modifier.padding(start = 27.dp, top = (bottom + 16f).dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                for (p in extra) PlaceCard(p, onClick = { onExtraPlace(p) })
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
            if (menuOpen) {
                // Toque fuera del menú: lo cierra
                Box(
                    Modifier
                        .placeAt(0.dp, 0.dp, scene.width.dp, scene.height.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { menuOpen = false }
                )
                val grid = categoryGrid(scene)
                CategoryMenu(
                    current = category,
                    top = ((grid?.let { it.y + it.h } ?: 175f) + 8f),
                    onSelect = {
                        menuOpen = false
                        if (it != category) onCategory(it)
                    }
                )
            }
        }
    )
}

/**
 * Tarjeta de Figma de un lugar que también está en la base de datos: se muestra igual, pero con el
 * nombre de la sede, el precio, el horario y Nequi reales.
 */
private class FigmaCard(val card: SceneNode, val info: Place) {
    /**
     * Fila del precio de Figma ("USD 35.00 / Cena Completa", "COP 16.000 / Boleta"...) que se
     * reemplaza. Cada categoría la nombra distinto ("Pricing Row", "HorizontalBorder", "Container"):
     * es el marco cuyo "Paragraph" trae el precio.
     */
    val pricing: SceneNode? = card.descendants { row ->
        row is SceneNode.Frame && row.children.any { p ->
            p.name == "Paragraph" && p.descendants { it is SceneNode.Text && Regex("^(USD|COP|\\$)").containsMatchIn(it.plain) }.isNotEmpty()
        }
    }.firstOrNull()

    fun hidden(): List<String> = buildList {
        pricing?.let { add(it.id) }
        if (!info.nequi) addAll(card.descendants { it is SceneNode.Frame && it.name.contains("Nequi") }.map { it.id })
    }

    /** El título de la tarjeta (debajo de la foto) pasa a ser el nombre de la sede. */
    fun texts(): Map<String, String> =
        card.descendants { it is SceneNode.Text && it.spans.firstOrNull()?.color == Color(0xFF1A2942) && it.spans.first().size >= 18f }
            .associate { it.id to info.name }
}

private fun figmaCards(scene: Scene, category: HomeCategory, places: List<Place>): List<FigmaCard> =
    articleCards(scene).zip(category.places).mapNotNull { (card, place) -> places.forFigma(place)?.let { FigmaCard(card, it) } }

private fun articleCards(scene: Scene) =
    scene.allNodes { it is SceneNode.Frame && it.name.startsWith("Article") }.sortedBy { it.y }

/**
 * Precio (a la izquierda) y horario (a la derecha) en el lugar del "Pricing Row" de Figma, sin
 * salirse de la tarjeta ([card] recorta lo que pase de su borde). Si el espacio no alcanza para dos
 * líneas (p. ej. Cine Colombia, con muchas etiquetas), va todo en una sola línea.
 */
@Composable
private fun PricingRow(place: Place, row: SceneNode, card: SceneNode) {
    val bottom = minOf(row.y + row.h, card.y + card.h - 10f)
    if (bottom - row.y < 34f) {
        Row(
            Modifier.placeAt(row.x.dp, (bottom - 20f).dp, row.w.dp, 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF1A2942))) { append(place.price) }
                    append(if (place.priceVaries) "  aprox." else "")
                },
                style = jakartaText(11.sp, 18.sp, Color(0xFF475569), FontWeight.Medium),
                maxLines = 1
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = buildAnnotatedString {
                    append("Horario: ")
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A2942))) { append(place.hours) }
                },
                style = jakartaText(11.sp, 18.sp, Color(0xFF475569), FontWeight.Medium, textAlign = TextAlign.End),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.4f, fill = false)
            )
        }
        return
    }
    Row(
        Modifier.placeAt(row.x.dp, (bottom - 34f).dp, row.w.dp, 34.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(Modifier.weight(1f)) {
            Text(place.price, style = jakartaText(15.sp, 18.sp, Color(0xFF1A2942), FontWeight.ExtraBold))
            Text(
                if (place.priceVaries) "Aprox. · según el evento" else "Rango de precios",
                style = jakartaText(12.sp, 16.sp, Color(0xFF475569), FontWeight.Medium)
            )
        }
        Text(
            text = buildAnnotatedString {
                append("Horario: ")
                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A2942))) { append(place.hours) }
            },
            style = jakartaText(11.sp, 15.sp, Color(0xFF475569), FontWeight.Medium, textAlign = TextAlign.End),
            maxLines = 2,
            modifier = Modifier.width(150.dp)
        )
    }
}

/** Cuadrícula 2x2 de iconos junto al título (abre el menú de categorías). */
private fun categoryGrid(scene: Scene): SceneNode? =
    scene.firstNode { it.name == "2x2 Soft-tinted Category Grid Buttons" }

private fun categoryClicks(
    scene: Scene,
    category: HomeCategory,
    onBack: () -> Unit,
    onPlace: (HomePlace) -> Unit,
    toggleMenu: () -> Unit
): Map<String, () -> Unit> {
    val clicks = HashMap<String, () -> Unit>()
    // Flecha "←" de arriba a la derecha (en Ocio hay dos superpuestas): vuelve al dashboard
    scene.allNodes { it is SceneNode.Picture && it.w == 40f && it.h == 40f && it.x > 300f && it.y < 60f }
        .forEach { clicks[it.id] = onBack }
    categoryGrid(scene)?.let { clicks[it.id] = toggleMenu }
    // Tarjetas de lugares, de arriba hacia abajo
    articleCards(scene).zip(category.places).forEach { (card, place) -> clicks[card.id] = { onPlace(place) } }
    return clicks
}

/**
 * Menú de categorías bajo la cuadrícula (no está en Figma: se armó con el icono y el nombre de
 * cada categoría, con el estilo de las tarjetas del Home).
 */
@Composable
private fun CategoryMenu(current: HomeCategory, top: Float, onSelect: (HomeCategory) -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .placeAt((412 - 16 - 224).dp, top.dp, 224.dp, (HomeCategory.entries.size * 52 + 16).dp)
            .dropShadow(shape, Shadow(radius = 24.dp, color = Color(0x26000000), offset = DpOffset(0.dp, 8.dp)))
            .clip(shape)
            .background(app_colors.white)
            .border(1.dp, Color(0xFFF1F5F9), shape)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (c in HomeCategory.entries) {
            CategoryMenuRow(c, selected = c == current, onClick = { onSelect(c) })
        }
    }
}

@Composable
private fun CategoryMenuRow(category: HomeCategory, selected: Boolean, onClick: () -> Unit) {
    val scene = rememberScene(category.sceneKey)
    val badge = scene?.find(category.badgeId) as? SceneNode.Picture
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0xFFF1F5F9) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp)) {
            if (badge != null) SceneImage(badge.src, Modifier.size(36.dp))
        }
        Box(Modifier.width(12.dp))
        Text(
            text = category.label,
            style = jakartaText(15.sp, 20.sp, Color(0xFF1A2942), if (selected) FontWeight.ExtraBold else FontWeight.Bold)
        )
    }
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun HomeCategoryPreview() {
    HomeCategoryScreen()
}
