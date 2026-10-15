package io.github.thomaswcode.notificationvibrator.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.thomaswcode.notificationvibrator.vibration.PatternLibrary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** How the other apps' own vibration is silenced, which decides the vibration type this app plays. */
enum class MuteMode {
    /** One UI notification vibration intensity at 0; this app plays alarm-type vibrations. */
    GLOBAL,

    /** Vibration turned off per app/category; this app plays notification-type vibrations. */
    PER_APP,
}

data class AppSettings(
    val defaultPatternId: String = PatternLibrary.DEFAULT_PATTERN_ID,
    val defaultIntensity: Float = 1f,
    val muteMode: MuteMode = MuteMode.GLOBAL,
    val cooldownMs: Long = DEFAULT_COOLDOWN_MS,
    /** Also vibrate for categories set to Silent (needed for the per-app mute fallback). */
    val includeSilentChannels: Boolean = false,
) {
    companion object {
        const val DEFAULT_COOLDOWN_MS = 1500L
        const val MAX_COOLDOWN_MS = 5000L
    }
}

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsStore(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        val defaults = AppSettings()
        AppSettings(
            defaultPatternId = prefs[DEFAULT_PATTERN_ID] ?: defaults.defaultPatternId,
            defaultIntensity = prefs[DEFAULT_INTENSITY] ?: defaults.defaultIntensity,
            muteMode = prefs[MUTE_MODE]?.let { name -> MuteMode.entries.firstOrNull { it.name == name } } ?: defaults.muteMode,
            cooldownMs = prefs[COOLDOWN_MS] ?: defaults.cooldownMs,
            includeSilentChannels = prefs[INCLUDE_SILENT_CHANNELS] ?: defaults.includeSilentChannels,
        )
    }

    suspend fun setDefaultPattern(patternId: String, intensity: Float) {
        dataStore.edit {
            it[DEFAULT_PATTERN_ID] = patternId
            it[DEFAULT_INTENSITY] = intensity
        }
    }

    suspend fun setMuteMode(mode: MuteMode) {
        dataStore.edit { it[MUTE_MODE] = mode.name }
    }

    suspend fun setCooldownMs(cooldownMs: Long) {
        dataStore.edit { it[COOLDOWN_MS] = cooldownMs }
    }

    suspend fun setIncludeSilentChannels(include: Boolean) {
        dataStore.edit { it[INCLUDE_SILENT_CHANNELS] = include }
    }

    private companion object {
        val DEFAULT_PATTERN_ID = stringPreferencesKey("default_pattern_id")
        val DEFAULT_INTENSITY = floatPreferencesKey("default_intensity")
        val MUTE_MODE = stringPreferencesKey("mute_mode")
        val COOLDOWN_MS = longPreferencesKey("cooldown_ms")
        val INCLUDE_SILENT_CHANNELS = booleanPreferencesKey("include_silent_channels")
    }
}
