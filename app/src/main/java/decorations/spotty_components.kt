package decorations

import android.content.Context
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.u_spot.R
import data.HomeCategory
import data.Place
import data.spotty.SpottyBudget
import data.spotty.SpottyCard
import data.spotty.SpottyChatSummary
import data.spotty.SpottyProblem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// =============================================================================================
// Spotty (chatbot). Medidas y colores de Figma: "Home App (Vista IA)" 444:32 (bienvenida),
// 579:369 y 593:11 (conversación), 674:4 (favorito guardado). Esquema en figma_export/spotty/*.txt.
// El panel de historial y el diálogo de borrado no existen en Figma: siguen el mismo estilo.
// =============================================================================================

/** Fondo de todas las pantallas de Spotty: azul arriba y verde azulado desde la mitad. */
val spottyBackground: Brush = Brush.verticalGradient(
    0f to app_colors.spottyGradTop,
    0.48f to app_colors.spottyGradBottom,
    1f to app_colors.spottyGradBottom
)

/** Alto del encabezado fijo de la conversación (TopHeader de 579:369). */
val SPOTTY_HEADER_HEIGHT = 143.dp

private val userShape = RoundedCornerShape(topStart = 16.dp, topEnd = 2.dp, bottomEnd = 16.dp, bottomStart = 16.dp)

// Estilo Glass para las respuestas de Spotty (translúcido, borde suave iluminado y sin contorno negro)
private val glassBubbleShape = RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 20.dp)
private val glassPillShape = RoundedCornerShape(12.dp)

private val glassBubbleBrush = Brush.verticalGradient(
    colors = listOf(
        app_colors.whiteAlpha35, // ~35% blanco translúcido arriba
        app_colors.whiteAlpha20  // ~20% blanco translúcido abajo
    )
)

private val glassBorderBrush = Brush.verticalGradient(
    colors = listOf(
        app_colors.whiteAlpha70, // ~70% blanco para el reflejo superior de cristal
        app_colors.whiteAlpha30  // ~30% blanco en la parte inferior
    )
)

private fun Modifier.glassShadow(shape: androidx.compose.ui.graphics.Shape) = this
    .dropShadow(shape, Shadow(radius = 16.dp, spread = (-2).dp, color = app_colors.shadowSpottyGlass12, offset = DpOffset(0.dp, 6.dp)))
    .dropShadow(shape, Shadow(radius = 4.dp, color = app_colors.shadowSpottyGlass07, offset = DpOffset(0.dp, 2.dp)))

private fun jakarta(size: Int, line: Float, color: Color, weight: Int = 400, spacing: Float = 0f, align: TextAlign = TextAlign.Start) =
    figmaText(app_fonts.plusJakarta, size.sp, line.sp, color, FontWeight(weight), spacing.sp, align)

/** "7:02 PM", igual que en Figma. */
fun spottyClock(time: Long): String = SimpleDateFormat("h:mm a", Locale.US).format(Date(time))

// ---------------------------------------------------------------------------------------------
// Encabezado: logo de U-Spot y esquina con el botón de historial
// ---------------------------------------------------------------------------------------------

/**
 * Logo de los frames de Spotty (Frame 10 / Frame 11): cuadro blanco con la U y el pin, "U-Spot"
 * y el lema en amarillo. Mide 270 x 91; las piezas están en sus posiciones de Figma.
 */
@Composable
fun SpottyLogo(modifier: Modifier = Modifier) {
    Box(modifier.size(270.dp, 91.dp)) {
        SceneImage("616b16dcec18289e.png", Modifier.offset(0.dp, 8.dp).size(86.dp, 83.dp))
        SceneImage("22685b4d86fe5a8f.png", Modifier.offset(18.5.dp, 27.5.dp).size(49.dp, 50.dp))
        SceneImage("2086daf58c3ecf4f.png", Modifier.offset(48.1.dp, 12.76.dp).size(26.61.dp, 35.56.dp))
        SceneImage("4536873abe546f40.png", Modifier.offset(56.dp, 21.dp).size(11.dp, 11.dp))
        SceneImage("2cc649d8176e3306.png", Modifier.offset(97.6.dp, 17.92.dp).size(155.47.dp, 48.22.dp))
        SceneImage("fcd2ec43a7ab3369.png", Modifier.offset(129.dp, 33.5.dp).size(11.dp, 6.5.dp))
        Text(
            stringResource(R.string.spotty_logo_tagline),
            style = figmaText(app_fonts.poppins, 11.sp, 17.sp, app_colors.spottyAmber, FontWeight.Bold),
            modifier = Modifier.offset(96.dp, 66.dp),
            maxLines = 1
        )
    }
}

