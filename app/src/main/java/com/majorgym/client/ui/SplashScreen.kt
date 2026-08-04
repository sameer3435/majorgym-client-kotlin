package com.majorgym.client.ui

import android.net.Uri
import android.view.ViewGroup
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Startup splash: plays the MajorGym launch intro video full-screen, then
 * hands off to the dashboard. Replaces the previous procedural
 * Canvas/Compose "heartbeat" animation.
 *
 * Uses the platform VideoView (no new dependency needed) pointed at the
 * bundled res/raw/intro_video.mp4. onFinished() fires as soon as the video
 * completes playback; MainActivity then cross-fades into the dashboard
 * exactly as it did for the old splash, so no caller changes are needed.
 */
private val SplashBackground = Color(0xFF090E18)

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashBackground),
        factory = { ctx ->
            VideoView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                val uri = Uri.parse("android.resource://${ctx.packageName}/${com.majorgym.client.R.raw.intro_video}")
                setVideoURI(uri)
                setOnPreparedListener { mp ->
                    mp.isLooping = false
                    start()
                }
                setOnCompletionListener {
                    onFinished()
                }
                setOnErrorListener { _, _, _ ->
                    // If playback can't start for any reason, don't strand the
                    // user on a black screen — just proceed to the dashboard.
                    onFinished()
                    true
                }
            }
        },
    )
}
