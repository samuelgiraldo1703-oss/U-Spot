package screens

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.u_spot.R
import data.HomePlace
import data.Place
import data.UserProfile
import decorations.CommentsSection
import decorations.FigmaSceneNode
import decorations.FigmaWidthScale
import decorations.HeroCategoryBadge
import decorations.PlaceFeatureRow
import decorations.featureNodes
import decorations.style
import decorations.PlaceInfoSection
import decorations.Scene
import decorations.SceneImage
import decorations.SceneNode
import decorations.photo
import decorations.placeCardTexts
import decorations.rememberScene
import decorations.singularRes
import decorations.statusBarHeight
import decorations.SceneScreen
import decorations.app_colors
import decorations.firstNode
import decorations.jakartaText
import decorations.placeAt


/** Fondo del encabezado (foto) de las pantallas de detalle, también detrás de la barra de estado. */
private val HeroDark = app_colors.placeHeroDark

/**
 * Detalle de un lugar: Figma "Home App (Restaurantes)", "(Cafeterias)", "(Bares)"... (17 pantallas).
 * Debajo de "Compartir" va la sección de comentarios (no estaba en Figma): los 2 más recientes y el
 * botón que abre el panel con todos.
 */
@Composable
fun HomePlaceScreen(
    place: HomePlace = HomePlace.WOK,
    info: Place? = null,
    user: UserProfile = UserProfile.Preview,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    LightStatusBarIcons()

    // Con [info] (el lugar está en la base de datos) se mezcla la pantalla de Figma con los datos
    // reales: rasgos, rango de precios, distancia, ambiente, categoría y el bloque "Información".
    SceneScreen(
        sceneKey = place.sceneKey,
        statusBarColor = HeroDark,
        onClick = { scene ->
            buildMap {
                shareButton(scene)?.let { node -> put(node.id) { sharePlace(context, info?.name ?: place.title) } }
            }
        },
        hide = { scene ->
            if (info == null) emptySet()
            else (scene.featureNodes() + listOfNotNull(categoryBadge(scene))).map { it.id }.toSet()
        },
        texts = { scene -> info?.let { scene.placeCardTexts(it, context) } ?: emptyMap() },
        overlay = { scene ->
            // Botón "Volver" dibujado sobre la foto
            Box(
                Modifier
                    .placeAt((place.backX - 24).dp, (place.backY - 24).dp, 48.dp, 48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack)
            )
            val favTarget = info?.id ?: place.key
            val favorites = data.rememberFavorites()
            val isFav = favTarget in favorites
            val heartX = 412f - place.backX
            val heartY = place.backY
            Box(
                Modifier
                    .placeAt((heartX - 24).dp, (heartY - 24).dp, 48.dp, 48.dp)
                    .clip(CircleShape)
                    .clickable { data.FavoriteStore.toggle(context, favTarget) },
                contentAlignment = Alignment.Center
            ) {
                if (isFav) {
                    Box(
                        Modifier.size(42.dp).clip(CircleShape).background(app_colors.placeHeartCircleBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Favorite,
                            contentDescription = stringResource(R.string.home_favorite_remove_cd),
                            tint = app_colors.placeHeartActive,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            if (info != null) {
                val features = scene.featureNodes()
                features.minByOrNull { it.x }?.let { first ->
                    val right = features.maxOf { it.x + it.w }
                    PlaceFeatureRow(info, x = first.x, y = first.y, width = right - first.x, wifi = true, own = scene)
                }
                categoryBadge(scene)?.let { badge ->
                    HeroCategoryBadge(stringResource(info.category.singularRes), Modifier.placeAt(badge.x.dp, badge.y.dp, 200.dp, badge.h.dp))
                }
            }
            val share = shareButton(scene)
            val top = share?.let { it.y + it.h + 20f } ?: 720f
            Column(
                Modifier
                    .padding(top = top.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
            ) {
                if (info != null) {
                    PlaceInfoSection(info, showName = true, cardColor = cardColor(scene))
                    Spacer(Modifier.height(24.dp))
                }
                CommentsSection(placeKey = place.key, user = user, accent = accentColor(scene), modifier = Modifier.fillMaxWidth())
            }
        }
    )
}

/**
 * Detalle de un lugar de la base de datos sin pantalla propia en Figma. Sigue el diseño de
 * "Home App (Restaurantes)": foto (la de su categoría), tarjeta blanca con nombre, rasgos, distancia,
 * ambiente, "Información" y "Compartir" (piezas copiadas de la pantalla de Figma de su categoría, con sus colores) y los comentarios.
 */
@Composable
fun PlaceDetailScreen(
    place: Place,
    user: UserProfile = UserProfile.Preview,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    LightStatusBarIcons()
    val template = rememberScene(place.category.style.template)
    val nav = rememberScene("place_wok")?.firstNode { it.name == "Top Action Navigation Bar" } as? SceneNode.Picture
    val badge = rememberScene(place.category.sceneKey)?.find(place.category.badgeId) as? SceneNode.Picture
    val singularName = stringResource(place.category.singularRes)

    val statusBar = statusBarHeight()
    Column(
        Modifier
            .fillMaxSize()
            .background(app_colors.white)
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        if (statusBar > 0.dp) {
            Spacer(Modifier.fillMaxWidth().height(statusBar).background(HeroDark))
        }
        FigmaWidthScale(412f) {
            Box(Modifier.fillMaxWidth()) {
                // Foto de la categoría con el degradado oscuro de Figma
                Box(Modifier.placeAt(0.dp, 0.dp, 412.dp, 268.dp).background(HeroDark)) {
                    SceneImage(place.photo, Modifier.fillMaxSize(), crop = true)
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(0f to app_colors.scrimDark50, 0.5f to Color.Transparent, 1f to app_colors.shadowSoft20)
                        )
                    )
                }
                // Botones "Volver" y favorito de Figma
                nav?.let { nav ->
                    SceneImage(nav.src, Modifier.placeAt(nav.x.dp, nav.y.dp, nav.w.dp, nav.h.dp))
                }
                Box(
                    Modifier
                        .placeAt(12.dp, 2.dp, 48.dp, 48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBack)
                )
                val favorites = data.rememberFavorites()
                val isPlaceFav = place.id in favorites
                Box(
                    Modifier
                        .placeAt(352.dp, 2.dp, 48.dp, 48.dp)
                        .clip(CircleShape)
                        .clickable { data.FavoriteStore.toggle(context, place.id) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaceFav) {
                        Box(
                            Modifier.size(42.dp).clip(CircleShape).background(app_colors.placeHeartCircleBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Favorite,
                                contentDescription = stringResource(R.string.home_favorite_remove_cd),
                                tint = app_colors.placeHeartActive,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                HeroCategoryBadge(singularName, Modifier.placeAt(16.dp, 160.dp, 200.dp, 30.dp))

                Column(
                    Modifier
                        .padding(top = 198.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                        .background(app_colors.white)
                        .padding(horizontal = 13.dp)
                ) {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(56.dp, 58.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (badge != null) SceneImage(badge.src, Modifier.size(56.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                place.name,
                                style = jakartaText(20.sp, 25.sp, app_colors.homeTitle, FontWeight.Bold, letterSpacing = (-0.5).sp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "$singularName • ${place.address}",
                                style = jakartaText(12.sp, 16.sp, app_colors.placeSlateLight, FontWeight.Medium)
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Box(Modifier.fillMaxWidth().height(89.dp)) {
                        PlaceFeatureRow(place, x = 0f, y = 0f, width = 386f, wifi = false)
                    }
                    Spacer(Modifier.height(14.dp))
                    if (template != null) {
                        val texts = template.placeCardTexts(place, context)
                        Box(Modifier.fillMaxWidth().height(86.dp)) {
                            template.firstNode { it.name == "Distance Card" }?.let {
                                FigmaSceneNode(template, it, dx = -it.x, dy = -it.y, texts = texts)
                            }
                            template.firstNode { it.name == "Atmosphere Card" }?.let {
                                FigmaSceneNode(template, it, dx = 386f - it.w - it.x, dy = -it.y, texts = texts)
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    PlaceInfoSection(place, showName = false, cardColor = cardColor(template), modifier = Modifier.padding(horizontal = 3.dp))
                    Spacer(Modifier.height(20.dp))
                    shareButton(template)?.let { share ->
                        Box(Modifier.fillMaxWidth().height(share.h.dp)) {
                            FigmaSceneNode(
                                template!!, share,
                                dx = (386f - share.w) / 2f - share.x, dy = -share.y,
                                onClick = mapOf(share.id to { sharePlace(context, place.name) })
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    CommentsSection(placeKey = place.id, user = user, accent = accentColor(template), modifier = Modifier.padding(horizontal = 3.dp).fillMaxWidth())
                }
            }
        }
    }
}

private fun shareButton(scene: Scene?) = scene?.firstNode { it.name == "Compartir Button" }

/** Color del botón "Compartir" de la categoría (null si Figma lo pinta con degradado). */
private fun accentColor(scene: Scene?): Color? = (shareButton(scene) as? SceneNode.Frame)?.fills?.firstOrNull()

/** Color de las tarjetas "Distancia" / "Ambiente" de la pantalla (para el bloque "Información"). */
private fun cardColor(scene: Scene?): Color =
    (scene?.firstNode { it.name == "Distance Card" } as? SceneNode.Frame)?.fills?.firstOrNull() ?: app_colors.placeDefaultCardBg

/** Etiqueta de categoría sobre la foto ("Cafetería Category Badge", "Museo Category Badge"...). */
private fun categoryBadge(scene: Scene) =
    scene.firstNode { it is SceneNode.Frame && it.name.endsWith("Category Badge") && it.y < 200f }

private fun sharePlace(context: android.content.Context, name: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.place_share_message, name))
    }
    context.startActivity(Intent.createChooser(send, context.getString(R.string.place_share_chooser_title)))
}

/** La foto del encabezado es oscura: íconos claros en la barra de estado mientras se ve esta pantalla. */
@Composable
private fun LightStatusBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val before = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { if (before != null) controller.isAppearanceLightStatusBars = before }
    }
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun HomePlacePreview() {
    HomePlaceScreen()
}
