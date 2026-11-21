package com.example.minierp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import com.example.minierp.R

private val tajawal = FontFamily(
    Font(R.font.tajawal_regular, FontWeight.Normal),
    Font(R.font.tajawal_bold, FontWeight.Bold)
)

@Composable
fun MiniERPTheme(content: @Composable () -> Unit) {
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

    MaterialTheme(
        colorScheme = lightColorScheme(
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
        ),
        typography = typed,
        content = content
    )
}
