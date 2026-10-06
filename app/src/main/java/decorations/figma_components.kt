package decorations

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.dropShadow
import com.example.u_spot.R

/** Estilo de texto con interlineado igual al CSS de Figma (texto centrado en la línea). */
fun figmaText(
    family: FontFamily,
    size: TextUnit,
    lineHeight: TextUnit,
    color: Color,
    weight: FontWeight = FontWeight.Normal,
    letterSpacing: TextUnit = 0.sp,
    textAlign: TextAlign = TextAlign.Start
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
    color = color,
    textAlign = textAlign,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None
    )
)

/**
 * Imagen dentro de un contenedor de Figma que está rotado (formas orgánicas de las esquinas).
 * [containerModifier] posiciona la caja exterior; la caja interior ([frameWidth] x [frameHeight])
 * se rota sobre su centro y la imagen se dibuja en ([imageX], [imageY]) con su tamaño original.
 */
@Composable
fun RotatedImage(
    @DrawableRes res: Int,
    frameWidth: Dp,
    frameHeight: Dp,
    rotation: Float,
    containerModifier: Modifier,
    imageWidth: Dp = frameWidth,
    imageHeight: Dp = frameHeight,
    imageX: Dp = 0.dp,
    imageY: Dp = 0.dp,
    clipFrame: Boolean = false
) {
    Box(modifier = containerModifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .requiredSize(frameWidth, frameHeight)
                .rotate(rotation)
                .then(if (clipFrame) Modifier.clipToBounds() else Modifier)
        ) {
            Image(
                painter = painterResource(res),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .offset(imageX, imageY)
                    .requiredSize(imageWidth, imageHeight)
            )
        }
    }
}

/** Trazo de un SVG exportado de Figma que necesita algo que VectorDrawable no soporta (trazo punteado). */
class SvgPath(
    val d: String,
    val fill: Color? = null,
    val stroke: Color? = null,
    val strokeWidth: Float = 0f,
    val dash: Float? = null,
    val dx: Float = 0f,
    val dy: Float = 0f
)

@Composable
fun SvgCanvas(
    paths: List<SvgPath>,
    viewportWidth: Float,
    viewportHeight: Float,
    modifier: Modifier
) {
    val parsed = remember(paths) { paths.map { it to PathParser().parsePathString(it.d).toPath() } }
    Canvas(modifier = modifier) {
        scale(size.width / viewportWidth, size.height / viewportHeight, pivot = Offset.Zero) {
            parsed.forEach { (spec, path) ->
                translate(spec.dx, spec.dy) {
                    spec.fill?.let { drawPath(path, it) }
                    spec.stroke?.let {
                        drawPath(
                            path, it,
                            style = Stroke(
                                width = spec.strokeWidth,
                                pathEffect = spec.dash?.let { d -> PathEffect.dashPathEffect(floatArrayOf(d, d)) }
                            )
                        )
                    }
                }
            }
        }
    }
}

// SVG "Sparkles" de Figma (40 x 40): estrella amarilla, verde, azul y trazo punteado.
private val sparklesPaths = listOf(
    SvgPath("M20 0C22 6.66667 26.6667 10 33.3333 13.3333C26.6667 16.6667 22 20 20 26.6667C18 20 13.3333 16.6667 6.66667 13.3333C13.3333 10 18 6.66667 20 0V0", fill = app_colors.sparkleYellow),
    SvgPath("M33.3333 26.6667C34.6667 30 36.6667 32 40 33.3333C36.6667 34.6667 34.6667 36.6667 33.3333 40C32 36.6667 30 34.6667 26.6667 33.3333C30 32 32 30 33.3333 26.6667V26.6667", fill = app_colors.sparkleGreen),
    SvgPath("M10 30C10.6667 32 12 33.3333 14.6667 34C12 34.6667 10.6667 36 10 38C9.33333 36 8 34.6667 5.33333 34C8 33.3333 9.33333 32 10 30V30", fill = app_colors.sparkleBlue),
    SvgPath("M6.66667 20C10 26.6667 16.6667 30 30 23.3333", stroke = app_colors.sparkleDash, strokeWidth = 1.33333f, dash = 2.67f)
)

@Composable
fun Sparkles(modifier: Modifier) {
    SvgCanvas(sparklesPaths, 40f, 40f, modifier)
}

