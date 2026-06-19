package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  lightColorScheme(
    primary = ProfessionalPrimary,
    secondary = ProfessionalSecondary,
    tertiary = ProfessionalSuccess,
    background = ProfessionalBackground,
    surface = ProfessionalCard,
    onPrimary = Color.White,
    onSecondary = ProfessionalText,
    onBackground = ProfessionalText,
    onSurface = ProfessionalText
  )

private val LightColorScheme = DarkColorScheme

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Use our professional light scholarly aesthetic with rich text highlights
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = LightColorScheme
  val context = LocalContext.current

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
