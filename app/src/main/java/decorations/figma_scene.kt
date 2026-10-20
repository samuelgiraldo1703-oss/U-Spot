package decorations

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/*
 * Escenas de Figma: descripción exacta de una pantalla del diseño (cajas, textos, sombras, recortes
 * e imágenes) generada desde el archivo de Figma con figma_export/gen_screens.js y guardada en
 * assets/figma/<clave>.json, con sus imágenes en assets/figma/img/. [FigmaScene] la dibuja tal cual;
 * las pantallas ponen encima lo interactivo (botones, textos con datos reales, comentarios...).
 *
 * Las coordenadas están en dp del frame de Figma (412 de ancho) y son absolutas respecto al frame.
 */

/** Sombra de Figma (DROP_SHADOW): color, desplazamiento, desenfoque y extensión en dp. */
class SceneShadow(val color: Color, val x: Float, val y: Float, val blur: Float, val spread: Float)

/** Borde de Figma. [sides] (arriba, derecha, abajo, izquierda) cuando cada lado tiene su grosor. */
class SceneStroke(val color: Color, val width: Float, val align: String, val sides: FloatArray?)

/** Tramo de texto con un mismo estilo. */
class SceneSpan(
    val text: String,
    val font: String?,
    val weight: Int,
    val italic: Boolean,
    val size: Float,
    val lineHeight: Float,
    val letterSpacing: Float,
    val color: Color,
    val underline: Boolean
)

sealed class SceneNode(
    val id: String,
    val name: String,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val opacity: Float
) {
    class Frame(
        id: String, name: String, x: Float, y: Float, w: Float, h: Float, opacity: Float,
        val fills: List<Color>,
        val radius: FloatArray?,
        val stroke: SceneStroke?,
        val shadows: List<SceneShadow>,
        val clip: Boolean,
        val children: List<SceneNode>
    ) : SceneNode(id, name, x, y, w, h, opacity)

    class Text(
        id: String, name: String, x: Float, y: Float, w: Float, h: Float, opacity: Float,
        val spans: List<SceneSpan>,
        val alignH: String,
        val alignV: String,
        val resize: String
    ) : SceneNode(id, name, x, y, w, h, opacity) {
        val plain: String get() = spans.joinToString("") { it.text }
    }

    class Picture(
        id: String, name: String, x: Float, y: Float, w: Float, h: Float, opacity: Float,
        val src: String,
        val tint: Color? = null
    ) : SceneNode(id, name, x, y, w, h, opacity)
}

/**
 * Copia del nodo movida [dx] / [dy] dp, con ancho [w] (los hijos quedan centrados en el nuevo
 * ancho) y con las imágenes cambiadas por [src] cuando devuelve algo. Sirve para reacomodar piezas
 * de Figma (p. ej. recuadros de rasgos más anchos cuando hay menos).
 */
fun SceneNode.copy(dx: Float = 0f, dy: Float = 0f, w: Float = this.w, src: (SceneNode.Picture) -> String? = { null }): SceneNode {
    val inner = (w - this.w) / 2f
    fun move(n: SceneNode, ox: Float, oy: Float): SceneNode = when (n) {
        is SceneNode.Frame -> SceneNode.Frame(
            n.id, n.name, n.x + ox, n.y + oy, n.w, n.h, n.opacity, n.fills, n.radius, n.stroke, n.shadows, n.clip,
            n.children.map { move(it, ox, oy) }
        )
        is SceneNode.Text -> SceneNode.Text(n.id, n.name, n.x + ox, n.y + oy, n.w, n.h, n.opacity, n.spans, n.alignH, n.alignV, n.resize)
        is SceneNode.Picture -> SceneNode.Picture(n.id, n.name, n.x + ox, n.y + oy, n.w, n.h, n.opacity, src(n) ?: n.src, n.tint)
    }
    return when (this) {
        is SceneNode.Frame -> SceneNode.Frame(
            id, name, x + dx, y + dy, w, h, opacity, fills, radius, stroke, shadows, clip,
            children.map { move(it, dx + inner, dy) }
        )
        else -> move(this, dx + inner, dy)
    }
}

