package decorations

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.u_spot.R

/** Estilo de texto en Plus Jakarta Sans, la tipografía del módulo de perfil. */
fun jakartaText(
    size: TextUnit,
    lineHeight: TextUnit,
    color: Color,
    weight: FontWeight = FontWeight.Normal,
    letterSpacing: TextUnit = 0.sp,
    textAlign: TextAlign = TextAlign.Start
) = figmaText(app_fonts.plusJakarta, size, lineHeight, color, weight, letterSpacing, textAlign)

/** Alto de la barra de estado (hora, batería), que Figma no dibuja. */
@Composable
fun statusBarHeight(): Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

/**
 * Lienzo de las pantallas del módulo de perfil. Igual que [FigmaCanvas], pero el diseño empieza
 * justo debajo de la barra de estado; los adornos pueden dibujarse por detrás de ella usando
 * posiciones negativas (ver [ProfileCorner]).
 */
@Composable
fun ProfileCanvas(
    designHeight: Dp,
    background: Color = app_colors.white,
    content: @Composable BoxScope.() -> Unit
) {
    val top = statusBarHeight()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        val canvasHeight = if (maxHeight - top > designHeight) maxHeight - top else designHeight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(canvasHeight + top)
                    .clipToBounds()
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = top)
                        .fillMaxSize(),
                    content = content
                )
            }
        }
    }
}

/** "Background Corner Ornaments": esquina de color superior derecha, extendida tras la barra de estado. */
@Composable
fun ProfileCorner(color: Color) {
    val top = statusBarHeight()
    Box(
        Modifier
            .figmaPosition(right = (-28).dp, top = (-20).dp - top, width = 140.dp, height = 140.dp + top)
            .background(color, RoundedCornerShape(bottomStart = 90.dp))
    )
}

/** Texto centrado dentro de una caja de tamaño fijo, como los textos de Figma con alineación CENTER/CENTER. */
@Composable
fun BoxedText(text: String, style: TextStyle, modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(text = text, style = style.copy(textAlign = TextAlign.Center))
    }
}

@Composable
fun BoxedText(text: AnnotatedString, style: TextStyle, modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(text = text, style = style.copy(textAlign = TextAlign.Center))
    }
}

/**
 * Capa de una ventana emergente: oscurece la pantalla y bloquea los toques sobre lo que hay
 * detrás. El contenido usa las coordenadas del frame de Figma, bajadas por la barra de estado.
 */
@Composable
fun ProfileOverlay(scrim: Color, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        Box(
            modifier = Modifier
                .padding(top = statusBarHeight())
                .fillMaxSize(),
            content = content
        )
    }
}

/**
 * Logo "App Fondo Blanco": la imagen completa del logo recortada dentro de un marco de 204 x 58
 * con esquinas redondeadas. [imageTop] es el desplazamiento vertical del recorte en cada frame.
 */
@Composable
fun ProfileLogo(modifier: Modifier, imageTop: Dp = (-71).dp) {
    Box(
        modifier
            .size(204.dp, 58.dp)
            .clip(RoundedCornerShape(43.dp))
    ) {
        Image(
            painter = painterResource(R.drawable.logo_login),
            contentDescription = "U-Spot",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .offset((-0.12).dp, imageTop)
                .requiredSize(204.22.dp, 203.dp)
        )
    }
}

/**
 * "FooterDecorativeGraphics": silueta de ciudad desvanecida, ruta punteada y el bloque de color
 * de la esquina inferior izquierda. Va anclado al borde inferior del lienzo.
 */
@Composable
fun BoxScope.ProfileFooter(cornerColor: Color) {
    Image(
        painter = painterResource(R.drawable.footer_skyline),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        alpha = 0.4f,
        modifier = Modifier
            .figmaPosition(left = 0.dp, right = 0.dp, bottom = 0.dp, height = 112.dp)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                // Máscara de Figma: opaca abajo (85 %), 40 % al 60 % del alto y transparente arriba
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.4f to Color.Black.copy(alpha = 0.4f),
                        1f to Color.Black.copy(alpha = 0.85f)
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
    )
    Image(
        painter = painterResource(R.drawable.ic_footer_route),
        contentDescription = null,
        modifier = Modifier.figmaPosition(right = 16.dp, bottom = 20.dp, width = 128.dp, height = 56.dp)
    )
    Box(
        Modifier
            .figmaPosition(left = (-40).dp, bottom = (-54).dp, width = 170.dp, height = 170.dp)
            .background(cornerColor, RoundedCornerShape(topEnd = 110.dp))
    )
}

/**
 * Tarjeta de las ventanas emergentes del módulo ("Mensaje ..."): 360 x 555, centrada, con borde
 * exterior verde de 2 dp y radio 40. El contenido usa coordenadas relativas a la tarjeta.
 */
