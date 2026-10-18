package screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import data.HomeCategory
import decorations.FigmaScene
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
    onProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
    onGoToLogin: () -> Unit = {}
) {
    var showMore by rememberSaveable { mutableStateOf(false) }
    var dialog by rememberSaveable { mutableStateOf(HomeDialog.None) }

    Box(Modifier.fillMaxSize()) {
        SceneScreen(
            // Siempre la pantalla 435:654; "Ver todos" solo cambia la fila de categorías (ver MoreCategories)
            sceneKey = "dash_main",
            statusBarColor = app_colors.white,
            onClick = { dashboardClicks(onCategory, onProfile, { showMore = true }, { dialog = HomeDialog.AskLogout }) },
            hide = { scene ->
                val ids = greetingNodes(scene).map { it.id }.toMutableSet()
                // La fila de Figma (5 categorías) se dibuja aparte con 4: ver MainCategories
                ids += MAIN_CATEGORY_ROW
                if (showMore) ids += MAIN_SEE_ALL
                ids
            },
            overlay = { scene ->
                Greeting(scene, userName)
                if (showMore) MoreCategories(onCategory = onCategory, onBack = { showMore = false })
                else MainCategories(scene, onCategory)
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

/** Fila de categorías (5 en Figma) y enlace "Ver todos →" de la pantalla 435:654. */
private const val MAIN_CATEGORY_ROW = "435:771"
private const val MAIN_SEE_ALL = "538:934"

private fun dashboardClicks(
    onCategory: (HomeCategory) -> Unit,
    onProfile: () -> Unit,
    showMore: () -> Unit,
    askLogout: () -> Unit
): Map<String, () -> Unit> = mapOf(
    MAIN_SEE_ALL to showMore,
    "454:2131" to askLogout,
    "435:1452" to onProfile
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
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.fillMaxWidth().height(statusBarHeight()))
            FigmaWidthScale(scene.width) {
                FigmaScene(scene, onClick = onClick)
            }
        }
    }
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun HomePreview() {
    HomeScreen()
}
