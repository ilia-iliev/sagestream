package com.example.SageStream.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.SageStream.model.MotivationalQuote
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class NotificationRepositoryTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockSharedPrefs: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var repository: NotificationRepository
    private lateinit var gson: Gson

    @Before
    fun setup() {
        gson = Gson()
        
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPrefs)
        `when`(mockSharedPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockSharedPrefs.getString(eq("motivational_quotes"), any())).thenReturn("[]")
        `when`(mockSharedPrefs.getString(eq("birthdays"), any())).thenReturn("[]")
        `when`(mockSharedPrefs.getString(eq("daily_tasks"), any())).thenReturn("[]")
        
        repository = NotificationRepository(mockContext)
    }

    @Test
    fun `test add motivational quote`() = runTest {
        val quote = MotivationalQuote(
            id = "test-id",
            quote = "Test quote"
        )
        
        repository.addMotivationalQuote(quote)
        
        val quotes = repository.motivationalQuotes.first()
        assertEquals(1, quotes.size)
        assertEquals("Test quote", quotes[0].quote)
        
        verify(mockEditor).putString(eq("motivational_quotes"), anyString())
        verify(mockEditor).apply()
    }

    @Test
    fun `test get next quote to display returns oldest quote first`() = runTest {
        val quote1 = MotivationalQuote(
            id = "test-id-1",
            quote = "Test quote 1",
            lastDisplayed = 1000L
        )
        
        val quote2 = MotivationalQuote(
            id = "test-id-2",
            quote = "Test quote 2",
            lastDisplayed = 500L
        )
        
        repository.addMotivationalQuote(quote1)
        repository.addMotivationalQuote(quote2)
        
        val nextQuote = repository.getNextQuoteToDisplay()
        
        assertNotNull(nextQuote)
        assertEquals("test-id-2", nextQuote.id)
        assertEquals("Test quote 2", nextQuote.quote)
    }

    @Test
    fun `test update quote last displayed`() = runTest {
        val quote = MotivationalQuote(
            id = "test-id",
            quote = "Test quote",
            lastDisplayed = 0L
        )
        
        repository.addMotivationalQuote(quote)
        repository.updateQuoteLastDisplayed("test-id", 1000L)
        
        val quotes = repository.motivationalQuotes.first()
        assertEquals(1, quotes.size)
        assertEquals(1000L, quotes[0].lastDisplayed)
    }
} 