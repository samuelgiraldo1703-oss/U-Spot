package screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.FavoriteStore

import data.Place
import data.rememberFavorites
import data.rememberPlaces
import android.util.Log
import data.PlaceStore
import data.isOnline
import data.spotty.AndroidSpottyLocator
import data.spotty.GeminiClient
import data.spotty.SpottyEngine
import data.spotty.SpottyChatRepository
import data.spotty.SpottyGps
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import com.google.firebase.firestore.ListenerRegistration
import data.spotty.SpottyException

import data.spotty.SpottyChatSummary

import data.spotty.SpottyMessage
import data.spotty.SpottyProblem
import data.spotty.SpottyRole
import decorations.SPOTTY_HEADER_HEIGHT
import decorations.SPOTTY_PANEL_WIDTH
import decorations.SpottyAnalysisPill
import decorations.SpottyChatHeader
import decorations.SpottyDeleteDialog
import decorations.SpottyFavoriteToast
import decorations.SpottyFollowUp
import decorations.SpottyHistoryCorner
import decorations.SpottyHistoryPanel
import decorations.SpottyInputBar
import decorations.SpottyLogo
import decorations.SpottyModelBadge
import decorations.HomeBottomBar
import decorations.SpottyPlaceCard
import decorations.SpottyProblemBubble
import decorations.SpottyQuickChips
import decorations.SpottyScrollDownButton
import decorations.SpottyTextBubble
import decorations.SpottyTypingBubble
import decorations.SpottyUserBubble
import decorations.SpottyWelcomeInput
import decorations.app_fonts
import decorations.figmaText
import decorations.spottyBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/** Nombre del modelo que muestra la etiqueta (Fase 3 lo toma del modelo realmente usado). */
private const val MODEL_LABEL = GeminiClient.MODEL_LABEL

/** Filtros rápidos bajo el saludo, como en Figma ("Cerca de UTADEO" pasa a "Cerca de mí"). */
private val QUICK_CHIPS = listOf("🎉 Conciertos & Música", "🍻 Bares & Cócteles", "📍 Cerca de mí", "💰 < \$30k COP")

