package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CalendarListResponse(
    @Json(name = "items") val items: List<CalendarItem>?
)

@JsonClass(generateAdapter = true)
data class CalendarItem(
    @Json(name = "id") val id: String,
    @Json(name = "summary") val summary: String,
    @Json(name = "primary") val primary: Boolean?
)

@JsonClass(generateAdapter = true)
data class EventsListResponse(
    @Json(name = "items") val items: List<GoogleEvent>?
)

@JsonClass(generateAdapter = true)
data class GoogleEvent(
    @Json(name = "id") val id: String,
    @Json(name = "summary") val summary: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "location") val location: String?,
    @Json(name = "start") val start: EventTime?,
    @Json(name = "end") val end: EventTime?,
    @Json(name = "colorId") val colorId: String?,
    @Json(name = "htmlLink") val htmlLink: String?
)

@JsonClass(generateAdapter = true)
data class EventTime(
    @Json(name = "dateTime") val dateTime: String?, // "2026-06-02T10:00:00Z"
    @Json(name = "date") val date: String? // "2026-06-02" for all-day events
)

@JsonClass(generateAdapter = true)
data class CreateEventRequest(
    @Json(name = "summary") val summary: String,
    @Json(name = "description") val description: String?,
    @Json(name = "location") val location: String?,
    @Json(name = "start") val start: EventTime,
    @Json(name = "end") val end: EventTime,
    @Json(name = "colorId") val colorId: String? = null
)
