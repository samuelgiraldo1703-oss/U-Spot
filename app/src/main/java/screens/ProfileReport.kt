package screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.u_spot.R
import decorations.BoxedText
import decorations.ProfileCanvas
import decorations.ProfileCardImage
import decorations.ProfileCorner
import decorations.ProfileDialogButton
import decorations.ProfileDialogCard
import decorations.ProfileLeaveDialog
import decorations.ProfileOverlay
import decorations.ProfileRowCard
import decorations.app_colors
import decorations.app_fonts
import decorations.figmaPosition
import decorations.figmaText
import decorations.jakartaText
import java.io.File

/** Pasos del reporte. Cada uno corresponde a uno o varios frames de Figma. */
private enum class ReportStep {
    Hub,            // 301:14
    Identify,       // 334:299
    Classify,       // 334:154 y variantes con opción marcada (314:434, 316:530, 316:609, 317:914)
    Details,        // 331:2500
    Evidence,       // 317:1111
    PreviewImage,   // 322:1278
    PreviewVideo,   // 323:1535
    FinalImage,     // 325:1864
    FinalVideo      // 325:1978
}

private enum class ReportDialog { None, LeaveReport, LeaveImage, LeaveVideo, ImageAttached, VideoAttached, Sent }

private val reportOptions = listOf("Fallo de inicio de sesión", "Enlace roto ", "Actividad mal vinculada", "Otro")

/** Modelo de datos para las solicitudes de reporte de problemas */
data class ProblemRequest(
    val id: String,
    val title: String,
    val classification: String,
    val description: String,
    val email: String,
    val username: String,
    val date: String,
    val status: String,
    val evidenceName: String? = null
)

private const val PREFS_REPORTS = "uspot_reports_prefs"
private const val KEY_REPORTS_JSON = "saved_reports"

private fun loadStoredRequests(context: Context, currentUserEmail: String, currentUserName: String): List<ProblemRequest> {
    val prefs = context.getSharedPreferences(PREFS_REPORTS, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_REPORTS_JSON, null)
    if (!raw.isNullOrBlank()) {
        try {
            val array = JSONArray(raw)
            val list = mutableListOf<ProblemRequest>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ProblemRequest(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        classification = obj.optString("classification"),
                        description = obj.optString("description"),
                        email = obj.optString("email"),
                        username = obj.optString("username"),
                        date = obj.optString("date"),
                        status = obj.optString("status"),
                        evidenceName = if (obj.has("evidenceName") && !obj.isNull("evidenceName")) obj.getString("evidenceName") else null
                    )
                )
            }
            if (list.isNotEmpty()) return list
        } catch (_: Exception) {}
    }
    val displayUser = if (currentUserName.isNotBlank()) currentUserName else "smorerar"
    val displayEmail = if (currentUserEmail.isNotBlank()) currentUserEmail else "santiago.morera@utadeo.edu.co"
    val defaults = listOf(
        ProblemRequest(
            id = "USP-1042",
            title = "Fallo de autenticación con Google institucional",
            classification = "Fallo de inicio de sesión",
            description = "Al intentar ingresar con Credential Manager y correo @utadeo.edu.co, la aplicación se cierra inesperadamente.",
            email = displayEmail,
            username = displayUser,
            date = "20 Oct 2026, 14:30",
            status = "En revisión",
            evidenceName = "Captura_error_auth.png"
        ),
        ProblemRequest(
            id = "USP-1018",
            title = "Enlace a Términos de Uso no responde",
            classification = "Enlace roto ",
            description = "El botón en el pie de página de la sección de perfil de ayuda no redirige al navegador ni carga el documento.",
            email = displayEmail,
            username = displayUser,
            date = "18 Oct 2026, 10:15",
            status = "Resuelto",
            evidenceName = null
        ),
        ProblemRequest(
            id = "USP-0985",
            title = "Horario incorrecto en cafetería central",
            classification = "Actividad mal vinculada",
            description = "El lugar aparece cerrado los sábados a mediodía cuando en realidad presta servicio continuo a estudiantes.",
            email = displayEmail,
            username = displayUser,
            date = "15 Oct 2026, 16:45",
            status = "En trámite",
            evidenceName = "Foto_horario_real.jpg"
        )
    )
    saveStoredRequests(context, defaults)
    return defaults
}

private fun saveStoredRequests(context: Context, list: List<ProblemRequest>) {
    val prefs = context.getSharedPreferences(PREFS_REPORTS, Context.MODE_PRIVATE)
    val array = JSONArray()
    for (item in list) {
        val obj = JSONObject().apply {
            put("id", item.id)
            put("title", item.title)
            put("classification", item.classification)
            put("description", item.description)
            put("email", item.email)
            put("username", item.username)
            put("date", item.date)
            put("status", item.status)
            put("evidenceName", item.evidenceName ?: JSONObject.NULL)
        }
        array.put(obj)
    }
    prefs.edit().putString(KEY_REPORTS_JSON, array.toString()).apply()
}

