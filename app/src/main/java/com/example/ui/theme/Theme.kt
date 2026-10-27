package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkMedicalGreenPrimary,
    onPrimary = DarkMedicalGreenOnPrimary,
    primaryContainer = DarkMedicalGreenContainer,
    onPrimaryContainer = DarkMedicalGreenOnContainer,
    secondary = DarkMedicalTealSecondary,
    onSecondary = DarkMedicalTealOnSecondary,
    secondaryContainer = DarkMedicalTealContainer,
    onSecondaryContainer = DarkMedicalTealOnContainer,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
  )

private val LightColorScheme =
  lightColorScheme(
    primary = MedicalGreenPrimary,
    onPrimary = MedicalGreenOnPrimary,
    primaryContainer = MedicalGreenContainer,
    onPrimaryContainer = MedicalGreenOnContainer,
    secondary = MedicalTealSecondary,
    onSecondary = MedicalTealOnSecondary,
    secondaryContainer = MedicalTealContainer,
    onSecondaryContainer = MedicalTealOnContainer,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
  )

@Composable
fun ThermoPrintTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

