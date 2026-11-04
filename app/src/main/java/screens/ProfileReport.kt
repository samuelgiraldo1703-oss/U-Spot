package screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
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

private enum class ReportDialog {
    None,
    LeaveReport,
    LeaveImage,
    LeaveVideo,
    ImageAttached,
    VideoAttached,
    Sent,
    NoFieldSelected,
    NoMediaAttached
}

/** Figma: los 19 frames "Reporte de Problemas", "Validacion de Imagen", "Cancelacion de Proceso" y "Reporte Enviado". */
@Composable
fun ReportProblemScreen(onExit: () -> Unit = {}) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableStateOf(ReportStep.Hub) }
    var dialog by rememberSaveable { mutableStateOf(ReportDialog.None) }
    var option by rememberSaveable { mutableStateOf(-1) }
    var email by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
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
            Toast.makeText(context, context.getString(R.string.profile_photo_no_camera_app), Toast.LENGTH_SHORT).show()
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
        else Toast.makeText(context, context.getString(R.string.profile_photo_permission_toast), Toast.LENGTH_LONG).show()
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

    Box(Modifier.fillMaxSize()) {
        ProfileCanvas(designHeight = 916.dp) {
            Box(
                Modifier
                    .figmaPosition(left = (-40).dp, bottom = (-31).dp, width = 130.dp, height = 147.dp)
                    .background(app_colors.profileYellow, RoundedCornerShape(topEnd = 110.dp))
            )
            ProfileCorner(app_colors.profileCorner)
            Image(
                painter = painterResource(R.drawable.report_home),
                contentDescription = stringResource(R.string.nav_tab_home),
                modifier = Modifier
                    .figmaPosition(right = 22.dp, top = 25.dp, width = 49.dp, height = 49.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onHome)
            )
            BoxedText(
                text = stringResource(R.string.report_title),
                style = jakartaText(25.sp, 37.5.sp, app_colors.cfgTitle, FontWeight.ExtraBold, letterSpacing = (-0.62).sp),
                modifier = Modifier.figmaPosition(left = 33.dp, top = 40.dp, width = 247.dp, height = 38.dp)
            )
            val introQuestion = stringResource(R.string.report_intro_question)
            val introFollowSteps = stringResource(R.string.report_intro_follow_steps)
            Text(
                text = buildAnnotatedString {
                    append(introQuestion)
                    withStyle(SpanStyle(color = app_colors.profileYellow, fontSize = 24.sp)) { append(introFollowSteps) }
                },
                style = jakartaText(17.sp, 25.5.sp, app_colors.cfgHeading, FontWeight.Bold),
                modifier = Modifier.figmaPosition(left = 16.dp, top = 132.dp)
            )

            when (step) {
                ReportStep.Hub -> HubStep(onOpen = { step = it })
                ReportStep.Identify -> IdentifyStep(
                    email = email,
                    onEmailChange = { email = it },
                    username = username,
                    onUsernameChange = { username = it },
                    onNext = { step = ReportStep.Classify }
                )
                ReportStep.Classify -> ClassifyStep(
                    selected = option,
                    onSelect = { option = it },
                    onBack = { step = ReportStep.Identify },
                    onNext = { step = ReportStep.Details },
                    onValidationError = { dialog = ReportDialog.NoFieldSelected }
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
                    onFinish = {
                        if (mediaUri == null) {
                            dialog = ReportDialog.NoMediaAttached
                        } else {
                            dialog = ReportDialog.Sent
                        }
                    }
                )
                ReportStep.PreviewImage -> EvidenceStep(
                    question = stringResource(R.string.report_preview_use_image),
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
                    question = stringResource(R.string.report_preview_use_video),
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
                question = stringResource(R.string.report_leave_question),
                warning = stringResource(R.string.report_leave_warning_changes),
                action = stringResource(R.string.report_leave_action),
                actionX = 164.dp,
                actionWidth = 39.dp,
                onBack = { dialog = ReportDialog.None },
                onAction = {
                    email = ""
                    username = ""
                    title = ""
                    description = ""
                    option = -1
                    mediaUriString = null
                    onExit()
                }
            )
            ReportDialog.LeaveImage, ReportDialog.LeaveVideo -> ProfileLeaveDialog(
                question = stringResource(R.string.report_leave_question),
                warning = if (dialog == ReportDialog.LeaveImage) stringResource(R.string.report_leave_warning_image) else stringResource(R.string.report_leave_warning_video),
                action = stringResource(R.string.report_leave_action),
                actionX = 164.dp,
                actionWidth = 39.dp,
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
                dialog = ReportDialog.None
                option = -1
                email = ""
                username = ""
                title = ""
                description = ""
                mediaUriString = null
                step = ReportStep.Hub
            })
            ReportDialog.NoFieldSelected -> WarningDialog(
                message = stringResource(R.string.report_error_no_field_selected),
                onContinue = { dialog = ReportDialog.None }
            )
            ReportDialog.NoMediaAttached -> WarningDialog(
                message = stringResource(R.string.report_error_no_media_attached),
                onContinue = { dialog = ReportDialog.None }
            )
            ReportDialog.None -> Unit
        }
    }
}

