package decorations

import android.content.Context
import androidx.annotation.StringRes
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.u_spot.R
import data.HomeCategory
import data.Place

/*
 * Piezas de los lugares con datos reales (assets/places/places.json). Usan los colores, fuentes y
 * medidas de las tarjetas y del detalle de Figma ("Home App (Pantalla Principal)" y "Home App
 * (Restaurantes)"); lo que no tiene equivalente en Figma (información y "Más info") sigue el estilo
 * de las tarjetas "Distancia" / "Ambiente".
 */

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
        HomeCategory.RESTAURANTES -> CategoryStyle("place_wok", app_colors.styleRestChipBg, app_colors.styleRestChipText, FontWeight.SemiBold, app_colors.placeNavy, app_colors.styleRestIconLight, app_colors.styleRestAccent, app_colors.styleRestAccent)
        HomeCategory.CAFES -> CategoryStyle("place_juanvaldez", app_colors.styleCafeChipBg, app_colors.styleCafeAccent, FontWeight.SemiBold, app_colors.placeNavy, app_colors.styleCafeIconLight, app_colors.styleCafeAccent, app_colors.styleCafeAccent)
        HomeCategory.BARES -> CategoryStyle("place_bbc", app_colors.styleBarChipBg, app_colors.styleBarChipText, FontWeight.SemiBold, app_colors.placeNavy, app_colors.styleBarIconLight, app_colors.styleBarIconDark, app_colors.styleBarAccent)
        HomeCategory.TEATROS -> CategoryStyle("place_cinecolombia", app_colors.styleTheaterChipBg, app_colors.styleTheaterAccent, FontWeight.SemiBold, app_colors.placeNavy, app_colors.styleTheaterIconLight, app_colors.white, app_colors.styleTheaterAccent)
        HomeCategory.OCIO -> CategoryStyle("place_billar", app_colors.styleOcioChipBg, app_colors.styleOcioAccent, FontWeight.SemiBold, app_colors.photoTitle, app_colors.styleOcioIconLight, app_colors.styleOcioIconDark, app_colors.styleOcioAccent)
        HomeCategory.MUSEOS -> CategoryStyle("place_museonacional", app_colors.styleMuseumChipBg, app_colors.styleMuseumAccent, FontWeight.Normal, app_colors.photoTitle, app_colors.styleMuseumIconLight, app_colors.styleMuseumAccent, app_colors.styleMuseumAccent)
        HomeCategory.DISCOTECAS -> CategoryStyle("place_campus", app_colors.styleClubChipBg, app_colors.styleClubAccent, FontWeight.Normal, app_colors.photoTitle, app_colors.styleClubChipBg, app_colors.styleClubIconDark, app_colors.styleClubAccent)
        HomeCategory.LIBRERIAS -> CategoryStyle("place_lerner", app_colors.styleBookChipBg, app_colors.styleBookAccent, FontWeight.Normal, app_colors.photoTitle, app_colors.styleBookIconLight, app_colors.white, app_colors.styleBookAccent)
    }

/** Foto de la categoría (una foto real de un local de la zona) en assets/figma/img. */
val HomeCategory.photo: String get() = "cat_$key.jpg"

/** Foto del lugar: la suya (assets/figma/img/place_<id>.jpg) o, si no tiene, la de su categoría. */
val Place.photo: String get() = photoFile ?: category.photo

/** Recurso de string del nombre de la categoría en singular ("Restaurante", "Café"...). */
@get:StringRes
val HomeCategory.singularRes: Int
    get() = when (this) {
        HomeCategory.RESTAURANTES -> R.string.cat_singular_restaurant
        HomeCategory.CAFES -> R.string.cat_singular_cafe
        HomeCategory.BARES -> R.string.cat_singular_bar
        HomeCategory.TEATROS -> R.string.cat_singular_theater
        HomeCategory.OCIO -> R.string.cat_singular_leisure
        HomeCategory.MUSEOS -> R.string.cat_singular_museum
        HomeCategory.DISCOTECAS -> R.string.cat_singular_club
        HomeCategory.LIBRERIAS -> R.string.cat_singular_bookstore
    }

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