/** Figma: los 19 frames "Reporte de Problemas", "Validacion de Imagen", "Cancelacion de Proceso" y "Reporte Enviado". */
@Composable
fun ReportProblemScreen(onExit: () -> Unit = {}) {
    val context = LocalContext.current
    val firebaseUser = remember { FirebaseAuth.getInstance().currentUser }
    var step by rememberSaveable { mutableStateOf(ReportStep.Hub) }
    var dialog by rememberSaveable { mutableStateOf(ReportDialog.None) }
    var option by rememberSaveable { mutableStateOf(-1) }
    var email by rememberSaveable { mutableStateOf(firebaseUser?.email ?: "") }
    var username by rememberSaveable { mutableStateOf(firebaseUser?.displayName ?: "") }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedRequestId by rememberSaveable { mutableStateOf<String?>(null) }
    var isRequestsDropdownExpanded by rememberSaveable { mutableStateOf(false) }

    var requestsList by remember {
        mutableStateOf(loadStoredRequests(context, email, username))
    }

    var mediaUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val mediaUri = mediaUriString?.let { Uri.parse(it) }

    // Cámara del teléfono con el botón del "Ganchito": abre la cámara y almacena la foto tomada
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uriStr = pendingCameraUriString
        if (ok && uriStr != null) {
            mediaUriString = uriStr
            step = ReportStep.PreviewImage
        }
    }
    val openCamera = {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "reporte_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingCameraUriString = uri.toString()
        try {
            takePicture.launch(uri)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No se encontró una app de cámara", Toast.LENGTH_SHORT).show()
        }
    }

    // Galería de fotos con el botón del "Play": abre el selector de fotos del sistema
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            mediaUriString = uri.toString()
            step = ReportStep.PreviewImage
        }
    }
    val openGallery = {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val requestPhotos = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) openGallery()
        else Toast.makeText(context, "Para elegir una foto, permite que U-Spot acceda a tus fotos", Toast.LENGTH_LONG).show()
    }
    val chooseFromGallery = {
        if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
            openGallery()
        } else {
            val permissions = galleryPermissions()
            if (permissions.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) openGallery()
            else requestPhotos.launch(permissions)
        }
    }

    // El ícono de casa cambia de destino según el paso, igual que en el prototipo.
    val onHome = {
        when (step) {
            ReportStep.Hub -> dialog = ReportDialog.LeaveReport
            ReportStep.PreviewImage -> dialog = ReportDialog.LeaveImage
            ReportStep.PreviewVideo -> dialog = ReportDialog.LeaveVideo
            else -> step = ReportStep.Hub
        }
    }

    val currentDesignHeight = when {
        step == ReportStep.Classify && isRequestsDropdownExpanded -> 1550.dp
        step == ReportStep.Classify && selectedRequestId != null -> 1250.dp
        step == ReportStep.Classify -> 1100.dp
        else -> 916.dp
    }

    Box(Modifier.fillMaxSize()) {
        ProfileCanvas(designHeight = currentDesignHeight) {
            Box(
                Modifier
                    .figmaPosition(left = (-40).dp, bottom = (-31).dp, width = 130.dp, height = 147.dp)
                    .background(app_colors.profileYellow, RoundedCornerShape(topEnd = 110.dp))
            )
            ProfileCorner(app_colors.profileCorner)
            Image(
                painter = painterResource(R.drawable.report_home),
                contentDescription = "Inicio",
                modifier = Modifier
                    .figmaPosition(right = 22.dp, top = 25.dp, width = 49.dp, height = 49.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onHome)
            )
            BoxedText(
                text = "Reporta un problema",
                style = jakartaText(25.sp, 37.5.sp, app_colors.cfgTitle, FontWeight.ExtraBold, letterSpacing = (-0.62).sp),
                modifier = Modifier.figmaPosition(left = 33.dp, top = 40.dp, width = 247.dp, height = 38.dp)
            )
            Text(
                text = buildAnnotatedString {
                    append("¿Tienes algún problema con nuestra página?\n\n")
                    withStyle(SpanStyle(color = app_colors.profileYellow, fontSize = 24.sp)) { append("¡Sigue estos pasos!") }
                },
                style = jakartaText(17.sp, 25.5.sp, app_colors.cfgHeading, FontWeight.Bold),
                modifier = Modifier.figmaPosition(left = 16.dp, top = 132.dp)
            )

            when (step) {
                ReportStep.Hub -> HubStep(
                    identifiedUser = if (username.isNotBlank()) username else null,
                    onOpen = { step = it }
                )
                ReportStep.Identify -> IdentifyStep(
                    email = email,
                    onEmailChange = { email = it },
                    username = username,
                    onUsernameChange = { username = it },
                    onNext = {
                        // Sincronizar solicitudes con el usuario identificado
                        requestsList = loadStoredRequests(context, email, username)
                        step = ReportStep.Classify
                    }
                )
                ReportStep.Classify -> ClassifyStep(
                    selected = option,
                    onSelect = { option = it },
                    requests = requestsList,
                    selectedRequestId = selectedRequestId,
                    isRequestsDropdownExpanded = isRequestsDropdownExpanded,
                    onToggleRequestsDropdown = { isRequestsDropdownExpanded = !isRequestsDropdownExpanded },
                    onSelectRequest = { req ->
                        if (selectedRequestId == req.id) {
                            selectedRequestId = null
                            title = ""
                            description = ""
                            option = -1
                        } else {
                            selectedRequestId = req.id
                            title = req.title
                            description = req.description
                            val matchIndex = reportOptions.indexOfFirst { it.trim().equals(req.classification.trim(), ignoreCase = true) }
                            if (matchIndex >= 0) {
                                option = matchIndex
                            }
                        }
                    },
                    onClearSelection = {
                        selectedRequestId = null
                        title = ""
                        description = ""
                        option = -1
                    },
                    onBack = { step = ReportStep.Identify },
                    onNext = { if (option >= 0) step = ReportStep.Details }
                )
                ReportStep.Details -> DetailsStep(
                    title = title,
                    onTitleChange = { title = it },
                    description = description,
                    onDescriptionChange = { description = it },
                    onBack = { step = ReportStep.Classify },
                    onNext = { step = ReportStep.Evidence }
                )
                ReportStep.Evidence -> EvidenceStep(
                    question = null,
                    mediaUri = null,
                    mediaRes = null,
                    onBack = { step = ReportStep.Details },
                    onAddCamera = openCamera,
                    onAddGallery = chooseFromGallery,
                    onFinish = { dialog = ReportDialog.Sent }
                )
                ReportStep.PreviewImage -> EvidenceStep(
                    question = "¿Deseas usar esta imágen?",
                    mediaUri = mediaUri,
                    mediaRes = if (mediaUri == null) R.drawable.report_image_sample else null,
                    onBack = { step = ReportStep.Details },
                    onAddCamera = openCamera,
                    onAddGallery = chooseFromGallery,
                    onConfirm = { dialog = ReportDialog.ImageAttached },
                    onDiscard = { mediaUriString = null; step = ReportStep.Evidence },
                    onFinish = { dialog = ReportDialog.Sent }
                )
                ReportStep.PreviewVideo -> EvidenceStep(
                    question = "¿Deseas usar este video?",
                    mediaUri = mediaUri,
                    mediaRes = if (mediaUri == null) R.drawable.report_video_sample else null,
                    onBack = { step = ReportStep.Details },
                    onAddCamera = openCamera,
                    onAddGallery = chooseFromGallery,
                    onConfirm = { dialog = ReportDialog.VideoAttached },
                    onDiscard = { mediaUriString = null; step = ReportStep.Evidence },
                    onFinish = { dialog = ReportDialog.Sent }
                )
                ReportStep.FinalImage, ReportStep.FinalVideo -> FinalStep(
                    mediaUri = mediaUri,
                    video = step == ReportStep.FinalVideo,
                    onBack = { step = ReportStep.Evidence },
                    onFinish = { dialog = ReportDialog.Sent }
                )
            }
        }

        when (dialog) {
            ReportDialog.LeaveReport -> ProfileLeaveDialog(
                question = "¿Estás seguro que deseas salir?\n", warning = "Perderás todos tus cambios",
                action = "Salir", actionX = 164.dp, actionWidth = 39.dp,
                onBack = { dialog = ReportDialog.None },
                onAction = {
                    email = ""
                    username = ""
                    title = ""
                    description = ""
                    selectedRequestId = null
                    onExit()
                }
            )
            ReportDialog.LeaveImage, ReportDialog.LeaveVideo -> ProfileLeaveDialog(
                question = "¿Estás seguro que deseas salir?\n",
                warning = if (dialog == ReportDialog.LeaveImage) "No confirmaste la imágen elegiida" else "No confirmaste el video elegiido",
                action = "Salir", actionX = 164.dp, actionWidth = 39.dp,
                onBack = { dialog = ReportDialog.None },
                onAction = { dialog = ReportDialog.None; mediaUriString = null; step = ReportStep.Hub }
            )
            ReportDialog.ImageAttached, ReportDialog.VideoAttached -> AttachedDialog(
                video = dialog == ReportDialog.VideoAttached,
                onContinue = {
                    step = if (dialog == ReportDialog.VideoAttached) ReportStep.FinalVideo else ReportStep.FinalImage
                    dialog = ReportDialog.None
                }
            )
            ReportDialog.Sent -> SentDialog(onFinish = {
                val now = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
                val selectedCategory = reportOptions.getOrElse(option) { "Otro" }
                val newId = "USP-${(1000..9999).random()}"
                val newReq = ProblemRequest(
                    id = newId,
                    title = title.ifBlank { "Reporte de $selectedCategory" },
                    classification = selectedCategory,
                    description = description.ifBlank { "Sin descripción adicional" },
                    email = email.ifBlank { "usuario@utadeo.edu.co" },
                    username = username.ifBlank { "Usuario Tadeísta" },
                    date = now,
                    status = "En revisión",
                    evidenceName = if (mediaUriString != null) "Evidencia_adjunta.jpg" else null
                )
                val updated = listOf(newReq) + requestsList
                requestsList = updated
                saveStoredRequests(context, updated)

                dialog = ReportDialog.None
                option = -1
                title = ""
                description = ""
                mediaUriString = null
                selectedRequestId = newId
                step = ReportStep.Hub
            })
            ReportDialog.None -> Unit
        }
    }
}

