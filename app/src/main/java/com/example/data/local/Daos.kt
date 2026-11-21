package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY id ASC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM login_history WHERE userId = :userId ORDER BY timestamp DESC LIMIT 20")
    fun observeLoginHistory(userId: String): Flow<List<LoginHistoryEntity>>

    @Insert
    suspend fun insertLoginHistory(entry: LoginHistoryEntity)
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY id ASC")
    fun observeAllChannels(): Flow<List<ChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<ChannelEntity>)

    @Update
    suspend fun updateChannel(channel: ChannelEntity)

    @Query("SELECT * FROM channel_workspace_items ORDER BY updatedAtMillis DESC")
    fun observeWorkspaceItems(): Flow<List<ChannelWorkspaceItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspaceItem(item: ChannelWorkspaceItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspaceItems(items: List<ChannelWorkspaceItemEntity>)

    @Update
    suspend fun updateWorkspaceItem(item: ChannelWorkspaceItemEntity)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY deadlineMillis ASC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance ORDER BY dateKey DESC, id DESC")
    fun observeAllAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM office_config WHERE id = 1 LIMIT 1")
    fun observeOfficeConfig(): Flow<OfficeConfigEntity?>

    @Query("SELECT * FROM office_config WHERE id = 1 LIMIT 1")
    suspend fun getOfficeConfig(): OfficeConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOfficeConfig(config: OfficeConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAttendance(records: List<AttendanceEntity>)

    @Update
    suspend fun updateAttendance(record: AttendanceEntity)

    @Query("SELECT * FROM attendance_audit ORDER BY timestamp DESC LIMIT 50")
    fun observeAuditLogs(): Flow<List<AttendanceAuditEntity>>

    @Insert
    suspend fun insertAudit(log: AttendanceAuditEntity)
}

@Dao
interface ScriptDao {
    @Query("SELECT * FROM scripts ORDER BY updatedAtMillis DESC")
    fun observeAllScripts(): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM script_versions ORDER BY versionNumber DESC")
    fun observeAllVersions(): Flow<List<ScriptVersionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: ScriptEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllScripts(scripts: List<ScriptEntity>)

    @Update
    suspend fun updateScript(script: ScriptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: ScriptVersionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllVersions(versions: List<ScriptVersionEntity>)
}

@Dao
interface CalendarShootingDao {
    @Query("SELECT * FROM calendar_entries ORDER BY shootingDateKey ASC, shootingTimeSlot ASC")
    fun observeCalendarEntries(): Flow<List<CalendarEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendarEntry(entry: CalendarEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCalendarEntries(entries: List<CalendarEntryEntity>)

    @Update
    suspend fun updateCalendarEntry(entry: CalendarEntryEntity)

    @Query("SELECT * FROM shooting_schedules ORDER BY shootDateKey ASC, startTime ASC")
    fun observeShootingSchedules(): Flow<List<ShootingScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShootingSchedule(schedule: ShootingScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllShootingSchedules(schedules: List<ShootingScheduleEntity>)

    @Update
    suspend fun updateShootingSchedule(schedule: ShootingScheduleEntity)
}

@Dao
interface AiFileSocialFinanceDao {
    @Query("SELECT * FROM ai_assignments ORDER BY deadlineMillis ASC")
    fun observeAiAssignments(): Flow<List<AiAssignmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiAssignment(assignment: AiAssignmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAiAssignments(assignments: List<AiAssignmentEntity>)

    @Update
    suspend fun updateAiAssignment(assignment: AiAssignmentEntity)

    @Query("SELECT * FROM studio_files ORDER BY uploadDateMillis DESC")
    fun observeStudioFiles(): Flow<List<StudioFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudioFile(file: StudioFileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllStudioFiles(files: List<StudioFileEntity>)

    @Update
    suspend fun updateStudioFile(file: StudioFileEntity)

    @Query("SELECT * FROM social_posts ORDER BY publishDateKey DESC")
    fun observeSocialPosts(): Flow<List<SocialPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSocialPost(post: SocialPostEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSocialPosts(posts: List<SocialPostEntity>)

    @Update
    suspend fun updateSocialPost(post: SocialPostEntity)

    @Query("SELECT * FROM financial_records ORDER BY recordDateKey DESC, id DESC")
    fun observeFinancialRecords(): Flow<List<FinancialRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinancialRecord(record: FinancialRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFinancialRecords(records: List<FinancialRecordEntity>)

    @Update
    suspend fun updateFinancialRecord(record: FinancialRecordEntity)

    @Query("SELECT * FROM notifications ORDER BY createdAtMillis DESC")
    fun observeNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationRead(id: Long)

    @Query("UPDATE notifications SET isRead = 1 WHERE recipientUserId = :userId OR recipientUserId = 'ALL'")
    suspend fun markAllReadForUser(userId: String)
}
