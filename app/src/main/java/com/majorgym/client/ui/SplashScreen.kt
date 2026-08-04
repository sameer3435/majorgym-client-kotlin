package com.majorgym.client.ui

import android.media.MediaPlayer
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majorgym.client.R
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Premium Gen-Z-inspired startup splash. Pure Compose, no MotionLayout/Lottie —
 * this app has no XML view hierarchy, so a Canvas + Animatable-driven timeline
 * gives the same 60fps result without pulling in a second UI toolkit.
 *
 * Timeline (single source of truth: [elapsedMs], advanced once per frame via
 * withFrameMillis so every stage below is a pure function of elapsed time):
 *   0     - 500ms   glowing center pulse ("heartbeat")
 *   500   - 1500ms  pulse resolves into the icon mark + wordmark, with glow
 *   1500  - 2500ms  "Train Hard." / "Stay Consistent." / "Become Unstoppable."
 *   2500  - 3500ms  logo settle-enlarge + expanding ring + light sweep
 *   ~3600ms         onFinished() fires; caller cross-fades into the dashboard
 *
 * Reuses the app's own "M" mark (see ic_launcher_foreground.xml) so the splash
 * logo is the real brand mark, not a placeholder.
 */
private object SplashColors {
    val Background = Color.Black
    val ElectricBlue = Color(0xFF00D9FF)
    val ElectricBlueGlow = Color(0xFF6FF0FF)
}

private val BackOutEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

private const val PULSE_END = 500
private const val LOGO_END = 1500
private const val TEXT_END = 2500
private const val RING_END = 3500
private const val FINISH_AT = 3600

private fun stageProgress(elapsed: Int, start: Int, end: Int): Float {
    if (elapsed <= start) return 0f
    if (elapsed >= end) return 1f
    return (elapsed - start).toFloat() / (end - start).toFloat()
}

/** The app's own mark from ic_launcher_foreground.xml, as a reusable Path. */
private fun majorGymMarkPath(): Path = Path().apply {
    fillType = PathFillType.NonZero
    moveTo(30f, 74f)
    lineTo(30f, 34f)
    lineTo(42f, 34f)
    lineTo(54f, 58f)
    lineTo(66f, 34f)
    lineTo(78f, 34f)
    lineTo(78f, 74f)
    lineTo(68f, 74f)
    lineTo(68f, 50f)
    lineTo(56f, 74f)
    lineTo(52f, 74f)
    lineTo(40f, 50f)
    lineTo(40f, 74f)
    close()
}

private data class Particle(val seedX: Float, val seedY: Float, val speed: Float, val phase: Float, val radius: Float)

