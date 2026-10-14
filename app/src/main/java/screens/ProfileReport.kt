package screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.layout.ContentScale
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

/** Figma: los 19 frames "Reporte de Problemas", "Validacion de Imagen", "Cancelacion de Proceso" y "Reporte Enviado". */
@Composable
fun ReportProblemScreen(onExit: () -> Unit = {}) {
    var step by rememberSaveable { mutableStateOf(ReportStep.Hub) }
    var dialog by rememberSaveable { mutableStateOf(ReportDialog.None) }
    var option by rememberSaveable { mutableStateOf(-1) }

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
                ReportStep.Hub -> HubStep(onOpen = { step = it })
                ReportStep.Identify -> IdentifyStep(onNext = { step = ReportStep.Classify })
                ReportStep.Classify -> ClassifyStep(
                    selected = option,
                    onSelect = { option = it },
                    onBack = { step = ReportStep.Identify },
                    onNext = { if (option >= 0) step = ReportStep.Details }
                )
                ReportStep.Details -> DetailsStep(onBack = { step = ReportStep.Classify }, onNext = { step = ReportStep.Evidence })
                ReportStep.Evidence -> EvidenceStep(
                    question = null, media = null,
                    onBack = { step = ReportStep.Details },
                    onAddImage = { step = ReportStep.PreviewImage },
                    onAddVideo = { step = ReportStep.PreviewVideo }
                )
                ReportStep.PreviewImage -> EvidenceStep(
                    question = "¿Deseas usar esta imágen?", media = R.drawable.report_image_sample,
                    onBack = { step = ReportStep.Details },
                    onConfirm = { dialog = ReportDialog.ImageAttached },
                    onDiscard = { step = ReportStep.Evidence }
                )
                ReportStep.PreviewVideo -> EvidenceStep(
                    question = "¿Deseas usar este video?", media = R.drawable.report_video_sample,
                    onBack = { step = ReportStep.Details },
                    onConfirm = { dialog = ReportDialog.VideoAttached },
                    onDiscard = { step = ReportStep.Evidence }
                )
                ReportStep.FinalImage, ReportStep.FinalVideo -> FinalStep(
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
                onAction = onExit
            )
            ReportDialog.LeaveImage, ReportDialog.LeaveVideo -> ProfileLeaveDialog(
                question = "¿Estás seguro que deseas salir?\n",
                warning = if (dialog == ReportDialog.LeaveImage) "No confirmaste la imágen elegiida" else "No confirmaste el video elegiido",
                action = "Salir", actionX = 164.dp, actionWidth = 39.dp,
                onBack = { dialog = ReportDialog.None },
                onAction = { dialog = ReportDialog.None; step = ReportStep.Hub }
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
            text = "Terminos de uso\nTodos los derechos reservados por U-Spot App\n 2026 ",
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
    SectionRow(235.dp, "Identifícate para poder ayudarte") { onOpen(ReportStep.Identify) }
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
    icon: @Composable BoxScope.() -> Unit,
    required: Boolean,
    radius: Dp = 9999.dp,
    singleLine: Boolean = true
) {
    var value by rememberSaveable { mutableStateOf("") }
    val text = figmaText(app_fonts.poppins, 14.sp, 21.sp, app_colors.loginPlaceholder)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(app_colors.loginInput)
    ) {
        icon()
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = singleLine,
            textStyle = text.copy(color = app_colors.loginTitle),
            cursorBrush = SolidColor(app_colors.formBlue),
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 48.dp, end = 40.dp, top = 12.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, style = text)
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
private fun BoxScope.IdentifyStep(onNext: () -> Unit) {
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
            required = true,
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
            required = false,
            icon = {
                Image(
                    painter = painterResource(R.drawable.ic_report_user),
                    contentDescription = null,
                    modifier = Modifier.offset(21.dp, 15.dp).size(14.dp, 16.dp)
                )
            }
        )
    }
    ReportButton("Siguiente paso", left = 75.dp, top = 618.dp, arrow = true, onClick = onNext)
    // En Figma las secciones siguientes quedan debajo, tapadas por "Terminos de uso"
    SectionRow(704.dp, "Clasificación del problema", chevron = false)
    SectionRow(805.dp, "Clasificación del problema ", chevron = false)
    TermsFooter()
}

@Composable
private fun BoxScope.ClassifyStep(selected: Int, onSelect: (Int) -> Unit, onBack: () -> Unit, onNext: () -> Unit) {
    SectionRow(235.dp, "Clasificación del problema")
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
                    .clickable { onSelect(index) }
            ) {
                Text(label, style = sectionTitle, modifier = Modifier.offset(20.dp, 27.dp))
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
    BackChevron(left = 36.09.dp, top = 819.09.dp, onClick = onBack)
    ReportButton("Siguiente paso", left = 69.dp, top = 800.dp, arrow = true, onClick = onNext)
}

@Composable
private fun BoxScope.DetailsStep(onBack: () -> Unit, onNext: () -> Unit) {
    SectionRow(235.dp, "Detalles del incidente")
    StepCard(height = 540.dp) {
        Text("Título", style = sectionTitle, modifier = Modifier.offset(25.dp, 11.dp))
        ReportInput(
            modifier = Modifier.offset(23.dp, 61.dp).size(348.dp, 45.dp),
            placeholder = "Resumen del problema",
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
    @DrawableRes media: Int?,
    onBack: () -> Unit,
    onAddImage: () -> Unit = {},
    onAddVideo: () -> Unit = {},
    onConfirm: () -> Unit = {},
    onDiscard: () -> Unit = {}
) {
    SectionRow(235.dp, "Evidencias o pruebas visuales")
    StepCard(height = 535.dp) {
        Text(
            text = question ?: "Adjunta una imágen o video aquí ",
            style = sectionTitle,
            modifier = Modifier.offset(25.dp, 28.dp)
        )
        if (media == null) {
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
            MediaBox(media, Modifier.offset(23.dp, 81.dp))
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
        AttachButtons(onAddImage, onAddVideo)
    }
    BackChevron(left = 40.09.dp, top = 816.09.dp, onClick = onBack)
    ReportButton("Terminar reporte", left = 75.dp, top = 795.dp, arrow = false, onClick = {})
}

@Composable
private fun BoxScope.AttachButtons(onAddImage: () -> Unit, onAddVideo: () -> Unit) {
    Image(
        painter = painterResource(R.drawable.report_add_image),
        contentDescription = "Adjuntar imagen",
        modifier = Modifier.offset(25.dp, 367.dp).size(50.dp).clickable(onClick = onAddImage)
    )
    Image(
        painter = painterResource(R.drawable.report_add_video),
        contentDescription = "Adjuntar video",
        modifier = Modifier.offset(79.dp, 365.dp).size(52.dp).clickable(onClick = onAddVideo)
    )
}

/** Imagen o video elegido: 348 x 274, radio 10 y borde negro de 1. */
@Composable
private fun MediaBox(@DrawableRes media: Int, modifier: Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Image(
        painter = painterResource(media),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(348.dp, 274.dp)
            .clip(shape)
            .border(1.dp, app_colors.black, shape)
    )
}

/** Paso final con la evidencia confirmada (325:1864 imagen, 325:1978 video). */
@Composable
private fun BoxScope.FinalStep(video: Boolean, onBack: () -> Unit, onFinish: () -> Unit) {
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
            if (video) R.drawable.report_video_sample else R.drawable.report_image_sample,
            Modifier.offset(if (video) 23.dp else 24.dp, 32.dp)
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
