package com.example.firebase

import android.content.Context
import android.os.Build
import com.example.R
import com.example.data.local.*
import com.example.data.model.LoginPortal
import com.example.data.model.ScriptStatus
import com.example.data.model.TaskStatus
import com.example.util.GeofenceHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import kotlin.math.roundToInt

class FirestoreWorkspaceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private inline fun <reified D : Any, T> observeSubcollection(
        uid: String,
        subcollection: String,
        crossinline mapper: (D) -> T
    ): Flow<List<T>> {
        val path = "users/$uid/$subcollection"
        return userDoc(uid).collection(subcollection)
            .snapshots()
            .map { snap ->
                snap.toObjects(D::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .map(mapper)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exToLog = firestoreError ?: (error as? Exception)
                if (exToLog != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(exToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeWorkspaceUser(uid: String): Flow<WorkspaceUserDoc?> =
        userDoc(uid).snapshots()
            .map { it.toObject(WorkspaceUserDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exToLog = firestoreError ?: (error as? Exception)
                if (exToLog != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(exToLog, OperationType.GET, "users/$uid")
                }
                throw error
            }

    fun observeMembers(uid: String): Flow<List<UserEntity>> =
        observeSubcollection<StudioMemberDoc, UserEntity>(uid, "studio_members") { it.toEntity() }

    fun observeChannels(uid: String): Flow<List<ChannelEntity>> =
        observeSubcollection<ChannelDoc, ChannelEntity>(uid, "channels") { it.toEntity() }

    fun observeWorkspaceItems(uid: String): Flow<List<ChannelWorkspaceItemEntity>> =
        observeSubcollection<WorkspaceItemDoc, ChannelWorkspaceItemEntity>(uid, "workspace_items") { it.toEntity() }

    fun observeTasks(uid: String): Flow<List<TaskEntity>> =
        observeSubcollection<TaskDoc, TaskEntity>(uid, "tasks") { it.toEntity() }
            .map { list -> list.sortedBy { it.deadlineMillis } }

    fun observeAttendance(uid: String): Flow<List<AttendanceEntity>> =
        observeSubcollection<AttendanceDoc, AttendanceEntity>(uid, "attendance") { it.toEntity() }
            .map { list -> list.sortedByDescending { it.checkInTimeMillis ?: 0L } }

    fun observeOfficeConfig(uid: String): Flow<OfficeConfigEntity?> =
        observeSubcollection<OfficeConfigDoc, OfficeConfigEntity>(uid, "office_configs") { it.toEntity() }
            .map { it.firstOrNull() }

    fun observeAttendanceAudits(uid: String): Flow<List<AttendanceAuditEntity>> =
        observeSubcollection<AttendanceAuditDoc, AttendanceAuditEntity>(uid, "attendance_audits") { it.toEntity() }
            .map { list -> list.sortedByDescending { it.timestamp } }

    fun observeScripts(uid: String): Flow<List<ScriptEntity>> =
        observeSubcollection<ScriptDoc, ScriptEntity>(uid, "scripts") { it.toEntity() }
            .map { list -> list.sortedByDescending { it.updatedAtMillis } }

    fun observeScriptVersions(uid: String): Flow<List<ScriptVersionEntity>> =
        observeSubcollection<ScriptVersionDoc, ScriptVersionEntity>(uid, "script_versions") { it.toEntity() }
            .map { list -> list.sortedByDescending { it.versionNumber } }

    fun observeCalendarEntries(uid: String): Flow<List<CalendarEntryEntity>> =
        observeSubcollection<CalendarEntryDoc, CalendarEntryEntity>(uid, "calendar_entries") { it.toEntity() }

    fun observeShootingSchedules(uid: String): Flow<List<ShootingScheduleEntity>> =
        observeSubcollection<ShootingScheduleDoc, ShootingScheduleEntity>(uid, "shooting_schedules") { it.toEntity() }

    fun observeAiAssignments(uid: String): Flow<List<AiAssignmentEntity>> =
        observeSubcollection<AiAssignmentDoc, AiAssignmentEntity>(uid, "ai_assignments") { it.toEntity() }
            .map { list -> list.sortedByDescending { it.assignedAtMillis } }

    fun observeStudioFiles(uid: String): Flow<List<StudioFileEntity>> =
        observeSubcollection<StudioFileDoc, StudioFileEntity>(uid, "studio_files") { it.toEntity() }
            .map { list -> list.sortedByDescending { it.uploadDateMillis } }

    fun observeSocialPosts(uid: String): Flow<List<SocialPostEntity>> =
        observeSubcollection<SocialPostDoc, SocialPostEntity>(uid, "social_posts") { it.toEntity() }

    fun observeFinancialRecords(uid: String): Flow<List<FinancialRecordEntity>> =
        observeSubcollection<FinancialRecordDoc, FinancialRecordEntity>(uid, "financial_records") { it.toEntity() }

    fun observeNotifications(uid: String): Flow<List<NotificationEntity>> =
        observeSubcollection<NotificationDoc, NotificationEntity>(uid, "notifications") { it.toEntity() }
            .map { list -> list.sortedByDescending { it.createdAtMillis } }

    fun observeLoginHistory(uid: String): Flow<List<LoginHistoryEntity>> =
        observeSubcollection<LoginHistoryDoc, LoginHistoryEntity>(uid, "login_history") { it.toEntity() }
            .map { list -> list.sortedByDescending { it.timestamp }.take(25) }

    suspend fun initializeWorkspace(uid: String, email: String?, displayName: String?): Result<Unit> =
        FirestoreWorkspaceSeeder.ensureSeeded(db, uid, email, displayName)

    private suspend inline fun executeWrite(
        op: OperationType,
        path: String,
        crossinline block: suspend (uid: String) -> Unit
    ): Result<Unit> {
        return try {
            val uid = requireUid()
            block(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, op, path)
            Result.failure(e)
        }
    }

    private fun buildCreatePayload(uid: String, fields: Map<String, Any?>): Map<String, Any> {
        val out = HashMap<String, Any>()
        fields.forEach { (k, v) -> if (v != null) out[k] = v }
        out["userId"] = uid
        out["createdAt"] = FieldValue.serverTimestamp()
        out["updatedAt"] = FieldValue.serverTimestamp()
        return out
    }

    private fun buildUpdatePayload(fields: Map<String, Any?>): Map<String, Any> {
        val out = HashMap<String, Any>()
        fields.forEach { (k, v) -> if (v != null) out[k] = v }
        out["updatedAt"] = FieldValue.serverTimestamp()
        return out
    }

    suspend fun selectPersona(
        uid: String,
        member: UserEntity,
        portal: LoginPortal
    ): Result<Unit> = executeWrite(OperationType.UPDATE, "users/$uid") { authUid ->
        check(authUid == uid) { "Workspace UID mismatch." }
        if (member.accountStatus != "ACTIVE") {
            throw IllegalStateException("This account is currently suspended. Contact Sandeep.")
        }
        userDoc(uid).update(
            buildUpdatePayload(
                mapOf(
                    "activePersonaId" to member.id,
                    "activePortal" to portal.name
                )
            )
        ).await()
        val memberRef = userDoc(uid).collection("studio_members").document(member.id)
        memberRef.update(buildUpdatePayload(mapOf("lastLoginAt" to System.currentTimeMillis()))).await()
        val histRef = userDoc(uid).collection("login_history").document()
        histRef.set(
            buildCreatePayload(
                uid,
                mapOf(
                    "memberId" to member.id,
                    "username" to member.username,
                    "portal" to portal.label,
                    "deviceInfo" to (Build.MODEL?.takeIf { it.isNotBlank() } ?: "Android Device").take(150),
                    "success" to true,
                    "timestampMillis" to System.currentTimeMillis()
                )
            )
        ).await()
    }

    suspend fun toggleMemberStatus(uid: String, target: UserEntity, adminName: String): Result<Unit> =
        executeWrite(OperationType.UPDATE, "users/$uid/studio_members/${target.id}") { authUid ->
            check(authUid == uid)
            val nextStatus = if (target.accountStatus == "ACTIVE") "SUSPENDED" else "ACTIVE"
            userDoc(uid).collection("studio_members").document(target.id)
                .update(buildUpdatePayload(mapOf("accountStatus" to nextStatus))).await()
            notify(uid, "ALL", "Account Status Updated", "${target.fullName} set to $nextStatus by $adminName.", "REPORT")
        }

    suspend fun createTask(
        uid: String,
        existingCount: Int,
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
    ): Result<Unit> = executeWrite(OperationType.CREATE, "users/$uid/tasks") { authUid ->
        check(authUid == uid)
        val cleanTitle = title.trim().take(200)
        require(cleanTitle.isNotEmpty()) { "Task title is required." }
        require(assignees.isNotEmpty()) { "At least one assignee is required." }
        val code = "TSK-${existingCount + 101}"
        val docRef = userDoc(uid).collection("tasks").document()
        docRef.set(
            buildCreatePayload(
                uid,
                mapOf(
                    "taskCode" to code,
                    "title" to cleanTitle,
                    "description" to description.trim().take(2000),
                    "instructions" to instructions.trim().take(2000),
                    "channelId" to channel.id,
                    "channelName" to channel.name,
                    "category" to category.take(64),
                    "assignedToIdsCsv" to assignees.joinToString(",") { it.id }.take(500),
                    "assignedToNamesCsv" to assignees.joinToString(", ") { it.fullName }.take(600),
                    "createdById" to createdBy.id,
                    "priority" to priority,
                    "status" to TaskStatus.ASSIGNED.name,
                    "reviewStatus" to "PENDING",
                    "startDateMillis" to System.currentTimeMillis(),
                    "deadlineMillis" to deadlineMillis,
                    "expectedMinutes" to expectedMinutes.coerceIn(0, 100000).toLong(),
                    "actualMinutes" to 0L,
                    "delayReason" to "",
                    "managerComments" to "",
                    "complexityWeight" to complexityWeight.coerceIn(1, 5).toLong(),
                    "attachedFileName" to attachedFileName.trim().take(255),
                    "submissionNote" to "",
                    "submissionFileName" to ""
                )
            )
        ).await()
        assignees.forEach { user ->
            notify(uid, user.id, "New Task Assigned: $code", "$cleanTitle (${channel.name}) — Priority: $priority", "TASK")
        }
    }

    suspend fun updateTaskStatus(
        uid: String,
        task: TaskEntity,
        newStatus: TaskStatus,
        submissionNote: String,
        submissionFile: String,
        delayReason: String,
        managerComments: String,
        revisedDeadlineMillis: Long?
    ): Result<Unit> = executeWrite(OperationType.UPDATE, "users/$uid/tasks/${task.docId}") { authUid ->
        check(authUid == uid)
        require(task.docId.isNotBlank()) { "Task document ID missing." }
        val now = System.currentTimeMillis()
        val completedAt = if (newStatus == TaskStatus.COMPLETED || newStatus == TaskStatus.APPROVED) now else task.completedAtMillis
        val revMet = if (revisedDeadlineMillis != null && completedAt != null) completedAt <= revisedDeadlineMillis else task.revisedDeadlineMet
        val reviewStatus = when (newStatus) {
            TaskStatus.SUBMITTED_FOR_REVIEW -> "AWAITING_APPROVAL"
            TaskStatus.REVISION_REQUIRED -> "REVISION_REQUIRED"
            TaskStatus.APPROVED, TaskStatus.COMPLETED -> "APPROVED"
            TaskStatus.OVERDUE -> "ACTION_NEEDED"
            else -> task.reviewStatus
        }
        userDoc(uid).collection("tasks").document(task.docId).update(
            buildUpdatePayload(
                mapOf(
                    "status" to newStatus.name,
                    "reviewStatus" to reviewStatus,
                    "submissionNote" to submissionNote.take(1500),
                    "submissionFileName" to submissionFile.take(255),
                    "delayReason" to delayReason.take(1000),
                    "managerComments" to managerComments.take(1500),
                    "revisedDeadlineMillis" to revisedDeadlineMillis,
                    "revisedDeadlineMet" to revMet,
                    "completedAtMillis" to completedAt
                )
            )
        ).await()
        notify(uid, "ALL", "Task Updated: ${task.taskCode}", "${task.title} moved to ${newStatus.label}.", "TASK")
    }

    suspend fun performOfficeCheckIn(
        uid: String,
        config: OfficeConfigEntity,
        existingToday: AttendanceEntity?,
        user: UserEntity,
        method: String,
        lat: Double,
        lng: Double,
        wifiSsid: String,
        qrTokenInput: String?
    ): Result<AttendanceEntity> {
        val path = "users/$uid/attendance"
        return try {
            val authUid = requireUid()
            check(authUid == uid)
            val now = System.currentTimeMillis()
            val today = GeofenceHelper.todayDateKey(now)
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
                userDoc(uid).collection("attendance_audits").document().set(
                    buildCreatePayload(
                        uid,
                        mapOf(
                            "targetUserName" to user.fullName,
                            "action" to "DENIED_CHECK_IN",
                            "performedBy" to "Geofence Guard",
                            "details" to "Blocked check-in (${distance.roundToInt()}m from HQ, Wi-Fi: $wifiSsid, Method: $method).",
                            "timestampMillis" to now
                        )
                    )
                ).await()
                return Result.failure(SecurityException("Attendance cannot be marked because you are outside the authorized office location."))
            }

            val cal = Calendar.getInstance().apply { timeInMillis = now }
            val shiftStartMins = config.workStartHour * 60 + config.workStartMinute
            val currentMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val isLate = currentMins > shiftStartMins + 15
            val status = if (isLate) "LATE" else "PRESENT"
            val docRef = userDoc(uid).collection("attendance").document()
            docRef.set(
                buildCreatePayload(
                    uid,
                    mapOf(
                        "memberId" to user.id,
                        "userName" to user.fullName,
                        "dateKey" to today,
                        "checkInTimeMillis" to now,
                        "verificationMethod" to method,
                        "latitude" to lat,
                        "longitude" to lng,
                        "distanceMeters" to distance.toDouble(),
                        "wifiSsid" to wifiSsid.take(100),
                        "lateArrival" to isLate,
                        "earlyDeparture" to false,
                        "workingMinutes" to 0L,
                        "status" to status,
                        "correctionReason" to "",
                        "correctionRequested" to false,
                        "correctionApprovedBy" to ""
                    )
                )
            ).await()
            if (isLate) {
                notify(uid, "usr_sandeep", "Late Arrival: ${user.fullName}", "Checked in at ${GeofenceHelper.formatTime(now)} via $method.", "ATTENDANCE")
            }
            Result.success(
                AttendanceEntity(
                    id = stableNumericId(docRef.id),
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
                    status = status,
                    docId = docRef.id
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun performOfficeCheckOut(
        uid: String,
        config: OfficeConfigEntity,
        existingToday: AttendanceEntity?,
        lat: Double,
        lng: Double
    ): Result<AttendanceEntity> {
        val path = "users/$uid/attendance/${existingToday?.docId ?: ""}"
        return try {
            val authUid = requireUid()
            check(authUid == uid)
            val existing = existingToday ?: return Result.failure(IllegalStateException("No check-in found for today."))
            if (existing.checkOutTimeMillis != null) {
                return Result.failure(IllegalStateException("Already checked out today."))
            }
            val distance = GeofenceHelper.calculateDistanceMeters(lat, lng, config.officeLat, config.officeLng)
            if (distance > config.geofenceRadiusMeters) {
                return Result.failure(SecurityException("Attendance cannot be marked because you are outside the authorized office location."))
            }
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply { timeInMillis = now }
            val shiftEndMins = config.workEndHour * 60 + config.workEndMinute
            val currentMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val isEarly = currentMins < shiftEndMins - 15
            val workedMins = (((now - (existing.checkInTimeMillis ?: now)) / 60000L).toInt()).coerceIn(1, 2000)
            userDoc(uid).collection("attendance").document(existing.docId).update(
                buildUpdatePayload(
                    mapOf(
                        "checkOutTimeMillis" to now,
                        "earlyDeparture" to isEarly,
                        "workingMinutes" to workedMins.toLong()
                    )
                )
            ).await()
            Result.success(existing.copy(checkOutTimeMillis = now, earlyDeparture = isEarly, workingMinutes = workedMins))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun requestAttendanceCorrection(
        uid: String,
        existingToday: AttendanceEntity?,
        user: UserEntity,
        reason: String
    ): Result<Unit> = executeWrite(OperationType.WRITE, "users/$uid/attendance") { authUid ->
        check(authUid == uid)
        val cleanReason = reason.trim().take(600)
        val today = GeofenceHelper.todayDateKey()
        if (existingToday != null && existingToday.docId.isNotBlank()) {
            userDoc(uid).collection("attendance").document(existingToday.docId).update(
                buildUpdatePayload(
                    mapOf(
                        "correctionRequested" to true,
                        "correctionReason" to cleanReason,
                        "status" to "CORRECTION_PENDING"
                    )
                )
            ).await()
        } else {
            userDoc(uid).collection("attendance").document().set(
                buildCreatePayload(
                    uid,
                    mapOf(
                        "memberId" to user.id,
                        "userName" to user.fullName,
                        "dateKey" to today,
                        "verificationMethod" to "MANUAL_CORRECTION",
                        "latitude" to 0.0,
                        "longitude" to 0.0,
                        "distanceMeters" to 0.0,
                        "wifiSsid" to "",
                        "lateArrival" to false,
                        "earlyDeparture" to false,
                        "workingMinutes" to 0L,
                        "status" to "CORRECTION_PENDING",
                        "correctionReason" to cleanReason,
                        "correctionRequested" to true,
                        "correctionApprovedBy" to ""
                    )
                )
            ).await()
        }
        userDoc(uid).collection("attendance_audits").document().set(
            buildCreatePayload(
                uid,
                mapOf(
                    "targetUserName" to user.fullName,
                    "action" to "CORRECTION_REQUESTED",
                    "performedBy" to user.fullName,
                    "details" to cleanReason,
                    "timestampMillis" to System.currentTimeMillis()
                )
            )
        ).await()
        notify(uid, "usr_sandeep", "Attendance Correction Request", "${user.fullName}: $cleanReason", "ATTENDANCE")
    }

    suspend fun approveAttendanceCorrection(
        uid: String,
        record: AttendanceEntity,
        approverName: String,
        approve: Boolean
    ): Result<Unit> = executeWrite(OperationType.UPDATE, "users/$uid/attendance/${record.docId}") { authUid ->
        check(authUid == uid)
        val now = System.currentTimeMillis()
        val nextStatus = if (approve) "PRESENT" else "ABSENT"
        userDoc(uid).collection("attendance").document(record.docId).update(
            buildUpdatePayload(
                mapOf(
                    "status" to nextStatus,
                    "correctionRequested" to false,
                    "correctionApprovedBy" to approverName.take(100),
                    "checkInTimeMillis" to (if (approve && record.checkInTimeMillis == null) now else record.checkInTimeMillis),
                    "workingMinutes" to (if (approve && record.workingMinutes == 0) 480L else record.workingMinutes.toLong())
                )
            )
        ).await()
        userDoc(uid).collection("attendance_audits").document().set(
            buildCreatePayload(
                uid,
                mapOf(
                    "targetUserName" to record.userName,
                    "action" to if (approve) "CORRECTION_APPROVED" else "CORRECTION_REJECTED",
                    "performedBy" to approverName,
                    "details" to "Reason: ${record.correctionReason}",
                    "timestampMillis" to now
                )
            )
        ).await()
    }

    suspend fun updateOfficeConfig(
        uid: String,
        currentDocId: String,
        officeName: String,
        lat: Double,
        lng: Double,
        radiusMeters: Float,
        wifiSsid: String,
        adminName: String
    ): Result<Unit> = executeWrite(OperationType.UPDATE, "users/$uid/office_configs/$currentDocId") { authUid ->
        check(authUid == uid)
        val now = System.currentTimeMillis()
        val targetId = currentDocId.ifBlank { "hq_main" }
        userDoc(uid).collection("office_configs").document(targetId).update(
            buildUpdatePayload(
                mapOf(
                    "officeName" to officeName.trim().ifBlank { "Serplive Studio HQ" }.take(140),
                    "officeLat" to lat,
                    "officeLng" to lng,
                    "geofenceRadiusMeters" to radiusMeters.coerceIn(10f, 10000f).toDouble(),
                    "authorizedWifiSsid" to wifiSsid.trim().ifBlank { "Serplive_Studio_5G" }.take(100),
                    "currentQrToken" to GeofenceHelper.generateDynamicQrToken(now),
                    "qrUpdatedAt" to now
                )
            )
        ).await()
        userDoc(uid).collection("attendance_audits").document().set(
            buildCreatePayload(
                uid,
                mapOf(
                    "targetUserName" to "Office Geofence",
                    "action" to "CONFIG_UPDATED",
                    "performedBy" to adminName,
                    "details" to "Lat: $lat, Lng: $lng, Radius: ${radiusMeters.roundToInt()}m, Wi-Fi: $wifiSsid",
                    "timestampMillis" to now
                )
            )
        ).await()
    }

    suspend fun saveScript(
        uid: String,
        existingCount: Int,
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
    ): Result<Unit> = executeWrite(OperationType.WRITE, "users/$uid/scripts") { authUid ->
        check(authUid == uid)
        val now = System.currentTimeMillis()
        val cleanTitle = videoTitle.trim().take(220)
        val cleanBody = contentBody.trim().take(25000)
        require(cleanTitle.isNotEmpty() && cleanBody.isNotEmpty())
        if (existing == null || existing.docId.isBlank()) {
            val docRef = userDoc(uid).collection("scripts").document()
            docRef.set(
                buildCreatePayload(
                    uid,
                    mapOf(
                        "scriptCode" to "SCR-${existingCount + 201}",
                        "channelId" to channel.id,
                        "channelName" to channel.name,
                        "videoTitle" to cleanTitle,
                        "writerId" to writer.id,
                        "writerName" to writer.fullName.ifBlank { "Studio Writer" }.take(100),
                        "anchorName" to anchorName.trim().ifBlank { "TBD" }.take(100),
                        "editorName" to editorName.trim().ifBlank { "TBD" }.take(100),
                        "deadlineMillis" to (now + 2 * 86400_000L),
                        "status" to status.name,
                        "currentVersion" to 1L,
                        "contentBody" to cleanBody,
                        "reviewerComments" to "",
                        "highlightedErrors" to "",
                        "factCheckVerified" to false,
                        "attachmentName" to attachmentName.trim().take(255),
                        "createdAtMillis" to now,
                        "updatedAtMillis" to now
                    )
                )
            ).await()
            userDoc(uid).collection("script_versions").document().set(
                buildCreatePayload(
                    uid,
                    mapOf(
                        "scriptDocId" to docRef.id,
                        "versionNumber" to 1L,
                        "contentSnapshot" to cleanBody,
                        "editedBy" to writer.fullName,
                        "changeNote" to changeNote.ifBlank { "Initial script creation" }.take(500),
                        "createdAtMillis" to now
                    )
                )
            ).await()
        } else {
            val nextVer = (existing.currentVersion + 1).toLong()
            userDoc(uid).collection("scripts").document(existing.docId).update(
                buildUpdatePayload(
                    mapOf(
                        "videoTitle" to cleanTitle,
                        "anchorName" to anchorName.trim().ifBlank { "TBD" }.take(100),
                        "editorName" to editorName.trim().ifBlank { "TBD" }.take(100),
                        "status" to status.name,
                        "currentVersion" to nextVer,
                        "contentBody" to cleanBody,
                        "attachmentName" to attachmentName.trim().take(255),
                        "updatedAtMillis" to now
                    )
                )
            ).await()
            userDoc(uid).collection("script_versions").document().set(
                buildCreatePayload(
                    uid,
                    mapOf(
                        "scriptDocId" to existing.docId,
                        "versionNumber" to nextVer,
                        "contentSnapshot" to cleanBody,
                        "editedBy" to writer.fullName,
                        "changeNote" to changeNote.ifBlank { "Script updated" }.take(500),
                        "createdAtMillis" to now
                    )
                )
            ).await()
        }
    }

    suspend fun reviewScript(
        uid: String,
        script: ScriptEntity,
        nextStatus: ScriptStatus,
        comments: String,
        highlightedErrors: String,
        factChecked: Boolean,
        reviewerName: String
    ): Result<Unit> = executeWrite(OperationType.UPDATE, "users/$uid/scripts/${script.docId}") { authUid ->
        check(authUid == uid)
        userDoc(uid).collection("scripts").document(script.docId).update(
            buildUpdatePayload(
                mapOf(
                    "status" to nextStatus.name,
                    "reviewerComments" to comments.trim().take(2000),
                    "highlightedErrors" to highlightedErrors.trim().take(2000),
                    "factCheckVerified" to factChecked,
                    "updatedAtMillis" to System.currentTimeMillis()
                )
            )
        ).await()
        val notifTitle = when (nextStatus) {
            ScriptStatus.APPROVED, ScriptStatus.SHOOTING_READY -> "Script Approved: ${script.scriptCode}"
            ScriptStatus.REVISION_REQUIRED -> "Script Revision Required: ${script.scriptCode}"
            else -> "Script Review Update: ${script.scriptCode}"
        }
        notify(
            uid,
            script.writerId,
            notifTitle,
            "${script.videoTitle} reviewed by $reviewerName. ${if (highlightedErrors.isNotBlank()) "Errors flagged: $highlightedErrors" else comments}",
            "SCRIPT"
        )
    }

    suspend fun addCalendarEntry(uid: String, entry: CalendarEntryEntity): Result<Unit> =
        executeWrite(OperationType.CREATE, "users/$uid/calendar_entries") { authUid ->
            check(authUid == uid)
            userDoc(uid).collection("calendar_entries").document().set(
                buildCreatePayload(
                    uid,
                    mapOf(
                        "channelId" to entry.channelId,
                        "channelName" to entry.channelName,
                        "contentTitle" to entry.contentTitle.trim().ifBlank { "Untitled Content" }.take(200),
                        "category" to entry.category.trim().ifBlank { "Studio Episode" }.take(100),
                        "scriptWriter" to entry.scriptWriter.trim().ifBlank { "Studio Team" }.take(100),
                        "anchorName" to entry.anchorName.trim().ifBlank { "TBD" }.take(100),
                        "editorName" to entry.editorName.trim().ifBlank { "TBD" }.take(100),
                        "shootingDateKey" to entry.shootingDateKey.trim().let { if (it.length < 4) GeofenceHelper.todayDateKey() else it }.take(20),
                        "shootingTimeSlot" to entry.shootingTimeSlot.trim().ifBlank { "10:00 - 12:00" }.take(40),
                        "editingDeadlineKey" to entry.editingDeadlineKey.trim().let { if (it.length < 4) GeofenceHelper.todayDateKey() else it }.take(20),
                        "publishingDateKey" to entry.publishingDateKey.trim().let { if (it.length < 4) GeofenceHelper.todayDateKey() else it }.take(20),
                        "publishingTime" to entry.publishingTime.trim().ifBlank { "06:00 PM" }.take(30),
                        "platform" to entry.platform.trim().ifBlank { "YouTube" }.take(40),
                        "status" to entry.status.trim().ifBlank { "Scheduled" }.take(60),
                        "studioRoom" to entry.studioRoom.trim().ifBlank { "Studio A" }.take(60)
                    )
                )
            ).await()
        }

    suspend fun rescheduleCalendarEntry(uid: String, entry: CalendarEntryEntity, newDateKey: String, newSlot: String): Result<Unit> =
        executeWrite(OperationType.UPDATE, "users/$uid/calendar_entries/${entry.docId}") { authUid ->
            check(authUid == uid)
            userDoc(uid).collection("calendar_entries").document(entry.docId).update(
                buildUpdatePayload(
                    mapOf(
                        "shootingDateKey" to newDateKey.trim().let { if (it.length < 4) entry.shootingDateKey else it }.take(20),
                        "shootingTimeSlot" to newSlot.trim().ifBlank { entry.shootingTimeSlot }.take(40)
                    )
                )
            ).await()
        }

    suspend fun addShootingSchedule(uid: String, schedule: ShootingScheduleEntity): Result<Unit> =
        executeWrite(OperationType.CREATE, "users/$uid/shooting_schedules") { authUid ->
            check(authUid == uid)
            userDoc(uid).collection("shooting_schedules").document().set(
                buildCreatePayload(
                    uid,
                    mapOf(
                        "scheduleMode" to schedule.scheduleMode.ifBlank { "ANCHOR" }.take(40),
                        "channelId" to schedule.channelId,
                        "channelName" to schedule.channelName,
                        "topicOrKathaName" to schedule.topicOrKathaName.trim().ifBlank { "Studio Shoot" }.take(200),
                        "scriptName" to schedule.scriptName.trim().ifBlank { "SCR-NEW" }.take(120),
                        "talentId" to schedule.talentId.ifBlank { "usr_parchi" },
                        "talentName" to schedule.talentName.trim().ifBlank { "Studio Talent" }.take(100),
                        "coordinatorName" to schedule.coordinatorName.trim().ifBlank { "Parchi" }.take(100),
                        "editorName" to schedule.editorName.trim().ifBlank { "Editor 1" }.take(100),
                        "shootDateKey" to schedule.shootDateKey.trim().let { if (it.length < 4) GeofenceHelper.todayDateKey() else it }.take(20),
                        "arrivalTime" to schedule.arrivalTime.trim().ifBlank { "10:30" }.take(30),
                        "startTime" to schedule.startTime.trim().ifBlank { "11:00" }.take(30),
                        "expectedEndTime" to schedule.expectedEndTime.trim().ifBlank { "13:00" }.take(30),
                        "actualEndTime" to schedule.actualEndTime.take(30),
                        "studioRoom" to schedule.studioRoom.trim().ifBlank { "Studio A" }.take(60),
                        "specialRequirements" to schedule.specialRequirements.take(1000),
                        "materialsRequired" to schedule.materialsRequired.take(1000),
                        "materialsAvailable" to schedule.materialsAvailable,
                        "purchaseRequired" to schedule.purchaseRequired.take(1000),
                        "recordingStatus" to schedule.recordingStatus.ifBlank { "SCHEDULED" }.take(40)
                    )
                )
            ).await()
            notify(uid, schedule.talentId, "Shooting Scheduled: ${schedule.topicOrKathaName}", "${schedule.shootDateKey} at ${schedule.startTime} (${schedule.studioRoom})", "SHOOT")
        }

    suspend fun updateShootingStatus(uid: String, schedule: ShootingScheduleEntity, status: String, actualEnd: String): Result<Unit> =
        executeWrite(OperationType.UPDATE, "users/$uid/shooting_schedules/${schedule.docId}") { authUid ->
            check(authUid == uid)
            userDoc(uid).collection("shooting_schedules").document(schedule.docId).update(
                buildUpdatePayload(
                    mapOf(
                        "recordingStatus" to status.take(40),
                        "actualEndTime" to actualEnd.take(30)
                    )
                )
            ).await()
        }

    suspend fun startAiAssignment(uid: String, assignment: AiAssignmentEntity): Result<Unit> =
        executeWrite(OperationType.UPDATE, "users/$uid/ai_assignments/${assignment.docId}") { authUid ->
            check(authUid == uid)
            userDoc(uid).collection("ai_assignments").document(assignment.docId).update(
                buildUpdatePayload(
                    mapOf(
                        "status" to "IN_PROGRESS",
                        "startedAtMillis" to (assignment.startedAtMillis ?: System.currentTimeMillis())
                    )
                )
            ).await()
        }

    suspend fun submitAiAssignment(uid: String, assignment: AiAssignmentEntity, promptText: String, attachedAssetName: String): Result<Unit> =
        executeWrite(OperationType.UPDATE, "users/$uid/ai_assignments/${assignment.docId}") { authUid ->
            check(authUid == uid)
            val now = System.currentTimeMillis()
            val started = assignment.startedAtMillis ?: assignment.assignedAtMillis
            val mins = (((now - started) / 60000L).toInt()).coerceIn(5, 100000)
            userDoc(uid).collection("ai_assignments").document(assignment.docId).update(
                buildUpdatePayload(
                    mapOf(
                        "generatedPromptText" to promptText.trim().take(25000),
                        "attachedAssetName" to attachedAssetName.trim().take(255),
                        "submittedAtMillis" to now,
                        "timeTakenMinutes" to mins.toLong(),
                        "status" to "SUBMITTED"
                    )
                )
            ).await()
            notify(uid, "usr_sandeep", "AI Prompt Submitted: ${assignment.assignmentCode}", "${assignment.title} delivered in $mins mins.", "TASK")
        }

    suspend fun createAiAssignment(uid: String, existingCount: Int, channel: ChannelEntity, promptType: String, title: String, brief: String): Result<Unit> =
        executeWrite(OperationType.CREATE, "users/$uid/ai_assignments") { authUid ->
            check(authUid == uid)
            val code = "AI-${existingCount + 301}"
            val now = System.currentTimeMillis()
            userDoc(uid).collection("ai_assignments").document().set(
                buildCreatePayload(
                    uid,
                    mapOf(
                        "assignmentCode" to code,
                        "channelId" to channel.id,
                        "channelName" to channel.name,
                        "promptType" to promptType.take(50),
                        "title" to title.trim().take(200),
                        "brief" to brief.trim().take(2000),
                        "deadlineMillis" to (now + 86400_000L),
                        "assignedAtMillis" to now,
                        "timeTakenMinutes" to 0L,
                        "generatedPromptText" to "",
                        "attachedAssetName" to "",
                        "status" to "ASSIGNED",
                        "reviewerNote" to ""
                    )
                )
            ).await()
            notify(uid, "usr_manoj", "New AI Prompt Assignment: $code", "$title (${channel.name})", "TASK")
        }

    suspend fun uploadStudioFile(uid: String, file: StudioFileEntity): Result<Unit> =
        executeWrite(OperationType.CREATE, "users/$uid/studio_files") { authUid ->
            check(authUid == uid)
            userDoc(uid).collection("studio_files").document().set(
                buildCreatePayload(
                    uid,
                    mapOf(
                        "channelId" to file.channelId,
                        "channelName" to file.channelName,
                        "projectName" to file.projectName.trim().ifBlank { "Studio Batch" }.take(150),
                        "contentTitle" to file.contentTitle.trim().ifBlank { file.fileName.ifBlank { "Studio Asset" } }.take(200),
                        "fileType" to file.fileType.ifBlank { "VIDEO_FILE" }.take(50),
                        "fileName" to file.fileName.trim().ifBlank { "studio_asset" }.take(255),
                        "uploadedBy" to file.uploadedBy.ifBlank { "Studio Team" }.take(100),
                        "uploadedById" to file.uploadedById.ifBlank { "usr_editor1" },
                        "uploadDateMillis" to file.uploadDateMillis,
                        "version" to file.version.trim().ifBlank { "v1.0" }.take(30),
                        "fileSizeLabel" to file.fileSizeLabel.trim().ifBlank { "Ready" }.take(40),
                        "approvalStatus" to file.approvalStatus.ifBlank { "PENDING" }.take(40),
                        "contentUri" to file.contentUri.take(2000)
                    )
                )
            ).await()
            notify(uid, "ALL", "New File Uploaded", "${file.fileName} added to ${file.channelName} -> ${file.projectName}.", "FILE")
        }

    suspend fun updateStudioFileStatus(uid: String, file: StudioFileEntity, status: String): Result<Unit> =
        executeWrite(OperationType.UPDATE, "users/$uid/studio_files/${file.docId}") { authUid ->
            check(authUid == uid)
            userDoc(uid).collection("studio_files").document(file.docId).update(
                buildUpdatePayload(mapOf("approvalStatus" to status.take(40)))
            ).await()
        }

    suspend fun saveSocialPost(uid: String, post: SocialPostEntity): Result<Unit> =
        executeWrite(OperationType.WRITE, "users/$uid/social_posts") { authUid ->
            check(authUid == uid)
            if (post.docId.isBlank()) {
                userDoc(uid).collection("social_posts").document().set(
                    buildCreatePayload(
                        uid,
                        mapOf(
                            "channelId" to post.channelId,
                            "channelName" to post.channelName,
                            "title" to post.title.trim().ifBlank { "Untitled Post" }.take(200),
                            "platform" to post.platform,
                            "videoFileName" to post.videoFileName.take(255),
                            "thumbnailName" to post.thumbnailName.take(255),
                            "caption" to post.caption.take(2200),
                            "description" to post.description.take(5000),
                            "hashtags" to post.hashtags.take(500),
                            "keywords" to post.keywords.take(500),
                            "scriptCode" to post.scriptCode.take(40),
                            "publishDateKey" to post.publishDateKey.trim().let { if (it.length < 4) GeofenceHelper.todayDateKey() else it }.take(20),
                            "publishTime" to post.publishTime.trim().ifBlank { "06:00 PM" }.take(30),
                            "status" to post.status,
                            "publishedUrl" to post.publishedUrl.take(500),
                            "checklistVerified" to post.checklistVerified,
                            "viewsCount" to post.viewsCount.coerceAtLeast(0).toLong()
                        )
                    )
                ).await()
            } else {
                userDoc(uid).collection("social_posts").document(post.docId).update(
                    buildUpdatePayload(
                        mapOf(
                            "title" to post.title.trim().ifBlank { "Untitled Post" }.take(200),
                            "platform" to post.platform,
                            "videoFileName" to post.videoFileName.take(255),
                            "thumbnailName" to post.thumbnailName.take(255),
                            "caption" to post.caption.take(2200),
                            "description" to post.description.take(5000),
                            "hashtags" to post.hashtags.take(500),
                            "keywords" to post.keywords.take(500),
                            "publishDateKey" to post.publishDateKey.trim().let { if (it.length < 4) GeofenceHelper.todayDateKey() else it }.take(20),
                            "publishTime" to post.publishTime.trim().ifBlank { "06:00 PM" }.take(30),
                            "status" to post.status,
                            "publishedUrl" to post.publishedUrl.take(500),
                            "checklistVerified" to post.checklistVerified,
                            "viewsCount" to post.viewsCount.coerceAtLeast(0).toLong()
                        )
                    )
                ).await()
            }
        }

    suspend fun addChannelWorkspaceItem(uid: String, item: ChannelWorkspaceItemEntity): Result<Unit> =
        executeWrite(OperationType.CREATE, "users/$uid/workspace_items") { authUid ->
            check(authUid == uid)
            userDoc(uid).collection("workspace_items").document().set(
                buildCreatePayload(
                    uid,
                    mapOf(
                        "channelId" to item.channelId,
                        "itemType" to item.itemType.ifBlank { "CRAFT_IDEA" }.take(64),
                        "title" to item.title.trim().ifBlank { "Studio Item" }.take(200),
                        "details" to item.details.take(1500),
                        "metaBadge" to item.metaBadge.trim().ifBlank { "Active" }.take(64),
                        "completed" to item.isCompleted,
                        "ownerName" to item.ownerName.trim().ifBlank { "Studio Team" }.take(100),
                        "updatedAtMillis" to System.currentTimeMillis()
                    )
                )
            ).await()
        }

    suspend fun toggleChannelWorkspaceItem(uid: String, item: ChannelWorkspaceItemEntity): Result<Unit> =
        executeWrite(OperationType.UPDATE, "users/$uid/workspace_items/${item.docId}") { authUid ->
            check(authUid == uid)
            userDoc(uid).collection("workspace_items").document(item.docId).update(
                buildUpdatePayload(
                    mapOf(
                        "completed" to !item.isCompleted,
                        "updatedAtMillis" to System.currentTimeMillis()
                    )
                )
            ).await()
        }

    suspend fun addFinancialRecord(uid: String, record: FinancialRecordEntity): Result<Unit> =
        executeWrite(OperationType.CREATE, "users/$uid/financial_records") { authUid ->
            check(authUid == uid)
            userDoc(uid).collection("financial_records").document().set(
                buildCreatePayload(
                    uid,
                    mapOf(
                        "recordType" to record.recordType,
                        "category" to record.category.take(80),
                        "title" to record.title.take(200),
                        "amountInr" to record.amountInr.coerceAtLeast(0.0),
                        "monthKey" to record.monthKey.take(20),
                        "recordDateKey" to record.recordDateKey.take(20),
                        "enteredBy" to record.enteredBy.take(100),
                        "notes" to record.notes.take(1000)
                    )
                )
            ).await()
        }

    suspend fun markNotificationRead(uid: String, notifDocId: String): Result<Unit> =
        executeWrite(OperationType.UPDATE, "users/$uid/notifications/$notifDocId") { authUid ->
            check(authUid == uid)
            userDoc(uid).collection("notifications").document(notifDocId).update(
                buildUpdatePayload(mapOf("read" to true))
            ).await()
        }

    suspend fun markAllNotificationsRead(uid: String, notifications: List<NotificationEntity>, memberId: String): Result<Unit> =
        executeWrite(OperationType.UPDATE, "users/$uid/notifications") { authUid ->
            check(authUid == uid)
            val batch = db.batch()
            notifications.filter { !it.isRead && (it.recipientUserId == "ALL" || it.recipientUserId == memberId) && it.docId.isNotBlank() }
                .forEach { n ->
                    batch.update(userDoc(uid).collection("notifications").document(n.docId), buildUpdatePayload(mapOf("read" to true)))
                }
            batch.commit().await()
        }

    private suspend fun notify(uid: String, recipientUserId: String, title: String, message: String, category: String) {
        userDoc(uid).collection("notifications").document().set(
            buildCreatePayload(
                uid,
                mapOf(
                    "recipientUserId" to recipientUserId.take(64),
                    "title" to title.take(200),
                    "message" to message.take(1000),
                    "category" to category.take(40),
                    "read" to false,
                    "createdAtMillis" to System.currentTimeMillis()
                )
            )
        ).await()
    }
}
