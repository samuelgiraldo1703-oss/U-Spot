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
