package com.example.flood.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsDialog(
    currentRadiusKm: Double,
    currentYellowThreshold: Double,
    currentRedThreshold: Double,
    onSave: (radiusKm: Double, yellowThreshold: Double, redThreshold: Double) -> Unit,
    onResetData: () -> Unit,
    onDismiss: () -> Unit
) {
    var radius by remember { mutableFloatStateOf(currentRadiusKm.toFloat()) }
    var yellow by remember { mutableFloatStateOf(currentYellowThreshold.toFloat()) }
    var red by remember { mutableFloatStateOf(currentRedThreshold.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Disaster Alert Thresholds",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBAE6FD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Calibrate risk triggers, detection perimeter, and rainfall danger levels for automatic emergency alerts.",
                            fontSize = 12.sp,
                            color = Color(0xFF0369A1)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Alert Radius Slider
                Text(
                    text = "Monitoring Alert Radius: ${radius.toInt()} km",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFF1E293B)
                )
                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 1f..15f,
                    steps = 13,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF0284C7),
                        activeTrackColor = Color(0xFF0284C7)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Yellow Warning Threshold Slider
                Text(
                    text = "Rainfall Warning Threshold: ${String.format("%.1f", yellow)} mm/h",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFFD97706)
                )
                Slider(
                    value = yellow,
                    onValueChange = { yellow = it },
                    valueRange = 1f..15f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFD97706),
                        activeTrackColor = Color(0xFFD97706)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Red Critical Threshold Slider
                Text(
                    text = "Rainfall Danger Threshold: ${String.format("%.1f", red)} mm/h",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFFDC2626)
                )
                Slider(
                    value = red,
                    onValueChange = { red = it },
                    valueRange = 5f..30f,
                    steps = 25,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFDC2626),
                        activeTrackColor = Color(0xFFDC2626)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Reset Data Button
                OutlinedButton(
                    onClick = {
                        onResetData()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Seed Data & Incidents", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(radius.toDouble(), yellow.toDouble(), red.toDouble())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF64748B))
            }
        }
    )
}
