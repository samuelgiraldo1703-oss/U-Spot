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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.HomeCategory
import data.HomePlace
import data.Place
import data.forFigma
import data.rememberPlaces
import decorations.FigmaScene
import decorations.SceneImage
import decorations.app_fonts
import decorations.figmaText
import decorations.singular
import decorations.FigmaSceneNode
import decorations.FigmaWidthScale
import decorations.Scene
import decorations.SceneNode
import decorations.SceneScreen
import decorations.allNodes
import decorations.app_colors
import decorations.baseStyle
import decorations.placeAt
import decorations.rememberScene
import decorations.statusBarHeight

/** Diálogos del dashboard: Figma "Home App (Confirmacion Cierre de Sesion)" y "(Cierre de Sesion)". */
private enum class HomeDialog { None, AskLogout, LoggedOut }

/**
 * Pantalla principal (dashboard) mostrada después de iniciar sesión. Figma: 435:654 (con
 * "Ver todos →" pasa a 468:8, la segunda fila de categorías). El saludo usa el nombre del usuario.
 * [onLogout] cierra la sesión de Firebase; [onGoToLogin] lleva al login desde el diálogo final.
 */
@Composable
fun HomeScreen(
    userName: String = "Alejandro",
    onCategory: (HomeCategory) -> Unit = {},
    /** Abre el detalle de un lugar por su clave de ruta (HomePlace.key o Place.id). */
    onPlace: (String) -> Unit = {},
    onProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
    onGoToLogin: () -> Unit = {}
) {
    var showMore by rememberSaveable { mutableStateOf(false) }
    var dialog by rememberSaveable { mutableStateOf(HomeDialog.None) }
    val places = rememberPlaces()
    // Fotos de Figma de los lugares que reemplazan a La Embajada y El Mono Bandido
    val photos = featuredCards.mapNotNull { f ->
        val (node, sceneKey, photo) = f.image ?: return@mapNotNull null
        (rememberScene(sceneKey)?.find(photo) as? SceneNode.Picture)?.let { node to it.src }
    }.toMap()

    val favorites = data.rememberFavorites()
    val context = LocalContext.current

    Box(Modifier.fillMaxSize()) {
        SceneScreen(
            // Siempre la pantalla 435:654; "Ver todos" solo cambia la fila de categorías (ver MoreCategories)
            sceneKey = "dash_main",
            statusBarColor = app_colors.white,
            onClick = {
                dashboardClicks(onCategory, onProfile, { showMore = true }, { dialog = HomeDialog.AskLogout }) { id ->
                    data.FavoriteStore.toggle(context, id)
                } + featuredCards.associate { it.card to { onPlace(it.route) } }
            },
            texts = { featuredTexts(places) },
            images = { photos },
            hide = { scene ->
                val ids = greetingNodes(scene).map { it.id }.toMutableSet()
                // La fila de Figma (5 categorías) se dibuja aparte con 4: ver MainCategories
                ids += MAIN_CATEGORY_ROW
                ids += featuredHidden()
                if (showMore) ids += MAIN_SEE_ALL
                // Ocultamos los nodos estáticos de corazón de Figma para reemplazarlos con el botón interactivo alineado
                ids += listOf("435:951", "435:1002", "432:582", "468:199", "468:227", "468:159")
                ids
            },
            overlay = { scene ->
                Greeting(scene, userName)
                FeaturedInfo(scene, places)
                if (showMore) MoreCategories(onCategory = onCategory, onBack = { showMore = false })
                else MainCategories(scene, onCategory)

                // Corazones de favoritos integrados y perfectamente alineados (sin desfase)
                decorations.FigmaCardHeartButton(
                    isFav = HomePlace.JUAN_VALDEZ.key in favorites,
                    onClick = { data.FavoriteStore.toggle(context, HomePlace.JUAN_VALDEZ.key) },
                    size = 19.dp,
                    modifier = Modifier.placeAt(156.dp, 401.dp, 32.dp, 33.dp)
                )
                decorations.FigmaCardHeartButton(
                    isFav = HomePlace.BBC.key in favorites,
                    onClick = { data.FavoriteStore.toggle(context, HomePlace.BBC.key) },
                    size = 19.dp,
                    modifier = Modifier.placeAt(358.dp, 404.dp, 32.dp, 33.dp)
                )
                decorations.FigmaCardHeartButton(
                    isFav = HomePlace.MUSEO_ORO.key in favorites,
                    onClick = { data.FavoriteStore.toggle(context, HomePlace.MUSEO_ORO.key) },
                    size = 16.dp,
                    modifier = Modifier.placeAt(141.dp, 689.dp, 22.dp, 23.dp)
                )
            }
        )

        when (dialog) {
            HomeDialog.AskLogout -> {
                BackHandler { dialog = HomeDialog.None }
                LogoutDialog(
                    sceneKey = "dlg_logout_ask",
                    onClick = mapOf(
                        "464:2420" to { dialog = HomeDialog.None },
                        "464:2422" to { dialog = HomeDialog.None },
                        "464:2424" to {
                            onLogout()
                            dialog = HomeDialog.LoggedOut
                        }
                    )
                )
            }
            HomeDialog.LoggedOut -> {
                BackHandler { onGoToLogin() }
                LogoutDialog(
                    sceneKey = "dlg_logout_done",
                    onClick = mapOf("465:2702" to onGoToLogin, "465:2703" to onGoToLogin)
                )
            }
            HomeDialog.None -> Unit
        }
    }
}