/**
 * Imagen "recoloreada": `recolor|<src>|<claro>|<oscuro>|<claro nuevo>|<oscuro nuevo>` (colores
 * RRGGBB). Cada píxel se ubica entre los dos colores de origen según su luminosidad y se pinta en
 * el mismo punto entre los dos nuevos. Sirve para usar un icono de Figma con la paleta de otra
 * categoría cuando esa pantalla no lo trae.
 */
fun recolorSrc(src: String, light: Int, dark: Int, newLight: Int, newDark: Int): String =
    "recolor|$src|%06X|%06X|%06X|%06X".format(light and 0xFFFFFF, dark and 0xFFFFFF, newLight and 0xFFFFFF, newDark and 0xFFFFFF)

private fun recolor(bitmap: android.graphics.Bitmap, light: Int, dark: Int, newLight: Int, newDark: Int): android.graphics.Bitmap {
    fun lum(c: Int) = 0.299f * ((c shr 16) and 255) + 0.587f * ((c shr 8) and 255) + 0.114f * (c and 255)
    val l0 = lum(light); val l1 = lum(dark)
    val out = bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
    val px = IntArray(out.width * out.height)
    out.getPixels(px, 0, out.width, 0, 0, out.width, out.height)
    for (i in px.indices) {
        val c = px[i]
        val a = c ushr 24
        if (a == 0) continue
        val t = if (l0 == l1) 0f else ((l0 - lum(c)) / (l0 - l1)).coerceIn(0f, 1f)
        fun ch(shift: Int) = ((((newLight shr shift) and 255) * (1 - t)) + (((newDark shr shift) and 255) * t)).toInt()
        px[i] = (a shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }
    out.setPixels(px, 0, out.width, 0, 0, out.width, out.height)
    return out
}

class Scene(val key: String, val width: Float, val height: Float, val background: List<Color>, val nodes: List<SceneNode>) {
    /** Busca un nodo por su id de Figma (p. ej. "442:1720"). */
    fun find(id: String): SceneNode? = findIn(nodes, id)

    private fun findIn(list: List<SceneNode>, id: String): SceneNode? {
        for (n in list) {
            if (n.id == id) return n
            if (n is SceneNode.Frame) findIn(n.children, id)?.let { return it }
        }
        return null
    }
}

private fun parseColor(hex: String): Color = Color(hex.removePrefix("#").toLong(16).toInt())

private fun JSONObject.f(name: String, default: Float = 0f): Float = if (has(name)) getDouble(name).toFloat() else default

private fun parseNode(o: JSONObject): SceneNode {
    val id = o.optString("id")
    val name = o.optString("n")
    val x = o.f("x"); val y = o.f("y"); val w = o.f("w"); val h = o.f("h")
    val op = o.f("op", 1f)
    return when (o.getString("k")) {
        "text" -> {
            val segs = o.getJSONArray("segs")
            SceneNode.Text(
                id, name, x, y, w, h, op,
                spans = (0 until segs.length()).map { i ->
                    val s = segs.getJSONObject(i)
                    SceneSpan(
                        text = s.getString("s"),
                        font = s.optString("f").ifEmpty { null },
                        weight = s.optInt("wt", 400),
                        italic = s.optInt("it") == 1,
                        size = s.f("sz", 14f),
                        lineHeight = s.f("lh"),
                        letterSpacing = s.f("ls"),
                        color = parseColor(s.getString("c")),
                        underline = s.optInt("u") == 1
                    )
                },
                alignH = o.optString("ta", "LEFT"),
                alignV = o.optString("tv", "TOP"),
                resize = o.optString("rs", "NONE")
            )
        }
        // La imagen exportada de Figma ya trae la opacidad de la capa
        "img" -> SceneNode.Picture(id, name, x, y, w, h, 1f, o.getString("src"), o.optString("tint").ifEmpty { null }?.let { parseColor(it) })
        else -> {
            val radius = when (val r = o.opt("r")) {
                is JSONArray -> FloatArray(4) { r.getDouble(it).toFloat() }
                is Number -> FloatArray(4) { r.toFloat() }
                else -> null
            }
            val stroke = o.optJSONObject("s")?.let { s ->
                SceneStroke(
                    color = parseColor(s.getString("c")),
                    width = s.f("w"),
                    align = s.optString("a", "INSIDE"),
                    sides = s.optJSONArray("sides")?.let { a -> FloatArray(4) { a.getDouble(it).toFloat() } }
                )
            }
            val shadows = o.optJSONArray("sh")?.let { a ->
                (0 until a.length()).map {
                    val s = a.getJSONObject(it)
                    SceneShadow(parseColor(s.getString("c")), s.f("x"), s.f("y"), s.f("b"), s.f("sp"))
                }
            } ?: emptyList()
            val fills = o.optJSONArray("f")?.let { a -> (0 until a.length()).map { parseColor(a.getString(it)) } } ?: emptyList()
            val children = o.optJSONArray("ch")?.let { a -> (0 until a.length()).map { parseNode(a.getJSONObject(it)) } } ?: emptyList()
            SceneNode.Frame(id, name, x, y, w, h, op, fills, radius, stroke, shadows, o.optInt("clip") == 1, children)
        }
    }
}

/** Carga y guarda en memoria las escenas y sus imágenes (assets/figma). */
object SceneStore {
    private val scenes = HashMap<String, Scene>()
    private val images = object : LruCache<String, ImageBitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    fun cachedScene(key: String): Scene? = synchronized(scenes) { scenes[key] }

    fun cachedImage(src: String): ImageBitmap? = images.get(src)

    fun loadScene(context: Context, key: String): Scene {
        cachedScene(key)?.let { return it }
        val json = context.assets.open("figma/$key.json").bufferedReader().use { it.readText() }
        val o = JSONObject(json)
        val bg = o.optJSONArray("bg")?.let { a -> (0 until a.length()).map { parseColor(a.getString(it)) } } ?: emptyList()
        val els = o.getJSONArray("els")
        val scene = Scene(key, o.f("w", 412f), o.f("h", 917f), bg, (0 until els.length()).map { parseNode(els.getJSONObject(it)) })
        synchronized(scenes) { scenes[key] = scene }
        return scene
    }

    fun loadImage(context: Context, src: String): ImageBitmap? {
        images.get(src)?.let { return it }
        if (src.startsWith("recolor|")) {
            val p = src.split("|")
            val base = loadImage(context, p[1])?.asAndroidBitmap() ?: return null
            val c = p.drop(2).map { it.toInt(16) }
            return recolor(base, c[0], c[1], c[2], c[3]).asImageBitmap().also { images.put(src, it) }
        }
        val bitmap = runCatching {
            context.assets.open("figma/img/$src").use { BitmapFactory.decodeStream(it) }
        }.getOrNull() ?: return null
        return bitmap.asImageBitmap().also { images.put(src, it) }
    }

    /** Carga por adelantado todas las imágenes de una escena (para que no aparezcan de a poco). */
    fun preload(context: Context, scene: Scene) {
        fun visit(list: List<SceneNode>) {
            for (n in list) {
                when (n) {
                    is SceneNode.Picture -> loadImage(context, n.src)
                    is SceneNode.Frame -> visit(n.children)
                    is SceneNode.Text -> Unit
                }
            }
        }
        visit(scene.nodes)
    }
}

/** Escena lista para dibujar (con sus imágenes cargadas), o null mientras se lee de assets. */
@Composable
fun rememberScene(key: String): Scene? {
    val context = LocalContext.current.applicationContext
    val scene by produceState(initialValue = SceneStore.cachedScene(key), key) {
        value = withContext(Dispatchers.IO) {
            SceneStore.loadScene(context, key).also { SceneStore.preload(context, it) }
        }
    }
    return scene
}

/**
 * Lienzo escalado al ancho del dispositivo: dentro de él, 1 dp equivale a 1 px del frame de Figma
 * (412 de ancho), así la pantalla se ve igual en teléfonos más angostos o más anchos.
 */
@Composable
fun FigmaWidthScale(designWidth: Float = 412f, content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val outer = LocalDensity.current
        val factor = maxWidth.value / designWidth
        CompositionLocalProvider(LocalDensity provides Density(outer.density * factor, outer.fontScale)) {
            content()
        }
    }
}

