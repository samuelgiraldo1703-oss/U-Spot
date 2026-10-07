package screens

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.u_spot.R
import decorations.FigmaCanvas
import decorations.RotatedImage
import decorations.Sparkles
import decorations.app_colors
import decorations.app_fonts
import decorations.figmaPosition
import decorations.figmaText

private data class CategorySpec(
    val label: String,
    @param:DrawableRes val iconRes: Int,
    val bgColor: Color,
    val borderColor: Color
)

private data class TagSpec(
    val text: String,
    val bgColor: Color,
    val textColor: Color
)

/**
 * Pantalla Principal (Home) mostrada inmediatamente después de iniciar sesión.
 */
@Composable
fun HomeScreen(
    onLogout: () -> Unit = {}
) {
    val categories = remember {
        listOf(
            CategorySpec("Restaurantes", R.drawable.ic_cat_restaurantes, app_colors.catRestaurantBg, app_colors.catRestaurantBorder),
            CategorySpec("Cafés", R.drawable.ic_cat_cafes, app_colors.catCafeBg, app_colors.catCafeBorder),
            CategorySpec("Bares", R.drawable.ic_cat_bares, app_colors.catBarBg, app_colors.catBarBorder),
            CategorySpec("Teatros", R.drawable.ic_cat_teatros, app_colors.catTheaterBg, app_colors.catTheaterBorder),
            CategorySpec("Ocio", R.drawable.ic_cat_ocio, app_colors.catOcioBg, app_colors.catOcioBorder)
        )
    }

    FigmaCanvas(designHeight = 917.dp, background = app_colors.white) {
        // Logo superior izquierdo "U-Spot / Descubre. Disfruta. Conecta."
        RotatedImage(
            res = R.drawable.logo_login,
            frameWidth = 262.dp,
            frameHeight = 86.dp,
            rotation = -0.3f,
            imageWidth = 260.dp,
            imageHeight = 258.dp,
            imageX = (-25).dp,
            imageY = (-73).dp,
            clipFrame = true,
            containerModifier = Modifier.figmaPosition(
                left = 18.dp,
                top = 22.dp,
                width = 262.dp,
                height = 86.dp
            )
        )

        // Forma turquesa superior derecha con botón de cerrar sesión / salir
        RotatedImage(
            res = R.drawable.shape_teal,
            frameWidth = 231.268.dp,
            frameHeight = 186.169.dp,
            rotation = -30f,
            imageWidth = 215.778.dp,
            imageHeight = 178.453.dp,
            imageY = 0.503.dp,
            containerModifier = Modifier.figmaPosition(
                right = (-145).dp,
                top = (-135).dp,
                width = 293.369.dp,
                height = 276.861.dp
            )
        )
        Box(
            modifier = Modifier
                .figmaPosition(right = 12.dp, top = 10.dp, width = 44.dp, height = 44.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onLogout
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logout),
                contentDescription = "Cerrar sesión",
                modifier = Modifier.size(34.dp)
            )
        }

        // Saludo principal ("Hola, Alejandro 👋" / "¿Qué quieres hacer hoy?")
        Column(
            modifier = Modifier.figmaPosition(left = 18.dp, top = 122.dp)
        ) {
            Text(
                text = "Hola, Alejandro \uD83D\uDC4B",
                style = figmaText(
                    family = app_fonts.inter,
                    size = 24.sp,
                    lineHeight = 30.sp,
                    color = app_colors.homeTitle,
                    weight = FontWeight.Bold
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "¿Qué quieres hacer hoy?",
                style = figmaText(
                    family = app_fonts.inter,
                    size = 15.sp,
                    lineHeight = 20.sp,
                    color = app_colors.homeSubtitle,
                    weight = FontWeight.Normal
                )
            )
        }

        // Nota manuscrita derecha ("La mejor experiencia está cerca ↗" + "Ver todos →")
        Image(
            painter = painterResource(R.drawable.note_experiencia),
            contentDescription = "La mejor experiencia está cerca",
            contentScale = ContentScale.Fit,
            modifier = Modifier.figmaPosition(
                right = 18.dp,
                top = 110.dp,
                width = 106.dp,
                height = 84.dp
            )
        )
        Text(
            text = "Ver todos \u2192",
            style = figmaText(
                family = app_fonts.inter,
                size = 13.sp,
                lineHeight = 18.sp,
                color = app_colors.homeTeal,
                weight = FontWeight.Bold
            ),
            modifier = Modifier.figmaPosition(right = 16.dp, top = 198.dp)
        )

        // Fila de 5 categorías (Restaurantes, Cafés, Bares, Teatros, Ocio)
        Row(
            modifier = Modifier.figmaPosition(left = 10.dp, right = 10.dp, top = 226.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            categories.forEach { cat ->
                CategoryItem(spec = cat)
            }
        }

        // Sección "Cerca de ti"
        Row(
            modifier = Modifier.figmaPosition(left = 14.dp, top = 344.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_location_pin),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Cerca de ti",
                style = figmaText(
                    family = app_fonts.inter,
                    size = 19.sp,
                    lineHeight = 24.sp,
                    color = app_colors.homeTitle,
                    weight = FontWeight.Bold
                )
            )
        }

        // Tarjetas de "Cerca de ti" (Juan Valdez Café y La Embajada)
        Row(
            modifier = Modifier.figmaPosition(left = 6.dp, right = 6.dp, top = 376.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NearbyVenueCard(
                photoRes = R.drawable.photo_juan_valdez,
                distanceText = "A 0.6 km",
                title = "Juan Valdez Café",
                priceText = "$$",
                tags = listOf(
                    TagSpec("Cafetería", app_colors.tagTealBg, app_colors.tagTealText),
                    TagSpec("Ambiente tranquilo", app_colors.tagBlueBg, app_colors.tagBlueText)
                ),
                modifier = Modifier.weight(1f)
            )
            NearbyVenueCard(
                photoRes = R.drawable.photo_la_embajada,
                distanceText = "A 1.2 km",
                title = "La Embajada",
                priceText = "$$",
                tags = listOf(
                    TagSpec("Bar", app_colors.tagTealBg, app_colors.tagTealText),
                    TagSpec("Ideal para grupos", app_colors.tagTealBg, app_colors.tagTealText)
                ),
                modifier = Modifier.weight(1f)
            )
        }

        // Sección "Recomendado para ti"
        Row(
            modifier = Modifier.figmaPosition(left = 18.dp, top = 620.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_sparkle_outline),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Recomendado para ti",
                style = figmaText(
                    family = app_fonts.inter,
                    size = 19.sp,
                    lineHeight = 24.sp,
                    color = app_colors.homeTitle,
                    weight = FontWeight.Bold
                )
            )
        }

        // Tarjeta horizontal "El Mono Bandido"
        RecommendedVenueCard(
            modifier = Modifier.figmaPosition(left = 18.dp, right = 26.dp, top = 656.dp)
        )

        // Barra de navegación inferior ("Spotty")
        SpottyBottomBar(
            modifier = Modifier.figmaPosition(
                left = 18.dp,
                right = 6.dp,
                bottom = 0.dp,
                height = 84.dp
            )
        )
    }
}

