package com.smarthealth.companion.alert

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.smarthealth.companion.MainActivity
import com.smarthealth.companion.model.AlertSeverity
import com.smarthealth.companion.model.RealtimeAlertItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Real-time Alert & Notification Manager.
 * Handles:
 * - In-app broadcast alerts (activeAlert StateFlow)
 * - Android system notifications outside the application
 * - Gentle sound touch (ToneGenerator, normal sound, not too loud)
 * - Anti-spam throttling (25-second cooldown for sound/system notifications)
 */
class RealtimeAlertManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _activeAlert = MutableStateFlow<RealtimeAlertItem?>(null)
    val activeAlert: StateFlow<RealtimeAlertItem?> = _activeAlert.asStateFlow()

    private var lastSoundNotificationTimestampMs = 0L
    private val throttleCooldownMs = 25_000L // 25 seconds anti-spam throttle

    companion object {
        const val CHANNEL_ID = "smart_health_realtime_alerts"
        const val CHANNEL_NAME = "Health & Atmosphere Alerts"
        const val NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Real-time alerts for abnormal room pressure, hypoxia risks, heart rate, and body temperature"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Triggers a real-time alert for abnormal atmospheric pressure or vital readings.
     */
    fun triggerAlert(alert: RealtimeAlertItem) {
        // Always publish to in-app active alert banner immediately
        _activeAlert.value = alert

        val now = System.currentTimeMillis()
        if (now - lastSoundNotificationTimestampMs >= throttleCooldownMs) {
            lastSoundNotificationTimestampMs = now

            // 1. Play gentle audio touch (normal sound, not too loud)
            playGentleAlertTouchSound()

            // 2. Post system notification outside the application
            postSystemNotification(alert)
        }
    }

    /**
     * Dismisses the active in-app alert banner.
     */
    fun dismissActiveAlert() {
        _activeAlert.value = null
    }

    /**
     * Plays a pleasant, subtle audio touch ("giving a small touch of sound, not too loud")
     * using Android's ToneGenerator STREAM_NOTIFICATION at 45% volume.
     */
    private fun playGentleAlertTouchSound() {
        scope.launch {
            try {
                // Subtle tactile vibration (60ms)
                triggerGentleHaptic()

                // Subtle audio tone: TONE_PROP_BEEP2 (180ms gentle double pip)
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 45)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
                delay(300L)
                toneGen.release()
            } catch (e: Exception) {
                android.util.Log.w("RealtimeAlertManager", "Audio touch play exception: ${e.message}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun triggerGentleHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(70)
                }
            }
        } catch (e: Exception) {
            android.util.Log.d("RealtimeAlertManager", "Vibrate not available: ${e.message}")
        }
    }

    /**
     * Posts a system notification outside the application.
     */
    @SuppressLint("NotificationPermission")
    private fun postSystemNotification(alert: RealtimeAlertItem) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smallIcon = android.R.drawable.ic_dialog_alert

        val expandedContent = buildString {
            appendLine(alert.message)
            if (alert.guidance.isNotEmpty()) {
                appendLine()
                append(alert.guidance)
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(alert.title)
            .setContentText(alert.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedContent))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            android.util.Log.w("RealtimeAlertManager", "Notification permission not granted: ${e.message}")
        }
    }
}
