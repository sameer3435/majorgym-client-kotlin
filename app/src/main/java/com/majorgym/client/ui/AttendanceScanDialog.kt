package com.majorgym.client.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.majorgym.client.data.LocalStore
import kotlinx.coroutines.delay

/**
 * Full-screen overlay (not a separate page) for scanning the gym's static
 * attendance QR. Any successful scan marks today present (once per day);
 * the gym only has one attendance QR, so we don't validate its exact
 * contents. Shown directly on top of the Attendance page — [onDone] hands
 * back the result so the page can update its stats in place, no navigation
 * involved.
 */
@Composable
fun AttendanceScanDialog(onDismiss: () -> Unit, onDone: (result: String) -> Unit) {
    val context = LocalContext.current
    val store = remember { LocalStore.getInstance(context) }
    val handled = remember { booleanArrayOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    // Brief success animation before handing back to the caller — the
    // attendance record is already written by the time this fires.
    LaunchedEffect(result) {
        val r = result
        if (r != null) {
            delay(450)
            onDone(r)
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
                        val marked = store.markAttendanceToday()
                        result = if (marked) "marked" else "already"
                    },
                )
                if (result != null) {
                    SuccessCheckOverlay(if (result == "marked") "Attendance marked" else "Already checked in")
                }
            }
        }
    }
}