private val sectionTitle = jakartaText(18.sp, 28.sp, Color(0xFF0F2548), FontWeight.Bold, letterSpacing = (-0.5).sp)

/** Fila de sección del reporte (101 dp de alto) con su título y, opcional, la flecha hacia abajo. */
@Composable
private fun BoxScope.SectionRow(top: Dp, title: String, chevron: Boolean = true, onClick: (() -> Unit)? = null) {
    ProfileRowCard(Modifier.figmaPosition(left = 11.dp, right = 7.dp, top = top, height = 101.dp), onClick = onClick) {
        Text(
            text = title,
            style = sectionTitle,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 13.dp)
        )
        if (chevron) {
            Image(
                painter = painterResource(R.drawable.ic_report_chevron_down),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-42).dp, y = 49.09.dp)
                    .size(17.83.dp, 9.83.dp)
            )
        }
    }
}

/** Tarjeta desplegada del paso actual (borde turquesa). */
@Composable
private fun BoxScope.StepCard(height: Dp, top: Dp = 336.dp, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .figmaPosition(left = 11.dp, right = 7.dp, top = top, height = height)
            .clip(shape)
            .background(app_colors.white)
            .border(1.dp, app_colors.profileCorner, shape),
        content = content
    )
}

/** "Terminos de uso" e ícono inferior: solo en el inicio y en el paso de identificación. */
@Composable
private fun BoxScope.TermsFooter() {
    ProfileRowCard(Modifier.figmaPosition(left = 9.dp, right = 9.dp, top = 717.dp, height = 69.dp)) {
        BoxedText(
            text = "Terminos de uso\nTodos los derechos reservados por U-Spot App\n 2026 ",
            style = jakartaText(14.5.sp, 21.75.sp, app_colors.cfgHeading, FontWeight.Bold),
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun BoxScope.HubStep(identifiedUser: String? = null, onOpen: (ReportStep) -> Unit) {
    Image(
        painter = painterResource(R.drawable.report_footer_icon),
        contentDescription = null,
        modifier = Modifier.figmaPosition(left = 187.dp, top = 791.dp, width = 36.dp, height = 36.dp)
    )
    val identifyTitle = if (!identifiedUser.isNullOrBlank()) {
        "Identifícate (✓ $identifiedUser)"
    } else {
        "Identifícate para poder ayudarte"
    }
    SectionRow(235.dp, identifyTitle) { onOpen(ReportStep.Identify) }
    SectionRow(336.dp, "Clasificación del problema") { onOpen(ReportStep.Classify) }
    SectionRow(437.dp, "Detalles del incidente") { onOpen(ReportStep.Details) }
    SectionRow(538.dp, "Evidencias o pruebas visuales") { onOpen(ReportStep.Evidence) }
    TermsFooter()
}

/** "Siguiente paso" / "Terminar reporte": botón azul de 272 x 52 con flecha opcional. */
@Composable
private fun BoxScope.ReportButton(text: String, left: Dp, top: Dp, arrow: Boolean, onClick: () -> Unit) {
    Box(Modifier.figmaPosition(left = left, top = top, width = 272.dp, height = 66.dp)) {
        ProfileDialogButton(
            text = text, color = app_colors.formBlue, textColor = app_colors.white,
            onClick = onClick, modifier = Modifier, width = 272.dp, height = 52.dp
        )
        if (arrow) {
            Image(
                painter = painterResource(R.drawable.ic_report_arrow),
                contentDescription = null,
                modifier = Modifier
                    .offset(242.dp, 18.dp)
                    .size(14.dp, 12.dp)
            )
        }
    }
}

@Composable
private fun BoxScope.BackChevron(left: Dp, top: Dp, onClick: () -> Unit) {
    Image(
        painter = painterResource(R.drawable.ic_report_back),
        contentDescription = "Paso anterior",
        modifier = Modifier
            .figmaPosition(left = left, top = top, width = 9.83.dp, height = 17.83.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
    )
}

/** Campo gris redondeado ("Input") con ícono, texto de ayuda y asterisco rojo opcional. */
@Composable
private fun ReportInput(
    modifier: Modifier,
    placeholder: String,
    value: String? = null,
    onValueChange: ((String) -> Unit)? = null,
    icon: @Composable BoxScope.() -> Unit,
    required: Boolean,
    isError: Boolean = false,
    radius: Dp = 9999.dp,
    singleLine: Boolean = true
) {
    var internalValue by rememberSaveable { mutableStateOf("") }
    val textValue = value ?: internalValue
    val handleValueChange = onValueChange ?: { internalValue = it }
    val text = figmaText(app_fonts.poppins, 14.sp, 21.sp, app_colors.loginPlaceholder)
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(app_colors.loginInput)
            .then(if (isError) Modifier.border(1.5.dp, app_colors.formError, shape) else Modifier)
    ) {
        icon()
        BasicTextField(
            value = textValue,
            onValueChange = handleValueChange,
            singleLine = singleLine,
            textStyle = text.copy(color = app_colors.loginTitle),
            cursorBrush = SolidColor(app_colors.formBlue),
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 48.dp, end = 40.dp, top = 12.dp),
            decorationBox = { inner ->
                Box {
                    if (textValue.isEmpty()) Text(placeholder, style = text)
                    inner()
                }
            }
        )
        if (required) {
            Text(
                text = "*",
                style = figmaText(app_fonts.poppins, 20.sp, 30.sp, app_colors.reportRequired),
                modifier = Modifier.offset(322.dp, 12.dp)
            )
        }
    }
}

@Composable
private fun BoxScope.IdentifyStep(
    email: String,
    onEmailChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var emailError by rememberSaveable { mutableStateOf(false) }
    var userError by rememberSaveable { mutableStateOf(false) }

    fun validateAndProceed() {
        val emailTrimmed = email.trim()
        val userTrimmed = username.trim()
        when {
            emailTrimmed.isEmpty() && userTrimmed.isEmpty() -> {
                errorMessage = "Es necesario introducir el correo electrónico y el usuario"
                emailError = true
                userError = true
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
            emailTrimmed.isEmpty() -> {
                errorMessage = "Es necesario introducir el campo del correo electrónico"
                emailError = true
                userError = false
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
            userTrimmed.isEmpty() -> {
                errorMessage = "Es necesario introducir el campo del usuario"
                emailError = false
                userError = true
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
            else -> {
                errorMessage = null
                emailError = false
                userError = false
                onNext()
            }
        }
    }

    Image(
        painter = painterResource(R.drawable.report_footer_icon),
        contentDescription = null,
        modifier = Modifier.figmaPosition(left = 187.dp, top = 791.dp, width = 36.dp, height = 36.dp)
    )
    SectionRow(235.dp, "Identifícate para poder ayudarte")
    StepCard(height = 368.dp) {
        Text(
            text = "Brindanos tus datos personales para una ayuda personalizada y al detalle",
            style = sectionTitle,
            modifier = Modifier.offset(25.dp, 28.dp).padding(end = 25.dp)
        )
        Text(
            text = "Reecuerda que U-Spot nunca te pedira contraseñas ",
            style = jakartaText(12.sp, 28.sp, app_colors.profileCorner, FontWeight.Bold, letterSpacing = (-0.5).sp),
            modifier = Modifier.offset(23.dp, 77.dp)
        )
        ReportInput(
            modifier = Modifier.offset(23.dp, 146.dp).size(348.dp, 45.dp),
            placeholder = "Correo electrónico",
            value = email,
            onValueChange = {
                onEmailChange(it)
                if (emailError && it.isNotBlank()) {
                    emailError = false
                    if (!userError) errorMessage = null
                }
            },
            required = true,
            isError = emailError,
            icon = {
                Image(
                    painter = painterResource(R.drawable.ic_report_mail),
                    contentDescription = null,
                    modifier = Modifier.offset(17.dp, 15.69.dp).size(16.67.dp, 27.61.dp)
                )
            }
        )
        ReportInput(
            modifier = Modifier.offset(23.dp, 213.dp).size(348.dp, 45.dp),
            placeholder = "Usuario",
            value = username,
            onValueChange = {
                onUsernameChange(it)
                if (userError && it.isNotBlank()) {
                    userError = false
                    if (!emailError) errorMessage = null
                }
            },
            required = true,
            isError = userError,
            icon = {
                Image(
                    painter = painterResource(R.drawable.ic_report_user),
                    contentDescription = null,
                    modifier = Modifier.offset(21.dp, 15.dp).size(14.dp, 16.dp)
                )
            }
        )
        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                style = jakartaText(12.5.sp, 18.sp, app_colors.formError, FontWeight.SemiBold),
                modifier = Modifier
                    .offset(25.dp, 272.dp)
                    .padding(end = 25.dp)
            )
        }
    }
    ReportButton("Siguiente paso", left = 75.dp, top = 618.dp, arrow = true, onClick = ::validateAndProceed)
    // En Figma las secciones siguientes quedan debajo, tapadas por "Terminos de uso"
    SectionRow(704.dp, "Clasificación del problema", chevron = false)
    SectionRow(805.dp, "Clasificación del problema ", chevron = false)
    TermsFooter()
}

/**
 * Tarjeta individual para mostrar una solicitud creada dentro de la lista desplegable.
 * Muestra todos los detalles seleccionados: ID, fecha, estado, clasificación, título, descripción, evidencia y usuario.
 */
@Composable
private fun RequestItemCard(
    req: ProblemRequest,
    isSelected: Boolean,
    isCompact: Boolean,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(14.dp)
    val cardBg = if (isSelected) app_colors.profileCorner.copy(alpha = 0.18f) else Color(0xFFF6F8FA)
    val borderColor = if (isSelected) app_colors.formBlue else Color(0x22000000)

    val (statusBg, statusTextColor) = when (req.status) {
        "Resuelto" -> Color(0xFFD1FAE5) to Color(0xFF047857)
        "En trámite" -> Color(0xFFDBEAFE) to Color(0xFF1D4ED8)
        else -> Color(0xFFFEF3C7) to Color(0xFFB45309)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(cardBg)
            .border(if (isSelected) 1.8.dp else 1.dp, borderColor, cardShape)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${req.id}",
                    style = jakartaText(14.sp, 18.sp, app_colors.cfgTitle, FontWeight.ExtraBold)
                )

                Spacer(Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusBg)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = req.status,
                        style = jakartaText(11.sp, 14.sp, statusTextColor, FontWeight.Bold)
                    )
                }

                Spacer(Modifier.weight(1f))

                Text(
                    text = req.date,
                    style = figmaText(app_fonts.poppins, 11.sp, 14.sp, app_colors.loginPlaceholder)
                )
            }

            Spacer(Modifier.height(7.dp))

            // Tag de clasificación seleccionada
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(app_colors.profileCorner.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "🏷️ Clasificación: ${req.classification}",
                    style = jakartaText(12.sp, 15.sp, app_colors.cfgTitle, FontWeight.Bold)
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = req.title,
                style = jakartaText(13.5.sp, 18.sp, app_colors.cfgTitle, FontWeight.Bold),
                maxLines = if (isCompact) 1 else 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!isCompact) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = req.description,
                    style = jakartaText(12.sp, 16.5.sp, Color(0xFF4A5568), FontWeight.Normal),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(7.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👤 ${req.username} (${req.email})",
                    style = figmaText(app_fonts.poppins, 10.5.sp, 14.sp, app_colors.loginPlaceholder),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (req.evidenceName != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "📎 ${req.evidenceName}",
                        style = jakartaText(10.5.sp, 14.sp, app_colors.formBlue, FontWeight.Bold)
                    )
                }
            }

            if (isSelected) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "✓ Solicitud seleccionada con sus detalles aplicados",
                    style = jakartaText(11.5.sp, 15.sp, app_colors.formBlue, FontWeight.Bold)
                )
            }
        }
    }
}

