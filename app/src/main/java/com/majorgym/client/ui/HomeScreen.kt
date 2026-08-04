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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CardMembership
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majorgym.client.data.LocalStore
import com.majorgym.client.data.Member
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

/**
 * Home page: shows the cached member profile (or a "no membership scanned
 * yet" placeholder), a button to view Attendance, and a button that opens
 * the membership QR scanner as a full-screen overlay dialog right here on
 * this page (see [MembershipScanDialog]) — scanning never leaves Home.
 */
@Composable
fun HomeScreen(
    onOpenAttendance: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember { LocalStore.getInstance(context) }
    var member by remember { mutableStateOf<Member?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showScanDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loading = true
        member = store.getMember()
        loading = false
    }

    // member.daysRemaining / member.isExpired are computed live from
    // LocalDate.now() (see Member.kt) rather than stored, so they're always
    // correct on a fresh load. But if the app is simply left open across
    // midnight, nothing else would trigger a recomposition to re-read
    // "today" — this ticks once at each local midnight (checking hourly as
    // a safety net) so the remaining-days count and ACTIVE/EXPIRED status
    // update automatically without needing to relaunch the app.
    var dayTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalDateTime.now()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
            val waitMs = Duration.between(now, nextMidnight).toMillis().coerceIn(1_000L, 60 * 60 * 1_000L)
            delay(waitMs)
            dayTick++
        }
    }

    Scaffold(
        containerColor = ClientColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "MajorGym",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
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
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    ShimmerBox(modifier = Modifier.fillMaxWidth().height(300.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    ShimmerBox(modifier = Modifier.fillMaxWidth().height(58.dp))
                    Spacer(modifier = Modifier.height(14.dp))
                    ShimmerBox(modifier = Modifier.fillMaxWidth().height(58.dp))
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    val m = member
                    if (m == null) {
                        NoMembershipCard()
                    } else {
                        // Keyed on dayTick so the card (and its
                        // daysRemaining/isExpired reads) recomposes fresh
                        // at each local midnight, not just on re-entry.
                        key(dayTick) {
                            ProfileCard(m)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    PremiumButton(
                        text = "VIEW ATTENDANCE",
                        icon = Icons.Rounded.FactCheck,
                        onClick = onOpenAttendance,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    PremiumOutlinedButton(
                        text = "SCAN TO UPDATE MEMBERSHIP",
                        icon = Icons.Rounded.QrCodeScanner,
                        onClick = { showScanDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (showScanDialog) {
        MembershipScanDialog(
            onDismiss = { showScanDialog = false },
            onSaved = { updated -> member = updated },
            onGoToAttendance = {
                showScanDialog = false
                onOpenAttendance()
            },
        )
    }
}

@Composable
private fun NoMembershipCard() {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(ClientColors.Accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = ClientColors.LightBlue,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("No membership scanned yet", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(modifier = Modifier.height(6.dp))
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
    val statusColor = if (active) ClientColors.Success else ClientColors.Danger

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            ClientColors.Primary.copy(alpha = 0.28f),
                            ClientColors.Surface,
                        )
                    )
                )
                .padding(vertical = 30.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(ClientColors.Primary, ClientColors.Accent))
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(initial, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = ClientColors.OnSurface)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(member.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                StatusPill(if (active) "ACTIVE" else "EXPIRED", statusColor)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (active) "${member.daysRemaining} day(s) remaining" else "0 Days Remaining · Renew to regain access",
                    color = ClientColors.Hint,
                )
            }
        }
        Column(modifier = Modifier.padding(20.dp)) {
            // NOTE: Member ID is intentionally NOT shown — it's an internal
            // key, not something the member needs to see, and as a long
            // UUID it was also overflowing this card's layout.
            InfoRow(Icons.Rounded.Phone, "Phone", member.phone)
            InfoDivider()
            InfoRow(Icons.Rounded.EventAvailable, "Joined", member.joiningDate.format(DATE_FORMAT))
            InfoDivider()
            InfoRow(Icons.Rounded.Autorenew, "Renewed", member.renewedDate.format(DATE_FORMAT))
            InfoDivider()
            InfoRow(
                Icons.Rounded.EventBusy,
                "Expires",
                member.expiryDate.format(DATE_FORMAT),
                valueColor = statusColor,
            )
            if (member.planLabel.isNotEmpty()) {
                InfoDivider()
                InfoRow(Icons.Rounded.CardMembership, "Plan", member.planLabel)
            }
        }
    }
}

@Composable
private fun InfoDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ClientColors.Divider),
    )
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = ClientColors.OnSurface,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(ClientColors.Accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = ClientColors.LightBlue, modifier = Modifier.size(18.dp))
        }
        Text(
            label,
            color = ClientColors.Hint,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
        )
        Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = valueColor)
    }
}
