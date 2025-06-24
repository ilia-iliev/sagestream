package com.example.SageStream

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.SageStream.databinding.ActivityMainBinding
import com.example.sagestream.notification.NotificationService
import com.example.SageStream.receiver.NotificationActionReceiver
import com.example.SageStream.ui.MotivationalQuotesFragment
import com.example.SageStream.notification.NotificationScheduler
import com.example.SageStream.repository.NotificationRepository
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Random

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val tabTitles = listOf("Quotes")
    private val fragments = listOf(
        MotivationalQuotesFragment.newInstance()
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }
    
    // Add a broadcast receiver for data update events
    private val dataUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == NotificationActionReceiver.ACTION_DATA_UPDATED) {
                val notificationType = intent.getStringExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_TYPE)
                
                // Always refresh all fragments of the relevant type, regardless of which one is visible
                when (notificationType) {
                    NotificationService.TYPE_QUOTE -> {
                        fragments.forEach { fragment ->
                            fragment.refreshData()
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        setupViewPager()
        requestNotificationPermission()
        setupFab()
        
        // Handle notification intent extras
        handleNotificationIntent(intent)
        
        // Check for missed notifications
        checkForMissedNotifications()
        
        // Register the global receiver for notification updates with high priority
        try {
            val filter = IntentFilter(NotificationActionReceiver.ACTION_DATA_UPDATED).apply {
                priority = IntentFilter.SYSTEM_HIGH_PRIORITY
            }
            ContextCompat.registerReceiver(
                this,
                dataUpdateReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }
    
    private fun handleNotificationIntent(intent: Intent?) {
        intent?.let {
            val notificationType = it.getStringExtra(NotificationService.EXTRA_NOTIFICATION_TYPE)
            val notificationId = it.getStringExtra(NotificationService.EXTRA_NOTIFICATION_ID)
            
            if (notificationType == NotificationService.TYPE_QUOTE && !notificationId.isNullOrEmpty()) {
                Log.d(TAG, "Handling notification intent for quote: $notificationId")
                // Navigate to the quotes fragment or show the specific quote
                binding.viewPager.currentItem = 0 // Switch to quotes tab
            }
        }
    }
    
    private fun checkForMissedNotifications() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val scheduler = NotificationScheduler(this@MainActivity)
                val repository = NotificationRepository(this@MainActivity)
                val notificationService = NotificationService(this@MainActivity)
                
                // Check if there are any enabled notification times that should have fired
                val enabledTimes = scheduler.getEnabledNotificationTimes()
                val currentTime = System.currentTimeMillis()
                
                // Only check for missed notifications if this is the first time opening the app today
                // or if we haven't shown any notifications today yet
                val lastNotificationTime = scheduler.getLastNotificationTime()
                val todayStart = getTodayStartTime()
                
                // Only proceed if we haven't shown any notifications today
                if (lastNotificationTime < todayStart) {
                    Log.d(TAG, "Checking for missed notifications - no notifications shown today yet")
                    
                    for (timePair in enabledTimes) {
                        val hour = timePair.first
                        val minute = timePair.second
                        val targetTime = calculateTargetTime(hour, minute)
                        
                        // Only show missed notification if:
                        // 1. The time has passed today
                        // 2. It's within the last 2 hours (to avoid showing very old missed notifications)
                        // 3. We haven't shown a notification for this time slot today
                        if (targetTime < currentTime && 
                            targetTime > currentTime - 2 * 60 * 60 * 1000 && // Within last 2 hours
                            !scheduler.hasNotificationBeenShown(hour, minute)) {
                            
                            Log.d(TAG, "Found missed notification for time $hour:$minute")
                            
                            // Show the missed notification
                            val quotes = repository.getAllQuotes()
                            if (quotes.isNotEmpty()) {
                                val randomQuote = quotes[Random().nextInt(quotes.size)]
                                notificationService.showQuoteNotification(randomQuote)
                                repository.updateQuoteLastDisplayed(randomQuote.id, currentTime)
                                scheduler.markNotificationShown(hour, minute)
                                
                                // Only show one missed notification to avoid spam
                                break
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "Skipping missed notification check - notifications already shown today")
                }
                
                // Reschedule notifications for the future
                scheduler.scheduleAllNotifications()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for missed notifications", e)
            }
        }
    }
    
    private fun getTodayStartTime(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun calculateTargetTime(hour: Int, minute: Int): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
        calendar.set(java.util.Calendar.MINUTE, minute)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        
        var targetTime = calendar.timeInMillis
        
        // If the time has already passed today, it's for today
        if (targetTime > System.currentTimeMillis()) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
            targetTime = calendar.timeInMillis
        }
        
        return targetTime
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            // Unregister the receiver
            unregisterReceiver(dataUpdateReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun setupViewPager() {
        // Set up ViewPager with fragments
        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = fragments.size
            override fun createFragment(position: Int) = fragments[position]
        }
        
        // Connect TabLayout with ViewPager
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }
    
    private fun setupFab() {
        binding.fab.setOnClickListener {
            when (val currentFragment = fragments[binding.viewPager.currentItem]) {
                else -> currentFragment.showAddQuoteDialog()
            }
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    companion object {
        private const val TAG = "MainActivity"
    }
} 