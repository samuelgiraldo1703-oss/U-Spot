package com.example.u_spot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.u_spot.ui.theme.USpotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            USpotTheme {
                navigation.AppNavigation()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    USpotTheme {
        screens.LoginScreen (
            onLogin = {_, _ -> },
            onRegister = {},
            onForgotPassword = {},
            onGoogleLogin = {}
        )
    }
}