/**
 * Esquina superior derecha con el botón de historial. En la bienvenida es la mancha verde de
 * 444:32 (donde Figma tenía ✨) y en la conversación el cuarto de círculo azul de 579:369
 * (donde tenía 🏠). El ícono de dos burbujas es el del campo de la bienvenida en Figma.
 */
@Composable
fun BoxScope.SpottyHistoryCorner(welcome: Boolean, onHistory: () -> Unit, topOffset: Dp = 0.dp) {
    val historyCd = stringResource(R.string.spotty_history_cd)
    if (welcome) {
        SceneImage("d16b0e4fc231ebc1.png", Modifier.figmaPosition(left = 291.36.dp, top = topOffset, width = 120.64.dp, height = 86.63.dp))
    } else {
        Box(
            Modifier
                .figmaPosition(left = 304.dp, top = (-18).dp, width = 123.dp, height = 123.dp + topOffset)
                .background(app_colors.spottyCornerBlue, RoundedCornerShape(bottomStart = 90.dp))
        )
    }
    Box(
        Modifier
            .figmaPosition(
                left = if (welcome) 344.dp else 347.dp,
                top = (if (welcome) 32.dp else 38.dp) + topOffset,
                width = 50.dp,
                height = 50.dp
            )
            .clip(CircleShape)
            .clickable(onClick = onHistory, role = Role.Button, onClickLabel = historyCd)
            .semantics { contentDescription = historyCd },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_spotty_sparkles_header),
            contentDescription = historyCd,
            modifier = Modifier.size(46.dp)
        )
    }
}

/** Encabezado fijo de la conversación (TopHeader de 579:369): fondo, logo y esquina con respeto a la barra de estado. */
@Composable
fun SpottyChatHeader(onHistory: () -> Unit, modifier: Modifier = Modifier) {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        modifier
            .fillMaxWidth()
            .height(SPOTTY_HEADER_HEIGHT + statusBar)
            .background(app_colors.spottyHeader)
    ) {
        SpottyLogo(Modifier.offset(16.dp, 39.dp + statusBar))
        SpottyHistoryCorner(welcome = false, onHistory = onHistory, topOffset = statusBar)
    }
}

/** Etiqueta del modelo ("Background+Border" 444:699): 26 de alto, borde turquesa, ✨ ámbar. */
@Composable
fun SpottyModelBadge(label: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .height(26.dp)
            .background(Brush.horizontalGradient(listOf(app_colors.spottyBadgeGradStart, app_colors.spottyBadgeGradEnd)), shape)
            .border(1.dp, app_colors.spottyChipBorderFirst.copy(alpha = 0.7f), shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = app_colors.spottyBadgeIcon, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = jakarta(12, 16f, app_colors.white, 600), maxLines = 1)
    }
}

// ---------------------------------------------------------------------------------------------
// Mensajes
// ---------------------------------------------------------------------------------------------

/** Respuesta de texto de Spotty con estilo Glass (translúcido, reflejo suave y sin contorno negro). */
@Composable
fun SpottyTextBubble(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(start = 31.dp, end = 16.dp)) {
        Box(
            modifier = Modifier
                .widthIn(max = 306.dp)
                .glassShadow(glassBubbleShape)
                .clip(glassBubbleShape)
                .background(glassBubbleBrush)
                .border(1.2.dp, glassBorderBrush, glassBubbleShape)
                .padding(15.dp)
        ) {
            Text(
                text = text,
                style = jakarta(15, 22f, app_colors.white, 500)
            )
        }
    }
}

