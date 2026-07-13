package com.prestonsplayer.tv.ui

import androidx.compose.ui.graphics.Color

/**
 * Guide palette. Default is a Bell Fibe-style deep navy + broadcast blue.
 * Swap ACCENT / ACCENT_SOFT to the Telus pair below for the purple/green look.
 */
object GuideTheme {
    // --- Bell-style (default) ---
    val BG = Color(0xFF0A0F1A)            // page background
    val SURFACE = Color(0xFF131B2C)       // guide cells
    val SURFACE_ALT = Color(0xFF0F1626)   // alternating rows / header
    val ACCENT = Color(0xFF1E88E5)        // focused cell + highlights
    val ACCENT_SOFT = Color(0xFF16324F)   // "airing now" tint
    val TEXT = Color(0xFFF2F5F9)
    val TEXT_DIM = Color(0xFF8A94A6)
    val LINE = Color(0xFF22304A)          // grid hairlines
    val NOW_LINE = Color(0xFFE53935)      // red "current time" needle

    // --- Telus-style variant (swap in if preferred) ---
    // val ACCENT = Color(0xFF4B286D)     // Telus purple
    // val ACCENT_SOFT = Color(0xFF2A1741)
    // val NOW_LINE = Color(0xFF66CC00)   // Telus green
}
