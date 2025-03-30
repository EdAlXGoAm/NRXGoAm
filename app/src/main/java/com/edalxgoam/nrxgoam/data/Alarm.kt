package com.edalxgoam.nrxgoam.data

import java.util.*

data class Alarm(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val description: String,
    val category: String,
    val date: Date,
    val isEnabled: Boolean = true,
    val createdAt: Date = Date()
) 