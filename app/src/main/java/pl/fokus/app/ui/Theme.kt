package pl.fokus.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val FokusTypography = Typography()

private val LightColors = lightColorScheme(
    primary = Color(0xFF765548),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCE),
    onPrimaryContainer = Color(0xFF2C160E),
    secondary = Color(0xFF6C5D58),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4DED7),
    onSecondaryContainer = Color(0xFF261915),
    tertiary = Color(0xFF646033),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEAE5A8),
    onTertiaryContainer = Color(0xFF1E1D00),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF201A18),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF201A18),
    surfaceVariant = Color(0xFFF4DED7),
    onSurfaceVariant = Color(0xFF51443F),
    outline = Color(0xFF84736D),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB59D),
    onPrimary = Color(0xFF452A20),
    primaryContainer = Color(0xFF5D4035),
    onPrimaryContainer = Color(0xFFFFDBCE),
    secondary = Color(0xFFD7C2BA),
    onSecondary = Color(0xFF3B2D29),
    secondaryContainer = Color(0xFF53443F),
    onSecondaryContainer = Color(0xFFF4DED7),
    tertiary = Color(0xFFCDCA8F),
    onTertiary = Color(0xFF33320B),
    tertiaryContainer = Color(0xFF4B4A20),
    onTertiaryContainer = Color(0xFFEAE5A8),
    background = Color(0xFF191311),
    onBackground = Color(0xFFF0DFDB),
    surface = Color(0xFF191311),
    onSurface = Color(0xFFF0DFDB),
    surfaceVariant = Color(0xFF51443F),
    onSurfaceVariant = Color(0xFFD7C2BA),
    outline = Color(0xFF9E8C85),
    error = Color(0xFFFFB4AB),
)

@Composable
fun FokusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = FokusTypography,
        content = content,
    )
}
