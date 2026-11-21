package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiPromptRepository
import com.example.data.local.*
import com.example.data.model.DateRangeFilter
import com.example.data.model.LoginPortal
import com.example.data.model.ScriptStatus
import com.example.data.model.TaskStatus
import com.example.data.model.UserRole
import com.example.data.repository.PerformanceCalculator
import com.example.data.repository.PerformanceSummary
import com.example.firebase.FirestoreWorkspaceRepository
import com.example.firebase.WorkspaceUserDoc
import com.example.util.GeofenceHelper
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val TAG = "StudioFlowVM"

data class StudioFlowUiState(
    val isInitializing: Boolean = true,
    val currentUser: UserEntity? = null,
    val currentRole: UserRole? = null,
    val googleEmail: String = "",
    val isPersonaPicked: Boolean = false,
    val users: List<UserEntity> = emptyList(),
    val channels: List<ChannelEntity> = emptyList(),
    val workspaceItems: List<ChannelWorkspaceItemEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val attendance: List<AttendanceEntity> = emptyList(),
    val officeConfig: OfficeConfigEntity? = null,
    val attendanceAudits: List<AttendanceAuditEntity> = emptyList(),
    val scripts: List<ScriptEntity> = emptyList(),
    val scriptVersions: List<ScriptVersionEntity> = emptyList(),
    val calendarEntries: List<CalendarEntryEntity> = emptyList(),
    val shootingSchedules: List<ShootingScheduleEntity> = emptyList(),
    val aiAssignments: List<AiAssignmentEntity> = emptyList(),
    val studioFiles: List<StudioFileEntity> = emptyList(),
    val socialPosts: List<SocialPostEntity> = emptyList(),
    val financialRecords: List<FinancialRecordEntity> = emptyList(),
    val notifications: List<NotificationEntity> = emptyList(),
    val loginHistory: List<LoginHistoryEntity> = emptyList(),
    val performanceSummaries: List<PerformanceSummary> = emptyList(),
    val selectedDateFilter: DateRangeFilter = DateRangeFilter.THIS_MONTH,
    val authError: String? = null,
    val firestoreError: String? = null,
    val bannerMessage: String? = null,
    val isBusy: Boolean = false,
    val aiOutputPreview: String? = null,
    val aiNoticeBanner: String = ""
)

