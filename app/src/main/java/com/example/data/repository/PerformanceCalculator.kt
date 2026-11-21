package com.example.data.repository

import com.example.data.local.AttendanceEntity
import com.example.data.local.TaskEntity
import com.example.data.local.UserEntity
import com.example.data.model.TaskStatus
import com.example.data.model.UserRole
import kotlin.math.roundToInt

object PerformanceCalculator {
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
                completedTasks.map { if (it.actualMinutes > 0) it.actualMinutes else it.expectedMinutes }
                    .average().roundToInt()
            } else 0

            val userAtt = attendance.filter { it.userId == user.id }
            val attPct = if (userAtt.isEmpty()) 92 else {
                val present = userAtt.count { it.status == "PRESENT" || it.status == "LATE" }
                ((present * 100f) / userAtt.size).roundToInt().coerceIn(0, 100)
            }

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