/** Mensaje del usuario ("User Query Bubble" 444:741): azul, alineado a la derecha. */
@Composable
fun SpottyUserBubble(text: String, time: Long, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(start = 60.dp, end = 16.dp), horizontalAlignment = Alignment.End) {
        Text(
            text,
            style = jakarta(14, 19.25f, app_colors.white, 500),
            modifier = Modifier
                .widthIn(max = 277.dp)
                .dropShadow(userShape, Shadow(radius = 4.dp, spread = (-2).dp, color = app_colors.shadowSoft10, offset = DpOffset(0.dp, 2.dp)))
                .dropShadow(userShape, Shadow(radius = 6.dp, spread = (-1).dp, color = app_colors.shadowSoft10, offset = DpOffset(0.dp, 4.dp)))
                .background(app_colors.spottyUserBubble, userShape)
                .padding(14.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.spotty_user_time_format, spottyClock(time)), style = jakarta(11, 15f, app_colors.white), modifier = Modifier.padding(end = 4.dp))
    }
}

/** "Reasoning Pill" (444:758): resumen de lo que analizó Spotty en estilo glass. */
@Composable
fun SpottyAnalysisPill(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(start = 31.dp, end = 16.dp)) {
        Row(
            Modifier
                .widthIn(max = 300.dp)
                .heightIn(min = 43.dp)
                .glassShadow(glassPillShape)
                .clip(glassPillShape)
                .background(glassBubbleBrush)
                .border(1.2.dp, glassBorderBrush, glassPillShape)
                .padding(start = 13.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).background(app_colors.spottyPillDotHalo, CircleShape))
            Spacer(Modifier.width(16.dp))
            Text(text, style = jakarta(12, 17f, app_colors.white, 700))
        }
    }
}

/** Pregunta de seguimiento y hora en ámbar ("Follow-up prompt hint" 446:1339). */
@Composable
fun SpottyFollowUp(text: String?, time: Long, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(start = 35.dp, end = 20.dp)) {
        if (text != null) {
            Text(text, style = jakarta(13, 19f, app_colors.white, 600))
            Spacer(Modifier.height(11.dp))
        }
        Text(stringResource(R.string.spotty_bot_time_format, spottyClock(time)), style = jakarta(12, 15f, app_colors.spottyAmber))
    }
}

/** Spotty está pensando: tres puntos que laten dentro de una burbuja glass. */
@Composable
fun SpottyTypingBubble(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typing")
    Box(modifier.fillMaxWidth().padding(start = 31.dp)) {
        Row(
            Modifier
                .glassShadow(glassBubbleShape)
                .clip(glassBubbleShape)
                .background(glassBubbleBrush)
                .border(1.2.dp, glassBorderBrush, glassBubbleShape)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { i ->
                val a by transition.animateFloat(
                    initialValue = 0.35f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(500, delayMillis = i * 160), RepeatMode.Reverse),
                    label = "dot$i"
                )
                Box(Modifier.size(8.dp).alpha(a).background(app_colors.white, CircleShape))
            }
        }
    }
}

/** Aviso en lugar de la respuesta cuando algo falla (sin internet, cuota, clave, error). */
@Composable
fun SpottyProblemBubble(problem: SpottyProblem, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    val (title, body) = when (problem) {
        SpottyProblem.OFFLINE -> stringResource(R.string.spotty_problem_offline_title) to stringResource(R.string.spotty_problem_offline_body)
        SpottyProblem.QUOTA -> stringResource(R.string.spotty_problem_quota_title) to stringResource(R.string.spotty_problem_quota_body)
        SpottyProblem.NO_KEY -> stringResource(R.string.spotty_problem_no_key_title) to stringResource(R.string.spotty_problem_no_key_body)
        SpottyProblem.GENERIC -> stringResource(R.string.spotty_problem_generic_title) to stringResource(R.string.spotty_problem_generic_body)
    }
    Box(modifier.fillMaxWidth().padding(start = 31.dp, end = 16.dp)) {
        Column(
            Modifier
                .widthIn(max = 306.dp)
                .glassShadow(glassBubbleShape)
                .clip(glassBubbleShape)
                .background(glassBubbleBrush)
                .border(1.2.dp, app_colors.spottyAmber.copy(alpha = 0.85f), glassBubbleShape)
                .padding(15.dp)
        ) {
            Text(title, style = jakarta(14, 20f, app_colors.spottyAmber, 700))
            Spacer(Modifier.height(4.dp))
            Text(body, style = jakarta(14, 20f, app_colors.white, 500))
            if (onRetry != null) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(app_colors.spottyPrimary)
                        .clickable(onClick = onRetry, role = Role.Button)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = app_colors.white, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.common_retry), style = jakarta(12, 16f, app_colors.white, 600))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Chips de sugerencias
// ---------------------------------------------------------------------------------------------

/** Filtros rápidos bajo el saludo ("Quick Filter Suggestion Chips" 444:724): blancos, redondos. */
@Composable
fun SpottyQuickChips(chips: List<String>, onClick: (String) -> Unit, modifier: Modifier = Modifier, start: Dp = 60.dp) {
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = start, end = 16.dp, top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEachIndexed { i, chip ->
            val shape = RoundedCornerShape(50)
            Text(
                chip,
                style = jakarta(12, 16f, app_colors.spottyChipText, 500),
                maxLines = 1,
                modifier = Modifier
                    .height(30.dp)
                    .dropShadow(shape, Shadow(radius = 2.dp, color = app_colors.shadowSoft05, offset = DpOffset(0.dp, 1.dp)))
                    .clip(shape)
                    .background(app_colors.white)
                    .border(1.dp, if (i == 0) app_colors.spottyChipBorderFirst else app_colors.spottyChipBorder, shape)
                    .clickable(role = Role.Button) { onClick(chip) }
                    .padding(horizontal = 13.dp, vertical = 7.dp)
            )
        }
    }
}

