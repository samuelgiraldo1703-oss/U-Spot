package navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import data.isOnline
import data.rememberOnlineState

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CHANGE_PASSWORD = "change_password"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    var online by rememberOnlineState()

    // Mensajes temporales mientras no existan las pantallas siguientes / el login con Google.
    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    val googlePending = { toast("Inicio con Google: pendiente de conectar con Firebase") }

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
                    onLoginSuccess = { user -> toast("Inicio de sesión exitoso: $user") },
                    onRegister = { navController.navigate(Routes.REGISTER) },
                    onForgotPassword = { navController.navigate(Routes.CHANGE_PASSWORD) },
                    onGoogleLogin = googlePending
                )
            }

            composable(Routes.REGISTER) {
                screens.RegisterScreen(
                    onBack = { navController.popBackStack() },
                    onRegistered = {
                        toast("Cuenta creada")
                        navController.popBackStack()
                    },
                    onGoogleLogin = googlePending
                )
            }

            composable(Routes.CHANGE_PASSWORD) {
                screens.ChangePasswordScreen(
                    onBack = { navController.popBackStack() },
                    onPasswordChanged = {
                        toast("Contraseña actualizada")
                        navController.popBackStack()
                    },
                    onGoogleLogin = googlePending
                )
            }
        }

        if (!online) {
            screens.OfflineScreen(onRetry = { online = isOnline(context) })
        }
    }
}
