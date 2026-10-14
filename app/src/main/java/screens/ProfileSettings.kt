package screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.u_spot.R
import data.UserProfile
import data.timeAgo
import decorations.BackButton
import decorations.BoxedText
import decorations.ProfileCanvas
import decorations.ProfileCardImage
import decorations.ProfileDialogCard
import decorations.ProfileCorner
import decorations.ProfileOverlay
import decorations.app_colors
import decorations.app_fonts
import decorations.figmaPosition
import decorations.figmaText
import decorations.jakartaText

private enum class SettingsDialog { None, Language, DeleteAccount, LoggedOut }

private val settingsLanguages = listOf(
    R.drawable.ic_lang_flag_1 to "Español (Mexico)",
    R.drawable.ic_lang_flag_2 to "Inglés (EE.UU)",
    R.drawable.ic_lang_flag_3 to "Alemán",
    R.drawable.ic_lang_flag_4 to "Ruso",
    R.drawable.ic_lang_flag_5 to "Francés"
)

/**
 * Figma: "Configuracion Cuenta" (255:14 contraseña oculta, 210:495 contraseña visible,
 * 227:1689 perfil privado, 264:183 cambio de idioma) y las ventanas "Cierre Sesion" (237:1847)
 * y "Cierre de Sesion - Confirmacion" (237:1887).
 */
@Composable
fun AccountSettingsScreen(
    user: UserProfile = UserProfile.Preview,
    onBack: () -> Unit = {},
    onAccountDeleted: () -> Unit = {},
    onLogin: () -> Unit = {}
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var privateProfile by rememberSaveable { mutableStateOf(false) }
    var language by rememberSaveable { mutableStateOf("Español (Colombia)") }
    var dialog by rememberSaveable { mutableStateOf(SettingsDialog.None) }

    // Con la contraseña visible la fila crece 19 dp y la sección siguiente baja 14 (frame 210:495).
    val passwordRowHeight = if (passwordVisible) 104.dp else 85.dp
    val shift = if (passwordVisible) 14.dp else 0.dp

    Box(Modifier.fillMaxSize()) {
        ProfileCanvas(designHeight = 916.dp) {
            ProfileCorner(app_colors.profileYellow)
            BackButton(
                onClick = onBack,
                modifier = Modifier.figmaPosition(right = 26.dp, top = 28.dp),
                res = R.drawable.ic_cfg_back
            )
            Text(
                text = "Configuraciones de cuenta",
                style = jakartaText(25.sp, 37.5.sp, app_colors.cfgTitle, FontWeight.ExtraBold, letterSpacing = (-0.62).sp),
                modifier = Modifier.figmaPosition(left = 31.dp, top = 28.dp, width = 206.dp)
            )

            SectionHeading("Información Personal", left = 11.dp, top = 132.dp)
            SettingsCard(top = 168.dp, height = 80.dp) {
                RowIcon(R.drawable.ic_cfg_mail)
                RowTexts("Correo electrónico", user.email, top = 13.dp, subtitleWidth = 280.dp)
            }
            SettingsCard(top = 258.dp, height = passwordRowHeight) {
                RowIcon(R.drawable.ic_cfg_lock)
                // Firebase guarda la contraseña cifrada (hash): no se puede leer, solo se muestra oculta.
                val updated = if (user.hasPassword) "Actualizada ${timeAgo(user.passwordUpdatedAt)}" else "Inicias sesión con Google"
                RowTexts(
                    title = "Contraseña",
                    subtitle = if (passwordVisible) "••••••••\n$updated" else updated,
                    top = 13.dp,
                    subtitleWidth = 82.3.dp
                )
                // "image 5" / "image 6": ojo tachado u ojo abierto
                Image(
                    painter = painterResource(if (passwordVisible) R.drawable.cfg_eye_on else R.drawable.cfg_eye_off),
                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-11).dp, y = if (passwordVisible) 42.dp else 29.dp)
                        .size(if (passwordVisible) 25.dp else 28.dp, if (passwordVisible) 20.dp else 23.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            passwordVisible = !passwordVisible
                        }
                )
            }

            SectionHeading("Preferencias de la App", left = 11.dp, top = 361.dp + shift)
            SettingsCard(top = 397.dp + shift, height = 85.dp, onClick = { dialog = SettingsDialog.Language }) {
                RowIcon(R.drawable.ic_cfg_globe)
                RowTexts("Idioma de la App", language, top = 13.dp, subtitleWidth = 110.dp)
                Image(
                    painter = painterResource(R.drawable.ic_cfg_chevron_down),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = (-13).dp, y = (-3).dp)
                        .size(20.dp)
                )
            }
            SettingsCard(top = 492.dp + shift, height = 70.dp) {
                RowIcon(R.drawable.ic_cfg_theme)
                Text(
                    text = "Tema Visual",
                    style = jakartaText(14.5.sp, 21.75.sp, app_colors.cfgHeading, FontWeight.Bold),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = 71.dp, y = (-0.87).dp)
                )
                ThemeSegmentedControl(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = (-13).dp)
                )
            }
            SettingsCard(top = 572.dp + shift, height = 70.dp) {
                RowIcon(R.drawable.ic_cfg_shield)
                RowTexts("Privacidad", " Pública/Privada", top = 15.dp, subtitleWidth = 100.dp)
                PrivacyToggle(
                    private = privateProfile,
                    onToggle = { privateProfile = !privateProfile },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = (-13).dp)
                )
            }

            SectionHeading("Cuenta", left = 9.dp, top = 670.dp)
            SettingsCard(top = 709.dp, height = 70.dp, left = 7.dp, right = 11.dp, onClick = { dialog = SettingsDialog.DeleteAccount }) {
                RowIcon(R.drawable.ic_cfg_danger)
                RowTexts("Eliminar Cuenta", "Acción peligrosa", top = 15.dp, subtitleWidth = 109.66.dp, titleColor = app_colors.cfgDanger)
            }

            LogoutButton(
                onClick = { dialog = SettingsDialog.LoggedOut },
                modifier = Modifier.figmaPosition(left = 9.dp, right = 9.dp, top = 837.5.dp, height = 52.dp)
            )
        }

        when (dialog) {
            SettingsDialog.Language -> LanguageDialog(
                selected = language,
                onSelect = { language = it },
                onClose = { dialog = SettingsDialog.None }
            )
            SettingsDialog.DeleteAccount -> DeleteAccountDialog(
                onYes = onAccountDeleted,
                onNo = { dialog = SettingsDialog.None }
            )
            SettingsDialog.LoggedOut -> LoggedOutDialog(onLogin = onLogin)
            SettingsDialog.None -> Unit
        }
    }
}

