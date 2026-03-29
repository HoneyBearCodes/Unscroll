package com.devsummit.scroll.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val UnscrollColorScheme = darkColorScheme(
    primary = Teal400,
    onPrimary = DeepNavy,
    primaryContainer = DeepTeal,
    onPrimaryContainer = Teal400,
    secondary = Amber400,
    onSecondary = DeepNavy,
    secondaryContainer = Color(0xFF3D2E10),
    onSecondaryContainer = Amber400,
    tertiary = StreakOrange,
    onTertiary = DeepNavy,
    error = LimitRed,
    onError = DeepNavy,
    errorContainer = Color(0xFF3D1515),
    onErrorContainer = LimitRed,
    background = DeepNavy,
    onBackground = OffWhite,
    surface = DarkSlate,
    onSurface = OffWhite,
    surfaceVariant = ElevatedSlate,
    onSurfaceVariant = MutedGray,
    outline = SubtleGray,
    outlineVariant = Color(0xFF2A3548)
)

private val UnscrollShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun UnscrollTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = UnscrollColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val context = view.context
            if (context is Activity) {
                val window = context.window
                window.statusBarColor = DeepNavy.toArgb()
                window.navigationBarColor = DeepNavy.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = UnscrollShapes,
        content = content
    )
}