/**
 * Spotty, el asistente de planes (Figma "Home App (Vista IA)"). Sin mensajes muestra la
 * bienvenida (444:32); con mensajes, la conversación (579:369 / 593:11). El historial sale como
 * panel desde la esquina superior derecha.
 *
 * Las respuestas vienen de Gemini (data/spotty/spotty_engine.kt) con búsquedas sobre el catálogo real.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpottyScreen(
    userName: String,
    onHome: () -> Unit,
    onProfile: () -> Unit,
    onOpenPlace: (String) -> Unit
) {
    val context = LocalContext.current.applicationContext
    val places = rememberPlaces()
    val placesById = remember(places) { places.associateBy { it.id } }
    val favorites = rememberFavorites()
    val scope = rememberCoroutineScope()
    val gps = remember { SpottyGps(context) }
    val chat = remember {
        SpottyChat(
            engine = SpottyEngine({ PlaceStore.all(context) }, AndroidSpottyLocator(context, gps::current)),
            repo = SpottyChatRepository.forCurrentUser(),
            isOnline = { isOnline(context) }
        )
    }
    DisposableEffect(Unit) {
        chat.start()
        onDispose { chat.stop() }
    }

    // Permiso de ubicación: se pide solo cuando Spotty lo necesita ("cerca de mí")
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        gps.onPermissionResult(result.values.any { it })
    }
    LaunchedEffect(gps.pendingRequest) {
        if (gps.pendingRequest != null) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    var input by rememberSaveable { mutableStateOf("") }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<SpottyChatSummary?>(null) }
    var showFavToast by remember { mutableStateOf(false) }

    fun send(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || chat.typing) return
        input = ""
        scope.launch { chat.send(clean, userName) }
    }

    BackHandler(enabled = showHistory || toDelete != null) {
        if (toDelete != null) toDelete = null else showHistory = false
    }
    LaunchedEffect(showFavToast) {
        if (showFavToast) { delay(2500); showFavToast = false }
    }

    val imeVisible = WindowInsets.isImeVisible
    Box(Modifier.fillMaxSize().background(spottyBackground)) {
        Column(Modifier.fillMaxSize().imePadding()) {
            Box(Modifier.fillMaxWidth().weight(1f)) {
                if (chat.messages.isEmpty() && !chat.typing) {
                    SpottyWelcome(
                        userName = userName,
                        input = input,
                        onInput = { input = it },
                        onSend = { send(input) },
                        onHistory = { showHistory = true },
                        onChip = ::send
                    )
                } else {
                    SpottyConversation(
                        chat = chat,
                        placesById = placesById,
                        favorites = favorites,
                        onHistory = { showHistory = true },
                        onOpenPlace = onOpenPlace,
                        onFavorite = { id ->
                            if (id !in favorites) showFavToast = true
                            FavoriteStore.toggle(context, id)
                        },
                        onRetry = { scope.launch { chat.retry(userName) } }
                    )
                }
            }
            if (chat.messages.isNotEmpty() || chat.typing) {
                SpottyInputBar(
                    value = input,
                    onValueChange = { input = it },
                    onSend = { send(input) },
                    suggestions = chat.messages.lastOrNull()?.takeIf { it.role == SpottyRole.SPOTTY }?.suggestions.orEmpty(),
                    onSuggestion = ::send,
                    enabled = !chat.typing
                )
            }
            if (!imeVisible) {
                // Misma barra del Home, con Spotty resaltado (estamos dentro de Spotty)
                Box(Modifier.fillMaxWidth().background(if (chat.messages.isEmpty() && !chat.typing) Color.Transparent else Color(0xFF3F8578))) {
                    HomeBottomBar(onHome = onHome, onSpotty = {}, onProfile = onProfile, spottyActive = true)
                }
            }
        }

        // Ventanita "¡Lo has guardado en favoritos!"
        AnimatedVisibility(
            visible = showFavToast,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            SpottyFavoriteToast(onClose = { showFavToast = false })
        }

        // Panel de historial
        AnimatedVisibility(visible = showHistory, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showHistory = false }
            )
        }
        AnimatedVisibility(
            visible = showHistory,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            SpottyHistoryPanel(
                chats = chat.chats,
                currentChatId = chat.currentChatId,
                onNewChat = { chat.newChat(); showHistory = false },
                onOpen = { chat.open(it.id); showHistory = false },
                onDelete = { toDelete = it },
                onClose = { showHistory = false },
                modifier = Modifier.width(SPOTTY_PANEL_WIDTH)
            )
        }

        toDelete?.let { summary ->
            SpottyDeleteDialog(
                chatTitle = summary.title,
                onCancel = { toDelete = null },
                onDelete = { scope.launch { chat.delete(summary.id) }; toDelete = null }
            )
        }
    }
}

/** Bienvenida (444:32): logo, esquina con historial, saludo centrado y campo para escribir. */
@Composable
private fun SpottyWelcome(
    userName: String,
    input: String,
    onInput: (String) -> Unit,
    onSend: () -> Unit,
    onHistory: () -> Unit,
    onChip: (String) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        SpottyLogo(Modifier.offset(30.dp, 32.dp))
        SpottyHistoryCorner(welcome = true, onHistory = onHistory)
        Column(
            Modifier.fillMaxSize().padding(top = 123.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))
            // En Figma el bloque del saludo está centrado sobre x = 215 (9 dp a la derecha del centro)
            Column(Modifier.offset(x = 9.dp).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "¡Hola, $userName!",
                    style = figmaText(app_fonts.inter, 45.sp, 39.sp, Color.White, FontWeight.ExtraBold, (-0.65).sp, TextAlign.Center)
                )
                Spacer(Modifier.height(6.dp))
                Text("👋", style = figmaText(app_fonts.inter, 26.sp, 39.sp, Color(0xFF091E42), FontWeight.Bold))
                Text(
                    "¿Qué quieres hacer hoy?",
                    style = figmaText(app_fonts.inter, 20.sp, 24.sp, Color.White, FontWeight.Medium, textAlign = TextAlign.Center)
                )
            }
            Spacer(Modifier.height(80.dp))
            SpottyWelcomeInput(input, onInput, onSend, MODEL_LABEL, Modifier.offset(x = 6.dp))
            // Ideas para empezar (antes estaban bajo el saludo de la conversación)
            Spacer(Modifier.height(14.dp))
            SpottyQuickChips(QUICK_CHIPS, onChip, start = 34.dp)
            Spacer(Modifier.weight(1f))
        }
    }
}

