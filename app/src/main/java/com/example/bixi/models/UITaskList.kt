package com.example.bixi.models

import java.time.LocalDateTime

data class UITaskList(
    val id: String,
    val title: String,
    val description: String,
    val assigneeName: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime
) {
    fun getFormattedPeriod(): String {
        return "${startDate.dayOfMonth}.${startDate.monthValue}.${startDate.year} - " +
                "${endDate.dayOfMonth}.${endDate.monthValue}.${endDate.year}"
    }
}