@Composable
private fun CategoryItem(spec: CategorySpec) {
    val boxShape = RoundedCornerShape(18.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(74.dp)
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .clip(boxShape)
                .background(spec.bgColor)
                .border(1.dp, spec.borderColor, boxShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(spec.iconRes),
                contentDescription = spec.label,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = spec.label,
            style = figmaText(
                family = app_fonts.inter,
                size = 11.5.sp,
                lineHeight = 15.sp,
                color = app_colors.homeSubtitle,
                weight = FontWeight.SemiBold
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun NearbyVenueCard(
    @DrawableRes photoRes: Int,
    distanceText: String,
    title: String,
    priceText: String,
    tags: List<TagSpec>,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(16.dp)
    val imageShape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = cardShape, ambientColor = Color(0x14000000), spotColor = Color(0x14000000))
            .clip(cardShape)
            .background(app_colors.white)
            .border(1.dp, app_colors.homeCardBorder, cardShape)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
                .clip(imageShape)
        ) {
            Image(
                painter = painterResource(photoRes),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Etiqueta de distancia ("A 0.6 km" / "A 1.2 km") nítida sobre la esquina inferior izquierda
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 7.dp, bottom = 7.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(app_colors.homeBadgeTeal)
                    .padding(horizontal = 9.dp, vertical = 3.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = distanceText,
                    style = figmaText(
                        family = app_fonts.inter,
                        size = 11.sp,
                        lineHeight = 14.sp,
                        color = app_colors.white,
                        weight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = title,
            style = figmaText(
                family = app_fonts.inter,
                size = 14.sp,
                lineHeight = 18.sp,
                color = app_colors.homeTitle,
                weight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = priceText,
            style = figmaText(
                family = app_fonts.inter,
                size = 12.sp,
                lineHeight = 16.sp,
                color = app_colors.homeSubtitle,
                weight = FontWeight.Medium
            )
        )

        Spacer(Modifier.height(22.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tags.forEach { tag ->
                VenueTagPill(tag)
            }
        }
    }
}

@Composable
private fun RecommendedVenueCard(modifier: Modifier = Modifier) {
    val cardShape = RoundedCornerShape(16.dp)
    val imageShape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = cardShape, ambientColor = Color(0x14000000), spotColor = Color(0x14000000))
            .clip(cardShape)
            .background(app_colors.white)
            .border(1.dp, app_colors.homeCardBorder, cardShape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.photo_mono_bandido),
            contentDescription = "El Mono Bandido",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 138.dp, height = 130.dp)
                .clip(imageShape)
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "El Mono Bandido",
                style = figmaText(
                    family = app_fonts.inter,
                    size = 16.sp,
                    lineHeight = 20.sp,
                    color = app_colors.homeTitle,
                    weight = FontWeight.Bold
                )
            )

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "\u2022 $$ \u2022 ",
                    style = figmaText(
                        family = app_fonts.inter,
                        size = 12.sp,
                        lineHeight = 16.sp,
                        color = app_colors.homeSubtitle,
                        weight = FontWeight.Medium
                    )
                )
                Icon(
                    painter = painterResource(R.drawable.ic_location_pin),
                    contentDescription = null,
                    tint = app_colors.homeTeal,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = "1.4 km",
                    style = figmaText(
                        family = app_fonts.inter,
                        size = 12.sp,
                        lineHeight = 16.sp,
                        color = app_colors.homeTeal,
                        weight = FontWeight.Bold
                    )
                )
            }

            Spacer(Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                VenueTagPill(TagSpec("Bar", app_colors.tagTealBg, app_colors.tagTealText))
                VenueTagPill(TagSpec("Grupos", app_colors.tagIndigoBg, app_colors.tagIndigoText))
            }

            Spacer(Modifier.height(5.dp))

            VenueTagPill(TagSpec("Ambiente genial", app_colors.tagAmberBg, app_colors.tagAmberText))

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Perfecto para salir con\namigos. Buen ambiente,...",
                style = figmaText(
                    family = app_fonts.inter,
                    size = 11.5.sp,
                    lineHeight = 16.sp,
                    color = app_colors.homeSubtitle,
                    weight = FontWeight.Normal
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun VenueTagPill(tag: TagSpec) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(tag.bgColor)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tag.text,
            style = figmaText(
                family = app_fonts.inter,
                size = 10.sp,
                lineHeight = 13.sp,
                color = tag.textColor,
                weight = FontWeight.SemiBold
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun SpottyBottomBar(modifier: Modifier = Modifier) {
    val barShape = RoundedCornerShape(
        topStart = 46.dp,
        topEnd = 46.dp,
        bottomStart = 20.dp,
        bottomEnd = 20.dp
    )
    val pillBrush = Brush.horizontalGradient(
        colors = listOf(app_colors.spottyGradStart, app_colors.spottyGradEnd)
    )

    Box(
        modifier = modifier
            .clip(barShape)
            .background(app_colors.bottomBarBlue)
            .border(2.dp, app_colors.black, barShape)
            .padding(horizontal = 32.dp)
    ) {
        // Icono "+" izquierdo
        Image(
            painter = painterResource(R.drawable.ic_nav_plus),
            contentDescription = "Agregar",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 8.dp)
                .size(48.dp)
        )

        // Botón central "Spotty" con píldora degradada y destellos
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(width = 144.dp, height = 46.dp)
                    .clip(CircleShape)
                    .background(pillBrush)
                    .border(2.dp, app_colors.black, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Sparkles(Modifier.size(38.dp))
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Spotty",
                style = figmaText(
                    family = app_fonts.inter,
                    size = 19.sp,
                    lineHeight = 22.sp,
                    color = app_colors.sparkleYellow,
                    weight = FontWeight.Bold
                )
            )
        }

        // Icono de perfil derecho
        Image(
            painter = painterResource(R.drawable.ic_nav_profile),
            contentDescription = "Perfil",
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-6).dp)
                .size(44.dp)
        )
    }
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun HomePreview() {
    HomeScreen()
}
