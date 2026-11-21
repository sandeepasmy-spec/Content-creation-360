package com.example.data.model

enum class UserRole(
    val displayName: String,
    val portal: LoginPortal,
    val isHigherAuthority: Boolean,
    val isExternal: Boolean,
    val canAssignTasks: Boolean,
    val canVerifyScripts: Boolean,
    val canEditFinance: Boolean,
    val canViewFinance: Boolean,
    val isReadOnly: Boolean
) {
    SUPER_ADMIN(
        displayName = "Super Admin & Ops Head",
        portal = LoginPortal.HIGHER_AUTHORITY,
        isHigherAuthority = true,
        isExternal = false,
        canAssignTasks = true,
        canVerifyScripts = true,
        canEditFinance = true,
        canViewFinance = true,
        isReadOnly = false
    ),
    STRATEGIC_DIRECTOR(
        displayName = "Strategic Director",
        portal = LoginPortal.HIGHER_AUTHORITY,
        isHigherAuthority = true,
        isExternal = false,
        canAssignTasks = false,
        canVerifyScripts = true,
        canEditFinance = false,
        canViewFinance = true,
        isReadOnly = false
    ),
    INVESTOR(
        displayName = "Investor Stakeholder",
        portal = LoginPortal.HIGHER_AUTHORITY,
        isHigherAuthority = true,
        isExternal = false,
        canAssignTasks = false,
        canVerifyScripts = false,
        canEditFinance = false,
        canViewFinance = true,
        isReadOnly = true
    ),
    COORDINATOR_ANCHOR(
        displayName = "Anchor & Coordinator",
        portal = LoginPortal.TEAM,
        isHigherAuthority = false,
        isExternal = false,
        canAssignTasks = true,
        canVerifyScripts = true,
        canEditFinance = false,
        canViewFinance = false,
        isReadOnly = false
    ),
    AI_PARTNER(
        displayName = "Strategic AI Partner",
        portal = LoginPortal.TEAM,
        isHigherAuthority = false,
        isExternal = false,
        canAssignTasks = false,
        canVerifyScripts = false,
        canEditFinance = false,
        canViewFinance = false,
        isReadOnly = false
    ),
    VIDEO_EDITOR(
        displayName = "Video Editor",
        portal = LoginPortal.TEAM,
        isHigherAuthority = false,
        isExternal = false,
        canAssignTasks = false,
        canVerifyScripts = false,
        canEditFinance = false,
        canViewFinance = false,
        isReadOnly = false
    ),
    EXTERNAL_PANDIT(
        displayName = "Vrat Katha Narrator",
        portal = LoginPortal.EXTERNAL,
        isHigherAuthority = false,
        isExternal = true,
        canAssignTasks = false,
        canVerifyScripts = false,
        canEditFinance = false,
        canViewFinance = false,
        isReadOnly = false
    ),
    EXTERNAL_ART_TEACHER(
        displayName = "Art & Craft Instructor",
        portal = LoginPortal.EXTERNAL,
        isHigherAuthority = false,
        isExternal = true,
        canAssignTasks = false,
        canVerifyScripts = false,
        canEditFinance = false,
        canViewFinance = false,
        isReadOnly = false
    );

    companion object {
        fun fromString(value: String): UserRole =
            entries.firstOrNull { it.name == value } ?: VIDEO_EDITOR
    }
}

enum class LoginPortal(val label: String, val subtitle: String) {
    HIGHER_AUTHORITY("Higher Authority", "Owner, Director & Investor"),
    TEAM("Studio Team", "Coordinators, AI Partner & Editors"),
    EXTERNAL("External Contributors", "Pandit Ji & Craft Instructor")
}

enum class TaskStatus(val label: String, val progress: Float) {
    ASSIGNED("Assigned", 0.1f),
    ACCEPTED("Accepted", 0.25f),
    IN_PROGRESS("In Progress", 0.5f),
    SUBMITTED_FOR_REVIEW("Submitted for Review", 0.75f),
    REVISION_REQUIRED("Revision Required", 0.6f),
    APPROVED("Approved", 0.9f),
    COMPLETED("Completed", 1.0f),
    OVERDUE("Overdue", 0.4f);

    companion object {
        fun fromString(value: String): TaskStatus =
            entries.firstOrNull { it.name == value } ?: ASSIGNED
    }
}

enum class TaskPriority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    URGENT("Urgent");

    companion object {
        fun fromString(value: String): TaskPriority =
            entries.firstOrNull { it.name == value } ?: MEDIUM
    }
}

enum class ScriptStatus(val label: String, val stepIndex: Int) {
    IDEA("Idea", 0),
    SCRIPT_WRITING("Script Writing", 1),
    SCRIPT_SUBMITTED("Script Submitted", 2),
    VERIFICATION("Verification", 3),
    REVISION_REQUIRED("Revision Required", 3),
    APPROVED("Approved", 4),
    SHOOTING_READY("Shooting Ready", 5);

    companion object {
        fun fromString(value: String): ScriptStatus =
            entries.firstOrNull { it.name == value } ?: IDEA
    }
}

enum class PublishingStatus(val label: String) {
    READY_FOR_PUBLISHING("Ready for Publishing"),
    SCHEDULED("Scheduled"),
    PUBLISHED("Published"),
    PERFORMANCE_TRACKING("Performance Tracking");

    companion object {
        fun fromString(value: String): PublishingStatus =
            entries.firstOrNull { it.name == value } ?: READY_FOR_PUBLISHING
    }
}

enum class SocialPlatform(val label: String) {
    YOUTUBE("YouTube"),
    FACEBOOK("Facebook"),
    INSTAGRAM("Instagram");

    companion object {
        fun fromString(value: String): SocialPlatform =
            entries.firstOrNull { it.name == value } ?: YOUTUBE
    }
}

enum class DateRangeFilter(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month")
}