/** Respuestas rápidas sobre la barra de escritura ("Suggested quick follow-up chips" 446:1154). */
@Composable
private fun SpottyFollowUpChips(chips: List<String>, onClick: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        chips.forEach { chip ->
            Text(
                chip,
                style = jakarta(11, 16.5f, app_colors.black, 500),
                maxLines = 1,
                modifier = Modifier
                    .height(25.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(app_colors.white)
                    .clickable(role = Role.Button) { onClick(chip) }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Tarjeta de lugar ("Article - Event Card" 444:767)
// ---------------------------------------------------------------------------------------------

private val HomeCategory.emoji: String
    get() = when (this) {
        HomeCategory.RESTAURANTES -> "🍽️"
        HomeCategory.CAFES -> "☕"
        HomeCategory.BARES -> "🍻"
        HomeCategory.TEATROS -> "🎭"
        HomeCategory.OCIO -> "🎳"
        HomeCategory.MUSEOS -> "🏛️"
        HomeCategory.DISCOTECAS -> "💃"
        HomeCategory.LIBRERIAS -> "📚"
    }

/** Colores de fondo y texto de las etiquetas de la tarjeta (los seis de Figma). */
private enum class CardChipColor(val bg: Color, val fg: Color) {
    TEAL(app_colors.spottyCardChipTealBg, app_colors.spottyCardChipTealFg),
    BLUE(app_colors.spottyCardChipBlueBg, app_colors.spottyCardChipBlueFg),
    SLATE(app_colors.spottyCardChipSlateBg, app_colors.spottyCardChipSlateFg),
    AMBER(app_colors.spottyCardChipAmberBg, app_colors.spottyCardChipAmberFg),
    PURPLE(app_colors.spottyCardChipPurpleBg, app_colors.spottyCardChipPurpleFg),
    GREEN(app_colors.spottyCardChipGreenBg, app_colors.spottyCardChipGreenFg)
}

/** Hasta tres etiquetas con datos reales: categoría, un rasgo y el nivel de precio. */
@Composable
private fun Place.cardChips(): List<Pair<String, CardChipColor>> = buildList {
    // Los cines están en la categoría teatros, pero se muestran como lo que son
    val cinema = category == HomeCategory.TEATROS && Regex("\\bcine").containsMatchIn(name.lowercase())
    add((if (cinema) stringResource(R.string.spotty_chip_cinema) else "${category.emoji} ${stringResource(category.singularRes)}") to CardChipColor.TEAL)
    when {
        group -> add(stringResource(R.string.spotty_chip_groups) to CardChipColor.PURPLE)
        nequi -> add(stringResource(R.string.spotty_chip_nequi) to CardChipColor.GREEN)
        study -> add(stringResource(R.string.spotty_chip_study) to CardChipColor.BLUE)
        solo -> add(stringResource(R.string.spotty_chip_solo) to CardChipColor.AMBER)
    }
    val min = SpottyBudget.pricePerPerson(this@cardChips)?.first
    if (min != null) {
        val priceText = when {
            min < 15000 -> stringResource(R.string.spotty_chip_price_low)
            min < 35000 -> stringResource(R.string.spotty_chip_price_mid)
            else -> stringResource(R.string.spotty_chip_price_high)
        }
        add(priceText to CardChipColor.SLATE)
    }
}

/**
 * Foto propia del lugar. Los 16 lugares con pantalla de Figma no traen foto en places.json: se
 * toma la foto de la cabecera de su pantalla, así ninguna tarjeta repite la foto genérica de la
 * categoría. Encima de la foto Figma pone una capa de degradado del mismo tamaño ("Gradient",
 * "Dark Gradient Overlay…"): esas capas se descartan para no tomar el degradado como foto.
 */
@Composable
fun rememberPlacePhoto(place: Place): String {
    val figma = place.figma ?: return place.photo
    val scene = rememberScene(figma.sceneKey) ?: return place.photo
    return remember(scene) {
        scene.allNodes {
            it is SceneNode.Picture && it.y < 300f && it.w >= 300f &&
                !it.name.contains("gradient", ignoreCase = true) && !it.name.contains("overlay", ignoreCase = true)
        }
            .maxByOrNull { it.w * it.h }
            ?.let { (it as SceneNode.Picture).src }
            ?: place.photo
    }
}

/** Tarjeta de lugar recomendado por Spotty con estilo Glass (translúcido, reflejo superior suave y sin contorno negro). */
@Composable
fun SpottyPlaceCard(
    place: Place,
    card: SpottyCard,
    isFavorite: Boolean,
    primary: Boolean,
    onDetails: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val favLabel = stringResource(if (isFavorite) R.string.home_favorite_remove_cd else R.string.home_favorite_add_cd)
    Box(modifier.fillMaxWidth().padding(start = 31.dp, end = 16.dp)) {
        Box(
            Modifier
                .width(306.dp)
                .glassShadow(shape)
                .clip(shape)
                .background(glassBubbleBrush)
                .clickable(onClick = onDetails)
        ) {
            Column(Modifier.fillMaxWidth()) {
                // Foto con degradado, insignias, corazón, nombre y dirección (112 de alto)
                Box(Modifier.fillMaxWidth().height(112.dp).background(app_colors.spottyPhotoBg)) {
                    // key(): SceneImage conserva la primera imagen que pinta; al llegar la foto de la
                    // pantalla de Figma (tras la genérica de la categoría) hay que recrearla
                    val photo = rememberPlacePhoto(place)
                    key(photo) { SceneImage(photo, Modifier.fillMaxSize(), crop = true) }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(0f to app_colors.shadowSoft10, 0.45f to app_colors.shadowSoft20, 1f to app_colors.scrimDark85))
                    )
                    Row(Modifier.offset(8.dp, 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (card.match > 0) Row(
                            Modifier.height(19.dp).background(app_colors.spottyMatch, CircleShape).padding(start = 8.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SceneImage("7f18ce5dc44f11bb.png", Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.spotty_match_format, card.match), style = jakarta(10, 15f, app_colors.white, 700), maxLines = 1)
                        }
                        if (card.distance != null) {
                            Text(
                                card.distance,
                                style = jakarta(10, 15f, app_colors.white, 700),
                                maxLines = 1,
                                modifier = Modifier.height(19.dp).background(app_colors.spottyDistance, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset((-8).dp, 8.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(app_colors.spottyFavBtnBg.copy(alpha = 0.85f))
                            .clickable(onClick = onFavorite, role = Role.Button, onClickLabel = favLabel),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite) app_colors.spottyAmber else app_colors.white,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column(Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 12.dp, bottom = 8.dp)) {
                        Text(
                            place.name,
                            style = jakarta(14, 20f, app_colors.white, 700, -0.35f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${place.address} • ${place.hours}",
                            style = jakarta(11, 16.5f, app_colors.spottySubtitle, 500),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                // Cuerpo: etiquetas, línea divisoria, precio y "Ver detalles" (90 de alto)
                Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        place.cardChips().forEach { (text, color) ->
                            Text(
                                text,
                                style = jakarta(10, 15f, color.fg, 600),
                                maxLines = 1,
                                modifier = Modifier.height(19.dp).background(color.bg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(app_colors.whiteAlpha20))
                    Row(Modifier.fillMaxWidth().height(37.dp).padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = app_colors.white.copy(alpha = 0.85f))) { append("${card.priceLabel}: ") }
                                withStyle(SpanStyle(color = app_colors.white, fontWeight = FontWeight.Bold)) { append(card.priceValue) }
                            },
                            style = jakarta(11, 16.5f, app_colors.white, 500),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        val btnShape = RoundedCornerShape(12.dp)
                        Text(
                            stringResource(R.string.spotty_view_details),
                            style = jakarta(12, 16f, if (primary) app_colors.white else app_colors.spottyPrimary, if (primary) 600 else 700),
                            modifier = Modifier
                                .height(28.dp)
                                .then(if (primary) Modifier.dropShadow(btnShape, Shadow(radius = 2.dp, color = app_colors.shadowSoft05, offset = DpOffset(0.dp, 1.dp))) else Modifier)
                                .clip(btnShape)
                                .background(if (primary) app_colors.spottyPrimary else app_colors.spottySecondaryBtn)
                                .clickable(onClick = onDetails, role = Role.Button)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            // Borde glass iluminado por encima de toda la tarjeta (incluso sobre la foto superior)
            Box(
                Modifier
                    .matchParentSize()
                    .border(1.2.dp, glassBorderBrush, shape)
            )
        }
    }
}

/** Botón redondo para bajar al último mensaje (degradado turquesa → azul de Figma). */
@Composable
fun SpottyScrollDownButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(40.dp)
            .dropShadow(CircleShape, Shadow(radius = 6.dp, color = app_colors.shadowSoft20, offset = DpOffset(0.dp, 3.dp)))
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(app_colors.spottyScrollGradStart, app_colors.spottyScrollGradEnd)))
            .clickable(onClick = onClick, role = Role.Button, onClickLabel = stringResource(R.string.spotty_scroll_down_cd)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = app_colors.white, modifier = Modifier.size(26.dp))
    }
}

// ---------------------------------------------------------------------------------------------
// Escritura: campo de la bienvenida y barra de la conversación
// ---------------------------------------------------------------------------------------------

/**
 * Campo de la bienvenida ("Search Bar" 444:62): caja verde oscura de 356 x 96 con el texto, la
 * etiqueta del modelo y la flecha de enviar. Sin el "+" de adjuntar (fuera de alcance).
 */
@Composable
fun SpottyWelcomeInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modelLabel: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(15.dp)
    val sendLabel = stringResource(R.string.common_send)
    val sendCd = stringResource(R.string.common_send_message_cd)
    Box(
        modifier
            .size(356.dp, 96.dp)
            .background(app_colors.spottyWelcomeInput, shape)
            .border(1.dp, app_colors.borderSubtle03, shape)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = figmaText(app_fonts.inter, 14.sp, 17.sp, app_colors.white, FontWeight.Medium),
            cursorBrush = SolidColor(app_colors.spottyAmber),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            maxLines = 2,
            modifier = Modifier.offset(18.dp, 14.dp).width(316.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(stringResource(R.string.spotty_welcome_input_placeholder), style = figmaText(app_fonts.inter, 14.sp, 17.sp, app_colors.white, FontWeight.Medium))
                }
                inner()
            }
        )
        SpottyModelBadge(modelLabel, Modifier.offset(18.dp, 57.dp))
        Box(
            Modifier
                .offset(302.dp, 50.dp)
                .size(40.dp)
                .clip(CircleShape)
                .clickable(enabled = value.isNotBlank(), onClick = onSend, role = Role.Button, onClickLabel = sendLabel)
                .semantics { contentDescription = sendCd },
            contentAlignment = Alignment.Center
        ) {
            SceneImage("34bf19e525a99cc3.png", Modifier.size(40.dp).alpha(if (value.isNotBlank()) 1f else 0.6f))
        }
    }
}

/**
 * Zona inferior de la conversación ("ChatInputArea" 446:1153): franja verde con las respuestas
 * rápidas, el campo blanco redondo y el botón de enviar con degradado. Sin "+" ni micrófono.
 */
@Composable
fun SpottyInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    suggestions: List<String>,
    onSuggestion: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val sendLabel = stringResource(R.string.common_send)
    val sendCd = stringResource(R.string.common_send_message_cd)
    Column(modifier.fillMaxWidth().background(app_colors.spottyInputBand).padding(top = 8.dp)) {
        if (suggestions.isNotEmpty()) {
            SpottyFollowUpChips(suggestions, onSuggestion)
            Spacer(Modifier.height(16.dp))
        } else {
            Spacer(Modifier.height(4.dp))
        }
        Row(Modifier.fillMaxWidth().padding(start = 25.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(app_colors.white)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = jakarta(12, 16f, app_colors.black),
                    cursorBrush = SolidColor(app_colors.spottyPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (enabled) onSend() }),
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (value.isEmpty()) Text(stringResource(R.string.spotty_chat_input_placeholder), style = jakarta(12, 16f, app_colors.spottyTimeGray))
                        inner()
                    }
                )
            }
            Box(
                Modifier
                    .size(51.dp, 50.dp)
                    .clip(CircleShape)
                    .clickable(enabled = enabled && value.isNotBlank(), onClick = onSend, role = Role.Button, onClickLabel = sendLabel)
                    .semantics { contentDescription = sendCd },
                contentAlignment = Alignment.Center
            ) {
                SceneImage("e93de06e24f5d8bc.png", Modifier.size(51.dp, 50.dp).alpha(if (enabled && value.isNotBlank()) 1f else 0.55f))
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

// ---------------------------------------------------------------------------------------------
// Ventanita de favorito (674:4) y diálogo de borrado (estilo de los diálogos de U-Spot)
// ---------------------------------------------------------------------------------------------

/** "GUARDADO FAVORITOS VENTANITA" (601:486): blanca, borde ámbar, estrella y X. */
@Composable
fun SpottyFavoriteToast(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(25.dp)
    Box(
        modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 20.dp)
            .size(360.dp, 87.dp)
            .background(app_colors.white, shape)
            .border(3.dp, app_colors.spottyAmber, shape)
    ) {
        SceneImage("2bae237ad14d1a94.png", Modifier.offset(12.dp, 20.dp).size(40.dp))
        Text(
            stringResource(R.string.spotty_favorite_toast),
            style = figmaText(app_fonts.poppins, 18.sp, 20.sp, app_colors.black, textAlign = TextAlign.Center),
            modifier = Modifier.offset(56.dp, 17.dp).size(274.dp, 57.dp).padding(top = 18.dp)
        )
        Box(
            Modifier
                .offset(323.dp, 0.dp)
                .size(37.dp)
                .clip(CircleShape)
                .clickable(onClick = onClose, role = Role.Button, onClickLabel = stringResource(R.string.common_close)),
            contentAlignment = Alignment.Center
        ) { SceneImage("522c4c7082a7e6a6.png", Modifier.size(18.dp)) }
    }
}

/**
 * "¿Deseas eliminar este chat?": mismo diálogo que "¿Deseas cerrar tu sesión?" del Home
 * (tarjeta 360 de ancho, borde verde, manchas de color en dos esquinas, VOLVER en ámbar y la
 * acción en rojo), sobre la pantalla aclarada al 70 %.
 */
@Composable
fun SpottyDeleteDialog(chatTitle: String, onCancel: () -> Unit, onDelete: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(app_colors.white.copy(alpha = 0.7f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCancel),
        contentAlignment = Alignment.Center
    ) {
        val shape = RoundedCornerShape(40.dp)
        Box(
            Modifier
                .size(360.dp, 360.dp)
                .clip(shape)
                .background(app_colors.spottyDialogBg)
                .border(2.dp, app_colors.spottyDialogBorder, shape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
        ) {
            SceneImage("1d1847feffd40384.png", Modifier.align(Alignment.TopEnd).size(80.81.dp, 76.65.dp))
            SceneImage("0ab1b41dd6d18fa3.png", Modifier.align(Alignment.BottomStart).size(75.39.dp, 113.57.dp))
            Column(
                Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SceneImage("42a19d5a3561fe7c.png", Modifier.size(38.dp, 42.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.spotty_delete_chat_title), style = figmaText(app_fonts.poppins, 20.sp, 24.sp, app_colors.black))
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "“$chatTitle”",
                    style = figmaText(app_fonts.poppins, 15.sp, 20.sp, app_colors.spottyChipText, textAlign = TextAlign.Center),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.spotty_delete_chat_subtitle),
                    style = figmaText(app_fonts.poppins, 13.sp, 18.sp, app_colors.spottyPanelSection, textAlign = TextAlign.Center)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(R.string.common_back_uppercase),
                    style = figmaText(app_fonts.poppins, 18.sp, 20.sp, app_colors.black, textAlign = TextAlign.Center),
                    modifier = Modifier
                        .size(115.dp, 40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(app_colors.spottyAmber)
                        .clickable(onClick = onCancel, role = Role.Button)
                        .padding(top = 10.dp)
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    stringResource(R.string.spotty_delete_chat_action),
                    style = figmaText(app_fonts.poppins, 18.sp, 20.sp, app_colors.spottyDanger, textAlign = TextAlign.Center),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onDelete, role = Role.Button)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Panel de historial (no existe en Figma; sale desde la derecha, donde está su botón)
// ---------------------------------------------------------------------------------------------

private fun dayBucket(context: Context, time: Long, now: Long = System.currentTimeMillis()): String {
    val start = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val day = 24L * 60 * 60 * 1000
    return when {
        time >= start -> context.getString(R.string.spotty_bucket_today)
        time >= start - day -> context.getString(R.string.spotty_bucket_yesterday)
        time >= start - 7 * day -> context.getString(R.string.spotty_bucket_last_7_days)
        else -> context.getString(R.string.spotty_bucket_older)
    }
}

private fun chatDate(context: Context, time: Long): String {
    val today = dayBucket(context, time) == context.getString(R.string.spotty_bucket_today)
    return if (today) spottyClock(time) else SimpleDateFormat("d MMM", Locale.forLanguageTag("es-CO")).format(Date(time)).replace(".", "")
}

@Composable
fun SpottyHistoryPanel(
    chats: List<SpottyChatSummary>,
    currentChatId: String?,
    onNewChat: () -> Unit,
    onOpen: (SpottyChatSummary) -> Unit,
    onDelete: (SpottyChatSummary) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
    val deleteChatLabel = stringResource(R.string.spotty_delete_chat_action)
    Column(
        modifier
            .fillMaxSize()
            .dropShadow(shape, Shadow(radius = 24.dp, color = app_colors.shadowSoft20, offset = DpOffset((-4).dp, 0.dp)))
            .clip(shape)
            .background(app_colors.spottyPanelBg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 10.dp, top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            SceneImage("09e6a5010822005b.png", Modifier.size(34.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.spotty_history_title), style = jakarta(18, 24f, app_colors.spottyGreetingTitle, 800), modifier = Modifier.weight(1f))
            Box(
                Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClose, role = Role.Button, onClickLabel = stringResource(R.string.spotty_history_close_cd)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Close, contentDescription = null, tint = app_colors.spottyPanelSection) }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(listOf(app_colors.spottyScrollGradStart, app_colors.spottyPrimary)))
                .clickable(onClick = onNewChat, role = Role.Button),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = app_colors.spottyAmber, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.spotty_new_chat), style = jakarta(15, 20f, app_colors.white, 700))
        }
        Spacer(Modifier.height(12.dp))
        if (chats.isEmpty()) {
            Text(
                stringResource(R.string.spotty_history_empty),
                style = jakarta(13, 19f, app_colors.spottyPanelSection, align = TextAlign.Center),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 32.dp)
            )
            return@Column
        }
        val grouped = remember(chats, context) { chats.sortedByDescending { it.updatedAt }.groupBy { dayBucket(context, it.updatedAt) } }
        LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
            grouped.forEach { (bucket, list) ->
                item(key = "h_$bucket") {
                    Text(
                        bucket.uppercase(),
                        style = jakarta(11, 16f, app_colors.spottyPanelSection, 700, 0.6f),
                        modifier = Modifier.padding(start = 24.dp, top = 14.dp, bottom = 4.dp)
                    )
                }
                items(list, key = { it.id }) { chat ->
                    val selected = chat.id == currentChatId
                    Row(
                        Modifier
                            .padding(horizontal = 12.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) app_colors.spottyPanelSelected else Color.Transparent)
                            .clickable { onOpen(chat) }
                            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                chat.title,
                                style = jakarta(14, 19f, if (selected) app_colors.spottyPrimary else app_colors.spottyBody, 600),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(chatDate(context, chat.updatedAt), style = jakarta(11, 15f, app_colors.spottyTimeGray))
                        }
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = deleteChatLabel) { onDelete(chat) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = app_colors.spottyTimeGray, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Ancho del panel de historial. */
val SPOTTY_PANEL_WIDTH: Dp = 320.dp
