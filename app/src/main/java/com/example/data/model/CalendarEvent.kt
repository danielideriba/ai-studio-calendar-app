package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_events")
data class CalendarEvent(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String?,
    val location: String?,
    val startTime: Long,
    val endTime: Long,
    val color: Int,
    val calendarId: String,
    val calendarName: String,
    val isDemo: Boolean
)
