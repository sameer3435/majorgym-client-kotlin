package com.majorgym.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Weekend
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
 * current streak, a "scan to mark attendance" action, and the full
 * present/absent/rest history since the member joined (capped at
 * [LocalStore.MAX_HISTORY_DAYS], i.e. 1 year). Sundays are always shown as
 * a rest day and never break the streak.
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
        val joiningDate = store.getMember()?.joiningDate ?: LocalDate.now()
        checkedInToday = store.checkedInToday()
        streak = store.currentStreak(joiningDate)
        days = store.attendanceHistory(joiningDate)
        loading = false
        when (scanResult) {
            "marked" -> snackbarHostState.showSnackbar("Attendance marked for today ✅")
            "already" -> snackbarHostState.showSnackbar("You're already checked in today")
        }
        if (scanResult != null) onResultConsumed()
    }

    Scaffold(
        containerColor = ClientColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Attendance", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ClientColors.Background,
                    titleContentColor = ClientColors.OnSurface,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ClientColors.Background)
                .padding(padding),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = ClientColors.Accent,
                )
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.CheckCircle,
                            label = "Today",
                            value = if (checkedInToday) "Present" else "Not marked",
                            color = if (checkedInToday) ClientColors.Success else ClientColors.Warning,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.LocalFireDepartment,
                            label = "Streak",
                            value = "$streak day${if (streak == 1) "" else "s"}",
                            color = ClientColors.LightBlue,
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    PremiumButton(
                        text = "SCAN TO MARK ATTENDANCE",
                        icon = Icons.Rounded.QrCodeScanner,
                        onClick = onScan,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(26.dp))
                    Text(
                        "History since joining",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = ClientColors.OnSurface,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PremiumCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
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
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
) {
    PremiumCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(label, color = ClientColors.Hint, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun HistoryRow(day: LocalDate, status: String) {
    val (icon, label, color) = when (status) {
        "present" -> Triple(Icons.Rounded.CheckCircle, "Present", ClientColors.Success)
        "rest" -> Triple(Icons.Rounded.Weekend, "Rest day", ClientColors.LightBlue)
        else -> Triple(Icons.Rounded.RemoveCircleOutline, "Absent", ClientColors.Hint)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Text(
            day.format(HISTORY_DATE_FORMAT),
            modifier = Modifier.padding(start = 14.dp).weight(1f),
        )
        StatusPill(label, color)
    }
}
