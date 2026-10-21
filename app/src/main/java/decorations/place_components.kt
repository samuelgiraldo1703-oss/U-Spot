package decorations

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.HomeCategory
import data.Place

/*
 * Piezas de los lugares con datos reales (assets/places/places.json). Usan los colores, fuentes y
 * medidas de las tarjetas y del detalle de Figma ("Home App (Pantalla Principal)" y "Home App
 * (Restaurantes)"); lo que no tiene equivalente en Figma (información y "Más info") sigue el estilo
 * de las tarjetas "Distancia" / "Ambiente".
 */

private val Navy = Color(0xFF1A2942)
private val Slate = Color(0xFF475569)
private val SlateLight = Color(0xFF64748B)
private val Ink = Color(0xFF1E293B)

/**
 * Colores de cada categoría, tomados de sus pantallas de Figma: etiquetas de las tarjetas de la
 * lista ("Home App (Pantalla Principal)") y detalle ("Home App (Restaurantes)", "(Cafeterias)"...).
 * [template] es la pantalla de detalle de Figma de la que se copian los recuadros de rasgos, las
 * tarjetas "Distancia" / "Ambiente" y "Compartir" para los lugares sin pantalla propia.
 * [iconLight] / [iconDark] son el círculo y el trazo de los iconos de rasgos (para recolorear un
 * icono que la pantalla de la categoría no trae).
 */
class CategoryStyle(
    val template: String,
    val chipBg: Color,
    val chipText: Color,
    val chipWeight: FontWeight,
    val title: Color,
    val iconLight: Color,
    val iconDark: Color,
    val accent: Color
)

val HomeCategory.style: CategoryStyle
    get() = when (this) {
        HomeCategory.RESTAURANTES -> CategoryStyle("place_wok", Color(0xFFD7F3ED), Color(0xFF136657), FontWeight.SemiBold, Navy, Color(0xFFD8F0F7), Color(0xFF0F766E), Color(0xFF0F766E))
        HomeCategory.CAFES -> CategoryStyle("place_juanvaldez", Color(0xFFECEBF9), Color(0xFF4B439C), FontWeight.SemiBold, Navy, Color(0xFFCFCBFF), Color(0xFF4B439C), Color(0xFF4B439C))
        HomeCategory.BARES -> CategoryStyle("place_bbc", Color(0xFFFFF2D9), Color(0xFFE5A016), FontWeight.SemiBold, Navy, Color(0xFFFACB53), Color(0xFF1F1A10), Color(0xFFB7791F))
        HomeCategory.TEATROS -> CategoryStyle("place_cinecolombia", Color(0xFFE0E7FF), Color(0xFF1E40AF), FontWeight.SemiBold, Navy, Color(0xFF7286C6), Color(0xFFFFFFFF), Color(0xFF1E40AF))
        HomeCategory.OCIO -> CategoryStyle("place_billar", Color(0xFFFCE7F3), Color(0xFF9D174D), FontWeight.SemiBold, Color(0xFF172B4D), Color(0xFFFFD7E8), Color(0xFFDB2777), Color(0xFF9D174D))
        HomeCategory.MUSEOS -> CategoryStyle("place_museonacional", Color(0x1A475569), Color(0xFF334155), FontWeight.Normal, Color(0xFF172B4D), Color(0xFFE1E3E3), Color(0xFF334155), Color(0xFF334155))
        HomeCategory.DISCOTECAS -> CategoryStyle("place_campus", Color(0xFFFFEDD5), Color(0xFFC2410C), FontWeight.Normal, Color(0xFF172B4D), Color(0xFFFFEDD5), Color(0xFFF5883C), Color(0xFFC2410C))
        HomeCategory.LIBRERIAS -> CategoryStyle("place_lerner", Color(0xFFF5EBE6), Color(0xFF78350F), FontWeight.Normal, Color(0xFF172B4D), Color(0xFF895A2C), Color(0xFFFFFFFF), Color(0xFF78350F))
    }

/** Foto de la categoría (una foto real de un local de la zona) en assets/figma/img. */
val HomeCategory.photo: String get() = "cat_$key.jpg"

/** Foto del lugar: la suya (assets/figma/img/place_<id>.jpg) o, si no tiene, la de su categoría. */
val Place.photo: String get() = photoFile ?: category.photo

/** Nombre de la categoría en singular, para la etiqueta sobre la foto ("Restaurante"). */
val HomeCategory.singular: String
    get() = when (this) {
        HomeCategory.RESTAURANTES -> "Restaurante"
        HomeCategory.CAFES -> "Café"
        HomeCategory.BARES -> "Bar"
        HomeCategory.TEATROS -> "Teatro"
        HomeCategory.OCIO -> "Ocio"
        HomeCategory.MUSEOS -> "Museo"
        HomeCategory.DISCOTECAS -> "Discoteca"
        HomeCategory.LIBRERIAS -> "Librería"
    }