/** Textos de la tarjeta "Distancia" (dos líneas, como en Figma) usando recursos de strings. */
fun Place.distanceLines(context: Context): Pair<String, String> =
    context.getString(R.string.place_distance_card_line_1, distance) to
        context.getString(R.string.place_distance_card_line_2, walkMin)

/** Textos de la tarjeta "Ambiente" usando recursos de strings. */
fun Place.atmosphereLines(context: Context): Pair<String, String> =
    if (quiet) {
        context.getString(R.string.place_atmosphere_quiet_line_1) to context.getString(R.string.place_atmosphere_quiet_line_2)
    } else {
        context.getString(R.string.place_atmosphere_lively_line_1) to context.getString(R.string.place_atmosphere_lively_line_2)
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
fun Scene.placeCardTexts(place: Place, context: Context? = null): Map<String, String> {
    val out = HashMap<String, String>()
    val (distance, walk) = if (context != null) place.distanceLines(context) else place.distanceLines
    val (mood, ideal) = if (context != null) place.atmosphereLines(context) else place.atmosphereLines
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
            .dropShadow(shape, Shadow(radius = 2.dp, color = app_colors.shadowSoft05, offset = DpOffset(0.dp, 1.dp)))
            .clip(shape)
            .background(app_colors.scrimDark60)
            .border(1.dp, app_colors.whiteAlpha10, shape)
            .padding(start = 13.dp, end = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = app_colors.white, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = jakartaText(12.sp, 16.sp, app_colors.white, FontWeight.Medium))
    }
}

// ---------------------------------------------------------------------------------------------
// Información (dirección, horario, precio) y "Más info"
// ---------------------------------------------------------------------------------------------

/**
 * Bloque "Información" del detalle: sede (si el nombre de la pantalla es el de la marca),
 * dirección, horario y precio. "Más info" (descripción, horario por día y notas) solo sale si el lugar lo tiene.
 */
@Composable
fun PlaceInfoSection(place: Place, showName: Boolean, cardColor: Color, modifier: Modifier = Modifier) {
    val style = place.category.style
    var open by rememberSaveable(place.id) { mutableStateOf(false) }
    Column(modifier) {
        Text(
            text = stringResource(R.string.place_info_sheet_title),
            style = jakartaText(20.sp, 28.sp, app_colors.placeInk, FontWeight.Bold, letterSpacing = (-0.3).sp)
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
            if (showName) InfoRow(style, Icons.Outlined.Storefront, stringResource(R.string.place_info_label_branch), place.name)
            InfoRow(style, Icons.Outlined.LocationOn, stringResource(R.string.place_info_label_address), place.address)
            InfoRow(
                style, Icons.Outlined.Schedule, stringResource(R.string.place_info_label_hours), place.hours,
                hint = if (place.hoursVaries) stringResource(R.string.place_info_hint_hours_varies) else null
            )
            InfoRow(
                style, Icons.Outlined.Payments, stringResource(R.string.place_info_label_price), place.price,
                hint = if (place.priceVaries) stringResource(R.string.place_info_hint_price_varies) else null
            )
            if (place.hasMoreInfo) {
                Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(app_colors.dividerBlack08))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { open = !open }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.place_info_label_more), style = jakartaText(12.sp, 16.sp, style.accent, FontWeight.Bold), modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(if (open) R.string.place_info_hide_cd else R.string.place_info_show_more_cd),
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
            Text(label, style = jakartaText(10.sp, 15.sp, app_colors.placeSlateLight, FontWeight.Medium))
            Text(value, style = jakartaText(12.sp, 16.sp, app_colors.placeInk, FontWeight.Bold))
            if (hint != null) Text(hint, style = jakartaText(10.sp, 15.sp, app_colors.placeSlateLight, FontWeight.Medium))
        }
    }
}

