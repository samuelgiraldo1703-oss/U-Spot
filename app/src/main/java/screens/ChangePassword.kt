package screens

import androidx.compose.foundation.Image
import kotlinx.coroutines.launch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.u_spot.R
import data.AuthProvider
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

/** Figma: "Actualizacion Contraseña App" (631:654, 631:734, 631:814). */
@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit = {},
    onPasswordChanged: (String) -> Unit = {},
    onGoogleLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    var email by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var isLoading by rememberSaveable { mutableStateOf(false) }

    fun submit() {
        if (isLoading) return
        val repository = AuthProvider.repository
        when {
            email.isBlank() -> {
                error = context.getString(R.string.change_password_error_email_not_linked)
            }
            newPassword.isEmpty() -> {
                error = context.getString(R.string.change_password_error_no_password)
            }
            newPassword.length < 6 -> {
                error = context.getString(R.string.change_password_error_weak_password)
            }
            newPassword != confirmPassword -> {
                error = context.getString(R.string.change_password_error_password_mismatch)
            }
            else -> {
                scope.launch {
                    isLoading = true
                    try {
                        when (val result = repository.changePassword(email, newPassword)) {
                            is data.ChangePasswordResult.Success -> {
                                error = null
                                val target = result.email.ifBlank { email.trim().lowercase() }
                                onPasswordChanged(target)
                            }
                            is data.ChangePasswordResult.AccountNotFound -> {
                                error = context.getString(R.string.change_password_error_email_not_linked)
                            }
                            is data.ChangePasswordResult.WeakPassword -> {
                                error = context.getString(R.string.change_password_error_weak_password)
                            }
                            is data.ChangePasswordResult.Error -> {
                                error = result.message
                            }
                        }
                    } catch (e: Exception) {
                        error = e.localizedMessage ?: context.getString(R.string.change_password_error_generic)
                    } finally {
                        isLoading = false
                    }
                }
            }
        }
    }

    // El frame de Figma mide 412 x 926; las coordenadas ya están restadas del grupo "Frame 1" (-45, -9).
    FigmaCanvas(designHeight = 926.dp, background = app_colors.white) {
        // "Vector 2": ola turquesa superior derecha
        Image(
            painter = painterResource(R.drawable.shape_teal_wave),
            contentDescription = null,
            modifier = Modifier.figmaPosition(left = 261.5.dp, top = (-9.5).dp, width = 425.dp, height = 229.dp)
        )
        // Foto inferior "Ellipse 3" (ya viene recortada en óvalo desde Figma)
        Image(
            painter = painterResource(R.drawable.photo_password),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.figmaPosition(left = (-45).dp, bottom = (-38).dp, width = 523.dp, height = 298.dp)
        )

        BackButton(onClick = onBack, modifier = Modifier.figmaPosition(left = 25.dp, top = belowStatusBar(21.dp)))
        Sparkles(Modifier.figmaPosition(left = 342.dp, top = belowStatusBar(21.dp), width = 40.dp, height = 40.dp))
        HeaderLogo(Modifier.figmaPosition(left = 56.dp, top = 97.dp, width = 287.dp, height = 100.dp))

        Text(
            text = stringResource(R.string.change_password_title),
            style = figmaText(app_fonts.inter, 24.sp, 32.sp, app_colors.formTitle, FontWeight.Bold),
            modifier = Modifier.figmaPosition(left = 41.dp, right = 0.dp, top = 204.dp)
        )

        FormInput(
            value = email, onValueChange = { email = it; error = null },
            placeholder = stringResource(R.string.change_password_email_placeholder), icon = mailIcon, keyboardType = KeyboardType.Email,
            modifier = Modifier.figmaPosition(left = 40.dp, right = 45.dp, top = 263.dp)
        )
        FormInput(
            value = newPassword, onValueChange = { newPassword = it; error = null },
            placeholder = stringResource(R.string.change_password_new_placeholder), icon = lockIcon, isPassword = true,
            modifier = Modifier.figmaPosition(left = 42.dp, right = 45.dp, top = 330.dp)
        )
        FormInput(
            value = confirmPassword, onValueChange = { confirmPassword = it; error = null },
            placeholder = stringResource(R.string.change_password_confirm_placeholder), icon = lockIcon, isPassword = true,
            modifier = Modifier.figmaPosition(left = 40.dp, right = 44.dp, top = 399.dp)
        )

        ErrorMessage(
            text = error,
            color = app_colors.formError,
            modifier = Modifier.figmaPosition(left = 2.dp, right = 0.dp, top = 451.dp)
        )

        StepProgress(Modifier.figmaPosition(left = 20.dp, right = 21.dp, top = 480.dp))
        PrimaryActionButton(
            text = if (isLoading) stringResource(R.string.change_password_submitting) else stringResource(R.string.change_password_submit),
            onClick = ::submit,
            modifier = Modifier.figmaPosition(left = 20.dp, right = 21.dp, top = 509.dp)
        )
        GoogleFormButton(
            onClick = onGoogleLogin,
            modifier = Modifier.figmaPosition(left = 19.dp, right = 22.dp, top = 597.dp)
        )
    }
}

@Preview(widthDp = 412, heightDp = 926)
@Composable
private fun ChangePasswordPreview() {
    ChangePasswordScreen()
}