private val sectionTitle = jakartaText(18.sp, 28.sp, app_colors.profileNavy, FontWeight.Bold, letterSpacing = (-0.5).sp)

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
private fun BoxScope.StepCard(height: Dp, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .figmaPosition(left = 11.dp, right = 7.dp, top = 336.dp, height = height)
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
            text = stringResource(R.string.report_terms_footer),
            style = jakartaText(14.5.sp, 21.75.sp, app_colors.cfgHeading, FontWeight.Bold),
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun BoxScope.HubStep(onOpen: (ReportStep) -> Unit) {
    Image(
        painter = painterResource(R.drawable.report_footer_icon),
        contentDescription = null,
        modifier = Modifier.figmaPosition(left = 187.dp, top = 791.dp, width = 36.dp, height = 36.dp)
    )
    SectionRow(235.dp, stringResource(R.string.report_section_identify)) { onOpen(ReportStep.Identify) }
    SectionRow(336.dp, stringResource(R.string.report_section_classify)) { onOpen(ReportStep.Classify) }
    SectionRow(437.dp, stringResource(R.string.report_section_details)) { onOpen(ReportStep.Details) }
    SectionRow(538.dp, stringResource(R.string.report_section_evidence)) { onOpen(ReportStep.Evidence) }
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
        contentDescription = stringResource(R.string.report_prev_step_cd),
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
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
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
            keyboardOptions = keyboardOptions,
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

internal fun isValidReportEmail(email: String): Boolean {
    val trimmed = email.trim()
    if (trimmed.isEmpty()) return false
    return runCatching {
        Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()
    }.getOrElse {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        emailRegex.matches(trimmed)
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
    var emailErrorRes by rememberSaveable { mutableStateOf<Int?>(null) }
    var userError by rememberSaveable { mutableStateOf(false) }

    fun validateAndProceed() {
        val emailTrimmed = email.trim()
        val userTrimmed = username.trim()
        val isEmailBlank = emailTrimmed.isEmpty()
        val isUserBlank = userTrimmed.isEmpty()
        val isEmailValid = isValidReportEmail(emailTrimmed)

        emailErrorRes = when {
            isEmailBlank -> R.string.report_identify_email_error
            !isEmailValid -> R.string.report_identify_email_invalid
            else -> null
        }
        userError = isUserBlank

        if (isEmailValid && !isUserBlank) {
            onEmailChange(emailTrimmed)
            onUsernameChange(userTrimmed)
            onNext()
        }
    }

    Image(
        painter = painterResource(R.drawable.report_footer_icon),
        contentDescription = null,
        modifier = Modifier.figmaPosition(left = 187.dp, top = 791.dp, width = 36.dp, height = 36.dp)
    )
    SectionRow(235.dp, stringResource(R.string.report_section_identify))
    StepCard(height = 368.dp) {
        Text(
            text = stringResource(R.string.report_identify_heading),
            style = sectionTitle,
            modifier = Modifier.offset(25.dp, 28.dp).padding(end = 25.dp)
        )
        Text(
            text = stringResource(R.string.report_identify_password_notice),
            style = jakartaText(12.sp, 28.sp, app_colors.profileCorner, FontWeight.Bold, letterSpacing = (-0.5).sp),
            modifier = Modifier.offset(23.dp, 77.dp)
        )
        ReportInput(
            modifier = Modifier.offset(23.dp, 132.dp).size(348.dp, 45.dp),
            placeholder = stringResource(R.string.report_identify_email_placeholder),
            value = email,
            onValueChange = {
                onEmailChange(it)
                if (emailErrorRes != null) {
                    emailErrorRes = when {
                        it.isBlank() -> R.string.report_identify_email_error
                        !isValidReportEmail(it) -> R.string.report_identify_email_invalid
                        else -> null
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            required = true,
            isError = emailErrorRes != null,
            icon = {
                Image(
                    painter = painterResource(R.drawable.ic_report_mail),
                    contentDescription = null,
                    modifier = Modifier.offset(17.dp, 15.69.dp).size(16.67.dp, 27.61.dp)
                )
            }
        )
        emailErrorRes?.let { errorRes ->
            Text(
                text = stringResource(errorRes),
                style = jakartaText(12.sp, 16.sp, app_colors.formError, FontWeight.SemiBold),
                modifier = Modifier.offset(25.dp, 180.dp)
            )
        }
        ReportInput(
            modifier = Modifier.offset(23.dp, if (emailErrorRes != null) 205.dp else 195.dp).size(348.dp, 45.dp),
            placeholder = stringResource(R.string.report_identify_user_placeholder),
            value = username,
            onValueChange = {
                onUsernameChange(it)
                if (userError && it.isNotBlank()) userError = false
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
        if (userError) {
            Text(
                text = stringResource(R.string.report_identify_user_error),
                style = jakartaText(12.sp, 16.sp, app_colors.formError, FontWeight.SemiBold),
                modifier = Modifier.offset(25.dp, if (emailErrorRes != null) 253.dp else 243.dp)
            )
        }
    }
    ReportButton(stringResource(R.string.report_next_step), left = 75.dp, top = 618.dp, arrow = true, onClick = ::validateAndProceed)
    // En Figma las secciones siguientes quedan debajo, tapadas por "Terminos de uso"
    SectionRow(704.dp, stringResource(R.string.report_section_classify), chevron = false)
    SectionRow(805.dp, stringResource(R.string.report_section_classify), chevron = false)
    TermsFooter()
}

@Composable
private fun BoxScope.ClassifyStep(
    selected: Int,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onValidationError: () -> Unit
) {
    var showError by rememberSaveable { mutableStateOf(false) }
    val reportOptions = listOf(
        stringResource(R.string.report_option_login_fail),
        stringResource(R.string.report_option_broken_link),
        stringResource(R.string.report_option_wrong_activity),
        stringResource(R.string.report_option_other)
    )

    fun validateAndProceed() {
        if (selected < 0) {
            showError = true
            onValidationError()
        } else {
            showError = false
            onNext()
        }
    }

    SectionRow(235.dp, stringResource(R.string.report_section_classify))
    StepCard(height = 541.dp) {
        val tops = listOf(16.dp, 116.dp, 216.dp, 326.dp)
        val lefts = listOf(10.dp, 10.dp, 12.dp, 10.dp)
        reportOptions.forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .offset(lefts[index], tops[index])
                    .size(370.dp, 85.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(app_colors.profileCorner)
                    .clickable {
                        onSelect(index)
                        showError = false
                    }
            ) {
                Text(label, style = sectionTitle, modifier = Modifier.offset(20.dp, 27.dp))
                val radio = Modifier
                    .offset(if (index == 3) 298.dp else 300.dp, if (index == 2) 23.dp else 20.dp)
                    .size(42.dp)
                if (index == selected) {
                    Image(painterResource(R.drawable.ic_report_option_on), contentDescription = stringResource(R.string.preferences_selected_cd), modifier = radio)
                } else {
                    Box(
                        radio
                            .background(app_colors.white, CircleShape)
                            .border(2.dp, app_colors.black, CircleShape)
                    )
                }
            }
        }
        if (showError && selected < 0) {
            Text(
                text = stringResource(R.string.report_classify_error),
                style = jakartaText(13.sp, 18.sp, app_colors.formError, FontWeight.SemiBold),
                modifier = Modifier.offset(20.dp, 435.dp)
            )
        }
    }
    BackChevron(left = 36.09.dp, top = 819.09.dp, onClick = onBack)
    ReportButton(stringResource(R.string.report_next_step), left = 69.dp, top = 800.dp, arrow = true, onClick = ::validateAndProceed)
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
    var titleError by rememberSaveable { mutableStateOf(false) }
    var descError by rememberSaveable { mutableStateOf(false) }

    fun validateAndProceed() {
        val isTitleEmpty = title.trim().isEmpty()
        val isDescEmpty = description.trim().isEmpty()
        titleError = isTitleEmpty
        descError = isDescEmpty
        if (!isTitleEmpty && !isDescEmpty) {
            onNext()
        }
    }

    SectionRow(235.dp, stringResource(R.string.report_section_details))
    StepCard(height = 540.dp) {
        Text(stringResource(R.string.report_details_title_label), style = sectionTitle, modifier = Modifier.offset(25.dp, 11.dp))
        ReportInput(
            modifier = Modifier.offset(23.dp, 44.dp).size(348.dp, 45.dp),
            placeholder = stringResource(R.string.report_details_title_placeholder),
            value = title,
            onValueChange = {
                onTitleChange(it)
                if (titleError && it.isNotBlank()) titleError = false
            },
            required = true,
            isError = titleError,
            icon = {
                Image(
                    painter = painterResource(R.drawable.report_title_icon),
                    contentDescription = null,
                    modifier = Modifier.offset(15.dp, 10.dp).size(24.dp)
                )
            }
        )
        if (titleError) {
            Text(
                text = stringResource(R.string.report_details_title_error),
                style = jakartaText(12.sp, 16.sp, app_colors.formError, FontWeight.SemiBold),
                modifier = Modifier.offset(25.dp, 93.dp)
            )
        }

        Text(
            text = stringResource(R.string.report_details_desc_label),
            style = sectionTitle,
            modifier = Modifier.offset(26.dp, if (titleError) 118.dp else 100.dp)
        )
        Text(
            text = stringResource(R.string.report_details_desc_range),
            style = figmaText(app_fonts.poppins, 14.sp, 21.sp, app_colors.loginPlaceholder),
            modifier = Modifier.offset(26.dp, if (titleError) 146.dp else 128.dp)
        )
        ReportInput(
            modifier = Modifier
                .offset(24.dp, if (titleError) 172.dp else 154.dp)
                .size(348.dp, 210.dp),
            placeholder = stringResource(R.string.report_details_desc_placeholder),
            value = description,
            onValueChange = {
                onDescriptionChange(it)
                if (descError && it.isNotBlank()) descError = false
            },
            required = true,
            isError = descError,
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
        if (descError) {
            Text(
                text = stringResource(R.string.report_details_desc_error),
                style = jakartaText(12.sp, 16.sp, app_colors.formError, FontWeight.SemiBold),
                modifier = Modifier.offset(25.dp, if (titleError) 386.dp else 368.dp)
            )
        }
    }
    BackChevron(left = 37.09.dp, top = 815.09.dp, onClick = onBack)
    ReportButton(stringResource(R.string.report_next_step), left = 72.dp, top = 800.dp, arrow = true, onClick = ::validateAndProceed)
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
    SectionRow(235.dp, stringResource(R.string.report_section_evidence))
    StepCard(height = 535.dp) {
        Text(
            text = question ?: stringResource(R.string.report_evidence_attach_prompt),
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
                contentDescription = stringResource(R.string.report_evidence_use_cd),
                modifier = Modifier.offset(260.dp, 363.dp).size(55.dp).clickable(onClick = onConfirm)
            )
            Image(
                painter = painterResource(R.drawable.ic_report_trash),
                contentDescription = stringResource(R.string.report_evidence_discard_cd),
                modifier = Modifier.offset(322.dp, 366.dp).size(49.dp, 56.dp).clickable(onClick = onDiscard)
            )
        }
        AttachButtons(onOpenCamera = onAddCamera, onOpenGallery = onAddGallery)
    }
    BackChevron(left = 40.09.dp, top = 816.09.dp, onClick = onBack)
    ReportButton(stringResource(R.string.report_finish_button), left = 75.dp, top = 795.dp, arrow = false, onClick = onFinish)
}

/** Botones para adjuntar evidencia: "Ganchito" (cámara) y "Play" (galería de fotos). */
@Composable
private fun BoxScope.AttachButtons(onOpenCamera: () -> Unit, onOpenGallery: () -> Unit) {
    Image(
        painter = painterResource(R.drawable.report_add_image),
        contentDescription = stringResource(R.string.report_evidence_open_camera_cd),
        modifier = Modifier
            .offset(25.dp, 367.dp)
            .size(50.dp)
            .clickable(onClick = onOpenCamera)
    )
    Image(
        painter = painterResource(R.drawable.report_add_video),
        contentDescription = stringResource(R.string.report_evidence_open_gallery_cd),
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
            contentDescription = stringResource(R.string.report_evidence_image_cd),
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
    SectionRow(235.dp, stringResource(R.string.report_section_evidence))
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
    ReportButton(stringResource(R.string.report_finish_button), left = 75.dp, top = 795.dp, arrow = false, onClick = onFinish)
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
    ProfileOverlay(scrim = app_colors.scrimDark70) {
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
                text = if (video) stringResource(R.string.report_attached_video_success) else stringResource(R.string.report_attached_image_success),
                style = figmaText(app_fonts.poppins, 18.sp, 20.sp, app_colors.black),
                modifier = Modifier.offset(55.dp, 231.dp).size(285.dp, 85.dp)
            )
            ProfileDialogButton(
                text = stringResource(R.string.common_continue), color = app_colors.formBlue, textColor = app_colors.white,
                onClick = onContinue, modifier = Modifier.offset(45.dp, 350.dp), width = 272.dp, height = 52.dp
            )
        }
    }
}

/** Figma: "Reporte Enviado" (334:78). */
@Composable
private fun SentDialog(onFinish: () -> Unit) {
    ProfileOverlay(scrim = app_colors.scrimDark70) {
        ProfileDialogCard(top = 180.dp, background = app_colors.dialogCard) {
            ProfileCardImage(R.drawable.shape_sent_teal, (-96.85).dp, 435.23.dp, 178.44.dp, 179.93.dp)
            ProfileCardImage(R.drawable.shape_sent_blue, 273.48.dp, (-98.05).dp, 155.35.dp, 179.44.dp)
            BoxedText(
                text = stringResource(R.string.report_sent_success),
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
                text = stringResource(R.string.report_sent_finish), color = app_colors.formBlue, textColor = app_colors.white,
                onClick = onFinish, modifier = Modifier.offset(48.dp, 358.dp), width = 272.dp, height = 52.dp
            )
        }
    }
}

/** Modales de advertencia ("Ningún campo seleccionado", "No se ha adjuntado ningún vídeo o imagen"). */
@Composable
private fun WarningDialog(message: String, onContinue: () -> Unit) {
    ProfileOverlay(scrim = app_colors.scrimDark70) {
        ProfileDialogCard(top = 198.dp, background = app_colors.dialogCard) {
            ProfileCardImage(R.drawable.shape_report_teal, (-96.85).dp, 435.23.dp, 178.44.dp, 179.93.dp)
            ProfileCardImage(R.drawable.shape_report_blue, 273.48.dp, (-98.05).dp, 155.35.dp, 179.44.dp)
            Image(
                painter = painterResource(R.drawable.ic_report_error_cross),
                contentDescription = null,
                modifier = Modifier
                    .offset(129.dp, 115.dp)
                    .size(105.dp)
            )
            Row(
                modifier = Modifier
                    .offset(20.dp, 248.dp)
                    .size(324.dp, 60.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_report_sparkles),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp, 30.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = message,
                    style = figmaText(app_fonts.poppins, 17.sp, 23.sp, app_colors.black, FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            }
            ProfileDialogButton(
                text = stringResource(R.string.common_continue),
                color = app_colors.formBlue,
                textColor = app_colors.white,
                onClick = onContinue,
                modifier = Modifier.offset(45.dp, 350.dp),
                width = 272.dp,
                height = 52.dp
            )
        }
    }
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun ReportProblemPreview() {
    ReportProblemScreen()
}
