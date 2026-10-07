package com.example.contris.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/** Parses `#RRGGBB` / `#AARRGGBB` / `RRGGBB` strings; returns null for anything else. */
fun parseHexColor(hex: String?): Color? {
    val s = hex?.trim()?.removePrefix("#") ?: return null
    return when (s.length) {
        6 -> s.toLongOrNull(16)?.let { Color((0xFF000000L or it).toInt()) }
        8 -> s.toLongOrNull(16)?.let { Color(it.toInt()) }
        else -> null
    }
}

/**
 * Blends a flag colour over the surface so it works as a header background in both light and dark mode.
 */
fun flagTint(hex: String?, surface: Color, alpha: Float = 0.35f): Color? =
    parseHexColor(hex)?.copy(alpha = alpha)?.compositeOver(surface)
