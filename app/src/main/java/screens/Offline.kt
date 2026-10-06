package screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.u_spot.R
import decorations.FigmaCanvas
import decorations.HeaderLogo
import decorations.RotatedImage
import decorations.Sparkles
import decorations.SvgCanvas
import decorations.SvgPath
import decorations.app_colors
import decorations.app_fonts
import decorations.figmaPosition
import decorations.figmaText

// Grupo de destellos junto al mensaje (4 vectores sueltos en Figma, caja de 38 x 42).
private val messageSparkles = listOf(
    SvgPath("M13.3333 0C15.3333 6.66667 20 10 26.6667 13.3333C20 16.6667 15.3333 20 13.3333 26.6667C11.3333 20 6.66667 16.6667 0 13.3333C6.66667 10 11.3333 6.66667 13.3333 0V0", fill = app_colors.sparkleYellow, dx = 6.335f, dy = 0f),
    SvgPath("M6.66667 0C8 3.33333 10 5.33333 13.3333 6.66667C10 8 8 10 6.66667 13.3333C5.33333 10 3.33333 8 0 6.66667C3.33333 5.33333 5.33333 3.33333 6.66667 0V0", fill = app_colors.sparkleGreen, dx = 25.335f, dy = 28f),
    SvgPath("M4.66667 0C5.33333 2 6.66667 3.33333 9.33333 4C6.66667 4.66667 5.33333 6 4.66667 8C4 6 2.66667 4.66667 0 4C2.66667 3.33333 4 2 4.66667 0V0", fill = app_colors.sparkleBlue, dx = 5.065f, dy = 31.5f),
    SvgPath("M0.596285 0.298142C3.92962 6.96481 10.5963 10.2981 23.9296 3.63148", stroke = app_colors.sparkleDash, strokeWidth = 1.33333f, dash = 2.67f, dx = 5.738f, dy = 20.70f)
)

/** Figma: "Conexion Internet Fallida App" (631:1865). Se muestra encima de cualquier pantalla. */
@Composable
fun OfflineScreen(onRetry: () -> Unit = {}) {
    FigmaCanvas(designHeight = 917.dp, background = app_colors.black) {
        // Pantalla de fondo atenuada (opacidad 40 %)
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.4f }
                .clip(RoundedCornerShape(30.dp))
                .background(app_colors.white)
                .border(1.dp, app_colors.black, RoundedCornerShape(30.dp))
        ) {
            HeaderLogo(Modifier.figmaPosition(left = 28.dp, top = 73.dp, width = 353.dp, height = 123.dp))
            Sparkles(Modifier.figmaPosition(left = 359.dp, top = 11.dp, width = 40.dp, height = 40.dp))
        }

        // Tarjeta "Mensaje Verificacion Correo"
        val cardShape = RoundedCornerShape(40.dp)
        Box(
            Modifier
                .figmaPosition(left = 26.dp, top = 246.dp, width = 360.dp, height = 555.dp)
                .clip(cardShape)
                .background(app_colors.white)
                .border(2.dp, app_colors.offlineCardBorder, cardShape)
        ) {
            Box(
                modifier = Modifier.figmaPosition(left = 28.dp, top = 196.dp, width = 312.dp, height = 190.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "¡Oops te has quedado sin conexión, conectate nuevamente!",
                    style = figmaText(app_fonts.poppins, 18.sp, 20.sp, app_colors.black, textAlign = TextAlign.Center)
                )
            }
            RotatedImage(
                res = R.drawable.shape_blue,
                frameWidth = 164.dp, frameHeight = 176.dp, rotation = 39.19f,
                containerModifier = Modifier.figmaPosition(left = (-111).dp, top = 403.dp, width = 238.32.dp, height = 240.039.dp)
            )
            RotatedImage(
                res = R.drawable.shape_teal_alt,
                frameWidth = 231.268.dp, frameHeight = 186.169.dp, rotation = -30f,
                imageWidth = 215.778.dp, imageHeight = 178.453.dp, imageY = 0.503.dp,
                containerModifier = Modifier.figmaPosition(right = (-153.37).dp, top = (-116).dp, width = 293.369.dp, height = 276.861.dp)
            )
            SvgCanvas(
                paths = messageSparkles, viewportWidth = 38f, viewportHeight = 42f,
                modifier = Modifier
                    .figmaPosition(left = 22.dp, top = 251.dp, width = 38.dp, height = 42.dp)
                    .clipToBounds()
            )
            Image(
                painter = painterResource(R.drawable.ic_wifi_off),
                contentDescription = "Sin conexión",
                contentScale = ContentScale.Crop,
                modifier = Modifier.figmaPosition(left = 141.dp, top = 150.dp, width = 101.dp, height = 101.dp)
            )
            Box(
                Modifier
                    .figmaPosition(left = 150.dp, top = 360.dp, width = 75.dp, height = 60.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(app_colors.offlineRetry)
                    .clickable(onClick = onRetry)
            )
            Image(
                painter = painterResource(R.drawable.ic_refresh),
                contentDescription = "Reintentar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.figmaPosition(left = 173.dp, top = 375.dp, width = 30.dp, height = 29.dp)
            )
        }
    }
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun OfflinePreview() {
    OfflineScreen()
}
