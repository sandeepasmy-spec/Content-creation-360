package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.*
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.GeofenceHelper
import com.example.util.ReportExporter
import com.example.viewmodel.StudioFlowUiState
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HigherAuthorityDashboard(
    state: StudioFlowUiState,
    onDateFilterChange: (DateRangeFilter) -> Unit,
    onNavigateModule: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val role = state.currentRole ?: UserRole.SUPER_ADMIN
    val todayKey = GeofenceHelper.todayDateKey()

    val internalEmployees = state.users.filter {
        val r = UserRole.fromString(it.role)
        !r.isExternal && !r.isHigherAuthority
    }
    val presentToday = state.attendance.count {
        it.dateKey == todayKey && (it.status == "PRESENT" || it.status == "LATE")
    }
    val absentToday = (internalEmployees.size - presentToday).coerceAtLeast(0)

    val completedTasks = state.tasks.count {
        val st = TaskStatus.fromString(it.status)
        st == TaskStatus.COMPLETED || st == TaskStatus.APPROVED
    }
    val overdueTasks = state.tasks.count { TaskStatus.fromString(it.status) == TaskStatus.OVERDUE }
    val pendingTasks = (state.tasks.size - completedTasks - overdueTasks).coerceAtLeast(0)

    val videosProduced = state.channels.sumOf { it.videosProduced }
    val videosPublished = state.channels.sumOf { it.videosPublished }
    val scriptsAwaiting = state.scripts.count {
        val st = ScriptStatus.fromString(it.status)
        st == ScriptStatus.SCRIPT_SUBMITTED || st == ScriptStatus.VERIFICATION || st == ScriptStatus.REVISION_REQUIRED
    }
    val upcomingShoots = state.shootingSchedules.count { it.recordingStatus == "SCHEDULED" }

    val totalExpenses = state.financialRecords.filter { it.recordType == "EXPENSE" }.sumOf { it.amountInr }
    val totalRevenue = state.financialRecords.filter { it.recordType == "REVENUE" }.sumOf { it.amountInr }
    val totalInvestment = state.financialRecords.filter { it.recordType == "INVESTMENT" }.sumOf { it.amountInr }
    val inrFmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            // Role-specific Portal Banner (Sandeep vs Sudeep vs Avishek)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = NavyDeep
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusBadge(
                            label = when (role) {
                                UserRole.SUPER_ADMIN -> "SANDEEP • FULL ADMIN PORTAL"
                                UserRole.STRATEGIC_DIRECTOR -> "SUDEEP • STRATEGIC DIRECTOR PORTAL"
                                else -> "AVISHEK • INVESTOR READ-ONLY PORTAL"
                            },
                            color = ElectricBlueBright
                        )
                        if (role.isReadOnly) {
                            StatusBadge(label = "READ-ONLY", color = StatusCompleted)
                        }
                    }
                    Text(
                        text = when (role) {
                            UserRole.SUPER_ADMIN -> "Serplive Solutions Executive Command"
                            UserRole.STRATEGIC_DIRECTOR -> "Channel Strategy & Quality Intelligence"
                            else -> "18-Month Investment & Growth Overview"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(
                        text = when (role) {
                            UserRole.SUPER_ADMIN -> "Full control across 5 channels, team tasks, geofenced attendance, scripts & financials."
                            UserRole.STRATEGIC_DIRECTOR -> "12-year video marketing lens: growth velocity, script verification & creative quality."
                            else -> "Live capital runway, monthly expenditure, production output & channel ROI."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }

        // Date Filter + PDF/Excel(CSV) Export Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DateRangeFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.selectedDateFilter == filter,
                        onClick = { onDateFilterChange(filter) },
                        label = { Text(filter.label) }
                    )
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        val res = ReportExporter.exportPdfReport(
                            context = context,
                            reportTitle = "StudioFlow 360 — Executive Report",
                            subtitle = "Serplive Solutions | Filter: ${state.selectedDateFilter.label}",
                            sections = listOf(
                                "Workforce & Attendance" to listOf(
                                    "Total Employees: ${internalEmployees.size} | Present: $presentToday | Absent: $absentToday",
                                    "Total Tasks: ${state.tasks.size} | Completed: $completedTasks | Pending: $pendingTasks | Overdue: $overdueTasks"
                                ),
                                "5-Channel Production" to state.channels.map {
                                    "${it.name}: ${it.videosProduced}/${it.monthlyVideoTarget} produced, ${it.videosPublished} published (${it.growthPercent}% growth)"
                                },
                                "Financial Summary" to listOf(
                                    "Capital Investment: ${inrFmt.format(totalInvestment)}",
                                    "Total Expenditure: ${inrFmt.format(totalExpenses)}",
                                    "Total Revenue: ${inrFmt.format(totalRevenue)}"
                                )
                            )
                        )
                        res.onSuccess { file ->
                            ReportExporter.shareFile(context, file, "application/pdf")
                        }.onFailure {
                            onShowMessage("PDF export failed: ${it.message}")
                        }
                    },
                    modifier = Modifier.testTag("export_pdf_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("PDF")
                }
                OutlinedButton(
                    onClick = {
                        val res = ReportExporter.exportCsvReport(
                            context = context,
                            fileNamePrefix = "StudioFlow_Channels",
                            headers = listOf("Channel", "Lead", "Produced", "Published", "Monthly Target", "Growth %"),
                            rows = state.channels.map {
                                listOf(it.name, it.leadCoordinator, "${it.videosProduced}", "${it.videosPublished}", "${it.monthlyVideoTarget}", "${it.growthPercent}")
                            }
                        )
                        res.onSuccess { file ->
                            ReportExporter.shareFile(context, file, "text/csv")
                        }.onFailure {
                            onShowMessage("CSV export failed: ${it.message}")
                        }
                    },
                    modifier = Modifier.testTag("export_csv_button")
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = "Export Excel CSV", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Excel/CSV")
                }
            }
        }

        // 16 Management KPI Widgets in responsive rows
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudioMetricCard(
                        title = "Team Size",
                        value = "${state.users.size}",
                        subtitle = "${internalEmployees.size} internal • 2 external",
                        icon = Icons.Default.Groups,
                        accentColor = ElectricBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StudioMetricCard(
                        title = "Present Today",
                        value = "$presentToday / ${internalEmployees.size}",
                        subtitle = "$absentToday absent or pending",
                        icon = Icons.Default.VerifiedUser,
                        accentColor = StatusCompleted,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudioMetricCard(
                        title = "Tasks Overview",
                        value = "${state.tasks.size}",
                        subtitle = "$completedTasks done • $pendingTasks active",
                        icon = Icons.Default.TaskAlt,
                        accentColor = StatusInfo,
                        modifier = Modifier.weight(1f)
                    )
                    StudioMetricCard(
                        title = "Overdue Tasks",
                        value = "$overdueTasks",
                        subtitle = "Immediate review needed",
                        icon = Icons.Default.WarningAmber,
                        accentColor = StatusOverdue,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudioMetricCard(
                        title = "Videos Produced",
                        value = "$videosProduced",
                        subtitle = "$videosPublished published live",
                        icon = Icons.Default.MovieCreation,
                        accentColor = ElectricBlueBright,
                        modifier = Modifier.weight(1f)
                    )
                    StudioMetricCard(
                        title = "Scripts & Shoots",
                        value = "$scriptsAwaiting / $upcomingShoots",
                        subtitle = "$scriptsAwaiting scripts • $upcomingShoots shoots",
                        icon = Icons.Default.Description,
                        accentColor = StatusPending,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudioMetricCard(
                        title = "Expenditure",
                        value = inrFmt.format(totalExpenses),
                        subtitle = "Cap: ${inrFmt.format(totalInvestment)}",
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = StatusPending,
                        modifier = Modifier.weight(1f)
                    )
                    StudioMetricCard(
                        title = "Revenue",
                        value = inrFmt.format(totalRevenue),
                        subtitle = "Agency + 5 Channels",
                        icon = Icons.Default.TrendingUp,
                        accentColor = StatusCompleted,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Charts: Channel Production Bar, Task Distribution Doughnut, Channel Growth Line
        item {
            BarChartCard(
                title = "Channel-Wise Production Progress",
                subtitle = "Videos produced vs monthly targets across all 5 channels",
                items = state.channels.map { ch ->
                    ChartBarItem(
                        label = ch.name.split(" ").first(),
                        value = ch.videosProduced.toFloat(),
                        max = ch.monthlyVideoTarget.toFloat(),
                        color = Color(ch.accentColorHex)
                    )
                }
            )
        }

        item {
            DoughnutChartCard(
                title = "Studio Task Status Distribution",
                centerLabel = "${state.tasks.size} Tasks",
                slices = listOf(
                    DoughnutSlice("Completed", completedTasks.toFloat(), StatusCompleted),
                    DoughnutSlice("Pending / Active", pendingTasks.toFloat(), StatusPending),
                    DoughnutSlice("Overdue", overdueTasks.toFloat(), StatusOverdue)
                )
            )
        }

        item {
            LineTrendChartCard(
                title = "5-Channel Growth Velocity",
                subtitle = "Monthly audience & output growth across Serplive Solutions",
                points = state.channels.map { it.name.take(5) to it.growthPercent },
                lineColor = ElectricBlue
            )
        }

        // Quick Module Links & Employee Performance Snapshot
        item {
            SectionHeader(
                title = "Top Team Efficiency Scores",
                subtitle = "Multi-factor score (On-time, Quality, Complexity, Attendance)",
                action = {
                    TextButton(onClick = { onNavigateModule("performance") }) {
                        Text("Full Report")
                    }
                }
            )
        }

        items(state.performanceSummaries.take(4)) { p ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceWhite
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.fullName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(
                            "${p.completed}/${p.totalAssigned} tasks • ${p.attendancePercent}% attendance",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    StatusBadge(
                        label = "${p.explainableScore}/100",
                        color = if (p.explainableScore >= 80) StatusCompleted else StatusPending
                    )
                }
            }
        }
    }
}

@Composable
fun EmployeeDashboard(
    state: StudioFlowUiState,
    onUpdateTask: (TaskEntity, TaskStatus, String, String, String) -> Unit,
    onNavigateModule: (String) -> Unit
) {
    val user = state.currentUser ?: return
    val myTasks = state.tasks.filter { it.assignedToIdsCsv.split(",").contains(user.id) }
    val pending = myTasks.filter {
        val st = TaskStatus.fromString(it.status)
        st != TaskStatus.COMPLETED && st != TaskStatus.APPROVED && st != TaskStatus.OVERDUE
    }
    val completed = myTasks.filter {
        val st = TaskStatus.fromString(it.status)
        st == TaskStatus.COMPLETED || st == TaskStatus.APPROVED
    }
    val overdue = myTasks.filter { TaskStatus.fromString(it.status) == TaskStatus.OVERDUE }
    val completionPct = if (myTasks.isEmpty()) 100 else (completed.size * 100 / myTasks.size)

    val todayKey = GeofenceHelper.todayDateKey()
    val todayAtt = state.attendance.firstOrNull { it.userId == user.id && it.dateKey == todayKey }
    val myPerf = state.performanceSummaries.firstOrNull { it.userId == user.id }
    val myScripts = state.scripts.filter {
        it.writerId == user.id || it.anchorName.contains(user.fullName, ignoreCase = true) ||
            it.editorName.contains(user.fullName.split(" ").first(), ignoreCase = true)
    }
    val todayShoots = state.shootingSchedules.filter {
        it.shootDateKey == todayKey && (it.talentId == user.id ||
            it.coordinatorName.equals(user.fullName, ignoreCase = true) ||
            it.editorName.contains(user.fullName.split(" ").first(), ignoreCase = true))
    }

    var submittingTask by remember { mutableStateOf<TaskEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = NavyDeep
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Welcome back, ${user.fullName}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(
                        text = "${user.designation} • ${user.department}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF93C5FD)
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusBadge(
                            label = if (todayAtt?.checkInTimeMillis != null) "Checked In: ${GeofenceHelper.formatTime(todayAtt.checkInTimeMillis)}" else "Not Checked In",
                            color = if (todayAtt?.checkInTimeMillis != null) StatusCompleted else StatusPending
                        )
                        StatusBadge(
                            label = "Score: ${myPerf?.explainableScore ?: 88}/100",
                            color = ElectricBlueBright
                        )
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StudioMetricCard(
                    title = "Completion",
                    value = "$completionPct%",
                    subtitle = "${completed.size}/${myTasks.size} tasks",
                    icon = Icons.Default.CheckCircle,
                    accentColor = StatusCompleted,
                    modifier = Modifier.weight(1f)
                )
                StudioMetricCard(
                    title = "Pending / Overdue",
                    value = "${pending.size} / ${overdue.size}",
                    subtitle = "Active queue",
                    icon = Icons.Default.Schedule,
                    accentColor = if (overdue.isNotEmpty()) StatusOverdue else StatusPending,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { onNavigateModule("attendance") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Attendance", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Mark Attendance")
                }
                if (state.currentRole == UserRole.AI_PARTNER) {
                    Button(
                        onClick = { onNavigateModule("ai_studio") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Studio", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("AI Prompt Studio")
                    }
                } else {
                    Button(
                        onClick = { onNavigateModule("files_publishing") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Upload Work", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Files & Upload")
                    }
                }
            }
        }

        item {
            SectionHeader("My Assigned Tasks", "${myTasks.size} total tasks")
        }

        if (myTasks.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.TaskAlt,
                    title = "No tasks assigned right now",
                    description = "New assignments from Sandeep or coordinators will appear here."
                )
            }
        } else {
            items(myTasks, key = { it.id }) { task ->
                val st = TaskStatus.fromString(task.status)
                val badgeColor = when (st) {
                    TaskStatus.COMPLETED, TaskStatus.APPROVED -> StatusCompleted
                    TaskStatus.OVERDUE, TaskStatus.REVISION_REQUIRED -> StatusOverdue
                    else -> StatusPending
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceWhite
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                "${task.taskCode} • ${task.channelName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricBlue
                            )
                            StatusBadge(st.label, badgeColor)
                        }
                        Text(task.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        if (task.managerComments.isNotBlank()) {
                            Text(
                                "Manager comment: ${task.managerComments}",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusInfo
                            )
                        }
                        LinearProgressIndicator(
                            progress = { st.progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = badgeColor,
                            trackColor = SurfaceMuted
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Due: ${GeofenceHelper.formatDateTime(task.deadlineMillis)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            if (st != TaskStatus.COMPLETED && st != TaskStatus.APPROVED) {
                                TextButton(onClick = { submittingTask = task }) {
                                    Text("Update / Submit")
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionHeader("Today's Shooting Schedule & Scripts", "${todayShoots.size} shoots • ${myScripts.size} scripts")
        }

        items(todayShoots) { shoot ->
            Surface(shape = RoundedCornerShape(14.dp), color = SurfaceWhite) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(shoot.topicOrKathaName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("${shoot.channelName} • ${shoot.studioRoom} • ${shoot.startTime}-${shoot.expectedEndTime}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    StatusBadge(shoot.recordingStatus, StatusInfo)
                }
            }
        }

        items(myScripts.take(3)) { scr ->
            Surface(shape = RoundedCornerShape(14.dp), color = SurfaceWhite) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${scr.scriptCode}: ${scr.videoTitle}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("${scr.channelName} • v${scr.currentVersion}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    StatusBadge(ScriptStatus.fromString(scr.status).label, ElectricBlue)
                }
            }
        }
    }

    submittingTask?.let { task ->
        var note by remember { mutableStateOf(task.submissionNote) }
        var fileName by remember { mutableStateOf(task.submissionFileName.ifBlank { "${task.taskCode}_Deliverable.mp4" }) }
        var delayReason by remember { mutableStateOf(task.delayReason) }
        var chosenStatus by remember { mutableStateOf(TaskStatus.SUBMITTED_FOR_REVIEW) }

        AlertDialog(
            onDismissRequest = { submittingTask = null },
            title = { Text("Submit / Update ${task.taskCode}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(TaskStatus.IN_PROGRESS, TaskStatus.SUBMITTED_FOR_REVIEW).forEach { opt ->
                            FilterChip(
                                selected = chosenStatus == opt,
                                onClick = { chosenStatus = opt },
                                label = { Text(opt.label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Work Submission Notes") }
                    )
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text("Attached Output File Name") }
                    )
                    if (TaskStatus.fromString(task.status) == TaskStatus.OVERDUE) {
                        OutlinedTextField(
                            value = delayReason,
                            onValueChange = { delayReason = it },
                            label = { Text("Reason for Delay (Required)") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateTask(task, chosenStatus, note, fileName, delayReason)
                    submittingTask = null
                }) { Text("Save Update") }
            },
            dismissButton = {
                TextButton(onClick = { submittingTask = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ExternalContributorDashboard(state: StudioFlowUiState) {
    val user = state.currentUser ?: return
    val role = state.currentRole ?: UserRole.EXTERNAL_PANDIT
    val myShoots = state.shootingSchedules.filter {
        it.talentId == user.id || it.talentName.equals(user.fullName, ignoreCase = true)
    }
    val myChannelIds = user.assignedChannelsCsv.split(",")
    val myScripts = state.scripts.filter { myChannelIds.contains(it.channelId) }
    val myFiles = state.studioFiles.filter { myChannelIds.contains(it.channelId) && it.approvalStatus == "APPROVED" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Surface(shape = RoundedCornerShape(18.dp), color = NavyDeep) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusBadge("EXTERNAL CONTRIBUTOR PORTAL", ChannelVratColor)
                    Text(
                        text = if (role == UserRole.EXTERNAL_PANDIT) "Namaste, ${user.fullName}" else "Welcome, ${user.fullName}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(
                        text = if (role == UserRole.EXTERNAL_PANDIT)
                            "Scoped access to your Vrat Katha narration schedules, approved scripts & studio instructions."
                        else
                            "Scoped access to Art & Craft studio schedules, project scripts, material lists & reference guides.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }

        item {
            SectionHeader("My Shooting Schedule", "${myShoots.size} assigned sessions")
        }

        items(myShoots) { shoot ->
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
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(shoot.topicOrKathaName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        StatusBadge(shoot.recordingStatus, StatusCompleted)
                    }
                    Text("Date: ${shoot.shootDateKey} • Arrival: ${shoot.arrivalTime} • Shoot: ${shoot.startTime}-${shoot.expectedEndTime}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("Studio: ${shoot.studioRoom} • Coordinator: ${shoot.coordinatorName}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    if (shoot.materialsRequired.isNotBlank()) {
                        Text("Materials: ${shoot.materialsRequired}", style = MaterialTheme.typography.bodySmall, color = ChannelArtColor)
                    }
                    if (shoot.specialRequirements.isNotBlank()) {
                        Text("Notes: ${shoot.specialRequirements}", style = MaterialTheme.typography.bodySmall, color = StatusInfo)
                    }
                }
            }
        }

        item {
            SectionHeader("Assigned Scripts & Reference Materials", "${myScripts.size} scripts • ${myFiles.size} files")
        }

        items(myScripts) { script ->
            Surface(shape = RoundedCornerShape(14.dp), color = SurfaceWhite) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${script.scriptCode} • ${script.videoTitle}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        StatusBadge(ScriptStatus.fromString(script.status).label, ElectricBlue)
                    }
                    Text(script.contentBody, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        items(myFiles) { file ->
            Surface(shape = RoundedCornerShape(12.dp), color = SurfaceMuted) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(file.fileName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("${file.contentTitle} • ${file.fileType} • ${file.fileSizeLabel}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    Icon(Icons.Default.Download, contentDescription = "Approved File", tint = ElectricBlue)
                }
            }
        }
    }
}
