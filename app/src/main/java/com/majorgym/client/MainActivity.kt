package com.majorgym.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.majorgym.client.ui.AttendanceScreen
import com.majorgym.client.ui.HomeScreen
import com.majorgym.client.ui.MajorGymClientTheme
import com.majorgym.client.ui.ScanAttendanceScreen
import com.majorgym.client.ui.ScanProfileScreen

/**
 * Single-activity app using manual sealed-class navigation, matching the
 * owner app's MainActivity pattern (a `when` over a [Screen] state variable —
 * no Navigation-Compose dependency).
 *
 * Screen graph (matches app.dart's Navigator.push flow):
 *   Home --(scan)--> ScanProfile --(saved)--> back to Home (profile refreshed)
 *   Home --(view attendance)--> Attendance --(scan)--> ScanAttendance
 *     --(marked/already)--> back to Attendance
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MajorGymClientTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.Home) }
                // Bumped whenever Home should re-read the cached member
                // profile (i.e. right after a successful profile scan).
                var homeRefreshKey by remember { mutableIntStateOf(0) }
                // One-shot result ("marked" | "already") shown as a Snackbar
                // by AttendanceScreen, mirroring the Flutter screen's
                // SnackBar shown on return from ScanAttendanceScreen.
                var attendanceScanResult by remember { mutableStateOf<String?>(null) }

                Surface(modifier = Modifier.fillMaxSize()) {
                    when (screen) {
                        Screen.Home -> HomeScreen(
                            refreshKey = homeRefreshKey,
                            onOpenAttendance = { screen = Screen.Attendance },
                            onScanProfile = { screen = Screen.ScanProfile },
                        )

                        Screen.ScanProfile -> ScanProfileScreen(
                            onDone = {
                                homeRefreshKey++
                                screen = Screen.Home
                            },
                            onBack = { screen = Screen.Home },
                        )

                        Screen.Attendance -> AttendanceScreen(
                            scanResult = attendanceScanResult,
                            onResultConsumed = { attendanceScanResult = null },
                            onScan = { screen = Screen.ScanAttendance },
                            onBack = { screen = Screen.Home },
                        )

                        Screen.ScanAttendance -> ScanAttendanceScreen(
                            onDone = { result ->
                                attendanceScanResult = result
                                screen = Screen.Attendance
                            },
                            onBack = { screen = Screen.Attendance },
                        )
                    }
                }
            }
        }
    }
}
