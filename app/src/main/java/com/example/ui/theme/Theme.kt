package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val SophisticatedDarkColorScheme = darkColorScheme(
    primary = PurpleLight,              // #D0BCFF (Light Purple Accent)
    onPrimary = Color(0xFF21005D),        // #21005D (Dark purple for text on primary elements)
    primaryContainer = PillBg,          // #4A4458 (Active navigation pill background)
    onPrimaryContainer = PillText,      // #E8DEF8 (Light text on active container)
    secondary = PurpleGrey80,           // #CCC2DC
    onSecondary = Color(0xFF332D41),
    secondaryContainer = CardBg,        // #49454F (Base card / non-active component)
    onSecondaryContainer = TextLight,   // #E6E1E5 (Text on container)
    tertiary = Pink80,
    onTertiary = Color(0xFF492532),
    background = DarkBg,                // #1C1B1F (Deep elegant gray-black)
    onBackground = TextLight,           // #E6E1E5 (High contrast ivory)
    surface = DarkBg,                   // #1C1B1F
    onSurface = TextLight,              // #E6E1E5
    surfaceVariant = CardBg,            // #49454F (Muted variant containers)
    onSurfaceVariant = TextSub,         // #CAC4D0 (Secondary content font)
    outline = TextMuted,                // #938F99 (Outline margins & dividers)
    outlineVariant = Color(0xFF49454F),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    surfaceContainer = NavBg            // #2B2930 (Bottom Navbar area)
)

// Fallback is also set to Sophisticated Dark to ensure consistent premium design
private val SophisticatedLightColorScheme = SophisticatedDarkColorScheme

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for the "Sophisticated Dark" design
    dynamicColor: Boolean = false, // Set to false to prevent device-level overrides and preserve our branding
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> SophisticatedDarkColorScheme
        else -> SophisticatedLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
