package screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.u_spot.Icons
import com.example.u_spot.R

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit = { _, _ -> },
    onRegister: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
    onGoogleLogin: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(decorations.app_colors.white)
            .clip(RoundedCornerShape(20.dp))
    ) {

        decorations.corner_shapes.DrawCornerShapes()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {

            Spacer(modifier = Modifier.height(75.dp))

            Image(
                painter = painterResource(id = R.drawable.logo_uspott),
                contentDescription = "U-Spot",
                modifier = Modifier
                    .width(160.dp)
                    .height(55.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(35.dp))

            Text(
                text = "¡Bienvenido!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = decorations.app_colors.black
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Inicia sesion para descubrir nuevos lugares y experiencias.",
                textAlign = TextAlign.Justify,
                fontSize = 10.sp,
                lineHeight = 15.sp,
                color = decorations.app_colors.black
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp),
                placeholder = {
                    Text(
                        text = "Correo Electronico",
                        textAlign = TextAlign.Justify,
                        fontSize = 10.sp,
                        color = decorations.app_colors.grey
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = decorations.app_colors.grey
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(25.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = decorations.app_colors.white,
                    focusedContainerColor = decorations.app_colors.white,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp),
                placeholder = {
                    Text(
                        text = "Contraseña",
                        textAlign = TextAlign.Justify,
                        fontSize = 10.sp,
                        color = decorations.app_colors.grey
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = decorations.app_colors.grey
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            passwordVisible = !passwordVisible
                        }
                    ) {
                        Icon(
                            imageVector =
                                if (passwordVisible)
                                    Icons.Outlined.Visibility
                                else
                                    Icons.Outlined.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = decorations.app_colors.grey
                        )
                    }
                },
                visualTransformation =
                    if (passwordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),

                singleLine = true,
                shape = RoundedCornerShape(25.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = decorations.app_colors.white,
                    focusedContainerColor = decorations.app_colors.white,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(15.dp))

            Button(
                onClick = {
                    onLogin(email, password)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = decorations.app_colors.blue
                )
            ) {
                Text(
                    text = "Iniciar Sesion →",
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    color = decorations.app_colors.white
                )
            }

            TextButton(
                onClick = onForgotPassword,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(35.dp)
            ) {
                Text(
                    text = "Olvide Mi Contraseña",
                    textAlign = TextAlign.Center,
                    fontSize = 9.sp,
                    color = decorations.app_colors.black
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = decorations.app_colors.grey
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(5.dp)
                        .border(
                            width = 1.dp,
                            color = decorations.app_colors.grey,
                            shape = CircleShape
                        )
                )

                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = decorations.app_colors.grey
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onGoogleLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(35.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    1.dp,
                    decorations.app_colors.white
                )
            ) {
                Text(
                    text = "G",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = decorations.app_colors.blue
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Continuar con Google",
                    textAlign = TextAlign.Center,
                    fontSize = 9.sp,
                    color = decorations.app_colors.black
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "¿No tienes una cuenta? ",
                    fontSize = 9.sp,
                    color = decorations.app_colors.black
                )

                Text(
                    text = "Regístrate",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = decorations.app_colors.blue,
                    modifier = Modifier.clickable {
                        onRegister()
                    }
                )
            }
        }
    }
}