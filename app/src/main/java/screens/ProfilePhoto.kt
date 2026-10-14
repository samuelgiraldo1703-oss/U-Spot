package screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.u_spot.R
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.PickVisualMediaRequest
import androidx.core.content.ContextCompat
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import decorations.profilePhotoPainter
import java.io.File
import decorations.BackButton
import decorations.BoxedText
import decorations.ProfileCanvas
import decorations.ProfileCorner
import decorations.ProfileDialogButton
import decorations.ProfileDialogCard
import decorations.ProfileFooter
import decorations.ProfileLeaveDialog
import decorations.ProfileLogo
import decorations.ProfileOverlay
import decorations.app_colors
import decorations.app_fonts
import decorations.figmaPosition
import decorations.figmaText
import decorations.jakartaText

/**
 * Figma: "Foto de Perfil" (337:93 con foto, 401:1106 sin foto) y la ventana
 * "Confirmacion Borrado de Imagen" (401:1104).
 */
@Composable
fun ProfilePhotoScreen(
    photo: ImageBitmap? = null,
    saving: Boolean = false,
    onBack: () -> Unit = {},
    onPhotoTaken: (Uri) -> Unit = {},
    onDeletePhoto: () -> Unit = {}
) {
    val photoDeleted = photo == null
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    // Cámara del teléfono: guarda la foto en la caché de la app (FileProvider) y la entrega por URI
    val context = LocalContext.current
    var pendingUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingUri
        if (ok && uri != null) onPhotoTaken(uri)
    }
    val openCamera = {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "perfil_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingUri = uri
        try {
            takePicture.launch(uri)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No se encontró una app de cámara", Toast.LENGTH_SHORT).show()
        }
    }

    // Galería del teléfono: primero se pide permiso para ver las fotos y, si lo acepta, se abre
    // el selector de fotos del sistema. La foto elegida se guarda igual que la de la cámara.
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPhotoTaken(uri)
    }
    val openGallery = {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val requestPhotos = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        // En Android 14+ el usuario puede dar acceso solo a algunas fotos: también cuenta como permitido
        if (result.values.any { it }) openGallery()
        else Toast.makeText(context, "Para elegir una foto, permite que U-Spot acceda a tus fotos", Toast.LENGTH_LONG).show()
    }
    val chooseFromGallery = {
        val permissions = galleryPermissions()
        if (permissions.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) openGallery()
        else requestPhotos.launch(permissions)
    }

    Box(Modifier.fillMaxSize()) {
        ProfileCanvas(designHeight = 916.dp) {
            ProfileFooter(app_colors.profileYellow)
            ProfileCorner(app_colors.formBlue)
            BackButton(
                onClick = onBack,
                modifier = Modifier.figmaPosition(right = 28.dp, top = 32.dp),
                res = R.drawable.ic_back_blue
            )
            ProfileLogo(Modifier.figmaPosition(left = 43.dp, top = 56.dp))

            Text(
                text = "Foto de Perfil",
                style = figmaText(app_fonts.poppins, 30.sp, 36.sp, app_colors.photoTitle, FontWeight.Bold, letterSpacing = (-0.75).sp),
                modifier = Modifier.figmaPosition(left = 34.dp, top = 169.5.dp)
            )

            // "Background+Shadow": aro de 250 x 260 con la foto (244 x 254) dentro
            if (photoDeleted) {
                Image(
                    painter = painterResource(R.drawable.photo_avatar_empty),
                    contentDescription = "Sin foto de perfil",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .figmaPosition(left = 81.dp, top = 225.dp, width = 250.dp, height = 260.dp)
                        .clip(CircleShape)
                        .border(6.5.dp, app_colors.profileCorner, CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .figmaPosition(left = 87.dp, top = 225.dp, width = 250.dp, height = 260.dp)
                        .dropShadow(CircleShape, Shadow(radius = 2.dp, color = Color(0x0D000000), offset = DpOffset(0.dp, 1.dp)))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(app_colors.profileTeal, app_colors.profileTealLight),
                                start = Offset(0f, Float.POSITIVE_INFINITY),
                                end = Offset(Float.POSITIVE_INFINITY, 0f)
                            ),
                            CircleShape
                        )
                        .padding(3.dp)
                ) {
                    Image(
                        painter = profilePhotoPainter(photo),
                        contentDescription = "Foto de perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            }

            if (saving) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = app_colors.profileTeal,
                    modifier = Modifier.figmaPosition(left = 182.dp, top = 331.dp, width = 48.dp, height = 48.dp)
                )
            }

            PhotoOption(
                top = 485.5.dp, icon = R.drawable.ic_photo_camera, title = "Tomar foto con\nla cámara",
                titleTop = 22.5.dp, onClick = { if (!saving) openCamera() }
            )
            PhotoOption(
                top = 591.5.dp, icon = R.drawable.ic_photo_gallery, title = "Elegir de la galería",
                titleTop = 33.5.dp, onClick = { if (!saving) chooseFromGallery() }
            )

            // "Section - DestructiveOption"
            Box(
                modifier = Modifier
                    .figmaPosition(left = 34.dp, right = 22.dp, top = 721.dp, height = 48.dp)
                    .clickable(enabled = !photoDeleted) { confirmDelete = true }
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_photo_trash),
                    contentDescription = null,
                    modifier = Modifier
                        .offset(8.dp, 8.dp)
                        .size(32.dp)
                )
                Text(
                    text = "Eliminar foto actual",
                    style = jakartaText(16.sp, 24.sp, app_colors.photoTitle, FontWeight.Bold),
                    modifier = Modifier.offset(52.dp, 12.dp)
                )
                Image(
                    painter = painterResource(R.drawable.ic_photo_chevron),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-8).dp, y = 14.dp)
                        .size(20.dp)
                )
            }
        }

        if (confirmDelete) {
            DeletePhotoDialog(
                onBack = { confirmDelete = false },
                onDelete = { onDeletePhoto(); confirmDelete = false }
            )
        }
    }
}

