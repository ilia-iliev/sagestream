package com.example.SageStream.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import com.example.SageStream.notification.NotificationScheduler
import com.example.sagestream.notification.NotificationService
import com.example.SageStream.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Random

class MotivationalQuoteWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = NotificationRepository(context)
    private val notificationService = NotificationService(context)
    private val scheduler = NotificationScheduler(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "MotivationalQuoteWorker started")
            
            // Check if there are any quotes available
            val quotes = repository.getAllQuotes()
            if (quotes.isEmpty()) {
                Log.w(TAG, "No quotes available for notification")
                return@withContext Result.success()
            }

            // Get a random quote to display
            val randomQuote = getRandomQuote(quotes)
            if (randomQuote != null) {
                Log.d(TAG, "Showing notification for quote: ${randomQuote.quote.take(50)}...")
                notificationService.showQuoteNotification(randomQuote)
                repository.updateQuoteLastDisplayed(randomQuote.id, System.currentTimeMillis())
                
                // Mark this notification as shown for the current time
                val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val currentMinute = java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE)
                scheduler.markNotificationShown(currentHour, currentMinute)
            } else {
                Log.w(TAG, "Failed to get random quote")
            }

            // Schedule the next notification
            scheduler.scheduleDailyQuotes()
            
            Log.d(TAG, "MotivationalQuoteWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in MotivationalQuoteWorker", e)
            Result.failure()
        }
    }

    private fun getRandomQuote(quotes: List<com.example.SageStream.model.MotivationalQuote>): com.example.SageStream.model.MotivationalQuote? {
        return if (quotes.isNotEmpty()) {
            val random = Random()
            quotes[random.nextInt(quotes.size)]
        } else null
    }

    companion object {
        private const val TAG = "MotivationalQuoteWorker"
        const val KEY_QUOTE_ID = "quote_id"
        
        // Create constraints for reliable background execution
        fun createConstraints(): Constraints {
            return Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
                .build()
        }
    }
} 