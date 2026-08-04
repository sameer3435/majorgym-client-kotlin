package com.majorgym.client.ui

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
import com.majorgym.client.data.LocalStore
import com.majorgym.client.data.Member
import kotlinx.coroutines.delay
import org.json.JSONObject

/**
 * Port of `scan_profile_screen.dart`. Scans the "join / renew" QR. Expects a
 * JSON payload with name, phone, id, joiningDate, plus plan info. Expiry is
 * always computed as renewed date + plan duration (see [Member.fromQrJson])
 * rather than trusted blindly from the QR, and the original joining date is
 * carried forward untouched on renewals. Saves it and replaces whatever
 * profile was cached before.
 */
@Composable
fun ScanProfileScreen(onDone: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { LocalStore.getInstance(context) }
    val handled = remember { booleanArrayOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        if (error != null) {
            delay(2000)
            handled[0] = false
            error = null
        }
    }

    // Brief success animation before handing back to the caller — purely
    // cosmetic, the profile is already saved by the time this fires.
    LaunchedEffect(success) {
        if (success) {
            delay(450)
            onDone()
        }
    }

    Scaffold(
        containerColor = ClientColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Scan to Update Membership", fontWeight = FontWeight.Bold) },
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                        success = true
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
            if (success) {
                SuccessCheckOverlay("Membership updated")
            }
        }
    }
}
