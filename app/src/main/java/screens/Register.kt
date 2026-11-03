package screens

import android.util.Patterns
import androidx.compose.foundation.Image
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.u_spot.R
import data.AuthProvider
import data.RegisterResult
import decorations.BackButton
import decorations.ErrorMessage
import decorations.FigmaCanvas
import decorations.FormInput
import decorations.GoogleFormButton
import decorations.HeaderLogo
import decorations.PrimaryActionButton
import decorations.Sparkles
import decorations.StepProgress
import decorations.app_colors
import decorations.app_fonts
import decorations.belowStatusBar
import decorations.figmaPosition
import decorations.figmaText
import decorations.lockIcon
import decorations.mailIcon
import decorations.personIcon

/** Figma: "Registro App (Completa tu perfil)" y sus 5 variantes de error (631:894 y hermanas). */
@Composable
fun RegisterScreen(
    onBack: () -> Unit = {},
    onRegistered: () -> Unit = {},
    onGoogleLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var isLoading by rememberSaveable { mutableStateOf(false) }

    fun submit() {
        if (isLoading) return
        when {
            username.isBlank() && email.isBlank() && password.isEmpty() && confirmPassword.isEmpty() -> {
                error = context.getString(R.string.register_error_no_data)
            }
            username.isBlank() -> {
                error = context.getString(R.string.register_error_username_exists)
            }
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> {
                error = context.getString(R.string.register_error_invalid_email)
            }
            password.isEmpty() -> {
                error = context.getString(R.string.register_error_no_password)
            }
            password.length < 6 -> {
                error = context.getString(R.string.register_error_weak_password)
            }
            password != confirmPassword -> {
                error = context.getString(R.string.register_error_password_mismatch)
            }
            else -> {
                scope.launch {
                    isLoading = true
                    try {
                        when (AuthProvider.repository.register(username, email, password)) {
                            RegisterResult.Success -> {
                                error = null
                                onRegistered()
                            }
                            RegisterResult.AlreadyExists -> {
                                error = context.getString(R.string.register_error_username_exists)
                            }
                        }
                    } catch (_: Exception) {
                        error = context.getString(R.string.register_error_username_exists)
                    } finally {
                        isLoading = false
                    }
                }
            }
        }
    }

    FigmaCanvas(designHeight = 917.dp, background = app_colors.white) {
        // Foto inferior "Ellipse 1" (ya viene recortada en óvalo desde Figma)
        Image(
            painter = painterResource(R.drawable.photo_register),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.figmaPosition(left = (-43).dp, bottom = (-49.74).dp, width = 526.846.dp, height = 289.744.dp)
        )
        PhotoCaption(Modifier.figmaPosition(left = 18.58.dp, bottom = 4.65.dp, width = 120.87.dp, height = 84.35.dp))

        BackButton(onClick = onBack, modifier = Modifier.figmaPosition(left = 15.dp, top = belowStatusBar(12.dp)))
        Sparkles(Modifier.figmaPosition(right = 16.dp, top = belowStatusBar(32.dp), width = 40.dp, height = 40.dp))
        HeaderLogo(Modifier.figmaPosition(left = 58.dp, top = belowStatusBar(32.dp), width = 287.dp, height = 100.dp))

        Column(Modifier.figmaPosition(left = 37.5.dp, right = 10.5.dp, top = 145.48.dp)) {
            Text(
                text = stringResource(R.string.register_title),
                style = figmaText(app_fonts.inter, 24.sp, 32.sp, app_colors.formTitle, FontWeight.Bold)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.register_subtitle),
                style = figmaText(app_fonts.inter, 14.sp, 20.sp, app_colors.formSubtitle)
            )
        }

        FormInput(
            value = username, onValueChange = { username = it; error = null },
            placeholder = stringResource(R.string.register_username_placeholder), icon = personIcon,
            modifier = Modifier.figmaPosition(left = 28.dp, right = 24.dp, top = 234.dp)
        )
        FormInput(
            value = email, onValueChange = { email = it; error = null },
            placeholder = stringResource(R.string.register_email_placeholder), icon = mailIcon, keyboardType = KeyboardType.Email,
            modifier = Modifier.figmaPosition(left = 28.5.dp, right = 21.5.dp, top = 293.48.dp)
        )
        FormInput(
            value = password, onValueChange = { password = it; error = null },
            placeholder = stringResource(R.string.register_password_placeholder), icon = lockIcon, isPassword = true,
            modifier = Modifier.figmaPosition(left = 28.5.dp, right = 21.5.dp, top = 355.48.dp)
        )
        FormInput(
            value = confirmPassword, onValueChange = { confirmPassword = it; error = null },
            placeholder = stringResource(R.string.register_confirm_password_placeholder), icon = lockIcon, isPassword = true,
            modifier = Modifier.figmaPosition(left = 26.dp, right = 19.dp, top = 420.dp)
        )

        ErrorMessage(
            text = error,
            color = app_colors.formError,
            modifier = Modifier.figmaPosition(left = 8.dp, right = 0.dp, top = 470.dp)
        )

        StepProgress(Modifier.figmaPosition(left = 24.dp, right = 24.dp, top = 499.dp))
        PrimaryActionButton(
            text = if (isLoading) stringResource(R.string.register_submitting) else stringResource(R.string.register_submit),
            onClick = ::submit,
            modifier = Modifier.figmaPosition(left = 24.dp, right = 24.dp, top = 528.dp)
        )
        GoogleFormButton(
            onClick = onGoogleLogin,
            modifier = Modifier.figmaPosition(left = 24.dp, right = 24.dp, top = 616.dp)
        )
    }
}

/** "Tu entorno / también es / parte de la U" sobre la foto, girado -4.3° como en Figma. */
@Composable
private fun PhotoCaption(modifier: Modifier) {
    val density = LocalDensity.current
    val style = figmaText(app_fonts.inter, 18.sp, 22.5.sp, app_colors.white, FontWeight.Bold).copy(
        shadow = with(density) {
            Shadow(color = app_colors.shadowSoft07, offset = Offset(0f, 4.dp.toPx()), blurRadius = 1.5.dp.toPx())
        }
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .requiredSize(116.28.dp, 75.79.dp)
                .graphicsLayer { rotationZ = -4.3f }
        ) {
            Text(stringResource(R.string.register_caption_line_1), style = style, modifier = Modifier.offset(y = (-0.75).dp))
            Text(stringResource(R.string.register_caption_line_2), style = style, modifier = Modifier.offset(x = (-0.02).dp, y = 22.dp))
            Text(stringResource(R.string.register_caption_line_3), style = style, modifier = Modifier.offset(x = (-0.1).dp, y = 45.68.dp))
        }
    }
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun RegisterPreview() {
    RegisterScreen()
}
