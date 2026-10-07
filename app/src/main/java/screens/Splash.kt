package screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.u_spot.R
import decorations.FigmaCanvas
import decorations.app_colors
import decorations.app_fonts
import decorations.figmaPosition
import decorations.figmaText
import kotlinx.coroutines.delay

/** Tiempo que se muestra el splash antes de pasar al login (2s base + 3s adicionales). */
const val SPLASH_DURATION_MS = 5000L

/** Figma: "Inicio App" (24:7). */
@Composable
fun SplashScreen(onFinished: () -> Unit = {}) {
    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MS)
        onFinished()
    }

    FigmaCanvas(designHeight = 917.dp, background = app_colors.splashBackground) {
        // "App fondo azul 1"
        Image(
            painter = painterResource(R.drawable.logo_splash),
            contentDescription = "U-Spot. Descubre. Disfruta. Conecta.",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.figmaPosition(left = (-5.01).dp, top = 216.98.dp, width = 426.03.dp, height = 445.dp)
        )
        Box(
            Modifier
                .figmaPosition(left = 0.dp, top = 640.dp, width = 417.dp, height = 30.98.dp)
                .background(app_colors.splashLogoStrip)
        )

        Text(
            text = "¡¡EXPLORA TU CAMPUS!!",
            style = figmaText(
                app_fonts.poppins, 14.sp, 20.sp, app_colors.splashText,
                weight = FontWeight.SemiBold, letterSpacing = 1.4.sp
            ),
            modifier = Modifier.figmaPosition(left = 108.5.dp, top = 678.dp)
        )

        // "Organic Decorative Element" (degradado turquesa → amarillo con desenfoque)
        Image(
            painter = painterResource(R.drawable.splash_organic),
            contentDescription = null,
            modifier = Modifier
                .figmaPosition(right = (-94.98).dp, top = (-95).dp, width = 295.984.dp, height = 295.06.dp)
                .blur(16.5.dp, BlurredEdgeTreatment.Unbounded)
        )
    }
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun SplashPreview() {
    SplashScreen()
}
