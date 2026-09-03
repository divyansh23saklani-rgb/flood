package com.example.flood.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flood.data.local.AppDatabase
import com.example.flood.data.model.Comment
import com.example.flood.data.model.DisasterSimulation
import com.example.flood.data.model.EmergencyService
import com.example.flood.data.model.Incident
import com.example.flood.data.model.IncidentType
import com.example.flood.data.model.RiskLevel
import com.example.flood.data.model.WeatherRisk
import com.example.flood.data.remote.CloudIncidentSyncManager
import com.example.flood.data.repository.IncidentRepository
import com.example.flood.data.repository.WeatherRepository
import com.example.flood.service.DisasterSyncService
import com.example.flood.util.NotificationHelper
import com.example.flood.util.SosAudioMode
import com.example.flood.util.SosBeaconManager
import com.example.flood.util.SosStrobeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class FloodUiState(
    val userLat: Double = 30.7268,
    val userLng: Double = 78.4350,
    val alertRadiusKm: Double = 3.0,
    val yellowThresholdMm: Double = 5.0,
    val redThresholdMm: Double = 10.0,
    val showIncidents: Boolean = true,
    val showEmergency: Boolean = true,
    val showRiskZone: Boolean = true,
    val isReportMode: Boolean = false,
    val pickedLocation: Pair<Double, Double>? = null,
    val isCustomLocationPicked: Boolean = false,
    val selectedSimulation: DisasterSimulation? = null,
    val isSimulatingRain: Boolean = false,
    val navigationTarget: Pair<Double, Double>? = null,
    val infoMessage: String? = null
)

class FloodViewModel(application: Application) : AndroidViewModel(application) {

    private val incidentRepository: IncidentRepository
    private val weatherRepository = WeatherRepository()
    val sosBeaconManager = SosBeaconManager(application)

    private val _uiState = MutableStateFlow(FloodUiState())
    val uiState: StateFlow<FloodUiState> = _uiState.asStateFlow()

    val isSosBeaconActive: StateFlow<Boolean> = sosBeaconManager.isBeaconActive
    val isSosSoundEnabled: StateFlow<Boolean> = sosBeaconManager.isSoundEnabled
    val isSosFlashlightEnabled: StateFlow<Boolean> = sosBeaconManager.isFlashlightEnabled
    val isSosScreenStrobeEnabled: StateFlow<Boolean> = sosBeaconManager.isScreenStrobeEnabled
    val sosAudioMode: StateFlow<SosAudioMode> = sosBeaconManager.audioMode
    val sosStrobeMode: StateFlow<SosStrobeMode> = sosBeaconManager.strobeMode
    val isSosPulseHigh: StateFlow<Boolean> = sosBeaconManager.isPulseHigh

    private val _weatherState = MutableStateFlow(
        WeatherRisk(
            precipitationMm = 3.2,
            riskLevel = RiskLevel.GREEN,
            advisory = "Normal conditions. River Bhagirathi flowing below warning level.",
            riverWaterLevel = "1124.5 m (Safe)",
            catchmentRainfall = 8.5,
            lastUpdated = System.currentTimeMillis()
        )
    )
    val weatherState: StateFlow<WeatherRisk> = _weatherState.asStateFlow()

    val incidents: StateFlow<List<Incident>>
    val comments: StateFlow<List<Comment>>

    val emergencyServices: List<EmergencyService> = weatherRepository.getEmergencyServices()
    val simulations: List<DisasterSimulation> = weatherRepository.getSimulations()

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        incidentRepository = IncidentRepository(db.incidentDao(), db.commentDao())