/**
 * Dibuja [scene] en una caja de su tamaño (dp de Figma). [onClick] hace tocables nodos por id;
 * [hide] oculta nodos que la pantalla reemplaza con contenido propio (p. ej. el nombre del usuario);
 * [texts] cambia el contenido de textos (con su mismo estilo) y [images] cambia imágenes por otras
 * de assets/figma/img (recortadas para llenar el nodo), p. ej. con los datos reales de un lugar.
 */
@Composable
fun FigmaScene(
    scene: Scene,
    modifier: Modifier = Modifier,
    onClick: Map<String, () -> Unit> = emptyMap(),
    hide: Set<String> = emptySet(),
    texts: Map<String, String> = emptyMap(),
    images: Map<String, String> = emptyMap(),
    content: @Composable BoxScope.() -> Unit = {}
) {
    val fills = scene.background
    Box(
        modifier
            .size(scene.width.dp, scene.height.dp)
            .drawBehind { fills.forEach { drawRect(it) } }
    ) {
        val ctx = SceneContext(scene, onClick, hide, texts, images)
        for (n in scene.nodes) SceneNodeView(n, 0f, 0f, ctx)
        content()
    }
}

/**
 * Dibuja un solo nodo de [scene] (con sus hijos) desplazado [dx] / [dy] dp de su posición en
 * Figma. Sirve para llevar una parte de una pantalla a otra (p. ej. la fila "Ver todos").
 */
