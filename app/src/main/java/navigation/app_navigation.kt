package navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import data.GoogleAuthHelper
import data.LoginResult
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
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    var online by rememberOnlineState()
    val scope = rememberCoroutineScope()
    val googleAuthHelper = remember { GoogleAuthHelper(context) }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    val onGoogleLoginAction: () -> Unit = {
        scope.launch {
            when (googleAuthHelper.signInWithGoogle()) {
                LoginResult.Success -> {
                    val user = FirebaseAuth.getInstance().currentUser
                    toast("Bienvenido, ${user?.displayName ?: user?.email ?: "Usuario"}")
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
                LoginResult.WrongPassword -> {
                    // El usuario canceló la selección de cuenta de Google
                }
                LoginResult.AccountNotFound -> {
                    toast("No se pudo iniciar sesión con Google")
                }
            }
        }
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
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
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