        incidents = incidentRepository.allIncidents.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        comments = incidentRepository.allComments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            incidentRepository.seedIfEmpty()
            refreshWeatherData()
            syncRemoteIncidents()
        }

        // Start background disaster mesh synchronization service
        DisasterSyncService.start(application)
    }

    private suspend fun syncRemoteIncidents() {
        val events = CloudIncidentSyncManager.fetchRecentEvents(getApplication())
        for (event in events) {
            when (event) {
                is CloudIncidentSyncManager.RemoteEvent.VerifiedAlert -> {
                    val existing = incidentRepository.getIncidentByCreatedAt(event.incidentCreatedAt)
                    if (existing == null) {
                        val newInc = Incident(
                            type = event.type,
                            note = event.note,
                            lat = event.lat,
                            lng = event.lng,
                            createdAt = event.incidentCreatedAt,
                            severity = event.severity,
                            userReported = true,
                            score = event.upvotes,
                            upvotes = event.upvotes,
                            downvotes = 0,
                            status = "OPEN",
                            userVote = 0,
                            isAlertBroadcasted = true
                        )
                        incidentRepository.insertRemoteIncident(newInc)
                    } else {
                        incidentRepository.applyRemoteVote(
                            createdAt = event.incidentCreatedAt,
                            upvotes = event.upvotes.coerceAtLeast(existing.upvotes),
                            downvotes = existing.downvotes,
                            score = event.upvotes.coerceAtLeast(existing.upvotes) - existing.downvotes
                        )
                        incidentRepository.markAlertBroadcastedByCreatedAt(event.incidentCreatedAt)
                    }

                    NotificationHelper.sendVerifiedHazardAlertNotification(
                        context = getApplication(),
                        typeLabel = IncidentType.fromString(event.type).label,
                        severity = event.severity,
                        locationNote = event.note.ifBlank { "Location: (${String.format(java.util.Locale.US, "%.4f", event.lat)}, ${String.format(java.util.Locale.US, "%.4f", event.lng)})" },
                        verifiedCount = event.upvotes
                    )
                }
                is CloudIncidentSyncManager.RemoteEvent.NewIncident -> {
                    val isNew = incidentRepository.insertRemoteIncident(event.incident)
                    if (isNew && event.incident.upvotes >= 3) {
                        NotificationHelper.sendVerifiedHazardAlertNotification(
                            context = getApplication(),
                            typeLabel = event.incident.incidentType.label,
                            severity = event.incident.severity,
                            locationNote = event.incident.note.ifBlank { "Location: (${String.format(java.util.Locale.US, "%.4f", event.incident.lat)}, ${String.format(java.util.Locale.US, "%.4f", event.incident.lng)})" },
                            verifiedCount = event.incident.upvotes
                        )
                    }
                }
                is CloudIncidentSyncManager.RemoteEvent.NewComment -> {
                    val isNew = incidentRepository.insertRemoteComment(event.comment)
                    if (isNew) {
                        NotificationHelper.sendCommentNotification(
                            context = getApplication(),
                            author = event.comment.authorName,
                            commentText = event.comment.text,
                            hazardType = "Community Hazard Report"
                        )
                    }
                }
                is CloudIncidentSyncManager.RemoteEvent.VoteUpdate -> {
                    val (updated, justReachedThreshold) = incidentRepository.applyRemoteVote(
                        createdAt = event.incidentCreatedAt,
                        upvotes = event.upvotes,
                        downvotes = event.downvotes,
                        score = event.score
                    )
                    if (justReachedThreshold && updated != null) {
                        NotificationHelper.sendVerifiedHazardAlertNotification(
                            context = getApplication(),
                            typeLabel = updated.incidentType.label,
                            severity = updated.severity,
                            locationNote = updated.note.ifBlank { "Location: (${String.format(java.util.Locale.US, "%.4f", updated.lat)}, ${String.format(java.util.Locale.US, "%.4f", updated.lng)})" },
                            verifiedCount = event.upvotes
                        )
                    }
                }
                is CloudIncidentSyncManager.RemoteEvent.StatusUpdate -> {
                    incidentRepository.applyRemoteStatus(
                        createdAt = event.incidentCreatedAt,
                        status = event.status
                    )
                }
            }
        }
    }

    fun triggerSync() {
        viewModelScope.launch(Dispatchers.IO) {
            syncRemoteIncidents()
            _uiState.update { it.copy(infoMessage = "Hazards synced with community network") }
        }
    }

    fun getCommentsForIncident(incidentId: Long, createdAt: Long): Flow<List<Comment>> {
        return incidentRepository.getCommentsForIncident(incidentId, createdAt)
    }

    fun upvoteIncident(id: Long) {
        viewModelScope.launch {
            val (updated, justReachedThreshold) = incidentRepository.toggleUpvote(id)
            if (updated != null) {
                // Sync vote to cloud network
                launch(Dispatchers.IO) {
                    CloudIncidentSyncManager.publishVote(
                        context = getApplication(),
                        incidentCreatedAt = updated.createdAt,
                        upvotes = updated.upvotes,
                        downvotes = updated.downvotes,
                        score = updated.score
                    )
                }

                // 3+ Verifications Threshold Gate: Push critical emergency broadcast when reached
                if (justReachedThreshold) {
                    // Local high-priority alert
                    NotificationHelper.sendVerifiedHazardAlertNotification(
                        context = getApplication(),
                        typeLabel = updated.incidentType.label,
                        severity = updated.severity,
                        locationNote = updated.note.ifBlank { "Location: (${String.format(java.util.Locale.US, "%.4f", updated.lat)}, ${String.format(java.util.Locale.US, "%.4f", updated.lng)})" },
                        verifiedCount = updated.upvotes
                    )

                    // Publish Verified Critical Alert across cloud stream
                    launch(Dispatchers.IO) {
                        CloudIncidentSyncManager.publishVerifiedAlert(
                            context = getApplication(),
                            incident = updated,
                            verifiedCount = updated.upvotes
                        )
                    }

                    _uiState.update {
                        it.copy(
                            infoMessage = "🚨 3+ Community Verifications reached! Emergency alert & siren broadcasted across the network."
                        )
                    }
                } else {
                    val remaining = updated.verificationsRemaining
                    if (remaining > 0) {
                        _uiState.update {
                            it.copy(infoMessage = "Upvoted! (${updated.upvotes}/3 verifications. Needs $remaining more for emergency broadcast)")
                        }
                    }
                }
            }
        }
    }

    fun downvoteIncident(id: Long) {
        viewModelScope.launch {
            val updated = incidentRepository.toggleDownvote(id)
            if (updated != null) {
                launch(Dispatchers.IO) {
                    CloudIncidentSyncManager.publishVote(
                        context = getApplication(),
                        incidentCreatedAt = updated.createdAt,
                        upvotes = updated.upvotes,
                        downvotes = updated.downvotes,
                        score = updated.score
                    )
                }
            }
        }
    }

    fun updateIncidentStatus(id: Long, newStatus: String) {
        viewModelScope.launch {
            val updated = incidentRepository.updateIncidentStatus(id, newStatus)
            if (updated != null) {
                launch(Dispatchers.IO) {
                    CloudIncidentSyncManager.publishStatus(
                        context = getApplication(),
                        incidentCreatedAt = updated.createdAt,
                        status = newStatus
                    )
                }
                _uiState.update { it.copy(infoMessage = "Incident marked as $newStatus") }
            }
        }
    }

    fun addComment(incidentId: Long, incidentCreatedAt: Long, authorName: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val deviceId = CloudIncidentSyncManager.getDeviceId(getApplication())
            val comment = incidentRepository.addComment(
                incidentId = incidentId,
                incidentCreatedAt = incidentCreatedAt,
                authorName = authorName,
                text = text.trim(),
                senderDeviceId = deviceId
            )
            launch(Dispatchers.IO) {
                CloudIncidentSyncManager.publishComment(getApplication(), comment)
            }
            _uiState.update { it.copy(infoMessage = "Comment posted & shared with responders") }
        }
    }

    fun refreshWeatherData() {
        val currentLat = _uiState.value.userLat
        val currentLng = _uiState.value.userLng
        val precip = weatherRepository.calculatePrecipitationNext6h(currentLat, currentLng)
        val weather = weatherRepository.evaluateRisk(
            precipMmPerHour = precip,
            yellowThreshold = _uiState.value.yellowThresholdMm,
            redThreshold = _uiState.value.redThresholdMm
        )
        _weatherState.value = weather
    }

    fun setAlertRadius(radiusKm: Double) {
        _uiState.update { it.copy(alertRadiusKm = radiusKm) }
    }

    fun setThresholds(yellow: Double, red: Double) {
        _uiState.update { it.copy(yellowThresholdMm = yellow, redThresholdMm = red) }
        refreshWeatherData()
    }

    fun setReportMode(enabled: Boolean) {
        _uiState.update { it.copy(isReportMode = enabled) }
    }

    fun toggleShowIncidents() {
        _uiState.update { it.copy(showIncidents = !it.showIncidents) }
    }

    fun toggleShowEmergency() {
        _uiState.update { it.copy(showEmergency = !it.showEmergency) }
    }

    fun toggleShowRiskZone() {
        _uiState.update { it.copy(showRiskZone = !it.showRiskZone) }
    }

    fun setSimulation(simId: String?) {
        if (simId == null) {
            _uiState.update {
                it.copy(
                    selectedSimulation = null,
                    isSimulatingRain = false
                )
            }
            refreshWeatherData()
        } else {
            val sim = simulations.find { it.id == simId }
            if (sim != null) {
                _uiState.update {
                    it.copy(
                        selectedSimulation = sim,
                        isSimulatingRain = true
                    )
                }
                _weatherState.value = WeatherRisk(
                    precipitationMm = sim.rainIntensity.toDouble() / 5.0,
                    riskLevel = if (sim.riskColor == "red") RiskLevel.RED else RiskLevel.YELLOW,
                    advisory = "[SIMULATION PLAYBACK: ${sim.year}] ${sim.description}",
                    riverWaterLevel = "Simulated Surge Level: +3.6m",
                    catchmentRainfall = sim.rainIntensity.toDouble(),
                    lastUpdated = System.currentTimeMillis()
                )
                // Trigger Simulation Push Alert
                NotificationHelper.sendRiskAlertNotification(
                    context = getApplication(),
                    title = "⚠️ SIMULATION ALERT: ${sim.title}",
                    message = "Simulated extreme rainfall (${sim.rainIntensity}mm/h). Surge advisory active!",
                    isCritical = sim.riskColor == "red"
                )
            }
        }
    }

    fun onMapClick(lat: Double, lng: Double) {
        _uiState.update {
            it.copy(
                pickedLocation = Pair(lat, lng),
                isCustomLocationPicked = true
            )
        }
    }

    fun clearPickedLocation() {
        _uiState.update { it.copy(pickedLocation = null, isCustomLocationPicked = false) }
    }

    fun navigateToCoords(lat: Double, lng: Double) {
        _uiState.update { it.copy(navigationTarget = Pair(lat, lng)) }
    }

    fun clearNavigationTarget() {
        _uiState.update { it.copy(navigationTarget = null) }
    }

    fun setUserLocation(lat: Double, lng: Double) {
        _uiState.update { it.copy(userLat = lat, userLng = lng) }
        refreshWeatherData()
    }

    fun toggleSosBeacon() {
        sosBeaconManager.toggleBeacon()
    }

    fun startSosBeacon() {
        sosBeaconManager.startBeacon()
    }

    fun stopSosBeacon() {
        sosBeaconManager.stopBeacon()
    }

    fun setSosSoundEnabled(enabled: Boolean) {
        sosBeaconManager.setSoundEnabled(enabled)
    }

    fun setSosFlashlightEnabled(enabled: Boolean) {
        sosBeaconManager.setFlashlightEnabled(enabled)
    }

    fun setSosScreenStrobeEnabled(enabled: Boolean) {
        sosBeaconManager.setScreenStrobeEnabled(enabled)
    }

    fun setSosAudioMode(mode: SosAudioMode) {
        sosBeaconManager.setAudioMode(mode)
    }

    fun setSosStrobeMode(mode: SosStrobeMode) {
        sosBeaconManager.setStrobeMode(mode)
    }

    fun addIncident(
        type: String,
        note: String,
        lat: Double?,
        lng: Double?,
        severity: String = "HIGH"
    ) {
        viewModelScope.launch {
            val finalLat = lat ?: _uiState.value.pickedLocation?.first ?: _uiState.value.userLat
            val finalLng = lng ?: _uiState.value.pickedLocation?.second ?: _uiState.value.userLng

            val savedIncident = incidentRepository.addIncident(
                type = type,
                note = note,
                lat = finalLat,
                lng = finalLng,
                severity = severity
            )

            // In accordance with the 3+ verification requirement, hazard reports start with 0 verifications.
            // When 3+ responders verify/upvote, the system automatically triggers the emergency siren broadcast!
            _uiState.update {
                it.copy(
                    isReportMode = false,
                    pickedLocation = null,
                    isCustomLocationPicked = false,
                    infoMessage = "Hazard reported! Awaiting community verification (0/3). Once 3+ citizens verify, it will automatically broadcast emergency alerts."
                )
            }

            // Dispatch local confirmation notice
            NotificationHelper.sendIncidentReportNotification(
                context = getApplication(),
                typeLabel = savedIncident.incidentType.label,
                severity = severity,
                locationNote = note.ifBlank { "Location: (${String.format(java.util.Locale.US, "%.4f", finalLat)}, ${String.format(java.util.Locale.US, "%.4f", finalLng)})" }
            )

            // Broadcast to other devices over shared disaster sync network so community can verify
            launch(Dispatchers.IO) {
                CloudIncidentSyncManager.publishIncident(getApplication(), savedIncident)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sosBeaconManager.release()
    }

    fun deleteIncident(id: Long) {
        viewModelScope.launch {
            incidentRepository.deleteIncident(id)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            incidentRepository.resetToDefaults()
            _uiState.update {
                it.copy(
                    userLat = 30.7268,
                    userLng = 78.4350,
                    alertRadiusKm = 3.0,
                    yellowThresholdMm = 5.0,
                    redThresholdMm = 10.0,
                    selectedSimulation = null,
                    isSimulatingRain = false,
                    isReportMode = false,
                    pickedLocation = null
                )
            }
            refreshWeatherData()
        }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun getMapJsonPayload(): String {
        val state = _uiState.value
        val weather = _weatherState.value
        val currentIncidents = if (state.selectedSimulation != null) {
            incidents.value + state.selectedSimulation.simulatedIncidents
        } else {
            incidents.value
        }

        val json = JSONObject()
        val center = JSONObject()
        center.put("lat", state.userLat)
        center.put("lng", state.userLng)
        json.put("center", center)

        json.put("radiusMeters", (state.alertRadiusKm * 1000).toInt())
        json.put("riskColor", weather.riskLevel.colorCode)
        json.put("showIncidents", state.showIncidents)
        json.put("showEmergency", state.showEmergency)
        json.put("showRiskZone", state.showRiskZone)
        json.put("reportMode", state.isReportMode)
        json.put("isSimulatingRain", state.isSimulatingRain)
        json.put("rainfallIntensity", if (state.selectedSimulation != null) state.selectedSimulation.rainIntensity else if (weather.riskLevel == RiskLevel.RED) 80 else if (weather.riskLevel == RiskLevel.YELLOW) 40 else 0)

        val incArray = JSONArray()
        currentIncidents.forEach { inc ->
            val incObj = JSONObject()
            incObj.put("id", inc.id)
            incObj.put("type", inc.type)
            incObj.put("note", inc.note)
            incObj.put("lat", inc.lat)
            incObj.put("lng", inc.lng)
            incObj.put("createdAt", inc.createdAt)
            incObj.put("severity", inc.severity)
            incObj.put("userReported", inc.userReported)
            incObj.put("upvotes", inc.upvotes)
            incObj.put("isVerified", inc.isVerified)
            incArray.put(incObj)
        }
        json.put("incidents", incArray)

        val srvArray = JSONArray()
        emergencyServices.forEach { srv ->
            val srvObj = JSONObject()
            srvObj.put("id", srv.id)
            srvObj.put("name", srv.name)
            srvObj.put("type", srv.type)
            srvObj.put("lat", srv.lat)
            srvObj.put("lng", srv.lng)
            srvObj.put("address", srv.address)
            srvObj.put("phone", srv.phone)
            srvArray.put(srvObj)
        }
        json.put("emergencyServices", srvArray)

        return json.toString()
    }
}
