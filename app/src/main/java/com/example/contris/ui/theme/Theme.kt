package com.example.contris.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF9EEFFD),
    secondary = BrandSecondaryDark,
    onSecondary = Color(0xFF1C3438),
    secondaryContainer = Color(0xFF334B4F),
    onSecondaryContainer = Color(0xFFCDE7EC),
    tertiary = BrandTertiaryDark,
    onTertiary = Color(0xFF3B2948),
    tertiaryContainer = Color(0xFF523F5F),
    onTertiaryContainer = Color(0xFFEFDBFF),
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9EEFFD),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = BrandSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE7EC),
    onSecondaryContainer = Color(0xFF051F23),
    tertiary = BrandTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEFDBFF),
    onTertiaryContainer = Color(0xFF251431),
)

/** Semantic colours not covered by Material: quiz correct / incorrect. */
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
)

private val LightExtended = ExtendedColors(
    success = Color(0xFF2E7D32), onSuccess = Color.White,
    successContainer = Color(0xFFC8E6C9), onSuccessContainer = Color(0xFF0B2E12),
    danger = Color(0xFFBA1A1A), onDanger = Color.White,
    dangerContainer = Color(0xFFFFDAD6), onDangerContainer = Color(0xFF410002),
)

private val DarkExtended = ExtendedColors(
    success = Color(0xFF81C784), onSuccess = Color(0xFF00390F),
    successContainer = Color(0xFF1B5E20), onSuccessContainer = Color(0xFFC8E6C9),
    danger = Color(0xFFFFB4AB), onDanger = Color(0xFF690005),
    dangerContainer = Color(0xFF93000A), onDangerContainer = Color(0xFFFFDAD6),
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtended }

object ContrisThemeExtras {
    val colors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}

@Composable
fun ContrisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val extended = if (darkTheme) DarkExtended else LightExtended

    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
