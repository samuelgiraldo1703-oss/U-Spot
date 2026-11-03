package decorations

import android.net.Uri
import android.util.LruCache
import android.view.ContextThemeWrapper
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.emoji2.emojipicker.EmojiPickerView
import com.example.u_spot.R
import com.google.firebase.auth.FirebaseAuth
import data.AuthorPicture
import data.COMMENT_MAX_LENGTH
import data.CommentAuthor
import data.PlaceComment
import data.UserProfile
import data.addCommentReply
import data.addPlaceComment
import data.commentAgeLabel
import data.decodeBase64Image
import data.deleteCommentReply
import data.deletePlaceComment
import data.deletePlaceCommentPhoto
import data.fillerAvatarIndex
import data.loadCommentAuthor
import data.rememberAuthorPicture
import data.rememberCommentCount
import data.rememberCommentReplies
import data.rememberIsLiked
import data.rememberPlaceComments
import data.setCommentLiked
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Comentarios que se piden cada vez que se llega al final de la lista. */
private const val COMMENTS_PAGE = 15

/** Respuestas que se muestran al desplegar un comentario, y cuántas más con "Ver más". */
private const val REPLIES_PAGE = 5

/** Alto del teclado de emojis, parecido al del teclado del sistema. */
private val EMOJI_KEYBOARD_HEIGHT = 280.dp

/** Imágenes de relleno para quienes comentaron y ya no tienen perfil. */
private val FILLER_AVATARS = listOf(
    R.drawable.ic_comment_avatar_1,
    R.drawable.ic_comment_avatar_2,
    R.drawable.ic_comment_avatar_3,
    R.drawable.ic_comment_avatar_4,
    R.drawable.ic_comment_avatar_5,
    R.drawable.ic_comment_avatar_6,
    R.drawable.ic_comment_avatar_7,
    R.drawable.ic_comment_avatar_8
)

/** Fotos de perfil ya decodificadas, por su base64. */
private val avatarCache = LruCache<String, ImageBitmap>(32)

/**
 * Sección de comentarios de la pantalla de un lugar: los 2 más recientes y el botón que abre el
 * panel con todos, para que la pantalla no crezca con cada comentario. No estaba en Figma.
 * [accent] es el color del botón (el de "Compartir" en esa categoría).
 */