/**
 * Lista desplegable ubicada arriba de "Clasificación del problema" para ver las distintas solicitudes
 * que se han creado con sus detalles seleccionados.
 */
@Composable
private fun BoxScope.RequestsDropdown(
    top: Dp,
    requests: List<ProblemRequest>,
    selectedRequestId: String?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectRequest: (ProblemRequest) -> Unit,
    onClearSelection: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val chevronRotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "chevron")
    val selectedReq = remember(selectedRequestId, requests) {
        requests.find { it.id == selectedRequestId }
    }

    Box(
        modifier = Modifier
            .figmaPosition(left = 11.dp, right = 7.dp, top = top)
            .clip(shape)
            .background(app_colors.white)
            .border(1.5.dp, app_colors.profileCorner, shape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Fila de encabezado / gatillo del dropdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleExpand
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(app_colors.profileCorner.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.report_title_icon),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Solicitudes creadas",
                            style = jakartaText(16.5.sp, 22.sp, app_colors.cfgTitle, FontWeight.Bold, letterSpacing = (-0.3).sp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(app_colors.profileYellow)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${requests.size}",
                                style = jakartaText(11.sp, 14.sp, app_colors.formBlue, FontWeight.ExtraBold)
                            )
                        }
                    }

                    Text(
                        text = when {
                            isExpanded -> "Toca para ocultar las solicitudes registradas"
                            selectedReq != null -> "Seleccionada: #${selectedReq.id} • ${selectedReq.classification}"
                            else -> "Despliega para ver detalles de solicitudes anteriores"
                        },
                        style = jakartaText(
                            12.sp,
                            16.sp,
                            if (selectedReq != null) app_colors.formBlue else app_colors.loginPlaceholder,
                            if (selectedReq != null) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                Image(
                    painter = painterResource(R.drawable.ic_report_chevron_down),
                    contentDescription = if (isExpanded) "Ocultar" else "Desplegar",
                    modifier = Modifier
                        .size(18.dp, 10.dp)
                        .graphicsLayer { rotationZ = chevronRotation }
                )
            }

            // Vista previa de la solicitud seleccionada cuando la lista está colapsada
            if (!isExpanded && selectedReq != null) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = app_colors.profileCorner.copy(alpha = 0.4f), thickness = 1.dp)
                Spacer(Modifier.height(10.dp))
                RequestItemCard(
                    req = selectedReq,
                    isSelected = true,
                    isCompact = true,
                    onClick = onClearSelection
                )
            }

            // Lista desplegada de todas las solicitudes creadas con sus detalles
            if (isExpanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = app_colors.profileCorner.copy(alpha = 0.4f), thickness = 1.dp)
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Selecciona una solicitud para revisar sus detalles seleccionados:",
                    style = jakartaText(12.sp, 16.sp, app_colors.cfgHeading, FontWeight.SemiBold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (requests.isEmpty()) {
                        Text(
                            text = "No has creado solicitudes aún.",
                            style = jakartaText(13.sp, 18.sp, app_colors.loginPlaceholder),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        requests.forEach { req ->
                            RequestItemCard(
                                req = req,
                                isSelected = selectedRequestId == req.id,
                                isCompact = false,
                                onClick = { onSelectRequest(req) }
                            )
                        }
                    }
                }

                if (selectedRequestId != null) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(app_colors.loginInput)
                            .clickable(onClick = onClearSelection)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕ Limpiar selección y reportar nuevo problema",
                            style = jakartaText(12.sp, 16.sp, app_colors.formBlue, FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.ClassifyStep(
    selected: Int,
    onSelect: (Int) -> Unit,
    requests: List<ProblemRequest>,
    selectedRequestId: String?,
    isRequestsDropdownExpanded: Boolean,
    onToggleRequestsDropdown: () -> Unit,
    onSelectRequest: (ProblemRequest) -> Unit,
    onClearSelection: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val dropdownTop = 225.dp
    val dropdownEstimatedHeight = when {
        isRequestsDropdownExpanded -> 460.dp
        selectedRequestId != null -> 195.dp
        else -> 66.dp
    }
    val sectionTop = dropdownTop + dropdownEstimatedHeight + 14.dp
    val cardTop = sectionTop + 101.dp
    val buttonsTop = cardTop + 541.dp + 16.dp

    // 1. Lista desplegable ubicada arriba de "Clasificación del problema"
    RequestsDropdown(
        top = dropdownTop,
        requests = requests,
        selectedRequestId = selectedRequestId,
        isExpanded = isRequestsDropdownExpanded,
        onToggleExpand = onToggleRequestsDropdown,
        onSelectRequest = onSelectRequest,
        onClearSelection = onClearSelection
    )

    // 2. Fila de la sección "Clasificación del problema"
    SectionRow(top = sectionTop, title = "Clasificación del problema")

    // 3. Tarjeta desplegada con las opciones de Clasificación
    StepCard(height = 541.dp, top = cardTop) {
        val tops = listOf(16.dp, 116.dp, 216.dp, 326.dp)
        val lefts = listOf(10.dp, 10.dp, 12.dp, 10.dp)
        reportOptions.forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .offset(lefts[index], tops[index])
                    .size(370.dp, 85.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(app_colors.profileCorner)
                    .clickable { onSelect(index) }
            ) {
                Column(
                    modifier = Modifier
                        .offset(20.dp, if (selectedRequestId != null && index == selected) 16.dp else 27.dp)
                        .padding(end = 65.dp)
                ) {
                    Text(label, style = sectionTitle)
                    if (selectedRequestId != null && index == selected) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Detalle seleccionado de la solicitud #$selectedRequestId",
                            style = jakartaText(11.sp, 14.sp, app_colors.formBlue, FontWeight.Bold)
                        )
                    }
                }

                val radio = Modifier
                    .offset(if (index == 3) 298.dp else 300.dp, if (index == 2) 23.dp else 20.dp)
                    .size(42.dp)
                if (index == selected) {
                    Image(painterResource(R.drawable.ic_report_option_on), contentDescription = "Seleccionado", modifier = radio)
                } else {
                    Box(
                        radio
                            .background(app_colors.white, CircleShape)
                            .border(2.dp, app_colors.black, CircleShape)
                    )
                }
            }
        }
    }

    BackChevron(left = 36.09.dp, top = buttonsTop + 19.09.dp, onClick = onBack)
    ReportButton("Siguiente paso", left = 69.dp, top = buttonsTop, arrow = true, onClick = onNext)
}

