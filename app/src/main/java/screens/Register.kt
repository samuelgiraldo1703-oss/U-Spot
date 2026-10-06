package screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.u_spot.Icons

@Composable
fun RegisterScreen() {

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(decorations.app_colors.white)
            .padding(horizontal = 16.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(decorations.app_colors.yellow),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "←",
                    fontSize = 25.sp,
                    color = decorations.app_colors.black
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(decorations.app_colors.blue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "U",
                        color = decorations.app_colors.white,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(7.dp))

                Text(
                    text = "U-Spot",
                    color = Color(0xFF003B73),
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "✦",
                color = decorations.app_colors.cyan,
                fontSize = 25.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "¡Completa tu Perfil!",
            fontSize = 17.sp,
            textAlign = TextAlign.Justify,
            fontWeight = FontWeight.Bold,
            color = decorations.app_colors.black
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Cuentanos sobre ti para personalizar tu experiencia.",
            fontSize = 11.sp,
            textAlign = TextAlign.Justify,
            color = decorations.app_colors.grey,
            lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        ProfileTextField(
            value = username,
            onValueChange = { username = it },
            placeholder = "Nombre De Usuario",
            icon = Icons.Default.Person
        )

        Spacer(modifier = Modifier.height(7.dp))

        ProfileTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = "Ingresa un Correo electrónico",
            icon = Icons.Default.Email,
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(7.dp))

        ProfileTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Crear Contraseña",
            icon = Icons.Default.Lock,
            isPassword = true
        )

        Spacer(modifier = Modifier.height(7.dp))

        ProfileTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            placeholder = "Confirmar nueva contraseña",
            icon = Icons.Default.Lock,
            isPassword = true
        )

        Spacer(modifier = Modifier.height(9.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(decorations.app_colors.blue)
            )

            Text(
                text = "Paso 2 de 2",
                modifier = Modifier.padding(horizontal = 8.dp),
                fontSize = 7.sp,
                color = decorations.app_colors.grey
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(decorations.app_colors.white)
            )
        }

        Spacer(modifier = Modifier.height(9.dp))

        Button(
            onClick = { },
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = decorations.app_colors.blue
            ),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {

            Text(
                text = "Crear cuenta y descubrir",
                fontSize = 11.sp,
                textAlign = TextAlign.Justify,
                color = decorations.app_colors.white,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "→",
                fontSize = 18.sp,
                color = decorations.app_colors.white
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = { },
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            shape = RoundedCornerShape(7.dp),
            border = BorderStroke(
                1.dp,
                decorations.app_colors.white
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = decorations.app_colors.white
            )
        ) {

            Text(
                text = "G",
                color = decorations.app_colors.blue,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Continuar con Google",
                textAlign = TextAlign.Center,
                color = decorations.app_colors.black,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 100.dp,
                        topEnd = 100.dp,
                        bottomStart = 12.dp,
                        bottomEnd = 12.dp
                    )
                )
        ) {

            /* Image(
                painter = painterResource(
                    id = R.drawable.entorno
                ),
                contentDescription = "Entorno",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            ) */

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        decorations.app_colors.black.copy(alpha = 0.25f)
                    )
            )

            Text(
                text = "Tu entorno\ntambien es\nparte de la U",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                color = decorations.app_colors.white,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
        placeholder = {
            Text(
                text = placeholder,
                fontSize = 9.sp,
                color = decorations.app_colors.grey
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = decorations.app_colors.grey
            )
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(
                    onClick = {
                        passwordVisible = !passwordVisible
                    }
                ) {
                    Icon(
                        imageVector = if (passwordVisible)
                            Icons.Default.Visibility
                        else
                            Icons.Default.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = decorations.app_colors.grey
                    )
                }
            }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        visualTransformation = if (
            isPassword && !passwordVisible
        ) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = decorations.app_colors.blue,
            unfocusedBorderColor = decorations.app_colors.white,
            focusedContainerColor = decorations.app_colors.white,
            unfocusedContainerColor = decorations.app_colors.white
        )
    )
}