@Composable
fun CommentsSection(placeKey: String, user: UserProfile, accent: Color?, modifier: Modifier = Modifier) {
    val latest = rememberPlaceComments(placeKey, limit = 2)
    var open by rememberSaveable(placeKey) { mutableStateOf(false) }
    var changes by remember { mutableIntStateOf(0) }
    val count = rememberCommentCount(placeKey, refresh = listOf(latest.items.map { it.id }, open, changes))
    val buttonColor = accent ?: app_colors.commentTeal

    Column(modifier) {
        Text(
            text = if (count != null && count > 0) stringResource(R.string.comments_title_with_count, count) else stringResource(R.string.comments_title),
            style = jakartaText(20.sp, 28.sp, app_colors.commentBody, FontWeight.Bold, letterSpacing = (-0.3).sp)
        )
        Spacer(Modifier.height(12.dp))
        if (latest.loaded && latest.items.isEmpty()) {
            Text(
                text = stringResource(R.string.comments_empty_section_hint),
                style = jakartaText(13.sp, 19.sp, app_colors.commentHint)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            for (c in latest.items) {
                CommentPreviewRow(c, onClick = { open = true })
            }
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(buttonColor)
                    .clickable { open = true }
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val content = if (buttonColor.luminance() > 0.6f) app_colors.commentBody else app_colors.white
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(if (latest.items.isNotEmpty()) R.string.comments_button_view else R.string.comments_button_first),
                    style = jakartaText(14.sp, 20.sp, content, FontWeight.Bold)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }

    if (open) {
        CommentsSheet(
            placeKey = placeKey,
            user = user,
            count = count,
            onChanged = { changes++ },
            onDismiss = { open = false }
        )
    }
}

/** Fila resumida de la pantalla del lugar: tocarla abre el panel completo. */
@Composable
private fun CommentPreviewRow(comment: PlaceComment, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        verticalAlignment = Alignment.Top
    ) {
        CommentAvatar(comment, 38.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            AuthorLine(comment)
            Spacer(Modifier.height(2.dp))
            Text(
                text = comment.text.ifEmpty { stringResource(R.string.comments_shared_photo) },
                style = jakartaText(14.sp, 20.sp, if (comment.text.isEmpty()) app_colors.commentHint else app_colors.commentBody),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** A qué se está respondiendo: siempre un comentario; [mention] si se respondió a una de sus respuestas. */
private data class ReplyTarget(val commentId: String, val authorName: String, val mention: Boolean)

/** Panel inferior con todos los comentarios del lugar, estilo Instagram/TikTok. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CommentsSheet(
    placeKey: String,
    user: UserProfile,
    count: Int?,
    onChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val list = rememberLazyListState()
    var limit by rememberSaveable(placeKey) { mutableIntStateOf(COMMENTS_PAGE) }
    val feed = rememberPlaceComments(placeKey, limit)
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid }

    var input by rememberSaveable(placeKey, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var photo by rememberSaveable(placeKey) { mutableStateOf<Uri?>(null) }
    var replyTo by remember { mutableStateOf<ReplyTarget?>(null) }
    var sending by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var emojisOpen by remember { mutableStateOf(false) }

    var toDelete by remember { mutableStateOf<PlaceComment?>(null) }
    var photoToDelete by remember { mutableStateOf<PlaceComment?>(null) }
    var profileOf by remember { mutableStateOf<PlaceComment?>(null) }

    // Al acercarse al final de la lista se pide la siguiente página
    val nearEnd by remember {
        derivedStateOf {
            val info = list.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 5
        }
    }
    LaunchedEffect(nearEnd, feed.items.size, feed.hasMore) {
        if (nearEnd && feed.hasMore && feed.items.size >= limit) limit += COMMENTS_PAGE
    }

    fun startReply(comment: PlaceComment) {
        val parent = comment.parentId
        replyTo = ReplyTarget(parent ?: comment.id, comment.authorName, mention = parent != null)
        photo = null
        // Como en Instagram, responder a una respuesta queda en el mismo hilo y la menciona
        val text = if (parent != null) "@${comment.authorName} " else ""
        input = TextFieldValue(text, TextRange(text.length))
        focus.requestFocus()
        keyboard?.show()
    }

    fun send() {
        if (sending || (input.text.isBlank() && photo == null)) return
        sending = true
        scope.launch {
            val target = replyTo
            val ok = if (target != null) addCommentReply(placeKey, target.commentId, input.text, user)
            else addPlaceComment(context, placeKey, input.text, photo, user)
            sending = false
            if (ok) {
                input = TextFieldValue("")
                photo = null
                replyTo = null
                emojisOpen = false
                keyboard?.hide()
                onChanged()
                if (target != null) expanded[target.commentId] = true else list.animateScrollToItem(0)
            } else {
                Toast.makeText(context, context.getString(R.string.comments_error_publish), Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = app_colors.white,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 4.dp).size(width = 36.dp, height = 4.dp).clip(CircleShape).background(app_colors.commentCardBorder))
        },
        contentWindowInsets = { WindowInsets(0) }
    ) {
        // Atrás cierra primero los emojis; si el teclado del sistema aparece, los reemplaza
        BackHandler(enabled = emojisOpen) { emojisOpen = false }
        val imeVisible = WindowInsets.isImeVisible
        LaunchedEffect(imeVisible) {
            if (imeVisible) emojisOpen = false
        }
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .imePadding()
        ) {
            Box(Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 8.dp)) {
                Text(
                    text = when (count) {
                        null, 0 -> stringResource(R.string.comments_title)
                        1 -> stringResource(R.string.comments_count_badge_singular)
                        else -> stringResource(R.string.comments_count_badge_plural, count)
                    },
                    style = jakartaText(15.sp, 20.sp, app_colors.commentBody, FontWeight.Bold),
                    modifier = Modifier.align(Alignment.Center)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable {
                            scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.comments_close_cd), tint = app_colors.commentBody, modifier = Modifier.size(22.dp))
                }
            }
            HorizontalDivider(color = app_colors.commentCardBorder)

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    !feed.loaded -> CircularProgressIndicator(
                        color = app_colors.commentTeal,
                        strokeWidth = 2.dp,
                        modifier = Modifier.align(Alignment.Center).size(28.dp)
                    )
                    feed.items.isEmpty() -> Text(
                        text = stringResource(R.string.comments_empty_sheet_hint),
                        style = jakartaText(14.sp, 20.sp, app_colors.commentHint, textAlign = TextAlign.Center),
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp)
                    )
                    else -> LazyColumn(
                        state = list,
                        contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(feed.items, key = { it.id }) { c ->
                            CommentThread(
                                placeKey = placeKey,
                                comment = c,
                                currentUid = currentUid,
                                expanded = expanded[c.id] == true,
                                onToggleReplies = { expanded[c.id] = expanded[c.id] != true },
                                onReply = ::startReply,
                                onDelete = { toDelete = it },
                                onDeletePhoto = { photoToDelete = it },
                                onOpenProfile = { profileOf = it }
                            )
                        }
                        if (feed.hasMore) {
                            item(key = "cargando") {
                                Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = app_colors.commentTeal, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = app_colors.commentCardBorder)
            CommentComposer(
                input = input,
                onInput = { input = it.copy(text = it.text.take(COMMENT_MAX_LENGTH)) },
                photo = photo,
                onPhoto = { photo = it },
                replyTo = replyTo,
                onCancelReply = {
                    replyTo = null
                    input = TextFieldValue("")
                },
                sending = sending,
                focus = focus,
                emojisOpen = emojisOpen,
                onToggleEmojis = {
                    if (emojisOpen) {
                        emojisOpen = false
                        focus.requestFocus()
                        keyboard?.show()
                    } else {
                        // Sin pedir el foco: al recibirlo, el campo abriría el teclado del sistema
                        emojisOpen = true
                        keyboard?.hide()
                    }
                },
                onEmoji = { input = input.withInserted(it) },
                onSend = ::send
            )
        }
    }

    toDelete?.let { c ->
        ConfirmDialog(
            title = stringResource(if (c.parentId != null) R.string.comments_delete_reply_title else R.string.comments_delete_comment_title),
            message = stringResource(R.string.comments_delete_confirm_message),
            confirm = stringResource(R.string.comments_delete_action),
            onDismiss = { toDelete = null },
            onConfirm = {
                toDelete = null
                scope.launch {
                    val ok = if (c.parentId != null) deleteCommentReply(placeKey, c) else deletePlaceComment(placeKey, c.id)
                    if (ok) onChanged()
                    Toast.makeText(
                        context,
                        context.getString(if (ok) R.string.comments_deleted_toast else R.string.comments_error_delete),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    photoToDelete?.let { c ->
        ConfirmDialog(
            title = stringResource(R.string.comments_delete_photo_title),
            message = stringResource(R.string.comments_delete_photo_message),
            confirm = stringResource(R.string.comments_delete_photo_action),
            onDismiss = { photoToDelete = null },
            onConfirm = {
                photoToDelete = null
                scope.launch {
                    val ok = deletePlaceCommentPhoto(placeKey, c.id)
                    Toast.makeText(
                        context,
                        context.getString(if (ok) R.string.comments_photo_deleted_toast else R.string.comments_error_delete_photo),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    profileOf?.let { c -> CommentAuthorCard(c, onDismiss = { profileOf = null }) }
}

/** Un comentario con sus respuestas desplegables. */
@Composable
private fun CommentThread(
    placeKey: String,
    comment: PlaceComment,
    currentUid: String?,
    expanded: Boolean,
    onToggleReplies: () -> Unit,
    onReply: (PlaceComment) -> Unit,
    onDelete: (PlaceComment) -> Unit,
    onDeletePhoto: (PlaceComment) -> Unit,
    onOpenProfile: (PlaceComment) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        CommentItem(placeKey, comment, currentUid, 40.dp, onReply, onDelete, onDeletePhoto, onOpenProfile)
        if (comment.replyCount > 0) {
            Row(
                modifier = Modifier
                    .padding(start = 50.dp, top = 8.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToggleReplies),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(24.dp).height(1.dp).background(app_colors.commentHint))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        expanded -> stringResource(R.string.comments_hide_replies)
                        comment.replyCount == 1 -> stringResource(R.string.comments_view_one_reply)
                        else -> stringResource(R.string.comments_view_replies_plural, comment.replyCount)
                    },
                    style = jakartaText(12.sp, 16.sp, app_colors.commentHint, FontWeight.SemiBold)
                )
            }
        }
        if (expanded) {
            var limit by rememberSaveable(comment.id) { mutableIntStateOf(REPLIES_PAGE) }
            val replies = rememberCommentReplies(placeKey, comment.id, limit)
            Column(
                modifier = Modifier.padding(start = 50.dp, top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                for (r in replies) {
                    CommentItem(placeKey, r, currentUid, 28.dp, onReply, onDelete, onDeletePhoto, onOpenProfile)
                }
                if (replies.size >= limit && comment.replyCount > limit) {
                    Text(
                        text = stringResource(R.string.comments_view_more_replies),
                        style = jakartaText(12.sp, 16.sp, app_colors.commentHint, FontWeight.SemiBold),
                        modifier = Modifier.clickable { limit += REPLIES_PAGE }
                    )
                }
            }
        }
    }
}

/** Autor, fecha, texto, foto, "Responder" y el corazón con su contador. */
@Composable
private fun CommentItem(
    placeKey: String,
    comment: PlaceComment,
    currentUid: String?,
    avatarSize: Dp,
    onReply: (PlaceComment) -> Unit,
    onDelete: (PlaceComment) -> Unit,
    onDeletePhoto: (PlaceComment) -> Unit,
    onOpenProfile: (PlaceComment) -> Unit
) {
    val photo = rememberCommentImage(comment.photoBase64)
    val isAuthor = currentUid != null && (comment.uid == currentUid || comment.uid.isEmpty())
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .clip(CircleShape)
                .clickable { onOpenProfile(comment) }
        ) {
            CommentAvatar(comment, avatarSize)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            AuthorLine(
                comment,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onOpenProfile(comment) }
                )
            )
            if (comment.text.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(text = comment.text, style = jakartaText(14.sp, 20.sp, app_colors.commentBody))
            }
            if (photo != null) {
                Spacer(Modifier.height(8.dp))
                Box {
                    Image(
                        bitmap = photo,
                        contentDescription = stringResource(R.string.comments_photo_cd),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .widthIn(max = 220.dp)
                            .aspectRatio(photo.width.toFloat() / photo.height.toFloat().coerceAtLeast(1f))
                            .clip(RoundedCornerShape(12.dp))
                    )
                    // Botón para eliminar únicamente la foto cuando hay texto adjunto
                    if (isAuthor && comment.text.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(app_colors.scrimDark80)
                                .clickable { onDeletePhoto(comment) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.comments_delete_photo_action), tint = app_colors.white, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.comments_reply_action),
                    style = jakartaText(12.sp, 16.sp, app_colors.commentHint, FontWeight.SemiBold),
                    modifier = Modifier
                        .clickable { onReply(comment) }
                        .padding(end = 12.dp, top = 4.dp, bottom = 4.dp)
                )
                if (isAuthor) {
                    Text(
                        text = stringResource(R.string.comments_delete_action),
                        style = jakartaText(12.sp, 16.sp, app_colors.commentHint, FontWeight.SemiBold),
                        modifier = Modifier
                            .clickable { onDelete(comment) }
                            .padding(end = 12.dp, top = 4.dp, bottom = 4.dp)
                    )
                }
            }
        }
        LikeButton(placeKey, comment)
    }
}

