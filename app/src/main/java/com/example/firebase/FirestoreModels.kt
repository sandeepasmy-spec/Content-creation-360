package com.example.firebase

import androidx.annotation.Keep
import com.example.data.local.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.abs

fun stableNumericId(docId: String): Long {
    val h = abs(docId.hashCode().toLong())
    return if (h == 0L) 1L else h
}

@Keep
@IgnoreExtraProperties
data class WorkspaceUserDoc(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val activePersonaId: String = "usr_sandeep",
    val activePortal: String = "HIGHER_AUTHORITY",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "email" to email.take(200),
        "displayName" to displayName.ifBlank { "Studio Member" }.take(120),
        "activePersonaId" to activePersonaId.ifBlank { "usr_sandeep" }.take(64),
        "activePortal" to activePortal.ifBlank { "HIGHER_AUTHORITY" }.take(64),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class StudioMemberDoc(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val username: String = "",
    val fullName: String = "",
    val designation: String = "",
    val department: String = "",
    val role: String = "VIDEO_EDITOR",
    val assignedChannelsCsv: String = "",
    val avatarInitials: String = "",
    val avatarColorHex: Long = 0xFF2563EB,
    val accountStatus: String = "ACTIVE",
    val lastLoginAt: Long = 0L,
    val email: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity() = UserEntity(
        id = docId,
        username = username,
        passwordHash = "",
        salt = "",
        fullName = fullName,
        designation = designation,
        department = department,
        role = role,
        assignedChannelsCsv = assignedChannelsCsv,
        avatarInitials = avatarInitials,
        avatarColorHex = avatarColorHex,
        accountStatus = accountStatus,
        lastLoginAt = lastLoginAt,
        email = email,
        docId = docId
    )
}

@Keep
@IgnoreExtraProperties
data class ChannelDoc(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val name: String = "",
    val categoryTag: String = "",
    val description: String = "",
    val leadCoordinator: String = "",
    val accentColorHex: Long = 0xFF2563EB,
    val monthlyVideoTarget: Long = 10L,
    val videosProduced: Long = 0L,
    val videosPublished: Long = 0L,
    val growthPercent: Double = 0.0,
    val subscribersLabel: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity() = ChannelEntity(
        id = docId,
        name = name,
        categoryTag = categoryTag,
        description = description,
        leadCoordinator = leadCoordinator,
        accentColorHex = accentColorHex,
        monthlyVideoTarget = monthlyVideoTarget.toInt(),
        videosProduced = videosProduced.toInt(),
        videosPublished = videosPublished.toInt(),
        growthPercent = growthPercent.toFloat(),
        subscribersLabel = subscribersLabel,
        docId = docId
    )
}

@Keep
@IgnoreExtraProperties
data class WorkspaceItemDoc(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val channelId: String = "",
    val itemType: String = "",
    val title: String = "",
    val details: String = "",
    val metaBadge: String = "",
    val completed: Boolean = false,
    val ownerName: String = "",
    val updatedAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity() = ChannelWorkspaceItemEntity(
        id = stableNumericId(docId),
        channelId = channelId,
        itemType = itemType,
        title = title,
        details = details,
        metaBadge = metaBadge,
        isCompleted = completed,
        ownerName = ownerName,
        updatedAtMillis = updatedAtMillis,
        docId = docId
    )
}

@Keep
@IgnoreExtraProperties
data class TaskDoc(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val taskCode: String = "",
    val title: String = "",
    val description: String = "",
    val instructions: String = "",
    val channelId: String = "",
    val channelName: String = "",
    val category: String = "",
    val assignedToIdsCsv: String = "",
    val assignedToNamesCsv: String = "",
    val createdById: String = "",
    val priority: String = "MEDIUM",
    val status: String = "ASSIGNED",
    val reviewStatus: String = "PENDING",
    val startDateMillis: Long = 0L,
    val deadlineMillis: Long = 0L,
    val expectedMinutes: Long = 60L,
    val actualMinutes: Long = 0L,
    val completedAtMillis: Long? = null,
    val delayReason: String = "",
    val revisedDeadlineMillis: Long? = null,
    val revisedDeadlineMet: Boolean? = null,
    val managerComments: String = "",
    val complexityWeight: Long = 2L,
    val attachedFileName: String = "",
    val submissionNote: String = "",
    val submissionFileName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity() = TaskEntity(
        id = stableNumericId(docId),
        taskCode = taskCode,
        title = title,
        description = description,
        instructions = instructions,
        channelId = channelId,
        channelName = channelName,
        category = category,
        assignedToIdsCsv = assignedToIdsCsv,
        assignedToNamesCsv = assignedToNamesCsv,
        createdById = createdById,
        priority = priority,
        status = status,
        reviewStatus = reviewStatus,
        startDateMillis = startDateMillis,
        deadlineMillis = deadlineMillis,
        expectedMinutes = expectedMinutes.toInt(),
        actualMinutes = actualMinutes.toInt(),
        completedAtMillis = completedAtMillis,
        delayReason = delayReason,
        revisedDeadlineMillis = revisedDeadlineMillis,
        revisedDeadlineMet = revisedDeadlineMet,
        managerComments = managerComments,
        complexityWeight = complexityWeight.toInt(),
        attachedFileName = attachedFileName,
        submissionNote = submissionNote,
        submissionFileName = submissionFileName,
        docId = docId
    )
}

@Keep
@IgnoreExtraProperties
data class AttendanceDoc(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val memberId: String = "",
    val userName: String = "",
    val dateKey: String = "",
    val checkInTimeMillis: Long? = null,
    val checkOutTimeMillis: Long? = null,
    val verificationMethod: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distanceMeters: Double = 0.0,
    val wifiSsid: String = "",
    val lateArrival: Boolean = false,
    val earlyDeparture: Boolean = false,
    val workingMinutes: Long = 0L,
    val status: String = "PRESENT",
    val correctionReason: String = "",
    val correctionRequested: Boolean = false,
    val correctionApprovedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity() = AttendanceEntity(
        id = stableNumericId(docId),
        userId = memberId,
        userName = userName,
        dateKey = dateKey,
        checkInTimeMillis = checkInTimeMillis,
        checkOutTimeMillis = checkOutTimeMillis,
        verificationMethod = verificationMethod,
        latitude = latitude,
        longitude = longitude,
        distanceMeters = distanceMeters.toFloat(),
        wifiSsid = wifiSsid,
        lateArrival = lateArrival,
        earlyDeparture = earlyDeparture,
        workingMinutes = workingMinutes.toInt(),
        status = status,
        correctionReason = correctionReason,
        correctionRequested = correctionRequested,
        correctionApprovedBy = correctionApprovedBy,
        docId = docId
    )
}

@Keep
@IgnoreExtraProperties
data class OfficeConfigDoc(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val officeName: String = "",
    val officeLat: Double = 28.6139,
    val officeLng: Double = 77.2090,
    val geofenceRadiusMeters: Double = 150.0,
    val authorizedWifiSsid: String = "",
    val workStartHour: Long = 10L,
    val workStartMinute: Long = 0L,
    val workEndHour: Long = 19L,
    val workEndMinute: Long = 0L,
    val currentQrToken: String = "",
    val qrUpdatedAt: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity() = OfficeConfigEntity(
        id = 1,
        officeName = officeName,
        officeLat = officeLat,
        officeLng = officeLng,
        geofenceRadiusMeters = geofenceRadiusMeters.toFloat(),
        authorizedWifiSsid = authorizedWifiSsid,
        workStartHour = workStartHour.toInt(),
        workStartMinute = workStartMinute.toInt(),
        workEndHour = workEndHour.toInt(),
        workEndMinute = workEndMinute.toInt(),
        currentQrToken = currentQrToken,
        qrUpdatedAt = qrUpdatedAt,
        docId = docId
    )
}

@Keep
@IgnoreExtraProperties
data class AttendanceAuditDoc(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val targetUserName: String = "",
    val action: String = "",
    val performedBy: String = "",
    val details: String = "",
    val timestampMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity() = AttendanceAuditEntity(
        id = stableNumericId(docId),
        targetUserName = targetUserName,
        action = action,
        performedBy = performedBy,
        details = details,
        timestamp = timestampMillis,
        docId = docId
    )
}

@Keep
@IgnoreExtraProperties
data class ScriptDoc(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val scriptCode: String = "",
    val channelId: String = "",
    val channelName: String = "",
    val videoTitle: String = "",
    val writerId: String = "",
    val writerName: String = "",
    val anchorName: String = "",
    val editorName: String = "",
    val deadlineMillis: Long = 0L,
    val status: String = "IDEA",
    val currentVersion: Long = 1L,
    val contentBody: String = "",
    val reviewerComments: String = "",
    val highlightedErrors: String = "",
    val factCheckVerified: Boolean = false,
    val attachmentName: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity() = ScriptEntity(
        id = stableNumericId(docId),
        scriptCode = scriptCode,
        channelId = channelId,
        channelName = channelName,
        videoTitle = videoTitle,
        writerId = writerId,
        writerName = writerName,
        anchorName = anchorName,
        editorName = editorName,
        deadlineMillis = deadlineMillis,
        status = status,
        currentVersion = currentVersion.toInt(),
        contentBody = contentBody,
        reviewerComments = reviewerComments,
        highlightedErrors = highlightedErrors,
        factCheckVerified = factCheckVerified,
        attachmentName = attachmentName,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        docId = docId
    )
}

@Keep
@IgnoreExtraProperties
data class ScriptVersionDoc(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val scriptDocId: String = "",
    val versionNumber: Long = 1L,
    val contentSnapshot: String = "",
    val editedBy: String = "",
    val changeNote: String = "",
    val createdAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity() = ScriptVersionEntity(
        id = stableNumericId(docId),
        scriptId = stableNumericId(scriptDocId),
        versionNumber = versionNumber.toInt(),
        contentSnapshot = contentSnapshot,
        editedBy = editedBy,
        changeNote = changeNote,
        createdAtMillis = createdAtMillis,
        scriptDocId = scriptDocId,
        docId = docId
    )
}

@Keep
@IgnoreExtraProperties
data class CalendarEntryDoc(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val channelId: String = "",
    val channelName: String = "",
    val contentTitle: String = "",
    val category: String = "",
    val scriptWriter: String = "",
    val anchorName: String = "",
    val editorName: String = "",
    val shootingDateKey: String = "",
    val shootingTimeSlot: String = "",
    val editingDeadlineKey: String = "",
    val publishingDateKey: String = "",
    val publishingTime: String = "",
    val platform: String = "",
    val status: String = "",
    val studioRoom: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity() = CalendarEntryEntity(
        id = stableNumericId(docId),
        channelId = channelId,
        channelName = channelName,
        contentTitle = contentTitle,
        category = category,
        scriptWriter = scriptWriter,
        anchorName = anchorName,
        editorName = editorName,
        shootingDateKey = shootingDateKey,
        shootingTimeSlot = shootingTimeSlot,
        editingDeadlineKey = editingDeadlineKey,
        publishingDateKey = publishingDateKey,
        publishingTime = publishingTime,
        platform = platform,
        status = status,
        studioRoom = studioRoom,
        docId = docId
    )
}

@Keep
@IgnoreExtraProperties
data class ShootingScheduleDoc(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val scheduleMode: String = "",
    val channelId: String = "",
    val channelName: String = "",
    val topicOrKathaName: String = "",
    val scriptName: String = "",
    val talentId: String = "",
    val talentName: String = "",
    val coordinatorName: String = "",
    val editorName: String = "",
    val shootDateKey: String = "",
    val arrivalTime: String = "",
    val startTime: String = "",
    val expectedEndTime: String = "",
    val actualEndTime: String = "",
    val studioRoom: String = "",
    val specialRequirements: String = "",
    val materialsRequired: String = "",
    val materialsAvailable: Boolean = true,
    val purchaseRequired: String = "",
    val recordingStatus: String = "SCHEDULED",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity() = ShootingScheduleEntity(
        id = stableNumericId(docId),
        scheduleMode = scheduleMode,
        channelId = channelId,
        channelName = channelName,
        topicOrKathaName = topicOrKathaName,
        scriptName = scriptName,
        talentId = talentId,
        talentName = talentName,
        coordinatorName = coordinatorName,
        editorName = editorName,
        shootDateKey = shootDateKey,
        arrivalTime = arrivalTime,
        startTime = startTime,
        expectedEndTime = expectedEndTime,
        actualEndTime = actualEndTime,
        studioRoom = studioRoom,
        specialRequirements = specialRequirements,
        materialsRequired = materialsRequired,
        materialsAvailable = materialsAvailable,
        purchaseRequired = purchaseRequired,
        recordingStatus = recordingStatus,
        docId = docId
    )
}

@Keep
@IgnoreExtraProperties
data class AiAssignmentDoc(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val assignmentCode: String = "",
    val channelId: String = "",
    val channelName: String = "",
    val promptType: String = "",
    val title: String = "",
    val brief: String = "",
    val deadlineMillis: Long = 0L,
    val assignedAtMillis: Long = 0L,
    val startedAtMillis: Long? = null,
    val submittedAtMillis: Long? = null,
    val timeTakenMinutes: Long = 0L,
    val generatedPromptText: String = "",
    val attachedAssetName: String = "",
    val status: String = "ASSIGNED",
    val reviewerNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity() = AiAssignmentEntity(
        id = stableNumericId(docId),
        assignmentCode = assignmentCode,
        channelId = channelId,
        channelName = channelName,
        promptType = promptType,
        title = title,
        brief = brief,
        deadlineMillis = deadlineMillis,
        assignedAtMillis = assignedAtMillis,
        startedAtMillis = startedAtMillis,
        submittedAtMillis = submittedAtMillis,
        timeTakenMinutes = timeTakenMinutes.toInt(),
        generatedPromptText = generatedPromptText,
        attachedAssetName = attachedAssetName,
        status = status,
        reviewerNote = reviewerNote,
        docId = docId
    )
}

@Keep
@IgnoreExtraProperties
data class StudioFileDoc(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val channelId: String = "",
    val channelName: String = "",
    val projectName: String = "",
    val contentTitle: String = "",
    val fileType: String = "",
    val fileName: String = "",
    val uploadedBy: String = "",
    val uploadedById: String = "",
    val uploadDateMillis: Long = 0L,
    val version: String = "v1.0",
    val fileSizeLabel: String = "",
    val approvalStatus: String = "PENDING",
    val contentUri: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity() = StudioFileEntity(
        id = stableNumericId(docId),
        channelId = channelId,
        channelName = channelName,
        projectName = projectName,
        contentTitle = contentTitle,
        fileType = fileType,
        fileName = fileName,
        uploadedBy = uploadedBy,
        uploadedById = uploadedById,
        uploadDateMillis = uploadDateMillis,
        version = version,
        fileSizeLabel = fileSizeLabel,
        approvalStatus = approvalStatus,
        contentUri = contentUri,
        docId = docId
    )
}

@Keep
@IgnoreExtraProperties
data class SocialPostDoc(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val channelId: String = "",
    val channelName: String = "",
    val title: String = "",
    val platform: String = "YOUTUBE",
    val videoFileName: String = "",
    val thumbnailName: String = "",
    val caption: String = "",
    val description: String = "",
    val hashtags: String = "",
    val keywords: String = "",
    val scriptCode: String = "",
    val publishDateKey: String = "",
    val publishTime: String = "",
    val status: String = "READY_FOR_PUBLISHING",
    val publishedUrl: String = "",
    val checklistVerified: Boolean = false,
    val viewsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity() = SocialPostEntity(
        id = stableNumericId(docId),
        channelId = channelId,
        channelName = channelName,
        title = title,
        platform = platform,
        videoFileName = videoFileName,
        thumbnailName = thumbnailName,
        caption = caption,
        description = description,
        hashtags = hashtags,
        keywords = keywords,
        scriptCode = scriptCode,
        publishDateKey = publishDateKey,
        publishTime = publishTime,
        status = status,
        publishedUrl = publishedUrl,
        checklistVerified = checklistVerified,
        viewsCount = viewsCount.toInt(),
        docId = docId
    )
}

@Keep
@IgnoreExtraProperties
data class FinancialRecordDoc(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val recordType: String = "EXPENSE",
    val category: String = "",
    val title: String = "",
    val amountInr: Double = 0.0,
    val monthKey: String = "",
    val recordDateKey: String = "",
    val enteredBy: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity() = FinancialRecordEntity(
        id = stableNumericId(docId),
        recordType = recordType,
        category = category,
        title = title,
        amountInr = amountInr,
        monthKey = monthKey,
        recordDateKey = recordDateKey,
        enteredBy = enteredBy,
        notes = notes,
        docId = docId
    )
}

@Keep
@IgnoreExtraProperties
data class NotificationDoc(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val recipientUserId: String = "ALL",
    val title: String = "",
    val message: String = "",
    val category: String = "TASK",
    val read: Boolean = false,
    val createdAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity() = NotificationEntity(
        id = stableNumericId(docId),
        recipientUserId = recipientUserId,
        title = title,
        message = message,
        category = category,
        isRead = read,
        createdAtMillis = createdAtMillis,
        docId = docId
    )
}

@Keep
@IgnoreExtraProperties
data class LoginHistoryDoc(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val memberId: String = "",
    val username: String = "",
    val portal: String = "",
    val deviceInfo: String = "",
    val success: Boolean = true,
    val timestampMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toEntity() = LoginHistoryEntity(
        id = stableNumericId(docId),
        userId = memberId,
        username = username,
        portal = portal,
        deviceInfo = deviceInfo,
        success = success,
        timestamp = timestampMillis,
        docId = docId
    )
}
