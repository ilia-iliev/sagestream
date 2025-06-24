package com.example.SageStream.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.lang.reflect.Type
import com.example.SageStream.model.MotivationalQuote

class NotificationRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val _motivationalQuotes = MutableStateFlow<List<MotivationalQuote>>(getMotivationalQuotes())
    val motivationalQuotes: StateFlow<List<MotivationalQuote>> = _motivationalQuotes.asStateFlow()
    
    suspend fun addMotivationalQuote(quote: MotivationalQuote) = withContext(Dispatchers.IO) {
        updateQuotes { quotes -> quotes + quote }
    }
    
    suspend fun updateMotivationalQuote(quote: MotivationalQuote) = withContext(Dispatchers.IO) {
        updateQuotes { quotes ->
            quotes.map { if (it.id == quote.id) quote else it }
        }
    }
    
    suspend fun deleteMotivationalQuote(quoteId: String) = withContext(Dispatchers.IO) {
        updateQuotes { quotes -> quotes.filter { it.id != quoteId } }
    }
    
    suspend fun updateQuoteLastDisplayed(quoteId: String, timestamp: Long) = withContext(Dispatchers.IO) {
        updateQuotes { quotes ->
            quotes.map { if (it.id == quoteId) it.copy(lastDisplayed = timestamp) else it }
        }
    }
    
    suspend fun getNextQuoteToDisplay(): MotivationalQuote? = withContext(Dispatchers.IO) {
        _motivationalQuotes.value.minByOrNull { it.lastDisplayed }
    }
    
    suspend fun getAllQuotes(): List<MotivationalQuote> = withContext(Dispatchers.IO) {
        _motivationalQuotes.value
    }
    
    suspend fun importQuotesFromFile(fileContent: String) = withContext(Dispatchers.IO) {
        val newQuotes = fileContent.lines()
            .filter { it.isNotBlank() }
            .map { MotivationalQuote(quote = it.trim()) }
        
        if (newQuotes.isNotEmpty()) {
            updateQuotes { quotes -> quotes + newQuotes }
        }
    }
    
    private suspend fun updateQuotes(transform: (List<MotivationalQuote>) -> List<MotivationalQuote>) {
        val updatedQuotes = transform(_motivationalQuotes.value)
        _motivationalQuotes.value = updatedQuotes
        saveMotivationalQuotes(updatedQuotes)
    }
    
    private fun getMotivationalQuotes(): List<MotivationalQuote> {
        val json = prefs.getString(KEY_QUOTES, null) ?: return emptyList()
        val type: Type = object : TypeToken<List<MotivationalQuote>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
    private fun saveMotivationalQuotes(quotes: List<MotivationalQuote>) {
        val json = gson.toJson(quotes)
        prefs.edit().putString(KEY_QUOTES, json).apply()
    }
    
    companion object {
        private const val PREFS_NAME = "notification_prefs"
        private const val KEY_QUOTES = "motivational_quotes"
    }
} 