@Composable
private fun BoxScope.DetailsStep(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    SectionRow(235.dp, "Detalles del incidente")
    StepCard(height = 540.dp) {
        Text("Título", style = sectionTitle, modifier = Modifier.offset(25.dp, 11.dp))
        ReportInput(
            modifier = Modifier.offset(23.dp, 61.dp).size(348.dp, 45.dp),
            placeholder = "Resumen del problema",
            value = title,
            onValueChange = onTitleChange,
            required = true,
            icon = {
                Image(
                    painter = painterResource(R.drawable.report_title_icon),
                    contentDescription = null,
                    modifier = Modifier.offset(15.dp, 10.dp).size(24.dp)
                )
            }
        )
        Text("Descripción detallada", style = sectionTitle, modifier = Modifier.offset(26.dp, 115.dp))
        Text(
            text = "Mín 0 - Máx 500",
            style = figmaText(app_fonts.poppins, 14.sp, 21.sp, app_colors.loginPlaceholder),
            modifier = Modifier.offset(26.dp, 153.dp)
        )
        ReportInput(
            modifier = Modifier.offset(24.dp, 182.dp).size(348.dp, 239.dp),
            placeholder = "Indiquenos la descipción detallada ",
            value = description,
            onValueChange = onDescriptionChange,
            required = true,
            radius = 10.dp,
            singleLine = false,
            icon = {
                Image(
                    painter = painterResource(R.drawable.report_desc_icon),
                    contentDescription = null,
                    alpha = 0.6f,
                    modifier = Modifier.offset(15.dp, 8.dp).size(25.dp)
                )
            }
        )
    }
    BackChevron(left = 37.09.dp, top = 815.09.dp, onClick = onBack)
    ReportButton("Siguiente paso", left = 72.dp, top = 800.dp, arrow = true, onClick = onNext)
}

