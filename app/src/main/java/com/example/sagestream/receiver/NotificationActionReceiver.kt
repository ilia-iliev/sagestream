package com.example.SageStream.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.sagestream.notification.NotificationService
import com.example.SageStream.repository.NotificationRepository
import kotlinx.coroutines.runBlocking

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == ACTION_MARK_DONE) {
            handleMarkDoneAction(context, intent)
        }
    }

    private fun handleMarkDoneAction(context: Context, intent: Intent) {
        val notificationType = intent.getStringExtra(EXTRA_NOTIFICATION_TYPE)
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        if (notificationId != -1 && !itemId.isNullOrEmpty() && !notificationType.isNullOrEmpty()) {
            val repository = NotificationRepository(context)
            
            runBlocking {
                repository.updateQuoteLastDisplayed(itemId, System.currentTimeMillis())
            }

            NotificationService(context).cancelNotification(notificationId)
            broadcastUpdate(context, notificationType, itemId)
        }
    }

    private fun broadcastUpdate(context: Context, notificationType: String, itemId: String) {
        val broadcastIntent = Intent(ACTION_DATA_UPDATED).apply {
            putExtra(EXTRA_NOTIFICATION_TYPE, notificationType)
            putExtra(EXTRA_ITEM_ID, itemId)
            flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
        }
        context.applicationContext.sendBroadcast(broadcastIntent)
    }
    
    companion object {
        const val ACTION_MARK_DONE = "com.example.SageStream.ACTION_MARK_DONE"
        const val ACTION_DATA_UPDATED = "com.example.SageStream.ACTION_DATA_UPDATED"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
        const val EXTRA_ITEM_ID = "item_id"
    }
} 