@Composable
fun BoxScope.ProfileDialogCard(top: Dp, background: Color, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .offset(y = top - 2.dp)
            .size(364.dp, 559.dp)
            .background(app_colors.dialogBorder, RoundedCornerShape(42.dp))
            .padding(2.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(background),
        content = content
    )
}

/** Imagen colocada en su posición de Figma dentro de una tarjeta, aunque sobresalga de sus bordes. */
@Composable
fun ProfileCardImage(@DrawableRes res: Int, x: Dp, y: Dp, width: Dp, height: Dp) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .wrapContentSize(Alignment.TopStart, unbounded = true)
            .offset(x, y)
            .requiredSize(width, height)
    )
}

/** "Action Buttons": botón de 65 dp de alto y radio 12 con su capa de sombra desplazada 14 dp. */
@Composable
fun ProfileDialogButton(
    text: String,
    color: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier,
    width: Dp = 340.dp,
    height: Dp = 65.dp
) {
    val shape = RoundedCornerShape(12.dp)
    Box(modifier = modifier.size(width, height)) {
        Box(
            Modifier
                .fillMaxSize()
                .offset(y = 14.dp)
                .dropShadow(shape, Shadow(radius = 6.dp, spread = (-1).dp, color = Color(0x1A000000), offset = DpOffset(0.dp, 4.dp)))
                .dropShadow(shape, Shadow(radius = 4.dp, spread = (-2).dp, color = Color(0x1A000000), offset = DpOffset(0.dp, 2.dp)))
        )
        BoxedText(
            text = text,
            style = figmaText(app_fonts.inter, 16.sp, 24.sp, textColor, FontWeight.SemiBold),
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(color)
                .clickable(onClick = onClick)
        )
    }
}

/** Tarjeta blanca de fila (borde #E9EEF5, radio 20, sombra suave) usada en ajustes, ayuda y status. */
@Composable
fun ProfileRowCard(modifier: Modifier, onClick: (() -> Unit)? = null, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .dropShadow(shape, Shadow(radius = 18.dp, color = Color(0x0A0F254B), offset = DpOffset(0.dp, 4.dp)))
            .clip(shape)
            .background(app_colors.white)
            .border(1.dp, app_colors.cfgCardBorder, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        content = content
    )
}

/** Ícono de 44 dp (fondo circular incluido en el vector) a 13 dp del borde izquierdo de la fila. */
@Composable
fun BoxScope.ProfileRowIcon(@DrawableRes res: Int) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = Modifier
            .align(Alignment.CenterStart)
            .offset(x = 13.dp)
            .size(44.dp)
    )
}

/**
 * Ventana de confirmación con la imagen de advertencia ("image 27"), un botón amarillo "Volver"
 * y una acción roja debajo. Figma: 401:1104 (borrar foto) y 334:81 / 334:379 / 334:443 (salir del reporte).
 */
@Composable
fun ProfileLeaveDialog(
    question: String,
    warning: String,
    action: String,
    actionX: Dp,
    actionWidth: Dp,
    onBack: () -> Unit,
    onAction: () -> Unit
) {
    val body = figmaText(app_fonts.poppins, 18.sp, 20.sp, app_colors.black)
    ProfileOverlay(scrim = Color(0xB3000000)) {
        ProfileDialogCard(top = 180.dp, background = app_colors.dialogCard) {
            BoxedText(
                text = buildAnnotatedString {
                    append(question)
                    withStyle(SpanStyle(color = app_colors.dialogRed)) { append(warning) }
                },
                style = body,
                modifier = Modifier
                    .offset(4.dp, 130.dp)
                    .size(356.dp, 85.dp)
            )
            Image(
                painter = painterResource(R.drawable.dialog_delete_photo),
                contentDescription = null,
                modifier = Modifier
                    .offset(108.dp, 200.dp)
                    .size(151.dp)
            )
            ProfileDialogButton(
                text = "Volver",
                color = app_colors.dialogAmber,
                textColor = app_colors.black,
                onClick = onBack,
                modifier = Modifier.offset(10.dp, 351.dp)
            )
            BoxedText(
                text = action,
                style = body.copy(color = app_colors.dialogRed),
                modifier = Modifier
                    .offset(actionX, 460.dp)
                    .size(actionWidth, 20.dp)
                    .clickable(onClick = onAction)
            )
        }
    }
}

/** Foto de perfil del usuario o, si no ha subido ninguna, el avatar vacío de Figma (401:1130). */
@Composable
fun profilePhotoPainter(photo: ImageBitmap?): Painter =
    if (photo != null) remember(photo) { BitmapPainter(photo) } else painterResource(R.drawable.photo_avatar_empty)
