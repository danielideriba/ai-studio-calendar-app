package com.example.data.remote

import com.example.data.model.CalendarListResponse
import com.example.data.model.CreateEventRequest
import com.example.data.model.EventsListResponse
import com.example.data.model.GoogleEvent
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface GoogleCalendarService {
    @GET("calendar/v3/users/me/calendarList")
    suspend fun getCalendarList(
        @Header("Authorization") authHeader: String
    ): CalendarListResponse

    @GET("calendar/v3/calendars/{calendarId}/events")
    suspend fun getEvents(
        @Header("Authorization") authHeader: String,
        @Path("calendarId") calendarId: String,
        @Query("timeMin") timeMin: String?,
        @Query("timeMax") timeMax: String?,
        @Query("singleEvents") singleEvents: Boolean = true,
        @Query("orderBy") orderBy: String = "startTime"
    ): EventsListResponse

    @POST("calendar/v3/calendars/{calendarId}/events")
    suspend fun createEvent(
        @Header("Authorization") authHeader: String,
        @Path("calendarId") calendarId: String,
        @Body request: CreateEventRequest
    ): GoogleEvent

    @DELETE("calendar/v3/calendars/{calendarId}/events/{eventId}")
    suspend fun deleteEvent(
        @Header("Authorization") authHeader: String,
        @Path("calendarId") calendarId: String,
        @Path("eventId") eventId: String
    ): Response<Unit>

    companion object {
        private const val BASE_URL = "https://www.googleapis.com/"

        fun create(): GoogleCalendarService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(GoogleCalendarService::class.java)
        }
    }
}
