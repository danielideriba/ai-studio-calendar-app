package com.example.data.local

import androidx.room.*
import com.example.data.model.CalendarEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM calendar_events ORDER BY startTime ASC")
    fun getAllEventsFlow(): Flow<List<CalendarEvent>>

    @Query("SELECT * FROM calendar_events WHERE startTime >= :startTime AND endTime <= :endTime ORDER BY startTime ASC")
    suspend fun getEventsInRange(startTime: Long, endTime: Long): List<CalendarEvent>

    @Query("SELECT * FROM calendar_events WHERE isDemo = :isDemo ORDER BY startTime ASC")
    suspend fun getEventsByDemoStatus(isDemo: Boolean): List<CalendarEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<CalendarEvent>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEvent)

    @Query("DELETE FROM calendar_events WHERE id = :eventId")
    suspend fun deleteEventById(eventId: String)

    @Query("DELETE FROM calendar_events WHERE isDemo = 1")
    suspend fun clearDemoEvents()

    @Query("DELETE FROM calendar_events WHERE isDemo = 0")
    suspend fun clearSyncedEvents()
}
