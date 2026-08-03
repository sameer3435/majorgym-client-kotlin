package com.majorgym.client.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majorgym.client.data.LocalStore
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HISTORY_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH)

/**
 * Port of `attendance_screen.dart`. Shows today's check-in status, the
 * current streak, a "scan to mark attendance" action, and the last
 * [LocalStore.HISTORY_DAYS] days of present/absent history.
 */
@Composable
fun AttendanceScreen(scanResult: String?, onResultConsumed: () -> Unit, onScan: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { LocalStore.getInstance(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    var loading by remember { mutableStateOf(true) }
    var checkedInToday by remember { mutableStateOf(false) }
    var streak by remember { mutableIntStateOf(0) }
    var days by remember { mutableStateOf<List<Pair<LocalDate, String>>>(emptyList()) }

    LaunchedEffect(Unit) {
        loading = true
        checkedInToday = store.checkedInToday()
        streak = store.currentStreak()
        days = store.lastTwoMonths()
        loading = false
        when (scanResult) {
            "marked" -> snackbarHostState.showSnackbar("Attendance marked for today ✅")
            "already" -> snackbarHostState.showSnackbar("You're already checked in today")
        }
        if (scanResult != null) onResultConsumed()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Attendance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            label = "Today",
                            value = if (checkedInToday) "Present" else "Not marked",
                            color = if (checkedInToday) ClientColors.Success else androidx.compose.ui.graphics.Color(0xFFFFA726),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        StatCard(
                            modifier = Modifier.weight(1f),
                            label = "Streak",
                            value = "$streak day${if (streak == 1) "" else "s"}",
                            color = ClientColors.Accent,
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onScan,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                        Text("  SCAN TO MARK ATTENDANCE", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Last ${LocalStore.HISTORY_DAYS} days",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        LazyColumn {
                            items(days) { (day, status) ->
                                HistoryRow(day, status)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, color = ClientColors.Hint)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun HistoryRow(day: LocalDate, status: String) {
    val present = status == "present"
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (present) Icons.Filled.CheckCircle else Icons.Filled.RemoveCircleOutline,
            contentDescription = null,
            tint = if (present) ClientColors.Success else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )
        Text(
            day.format(HISTORY_DATE_FORMAT),
            modifier = Modifier.padding(start = 12.dp).weight(1f),
        )
        Text(
            if (present) "Present" else "Absent",
            fontWeight = FontWeight.SemiBold,
            color = if (present) ClientColors.Success else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )
    }
}