@Composable
fun FigmaSceneNode(
    scene: Scene,
    node: SceneNode,
    dx: Float = 0f,
    dy: Float = 0f,
    onClick: Map<String, () -> Unit> = emptyMap(),
    hide: Set<String> = emptySet(),
    texts: Map<String, String> = emptyMap()
) {
    SceneNodeView(node, -dx, -dy, SceneContext(scene, onClick, hide, texts, emptyMap()))
}

private class SceneContext(
    val scene: Scene,
    val onClick: Map<String, () -> Unit>,
    val hide: Set<String>,
    val texts: Map<String, String>,
    val images: Map<String, String>
)

private fun shapeOf(radius: FloatArray?): Shape =
    if (radius == null || radius.all { it == 0f }) RectangleShape
    else RoundedCornerShape(radius[0].dp, radius[1].dp, radius[2].dp, radius[3].dp)

@Composable
private fun SceneNodeView(node: SceneNode, parentX: Float, parentY: Float, ctx: SceneContext) {
    if (node.id in ctx.hide) return
    val click = ctx.onClick[node.id]
    var mod = Modifier.placeAt((node.x - parentX).dp, (node.y - parentY).dp, node.w.dp, node.h.dp)
    if (node.opacity < 1f) mod = mod.alpha(node.opacity)
    when (node) {
        is SceneNode.Frame -> FrameView(node, mod, click, ctx)
        is SceneNode.Text -> TextView(node, mod.then(clickMod(click)), ctx.texts[node.id])
        is SceneNode.Picture -> {
            val replacement = ctx.images[node.id]
            if (replacement != null) SceneImage(replacement, mod.then(clickMod(click)), crop = true)
            else PictureView(node, mod.then(clickMod(click)))
        }
    }
}

@Composable
private fun clickMod(click: (() -> Unit)?): Modifier =
    if (click == null) Modifier
    else Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = click)

