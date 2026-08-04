package com.majorgym.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.majorgym.client.ui.AttendanceScreen
import com.majorgym.client.ui.ClientColors
import com.majorgym.client.ui.HomeScreen
import com.majorgym.client.ui.MajorGymClientTheme
import com.majorgym.client.ui.SplashScreen

/**
 * Single-activity app using manual sealed-class navigation, matching the
 * owner app's MainActivity pattern (a `when` over a [Screen] state variable —
 * no Navigation-Compose dependency).
 *
 * Only two real pages: Home and Attendance. Scanning a QR (either the
 * join/renew QR from Home, or the gym's attendance QR from Attendance) opens
 * as a full-screen overlay dialog directly on top of the current page —
 * it's never pushed as its own page, so there's no separate "scan" screen to
 * get stuck on or navigate out of. Each page owns and refreshes its own
 * scan dialog and state; Home does expose a shortcut straight into
 * Attendance from inside its scan-confirmation dialog.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MajorGymClientTheme {
                var showSplash by remember { mutableStateOf(true) }
                var screen by remember { mutableStateOf<Screen>(Screen.Home) }

                Surface(modifier = Modifier.fillMaxSize(), color = ClientColors.Background) {
                    // Splash owns the first ~3.6s; the 400ms cross-fade below
                    // is the "smooth transition into the dashboard" step
                    // (this Compose BOM predates stable SharedTransitionLayout,
                    // so a fade is the safe premium equivalent).
                    Crossfade(
                        targetState = showSplash,
                        animationSpec = tween(durationMillis = 400),
                        label = "splash-to-dashboard",
                    ) { splashVisible ->
                        if (splashVisible) {
                            SplashScreen(onFinished = { showSplash = false })
                        } else {
                            // Fade + slide between the two pages — added
                            // 250-300ms transition for a premium feel.
                            AnimatedContent(
                                targetState = screen,
                                transitionSpec = {
                                    (fadeIn(tween(280)) + slideInHorizontally(tween(280)) { w -> w / 6 })
                                        .togetherWith(
                                            fadeOut(tween(220)) + slideOutHorizontally(tween(220)) { w -> -w / 6 }
                                        )
                                },
                                label = "screen-transition",
                            ) { target ->
                                when (target) {
                                    Screen.Home -> HomeScreen(
                                        onOpenAttendance = { screen = Screen.Attendance },
                                    )

                                    Screen.Attendance -> AttendanceScreen(
                                        onBack = { screen = Screen.Home },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
