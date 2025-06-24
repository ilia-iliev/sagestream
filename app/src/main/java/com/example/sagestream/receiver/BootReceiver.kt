package com.example.SageStream.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.SageStream.notification.NotificationScheduler

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == Intent.ACTION_PACKAGE_REPLACED) {
            
            // Reschedule all notifications after device reboot
            val scheduler = NotificationScheduler(context)
            scheduler.scheduleAllNotifications()
        }
    }
} 