@Composable
private fun FrameView(node: SceneNode.Frame, base: Modifier, click: (() -> Unit)?, ctx: SceneContext) {
    // El fondo que ocupa todo el frame es el marco del teléfono en Figma: sin esquinas ni sombra
    val fullScreen = node.x <= 0.5f && node.y <= 0.5f && node.w >= ctx.scene.width - 1f && node.h >= ctx.scene.height - 1f
    val radius = if (fullScreen) null else node.radius
    val shape = shapeOf(radius)
    var mod = base
    if (!fullScreen) {
        for (s in node.shadows) {
            mod = mod.dropShadow(shape, Shadow(radius = s.blur.dp, spread = s.spread.dp, color = s.color, offset = DpOffset(s.x.dp, s.y.dp)))
        }
    }
    for (f in node.fills) mod = mod.background(f, shape)
    node.stroke?.let { stroke -> mod = mod.drawBehind { drawFigmaStroke(stroke, radius) } }
    if (node.clip) mod = mod.clip(shape)
    mod = mod.then(clickMod(click))
    Box(mod) {
        for (c in node.children) SceneNodeView(c, node.x, node.y, ctx)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFigmaStroke(stroke: SceneStroke, radius: FloatArray?) {
    val sides = stroke.sides
    if (sides != null) {
        val (t, r, b, l) = sides.map { it.dp.toPx() }
        if (t > 0) drawRect(stroke.color, Offset.Zero, Size(size.width, t))
        if (b > 0) drawRect(stroke.color, Offset(0f, size.height - b), Size(size.width, b))
        if (l > 0) drawRect(stroke.color, Offset.Zero, Size(l, size.height))
        if (r > 0) drawRect(stroke.color, Offset(size.width - r, 0f), Size(r, size.height))
        return
    }
    val w = stroke.width.dp.toPx()
    // Desplazamiento del centro del trazo respecto al borde de la caja
    val inset = when (stroke.align) {
        "INSIDE" -> w / 2
        "OUTSIDE" -> -w / 2
        else -> 0f
    }
    val rr = radius ?: FloatArray(4)
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = inset, top = inset, right = size.width - inset, bottom = size.height - inset,
                topLeftCornerRadius = CornerRadius((rr[0].dp.toPx() - inset).coerceAtLeast(0f)),
                topRightCornerRadius = CornerRadius((rr[1].dp.toPx() - inset).coerceAtLeast(0f)),
                bottomRightCornerRadius = CornerRadius((rr[2].dp.toPx() - inset).coerceAtLeast(0f)),
                bottomLeftCornerRadius = CornerRadius((rr[3].dp.toPx() - inset).coerceAtLeast(0f))
            )
        )
    }
    drawPath(path, stroke.color, style = Stroke(width = w))
}

private fun familyOf(name: String?): FontFamily = when (name) {
    "poppins" -> app_fonts.poppins
    "inter" -> app_fonts.inter
    // "Liberation Sans" (métricas de Arial): la fuente del sistema tiene un ancho parecido
    "sans" -> FontFamily.SansSerif
    else -> app_fonts.plusJakarta
}

/** Texto de una escena con los estilos de cada tramo (AnnotatedString). */
fun SceneNode.Text.annotated(): AnnotatedString = buildAnnotatedString {
    for (s in spans) {
        withStyle(spanStyleOf(s)) { append(s.text) }
    }
}

fun spanStyleOf(s: SceneSpan) = SpanStyle(
    color = s.color,
    fontFamily = familyOf(s.font),
    fontWeight = FontWeight(s.weight),
    fontStyle = if (s.italic) FontStyle.Italic else FontStyle.Normal,
    fontSize = s.size.sp,
    letterSpacing = s.letterSpacing.sp,
    textDecoration = if (s.underline) TextDecoration.Underline else null
)

/** Estilo base del texto (el del primer tramo) con el interlineado centrado como en Figma. */
fun SceneNode.Text.baseStyle(): TextStyle {
    val first = spans.first()
    val lineHeight: TextUnit = spans.maxOf { it.lineHeight }.let { if (it > 0f) it.sp else TextUnit.Unspecified }
    return TextStyle(
        fontFamily = familyOf(first.font),
        fontWeight = FontWeight(first.weight),
        fontSize = first.size.sp,
        lineHeight = lineHeight,
        letterSpacing = first.letterSpacing.sp,
        color = first.color,
        textAlign = when (alignH) {
            "CENTER" -> TextAlign.Center
            "RIGHT" -> TextAlign.End
            "JUSTIFIED" -> TextAlign.Justify
            else -> TextAlign.Start
        },
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
    )
}

