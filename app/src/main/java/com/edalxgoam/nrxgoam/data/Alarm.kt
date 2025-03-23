package com.edalxgoam.nrxgoam.data

import java.util.*

data class Alarm(
    val id: Long = System.currentTimeMillis(),
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val createdAt: Date = Date()
) 