package com.example.fitnessapp.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

@Composable
fun FitnessAppTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    val colors = if (darkTheme) darkColorScheme(
        primary = Color(0xFFD0BCFF), onPrimary = Color(0xFF21163A), background = Color(0xFF1C1B1F),
        surface = Color(0xFF25232A), surfaceVariant = Color(0xFF49454F), onSurface = Color(0xFFE6E1E5), onBackground = Color(0xFFE6E1E5),
        onSurfaceVariant = Color(0xFFCAC4D0), outlineVariant = Color(0xFF49454F), secondary = Color(0xFFD0BCFF)
    ) else lightColorScheme(
        primary = Color(0xFF111827), onPrimary = Color.White, background = Color(0xFFF8FAFC),
        surface = Color.White, surfaceVariant = Color(0xFFF1F5F9), onSurface = Color(0xFF111827), onBackground = Color(0xFF111827),
        onSurfaceVariant = Color(0xFF6B7280), outlineVariant = Color(0xFFE5E7EB), secondary = Color(0xFF2563EB)
    )
    val view = LocalView.current
    SideEffect { view.context.activity()?.let { activity ->
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    } }
    MaterialTheme(colorScheme = colors, typography = Typography, content = content)
}
