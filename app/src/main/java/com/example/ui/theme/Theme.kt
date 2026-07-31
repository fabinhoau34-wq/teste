package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PhotoshopColorScheme = darkColorScheme(
    primary = OrangePrimary,
    onPrimary = Color.White,
    primaryContainer = OrangeContainer,
    onPrimaryContainer = OnOrangeContainer,
    secondary = OrangeSecondary,
    onSecondary = Color.Black,
    tertiary = WhiteReceiptBg,
    onTertiary = BlackReceiptText,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder
)

@Composable
fun VistoriaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PhotoshopColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    VistoriaTheme(content = content)
}

