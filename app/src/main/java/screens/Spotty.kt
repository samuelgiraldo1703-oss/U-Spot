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
import decorations.SpottyGreeting
import decorations.SpottyHistoryCorner
import decorations.SpottyHistoryPanel
import decorations.SpottyInputBar
import decorations.SpottyLogo
import decorations.SpottyModelBadge
import decorations.SpottyNavBar
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
    val demo = remember { SpottyChat(SpottyEngine({ PlaceStore.all(context) }, AndroidSpottyLocator(context))) { isOnline(context) } }

    var input by rememberSaveable { mutableStateOf("") }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<SpottyChatSummary?>(null) }
    var showFavToast by remember { mutableStateOf(false) }

    fun send(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || demo.typing) return
        input = ""
        scope.launch { demo.send(clean, userName) }
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
                if (demo.messages.isEmpty() && !demo.typing) {
                    SpottyWelcome(
                        userName = userName,
                        input = input,
                        onInput = { input = it },
                        onSend = { send(input) },
                        onHistory = { showHistory = true }
                    )
                } else {
                    SpottyConversation(
                        userName = userName,
                        demo = demo,
                        placesById = placesById,
                        favorites = favorites,
                        onChip = ::send,
                        onHistory = { showHistory = true },
                        onOpenPlace = onOpenPlace,
                        onFavorite = { id ->
                            if (id !in favorites) showFavToast = true
                            FavoriteStore.toggle(context, id)
                        },
                        onRetry = { scope.launch { demo.retry(userName) } }
                    )
                }
            }
            if (demo.messages.isNotEmpty() || demo.typing) {
                SpottyInputBar(
                    value = input,
                    onValueChange = { input = it },
                    onSend = { send(input) },
                    suggestions = demo.messages.lastOrNull()?.takeIf { it.role == SpottyRole.SPOTTY }?.suggestions.orEmpty(),
                    onSuggestion = ::send,
                    enabled = !demo.typing
                )
            }
            if (!imeVisible) {
                SpottyNavBar(
                    onHome = onHome,
                    onProfile = onProfile,
                    background = if (demo.messages.isEmpty() && !demo.typing) Color.Transparent else Color(0xFF3F8578)
                )
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
                chats = demo.chats,
                currentChatId = demo.currentChatId,
                onNewChat = { demo.newChat(); showHistory = false },
                onOpen = { demo.open(it.id); showHistory = false },
                onDelete = { toDelete = it },
                onClose = { showHistory = false },
                modifier = Modifier.width(SPOTTY_PANEL_WIDTH)
            )
        }

        toDelete?.let { chat ->
            SpottyDeleteDialog(
                chatTitle = chat.title,
                onCancel = { toDelete = null },
                onDelete = { demo.delete(chat.id); toDelete = null }
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
    onHistory: () -> Unit
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
            Spacer(Modifier.weight(1f))
        }
    }
}

/** Conversación (579:369 arriba, 593:11 abajo) bajo el encabezado fijo. */
@Composable
private fun SpottyConversation(
    userName: String,
    demo: SpottyChat,
    placesById: Map<String, Place>,
    favorites: Set<String>,
    onChip: (String) -> Unit,
    onHistory: () -> Unit,
    onOpenPlace: (String) -> Unit,
    onFavorite: (String) -> Unit,
    onRetry: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Índices: 0 etiqueta del modelo, 1 saludo, 2.. mensajes, luego "escribiendo" o el aviso
    val extra = if (demo.typing || demo.problem != null) 1 else 0
    val lastIndex = 1 + demo.messages.size + extra
    LaunchedEffect(demo.messages.size, extra) {
        val last = demo.messages.lastOrNull() ?: return@LaunchedEffect
        if (last.role == SpottyRole.SPOTTY && extra == 0) {
            // Como en los chats de IA: la pregunta queda arriba y la respuesta se lee hacia abajo
            val question = demo.messages.indexOfLast { it.role == SpottyRole.USER }
            listState.animateScrollToItem(2 + question.coerceAtLeast(0))
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
            item(key = "greeting") {
                Column {
                    SpottyGreeting(userName, demo.startedAt)
                    Spacer(Modifier.height(12.dp))
                    SpottyQuickChips(QUICK_CHIPS, onChip)
                }
            }
            items(demo.messages, key = { it.id }) { msg ->
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
            if (demo.typing) item(key = "typing") { SpottyTypingBubble() }
            demo.problem?.let { p -> item(key = "problem") { SpottyProblemBubble(p, onRetry.takeIf { p == SpottyProblem.OFFLINE || p == SpottyProblem.GENERIC }) } }
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
// Estado de los chats. Fase 3: en memoria (se pierde al cerrar la app); Fase 5 lo guarda en
// Firestore (users/{uid}/chats).
// =============================================================================================

private class SpottyChat(private val engine: SpottyEngine, private val isOnline: () -> Boolean) {
    val chats = mutableStateListOf<SpottyChatSummary>()
    private val store = mutableStateMapOf<String, List<SpottyMessage>>()
    var currentChatId by mutableStateOf<String?>(null)
    var typing by mutableStateOf(false)
    var problem by mutableStateOf<SpottyProblem?>(null)
    var startedAt by mutableStateOf(System.currentTimeMillis())

    val messages: List<SpottyMessage> get() = currentChatId?.let { store[it] }.orEmpty()

    fun newChat() {
        currentChatId = null; problem = null; typing = false; startedAt = System.currentTimeMillis()
    }

    fun open(id: String) {
        currentChatId = id; problem = null; typing = false
        startedAt = store[id]?.firstOrNull()?.time ?: System.currentTimeMillis()
    }

    fun delete(id: String) {
        chats.removeAll { it.id == id }
        store.remove(id)
        if (currentChatId == id) newChat()
    }

    private fun add(chatId: String, msg: SpottyMessage) {
        store[chatId] = store[chatId].orEmpty() + msg
        val i = chats.indexOfFirst { it.id == chatId }
        if (i >= 0) chats[i] = chats[i].copy(updatedAt = msg.time)
    }

    suspend fun send(text: String, userName: String) {
        val now = System.currentTimeMillis()
        val chatId = currentChatId ?: UUID.randomUUID().toString().also { id ->
            // El título sale de la primera pregunta (sin gastar una llamada a Gemini)
            chats.add(0, SpottyChatSummary(id, text.take(60), now))
            currentChatId = id
            startedAt = now
        }
        problem = null
        add(chatId, SpottyMessage(UUID.randomUUID().toString(), SpottyRole.USER, text, now))
        answer(chatId, userName)
    }

    /** Vuelve a pedir la respuesta de la última pregunta (después de un error). */
    suspend fun retry(userName: String) {
        val chatId = currentChatId ?: return
        if (store[chatId]?.lastOrNull()?.role != SpottyRole.USER) return
        problem = null
        answer(chatId, userName)
    }

    private suspend fun answer(chatId: String, userName: String) {
        if (!isOnline()) { problem = SpottyProblem.OFFLINE; return }
        typing = true
        try {
            val reply = engine.reply(store[chatId].orEmpty(), userName)
            add(chatId, reply)
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
