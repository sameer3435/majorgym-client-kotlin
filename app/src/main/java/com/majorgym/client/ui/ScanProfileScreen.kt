package com.majorgym.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.majorgym.client.data.LocalStore
import com.majorgym.client.data.Member
import kotlinx.coroutines.delay
import org.json.JSONObject

/**
 * Port of `scan_profile_screen.dart`. Scans the "join / renew" QR. Expects a
 * JSON payload with name, phone, id, joiningDate, plus plan info. Expiry is
 * computed from joining date + plan duration (see [Member.fromQrJson])
 * rather than trusted blindly from the QR. Saves it and replaces whatever
 * profile was cached before.
 */
@Composable
fun ScanProfileScreen(onDone: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { LocalStore.getInstance(context) }
    val handled = remember { booleanArrayOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(error) {
        if (error != null) {
            delay(2000)
            handled[0] = false
            error = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan to Update Membership") },
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
                    handled[0] = true
                    try {
                        val json = JSONObject(raw)
                        val member = Member.fromQrJson(json)
                        if (member.name.isEmpty() || member.id.isEmpty()) {
                            throw IllegalArgumentException("Missing name/id in QR")
                        }
                        store.saveMember(member)
                        onDone()
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
                        .background(Color(0xFFD32F2F), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                ) {
                    Text(
                        error!!,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
