package screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.u_spot.R
import data.UserProfile
import decorations.BackButton
import decorations.BoxedText
import decorations.ProfileCanvas
import decorations.ProfileCorner
import decorations.ProfileLogo
import decorations.ProfileRowCard
import decorations.ProfileRowIcon
import decorations.app_colors
import decorations.figmaPosition
import decorations.jakartaText
import decorations.profilePhotoPainter

private val ovalShape = GenericShape { size, _ -> addOval(Rect(0f, 0f, size.width, size.height)) }

/** Figma: "Ayuda y Soporte" (214:1268). */
@Composable
fun HelpSupportScreen(
    onBack: () -> Unit = {},
    onHelpCenter: () -> Unit = {},
    onSecurityStatus: () -> Unit = {},
    onReportProblem: () -> Unit = {}
) {
    ProfileCanvas(designHeight = 917.dp) {
        // "Ellipse 1": foto de la ciudad recortada en óvalo, anclada abajo
        Image(
            painter = painterResource(R.drawable.help_city_oval),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = 11.dp, y = 53.dp)
                .requiredSize(506.dp, 256.dp)
                .clip(ovalShape)
        )
        ProfileCorner(app_colors.profileCorner)
        BackButton(
            onClick = onBack,
            modifier = Modifier.figmaPosition(right = 28.dp, top = 32.dp),
            res = R.drawable.ic_help_back
        )
        ProfileLogo(Modifier.figmaPosition(left = 72.dp, top = 27.dp))

        Text(
            text = "Ayuda y soporte",
            style = jakartaText(26.sp, 39.sp, app_colors.helpTitle, FontWeight.ExtraBold, letterSpacing = (-0.65).sp),
            modifier = Modifier.figmaPosition(left = 18.dp, top = 109.dp)
        )

        HelpCard(
            top = 194.dp, height = 154.dp, icon = R.drawable.ic_help_center, iconTop = 44.dp,
            title = "Centro de ayuda", titleTop = 37.37.dp,
            body = "Encuentra respuestas a\npreguntas frecuentes sobre\nspots, pagos y más.", bodyWidth = 190.dp,
            arrowRight = 12.dp, arrowTop = 58.dp, onClick = onHelpCenter
        ) {
            // "?" dentro de la ilustración (en Figma es texto, no vector)
            Text(
                text = "?",
                style = jakartaText(11.sp, 12.29.sp, app_colors.helpQuestion, FontWeight.Bold),
                modifier = Modifier.offset(47.dp, 65.04.dp)
            )
        }
        HelpCard(
            top = 394.dp, height = 112.88.dp, icon = R.drawable.ic_help_security, iconTop = 23.44.dp,
            title = "Status de seguridad", titleTop = 16.dp,
            body = "Consulta el estado de tu\ncuenta y la seguridad de la\nplataforma.", bodyWidth = 165.05.dp,
            arrowRight = 17.dp, arrowTop = 37.44.dp, onClick = onSecurityStatus
        )
        HelpCard(
            top = 563.dp, height = 112.88.dp, icon = R.drawable.ic_help_report, iconTop = 23.44.dp,
            title = "Reportar un problema", titleTop = 16.dp,
            body = "Ayudanos a reportar un problema usando esta sección de nuestra App", bodyWidth = 232.dp,
            arrowRight = 17.dp, arrowTop = 37.44.dp, onClick = onReportProblem
        )
    }
}

