package decorations

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.windowInsetsBottomHeight
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
import androidx.compose.ui.res.painterResource
import com.example.u_spot.R
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

private object SpottyColors {
    val gradTop = Color(0xFF608ACF)
    val gradBottom = Color(0xFF3F8678)
    val header = Color(0xFF5488B0)
    val cornerBlue = Color(0xFF1363D5)
    val bubble = Color(0xFF87B8AE)
    val bubbleBorder = Color(0xFFF1F5F9)
    // Contorno negro de las burbujas y tarjetas de Spotty (más legible sobre el fondo verde)
    val outline = Color(0xFF000000)
    val bubbleShadow1 = Color(0x0A0D56CF)
    val bubbleShadow2 = Color(0x120D56CF)
    val greetingTitle = Color(0xFF0A2540)
    val body = Color(0xFF1E293B)
    val timeGray = Color(0xFF94A3B8)
    val amber = Color(0xFFFBBF24)
    val userBubble = Color(0xFF0D56CF)
    val chipText = Color(0xFF334155)
    val chipBorder = Color(0xFFE2E8F0)
    val chipBorderFirst = Color(0xFF99F6E4)
    val pillBorder = Color(0xB299F6E4)
    val pillDot = Color(0xFF14B8A6)
    val pillDotHalo = Color(0xFF2DD4BF)
    val pillText = Color(0xFF115E59)
    val match = Color(0xE510B981)
    val distance = Color(0xFF14B8A6)
    val subtitle = Color(0xFF99F6E4)
    val photoBg = Color(0xFF1E293B)
    val priceLabel = Color(0xFF475569)
    val primary = Color(0xFF0D56CF)
    val secondaryBtn = Color(0xFFF1F5F9)
    val inputBand = Color(0xFF3F8578)
    val welcomeInput = Color(0xFF274E47)
    val nav = Color(0xED0157D2)
    val navLabel = Color(0xFFFBBF24)
    val dialogBg = Color(0xFFFBF9F9)
    val dialogBorder = Color(0xFF084B42)
    val danger = Color(0xFFFF0000)
    val panelBg = Color(0xFFFFFFFF)
    val panelSelected = Color(0xFFEFF6FF)
    val panelSection = Color(0xFF64748B)
}

/** Fondo de todas las pantallas de Spotty: azul arriba y verde azulado desde la mitad. */
val spottyBackground: Brush = Brush.verticalGradient(
    0f to SpottyColors.gradTop,
    0.48f to SpottyColors.gradBottom,
    1f to SpottyColors.gradBottom
)

/** Alto del encabezado fijo de la conversación (TopHeader de 579:369). */
val SPOTTY_HEADER_HEIGHT = 143.dp

private val bubbleShape = RoundedCornerShape(topStart = 2.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp)
private val userShape = RoundedCornerShape(topStart = 16.dp, topEnd = 2.dp, bottomEnd = 16.dp, bottomStart = 16.dp)

private fun jakarta(size: Int, line: Float, color: Color, weight: Int = 400, spacing: Float = 0f, align: TextAlign = TextAlign.Start) =
    figmaText(app_fonts.plusJakarta, size.sp, line.sp, color, FontWeight(weight), spacing.sp, align)

