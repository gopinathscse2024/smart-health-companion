package com.smarthealth.companion.data.local

import android.content.Context
import android.content.SharedPreferences
import com.smarthealth.companion.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("smart_health_prefs", Context.MODE_PRIVATE)

    private val _userProfileFlow = MutableStateFlow(loadProfile())
    val userProfileFlow: StateFlow<UserProfile> = _userProfileFlow.asStateFlow()

    fun loadProfile(): UserProfile {
        return UserProfile(
            profileId = prefs.getString("profile_id", "default_user") ?: "default_user",
            name = prefs.getString("user_name", "Primary User") ?: "Primary User",
            age = prefs.getInt("user_age", 32),
            gender = prefs.getString("user_gender", "Not Specified") ?: "Not Specified",
            targetHrRestingMin = prefs.getFloat("hr_min", 55.0f).toDouble(),
            targetHrRestingMax = prefs.getFloat("hr_max", 90.0f).toDouble(),
            targetBodyTempMin = prefs.getFloat("temp_min", 36.1f).toDouble(),
            targetBodyTempMax = prefs.getFloat("temp_max", 37.5f).toDouble(),
            hasAsthmaOrCopd = prefs.getBoolean("has_respiratory", true),
            pefBaselineLpm = prefs.getFloat("pef_baseline", 480.0f).toDouble(),
            enableGpsLogging = prefs.getBoolean("enable_gps", false),
            useFahrenheit = prefs.getBoolean("use_fahrenheit", false),
            simulationModeEnabled = false
        )
    }

    fun saveProfile(profile: UserProfile) {
        prefs.edit()
            .putString("profile_id", profile.profileId)
            .putString("user_name", profile.name)
            .putInt("user_age", profile.age)
            .putString("user_gender", profile.gender)
            .putFloat("hr_min", profile.targetHrRestingMin.toFloat())
            .putFloat("hr_max", profile.targetHrRestingMax.toFloat())
            .putFloat("temp_min", profile.targetBodyTempMin.toFloat())
            .putFloat("temp_max", profile.targetBodyTempMax.toFloat())
            .putBoolean("has_respiratory", profile.hasAsthmaOrCopd)
            .putFloat("pef_baseline", profile.pefBaselineLpm.toFloat())
            .putBoolean("enable_gps", profile.enableGpsLogging)
            .putBoolean("use_fahrenheit", profile.useFahrenheit)
            .putBoolean("simulation_mode", false)
            .apply()

        _userProfileFlow.value = profile.copy(simulationModeEnabled = false)
    }

    fun setSimulationMode(enabled: Boolean) {
        val current = _userProfileFlow.value
        val updated = current.copy(simulationModeEnabled = false)
        saveProfile(updated)
    }

    fun setGpsLogging(enabled: Boolean) {
        val current = _userProfileFlow.value
        val updated = current.copy(enableGpsLogging = enabled)
        saveProfile(updated)
    }
}
