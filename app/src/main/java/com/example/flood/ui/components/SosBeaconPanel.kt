package com.example.flood.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.flood.util.SosAudioMode
import com.example.flood.util.SosStrobeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosBeaconBottomSheet(
    isActive: Boolean,
    isSoundEnabled: Boolean,
    isFlashlightEnabled: Boolean,
    isScreenStrobeEnabled: Boolean,
    audioMode: SosAudioMode,
    strobeMode: SosStrobeMode,
    isPulseHigh: Boolean,
    onToggleBeacon: () -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleFlashlight: (Boolean) -> Unit,
    onToggleScreenStrobe: (Boolean) -> Unit,
    onSelectAudioMode: (SosAudioMode) -> Unit,
    onSelectStrobeMode: (SosStrobeMode) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showFullscreenStrobe by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A),
        modifier = modifier.testTag("sos_beacon_bottom_sheet")
    ) {
        SosBeaconContent(
            isActive = isActive,
            isSoundEnabled = isSoundEnabled,
            isFlashlightEnabled = isFlashlightEnabled,
            isScreenStrobeEnabled = isScreenStrobeEnabled,
            audioMode = audioMode,
            strobeMode = strobeMode,
            isPulseHigh = isPulseHigh,
            onToggleBeacon = onToggleBeacon,
            onToggleSound = onToggleSound,
            onToggleFlashlight = onToggleFlashlight,
            onToggleScreenStrobe = onToggleScreenStrobe,
            onSelectAudioMode = onSelectAudioMode,
            onSelectStrobeMode = onSelectStrobeMode,
            onOpenFullscreenStrobe = { showFullscreenStrobe = true },
            onClose = onDismiss
        )
    }

    if (showFullscreenStrobe) {
        FullscreenStrobeOverlay(
            isPulseHigh = isPulseHigh,
            onDismiss = { showFullscreenStrobe = false }
        )
    }
}

