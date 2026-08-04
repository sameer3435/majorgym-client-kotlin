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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Phone
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majorgym.client.data.LocalStore
import com.majorgym.client.data.Member
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SCAN_PROFILE_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

/**
 * Port of `scan_profile_screen.dart`. Scans the "join / renew" QR. Expects a
 * JSON payload with name, phone, id, joiningDate, plus plan info. Expiry is
 * always computed as renewed date + plan duration (see [Member.fromQrJson])
 * rather than trusted blindly from the QR, and the original joining date is
 * carried forward untouched on renewals. Saves it and then shows a
 * confirmation with the saved details plus a direct shortcut into the
 * Attendance section, instead of silently bouncing back to Home.
 */
@Composable
fun ScanProfileScreen(
    onDone: () -> Unit,
    onGoToAttendance: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember { LocalStore.getInstance(context) }
    val handled = remember { booleanArrayOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // Once non-null, the scan succeeded and we show the confirmation card
    // (name/phone/joining/renew/expiry) with a "Go to Attendance" option,
    // instead of auto-navigating away.
    var scannedMember by remember { mutableStateOf<Member?>(null) }

    LaunchedEffect(error) {
        if (error != null) {
            delay(2000)
            handled[0] = false
            error = null
        }
    }

    Scaffold(
        containerColor = ClientColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Scan to Update Membership", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = if (scannedMember == null) onBack else onDone) {
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val confirmed = scannedMember
            if (confirmed == null) {
                QrScannerView(
                    modifier = Modifier.fillMaxSize(),
                    onDetect = { raw ->
                        if (handled[0]) return@QrScannerView
                        handled[0] = true
                        try {
                            val json = JSONObject(raw)
                            // Pass the cached profile (if any) so a renewal scan
                            // carries the original joining date forward instead
                            // of resetting it — see Member.fromQrJson.
                            val existing = store.getMember()
                            val member = Member.fromQrJson(json, existing)
                            if (member.name.isEmpty() || member.id.isEmpty()) {
                                throw IllegalArgumentException("Missing name/id in QR")
                            }
                            store.saveMember(member)
                            scannedMember = member
                        } catch (e: Exception) {
                            error = "That doesn't look like a valid membership QR."
                        }
                    },
                )
                if (error != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 32.dp)
                            .background(ClientColors.Danger, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                    ) {
                        Text(
                            error!!,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                ScanProfileConfirmation(
                    member = confirmed,
                    onGoToAttendance = onGoToAttendance,
                    onBackToHome = onDone,
                )
            }
        }
    }
}

/**
 * Shown right after a successful join/renew scan: a green success mark, the
 * saved member's name/phone/joining/renew/expiry (+ days remaining), and two
 * explicit next steps — jump straight into Attendance, or head back Home.
 */
@Composable
private fun ScanProfileConfirmation(
    member: Member,
    onGoToAttendance: () -> Unit,
    onBackToHome: () -> Unit,
) {
    val active = !member.isExpired
    val statusColor = if (active) ClientColors.Success else ClientColors.Danger

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ClientColors.Success.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = ClientColors.Success,
                        modifier = Modifier.size(34.dp),
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text("Membership updated", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(member.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                StatusPill(if (active) "ACTIVE" else "EXPIRED", statusColor)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    if (active) "${member.daysRemaining} day(s) remaining" else "0 Days Remaining · Renew to regain access",
                    color = ClientColors.Hint,
                )
            }
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                ScanConfirmDivider()
                ScanConfirmRow(Icons.Rounded.Phone, "Phone", member.phone)
                ScanConfirmDivider()
                ScanConfirmRow(Icons.Rounded.Badge, "Member ID", member.id)
                ScanConfirmDivider()
                ScanConfirmRow(Icons.Rounded.EventAvailable, "Joined", member.joiningDate.format(SCAN_PROFILE_DATE_FORMAT))
                ScanConfirmDivider()
                ScanConfirmRow(Icons.Rounded.Autorenew, "Renewed", member.renewedDate.format(SCAN_PROFILE_DATE_FORMAT))
                ScanConfirmDivider()
                ScanConfirmRow(
                    Icons.Rounded.EventBusy,
                    "Expires",
                    member.expiryDate.format(SCAN_PROFILE_DATE_FORMAT),
                    valueColor = statusColor,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        PremiumButton(
            text = "GO TO ATTENDANCE",
            icon = Icons.Rounded.FactCheck,
            onClick = onGoToAttendance,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(14.dp))
        PremiumOutlinedButton(
            text = "BACK TO HOME",
            icon = Icons.Rounded.Home,
            onClick = onBackToHome,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ScanConfirmDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ClientColors.Divider),
    )
}

@Composable
private fun ScanConfirmRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = ClientColors.OnSurface,
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