@Composable
private fun BoxScope.HelpCard(
    top: Dp,
    height: Dp,
    @DrawableRes icon: Int,
    iconTop: Dp,
    title: String,
    titleTop: Dp,
    body: String,
    bodyWidth: Dp,
    arrowRight: Dp,
    arrowTop: Dp,
    onClick: () -> Unit,
    extra: @Composable BoxScope.() -> Unit = {}
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .figmaPosition(left = 6.dp, right = 6.dp, top = top, height = height)
            .dropShadow(shape, Shadow(radius = 25.dp, spread = (-4).dp, color = Color(0x14146E8C), offset = DpOffset(0.dp, 10.dp)))
            .dropShadow(shape, Shadow(radius = 10.dp, spread = (-2).dp, color = Color(0x08000000), offset = DpOffset(0.dp, 4.dp)))
            .clip(shape)
            .background(app_colors.helpCard)
            .border(1.dp, app_colors.profileCardBorder, shape)
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier
                .offset(17.dp, iconTop)
                .size(66.dp)
        )
        extra()
        Text(
            text = title,
            style = jakartaText(17.sp, 21.25.sp, app_colors.helpTitle, FontWeight.Bold),
            modifier = Modifier.offset(97.dp, titleTop)
        )
        Text(
            text = body,
            style = jakartaText(13.sp, 17.88.sp, app_colors.helpBody),
            modifier = Modifier
                .offset(97.dp, titleTop + 24.87.dp)
                .width(bodyWidth)
        )
        // "Action Arrow Button"
        Image(
            painter = painterResource(R.drawable.ic_help_arrow),
            contentDescription = title,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = -arrowRight, y = arrowTop)
                .size(38.dp)
                .dropShadow(CircleShape, Shadow(radius = 12.dp, color = Color(0x5912A4B2), offset = DpOffset(0.dp, 4.dp)))
                .clip(CircleShape)
                .clickable(onClick = onClick)
        )
    }
}

private data class FaqSpec(@param:DrawableRes val icon: Int, val question: String, val answer: String, val danger: Boolean = false)

private val faqs = listOf(
    FaqSpec(
        R.drawable.ic_cfg_lock, "¿Cómo cambiar mi contraseña?",
        "Puede cambiar su contraseña en el inicio de sesion de su cuenta en el apartado de “Olvide Mi Contraseña”."
    ),
    FaqSpec(
        R.drawable.ic_cfg_globe, "¿Cómo cambio el idioma de la app?",
        "Puede cambiar el idioma de la aplicacion en la configuracion de su cuenta."
    ),
    FaqSpec(
        R.drawable.ic_cfg_theme, "¿Cómo cambiar mi tema visual?",
        "Podra cambiar su tema visual de oscuro a claro y viceversea en la configuracion de su cuenta seleccionando la opcion que requiera."
    ),
    FaqSpec(
        R.drawable.ic_cfg_shield, "¿Como cambiar mi  privacidad?",
        "Puede cambiar su privacidad de privada a publica y viceversa en la configuracion de su cuenta deslizando el icono hacia la opcion que usted requiera."
    ),
    FaqSpec(
        R.drawable.ic_cfg_danger, "¿Cómo eliminar mi cuenta?",
        "Si desea eliminar su cuenta de U-Spot tiene que dirigirse a la configuracion de su cuenta en el apartado de “Cuenta” para realizar el proceso de eliminacion de cuenta.",
        danger = true
    )
)

/**
 * Figma: "Centro de Ayuda" (255:142) y sus cinco variantes con una respuesta desplegada
 * (401:510, 401:615, 401:713, 401:811, 401:908).
 */
