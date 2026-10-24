package screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.u_spot.R
import data.BIO_MAX_LENGTH
import data.UserProfile
import data.FavoriteStore
import data.HomeCategory
import data.Place
import data.rememberFavorites
import data.rememberPlaces
import data.spotty.SpottyBudget
import data.spotty.SpottyGeo
import decorations.SceneImage
import decorations.rememberPlacePhoto
import decorations.singular
import androidx.compose.runtime.key
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.ui.platform.LocalContext
import decorations.BackButton
import decorations.ProfileCanvas
import decorations.ProfileCorner
import decorations.app_colors
import decorations.figmaLineBox
import decorations.figmaPosition
import decorations.jakartaText
import decorations.profilePhotoPainter
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization

private data class ProfileMenuSpec(
    @param:DrawableRes val iconRes: Int,
    val iconBg: Color,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

/** Orden de las categorías en "Recomendaciones": una tarjeta por categoría, para que sean variadas. */
private val RECOMMENDATION_ORDER = listOf(
    HomeCategory.CAFES, HomeCategory.BARES, HomeCategory.OCIO, HomeCategory.RESTAURANTES,
    HomeCategory.MUSEOS, HomeCategory.TEATROS
)

/**
 * Recomendaciones del perfil con lugares reales: el mejor de cada categoría por cercanía a la
 * Tadeo y precio, sin los que el usuario ya tiene en favoritos. Entre los 3 mejores de cada
 * categoría se rota uno distinto cada día, para que la sección no sea siempre igual.
 */
private fun recommendedPlaces(places: List<Place>, favorites: Set<String>): List<Place> {
    val day = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
    fun score(p: Place): Double {
        val meters = SpottyGeo.meters(SpottyGeo.UTADEO, p) ?: 3000
        val price = SpottyBudget.pricePerPerson(p)?.let { (it.first + it.last) / 2 } ?: 60000
        return meters / 1000.0 + price / 25000.0
    }
    return RECOMMENDATION_ORDER.mapNotNull { category ->
        val top = places.filter { it.category == category && it.id !in favorites && it.lat != null }
            .sortedBy(::score)
            .take(3)
        top.getOrNull(day % top.size.coerceAtLeast(1))
    }
}

/** "1.2 km" / "450 m" desde la Tadeo, como en la tarjeta de Figma. */
private fun Place.distanceFromUtadeo(): String =
    SpottyGeo.meters(SpottyGeo.UTADEO, this)?.let { SpottyGeo.label(it).removePrefix("A ") } ?: distance

/** "$", "$$" o "$$$" según lo que gasta una persona. */
private fun Place.priceLevel(): String =
    when (val min = SpottyBudget.pricePerPerson(this)?.first) {
        null -> "$$"
        in 0 until 15000 -> "$"
        in 15000 until 35000 -> "$$"
        else -> "$$$"
    }

/** Figma: "Bienvenida Perfil App" (202:6). */
@Composable
fun ProfileScreen(
    user: UserProfile = UserProfile.Preview,
    onBack: () -> Unit = {},
    onChangePhoto: () -> Unit = {},
    onFavorites: () -> Unit = {},
    onAccountSettings: () -> Unit = {},
    onPreferences: () -> Unit = {},
    onHelp: () -> Unit = {},
    /** Abre el detalle de un lugar recomendado (Place.id). */
    onOpenPlace: (String) -> Unit = {},
    onSaveBio: (String) -> Unit = {}
) {
    val context = LocalContext.current.applicationContext
    val places = rememberPlaces()
    val favorites = rememberFavorites()
    val menu = listOf(
        ProfileMenuSpec(
            R.drawable.ic_profile_menu_heart, app_colors.profileMenuFavoritesBg,
            "Mis lugares favoritos", "Todos los que has guardado", onFavorites
        ),
        ProfileMenuSpec(
            R.drawable.ic_profile_menu_settings, app_colors.profileMenuSettingsBg,
            "Configuración de la cuenta", "Correo, contraseña y más", onAccountSettings
        ),
        ProfileMenuSpec(
            R.drawable.ic_profile_menu_preferences, app_colors.profileMenuPreferencesBg,
            "Preferencias", "Intereses y estilo de vida", onPreferences
        ),
        ProfileMenuSpec(
            R.drawable.ic_profile_menu_help, app_colors.profileMenuHelpBg,
            "Ayuda y soporte", "Preguntas frecuentes y contacto", onHelp
        )
    )
    // Se calcula al entrar (con los favoritos de ese momento): si el usuario guarda una tarjeta,
    // se queda en su sitio con el corazón lleno en vez de desaparecer.
    val recommendations = remember(places) { recommendedPlaces(places, favorites) }

    ProfileCanvas(designHeight = 916.dp) {
        ProfileCorner(app_colors.profileCorner)
        BackButton(
            onClick = onBack,
            modifier = Modifier.figmaPosition(right = 28.dp, top = 32.dp),
            res = R.drawable.ic_back_teal
        )

        // "App Fondo Blanco 4": la imagen del logo se recorta dentro de un marco de 204 x 58
        Box(
            Modifier
                .figmaPosition(left = 14.dp, top = 27.dp, width = 204.dp, height = 58.dp)
                .clip(RoundedCornerShape(43.dp))
        ) {
            Image(
                painter = painterResource(R.drawable.logo_login),
                contentDescription = "U-Spot",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .offset((-0.12).dp, (-71).dp)
                    .requiredSize(204.22.dp, 203.dp)
            )
        }

        // "UserProfileSection"
        Row(
            modifier = Modifier
                .figmaPosition(left = 0.dp, right = 0.dp, top = 110.5.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(user.photo, onChangePhoto)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.name,
                        style = jakartaText(
                            20.sp, 28.sp, app_colors.profileNavy,
                            FontWeight.Bold, letterSpacing = (-0.5).sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(app_colors.profileTeal, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_profile_check),
                            contentDescription = "Verificado",
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
                Text(
                    text = user.email,
                    style = jakartaText(13.sp, 19.5.sp, app_colors.profileMuted, FontWeight.Medium)
                )
                EditableBio(
                    bio = user.bio,
                    onSave = onSaveBio,
                    modifier = Modifier.padding(top = 4.19.dp, bottom = 0.56.dp)
                )
            }
        }

        // Tarjeta resumen de 4 columnas. Los anchos de Figma (80.81 / 91.56) y la separación
        val favorites = data.rememberFavorites()
        Row(
            modifier = Modifier
                .figmaPosition(left = 20.dp, right = 20.dp, top = 214.75.dp)
                .background(app_colors.profileStatsBg, RoundedCornerShape(16.dp))
                .padding(horizontal = 8.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatColumn(R.drawable.ic_profile_stat_pin, "${favorites.size}", "Lugares\nguardados")
            Spacer(Modifier.weight(7.33f))
            StatColumn(R.drawable.ic_profile_stat_star, "4.8", "Calificación\npromedio")
            Spacer(Modifier.weight(7.33f))
            StatColumn(R.drawable.ic_profile_stat_plans, "6", "Planes\nrealizados")
            Spacer(Modifier.weight(7.33f))
            StatColumn(
                R.drawable.ic_profile_stat_book, "Est. en UTADEO", "Sem. 6",
                weight = 91.56f, valueSize = 11.sp, valueLineHeight = 13.75.sp, divider = false
            )
        }

        // "Section - 4 Rounded Menu Cards"
        Column(
            modifier = Modifier.figmaPosition(left = 20.dp, right = 20.dp, top = 321.75.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            menu.forEach { ProfileMenuCard(it) }
        }

        // "SavedPlacesSection"
        Row(
            modifier = Modifier.figmaPosition(left = 20.dp, right = 20.dp, top = 655.75.dp, height = 25.5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recomendaciones",
                style = jakartaText(17.sp, 25.5.sp, app_colors.profileNavy, FontWeight.Bold)
            )
        }
        Row(
            modifier = Modifier
                .figmaPosition(left = 0.dp, right = 0.dp, top = 693.25.dp, height = 165.25.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            recommendations.forEach { place ->
                RecommendationCard(
                    place = place,
                    isFavorite = place.id in favorites,
                    onClick = { onOpenPlace(place.id) },
                    onFavorite = { FavoriteStore.toggle(context, place.id) }
                )
            }
        }
    }
}

/**
 * Descripción del perfil. Al tocarla se edita en el mismo lugar (mismo estilo de Figma) y se
 * guarda al pulsar "Listo" en el teclado o al salir del campo. Máximo [BIO_MAX_LENGTH] caracteres.
 */
@Composable
private fun EditableBio(bio: String, onSave: (String) -> Unit, modifier: Modifier) {
    val style = jakartaText(11.5.sp, 14.38.sp, app_colors.profileBio)
    var editing by remember { mutableStateOf(false) }
    var draft by remember(bio) { mutableStateOf(bio) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    var hadFocus by remember { mutableStateOf(false) }

    fun finish() {
        if (!editing) return
        editing = false
        hadFocus = false
        keyboard?.hide()
        if (draft.trim() != bio) onSave(draft.trim())
    }

    if (editing) {
        LaunchedEffect(Unit) { focus.requestFocus() }
        BasicTextField(
            value = draft,
            onValueChange = { draft = it.replace("\n", " ").take(BIO_MAX_LENGTH) },
            textStyle = style,
            maxLines = 2,
            cursorBrush = SolidColor(app_colors.profileTeal),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.Sentences),
            keyboardActions = KeyboardActions(onDone = { finish() }),
            modifier = modifier
                .fillMaxWidth()
                .focusRequester(focus)
                // Al aparecer el campo todavía no tiene el foco: solo se guarda cuando lo pierde
                .onFocusChanged { if (it.isFocused) hadFocus = true else if (hadFocus) finish() },
            decorationBox = { inner ->
                Box {
                    if (draft.isEmpty()) Text("Escribe una frase corta sobre ti", style = style.copy(color = app_colors.profileMuted))
                    inner()
                }
            }
        )
    } else {
        Text(
            text = bio.ifBlank { "Toca aquí para agregar una descripción ✏️" },
            style = if (bio.isBlank()) style.copy(color = app_colors.profileMuted) else style,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { editing = true }
        )
    }
}

/** Foto con aro degradado turquesa y botón "Cambiar foto de perfil". */
@Composable
private fun ProfileAvatar(photo: ImageBitmap?, onChangePhoto: () -> Unit) {
    Box(Modifier.size(84.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .dropShadow(CircleShape, Shadow(radius = 1.dp, color = Color(0x0D000000), offset = DpOffset(0.dp, 1.dp)))
                .background(
                    Brush.linearGradient(
                        colors = listOf(app_colors.profileTeal, app_colors.profileTealLight),
                        start = Offset(0f, Float.POSITIVE_INFINITY),
                        end = Offset(Float.POSITIVE_INFINITY, 0f)
                    ),
                    CircleShape
                )
                .padding(3.dp)
        ) {
            Image(
                painter = profilePhotoPainter(photo),
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .clickable(onClick = onChangePhoto)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(24.dp)
                .dropShadow(CircleShape, Shadow(radius = 3.dp, color = Color(0x1A000000), offset = DpOffset(0.dp, 1.dp)))
                .dropShadow(CircleShape, Shadow(radius = 2.dp, spread = (-1).dp, color = Color(0x1A000000), offset = DpOffset(0.dp, 1.dp)))
                .clip(CircleShape)
                .background(app_colors.profileNavy)
                .border(2.dp, app_colors.white, CircleShape)
                .clickable(onClick = onChangePhoto),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_profile_camera),
                contentDescription = "Cambiar foto de perfil",
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun RowScope.StatColumn(
    @DrawableRes iconRes: Int,
    value: String,
    label: String,
    weight: Float = 80.81f,
    valueSize: TextUnit = 14.sp,
    valueLineHeight: TextUnit = 14.sp,
    divider: Boolean = true
) {
    val dividerColor = app_colors.profileStatsDivider
    Column(
        modifier = Modifier
            .weight(weight)
            .then(
                if (divider) Modifier.drawBehind {
                    val stroke = 1.dp.toPx()
                    val x = size.width - stroke / 2
                    drawLine(dividerColor, Offset(x, 0f), Offset(x, size.height), stroke)
                } else Modifier
            )
            .padding(start = 4.dp, end = if (divider) 5.dp else 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        // En Figma estos textos no hacen salto de línea automático: si no caben, sobresalen centrados.
        Text(
            text = value,
            style = jakartaText(
                valueSize, valueLineHeight, app_colors.profileNavy,
                FontWeight.Bold, textAlign = TextAlign.Center
            ),
            softWrap = false,
            modifier = Modifier
                .wrapContentWidth(unbounded = true)
                .figmaLineBox(valueLineHeight)
        )
        Text(
            text = label,
            style = jakartaText(
                10.sp, 12.5.sp, app_colors.profileMuted,
                FontWeight.Medium, textAlign = TextAlign.Center
            ),
            softWrap = false,
            modifier = Modifier.wrapContentWidth(unbounded = true)
        )
    }
}

@Composable
private fun ProfileMenuCard(spec: ProfileMenuSpec) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .dropShadow(shape, Shadow(radius = 4.dp, color = Color(0x08000000), offset = DpOffset(0.dp, 2.dp)))
            .clip(shape)
            .background(app_colors.white)
            .border(1.dp, app_colors.profileCardBorder, shape)
            .clickable(onClick = spec.onClick)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(spec.iconBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(spec.iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
        // En Figma el bloque de texto queda 1.5 dp por encima del centro del ícono.
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
                .offset(y = (-1.5).dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = spec.title,
                style = jakartaText(14.sp, 19.25.sp, app_colors.profileNavy, FontWeight.Bold),
                maxLines = 1
            )
            Text(
                text = spec.subtitle,
                style = jakartaText(11.sp, 16.5.sp, app_colors.profileMuted, FontWeight.Medium),
                maxLines = 1
            )
        }
        Image(
            painter = painterResource(R.drawable.ic_profile_chevron),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun RecommendationCard(place: Place, isFavorite: Boolean, onClick: () -> Unit, onFavorite: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .size(124.dp, 157.25.dp)
            .dropShadow(shape, Shadow(radius = 2.dp, color = Color(0x0D000000), offset = DpOffset(0.dp, 1.dp)))
            .clip(shape)
            .background(app_colors.white)
            .border(1.dp, app_colors.profileCardBorder, shape)
            .clickable(onClick = onClick, onClickLabel = "Ver ${place.name}")
            .padding(1.dp)
    ) {
        // "Thumbnail with Heart Badge" (sin calificación: no hay estrellas reales)
        Box(
            Modifier
                .fillMaxWidth()
                .height(88.dp)
        ) {
            // Foto propia del lugar (la de su pantalla de Figma o la del Excel); key() porque la
            // de Figma llega después de la genérica de la categoría
            val photo = rememberPlacePhoto(place)
            key(photo) { SceneImage(photo, Modifier.fillMaxSize(), crop = true) }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(app_colors.profileSavedBadge)
                    .clickable(onClick = onFavorite, onClickLabel = if (isFavorite) "Quitar de favoritos" else "Guardar en favoritos"),
                contentAlignment = Alignment.Center
            ) {
                if (isFavorite) {
                    Image(
                        painter = painterResource(R.drawable.ic_profile_heart_filled),
                        contentDescription = "Guardado en favoritos",
                        modifier = Modifier.size(12.dp)
                    )
                } else {
                    Icon(
                        Icons.Outlined.FavoriteBorder,
                        contentDescription = "Guardar en favoritos",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        // "Info Details"
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = place.name,
                    style = jakartaText(12.sp, 16.sp, app_colors.profileNavy, FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (place.category == HomeCategory.TEATROS && Regex("\\bcine").containsMatchIn(place.name.lowercase())) "Cine" else place.category.singular,
                    style = jakartaText(10.sp, 15.sp, app_colors.profileMuted, FontWeight.Medium),
                    maxLines = 1
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_profile_card_pin),
                        contentDescription = null,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = place.distanceFromUtadeo(),
                        style = jakartaText(9.5.sp, 14.25.sp, app_colors.profileMuted, FontWeight.Medium)
                    )
                }
                Text(
                    text = place.priceLevel(),
                    style = jakartaText(9.5.sp, 14.25.sp, app_colors.profilePrice, FontWeight.SemiBold)
                )
            }
        }
    }
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun ProfilePreview() {
    ProfileScreen()
}