/** Textos de la tarjeta "Distancia" (dos líneas, como en Figma). */
val Place.distanceLines: Pair<String, String> get() = "A $distance de\nUTADEO" to "$walkMin min\ncaminando"

/** Textos de la tarjeta "Ambiente": tranquilo para estudiar o animado para ir en grupo. */
val Place.atmosphereLines: Pair<String, String>
    get() = if (quiet) "Ambiente\ntranquilo" to "Ideal para estudiar" else "Ambiente\nanimado" to "Ideal para parchar"

/** Rasgos del lugar en el orden de los recuadros de Figma (Feature 2..5). */
fun Place.features(): List<String> = buildList {
    if (study) add("Feature 2")
    if (group) add("Feature 3")
    if (solo) add("Feature 4")
    if (nequi) add("Feature 5")
}

// ---------------------------------------------------------------------------------------------
// Recuadros de rasgos (Wi-Fi, estudiar, grupo, persona sola, Nequi, rango) tomados de Figma
// ---------------------------------------------------------------------------------------------

/** Pantalla con los seis recuadros de rasgos, de donde sale el icono que le falte a otra (recoloreado). */
private const val FULL_FEATURES = "place_wok"

/** Separación entre recuadros de rasgos en Figma. */
private const val FEATURE_GAP = 8f

fun Scene.feature(prefix: String): SceneNode? = firstNode { it is SceneNode.Frame && it.name.startsWith("$prefix:") }

/** Recuadros de rasgos de una pantalla de Figma ("Feature 1: Wi-Fi" ... "Feature 6: Precios"). */
fun Scene.featureNodes(): List<SceneNode> =
    allNodes { it is SceneNode.Frame && Regex("^Feature \\d:").containsMatchIn(it.name) }

/**
 * Fila de recuadros de rasgos que sí aplican al lugar, repartidos en todo el ancho [width] desde
 * ([x], [y]) (dp de Figma), como en Figma (con menos recuadros, cada uno es más ancho). Cada
 * recuadro sale de [own] (la pantalla de Figma del lugar, si tiene), si no de la pantalla de su
 * categoría y, si tampoco lo trae, del icono de [FULL_FEATURES] con los colores de la categoría.
 * [wifi] agrega el de Wi-Fi (solo lo traen las pantallas de Figma).
 */
@Composable
fun PlaceFeatureRow(place: Place, x: Float, y: Float, width: Float, wifi: Boolean, own: Scene? = null) {
    val style = place.category.style
    val category = rememberScene(style.template)
    val full = rememberScene(FULL_FEATURES)
    if (category == null || full == null) return
    val names = buildList {
        if (wifi) add("Feature 1")
        addAll(place.features())
        add("Feature 6")
    }
    val pieces = names.mapNotNull { name ->
        own?.feature(name)?.let { return@mapNotNull it to own }
        category.feature(name)?.let { return@mapNotNull it to category }
        val base = full.feature(name) ?: return@mapNotNull null
        // Recuadro que la categoría no trae: el de la pantalla completa con la paleta de la categoría
        val sample = (category.featureNodes().firstOrNull() as? SceneNode.Frame)
        val recolored = base.copy(src = { pic ->
            recolorSrc(pic.src, 0xD8F0F7, 0x0F766E, style.iconLight.toArgb(), style.iconDark.toArgb())
        })
        val frame = recolored as SceneNode.Frame
        SceneNode.Frame(
            frame.id, frame.name, frame.x, frame.y, frame.w, frame.h, frame.opacity,
            sample?.let { (it as SceneNode.Frame).fills } ?: frame.fills,
            frame.radius, frame.stroke, frame.shadows, frame.clip, frame.children
        ) to full
    }
    if (pieces.isEmpty()) return
    val w = (width - FEATURE_GAP * (pieces.size - 1)) / pieces.size
    pieces.forEachIndexed { i, (node, scene) ->
        val texts = if (node.name.startsWith("Feature 6")) {
            node.descendants { it is SceneNode.Text && it.plain.trim().let { t -> t.startsWith("$") || t.first().isDigit() } }
                .firstOrNull()?.let { mapOf(it.id to place.price) } ?: emptyMap()
        } else emptyMap()
        val moved = node.copy(dx = x + i * (w + FEATURE_GAP) - node.x, dy = y - node.y, w = w)
        FigmaSceneNode(scene, moved, texts = texts)
    }
}

