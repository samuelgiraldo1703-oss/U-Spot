package navigation

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.Composable

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {

        composable("login") {
            screens.LoginScreen(
                onLogin = { email, password ->
                    // Autenticación
                },

                onRegister = {
                    navController.navigate("register")
                }
            )
        }

        composable("register") {
            screens.RegisterScreen()
        }
    }
}