/** Círculo con icono, como el de las tarjetas "Distancia" / "Ambiente" de Figma. */
@Composable
private fun InfoIcon(style: CategoryStyle, icon: ImageVector) {
    // Mismo círculo y trazo que los iconos de rasgos de la categoría (en Discotecas el círculo es del
    // color de la tarjeta: va blanco para que se vea)
    val circle = if (style.iconLight == style.chipBg) app_colors.white else style.iconLight
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
        if (place.description.isNotBlank()) {
            Text(
                place.description,
                style = jakartaText(11.sp, 16.5.sp, app_colors.placeInk, FontWeight.Medium),
                modifier = Modifier.fillMaxWidth().padding(bottom = if (place.schedule.isNotEmpty() || place.notes.isNotEmpty()) 10.dp else 0.dp)
            )
        }
        if (place.schedule.isNotEmpty()) {
            Text(stringResource(R.string.place_schedule_sheet_title), style = jakartaText(11.sp, 16.sp, app_colors.placeSlateLight, FontWeight.SemiBold))
            Spacer(Modifier.height(6.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(app_colors.white).padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                place.schedule.forEachIndexed { i, s ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(app_colors.profileCardBorder))
                    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(s.days, style = jakartaText(11.sp, 16.sp, app_colors.helpBody, FontWeight.SemiBold), modifier = Modifier.width(96.dp))
                        Text(
                            s.hours.replaceFirstChar { it.uppercase() },
                            style = jakartaText(11.sp, 16.sp, if (s.hours == "cerrado") app_colors.placeSlateLight else app_colors.placeInk, FontWeight.Medium, textAlign = TextAlign.End),
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
                        Text(note, style = jakartaText(11.sp, 16.sp, app_colors.placeSlate, FontWeight.Medium))
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
@Composable
private fun Place.cardBadges(): List<String> = buildList {
    if (distanceM <= 600) add(stringResource(R.string.place_badge_near_u))
    add(stringResource(if (study) R.string.place_badge_study else R.string.place_badge_lively))
    if (solo) add(stringResource(R.string.place_badge_solo))
    if (group) add(stringResource(R.string.place_badge_group))
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
            .dropShadow(shape, Shadow(radius = 16.dp, color = app_colors.shadowSoft06, offset = DpOffset(0.dp, 4.dp)))
            .clip(shape)
            .background(app_colors.white)
            .border(1.dp, app_colors.profileCardBorder, shape)
            .clickable(onClick = onClick)
    ) {
        // Foto con el nombre encima
        Box(Modifier.fillMaxWidth().height(176.dp).background(app_colors.homeTitle)) {
            SceneImage(place.photo, Modifier.fillMaxSize(), crop = true)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0f to app_colors.shadowSoft10, 0.45f to app_colors.shadowSoft20, 1f to app_colors.scrimDark80)
                )
            )
            HeroCategoryBadge(stringResource(place.category.singularRes), Modifier.align(Alignment.TopStart).padding(12.dp))
            val context = LocalContext.current
            val favorites = data.rememberFavorites()
            val isFav = place.id in favorites
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(app_colors.scrimDark60)
                    .clickable { data.FavoriteStore.toggle(context, place.id) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = stringResource(if (isFav) R.string.home_favorite_remove_cd else R.string.home_favorite_add_cd),
                    tint = if (isFav) app_colors.placeHeartActive else app_colors.white,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                Text(
                    place.name,
                    style = jakartaText(18.sp, 22.5.sp, app_colors.white, FontWeight.ExtraBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(place.address, style = jakartaText(11.sp, 15.13.sp, app_colors.formBorder, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.place_card_distance_walk, place.distance, place.walkMin),
                    style = jakartaText(11.sp, 15.13.sp, app_colors.formBorder, FontWeight.Medium)
                )
            }
        }
        // Detalles
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (t in place.cardBadges()) Chip(t, style)
                if (place.nequi) NequiChip(style)
            }
            Box(Modifier.padding(vertical = 12.dp).fillMaxWidth().height(1.dp).background(app_colors.profileCardBorder))
            val priceSuffix = stringResource(if (place.priceVaries) R.string.place_card_price_varies_suffix else R.string.place_card_price_range_suffix)
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = style.title)) { append(place.price) }
                    withStyle(SpanStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, color = app_colors.placeSlate)) {
                        append(priceSuffix)
                    }
                },
                style = jakartaText(15.sp, 20.sp, app_colors.placeNavy)
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = app_colors.placeSlate, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (place.hoursVaries) stringResource(R.string.place_card_hours_varies_format, place.hours) else place.hours,
                    style = jakartaText(12.sp, 16.sp, app_colors.placeSlate, FontWeight.Medium),
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
        Text(stringResource(R.string.place_nequi_chip), style = jakartaText(11.sp, 16.5.sp, style.chipText, style.chipWeight))
    }
}
