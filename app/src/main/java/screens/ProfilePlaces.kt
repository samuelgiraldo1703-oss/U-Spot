package screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.u_spot.R
import data.FavoriteStore
import data.HomePlace
import data.Place
import data.rememberFavorites
import data.rememberPlaces
import decorations.BackButton
import decorations.BoxedText
import decorations.ProfileCanvas
import decorations.ProfileCorner
import decorations.ProfileFooter
import decorations.ProfileLogo
import decorations.SceneImage
import decorations.app_colors
import decorations.figmaPosition
import decorations.jakartaText
import decorations.photo
import decorations.singular

private data class InterestSpec(@param:DrawableRes val photo: Int, val label: String)

/** Figma: "Preferencias" (212:1119). */
@Composable
fun PreferencesScreen(onBack: () -> Unit = {}) {
    // Tercera fila: en Figma el hueco del centro queda vacío.
    val rows: List<List<InterestSpec?>> = listOf(
        listOf(
            InterestSpec(R.drawable.pref_cafes, "Cafés"),
            InterestSpec(R.drawable.pref_restaurantes, "Restaurantes"),
            InterestSpec(R.drawable.pref_cines, "Cines")
        ),
        listOf(
            InterestSpec(R.drawable.pref_museos, "Museos"),
            InterestSpec(R.drawable.pref_bares, "Bares"),
            InterestSpec(R.drawable.pref_discotecas, "Discotecas")
        ),
        listOf(
            InterestSpec(R.drawable.pref_librerias, "Librerías"),
            null,
            InterestSpec(R.drawable.pref_ocio, "Ocio")
        )
    )
    val selected = remember { mutableStateListOf("Cafés") }

    ProfileCanvas(designHeight = 916.dp) {
        ProfileFooter(app_colors.profileYellow)
        ProfileCorner(app_colors.formBlue)
        BackButton(
            onClick = onBack,
            modifier = Modifier.figmaPosition(right = 28.dp, top = 32.dp),
            res = R.drawable.ic_back_blue
        )
        ProfileLogo(Modifier.figmaPosition(left = 50.dp, top = 21.dp))

        Text(
            text = "Preferencias",
            style = jakartaText(25.sp, 34.38.sp, app_colors.prefTitle, FontWeight.ExtraBold, letterSpacing = (-0.62).sp),
            modifier = Modifier.figmaPosition(left = 20.dp, top = 149.dp)
        )
        Text(
            text = "Edita tus intereses para recibir recomendacionespersonalizadas",
            style = jakartaText(14.sp, 21.sp, app_colors.prefTitle.copy(alpha = 0.8f)),
            modifier = Modifier.figmaPosition(left = 20.dp, right = 20.dp, top = 204.dp)
        )

        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier.figmaPosition(left = 20.dp, right = 20.dp, top = 266.dp + 160.67.dp * index),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { spec ->
                    if (spec == null) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        InterestTile(
                            spec = spec,
                            selected = spec.label in selected,
                            onToggle = { if (!selected.remove(spec.label)) selected.add(spec.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InterestTile(spec: InterestSpec, selected: Boolean, onToggle: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.67.dp)
                .dropShadow(shape, Shadow(radius = 2.dp, color = Color(0x0D000000), offset = DpOffset(0.dp, 1.dp)))
                .clip(shape)
                .background(app_colors.profileCardBorder)
                .clickable(onClick = onToggle)
        ) {
            Image(
                painter = painterResource(spec.photo),
                contentDescription = spec.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Image(
                painter = painterResource(if (selected) R.drawable.ic_pref_selected else R.drawable.ic_pref_add),
                contentDescription = if (selected) "Seleccionado" else "Agregar",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-6.33).dp, y = (-6).dp)
                    .size(24.dp)
            )
        }
        Text(
            text = spec.label,
            style = jakartaText(12.sp, 15.sp, app_colors.prefTitle, FontWeight.Bold),
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

private data class FavoriteSpec(
    val id: String,
    @param:DrawableRes val photoRes: Int? = null,
    val photoSrc: String? = null,
    val name: String,
    val type: String,
    val rating: String,
    val distance: String,
    val price: String,
    val tag: String,
    val tagBg: Color,
    val tagText: Color,
    val saved: Boolean = true,
    val nameSize: TextUnit = 12.sp,
    val tagSize: TextUnit = 8.sp
)

private fun resolveFavoriteSpec(
    placeId: String,
    allPlaces: List<Place>,
    colorPair: Pair<Color, Color>
): FavoriteSpec {
    val figmaPlace = HomePlace.fromKey(placeId)
    val dbPlace = allPlaces.firstOrNull { it.id == placeId || (figmaPlace != null && it.figma == figmaPlace) }

    val name = dbPlace?.name ?: figmaPlace?.title ?: when (placeId) {
        "juanvaldez" -> "Juan Valdez"
        "starbucks" -> "Starbucks"
        "tostao" -> "Tostao"
        "wok" -> "Wok"
        "crepes" -> "Crepes & Waffles"
        "goyurt" -> "Goyurt"
        "bbc" -> "BBC Cervecería"
        "museo-del-oro", "museooro" -> "Museo del Oro"
        else -> placeId.replace('-', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    val photoRes: Int? = when (placeId) {
        "juanvaldez" -> R.drawable.fav_juan_valdez
        "starbucks" -> R.drawable.fav_starbucks
        "tostao" -> R.drawable.fav_tostao
        "wok" -> R.drawable.fav_wok
        "crepes" -> R.drawable.fav_crepes
        "goyurt" -> R.drawable.fav_goyurt
        else -> null
    }

    val photoSrc: String? = if (photoRes != null) null else dbPlace?.photo

    val type = dbPlace?.category?.singular ?: when (placeId) {
        "juanvaldez", "starbucks", "tostao" -> "Cafetería"
        "wok", "crepes" -> "Restaurante"
        "goyurt" -> "Postres"
        "bbc", "beerbar" -> "Bar"
        "museo-del-oro", "museonacional", "museooro" -> "Museo"
        else -> "Lugar"
    }

    val ratingVal = 4.2 + (kotlin.math.abs(placeId.hashCode()) % 7) / 10.0
    val rating = String.format(java.util.Locale.US, "%.1f", ratingVal)
    val distance = dbPlace?.let { "${it.walkMin}\nmin" } ?: "0.6\nkm"
    val price = dbPlace?.price?.takeIf { it.isNotBlank() } ?: "$$"
    val tag = when {
        dbPlace?.group == true -> "Ideal para grupos"
        dbPlace?.quiet == true -> "Ambiente tranquilo"
        dbPlace?.study == true -> "Zona de estudio"
        dbPlace?.solo == true -> "Plan individual"
        placeId == "wok" -> "Ideal para parejas"
        placeId == "crepes" -> "Ambiente agradable"
        placeId in listOf("tostao", "goyurt") -> "Económico"
        else -> "Recomendado"
    }

    return FavoriteSpec(
        id = placeId,
        photoRes = photoRes,
        photoSrc = photoSrc,
        name = name,
        type = type,
        rating = rating,
        distance = distance,
        price = price,
        tag = tag,
        tagBg = colorPair.first,
        tagText = colorPair.second,
        saved = true,
        nameSize = if (name.length > 14) 10.sp else 12.sp,
        tagSize = if (tag.length > 15) 7.sp else 8.sp
    )
}

/** Figma: "Lugares Favoritos" (209:251). */
@Composable
fun FavoritePlacesScreen(
    onBack: () -> Unit = {},
    onPlace: (String) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val favorites = rememberFavorites()
    val allPlaces = rememberPlaces()

    val colorPairs = listOf(
        app_colors.favTagGreenBg to app_colors.favTagGreenText,
        app_colors.favTagBlueBg to app_colors.favTagBlueText,
        app_colors.favTagPurpleBg to app_colors.favTagPurpleText
    )

    // Los 6 recuadros de prueba fueron eliminados. Solo se muestran los guardados por el usuario.
    val favoriteSpecs = remember(favorites, allPlaces) {
        favorites.toList().mapIndexed { index, id ->
            resolveFavoriteSpec(id, allPlaces, colorPairs[index % colorPairs.size])
        }
    }

    val rowsCount = (favoriteSpecs.size + 2) / 3
    val cardsBottom = if (rowsCount == 0) 180.dp else (207.38.dp + 197.48.dp * rowsCount - 10.dp)
    val sparkleTop = if (rowsCount == 0) 380.dp else cardsBottom + 36.dp
    val designHeight = maxOf(913.dp, sparkleTop + 340.dp)

    ProfileCanvas(designHeight = designHeight) {
        // "Background Corner Ornaments" inferior: franja amarilla que sale por la derecha
        Box(
            Modifier
                .figmaPosition(left = 22.dp, right = (-45).dp, bottom = (-10.88).dp, height = 98.dp)
                .background(app_colors.profileYellow, RoundedCornerShape(topEnd = 76.dp, bottomEnd = 70.dp))
        )
        ProfileCorner(app_colors.formBlue)
        Box(
            Modifier
                .figmaPosition(left = (-40).dp, bottom = (-57).dp, width = 170.dp, height = 170.dp)
                .background(app_colors.profileCorner, RoundedCornerShape(topEnd = 110.dp))
        )
        BackButton(
            onClick = onBack,
            modifier = Modifier.figmaPosition(right = 28.dp, top = 32.dp),
            res = R.drawable.ic_back_blue
        )

        Text(
            text = "Mis lugares favoritos",
            style = jakartaText(26.sp, 32.5.sp, app_colors.favTitle, FontWeight.ExtraBold, letterSpacing = (-0.65).sp),
            modifier = Modifier.figmaPosition(left = 22.dp, top = 97.dp)
        )
        Text(
            text = "Aquí encuentras todos los lugares que has\nguardado para visitar más tarde.",
            style = jakartaText(14.sp, 22.75.sp, app_colors.favSubtitle, FontWeight.Medium),
            modifier = Modifier.figmaPosition(left = 22.dp, top = 137.38.dp)
        )

        // Si no hay favoritos, queda el espacio vacío. Si hay, se muestran en cuadrícula de 3 columnas
        favoriteSpecs.chunked(3).forEachIndexed { index, row ->
            Row(
                modifier = Modifier.figmaPosition(left = 22.dp, right = 15.dp, top = 207.38.dp + 197.48.dp * index),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { item ->
                    FavoriteCard(
                        spec = item,
                        onClick = { onPlace(item.id) },
                        onToggleSave = { FavoriteStore.toggle(context, item.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        // Bloque final "¡Sigue explorando!" ubicado centrado y uniforme en la parte blanca de la interfaz
        Column(
            modifier = Modifier
                .figmaPosition(left = 0.dp, right = 0.dp, top = sparkleTop)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FavoriteSparkle()
            Spacer(Modifier.height(14.dp))
            Text(
                text = "¡Sigue explorando!",
                style = jakartaText(20.sp, 28.sp, app_colors.favTitle, FontWeight.ExtraBold, letterSpacing = (-0.5).sp, textAlign = TextAlign.Center),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Guarda más lugares y crea tu lista\npersonal de favoritos.",
                style = jakartaText(14.sp, 22.75.sp, app_colors.favSubtitle, FontWeight.Medium, textAlign = TextAlign.Center),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FavoriteCard(
    spec: FavoriteSpec,
    onClick: () -> Unit = {},
    onToggleSave: () -> Unit = {},
    modifier: Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val small = jakartaText(10.sp, 15.sp, app_colors.profilePrice)
    Column(
        modifier = modifier
            .height(187.48.dp)
            .dropShadow(shape, Shadow(radius = 10.dp, color = Color(0x0F000000), offset = DpOffset(0.dp, 2.dp)))
            .clip(shape)
            .background(app_colors.white)
            .border(2.dp, app_colors.favCardBorder, shape)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        // "Place Thumbnail"
        Box(
            Modifier
                .fillMaxWidth()
                .height(71.48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(app_colors.galleryBorder)
        ) {
            if (spec.photoRes != null) {
                Image(
                    painter = painterResource(spec.photoRes),
                    contentDescription = spec.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (!spec.photoSrc.isNullOrBlank()) {
                SceneImage(
                    src = spec.photoSrc,
                    modifier = Modifier.fillMaxSize(),
                    crop = true
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(app_colors.galleryBorder)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleSave
                    )
                    .padding(6.dp)
            ) {
                Image(
                    painter = painterResource(if (spec.saved) R.drawable.ic_fav_bookmark_on else R.drawable.ic_fav_bookmark_off),
                    contentDescription = if (spec.saved) "Guardado" else "Guardar",
                    modifier = Modifier.size(20.dp, 24.dp)
                )
            }
        }
        // "Content": padding [8, 4, 0, 4] y separación de 2
        Column(
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = spec.name,
                style = jakartaText(spec.nameSize, 18.sp, app_colors.favTitle, FontWeight.Bold),
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_fav_type),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(text = spec.type, style = small, maxLines = 1)
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_fav_star),
                    contentDescription = null,
                    modifier = Modifier.size(11.dp, 16.5.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(text = spec.rating, style = jakartaText(10.sp, 15.sp, app_colors.favRating, FontWeight.SemiBold))
                Spacer(Modifier.width(4.dp))
                Text(text = "•", style = jakartaText(10.sp, 15.sp, app_colors.favDot, FontWeight.SemiBold))
                Spacer(Modifier.width(4.dp))
                Image(
                    painter = painterResource(R.drawable.ic_fav_pin),
                    contentDescription = null,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(text = spec.distance, style = small)
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = spec.price, style = jakartaText(10.sp, 15.sp, app_colors.profilePrice, FontWeight.SemiBold))
            Spacer(Modifier.width(4.dp))
            Text(
                text = spec.tag,
                style = jakartaText(spec.tagSize, 10.sp, spec.tagText, FontWeight.SemiBold, textAlign = TextAlign.Center),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .wrapContentWidth(Alignment.Start, unbounded = true)
                    .background(spec.tagBg, CircleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun PreferencesPreview() {
    PreferencesScreen()
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun FavoritePlacesPreview() {
    FavoritePlacesScreen()
}

/**
 * "Bookmark Sparkle Central Icon": marcador con cinco destellos. En Figma está construido con
 * bordes de marco (no con vectores), así que se dibuja con las mismas medidas en un lienzo de 72 x 61.
 */
@Composable
private fun FavoriteSparkle(modifier: Modifier = Modifier) {
    val teal = app_colors.profileCorner
    val navy = app_colors.favTitle
    Canvas(modifier.size(72.dp, 61.dp)) {
        val u = 1.dp.toPx()
        fun ray(cx: Float, cy: Float, w: Float, h: Float, degrees: Float) {
            rotate(degrees, Offset(cx * u, cy * u)) {
                drawRoundRect(teal, Offset((cx - w / 2) * u, (cy - h / 2) * u), Size(w * u, h * u), CornerRadius(2 * u))
            }
        }
        ray(18f, 10f, 4f, 12f, -45f)
        ray(36f, 7f, 4f, 14f, 0f)
        ray(54f, 10f, 4f, 12f, 45f)
        ray(6f, 30f, 12f, 4f, 0f)
        ray(66f, 30f, 12f, 4f, 0f)
        // "Main Bookmark Badge": marco de 40 x 49 con borde interior de 3
        drawRoundRect(
            navy, Offset(17.5f * u, 13.5f * u), Size(37 * u, 46 * u), CornerRadius(1.5f * u),
            style = Stroke(3 * u)
        )
        // "Border": triángulo blanco que forma la muesca inferior del marcador
        drawPath(
            Path().apply {
                moveTo(21 * u, 58 * u); lineTo(36 * u, 46 * u); lineTo(51 * u, 58 * u); close()
            },
            Color.White
        )
    }
}
