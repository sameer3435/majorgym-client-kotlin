package com.majorgym.client.ui

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.majorgym.client.R
import com.majorgym.client.data.LocalStore
import kotlinx.coroutines.delay

/**
 * The exact text content encoded in the gym's official attendance QR.
 * A scanned code must match this EXACTLY (after trimming whitespace) for
 * attendance to be marked.
 */
private const val OFFICIAL_ATTENDANCE_QR_VALUE = "MAJOR_GYM_ATTENDANCE_2026"

/**
 * Full-screen overlay (not a separate page) for scanning the gym's static
 * attendance QR. A scan only marks today present if its content exactly
 * matches [OFFICIAL_ATTENDANCE_QR_VALUE]; any other QR is rejected and the
 * user stays on the scanner to try again. Shown directly on top of the
 * Attendance page — [onDone] hands back the result so the page can update
 * its stats in place, no navigation involved.
 */
@Composable
fun AttendanceScanDialog(onDismiss: () -> Unit, onDone: (result: String) -> Unit) {
    val context = LocalContext.current
    val store = remember { LocalStore.getInstance(context) }
    val handled = remember { booleanArrayOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var invalidQr by remember { mutableStateOf(false) }

    // Brief success animation before handing back to the caller — the
    // attendance record is already written by the time this fires.
    LaunchedEffect(result) {
        val r = result
        if (r != null) {
            delay(450)
            onDone(r)
        }
    }

    // Let the user try again after an invalid-QR message is shown.
    LaunchedEffect(invalidQr) {
        if (invalidQr) {
            delay(2000)
            handled[0] = false
            invalidQr = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Scaffold(
            containerColor = ClientColors.Background,
            topBar = {
                TopAppBar(
                    title = { Text("Scan to Mark Attendance", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ClientColors.Background,
                        titleContentColor = ClientColors.OnSurface,
                    ),
                )
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                QrScannerView(
                    modifier = Modifier.fillMaxSize(),
                    onDetect = { raw ->
                        if (handled[0]) return@QrScannerView
                        if (raw.isEmpty()) return@QrScannerView
                        handled[0] = true
                        if (raw.trim() != OFFICIAL_ATTENDANCE_QR_VALUE) {
                            invalidQr = true
                            return@QrScannerView
                        }
                        val marked = store.markAttendanceToday()
                        if (marked) {
                            try {
                                MediaPlayer.create(context, R.raw.attendance_success_sound)?.apply {
                                    setOnCompletionListener { mp -> mp.release() }
                                    start()
                                }
                            } catch (_: Exception) {
                                // Never let sound playback failures block attendance marking.
                            }
                        }
                        result = if (marked) "marked" else "already"
                    },
                )
                if (result != null) {
                    SuccessCheckOverlay(if (result == "marked") "Attendance marked" else "Already checked in")
                }
                if (invalidQr) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 32.dp)
                            .background(ClientColors.Danger, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                    ) {
                        Text(
                            "Invalid Major Gym QR Code",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
