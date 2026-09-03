package com.example.flood.util

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

enum class SosAudioMode(val title: String, val description: String, val frequencyLabel: String) {
    HIGH_FREQUENCY_SIREN(
        "Acoustic SAR Siren",
        "Sweeping 2.4 kHz – 3.8 kHz warble engineered to penetrate torrential rain, wind, and boat engine noise.",
        "2.4 – 3.8 kHz Warble"
    ),
    SOS_MORSE(
        "International Morse SOS",
        "Standard SAR acoustic distress beacon (... --- ...) at 3.0 kHz piercing frequency.",
        "3.0 kHz Morse (... --- ...)"
    ),
    PIERCING_WHISTLE(
        "Rescue Whistle Pulse",
        "Continuous 3.5 kHz pulsed whistle tone to direct NDRF / SDRF search teams.",
        "3.5 kHz Pulsed Whistle"
    )
}

enum class SosStrobeMode(val title: String, val description: String, val rateLabel: String) {
    TACTICAL_PULSE(
        "Tactical SAR Strobe",
        "High-frequency 8 Hz flashing pattern for rapid optical location in dark flood conditions.",
        "8 Hz Fast Pulse"
    ),
    SOS_MORSE_FLASH(
        "Morse SOS Flasher",
        "Optical (... --- ...) pulses easily recognizable by rescue helicopters and patrol boats.",
        "SOS Optical Morse"
    ),
    BEACON_BURST(
        "Energy-Saving Beacon",
        "High-intensity double-burst once per second to conserve battery during prolonged isolation.",
        "1 Hz Double Burst"
    )
}

class SosBeaconManager(private val context: Context) {