/** Alineación de Figma (horizontal / vertical) como Alignment de Compose. */
fun SceneNode.Text.alignment(): Alignment {
    val h = when (alignH) { "CENTER" -> Alignment.CenterHorizontally; "RIGHT" -> Alignment.End; else -> Alignment.Start }
    // Los textos de alto automático miden exactamente su contenido: centrarlos evita el desfase
    // de Compose con interlineados menores que la altura natural de la fuente.
    val v = if (resize == "WIDTH_AND_HEIGHT" || resize == "HEIGHT") Alignment.CenterVertically
    else when (alignV) { "CENTER" -> Alignment.CenterVertically; "BOTTOM" -> Alignment.Bottom; else -> Alignment.Top }
    return BiasAlignmentOf(h, v)
}

private fun BiasAlignmentOf(h: Alignment.Horizontal, v: Alignment.Vertical): Alignment {
    val hb = when (h) { Alignment.CenterHorizontally -> 0f; Alignment.End -> 1f; else -> -1f }
    val vb = when (v) { Alignment.CenterVertically -> 0f; Alignment.Bottom -> 1f; else -> -1f }
    return androidx.compose.ui.BiasAlignment(hb, vb)
}

@Composable
private fun TextView(node: SceneNode.Text, mod: Modifier, replacement: String? = null) {
    val align = node.alignment()
    val noWrap = node.resize == "WIDTH_AND_HEIGHT"
    Box(mod, contentAlignment = align) {
        Text(
            text = if (replacement != null) AnnotatedString(replacement, spanStyleOf(node.spans.first())) else node.annotated(),
            style = node.baseStyle(),
            softWrap = !noWrap,
            modifier = if (noWrap) Modifier.wrapContentSize(align, unbounded = true)
            else Modifier.fillMaxWidth().wrapContentSize(align, unbounded = true)
        )
    }
}

@Composable
private fun PictureView(node: SceneNode.Picture, mod: Modifier) {
    SceneImage(node.src, mod, node.tint)
}

/**
 * Imagen exportada de Figma (assets/figma/img/[src]) estirada al tamaño de [modifier]; con [crop]
 * se recorta para llenarlo sin deformarse (fotos que no se exportaron a la medida del nodo).
 */
@Composable
fun SceneImage(src: String, modifier: Modifier, tint: Color? = null, crop: Boolean = false) {
    val context = LocalContext.current.applicationContext
    val bitmap by produceState(SceneStore.cachedImage(src), src) {
        if (value == null) value = withContext(Dispatchers.IO) { SceneStore.loadImage(context, src) }
    }
    val b = bitmap ?: return
    Image(
        bitmap = b,
        contentDescription = null,
        contentScale = if (crop) ContentScale.Crop else ContentScale.FillBounds,
        colorFilter = tint?.let { ColorFilter.tint(it) },
        modifier = modifier
    )
}

/**
 * Coloca el elemento en ([x], [y]) con tamaño fijo [w] x [h] dentro de su caja padre, aunque se
 * salga de ella (como en Figma). No ocupa espacio en el padre: solo se dibuja ahí.
 */
fun Modifier.placeAt(x: Dp, y: Dp, w: Dp, h: Dp): Modifier = layout { measurable, _ ->
    val placeable = measurable.measure(Constraints.fixed(w.roundToPx().coerceAtLeast(0), h.roundToPx().coerceAtLeast(0)))
    layout(0, 0) { placeable.place(x.roundToPx(), y.roundToPx()) }
}

/** Posición de un nodo de la escena como modificador (para poner contenido propio encima). */
fun Modifier.sceneBounds(node: SceneNode, dx: Dp = 0.dp, dy: Dp = 0.dp): Modifier =
    placeAt(node.x.dp + dx, node.y.dp + dy, node.w.dp, node.h.dp)
