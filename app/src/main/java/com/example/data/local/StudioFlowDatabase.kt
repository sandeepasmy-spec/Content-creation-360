package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        LoginHistoryEntity::class,
        ChannelEntity::class,
        ChannelWorkspaceItemEntity::class,
        TaskEntity::class,
        AttendanceEntity::class,
        OfficeConfigEntity::class,
        AttendanceAuditEntity::class,
        ScriptEntity::class,
        ScriptVersionEntity::class,
        CalendarEntryEntity::class,
        ShootingScheduleEntity::class,
        AiAssignmentEntity::class,
        StudioFileEntity::class,
        SocialPostEntity::class,
        FinancialRecordEntity::class,
        NotificationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class StudioFlowDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun channelDao(): ChannelDao
    abstract fun taskDao(): TaskDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun scriptDao(): ScriptDao
    abstract fun calendarShootingDao(): CalendarShootingDao
    abstract fun opsDao(): AiFileSocialFinanceDao

    companion object {
        @Volatile
        private var INSTANCE: StudioFlowDatabase? = null

        fun getInstance(context: Context): StudioFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudioFlowDatabase::class.java,
                    "studioflow360.db"
                ).fallbackToDestructiveMigration(true).build().also { INSTANCE = it }
            }
        }
    }
}
