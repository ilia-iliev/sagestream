package com.example.SageStream.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.SageStream.R
import com.example.SageStream.adapter.QuotesAdapter
import com.example.SageStream.databinding.DialogEditQuoteBinding
import com.example.SageStream.databinding.DialogTimePickerBinding
import com.example.SageStream.databinding.FragmentMotivationalQuotesBinding
import com.example.SageStream.model.MotivationalQuote
import com.example.SageStream.notification.NotificationScheduler
import com.example.SageStream.repository.NotificationRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MotivationalQuotesFragment : Fragment() {

    private var _binding: FragmentMotivationalQuotesBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var repository: NotificationRepository
    private lateinit var scheduler: NotificationScheduler
    private lateinit var adapter: QuotesAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMotivationalQuotesBinding.inflate(inflater, container, false)
        setupMenu()
        return binding.root
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: android.view.MenuInflater) {
                menuInflater.inflate(R.menu.quotes_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_import_quotes -> {
                        showImportQuotesDialog()
                        true
                    }
                    R.id.action_set_notification_time -> {
                        showTimePickerDialog()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        repository = NotificationRepository(requireContext())
        scheduler = NotificationScheduler(requireContext())
        
        setupRecyclerView()
        observeQuotes()
    }
    
    private fun showTimePickerDialog() {
        // Create a list of time slots
        val timeSlots = List(NotificationScheduler.MAX_NOTIFICATIONS) { index ->
            Triple(
                index,
                scheduler.getQuoteHour(index),
                scheduler.getQuoteMinute(index)
            )
        }
        
        // Create a list of switches for each time slot
        val switches = timeSlots.map { (index, hour, minute) ->
            val switch = SwitchCompat(requireContext()).apply {
                text = formatTime(hour, minute)
                isChecked = scheduler.isNotificationTimeEnabled(index)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        showTimePickerForSlot(index, hour, minute) { newHour, newMinute ->
                            // Update the switch text with the new time
                            text = formatTime(newHour, newMinute)
                        }
                    } else {
                        scheduler.saveQuoteNotificationTime(index, hour, minute, false)
                    }
                }
            }
            switch
        }
        
        // Create a vertical layout for the switches
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            switches.forEach { addView(it) }
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Set Time")
            .setView(layout)
            .setPositiveButton("Done", null)
            .show()
    }
    
    private fun showTimePickerForSlot(
        slot: Int,
        currentHour: Int,
        currentMinute: Int,
        onTimeUpdated: (hour: Int, minute: Int) -> Unit
    ) {
        val dialogBinding = DialogTimePickerBinding.inflate(layoutInflater)
        
        // Set current notification time
        dialogBinding.timePicker.hour = currentHour
        dialogBinding.timePicker.minute = currentMinute
        
        AlertDialog.Builder(requireContext())
            .setTitle("Set Time for Quote ${slot + 1}")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val hour = dialogBinding.timePicker.hour
                val minute = dialogBinding.timePicker.minute
                
                // Save the new notification time
                scheduler.saveQuoteNotificationTime(slot, hour, minute, true)
                
                // Update the switch text
                onTimeUpdated(hour, minute)
                
                // Show confirmation toast
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                }
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                Toast.makeText(
                    requireContext(),
                    "Quote ${slot + 1} set for ${timeFormat.format(calendar.time)}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun formatTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(calendar.time)
    }
    
    private fun setupRecyclerView() {
        adapter = QuotesAdapter(
            onEditClick = { quote -> showEditQuoteDialog(quote) },
            onDeleteClick = { quote -> deleteQuote(quote) }
        )
        
        binding.quotesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MotivationalQuotesFragment.adapter
        }
    }
    
    private fun observeQuotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.motivationalQuotes.collectLatest { quotes ->
                adapter.submitList(quotes)
                
                // Show empty state if there are no quotes
                binding.emptyStateTextView.visibility = if (quotes.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
    
    fun showAddQuoteDialog() {
        showEditQuoteDialog(null)
    }
    
    private fun showEditQuoteDialog(quote: MotivationalQuote? = null) {
        val dialogBinding = DialogEditQuoteBinding.inflate(layoutInflater)
        val isNewQuote = quote == null
        
        // Populate fields if editing an existing quote
        if (!isNewQuote) {
            dialogBinding.quoteEditText.setText(quote?.quote)
        }
        
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(if (isNewQuote) "Add Quote" else "Edit Quote")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val quoteText = dialogBinding.quoteEditText.text.toString()
                
                if (quoteText.isBlank()) {
                    Toast.makeText(requireContext(), "Quote cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (isNewQuote) {
                    val newQuote = MotivationalQuote(
                        quote = quoteText
                    )
                    saveQuote(newQuote)
                } else {
                    val updatedQuote = quote!!.copy(
                        quote = quoteText
                    )
                    saveQuote(updatedQuote)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
            
        dialog.show()
    }
    
    private fun saveQuote(quote: MotivationalQuote) {
        viewLifecycleOwner.lifecycleScope.launch {
            if (quote.id in adapter.currentList.map { it.id }) {
                repository.updateMotivationalQuote(quote)
            } else {
                repository.addMotivationalQuote(quote)
            }
        }
    }
    
    private fun deleteQuote(quote: MotivationalQuote) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Quote")
            .setMessage("Are you sure you want to delete this quote?")
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.deleteMotivationalQuote(quote.id)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showImportQuotesDialog() {
        val dialogBinding = DialogEditQuoteBinding.inflate(layoutInflater)
        dialogBinding.quoteInputLayout.hint = "Paste quotes (one per line)"
        dialogBinding.quoteEditText.minLines = 5
        
        AlertDialog.Builder(requireContext())
            .setTitle("Import Quotes")
            .setView(dialogBinding.root)
            .setPositiveButton("Import") { _, _ ->
                val quotesText = dialogBinding.quoteEditText.text.toString()
                
                if (quotesText.isBlank()) {
                    Toast.makeText(requireContext(), "No quotes to import", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.importQuotesFromFile(quotesText)
                    Toast.makeText(requireContext(), "Quotes imported successfully", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    /**
     * Public method to refresh data when called from MainActivity
     */
    fun refreshData() {
        if (view != null && isAdded) {
            viewLifecycleOwner.lifecycleScope.launch {
                repository.motivationalQuotes.collectLatest { quotes ->
                    adapter.submitList(quotes)
                    binding.emptyStateTextView.visibility = if (quotes.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }
    
    companion object {
        fun newInstance() = MotivationalQuotesFragment()
    }
} 