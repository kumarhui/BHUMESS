package cvam.dignity.bhumess.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(

    primary = Primary,
    onPrimary = OnPrimary,

    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),

    secondary = Secondary,
    onSecondary = OnSecondary,

    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF2E1065),

    tertiary = Tertiary,
    onTertiary = OnTertiary,

    tertiaryContainer = Color(0xFFCCFBF1),
    onTertiaryContainer = Color(0xFF134E4A),

    background = Background,
    onBackground = TextPrimary,

    surface = Surface,
    onSurface = TextPrimary,

    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,

    outline = Outline,
    outlineVariant = OutlineVariant,

    error = Error,
    onError = Color.White,

    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

@Composable
fun BHUMESSTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography(),
        content = content
    )
}