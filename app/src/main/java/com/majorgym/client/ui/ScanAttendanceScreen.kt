package com.majorgym.client.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.majorgym.client.data.LocalStore

/**
 * Port of `scan_attendance_screen.dart`. Scans the gym's static attendance
 * QR — any successful scan marks today present (once per day); the gym only
 * has one attendance QR, so we don't validate its exact contents.
 */
@Composable
fun ScanAttendanceScreen(onDone: (result: String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { LocalStore.getInstance(context) }
    val handled = remember { booleanArrayOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan to Mark Attendance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
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
                    onDone(if (marked) "marked" else "already")
                },
            )
        }
    }
}
