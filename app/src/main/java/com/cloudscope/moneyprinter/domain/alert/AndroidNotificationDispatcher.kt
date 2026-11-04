package com.cloudscope.moneyprinter.domain.alert

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cloudscope.moneyprinter.MainActivity
import com.cloudscope.moneyprinter.R

class AndroidNotificationDispatcher(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                "NIFTY 50 Market Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time direction changes, AI confidence surges, and technical breakouts"
                enableLights(true)
                enableVibration(true)
            }

            val monitoringChannel = NotificationChannel(
                CHANNEL_MONITORING_ID,
                "NIFTY 50 Live Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background persistence for NIFTY 50 live market data and trigger evaluation"
            }

            val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            systemManager?.createNotificationChannel(alertChannel)
            systemManager?.createNotificationChannel(monitoringChannel)
        }
    }

    fun dispatchAlert(alert: MarketAlert, settings: AlertSettings) {
        if (!settings.notificationsEnabled) return

        // 1. Audio tone if enabled
        if (settings.soundEnabled) {
            playAudioTone(alert.type)
        }

        // 2. Check Android 13+ POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Log.d(TAG, "Skipping system notification: POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(alert.title)
                .setContentText(alert.message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .build()

            val notificationId = alert.id.hashCode()
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException while dispatching alert: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error dispatching notification", e)
        }
    }

    private fun playAudioTone(type: MarketAlertType) {
        try {
            val toneType = when (type) {
                MarketAlertType.DIRECTION_BULLISH,
                MarketAlertType.CONFIDENCE_INCREASE,
                MarketAlertType.VWAP_BREAKOUT,
                MarketAlertType.EMA_BULLISH_CROSS,
                MarketAlertType.MACD_BULLISH_CROSS -> ToneGenerator.TONE_PROP_ACK

                MarketAlertType.DIRECTION_BEARISH,
                MarketAlertType.CONFIDENCE_DECREASE,
                MarketAlertType.VWAP_BREAKDOWN,
                MarketAlertType.EMA_BEARISH_CROSS,
                MarketAlertType.MACD_BEARISH_CROSS -> ToneGenerator.TONE_PROP_NACK

                MarketAlertType.VOLUME_SURGE,
                MarketAlertType.VIX_SPIKE -> ToneGenerator.TONE_PROP_BEEP2

                else -> ToneGenerator.TONE_PROP_BEEP
            }

            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            toneGen.startTone(toneType, 220)
            toneGen.release()
        } catch (t: Throwable) {
            Log.w(TAG, "Could not play notification audio tone: ${t.message}")
        }
    }

    fun buildMonitoringNotification(statusText: String = "Monitoring NIFTY 50 live feed"): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_MONITORING_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("NIFTY 50 Live Predictor")
            .setContentText(statusText)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        const val CHANNEL_ALERTS_ID = "nifty_market_alerts"
        const val CHANNEL_MONITORING_ID = "nifty_market_monitoring"
        const val MONITORING_NOTIFICATION_ID = 9001
        private const val TAG = "NotificationDispatcher"
    }
}
