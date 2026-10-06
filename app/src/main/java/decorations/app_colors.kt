package decorations

import androidx.compose.ui.graphics.Color

/**
 * Colores tomados directamente de Figma (archivo "U-Spot App", módulo de login).
 */
object app_colors {
    val white = Color(0xFFFFFFFF)
    val black = Color(0xFF000000)

    // Splash ("Inicio App")
    val splashBackground = Color(0xFF10254A)
    val splashLogoStrip = Color(0xFF0C264D)
    val splashText = Color(0xFFC5C6CF)

    // Bienvenida / Login
    val loginTitle = Color(0xFF1F2937)
    val loginInput = Color(0xFFEFEDF1)
    val loginPlaceholder = Color(0xFF6B7280)
    val loginBlue = Color(0xFF2563EB)
    val loginDivider = Color(0xFFC5C6CF)
    val loginError = Color(0xFFBC3232)

    // Registro / Cambiar contraseña
    val formTitle = Color(0xFF1E293B)
    val formSubtitle = Color(0xFF64748B)
    val formInput = Color(0xFFF8FAFC)
    val formBorder = Color(0xFFE2E8F0)
    val formPlaceholder = Color(0xFF94A3B8)
    val formBlue = Color(0xFF0056D2)
    val formError = Color(0xFFFF383C)

    // Sin conexión
    val offlineCardBorder = Color(0xFF084B42)
    val offlineRetry = Color(0xFFFBBF24)

    // Destellos decorativos (SVG "Sparkles")
    val sparkleYellow = Color(0xFFFBBF24)
    val sparkleGreen = Color(0xFF34D399)
    val sparkleBlue = Color(0xFF60A5FA)
    val sparkleDash = Color(0xFF10B981)
}
