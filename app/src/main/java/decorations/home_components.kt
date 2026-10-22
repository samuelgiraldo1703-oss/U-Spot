package decorations

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Pantalla del módulo Home dibujada desde una escena de Figma ([sceneKey], ver [FigmaScene]).
 * El diseño se dibuja desde el borde superior de la pantalla, combinándose uniformemente
 * con la barra de estado de Android. Se escala al ancho del teléfono y se desplaza
 * verticalmente si no cabe.
 */
@Composable
fun SceneScreen(
    sceneKey: String,
    statusBarColor: Color = Color.Transparent,
    background: Color = app_colors.white,
    scrollState: ScrollState = rememberScrollState(),
    onClick: (Scene) -> Map<String, () -> Unit> = { emptyMap() },
    hide: (Scene) -> Set<String> = { emptySet() },
    texts: (Scene) -> Map<String, String> = { emptyMap() },
    images: (Scene) -> Map<String, String> = { emptyMap() },
    /** Espacio libre al final del contenido (además de la barra de navegación del sistema), p. ej. para no quedar bajo [HomeBottomBar]. */
    bottomSpace: Dp? = null,
    overlay: @Composable BoxScope.(Scene) -> Unit = {}
) {
    val scene = rememberScene(sceneKey)
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(background)
            .imePadding()
    ) {
        val screenHeight = maxHeight
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            if (scene != null) {
                FigmaWidthScale(scene.width) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = scene.height.dp)
                    ) {
                        FigmaScene(scene, onClick = onClick(scene), hide = hide(scene), texts = texts(scene), images = images(scene))
                        overlay(scene)
                    }
                }
            } else {
                Spacer(Modifier.height(screenHeight))
            }
            if (bottomSpace != null) {
                Spacer(Modifier.height(bottomSpace))
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }
}

/** Primer nodo de la escena (en profundidad) que cumple [predicate]. */
fun Scene.firstNode(predicate: (SceneNode) -> Boolean): SceneNode? {
    fun visit(list: List<SceneNode>): SceneNode? {
        for (n in list) {
            if (predicate(n)) return n
            if (n is SceneNode.Frame) visit(n.children)?.let { return it }
        }
        return null
    }
    return visit(nodes)
}

/** Nodos dentro de este (sin incluirlo) que cumplen [predicate], en orden de dibujo. */
fun SceneNode.descendants(predicate: (SceneNode) -> Boolean): List<SceneNode> {
    val out = ArrayList<SceneNode>()
    fun visit(list: List<SceneNode>) {
        for (n in list) {
            if (predicate(n)) out.add(n)
            if (n is SceneNode.Frame) visit(n.children)
        }
    }
    if (this is SceneNode.Frame) visit(children)
    return out
}

/** Todos los nodos de la escena que cumplen [predicate], en orden de dibujo. */
fun Scene.allNodes(predicate: (SceneNode) -> Boolean): List<SceneNode> {
    val out = ArrayList<SceneNode>()
    fun visit(list: List<SceneNode>) {
        for (n in list) {
            if (predicate(n)) out.add(n)
            if (n is SceneNode.Frame) visit(n.children)
        }
    }
    visit(nodes)
    return out
}

/**
 * Botón de favorito para las tarjetas de Figma.
 * Cuando está guardado (isFav = true), dibuja el corazón con relleno rojo (#E11D48)
 * y un contorno blanco nítido alrededor (exactamente como en la captura de diseño).
 * Cuando no está guardado (isFav = false), dibuja la silueta blanca con sombra suave.
 * Se ubica en las coordenadas exactas de Figma sin ningún desfase.
 */
@Composable
fun FigmaCardHeartButton(
    isFav: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 19.dp
) {
    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isFav) {
            // Sombra sutil exterior
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = Color(0x55000000),
                modifier = Modifier
                    .size(size + 3.dp)
                    .offset(y = 1.dp)
            )
            // Contorno blanco nítido exterior
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size + 2.4.dp)
            )
            // Relleno rojo centrado exactamente
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = "Quitar de favoritos",
                tint = Color(0xFFE11D48),
                modifier = Modifier.size(size)
            )
        } else {
            // Contorno blanco sin rellenar con sombra de contraste sobre la foto
            Icon(
                imageVector = Icons.Rounded.FavoriteBorder,
                contentDescription = null,
                tint = Color(0x66000000),
                modifier = Modifier
                    .size(size + 1.dp)
                    .offset(y = 1.dp)
            )
            Icon(
                imageVector = Icons.Rounded.FavoriteBorder,
                contentDescription = "Guardar en favoritos",
                tint = Color.White,
                modifier = Modifier.size(size)
            )
        }
    }
}

/** Destellos amarillos de Figma (446:942) que identifican a Spotty, el asistente de IA. */
private const val SPOTTY_SPARKLES = "3b548698d9c8c1be.png"

/**
 * Barra de navegación inferior del Home: Inicio (activa), Spotty (asistente de IA) y Perfil.
 * Flota fija sobre el contenido como una tarjeta blanca redondeada con sombra suave, igual que las
 * tarjetas del dashboard; Spotty sobresale al centro con el degradado verde azulado de la marca.
 * Cada destino lleva ícono y etiqueta para que se lea de un vistazo.
 */
@Composable
fun HomeBottomBar(
    onHome: () -> Unit,
    onSpotty: () -> Unit,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT)
                .shadow(18.dp, shape, ambientColor = app_colors.navShadow, spotColor = app_colors.navShadow)
                .background(app_colors.white, shape)
                .border(1.dp, app_colors.navBorder, shape)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem("Inicio", Icons.Rounded.Home, selected = true, onClick = onHome, modifier = Modifier.weight(1f))
            // Hueco para el botón central de Spotty, que se dibuja encima
            Spacer(Modifier.width(SPOTTY_SIZE + 24.dp))
            NavItem("Perfil", Icons.Outlined.Person, selected = false, onClick = onProfile, modifier = Modifier.weight(1f))
        }
        SpottyButton(onSpotty, Modifier.padding(bottom = 8.dp))
    }
}

/** Alto de la barra sin márgenes; las pantallas dejan este espacio libre al final del contenido. */
val BAR_HEIGHT = 66.dp
private val SPOTTY_SIZE = 62.dp

@Composable
private fun NavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val tint = if (selected) app_colors.navActive else app_colors.navInactive
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick, role = Role.Tab, onClickLabel = label)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(width = 56.dp, height = 30.dp)
                .background(if (selected) app_colors.navActiveBg else Color.Transparent, RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            style = figmaText(app_fonts.inter, 12.sp, 16.sp, tint, if (selected) FontWeight.Bold else FontWeight.SemiBold),
            maxLines = 1
        )
    }
}

/** Botón central de Spotty: círculo con el degradado de la marca, borde blanco y su nombre debajo. */
@Composable
private fun SpottyButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(SPOTTY_SIZE)
                .shadow(10.dp, CircleShape, ambientColor = app_colors.spottyGradEnd, spotColor = app_colors.spottyGradStart)
                .background(app_colors.white, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(app_colors.spottyGradStart, app_colors.spottyGradEnd)))
                .clickable(onClick = onClick, role = Role.Button, onClickLabel = "Abrir Spotty, asistente de IA"),
            contentAlignment = Alignment.Center
        ) {
            SceneImage(SPOTTY_SPARKLES, Modifier.size(32.dp))
        }
        Spacer(Modifier.height(2.dp))
        Text("Spotty", style = figmaText(app_fonts.inter, 12.sp, 16.sp, app_colors.navLabel, FontWeight.ExtraBold), maxLines = 1)
    }
}