@Composable
private fun BoxScope.PhotoOption(top: Dp, @DrawableRes icon: Int, title: String, titleTop: Dp, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .figmaPosition(left = 34.dp, right = 22.dp, top = top, height = 90.dp)
            .dropShadow(shape, Shadow(radius = 20.dp, color = Color(0x0F000000), offset = DpOffset(0.dp, 6.dp)))
            .clip(shape)
            .background(app_colors.white)
            .border(1.dp, app_colors.profileCardBorder, shape)
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier
                .offset(17.dp, 17.dp)
                .size(64.dp, 56.dp)
        )
        Text(
            text = title,
            style = jakartaText(18.sp, 22.5.sp, app_colors.photoTitle, FontWeight.Bold),
            modifier = Modifier.offset(97.dp, titleTop)
        )
        Image(
            painter = painterResource(R.drawable.ic_photo_arrow),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-17).dp, y = 25.dp)
                .size(44.dp, 40.dp)
        )
    }
}

/** Figma: "Mensaje Validacion de Imagen" dentro de 401:1104. */
@Composable
private fun DeletePhotoDialog(onBack: () -> Unit, onDelete: () -> Unit) {
    ProfileLeaveDialog(
        question = "¿Estás seguro que deseas borrar tu imagen de perfil actual?\n",
        warning = "Perderás todos tus cambios",
        action = "Borrar imagen de perfil",
        actionX = 79.dp,
        actionWidth = 208.dp,
        onBack = onBack,
        onAction = onDelete
    )
}

/** Permiso de fotos según la versión de Android (13+ usa READ_MEDIA_IMAGES; 14+ admite acceso parcial). */
private fun galleryPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
    )
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun ProfilePhotoPreview() {
    ProfilePhotoScreen()
}
