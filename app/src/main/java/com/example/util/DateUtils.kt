package com.example.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    fun formatDateHeader(timeMs: Long): String {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)
        return sdf.format(Date(timeMs))
    }

    fun formatDateShort(timeMs: Long): String {
        val sdf = SimpleDateFormat("MMM d", Locale.US)
        return sdf.format(Date(timeMs))
    }

    fun formatTimeRange(startMs: Long, endMs: Long): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.US)
        return "${sdf.format(Date(startMs))} - ${sdf.format(Date(endMs))}"
    }

    fun formatHour(hour: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
        }
        val sdf = SimpleDateFormat("h:mm a", Locale.US)
        return sdf.format(calendar.time)
    }

    fun getDayOfMonth(timeMs: Long): String {
        val sdf = SimpleDateFormat("d", Locale.US)
        return sdf.format(Date(timeMs))
    }

    fun getDayOfWeekAbbreviation(timeMs: Long): String {
        val sdf = SimpleDateFormat("EEE", Locale.US).apply {
            // Ensure abbreviation is all capitals
        }
        return sdf.format(Date(timeMs)).uppercase(Locale.US)
    }

    // Centering the date: returns 7 days of the week containing selectedDateMs
    fun getWeekDaysAround(selectedDateMs: Long): List<Long> {
        val list = mutableListOf<Long>()
        val cal = Calendar.getInstance().apply {
            timeInMillis = selectedDateMs
            // Go back to Sunday
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        for (i in 0 until 7) {
            list.add(cal.timeInMillis)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return list
    }
}
