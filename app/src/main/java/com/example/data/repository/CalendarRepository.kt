package com.example.data.repository

import android.util.Log
import com.example.data.local.EventDao
import com.example.data.model.CalendarEvent
import com.example.data.model.CreateEventRequest
import com.example.data.model.EventTime
import com.example.data.remote.GoogleCalendarService
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class CalendarRepository(
    private val eventDao: EventDao,
    private val apiService: GoogleCalendarService
) {
    val allEventsFlow: Flow<List<CalendarEvent>> = eventDao.getAllEventsFlow()

    // Map Google Calendar color IDs to clean polished Material 3 color integers
    fun getColorByGoogleId(colorId: String?): Int {
        return when (colorId) {
            "1" -> 0xFFA4BDFC.toInt() // Lavender-Blue
            "2" -> 0xFF7AE7BF.toInt() // Sage / Emerald Green
            "3" -> 0xFFDBADFF.toInt() // Grape / Lilac
            "4" -> 0xFFFF887C.toInt() // Flamingo / Salmon
            "5" -> 0xFFFBD75B.toInt() // Banana / Sunny Yellow
            "6" -> 0xFFFFB878.toInt() // Tangerine / Warm Orange
            "7" -> 0xFF46D6DB.toInt() // Peacock / Cyan
            "8" -> 0xFFE1E1E1.toInt() // Graphite / Calm Gray
            "9" -> 0xFF5484ED.toInt() // Blueberry / Deep Ocean
            "10" -> 0xFF51B749.toInt() // Basil / Forest Green
            "11" -> 0xFFDC2127.toInt() // Tomato / Hot Red
            else -> 0xFF6200EE.toInt() // Default Primary Indigo
        }
    }

    suspend fun loadDemoEventsIfEmpty() {
        val existingDemo = eventDao.getEventsByDemoStatus(true)
        if (existingDemo.isEmpty()) {
            val baseTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val demoList = listOf(
                CalendarEvent(
                    id = "demo_1",
                    title = "🌅 Morning Yoga & Mindfulness",
                    description = "Start the day fresh with some deep breathing and active stretching routines.",
                    location = "Living Room",
                    startTime = baseTime + 7 * 3600000 + 30 * 60000, // 07:30
                    endTime = baseTime + 8 * 3600000 + 15 * 60000, // 08:15
                    color = 0xFFA4BDFC.toInt(), // Lavender
                    calendarId = "demo_cal",
                    calendarName = "Healthy Habits",
                    isDemo = true
                ),
                CalendarEvent(
                    id = "demo_2",
                    title = "☕ Daily Sync & Product Align",
                    description = "Aligning team on targets, review progress blockers, and walk through current sprint priorities.",
                    location = "Google Meet (meet.google.com/abc-xyz)",
                    startTime = baseTime + 9 * 3600000, // 09:00
                    endTime = baseTime + 10 * 3600000, // 10:00
                    color = 0xFF5484ED.toInt(), // Blueberry
                    calendarId = "demo_cal",
                    calendarName = "Work Sync",
                    isDemo = true
                ),
                CalendarEvent(
                    id = "demo_3",
                    title = "🎨 Design Review with UX Team",
                    description = "Reviewing component states, typography scales, spacing density, and color accessibility benchmarks.",
                    location = "Design Lab (Room 4B)",
                    startTime = baseTime + 11 * 3600000, // 11:00
                    endTime = baseTime + 12 * 3600000 + 30 * 60000, // 12:30
                    color = 0xFFDBADFF.toInt(), // Grape
                    calendarId = "demo_cal",
                    calendarName = "Work Sync",
                    isDemo = true
                ),
                CalendarEvent(
                    id = "demo_4",
                    title = "🍔 Developer Tech Lunch",
                    description = "Grabbing tech talk and burgers with the mobile engineers. Discussing modern desugaring features and Material Design 3.",
                    location = "Main Street Café",
                    startTime = baseTime + 13 * 3600000, // 13:00
                    endTime = baseTime + 14 * 3600000, // 14:00
                    color = 0xFFFBD75B.toInt(), // Banana Yellow
                    calendarId = "demo_cal",
                    calendarName = "Social",
                    isDemo = true
                ),
                CalendarEvent(
                    id = "demo_5",
                    title = "💬 Product Strategy & Roadmap Plan",
                    description = "Writing key requirements for the unified calendar dashboard capability block.",
                    location = "Huddle Space 1",
                    startTime = baseTime + 14 * 3600000 + 30 * 60000, // 14:30
                    endTime = baseTime + 15 * 3600000 + 45 * 60000, // 15:45
                    color = 0xFFFFB878.toInt(), // Orange
                    calendarId = "demo_cal",
                    calendarName = "Work Sync",
                    isDemo = true
                ),
                CalendarEvent(
                    id = "demo_6",
                    title = "🏋️ Gym & Core Cardio Session",
                    description = "HIIT run, core training, and physical muscle regeneration exercises.",
                    location = "Dynamic Performance Gym",
                    startTime = baseTime + 17 * 3600000 + 30 * 60000, // 17:30
                    endTime = baseTime + 19 * 3600000, // 19:00
                    color = 0xFF7AE7BF.toInt(), // Sage Green
                    calendarId = "demo_cal",
                    calendarName = "Healthy Habits",
                    isDemo = true
                ),
                CalendarEvent(
                    id = "demo_7",
                    title = "🍕 Dinner & Movie with Sarah",
                    description = "Catching up over deep-dish pizza and reviewing sci-fi documentaries.",
                    location = "Downtown Pizzeria & Cinema",
                    startTime = baseTime + 20 * 3600000, // 20:00
                    endTime = baseTime + 22 * 3600000 + 30 * 60000, // 22:30
                    color = 0xFFFF887C.toInt(), // Flamingo Pink
                    calendarId = "demo_cal",
                    calendarName = "Personal",
                    isDemo = true
                )
            )
            eventDao.insertEvents(demoList)
        }
    }

    suspend fun clearDemoEvents() {
        eventDao.clearDemoEvents()
    }

    suspend fun clearSyncedEvents() {
        eventDao.clearSyncedEvents()
    }

    suspend fun insertLocalEvent(event: CalendarEvent) {
        eventDao.insertEvent(event)
    }

    suspend fun deleteEventById(eventId: String) {
        eventDao.deleteEventById(eventId)
    }

    // Connect to Google Calendar v3 REST endpoints to sync events around chosen date
    suspend fun syncWithGoogleCalendar(accessToken: String, centerDateMs: Long): Result<Unit> {
        return try {
            val authHeader = "Bearer $accessToken"
            
            // Calc timeMin and timeMax for search: +- 7 days
            val cal = Calendar.getInstance()
            cal.timeInMillis = centerDateMs
            cal.add(Calendar.DAY_OF_YEAR, -7)
            val timeMinString = formatRfc3339(cal.time)

            cal.timeInMillis = centerDateMs
            cal.add(Calendar.DAY_OF_YEAR, 7)
            val timeMaxString = formatRfc3339(cal.time)

            Log.d("CalendarRepo", "Syncing events between $timeMinString and $timeMaxString")

            // 1. Fetch Google events of primary calendar
            val response = apiService.getEvents(
                authHeader = authHeader,
                calendarId = "primary",
                timeMin = timeMinString,
                timeMax = timeMaxString
            )

            val googleEvents = response.items ?: emptyList()
            
            // 2. Clear old non-demo sync events
            eventDao.clearSyncedEvents()

            // 3. Map GoogleEvent items to Local CalendarEvent entities
            val localEvents = googleEvents.mapNotNull { g ->
                val startMs = parseRfc3339ToMs(g.start?.dateTime ?: g.start?.date)
                val endMs = parseRfc3339ToMs(g.end?.dateTime ?: g.end?.date)
                
                if (startMs == null || endMs == null) return@mapNotNull null

                CalendarEvent(
                    id = g.id,
                    title = g.summary ?: "(No Title)",
                    description = g.description,
                    location = g.location,
                    startTime = startMs,
                    endTime = endMs,
                    color = getColorByGoogleId(g.colorId),
                    calendarId = "primary",
                    calendarName = "Google Workspace (Primary)",
                    isDemo = false
                )
            }

            // 4. Save to database
            eventDao.insertEvents(localEvents)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CalendarRepo", "Error fetching from Google Calendar: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun pushEventToGoogle(
        accessToken: String,
        title: String,
        description: String?,
        location: String?,
        startTimeMs: Long,
        endTimeMs: Long,
        colorId: String?
    ): Result<CalendarEvent> {
        return try {
            val authHeader = "Bearer $accessToken"
            
            val startRfc = formatRfc3339(Date(startTimeMs))
            val endRfc = formatRfc3339(Date(endTimeMs))

            val request = CreateEventRequest(
                summary = title,
                description = description,
                location = location,
                start = EventTime(dateTime = startRfc, date = null),
                end = EventTime(dateTime = endRfc, date = null),
                colorId = colorId
            )

            val created = apiService.createEvent(authHeader, "primary", request)
            
            val startMs = parseRfc3339ToMs(created.start?.dateTime ?: created.start?.date) ?: startTimeMs
            val endMs = parseRfc3339ToMs(created.end?.dateTime ?: created.end?.date) ?: endTimeMs

            val localEvent = CalendarEvent(
                id = created.id,
                title = created.summary ?: title,
                description = created.description ?: description,
                location = created.location ?: location,
                startTime = startMs,
                endTime = endMs,
                color = getColorByGoogleId(created.colorId),
                calendarId = "primary",
                calendarName = "Google Workspace (Primary)",
                isDemo = false
            )

            eventDao.insertEvent(localEvent)
            Result.success(localEvent)
        } catch (e: Exception) {
            Log.e("CalendarRepo", "Failed to push event: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteEventFromGoogle(accessToken: String, eventId: String): Result<Unit> {
        return try {
            val authHeader = "Bearer $accessToken"
            apiService.deleteEvent(authHeader, "primary", eventId)
            eventDao.deleteEventById(eventId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CalendarRepo", "Failed to delete remote event: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Parse RFC-3339 dates like "2026-06-02T10:00:00Z" or "2026-06-02T10:00:00-07:00"
    private fun parseRfc3339ToMs(dateStr: String?): Long? {
        if (dateStr.isNullOrEmpty()) return null
        val formats = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (fmt in formats) {
            try {
                val parser = SimpleDateFormat(fmt, Locale.US)
                if (fmt.contains("XXX")) {
                    // Java 7+ handles XXX, but SimpleDateFormat in Android sdk < 24 might struggle,
                    // we normalize to Z or basic offsets or just fallback desugared formats
                }
                if (fmt == "yyyy-MM-dd") {
                    parser.timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = parser.parse(dateStr)
                if (date != null) {
                    return date.time
                }
            } catch (e: Exception) {
                // Keep checking
            }
        }
        // Fallback using modern java.time for minSdk 24+
        return try {
            java.time.Instant.parse(dateStr).toEpochMilli()
        } catch (e1: Exception) {
            try {
                // If it's a date-only (like 2026-06-02)
                java.time.LocalDate.parse(dateStr)
                    .atStartOfDay(java.time.ZoneId.of("UTC"))
                    .toInstant()
                    .toEpochMilli()
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun formatRfc3339(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(date)
    }
}