/** Paso "Evidencias o pruebas visuales" (317:1111) y sus vistas previas (322:1278, 323:1535). */
@Composable
private fun BoxScope.EvidenceStep(
    question: String?,
    mediaUri: Uri?,
    @DrawableRes mediaRes: Int?,
    onBack: () -> Unit,
    onAddCamera: () -> Unit = {},
    onAddGallery: () -> Unit = {},
    onConfirm: () -> Unit = {},
    onDiscard: () -> Unit = {},
    onFinish: () -> Unit = {}
) {
    SectionRow(235.dp, "Evidencias o pruebas visuales")
    StepCard(height = 535.dp) {
        Text(
            text = question ?: "Adjunta una imágen o video aquí ",
            style = sectionTitle,
            modifier = Modifier.offset(25.dp, 28.dp)
        )
        if (mediaUri == null && mediaRes == null) {
            Image(
                painter = painterResource(R.drawable.report_media_placeholder),
                contentDescription = null,
                modifier = Modifier
                    .offset(23.dp, 81.dp)
                    .size(348.dp, 274.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, app_colors.black, RoundedCornerShape(10.dp))
            )
        } else {
            MediaBox(
                mediaUri = mediaUri,
                fallbackRes = mediaRes ?: R.drawable.report_image_sample,
                modifier = Modifier.offset(23.dp, 81.dp)
            )
            Image(
                painter = painterResource(R.drawable.report_confirm),
                contentDescription = "Usar",
                modifier = Modifier.offset(260.dp, 363.dp).size(55.dp).clickable(onClick = onConfirm)
            )
            Image(
                painter = painterResource(R.drawable.ic_report_trash),
                contentDescription = "Descartar",
                modifier = Modifier.offset(322.dp, 366.dp).size(49.dp, 56.dp).clickable(onClick = onDiscard)
            )
        }
        AttachButtons(onOpenCamera = onAddCamera, onOpenGallery = onAddGallery)
    }
    BackChevron(left = 40.09.dp, top = 816.09.dp, onClick = onBack)
    ReportButton("Terminar reporte", left = 75.dp, top = 795.dp, arrow = false, onClick = onFinish)
}