/** Saludo de Figma ("Hola, Alejandro" + "👋"): se ocultan para dibujarlos con el nombre real. */
private fun greetingNodes(scene: Scene): List<SceneNode.Text> =
    scene.allNodes { it is SceneNode.Text && (it.plain.startsWith("Hola,") || it.plain == "👋") }
        .filterIsInstance<SceneNode.Text>()

@Composable
private fun Greeting(scene: Scene, userName: String) {
    val nodes = greetingNodes(scene)
    val hello = nodes.firstOrNull { it.plain.startsWith("Hola,") } ?: return
    val wave = nodes.firstOrNull { it != hello }
    // Termina antes de la nota manuscrita de la derecha ("La mejor experiencia está cerca")
    Row(
        modifier = Modifier.placeAt(hello.x.dp, hello.y.dp, (301 - hello.x).dp, hello.h.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Hola, $userName",
            style = hello.baseStyle(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (wave != null) {
            Spacer(Modifier.widthIn(min = (wave.x - hello.x - hello.w).dp))
            Text(text = wave.plain, style = wave.baseStyle(), maxLines = 1)
        }
    }
}

/**
 * Tarjetas de lugares del dashboard ("Cerca de ti" y "Recomendado para ti") con datos reales.
 * Juan Valdez Café es la pantalla de Juan Valdez de Cafés; "La Embajada" y "El Mono Bandido" no
 * están en la base de datos y se reemplazaron (decisión del equipo) por BBC Bodega Las Aguas y el
 * Museo del Oro. Se conserva el diseño de Figma; lo que eran textos de relleno ("$$", etiquetas,
 * descripción) pasa a ser el precio, el horario, Nequi y los rasgos reales del lugar.
 */
private class FeaturedCard(
    val card: String,
    val route: String,
    /** Título de Figma que se cambia por el nombre del lugar (null: se deja el de Figma). */
    val title: String?,
    /** Foto de Figma que se cambia por la del lugar: (nodo, escena e id de la foto de su pantalla). */
    val image: Triple<String, String, String>?,
    /** "$$" de Figma y fila de etiquetas, que se reemplazan por los datos reales. */
    val price: String,
    val tags: String,
    val distance: String
)

private val nearCards = listOf(
    FeaturedCard("435:945", HomePlace.JUAN_VALDEZ.key, null, null, "435:966", "435:967", "435:950"),
    FeaturedCard(
        "435:996", HomePlace.BBC.key, "435:1006",
        Triple("435:999", HomePlace.BBC.sceneKey, "523:1228"), "435:1017", "435:1018", "435:1001"
    )
)

/** "Recomendado para ti": la fila "• $$ • 1.4 km" (432:589) y la descripción (432:615) también cambian. */
private val recommended = FeaturedCard(
    "432:573", HomePlace.MUSEO_ORO.key, "432:588",
    Triple("432:576", HomePlace.MUSEO_ORO.sceneKey, "493:4521"), "432:589", "432:606", "432:605"
)
private const val RECOMMENDED_TEXT = "432:615"
private const val RECOMMENDED_PIN = "432:603"

private val featuredCards = nearCards + recommended

private fun List<Place>.of(f: FeaturedCard): Place? =
    HomePlace.fromKey(f.route)?.let { forFigma(it) } ?: firstOrNull { it.id == f.route }

private fun featuredHidden(): Set<String> = featuredCards.flatMap { listOf(it.price, it.tags) }.toSet()

private fun featuredTexts(places: List<Place>): Map<String, String> = buildMap {
    for (f in nearCards) {
        val place = places.of(f) ?: continue
        f.title?.let { put(it, place.name) }
        put(f.distance, "A ${place.distance}")
    }
    places.of(recommended)?.let { place ->
        recommended.title?.let { put(it, place.name) }
        put(RECOMMENDED_TEXT, "Horario: ${place.hours}\n${place.address}")
    }
}

/** Etiquetas de Figma de estas tarjetas: verde (categoría), azul / índigo y ámbar. */
private class HomeTag(val bg: Color, val border: Color?, val text: Color)

private val nearTags = listOf(
    HomeTag(Color(0xFFF0FDFA), Color(0x80CCFBF1), Color(0xFF0F766E)),
    HomeTag(Color(0xFFEFF6FF), Color(0x80DBEAFE), Color(0xFF1D4ED8))
)
private val recommendedTags = listOf(
    HomeTag(Color(0xFFF0FDFA), null, Color(0xFF0F766E)),
    HomeTag(Color(0xFFEEF2FF), null, Color(0xFF4338CA)),
    HomeTag(Color(0xFFFFF2CD), null, Color(0xFF78350F))
)

private val Muted = Color(0xFF64748B)

private fun interText(size: Float, line: Float, color: Color, weight: FontWeight = FontWeight.Medium) =
    figmaText(app_fonts.inter, size.sp, line.sp, color, weight)

/** Datos reales sobre las tarjetas: precio, Nequi, horario y etiquetas (en el lugar de los de Figma). */
@Composable
private fun FeaturedInfo(scene: Scene, places: List<Place>) {
    for (f in nearCards) {
        val place = places.of(f) ?: continue
        val price = scene.find(f.price) ?: continue
        val tags = scene.find(f.tags)
        val x = price.x
        Column(Modifier.placeAt(x.dp, price.y.dp, 183.dp, 36.dp)) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))) { append(place.price) }
                    if (place.nequi) append("  ·  Acepta Nequi")
                },
                style = interText(11f, 16.5f, Muted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Muted, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(4.dp))
                Text(place.hours, style = interText(10.5f, 16f, Muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (tags != null) {
            val labels = listOf(place.category.singular, if (place.quiet) "Ambiente tranquilo" else "Ideal para grupos")
            Row(
                Modifier.placeAt(x.dp, (tags.y + tags.h - 21f).dp, 190.dp, 21.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                labels.forEachIndexed { i, label -> HomeTagChip(label, nearTags[i], 21f, FontWeight.Medium) }
            }
        }
    }

    val place = places.of(recommended) ?: return
    val row = scene.find(recommended.price) ?: return
    val pin = scene.find(RECOMMENDED_PIN) as? SceneNode.Picture
    Row(Modifier.placeAt(row.x.dp, row.y.dp, row.w.dp, row.h.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("•  ${place.price}  •  ", style = interText(11f, 16.5f, Muted, FontWeight.Normal), maxLines = 1)
        if (pin != null) SceneImage(pin.src, Modifier.size(10.dp))
        Spacer(Modifier.width(2.dp))
        Text(place.distance, style = interText(11f, 16.5f, Color(0xFF0D9488), FontWeight.SemiBold), maxLines = 1)
    }
    scene.find(recommended.tags)?.let { tags ->
        val labels = listOf(
            place.category.singular,
            if (place.study) "Apto para estudiar" else if (place.group) "Grupos" else "Persona sola",
            if (place.quiet) "Ambiente tranquilo" else "Ambiente animado"
        )
        Column(Modifier.placeAt(tags.x.dp, (tags.y + 6.5f).dp, tags.w.dp, 44.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HomeTagChip(labels[0], recommendedTags[0], 19f, FontWeight.SemiBold)
                HomeTagChip(labels[1], recommendedTags[1], 19f, FontWeight.SemiBold)
            }
            HomeTagChip(labels[2], recommendedTags[2], 19f, FontWeight.SemiBold)
        }
    }
}

@Composable
private fun HomeTagChip(label: String, tag: HomeTag, height: Float, weight: FontWeight) {
    val shape = RoundedCornerShape(6.dp)
    var mod = Modifier.height(height.dp).clip(shape).background(tag.bg)
    if (tag.border != null) mod = mod.border(1.dp, tag.border, shape)
    Box(mod.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
        Text(label, style = interText(10f, 15f, tag.text, weight), maxLines = 1)
    }
}


/** Fila de categorías (5 en Figma) y enlace "Ver todos →" de la pantalla 435:654. */
private const val MAIN_CATEGORY_ROW = "435:771"
private const val MAIN_SEE_ALL = "538:934"

private fun dashboardClicks(
    onCategory: (HomeCategory) -> Unit,
    onProfile: () -> Unit,
    showMore: () -> Unit,
    askLogout: () -> Unit,
    onFav: (String) -> Unit
): Map<String, () -> Unit> = mapOf(
    MAIN_SEE_ALL to showMore,
    "454:2131" to askLogout,
    "435:1452" to onProfile,
    "435:951" to { onFav(HomePlace.JUAN_VALDEZ.key) },
    "435:1002" to { onFav(HomePlace.BBC.key) },
    "432:582" to { onFav(HomePlace.MUSEO_ORO.key) },
    "468:199" to { onFav(HomePlace.JUAN_VALDEZ.key) },
    "468:227" to { onFav(HomePlace.BBC.key) },
    "468:159" to { onFav(HomePlace.MUSEO_ORO.key) }
)

/** Centro horizontal del icono [i] de [count]: del primero (46.5) al último (362.8) de la fila de Figma. */
private fun tileCenter(i: Int, count: Int) = 46.5f + (362.8f - 46.5f) * i / (count - 1)

/** Botones de la primera fila (435:654): icono y nombre de cada categoría. */
private val mainTiles = listOf(
    "435:772" to HomeCategory.RESTAURANTES,
    "435:781" to HomeCategory.CAFES,
    "435:792" to HomeCategory.BARES,
    "435:803" to HomeCategory.TEATROS
)

/**
 * Primera fila: 4 categorías (Ocio queda solo en "Ver todos"), repartidas en el mismo ancho que la
 * segunda fila para que ambas se vean iguales.
 */
@Composable
private fun MainCategories(scene: Scene, onCategory: (HomeCategory) -> Unit) {
    mainTiles.forEachIndexed { i, (id, category) ->
        val button = scene.find(id) as? SceneNode.Frame ?: return@forEachIndexed
        val icon = button.children.firstOrNull { it is SceneNode.Picture } ?: button
        FigmaSceneNode(
            scene, button,
            dx = tileCenter(i, mainTiles.size) - (icon.x + icon.w / 2),
            onClick = mapOf(button.id to { onCategory(category) })
        )
    }
}

/** Icono y nombre de una categoría de la segunda fila (Figma 468:8). [label] es null si el icono ya lo incluye. */
private class MoreTile(val category: HomeCategory, val badge: String, val label: String?)

private val moreTiles = listOf(
    MoreTile(HomeCategory.OCIO, "468:106", null),
    MoreTile(HomeCategory.MUSEOS, "468:532", "468:547"),
    MoreTile(HomeCategory.DISCOTECAS, "468:536", "468:550"),
    MoreTile(HomeCategory.LIBRERIAS, "468:540", "468:554")
)

/**
 * "Ver todos": la segunda fila de categorías de Figma (Ocio, Museos, Discotecas, Librerías) en el
 * lugar de la primera. Los iconos se reparten en el mismo ancho que la fila principal (primer y
 * último icono en las mismas posiciones) y a la misma altura, para que nada más se mueva.
 */
@Composable
private fun MoreCategories(onCategory: (HomeCategory) -> Unit, onBack: () -> Unit) {
    val scene = rememberScene("dash_more") ?: return
    moreTiles.forEachIndexed { i, tile ->
        val center = tileCenter(i, moreTiles.size)
        val open = { onCategory(tile.category) }
        val badge = scene.find(tile.badge) ?: return@forEachIndexed
        // El icono de Ocio viene dentro de su botón: se alinea por la imagen interna
        val icon = (badge as? SceneNode.Frame)?.children?.firstOrNull { it is SceneNode.Picture } ?: badge
        FigmaSceneNode(
            scene, badge,
            dx = center - (icon.x + icon.w / 2),
            dy = 228f - icon.y,
            onClick = mapOf(badge.id to open)
        )
        tile.label?.let { scene.find(it) }?.let { label ->
            FigmaSceneNode(
                scene, label,
                dx = center - (label.x + label.w / 2),
                dy = 310f - (label.y + label.h / 2),
                onClick = mapOf(label.id to open)
            )
        }
    }
    // "←Ver todos" en el lugar de "Ver todos →"
    scene.find("468:913")?.let { link ->
        FigmaSceneNode(scene, link, dx = 318f - link.x, dy = 206f - link.y, onClick = mapOf(link.id to onBack))
    }
}

/**
 * Diálogo de cierre de sesión dibujado desde Figma sobre el dashboard. En Figma el dashboard
 * queda al 30 % de opacidad sobre blanco: equivale a cubrirlo con blanco al 70 %.
 */
@Composable
private fun LogoutDialog(sceneKey: String, onClick: Map<String, () -> Unit>) {
    val scene = rememberScene(sceneKey)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.7f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        if (scene == null) return@Box
        FigmaWidthScale(scene.width) {
            FigmaScene(scene, onClick = onClick)
        }
    }
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun HomePreview() {
    HomeScreen()
}
