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
 * Header backdrop: big grass-capped letters spelling PRESTON'S PLAYER scroll by
 * at varying heights, and the blocky runner sprints and hops from letter to
 * letter — jumping the tall ones. Sits behind the clock/PiP. Self-contained so
 * it never touches SplashScreen.
 */

private const val WORD = "PRESTON'S PLAYER"
private const val A = 0.72f   // letter opacity, so header text stays readable on top

private val M_GRASS   = Color(0xFF57B14F).copy(alpha = A)
private val M_GRASS_HI = Color(0xFF6FCC63).copy(alpha = A)
private val M_DIRT    = Color(0xFF5E3F25).copy(alpha = A)
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
private val M_DUST    = Color(0x55FFFFFF)

private val BLANK = listOf("     ", "     ", "     ", "     ", "     ", "     ", "     ")
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
    ' ' to BLANK,
)

private fun lerp(a: Float, b: Float, q: Float) = a + (b - a) * q

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
        val h = size.height
        val baseline = h * 0.92f
        val baseCell = h * 0.050f
        val n = WORD.length

        // per-letter metrics: varied size -> varied width & terrain height
        val cell = FloatArray(n)
        val width = FloatArray(n)
        val topY = FloatArray(n)          // where the runner stands on this letter
        val cumX = FloatArray(n)
        var cx = 0f
        for (i in 0 until n) {
            val f = 0.8f + 0.7f * abs(sin(i * 1.3f))     // deterministic 0.8..1.5
            val c = baseCell * f
            cell[i] = c
            width[i] = 6f * c
            val rows = FONT[WORD[i]] ?: BLANK
            var topLit = 7
            for (r in 0..6) { if (rows[r].indexOf('#') >= 0) { topLit = r; break } }
            topY[i] = if (topLit == 7) baseline else baseline - (7 - topLit) * c
            cumX[i] = cx
            cx += width[i]
        }
        val totalW = cx
        val speed = baseCell * 6.5f
        val scroll = (t * speed) % totalW

        // draw the word, wrapped
        for (rep in -1..1) {
            for (i in 0 until n) {
                val gx = cumX[i] - scroll + rep * totalW
                if (gx > size.width || gx + width[i] < 0f) continue
                drawGlyph(WORD[i], gx, baseline, cell[i])
            }
        }

        // runner hops letter-to-letter at a fixed screen x
        val runX = size.width * 0.22f
        val minHop = h * 0.05f
        val margin = h * 0.06f
        val worldX = (runX + scroll) % totalW
        var li = 0
        for (i in 0 until n) { if (worldX >= cumX[i] && worldX < cumX[i] + width[i]) { li = i; break } }
        val q = ((worldX - cumX[li]) / width[li]).coerceIn(0f, 1f)
        val ni = (li + 1) % n
        val t0 = topY[li]; val t1 = topY[ni]
        val hop = maxOf(minHop, abs(t0 - t1) / 2f + margin)
        val airborne = hop > minHop * 1.35f
        val feetY = lerp(t0, t1, q) - hop * sin(PI.toFloat() * q)
        val rScale = (h * 0.26f) / 18f

        drawMarqueeRunner(runX, feetY, rScale, running = !airborne, runPhase = t * 11f, jumpP = if (airborne) q else -1f)

        if (!airborne) {
            for (k in 0..2) {
                val age = (t * 6f + k * 0.33f) % 1f
                val s = (2.6f - age * 1.7f).coerceAtLeast(0.4f) * rScale
                drawRect(
                    M_DUST.copy(alpha = (1f - age) * 0.3f),
                    topLeft = Offset(runX - 4f * rScale - age * 9f * rScale, feetY - 1f * rScale - age * 2.5f * rScale),
                    size = Size(s, s)
                )
            }
        }
    }
}

/** One grass-capped letter, bottom row sitting on the baseline. */
private fun DrawScope.drawGlyph(chr: Char, gx: Float, baseline: Float, cell: Float) {
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
            val y = baseline - (7 - r) * cell
            drawRect(col, topLeft = Offset(gx + c * cell, y), size = Size(cell + 0.6f, cell + 0.6f))
        }
    }
}

private fun DrawScope.drawMarqueeRunner(
    x: Float, groundY: Float, scale: Float,
    running: Boolean, runPhase: Float, jumpP: Float
) {
    fun px(ux: Float, uy: Float, uw: Float, uh: Float, c: Color) =
        drawRect(c, topLeft = Offset(x + ux * scale, groundY + uy * scale), size = Size(uw * scale, uh * scale))

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
