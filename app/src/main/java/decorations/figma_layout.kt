package decorations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Posiciona un elemento dentro de un [FigmaCanvas] con las mismas restricciones que tiene en
 * Figma (left / top / right / bottom en dp, igual que el "absolute" del diseño).
 * Si se dan left y right, el ancho se estira; si se dan top y bottom, el alto se estira.
 */
fun Modifier.figmaPosition(
    left: Dp? = null,
    top: Dp? = null,
    right: Dp? = null,
    bottom: Dp? = null,
    width: Dp? = null,
    height: Dp? = null
): Modifier = layout { measurable, constraints ->
    val parentWidth = constraints.maxWidth
    val parentHeight = constraints.maxHeight

    val w = width?.roundToPx()
        ?: if (left != null && right != null) parentWidth - left.roundToPx() - right.roundToPx() else null
    val h = height?.roundToPx()
        ?: if (top != null && bottom != null) parentHeight - top.roundToPx() - bottom.roundToPx() else null

    val placeable = measurable.measure(
        Constraints(
            minWidth = w ?: 0,
            maxWidth = w ?: Constraints.Infinity,
            minHeight = h ?: 0,
            maxHeight = h ?: Constraints.Infinity
        )
    )

    val x = left?.roundToPx() ?: right?.let { parentWidth - it.roundToPx() - placeable.width } ?: 0
    val y = top?.roundToPx() ?: bottom?.let { parentHeight - it.roundToPx() - placeable.height } ?: 0

    layout(parentWidth, parentHeight) { placeable.place(x, y) }
}

/**
 * Lienzo de una pantalla de Figma. Ocupa todo el ancho y como mínimo el alto del frame
 * ([designHeight]); si la pantalla del dispositivo es más baja (o aparece el teclado) se
 * puede desplazar verticalmente.
 */
@Composable
fun FigmaCanvas(
    designHeight: Dp,
    background: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(background)
    ) {
        val canvasHeight = if (maxHeight > designHeight) maxHeight else designHeight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(canvasHeight)
                    .clipToBounds(),
                content = content
            )
        }
    }
}

/**
 * Alto de la barra de estado. Los botones que en Figma están pegados al borde superior
 * se bajan lo justo para no quedar debajo de la hora / batería.
 */
@Composable
fun belowStatusBar(figmaTop: Dp): Dp {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    return if (statusBar > figmaTop) statusBar else figmaTop
}