private fun buildParticles(count: Int): List<Particle> {
    val rnd = Random(42)
    return List(count) {
        Particle(
            seedX = rnd.nextFloat(),
            seedY = rnd.nextFloat(),
            speed = 0.4f + rnd.nextFloat() * 0.6f,
            phase = rnd.nextFloat() * 6.28f,
            radius = 1.5f + rnd.nextFloat() * 2f,
        )
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var elapsedMs by remember { mutableFloatStateOf(0f) }
    var hapticFired by remember { mutableStateOf(false) }
    var finishedCalled by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val particles = remember { buildParticles(18) }
    val context = LocalContext.current

    // Splash startup sound: fires once as the pulse/logo stage begins,
    // released as soon as it finishes playing (or when the composable leaves).
    DisposableEffect(Unit) {
        val player = MediaPlayer.create(context, R.raw.splash_sound)
        player?.setOnCompletionListener { it.release() }
        player?.start()
        onDispose {
            player?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        }
    }

    LaunchedEffect(Unit) {
        val startFrame = withFrameMillis { it }
        while (true) {
            val now = withFrameMillis { it }
            elapsedMs = (now - startFrame).toFloat()
            val elapsed = elapsedMs.toInt()

            if (!hapticFired && elapsed >= LOGO_END) {
                hapticFired = true
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            if (!finishedCalled && elapsed >= FINISH_AT) {
                finishedCalled = true
                onFinished()
                break
            }
        }
    }

    val elapsed = elapsedMs.toInt()

    // --- Stage progress ---
    val pulseP = stageProgress(elapsed, 0, PULSE_END)
    val logoP = BackOutEasing.transform(stageProgress(elapsed, 300, LOGO_END))
    val logoAlpha = min(1f, stageProgress(elapsed, 300, 900) * 1.2f)
    val ringP = EaseOutCubic.transform(stageProgress(elapsed, TEXT_END, RING_END))
    val particleAlpha = stageProgress(elapsed, PULSE_END, LOGO_END) * 0.5f

    val logoScale = 0.25f + logoP * 0.75f + ringP * 0.08f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashColors.Background),
    ) {
        // Ambient drifting particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (particleAlpha > 0f) {
                particles.forEach { p ->
                    val x = p.seedX * size.width
                    val baseY = p.seedY * size.height
                    val drift = sin(elapsedMs / 1000f * p.speed + p.phase) * 14f
                    drawCircle(
                        color = SplashColors.ElectricBlue.copy(alpha = particleAlpha * 0.6f),
                        radius = p.radius,
                        center = Offset(x, baseY + drift),
                    )
                }
            }
        }

        // Center pulse -> logo -> ring, all anchored to the same point
        Box(
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.Center),
        ) {
            // 1) Heartbeat pulse (0-500ms), fades out as the logo takes over
            if (elapsed < LOGO_END) {
                val pulseFade = 1f - stageProgress(elapsed, PULSE_END, 900)
                Canvas(modifier = Modifier.height(120.dp)) {
                    val r = 6.dp.toPx() + pulseP * 10.dp.toPx()
                    drawCircle(
                        color = SplashColors.ElectricBlueGlow.copy(alpha = pulseFade * (0.5f + 0.5f * sin(pulseP * 3.14f))),
                        radius = r,
                    )
                }
            }

            // 2) Logo mark + soft glow + expanding ring + light sweep
            if (elapsed >= 300) {
                Box(
                    modifier = Modifier.graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                        alpha = logoAlpha
                        transformOrigin = TransformOrigin.Center
                    },
                ) {
                    Canvas(modifier = Modifier.height(96.dp)) {
                        val markPath = majorGymMarkPath()
                        val bounds = markPath.getBounds()
                        val scaleToFit = (72.dp.toPx()) / bounds.height
                        val dx = -bounds.left * scaleToFit - (bounds.width * scaleToFit) / 2f
                        val dy = -bounds.top * scaleToFit - (bounds.height * scaleToFit) / 2f

                        // Soft glow behind the mark
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    SplashColors.ElectricBlue.copy(alpha = 0.35f),
                                    SplashColors.ElectricBlue.copy(alpha = 0f),
                                ),
                                radius = 70.dp.toPx(),
                            ),
                            radius = 70.dp.toPx(),
                        )

                        // Expanding ring (2.5s-3.5s)
                        if (ringP > 0f) {
                            val ringRadius = 44.dp.toPx() + ringP * 40.dp.toPx()
                            drawCircle(
                                color = SplashColors.ElectricBlue.copy(alpha = (1f - ringP) * 0.9f),
                                radius = ringRadius,
                                style = Stroke(width = 1.5.dp.toPx()),
                            )
                        }

                        translate(left = dx + size.width / 2f, top = dy + size.height / 2f) {
                            scale(scale = scaleToFit, pivot = Offset.Zero) {
                                drawPath(path = markPath, color = SplashColors.ElectricBlue)
                            }
                        }

                        // Light sweep across the mark while the ring is active
                        if (ringP in 0.01f..0.99f) {
                            rotate(degrees = 20f) {
                                val sweepX = -size.width + ringP * size.width * 2.5f
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.5f),
                                            Color.Transparent,
                                        ),
                                        start = Offset(sweepX, 0f),
                                        end = Offset(sweepX + 40f, size.height),
                                    ),
                                    topLeft = Offset(0f, 0f),
                                    size = size,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Wordmark + motivational lines, stacked below center
        Column(
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.Center)
                .padding(top = 96.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        ) {
            if (elapsed >= 700) {
                val wordmarkAlpha = min(1f, stageProgress(elapsed, 700, 1300) * 1.1f)
                Text(
                    text = "MAJORGYM",
                    color = SplashColors.ElectricBlue.copy(alpha = wordmarkAlpha),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 6.sp,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .graphicsLayer { alpha = wordmarkAlpha },
                )
            }

            val phrases = listOf("Train Hard.", "Stay Consistent.", "Become Unstoppable.")
            val phraseWindow = (TEXT_END - LOGO_END) // 1000ms total, staggered
            phrases.forEachIndexed { index, phrase ->
                val start = LOGO_END + index * (phraseWindow / phrases.size)
                val end = start + phraseWindow / phrases.size + 300
                val p = EaseOutCubic.transform(stageProgress(elapsed, start, end))
                if (p > 0f) {
                    Text(
                        text = phrase,
                        color = Color.White.copy(alpha = p),
                        fontSize = 15.sp,
                        fontWeight = if (index == 2) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = if (index == 0) 20.dp else 2.dp)
                            .graphicsLayer {
                                translationY = (1f - p) * 18f
                            },
                    )
                }
            }
        }
    }
}
