package com.example.data.local

import com.example.data.model.UserRole
import com.example.util.GeofenceHelper
import com.example.util.SecurityUtils
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DatabaseSeeder {

    suspend fun seedIfEmpty(db: StudioFlowDatabase) {
        val existingUsers = db.userDao().observeAllUsers().first()
        if (existingUsers.isNotEmpty()) return

        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val monthFmt = SimpleDateFormat("yyyy-MM", Locale.US)
        val todayKey = dateFmt.format(Date(now))
        val tomorrowKey = dateFmt.format(Date(now + dayMs))
        val dayAfterKey = dateFmt.format(Date(now + 2 * dayMs))
        val yesterdayKey = dateFmt.format(Date(now - dayMs))
        val thisMonthKey = monthFmt.format(Date(now))

        val calLast = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val lastMonthKey = monthFmt.format(calLast.time)

        fun makeUser(
            id: String,
            username: String,
            fullName: String,
            designation: String,
            department: String,
            role: UserRole,
            channels: String,
            initials: String,
            colorHex: Long,
            email: String
        ): UserEntity {
            val salt = SecurityUtils.generateSalt()
            val hash = SecurityUtils.hashPassword("studio360", salt)
            return UserEntity(
                id = id,
                username = username,
                passwordHash = hash,
                salt = salt,
                fullName = fullName,
                designation = designation,
                department = department,
                role = role.name,
                assignedChannelsCsv = channels,
                avatarInitials = initials,
                avatarColorHex = colorHex,
                accountStatus = "ACTIVE",
                lastLoginAt = now - dayMs,
                email = email
            )
        }

        val users = listOf(
            makeUser(
                id = "usr_sandeep",
                username = "sandeep",
                fullName = "Sandeep",
                designation = "Owner & Operations Head",
                department = "Executive Operations",
                role = UserRole.SUPER_ADMIN,
                channels = "CH_ART_CRAFT,CH_VRAT_KATHA,CH_TECH,CH_SERPLIVE,CH_DOCUMENTARY",
                initials = "SA",
                colorHex = 0xFF2563EB,
                email = "sandeep@serplivesolutions.com"
            ),
            makeUser(
                id = "usr_sudeep",
                username = "sudeep",
                fullName = "Sudeep",
                designation = "Strategic Director (12 Yrs Exp)",
                department = "Content Strategy",
                role = UserRole.STRATEGIC_DIRECTOR,
                channels = "CH_ART_CRAFT,CH_VRAT_KATHA,CH_TECH,CH_SERPLIVE,CH_DOCUMENTARY",
                initials = "SU",
                colorHex = 0xFF6366F1,
                email = "sudeep@serplivesolutions.com"
            ),
            makeUser(
                id = "usr_avishek",
                username = "avishek",
                fullName = "Avishek",
                designation = "Lead Investor (18-Month Term)",
                department = "Capital & Governance",
                role = UserRole.INVESTOR,
                channels = "CH_ART_CRAFT,CH_VRAT_KATHA,CH_TECH,CH_SERPLIVE,CH_DOCUMENTARY",
                initials = "AV",
                colorHex = 0xFF10B981,
                email = "avishek@investor.serplivesolutions.com"
            ),
            makeUser(
                id = "usr_parchi",
                username = "parchi",
                fullName = "Parchi",
                designation = "Anchor & Production Coordinator",
                department = "Anchoring & Production",
                role = UserRole.COORDINATOR_ANCHOR,
                channels = "CH_TECH,CH_SERPLIVE,CH_DOCUMENTARY",
                initials = "PA",
                colorHex = 0xFFEC4899,
                email = "parchi@serplivesolutions.com"
            ),
            makeUser(
                id = "usr_kritika",
                username = "kritika",
                fullName = "Kritika",
                designation = "Anchor & Production Coordinator",
                department = "Anchoring & Production",
                role = UserRole.COORDINATOR_ANCHOR,
                channels = "CH_ART_CRAFT,CH_VRAT_KATHA,CH_TECH,CH_SERPLIVE,CH_DOCUMENTARY",
                initials = "KR",
                colorHex = 0xFF8B5CF6,
                email = "kritika@serplivesolutions.com"
            ),
            makeUser(
                id = "usr_manoj",
                username = "manoj",
                fullName = "Manoj",
                designation = "Strategic Partner & AI Prompt Lead",
                department = "AI Content Studio",
                role = UserRole.AI_PARTNER,
                channels = "CH_ART_CRAFT,CH_VRAT_KATHA,CH_TECH,CH_SERPLIVE,CH_DOCUMENTARY",
                initials = "MA",
                colorHex = 0xFF06B6D4,
                email = "manoj@serplivesolutions.com"
            ),
            makeUser(
                id = "usr_editor1",
                username = "editor1",
                fullName = "Editor 1 (Aarav)",
                designation = "Senior Video Editor & AI Visuals",
                department = "Post-Production",
                role = UserRole.VIDEO_EDITOR,
                channels = "CH_TECH,CH_SERPLIVE,CH_DOCUMENTARY",
                initials = "E1",
                colorHex = 0xFFF59E0B,
                email = "editor1@serplivesolutions.com"
            ),
            makeUser(
                id = "usr_editor2",
                username = "editor2",
                fullName = "Editor 2 (Rohan)",
                designation = "Video Editor & Client Publisher",
                department = "Post-Production",
                role = UserRole.VIDEO_EDITOR,
                channels = "CH_ART_CRAFT,CH_VRAT_KATHA,CH_SERPLIVE",
                initials = "E2",
                colorHex = 0xFFF97316,
                email = "editor2@serplivesolutions.com"
            ),
            makeUser(
                id = "usr_panditji",
                username = "panditji",
                fullName = "Pandit Ji",
                designation = "Vrat Katha Narrator (External)",
                department = "External Talent",
                role = UserRole.EXTERNAL_PANDIT,
                channels = "CH_VRAT_KATHA",
                initials = "PJ",
                colorHex = 0xFFEA580C,
                email = "panditji@external.serplive"
            ),
            makeUser(
                id = "usr_artteacher",
                username = "artteacher",
                fullName = "Art & Craft Teacher",
                designation = "Craft Instructor (External)",
                department = "External Talent",
                role = UserRole.EXTERNAL_ART_TEACHER,
                channels = "CH_ART_CRAFT",
                initials = "AC",
                colorHex = 0xFFDB2777,
                email = "craftteacher@external.serplive"
            )
        )
        db.userDao().insertAll(users)

        val channels = listOf(
            ChannelEntity(
                id = "CH_ART_CRAFT",
                name = "Art and Craft",
                categoryTag = "DIY & Studio Projects",
                description = "Studio craft demonstrations by Art & Craft Teacher with material prep, raw shoot tracking & thumbnails.",
                leadCoordinator = "Kritika",
                accentColorHex = 0xFFEC4899,
                monthlyVideoTarget = 16,
                videosProduced = 11,
                videosPublished = 9,
                growthPercent = 18.4f,
                subscribersLabel = "42.8K"
            ),
            ChannelEntity(
                id = "CH_VRAT_KATHA",
                name = "Vrat Katha",
                categoryTag = "Devotional Narration",
                description = "Authentic voice narrations by Pandit Ji with ritual calendar alignment, script verification & studio audio.",
                leadCoordinator = "Kritika",
                accentColorHex = 0xFFF97316,
                monthlyVideoTarget = 20,
                videosProduced = 15,
                videosPublished = 13,
                growthPercent = 24.2f,
                subscribersLabel = "68.5K"
            ),
            ChannelEntity(
                id = "CH_TECH",
                name = "Technology Channel",
                categoryTag = "Tech Reviews & AI Guides",
                description = "Anchored by Parchi & Kritika covering research-verified tech tutorials, AI visuals & high-CTR thumbnails.",
                leadCoordinator = "Parchi",
                accentColorHex = 0xFF06B6D4,
                monthlyVideoTarget = 14,
                videosProduced = 10,
                videosPublished = 8,
                growthPercent = 31.6f,
                subscribersLabel = "54.1K"
            ),
            ChannelEntity(
                id = "CH_SERPLIVE",
                name = "Serplive Solutions",
                categoryTag = "Digital Marketing Agency",
                description = "Agency thought-leadership, client campaign videos, promotional reels & multi-platform lead tracking.",
                leadCoordinator = "Parchi",
                accentColorHex = 0xFF2563EB,
                monthlyVideoTarget = 18,
                videosProduced = 12,
                videosPublished = 10,
                growthPercent = 22.0f,
                subscribersLabel = "29.4K"
            ),
            ChannelEntity(
                id = "CH_DOCUMENTARY",
                name = "Documentary Channel",
                categoryTag = "Deep-Dive Research",
                description = "Fact-checked storytelling anchored by Parchi & Kritika with voice-over, archival graphics & post-production.",
                leadCoordinator = "Parchi",
                accentColorHex = 0xFF8B5CF6,
                monthlyVideoTarget = 8,
                videosProduced = 5,
                videosPublished = 4,
                growthPercent = 27.5f,
                subscribersLabel = "37.2K"
            )
        )
        db.channelDao().insertAll(channels)

        db.channelDao().insertWorkspaceItems(
            listOf(
                ChannelWorkspaceItemEntity(
                    channelId = "CH_ART_CRAFT",
                    itemType = "CRAFT_IDEA",
                    title = "Terracotta Diwali Lantern & Mirror Mosaic",
                    details = "Step-by-step studio demo focusing on overhead macro shots.",
                    metaBadge = "Approved Idea",
                    isCompleted = true,
                    ownerName = "Art & Craft Teacher"
                ),
                ChannelWorkspaceItemEntity(
                    channelId = "CH_ART_CRAFT",
                    itemType = "MATERIAL_SHOPPING",
                    title = "Acrylic Metallic Paints (12-color set) & Craft Mirrors",
                    details = "Required: 4 sets metallic acrylics, 200g diamond mirrors, Fevicryl glue.",
                    metaBadge = "Purchase Pending",
                    isCompleted = false,
                    ownerName = "Kritika"
                ),
                ChannelWorkspaceItemEntity(
                    channelId = "CH_TECH",
                    itemType = "TECH_RESEARCH",
                    title = "2026 Smartphone Camera Sensor Benchmarks",
                    details = "Verified specs comparison table & low-light sample test cards.",
                    metaBadge = "Fact-Checked",
                    isCompleted = true,
                    ownerName = "Parchi"
                ),
                ChannelWorkspaceItemEntity(
                    channelId = "CH_SERPLIVE",
                    itemType = "CLIENT_LEAD",
                    title = "Apex Dental Clinic — Performance Video Ad Series",
                    details = "3 promotional reels + YouTube explainer. Client feedback pending on Cut v2.",
                    metaBadge = "Active Client",
                    isCompleted = false,
                    ownerName = "Editor 2 (Rohan)"
                ),
                ChannelWorkspaceItemEntity(
                    channelId = "CH_DOCUMENTARY",
                    itemType = "DOC_REFERENCE",
                    title = "Ancient Stepwells of Western India — Archival Sources",
                    details = "ASI architectural survey citations & 19th-century gazetteer cross-check.",
                    metaBadge = "Verified Source",
                    isCompleted = true,
                    ownerName = "Kritika"
                )
            )
        )

        db.attendanceDao().upsertOfficeConfig(
            OfficeConfigEntity(
                id = 1,
                officeName = "Serplive Solutions Studio HQ",
                officeLat = 28.6139,
                officeLng = 77.2090,
                geofenceRadiusMeters = 150f,
                authorizedWifiSsid = "Serplive_Studio_5G",
                workStartHour = 10,
                workStartMinute = 0,
                workEndHour = 19,
                workEndMinute = 0,
                currentQrToken = GeofenceHelper.generateDynamicQrToken(now),
                qrUpdatedAt = now
            )
        )

        db.attendanceDao().insertAllAttendance(
            listOf(
                AttendanceEntity(
                    userId = "usr_parchi",
                    userName = "Parchi",
                    dateKey = todayKey,
                    checkInTimeMillis = now - 4 * 3600_000L,
                    verificationMethod = "GPS_GEOFENCE",
                    latitude = 28.6139,
                    longitude = 77.2090,
                    distanceMeters = 18f,
                    wifiSsid = "Serplive_Studio_5G",
                    lateArrival = false,
                    workingMinutes = 240,
                    status = "PRESENT"
                ),
                AttendanceEntity(
                    userId = "usr_kritika",
                    userName = "Kritika",
                    dateKey = todayKey,
                    checkInTimeMillis = now - 3 * 3600_000L,
                    verificationMethod = "DYNAMIC_QR",
                    latitude = 28.6140,
                    longitude = 77.2091,
                    distanceMeters = 24f,
                    wifiSsid = "Serplive_Studio_5G",
                    lateArrival = true,
                    workingMinutes = 180,
                    status = "LATE"
                ),
                AttendanceEntity(
                    userId = "usr_manoj",
                    userName = "Manoj",
                    dateKey = todayKey,
                    checkInTimeMillis = now - 4 * 3600_000L,
                    verificationMethod = "WIFI_SSID",
                    latitude = 28.6138,
                    longitude = 77.2089,
                    distanceMeters = 12f,
                    wifiSsid = "Serplive_Studio_5G",
                    lateArrival = false,
                    workingMinutes = 250,
                    status = "PRESENT"
                ),
                AttendanceEntity(
                    userId = "usr_editor1",
                    userName = "Editor 1 (Aarav)",
                    dateKey = yesterdayKey,
                    checkInTimeMillis = now - dayMs - 3 * 3600_000L,
                    checkOutTimeMillis = now - dayMs + 6 * 3600_000L,
                    verificationMethod = "GPS_GEOFENCE",
                    latitude = 28.6139,
                    longitude = 77.2090,
                    distanceMeters = 15f,
                    wifiSsid = "Serplive_Studio_5G",
                    lateArrival = false,
                    workingMinutes = 540,
                    status = "PRESENT"
                )
            )
        )

        db.attendanceDao().insertAudit(
            AttendanceAuditEntity(
                targetUserName = "Kritika",
                action = "LATE_CHECK_IN_LOGGED",
                performedBy = "System Geofence",
                details = "Checked in via Dynamic QR 22 mins after 10:00 AM shift start.",
                timestamp = now - 3 * 3600_000L
            )
        )

        db.taskDao().insertAll(
            listOf(
                TaskEntity(
                    taskCode = "TSK-101",
                    title = "Edit AI Workflows Documentary Cut v1",
                    description = "Assemble A-roll narration with animated timeline graphics and sound design.",
                    instructions = "Keep pacing crisp under 14 minutes. Export 4K H.264 master.",
                    channelId = "CH_DOCUMENTARY",
                    channelName = "Documentary Channel",
                    category = "EDIT",
                    assignedToIdsCsv = "usr_editor1",
                    assignedToNamesCsv = "Editor 1 (Aarav)",
                    createdById = "usr_sandeep",
                    priority = "HIGH",
                    status = "IN_PROGRESS",
                    reviewStatus = "PENDING",
                    startDateMillis = now - dayMs,
                    deadlineMillis = now + 6 * 3600_000L,
                    expectedMinutes = 240,
                    actualMinutes = 145,
                    managerComments = "Focus on lower-third fact callouts in chapter 2.",
                    complexityWeight = 4,
                    attachedFileName = "SCR-204_Doc_Script.pdf"
                ),
                TaskEntity(
                    taskCode = "TSK-102",
                    title = "Generate 6 Midjourney/AI Visual Prompts for Tech Ep 18",
                    description = "Create photorealistic AI image + image-to-video motion prompts for B-roll.",
                    instructions = "Deliver prompts ready for Editor 1 in Manoj AI Workspace.",
                    channelId = "CH_TECH",
                    channelName = "Technology Channel",
                    category = "AI_PROMPT",
                    assignedToIdsCsv = "usr_manoj",
                    assignedToNamesCsv = "Manoj",
                    createdById = "usr_parchi",
                    priority = "URGENT",
                    status = "ASSIGNED",
                    reviewStatus = "PENDING",
                    startDateMillis = now,
                    deadlineMillis = now + 4 * 3600_000L,
                    expectedMinutes = 90,
                    complexityWeight = 3
                ),
                TaskEntity(
                    taskCode = "TSK-103",
                    title = "Client Promo Reels — Apex Dental Package",
                    description = "Deliver 2 vertical 9:16 Instagram reels and coordinate client sign-off.",
                    instructions = "Use Serplive Solutions brand outro and subtitles.",
                    channelId = "CH_SERPLIVE",
                    channelName = "Serplive Solutions",
                    category = "CLIENT_WORK",
                    assignedToIdsCsv = "usr_editor2",
                    assignedToNamesCsv = "Editor 2 (Rohan)",
                    createdById = "usr_sandeep",
                    priority = "HIGH",
                    status = "OVERDUE",
                    reviewStatus = "ACTION_NEEDED",
                    startDateMillis = now - 3 * dayMs,
                    deadlineMillis = now - 5 * 3600_000L,
                    expectedMinutes = 180,
                    actualMinutes = 150,
                    delayReason = "Awaiting revised clinic logo PNG from client.",
                    managerComments = "Follow up with client today and submit draft by 5 PM.",
                    complexityWeight = 3
                ),
                TaskEntity(
                    taskCode = "TSK-104",
                    title = "Verify Ekadashi Vrat Katha Script & Pronunciation Notes",
                    description = "Review Sanskrit shloka cues and mark breathing pauses for Pandit Ji.",
                    instructions = "Coordinate studio audio check before 11 AM recording.",
                    channelId = "CH_VRAT_KATHA",
                    channelName = "Vrat Katha",
                    category = "SCRIPT",
                    assignedToIdsCsv = "usr_kritika",
                    assignedToNamesCsv = "Kritika",
                    createdById = "usr_sandeep",
                    priority = "MEDIUM",
                    status = "SUBMITTED_FOR_REVIEW",
                    reviewStatus = "AWAITING_APPROVAL",
                    startDateMillis = now - dayMs,
                    deadlineMillis = now + dayMs,
                    expectedMinutes = 75,
                    actualMinutes = 65,
                    submissionNote = "Uploaded verified script v2 with pause markers.",
                    submissionFileName = "Ekadashi_Katha_v2.docx",
                    complexityWeight = 2
                ),
                TaskEntity(
                    taskCode = "TSK-105",
                    title = "Anchor & Shoot Tech Tutorial: Local AI Productivity Stack",
                    description = "Studio A shoot with Parchi anchoring and live screen overlay markers.",
                    instructions = "Confirm teleprompter script SCR-202 is locked before shoot.",
                    channelId = "CH_TECH",
                    channelName = "Technology Channel",
                    category = "SHOOT",
                    assignedToIdsCsv = "usr_parchi",
                    assignedToNamesCsv = "Parchi",
                    createdById = "usr_sandeep",
                    priority = "HIGH",
                    status = "COMPLETED",
                    reviewStatus = "APPROVED",
                    startDateMillis = now - 2 * dayMs,
                    deadlineMillis = now - dayMs,
                    expectedMinutes = 120,
                    actualMinutes = 110,
                    completedAtMillis = now - dayMs - 2 * 3600_000L,
                    managerComments = "Great energy and crisp delivery.",
                    complexityWeight = 3
                )
            )
        )

        db.scriptDao().insertAllScripts(
            listOf(
                ScriptEntity(
                    id = 1L,
                    scriptCode = "SCR-201",
                    channelId = "CH_VRAT_KATHA",
                    channelName = "Vrat Katha",
                    videoTitle = "Devutthana Ekadashi Vrat Katha & Puja Vidhi",
                    writerId = "usr_kritika",
                    writerName = "Kritika",
                    anchorName = "Pandit Ji",
                    editorName = "Editor 2 (Rohan)",
                    deadlineMillis = now + dayMs,
                    status = "SHOOTING_READY",
                    currentVersion = 2,
                    contentBody = "Intro Invocation: Om Namo Bhagavate Vasudevaya...\n\nSection 1: Significance of Kartik Shukla Ekadashi...\nSection 2: Complete Katha Narration with calm 2-second pauses between shlokas.\nClosing Blessing & Channel Subscribe Appeal.",
                    reviewerComments = "Approved by Sudeep & Sandeep. Ready for Pandit Ji audio recording.",
                    factCheckVerified = true,
                    attachmentName = "Ekadashi_Katha_Final.pdf"
                ),
                ScriptEntity(
                    id = 2L,
                    scriptCode = "SCR-202",
                    channelId = "CH_TECH",
                    channelName = "Technology Channel",
                    videoTitle = "Top 5 AI Video Production Tools for Creators in 2026",
                    writerId = "usr_parchi",
                    writerName = "Parchi",
                    anchorName = "Parchi",
                    editorName = "Editor 1 (Aarav)",
                    deadlineMillis = now + 8 * 3600_000L,
                    status = "VERIFICATION",
                    currentVersion = 2,
                    contentBody = "HOOK (0:00): Still spending 8 hours editing B-roll manually?\n\nCHAPTER 1: AI Script Structuring\nCHAPTER 2: Keyframe Image-to-Video\nCHAPTER 3: Automated Color & Sound\nOUTRO: Comment 'STUDIO' for prompt pack.",
                    reviewerComments = "Verify pricing tier slide in Chapter 2 before final approval.",
                    highlightedErrors = "Line 14: Confirm monthly price for Tool #3.",
                    factCheckVerified = false,
                    attachmentName = "Tech_AI_Tools_v2.docx"
                ),
                ScriptEntity(
                    id = 3L,
                    scriptCode = "SCR-203",
                    channelId = "CH_ART_CRAFT",
                    channelName = "Art and Craft",
                    videoTitle = "Handmade Terracotta Mirror Mosaic Wall Hanging",
                    writerId = "usr_kritika",
                    writerName = "Kritika",
                    anchorName = "Art & Craft Teacher",
                    editorName = "Editor 2 (Rohan)",
                    deadlineMillis = now + 2 * dayMs,
                    status = "APPROVED",
                    currentVersion = 1,
                    contentBody = "Tabletop Setup: Overhead 4K camera + side detail cam.\nStep 1: Base primer coat (dry 10 mins).\nStep 2: Geometric chalk grid.\nStep 3: Mirror placement & metallic border finish.",
                    reviewerComments = "Material list verified with store inventory.",
                    factCheckVerified = true,
                    attachmentName = "Terracotta_Mosaic_Guide.pdf"
                ),
                ScriptEntity(
                    id = 4L,
                    scriptCode = "SCR-204",
                    channelId = "CH_DOCUMENTARY",
                    channelName = "Documentary Channel",
                    videoTitle = "How Ancient Indian Stepwells Engineered Monsoon Water",
                    writerId = "usr_manoj",
                    writerName = "Manoj",
                    anchorName = "Parchi",
                    editorName = "Editor 1 (Aarav)",
                    deadlineMillis = now + 3 * dayMs,
                    status = "REVISION_REQUIRED",
                    currentVersion = 1,
                    contentBody = "Prologue: Rani ki Vav aerial CGI opener.\nAct 1: Subterranean thermal cooling.\nAct 2: Stone masonry & aquifer recharge.",
                    reviewerComments = "Add architectural depth metrics from ASI survey citation.",
                    highlightedErrors = "Act 1 timestamp 03:20 needs citation source tag.",
                    factCheckVerified = false,
                    attachmentName = "Stepwells_Draft_v1.docx"
                )
            )
        )

        db.scriptDao().insertAllVersions(
            listOf(
                ScriptVersionEntity(
                    scriptId = 1L,
                    versionNumber = 1,
                    contentSnapshot = "Initial Katha draft without pause cues.",
                    editedBy = "Kritika",
                    changeNote = "Initial draft"
                ),
                ScriptVersionEntity(
                    scriptId = 1L,
                    versionNumber = 2,
                    contentSnapshot = "Added shloka pronunciation guide & 2-second studio pause cues.",
                    editedBy = "Kritika",
                    changeNote = "Added pronunciation cues per review"
                ),
                ScriptVersionEntity(
                    scriptId = 2L,
                    versionNumber = 1,
                    contentSnapshot = "Outline + 5 tool list.",
                    editedBy = "Parchi",
                    changeNote = "Initial tech script"
                ),
                ScriptVersionEntity(
                    scriptId = 2L,
                    versionNumber = 2,
                    contentSnapshot = "Updated hook & added CTA prompt.",
                    editedBy = "Parchi",
                    changeNote = "Strengthened 15s hook"
                )
            )
        )

        db.calendarShootingDao().insertAllCalendarEntries(
            listOf(
                CalendarEntryEntity(
                    channelId = "CH_VRAT_KATHA",
                    channelName = "Vrat Katha",
                    contentTitle = "Devutthana Ekadashi Katha",
                    category = "Devotional Narration",
                    scriptWriter = "Kritika",
                    anchorName = "Pandit Ji",
                    editorName = "Editor 2 (Rohan)",
                    shootingDateKey = todayKey,
                    shootingTimeSlot = "11:00 - 12:30",
                    editingDeadlineKey = tomorrowKey,
                    publishingDateKey = dayAfterKey,
                    publishingTime = "06:30 AM",
                    platform = "YouTube",
                    status = "Shooting Today",
                    studioRoom = "Audio Booth"
                ),
                CalendarEntryEntity(
                    channelId = "CH_TECH",
                    channelName = "Technology Channel",
                    contentTitle = "Top 5 AI Video Tools 2026",
                    category = "Tech Review",
                    scriptWriter = "Parchi",
                    anchorName = "Parchi",
                    editorName = "Editor 1 (Aarav)",
                    shootingDateKey = todayKey,
                    shootingTimeSlot = "14:00 - 16:00",
                    editingDeadlineKey = dayAfterKey,
                    publishingDateKey = dayAfterKey,
                    publishingTime = "06:00 PM",
                    platform = "YouTube",
                    status = "Scheduled",
                    studioRoom = "Studio A"
                ),
                CalendarEntryEntity(
                    channelId = "CH_ART_CRAFT",
                    channelName = "Art and Craft",
                    contentTitle = "Terracotta Mirror Mosaic",
                    category = "Craft Demo",
                    scriptWriter = "Kritika",
                    anchorName = "Art & Craft Teacher",
                    editorName = "Editor 2 (Rohan)",
                    shootingDateKey = tomorrowKey,
                    shootingTimeSlot = "11:30 - 13:30",
                    editingDeadlineKey = dayAfterKey,
                    publishingDateKey = dayAfterKey,
                    publishingTime = "04:00 PM",
                    platform = "YouTube",
                    status = "Prep Ready",
                    studioRoom = "Studio B"
                ),
                CalendarEntryEntity(
                    channelId = "CH_SERPLIVE",
                    channelName = "Serplive Solutions",
                    contentTitle = "B2B Performance Funnel Breakdown",
                    category = "Agency Education",
                    scriptWriter = "Parchi",
                    anchorName = "Kritika",
                    editorName = "Editor 1 (Aarav)",
                    shootingDateKey = tomorrowKey,
                    shootingTimeSlot = "15:00 - 16:30",
                    editingDeadlineKey = dayAfterKey,
                    publishingDateKey = dayAfterKey,
                    publishingTime = "07:30 PM",
                    platform = "Instagram",
                    status = "Scheduled",
                    studioRoom = "Studio A"
                )
            )
        )

        db.calendarShootingDao().insertAllShootingSchedules(
            listOf(
                ShootingScheduleEntity(
                    scheduleMode = "PANDIT_JI",
                    channelId = "CH_VRAT_KATHA",
                    channelName = "Vrat Katha",
                    topicOrKathaName = "Devutthana Ekadashi Vrat Katha",
                    scriptName = "SCR-201",
                    talentId = "usr_panditji",
                    talentName = "Pandit Ji",
                    coordinatorName = "Kritika",
                    editorName = "Editor 2 (Rohan)",
                    shootDateKey = todayKey,
                    arrivalTime = "10:45 AM",
                    startTime = "11:00",
                    expectedEndTime = "12:30",
                    studioRoom = "Audio Booth",
                    specialRequirements = "Warm water, printed large-font Hindi script, condenser mic pop filter.",
                    recordingStatus = "SCHEDULED"
                ),
                ShootingScheduleEntity(
                    scheduleMode = "ART_CRAFT_TEACHER",
                    channelId = "CH_ART_CRAFT",
                    channelName = "Art and Craft",
                    topicOrKathaName = "Terracotta Mirror Mosaic Wall Art",
                    scriptName = "SCR-203",
                    talentId = "usr_artteacher",
                    talentName = "Art & Craft Teacher",
                    coordinatorName = "Kritika",
                    editorName = "Editor 2 (Rohan)",
                    shootDateKey = tomorrowKey,
                    arrivalTime = "11:15 AM",
                    startTime = "11:30",
                    expectedEndTime = "13:30",
                    studioRoom = "Studio B",
                    specialRequirements = "Overhead macro rig + warm tabletop lights.",
                    materialsRequired = "Terracotta plate (2), Acrylic Paints (12), Craft Mirrors (200g), Glue (2)",
                    materialsAvailable = false,
                    purchaseRequired = "Buy 12-color metallic acrylic set & 200g craft mirrors.",
                    recordingStatus = "SCHEDULED"
                ),
                ShootingScheduleEntity(
                    scheduleMode = "ANCHOR",
                    channelId = "CH_TECH",
                    channelName = "Technology Channel",
                    topicOrKathaName = "Top 5 AI Video Production Tools",
                    scriptName = "SCR-202",
                    talentId = "usr_parchi",
                    talentName = "Parchi",
                    coordinatorName = "Parchi",
                    editorName = "Editor 1 (Aarav)",
                    shootDateKey = todayKey,
                    arrivalTime = "01:45 PM",
                    startTime = "14:00",
                    expectedEndTime = "16:00",
                    studioRoom = "Studio A",
                    specialRequirements = "Smart casual navy blazer, teleprompter loaded with SCR-202, demo laptop.",
                    recordingStatus = "SCHEDULED"
                )
            )
        )

        db.opsDao().insertAllAiAssignments(
            listOf(
                AiAssignmentEntity(
                    assignmentCode = "AI-301",
                    channelId = "CH_DOCUMENTARY",
                    channelName = "Documentary Channel",
                    promptType = "IMAGE_TO_VIDEO_PROMPT",
                    title = "Stepwell Ancient Sunlight Shafts Motion Prompts",
                    brief = "Generate 4 image-to-video motion prompts for 3D animated archival stills.",
                    deadlineMillis = now + 5 * 3600_000L,
                    assignedAtMillis = now - 3 * 3600_000L,
                    startedAtMillis = now - 2 * 3600_000L,
                    timeTakenMinutes = 42,
                    generatedPromptText = "Slow vertical crane descent through sunlit carved sandstone arches, subtle atmospheric dust motes, calm cinematic 24fps.",
                    status = "IN_PROGRESS"
                ),
                AiAssignmentEntity(
                    assignmentCode = "AI-302",
                    channelId = "CH_TECH",
                    channelName = "Technology Channel",
                    promptType = "IMAGE_PROMPT",
                    title = "Cyber-Clean Desk Setup Thumbnail Visuals",
                    brief = "High-contrast 16:9 thumbnail background concepts without text overlays.",
                    deadlineMillis = now + dayMs,
                    assignedAtMillis = now - dayMs,
                    startedAtMillis = now - dayMs + 3600_000L,
                    submittedAtMillis = now - dayMs + 2 * 3600_000L,
                    timeTakenMinutes = 55,
                    generatedPromptText = "Minimalist obsidian desk, glowing cyan edge light, floating glass UI card silhouette, 85mm shallow depth of field.",
                    attachedAssetName = "tech_thumb_prompts_v1.txt",
                    status = "APPROVED",
                    reviewerNote = "Strong visual framing — approved for Editor 1."
                )
            )
        )

        db.opsDao().insertAllStudioFiles(
            listOf(
                StudioFileEntity(
                    channelId = "CH_VRAT_KATHA",
                    channelName = "Vrat Katha",
                    projectName = "Kartik Month Series",
                    contentTitle = "Devutthana Ekadashi Katha",
                    fileType = "SCRIPT",
                    fileName = "Ekadashi_Katha_Final_v2.pdf",
                    uploadedBy = "Kritika",
                    uploadedById = "usr_kritika",
                    version = "v2.0",
                    fileSizeLabel = "1.4 MB",
                    approvalStatus = "APPROVED"
                ),
                StudioFileEntity(
                    channelId = "CH_ART_CRAFT",
                    channelName = "Art and Craft",
                    projectName = "Festive Decor Batch",
                    contentTitle = "Terracotta Mirror Mosaic",
                    fileType = "SHOOTING_INSTRUCTIONS",
                    fileName = "Terracotta_ShotList_&_Materials.pdf",
                    uploadedBy = "Kritika",
                    uploadedById = "usr_kritika",
                    version = "v1.0",
                    fileSizeLabel = "860 KB",
                    approvalStatus = "APPROVED"
                ),
                StudioFileEntity(
                    channelId = "CH_DOCUMENTARY",
                    channelName = "Documentary Channel",
                    projectName = "Heritage Engineering",
                    contentTitle = "Ancient Stepwells",
                    fileType = "VIDEO_FILE",
                    fileName = "Stepwells_RoughCut_v1.mp4",
                    uploadedBy = "Editor 1 (Aarav)",
                    uploadedById = "usr_editor1",
                    version = "v1.2",
                    fileSizeLabel = "412 MB",
                    approvalStatus = "PENDING"
                ),
                StudioFileEntity(
                    channelId = "CH_TECH",
                    channelName = "Technology Channel",
                    projectName = "AI Creators 2026",
                    contentTitle = "Top 5 AI Video Tools",
                    fileType = "GRAPHICS",
                    fileName = "Tech_LowerThirds_Pack.zip",
                    uploadedBy = "Editor 1 (Aarav)",
                    uploadedById = "usr_editor1",
                    version = "v1.0",
                    fileSizeLabel = "28.5 MB",
                    approvalStatus = "APPROVED"
                )
            )
        )

        db.opsDao().insertAllSocialPosts(
            listOf(
                SocialPostEntity(
                    channelId = "CH_VRAT_KATHA",
                    channelName = "Vrat Katha",
                    title = "Tulsi Vivah Complete Katha & Muhurat",
                    platform = "YOUTUBE",
                    videoFileName = "Tulsi_Vivah_Final_4K.mp4",
                    thumbnailName = "Tulsi_Vivah_Thumb.png",
                    caption = "Listen to the sacred Tulsi Vivah Katha narrated by Pandit Ji.",
                    description = "Complete devotional narration with shloka meanings and puja vidhi timings.",
                    hashtags = "#VratKatha #TulsiVivah #Devotional #Serplive",
                    keywords = "tulsi vivah katha, vrat katha hindi, pandit ji narration",
                    scriptCode = "SCR-198",
                    publishDateKey = yesterdayKey,
                    publishTime = "06:30 AM",
                    status = "PUBLISHED",
                    publishedUrl = "https://youtube.com/watch?v=sf360_tulsivivah",
                    checklistVerified = true,
                    viewsCount = 48200
                ),
                SocialPostEntity(
                    channelId = "CH_TECH",
                    channelName = "Technology Channel",
                    title = "Top 5 AI Video Production Tools 2026",
                    platform = "YOUTUBE",
                    videoFileName = "Tech_AI_Tools_Master.mp4",
                    thumbnailName = "AI_Tools_Thumb_v2.png",
                    caption = "Cut your video editing workflow in half with these 5 studio-tested AI tools.",
                    description = "Parchi breaks down real production benchmarks across scripting, motion graphics & audio.",
                    hashtags = "#TechIndia #AIVideoTools #CreatorWorkflow",
                    keywords = "ai video tools 2026, creator workflow, tech tutorial",
                    scriptCode = "SCR-202",
                    publishDateKey = tomorrowKey,
                    publishTime = "06:00 PM",
                    status = "READY_FOR_PUBLISHING",
                    checklistVerified = false,
                    viewsCount = 0
                ),
                SocialPostEntity(
                    channelId = "CH_SERPLIVE",
                    channelName = "Serplive Solutions",
                    title = "3 Meta Ads Hooks Scaling Local Clinics",
                    platform = "INSTAGRAM",
                    videoFileName = "Serplive_MetaAds_Reel.mp4",
                    thumbnailName = "MetaAds_Cover.png",
                    caption = "Steal these 3 high-converting video ad hooks from Serplive Solutions.",
                    description = "Agency case study breakdown for local healthcare & service brands.",
                    hashtags = "#DigitalMarketing #SerpliveSolutions #PerformanceMarketing",
                    keywords = "meta ads hooks, digital marketing agency india",
                    scriptCode = "SCR-199",
                    publishDateKey = todayKey,
                    publishTime = "07:30 PM",
                    status = "SCHEDULED",
                    checklistVerified = true,
                    viewsCount = 0
                )
            )
        )

        db.opsDao().insertAllFinancialRecords(
            listOf(
                FinancialRecordEntity(
                    recordType = "INVESTMENT",
                    category = "INITIAL_INVESTMENT",
                    title = "18-Month Seed Capital Tranche 1 (Avishek)",
                    amountInr = 2500000.0,
                    monthKey = lastMonthKey,
                    recordDateKey = "$lastMonthKey-01",
                    enteredBy = "Sandeep",
                    notes = "Initial studio setup, camera gear & 18-month runway pool."
                ),
                FinancialRecordEntity(
                    recordType = "EXPENSE",
                    category = "EQUIPMENT",
                    title = "Sony FX30 Cinema Line + Studio Lighting Rig",
                    amountInr = 385000.0,
                    monthKey = lastMonthKey,
                    recordDateKey = "$lastMonthKey-08",
                    enteredBy = "Sandeep",
                    notes = "Studio A & Studio B multi-cam setup."
                ),
                FinancialRecordEntity(
                    recordType = "EXPENSE",
                    category = "OFFICE_RENT",
                    title = "Serplive Studio HQ Monthly Lease",
                    amountInr = 65000.0,
                    monthKey = thisMonthKey,
                    recordDateKey = "$thisMonthKey-01",
                    enteredBy = "Sandeep",
                    notes = "Includes Studio A, Studio B, Audio Booth & Editing Bay."
                ),
                FinancialRecordEntity(
                    recordType = "EXPENSE",
                    category = "EMPLOYEE_PAYMENTS",
                    title = "Production, Anchoring & Editing Payroll",
                    amountInr = 215000.0,
                    monthKey = thisMonthKey,
                    recordDateKey = "$thisMonthKey-05",
                    enteredBy = "Sandeep",
                    notes = "Monthly team compensation & contributor honorariums."
                ),
                FinancialRecordEntity(
                    recordType = "EXPENSE",
                    category = "SOFTWARE",
                    title = "Adobe CC, AI Studio Tooling & Stock Assets",
                    amountInr = 28500.0,
                    monthKey = thisMonthKey,
                    recordDateKey = "$thisMonthKey-06",
                    enteredBy = "Sandeep",
                    notes = "Team editing & prompt licenses."
                ),
                FinancialRecordEntity(
                    recordType = "EXPENSE",
                    category = "PRODUCTION",
                    title = "Art & Craft Supplies + Set Props",
                    amountInr = 18200.0,
                    monthKey = thisMonthKey,
                    recordDateKey = "$thisMonthKey-10",
                    enteredBy = "Sandeep",
                    notes = "Terracotta, paints, mirrors & studio props."
                ),
                FinancialRecordEntity(
                    recordType = "REVENUE",
                    category = "AGENCY_REVENUE",
                    title = "Serplive Solutions Retainer & Client Video Billing",
                    amountInr = 340000.0,
                    monthKey = thisMonthKey,
                    recordDateKey = "$thisMonthKey-12",
                    enteredBy = "Sandeep",
                    notes = "3 active agency retainer accounts."
                ),
                FinancialRecordEntity(
                    recordType = "REVENUE",
                    category = "CHANNEL_REVENUE",
                    title = "YouTube AdSense & Brand Integration Payout",
                    amountInr = 128000.0,
                    monthKey = thisMonthKey,
                    recordDateKey = "$thisMonthKey-14",
                    enteredBy = "Sandeep",
                    notes = "Combined payout across 5 owned channels."
                )
            )
        )

        db.opsDao().insertAllNotifications(
            listOf(
                NotificationEntity(
                    recipientUserId = "ALL",
                    title = "Task Overdue Alert: TSK-103",
                    message = "Client Promo Reels for Apex Dental missed deadline. Delay reason logged by Editor 2.",
                    category = "TASK"
                ),
                NotificationEntity(
                    recipientUserId = "usr_panditji",
                    title = "Today's Recording Schedule: 11:00 AM",
                    message = "Devutthana Ekadashi Vrat Katha in Audio Booth. Arrival at 10:45 AM.",
                    category = "SHOOT"
                ),
                NotificationEntity(
                    recipientUserId = "usr_manoj",
                    title = "Revision Requested on SCR-204",
                    message = "Stepwells Documentary script needs ASI citation tag in Act 1.",
                    category = "SCRIPT"
                ),
                NotificationEntity(
                    recipientUserId = "usr_sandeep",
                    title = "Late Arrival Detected: Kritika",
                    message = "Checked in 22 mins after shift start via Dynamic QR verification.",
                    category = "ATTENDANCE"
                )
            )
        )
    }
}
