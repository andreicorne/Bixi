package com.example.bixi.helper

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Utils {

    private val isoFormat: SimpleDateFormat
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf
        }

    fun calendarToUtcIsoString(calendar: Calendar): String {
        return isoFormat.format(calendar.time)
    }

    fun dateToUtcIsoString(date: Date): String {
        return isoFormat.format(date)
    }

    fun utcIsoStringToCalendar(isoString: String): Calendar {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.time = isoFormat.parse(isoString) ?: Date()
        return calendar
    }
}