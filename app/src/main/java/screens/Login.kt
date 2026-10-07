package screens

import androidx.compose.foundation.Image
import kotlinx.coroutines.launch
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.u_spot.R
import data.AuthProvider
import data.LoginResult
import decorations.ErrorMessage
import decorations.FigmaCanvas
import decorations.RotatedImage
import decorations.app_colors
import decorations.app_fonts
import decorations.figmaPosition
import decorations.figmaText

/** Mensajes de las variantes "Bienvenida App (Inicio Sesion Fallido - …)". */
private object LoginErrors {
    const val NO_DATA = "Ningun dato registrado en el inicio de sesión"
    const val ACCOUNT_NOT_FOUND = "Correo electrónico no coincide con ninguna cuenta"
    const val WRONG_PASSWORD = "Contraseña Incorrecta, digitela nuevamente"
}

/** Figma: "Bienvenida App" (24:9) y sus 3 variantes de error. */
@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit = {},
    onRegister: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
    onGoogleLogin: () -> Unit = {}
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var isLoading by rememberSaveable { mutableStateOf(false) }

    fun submit() {
        if (isLoading) return
        if (email.isBlank() && password.isEmpty()) {
            error = LoginErrors.NO_DATA
            return
        }
        scope.launch {
            isLoading = true
            try {
                when (AuthProvider.repository.login(email, password)) {
                    LoginResult.AccountNotFound -> error = LoginErrors.ACCOUNT_NOT_FOUND
                    LoginResult.WrongPassword -> error = LoginErrors.WRONG_PASSWORD
                    LoginResult.Success -> {
                        error = null
                        onLoginSuccess(email.trim())
                    }
                }
            } catch (_: Exception) {
                error = LoginErrors.ACCOUNT_NOT_FOUND
            } finally {
                isLoading = false
            }
        }
    }

    // El orden de las capas es el de Figma: el logo queda debajo de las formas de colores.
    FigmaCanvas(designHeight = 917.dp, background = app_colors.white) {
        // Logo "App Fondo Blanco 1" (recortado igual que en Figma)
        RotatedImage(
            res = R.drawable.logo_login,
            frameWidth = 314.108.dp, frameHeight = 103.217.dp, rotation = -0.3f,
            imageWidth = 311.22.dp, imageHeight = 309.35.dp, imageX = (-30.22).dp, imageY = (-87.57).dp,
            clipFrame = true,
            containerModifier = Modifier.figmaPosition(left = 44.dp, top = 107.dp, width = 314.645.dp, height = 104.86.dp)
        )

        // Textos de bienvenida
        Column(Modifier.figmaPosition(left = 30.dp, top = 256.dp)) {
            Text(
                text = "¡Bienvenido!",
                style = figmaText(app_fonts.poppins, 30.sp, 36.sp, app_colors.loginTitle, FontWeight.Bold)
            )
            Spacer(Modifier.height(7.375.dp))
            Text(
                text = "Inicia sesión para descubrir nuevos\nlugares y experiencias.",
                style = figmaText(app_fonts.poppins, 14.sp, 22.75.sp, app_colors.black)
            )
        }

        // Formulario
        Column(Modifier.figmaPosition(left = 32.dp, right = 32.dp, top = 376.dp)) {
            LoginInput(
                value = email,
                onValueChange = { email = it; error = null },
                placeholder = "Correo electrónico",
                keyboardType = KeyboardType.Email,
                icon = R.drawable.ic_mail, iconWidth = 16.667.dp, iconHeight = 13.333.dp,
                iconX = 17.dp, iconY = 15.69.dp
            )
            Spacer(Modifier.height(16.dp))
            LoginInput(
                value = password,
                onValueChange = { password = it; error = null },
                placeholder = "Contraseña",
                isPassword = true,
                icon = R.drawable.ic_lock_login, iconWidth = 13.333.dp, iconHeight = 17.5.dp,
                iconX = 18.dp, iconY = 12.69.dp
            )
            Spacer(Modifier.height(16.dp + 8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(app_colors.loginBlue)
                    .clickable(onClick = ::submit),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLoading) "Iniciando sesión..." else "Iniciar sesión",
                    style = figmaText(app_fonts.poppins, 16.sp, 24.sp, app_colors.white, FontWeight.Medium)
                )
                Spacer(Modifier.width(8.01.dp))
                Image(painterResource(R.drawable.ic_arrow_login), null, Modifier.size(12.dp))
            }
        }

        ErrorMessage(
            text = error,
            color = app_colors.loginError,
            modifier = Modifier.figmaPosition(left = 0.dp, right = 8.dp, top = 486.dp)
        )

        Text(
            text = "Olvide Mi Contraseña",
            style = figmaText(app_fonts.poppins, 14.sp, 20.sp, app_colors.black, textAlign = TextAlign.Center),
            modifier = Modifier
                .figmaPosition(left = 0.dp, right = 10.dp, top = 561.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onForgotPassword
                )
        )

        // Divisor
        Row(
            modifier = Modifier.figmaPosition(left = 21.dp, right = 43.dp, top = 600.dp, height = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f).height(1.dp).background(app_colors.loginDivider))
            Spacer(Modifier.width(16.dp))
            Box(Modifier.size(8.dp).border(1.dp, app_colors.loginDivider, CircleShape))
            Spacer(Modifier.width(16.dp))
            Box(Modifier.weight(1f).height(1.dp).background(app_colors.loginDivider))
        }

        // Continuar con Google
        Row(
            modifier = Modifier
                .figmaPosition(left = 34.dp, right = 30.dp, top = 639.dp, height = 46.dp)
                .clip(CircleShape)
                .background(app_colors.white)
                .border(1.dp, app_colors.loginDivider, CircleShape)
                .clickable(onClick = onGoogleLogin),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(R.drawable.ic_google), null, Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Continuar con Google",
                style = figmaText(app_fonts.poppins, 14.sp, 20.sp, app_colors.loginTitle, FontWeight.Medium)
            )
        }

        // ¿No tienes una cuenta? Regístrate
        Text(
            text = buildAnnotatedString {
                append("¿No tienes una cuenta? ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = app_colors.loginBlue)) {
                    append("Regístrate")
                }
            },
            style = figmaText(app_fonts.poppins, 14.sp, 20.sp, app_colors.black, textAlign = TextAlign.Center),
            modifier = Modifier
                .figmaPosition(left = 0.dp, right = 0.dp, top = 710.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onRegister
                )
        )

        // Forma amarilla inferior izquierda
        RotatedImage(
            res = R.drawable.shape_yellow,
            frameWidth = 164.dp, frameHeight = 176.dp, rotation = 39.19f,
            containerModifier = Modifier.figmaPosition(left = (-75.16).dp, bottom = (-88.02).dp, width = 238.32.dp, height = 240.039.dp)
        )
        // Forma turquesa superior derecha
        RotatedImage(
            res = R.drawable.shape_teal,
            frameWidth = 231.268.dp, frameHeight = 186.169.dp, rotation = -30f,
            imageWidth = 215.778.dp, imageHeight = 178.453.dp, imageY = 0.503.dp,
            containerModifier = Modifier.figmaPosition(right = (-123.37).dp, top = (-74).dp, width = 293.369.dp, height = 276.861.dp)
        )
    }
}

/** Campo tipo píldora del login (fondo #EFEDF1, 45 dp de alto). */
@Composable
private fun LoginInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: Int,
    iconWidth: Dp,
    iconHeight: Dp,
    iconX: Dp,
    iconY: Dp,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val textStyle = figmaText(app_fonts.poppins, 14.sp, 21.sp, app_colors.loginTitle)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp)
            .clip(CircleShape)
            .background(app_colors.loginInput)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = textStyle,
            cursorBrush = SolidColor(app_colors.loginBlue),
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 48.dp, end = if (isPassword) 48.dp else 16.dp, top = 12.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(placeholder, style = textStyle.copy(color = app_colors.loginPlaceholder), maxLines = 1)
                    }
                    innerTextField()
                }
            }
        )
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier
                .offset(iconX, iconY)
                .size(iconWidth, iconHeight)
        )
        if (isPassword) {
            Image(
                painter = painterResource(R.drawable.ic_eye_off_login),
                contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-16).dp, y = 10.75.dp)
                    .size(18.333.dp, 16.5.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { passwordVisible = !passwordVisible }
            )
        }
    }
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun LoginPreview() {
    LoginScreen()
}
