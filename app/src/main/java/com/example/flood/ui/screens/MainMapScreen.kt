package com.example.flood.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.flood.ui.components.MapView
import com.example.flood.ui.components.ReportIncidentSheet
import com.example.flood.ui.components.SettingsDialog
import com.example.flood.ui.components.SimulationBar
import com.example.flood.ui.components.WeatherAlertBanner
import com.example.flood.viewmodel.FloodViewModel

@Composable
fun MainMapScreen(
    viewModel: FloodViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val weather by viewModel.weatherState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showReportSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showLayersMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearInfoMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Fullscreen Leaflet WebView
        MapView(
            mapDataJson = viewModel.getMapJsonPayload(),
            onMapClick = { lat, lng ->
                viewModel.onMapClick(lat, lng)
            },
            onDirections = { lat, lng ->
                val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Emergency Location)")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
                    context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                }
            }
        )

        // Top Overlay Header: Simulation playback bar and Weather Alert Card
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            SimulationBar(
                simulations = viewModel.simulations,
                activeSimulation = uiState.selectedSimulation,
                onSelectSimulation = { simId ->
                    viewModel.setSimulation(simId)
                }
            )

            WeatherAlertBanner(
                weather = weather,
                alertRadiusKm = uiState.alertRadiusKm,
                isSimulationActive = uiState.selectedSimulation != null
            )
        }

        // Pinned Location Action Card (appears when user clicks map)
        AnimatedVisibility(
            visible = uiState.isCustomLocationPicked && uiState.pickedLocation != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
                .padding(horizontal = 16.dp)
        ) {
            uiState.pickedLocation?.let { loc ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Location Selected",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Lat: ${String.format("%.4f", loc.first)}, Lng: ${String.format("%.4f", loc.second)}",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.clearPickedLocation() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Clear", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { showReportSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("report_pinned_location_button")
                            ) {
                                Text("Report Here", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Side Controls (Recenter, Layers, Settings)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Layer Toggles Menu Button
            SmallFloatingActionButton(
                onClick = { showLayersMenu = !showLayersMenu },
                containerColor = Color.White,
                contentColor = Color(0xFF0F172A),
                modifier = Modifier.testTag("layer_toggle_button")
            ) {
                Icon(imageVector = Icons.Default.Layers, contentDescription = "Toggle Layers")
            }

            // Recenter GPS Button
            SmallFloatingActionButton(
                onClick = {
                    viewModel.setUserLocation(30.7268, 78.4350)
                },
                containerColor = Color.White,
                contentColor = Color(0xFF0284C7),
                modifier = Modifier.testTag("recenter_button")
            ) {
                Icon(imageVector = Icons.Default.MyLocation, contentDescription = "Recenter on Uttarkashi")
            }

            // Settings Button
            SmallFloatingActionButton(
                onClick = { showSettingsDialog = true },
                containerColor = Color.White,
                contentColor = Color(0xFF475569),
                modifier = Modifier.testTag("map_settings_button")
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
            }
        }

        // Layer toggles popup card
        if (showLayersMenu) {
            Card(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 70.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Map Layers",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LayerToggleRow(
                        label = "Incidents (⚠️)",
                        checked = uiState.showIncidents,
                        onToggle = { viewModel.toggleShowIncidents() }
                    )
                    LayerToggleRow(
                        label = "Emergency Services (🏥)",
                        checked = uiState.showEmergency,
                        onToggle = { viewModel.toggleShowEmergency() }
                    )
                    LayerToggleRow(
                        label = "Alert Radius Buffer (⭕)",
                        checked = uiState.showRiskZone,
                        onToggle = { viewModel.toggleShowRiskZone() }
                    )
                }
            }
        }

        // Primary FAB: Report Disaster / Incident
        FloatingActionButton(
            onClick = { showReportSheet = true },
            containerColor = Color(0xFFDC2626),
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp, end = 16.dp)
                .testTag("fab_report_incident")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Report Incident", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        // Snackbar host for notifications
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        )
    }

    // Bottom Sheet: Report Incident
    if (showReportSheet) {
        ReportIncidentSheet(
            onDismiss = { showReportSheet = false },
            onSubmit = { type, note, severity ->
                viewModel.addIncident(type, note, null, null, severity)
                showReportSheet = false
            },
            pickedLocation = uiState.pickedLocation,
            userLocation = Pair(uiState.userLat, uiState.userLng)
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            currentRadiusKm = uiState.alertRadiusKm,
            currentYellowThreshold = uiState.yellowThresholdMm,
            currentRedThreshold = uiState.redThresholdMm,
            onSave = { r, y, red ->
                viewModel.setAlertRadius(r)
                viewModel.setThresholds(y, red)
            },
            onResetData = {
                viewModel.resetToDefaults()
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun LayerToggleRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(8.dp),
        color = if (checked) Color(0xFFE0F2FE) else Color(0xFFF1F5F9),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal,
                color = if (checked) Color(0xFF0284C7) else Color(0xFF475569)
            )
            Text(
                text = if (checked) "ON" else "OFF",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (checked) Color(0xFF0284C7) else Color(0xFF94A3B8),
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
