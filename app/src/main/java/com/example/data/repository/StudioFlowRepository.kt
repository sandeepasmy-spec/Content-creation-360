package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.*
import com.example.data.model.LoginPortal
import com.example.data.model.ScriptStatus
import com.example.data.model.TaskStatus
import com.example.data.model.UserRole
import com.example.util.GeofenceHelper
import com.example.util.SecurityUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar
import kotlin.math.roundToInt

private val Context.sessionDataStore by preferencesDataStore(name = "studioflow_session")

data class PerformanceSummary(
    val userId: String,
    val fullName: String,
    val designation: String,
    val totalAssigned: Int,
    val completed: Int,
    val onTime: Int,
    val delayed: Int,
    val awaitingApproval: Int,
    val revisionsCount: Int,
    val avgCompletionMinutes: Int,
    val attendancePercent: Int,
    val explainableScore: Int,
    val scoreBreakdown: String
)

class StudioFlowRepository(
    private val context: Context,
    private val db: StudioFlowDatabase
) {
    private val KEY_LOGGED_USER_ID = stringPreferencesKey("logged_in_user_id")

    val currentUserIdFlow: Flow<String?> = context.sessionDataStore.data.map { prefs ->
        prefs[KEY_LOGGED_USER_ID]
    }

    val allUsers: Flow<List<UserEntity>> = db.userDao().observeAllUsers()
    val allChannels: Flow<List<ChannelEntity>> = db.channelDao().observeAllChannels()
    val workspaceItems: Flow<List<ChannelWorkspaceItemEntity>> = db.channelDao().observeWorkspaceItems()
    val allTasks: Flow<List<TaskEntity>> = db.taskDao().observeAllTasks()
    val allAttendance: Flow<List<AttendanceEntity>> = db.attendanceDao().observeAllAttendance()
    val officeConfig: Flow<OfficeConfigEntity?> = db.attendanceDao().observeOfficeConfig()
    val attendanceAuditLogs: Flow<List<AttendanceAuditEntity>> = db.attendanceDao().observeAuditLogs()
    val allScripts: Flow<List<ScriptEntity>> = db.scriptDao().observeAllScripts()
    val scriptVersions: Flow<List<ScriptVersionEntity>> = db.scriptDao().observeAllVersions()
    val calendarEntries: Flow<List<CalendarEntryEntity>> = db.calendarShootingDao().observeCalendarEntries()
    val shootingSchedules: Flow<List<ShootingScheduleEntity>> = db.calendarShootingDao().observeShootingSchedules()
    val aiAssignments: Flow<List<AiAssignmentEntity>> = db.opsDao().observeAiAssignments()
    val studioFiles: Flow<List<StudioFileEntity>> = db.opsDao().observeStudioFiles()
    val socialPosts: Flow<List<SocialPostEntity>> = db.opsDao().observeSocialPosts()
    val financialRecords: Flow<List<FinancialRecordEntity>> = db.opsDao().observeFinancialRecords()
    val notifications: Flow<List<NotificationEntity>> = db.opsDao().observeNotifications()

    fun observeLoginHistory(userId: String): Flow<List<LoginHistoryEntity>> =
        db.userDao().observeLoginHistory(userId)

    suspend fun initializeDatabase() {
        DatabaseSeeder.seedIfEmpty(db)
        sweepOverdueTasks()
    }

    suspend fun login(username: String, password: String, selectedPortal: LoginPortal): Result<UserEntity> {
        val cleanUser = username.trim().lowercase()
        val user = db.userDao().getByUsername(cleanUser)
            ?: return Result.failure(IllegalArgumentException("No account found for username '$cleanUser'."))

        if (user.accountStatus != "ACTIVE") {
            return Result.failure(IllegalStateException("This account is currently suspended. Contact Sandeep."))
        }

        val role = UserRole.fromString(user.role)
        if (role.portal != selectedPortal) {
            return Result.failure(
                IllegalArgumentException("Account '$cleanUser' must sign in through the ${role.portal.label} portal.")
            )
        }

        val ok = SecurityUtils.verifyPassword(password, user.salt, user.passwordHash)
        db.userDao().insertLoginHistory(
            LoginHistoryEntity(
                userId = user.id,
                username = user.username,
                portal = selectedPortal.label,
                deviceInfo = android.os.Build.MODEL ?: "Android Device",
                success = ok
            )
        )

        if (!ok) {
            return Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
        }

        val updated = user.copy(lastLoginAt = System.currentTimeMillis())
        db.userDao().updateUser(updated)
        context.sessionDataStore.edit { it[KEY_LOGGED_USER_ID] = user.id }
        return Result.success(updated)
    }

    suspend fun logout() {
        context.sessionDataStore.edit { it.remove(KEY_LOGGED_USER_ID) }
    }

    suspend fun changePassword(userId: String, currentPass: String, newPass: String): Result<Unit> {
        if (newPass.length < 6) {
            return Result.failure(IllegalArgumentException("New password must be at least 6 characters."))
        }
        val user = db.userDao().getById(userId)
            ?: return Result.failure(IllegalStateException("User not found."))
        if (!SecurityUtils.verifyPassword(currentPass, user.salt, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("Current password is incorrect."))
        }
        val newSalt = SecurityUtils.generateSalt()
        val newHash = SecurityUtils.hashPassword(newPass, newSalt)
        db.userDao().updateUser(user.copy(salt = newSalt, passwordHash = newHash))
        return Result.success(Unit)
    }

    suspend fun resetPasswordByUsername(username: String, recoveryCode: String, newPassword: String): Result<Unit> {
        if (recoveryCode.trim() != "SERPLIVE360") {
            return Result.failure(IllegalArgumentException("Invalid company recovery code (Hint: SERPLIVE360)."))
        }
        if (newPassword.length < 6) {
            return Result.failure(IllegalArgumentException("New password must be at least 6 characters."))
        }
        val user = db.userDao().getByUsername(username.trim().lowercase())
            ?: return Result.failure(IllegalArgumentException("Username not found."))
        val newSalt = SecurityUtils.generateSalt()
        val newHash = SecurityUtils.hashPassword(newPassword, newSalt)
        db.userDao().updateUser(user.copy(salt = newSalt, passwordHash = newHash))
        return Result.success(Unit)
    }

    suspend fun toggleUserAccountStatus(targetUser: UserEntity, adminName: String) {
        val nextStatus = if (targetUser.accountStatus == "ACTIVE") "SUSPENDED" else "ACTIVE"
        db.userDao().updateUser(targetUser.copy(accountStatus = nextStatus))
        notify("ALL", "Account Status Updated", "${targetUser.fullName} set to $nextStatus by $adminName.", "REPORT")
    }

    suspend fun sweepOverdueTasks() {
        val now = System.currentTimeMillis()
        val tasks = db.taskDao().observeAllTasks().first()
        tasks.forEach { task ->
            val st = TaskStatus.fromString(task.status)
            if (st != TaskStatus.COMPLETED && st != TaskStatus.APPROVED && st != TaskStatus.OVERDUE && task.deadlineMillis < now) {
                db.taskDao().updateTask(task.copy(status = TaskStatus.OVERDUE.name, reviewStatus = "ACTION_NEEDED"))
                notify(
                    "ALL",
                    "Task Overdue: ${task.taskCode}",
                    "${task.title} assigned to ${task.assignedToNamesCsv} missed deadline. Delay reason required.",
                    "TASK"
                )
            }
        }
    }

    suspend fun createTask(
        title: String,
        description: String,
        instructions: String,
        channel: ChannelEntity,
        category: String,
        assignees: List<UserEntity>,
        createdBy: UserEntity,
        priority: String,
        deadlineMillis: Long,
        expectedMinutes: Int,
        complexityWeight: Int,
        attachedFileName: String
    ) {
        val count = db.taskDao().observeAllTasks().first().size + 101
        val code = "TSK-$count"
        val idsCsv = assignees.joinToString(",") { it.id }
        val namesCsv = assignees.joinToString(", ") { it.fullName }
        db.taskDao().insertTask(
            TaskEntity(
                taskCode = code,
                title = title,
                description = description,
                instructions = instructions,
                channelId = channel.id,
                channelName = channel.name,
                category = category,
                assignedToIdsCsv = idsCsv,
                assignedToNamesCsv = namesCsv,
                createdById = createdBy.id,
                priority = priority,
                status = TaskStatus.ASSIGNED.name,
                reviewStatus = "PENDING",
                startDateMillis = System.currentTimeMillis(),
                deadlineMillis = deadlineMillis,
                expectedMinutes = expectedMinutes,
                complexityWeight = complexityWeight.coerceIn(1, 5),
                attachedFileName = attachedFileName
            )
        )
        assignees.forEach { user ->
            notify(
                user.id,
                "New Task Assigned: $code",
                "$title (${channel.name}) — Priority: $priority",
                "TASK"
            )
        }
    }

    suspend fun updateTaskStatus(
        task: TaskEntity,
        newStatus: TaskStatus,
        submissionNote: String = task.submissionNote,
        submissionFile: String = task.submissionFileName,
        delayReason: String = task.delayReason,
        managerComments: String = task.managerComments,
        revisedDeadlineMillis: Long? = task.revisedDeadlineMillis
    ) {
        val now = System.currentTimeMillis()
        val completedAt = if (newStatus == TaskStatus.COMPLETED || newStatus == TaskStatus.APPROVED) now else task.completedAtMillis
        val revMet = if (revisedDeadlineMillis != null && completedAt != null) {
            completedAt <= revisedDeadlineMillis
        } else {
            task.revisedDeadlineMet
        }
        val reviewStatus = when (newStatus) {
            TaskStatus.SUBMITTED_FOR_REVIEW -> "AWAITING_APPROVAL"
            TaskStatus.REVISION_REQUIRED -> "REVISION_REQUIRED"
            TaskStatus.APPROVED, TaskStatus.COMPLETED -> "APPROVED"
            TaskStatus.OVERDUE -> "ACTION_NEEDED"
            else -> task.reviewStatus
        }
        db.taskDao().updateTask(
            task.copy(
                status = newStatus.name,
                reviewStatus = reviewStatus,
                submissionNote = submissionNote,
                submissionFileName = submissionFile,
                delayReason = delayReason,
                managerComments = managerComments,
                revisedDeadlineMillis = revisedDeadlineMillis,
                revisedDeadlineMet = revMet,
                completedAtMillis = completedAt
            )
        )
        notify(
            "ALL",
            "Task Updated: ${task.taskCode}",
            "${task.title} moved to ${newStatus.label}.",
            "TASK"
        )
    }

    suspend fun performOfficeCheckIn(
        user: UserEntity,
        method: String,
        lat: Double,
        lng: Double,
        wifiSsid: String,
        qrTokenInput: String?
    ): Result<AttendanceEntity> {
        val config = db.attendanceDao().getOfficeConfig()
            ?: return Result.failure(IllegalStateException("Office geofence configuration missing."))

        val now = System.currentTimeMillis()
        val today = GeofenceHelper.todayDateKey(now)
        val existingToday = db.attendanceDao().observeAllAttendance().first()
            .firstOrNull { it.userId == user.id && it.dateKey == today }

        if (existingToday?.checkInTimeMillis != null) {
            return Result.failure(IllegalStateException("Already checked in today at ${GeofenceHelper.formatTime(existingToday.checkInTimeMillis)}."))
        }

        val distance = GeofenceHelper.calculateDistanceMeters(lat, lng, config.officeLat, config.officeLng)
        val withinGeofence = distance <= config.geofenceRadiusMeters
        val wifiMatched = wifiSsid.equals(config.authorizedWifiSsid, ignoreCase = true)
        val expectedQr = GeofenceHelper.generateDynamicQrToken(now)
        val qrValid = qrTokenInput != null && qrTokenInput.trim().equals(expectedQr, ignoreCase = true)

        val authorized = when (method) {
            "DYNAMIC_QR" -> qrValid && (withinGeofence || wifiMatched)
            "WIFI_SSID" -> wifiMatched || withinGeofence
            else -> withinGeofence
        }

        if (!authorized) {
            db.attendanceDao().insertAudit(
                AttendanceAuditEntity(
                    targetUserName = user.fullName,
                    action = "DENIED_CHECK_IN",
                    performedBy = "Geofence Guard",
                    details = "Blocked check-in (${distance.roundToInt()}m from HQ, Wi-Fi: $wifiSsid, Method: $method)."
                )
            )
            return Result.failure(
                SecurityException("Attendance cannot be marked because you are outside the authorized office location.")
            )
        }

        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val shiftStartMins = config.workStartHour * 60 + config.workStartMinute
        val currentMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val isLate = currentMins > shiftStartMins + 15

        val entity = AttendanceEntity(
            userId = user.id,
            userName = user.fullName,
            dateKey = today,
            checkInTimeMillis = now,
            verificationMethod = method,
            latitude = lat,
            longitude = lng,
            distanceMeters = distance,
            wifiSsid = wifiSsid,
            lateArrival = isLate,
            status = if (isLate) "LATE" else "PRESENT"
        )
        db.attendanceDao().insertAttendance(entity)
        if (isLate) {
            notify("usr_sandeep", "Late Arrival: ${user.fullName}", "Checked in at ${GeofenceHelper.formatTime(now)} via $method.", "ATTENDANCE")
        }
        return Result.success(entity)
    }

    suspend fun performOfficeCheckOut(
        user: UserEntity,
        lat: Double,
        lng: Double
    ): Result<AttendanceEntity> {
        val config = db.attendanceDao().getOfficeConfig()
            ?: return Result.failure(IllegalStateException("Office geofence configuration missing."))

        val now = System.currentTimeMillis()
        val today = GeofenceHelper.todayDateKey(now)
        val existing = db.attendanceDao().observeAllAttendance().first()
            .firstOrNull { it.userId == user.id && it.dateKey == today }
            ?: return Result.failure(IllegalStateException("No check-in found for today."))

        if (existing.checkOutTimeMillis != null) {
            return Result.failure(IllegalStateException("Already checked out today."))
        }

        val distance = GeofenceHelper.calculateDistanceMeters(lat, lng, config.officeLat, config.officeLng)
        if (distance > config.geofenceRadiusMeters) {
            return Result.failure(
                SecurityException("Attendance cannot be marked because you are outside the authorized office location.")
            )
        }

        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val shiftEndMins = config.workEndHour * 60 + config.workEndMinute
        val currentMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val isEarly = currentMins < shiftEndMins - 15
        val workedMins = (((now - (existing.checkInTimeMillis ?: now)) / 60000L).toInt()).coerceAtLeast(1)

        val updated = existing.copy(
            checkOutTimeMillis = now,
            earlyDeparture = isEarly,
            workingMinutes = workedMins
        )
        db.attendanceDao().updateAttendance(updated)
        return Result.success(updated)
    }

    suspend fun requestAttendanceCorrection(user: UserEntity, reason: String) {
        val today = GeofenceHelper.todayDateKey()
        val existing = db.attendanceDao().observeAllAttendance().first()
            .firstOrNull { it.userId == user.id && it.dateKey == today }

        if (existing != null) {
            db.attendanceDao().updateAttendance(
                existing.copy(correctionRequested = true, correctionReason = reason, status = "CORRECTION_PENDING")
            )
        } else {
            db.attendanceDao().insertAttendance(
                AttendanceEntity(
                    userId = user.id,
                    userName = user.fullName,
                    dateKey = today,
                    verificationMethod = "MANUAL_CORRECTION",
                    status = "CORRECTION_PENDING",
                    correctionRequested = true,
                    correctionReason = reason
                )
            )
        }
        db.attendanceDao().insertAudit(
            AttendanceAuditEntity(
                targetUserName = user.fullName,
                action = "CORRECTION_REQUESTED",
                performedBy = user.fullName,
                details = reason
            )
        )
        notify("usr_sandeep", "Attendance Correction Request", "${user.fullName}: $reason", "ATTENDANCE")
    }

    suspend fun approveAttendanceCorrection(record: AttendanceEntity, approverName: String, approve: Boolean) {
        val nextStatus = if (approve) "PRESENT" else "ABSENT"
        val now = System.currentTimeMillis()
        db.attendanceDao().updateAttendance(
            record.copy(
                status = nextStatus,
                correctionRequested = false,
                correctionApprovedBy = approverName,
                checkInTimeMillis = if (approve && record.checkInTimeMillis == null) now else record.checkInTimeMillis,
                workingMinutes = if (approve && record.workingMinutes == 0) 480 else record.workingMinutes
            )
        )
        db.attendanceDao().insertAudit(
            AttendanceAuditEntity(
                targetUserName = record.userName,
                action = if (approve) "CORRECTION_APPROVED" else "CORRECTION_REJECTED",
                performedBy = approverName,
                details = "Reason: ${record.correctionReason}"
            )
        )
    }

    suspend fun updateOfficeConfig(
        officeName: String,
        lat: Double,
        lng: Double,
        radiusMeters: Float,
        wifiSsid: String,
        adminName: String
    ) {
        val current = db.attendanceDao().getOfficeConfig()
        val now = System.currentTimeMillis()
        db.attendanceDao().upsertOfficeConfig(
            (current ?: OfficeConfigEntity(
                officeName = officeName,
                officeLat = lat,
                officeLng = lng,
                geofenceRadiusMeters = radiusMeters,
                authorizedWifiSsid = wifiSsid,
                workStartHour = 10,
                workStartMinute = 0,
                workEndHour = 19,
                workEndMinute = 0,
                currentQrToken = GeofenceHelper.generateDynamicQrToken(now),
                qrUpdatedAt = now
            )).copy(
                officeName = officeName,
                officeLat = lat,
                officeLng = lng,
                geofenceRadiusMeters = radiusMeters,
                authorizedWifiSsid = wifiSsid,
                currentQrToken = GeofenceHelper.generateDynamicQrToken(now),
                qrUpdatedAt = now
            )
        )
        db.attendanceDao().insertAudit(
            AttendanceAuditEntity(
                targetUserName = "Office Geofence",
                action = "CONFIG_UPDATED",
                performedBy = adminName,
                details = "Lat: $lat, Lng: $lng, Radius: ${radiusMeters.roundToInt()}m, Wi-Fi: $wifiSsid"
            )
        )
    }

    suspend fun saveScript(
        existing: ScriptEntity?,
        channel: ChannelEntity,
        videoTitle: String,
        writer: UserEntity,
        anchorName: String,
        editorName: String,
        contentBody: String,
        attachmentName: String,
        changeNote: String,
        status: ScriptStatus
    ) {
        val now = System.currentTimeMillis()
        if (existing == null) {
            val count = db.scriptDao().observeAllScripts().first().size + 201
            val newId = db.scriptDao().insertScript(
                ScriptEntity(
                    scriptCode = "SCR-$count",
                    channelId = channel.id,
                    channelName = channel.name,
                    videoTitle = videoTitle,
                    writerId = writer.id,
                    writerName = writer.fullName,
                    anchorName = anchorName,
                    editorName = editorName,
                    deadlineMillis = now + 2 * 86400_000L,
                    status = status.name,
                    currentVersion = 1,
                    contentBody = contentBody,
                    attachmentName = attachmentName,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            )
            db.scriptDao().insertVersion(
                ScriptVersionEntity(
                    scriptId = newId,
                    versionNumber = 1,
                    contentSnapshot = contentBody,
                    editedBy = writer.fullName,
                    changeNote = changeNote.ifBlank { "Initial script creation" },
                    createdAtMillis = now
                )
            )
        } else {
            val nextVer = existing.currentVersion + 1
            db.scriptDao().updateScript(
                existing.copy(
                    videoTitle = videoTitle,
                    anchorName = anchorName,
                    editorName = editorName,
                    status = status.name,
                    currentVersion = nextVer,
                    contentBody = contentBody,
                    attachmentName = attachmentName,
                    updatedAtMillis = now
                )
            )
            db.scriptDao().insertVersion(
                ScriptVersionEntity(
                    scriptId = existing.id,
                    versionNumber = nextVer,
                    contentSnapshot = contentBody,
                    editedBy = writer.fullName,
                    changeNote = changeNote.ifBlank { "Script updated" },
                    createdAtMillis = now
                )
            )
        }
    }

    suspend fun reviewScript(
        script: ScriptEntity,
        nextStatus: ScriptStatus,
        comments: String,
        highlightedErrors: String,
        factChecked: Boolean,
        reviewerName: String
    ) {
        db.scriptDao().updateScript(
            script.copy(
                status = nextStatus.name,
                reviewerComments = comments,
                highlightedErrors = highlightedErrors,
                factCheckVerified = factChecked,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
        val notifTitle = when (nextStatus) {
            ScriptStatus.APPROVED, ScriptStatus.SHOOTING_READY -> "Script Approved: ${script.scriptCode}"
            ScriptStatus.REVISION_REQUIRED -> "Script Revision Required: ${script.scriptCode}"
            else -> "Script Review Update: ${script.scriptCode}"
        }
        notify(
            script.writerId,
            notifTitle,
            "${script.videoTitle} reviewed by $reviewerName. ${if (highlightedErrors.isNotBlank()) "Errors flagged: $highlightedErrors" else comments}",
            "SCRIPT"
        )
        if (nextStatus == ScriptStatus.REVISION_REQUIRED) {
            notify("ALL", notifTitle, "Coordinator & writer alerted on ${script.channelName}: $highlightedErrors", "SCRIPT")
        }
    }

    suspend fun addCalendarEntryWithConflictCheck(entry: CalendarEntryEntity): String? {
        val existing = db.calendarShootingDao().observeCalendarEntries().first()
        val conflict = existing.firstOrNull {
            it.shootingDateKey == entry.shootingDateKey &&
                it.shootingTimeSlot == entry.shootingTimeSlot &&
                (it.anchorName.equals(entry.anchorName, ignoreCase = true) ||
                    it.studioRoom.equals(entry.studioRoom, ignoreCase = true))
        }
        db.calendarShootingDao().insertCalendarEntry(entry)
        return if (conflict != null) {
            "Warning: Conflict detected with '${conflict.contentTitle}' (${conflict.anchorName} / ${conflict.studioRoom}) on ${entry.shootingDateKey}."
        } else {
            null
        }
    }

    suspend fun updateCalendarEntry(entry: CalendarEntryEntity) {
        db.calendarShootingDao().updateCalendarEntry(entry)
    }

    suspend fun addShootingSchedule(schedule: ShootingScheduleEntity): String? {
        val existing = db.calendarShootingDao().observeShootingSchedules().first()
        val conflict = existing.firstOrNull {
            it.shootDateKey == schedule.shootDateKey &&
                it.startTime == schedule.startTime &&
                (it.talentName.equals(schedule.talentName, ignoreCase = true) ||
                    it.studioRoom.equals(schedule.studioRoom, ignoreCase = true))
        }
        db.calendarShootingDao().insertShootingSchedule(schedule)
        notify(
            schedule.talentId,
            "Shooting Scheduled: ${schedule.topicOrKathaName}",
            "${schedule.shootDateKey} at ${schedule.startTime} (${schedule.studioRoom})",
            "SHOOT"
        )
        return if (conflict != null) {
            "Schedule conflict: ${conflict.talentName} / ${conflict.studioRoom} already booked at ${conflict.startTime}."
        } else null
    }

    suspend fun updateShootingSchedule(schedule: ShootingScheduleEntity) {
        db.calendarShootingDao().updateShootingSchedule(schedule)
    }

    suspend fun startAiAssignment(assignment: AiAssignmentEntity) {
        db.opsDao().updateAiAssignment(
            assignment.copy(
                status = "IN_PROGRESS",
                startedAtMillis = assignment.startedAtMillis ?: System.currentTimeMillis()
            )
        )
    }

    suspend fun submitAiAssignment(
        assignment: AiAssignmentEntity,
        promptText: String,
        attachedAssetName: String
    ) {
        val now = System.currentTimeMillis()
        val started = assignment.startedAtMillis ?: assignment.assignedAtMillis
        val mins = (((now - started) / 60000L).toInt()).coerceAtLeast(5)
        db.opsDao().updateAiAssignment(
            assignment.copy(
                generatedPromptText = promptText,
                attachedAssetName = attachedAssetName,
                submittedAtMillis = now,
                timeTakenMinutes = mins,
                status = "SUBMITTED"
            )
        )
        notify("usr_sandeep", "AI Prompt Submitted: ${assignment.assignmentCode}", "${assignment.title} delivered in $mins mins.", "TASK")
    }

    suspend fun createAiAssignment(
        channel: ChannelEntity,
        promptType: String,
        title: String,
        brief: String
    ) {
        val count = db.opsDao().observeAiAssignments().first().size + 301
        val now = System.currentTimeMillis()
        db.opsDao().insertAiAssignment(
            AiAssignmentEntity(
                assignmentCode = "AI-$count",
                channelId = channel.id,
                channelName = channel.name,
                promptType = promptType,
                title = title,
                brief = brief,
                deadlineMillis = now + 86400_000L,
                assignedAtMillis = now,
                status = "ASSIGNED"
            )
        )
        notify("usr_manoj", "New AI Prompt Assignment: AI-$count", "$title (${channel.name})", "TASK")
    }

    suspend fun uploadStudioFile(file: StudioFileEntity) {
        db.opsDao().insertStudioFile(file)
        notify("ALL", "New File Uploaded", "${file.fileName} added to ${file.channelName} -> ${file.projectName}.", "FILE")
    }

    suspend fun updateStudioFileStatus(file: StudioFileEntity, status: String) {
        db.opsDao().updateStudioFile(file.copy(approvalStatus = status))
    }

    suspend fun saveSocialPost(post: SocialPostEntity) {
        if (post.id == 0L) {
            db.opsDao().insertSocialPost(post)
        } else {
            db.opsDao().updateSocialPost(post)
        }
    }

    suspend fun addChannelWorkspaceItem(item: ChannelWorkspaceItemEntity) {
        db.channelDao().insertWorkspaceItem(item)
    }

    suspend fun toggleChannelWorkspaceItem(item: ChannelWorkspaceItemEntity) {
        db.channelDao().updateWorkspaceItem(item.copy(isCompleted = !item.isCompleted, updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun addFinancialRecord(record: FinancialRecordEntity) {
        db.opsDao().insertFinancialRecord(record)
    }

    suspend fun markNotificationRead(id: Long) {
        db.opsDao().markNotificationRead(id)
    }

    suspend fun markAllNotificationsRead(userId: String) {
        db.opsDao().markAllReadForUser(userId)
    }

    suspend fun notify(recipientUserId: String, title: String, message: String, category: String) {
        db.opsDao().insertNotification(
            NotificationEntity(
                recipientUserId = recipientUserId,
                title = title,
                message = message,
                category = category
            )
        )
    }

    fun computePerformanceSummaries(
        users: List<UserEntity>,
        tasks: List<TaskEntity>,
        attendance: List<AttendanceEntity>
    ): List<PerformanceSummary> {
        return users.filter {
            val r = UserRole.fromString(it.role)
            !r.isHigherAuthority && !r.isExternal
        }.map { user ->
            val userTasks = tasks.filter { it.assignedToIdsCsv.split(",").contains(user.id) }
            val completedTasks = userTasks.filter {
                val st = TaskStatus.fromString(it.status)
                st == TaskStatus.COMPLETED || st == TaskStatus.APPROVED
            }
            val onTimeCount = completedTasks.count { t ->
                val doneAt = t.completedAtMillis ?: Long.MAX_VALUE
                doneAt <= t.deadlineMillis || t.revisedDeadlineMet == true
            }
            val delayedCount = userTasks.count {
                TaskStatus.fromString(it.status) == TaskStatus.OVERDUE || it.delayReason.isNotBlank()
            }
            val awaitingCount = userTasks.count {
                TaskStatus.fromString(it.status) == TaskStatus.SUBMITTED_FOR_REVIEW
            }
            val revisionsCount = userTasks.count {
                TaskStatus.fromString(it.status) == TaskStatus.REVISION_REQUIRED
            }
            val avgMins = if (completedTasks.isNotEmpty()) {
                completedTasks.map { if (it.actualMinutes > 0) it.actualMinutes else it.expectedMinutes }.average().roundToInt()
            } else 0

            val userAtt = attendance.filter { it.userId == user.id }
            val attPct = if (userAtt.isEmpty()) 92 else {
                val present = userAtt.count { it.status == "PRESENT" || it.status == "LATE" }
                ((present * 100f) / userAtt.size).roundToInt().coerceIn(0, 100)
            }

            // Explainable multi-factor score:
            // 35% On-time delivery + 25% Low-revision quality + 20% Complexity-weighted completion + 20% Attendance
            val onTimeRatio = if (userTasks.isEmpty()) 0.85f else (onTimeCount.toFloat() / userTasks.size.coerceAtLeast(1)).coerceIn(0f, 1f)
            val qualityRatio = if (userTasks.isEmpty()) 0.9f else (1f - (revisionsCount * 0.2f)).coerceIn(0.3f, 1f)
            val complexityAvg = if (completedTasks.isEmpty()) 0.7f else (completedTasks.map { it.complexityWeight }.average().toFloat() / 4f).coerceIn(0.4f, 1f)
            val attRatio = attPct / 100f

            val score = ((onTimeRatio * 35f) + (qualityRatio * 25f) + (complexityAvg * 20f) + (attRatio * 20f))
                .roundToInt().coerceIn(45, 99)

            val breakdown = "On-time ${(onTimeRatio * 100).roundToInt()}% (35%) • Quality ${(qualityRatio * 100).roundToInt()}% (25%) • Complexity (20%) • Attendance $attPct% (20%)"

            PerformanceSummary(
                userId = user.id,
                fullName = user.fullName,
                designation = user.designation,
                totalAssigned = userTasks.size,
                completed = completedTasks.size,
                onTime = onTimeCount,
                delayed = delayedCount,
                awaitingApproval = awaitingCount,
                revisionsCount = revisionsCount,
                avgCompletionMinutes = avgMins,
                attendancePercent = attPct,
                explainableScore = score,
                scoreBreakdown = breakdown
            )
        }
    }
}
