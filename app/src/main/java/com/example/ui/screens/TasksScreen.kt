package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.ChannelEntity
import com.example.data.local.TaskEntity
import com.example.data.local.UserEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.UserRole
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.GeofenceHelper
import com.example.viewmodel.StudioFlowUiState

@Composable
fun TasksScreen(
    state: StudioFlowUiState,
    onCreateTask: (
        title: String,
        desc: String,
        instructions: String,
        channel: ChannelEntity,
        category: String,
        assignees: List<UserEntity>,
        priority: String,
        deadlineHours: Int,
        expectedMinutes: Int,
        complexity: Int,
        attachedFileName: String
    ) -> Unit,
    onUpdateTaskStatus: (
        task: TaskEntity,
        status: TaskStatus,
        submissionNote: String,
        submissionFile: String,
        delayReason: String,
        managerComments: String,
        extendHours: Int?
    ) -> Unit
) {
    val user = state.currentUser ?: return
    val role = state.currentRole ?: UserRole.VIDEO_EDITOR
    val canCreate = role.canAssignTasks
    val isReadOnly = role.isReadOnly

    var statusFilter by remember { mutableStateOf<TaskStatus?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var activeTask by remember { mutableStateOf<TaskEntity?>(null) }

    // Employees only see their assigned tasks unless Higher Authority or Coordinator
    val scopedTasks = remember(state.tasks, user, role) {
        if (role.isHigherAuthority || role == UserRole.COORDINATOR_ANCHOR) {
            state.tasks
        } else {
            state.tasks.filter { it.assignedToIdsCsv.split(",").contains(user.id) }
        }
    }

    val filteredTasks = remember(scopedTasks, statusFilter) {
        if (statusFilter == null) scopedTasks
        else scopedTasks.filter { TaskStatus.fromString(it.status) == statusFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        SectionHeader(
            title = "Task Management",
            subtitle = "${filteredTasks.size} tasks visible",
            action = if (canCreate) {
                {
                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("create_task_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Assign Task", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Assign Task")
                    }
                }
            } else null
        )

        Spacer(Modifier.height(10.dp))

        // Status filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = statusFilter == null,
                onClick = { statusFilter = null },
                label = { Text("All (${scopedTasks.size})") }
            )
            TaskStatus.entries.forEach { st ->
                val count = scopedTasks.count { TaskStatus.fromString(it.status) == st }
                if (count > 0 || st == TaskStatus.OVERDUE) {
                    FilterChip(
                        selected = statusFilter == st,
                        onClick = { statusFilter = st },
                        label = { Text("${st.label} ($count)") }
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (filteredTasks.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.TaskAlt,
                        title = "No matching tasks",
                        description = "Switch filter chips or assign a new production task."
                    )
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    val st = TaskStatus.fromString(task.status)
                    val prio = TaskPriority.fromString(task.priority)
                    val statusColor = when (st) {
                        TaskStatus.COMPLETED, TaskStatus.APPROVED -> StatusCompleted
                        TaskStatus.OVERDUE, TaskStatus.REVISION_REQUIRED -> StatusOverdue
                        TaskStatus.SUBMITTED_FOR_REVIEW, TaskStatus.IN_PROGRESS -> StatusInfo
                        else -> StatusPending
                    }
                    val prioColor = when (prio) {
                        TaskPriority.URGENT -> StatusOverdue
                        TaskPriority.HIGH -> StatusPending
                        TaskPriority.MEDIUM -> ElectricBlue
                        TaskPriority.LOW -> TextMuted
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceWhite,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isReadOnly) { activeTask = task }
                            .testTag("task_card_${task.taskCode}")
                    ) {
                        Column(
                            modifier = Modifier
                                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${task.taskCode} • ${task.channelName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricBlue
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StatusBadge(prio.label, prioColor)
                                    StatusBadge(st.label, statusColor)
                                }
                            }
                            Text(task.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(task.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                            LinearProgressIndicator(
                                progress = { st.progress },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = statusColor,
                                trackColor = SurfaceMuted
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Assigned: ${task.assignedToNamesCsv}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    "Due: ${GeofenceHelper.formatDateTime(task.deadlineMillis)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (st == TaskStatus.OVERDUE) StatusOverdue else TextMuted
                                )
                            }

                            if (task.delayReason.isNotBlank()) {
                                Text(
                                    "Delay reason: ${task.delayReason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusOverdue
                                )
                            }
                            if (task.submissionFileName.isNotBlank()) {
                                Text(
                                    "Submitted file: ${task.submissionFileName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusCompleted
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Task Dialog (Multi-employee assignment, priority, complexity, attachment)
    if (showCreateDialog) {
        val context = LocalContext.current
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var instructions by remember { mutableStateOf("") }
        var selectedChannel by remember { mutableStateOf(state.channels.first()) }
        var category by remember { mutableStateOf("EDIT") }
        var priority by remember { mutableStateOf(TaskPriority.HIGH.name) }
        var deadlineHours by remember { mutableStateOf("24") }
        var expectedMins by remember { mutableStateOf("120") }
        var complexity by remember { mutableStateOf(3) }
        var attachedFile by remember { mutableStateOf("") }
        val teamCandidates = state.users.filter { !UserRole.fromString(it.role).isHigherAuthority }
        var selectedAssigneeIds by remember { mutableStateOf(setOf(teamCandidates.firstOrNull()?.id ?: "")) }

        val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                val cursor = context.contentResolver.query(it, null, null, null, null)
                val name = cursor?.use { c ->
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (c.moveToFirst() && idx >= 0) c.getString(idx) else null
                }
                attachedFile = name ?: it.lastPathSegment ?: "brief_attachment"
            }
        }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Assign New Task") },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task Title") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                    OutlinedTextField(value = instructions, onValueChange = { instructions = it }, label = { Text("Instructions") })

                    Text("Channel", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.channels.forEach { ch ->
                            FilterChip(
                                selected = selectedChannel.id == ch.id,
                                onClick = { selectedChannel = ch },
                                label = { Text(ch.name) }
                            )
                        }
                    }

                    Text("Priority & Category", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TaskPriority.entries.forEach { p ->
                            FilterChip(
                                selected = priority == p.name,
                                onClick = { priority = p.name },
                                label = { Text(p.label) }
                            )
                        }
                    }
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("SCRIPT", "SHOOT", "EDIT", "THUMBNAIL", "AI_PROMPT", "CLIENT_WORK", "PUBLISH").forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) }
                            )
                        }
                    }

                    Text("Assign Employees (Multi-select)", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        teamCandidates.forEach { u ->
                            val picked = selectedAssigneeIds.contains(u.id)
                            FilterChip(
                                selected = picked,
                                onClick = {
                                    selectedAssigneeIds = if (picked) selectedAssigneeIds - u.id else selectedAssigneeIds + u.id
                                },
                                label = { Text(u.fullName) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = deadlineHours,
                            onValueChange = { deadlineHours = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Due (hrs)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = expectedMins,
                            onValueChange = { expectedMins = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Est (mins)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Complexity Weight: $complexity / 5", style = MaterialTheme.typography.bodySmall)
                        Row {
                            IconButton(onClick = { if (complexity > 1) complexity-- }) { Icon(Icons.Default.Remove, contentDescription = "Decrease") }
                            IconButton(onClick = { if (complexity < 5) complexity++ }) { Icon(Icons.Default.Add, contentDescription = "Increase") }
                        }
                    }

                    OutlinedButton(onClick = { docPicker.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (attachedFile.isBlank()) "Attach Script / Reference" else attachedFile)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val chosenAssignees = teamCandidates.filter { selectedAssigneeIds.contains(it.id) }
                    onCreateTask(
                        title,
                        desc,
                        instructions,
                        selectedChannel,
                        category,
                        chosenAssignees,
                        priority,
                        deadlineHours.toIntOrNull() ?: 24,
                        expectedMins.toIntOrNull() ?: 120,
                        complexity,
                        attachedFile
                    )
                    showCreateDialog = false
                }) { Text("Assign Task") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Task Action / Review Dialog
    activeTask?.let { task ->
        val context = LocalContext.current
        var nextStatus by remember { mutableStateOf(TaskStatus.fromString(task.status)) }
        var submissionNote by remember { mutableStateOf(task.submissionNote) }
        var submissionFile by remember { mutableStateOf(task.submissionFileName) }
        var delayReason by remember { mutableStateOf(task.delayReason) }
        var managerComments by remember { mutableStateOf(task.managerComments) }
        var extendHours by remember { mutableStateOf("") }

        val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                val cursor = context.contentResolver.query(it, null, null, null, null)
                val name = cursor?.use { c ->
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (c.moveToFirst() && idx >= 0) c.getString(idx) else null
                }
                submissionFile = name ?: it.lastPathSegment ?: "completed_work"
            }
        }

        val allowedStatuses = if (role.canAssignTasks || role.canVerifyScripts) {
            TaskStatus.entries
        } else {
            listOf(
                TaskStatus.ACCEPTED,
                TaskStatus.IN_PROGRESS,
                TaskStatus.SUBMITTED_FOR_REVIEW,
                TaskStatus.OVERDUE
            )
        }

        AlertDialog(
            onDismissRequest = { activeTask = null },
            title = { Text("${task.taskCode} • Update Workflow") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(task.title, style = MaterialTheme.typography.titleSmall)
                    if (task.instructions.isNotBlank()) {
                        Text("Instructions: ${task.instructions}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        allowedStatuses.forEach { st ->
                            FilterChip(
                                selected = nextStatus == st,
                                onClick = { nextStatus = st },
                                label = { Text(st.label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = submissionNote,
                        onValueChange = { submissionNote = it },
                        label = { Text("Submission / Work Progress Notes") }
                    )
                    OutlinedButton(onClick = { filePicker.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Attach Work", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (submissionFile.isBlank()) "Attach Completed Work File" else submissionFile)
                    }
                    if (TaskStatus.fromString(task.status) == TaskStatus.OVERDUE || nextStatus == TaskStatus.OVERDUE) {
                        OutlinedTextField(
                            value = delayReason,
                            onValueChange = { delayReason = it },
                            label = { Text("Reason for Delay") }
                        )
                    }
                    if (role.canAssignTasks || role.canVerifyScripts) {
                        OutlinedTextField(
                            value = managerComments,
                            onValueChange = { managerComments = it },
                            label = { Text("Manager Review / Corrective Comments") }
                        )
                        OutlinedTextField(
                            value = extendHours,
                            onValueChange = { extendHours = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Revised Deadline (+hrs, optional)") },
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateTaskStatus(
                        task,
                        nextStatus,
                        submissionNote,
                        submissionFile,
                        delayReason,
                        managerComments,
                        extendHours.toIntOrNull()
                    )
                    activeTask = null
                }) { Text("Apply Update") }
            },
            dismissButton = {
                TextButton(onClick = { activeTask = null }) { Text("Close") }
            }
        )
    }
}