class StudioFlowViewModel(
    private val repo: FirestoreWorkspaceRepository,
    private val aiRepo: AiPromptRepository,
    private val currentUserId: String,
    private val userEmail: String?,
    private val userDisplayName: String?
) : ViewModel() {

    private val _authError = MutableStateFlow<String?>(null)
    private val _firestoreError = MutableStateFlow<String?>(null)
    private val _bannerMessage = MutableStateFlow<String?>(null)
    private val _isBusy = MutableStateFlow(false)
    private val _isInitializing = MutableStateFlow(true)
    private val _isPersonaPicked = MutableStateFlow(false)
    private val _dateFilter = MutableStateFlow(DateRangeFilter.THIS_MONTH)
    private val _aiOutputPreview = MutableStateFlow<String?>(null)

    private fun <T> Flow<T>.safeFirestoreFlow(fallback: T, label: String): Flow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing $label", rootError)
            _firestoreError.value = rootError.message ?: "Failed to sync $label from Firestore"
            emit(fallback)
        }

    private val bundleA = combine(
        repo.observeWorkspaceUser(currentUserId).safeFirestoreFlow(null, "workspaceUser"),
        repo.observeMembers(currentUserId).safeFirestoreFlow(emptyList(), "members"),
        repo.observeChannels(currentUserId).safeFirestoreFlow(emptyList(), "channels"),
        repo.observeWorkspaceItems(currentUserId).safeFirestoreFlow(emptyList(), "workspaceItems"),
        repo.observeTasks(currentUserId).safeFirestoreFlow(emptyList(), "tasks")
    ) { wsUser, members, channels, wsItems, tasks ->
        Tuple5(wsUser, members, channels, wsItems, tasks)
    }

    private val bundleB = combine(
        repo.observeAttendance(currentUserId).safeFirestoreFlow(emptyList(), "attendance"),
        repo.observeOfficeConfig(currentUserId).safeFirestoreFlow(null, "officeConfig"),
        repo.observeAttendanceAudits(currentUserId).safeFirestoreFlow(emptyList(), "audits"),
        repo.observeScripts(currentUserId).safeFirestoreFlow(emptyList(), "scripts"),
        repo.observeScriptVersions(currentUserId).safeFirestoreFlow(emptyList(), "scriptVersions")
    ) { att, cfg, audits, scripts, versions ->
        Tuple5(att, cfg, audits, scripts, versions)
    }

    private val bundleC = combine(
        repo.observeCalendarEntries(currentUserId).safeFirestoreFlow(emptyList(), "calendar"),
        repo.observeShootingSchedules(currentUserId).safeFirestoreFlow(emptyList(), "shoots"),
        repo.observeAiAssignments(currentUserId).safeFirestoreFlow(emptyList(), "aiAssignments"),
        repo.observeStudioFiles(currentUserId).safeFirestoreFlow(emptyList(), "studioFiles"),
        repo.observeSocialPosts(currentUserId).safeFirestoreFlow(emptyList(), "socialPosts")
    ) { cal, shoots, ai, files, posts ->
        Tuple5(cal, shoots, ai, files, posts)
    }

    private val bundleD = combine(
        repo.observeFinancialRecords(currentUserId).safeFirestoreFlow(emptyList(), "finance"),
        repo.observeNotifications(currentUserId).safeFirestoreFlow(emptyList(), "notifications"),
        repo.observeLoginHistory(currentUserId).safeFirestoreFlow(emptyList(), "loginHistory"),
        _dateFilter,
        _bannerMessage
    ) { fin, notifs, hist, filter, banner ->
        Tuple5(fin, notifs, hist, filter, banner)
    }

    private val statusBundle = combine(
        _isInitializing,
        _isBusy,
        _isPersonaPicked,
        _authError,
        combine(_firestoreError, _aiOutputPreview) { fsErr, aiOut -> fsErr to aiOut }
    ) { init, busy, picked, authErr, fsAndAi ->
        Tuple5(init, busy, picked, authErr, fsAndAi)
    }

    val uiState: StateFlow<StudioFlowUiState> = combine(
        bundleA,
        bundleB,
        bundleC,
        bundleD,
        statusBundle
    ) { a, b, c, d, st ->
        val (wsUser, users, channels, workspace, tasks) = a
        val (att, cfg, audits, scripts, versions) = b
        val (cal, shoots, ai, files, posts) = c
        val (fin, notifs, history, filter, banner) = d
        val (init, busy, picked, authErr, fsAndAi) = st
        val (fsErr, aiPreview) = fsAndAi

        val personaId = wsUser?.activePersonaId
        val activeUser = if (picked && !personaId.isNullOrBlank()) {
            users.firstOrNull { it.id == personaId }
        } else null
        val role = activeUser?.let { UserRole.fromString(it.role) }
        val perf = PerformanceCalculator.computePerformanceSummaries(users, tasks, att)

        StudioFlowUiState(
            isInitializing = init,
            currentUser = activeUser,
            currentRole = role,
            googleEmail = wsUser?.email ?: userEmail.orEmpty(),
            isPersonaPicked = picked,
            users = users,
            channels = channels,
            workspaceItems = workspace,
            tasks = tasks,
            attendance = att,
            officeConfig = cfg,
            attendanceAudits = audits,
            scripts = scripts,
            scriptVersions = versions,
            calendarEntries = cal,
            shootingSchedules = shoots,
            aiAssignments = ai,
            studioFiles = files,
            socialPosts = posts,
            financialRecords = fin,
            notifications = notifs,
            loginHistory = history,
            performanceSummaries = perf,
            selectedDateFilter = filter,
            authError = authErr,
            firestoreError = fsErr,
            bannerMessage = banner,
            isBusy = busy,
            aiOutputPreview = aiPreview,
            aiNoticeBanner = aiRepo.noticeBanner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = StudioFlowUiState(aiNoticeBanner = aiRepo.noticeBanner)
    )

    init {
        viewModelScope.launch {
            val res = repo.initializeWorkspace(currentUserId, userEmail, userDisplayName)
            res.onFailure { _firestoreError.value = it.localizedMessage ?: "Failed to initialize workspace." }
            _isInitializing.value = false
        }
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun setDateFilter(filter: DateRangeFilter) {
        _dateFilter.value = filter
    }

    fun selectRolePersona(member: UserEntity, portal: LoginPortal) {
        viewModelScope.launch {
            _isBusy.value = true
            _authError.value = null
            val role = UserRole.fromString(member.role)
            if (role.portal != portal) {
                _authError.value = "${member.fullName} belongs to the ${role.portal.label} portal."
                _isBusy.value = false
                return@launch
            }
            val res = repo.selectPersona(currentUserId, member, portal)
            res.onSuccess {
                _isPersonaPicked.value = true
                _bannerMessage.value = "Active role: ${member.fullName} (${role.displayName})"
            }
            res.onFailure {
                _authError.value = it.message ?: "Unable to open role workspace."
            }
            _isBusy.value = false
        }
    }

    fun switchRolePortal() {
        _isPersonaPicked.value = false
        _authError.value = null
    }

    fun toggleUserStatus(target: UserEntity) {
        val admin = uiState.value.currentUser ?: return
        viewModelScope.launch {
            val res = repo.toggleMemberStatus(currentUserId, target, admin.fullName)
            _bannerMessage.value = res.fold(
                onSuccess = { "Updated status for ${target.fullName}." },
                onFailure = { it.message ?: "Failed to update account status." }
            )
        }
    }

    fun createTask(
        title: String,
        description: String,
        instructions: String,
        channel: ChannelEntity,
        category: String,
        assignees: List<UserEntity>,
        priority: String,
        deadlineHoursFromNow: Int,
        expectedMinutes: Int,
        complexityWeight: Int,
        attachedFileName: String
    ) {
        val creator = uiState.value.currentUser ?: return
        if (title.isBlank() || assignees.isEmpty()) {
            _bannerMessage.value = "Enter a task title and select at least one employee."
            return
        }
        viewModelScope.launch {
            val deadline = System.currentTimeMillis() + deadlineHoursFromNow * 3600_000L
            val res = repo.createTask(
                uid = currentUserId,
                existingCount = uiState.value.tasks.size,
                title = title,
                description = description,
                instructions = instructions,
                channel = channel,
                category = category,
                assignees = assignees,
                createdBy = creator,
                priority = priority,
                deadlineMillis = deadline,
                expectedMinutes = expectedMinutes,
                complexityWeight = complexityWeight,
                attachedFileName = attachedFileName
            )
            _bannerMessage.value = res.fold(
                onSuccess = { "Task assigned to ${assignees.joinToString(", ") { it.fullName }}." },
                onFailure = { it.message ?: "Failed to create task." }
            )
        }
    }

    fun updateTaskStatus(
        task: TaskEntity,
        status: TaskStatus,
        submissionNote: String = task.submissionNote,
        submissionFile: String = task.submissionFileName,
        delayReason: String = task.delayReason,
        managerComments: String = task.managerComments,
        extendHours: Int? = null
    ) {
        viewModelScope.launch {
            val revisedDeadline = extendHours?.let { System.currentTimeMillis() + it * 3600_000L } ?: task.revisedDeadlineMillis
            val res = repo.updateTaskStatus(
                uid = currentUserId,
                task = task,
                newStatus = status,
                submissionNote = submissionNote,
                submissionFile = submissionFile,
                delayReason = delayReason,
                managerComments = managerComments,
                revisedDeadlineMillis = revisedDeadline
            )
            _bannerMessage.value = res.fold(
                onSuccess = { "${task.taskCode} updated to ${status.label}." },
                onFailure = { it.message ?: "Failed to update task." }
            )
        }
    }

    fun markAttendanceCheckIn(
        method: String,
        lat: Double,
        lng: Double,
        wifiSsid: String,
        qrTokenInput: String? = null
    ) {
        val user = uiState.value.currentUser ?: return
        val cfg = uiState.value.officeConfig ?: return
        val todayKey = GeofenceHelper.todayDateKey()
        val existingToday = uiState.value.attendance.firstOrNull { it.userId == user.id && it.dateKey == todayKey }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repo.performOfficeCheckIn(currentUserId, cfg, existingToday, user, method, lat, lng, wifiSsid, qrTokenInput)
            _bannerMessage.value = res.fold(
                onSuccess = { "Checked in successfully at ${GeofenceHelper.formatTime(it.checkInTimeMillis)} (${it.status})." },
                onFailure = { it.message ?: "Attendance verification failed." }
            )
            _isBusy.value = false
        }
    }

    fun markAttendanceCheckOut(lat: Double, lng: Double) {
        val user = uiState.value.currentUser ?: return
        val cfg = uiState.value.officeConfig ?: return
        val todayKey = GeofenceHelper.todayDateKey()
        val existingToday = uiState.value.attendance.firstOrNull { it.userId == user.id && it.dateKey == todayKey }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repo.performOfficeCheckOut(currentUserId, cfg, existingToday, lat, lng)
            _bannerMessage.value = res.fold(
                onSuccess = { "Checked out at ${GeofenceHelper.formatTime(it.checkOutTimeMillis)}. Total: ${it.workingMinutes} mins." },
                onFailure = { it.message ?: "Check-out failed." }
            )
            _isBusy.value = false
        }
    }

    fun requestAttendanceCorrection(reason: String) {
        val user = uiState.value.currentUser ?: return
        if (reason.isBlank()) return
        val todayKey = GeofenceHelper.todayDateKey()
        val existingToday = uiState.value.attendance.firstOrNull { it.userId == user.id && it.dateKey == todayKey }
        viewModelScope.launch {
            val res = repo.requestAttendanceCorrection(currentUserId, existingToday, user, reason)
            _bannerMessage.value = res.fold(
                onSuccess = { "Manual attendance correction sent for manager approval." },
                onFailure = { it.message ?: "Failed to submit correction request." }
            )
        }
    }

    fun resolveAttendanceCorrection(record: AttendanceEntity, approve: Boolean) {
        val admin = uiState.value.currentUser ?: return
        viewModelScope.launch {
            val res = repo.approveAttendanceCorrection(currentUserId, record, admin.fullName, approve)
            _bannerMessage.value = res.fold(
                onSuccess = { if (approve) "Correction approved for ${record.userName}." else "Correction rejected." },
                onFailure = { it.message ?: "Failed to update correction." }
            )
        }
    }

    fun updateOfficeConfig(
        officeName: String,
        lat: Double,
        lng: Double,
        radiusMeters: Float,
        wifiSsid: String
    ) {
        val admin = uiState.value.currentUser ?: return
        val docId = uiState.value.officeConfig?.docId ?: "hq_main"
        viewModelScope.launch {
            val res = repo.updateOfficeConfig(currentUserId, docId, officeName, lat, lng, radiusMeters, wifiSsid, admin.fullName)
            _bannerMessage.value = res.fold(
                onSuccess = { "Office geofence & QR updated." },
                onFailure = { it.message ?: "Failed to update office config." }
            )
        }
    }

    fun saveScript(
        existing: ScriptEntity?,
        channel: ChannelEntity,
        videoTitle: String,
        anchorName: String,
        editorName: String,
        contentBody: String,
        attachmentName: String,
        changeNote: String,
        status: ScriptStatus
    ) {
        val writer = uiState.value.currentUser ?: return
        if (videoTitle.isBlank() || contentBody.isBlank()) return
        viewModelScope.launch {
            val res = repo.saveScript(
                uid = currentUserId,
                existingCount = uiState.value.scripts.size,
                existing = existing,
                channel = channel,
                videoTitle = videoTitle,
                writer = writer,
                anchorName = anchorName,
                editorName = editorName,
                contentBody = contentBody,
                attachmentName = attachmentName,
                changeNote = changeNote,
                status = status
            )
            _bannerMessage.value = res.fold(
                onSuccess = { "Script saved (${status.label})." },
                onFailure = { it.message ?: "Failed to save script." }
            )
        }
    }

    fun reviewScript(
        script: ScriptEntity,
        nextStatus: ScriptStatus,
        comments: String,
        highlightedErrors: String,
        factChecked: Boolean
    ) {
        val reviewer = uiState.value.currentUser ?: return
        viewModelScope.launch {
            val res = repo.reviewScript(currentUserId, script, nextStatus, comments, highlightedErrors, factChecked, reviewer.fullName)
            _bannerMessage.value = res.fold(
                onSuccess = { "${script.scriptCode} marked ${nextStatus.label}." },
                onFailure = { it.message ?: "Failed to review script." }
            )
        }
    }

    fun addCalendarEntry(entry: CalendarEntryEntity) {
        viewModelScope.launch {
            val conflict = uiState.value.calendarEntries.firstOrNull {
                it.shootingDateKey == entry.shootingDateKey &&
                    it.shootingTimeSlot == entry.shootingTimeSlot &&
                    (it.anchorName.equals(entry.anchorName, ignoreCase = true) ||
                        it.studioRoom.equals(entry.studioRoom, ignoreCase = true))
            }
            val res = repo.addCalendarEntry(currentUserId, entry)
            _bannerMessage.value = res.fold(
                onSuccess = {
                    if (conflict != null) {
                        "Warning: Conflict detected with '${conflict.contentTitle}' (${conflict.anchorName} / ${conflict.studioRoom})."
                    } else {
                        "Calendar entry scheduled for ${entry.channelName}."
                    }
                },
                onFailure = { it.message ?: "Failed to schedule calendar entry." }
            )
        }
    }

    fun rescheduleCalendarEntry(entry: CalendarEntryEntity, newDateKey: String, newSlot: String) {
        viewModelScope.launch {
            val res = repo.rescheduleCalendarEntry(currentUserId, entry, newDateKey, newSlot)
            _bannerMessage.value = res.fold(
                onSuccess = { "Rescheduled '${entry.contentTitle}' to $newDateKey ($newSlot)." },
                onFailure = { it.message ?: "Failed to reschedule entry." }
            )
        }
    }

    fun addShootingSchedule(schedule: ShootingScheduleEntity) {
        viewModelScope.launch {
            val conflict = uiState.value.shootingSchedules.firstOrNull {
                it.shootDateKey == schedule.shootDateKey &&
                    it.startTime == schedule.startTime &&
                    (it.talentName.equals(schedule.talentName, ignoreCase = true) ||
                        it.studioRoom.equals(schedule.studioRoom, ignoreCase = true))
            }
            val res = repo.addShootingSchedule(currentUserId, schedule)
            _bannerMessage.value = res.fold(
                onSuccess = {
                    if (conflict != null) {
                        "Schedule conflict: ${conflict.talentName} / ${conflict.studioRoom} already booked at ${conflict.startTime}."
                    } else {
                        "Shooting scheduled for ${schedule.talentName}."
                    }
                },
                onFailure = { it.message ?: "Failed to add shooting schedule." }
            )
        }
    }

    fun updateShootingStatus(schedule: ShootingScheduleEntity, status: String, actualEnd: String = schedule.actualEndTime) {
        viewModelScope.launch {
            val res = repo.updateShootingStatus(currentUserId, schedule, status, actualEnd)
            _bannerMessage.value = res.fold(
                onSuccess = { "Recording status updated to $status." },
                onFailure = { it.message ?: "Failed to update recording status." }
            )
        }
    }

    fun startAiAssignment(assignment: AiAssignmentEntity) {
        viewModelScope.launch {
            val res = repo.startAiAssignment(currentUserId, assignment)
            _bannerMessage.value = res.fold(
                onSuccess = { "${assignment.assignmentCode} timer started." },
                onFailure = { it.message ?: "Failed to start timer." }
            )
        }
    }

    fun generateAiSamplePrompt(channelName: String, promptType: String, brief: String) {
        viewModelScope.launch {
            _isBusy.value = true
            _aiOutputPreview.value = null
            try {
                aiRepo.generatePromptStream(channelName, promptType, brief).collect { partial ->
                    _aiOutputPreview.value = partial
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI generation timed out", e)
                _bannerMessage.value = "Gemini request timed out after 60 seconds. Please try again."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini prompt generation failed", e)
                _bannerMessage.value = e.localizedMessage ?: "Gemini generation failed. Check App Check / network."
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun submitAiAssignment(assignment: AiAssignmentEntity, promptText: String, attachedAssetName: String) {
        if (promptText.isBlank()) return
        viewModelScope.launch {
            val res = repo.submitAiAssignment(currentUserId, assignment, promptText, attachedAssetName)
            _bannerMessage.value = res.fold(
                onSuccess = { "${assignment.assignmentCode} submitted." },
                onFailure = { it.message ?: "Failed to submit assignment." }
            )
        }
    }

    fun createAiAssignment(channel: ChannelEntity, promptType: String, title: String, brief: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val res = repo.createAiAssignment(currentUserId, uiState.value.aiAssignments.size, channel, promptType, title, brief)
            _bannerMessage.value = res.fold(
                onSuccess = { "Assigned prompt task to Manoj." },
                onFailure = { it.message ?: "Failed to assign AI task." }
            )
        }
    }

    fun uploadStudioFile(file: StudioFileEntity) {
        viewModelScope.launch {
            val res = repo.uploadStudioFile(currentUserId, file)
            _bannerMessage.value = res.fold(
                onSuccess = { "Uploaded ${file.fileName} (${file.channelName})." },
                onFailure = { it.message ?: "Failed to upload file metadata." }
            )
        }
    }

    fun approveStudioFile(file: StudioFileEntity, status: String) {
        viewModelScope.launch {
            val res = repo.updateStudioFileStatus(currentUserId, file, status)
            _bannerMessage.value = res.fold(
                onSuccess = { "${file.fileName} marked $status." },
                onFailure = { it.message ?: "Failed to update file status." }
            )
        }
    }

    fun saveSocialPost(post: SocialPostEntity) {
        viewModelScope.launch {
            val res = repo.saveSocialPost(currentUserId, post)
            _bannerMessage.value = res.fold(
                onSuccess = { "Publishing item '${post.title}' updated (${post.status})." },
                onFailure = { it.message ?: "Failed to save social post." }
            )
        }
    }

    fun addChannelWorkspaceItem(item: ChannelWorkspaceItemEntity) {
        viewModelScope.launch {
            val res = repo.addChannelWorkspaceItem(currentUserId, item)
            _bannerMessage.value = res.fold(
                onSuccess = { "Added '${item.title}' to workspace." },
                onFailure = { it.message ?: "Failed to add workspace item." }
            )
        }
    }

    fun toggleChannelWorkspaceItem(item: ChannelWorkspaceItemEntity) {
        viewModelScope.launch {
            repo.toggleChannelWorkspaceItem(currentUserId, item)
                .onFailure { _bannerMessage.value = it.message ?: "Failed to update item." }
        }
    }

    fun addFinancialRecord(record: FinancialRecordEntity) {
        viewModelScope.launch {
            val res = repo.addFinancialRecord(currentUserId, record)
            _bannerMessage.value = res.fold(
                onSuccess = { "Financial entry '${record.title}' saved." },
                onFailure = { it.message ?: "Failed to save financial record." }
            )
        }
    }

    fun markNotificationRead(notifDocId: String) {
        if (notifDocId.isBlank()) return
        viewModelScope.launch { repo.markNotificationRead(currentUserId, notifDocId) }
    }

    fun markAllNotificationsRead() {
        val memberId = uiState.value.currentUser?.id ?: return
        viewModelScope.launch { repo.markAllNotificationsRead(currentUserId, uiState.value.notifications, memberId) }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
