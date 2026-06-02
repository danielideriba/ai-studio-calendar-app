package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CalendarEvent
import com.example.data.remote.GoogleCalendarService
import com.example.data.repository.CalendarRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

sealed interface SyncState {
    object Idle : SyncState
    object Loading : SyncState
    data class Success(val message: String) : SyncState
    data class Error(val error: String) : SyncState
}

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences("calendar_settings", Context.MODE_PRIVATE)
    
    private val database = AppDatabase.getDatabase(application)
    private val apiService = GoogleCalendarService.create()
    private val repository = CalendarRepository(database.eventDao(), apiService)

    // State of selected day focuses (in ms)
    private val _selectedDateMs = MutableStateFlow(System.currentTimeMillis())
    val selectedDateMs: StateFlow<Long> = _selectedDateMs.asStateFlow()

    // Mode: "DEMO" or "LIVE"
    private val _useLiveSync = MutableStateFlow(sharedPrefs.getString("active_mode", "DEMO") == "LIVE")
    val useLiveSync: StateFlow<Boolean> = _useLiveSync.asStateFlow()

    // Token & configuration
    private val _accessToken = MutableStateFlow(sharedPrefs.getString("google_access_token", "") ?: "")
    val accessToken: StateFlow<String> = _accessToken.asStateFlow()

    // Status tracker
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // Load all events from DB
    val allEvents: StateFlow<List<CalendarEvent>> = repository.allEventsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered events to displaying on the active day
    val filteredEvents: StateFlow<List<CalendarEvent>> = combine(allEvents, _selectedDateMs) { events, selectedMs ->
        val targetCal = Calendar.getInstance().apply { timeInMillis = selectedMs }
        val targetYear = targetCal.get(Calendar.YEAR)
        val targetDay = targetCal.get(Calendar.DAY_OF_YEAR)

        events.filter { event ->
            val eventCalStart = Calendar.getInstance().apply { timeInMillis = event.startTime }
            val eventCalEnd = Calendar.getInstance().apply { timeInMillis = event.endTime }
            
            // Matches if the event starts, ends or spans over the target day
            val startsToday = (eventCalStart.get(Calendar.YEAR) == targetYear && eventCalStart.get(Calendar.DAY_OF_YEAR) == targetDay)
            val endsToday = (eventCalEnd.get(Calendar.YEAR) == targetYear && eventCalEnd.get(Calendar.DAY_OF_YEAR) == targetDay)
            val spansToday = (event.startTime < selectedMs && event.endTime > selectedMs + 24 * 3600000)
            
            (startsToday || endsToday || spansToday) && (event.isDemo == !_useLiveSync.value)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Automatically prefill demo events on startup
        viewModelScope.launch {
            repository.loadDemoEventsIfEmpty()
        }
    }

    fun selectDate(timeMs: Long) {
        _selectedDateMs.value = timeMs
    }

    fun setLiveSyncMode(enabled: Boolean) {
        _useLiveSync.value = enabled
        sharedPrefs.edit().putString("active_mode", if (enabled) "LIVE" else "DEMO").apply()
        
        // Refresh triggers
        viewModelScope.launch {
            if (!enabled) {
                repository.loadDemoEventsIfEmpty()
            } else if (_accessToken.value.isNotEmpty()) {
                syncWithGoogle()
            }
        }
    }

    fun saveAccessToken(token: String) {
        _accessToken.value = token
        sharedPrefs.edit().putString("google_access_token", token).apply()
        if (token.isNotEmpty() && _useLiveSync.value) {
            syncWithGoogle()
        }
    }

    fun logout() {
        saveAccessToken("")
        setLiveSyncMode(false)
        viewModelScope.launch {
            repository.clearSyncedEvents()
        }
    }

    fun syncWithGoogle() {
        val token = _accessToken.value
        if (token.isEmpty()) {
            _syncState.value = SyncState.Error("Access token is empty. Please authenticate with Google first.")
            return
        }

        viewModelScope.launch {
            _syncState.value = SyncState.Loading
            val result = repository.syncWithGoogleCalendar(token, _selectedDateMs.value)
            if (result.isSuccess) {
                _syncState.value = SyncState.Success("Synced successfully!")
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown sync error"
                _syncState.value = SyncState.Error(errorMsg)
            }
        }
    }

    fun addEvent(
        title: String,
        description: String?,
        location: String?,
        startTimeMs: Long,
        endTimeMs: Long,
        colorHex: Int
    ) {
        viewModelScope.launch {
            if (_useLiveSync.value) {
                val token = _accessToken.value
                if (token.isEmpty()) {
                    _syncState.value = SyncState.Error("Log in to Google to add live events.")
                    return@launch
                }
                _syncState.value = SyncState.Loading
                // Try mapping Hex color back to closest Google Color ID
                val colorId = when (colorHex) {
                    0xFFA4BDFC.toInt() -> "1"
                    0xFF7AE7BF.toInt() -> "2"
                    0xFFDBADFF.toInt() -> "3"
                    0xFFFF887C.toInt() -> "4"
                    0xFFFBD75B.toInt() -> "5"
                    0xFFFFB878.toInt() -> "6"
                    0xFF46D6DB.toInt() -> "7"
                    0xFFE1E1E1.toInt() -> "8"
                    0xFF5484ED.toInt() -> "9"
                    0xFF51B749.toInt() -> "10"
                    0xFFDC2127.toInt() -> "11"
                    else -> "9"
                }

                val result = repository.pushEventToGoogle(
                    accessToken = token,
                    title = title,
                    description = description,
                    location = location,
                    startTimeMs = startTimeMs,
                    endTimeMs = endTimeMs,
                    colorId = colorId
                )

                if (result.isSuccess) {
                    _syncState.value = SyncState.Success("Event added to Google Calendar!")
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to push event"
                    _syncState.value = SyncState.Error(errorMsg)
                }
            } else {
                // Save locally to database as Demo Mode event
                val id = "demo_event_" + UUID.randomUUID().toString()
                val localEvent = CalendarEvent(
                    id = id,
                    title = title,
                    description = description,
                    location = location,
                    startTime = startTimeMs,
                    endTime = endTimeMs,
                    color = colorHex,
                    calendarId = "demo_cal",
                    calendarName = "Healthy Habits (Local Demo)",
                    isDemo = true
                )
                repository.insertLocalEvent(localEvent)
                _syncState.value = SyncState.Success("Event added to Demo Mode calendar!")
            }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            if (_useLiveSync.value) {
                val token = _accessToken.value
                if (token.isEmpty()) {
                    _syncState.value = SyncState.Error("Log in to Google to delete live events.")
                    return@launch
                }
                _syncState.value = SyncState.Loading
                val result = repository.deleteEventFromGoogle(token, eventId)
                if (result.isSuccess) {
                    _syncState.value = SyncState.Success("Event deleted from Google Calendar!")
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to delete event"
                    _syncState.value = SyncState.Error(errorMsg)
                }
            } else {
                repository.deleteEventById(eventId)
                _syncState.value = SyncState.Success("Event deleted from local calendar!")
            }
        }
    }

    fun dismissSyncStatus() {
        _syncState.value = SyncState.Idle
    }
}
