package com.cumplr.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CumplrColorScheme = darkColorScheme(
    background          = CumplrBackground,
    surface             = CumplrSurface,
    surfaceVariant      = CumplrSurface2,
    onBackground        = CumplrFg,
    onSurface           = CumplrFg,
    onSurfaceVariant    = CumplrFgMuted,
    primary             = CumplrAccent,
    onPrimary           = CumplrAccentInk,
    primaryContainer    = CumplrStatusDoneBg,
    onPrimaryContainer  = CumplrStatusDoneFg,
    outline             = CumplrBorder,
    outlineVariant      = CumplrBorder,
)

@Composable
fun CumplrTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CumplrColorScheme,
        typography  = CumplrTypography,
        content     = content
    )
}
