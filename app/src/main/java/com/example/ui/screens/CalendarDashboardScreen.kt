package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CalendarEvent
import com.example.ui.viewmodel.CalendarViewModel
import com.example.ui.viewmodel.SyncState
import com.example.util.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDashboardScreen(viewModel: CalendarViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val selectedDateMs by viewModel.selectedDateMs.collectAsState()
    val useLiveSync by viewModel.useLiveSync.collectAsState()
    val accessToken by viewModel.accessToken.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val events by viewModel.filteredEvents.collectAsState()

    var showAddEventDialog by remember { mutableStateOf(false) }
    var selectedEventForDetail by remember { mutableStateOf<CalendarEvent?>(null) }
    var showAuthDialog by remember { mutableStateOf(false) }

    // To auto-update the current time line
    var currentTimeMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMs = System.currentTimeMillis()
            delay(60000) // Update every minute
        }
    }

    // Floating status notifier
    LaunchedEffect(syncState) {
        if (syncState is SyncState.Success) {
            Toast.makeText(context, (syncState as SyncState.Success).message, Toast.LENGTH_SHORT).show()
            viewModel.dismissSyncStatus()
        } else if (syncState is SyncState.Error) {
            Toast.makeText(context, (syncState as SyncState.Error).error, Toast.LENGTH_LONG).show()
            viewModel.dismissSyncStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Daily Calendar",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (useLiveSync) "Connected to Google Workspace" else "Evaluating Offline Sandbox",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { showAuthDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DI",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    // Sync Status Indicator
                    if (syncState is SyncState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (useLiveSync) {
                        IconButton(
                            onClick = { viewModel.syncWithGoogle() },
                            modifier = Modifier.testTag("refresh_sync_button")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Manual Refresh")
                        }
                    }

                    // Mode Toggle (Demo / Live)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (useLiveSync) "LIVE" else "DEMO",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (useLiveSync) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = useLiveSync,
                            onCheckedChange = { checked ->
                                if (checked && accessToken.isEmpty()) {
                                    showAuthDialog = true
                                } else {
                                    viewModel.setLiveSyncMode(checked)
                                }
                            },
                            modifier = Modifier
                                .scale(0.85f)
                                .testTag("live_sync_toggle")
                        )
                    }
                    
                    // Options Menu for Auth configuration
                    IconButton(
                        onClick = { showAuthDialog = true },
                        modifier = Modifier.testTag("auth_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "OAuth Credentials configuration"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddEventDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("add_event_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Schedule New Event")
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Already on Today */ },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Today",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    label = { Text("Today") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )

                NavigationBarItem(
                    selected = false,
                    onClick = {
                        Toast.makeText(context, "Explore agenda views within this workspace", Toast.LENGTH_SHORT).show()
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Schedule"
                        )
                    },
                    label = { Text("Schedule") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )

                NavigationBarItem(
                    selected = false,
                    onClick = {
                        Toast.makeText(context, "Full tasks engine sync offline sandbox", Toast.LENGTH_SHORT).show()
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Tasks"
                        )
                    },
                    label = { Text("Tasks") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )

                NavigationBarItem(
                    selected = false,
                    onClick = { showAuthDialog = true },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Horizontal Weekday Strip Header
            WeekCalendarStrip(
                selectedDateMs = selectedDateMs,
                onDateSelected = { viewModel.selectDate(it) }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Dynamic Timeline Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (events.isEmpty()) {
                    // Elevated, friendly empty space screen
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(72.dp)
                                    .padding(bottom = 12.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "Zero events scheduled on this day",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "This is the perfect timeline slot to read, design, or hit the gym. Press the button below to add custom plans.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
                            )
                            Button(
                                onClick = { showAddEventDialog = true },
                                modifier = Modifier.testTag("add_plan_empty_button")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Custom Event")
                            }
                        }
                    }
                } else {
                    // Real-time beautiful scrollable calendar timeline
                    DailyTimelineGrid(
                        events = events,
                        selectedDateMs = selectedDateMs,
                        currentTimeMs = currentTimeMs,
                        onEventClicked = { selectedEventForDetail = it }
                    )
                }
            }
        }
    }

    // Detail View Dialog
    selectedEventForDetail?.let { event ->
        EventDetailDialog(
            event = event,
            onDismiss = { selectedEventForDetail = null },
            onDeleteClicked = {
                viewModel.deleteEvent(event.id)
                selectedEventForDetail = null
            }
        )
    }

    // Add Plan Quick Dialog
    if (showAddEventDialog) {
        AddEventDialog(
            selectedDayMs = selectedDateMs,
            onDismiss = { showAddEventDialog = false },
            onSave = { title, desc, place, start, end, col ->
                viewModel.addEvent(title, desc, place, start, end, col)
                showAddEventDialog = false
            }
        )
    }

    // Google API Configuration & Authentication Panel
    if (showAuthDialog) {
        GoogleAuthDialog(
            accessToken = accessToken,
            isLiveSync = useLiveSync,
            onDismiss = { showAuthDialog = false },
            onSaveToken = { token ->
                viewModel.saveAccessToken(token)
                viewModel.setLiveSyncMode(token.isNotEmpty())
                showAuthDialog = false
            },
            onLogout = {
                viewModel.logout()
                showAuthDialog = false
            }
        )
    }
}