/** Conversación (579:369 arriba, 593:11 abajo) bajo el encabezado fijo. */
@Composable
private fun SpottyConversation(
    chat: SpottyChat,
    placesById: Map<String, Place>,
    favorites: Set<String>,
    onHistory: () -> Unit,
    onOpenPlace: (String) -> Unit,
    onFavorite: (String) -> Unit,
    onRetry: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Índices: 0 etiqueta del modelo, 1.. mensajes, luego "escribiendo" o el aviso
    val extra = if (chat.typing || chat.problem != null) 1 else 0
    val lastIndex = chat.messages.size + extra
    LaunchedEffect(chat.messages.size, extra) {
        val last = chat.messages.lastOrNull() ?: return@LaunchedEffect
        if (last.role == SpottyRole.SPOTTY && extra == 0) {
            // Como en los chats de IA: la pregunta queda arriba y la respuesta se lee hacia abajo
            val question = chat.messages.indexOfLast { it.role == SpottyRole.USER }
            listState.animateScrollToItem(1 + question.coerceAtLeast(0))
        } else {
            listState.animateScrollToItem(lastIndex)
            listState.scrollToItem(lastIndex, Int.MAX_VALUE / 2)
        }
    }
    val atBottom by remember { derivedStateOf { !listState.canScrollForward } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = SPOTTY_HEADER_HEIGHT, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "badge") {
                Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                    SpottyModelBadge(MODEL_LABEL)
                }
            }
            items(chat.messages, key = { it.id }) { msg ->
                if (msg.role == SpottyRole.USER) {
                    SpottyUserBubble(msg.text, msg.time)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        msg.analysis?.let { SpottyAnalysisPill(it) }
                        SpottyTextBubble(msg.text)
                        msg.cards.forEachIndexed { i, card ->
                            val place = placesById[card.placeId] ?: return@forEachIndexed
                            SpottyPlaceCard(
                                place = place,
                                card = card,
                                isFavorite = place.id in favorites,
                                primary = i == 0,
                                onDetails = { onOpenPlace(place.id) },
                                onFavorite = { onFavorite(place.id) }
                            )
                        }
                        SpottyFollowUp(msg.followUp, msg.time)
                    }
                }
            }
            if (chat.typing) item(key = "typing") { SpottyTypingBubble() }
            chat.problem?.let { p -> item(key = "problem") { SpottyProblemBubble(p, onRetry.takeIf { p == SpottyProblem.OFFLINE || p == SpottyProblem.GENERIC }) } }
        }

        SpottyChatHeader(onHistory = onHistory, modifier = Modifier.align(Alignment.TopCenter))

        AnimatedVisibility(
            visible = !atBottom,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 12.dp)
        ) {
            SpottyScrollDownButton(onClick = {
                scope.launch {
                    listState.animateScrollToItem(lastIndex)
                    listState.scrollToItem(lastIndex, Int.MAX_VALUE / 2)
                }
            })
        }
    }
}

// =============================================================================================
// Estado de los chats: historial en Firestore (users/{uid}/chats). Sin sesión, solo en memoria.
// =============================================================================================