/** Botones para adjuntar evidencia: "Ganchito" (cámara) y "Play" (galería de fotos). */
@Composable
private fun BoxScope.AttachButtons(onOpenCamera: () -> Unit, onOpenGallery: () -> Unit) {
    Image(
        painter = painterResource(R.drawable.report_add_image),
        contentDescription = "Abrir cámara",
        modifier = Modifier
            .offset(25.dp, 367.dp)
            .size(50.dp)
            .clickable(onClick = onOpenCamera)
    )
    Image(
        painter = painterResource(R.drawable.report_add_video),
        contentDescription = "Abrir galería",
        modifier = Modifier
            .offset(79.dp, 365.dp)
            .size(52.dp)
            .clickable(onClick = onOpenGallery)
    )
}

/** Imagen o video elegido: 348 x 274, radio 10 y borde negro de 1. Muestra la foto real si hay URI. */
@Composable
private fun MediaBox(
    mediaUri: Uri? = null,
    @DrawableRes fallbackRes: Int = R.drawable.report_image_sample,
    modifier: Modifier
) {
    val context = LocalContext.current
    val bitmap = remember(mediaUri) {
        mediaUri?.let { uri ->
            runCatching {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val scale = 800f / maxOf(info.size.width, info.size.height)
                    if (scale < 1f) decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }.asImageBitmap()
            }.getOrNull()
        }
    }
    val shape = RoundedCornerShape(10.dp)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Evidencia",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(348.dp, 274.dp)
                .clip(shape)
                .border(1.dp, app_colors.black, shape)
        )
    } else {
        Image(
            painter = painterResource(fallbackRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(348.dp, 274.dp)
                .clip(shape)
                .border(1.dp, app_colors.black, shape)
        )
    }
}

