package com.whataicando.codeoba.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

data class ColorTheme(
    val name: String,
    val code: String,
    val obsidianBg: Color,
    val slateSurface: Color,
    val cardSurface: Color,
    val accentCyan: Color,
    val accentPurple: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val borderColor: Color
)

object ThemeManager {
    var currentThemeCode by mutableStateOf(SettingsManager.getThemeCode())
    
    var customBg by mutableStateOf(Color(SettingsManager.getCustomBg()))
    var customSurface by mutableStateOf(Color(SettingsManager.getCustomSurface()))
    var customAccent1 by mutableStateOf(Color(SettingsManager.getCustomAccent1()))
    var customAccent2 by mutableStateOf(Color(SettingsManager.getCustomAccent2()))

    fun updateCustomBg(color: Color) {
        customBg = color
        SettingsManager.setCustomBg(color.toArgb())
    }

    fun updateCustomSurface(color: Color) {
        customSurface = color
        SettingsManager.setCustomSurface(color.toArgb())
    }

    fun updateCustomAccent1(color: Color) {
        customAccent1 = color
        SettingsManager.setCustomAccent1(color.toArgb())
    }

    fun updateCustomAccent2(color: Color) {
        customAccent2 = color
        SettingsManager.setCustomAccent2(color.toArgb())
    }

    val currentTheme: ColorTheme
        get() = if (currentThemeCode == "custom") {
            ColorTheme(
                name = "Custom",
                code = "custom",
                obsidianBg = customBg,
                slateSurface = customSurface,
                cardSurface = getLighterColor(customSurface, 0.04f),
                accentCyan = customAccent1,
                accentPurple = customAccent2,
                textPrimary = getLighterColor(customBg, 0.88f),
                textSecondary = getLighterColor(customBg, 0.58f),
                borderColor = getLighterColor(customSurface, 0.08f)
            )
        } else {
            themes[currentThemeCode] ?: ObsidianTheme
        }
}

val ObsidianTheme = ColorTheme(
    name = "Obsidian",
    code = "obsidian",
    obsidianBg = Color(0xFF0C0C0E),
    slateSurface = Color(0xFF14141A),
    cardSurface = Color(0xFF1E1E28),
    accentCyan = Color(0xFF00E5FF),
    accentPurple = Color(0xFFAB47BC),
    textPrimary = Color(0xFFF5F5F7),
    textSecondary = Color(0xFF9E9EAE),
    borderColor = Color(0xFF2C2C3A)
)

val NordicFrostTheme = ColorTheme(
    name = "Nordic Frost",
    code = "nordic_frost",
    obsidianBg = Color(0xFF0F1216),
    slateSurface = Color(0xFF161B22),
    cardSurface = Color(0xFF21262D),
    accentCyan = Color(0xFF58A6FF),
    accentPurple = Color(0xFFB48EAD),
    textPrimary = Color(0xFFECEFF4),
    textSecondary = Color(0xFF8892B0),
    borderColor = Color(0xFF30363D)
)

val EmeraldForestTheme = ColorTheme(
    name = "Emerald Forest",
    code = "emerald_forest",
    obsidianBg = Color(0xFF090F0D),
    slateSurface = Color(0xFF101915),
    cardSurface = Color(0xFF192520),
    accentCyan = Color(0xFF00E676),
    accentPurple = Color(0xFF81C784),
    textPrimary = Color(0xFFE8F5E9),
    textSecondary = Color(0xFFA5D6A7),
    borderColor = Color(0xFF253B32)
)

val SunsetCopperTheme = ColorTheme(
    name = "Sunset Copper",
    code = "sunset_copper",
    obsidianBg = Color(0xFF0F0B0A),
    slateSurface = Color(0xFF171210),
    cardSurface = Color(0xFF231B18),
    accentCyan = Color(0xFFFF7043),
    accentPurple = Color(0xFFFFB74D),
    textPrimary = Color(0xFFFBE9E7),
    textSecondary = Color(0xFFB0BEC5),
    borderColor = Color(0xFF382A25)
)

val RoyalAmethystTheme = ColorTheme(
    name = "Royal Amethyst",
    code = "royal_amethyst",
    obsidianBg = Color(0xFF0A0810),
    slateSurface = Color(0xFF120E1C),
    cardSurface = Color(0xFF1C172B),
    accentCyan = Color(0xFFBB86FC),
    accentPurple = Color(0xFFFF4081),
    textPrimary = Color(0xFFF3E5F5),
    textSecondary = Color(0xFFD1C4E9),
    borderColor = Color(0xFF2C223E)
)

