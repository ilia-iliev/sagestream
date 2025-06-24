package com.example.SageStream.model

import java.util.UUID

data class MotivationalQuote(
    val id: String = UUID.randomUUID().toString(),
    val quote: String,
    var lastDisplayed: Long = 0L  // Timestamp to track when it was last shown
) 