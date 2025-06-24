package com.example.SageStream

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.SageStream.notification.NotificationScheduler
import com.example.SageStream.repository.NotificationRepository
import com.example.SageStream.model.MotivationalQuote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SageStream : Application() {
    companion object {
        const val CHANNEL_ID = "default_channel"
        private const val TAG = "SageStream"
    }

    private lateinit var notificationScheduler: NotificationScheduler
    private lateinit var repository: NotificationRepository

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        // Initialize the notification scheduler and repository
        notificationScheduler = NotificationScheduler(this)
        repository = NotificationRepository(this)
        
        // Add default quotes if none exist
        addDefaultQuotesIfNeeded()
        
        // Schedule all notifications
        notificationScheduler.scheduleAllNotifications()
    }

    private fun addDefaultQuotesIfNeeded() {
        CoroutineScope(Dispatchers.IO).launch {
            val existingQuotes = repository.getAllQuotes()
            if (existingQuotes.isEmpty()) {
                Log.d(TAG, "No quotes found, adding default quotes")
                val defaultQuotes = listOf(
                    MotivationalQuote(quote = "The only way to do great work is to love what you do."),
                    MotivationalQuote(quote = "Success is not final, failure is not fatal: it is the courage to continue that counts."),
                    MotivationalQuote(quote = "Believe you can and you're halfway there."),
                    MotivationalQuote(quote = "The future belongs to those who believe in the beauty of their dreams."),
                    MotivationalQuote(quote = "Don't watch the clock; do what it does. Keep going."),
                    MotivationalQuote(quote = "The only limit to our realization of tomorrow is our doubts of today."),
                    MotivationalQuote(quote = "It always seems impossible until it's done."),
                    MotivationalQuote(quote = "Your time is limited, don't waste it living someone else's life."),
                    MotivationalQuote(quote = "The way to get started is to quit talking and begin doing."),
                    MotivationalQuote(quote = "What you get by achieving your goals is not as important as what you become by achieving your goals.")
                )
                
                defaultQuotes.forEach { quote ->
                    repository.addMotivationalQuote(quote)
                }
                Log.d(TAG, "Added ${defaultQuotes.size} default quotes")
            } else {
                Log.d(TAG, "Found ${existingQuotes.size} existing quotes")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Default Channel"
            val descriptionText = "Default notification channel"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
} 