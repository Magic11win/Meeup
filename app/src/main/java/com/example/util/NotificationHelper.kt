package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlin.random.Random

/**
 * Manages real Android system push notifications for incoming messages,
 * file shares, voice/video calls, and persistent live location radar alerts.
 */
object NotificationHelper {
    const val CHANNEL_MESSAGES = "meetup_messages_channel"
    const val CHANNEL_RADAR = "meetup_radar_channel"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val msgChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Meetup Encrypted Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time notifications for new encrypted messages and group chats"
                enableVibration(true)
            }

            val radarChannel = NotificationChannel(
                CHANNEL_RADAR,
                "Meetup Live Location Radar",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts for persistent live location permissions and movement tracking"
            }

            manager.createNotificationChannel(msgChannel)
            manager.createNotificationChannel(radarChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun sendPushNotification(
        context: Context,
        senderName: String,
        messageBody: String,
        isRadarAlert: Boolean = false
    ) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (isRadarAlert) CHANNEL_RADAR else CHANNEL_MESSAGES
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(senderName)
            .setContentText(messageBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(Random.nextInt(1000, 99999), builder.build())
        } catch (_: SecurityException) {
            // Ignored if permission was revoked at runtime
        }
    }
}
