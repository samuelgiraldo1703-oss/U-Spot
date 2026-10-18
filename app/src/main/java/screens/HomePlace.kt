package screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import data.COMMENT_MAX_LENGTH
import data.HomePlace
import data.PlaceComment
import data.UserProfile
import data.addPlaceComment
import data.decodeBase64Image
import data.rememberPlaceComments
import decorations.Scene
import decorations.SceneScreen
import decorations.firstNode
import decorations.jakartaText
import decorations.placeAt
import kotlinx.coroutines.launch

private val CommentTeal = Color(0xFF14A39E)
private val CommentName = Color(0xFF0F766E)
private val CommentBubble = Color(0xFFEEF2F6)
private val CommentInput = Color(0xFFEDF1F5)
private val CommentText = Color(0xFF1E293B)
private val CommentHint = Color(0xFF8A94A6)

/** Fondo del encabezado (foto) de las pantallas de detalle, también detrás de la barra de estado. */
private val HeroDark = Color(0xFF171717)

/**
 * Detalle de un lugar: Figma "Home App (Restaurantes)", "(Cafeterias)", "(Bares)"... (17 pantallas).
 * Debajo de "Compartir" va la sección de comentarios (no estaba en Figma: se hizo con la imagen
 * de referencia): lista guardada en Firestore, campo para escribir, "+" para adjuntar una foto de
 * la galería y botón de enviar.
 */
@Composable
fun HomePlaceScreen(
    place: HomePlace = HomePlace.WOK,
    user: UserProfile = UserProfile.Preview,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    LightStatusBarIcons()

    SceneScreen(
        sceneKey = place.sceneKey,
        statusBarColor = HeroDark,
        onClick = { scene ->
            buildMap {
                shareButton(scene)?.let { node ->
                    put(node.id) {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Mira ${place.title} en U-Spot: descubre, disfruta y conecta.")
                        }
                        context.startActivity(Intent.createChooser(send, "Compartir"))
                    }
                }
            }
        },
        overlay = { scene ->
            // Botón "Volver" dibujado sobre la foto
            Box(
                Modifier
                    .placeAt((place.backX - 24).dp, (place.backY - 24).dp, 48.dp, 48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack)
            )
            val share = shareButton(scene)
            val top = share?.let { it.y + it.h + 20f } ?: 720f
            CommentsSection(
                placeKey = place.key,
                user = user,
                modifier = Modifier
                    .padding(top = top.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
            )
        }
    )
}

private fun shareButton(scene: Scene) = scene.firstNode { it.name == "Compartir Button" }

/** La foto del encabezado es oscura: íconos claros en la barra de estado mientras se ve esta pantalla. */
@Composable
private fun LightStatusBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val before = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { if (before != null) controller.isAppearanceLightStatusBars = before }
    }
}

@Composable
private fun CommentsSection(placeKey: String, user: UserProfile, modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val comments = rememberPlaceComments(placeKey)
    var text by rememberSaveable(placeKey) { mutableStateOf("") }
    var photo by rememberSaveable(placeKey) { mutableStateOf<Uri?>(null) }
    var sending by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photo = uri
    }

    fun send() {
        if (sending || (text.isBlank() && photo == null)) return
        sending = true
        scope.launch {
            val ok = addPlaceComment(context, placeKey, text, photo, user)
            sending = false
            if (ok) {
                text = ""
                photo = null
                keyboard?.hide()
            } else {
                Toast.makeText(context, "No se pudo publicar el comentario", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(modifier) {
        Text(
            text = "Comentarios",
            style = jakartaText(20.sp, 28.sp, CommentText, FontWeight.Bold, letterSpacing = (-0.3).sp)
        )
        Spacer(Modifier.height(12.dp))
        if (comments.isEmpty()) {
            Text(
                text = "Aún no hay comentarios. ¡Sé el primero en contar tu experiencia!",
                style = jakartaText(13.sp, 19.sp, CommentHint)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (c in comments) CommentRow(c)
        }
        Spacer(Modifier.height(16.dp))

        photo?.let { uri ->
            PendingPhoto(uri, onRemove = { photo = null })
            Spacer(Modifier.height(10.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // "+": adjuntar una foto de la galería
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(26.dp)
                        .border(BorderStroke(2.dp, CommentTeal), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Agregar foto", tint = CommentTeal, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(CommentInput)
                    .padding(start = 18.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
                    if (text.isEmpty()) {
                        Text(
                            text = "Comparte tu experiencia...",
                            style = jakartaText(15.sp, 20.sp, CommentHint),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it.take(COMMENT_MAX_LENGTH) },
                        textStyle = jakartaText(15.sp, 20.sp, CommentText),
                        cursorBrush = SolidColor(CommentTeal),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus)
                    )
                }
                // Carita: abre el teclado (desde ahí se eligen los emojis)
                Icon(
                    imageVector = Icons.Outlined.SentimentSatisfied,
                    contentDescription = "Emojis",
                    tint = CommentTeal,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable {
                            focus.requestFocus()
                            keyboard?.show()
                        }
                        .padding(4.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(CommentTeal)
                    .clickable(enabled = !sending) { send() },
                contentAlignment = Alignment.Center
            ) {
                if (sending) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Enviar comentario", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun CommentRow(comment: PlaceComment) {
    val avatar = remember(comment.id) { decodeBase64Image(comment.authorPhotoBase64) }
    val photo = remember(comment.id) { decodeBase64Image(comment.photoBase64) }
    Row {
        Column(Modifier.width(54.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(avatar)
            Spacer(Modifier.height(4.dp))
            Text(
                text = comment.authorName,
                style = jakartaText(11.sp, 14.sp, CommentName, FontWeight.SemiBold, textAlign = TextAlign.Center),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(
            Modifier
                .weight(1f, fill = false)
                .clip(RoundedCornerShape(18.dp))
                .background(CommentBubble)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (comment.text.isNotEmpty()) {
                Text(text = comment.text, style = jakartaText(14.5.sp, 21.sp, CommentText))
            }
            if (photo != null) {
                if (comment.text.isNotEmpty()) Spacer(Modifier.height(8.dp))
                Image(
                    bitmap = photo,
                    contentDescription = "Foto del comentario",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .widthIn(max = 220.dp)
                        .aspectRatio(photo.width.toFloat() / photo.height.toFloat().coerceAtLeast(1f))
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }
    }
}

@Composable
private fun Avatar(photo: ImageBitmap?) {
    Box(
        Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Color(0xFFD7F3ED)),
        contentAlignment = Alignment.Center
    ) {
        if (photo != null) {
            Image(bitmap = photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(46.dp))
        } else {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = CommentName, modifier = Modifier.size(26.dp))
        }
    }
}

/** Foto elegida de la galería, antes de enviarla (con "x" para quitarla). */
@Composable
private fun PendingPhoto(uri: Uri, onRemove: () -> Unit) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        runCatching {
            android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                val scale = 240f / maxOf(info.size.width, info.size.height)
                if (scale < 1f) decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
            }
        }.getOrNull()
    }
    Box(Modifier.size(72.dp)) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Foto para adjuntar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(0x99000000))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "Quitar foto", tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun HomePlacePreview() {
    HomePlaceScreen()
}
