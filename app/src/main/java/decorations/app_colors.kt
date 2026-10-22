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

    // Pantalla Principal (Home)
    val homeTitle = Color(0xFF0F172A)
    val homeSubtitle = Color(0xFF64748B)
    val homeTeal = Color(0xFF0D9488)
    val homeBadgeTeal = Color(0xFF00B894)
    val homeCardBorder = Color(0xFFEEF2F6)

    // Categorías (Home)
    val catRestaurantBg = Color(0xFFD1F4EE)
    val catRestaurantBorder = Color(0xFFB5EBE2)
    val catCafeBg = Color(0xFFECEBF9)
    val catCafeBorder = Color(0xFFDDD9F4)
    val catBarBg = Color(0xFFFFF2CD)
    val catBarBorder = Color(0xFFFCE5A6)
    val catTheaterBg = Color(0xFFE3ECFA)
    val catTheaterBorder = Color(0xFFCDE0F7)
    val catOcioBg = Color(0xFFFDE5EC)
    val catOcioBorder = Color(0xFFF9CEDC)

    // Etiquetas (Pills de tarjetas en Home)
    val tagTealBg = Color(0xFFE6F8F3)
    val tagTealText = Color(0xFF0D9488)
    val tagBlueBg = Color(0xFFEBF2FE)
    val tagBlueText = Color(0xFF2563EB)
    val tagIndigoBg = Color(0xFFEEF0FF)
    val tagIndigoText = Color(0xFF4F46E5)
    val tagAmberBg = Color(0xFFFEF3C7)
    val tagAmberText = Color(0xFF92400E)

    // Barra inferior (Inicio · Spotty · Perfil)
    val spottyGradStart = Color(0xFF1B5158)
    val spottyGradEnd = Color(0xFF10B981)
    val navBorder = Color(0xFFE6ECF3)
    val navShadow = Color(0xFF0F2548)
    val navActiveBg = Color(0xFFE6F8F3)
    val navActive = Color(0xFF0F766E)
    val navInactive = Color(0xFF64748B)
    val navLabel = Color(0xFF091E42)

    // Perfil ("Bienvenida Perfil App")
    val profileCorner = Color(0xFF00BFA5)
    val profileNavy = Color(0xFF0F2548)
    val profileTeal = Color(0xFF00C1B8)
    val profileTealLight = Color(0xFF2DD4BF)
    val profileMuted = Color(0xFF7A8FA6)
    val profileBio = Color(0xFF556987)
    val profilePrice = Color(0xFF64748B)
    val profileStatsBg = Color(0xFFF0F6FD)
    val profileStatsDivider = Color(0xB3E2E8F0)
    val profileCardBorder = Color(0xFFF1F5F9)
    val profileRatingBadge = Color(0xF2FFFFFF)
    val profileSavedBadge = Color(0x40000000)

    // Íconos del menú del perfil
    val profileMenuFavoritesBg = Color(0xFFE0F7F6)
    val profileMenuSettingsBg = Color(0xFFE6F0FA)
    val profileMenuPreferencesBg = Color(0xFFFEF3D6)
    val profileMenuHelpBg = Color(0xFFEAE8FD)

    // Perfil: configuraciones de cuenta
    val profileYellow = Color(0xFFFBBF24)
    val cfgTitle = Color(0xFF0B2545)
    val cfgHeading = Color(0xFF0F254B)
    val cfgSubtitle = Color(0xFF4B5E7E)
    val cfgCardBorder = Color(0xFFE9EEF5)
    val cfgSegmentBg = Color(0xFFDFE7F1)
    val cfgPrivacyPill = Color(0xFFD6E6F5)
    val cfgDanger = Color(0xFFE06764)
    val cfgLogoutBlue = Color(0xFF0085FF)

    // Perfil: ventanas emergentes
    val dialogCard = Color(0xFFFBF9F9)
    val dialogBorder = Color(0xFF084B42)
    val dialogNeutralButton = Color(0xFFF2EFE7)
    val dialogRed = Color(0xFFFF0000)
    val dialogAmber = Color(0xFFFCC73F)

    // Perfil: foto de perfil y galería
    val photoTitle = Color(0xFF172B4D)
    val galleryBackground = Color(0xFFF8FAFC)
    val galleryMuted = Color(0xFF94A3B8)
    val galleryDot = Color(0xFFFACC15)
    val galleryBorder = Color(0xFFE2E8F0)
    val galleryTeal = Color(0xFF14B8A6)

    // Perfil: preferencias y lugares favoritos
    val prefTitle = Color(0xFF122B49)
    val favTitle = Color(0xFF0D2B59)
    val favSubtitle = Color(0xFF60728C)
    val favCardBorder = Color(0xFF007FFF)
    val favRating = Color(0xFF475569)
    val favDot = Color(0xFFCBD5E1)
    val favTagGreenBg = Color(0xFFDCFCE7)
    val favTagGreenText = Color(0xFF15803D)
    val favTagBlueBg = Color(0xFFE0F2FE)
    val favTagBlueText = Color(0xFF0369A1)
    val favTagPurpleBg = Color(0xFFF3E8FF)
    val favTagPurpleText = Color(0xFF7E22CE)

    // Perfil: ayuda y soporte
    val helpTitle = Color(0xFF192A56)
    val helpCard = Color(0xFFCFE4E9)
    val helpBody = Color(0xFF334155)
    val helpQuestion = Color(0xFF00A8B5)
    val reportRequired = Color(0xFFFF0606)
}

