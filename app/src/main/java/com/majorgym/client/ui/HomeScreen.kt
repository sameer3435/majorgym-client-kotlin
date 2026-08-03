package com.majorgym.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majorgym.client.data.LocalStore
import com.majorgym.client.data.Member
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

/**
 * Port of `home_screen.dart`. Shows the cached member profile (or a
 * "no membership scanned yet" placeholder), plus buttons to view attendance
 * and to scan a join/renew QR. [refreshKey] is bumped by the caller whenever
 * this screen is navigated back to, so it re-reads from [LocalStore].
 */
@Composable
fun HomeScreen(
    refreshKey: Int,
    onOpenAttendance: () -> Unit,
    onScanProfile: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember { LocalStore.getInstance(context) }
    var member by remember { mutableStateOf<Member?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(refreshKey) {
        loading = true
        member = store.getMember()
        loading = false
    }

    Scaffold(topBar = { TopAppBar(title = { Text("MajorGym") }) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    val m = member
                    if (m == null) {
                        NoMembershipCard()
                    } else {
                        ProfileCard(m)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onOpenAttendance,
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                    ) {
                        Icon(Icons.Filled.FactCheck, contentDescription = null)
                        Text(
                            "  VIEW ATTENDANCE",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = onScanProfile,
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                        Text(
                            "  SCAN TO UPDATE MEMBERSHIP",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoMembershipCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Filled.PersonOutline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("No membership scanned yet", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Use \"Scan to Update Membership\" below to get started.",
                textAlign = TextAlign.Center,
                color = ClientColors.Hint,
            )
        }
    }
}

@Composable
private fun ProfileCard(member: Member) {
    val active = !member.isExpired
    val initial = if (member.name.isNotEmpty()) member.name.first().uppercaseChar().toString() else "?"

    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                ClientColors.Accent.copy(alpha = 0.25f),
                                MaterialTheme.colorScheme.surface,
                            )
                        )
                    )
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(ClientColors.Accent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(initial, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = ClientColors.Accent)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(member.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                (if (active) ClientColors.Success else ClientColors.Danger).copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                    ) {
                        Text(
                            if (active) "ACTIVE" else "EXPIRED",
                            fontWeight = FontWeight.Bold,
                            color = if (active) ClientColors.Success else ClientColors.Danger,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        if (active) "${member.daysRemaining} day(s) remaining" else "Renew to regain access",
                        color = ClientColors.Hint,
                    )
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow(Icons.Filled.Phone, "Phone", member.phone)
                InfoRow(Icons.Filled.Badge, "ID", member.id)
                InfoRow(Icons.Filled.EventAvailable, "Joined", member.joiningDate.format(DATE_FORMAT))
                InfoRow(Icons.Filled.EventBusy, "Expires", member.expiryDate.format(DATE_FORMAT))
                if (member.planLabel.isNotEmpty()) {
                    InfoRow(Icons.Filled.CardMembership, "Plan", member.planLabel)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = ClientColors.Hint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(0.dp))
        Text(
            label,
            color = ClientColors.Hint,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
        )
        Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}