@Composable
fun SosBeaconCard(
    isActive: Boolean,
    isSoundEnabled: Boolean,
    isFlashlightEnabled: Boolean,
    isScreenStrobeEnabled: Boolean,
    audioMode: SosAudioMode,
    strobeMode: SosStrobeMode,
    isPulseHigh: Boolean,
    onToggleBeacon: () -> Unit,
    onOpenFullSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val cardBg = if (isActive) Color(0xFF450A0A) else Color(0xFF1E293B)
    val borderColor = if (isActive) Color(0xFFEF4444) else Color(0xFF334155)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(if (isActive) 2.dp else 1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .testTag("sos_beacon_hero_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isActive) Color(0xFFDC2626) else Color(0xFF475569))
                            .scale(if (isActive) pulseScale else 1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "SOS Sound Beacon & Flashlight Strobe",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isActive) "BEACON BROADCASTING ACTIVE" else "Search & Rescue Locator for Stranded Victims",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isActive) Color(0xFFFCA5A5) else Color(0xFF94A3B8)
                        )
                    }
                }

                if (isActive) {
                    // Visual pulse indicator badge
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (isPulseHigh) Color(0xFFEF4444) else Color(0xFF7F1D1D))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Emits a high-frequency acoustic siren and max-brightness pulsing flashlight pattern to help NDRF, SDRF, and rescue boats locate stranded victims in heavy rain or darkness.",
                fontSize = 12.sp,
                color = Color(0xFFCBD5E1),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Status badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = if (isSoundEnabled && isActive) Color(0xFF4ADE80) else Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = audioMode.frequencyLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0),
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = if (isFlashlightEnabled && isActive) Color(0xFFFBBF24) else Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strobeMode.rateLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onToggleBeacon,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) Color(0xFFDC2626) else Color(0xFFB91C1C)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(44.dp)
                        .testTag("sos_beacon_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.Close else Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isActive) "STOP BEACON" else "START SOS BEACON",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = onOpenFullSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(44.dp)
                        .testTag("sos_beacon_configure_button")
                ) {
                    Text(
                        text = "Customize",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun SosBeaconContent(
    isActive: Boolean,
    isSoundEnabled: Boolean,
    isFlashlightEnabled: Boolean,
    isScreenStrobeEnabled: Boolean,
    audioMode: SosAudioMode,
    strobeMode: SosStrobeMode,
    isPulseHigh: Boolean,
    onToggleBeacon: () -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleFlashlight: (Boolean) -> Unit,
    onToggleScreenStrobe: (Boolean) -> Unit,
    onSelectAudioMode: (SosAudioMode) -> Unit,
    onSelectStrobeMode: (SosStrobeMode) -> Unit,
    onOpenFullscreenStrobe: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "beacon_glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 36.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SOS Sound Beacon & Strobe",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Acoustic SAR Locator & Optical Distress Pattern",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Center Pulsing Activation Circle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background pulsing glow when active
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(glowScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0x66EF4444), Color.Transparent)
                            )
                        )
                )
            }

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) {
                            if (isPulseHigh) Color(0xFFEF4444) else Color(0xFFB91C1C)
                        } else {
                            Color(0xFF1E293B)
                        }
                    )
                    .border(
                        3.dp,
                        if (isActive) Color(0xFFFCA5A5) else Color(0xFF475569),
                        CircleShape
                    )
                    .clickable { onToggleBeacon() }
                    .testTag("sos_beacon_main_circle_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.Warning else Icons.Default.Security,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isActive) "ACTIVE" else "START",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Text(
            text = if (isActive) {
                "🚨 BEACON IS ACTIVE: Piercing siren sounding & flashlight pulsing"
            } else {
                "Tap circle to activate high-frequency siren and max flashlight strobe"
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isActive) Color(0xFFF87171) else Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Toggle row for hardware signals
        Text(
            text = "HARDWARE BEACON CHANNELS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Sound Siren Switch
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = if (isSoundEnabled) Color(0xFF4ADE80) else Color(0xFF64748B),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "High-Frequency Acoustic Siren",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Piercing sound cuts through rain & boat engines",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                Switch(
                    checked = isSoundEnabled,
                    onCheckedChange = onToggleSound,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF16A34A)
                    ),
                    modifier = Modifier.testTag("switch_sos_sound")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Flashlight Torch Switch
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = if (isFlashlightEnabled) Color(0xFFFBBF24) else Color(0xFF64748B),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Camera Flashlight Strobe",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Max-brightness rapid optical pulses",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                Switch(
                    checked = isFlashlightEnabled,
                    onCheckedChange = onToggleFlashlight,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFD97706)
                    ),
                    modifier = Modifier.testTag("switch_sos_flashlight")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Screen Strobe Switch
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LightMode,
                        contentDescription = null,
                        tint = if (isScreenStrobeEnabled) Color(0xFF38BDF8) else Color(0xFF64748B),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Display Screen Strobe",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Flashes device display to alert helicopters/boats",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                Switch(
                    checked = isScreenStrobeEnabled,
                    onCheckedChange = onToggleScreenStrobe,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF0284C7)
                    ),
                    modifier = Modifier.testTag("switch_sos_screen_strobe")
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Audio Siren Mode Selector
        Text(
            text = "ACOUSTIC SIREN FREQUENCY & PATTERN",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SosAudioMode.entries.forEach { mode ->
                val isSelected = audioMode == mode
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color(0xFF1E3A5F) else Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectAudioMode(mode) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Text(
                                text = mode.description,
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = mode.frequencyLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Strobe Flash Pattern Selector
        Text(
            text = "FLASHLIGHT STROBE PATTERN",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SosStrobeMode.entries.forEach { mode ->
                val isSelected = strobeMode == mode
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color(0xFF3B2F17) else Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFFFBBF24) else Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectStrobeMode(mode) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Text(
                                text = mode.description,
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = mode.rateLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFFFBBF24) else Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Full Screen Blinding Strobe Action Button
        Button(
            onClick = onOpenFullscreenStrobe,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0369A1)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("open_fullscreen_strobe_button")
        ) {
            Icon(imageVector = Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Fullscreen Optical Beacon", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Direct Helpline Button
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("sos_dial_112_button")
        ) {
            Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Call Disaster Helpline (112 / 1070)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Search & Rescue Best Practices Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E293B),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Search & Rescue (SAR) Field Protocol",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF38BDF8)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Point rear flashlight towards open sky, river channel, or tree line.\n" +
                            "• Keep device elevated above wet surfaces to avoid acoustic damping.\n" +
                            "• High-frequency audio cuts through torrential downpours and diesel motor noise.\n" +
                            "• If battery is below 20%, switch strobe mode to Energy-Saving Beacon.",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun FullscreenStrobeOverlay(
    isPulseHigh: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val bgColor = if (isPulseHigh) Color.White else Color(0xFFDC2626)
        val textColor = if (isPulseHigh) Color.Black else Color.White

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .clickable { onDismiss() }
                .padding(24.dp)
                .testTag("fullscreen_strobe_view"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🆘 SOS",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    letterSpacing = 4.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "DISASTER SEARCH & RESCUE BEACON",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "HOLD SCREEN UPWARD TOWARDS HELICOPTERS OR RESCUE BOATS\n\nTap anywhere to close",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
