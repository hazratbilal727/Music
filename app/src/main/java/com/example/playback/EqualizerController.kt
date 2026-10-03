package com.example.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqualizerState(
    val isEnabled: Boolean = true,
    val presets: List<String> = emptyList(),
    val currentPresetIndex: Int = 0,
    val numberOfBands: Int = 5,
    val minBandLevelMilliBels: Short = -1000,
    val maxBandLevelMilliBels: Short = 1000,
    val bandLevels: List<Short> = listOf(0, 0, 0, 0, 0),
    val bandCenterFreqs: List<Int> = listOf(60, 230, 910, 3600, 14000), // in Hz
    val bassBoostStrength: Int = 300, // 0 to 1000
    val virtualizerStrength: Int = 200 // 0 to 1000
)

class EqualizerController {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        release()
        try {
            val eq = Equalizer(0, audioSessionId).apply {
                enabled = _state.value.isEnabled
            }
            equalizer = eq

            val bb = BassBoost(0, audioSessionId).apply {
                enabled = _state.value.isEnabled
                if (strengthSupported) {
                    setStrength(_state.value.bassBoostStrength.toShort())
                }
            }
            bassBoost = bb

            val virt = Virtualizer(0, audioSessionId).apply {
                enabled = _state.value.isEnabled
                if (strengthSupported) {
                    setStrength(_state.value.virtualizerStrength.toShort())
                }
            }
            virtualizer = virt

            val numBands = eq.numberOfBands.toInt().coerceAtLeast(1)
            val bandRange = eq.bandLevelRange ?: shortArrayOf(-1000, 1000)
            val minLevel = bandRange[0]
            val maxLevel = bandRange[1]

            val presetNames = mutableListOf<String>()
            val numPresets = eq.numberOfPresets.toInt()
            for (p in 0 until numPresets) {
                presetNames.add(eq.getPresetName(p.toShort()) ?: "Preset $p")
            }
            presetNames.add("Custom")

            val freqs = mutableListOf<Int>()
            val levels = mutableListOf<Short>()
            for (b in 0 until numBands) {
                freqs.add(eq.getCenterFreq(b.toShort()) / 1000) // Convert milliHertz to Hz
                levels.add(eq.getBandLevel(b.toShort()))
            }

            _state.value = _state.value.copy(
                presets = presetNames,
                numberOfBands = numBands,
                minBandLevelMilliBels = minLevel,
                maxBandLevelMilliBels = maxLevel,
                bandCenterFreqs = freqs,
                bandLevels = levels
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
            _state.value = _state.value.copy(isEnabled = enabled)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setPreset(index: Int) {
        val eq = equalizer ?: return
        try {
            if (index < eq.numberOfPresets.toInt()) {
                eq.usePreset(index.toShort())
                val newLevels = mutableListOf<Short>()
                for (b in 0 until eq.numberOfBands.toInt()) {
                    newLevels.add(eq.getBandLevel(b.toShort()))
                }
                _state.value = _state.value.copy(
                    currentPresetIndex = index,
                    bandLevels = newLevels
                )
            } else {
                // Custom
                _state.value = _state.value.copy(currentPresetIndex = index)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setBandLevel(band: Int, levelMilliBels: Short) {
        val eq = equalizer ?: return
        try {
            eq.setBandLevel(band.toShort(), levelMilliBels)
            val currentLevels = _state.value.bandLevels.toMutableList()
            if (band in currentLevels.indices) {
                currentLevels[band] = levelMilliBels
            }
            val customIndex = _state.value.presets.size - 1
            _state.value = _state.value.copy(
                bandLevels = currentLevels,
                currentPresetIndex = if (customIndex >= 0) customIndex else _state.value.currentPresetIndex
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        try {
            bassBoost?.let {
                if (it.strengthSupported) {
                    it.setStrength(clamped.toShort())
                }
            }
            _state.value = _state.value.copy(bassBoostStrength = clamped)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        try {
            virtualizer?.let {
                if (it.strengthSupported) {
                    it.setStrength(clamped.toShort())
                }
            }
            _state.value = _state.value.copy(virtualizerStrength = clamped)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun reset() {
        try {
            val eq = equalizer
            val numBands = (eq?.numberOfBands?.toInt() ?: _state.value.numberOfBands).coerceAtLeast(1)
            val zeroLevels = mutableListOf<Short>()
            for (b in 0 until numBands) {
                try {
                    eq?.setBandLevel(b.toShort(), 0)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                zeroLevels.add(0)
            }

            // Find flat or normal preset if available
            val flatIndex = _state.value.presets.indexOfFirst {
                it.equals("Flat", ignoreCase = true) || it.equals("Normal", ignoreCase = true)
            }
            val targetPresetIndex = if (flatIndex >= 0) flatIndex else 0
            if (eq != null && targetPresetIndex >= 0 && targetPresetIndex < (eq.numberOfPresets.toInt())) {
                try {
                    eq.usePreset(targetPresetIndex.toShort())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            bassBoost?.let {
                if (it.strengthSupported) {
                    try {
                        it.setStrength(0)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            virtualizer?.let {
                if (it.strengthSupported) {
                    try {
                        it.setStrength(0)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            _state.value = _state.value.copy(
                bandLevels = zeroLevels,
                currentPresetIndex = targetPresetIndex,
                bassBoostStrength = 0,
                virtualizerStrength = 0
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            equalizer?.release()
            equalizer = null
            bassBoost?.release()
            bassBoost = null
            virtualizer?.release()
            virtualizer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