@Composable
fun HelpCenterScreen(onBack: () -> Unit = {}) {
    var expanded by rememberSaveable { mutableIntStateOf(-1) }

    ProfileCanvas(designHeight = 916.dp) {
        // "Ellipse 6": círculo turquesa de 600 que asoma por abajo
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 469.dp)
                .requiredSize(600.dp)
                .background(app_colors.profileCorner, CircleShape)
        )
        ProfileCorner(app_colors.profileYellow)
        BackButton(
            onClick = onBack,
            modifier = Modifier.figmaPosition(right = 26.dp, top = 28.dp),
            res = R.drawable.ic_cfg_back
        )
        BoxedText(
            text = "¿En qué requieres ayuda?",
            style = jakartaText(25.sp, 37.5.sp, app_colors.cfgTitle, FontWeight.ExtraBold, letterSpacing = (-0.62).sp),
            modifier = Modifier.figmaPosition(left = 31.dp, top = 16.dp, width = 243.dp, height = 75.dp)
        )
        Text(
            text = "Soluciones que te podemos brindar...",
            style = jakartaText(17.sp, 25.5.sp, app_colors.cfgHeading, FontWeight.Bold),
            modifier = Modifier.figmaPosition(left = 11.dp, top = 132.dp)
        )

        Column(Modifier.figmaPosition(left = 9.dp, right = 9.dp, top = 177.dp)) {
            faqs.forEachIndexed { index, faq ->
                ProfileRowCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp),
                    onClick = { expanded = if (expanded == index) -1 else index }
                ) {
                    ProfileRowIcon(faq.icon)
                    Text(
                        text = faq.question,
                        style = jakartaText(14.5.sp, 18.13.sp, if (faq.danger) app_colors.cfgDanger else app_colors.cfgHeading, FontWeight.Bold),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = 71.dp)
                    )
                    Image(
                        painter = painterResource(R.drawable.ic_cfg_chevron_down),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = (-13).dp)
                            .size(20.dp)
                    )
                }
                if (expanded == index) {
                    // La respuesta ocupa el hueco entre filas, 3 dp pegada a la fila de arriba
                    Text(
                        text = faq.answer,
                        style = jakartaText(17.sp, 25.5.sp, app_colors.cfgHeading, FontWeight.Bold),
                        modifier = Modifier
                            .offset(y = (-3).dp)
                            .padding(start = 28.dp, end = 30.dp)
                    )
                } else if (index < faqs.lastIndex) {
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

/** Figma: "Status de Seguridad" (257:272). */
@Composable
fun SecurityStatusScreen(user: UserProfile = UserProfile.Preview, onBack: () -> Unit = {}) {
    val rowTitle = jakartaText(14.5.sp, 21.75.sp, app_colors.cfgHeading, FontWeight.Bold)
    val small = jakartaText(10.sp, 21.75.sp, app_colors.cfgHeading, FontWeight.Bold)

    ProfileCanvas(designHeight = 916.dp) {
        Box(
            Modifier
                .figmaPosition(left = (-40).dp, bottom = (-31).dp, width = 130.dp, height = 147.dp)
                .background(app_colors.profileYellow, RoundedCornerShape(topEnd = 110.dp))
        )
        ProfileCorner(app_colors.profileCorner)
        BackButton(
            onClick = onBack,
            modifier = Modifier.figmaPosition(right = 26.dp, top = 28.dp),
            res = R.drawable.ic_status_back
        )
        BoxedText(
            text = "Status de Seguridad",
            style = jakartaText(25.sp, 37.5.sp, app_colors.cfgTitle, FontWeight.ExtraBold, letterSpacing = (-0.62).sp),
            modifier = Modifier.figmaPosition(left = 33.dp, top = 40.dp, width = 247.dp, height = 38.dp)
        )
        Text(
            text = "Datos generales",
            style = jakartaText(17.sp, 25.5.sp, app_colors.cfgHeading, FontWeight.Bold),
            modifier = Modifier.figmaPosition(left = 11.dp, top = 132.dp)
        )

        // Fila del usuario
        ProfileRowCard(Modifier.figmaPosition(left = 9.dp, right = 9.dp, top = 168.dp, height = 88.dp)) {
            Text(
                text = user.name,
                style = jakartaText(20.sp, 28.sp, app_colors.profileNavy, FontWeight.Bold, letterSpacing = (-0.5).sp),
                modifier = Modifier.offset(18.dp, 4.dp)
            )
            Text(
                text = user.email,
                style = jakartaText(13.sp, 19.5.sp, app_colors.profileMuted, FontWeight.Medium),
                modifier = Modifier.offset(18.dp, 33.dp)
            )
            VerifiedBadge(Modifier.align(Alignment.TopEnd).offset(x = (-19).dp, y = 21.dp))
        }
        StatusRow(top = 266.dp, left = 9.dp, icon = R.drawable.ic_status_password, title = "Mi Contraseña")
        StatusRow(top = 336.dp, left = 7.dp, icon = R.drawable.ic_status_phone, title = "Número Telefonico")
        ProfileRowCard(Modifier.figmaPosition(left = 7.dp, right = 11.dp, top = 416.dp, height = 70.dp)) {
            // "Background" azul oscuro con "image 7"
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = 13.dp)
                    .size(44.dp)
                    .background(Color(0xFF0D254C), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.status_address),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .size(24.dp, 28.dp)
                        .clip(RoundedCornerShape(30.dp))
                )
            }
            Text("Direccion de Residencia", style = rowTitle, modifier = Modifier.align(Alignment.CenterStart).offset(x = 71.dp))
            VerifiedBadge(Modifier.align(Alignment.CenterEnd).offset(x = (-15).dp))
        }
        ProfileRowCard(Modifier.figmaPosition(left = 7.dp, right = 11.dp, top = 496.dp, height = 69.dp)) {
            ProfileRowIcon(R.drawable.ic_status_twostep)
            Text(
                text = "Verificación de dos pasos (Opcional)",
                style = rowTitle,
                modifier = Modifier.offset(72.dp, 23.dp)
            )
            // Insignia roja con "x": verificación pendiente
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-13).dp, y = 27.dp)
                    .size(16.dp)
                    .background(app_colors.dialogRed, CircleShape)
                    .border(1.dp, app_colors.black, CircleShape)
            )
            Text(
                text = "x",
                style = jakartaText(14.5.sp, 21.75.sp, app_colors.white, FontWeight.Bold),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-13).dp, y = 21.dp)
                    .width(13.dp)
            )
        }

        // Foto con aro degradado y botón de cámara (mismo componente visual del perfil)
        Box(Modifier.figmaPosition(left = 167.dp, top = 581.dp, width = 84.dp, height = 84.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                    painter = profilePhotoPainter(user.photo),
                    contentDescription = "Foto de perfil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
            Image(
                painter = painterResource(R.drawable.ic_status_camera),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(2.dp, app_colors.white, CircleShape)
            )
        }

        ProfileRowCard(Modifier.figmaPosition(left = 9.dp, right = 9.dp, top = 681.dp, height = 69.dp)) {
            BoxedText(
                text = "Gracias por respetar las condiciones del servicio\ny las  Directivas de la comuindad de U-spot.\nSi infringes las reglas, aparareceran aquí.",
                style = rowTitle,
                modifier = Modifier.fillMaxSize()
            )
        }

        // "Estado de Cuenta": En Orden -> En Peligro -> Suspendida
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(x = 1.dp, y = 773.dp)
                .size(230.dp, 82.dp)
        ) {
            BoxedText("Estado de Cuenta", small, Modifier.align(Alignment.TopCenter).height(22.dp))
            Image(
                painter = painterResource(R.drawable.status_ok),
                contentDescription = "En orden",
                modifier = Modifier
                    .offset(15.dp, 29.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .border(2.dp, app_colors.black, CircleShape)
            )
            listOf(100.dp, 185.dp).forEach { x ->
                Box(
                    Modifier
                        .offset(x, 29.dp)
                        .size(30.dp)
                        .background(app_colors.white, CircleShape)
                        .border(2.dp, app_colors.black, CircleShape)
                )
            }
            listOf(57.dp, 142.dp).forEach { x ->
                Image(
                    painter = painterResource(R.drawable.ic_status_arrow),
                    contentDescription = null,
                    modifier = Modifier
                        .offset(x, 36.54.dp)
                        .size(32.12.dp, 14.92.dp)
                )
            }
            BoxedText("En Orden!", small, Modifier.offset(8.dp, 59.dp).size(49.dp, 22.dp))
            BoxedText("En Peligro", small, Modifier.offset(91.dp, 59.dp).size(48.dp, 22.dp))
            BoxedText("Suspendida", small, Modifier.offset(171.dp, 59.dp).size(59.dp, 22.dp))
        }
    }
}

@Composable
private fun BoxScope.StatusRow(top: Dp, left: Dp, @DrawableRes icon: Int, title: String) {
    ProfileRowCard(Modifier.figmaPosition(left = left, right = 18.dp - left, top = top, height = 70.dp)) {
        ProfileRowIcon(icon)
        Text(
            text = title,
            style = jakartaText(14.5.sp, 18.13.sp, app_colors.cfgHeading, FontWeight.Bold),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 71.dp)
        )
        VerifiedBadge(
            Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-13).dp)
        )
    }
}

@Composable
private fun VerifiedBadge(modifier: Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_status_verified),
        contentDescription = "Verificado",
        modifier = modifier.size(16.dp)
    )
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun HelpSupportPreview() {
    HelpSupportScreen()
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun HelpCenterPreview() {
    HelpCenterScreen()
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun SecurityStatusPreview() {
    SecurityStatusScreen()
}
