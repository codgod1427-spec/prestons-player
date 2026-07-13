package com.prestonsplayer.tv.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/*
 * A slim header banner: the word PRESTON'S PLAYER, spelled in grass-capped
 * voxel letters, scrolls by while the same blocky runner from the splash
 * sprints and hops along the top. Fully self-contained (its own copy of the
 * art primitives) so it never touches SplashScreen.
 */

private val M_GRASS   = Color(0xFF57B14F)
private val M_GRASS_HI = Color(0xFF6FCC63)
private val M_DIRT    = Color(0xFF5E3F25)
private val M_EDGE    = Color(0xFF2A1B10)
private val M_SKIN    = Color(0xFFD9A066)
private val M_SKIN_S  = Color(0xFFB07E4B)
private val M_SHIRT   = Color(0xFF2F7FD8)
private val M_SHIRT_S = Color(0xFF215FA3)
private val M_PANTS   = Color(0xFF3B3F58)
private val M_PANTS_S = Color(0xFF2B2E42)
private val M_HAIR    = Color(0xFF3A2A1C)
private val M_EYE     = Color(0xFFFFFFFF)
private val M_PUPIL   = Color(0xFF16202E)
private val M_DUST    = Color(0x66FFFFFF)

// 5x7 blocky font, only the glyphs PRESTON'S PLAYER needs.
private val FONT: Map<Char, List<String>> = mapOf(
    'P' to listOf("#### ", "#   #", "#   #", "#### ", "#    ", "#    ", "#    "),
    'R' to listOf("#### ", "#   #", "#   #", "#### ", "# #  ", "#  # ", "#   #"),
    'E' to listOf("#####", "#    ", "#    ", "#### ", "#    ", "#    ", "#####"),
    'S' to listOf(" ####", "#    ", "#    ", " ### ", "    #", "    #", "#### "),
    'T' to listOf("#####", "  #  ", "  #  ", "  #  ", "  #  ", "  #  ", "  #  "),
    'O' to listOf(" ### ", "#   #", "#   #", "#   #", "#   #", "#   #", " ### "),
    'N' to listOf("#   #", "##  #", "# # #", "# # #", "#  ##", "#   #", "#   #"),
    'L' to listOf("#    ", "#    ", "#    ", "#    ", "#    ", "#    ", "#####"),
    'A' to listOf(" ### ", "#   #", "#   #", "#####", "#   #", "#   #", "#   #"),
    'Y' to listOf("#   #", "#   #", " # # ", "  #  ", "  #  ", "  #  ", "  #  "),
    '\'' to listOf("  #  ", "  #  ", "  #  ", "     ", "     ", "     ", "     "),
    ' ' to listOf("     ", "     ", "     ", "     ", "     ", "     ", "     "),
)

private const val WORD = "PRESTON'S PLAYER"

@Composable
fun ParkourMarquee(modifier: Modifier = Modifier) {
    var t by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { n ->
                if (last != 0L) t += (n - last) / 1_000_000_000f
                last = n
            }
        }
    }

    Canvas(modifier) {
        val ch = size.height
        val cell = (ch * 0.40f) / 7f              // one font pixel
        val glyphW = 6f * cell                    // 5 wide + 1 spacing
        val totalW = WORD.length * glyphW
        val wordTopY = ch * 0.50f
        val speed = cell * 7f                      // px / sec
        val scrollBase = -((t * speed) % totalW)

        // scrolling word (drawn twice for a seamless wrap)
        for (rep in 0..1) {
            var gx = scrollBase + rep * totalW
            for (chr in WORD) {
                drawGlyph(chr, gx, wordTopY, cell)
                gx += glyphW
            }
        }

        // runner: sprint, then a hop, on a loop — fixed screen x while the word scrolls under him
        val cycle = 1.7f
        val ph = t % cycle
        val running = ph < 1.15f
        val jp = if (running) -1f else (ph - 1.15f) / (cycle - 1.15f)
        val peakPx = ch * 0.20f
        val lift = if (!running) peakPx * 4f * jp * (1f - jp) else 0f
        val runX = size.width * 0.24f
        val rScale = (ch * 0.48f) / 18f

        drawMarqueeRunner(runX, wordTopY - lift, rScale, running, t * 11f, jp)

        if (running) {
            for (k in 0..2) {
                val age = (t * 6f + k * 0.33f) % 1f
                val s = (2.6f - age * 1.7f).coerceAtLeast(0.4f) * rScale
                drawRect(
                    M_DUST.copy(alpha = (1f - age) * 0.35f),
                    topLeft = Offset(runX - 4f * rScale - age * 9f * rScale, wordTopY - 1f * rScale - age * 2.5f * rScale),
                    size = Size(s, s)
                )
            }
        }
    }
}

