package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.CalendarEntryEntity
import com.example.data.local.ShootingScheduleEntity
import com.example.data.model.UserRole
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.GeofenceHelper
import com.example.viewmodel.StudioFlowUiState

@Composable
fun CalendarAndShootingScreen(
    state: StudioFlowUiState,
    onAddCalendarEntry: (CalendarEntryEntity) -> Unit,
    onRescheduleCalendarEntry: (CalendarEntryEntity, String, String) -> Unit,
    onAddShootingSchedule: (ShootingScheduleEntity) -> Unit,
    onUpdateShootingStatus: (ShootingScheduleEntity, String, String) -> Unit
) {
    val user = state.currentUser ?: return
    val role = state.currentRole ?: UserRole.VIDEO_EDITOR
    val canEdit = !role.isReadOnly && !role.isExternal

    var activeTab by remember { mutableStateOf(0) } // 0 = Content Calendars, 1 = Shooting & Studio Rooms
    var selectedChannelId by remember { mutableStateOf("ALL") }
    var calendarViewMode by remember { mutableStateOf("Weekly") } // Daily, Weekly, Monthly
    var showAddCalDialog by remember { mutableStateOf(false) }
    var showAddShootDialog by remember { mutableStateOf(false) }
    var rescheduleTarget by remember { mutableStateOf<CalendarEntryEntity?>(null) }

    val todayKey = GeofenceHelper.todayDateKey()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        TabRow(selectedTabIndex = activeTab) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Content Calendars") })
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Shooting & Studios") })
        }
        Spacer(Modifier.height(12.dp))

        if (activeTab == 0) {
            val filteredEntries = state.calendarEntries.filter { entry ->
                val chMatch = selectedChannelId == "ALL" || entry.channelId == selectedChannelId
                val viewMatch = if (calendarViewMode == "Daily") entry.shootingDateKey == todayKey else true
                chMatch && viewMatch
            }

            // Conflict detector across anchor/studio/date/slot
            val conflicts = remember(state.calendarEntries) {
                val map = mutableMapOf<Long, String>()
                state.calendarEntries.forEach { a ->
                    val hit = state.calendarEntries.firstOrNull { b ->
                        b.id != a.id &&
                            b.shootingDateKey == a.shootingDateKey &&
                            b.shootingTimeSlot == a.shootingTimeSlot &&
                            (b.anchorName.equals(a.anchorName, ignoreCase = true) || b.studioRoom.equals(a.studioRoom, ignoreCase = true))
                    }
                    if (hit != null) {
                        map[a.id] = "Overlap with '${hit.contentTitle}' (${hit.studioRoom})"
                    }
                }
                map
            }

            SectionHeader(
                title = "5-Channel Content Calendars",
                subtitle = "$calendarViewMode View • ${filteredEntries.size} entries",
                action = if (canEdit) {
                    {
                        Button(
                            onClick = { showAddCalDialog = true },
                            modifier = Modifier.testTag("add_calendar_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Schedule", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Schedule")
                        }
                    }
                } else null
            )

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Daily", "Weekly", "Monthly").forEach { mode ->
                    FilterChip(selected = calendarViewMode == mode, onClick = { calendarViewMode = mode }, label = { Text(mode) })
                }
            }

            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = selectedChannelId == "ALL", onClick = { selectedChannelId = "ALL" }, label = { Text("All Channels") })
                state.channels.forEach { ch ->
                    FilterChip(selected = selectedChannelId == ch.id, onClick = { selectedChannelId = ch.id }, label = { Text(ch.name) })
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (filteredEntries.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.CalendarMonth,
                            title = "No calendar entries",
                            description = "Switch to Weekly/Monthly or schedule a new shoot & publish slot."
                        )
                    }
                } else {
                    items(filteredEntries, key = { it.id }) { entry ->
                        val conflictNote = conflicts[entry.id]
                        Surface(shape = RoundedCornerShape(14.dp), color = SurfaceWhite) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (conflictNote != null) StatusOverdue else BorderSubtle,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${entry.channelName} • ${entry.platform}", style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
                                    StatusBadge(entry.status, if (conflictNote != null) StatusOverdue else StatusCompleted)
                                }
                                Text(entry.contentTitle, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(
                                    "Shoot: ${entry.shootingDateKey} (${entry.shootingTimeSlot}) • ${entry.studioRoom}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    "Anchor: ${entry.anchorName} • Script: ${entry.scriptWriter} • Editor: ${entry.editorName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                                Text(
                                    "Edit Due: ${entry.editingDeadlineKey} • Publish: ${entry.publishingDateKey} ${entry.publishingTime}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                                if (conflictNote != null) {
                                    Text("CONFLICT: $conflictNote", style = MaterialTheme.typography.labelSmall, color = StatusOverdue)
                                }
                                if (canEdit) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        TextButton(onClick = { rescheduleTarget = entry }) {
                                            Icon(Icons.Default.EditCalendar, contentDescription = "Reschedule", modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Move / Reschedule")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Shooting Schedules & Studio Room Availability
            var shootModeFilter by remember { mutableStateOf("ALL") }
            val filteredShoots = state.shootingSchedules.filter {
                shootModeFilter == "ALL" || it.scheduleMode == shootModeFilter
            }

            SectionHeader(
                title = "Shooting & Studio Availability",
                subtitle = "Pandit Ji, Art & Craft Teacher, Anchors + Studio Rooms",
                action = if (canEdit) {
                    {
                        Button(
                            onClick = { showAddShootDialog = true },
                            modifier = Modifier.testTag("add_shoot_button")
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = "Book Shoot", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Book Studio")
                        }
                    }
                } else null
            )

            Spacer(Modifier.height(8.dp))

            // Studio Room Matrix Card
            Surface(shape = RoundedCornerShape(14.dp), color = NavyDeep) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Studio Room Availability ($todayKey)", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Studio A", "Studio B", "Audio Booth").forEach { room ->
                            val bookings = state.shootingSchedules.count { it.shootDateKey == todayKey && it.studioRoom == room }
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = NavySlate,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(room, style = MaterialTheme.typography.labelLarge, color = Color.White)
                                    Text(
                                        if (bookings == 0) "Open Today" else "$bookings Booked",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (bookings == 0) StatusCompleted else StatusPending
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "ALL" to "All Talent",
                    "PANDIT_JI" to "Pandit Ji",
                    "ART_CRAFT_TEACHER" to "Art & Craft",
                    "ANCHOR" to "Parchi & Kritika"
                ).forEach { (k, label) ->
                    FilterChip(selected = shootModeFilter == k, onClick = { shootModeFilter = k }, label = { Text(label) })
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredShoots, key = { it.id }) { shoot ->
                    Surface(shape = RoundedCornerShape(14.dp), color = SurfaceWhite) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${shoot.talentName} • ${shoot.channelName}", style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
                                StatusBadge(
                                    shoot.recordingStatus,
                                    if (shoot.recordingStatus == "RECORDED") StatusCompleted else StatusInfo
                                )
                            }
                            Text(shoot.topicOrKathaName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                "Date: ${shoot.shootDateKey} • Arrival: ${shoot.arrivalTime} • ${shoot.startTime}-${shoot.expectedEndTime} (${shoot.studioRoom})",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text("Script: ${shoot.scriptName} • Coord: ${shoot.coordinatorName} • Editor: ${shoot.editorName}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            if (shoot.materialsRequired.isNotBlank()) {
                                Text("Materials: ${shoot.materialsRequired}", style = MaterialTheme.typography.bodySmall, color = ChannelArtColor)
                            }
                            if (shoot.purchaseRequired.isNotBlank()) {
                                Text("Purchase Needed: ${shoot.purchaseRequired}", style = MaterialTheme.typography.bodySmall, color = StatusOverdue)
                            }
                            if (shoot.specialRequirements.isNotBlank()) {
                                Text("Requirements: ${shoot.specialRequirements}", style = MaterialTheme.typography.bodySmall, color = StatusInfo)
                            }
                            if (canEdit && shoot.recordingStatus != "RECORDED") {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { onUpdateShootingStatus(shoot, "RECORDED", shoot.expectedEndTime) }) {
                                        Text("Mark Recorded")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCalDialog) {
        var selectedCh by remember { mutableStateOf(state.channels.first()) }
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Studio Episode") }
        var anchor by remember { mutableStateOf("Parchi") }
        var editor by remember { mutableStateOf("Editor 1 (Aarav)") }
        var dateKey by remember { mutableStateOf(todayKey) }
        var slot by remember { mutableStateOf("14:00 - 16:00") }
        var studioRoom by remember { mutableStateOf("Studio A") }
        var platform by remember { mutableStateOf("YouTube") }

        AlertDialog(
            onDismissRequest = { showAddCalDialog = false },
            title = { Text("Add Content Calendar Entry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.channels.forEach { ch ->
                            FilterChip(selected = selectedCh.id == ch.id, onClick = { selectedCh = ch }, label = { Text(ch.name) })
                        }
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Content Title") })
                    OutlinedTextField(value = anchor, onValueChange = { anchor = it }, label = { Text("Anchor / Narrator / Teacher") })
                    OutlinedTextField(value = dateKey, onValueChange = { dateKey = it }, label = { Text("Shoot Date (yyyy-MM-dd)") })
                    OutlinedTextField(value = slot, onValueChange = { slot = it }, label = { Text("Time Slot (e.g. 14:00 - 16:00)") })
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Studio A", "Studio B", "Audio Booth").forEach { room ->
                            FilterChip(selected = studioRoom == room, onClick = { studioRoom = room }, label = { Text(room) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("YouTube", "Instagram", "Facebook").forEach { p ->
                            FilterChip(selected = platform == p, onClick = { platform = p }, label = { Text(p) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank()) {
                        onAddCalendarEntry(
                            CalendarEntryEntity(
                                channelId = selectedCh.id,
                                channelName = selectedCh.name,
                                contentTitle = title.trim(),
                                category = category,
                                scriptWriter = user.fullName,
                                anchorName = anchor.trim(),
                                editorName = editor,
                                shootingDateKey = dateKey,
                                shootingTimeSlot = slot,
                                editingDeadlineKey = dateKey,
                                publishingDateKey = dateKey,
                                publishingTime = "06:00 PM",
                                platform = platform,
                                status = "Scheduled",
                                studioRoom = studioRoom
                            )
                        )
                        showAddCalDialog = false
                    }
                }) { Text("Save Entry") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCalDialog = false }) { Text("Cancel") }
            }
        )
    }

    rescheduleTarget?.let { entry ->
        var newDate by remember { mutableStateOf(entry.shootingDateKey) }
        var newSlot by remember { mutableStateOf(entry.shootingTimeSlot) }
        AlertDialog(
            onDismissRequest = { rescheduleTarget = null },
            title = { Text("Reschedule '${entry.contentTitle}'") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newDate, onValueChange = { newDate = it }, label = { Text("Shooting Date (yyyy-MM-dd)") })
                    OutlinedTextField(value = newSlot, onValueChange = { newSlot = it }, label = { Text("Time Slot") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onRescheduleCalendarEntry(entry, newDate, newSlot)
                    rescheduleTarget = null
                }) { Text("Confirm Move") }
            },
            dismissButton = {
                TextButton(onClick = { rescheduleTarget = null }) { Text("Cancel") }
            }
        )
    }

    if (showAddShootDialog) {
        var mode by remember { mutableStateOf("ANCHOR") }
        var selectedCh by remember { mutableStateOf(state.channels.first()) }
        var topic by remember { mutableStateOf("") }
        var talent by remember { mutableStateOf("Parchi") }
        var dateKey by remember { mutableStateOf(todayKey) }
        var startTime by remember { mutableStateOf("11:00") }
        var endTime by remember { mutableStateOf("13:00") }
        var room by remember { mutableStateOf("Studio A") }
        var materials by remember { mutableStateOf("") }
        var specialReq by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddShootDialog = false },
            title = { Text("Book Studio Shooting Session") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("PANDIT_JI" to "Pandit Ji", "ART_CRAFT_TEACHER" to "Craft Teacher", "ANCHOR" to "Anchor").forEach { (k, lbl) ->
                            FilterChip(
                                selected = mode == k,
                                onClick = {
                                    mode = k
                                    talent = when (k) {
                                        "PANDIT_JI" -> "Pandit Ji"
                                        "ART_CRAFT_TEACHER" -> "Art & Craft Teacher"
                                        else -> "Parchi"
                                    }
                                },
                                label = { Text(lbl) }
                            )
                        }
                    }
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.channels.forEach { ch ->
                            FilterChip(selected = selectedCh.id == ch.id, onClick = { selectedCh = ch }, label = { Text(ch.name) })
                        }
                    }
                    OutlinedTextField(value = topic, onValueChange = { topic = it }, label = { Text("Topic / Vrat Katha / Craft Project") })
                    OutlinedTextField(value = talent, onValueChange = { talent = it }, label = { Text("Talent Name") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = dateKey, onValueChange = { dateKey = it }, label = { Text("Date") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = startTime, onValueChange = { startTime = it }, label = { Text("Start") }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Studio A", "Studio B", "Audio Booth").forEach { r ->
                            FilterChip(selected = room == r, onClick = { room = r }, label = { Text(r) })
                        }
                    }
                    if (mode == "ART_CRAFT_TEACHER") {
                        OutlinedTextField(value = materials, onValueChange = { materials = it }, label = { Text("Materials & Quantities") })
                    }
                    OutlinedTextField(value = specialReq, onValueChange = { specialReq = it }, label = { Text("Costume / Props / Mic Notes") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (topic.isNotBlank()) {
                        val matchedUser = state.users.firstOrNull { it.fullName.contains(talent, ignoreCase = true) }
                        onAddShootingSchedule(
                            ShootingScheduleEntity(
                                scheduleMode = mode,
                                channelId = selectedCh.id,
                                channelName = selectedCh.name,
                                topicOrKathaName = topic.trim(),
                                scriptName = "SCR-NEW",
                                talentId = matchedUser?.id ?: user.id,
                                talentName = talent.trim(),
                                coordinatorName = user.fullName,
                                editorName = "Editor 1 (Aarav)",
                                shootDateKey = dateKey,
                                arrivalTime = startTime,
                                startTime = startTime,
                                expectedEndTime = endTime,
                                studioRoom = room,
                                specialRequirements = specialReq.trim(),
                                materialsRequired = materials.trim()
                            )
                        )
                        showAddShootDialog = false
                    }
                }) { Text("Confirm Booking") }
            },
            dismissButton = {
                TextButton(onClick = { showAddShootDialog = false }) { Text("Cancel") }
            }
        )
    }
}