/** Corazón con contador. Cambia al instante y lo confirma Firestore; si falla, vuelve como estaba. */
@Composable
private fun LikeButton(placeKey: String, comment: PlaceComment) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val saved = rememberIsLiked(placeKey, comment)
    var pending by remember(comment.id) { mutableStateOf<Boolean?>(null) }
    val liked = pending ?: saved
    // El contador guardado aún no incluye el toque en curso
    val shown = (comment.likeCount + when {
        pending == null || pending == saved -> 0
        pending == true -> 1
        else -> -1
    }).coerceAtLeast(0)
    Column(
        modifier = Modifier
            .width(44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = pending == null
            ) {
                val target = !saved
                pending = target
                scope.launch {
                    val ok = setCommentLiked(placeKey, comment, target)
                    pending = null
                    if (!ok) Toast.makeText(context, context.getString(R.string.comments_error_reaction), Toast.LENGTH_SHORT).show()
                }
            }
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (liked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = stringResource(if (liked) R.string.comments_like_remove_cd else R.string.comments_like_add_cd),
            tint = if (liked) app_colors.commentLiked else app_colors.commentHint,
            modifier = Modifier.size(20.dp)
        )
        if (shown > 0) {
            Text(text = "$shown", style = jakartaText(11.sp, 14.sp, app_colors.commentHint, FontWeight.SemiBold))
        }
    }
}

