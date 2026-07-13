package com.prestonsplayer.tv.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.withFrameNanos
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Original voxel-style runner. Every shape is drawn from primitives here —
 * no third-party game assets, textures, or characters are used.
 *
 * The course is a looping parkour line: sprint, leap a gap, wall-hop up,
 * drop down, sprint again.
 */

// ---- Palette (grassy-block world, but our own colors) ----
private val SKY_TOP = Color(0xFF0B1020)
private val SKY_BOT = Color(0xFF1B2C4E)
private val BLOCK_TOP = Color(0xFF57B14F)   // grass cap
private val BLOCK_TOP_HI = Color(0xFF6FCC63)
private val BLOCK_DIRT = Color(0xFF7A5230)
private val BLOCK_DIRT_D = Color(0xFF5E3F25)
private val BLOCK_EDGE = Color(0xFF2A1B10)
private val SKIN = Color(0xFFD9A066)
private val SKIN_SHADE = Color(0xFFB07E4B)
private val SHIRT = Color(0xFF2F7FD8)
private val SHIRT_SHADE = Color(0xFF215FA3)
private val PANTS = Color(0xFF3B3F58)
private val PANTS_SHADE = Color(0xFF2B2E42)
private val HAIR = Color(0xFF3A2A1C)
private val EYE = Color(0xFFFFFFFF)
private val PUPIL = Color(0xFF16202E)
private val DUST = Color(0x66FFFFFF)

/** One parkour move in the loop. */
private data class Move(
    val kind: Kind,
    val x0: Float, val y0: Float,
    val x1: Float, val y1: Float,
    val dur: Float,           // relative duration
    val peak: Float = 0f      // extra jump height (world units)
) { enum class Kind { RUN, JUMP } }

// World is 200 wide x 100 tall; the camera scales it to fit.
private val COURSE = listOf(
    Move(Move.Kind.RUN,   10f, 62f,  46f, 62f, 1.05f),
    Move(Move.Kind.JUMP,  46f, 62f,  84f, 62f, 0.80f, peak = 22f),   // gap leap
    Move(Move.Kind.RUN,   84f, 62f, 104f, 62f, 0.55f),
    Move(Move.Kind.JUMP, 104f, 62f, 128f, 44f, 0.70f, peak = 16f),   // hop up
    Move(Move.Kind.RUN,  128f, 44f, 150f, 44f, 0.60f),
    Move(Move.Kind.JUMP, 150f, 44f, 186f, 62f, 0.80f, peak = 12f),   // drop down
    Move(Move.Kind.RUN,  186f, 62f, 200f, 62f, 0.40f)
)

// Platforms: x, y(top), width  (world units)
private val PLATFORMS = listOf(
    Triple(0f, 62f, 54f),
    Triple(76f, 62f, 36f),
    Triple(120f, 44f, 38f),
    Triple(170f, 62f, 40f)
)

