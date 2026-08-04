package com.majorgym.client.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared premium building blocks used across every screen so the whole app
 * reads as one consistent design system: blue-gradient pill buttons, glass
 * outline buttons, glassmorphism cards, a loading shimmer, and a green
 * success-check animation for QR scans. Nothing here changes any screen's
 * behavior/callbacks — purely presentational.
 */

private const val ANIM_DURATION_MS = 300

/** Primary action button: blue gradient, pill shape, soft glow, press-scale. */
@Composable
fun PremiumButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "primary-button-scale",
    )

    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .height(58.dp)
            .scale(scale)
            .shadow(
                elevation = if (pressed) 4.dp else 14.dp,
                shape = RoundedCornerShape(29.dp),
                ambientColor = ClientColors.Primary.copy(alpha = 0.55f),
                spotColor = ClientColors.Primary.copy(alpha = 0.55f),
            )
            .clip(RoundedCornerShape(29.dp))
            .background(Brush.horizontalGradient(listOf(ClientColors.Primary, ClientColors.Accent))),
        shape = RoundedCornerShape(29.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.White.copy(alpha = 0.5f),
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
    ) {
        Icon(icon, contentDescription = null)
        Text("  $text", fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
    }
}

/** Secondary action button: blue outline, glass fill, rounded corners. */
@Composable
fun PremiumOutlinedButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "outlined-button-scale",
    )

    OutlinedButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .height(58.dp)
            .scale(scale)
            .clip(RoundedCornerShape(29.dp))
            .background(ClientColors.Accent.copy(alpha = 0.10f)),
        border = BorderStroke(1.5.dp, ClientColors.Accent.copy(alpha = 0.65f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ClientColors.LightBlue),
        shape = RoundedCornerShape(29.dp),
    ) {
        Icon(icon, contentDescription = null)
        Text("  $text", fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
    }
}

/**
 * Glassmorphism card: 22dp radius, soft blue glow, subtle divider border,
 * and a smooth elevation-in animation the first time it's composed.
 */
@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { settled = true }
    val elevation by animateDpAsState(
        targetValue = if (settled) 10.dp else 0.dp,
        animationSpec = tween(ANIM_DURATION_MS),
        label = "card-elevation",
    )
    val alpha by animateFloatAsState(
        targetValue = if (settled) 1f else 0f,
        animationSpec = tween(ANIM_DURATION_MS),
        label = "card-alpha",
    )

    Card(
        modifier = modifier
            .alpha(alpha)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(22.dp),
                ambientColor = ClientColors.Accent.copy(alpha = 0.35f),
                spotColor = ClientColors.Accent.copy(alpha = 0.35f),
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ClientColors.Surface),
        border = BorderStroke(1.dp, ClientColors.Divider),
    ) {
        Column(content = content)
    }
}

/** Small rounded status/badge pill, e.g. ACTIVE / EXPIRED / Present / Absent. */
@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 14.dp, vertical = 5.dp),
    ) {
        Text(text, fontWeight = FontWeight.Bold, color = color, fontSize = 13.sp, letterSpacing = 0.5.sp)
    }
}

/** Shimmering skeleton block, used while data is loading. */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = -400f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-translate",
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            ClientColors.Divider,
            ClientColors.Accent.copy(alpha = 0.28f),
            ClientColors.Divider,
        ),
        start = Offset(translate, 0f),
        end = Offset(translate + 300f, 300f),
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(brush),
    )
}

/**
 * Full-bleed success overlay with an animated green check — shown briefly
 * after a QR scan succeeds (join/renewal saved, or attendance marked)
 * before handing control back to the caller.
 */
@Composable
fun SuccessCheckOverlay(message: String) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "success-check-scale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(ANIM_DURATION_MS),
        label = "success-check-alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ClientColors.Background.copy(alpha = 0.94f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale).alpha(alpha),
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(ClientColors.Success.copy(alpha = 0.16f))
                    .padding(20.dp),
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = ClientColors.Success,
                    modifier = Modifier.height(56.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
