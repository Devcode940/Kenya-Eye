package com.kenyaeye.eclipse.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun EclipseTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Material You: derive the base palette from the system wallpaper, then pin
        // the signature cyan/magenta accents of the Eclipse wallpaper on top.
        dynamicDarkColorScheme(context).copy(
            primary = EclipseCyan,
            tertiary = EclipseMagenta,
            background = EclipseBackground,
            surface = EclipseSurface,
        )
    } else {
        darkColorScheme(
            primary = EclipseCyan,
            secondary = EclipseMagenta,
            background = EclipseBackground,
            surface = EclipseSurface,
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = EclipseTypography,
        content = content,
    )
}