@Composable
private fun BoxScope.SectionHeading(text: String, left: Dp, top: Dp) {
    Text(
        text = text,
        style = jakartaText(17.sp, 25.5.sp, app_colors.cfgHeading, FontWeight.Bold),
        modifier = Modifier.figmaPosition(left = left, top = top)
    )
}

/** Tarjeta blanca de una fila de ajustes (borde #E9EEF5, radio 20, sombra suave). */
@Composable
private fun BoxScope.SettingsCard(
    top: Dp,
    height: Dp,
    left: Dp = 9.dp,
    right: Dp = 9.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .figmaPosition(left = left, right = right, top = top, height = height)
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
private fun BoxScope.RowIcon(@DrawableRes res: Int) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = Modifier
            .align(Alignment.CenterStart)
            .offset(x = 13.dp)
            .size(44.dp)
    )
}

@Composable
private fun BoxScope.RowTexts(
    title: String,
    subtitle: String,
    top: Dp,
    subtitleWidth: Dp,
    titleColor: Color = app_colors.cfgHeading
) {
    Column(
        Modifier
            .align(Alignment.TopStart)
            .offset(x = 71.dp, y = top)
    ) {
        Text(
            text = title,
            style = jakartaText(14.5.sp, 18.13.sp, titleColor, FontWeight.Bold),
            maxLines = 1,
            modifier = Modifier.height(19.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = jakartaText(12.5.sp, 18.75.sp, app_colors.cfgSubtitle, FontWeight.Medium),
            modifier = Modifier.width(subtitleWidth)
        )
    }
}

/** "Segmented Control (Claro / Oscuro)". "Oscuro" queda sin acción: su diseño no es parte del módulo. */
@Composable
private fun ThemeSegmentedControl(modifier: Modifier) {
    Box(
        modifier = modifier
            .size(140.52.dp, 34.dp)
            .background(app_colors.cfgSegmentBg, CircleShape)
    ) {
        BoxedText(
            text = "Claro",
            style = jakartaText(12.sp, 16.sp, app_colors.cfgHeading, FontWeight.Bold),
            modifier = Modifier
                .offset(3.dp, 3.dp)
                .size(63.59.dp, 28.dp)
                .dropShadow(CircleShape, Shadow(radius = 6.dp, color = Color(0x1F0F254B), offset = DpOffset(0.dp, 2.dp)))
                .background(app_colors.white, CircleShape)
        )
        BoxedText(
            text = "Oscuro",
            style = jakartaText(12.sp, 16.sp, app_colors.cfgSubtitle, FontWeight.Medium),
            modifier = Modifier
                .offset(66.6.dp, 3.dp)
                .size(70.92.dp, 28.dp)
        )
    }
}

/** "Toggle Switch Pill": etiqueta Pública / Privada con su interruptor. */
@Composable
private fun PrivacyToggle(private: Boolean, onToggle: () -> Unit, modifier: Modifier) {
    Row(
        modifier = modifier
            .height(32.dp)
            .clip(CircleShape)
            .background(app_colors.cfgPrivacyPill)
            .clickable(onClick = onToggle)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (private) "Privada" else "Pública",
            style = jakartaText(12.sp, 18.sp, app_colors.cfgHeading, FontWeight.Bold),
            modifier = Modifier.width(if (private) 44.dp else 43.23.dp)
        )
        Spacer(Modifier.width(8.dp))
        Image(
            painter = painterResource(if (private) R.drawable.ic_cfg_switch_off else R.drawable.ic_cfg_switch_on),
            contentDescription = if (private) "Perfil privado" else "Perfil público",
            modifier = Modifier.size(44.dp, 24.dp)
        )
    }
}

