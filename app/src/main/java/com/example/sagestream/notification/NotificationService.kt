package com.example.sagestream.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.SageStream.MainActivity
import com.example.SageStream.R
import com.example.SageStream.model.MotivationalQuote
import androidx.core.content.ContextCompat

class NotificationService(private val context: Context) {

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val quotesChannel = NotificationChannel(
                CHANNEL_QUOTES,
                "Motivational Quotes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for motivational quotes"
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(quotesChannel)
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // For Android 12 and below, notification permission is granted by default
        }
    }

    fun showQuoteNotification(quote: MotivationalQuote) {
        try {
            if (!hasNotificationPermission()) {
                return
            }
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_NOTIFICATION_TYPE, TYPE_QUOTE)
                putExtra(EXTRA_NOTIFICATION_ID, quote.id)
            }
            val pendingIntent = PendingIntent.getActivity(
                context, quote.id.hashCode(), intent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_QUOTES)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Daily Motivation")
                .setContentText(quote.quote)
                .setStyle(NotificationCompat.BigTextStyle().bigText(quote.quote))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)

            with(NotificationManagerCompat.from(context)) {
                notify(quote.id.hashCode(), builder.build())
            }
        } catch (e: SecurityException) {
            // Handle the case where notification permission is not granted
            e.printStackTrace()
        }
    }

    fun cancelNotification(notificationId: Int) {
        try {
            if (!hasNotificationPermission()) {
                return
            }
            NotificationManagerCompat.from(context).cancel(notificationId)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    companion object {
        const val CHANNEL_QUOTES = "quotes_channel"
        const val TYPE_QUOTE = "quote"
        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
} 