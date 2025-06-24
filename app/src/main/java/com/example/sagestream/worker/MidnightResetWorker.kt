package com.example.SageStream.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.SageStream.notification.NotificationScheduler
import com.example.SageStream.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MidnightResetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = NotificationRepository(context)
    private val scheduler = NotificationScheduler(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "MidnightResetWorker started - rescheduling notifications for new day")
            
            // Schedule the next midnight reset and all daily notifications
            scheduler.scheduleAllNotifications()
            
            Log.d(TAG, "MidnightResetWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in MidnightResetWorker", e)
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "MidnightResetWorker"
    }
} 