@Composable
fun WeekCalendarStrip(
    selectedDateMs: Long,
    onDateSelected: (Long) -> Unit
) {
    val weekDays = remember(selectedDateMs) {
        DateUtils.getWeekDaysAround(selectedDateMs)
    }

    val dateHeaderParts = remember(selectedDateMs) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMs }
        val dayOfMonthStr = cal.get(Calendar.DAY_OF_MONTH).toString()
        val dayOfWeekName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.US)?.uppercase(Locale.US) ?: ""
        val monthAndYear = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US) + " " + cal.get(Calendar.YEAR)
        Triple(dayOfMonthStr, dayOfWeekName, monthAndYear)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 12.dp)
    ) {
        // Sophisticated custom date display from mockup
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, bottom = 14.dp, top = 4.dp)
        ) {
            Text(
                text = dateHeaderParts.first,
                fontSize = 54.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.primary,
                lineHeight = 54.sp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text(
                    text = dateHeaderParts.second,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = dateHeaderParts.third,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            weekDays.forEach { dayMs ->
                val isSelected = remember(selectedDateMs, dayMs) {
                    val cal1 = Calendar.getInstance().apply { timeInMillis = selectedDateMs }
                    val cal2 = Calendar.getInstance().apply { timeInMillis = dayMs }
                    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
                }
                
                val isToday = remember(dayMs) {
                    val cal1 = Calendar.getInstance()
                    val cal2 = Calendar.getInstance().apply { timeInMillis = dayMs }
                    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.85f)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            when {
                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                else -> Color.Transparent
                            }
                        )
                        .clickable { onDateSelected(dayMs) }
                        .testTag("weekday_strip_${DateUtils.getDayOfMonth(dayMs)}")
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = DateUtils.getDayOfWeekAbbreviation(dayMs),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = DateUtils.getDayOfMonth(dayMs),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                isToday -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        
                        if (isToday && !isSelected) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(5.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Generates overlapping event layout lanes
fun computeOverlappingLanes(events: List<CalendarEvent>): Map<String, Pair<Int, Int>> {
    val sorted = events.sortedBy { it.startTime }
    val result = mutableMapOf<String, Pair<Int, Int>>() // eventId -> (laneIndex, totalLanes)
    val lanes = mutableListOf<MutableList<CalendarEvent>>()

    for (event in sorted) {
        var placed = false
        for (i in lanes.indices) {
            val lane = lanes[i]
            val overlaps = lane.any { e ->
                (event.startTime < e.endTime && event.endTime > e.startTime)
            }
            if (!overlaps) {
                lane.add(event)
                result[event.id] = Pair(i, 0)
                placed = true
                break
            }
        }
        if (!placed) {
            val newLane = mutableListOf(event)
            lanes.add(newLane)
            result[event.id] = Pair(lanes.size - 1, 0)
        }
    }

    val totalLanes = lanes.size
    for (event in sorted) {
        val laneIndex = result[event.id]?.first ?: 0
        result[event.id] = Pair(laneIndex, totalLanes)
    }
    return result
}

@Composable
fun DailyTimelineGrid(
    events: List<CalendarEvent>,
    selectedDateMs: Long,
    currentTimeMs: Long,
    onEventClicked: (CalendarEvent) -> Unit
) {
    val hourHeightDp = 64.dp
    val scale = hourHeightDp.value
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val laneOffsets = remember(events) {
        computeOverlappingLanes(events)
    }

    // Scroll to current hour on load
    LaunchedEffect(Unit) {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        if (currentHour > 1) {
            val scrollValue = ((currentHour - 1) * scale).toInt()
            scrollState.animateScrollTo(scrollValue)
        }
    }

    val isSelectedDayToday = remember(selectedDateMs, currentTimeMs) {
        val targetCal = Calendar.getInstance().apply { timeInMillis = selectedDateMs }
        val nowCal = Calendar.getInstance().apply { timeInMillis = currentTimeMs }
        targetCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) && targetCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Draw Hour Grid lines and Labels
        Column {
            for (hour in 0..24) {
                Box(
                    modifier = Modifier
                        .height(hourHeightDp)
                        .fillMaxWidth()
                ) {
                    // Line separating hours
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        modifier = Modifier.align(Alignment.BottomStart)
                    )

                    // Hour Stamp
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(start = 12.dp, top = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = if (hour == 24) "" else DateUtils.formatHour(hour),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.width(64.dp)
                        )
                    }
                }
            }
        }

        // Draw Events overlays on top of the grid
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 80.dp) // Offset of the hour stamp column
        ) {
            val totalWidth = maxWidth

            events.forEach { event ->
                val calStart = Calendar.getInstance().apply { timeInMillis = event.startTime }
                val startHour = calStart.get(Calendar.HOUR_OF_DAY) + calStart.get(Calendar.MINUTE) / 60.0
                
                val calEnd = Calendar.getInstance().apply { timeInMillis = event.endTime }
                val endHour = calEnd.get(Calendar.HOUR_OF_DAY) + calEnd.get(Calendar.MINUTE) / 60.0
                
                // Keep spans strictly within the same day for drawing logic
                val duration = (endHour - startHour).coerceAtLeast(0.5)

                val yOffset = (startHour * scale).dp
                val cardHeight = (duration * scale).dp

                val laneInfo = laneOffsets[event.id] ?: Pair(0, 1)
                val laneIndex = laneInfo.first
                val totalLanes = laneInfo.second

                val eventCardWidth = totalWidth / totalLanes
                val xOffset = eventCardWidth * laneIndex

                Box(
                    modifier = Modifier
                        .offset(x = xOffset, y = yOffset)
                        .width(eventCardWidth)
                        .height(cardHeight)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onEventClicked(event) }
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(16.dp)
                        )
                        .testTag("event_card_${event.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Vertical bar on left reflecting calendar tag color
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(event.color))
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = if (cardHeight < 55.dp) 1 else 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            if (cardHeight >= 45.dp) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = DateUtils.formatTimeRange(event.startTime, event.endTime),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (cardHeight >= 75.dp && !event.location.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = event.location ?: "",
                                        style = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE.let { 
                                            MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                                        },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Draw Real-time Current Hour Red Indicator bar (only on modern active day view!)
            if (isSelectedDayToday) {
                val now = Calendar.getInstance().apply { timeInMillis = currentTimeMs }
                val hourNow = now.get(Calendar.HOUR_OF_DAY)
                val minuteNow = now.get(Calendar.MINUTE)
                val currentHourOffset = hourNow + minuteNow / 60.0
                val yRedBarOffset = (currentHourOffset * scale).dp

                Box(
                    modifier = Modifier
                        .offset(y = yRedBarOffset - 1.dp)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.error)
                ) {
                    // Small glowing dot node on left side
                    Box(
                        modifier = Modifier
                            .offset(x = (-4).dp, y = (-3).dp)
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                    )
                }
            }
        }
    }
}

