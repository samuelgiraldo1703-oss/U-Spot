package navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import data.GoogleAuthHelper
import data.GoogleSignInResult
import data.isOnline
import data.rememberOnlineState
import kotlinx.coroutines.launch

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CHANGE_PASSWORD = "change_password"
    const val EMAIL_VERIFICATION = "email_verification/{email}"
    const val HOME = "home"
    const val HOME_CATEGORY = "home/category/{key}"
    const val HOME_PLACE = "home/place/{key}"
    const val PROFILE = "profile"
    const val PROFILE_SETTINGS = "profile/settings"
    const val PROFILE_PHOTO = "profile/photo"
    const val PROFILE_PREFERENCES = "profile/preferences"
    const val PROFILE_FAVORITES = "profile/favorites"
    const val PROFILE_HELP = "profile/help"
    const val PROFILE_HELP_CENTER = "profile/help/center"
    const val PROFILE_SECURITY_STATUS = "profile/help/security"
    const val PROFILE_REPORT = "profile/help/report"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // Perfil del usuario con sesión iniciada (Firebase Auth + documento users/{uid} en Firestore)
    val user = data.rememberUserProfile()
    val context = LocalContext.current
    var online by rememberOnlineState()
    val scope = rememberCoroutineScope()
    val googleAuthHelper = remember { GoogleAuthHelper(context) }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    var googleSigningIn by remember { mutableStateOf(false) }

    // "Continuar con Google" desde Login, Registro o Cambiar contraseña
    val onGoogleLoginAction: () -> Unit = {
        if (!googleSigningIn) {
            googleSigningIn = true
            scope.launch {
                try {
                    when (val result = googleAuthHelper.signInWithGoogle()) {
                        is GoogleSignInResult.Success -> {
                            toast("Bienvenido, ${result.name}")
                            // Limpia toda la pila (Login/Registro/...) para que "atrás" no vuelva al login
                            navController.navigate(Routes.HOME) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        }
                        GoogleSignInResult.Cancelled -> Unit
                        GoogleSignInResult.NoNetwork -> online = false
                        is GoogleSignInResult.Error -> toast(result.message)
                    }
                } finally {
                    googleSigningIn = false
                }
            }
        }
    }

    // Cierra la sesión de Firebase y de Credential Manager (vuelve a salir el selector de cuentas)
    fun signOut() {
        scope.launch { googleAuthHelper.signOut() }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = Routes.SPLASH) {

            composable(Routes.SPLASH) {
                screens.SplashScreen(
                    onFinished = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.LOGIN) {
                screens.LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onRegister = { navController.navigate(Routes.REGISTER) },
                    onForgotPassword = { navController.navigate(Routes.CHANGE_PASSWORD) },
                    onGoogleLogin = onGoogleLoginAction
                )
            }

            composable(Routes.HOME) {
                screens.HomeScreen(
                    userName = user.name,
                    onCategory = { navController.navigate("home/category/${it.key}") },
                    onPlace = { navController.navigate("home/place/$it") },
                    onProfile = { navController.navigate(Routes.PROFILE) },
                    // "Cerrar sesión" cierra la sesión de Firebase y muestra "Se ha cerrado la sesión con éxito"
                    onLogout = { signOut() },
                    onGoToLogin = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.HOME_CATEGORY) { entry ->
                val category = data.HomeCategory.fromKey(entry.arguments?.getString("key")) ?: data.HomeCategory.RESTAURANTES
                screens.HomeCategoryScreen(
                    category = category,
                    onBack = { navController.popBackStack() },
                    onPlace = { navController.navigate("home/place/${it.key}") },
                    onExtraPlace = { navController.navigate("home/place/${it.id}") },
                    // Cambiar de categoría desde el menú reemplaza la pantalla (la flecha vuelve al dashboard)
                    onCategory = {
                        navController.navigate("home/category/${it.key}") {
                            popUpTo(Routes.HOME_CATEGORY) { inclusive = true }
                        }
                    }
                )
            }

            // {key}: pantalla de Figma (HomePlace.key) o lugar de la base de datos (Place.id)
            composable(Routes.HOME_PLACE) { entry ->
                val key = entry.arguments?.getString("key")
                val places = data.rememberPlaces()
                val figma = data.HomePlace.fromKey(key)
                val extra = places.firstOrNull { it.id == key && it.figma == null }
                if (figma == null && extra != null) {
                    screens.PlaceDetailScreen(
                        place = extra,
                        user = user,
                        onBack = { navController.popBackStack() }
                    )
                } else {
                    val place = figma ?: data.HomePlace.WOK
                    screens.HomePlaceScreen(
                        place = place,
                        info = places.firstOrNull { it.figma == place },
                        user = user,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(Routes.PROFILE) {
                screens.ProfileScreen(
                    user = user,
                    onBack = { navController.popBackStack() },
                    onChangePhoto = { navController.navigate(Routes.PROFILE_PHOTO) },
                    onFavorites = { navController.navigate(Routes.PROFILE_FAVORITES) },
                    onAccountSettings = { navController.navigate(Routes.PROFILE_SETTINGS) },
                    onPreferences = { navController.navigate(Routes.PROFILE_PREFERENCES) },
                    onHelp = { navController.navigate(Routes.PROFILE_HELP) },
                    onSaveBio = { text ->
                        scope.launch { if (!data.saveBio(text)) toast("No se pudo guardar la descripción") }
                    }
                )
            }

            composable(Routes.PROFILE_PHOTO) {
                var savingPhoto by remember { mutableStateOf(false) }
                screens.ProfilePhotoScreen(
                    photo = user.photo,
                    saving = savingPhoto,
                    onBack = { navController.popBackStack() },
                    onPhotoTaken = { uri ->
                        scope.launch {
                            savingPhoto = true
                            if (!data.saveProfilePhoto(context, uri)) toast("No se pudo guardar la foto")
                            savingPhoto = false
                        }
                    },
                    onDeletePhoto = {
                        scope.launch { if (!data.deleteProfilePhoto()) toast("No se pudo borrar la foto") }
                    }
                )
            }

            composable(Routes.PROFILE_PREFERENCES) {
                screens.PreferencesScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.PROFILE_FAVORITES) {
                screens.FavoritePlacesScreen(
                    onBack = { navController.popBackStack() },
                    onPlace = { navController.navigate("home/place/$it") }
                )
            }

            composable(Routes.PROFILE_HELP) {
                screens.HelpSupportScreen(
                    onBack = { navController.popBackStack() },
                    onHelpCenter = { navController.navigate(Routes.PROFILE_HELP_CENTER) },
                    onSecurityStatus = { navController.navigate(Routes.PROFILE_SECURITY_STATUS) },
                    onReportProblem = { navController.navigate(Routes.PROFILE_REPORT) }
                )
            }

            composable(Routes.PROFILE_REPORT) {
                screens.ReportProblemScreen(onExit = { navController.popBackStack() })
            }

            composable(Routes.PROFILE_HELP_CENTER) {
                screens.HelpCenterScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.PROFILE_SECURITY_STATUS) {
                screens.SecurityStatusScreen(user = user, onBack = { navController.popBackStack() })
            }

            composable(Routes.PROFILE_SETTINGS) {
                // "Inicia Sesión" (tras "Cerrar sesión") cierra la sesión de Firebase y vuelve al login.
                // "SI" en eliminar cuenta solo lleva al login: todavía no borra la cuenta.
                val toLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
                screens.AccountSettingsScreen(
                    user = user,
                    onBack = { navController.popBackStack() },
                    onAccountDeleted = toLogin,
                    onLogin = {
                        signOut()
                        toLogin()
                    }
                )
            }

            composable(Routes.REGISTER) {
                screens.RegisterScreen(
                    onBack = { navController.popBackStack() },
                    onRegistered = {
                        toast("Cuenta creada con éxito")
                        navController.popBackStack()
                    },
                    onGoogleLogin = onGoogleLoginAction
                )
            }

            composable(Routes.CHANGE_PASSWORD) {
                screens.ChangePasswordScreen(
                    onBack = { navController.popBackStack() },
                    onPasswordChanged = { targetEmail ->
                        val encoded = try {
                            java.net.URLEncoder.encode(targetEmail, "UTF-8")
                        } catch (_: Exception) {
                            targetEmail
                        }
                        navController.navigate("email_verification/$encoded")
                    },
                    onGoogleLogin = onGoogleLoginAction
                )
            }

            composable(
                route = Routes.EMAIL_VERIFICATION,
                arguments = listOf(androidx.navigation.navArgument("email") {
                    type = androidx.navigation.NavType.StringType
                    defaultValue = ""
                })
            ) { backStackEntry ->
                val rawEmail = backStackEntry.arguments?.getString("email") ?: ""
                val decodedEmail = try {
                    java.net.URLDecoder.decode(rawEmail, "UTF-8")
                } catch (_: Exception) {
                    rawEmail
                }
                screens.EmailVerificationScreen(
                    email = decodedEmail,
                    onBack = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.LOGIN) { inclusive = false }
                        }
                    }
                )
            }
        }

        if (!online) {
            screens.OfflineScreen(onRetry = { online = isOnline(context) })
        }
    }
}
