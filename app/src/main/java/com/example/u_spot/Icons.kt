package com.example.u_spot

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

class Icons(
    imageVector: Any,
    contentDescription: Nothing?,
    modifier: Any,
    tint: Color
) {
    class Outlined(
        imageVector: ImageVector,
        contentDescription: Nothing?,
        modifier: Any,
        tint: Color
    ) {
        companion object {
            val Lock: ImageVector = TODO()
            val Email: ImageVector = TODO()
            val VisibilityOff: ImageVector = TODO()
            val Visibility: ImageVector = TODO()
        }

    }

    class Default(value: String, onValueChange: (String) -> Unit, placeholder: String, icon: Any) {
        companion object {
            val VisibilityOff: ImageVector = TODO()
            val Visibility: ImageVector
            val Lock: ImageVector
            val Email: ImageVector
            val Person: ImageVector
        }

    }

}
