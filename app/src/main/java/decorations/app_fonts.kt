package decorations

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.example.u_spot.R

/**
 * Tipografías usadas en Figma: Poppins (splash, login, mensajes de error), Inter
 * (registro, cambio de contraseña y saludo del Home) y Plus Jakarta Sans (módulos de perfil y Home).
 * Archivos en res/font, licencia SIL OFL (Google Fonts).
 */
@OptIn(ExperimentalTextApi::class)
object app_fonts {
    val poppins = FontFamily(
        Font(R.font.poppins_regular, FontWeight.Normal),
        Font(R.font.poppins_medium, FontWeight.Medium),
        Font(R.font.poppins_semibold, FontWeight.SemiBold),
        Font(R.font.poppins_bold, FontWeight.Bold)
    )

    val inter = FontFamily(
        listOf(400, 500, 600, 700, 800).map { weight ->
            Font(
                R.font.inter_variable,
                FontWeight(weight),
                variationSettings = FontVariation.Settings(FontVariation.weight(weight))
            )
        }
    )

    val plusJakarta = FontFamily(
        listOf(400, 500, 600, 700, 800).map { weight ->
            Font(
                R.font.plus_jakarta_sans_variable,
                FontWeight(weight),
                variationSettings = FontVariation.Settings(FontVariation.weight(weight))
            )
        }
    )
}