@Composable
fun SplashScreen(status: String = "Loading channels…") {
    var t by remember { mutableStateOf(0f) }   // seconds
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { n ->
                if (last != 0L) t += (n - last) / 1_000_000_000f
                last = n
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SKY_TOP, SKY_BOT))),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "PRESTON'S",
            color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace, letterSpacing = 10.sp
        )
        Text(
            "P L A Y E R",
            color = BLOCK_TOP_HI, fontSize = 44.sp, fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace, letterSpacing = 6.sp
        )

        Spacer(Modifier.height(22.dp))

        Canvas(
            Modifier
                .fillMaxWidth(0.82f)
                .height(230.dp)
        ) {
            val scale = size.width / 200f
            val stars = 40

            // --- Star field (deterministic twinkle) ---
            for (i in 0 until stars) {
                val sx = ((i * 71) % 200).toFloat()
                val sy = ((i * 37) % 40).toFloat()
                val tw = 0.35f + 0.35f * sin(t * 2f + i)
                drawRect(
                    Color.White.copy(alpha = tw * 0.5f),
                    topLeft = Offset(sx * scale, sy * scale),
                    size = Size(1.6f * scale, 1.6f * scale)
                )
            }

            // --- Platforms ---
            PLATFORMS.forEach { (px, py, pw) ->
                drawPlatform(px, py, pw, scale)
            }

            // --- Runner along the course ---
            val total = COURSE.sumOf { it.dur.toDouble() }.toFloat()
            val loopT = (t % total)
            var acc = 0f
            var move = COURSE.last()
            var p = 0f
            for (m in COURSE) {
                if (loopT < acc + m.dur) {
                    move = m
                    p = (loopT - acc) / m.dur
                    break
                }
                acc += m.dur
            }

            val cx = move.x0 + (move.x1 - move.x0) * p
            val airLift = if (move.kind == Move.Kind.JUMP) move.peak * 4f * p * (1f - p) else 0f
            val cy = move.y0 + (move.y1 - move.y0) * p - airLift

            // Landing squash right after a jump ends
            val squash = if (move.kind == Move.Kind.RUN && p < 0.12f)
                1f - 0.18f * (1f - p / 0.12f) else 1f

            drawRunner(
                x = cx, groundY = cy, scale = scale,
                running = move.kind == Move.Kind.RUN,
                runPhase = t * 11f,
                jumpP = if (move.kind == Move.Kind.JUMP) p else -1f,
                squash = squash
            )

            // --- Dust puffs while sprinting ---
            if (move.kind == Move.Kind.RUN) {
                for (k in 0..2) {
                    val age = ((t * 6f + k * 0.33f) % 1f)
                    val dx = (cx - 4f - age * 9f) * scale
                    val dy = (cy - 1f - age * 2.5f) * scale
                    val s = (2.6f - age * 1.7f).coerceAtLeast(0.4f) * scale
                    drawRect(
                        DUST.copy(alpha = (1f - age) * 0.4f),
                        topLeft = Offset(dx, dy), size = Size(s, s)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- Blocky loading bar ---
        Canvas(Modifier.fillMaxWidth(0.42f).height(14.dp)) {
            val cells = 16
            val gap = size.width * 0.012f
            val cw = (size.width - gap * (cells - 1)) / cells
            for (i in 0 until cells) {
                val wave = sin(t * 3.4f - i * 0.42f)
                val on = wave > 0.15f
                drawRect(
                    if (on) BLOCK_TOP_HI else Color.White.copy(alpha = 0.10f),
                    topLeft = Offset(i * (cw + gap), 0f),
                    size = Size(cw, size.height)
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            status,
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 13.sp, fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------- drawing helpers (world units -> px via scale) ----------------

private fun DrawScope.px(x: Float, y: Float, w: Float, h: Float, c: Color, s: Float) {
    drawRect(c, topLeft = Offset(x * s, y * s), size = Size(w * s, h * s))
}

/** A grass-capped block platform with a chunky 3D lip. */
private fun DrawScope.drawPlatform(x: Float, top: Float, w: Float, s: Float) {
    val depth = 26f
    px(x, top, w, 4f, BLOCK_TOP, s)                       // grass cap
    px(x, top, w, 1.2f, BLOCK_TOP_HI, s)                  // cap highlight
    px(x, top + 4f, w, depth, BLOCK_DIRT, s)              // dirt body
    // block seams so it reads as stacked cubes, not a slab
    var seam = x + 12f
    while (seam < x + w) {
        px(seam, top + 4f, 0.7f, depth, BLOCK_DIRT_D, s)
        seam += 12f
    }
    px(x, top + 16f, w, 0.7f, BLOCK_DIRT_D, s)
    px(x, top + 4f + depth - 1.4f, w, 1.4f, BLOCK_EDGE, s) // bottom shadow
}

/**
 * Blocky runner, ~18 units tall, drawn from the feet up.
 * Original design: square head, tuft of hair, two-tone shirt, boot-cut legs.
 */
private fun DrawScope.drawRunner(
    x: Float,
    groundY: Float,
    scale: Float,
    running: Boolean,
    runPhase: Float,
    jumpP: Float,
    squash: Float
) {
    val s = scale
    val h = 18f * squash
    val feetY = groundY
    val headSize = 6f

    // limb swing
    val swing = if (running) sin(runPhase) * 3.2f else 0f
    val swing2 = if (running) sin(runPhase + Math.PI.toFloat()) * 3.2f else 0f
    val bob = if (running) abs(sin(runPhase)) * 0.7f else 0f

    val bodyTop = feetY - h + bob
    val headTop = bodyTop - headSize

    // ---- LEGS ----
    if (jumpP >= 0f) {
        // tucked in the air: front knee up, back leg trailing
        val tuck = sin(jumpP * Math.PI.toFloat())
        px(x - 1.2f, feetY - 7f - tuck * 2.2f, 2.6f, 6f, PANTS, s)          // back leg
        px(x + 1.6f, feetY - 8f - tuck * 3.4f, 2.6f, 5.4f, PANTS_SHADE, s)  // front knee up
        px(x + 1.6f, feetY - 3.4f - tuck * 3.4f, 3.2f, 1.6f, BLOCK_EDGE, s) // boot
    } else {
        px(x - 1.6f + swing, feetY - 7f, 2.6f, 7f, PANTS_SHADE, s)
        px(x + 0.9f + swing2, feetY - 7f, 2.6f, 7f, PANTS, s)
        px(x - 1.9f + swing, feetY - 1.4f, 3.2f, 1.4f, BLOCK_EDGE, s)
        px(x + 0.7f + swing2, feetY - 1.4f, 3.2f, 1.4f, BLOCK_EDGE, s)
    }

    // ---- TORSO ----
    px(x - 2.2f, bodyTop, 5.6f, 7.4f, SHIRT, s)
    px(x + 1.2f, bodyTop, 2.2f, 7.4f, SHIRT_SHADE, s)     // shaded right side
    px(x - 2.2f, bodyTop + 5.6f, 5.6f, 1.8f, SHIRT_SHADE, s)

    // ---- ARMS ----
    if (jumpP >= 0f) {
        val reach = sin(jumpP * Math.PI.toFloat())
        px(x + 2.6f, bodyTop - 2.4f * reach, 2.2f, 6f, SKIN, s)      // front arm up
        px(x - 4.0f, bodyTop + 1.6f * reach, 2.2f, 5.6f, SKIN_SHADE, s)
    } else {
        px(x + 2.6f, bodyTop + 0.6f + swing2, 2.2f, 6f, SKIN, s)
        px(x - 4.0f, bodyTop + 0.6f + swing, 2.2f, 6f, SKIN_SHADE, s)
    }

    // ---- HEAD ----
    px(x - 3f, headTop, headSize, headSize, SKIN, s)
    px(x + 0.6f, headTop, 2.4f, headSize, SKIN_SHADE, s)     // face shading
    px(x - 3f, headTop, headSize, 1.7f, HAIR, s)             // hair cap
    px(x - 3f, headTop - 0.9f, 4.2f, 1.2f, HAIR, s)          // tuft, wind-blown forward
    // eye, looking where he's going
    px(x + 0.2f, headTop + 2.6f, 1.6f, 1.6f, EYE, s)
    px(x + 1.0f, headTop + 2.9f, 0.9f, 1.1f, PUPIL, s)
}
