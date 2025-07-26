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
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        
        // Only reschedule notifications for the future, don't show missed ones
        rescheduleNotifications()
        
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
    
    private fun rescheduleNotifications() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val scheduler = NotificationScheduler(this@MainActivity)
                scheduler.scheduleAllNotifications()
                Log.d(TAG, "Notifications rescheduled for the future.")
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling notifications", e)
            }
        }
    }
    
    companion object {
        private const val TAG = "MainActivity"
    }
} 