@Composable
private fun LogoutButton(onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier = modifier
            .dropShadow(CircleShape, Shadow(radius = 18.dp, color = Color(0x590085FF), offset = DpOffset(0.dp, 6.dp)))
            .clip(CircleShape)
            .background(app_colors.cfgLogoutBlue)
            .clickable(onClick = onClick),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_cfg_logout),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Cerrar sesión",
            style = jakartaText(16.sp, 24.sp, app_colors.white, FontWeight.Bold)
        )
    }
}

/** Figma: "Configuracion Cuenta - Cambio Idioma" (264:183). */
@Composable
private fun LanguageDialog(selected: String, onSelect: (String) -> Unit, onClose: () -> Unit) {
    ProfileOverlay(scrim = Color(0x80000000)) {
        ProfileDialogCard(top = 159.dp, background = app_colors.dialogCard) {
            ProfileCardImage(R.drawable.shape_lang_blue, (-80.85).dp, (-81.86).dp, 185.05.dp, 187.71.dp)
            ProfileCardImage(R.drawable.shape_lang_teal, 222.87.dp, 451.38.dp, 238.47.dp, 200.49.dp)
            BackButton(
                onClick = onClose,
                modifier = Modifier.offset(16.dp, 19.dp),
                res = R.drawable.ic_lang_back
            )
            BoxedText(
                text = "Selección de Idioma",
                style = figmaText(app_fonts.poppins, 24.sp, 30.sp, app_colors.black, FontWeight.SemiBold),
                modifier = Modifier
                    .offset(77.dp, 42.dp)
                    .size(277.dp, 70.dp)
            )
            settingsLanguages.forEachIndexed { index, (flag, name) ->
                LanguageRow(flag, name, onClick = { onSelect(name) }, modifier = Modifier.offset(7.dp, 139.dp + 60.dp * index))
            }
            BoxedText(
                text = "Idioma seleccionado:",
                style = figmaText(app_fonts.poppins, 16.sp, 30.sp, app_colors.black, FontWeight.SemiBold),
                modifier = Modifier
                    .offset(13.dp, 457.dp)
                    .size(237.dp, 60.dp)
            )
            BoxedText(
                text = selected,
                style = jakartaText(14.sp, 25.sp, app_colors.cfgSubtitle, FontWeight.Medium),
                modifier = Modifier
                    .offset(34.dp, 500.dp)
                    .size(187.dp, 43.dp)
            )
        }
    }
}

@Composable
private fun LanguageRow(@DrawableRes flag: Int, name: String, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .size(345.dp, 57.dp)
            .dropShadow(shape, Shadow(radius = 18.dp, color = Color(0x0A0F254B), offset = DpOffset(0.dp, 4.dp)))
            .clip(shape)
            .background(app_colors.white)
            .border(1.dp, app_colors.cfgCardBorder, shape)
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(flag),
            contentDescription = null,
            modifier = Modifier
                .offset(9.dp, 8.dp)
                .size(38.dp, 42.dp)
        )
        Text(
            text = name,
            style = jakartaText(20.sp, 25.sp, app_colors.cfgSubtitle, FontWeight.Medium),
            modifier = Modifier.offset(58.dp, 16.dp)
        )
    }
}

