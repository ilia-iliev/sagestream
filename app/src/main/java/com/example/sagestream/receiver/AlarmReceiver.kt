package com.example.SageStream.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.sagestream.notification.NotificationService
import com.example.SageStream.repository.NotificationRepository
import com.example.SageStream.notification.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Random

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        Log.d(TAG, "AlarmReceiver received intent: ${intent.action}")

        if (intent.action == NotificationScheduler.ACTION_SHOW_QUOTE_NOTIFICATION) {
            handleQuoteNotification(context, intent)
        }
    }

    private fun handleQuoteNotification(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(NotificationScheduler.EXTRA_NOTIFICATION_ID, -1)
        val hour = intent.getIntExtra(NotificationScheduler.EXTRA_NOTIFICATION_HOUR, -1)
        val minute = intent.getIntExtra(NotificationScheduler.EXTRA_NOTIFICATION_MINUTE, -1)

        Log.d(TAG, "Handling quote notification: id=$notificationId, time=$hour:$minute")

        // Use coroutine to handle async operations
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = NotificationRepository(context)
                val notificationService = NotificationService(context)
                val scheduler = NotificationScheduler(context)

                // Get all quotes
                val quotes = repository.getAllQuotes()
                if (quotes.isEmpty()) {
                    Log.w(TAG, "No quotes available for notification")
                    return@launch
                }

                // Select a random quote
                val randomQuote = if (quotes.isNotEmpty()) {
                    val random = Random()
                    quotes[random.nextInt(quotes.size)]
                } else null

                if (randomQuote != null) {
                    Log.d(TAG, "Showing AlarmManager notification for quote: ${randomQuote.quote.take(50)}...")
                    notificationService.showQuoteNotification(randomQuote)
                    repository.updateQuoteLastDisplayed(randomQuote.id, System.currentTimeMillis())
                    
                    // Mark this notification as shown for the scheduled time
                    scheduler.markNotificationShown(hour, minute)
                } else {
                    Log.w(TAG, "Failed to get random quote")
                }

                // Reschedule for next day
                scheduler.scheduleAllNotifications()

            } catch (e: Exception) {
                Log.e(TAG, "Error handling quote notification", e)
            }
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
    }
} 