/** Ids de los dos textos de una tarjeta de Figma ("Distance Card", "Atmosphere Card"), de arriba abajo. */
fun Scene.cardTexts(cardName: String): List<SceneNode.Text> {
    val card = firstNode { it is SceneNode.Frame && it.name == cardName } as? SceneNode.Frame ?: return emptyList()
    val out = ArrayList<SceneNode.Text>()
    fun visit(list: List<SceneNode>) {
        for (n in list) {
            if (n is SceneNode.Text) out.add(n)
            if (n is SceneNode.Frame) visit(n.children)
        }
    }
    visit(card.children)
    return out.sortedBy { it.y }
}

/** Textos de las tarjetas "Distancia" y "Ambiente" de [scene] con los datos de [place]. */
fun Scene.placeCardTexts(place: Place): Map<String, String> {
    val out = HashMap<String, String>()
    val (distance, walk) = place.distanceLines
    val (mood, ideal) = place.atmosphereLines
    cardTexts("Distance Card").let { t ->
        t.getOrNull(0)?.let { out[it.id] = distance }
        t.getOrNull(1)?.let { out[it.id] = walk }
    }
    cardTexts("Atmosphere Card").let { t ->
        t.getOrNull(0)?.let { out[it.id] = mood }
        t.getOrNull(1)?.let { out[it.id] = ideal }
    }
    return out
}

// ---------------------------------------------------------------------------------------------
// Etiqueta de categoría sobre la foto ("Cafetería Category Badge")
// ---------------------------------------------------------------------------------------------

@Composable
fun HeroCategoryBadge(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .wrapContentWidth(Alignment.Start)
            .height(30.dp)
            .dropShadow(shape, Shadow(radius = 2.dp, color = Color(0x0D000000), offset = DpOffset(0.dp, 1.dp)))
            .clip(shape)
            .background(Color(0x99000000))
            .border(1.dp, Color(0x1AFFFFFF), shape)
            .padding(start = 13.dp, end = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = jakartaText(12.sp, 16.sp, Color.White, FontWeight.Medium))
    }
}

// ---------------------------------------------------------------------------------------------
// Información (dirección, horario, precio) y "Más info"
// ---------------------------------------------------------------------------------------------

/**
 * Bloque "Información" del detalle: sede (si el nombre de la pantalla es el de la marca),
 * dirección, horario y precio. "Más info" (horario por día y notas) solo sale si el lugar lo tiene.
 */
@Composable
fun PlaceInfoSection(place: Place, showName: Boolean, cardColor: Color, modifier: Modifier = Modifier) {
    val style = place.category.style
    var open by rememberSaveable(place.id) { mutableStateOf(false) }
    Column(modifier) {
        Text(
            text = "Información",
            style = jakartaText(20.sp, 28.sp, Ink, FontWeight.Bold, letterSpacing = (-0.3).sp)
        )
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardColor)
                .animateContentSize()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            if (showName) InfoRow(style, Icons.Outlined.Storefront, "Sede", place.name)
            InfoRow(style, Icons.Outlined.LocationOn, "Dirección", place.address)
            InfoRow(
                style, Icons.Outlined.Schedule, "Horario", place.hours,
                hint = if (place.hoursVaries) "Varía según la programación · horario habitual" else null
            )
            InfoRow(
                style, Icons.Outlined.Payments, "Precio", place.price,
                hint = if (place.priceVaries) "Depende del evento · rango promedio aproximado" else null
            )
            if (place.hasMoreInfo) {
                Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(Color(0x14000000)))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { open = !open }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Más info", style = jakartaText(12.sp, 16.sp, style.accent, FontWeight.Bold), modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (open) "Ocultar" else "Ver más",
                        tint = style.accent,
                        modifier = Modifier.size(20.dp).rotate(if (open) 180f else 0f)
                    )
                }
                if (open) MoreInfo(place, style)
            }
        }
    }
}

@Composable
private fun InfoRow(style: CategoryStyle, icon: ImageVector, label: String, value: String, hint: String? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        InfoIcon(style, icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = jakartaText(10.sp, 15.sp, SlateLight, FontWeight.Medium))
            Text(value, style = jakartaText(12.sp, 16.sp, Ink, FontWeight.Bold))
            if (hint != null) Text(hint, style = jakartaText(10.sp, 15.sp, SlateLight, FontWeight.Medium))
        }
    }
}