/** Mensaje de error rojo, centrado horizontalmente en su posición de Figma. */
@Composable
fun ErrorMessage(text: String?, color: Color, modifier: Modifier) {
    if (text == null) return
    Text(
        text = text,
        style = figmaText(app_fonts.poppins, 14.sp, 20.sp, color, textAlign = TextAlign.Center),
        modifier = modifier
    )
}

// ---------------------------------------------------------------------------------------------
// Componentes de "Registro" y "Cambia tu contraseña" (tipografía Inter)
// ---------------------------------------------------------------------------------------------

@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_back),
        contentDescription = "Volver",
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    )
}

/** Ícono a la izquierda del campo: recurso, tamaño y posición (x, y) dentro del campo. */
class FieldIcon(@DrawableRes val res: Int, val width: Dp, val height: Dp, val x: Dp, val y: Dp)

val mailIcon = FieldIcon(R.drawable.ic_mail, 16.667.dp, 13.333.dp, 16.dp, 17.69.dp)
val lockIcon = FieldIcon(R.drawable.ic_lock, 14.dp, 16.dp, 16.dp, 15.dp)
val personIcon = FieldIcon(R.drawable.ic_person, 14.dp, 16.dp, 16.dp, 15.dp)

@Composable
fun FormInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: FieldIcon,
    modifier: Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val textStyle = figmaText(app_fonts.inter, 14.sp, 17.sp, app_colors.formTitle)
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .height(46.dp)
            .clip(shape)
            .background(app_colors.formInput)
            .border(1.dp, app_colors.formBorder, shape)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = textStyle,
            cursorBrush = SolidColor(app_colors.formBlue),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Password else keyboardType
            ),
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 41.dp, end = if (isPassword) 41.dp else 17.dp, top = 14.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(placeholder, style = textStyle.copy(color = app_colors.formPlaceholder), maxLines = 1)
                    }
                    innerTextField()
                }
            }
        )
        Image(
            painter = painterResource(icon.res),
            contentDescription = null,
            modifier = Modifier
                .offset(icon.x, icon.y)
                .size(icon.width, icon.height)
        )
        if (isPassword) {
            Image(
                painter = painterResource(R.drawable.ic_eye_off),
                contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .size(20.009.dp, 16.009.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { passwordVisible = !passwordVisible }
            )
        }
    }
}

/** Indicador "Paso 2 de 2". */
@Composable
fun StepProgress(modifier: Modifier) {
    Box(modifier = modifier.height(16.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(app_colors.formBorder)
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.5f)
                .height(2.dp)
                .background(app_colors.formBlue)
        )
        Text(
            text = "Paso 2 de 2",
            style = figmaText(app_fonts.inter, 12.sp, 16.sp, app_colors.formSubtitle, FontWeight.Medium),
            modifier = Modifier
                .background(app_colors.white)
                .padding(horizontal = 12.dp)
        )
    }
}

/** Botón azul principal con flecha a la derecha y su capa de sombra desplazada 14 dp. */
@Composable
fun PrimaryActionButton(text: String, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(modifier = modifier.height(52.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .offset(y = 14.dp)
                .dropShadow(shape, Shadow(radius = 6.dp, spread = (-1).dp, color = Color(0x1A000000), offset = DpOffset(0.dp, 4.dp)))
                .dropShadow(shape, Shadow(radius = 4.dp, spread = (-2).dp, color = Color(0x1A000000), offset = DpOffset(0.dp, 2.dp)))
                .background(app_colors.white, shape)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(app_colors.formBlue)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = figmaText(app_fonts.inter, 16.sp, 24.sp, app_colors.white, FontWeight.SemiBold)
            )
            Image(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-16).dp, y = 18.dp)
                    .size(14.dp, 12.dp)
            )
        }
    }
}

@Composable
fun GoogleFormButton(onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .height(50.dp)
            .clip(shape)
            .background(app_colors.white)
            .border(1.dp, app_colors.formBorder, shape)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_google),
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Continuar con Google",
            style = figmaText(app_fonts.inter, 16.sp, 24.sp, app_colors.formTitle, FontWeight.Medium)
        )
    }
}

/** Logo "App Fondo Blanco 2": en Figma la imagen se estira al tamaño del contenedor. */
@Composable
fun HeaderLogo(modifier: Modifier) {
    Image(
        painter = painterResource(R.drawable.logo_header),
        contentDescription = "U-Spot",
        contentScale = ContentScale.FillBounds,
        modifier = modifier
    )
}