/** Paso final con la evidencia confirmada (325:1864 imagen, 325:1978 video). */
@Composable
private fun BoxScope.FinalStep(
    mediaUri: Uri?,
    video: Boolean,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    SectionRow(235.dp, "Evidencias o pruebas visuales")
    StepCard(height = 535.dp) {
        // Debajo asoma el recuadro gris de la vista previa, como en Figma
        Image(
            painter = painterResource(R.drawable.report_media_placeholder),
            contentDescription = null,
            modifier = Modifier
                .offset(23.dp, 81.dp)
                .size(348.dp, 274.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, app_colors.black, RoundedCornerShape(10.dp))
        )
        MediaBox(
            mediaUri = mediaUri,
            fallbackRes = if (video) R.drawable.report_video_sample else R.drawable.report_image_sample,
            modifier = Modifier.offset(if (video) 23.dp else 24.dp, 32.dp)
        )
        Image(
            painter = painterResource(R.drawable.report_add_image),
            contentDescription = null,
            modifier = Modifier.offset(25.dp, 367.dp).size(50.dp)
        )
        Image(
            painter = painterResource(R.drawable.report_add_video),
            contentDescription = null,
            modifier = Modifier.offset(79.dp, 365.dp).size(52.dp)
        )
    }
    if (video) {
        Image(
            painter = painterResource(R.drawable.report_play),
            contentDescription = null,
            modifier = Modifier.figmaPosition(left = 174.dp, top = 554.dp, width = 74.dp, height = 74.dp)
        )
    }
    BackChevron(left = 40.09.dp, top = 816.09.dp, onClick = onBack)
    ReportButton("Terminar reporte", left = 75.dp, top = 795.dp, arrow = false, onClick = onFinish)
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

/** Figma: "Validacion de Imagen" (323:1534 imagen, 323:1592 video). */
@Composable
private fun AttachedDialog(video: Boolean, onContinue: () -> Unit) {
    ProfileOverlay(scrim = Color(0xB3000000)) {
        ProfileDialogCard(top = 198.dp, background = app_colors.dialogCard) {
            ProfileCardImage(R.drawable.shape_report_teal, (-96.85).dp, 435.23.dp, 178.44.dp, 179.93.dp)
            ProfileCardImage(R.drawable.shape_report_blue, 273.48.dp, (-98.05).dp, 155.35.dp, 179.44.dp)
            Image(
                painter = painterResource(R.drawable.report_success),
                contentDescription = null,
                modifier = Modifier.offset(80.dp, 115.dp).size(208.dp, 139.dp)
            )
            Image(
                painter = painterResource(R.drawable.ic_report_sparkles),
                contentDescription = null,
                modifier = Modifier.offset(20.dp, 253.dp).size(38.dp, 32.dp)
            )
            BoxedText(
                text = if (video) "¡Video adjuntado con éxito!" else "¡Imágen adjuntada con éxito!",
                style = figmaText(app_fonts.poppins, 18.sp, 20.sp, app_colors.black),
                modifier = Modifier.offset(55.dp, 231.dp).size(285.dp, 85.dp)
            )
            ProfileDialogButton(
                text = "Continuar", color = app_colors.formBlue, textColor = app_colors.white,
                onClick = onContinue, modifier = Modifier.offset(45.dp, 350.dp), width = 272.dp, height = 52.dp
            )
        }
    }
}

/** Figma: "Reporte Enviado" (334:78). */
@Composable
private fun SentDialog(onFinish: () -> Unit) {
    ProfileOverlay(scrim = Color(0xB3000000)) {
        ProfileDialogCard(top = 180.dp, background = app_colors.dialogCard) {
            ProfileCardImage(R.drawable.shape_sent_teal, (-96.85).dp, 435.23.dp, 178.44.dp, 179.93.dp)
            ProfileCardImage(R.drawable.shape_sent_blue, 273.48.dp, (-98.05).dp, 155.35.dp, 179.44.dp)
            BoxedText(
                text = "Reporte realizado con éxito",
                style = figmaText(app_fonts.poppins, 20.sp, 20.sp, app_colors.black),
                modifier = Modifier.offset(20.dp, 151.dp).size(356.dp, 85.dp)
            )
            Image(
                painter = painterResource(R.drawable.ic_sent_sparkles),
                contentDescription = null,
                modifier = Modifier.offset(23.dp, 176.dp).size(38.dp, 32.dp)
            )
            Image(
                painter = painterResource(R.drawable.report_success),
                contentDescription = null,
                modifier = Modifier.offset(79.dp, 210.dp).size(208.dp, 139.dp)
            )
            ProfileDialogButton(
                text = "Finalizar", color = app_colors.formBlue, textColor = app_colors.white,
                onClick = onFinish, modifier = Modifier.offset(48.dp, 358.dp), width = 272.dp, height = 52.dp
            )
        }
    }
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun ReportProblemPreview() {
    ReportProblemScreen()
}
