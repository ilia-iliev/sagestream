package com.example.SageStream.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.SageStream.worker.MotivationalQuoteWorker
import com.example.SageStream.worker.MidnightResetWorker
import com.example.SageStream.receiver.AlarmReceiver
import java.util.Calendar
import java.util.concurrent.TimeUnit
import androidx.core.content.edit

class NotificationScheduler(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // Schedule all notifications for the day
    fun scheduleAllNotifications() {
        Log.d(TAG, "Scheduling all notifications")
        scheduleMidnightReset()
        scheduleDailyQuotes()
        // Also schedule with AlarmManager as backup
        scheduleAlarmManagerNotifications()
    }

    // Schedule the midnight reset worker to run daily at midnight
    private fun scheduleMidnightReset() {
        val midnight = calculateNextMidnight()
        
        val resetRequest = OneTimeWorkRequestBuilder<MidnightResetWorker>()
            .setInitialDelay(midnight, TimeUnit.MILLISECONDS)
            .addTag("midnight_reset")
            .build()
            
        workManager.enqueueUniqueWork(
            MIDNIGHT_RESET_WORK,
            ExistingWorkPolicy.REPLACE,
            resetRequest
        )
        
        Log.d(TAG, "Scheduled midnight reset for ${midnight}ms from now")
    }

    // Schedule all daily quote notifications using WorkManager
    fun scheduleDailyQuotes() {
        Log.d(TAG, "Scheduling daily quotes with WorkManager")
        
        // Cancel any existing quote notifications
        workManager.cancelAllWorkByTag("daily_quote")
        
        // Schedule each notification time
        for (i in 0 until MAX_NOTIFICATIONS) {
            if (isNotificationTimeEnabled(i)) {
                val hour = getQuoteHour(i)
                val minute = getQuoteMinute(i)
                val delay = calculateTimeUntil(hour, minute)
                
                val notificationWork = OneTimeWorkRequestBuilder<MotivationalQuoteWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setConstraints(MotivationalQuoteWorker.createConstraints())
                    .addTag("daily_quote")
                    .addTag("${DAILY_QUOTE_WORK}_$i")
                    .build()
                    
                workManager.enqueue(notificationWork)
                
                Log.d(TAG, "Scheduled WorkManager notification $i for ${hour}:${minute} (${delay}ms from now)")
            }
        }
    }

    // Schedule notifications using AlarmManager as a backup
    private fun scheduleAlarmManagerNotifications() {
        Log.d(TAG, "Scheduling daily quotes with AlarmManager backup")
        
        // Cancel existing alarms
        cancelAlarmManagerNotifications()
        
        for (i in 0 until MAX_NOTIFICATIONS) {
            if (isNotificationTimeEnabled(i)) {
                val hour = getQuoteHour(i)
                val minute = getQuoteMinute(i)
                val targetTime = calculateTargetTime(hour, minute)
                
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_SHOW_QUOTE_NOTIFICATION
                    putExtra(EXTRA_NOTIFICATION_ID, i)
                    putExtra(EXTRA_NOTIFICATION_HOUR, hour)
                    putExtra(EXTRA_NOTIFICATION_MINUTE, minute)
                }
                
                val pendingIntent = PendingIntent.getBroadcast(
                    context, 
                    i, 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                // Use setExactAndAllowWhileIdle for reliable delivery even in doze mode
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        targetTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        targetTime,
                        pendingIntent
                    )
                }
                
                Log.d(TAG, "Scheduled AlarmManager notification $i for ${hour}:${minute} at ${targetTime}")
            }
        }
    }

    // Cancel AlarmManager notifications
    private fun cancelAlarmManagerNotifications() {
        for (i in 0 until MAX_NOTIFICATIONS) {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_SHOW_QUOTE_NOTIFICATION
                putExtra(EXTRA_NOTIFICATION_ID, i)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 
                i, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    // Save notification time for a specific slot
    fun saveQuoteNotificationTime(slot: Int, hour: Int, minute: Int, enabled: Boolean) {
        sharedPreferences.edit() {
            putInt("${KEY_QUOTE_HOUR}_$slot", hour)
                .putInt("${KEY_QUOTE_MINUTE}_$slot", minute)
                .putBoolean("${KEY_NOTIFICATION_ENABLED}_$slot", enabled)
        }
            
        // Reschedule all quote notifications
        scheduleAllNotifications()
    }

    // Mark that a notification was shown for a specific time
    fun markNotificationShown(hour: Int, minute: Int) {
        val todayStart = getTodayStartTime()
        val key = "shown_${hour}_${minute}_$todayStart"
        sharedPreferences.edit()
            .putBoolean(key, true)
            .putLong("last_notification_time", System.currentTimeMillis())
            .apply()
        
        Log.d(TAG, "Marked notification as shown for $hour:$minute")
    }

    // Check if a notification was shown for a specific time today
    fun hasNotificationBeenShown(hour: Int, minute: Int): Boolean {
        val todayStart = getTodayStartTime()
        val key = "shown_${hour}_${minute}_$todayStart"
        return sharedPreferences.getBoolean(key, false)
    }

    // Get the last notification time
    fun getLastNotificationTime(): Long {
        return sharedPreferences.getLong("last_notification_time", 0L)
    }

    // Get today's start time
    private fun getTodayStartTime(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    // Get the saved hour for a specific notification slot
    fun getQuoteHour(slot: Int): Int {
        return sharedPreferences.getInt("${KEY_QUOTE_HOUR}_$slot", DEFAULT_HOURS[slot])
    }

    // Get the saved minute for a specific notification slot
    fun getQuoteMinute(slot: Int): Int {
        return sharedPreferences.getInt("${KEY_QUOTE_MINUTE}_$slot", 0)
    }

    // Check if a notification slot is enabled
    fun isNotificationTimeEnabled(slot: Int): Boolean {
        return sharedPreferences.getBoolean("${KEY_NOTIFICATION_ENABLED}_$slot", true)
    }

    // Get all enabled notification times
    fun getEnabledNotificationTimes(): List<Pair<Int, Int>> {
        return (0 until MAX_NOTIFICATIONS)
            .filter { isNotificationTimeEnabled(it) }
            .map { Pair(getQuoteHour(it), getQuoteMinute(it)) }
    }

    // Calculate milliseconds until the next occurrence of a specific time
    private fun calculateTimeUntil(hour: Int, minute: Int): Long {
        val targetTime = calculateTargetTime(hour, minute)
        return targetTime - System.currentTimeMillis()
    }

    // Calculate the target time in milliseconds
    private fun calculateTargetTime(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        var targetTime = calendar.timeInMillis
        
        // If the time has already passed today, schedule for tomorrow
        if (targetTime <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            targetTime = calendar.timeInMillis
        }
        
        return targetTime
    }

    // Calculate milliseconds until the next midnight
    private fun calculateNextMidnight(): Long {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        // Set time to the next midnight
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        return calendar.timeInMillis - now
    }

    companion object {
        private const val TAG = "NotificationScheduler"
        const val MIDNIGHT_RESET_WORK = "midnight_reset"
        const val DAILY_QUOTE_WORK = "daily_quote"
        
        const val PREFS_NAME = "notification_scheduler_prefs"
        const val KEY_QUOTE_HOUR = "quote_notification_hour"
        const val KEY_QUOTE_MINUTE = "quote_notification_minute"
        const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
        
        const val MAX_NOTIFICATIONS = 3
        val DEFAULT_HOURS = intArrayOf(8, 13, 20) // 8 AM, 1 PM, 8 PM
        
        // AlarmManager constants
        const val ACTION_SHOW_QUOTE_NOTIFICATION = "com.example.SageStream.SHOW_QUOTE_NOTIFICATION"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_NOTIFICATION_HOUR = "notification_hour"
        const val EXTRA_NOTIFICATION_MINUTE = "notification_minute"
    }
} 