/** Figma: "Cierre Sesion" (237:1847): confirmación de eliminar la cuenta. */
@Composable
private fun DeleteAccountDialog(onYes: () -> Unit, onNo: () -> Unit) {
    val text = figmaText(app_fonts.poppins, 18.sp, 20.sp, app_colors.black)
    ProfileOverlay(scrim = Color(0x99000000)) {
        ProfileDialogCard(top = 258.dp, background = app_colors.dialogCard) {
            ProfileCardImage(R.drawable.shape_delete_blue, (-96.85).dp, 417.14.dp, 210.03.dp, 211.75.dp)
            ProfileCardImage(R.drawable.shape_dialog_yellow, 236.35.dp, (-93.78).dp, 194.73.dp, 223.33.dp)
            Image(
                painter = painterResource(R.drawable.dialog_delete_account),
                contentDescription = null,
                modifier = Modifier
                    .offset(123.dp, 138.dp)
                    .size(115.dp)
            )
            BoxedText(
                text = "¿Estás seguro que deseas eliminar tu cuenta de \nU-Spot?",
                style = text,
                modifier = Modifier
                    .offset(55.dp, 255.dp)
                    .size(265.dp, 85.dp)
            )
            Image(
                painter = painterResource(R.drawable.ic_dialog_sparkles),
                contentDescription = null,
                modifier = Modifier
                    .offset(30.dp, 270.dp)
                    .size(38.dp, 32.dp)
            )
            DialogChoice("SI", app_colors.dialogNeutralButton, onYes, Modifier.offset(98.dp, 336.dp))
            DialogChoice("NO", app_colors.profileYellow, onNo, Modifier.offset(188.dp, 336.dp))
        }
    }
}

@Composable
private fun DialogChoice(label: String, color: Color, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(20.dp)
    BoxedText(
        text = label,
        style = figmaText(app_fonts.poppins, 18.sp, 20.sp, app_colors.black),
        modifier = modifier
            .size(75.dp, 60.dp)
            .clip(shape)
            .background(color)
            .border(2.dp, app_colors.black, shape)
            .clickable(onClick = onClick)
    )
}

/** Figma: "Cierre de Sesion - Confirmacion" (237:1887). */
@Composable
private fun LoggedOutDialog(onLogin: () -> Unit) {
    ProfileOverlay(scrim = Color(0x99000000)) {
        ProfileDialogCard(top = 258.dp, background = app_colors.white) {
            ProfileCardImage(R.drawable.shape_logout_teal, (-93.01).dp, 430.14.dp, 198.14.dp, 198.75.dp)
            ProfileCardImage(R.drawable.shape_dialog_yellow, 236.35.dp, (-93.78).dp, 194.73.dp, 223.33.dp)
            // "App Fondo Blanco 4": mismo recorte del logo que en la pantalla principal del perfil
            Box(
                Modifier
                    .offset(78.dp, 138.dp)
                    .size(204.dp, 58.dp)
                    .clip(RoundedCornerShape(43.dp))
            ) {
                ProfileCardImage(R.drawable.logo_login, (-0.12).dp, (-71).dp, 204.22.dp, 203.dp)
            }
            // "image 4": la imagen se recorta dentro de un marco de 70 x 45
            Box(
                Modifier
                    .offset(150.dp, 233.dp)
                    .size(70.dp, 45.dp)
                    .clip(RoundedCornerShape(0.dp))
            ) {
                ProfileCardImage(R.drawable.dialog_logout_hand, 9.37.dp, 0.22.dp, 50.87.dp, 54.78.dp)
            }
            BoxedText(
                text = "Has cerrado tu sesión ",
                style = figmaText(app_fonts.poppins, 18.sp, 20.sp, app_colors.black),
                modifier = Modifier
                    .offset(100.dp, 278.dp)
                    .size(212.dp, 57.dp)
            )
            Image(
                painter = painterResource(R.drawable.ic_dialog_sparkles_tilted),
                contentDescription = null,
                modifier = Modifier
                    .offset(62.99.dp, 286.43.dp)
                    .size(33.61.dp, 41.6.dp)
            )
            BoxedText(
                text = buildAnnotatedString {
                    append("¿Deseas volver a descubrir lugares?\n ")
                    withStyle(SpanStyle(color = app_colors.loginBlue, fontWeight = FontWeight.SemiBold)) { append("Inicia Sesión") }
                },
                style = figmaText(app_fonts.poppins, 14.sp, 20.sp, app_colors.black),
                modifier = Modifier
                    .offset(53.dp, 342.dp)
                    .size(278.dp, 64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(app_colors.profileYellow)
                    .clickable(onClick = onLogin)
            )
        }
    }
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun AccountSettingsPreview() {
    AccountSettingsScreen()
}
