package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val passwordHash: String,
    val salt: String,
    val fullName: String,
    val designation: String,
    val department: String,
    val role: String,
    val assignedChannelsCsv: String,
    val avatarInitials: String,
    val avatarColorHex: Long,
    val accountStatus: String = "ACTIVE",
    val lastLoginAt: Long = 0L,
    val email: String = "",
    val docId: String = ""
)

@Entity(tableName = "login_history")
data class LoginHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val username: String,
    val portal: String,
    val deviceInfo: String,
    val success: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val docId: String = ""
)

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val categoryTag: String,
    val description: String,
    val leadCoordinator: String,
    val accentColorHex: Long,
    val monthlyVideoTarget: Int,
    val videosProduced: Int,
    val videosPublished: Int,
    val growthPercent: Float,
    val subscribersLabel: String,
    val docId: String = ""
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskCode: String,
    val title: String,
    val description: String,
    val instructions: String,
    val channelId: String,
    val channelName: String,
    val category: String,
    val assignedToIdsCsv: String,
    val assignedToNamesCsv: String,
    val createdById: String,
    val priority: String,
    val status: String,
    val reviewStatus: String,
    val startDateMillis: Long,
    val deadlineMillis: Long,
    val expectedMinutes: Int,
    val actualMinutes: Int = 0,
    val completedAtMillis: Long? = null,
    val delayReason: String = "",
    val revisedDeadlineMillis: Long? = null,
    val revisedDeadlineMet: Boolean? = null,
    val managerComments: String = "",
    val complexityWeight: Int = 2,
    val attachedFileName: String = "",
    val submissionNote: String = "",
    val submissionFileName: String = "",
    val docId: String = ""
)

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val userName: String,
    val dateKey: String,
    val checkInTimeMillis: Long? = null,
    val checkOutTimeMillis: Long? = null,
    val verificationMethod: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distanceMeters: Float = 0f,
    val wifiSsid: String = "",
    val lateArrival: Boolean = false,
    val earlyDeparture: Boolean = false,
    val workingMinutes: Int = 0,
    val status: String,
    val correctionReason: String = "",
    val correctionRequested: Boolean = false,
    val correctionApprovedBy: String = "",
    val docId: String = ""
)

@Entity(tableName = "office_config")
data class OfficeConfigEntity(
    @PrimaryKey val id: Int = 1,
    val officeName: String,
    val officeLat: Double,
    val officeLng: Double,
    val geofenceRadiusMeters: Float,
    val authorizedWifiSsid: String,
    val workStartHour: Int,
    val workStartMinute: Int,
    val workEndHour: Int,
    val workEndMinute: Int,
    val currentQrToken: String,
    val qrUpdatedAt: Long,
    val docId: String = ""
)

@Entity(tableName = "attendance_audit")
data class AttendanceAuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetUserName: String,
    val action: String,
    val performedBy: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis(),
    val docId: String = ""
)

@Entity(tableName = "scripts")
data class ScriptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scriptCode: String,
    val channelId: String,
    val channelName: String,
    val videoTitle: String,
    val writerId: String,
    val writerName: String,
    val anchorName: String,
    val editorName: String,
    val deadlineMillis: Long,
    val status: String,
    val currentVersion: Int = 1,
    val contentBody: String,
    val reviewerComments: String = "",
    val highlightedErrors: String = "",
    val factCheckVerified: Boolean = false,
    val attachmentName: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val docId: String = ""
)

@Entity(tableName = "script_versions")
data class ScriptVersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scriptId: Long,
    val versionNumber: Int,
    val contentSnapshot: String,
    val editedBy: String,
    val changeNote: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val scriptDocId: String = "",
    val docId: String = ""
)

@Entity(tableName = "calendar_entries")
data class CalendarEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: String,
    val channelName: String,
    val contentTitle: String,
    val category: String,
    val scriptWriter: String,
    val anchorName: String,
    val editorName: String,
    val shootingDateKey: String,
    val shootingTimeSlot: String,
    val editingDeadlineKey: String,
    val publishingDateKey: String,
    val publishingTime: String,
    val platform: String,
    val status: String,
    val studioRoom: String,
    val docId: String = ""
)

@Entity(tableName = "shooting_schedules")
data class ShootingScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scheduleMode: String,
    val channelId: String,
    val channelName: String,
    val topicOrKathaName: String,
    val scriptName: String,
    val talentId: String,
    val talentName: String,
    val coordinatorName: String,
    val editorName: String,
    val shootDateKey: String,
    val arrivalTime: String,
    val startTime: String,
    val expectedEndTime: String,
    val actualEndTime: String = "",
    val studioRoom: String,
    val specialRequirements: String = "",
    val materialsRequired: String = "",
    val materialsAvailable: Boolean = true,
    val purchaseRequired: String = "",
    val recordingStatus: String = "SCHEDULED",
    val docId: String = ""
)

@Entity(tableName = "ai_assignments")
data class AiAssignmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assignmentCode: String,
    val channelId: String,
    val channelName: String,
    val promptType: String,
    val title: String,
    val brief: String,
    val deadlineMillis: Long,
    val assignedAtMillis: Long,
    val startedAtMillis: Long? = null,
    val submittedAtMillis: Long? = null,
    val timeTakenMinutes: Int = 0,
    val generatedPromptText: String = "",
    val attachedAssetName: String = "",
    val status: String = "ASSIGNED",
    val reviewerNote: String = "",
    val docId: String = ""
)

@Entity(tableName = "studio_files")
data class StudioFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: String,
    val channelName: String,
    val projectName: String,
    val contentTitle: String,
    val fileType: String,
    val fileName: String,
    val uploadedBy: String,
    val uploadedById: String,
    val uploadDateMillis: Long = System.currentTimeMillis(),
    val version: String = "v1.0",
    val fileSizeLabel: String,
    val approvalStatus: String = "PENDING",
    val contentUri: String = "",
    val docId: String = ""
)

@Entity(tableName = "social_posts")
data class SocialPostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: String,
    val channelName: String,
    val title: String,
    val platform: String,
    val videoFileName: String,
    val thumbnailName: String,
    val caption: String,
    val description: String,
    val hashtags: String,
    val keywords: String,
    val scriptCode: String,
    val publishDateKey: String,
    val publishTime: String,
    val status: String,
    val publishedUrl: String = "",
    val checklistVerified: Boolean = false,
    val viewsCount: Int = 0,
    val docId: String = ""
)

@Entity(tableName = "financial_records")
data class FinancialRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordType: String,
    val category: String,
    val title: String,
    val amountInr: Double,
    val monthKey: String,
    val recordDateKey: String,
    val enteredBy: String,
    val notes: String = "",
    val docId: String = ""
)

@Entity(tableName = "channel_workspace_items")
data class ChannelWorkspaceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: String,
    val itemType: String,
    val title: String,
    val details: String,
    val metaBadge: String,
    val isCompleted: Boolean = false,
    val ownerName: String,
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val docId: String = ""
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipientUserId: String,
    val title: String,
    val message: String,
    val category: String,
    val isRead: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val docId: String = ""
)
