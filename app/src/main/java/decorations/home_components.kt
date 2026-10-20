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
import androidx.compose.ui.unit.dp

/**
 * Pantalla del módulo Home dibujada desde una escena de Figma ([sceneKey], ver [FigmaScene]).
 * El diseño empieza debajo de la barra de estado (pintada con [statusBarColor]), se escala al
 * ancho del teléfono y se desplaza verticalmente si no cabe. [overlay] dibuja encima, en
 * coordenadas de Figma; lo que ponga más abajo del frame alarga la pantalla (p. ej. comentarios).
 */
@Composable
fun SceneScreen(
    sceneKey: String,
    statusBarColor: Color,
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
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(statusBarHeight())
                    .background(statusBarColor)
            )
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
