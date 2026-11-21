package com.example.minierp.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import com.example.minierp.R

private val tajawal = FontFamily(
    Font(R.font.tajawal_regular, FontWeight.Normal),
    Font(R.font.tajawal_bold, FontWeight.Bold)
)

@Composable
fun MiniERPTheme(theme: String = "system", content: @Composable () -> Unit) {
    val dark = when (theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colors = if (dark) {
        darkColorScheme(
            primary = Color(0xFF6FC2AC),
            onPrimary = Color(0xFF0B1F19),
            primaryContainer = Color(0xFF1C3D34),
            onPrimaryContainer = Color(0xFFD6EDE5),
            secondary = Color(0xFFF0B361),
            onSecondary = Color(0xFF3A2405),
            secondaryContainer = Color(0xFF4A3514),
            onSecondaryContainer = Color(0xFFFBE9D0),
            background = Color(0xFF0E1412),
            onBackground = Color(0xFFE4ECE8),
            surface = Color(0xFF16201C),
            onSurface = Color(0xFFE4ECE8),
            surfaceVariant = Color(0xFF24322C),
            onSurfaceVariant = Color(0xFFB4C3BC),
            error = Color(0xFFFF8A80)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF2E7D6B),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD7EDE5),
            onPrimaryContainer = Color(0xFF1F5A4D),
            secondary = Color(0xFFE08A1E),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFFBE9D0),
            onSecondaryContainer = Color(0xFF7A4706),
            background = Color(0xFFF6F7F6),
            onBackground = Color(0xFF1E293B),
            surface = Color.White,
            onSurface = Color(0xFF1E293B),
            surfaceVariant = Color(0xFFEEF1EF),
            onSurfaceVariant = Color(0xFF52615B),
            error = Color(0xFFC62828)
        )
    }

    val base = Typography()
    val typed = base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = tajawal),
        displayMedium = base.displayMedium.copy(fontFamily = tajawal),
        displaySmall = base.displaySmall.copy(fontFamily = tajawal),
        headlineLarge = base.headlineLarge.copy(fontFamily = tajawal),
        headlineMedium = base.headlineMedium.copy(fontFamily = tajawal),
        headlineSmall = base.headlineSmall.copy(fontFamily = tajawal),
        titleLarge = base.titleLarge.copy(fontFamily = tajawal),
        titleMedium = base.titleMedium.copy(fontFamily = tajawal),
        titleSmall = base.titleSmall.copy(fontFamily = tajawal),
        bodyLarge = base.bodyLarge.copy(fontFamily = tajawal),
        bodyMedium = base.bodyMedium.copy(fontFamily = tajawal),
        bodySmall = base.bodySmall.copy(fontFamily = tajawal),
        labelLarge = base.labelLarge.copy(fontFamily = tajawal),
        labelMedium = base.labelMedium.copy(fontFamily = tajawal),
        labelSmall = base.labelSmall.copy(fontFamily = tajawal)
    )

    MaterialTheme(colorScheme = colors, typography = typed, content = content)
}
