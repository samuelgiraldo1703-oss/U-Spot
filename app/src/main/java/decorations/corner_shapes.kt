package decorations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object corner_shapes {
    @Composable
    fun DrawCornerShapes() {
        Box(
            modifier = Modifier
                .size(130.dp)
                .offset(x = 155.dp, y = (-35).dp)
                .background(
                    color = app_colors.cyan,
                    shape = RoundedCornerShape(40.dp)
                )
        )

        Box(
            modifier = Modifier
                .size(100.dp)
                .offset(x = (-15).dp, y = 460.dp)
                .background(
                    color = app_colors.yellow,
                    shape = RoundedCornerShape(35.dp)
                )
        )
    }
}