/** Sombra doble de las burbujas y tarjetas de Spotty (0,8 b10 s-6 | 0,10 b25 s-5). */
private fun Modifier.spottyShadow(shape: androidx.compose.ui.graphics.Shape) = this
    .dropShadow(shape, Shadow(radius = 10.dp, spread = (-6).dp, color = SpottyColors.bubbleShadow1, offset = DpOffset(0.dp, 8.dp)))
    .dropShadow(shape, Shadow(radius = 25.dp, spread = (-5).dp, color = SpottyColors.bubbleShadow2, offset = DpOffset(0.dp, 10.dp)))

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
            "Descubre. Disfruta. Conecta.",
            style = figmaText(app_fonts.poppins, 11.sp, 17.sp, SpottyColors.amber, FontWeight.Bold),
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
fun BoxScope.SpottyHistoryCorner(welcome: Boolean, onHistory: () -> Unit) {
    if (welcome) {
        SceneImage("d16b0e4fc231ebc1.png", Modifier.figmaPosition(left = 291.36.dp, top = 0.dp, width = 120.64.dp, height = 86.63.dp))
    } else {
        Box(
            Modifier
                .figmaPosition(left = 304.dp, top = (-18).dp, width = 123.dp, height = 123.dp)
                .background(SpottyColors.cornerBlue, RoundedCornerShape(bottomStart = 90.dp))
        )
    }
    Box(
        Modifier
            .figmaPosition(left = if (welcome) 344.dp else 347.dp, top = if (welcome) 32.dp else 38.dp, width = 50.dp, height = 50.dp)
            .clip(CircleShape)
            .clickable(onClick = onHistory, role = Role.Button, onClickLabel = "Ver historial de chats")
            .semantics { contentDescription = "Ver historial de chats" },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_spotty_sparkles_header),
            contentDescription = "Ver historial de chats",
            modifier = Modifier.size(46.dp)
        )
    }
}

/** Encabezado fijo de la conversación (TopHeader de 579:369): fondo, logo y esquina. */
@Composable
fun SpottyChatHeader(onHistory: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(SPOTTY_HEADER_HEIGHT)
            .background(SpottyColors.header)
    ) {
        SpottyLogo(Modifier.offset(16.dp, 39.dp))
        SpottyHistoryCorner(welcome = false, onHistory = onHistory)
    }
}

/** Etiqueta del modelo ("Background+Border" 444:699): 26 de alto, borde turquesa, ✨ ámbar. */
@Composable
fun SpottyModelBadge(label: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .height(26.dp)
            .background(Brush.horizontalGradient(listOf(Color(0x33CCFBF1), Color(0x33DBEAFE))), shape)
            .border(1.dp, SpottyColors.chipBorderFirst.copy(alpha = 0.7f), shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = jakarta(12, 16f, Color.White, 600), maxLines = 1)
    }
}

// ---------------------------------------------------------------------------------------------
// Mensajes
// ---------------------------------------------------------------------------------------------

/** Respuesta de texto de Spotty ("Text Intro" 444:764). */
@Composable
fun SpottyTextBubble(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(start = 31.dp, end = 16.dp)) {
        Text(
            text,
            style = jakarta(15, 22f, SpottyColors.body, 500),
            modifier = Modifier
                .widthIn(max = 306.dp)
                .spottyShadow(bubbleShape)
                .background(SpottyColors.bubble, bubbleShape)
                .border(1.5.dp, SpottyColors.outline, bubbleShape)
                .padding(15.dp)
        )
    }
}

/** Mensaje del usuario ("User Query Bubble" 444:741): azul, alineado a la derecha. */
@Composable
fun SpottyUserBubble(text: String, time: Long, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(start = 60.dp, end = 16.dp), horizontalAlignment = Alignment.End) {
        Text(
            text,
            style = jakarta(14, 19.25f, Color.White, 500),
            modifier = Modifier
                .widthIn(max = 277.dp)
                .dropShadow(userShape, Shadow(radius = 4.dp, spread = (-2).dp, color = Color(0x1A000000), offset = DpOffset(0.dp, 2.dp)))
                .dropShadow(userShape, Shadow(radius = 6.dp, spread = (-1).dp, color = Color(0x1A000000), offset = DpOffset(0.dp, 4.dp)))
                .background(SpottyColors.userBubble, userShape)
                .padding(14.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text("Tú • ${spottyClock(time)}", style = jakarta(11, 15f, Color.White), modifier = Modifier.padding(end = 4.dp))
    }
}

/** "Reasoning Pill" (444:758): resumen de lo que analizó Spotty. */
@Composable
fun SpottyAnalysisPill(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Box(modifier.fillMaxWidth().padding(start = 31.dp, end = 16.dp)) {
        Row(
            Modifier
                .widthIn(max = 300.dp)
                .heightIn(min = 43.dp)
                .background(SpottyColors.bubble, shape)
                .border(1.5.dp, SpottyColors.outline, shape)
                .padding(start = 13.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).background(SpottyColors.pillDot, CircleShape))
            Spacer(Modifier.width(16.dp))
            Text(text, style = jakarta(12, 17f, SpottyColors.pillText, 700))
        }
    }
}