/** Círculo con icono, como el de las tarjetas "Distancia" / "Ambiente" de Figma. */
@Composable
private fun InfoIcon(style: CategoryStyle, icon: ImageVector) {
    // Mismo círculo y trazo que los iconos de rasgos de la categoría (en Discotecas el círculo es del
    // color de la tarjeta: va blanco para que se vea)
    val circle = if (style.iconLight == style.chipBg) Color.White else style.iconLight
    Box(
        Modifier.size(32.dp).clip(CircleShape).background(circle),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = style.iconDark, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun MoreInfo(place: Place, style: CategoryStyle) {
    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        if (place.schedule.isNotEmpty()) {
            Text("Horario por día", style = jakartaText(11.sp, 16.sp, SlateLight, FontWeight.SemiBold))
            Spacer(Modifier.height(6.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                place.schedule.forEachIndexed { i, s ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF1F5F9)))
                    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(s.days, style = jakartaText(11.sp, 16.sp, Color(0xFF334155), FontWeight.SemiBold), modifier = Modifier.width(96.dp))
                        Text(
                            s.hours.replaceFirstChar { it.uppercase() },
                            style = jakartaText(11.sp, 16.sp, if (s.hours == "cerrado") SlateLight else Ink, FontWeight.Medium, textAlign = TextAlign.End),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        if (place.notes.isNotEmpty()) {
            if (place.schedule.isNotEmpty()) Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (note in place.notes) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = style.accent, modifier = Modifier.padding(top = 1.dp).size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(note, style = jakartaText(11.sp, 16.sp, Slate, FontWeight.Medium))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Tarjeta de un lugar en la lista de la categoría ("Article - Card1_Wok" de Figma)
// ---------------------------------------------------------------------------------------------

/** Etiquetas de la tarjeta según los datos del lugar. */
private fun Place.tags(): List<String> = buildList {
    if (distanceM <= 600) add("Cerca de la U")
    add(if (study) "Apto para estudiar" else "Ambiente animado")
    if (solo) add("Ideal para persona sola")
    if (group) add("Apto para grupo")
}

/** Tarjeta de 358 dp de ancho (medidas de Figma) para los lugares sin pantalla propia en Figma. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaceCard(place: Place, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val style = place.category.style
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .width(358.dp)
            .dropShadow(shape, Shadow(radius = 16.dp, color = Color(0x0F000000), offset = DpOffset(0.dp, 4.dp)))
            .clip(shape)
            .background(Color.White)
            .border(1.dp, Color(0xFFF1F5F9), shape)
            .clickable(onClick = onClick)
    ) {
        // Foto con el nombre encima
        Box(Modifier.fillMaxWidth().height(176.dp).background(Color(0xFF0F172A))) {
            SceneImage(place.photo, Modifier.fillMaxSize(), crop = true)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0f to Color(0x1A000000), 0.45f to Color(0x33000000), 1f to Color(0xCC000000))
                )
            )
            HeroCategoryBadge(place.category.singular, Modifier.align(Alignment.TopStart).padding(12.dp))
            val context = LocalContext.current
            val favorites = data.rememberFavorites()
            val isFav = place.id in favorites
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000))
                    .clickable { data.FavoriteStore.toggle(context, place.id) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (isFav) "Quitar de favoritos" else "Guardar en favoritos",
                    tint = if (isFav) Color(0xFFE11D48) else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                Text(
                    place.name,
                    style = jakartaText(18.sp, 22.5.sp, Color.White, FontWeight.ExtraBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(place.address, style = jakartaText(11.sp, 15.13.sp, Color(0xFFE2E8F0), FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "A ${place.distance} · ${place.walkMin} min caminando",
                    style = jakartaText(11.sp, 15.13.sp, Color(0xFFE2E8F0), FontWeight.Medium)
                )
            }
        }
        // Detalles
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (t in place.tags()) Chip(t, style)
                if (place.nequi) NequiChip(style)
            }
            Box(Modifier.padding(vertical = 12.dp).fillMaxWidth().height(1.dp).background(Color(0xFFF1F5F9)))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = style.title)) { append(place.price) }
                    withStyle(SpanStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, color = Slate)) {
                        append(if (place.priceVaries) "  / Aprox. · según el evento" else "  / Rango de precios")
                    }
                },
                style = jakartaText(15.sp, 20.sp, Navy)
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Slate, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (place.hoursVaries) "${place.hours} · varía según la programación" else place.hours,
                    style = jakartaText(12.sp, 16.sp, Slate, FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun Chip(text: String, style: CategoryStyle) {
    Box(
        Modifier.height(20.5.dp).clip(RoundedCornerShape(50)).background(style.chipBg).padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = jakartaText(11.sp, 16.5.sp, style.chipText, style.chipWeight))
    }
}

/** "Acepta Nequi" con el rombo de Nequi de Figma. */
@Composable
private fun NequiChip(style: CategoryStyle) {
    val scene = rememberScene("list_restaurantes")
    val icon = scene?.firstNode { it is SceneNode.Picture && it.name == "Nequi Rhombus icon mock" } as? SceneNode.Picture
    Row(
        Modifier.height(21.dp).clip(RoundedCornerShape(50)).background(style.chipBg).padding(start = 8.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) SceneImage(icon.src, Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text("Acepta Nequi", style = jakartaText(11.sp, 16.5.sp, style.chipText, style.chipWeight))
    }
}
