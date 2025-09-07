

// ui/theme/Theme.kt
package com.dksport.earnez.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun EarnezTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        typography = Typography(),
        content = content
    )
}
