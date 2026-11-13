package com.funsol.orbit.core.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.funsol.orbit.R
import com.funsol.orbit.core.datastore.ThemeMode

val OrbitIndigo = Color(0xFF1F1B5C)
val OrbitViolet = Color(0xFF6A4DF4)
val OrbitVioletSoft = Color(0xFF8F78FF)
val OrbitPurple = Color(0xFF9B5CF6)
val OrbitCyan = Color(0xFF0EA5C6)
val OrbitCoral = Color(0xFFF0617D)
val OrbitAmber = Color(0xFFFFB23F)

val SuccessGreen = Color(0xFF1FA463)
val WarningAmber = Color(0xFFD98A00)
val DangerRed = Color(0xFFD64545)
val InfoBlue = Color(0xFF3B82F6)

private val LightBackground = Color(0xFFF3F4FA)
private val LightCard = Color(0xFFFFFFFF)
private val LightCardBottom = Color(0xFFFDFDFF)
private val LightWell = Color(0xFFECEFF8)
private val LightText = Color(0xFF14172B)
private val LightMuted = Color(0xFF5F6483)
private val DarkBackground = Color(0xFF0B0D1C)
private val DarkCard = Color(0xFF14172B)
private val DarkCardBottom = Color(0xFF171A30)
private val DarkWell = Color(0xFF1C2038)
private val DarkText = Color(0xFFEEF0FF)

val BOTTOM_BAR_CONTENT_PADDING: Dp = 100.dp

enum class FeatureAccent(val light: Color, val dark: Color, val to: Color) {
    HOME(Color(0xFF6A4DF4), Color(0xFF8F78FF), Color(0xFF9B5CF6)),
    TASKS(Color(0xFF6A4DF4), Color(0xFF8F78FF), Color(0xFF8E6CFA)),
    PROJECTS(Color(0xFF1596B2), Color(0xFF45CBE0), Color(0xFF2BA8C4)),
    CALENDAR(Color(0xFF5B5BD6), Color(0xFF8B8BF0), Color(0xFF7C7CE8)),
    NOTES(Color(0xFFE09A1F), Color(0xFFF2BC66), Color(0xFFF0B74F)),
    HABITS(Color(0xFF2E9E62), Color(0xFF5FD393), Color(0xFF49BE86)),
    GOALS(Color(0xFFD14A86), Color(0xFFEF86B6), Color(0xFFE5719B)),
    ANALYTICS(Color(0xFF2563EB), Color(0xFF60A5FA), Color(0xFF38BDF8)),
    SETTINGS(Color(0xFF6B63D6), Color(0xFFA29CF2), Color(0xFF8B85E6)),
    PRIVACY(Color(0xFF0D9488), Color(0xFF2DD4BF), Color(0xFF14B8A6)),
    BACKUP(Color(0xFF7C3AED), Color(0xFFA78BFA), Color(0xFFC084FC)),
    PREMIUM(Color(0xFFE3A64A), Color(0xFFEBC27A), Color(0xFFEFBE7A)),
}

@Immutable
class OrbitExtras(
    val isDark: Boolean,
    val brandGradient: Brush,
    val heroGradient: Brush,
    val cardStroke: Color,
    val cardStrokeWidth: Dp,
    val cardGlow: Color,
    val actionGlow: Color,
    val well: Color,
    val textMuted: Color,
    val sectionTitle: Color,
    val cardTop: Color,
    val cardBottom: Color,
    val glassFill: Color,
    val glassStroke: Color,
    val amber: Color,
) {
    fun accent(accent: FeatureAccent): Color = if (isDark) accent.dark else accent.light

    fun readable(color: Color): Color {
        if (isDark) return color
        var c = color
        var step = 0
        while (step < 12 && contrastRatio(c, cardBottom) < 4.5f) {
            c = androidx.compose.ui.graphics.lerp(c, Color.Black, 0.12f)
            step++
        }
        return c
    }

    fun accentText(accent: FeatureAccent): Color = readable(accent(accent))
    fun accentGradient(accent: FeatureAccent): Brush = Brush.linearGradient(listOf(accent(accent), accent.to))

    val cardBrush: Brush get() = Brush.verticalGradient(listOf(cardTop, cardBottom))

    val ringBrush: Brush get() = Brush.linearGradient(listOf(amber, OrbitCoral, amber))

    fun tinted(accent: Color, strength: Float = 0.06f): Color = accent.copy(alpha = strength).compositeOver(cardBottom)

    fun tintedBrush(accent: Color, strength: Float = 0.06f): Brush = Brush.verticalGradient(
        listOf(accent.copy(alpha = strength + 0.01f).compositeOver(cardTop), tinted(accent, strength)),
    )

    fun softGradient(accent: Color, from: Float = 0.22f, to: Float = 0.08f): Brush =
        Brush.linearGradient(listOf(accent.copy(alpha = from), accent.copy(alpha = to)))
}