/** Draws one grass-capped letter at (gx, topY); each lit font pixel is a small block. */
private fun DrawScope.drawGlyph(chr: Char, gx: Float, topY: Float, cell: Float) {
    val rows = FONT[chr] ?: return
    for (c in 0..4) {
        var topLit = -1
        for (r in 0..6) if (rows[r][c] == '#') { topLit = r; break }
        for (r in 0..6) {
            if (rows[r][c] != '#') continue
            val col = when {
                r == topLit -> M_GRASS_HI
                r >= 5      -> M_DIRT
                else        -> M_GRASS
            }
            drawRect(col, topLeft = Offset(gx + c * cell, topY + r * cell), size = Size(cell + 0.6f, cell + 0.6f))
        }
    }
}

// ---- runner (self-contained copy of the splash art) ----
private fun DrawScope.mpx(x: Float, y: Float, w: Float, h: Float, c: Color) =
    drawRect(c, topLeft = Offset(x, y), size = Size(w, h))

private fun DrawScope.drawMarqueeRunner(
    x: Float, groundY: Float, scale: Float,
    running: Boolean, runPhase: Float, jumpP: Float
) {
    fun px(ux: Float, uy: Float, uw: Float, uh: Float, c: Color) =
        mpx(x + ux * scale, groundY + uy * scale, uw * scale, uh * scale, c)

    val bob = if (running) abs(sin(runPhase)) * 0.7f else 0f
    val swing = if (running) sin(runPhase) * 3.2f else 0f
    val swing2 = if (running) sin(runPhase + PI.toFloat()) * 3.2f else 0f
    val bodyTop = -18f + bob
    val headTop = bodyTop - 6f

    if (jumpP >= 0f) {
        val tuck = sin(jumpP * PI.toFloat())
        px(-1.2f, -7f - tuck * 2.2f, 2.6f, 6f, M_PANTS)
        px(1.6f, -8f - tuck * 3.4f, 2.6f, 5.4f, M_PANTS_S)
        px(1.6f, -3.4f - tuck * 3.4f, 3.2f, 1.6f, M_EDGE)
    } else {
        px(-1.6f + swing, -7f, 2.6f, 7f, M_PANTS_S)
        px(0.9f + swing2, -7f, 2.6f, 7f, M_PANTS)
        px(-1.9f + swing, -1.4f, 3.2f, 1.4f, M_EDGE)
        px(0.7f + swing2, -1.4f, 3.2f, 1.4f, M_EDGE)
    }

    px(-2.2f, bodyTop, 5.6f, 7.4f, M_SHIRT)
    px(1.2f, bodyTop, 2.2f, 7.4f, M_SHIRT_S)
    px(-2.2f, bodyTop + 5.6f, 5.6f, 1.8f, M_SHIRT_S)

    if (jumpP >= 0f) {
        val reach = sin(jumpP * PI.toFloat())
        px(2.6f, bodyTop - 2.4f * reach, 2.2f, 6f, M_SKIN)
        px(-4.0f, bodyTop + 1.6f * reach, 2.2f, 5.6f, M_SKIN_S)
    } else {
        px(2.6f, bodyTop + 0.6f + swing2, 2.2f, 6f, M_SKIN)
        px(-4.0f, bodyTop + 0.6f + swing, 2.2f, 6f, M_SKIN_S)
    }

    px(-3f, headTop, 6f, 6f, M_SKIN)
    px(0.6f, headTop, 2.4f, 6f, M_SKIN_S)
    px(-3f, headTop, 6f, 1.7f, M_HAIR)
    px(-3f, headTop - 0.9f, 4.2f, 1.2f, M_HAIR)
    px(0.2f, headTop + 2.6f, 1.6f, 1.6f, M_EYE)
    px(1.0f, headTop + 2.9f, 0.9f, 1.1f, M_PUPIL)
}
