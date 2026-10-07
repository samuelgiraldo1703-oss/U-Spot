package screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.u_spot.R
import data.AuthProvider
import decorations.BackButton
import decorations.FigmaCanvas
import decorations.HeaderLogo
import decorations.Sparkles
import decorations.app_colors
import decorations.app_fonts
import decorations.belowStatusBar
import decorations.figmaPosition
import decorations.figmaText
import kotlinx.coroutines.launch

private fun openGmailApp(context: Context) {
    try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.gm")
        if (launchIntent != null) {
            context.startActivity(launchIntent)
            return
        }
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://mail.google.com")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(webIntent)
    } catch (_: Exception) {
        val fallback = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_EMAIL)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(Intent.createChooser(fallback, "Abrir correo"))
        } catch (_: Exception) {
            Toast.makeText(context, "No se encontró aplicación de correo", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun openOutlookApp(context: Context) {
    try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.microsoft.office.outlook")
        if (launchIntent != null) {
            context.startActivity(launchIntent)
            return
        }
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://outlook.live.com")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(webIntent)
    } catch (_: Exception) {
        val fallback = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_EMAIL)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(Intent.createChooser(fallback, "Abrir correo"))
        } catch (_: Exception) {
            Toast.makeText(context, "No se encontró aplicación de correo", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Pantalla de verificación tras enviar el correo de confirmación de cambio de contraseña.
 * Figma: Nodo 211-1149 (Modal de confirmación "¿Te enviamos una verificación a tu correo?").
 */
@Composable
fun EmailVerificationScreen(
    email: String = "",
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isResending by rememberSaveable { mutableStateOf(false) }

    fun resend() {
        if (isResending || email.isBlank()) return
        scope.launch {
            isResending = true
            val ok = AuthProvider.repository.sendPasswordReset(email)
            if (ok) {
                Toast.makeText(context, "Correo reenviado a $email", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Error reenviando el correo", Toast.LENGTH_SHORT).show()
            }
            isResending = false
        }
    }

    // Fondo estándar de U-Spot a 412 x 926
    FigmaCanvas(designHeight = 926.dp, background = app_colors.white) {
        // Ola turquesa superior derecha
        Image(
            painter = painterResource(R.drawable.shape_teal_wave),
            contentDescription = null,
            modifier = Modifier.figmaPosition(left = 261.5.dp, top = (-9.5).dp, width = 425.dp, height = 229.dp)
        )
        // Foto inferior de la Gruta
        Image(
            painter = painterResource(R.drawable.photo_password),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.figmaPosition(left = (-45).dp, bottom = (-38).dp, width = 523.dp, height = 298.dp)
        )

        Sparkles(Modifier.figmaPosition(left = 342.dp, top = belowStatusBar(21.dp), width = 40.dp, height = 40.dp))
        HeaderLogo(Modifier.figmaPosition(left = 56.dp, top = 97.dp, width = 287.dp, height = 100.dp))

        // Overlay oscuro semitransparente sobre el fondo para dar efecto de modal
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x55000000))
        )

        // Tarjeta central de verificación (Figma nodo 211-1149)
        val cardShape = RoundedCornerShape(36.dp)
        val cardBorderColor = Color(0xFF084B42)

        Box(
            modifier = Modifier
                .figmaPosition(left = 24.dp, right = 24.dp, top = 245.dp, height = 375.dp)
                .clip(cardShape)
                .background(Color.White, cardShape)
                .border(BorderStroke(2.dp, cardBorderColor), cardShape)
        ) {
            // Forma orgánica amarilla en la esquina superior derecha
            Image(
                painter = painterResource(R.drawable.shape_yellow),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 30.dp, y = (-25).dp)
                    .size(150.dp, 160.dp)
            )

            // Forma orgánica azul en la esquina inferior izquierda
            Image(
                painter = painterResource(R.drawable.shape_blue),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-30).dp, y = 30.dp)
                    .size(145.dp, 155.dp)
            )

            // Botón volver en la esquina superior izquierda de la tarjeta
            BackButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 18.dp, top = 18.dp)
            )

            // Contenido central: Sparkle + Texto de confirmación
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Sparkles(modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "¡Te enviamos una verificación",
                            style = figmaText(
                                family = app_fonts.poppins,
                                size = 16.sp,
                                lineHeight = 22.sp,
                                color = Color(0xFF111827),
                                weight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "a tu correo!",
                            style = figmaText(
                                family = app_fonts.poppins,
                                size = 16.sp,
                                lineHeight = 22.sp,
                                color = Color(0xFF111827),
                                weight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Spacer(Modifier.height(34.dp))

                // Fila con los proveedores de correo: Gmail | Outlook
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Botón Gmail
                    Box(
                        modifier = Modifier
                            .size(64.dp, 52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { openGmailApp(context) },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_gmail),
                            contentDescription = "Abrir Gmail",
                            modifier = Modifier.size(46.dp, 38.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    // Línea divisoria vertical
                    Box(
                        modifier = Modifier
                            .width(1.5.dp)
                            .height(52.dp)
                            .background(Color(0xFFE5E7EB))
                    )

                    Spacer(Modifier.width(12.dp))

                    // Botón Outlook
                    Box(
                        modifier = Modifier
                            .size(64.dp, 52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { openOutlookApp(context) },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_outlook),
                            contentDescription = "Abrir Outlook",
                            modifier = Modifier.size(46.dp, 38.dp)
                        )
                    }
                }

                if (email.isNotBlank()) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = if (isResending) "Reenviando..." else "¿No te llegó? Reenviar correo",
                        style = figmaText(
                            family = app_fonts.inter,
                            size = 12.sp,
                            lineHeight = 16.sp,
                            color = app_colors.formBlue,
                            weight = FontWeight.Medium
                        ),
                        modifier = Modifier
                            .clickable(enabled = !isResending) { resend() }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

@Preview(widthDp = 412, heightDp = 926)
@Composable
private fun EmailVerificationPreview() {
    EmailVerificationScreen(email = "ejemplo@gmail.com")
}