    companion object {
        private const val TAG = "SosBeaconManager"
        private const val SAMPLE_RATE = 44100
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isBeaconActive = MutableStateFlow(false)
    val isBeaconActive: StateFlow<Boolean> = _isBeaconActive.asStateFlow()

    private val _isSoundEnabled = MutableStateFlow(true)
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled.asStateFlow()

    private val _isFlashlightEnabled = MutableStateFlow(true)
    val isFlashlightEnabled: StateFlow<Boolean> = _isFlashlightEnabled.asStateFlow()

    private val _isScreenStrobeEnabled = MutableStateFlow(true)
    val isScreenStrobeEnabled: StateFlow<Boolean> = _isScreenStrobeEnabled.asStateFlow()

    private val _audioMode = MutableStateFlow(SosAudioMode.HIGH_FREQUENCY_SIREN)
    val audioMode: StateFlow<SosAudioMode> = _audioMode.asStateFlow()

    private val _strobeMode = MutableStateFlow(SosStrobeMode.TACTICAL_PULSE)
    val strobeMode: StateFlow<SosStrobeMode> = _strobeMode.asStateFlow()

    private val _isFlashHardwareAvailable = MutableStateFlow(checkFlashAvailable())
    val isFlashHardwareAvailable: StateFlow<Boolean> = _isFlashHardwareAvailable.asStateFlow()

    // Live state indicating whether flashlight torch or screen is currently in ON phase of pulse
    private val _isPulseHigh = MutableStateFlow(false)
    val isPulseHigh: StateFlow<Boolean> = _isPulseHigh.asStateFlow()

    private var audioJob: Job? = null
    private var strobeJob: Job? = null
    private var audioTrack: AudioTrack? = null

    private val cameraManager: CameraManager? by lazy {
        try {
            context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        } catch (e: Exception) {
            null
        }
    }

    private fun checkFlashAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            cm?.cameraIdList?.any { id ->
                val chars = cm.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun getFlashCameraId(): String? {
        return try {
            cameraManager?.cameraIdList?.firstOrNull { id ->
                val chars = cameraManager?.getCameraCharacteristics(id)
                val hasFlash = chars?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars?.get(CameraCharacteristics.LENS_FACING)
                hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager?.cameraIdList?.firstOrNull { id ->
                val chars = cameraManager?.getCameraCharacteristics(id)
                chars?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            null
        }
    }

    fun startBeacon() {
        if (_isBeaconActive.value) return
        _isBeaconActive.value = true

        if (_isSoundEnabled.value) {
            startAudioLoop()
        }
        if (_isFlashlightEnabled.value || _isScreenStrobeEnabled.value) {
            startStrobeLoop()
        }
    }

    fun stopBeacon() {
        _isBeaconActive.value = false
        stopAudioLoop()
        stopStrobeLoop()
        setTorchState(false)
        _isPulseHigh.value = false
    }

    fun toggleBeacon() {
        if (_isBeaconActive.value) stopBeacon() else startBeacon()
    }

    fun setSoundEnabled(enabled: Boolean) {
        _isSoundEnabled.value = enabled
        if (_isBeaconActive.value) {
            if (enabled) startAudioLoop() else stopAudioLoop()
        }
    }

    fun setFlashlightEnabled(enabled: Boolean) {
        _isFlashlightEnabled.value = enabled
        if (!enabled) {
            setTorchState(false)
        }
        if (_isBeaconActive.value && (_isFlashlightEnabled.value || _isScreenStrobeEnabled.value)) {
            if (strobeJob?.isActive != true) startStrobeLoop()
        }
    }

    fun setScreenStrobeEnabled(enabled: Boolean) {
        _isScreenStrobeEnabled.value = enabled
        if (_isBeaconActive.value && (_isFlashlightEnabled.value || _isScreenStrobeEnabled.value)) {
            if (strobeJob?.isActive != true) startStrobeLoop()
        }
    }

    fun setAudioMode(mode: SosAudioMode) {
        _audioMode.value = mode
        if (_isBeaconActive.value && _isSoundEnabled.value) {
            stopAudioLoop()
            startAudioLoop()
        }
    }

    fun setStrobeMode(mode: SosStrobeMode) {
        _strobeMode.value = mode
        if (_isBeaconActive.value && (_isFlashlightEnabled.value || _isScreenStrobeEnabled.value)) {
            stopStrobeLoop()
            startStrobeLoop()
        }
    }

    // ==========================================
    // ACOUSTIC SIREN SYNTHESIS (AudioTrack)
    // ==========================================

    private fun startAudioLoop() {
        stopAudioLoop()

        audioJob = scope.launch(Dispatchers.Default) {
            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = (minBuf * 2).coerceAtLeast(4096)

            val track = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val attrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    val format = AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                    AudioTrack(
                        attrs,
                        format,
                        bufferSize,
                        AudioTrack.MODE_STREAM,
                        AudioManager.AUDIO_SESSION_ID_GENERATE
                    )
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_ALARM,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STREAM
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize AudioTrack: ${e.message}")
                return@launch
            }

            audioTrack = track
            try {
                track.play()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play AudioTrack: ${e.message}")
                return@launch
            }

            val chunkDurationMs = 20
            val chunkSamples = (SAMPLE_RATE * chunkDurationMs) / 1000
            val shortBuffer = ShortArray(chunkSamples)

            var phase = 0.0
            var timeElapsedMs = 0L

            while (isActive && _isBeaconActive.value && _isSoundEnabled.value) {
                val mode = _audioMode.value

                when (mode) {
                    SosAudioMode.HIGH_FREQUENCY_SIREN -> {
                        // Continuous sweeping warble between 2400 Hz and 3800 Hz
                        // Cycle period = 400ms (fast aggressive siren)
                        val cycleMs = 400L
                        val phaseInCycle = (timeElapsedMs % cycleMs).toDouble() / cycleMs
                        // Triangle wave sweep
                        val sweepFactor = if (phaseInCycle < 0.5) {
                            phaseInCycle * 2.0
                        } else {
                            (1.0 - phaseInCycle) * 2.0
                        }
                        val currentFreq = 2400.0 + sweepFactor * (3800.0 - 2400.0)

                        val phaseIncrement = (2.0 * PI * currentFreq) / SAMPLE_RATE
                        for (i in 0 until chunkSamples) {
                            phase += phaseIncrement
                            if (phase > 2.0 * PI) phase -= 2.0 * PI
                            shortBuffer[i] = (sin(phase) * 32000).toInt().toShort()
                        }
                        track.write(shortBuffer, 0, chunkSamples)
                        timeElapsedMs += chunkDurationMs
                    }

                    SosAudioMode.SOS_MORSE -> {
                        // Morse Code: S = . . . (120ms sound, 80ms silence)
                        //             O = - - - (360ms sound, 80ms silence)
                        //             S = . . .
                        //             Pause = 1000ms
                        // Total pattern cycle = ~3400ms
                        val morseFreq = 3000.0
                        val isSoundOn = isMorseAudioSoundOn(timeElapsedMs % 3400L)

                        if (isSoundOn) {
                            val phaseIncrement = (2.0 * PI * morseFreq) / SAMPLE_RATE
                            for (i in 0 until chunkSamples) {
                                phase += phaseIncrement
                                if (phase > 2.0 * PI) phase -= 2.0 * PI
                                shortBuffer[i] = (sin(phase) * 32000).toInt().toShort()
                            }
                        } else {
                            shortBuffer.fill(0)
                        }

                        track.write(shortBuffer, 0, chunkSamples)
                        timeElapsedMs += chunkDurationMs
                    }

                    SosAudioMode.PIERCING_WHISTLE -> {
                        // 3500 Hz Rescue Whistle: 300ms whistle ON, 120ms OFF
                        val whistleCycle = 420L
                        val isWhistleOn = (timeElapsedMs % whistleCycle) < 300L
                        val whistleFreq = 3500.0

                        if (isWhistleOn) {
                            val phaseIncrement = (2.0 * PI * whistleFreq) / SAMPLE_RATE
                            for (i in 0 until chunkSamples) {
                                phase += phaseIncrement
                                if (phase > 2.0 * PI) phase -= 2.0 * PI
                                shortBuffer[i] = (sin(phase) * 32000).toInt().toShort()
                            }
                        } else {
                            shortBuffer.fill(0)
                        }

                        track.write(shortBuffer, 0, chunkSamples)
                        timeElapsedMs += chunkDurationMs
                    }
                }
            }
        }
    }

    private fun isMorseAudioSoundOn(cycleMs: Long): Boolean {
        // ... --- ...
        // S: . (0..120) pause (120..200) . (200..320) pause (320..400) . (400..520) pause (520..680)
        // O: - (680..1040) pause (1040..1120) - (1120..1480) pause (1480..1560) - (1560..1920) pause (1920..2080)
        // S: . (2080..2200) pause (2200..2280) . (2280..2400) pause (2400..2480) . (2480..2600)
        // Word pause: 2600..3400
        return when (cycleMs) {
            in 0L..120L -> true
            in 200L..320L -> true
            in 400L..520L -> true
            in 680L..1040L -> true
            in 1120L..1480L -> true
            in 1560L..1920L -> true
            in 2080L..2200L -> true
            in 2280L..2400L -> true
            in 2480L..2600L -> true
            else -> false
        }
    }

    private fun stopAudioLoop() {
        audioJob?.cancel()
        audioJob = null
        try {
            audioTrack?.let {
                it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio track: ${e.message}")
        }
        audioTrack = null
    }

    // ==========================================
    // FLASHLIGHT & SCREEN STROBE LOGIC
    // ==========================================

    private fun startStrobeLoop() {
        stopStrobeLoop()

        strobeJob = scope.launch(Dispatchers.Default) {
            val cameraId = getFlashCameraId()

            while (isActive && _isBeaconActive.value) {
                when (_strobeMode.value) {
                    SosStrobeMode.TACTICAL_PULSE -> {
                        // 8 Hz fast pulse: ~65ms ON, ~60ms OFF
                        setPulse(true, cameraId)
                        delay(65)
                        setPulse(false, cameraId)
                        delay(60)
                    }

                    SosStrobeMode.SOS_MORSE_FLASH -> {
                        // Morse SOS: 3 short, 3 long, 3 short
                        // Short: 140ms ON, 100ms OFF
                        for (i in 0..2) {
                            if (!isActive || !_isBeaconActive.value) break
                            setPulse(true, cameraId)
                            delay(140)
                            setPulse(false, cameraId)
                            delay(100)
                        }
                        delay(150) // inter-letter

                        // Long: 380ms ON, 100ms OFF
                        for (i in 0..2) {
                            if (!isActive || !_isBeaconActive.value) break
                            setPulse(true, cameraId)
                            delay(380)
                            setPulse(false, cameraId)
                            delay(100)
                        }
                        delay(150) // inter-letter

                        // Short: 140ms ON, 100ms OFF
                        for (i in 0..2) {
                            if (!isActive || !_isBeaconActive.value) break
                            setPulse(true, cameraId)
                            delay(140)
                            setPulse(false, cameraId)
                            delay(100)
                        }
                        delay(900) // Word pause
                    }

                    SosStrobeMode.BEACON_BURST -> {
                        // Double burst: 60ms ON, 70ms OFF, 60ms ON, 810ms OFF
                        setPulse(true, cameraId)
                        delay(60)
                        setPulse(false, cameraId)
                        delay(70)
                        setPulse(true, cameraId)
                        delay(60)
                        setPulse(false, cameraId)
                        delay(810)
                    }
                }
            }

            setTorchState(false, cameraId)
            _isPulseHigh.value = false
        }
    }

    private fun setPulse(high: Boolean, cameraId: String?) {
        _isPulseHigh.value = high
        if (_isFlashlightEnabled.value) {
            setTorchState(high, cameraId)
        }
    }

    private fun setTorchState(on: Boolean, specificCamId: String? = null) {
        val camId = specificCamId ?: getFlashCameraId() ?: return
        try {
            cameraManager?.setTorchMode(camId, on)
        } catch (e: CameraAccessException) {
            Log.d(TAG, "CameraAccessException setting torch: ${e.message}")
        } catch (e: Exception) {
            Log.d(TAG, "Exception setting torch: ${e.message}")
        }
    }

    private fun stopStrobeLoop() {
        strobeJob?.cancel()
        strobeJob = null
        setTorchState(false)
        _isPulseHigh.value = false
    }

    fun release() {
        stopBeacon()
    }
}