private class SpottyChat(
    private val engine: SpottyEngine,
    repo: SpottyChatRepository?,
    private val isOnline: () -> Boolean
) {
    /** Se vuelve null si Firestore rechaza el acceso: desde ahí el historial vive en memoria. */
    private var repo: SpottyChatRepository? = repo
    val chats = mutableStateListOf<SpottyChatSummary>()
    val messages = mutableStateListOf<SpottyMessage>()
    var currentChatId by mutableStateOf<String?>(null)
    var typing by mutableStateOf(false)
    var problem by mutableStateOf<SpottyProblem?>(null)
    var startedAt by mutableStateOf(System.currentTimeMillis())

    private var chatsListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null
    /** Sin sesión: mensajes de cada chat en memoria. */
    private val memory = mutableMapOf<String, List<SpottyMessage>>()

    fun start() {
        chatsListener = repo?.observeChats(onError = { goLocal() }) { list -> chats.clear(); chats.addAll(list) }
    }

    /** Firestore no disponible (p. ej. reglas sin publicar): se sigue en memoria sin perder lo que hay en pantalla. */
    private fun goLocal() {
        repo = null
        messagesListener?.remove(); messagesListener = null
        currentChatId?.let { memory[it] = messages.toList() }
    }

    fun stop() {
        chatsListener?.remove(); messagesListener?.remove()
    }

    fun newChat() {
        messagesListener?.remove(); messagesListener = null
        currentChatId = null; messages.clear(); problem = null; typing = false
        startedAt = System.currentTimeMillis()
    }

    fun open(id: String) {
        newChat()
        currentChatId = id
        if (repo == null) { messages.addAll(memory[id].orEmpty()); startedAt = messages.firstOrNull()?.time ?: startedAt; return }
        listen(id)
    }

    private fun listen(id: String) {
        messagesListener?.remove()
        messagesListener = repo?.observeMessages(id) { list ->
            if (currentChatId != id) return@observeMessages
            messages.clear(); messages.addAll(list)
            list.firstOrNull()?.let { startedAt = it.time }
        }
    }

    suspend fun delete(id: String) {
        if (currentChatId == id) newChat()
        chats.removeAll { it.id == id }
        memory.remove(id)
        repo?.delete(id)
    }

    private fun save(chatId: String, msg: SpottyMessage, title: String? = null) {
        if (currentChatId == chatId && messages.none { it.id == msg.id }) messages.add(msg)
        val cloud = repo
        if (cloud != null) cloud.add(chatId, msg, title)
        else {
            memory[chatId] = memory[chatId].orEmpty() + msg
            val i = chats.indexOfFirst { it.id == chatId }
            if (i >= 0) chats[i] = chats[i].copy(updatedAt = msg.time) else chats.add(0, SpottyChatSummary(chatId, title ?: msg.text.take(60), msg.time))
        }
    }

    suspend fun send(text: String, userName: String) {
        val now = System.currentTimeMillis()
        var title: String? = null
        val chatId = currentChatId ?: (repo?.newChatId() ?: UUID.randomUUID().toString()).also { id ->
            // El título sale de la primera pregunta (sin gastar una llamada a Gemini)
            title = text.take(60)
            currentChatId = id
            startedAt = now
        }
        problem = null
        save(chatId, SpottyMessage(UUID.randomUUID().toString(), SpottyRole.USER, text, now), title)
        if (title != null && repo != null) listen(chatId)
        answer(chatId, userName)
    }

    /** Vuelve a pedir la respuesta de la última pregunta (después de un error). */
    suspend fun retry(userName: String) {
        val chatId = currentChatId ?: return
        if (messages.lastOrNull()?.role != SpottyRole.USER) return
        problem = null
        answer(chatId, userName)
    }

    private suspend fun answer(chatId: String, userName: String) {
        if (!isOnline()) { problem = SpottyProblem.OFFLINE; return }
        typing = true
        try {
            val reply = engine.reply(messages.toList(), userName)
            save(chatId, reply)
        } catch (e: SpottyException) {
            if (currentChatId == chatId) problem = e.problem
        } catch (e: Exception) {
            Log.e("Spotty", "Error inesperado", e)
            if (currentChatId == chatId) problem = SpottyProblem.GENERIC
        } finally {
            typing = false
        }
    }
}

@Preview(widthDp = 412, heightDp = 916)
@Composable
private fun SpottyPreview() {
    SpottyScreen(userName = "Usuario", onHome = {}, onProfile = {}, onOpenPlace = {})
}
