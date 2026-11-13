package io.github.dashkow.quietfriend.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Пока только светлая тема; тёмная — отдельной задачей
private val LightColors = lightColorScheme(
    primary = Mint,
    onPrimary = Forest,
    primaryContainer = MintSoft,
    onPrimaryContainer = Forest,
    secondary = MintDark,
    onSecondary = White,
    tertiaryContainer = Peach,
    onTertiaryContainer = Ink,
    background = Cream,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    onSurfaceVariant = InkMuted,
    outline = MintBorder,
)

@Composable
fun QuietFriendTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content,
    )
}