// Ext helper for lane width percentages
fun Int.percentDp(): Dp = (this * 3.2).dp // Approximate spacing offset

@Composable
fun EventDetailDialog(
    event: CalendarEvent,
    onDismiss: () -> Unit,
    onDeleteClicked: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("event_detail_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header with vibrant color band
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color(event.color), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = event.calendarName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Detail Modal")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = event.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Date Time Slot Row
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Date and Time",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = DateUtils.formatDateHeader(event.startTime),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = DateUtils.formatTimeRange(event.startTime, event.endTime),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Location row
                if (!event.location.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Location",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = event.location,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Clickable map launch trigger
                            Text(
                                text = "Launch in Google Maps",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable {
                                        try {
                                            val mapUri = Uri.parse("geo:0,0?q=" + Uri.encode(event.location))
                                            val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                                            mapIntent.setPackage("com.google.android.apps.maps")
                                            context.startActivity(mapIntent)
                                        } catch (e: Exception) {
                                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(event.location)))
                                            context.startActivity(webIntent)
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                // Description Detail row
                if (!event.description.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Description details",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = event.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dismiss_detail_button")
                    ) {
                        Text("Dismiss")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onDeleteClicked,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.testTag("delete_detail_button")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete Plan")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    selectedDayMs: Long,
    onDismiss: () -> Unit,
    onSave: (String, String?, String?, Long, Long, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Start with rounded slot hour values
    val startCalendar = remember(selectedDayMs) {
        Calendar.getInstance().apply {
            timeInMillis = selectedDayMs
            val nowTimeCal = Calendar.getInstance()
            set(Calendar.HOUR_OF_DAY, nowTimeCal.get(Calendar.HOUR_OF_DAY) + 1)
            set(Calendar.MINUTE, 0)
        }
    }
    
    val endCalendar = remember(startCalendar) {
        val cal = startCalendar.clone() as Calendar
        cal.add(Calendar.HOUR_OF_DAY, 1)
        cal
    }

    var startHourSelected by remember { mutableStateOf(startCalendar.get(Calendar.HOUR_OF_DAY)) }
    var startMinuteSelected by remember { mutableStateOf(0) }
    var endHourSelected by remember { mutableStateOf(endCalendar.get(Calendar.HOUR_OF_DAY)) }
    var endMinuteSelected by remember { mutableStateOf(0) }

    // Supported Google Calendar theme colors
    val colors = listOf(
        0xFFA4BDFC.toInt(), // Lavender
        0xFF7AE7BF.toInt(), // Sage
        0xFFDBADFF.toInt(), // Grape
        0xFFFF887C.toInt(), // Flamingo
        0xFFFBD75B.toInt(), // Banana
        0xFFFFB878.toInt(), // Orange
        0xFF46D6DB.toInt(), // Peacock
        0xFFE1E1E1.toInt(), // Graphite
        0xFF5484ED.toInt(), // Blueberry
        0xFF51B749.toInt(), // Basil
        0xFFDC2127.toInt()  // Tomato
    )
    var selectedColor by remember { mutableStateOf(colors[0]) }

    var isTitleError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("add_event_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Add Event",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotEmpty()) isTitleError = false
                    },
                    label = { Text("Event Name *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_event_title_field"),
                    isError = isTitleError,
                    singleLine = true
                )
                if (isTitleError) {
                    Text(
                        text = "The title block cannot be left empty",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location (e.g., room address)") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Place, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_event_location_field"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Event description or notes") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Edit, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .testTag("add_event_desc_field"),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Start Hour selection row
                Text(
                    text = "Schedule Time Frame",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Starts At: ",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(80.dp)
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Slider(
                            value = startHourSelected.toFloat(),
                            onValueChange = { startHourSelected = it.toInt() },
                            valueRange = 0f..23f,
                            steps = 22,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format("%02d:00", startHourSelected),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(48.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ends At: ",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(80.dp)
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Slider(
                            value = endHourSelected.toFloat(),
                            onValueChange = { endHourSelected = it.toInt() },
                            valueRange = 1f..24f,
                            steps = 22,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format("%02d:00", endHourSelected),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Vibrant Google Calendar color picker dots
                Text(
                    text = "Calendar Tag Color",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(colors) { colorInt ->
                        val isColorSelected = selectedColor == colorInt
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(colorInt))
                                .border(
                                    width = if (isColorSelected) 3.dp else 1.dp,
                                    color = if (isColorSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorInt }
                        ) {
                            if (isColorSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dismiss_add_button")
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            if (title.trim().isEmpty()) {
                                isTitleError = true
                                return@Button
                            }

                            // Build local cal dates
                            val startCalVal = Calendar.getInstance().apply {
                                timeInMillis = selectedDayMs
                                set(Calendar.HOUR_OF_DAY, startHourSelected)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }

                            val endCalVal = Calendar.getInstance().apply {
                                timeInMillis = selectedDayMs
                                set(Calendar.HOUR_OF_DAY, endHourSelected)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }

                            // Fix user error where start hour >= end hour
                            if (startCalVal.timeInMillis >= endCalVal.timeInMillis) {
                                endCalVal.timeInMillis = startCalVal.timeInMillis + 3600000 // default plus 1 hour
                            }

                            onSave(
                                title.trim(),
                                description.trim().takeIf { it.isNotEmpty() },
                                location.trim().takeIf { it.isNotEmpty() },
                                startCalVal.timeInMillis,
                                endCalVal.timeInMillis,
                                selectedColor
                            )
                        },
                        modifier = Modifier.testTag("save_event_button")
                    ) {
                        Text("Add Event")
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleAuthDialog(
    accessToken: String,
    isLiveSync: Boolean,
    onDismiss: () -> Unit,
    onSaveToken: (String) -> Unit,
    onLogout: () -> Unit
) {
    var rawInputToken by remember { mutableStateOf(accessToken) }
    val context = LocalContext.current

    // Web Instructions for OAuth setup
    val oauthUrlInstruction = "https://accounts.google.com/o/oauth2/v2/auth?client_id=1055562725064-aistudio-sample-google-client.apps.googleusercontent.com&redirect_uri=https://localhost/callback&response_type=token&scope=https://www.googleapis.com/auth/calendar.readonly https://www.googleapis.com/auth/calendar.events.readonly"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.6.dp)
                .testTag("google_auth_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Google workspace Auth Setting",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = "Integrate your Android app with Google Calendar in seconds. Since this environment operates as an AI Studio simulator sandbox, we have pre-granted requested scopes for your email.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (accessToken.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Authenticated",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LIVE Google Calendar Active",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Token (Truncated): " + accessToken.take(12) + "...",
                                style = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE.let { 
                                    MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                                },
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                } else {
                    Text(
                        text = "Connection options:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(oauthUrlInstruction))
                            context.startActivity(webIntent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("launch_oauth_browser_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Get OAuth Token")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = rawInputToken,
                    onValueChange = { rawInputToken = it },
                    label = { Text("Paste Google Access Token") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_token_field"),
                    placeholder = { Text("ya29.a0Axoo...") }
                )

                Text(
                    text = "Simply paste any valid Google Calendar Oauth Bearer access token above to immediately trigger active calendar polling and scheduled synchronization.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (accessToken.isNotEmpty()) {
                        TextButton(
                            onClick = onLogout,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("logout_auth_button")
                        ) {
                            Text("Unlink Calendar")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row {
                        TextButton(onClick = onDismiss, modifier = Modifier.testTag("dismiss_auth_button")) {
                            Text("Cancel")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                onSaveToken(rawInputToken.trim())
                            },
                            modifier = Modifier.testTag("save_auth_button")
                        ) {
                            Text("Apply Settings")
                        }
                    }
                }
            }
        }
    }
}
