package com.cloudscope.moneyprinter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class TerminalColorScheme(
    val background: Color,
    val surface: Color,
    val card: Color,
    val cardElevated: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val bullish: Color,
    val bullishBg: Color,
    val bearish: Color,
    val bearishBg: Color,
    val sideways: Color,
    val sidewaysBg: Color,
    val accentBlue: Color,
    val itmCallTint: Color,
    val itmPutTint: Color,
    val isDark: Boolean,
    val warning: Color = sideways
)

val LocalTerminalColors = staticCompositionLocalOf {
    TerminalColorScheme(
        background = TerminalBackgroundDark,
        surface = TerminalSurfaceDark,
        card = TerminalCardDark,
        cardElevated = TerminalCardElevatedDark,
        border = TerminalBorderDark,
        textPrimary = TerminalTextPrimaryDark,
        textSecondary = TerminalTextSecondaryDark,
        textMuted = TerminalTextMutedDark,
        bullish = BullishGreenDark,
        bullishBg = BullishGreenBgDark,
        bearish = BearishRedDark,
        bearishBg = BearishRedBgDark,
        sideways = SidewaysAmberDark,
        sidewaysBg = SidewaysAmberBgDark,
        accentBlue = AccentBlueDark,
        itmCallTint = ItmCallTintDark,
        itmPutTint = ItmPutTintDark,
        isDark = true
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = BullishGreenDark,
    background = TerminalBackgroundDark,
    surface = TerminalSurfaceDark,
    onPrimary = TerminalBackgroundDark,
    onBackground = TerminalTextPrimaryDark,
    onSurface = TerminalTextPrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = BullishGreenLight,
    background = TerminalBackgroundLight,
    surface = TerminalSurfaceLight,
    onPrimary = Color.White,
    onBackground = TerminalTextPrimaryLight,
    onSurface = TerminalTextPrimaryLight
)

@Composable
fun CloudScopeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val terminalColors = if (darkTheme) {
        TerminalColorScheme(
            background = TerminalBackgroundDark,
            surface = TerminalSurfaceDark,
            card = TerminalCardDark,
            cardElevated = TerminalCardElevatedDark,
            border = TerminalBorderDark,
            textPrimary = TerminalTextPrimaryDark,
            textSecondary = TerminalTextSecondaryDark,
            textMuted = TerminalTextMutedDark,
            bullish = BullishGreenDark,
            bullishBg = BullishGreenBgDark,
            bearish = BearishRedDark,
            bearishBg = BearishRedBgDark,
            sideways = SidewaysAmberDark,
            sidewaysBg = SidewaysAmberBgDark,
            accentBlue = AccentBlueDark,
            itmCallTint = ItmCallTintDark,
            itmPutTint = ItmPutTintDark,
            isDark = true
        )
    } else {
        TerminalColorScheme(
            background = TerminalBackgroundLight,
            surface = TerminalSurfaceLight,
            card = TerminalCardLight,
            cardElevated = TerminalCardElevatedLight,
            border = TerminalBorderLight,
            textPrimary = TerminalTextPrimaryLight,
            textSecondary = TerminalTextSecondaryLight,
            textMuted = TerminalTextMutedLight,
            bullish = BullishGreenLight,
            bullishBg = BullishGreenBgLight,
            bearish = BearishRedLight,
            bearishBg = BearishRedBgLight,
            sideways = SidewaysAmberLight,
            sidewaysBg = SidewaysAmberBgLight,
            accentBlue = AccentBlueLight,
            itmCallTint = ItmCallTintLight,
            itmPutTint = ItmPutTintLight,
            isDark = false
        )
    }

    val materialColors = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalTerminalColors provides terminalColors) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = Typography,
            content = content
        )
    }
}