/** Barra inferior para escribir: "+" para adjuntar foto, campo de texto y enviar. */
@Composable
private fun CommentComposer(
    input: TextFieldValue,
    onInput: (TextFieldValue) -> Unit,
    photo: Uri?,
    onPhoto: (Uri?) -> Unit,
    replyTo: ReplyTarget?,
    onCancelReply: () -> Unit,
    sending: Boolean,
    focus: FocusRequester,
    emojisOpen: Boolean,
    onToggleEmojis: () -> Unit,
    onEmoji: (String) -> Unit,
    onSend: () -> Unit
) {
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPhoto(uri)
    }
    Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (replyTo != null) {
                Row(Modifier.fillMaxWidth().padding(start = 6.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.comments_replying_to, replyTo.authorName),
                        style = jakartaText(12.sp, 16.sp, app_colors.commentHint, FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.comments_cancel_reply_cd),
                        tint = app_colors.commentHint,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onCancelReply)
                            .padding(4.dp)
                    )
                }
            }
            photo?.let { uri ->
                PendingPhoto(uri, onRemove = { onPhoto(null) })
                Spacer(Modifier.height(10.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Las respuestas son solo texto: el "+" de la foto es para comentarios
                if (replyTo == null) {
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
                                .border(BorderStroke(2.dp, app_colors.commentTeal), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.comments_add_photo_cd), tint = app_colors.commentTeal, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(app_colors.commentInput)
                        .padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f).padding(vertical = 10.dp)) {
                        if (input.text.isEmpty()) {
                            Text(
                                text = stringResource(if (replyTo != null) R.string.comments_reply_input_placeholder else R.string.comments_experience_input_placeholder),
                                style = jakartaText(14.sp, 20.sp, app_colors.commentHint),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = input,
                            onValueChange = onInput,
                            textStyle = jakartaText(14.sp, 20.sp, app_colors.commentBody),
                            cursorBrush = SolidColor(app_colors.commentTeal),
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focus)
                        )
                    }
                    // Carita: cambia entre el teclado del sistema y el de emojis
                    Icon(
                        imageVector = if (emojisOpen) Icons.Outlined.Keyboard else Icons.Outlined.SentimentSatisfied,
                        contentDescription = stringResource(if (emojisOpen) R.string.comments_show_keyboard_cd else R.string.comments_show_emojis_cd),
                        tint = app_colors.commentTeal,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onToggleEmojis)
                            .padding(4.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(app_colors.commentTeal)
                        .clickable(enabled = !sending, onClick = onSend),
                    contentAlignment = Alignment.Center
                ) {
                    if (sending) {
                        CircularProgressIndicator(color = app_colors.white, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = stringResource(R.string.comments_send_cd), tint = app_colors.white, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
        if (emojisOpen) EmojiKeyboard(onEmoji)
    }
}

/** Teclado de emojis de Android (categorías, recientes y tonos de piel) dentro del panel. */
@Composable
private fun EmojiKeyboard(onEmoji: (String) -> Unit) {
    val latestOnEmoji by rememberUpdatedState(onEmoji)
    AndroidView(
        factory = { context ->
            // Con el tema de la app sus títulos salen casi invisibles sobre el fondo blanco
            EmojiPickerView(ContextThemeWrapper(context, android.R.style.Theme_Material_Light_NoActionBar)).apply {
                setOnEmojiPickedListener { latestOnEmoji(it.emoji) }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(EMOJI_KEYBOARD_HEIGHT)
            .background(app_colors.white)
    )
}

/** Inserta [text] donde está el cursor (reemplaza lo seleccionado), sin pasar del largo máximo. */
private fun TextFieldValue.withInserted(text: String): TextFieldValue {
    val start = selection.min
    val end = selection.max
    if (this.text.length - (end - start) + text.length > COMMENT_MAX_LENGTH) return this
    return copy(
        text = this.text.replaceRange(start, end, text),
        selection = TextRange(start + text.length)
    )
}

/** Tarjeta con el perfil público de quien comentó; arranca con lo que ya trae el comentario. */
@Composable
private fun CommentAuthorCard(comment: PlaceComment, onDismiss: () -> Unit) {
    val author by produceState<CommentAuthor?>(null, comment.uid) {
        if (comment.uid.isNotEmpty()) value = loadCommentAuthor(comment.uid)
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(app_colors.white)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CommentAvatar(comment, 88.dp)
            Spacer(Modifier.height(14.dp))
            Text(
                text = author?.name ?: comment.authorName,
                style = jakartaText(18.sp, 24.sp, app_colors.commentBody, FontWeight.Bold, textAlign = TextAlign.Center)
            )
            author?.bio?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(6.dp))
                Text(text = it, style = jakartaText(14.sp, 20.sp, app_colors.commentHint, textAlign = TextAlign.Center))
            }
            author?.commentCount?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (it == 1) stringResource(R.string.comments_count_badge_singular) else stringResource(R.string.comments_count_badge_plural, it),
                    style = jakartaText(13.sp, 18.sp, app_colors.commentName, FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun ConfirmDialog(title: String, message: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = jakartaText(18.sp, 24.sp, app_colors.commentBody, FontWeight.Bold)) },
        text = { Text(text = message, style = jakartaText(14.sp, 20.sp, app_colors.commentBody.copy(alpha = 0.8f))) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirm, color = app_colors.commentLiked, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel), color = app_colors.commentBody.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun AuthorLine(comment: PlaceComment, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = comment.authorName,
            style = jakartaText(13.sp, 18.sp, app_colors.commentName, FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val age = commentAgeLabel(comment.createdAt)
        if (age.isNotEmpty()) {
            Text(text = "  ·  $age", style = jakartaText(12.sp, 18.sp, app_colors.commentHint), maxLines = 1)
        }
    }
}

/** Decodifica la imagen en segundo plano para que la lista no se trabe al bajar. */
@Composable
private fun rememberCommentImage(base64: String?): ImageBitmap? {
    val image by produceState<ImageBitmap?>(null, base64) {
        value = withContext(Dispatchers.Default) { decodeBase64Image(base64) }
    }
    return image
}

/** Igual que [rememberCommentImage], pero guarda las fotos ya decodificadas: un autor puede repetirse mucho en la lista. */
@Composable
private fun rememberAvatarImage(base64: String?): ImageBitmap? {
    val image by produceState(base64?.let { avatarCache.get(it) }, base64) {
        value = base64?.let { key ->
            avatarCache.get(key) ?: withContext(Dispatchers.Default) { decodeBase64Image(key) }?.also { avatarCache.put(key, it) }
        }
    }
    return image
}

/**
 * Foto de quien comentó: la de su perfil si existe y tiene; el icono genérico si existe sin foto; una
 * imagen de relleno si el perfil no existe. Sin conexión se usa la miniatura guardada en el comentario.
 */
@Composable
private fun CommentAvatar(comment: PlaceComment, size: Dp) {
    val picture = rememberAuthorPicture(comment.uid)
    val photo = rememberAvatarImage(
        when (picture) {
            is AuthorPicture.Profile -> picture.base64
            AuthorPicture.Unknown -> comment.authorPhotoBase64
            AuthorPicture.Empty, AuthorPicture.NoProfile -> null
        }
    )
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(app_colors.commentAvatarMint),
        contentAlignment = Alignment.Center
    ) {
        when {
            photo != null ->
                Image(bitmap = photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
            picture == AuthorPicture.NoProfile ->
                Image(
                    painter = painterResource(FILLER_AVATARS[fillerAvatarIndex(comment, FILLER_AVATARS.size)]),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(size)
                )
            else ->
                Icon(Icons.Rounded.Person, contentDescription = null, tint = app_colors.commentName, modifier = Modifier.size(size * 0.56f))
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
                contentDescription = stringResource(R.string.comments_pending_photo_cd),
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
                .background(app_colors.scrimDark60)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.comments_remove_photo_cd), tint = app_colors.white, modifier = Modifier.size(14.dp))
        }
    }
}