private val LightExtras = OrbitExtras(
    isDark = false,
    brandGradient = Brush.linearGradient(listOf(OrbitViolet, OrbitPurple)),
    heroGradient = Brush.linearGradient(listOf(Color(0xFF3A3196), Color(0xFF4E40C4), Color(0xFF6352D2))),
    cardStroke = Color(0xFF3A4A8C).copy(alpha = 0.10f),
    cardStrokeWidth = 0.8.dp,
    cardGlow = Color(0xFF3A4A8C).copy(alpha = 0.06f),
    actionGlow = OrbitViolet.copy(alpha = 0.36f),
    well = LightWell,
    textMuted = LightMuted,
    sectionTitle = Color(0xFF5F6483),
    cardTop = LightCard,
    cardBottom = LightCardBottom,
    glassFill = Color.White.copy(alpha = 0.14f),
    glassStroke = Color.White.copy(alpha = 0.24f),
    amber = OrbitAmber,
)

private val DarkExtras = OrbitExtras(
    isDark = true,
    brandGradient = Brush.linearGradient(listOf(OrbitVioletSoft, OrbitPurple)),
    heroGradient = Brush.linearGradient(listOf(Color(0xFF241D78), Color(0xFF3F32B8), Color(0xFF5B45D6))),
    cardStroke = Color.White.copy(alpha = 0.08f),
    cardStrokeWidth = 0.8.dp,
    cardGlow = Color.Black.copy(alpha = 0.28f),
    actionGlow = OrbitVioletSoft.copy(alpha = 0.34f),
    well = DarkWell,
    textMuted = Color(0xFFA3A9C9),
    sectionTitle = Color(0xFF8E94B6),
    cardTop = DarkCard,
    cardBottom = DarkCardBottom,
    glassFill = Color.White.copy(alpha = 0.14f),
    glassStroke = Color.White.copy(alpha = 0.24f),
    amber = OrbitAmber,
)

val LocalOrbitExtras = staticCompositionLocalOf { LightExtras }

object Orbit {
    val extras: OrbitExtras @Composable @ReadOnlyComposable get() = LocalOrbitExtras.current
}

private val LightColors = lightColorScheme(
    primary = OrbitViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAE6FF),
    onPrimaryContainer = Color(0xFF1B0E6B),
    secondary = Color(0xFF0B8FAD),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6F4FB),
    onSecondaryContainer = Color(0xFF05424F),
    tertiary = Color(0xFFD9466A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0E7),
    onTertiaryContainer = Color(0xFF5E0D22),
    background = LightBackground,
    onBackground = LightText,
    surface = LightCard,
    onSurface = LightText,
    surfaceVariant = LightWell,
    onSurfaceVariant = LightMuted,
    surfaceContainerLowest = LightCard,
    surfaceContainerLow = Color(0xFFF7F7FD),
    surfaceContainer = Color(0xFFF3F4FA),
    surfaceContainerHigh = Color(0xFFEEF0F9),
    surfaceContainerHighest = LightWell,
    outline = Color(0xFF7A78A0),
    outlineVariant = Color(0xFFDFE2F0),
    error = DangerRed,
)

private val DarkColors = darkColorScheme(
    primary = OrbitVioletSoft,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF33267F),
    onPrimaryContainer = Color(0xFFE6E0FF),
    secondary = Color(0xFF38D5EE),
    onSecondary = Color(0xFF00363F),
    secondaryContainer = Color(0xFF0E4A57),
    onSecondaryContainer = Color(0xFFCFF5FC),
    tertiary = Color(0xFFFF7F96),
    onTertiary = Color(0xFF55081F),
    tertiaryContainer = Color(0xFF6B1D35),
    onTertiaryContainer = Color(0xFFFFD9E1),
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkCard,
    onSurface = DarkText,
    surfaceVariant = DarkWell,
    onSurfaceVariant = Color(0xFFB4B1D8),
    surfaceContainerLowest = Color(0xFF0F1124),
    surfaceContainerLow = Color(0xFF111428),
    surfaceContainer = DarkCard,
    surfaceContainerHigh = DarkWell,
    surfaceContainerHighest = Color(0xFF252A47),
    outline = Color(0xFF8D8AB5),
    outlineVariant = Color(0xFF2A2F4D),
    error = Color(0xFFFF8A8A),
)

val OrbitFontFamily: FontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
)

private val Sans = OrbitFontFamily

private val OrbitTypography = Typography(
    displaySmall = TextStyle(fontFamily = Sans, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.6).sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontSize = 20.sp, lineHeight = 27.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = Sans, fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontFamily = Sans, fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.1).sp),
    titleSmall = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontFamily = Sans, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontFamily = Sans, fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium),
)

private val OrbitShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val touchTarget = 48.dp
    val section = 20.dp
}

object Radius {
    val card = 18.dp
    val sheet = 22.dp
    val button = 26.dp
    val chip = 12.dp
    val tile = 12.dp
}

@Composable
fun isAppInDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun OrbitTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = isAppInDarkTheme(themeMode)
    ApplySystemBarColors(dark)
    CompositionLocalProvider(LocalOrbitExtras provides if (dark) DarkExtras else LightExtras) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = OrbitTypography,
            shapes = OrbitShapes,
            content = content,
        )
    }
}

@Composable
fun ApplySystemBarColors(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.Transparent.toArgb()
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.Transparent.toArgb()
    }
}

internal fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    val hi = maxOf(la, lb)
    val lo = minOf(la, lb)
    return (hi + 0.05f) / (lo + 0.05f)
}