val DraculaTheme = ColorTheme(
    name = "Dracula",
    code = "dracula",
    obsidianBg = Color(0xFF1E1E2F),
    slateSurface = Color(0xFF21222C),
    cardSurface = Color(0xFF282A36),
    accentCyan = Color(0xFF8BE9FD),
    accentPurple = Color(0xFFFF79C6),
    textPrimary = Color(0xFFF8F8F2),
    textSecondary = Color(0xFF6272A4),
    borderColor = Color(0xFF44475A)
)

val CyberpunkNeonTheme = ColorTheme(
    name = "Cyberpunk Neon",
    code = "cyberpunk_neon",
    obsidianBg = Color(0xFF0D0A14),
    slateSurface = Color(0xFF151022),
    cardSurface = Color(0xFF221A33),
    accentCyan = Color(0xFFFCEE09),
    accentPurple = Color(0xFFFF003C),
    textPrimary = Color(0xFFF5F5F7),
    textSecondary = Color(0xFF8F7CA3),
    borderColor = Color(0xFF3C2C5E)
)

val MonochromeSlateTheme = ColorTheme(
    name = "Monochrome Slate",
    code = "monochrome_slate",
    obsidianBg = Color(0xFF0E0E0E),
    slateSurface = Color(0xFF161616),
    cardSurface = Color(0xFF222222),
    accentCyan = Color(0xFFFFFFFF),
    accentPurple = Color(0xFF888888),
    textPrimary = Color(0xFFEDEDED),
    textSecondary = Color(0xFF8A8A8A),
    borderColor = Color(0xFF333333)
)

val CustomThemeOption: ColorTheme
    get() = ColorTheme(
        name = "Custom",
        code = "custom",
        obsidianBg = ThemeManager.customBg,
        slateSurface = ThemeManager.customSurface,
        cardSurface = getLighterColor(ThemeManager.customSurface, 0.04f),
        accentCyan = ThemeManager.customAccent1,
        accentPurple = ThemeManager.customAccent2,
        textPrimary = getLighterColor(ThemeManager.customBg, 0.88f),
        textSecondary = getLighterColor(ThemeManager.customBg, 0.58f),
        borderColor = getLighterColor(ThemeManager.customSurface, 0.08f)
    )

val themes: Map<String, ColorTheme>
    get() = listOf(
        ObsidianTheme,
        NordicFrostTheme,
        EmeraldForestTheme,
        SunsetCopperTheme,
        RoyalAmethystTheme,
        DraculaTheme,
        CyberpunkNeonTheme,
        MonochromeSlateTheme,
        CustomThemeOption
    ).associateBy { it.code }

val ObsidianBg: Color get() = ThemeManager.currentTheme.obsidianBg
val SlateSurface: Color get() = ThemeManager.currentTheme.slateSurface
val CardSurface: Color get() = ThemeManager.currentTheme.cardSurface
val AccentCyan: Color get() = ThemeManager.currentTheme.accentCyan
val AccentPurple: Color get() = ThemeManager.currentTheme.accentPurple
val TextPrimary: Color get() = ThemeManager.currentTheme.textPrimary
val TextSecondary: Color get() = ThemeManager.currentTheme.textSecondary
val BorderColor: Color get() = ThemeManager.currentTheme.borderColor

// Helper & Utility functions

fun Color.toArgb(): Int {
    return ((this.alpha * 255).toInt() shl 24) or
           ((this.red * 255).toInt() shl 16) or
           ((this.green * 255).toInt() shl 8) or
           (this.blue * 255).toInt()
}

fun getLighterColor(color: Color, amount: Float): Color {
    return Color(
        red = (color.red + amount).coerceIn(0f, 1f),
        green = (color.green + amount).coerceIn(0f, 1f),
        blue = (color.blue + amount).coerceIn(0f, 1f),
        alpha = color.alpha
    )
}

fun hslToColor(h: Float, s: Float, l: Float): Color {
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (r, g, b) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(
        red = (r + m).coerceIn(0f, 1f),
        green = (g + m).coerceIn(0f, 1f),
        blue = (b + m).coerceIn(0f, 1f),
        alpha = 1f
    )
}

fun colorToHsl(color: Color): FloatArray {
    val r = color.red
    val g = color.green
    val b = color.blue

    val max = maxOf(r, maxOf(g, b))
    val min = minOf(r, minOf(g, b))
    val d = max - min

    var h = 0f
    var s = 0f
    val l = (max + min) / 2f

    if (max != min) {
        s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
        h = when (max) {
            r -> (g - b) / d + (if (g < b) 6f else 0f)
            g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        }
        h *= 60f
    }
    return floatArrayOf(h, s, l)
}
