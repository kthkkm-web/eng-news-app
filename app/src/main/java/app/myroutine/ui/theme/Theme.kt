package app.myroutine.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.myroutine.data.settings.ThemeMode

/** 앱 고유 색 토큰 (Material 색 체계 외 추가분). */
@Immutable
data class AppColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,        // 카드
    val surfaceHigh: Color,    // 칩, 입력창
    val outline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,         // 선택 날짜, 탭 선택, 토글 ON
    val onAccent: Color,
    val danger: Color,
    val heatEmpty: Color,
)

val DarkAppColors = AppColors(
    isDark = true,
    background = Color(0xFF101214),
    surface = Color(0xFF1B1E22),
    surfaceHigh = Color(0xFF262A2F),
    outline = Color(0xFF33383E),
    textPrimary = Color(0xFFF2F4F5),
    textSecondary = Color(0xFFA3A9B0),
    textTertiary = Color(0xFF6B7178),
    accent = Color(0xFF3DDC97),
    onAccent = Color(0xFF06231A),
    danger = Color(0xFFFF6B6B),
    heatEmpty = Color(0xFF23272C),
)

val LightAppColors = AppColors(
    isDark = false,
    background = Color(0xFFF4F5F7),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFEBEDF0),
    outline = Color(0xFFD9DCE1),
    textPrimary = Color(0xFF15181B),
    textSecondary = Color(0xFF5C636B),
    textTertiary = Color(0xFF9AA0A7),
    accent = Color(0xFF13B57A),
    onAccent = Color(0xFFFFFFFF),
    danger = Color(0xFFE5484D),
    heatEmpty = Color(0xFFE6E8EB),
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

object AppTheme {
    val colors: AppColors @Composable get() = LocalAppColors.current
}

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun MyRoutineTheme(themeMode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val c = if (dark) DarkAppColors else LightAppColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.onAccent, background = c.background, onBackground = c.textPrimary,
            surface = c.surface, onSurface = c.textPrimary, surfaceVariant = c.surfaceHigh,
            onSurfaceVariant = c.textSecondary, outline = c.outline, error = c.danger,
            surfaceContainer = c.surface, surfaceContainerHigh = c.surfaceHigh, surfaceContainerLow = c.surface,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = c.onAccent, background = c.background, onBackground = c.textPrimary,
            surface = c.surface, onSurface = c.textPrimary, surfaceVariant = c.surfaceHigh,
            onSurfaceVariant = c.textSecondary, outline = c.outline, error = c.danger,
            surfaceContainer = c.surface, surfaceContainerHigh = c.surfaceHigh, surfaceContainerLow = c.surface,
        )
    }
    CompositionLocalProvider(LocalAppColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}