/** Pregunta de seguimiento y hora en ámbar ("Follow-up prompt hint" 446:1339). */
@Composable
fun SpottyFollowUp(text: String?, time: Long, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(start = 35.dp, end = 20.dp)) {
        if (text != null) {
            Text(text, style = jakarta(13, 19f, Color.White, 600))
            Spacer(Modifier.height(11.dp))
        }
        Text("Spotty • ${spottyClock(time)}", style = jakarta(12, 15f, SpottyColors.amber))
    }
}

/** Spotty está pensando: tres puntos que laten dentro de una burbuja. */
@Composable
fun SpottyTypingBubble(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typing")
    Box(modifier.fillMaxWidth().padding(start = 31.dp)) {
        Row(
            Modifier
                .spottyShadow(bubbleShape)
                .background(SpottyColors.bubble, bubbleShape)
                .border(1.5.dp, SpottyColors.outline, bubbleShape)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { i ->
                val a by transition.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(500, delayMillis = i * 160), RepeatMode.Reverse),
                    label = "dot$i"
                )
                Box(Modifier.size(8.dp).alpha(a).background(SpottyColors.pillText, CircleShape))
            }
        }
    }
}

/** Aviso en lugar de la respuesta cuando algo falla (sin internet, cuota, clave, error). */
@Composable
fun SpottyProblemBubble(problem: SpottyProblem, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    val (title, body) = when (problem) {
        SpottyProblem.OFFLINE -> "Sin conexión 📡" to "Necesito internet para buscar planes. Revisa tu conexión e inténtalo de nuevo."
        SpottyProblem.QUOTA -> "Spotty está descansando 😴" to "Hoy ya respondí todas las preguntas gratuitas. Vuelve mañana y seguimos buscando planes."
        SpottyProblem.NO_KEY -> "Falta configurar Spotty 🔑" to "La app no tiene la clave de Gemini. Revisa docs/spotty/TUTORIAL_API_KEY.md."
        SpottyProblem.GENERIC -> "Algo salió mal 🤔" to "No pude responder esta vez. Inténtalo de nuevo en un momento."
    }
    Box(modifier.fillMaxWidth().padding(start = 31.dp, end = 16.dp)) {
        Column(
            Modifier
                .widthIn(max = 306.dp)
                .spottyShadow(bubbleShape)
                .background(SpottyColors.bubble, bubbleShape)
                .border(1.dp, SpottyColors.amber, bubbleShape)
                .padding(15.dp)
        ) {
            Text(title, style = jakarta(14, 20f, SpottyColors.greetingTitle, 700))
            Spacer(Modifier.height(4.dp))
            Text(body, style = jakarta(14, 20f, SpottyColors.body, 500))
            if (onRetry != null) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SpottyColors.primary)
                        .clickable(onClick = onRetry, role = Role.Button)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reintentar", style = jakarta(12, 16f, Color.White, 600))
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
                style = jakarta(12, 16f, SpottyColors.chipText, 500),
                maxLines = 1,
                modifier = Modifier
                    .height(30.dp)
                    .dropShadow(shape, Shadow(radius = 2.dp, color = Color(0x0D000000), offset = DpOffset(0.dp, 1.dp)))
                    .clip(shape)
                    .background(Color.White)
                    .border(1.dp, if (i == 0) SpottyColors.chipBorderFirst else SpottyColors.chipBorder, shape)
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
                style = jakarta(11, 16.5f, Color.Black, 500),
                maxLines = 1,
                modifier = Modifier
                    .height(25.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
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
    TEAL(Color(0xFFF0FDFA), Color(0xFF0F766E)),
    BLUE(Color(0xFFEFF6FF), Color(0xFF1D4ED8)),
    SLATE(Color(0xFFF1F5F9), Color(0xFF475569)),
    AMBER(Color(0xFFFFFBEB), Color(0xFFB45309)),
    PURPLE(Color(0xFFFAF5FF), Color(0xFF7E22CE)),
    GREEN(Color(0xFFECFDF5), Color(0xFF047857))
}

/** Hasta tres etiquetas con datos reales: categoría, un rasgo y el nivel de precio. */
private fun Place.cardChips(): List<Pair<String, CardChipColor>> = buildList {
    // Los cines están en la categoría teatros, pero se muestran como lo que son
    val cinema = category == HomeCategory.TEATROS && Regex("\\bcine").containsMatchIn(name.lowercase())
    add((if (cinema) "🎬 Cine" else "${category.emoji} ${category.singular}") to CardChipColor.TEAL)
    when {
        group -> add("👥 Apto grupos" to CardChipColor.PURPLE)
        nequi -> add("📲 Acepta Nequi" to CardChipColor.GREEN)
        study -> add("📖 Para estudiar" to CardChipColor.BLUE)
        solo -> add("🧍 Plan solo" to CardChipColor.AMBER)
    }
    val min = SpottyBudget.pricePerPerson(this@cardChips)?.first
    if (min != null) add((if (min < 15000) "$ Económico" else if (min < 35000) "$$ Moderado" else "$$$ Alto") to CardChipColor.SLATE)
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
    Box(modifier.fillMaxWidth().padding(start = 31.dp, end = 16.dp)) {
        Column(
            Modifier
                .width(306.dp)
                .spottyShadow(shape)
                .clip(shape)
                .background(SpottyColors.bubble)
                .border(1.5.dp, SpottyColors.outline, shape)
                .clickable(onClick = onDetails)
                .padding(1.dp)
        ) {
            // Foto con degradado, insignias, corazón, nombre y dirección (112 de alto)
            Box(Modifier.fillMaxWidth().height(112.dp).background(SpottyColors.photoBg)) {
                // key(): SceneImage conserva la primera imagen que pinta; al llegar la foto de la
                // pantalla de Figma (tras la genérica de la categoría) hay que recrearla
                val photo = rememberPlacePhoto(place)
                key(photo) { SceneImage(photo, Modifier.fillMaxSize(), crop = true) }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(0f to Color(0x1A000000), 0.45f to Color(0x33000000), 1f to Color(0xD9000000)))
                )
                Row(Modifier.offset(8.dp, 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (card.match > 0) Row(
                        Modifier.height(19.dp).background(SpottyColors.match, CircleShape).padding(start = 8.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SceneImage("7f18ce5dc44f11bb.png", Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${card.match}% Match", style = jakarta(10, 15f, Color.White, 700), maxLines = 1)
                    }
                    if (card.distance != null) {
                        Text(
                            card.distance,
                            style = jakarta(10, 15f, Color.White, 700),
                            maxLines = 1,
                            modifier = Modifier.height(19.dp).background(SpottyColors.distance, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset((-8).dp, 8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF020A26))
                        .clickable(onClick = onFavorite, role = Role.Button, onClickLabel = if (isFavorite) "Quitar de favoritos" else "Guardar en favoritos"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) SpottyColors.amber else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column(Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 12.dp, bottom = 8.dp)) {
                    Text(
                        place.name,
                        style = jakarta(14, 20f, Color.White, 700, -0.35f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${place.address} • ${place.hours}",
                        style = jakarta(11, 16.5f, SpottyColors.subtitle, 500),
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
                Box(Modifier.fillMaxWidth().height(1.dp).background(SpottyColors.bubbleBorder))
                Row(Modifier.fillMaxWidth().height(37.dp).padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = SpottyColors.priceLabel)) { append("${card.priceLabel}: ") }
                            withStyle(SpanStyle(color = SpottyColors.body, fontWeight = FontWeight.SemiBold)) { append(card.priceValue) }
                        },
                        style = jakarta(11, 16.5f, SpottyColors.priceLabel, 500),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    val btnShape = RoundedCornerShape(12.dp)
                    Text(
                        "Ver detalles",
                        style = jakarta(12, 16f, if (primary) Color.White else SpottyColors.primary, if (primary) 600 else 700),
                        modifier = Modifier
                            .height(28.dp)
                            .then(if (primary) Modifier.dropShadow(btnShape, Shadow(radius = 2.dp, color = Color(0x0D000000), offset = DpOffset(0.dp, 1.dp))) else Modifier)
                            .clip(btnShape)
                            .background(if (primary) SpottyColors.primary else SpottyColors.secondaryBtn)
                            .clickable(onClick = onDetails, role = Role.Button)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Botón redondo para bajar al último mensaje (degradado turquesa → azul de Figma). */
@Composable
fun SpottyScrollDownButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(40.dp)
            .dropShadow(CircleShape, Shadow(radius = 6.dp, color = Color(0x33000000), offset = DpOffset(0.dp, 3.dp)))
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF14A9A5), Color(0xFF0D5FD0))))
            .clickable(onClick = onClick, role = Role.Button, onClickLabel = "Ir al último mensaje"),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
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
    Box(
        modifier
            .size(356.dp, 96.dp)
            .background(SpottyColors.welcomeInput, shape)
            .border(1.dp, Color(0x03000000), shape)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = figmaText(app_fonts.inter, 14.sp, 17.sp, Color.White, FontWeight.Medium),
            cursorBrush = SolidColor(SpottyColors.amber),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            maxLines = 2,
            modifier = Modifier.offset(18.dp, 14.dp).width(316.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text("¡Pulsa aquí e inicia una conversación!", style = figmaText(app_fonts.inter, 14.sp, 17.sp, Color.White, FontWeight.Medium))
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
                .clickable(enabled = value.isNotBlank(), onClick = onSend, role = Role.Button, onClickLabel = "Enviar")
                .semantics { contentDescription = "Enviar mensaje" },
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
    Column(modifier.fillMaxWidth().background(SpottyColors.inputBand).padding(top = 8.dp)) {
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
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = jakarta(12, 16f, Color.Black),
                    cursorBrush = SolidColor(SpottyColors.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (enabled) onSend() }),
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (value.isEmpty()) Text("Escríbele a Spotty…", style = jakarta(12, 16f, Color(0xFF94A3B8)))
                        inner()
                    }
                )
            }
            Box(
                Modifier
                    .size(51.dp, 50.dp)
                    .clip(CircleShape)
                    .clickable(enabled = enabled && value.isNotBlank(), onClick = onSend, role = Role.Button, onClickLabel = "Enviar")
                .semantics { contentDescription = "Enviar mensaje" },
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
            .background(Color.White, shape)
            .border(3.dp, SpottyColors.amber, shape)
    ) {
        SceneImage("2bae237ad14d1a94.png", Modifier.offset(12.dp, 20.dp).size(40.dp))
        Text(
            "¡Lo has guardado en favoritos!",
            style = figmaText(app_fonts.poppins, 18.sp, 20.sp, Color.Black, textAlign = TextAlign.Center),
            modifier = Modifier.offset(56.dp, 17.dp).size(274.dp, 57.dp).padding(top = 18.dp)
        )
        Box(
            Modifier
                .offset(323.dp, 0.dp)
                .size(37.dp)
                .clip(CircleShape)
                .clickable(onClick = onClose, role = Role.Button, onClickLabel = "Cerrar"),
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
            .background(Color.White.copy(alpha = 0.7f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCancel),
        contentAlignment = Alignment.Center
    ) {
        val shape = RoundedCornerShape(40.dp)
        Box(
            Modifier
                .size(360.dp, 360.dp)
                .clip(shape)
                .background(SpottyColors.dialogBg)
                .border(2.dp, SpottyColors.dialogBorder, shape)
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
                    Text("¿Deseas eliminar este chat?", style = figmaText(app_fonts.poppins, 20.sp, 24.sp, Color.Black))
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "“$chatTitle”",
                    style = figmaText(app_fonts.poppins, 15.sp, 20.sp, Color(0xFF334155), textAlign = TextAlign.Center),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Se borrará de tu historial y no se puede recuperar.",
                    style = figmaText(app_fonts.poppins, 13.sp, 18.sp, Color(0xFF64748B), textAlign = TextAlign.Center)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "VOLVER",
                    style = figmaText(app_fonts.poppins, 18.sp, 20.sp, Color.Black, textAlign = TextAlign.Center),
                    modifier = Modifier
                        .size(115.dp, 40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SpottyColors.amber)
                        .clickable(onClick = onCancel, role = Role.Button)
                        .padding(top = 10.dp)
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    "Eliminar chat",
                    style = figmaText(app_fonts.poppins, 18.sp, 20.sp, SpottyColors.danger, textAlign = TextAlign.Center),
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

private fun dayBucket(time: Long, now: Long = System.currentTimeMillis()): String {
    val start = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val day = 24L * 60 * 60 * 1000
    return when {
        time >= start -> "Hoy"
        time >= start - day -> "Ayer"
        time >= start - 7 * day -> "Últimos 7 días"
        else -> "Anteriores"
    }
}

private fun chatDate(time: Long): String {
    val today = dayBucket(time) == "Hoy"
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
    val shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
    Column(
        modifier
            .fillMaxSize()
            .dropShadow(shape, Shadow(radius = 24.dp, color = Color(0x33000000), offset = DpOffset((-4).dp, 0.dp)))
            .clip(shape)
            .background(SpottyColors.panelBg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 10.dp, top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            SceneImage("09e6a5010822005b.png", Modifier.size(34.dp))
            Spacer(Modifier.width(8.dp))
            Text("Tus chats con Spotty", style = jakarta(18, 24f, SpottyColors.greetingTitle, 800), modifier = Modifier.weight(1f))
            Box(
                Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClose, role = Role.Button, onClickLabel = "Cerrar historial"),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Close, contentDescription = null, tint = SpottyColors.panelSection) }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF14A9A5), SpottyColors.primary)))
                .clickable(onClick = onNewChat, role = Role.Button),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = SpottyColors.amber, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Nuevo chat", style = jakarta(15, 20f, Color.White, 700))
        }
        Spacer(Modifier.height(12.dp))
        if (chats.isEmpty()) {
            Text(
                "Aún no tienes conversaciones. Pregúntale algo a Spotty y aparecerán aquí.",
                style = jakarta(13, 19f, SpottyColors.panelSection, align = TextAlign.Center),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 32.dp)
            )
            return@Column
        }
        val grouped = remember(chats) { chats.sortedByDescending { it.updatedAt }.groupBy { dayBucket(it.updatedAt) } }
        LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
            grouped.forEach { (bucket, list) ->
                item(key = "h_$bucket") {
                    Text(
                        bucket.uppercase(),
                        style = jakarta(11, 16f, SpottyColors.panelSection, 700, 0.6f),
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
                            .background(if (selected) SpottyColors.panelSelected else Color.Transparent)
                            .clickable { onOpen(chat) }
                            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                chat.title,
                                style = jakarta(14, 19f, if (selected) SpottyColors.primary else SpottyColors.body, 600),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(chatDate(chat.updatedAt), style = jakarta(11, 15f, SpottyColors.timeGray))
                        }
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = "Eliminar chat") { onDelete(chat) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = SpottyColors.timeGray, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Ancho del panel de historial. */
val SPOTTY_PANEL_WIDTH: Dp = 320.dp
