package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
  primary = AccentViolet,
  secondary = AccentIndigo,
  tertiary = PlayActive,
  background = CosmicBg,
  surface = CosmicSurface,
  onPrimary = CosmicBg,
  onSecondary = TextSecondary,
  onTertiary = TextPrimary,
  onBackground = TextPrimary,
  onSurface = TextPrimary,
  surfaceVariant = CosmicSurfaceLighter,
  error = DeleteCrimson
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force Dark theme for high-end cinema immersion!
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      window.statusBarColor = colorScheme.background.toArgb()
      window.navigationBarColor = colorScheme.background.toArgb()
      val wic = WindowCompat.getInsetsController(window, view)
      // Status bar and nav bar controls in dark elements: light status icons
      wic.isAppearanceLightStatusBars = false
      wic.isAppearanceLightNavigationBars = false
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
