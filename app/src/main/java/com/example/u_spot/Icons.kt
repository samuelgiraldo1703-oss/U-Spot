package com.example.u_spot

import androidx.compose.material.icons.Icons as MaterialIcons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Catálogo de iconos accesibles para U-Spot, mapeados a Jetpack Compose Material Icons.
 */
object Icons {
    object Outlined {
        val Email: ImageVector = MaterialIcons.Outlined.Email
        val Lock: ImageVector = MaterialIcons.Outlined.Lock
        val Visibility: ImageVector = MaterialIcons.Outlined.Visibility
        val VisibilityOff: ImageVector = MaterialIcons.Outlined.VisibilityOff
    }

    object Default {
        val Person: ImageVector = MaterialIcons.Default.Person
        val Email: ImageVector = MaterialIcons.Default.Email
        val Lock: ImageVector = MaterialIcons.Default.Lock
        val Visibility: ImageVector = MaterialIcons.Default.Visibility
        val VisibilityOff: ImageVector = MaterialIcons.Default.VisibilityOff
    }
}
