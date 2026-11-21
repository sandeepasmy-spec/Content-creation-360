package com.example.firebase

import com.example.data.model.UserRole
import com.example.util.GeofenceHelper
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FirestoreWorkspaceSeeder {

    suspend fun ensureSeeded(
        db: FirebaseFirestore,
        uid: String,
        email: String?,
        displayName: String?
    ): Result<Unit> {
        val userDocRef = db.collection("users").document(uid)
        return try {
            val userSnap = userDocRef.get().await()
            if (!userSnap.exists()) {
                val userDoc = WorkspaceUserDoc(
                    userId = uid,
                    email = (email ?: "$uid@serplivesolutions.com").take(200),
                    displayName = (displayName ?: "Sandeep").take(120),
                    activePersonaId = "usr_sandeep",
                    activePortal = "HIGHER_AUTHORITY"
                )
                userDocRef.set(userDoc.toCreateMap()).await()
            }

            val channelsSnap = userDocRef.collection("channels").limit(1).get().await()
            if (!channelsSnap.isEmpty) {
                return Result.success(Unit)
            }

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

            val batch = db.batch()
            fun tsMap(base: Map<String, Any?>): Map<String, Any> {
                val out = HashMap<String, Any>()
                base.forEach { (k, v) -> if (v != null) out[k] = v }
                out["userId"] = uid
                out["createdAt"] = FieldValue.serverTimestamp()
                out["updatedAt"] = FieldValue.serverTimestamp()
                return out
            }

            // 1. Studio Members (10 Roles)
            data class MemberSeed(
                val id: String,
                val username: String,
                val fullName: String,
                val designation: String,
                val department: String,
                val role: UserRole,
                val channels: String,
                val initials: String,
                val colorHex: Long,
                val email: String
            )
            val members = listOf(
                MemberSeed("usr_sandeep", "sandeep", "Sandeep", "Owner & Operations Head", "Executive Operations", UserRole.SUPER_ADMIN, "CH_ART_CRAFT,CH_VRAT_KATHA,CH_TECH,CH_SERPLIVE,CH_DOCUMENTARY", "SA", 0xFF2563EB, "sandeep@serplivesolutions.com"),
                MemberSeed("usr_sudeep", "sudeep", "Sudeep", "Strategic Director (12 Yrs Exp)", "Content Strategy", UserRole.STRATEGIC_DIRECTOR, "CH_ART_CRAFT,CH_VRAT_KATHA,CH_TECH,CH_SERPLIVE,CH_DOCUMENTARY", "SU", 0xFF6366F1, "sudeep@serplivesolutions.com"),
                MemberSeed("usr_avishek", "avishek", "Avishek", "Lead Investor (18-Month Term)", "Capital & Governance", UserRole.INVESTOR, "CH_ART_CRAFT,CH_VRAT_KATHA,CH_TECH,CH_SERPLIVE,CH_DOCUMENTARY", "AV", 0xFF10B981, "avishek@investor.serplivesolutions.com"),
                MemberSeed("usr_parchi", "parchi", "Parchi", "Anchor & Production Coordinator", "Anchoring & Production", UserRole.COORDINATOR_ANCHOR, "CH_TECH,CH_SERPLIVE,CH_DOCUMENTARY", "PA", 0xFFEC4899, "parchi@serplivesolutions.com"),
                MemberSeed("usr_kritika", "kritika", "Kritika", "Anchor & Production Coordinator", "Anchoring & Production", UserRole.COORDINATOR_ANCHOR, "CH_ART_CRAFT,CH_VRAT_KATHA,CH_TECH,CH_SERPLIVE,CH_DOCUMENTARY", "KR", 0xFF8B5CF6, "kritika@serplivesolutions.com"),
                MemberSeed("usr_manoj", "manoj", "Manoj", "Strategic Partner & AI Prompt Lead", "AI Content Studio", UserRole.AI_PARTNER, "CH_ART_CRAFT,CH_VRAT_KATHA,CH_TECH,CH_SERPLIVE,CH_DOCUMENTARY", "MA", 0xFF06B6D4, "manoj@serplivesolutions.com"),
                MemberSeed("usr_editor1", "editor1", "Editor 1 (Aarav)", "Senior Video Editor & AI Visuals", "Post-Production", UserRole.VIDEO_EDITOR, "CH_TECH,CH_SERPLIVE,CH_DOCUMENTARY", "E1", 0xFFF59E0B, "editor1@serplivesolutions.com"),
                MemberSeed("usr_editor2", "editor2", "Editor 2 (Rohan)", "Video Editor & Client Publisher", "Post-Production", UserRole.VIDEO_EDITOR, "CH_ART_CRAFT,CH_VRAT_KATHA,CH_SERPLIVE", "E2", 0xFFF97316, "editor2@serplivesolutions.com"),
                MemberSeed("usr_panditji", "panditji", "Pandit Ji", "Vrat Katha Narrator (External)", "External Talent", UserRole.EXTERNAL_PANDIT, "CH_VRAT_KATHA", "PJ", 0xFFEA580C, "panditji@external.serplive"),
                MemberSeed("usr_artteacher", "artteacher", "Art & Craft Teacher", "Craft Instructor (External)", "External Talent", UserRole.EXTERNAL_ART_TEACHER, "CH_ART_CRAFT", "AC", 0xFFDB2777, "craftteacher@external.serplive")
            )
            members.forEach { m ->
                batch.set(
                    userDocRef.collection("studio_members").document(m.id),
                    tsMap(
                        mapOf(
                            "username" to m.username,
                            "fullName" to m.fullName,
                            "designation" to m.designation,
                            "department" to m.department,
                            "role" to m.role.name,
                            "assignedChannelsCsv" to m.channels,
                            "avatarInitials" to m.initials,
                            "avatarColorHex" to m.colorHex,
                            "accountStatus" to "ACTIVE",
                            "lastLoginAt" to now - dayMs,
                            "email" to m.email
                        )
                    )
                )
            }

            // 2. Five Isolated Channels
            val channels = listOf(
                Triple("CH_ART_CRAFT", "Art and Craft", 0xFFEC4899L) to mapOf("categoryTag" to "DIY & Studio Projects", "description" to "Studio craft demonstrations by Art & Craft Teacher with material prep, raw shoot tracking & thumbnails.", "leadCoordinator" to "Kritika", "monthlyVideoTarget" to 16L, "videosProduced" to 11L, "videosPublished" to 9L, "growthPercent" to 18.4, "subscribersLabel" to "42.8K"),
                Triple("CH_VRAT_KATHA", "Vrat Katha", 0xFFF97316L) to mapOf("categoryTag" to "Devotional Narration", "description" to "Authentic voice narrations by Pandit Ji with ritual calendar alignment, script verification & studio audio.", "leadCoordinator" to "Kritika", "monthlyVideoTarget" to 20L, "videosProduced" to 15L, "videosPublished" to 13L, "growthPercent" to 24.2, "subscribersLabel" to "68.5K"),
                Triple("CH_TECH", "Technology Channel", 0xFF06B6D4L) to mapOf("categoryTag" to "Tech Reviews & AI Guides", "description" to "Anchored by Parchi & Kritika covering research-verified tech tutorials, AI visuals & high-CTR thumbnails.", "leadCoordinator" to "Parchi", "monthlyVideoTarget" to 14L, "videosProduced" to 10L, "videosPublished" to 8L, "growthPercent" to 31.6, "subscribersLabel" to "54.1K"),
                Triple("CH_SERPLIVE", "Serplive Solutions", 0xFF2563EBL) to mapOf("categoryTag" to "Digital Marketing Agency", "description" to "Agency thought-leadership, client campaign videos, promotional reels & multi-platform lead tracking.", "leadCoordinator" to "Parchi", "monthlyVideoTarget" to 18L, "videosProduced" to 12L, "videosPublished" to 10L, "growthPercent" to 22.0, "subscribersLabel" to "29.4K"),
                Triple("CH_DOCUMENTARY", "Documentary Channel", 0xFF8B5CF6L) to mapOf("categoryTag" to "Deep-Dive Research", "description" to "Fact-checked storytelling anchored by Parchi & Kritika with voice-over, archival graphics & post-production.", "leadCoordinator" to "Parchi", "monthlyVideoTarget" to 8L, "videosProduced" to 5L, "videosPublished" to 4L, "growthPercent" to 27.5, "subscribersLabel" to "37.2K")
            )
            channels.forEach { (meta, extra) ->
                val (cid, cname, accent) = meta
                batch.set(
                    userDocRef.collection("channels").document(cid),
                    tsMap(extra + mapOf("name" to cname, "accentColorHex" to accent))
                )
            }

            // 3. Workspace Items
            val wsItems = listOf(
                "ws_1" to mapOf("channelId" to "CH_ART_CRAFT", "itemType" to "CRAFT_IDEA", "title" to "Terracotta Diwali Lantern & Mirror Mosaic", "details" to "Step-by-step studio demo focusing on overhead macro shots.", "metaBadge" to "Approved Idea", "completed" to true, "ownerName" to "Art & Craft Teacher", "updatedAtMillis" to now),
                "ws_2" to mapOf("channelId" to "CH_ART_CRAFT", "itemType" to "MATERIAL_SHOPPING", "title" to "Acrylic Metallic Paints (12-color set) & Craft Mirrors", "details" to "Required: 4 sets metallic acrylics, 200g diamond mirrors, Fevicryl glue.", "metaBadge" to "Purchase Pending", "completed" to false, "ownerName" to "Kritika", "updatedAtMillis" to now),
                "ws_3" to mapOf("channelId" to "CH_TECH", "itemType" to "TECH_RESEARCH", "title" to "2026 Smartphone Camera Sensor Benchmarks", "details" to "Verified specs comparison table & low-light sample test cards.", "metaBadge" to "Fact-Checked", "completed" to true, "ownerName" to "Parchi", "updatedAtMillis" to now),
                "ws_4" to mapOf("channelId" to "CH_SERPLIVE", "itemType" to "CLIENT_LEAD", "title" to "Apex Dental Clinic — Performance Video Ad Series", "details" to "3 promotional reels + YouTube explainer. Client feedback pending on Cut v2.", "metaBadge" to "Active Client", "completed" to false, "ownerName" to "Editor 2 (Rohan)", "updatedAtMillis" to now),
                "ws_5" to mapOf("channelId" to "CH_DOCUMENTARY", "itemType" to "DOC_REFERENCE", "title" to "Ancient Stepwells of Western India — Archival Sources", "details" to "ASI architectural survey citations & 19th-century gazetteer cross-check.", "metaBadge" to "Verified Source", "completed" to true, "ownerName" to "Kritika", "updatedAtMillis" to now)
            )
            wsItems.forEach { (id, m) ->
                batch.set(userDocRef.collection("workspace_items").document(id), tsMap(m))
            }

            // 4. Office Config
            batch.set(
                userDocRef.collection("office_configs").document("hq_main"),
                tsMap(
                    mapOf(
                        "officeName" to "Serplive Solutions Studio HQ",
                        "officeLat" to 28.6139,
                        "officeLng" to 77.2090,
                        "geofenceRadiusMeters" to 150.0,
                        "authorizedWifiSsid" to "Serplive_Studio_5G",
                        "workStartHour" to 10L,
                        "workStartMinute" to 0L,
                        "workEndHour" to 19L,
                        "workEndMinute" to 0L,
                        "currentQrToken" to GeofenceHelper.generateDynamicQrToken(now),
                        "qrUpdatedAt" to now
                    )
                )
            )

            // 5. Attendance & Audit
            val attSeeds = listOf(
                "att_parchi" to mapOf("memberId" to "usr_parchi", "userName" to "Parchi", "dateKey" to todayKey, "checkInTimeMillis" to (now - 4 * 3600_000L), "verificationMethod" to "GPS_GEOFENCE", "latitude" to 28.6139, "longitude" to 77.2090, "distanceMeters" to 18.0, "wifiSsid" to "Serplive_Studio_5G", "lateArrival" to false, "earlyDeparture" to false, "workingMinutes" to 240L, "status" to "PRESENT", "correctionReason" to "", "correctionRequested" to false, "correctionApprovedBy" to ""),
                "att_kritika" to mapOf("memberId" to "usr_kritika", "userName" to "Kritika", "dateKey" to todayKey, "checkInTimeMillis" to (now - 3 * 3600_000L), "verificationMethod" to "DYNAMIC_QR", "latitude" to 28.6140, "longitude" to 77.2091, "distanceMeters" to 24.0, "wifiSsid" to "Serplive_Studio_5G", "lateArrival" to true, "earlyDeparture" to false, "workingMinutes" to 180L, "status" to "LATE", "correctionReason" to "", "correctionRequested" to false, "correctionApprovedBy" to ""),
                "att_manoj" to mapOf("memberId" to "usr_manoj", "userName" to "Manoj", "dateKey" to todayKey, "checkInTimeMillis" to (now - 4 * 3600_000L), "verificationMethod" to "WIFI_SSID", "latitude" to 28.6138, "longitude" to 77.2089, "distanceMeters" to 12.0, "wifiSsid" to "Serplive_Studio_5G", "lateArrival" to false, "earlyDeparture" to false, "workingMinutes" to 250L, "status" to "PRESENT", "correctionReason" to "", "correctionRequested" to false, "correctionApprovedBy" to ""),
                "att_editor1" to mapOf("memberId" to "usr_editor1", "userName" to "Editor 1 (Aarav)", "dateKey" to yesterdayKey, "checkInTimeMillis" to (now - dayMs - 3 * 3600_000L), "checkOutTimeMillis" to (now - dayMs + 6 * 3600_000L), "verificationMethod" to "GPS_GEOFENCE", "latitude" to 28.6139, "longitude" to 77.2090, "distanceMeters" to 15.0, "wifiSsid" to "Serplive_Studio_5G", "lateArrival" to false, "earlyDeparture" to false, "workingMinutes" to 540L, "status" to "PRESENT", "correctionReason" to "", "correctionRequested" to false, "correctionApprovedBy" to "")
            )
            attSeeds.forEach { (id, m) ->
                batch.set(userDocRef.collection("attendance").document(id), tsMap(m))
            }

            batch.set(
                userDocRef.collection("attendance_audits").document("audit_1"),
                tsMap(
                    mapOf(
                        "targetUserName" to "Kritika",
                        "action" to "LATE_CHECK_IN_LOGGED",
                        "performedBy" to "System Geofence",
                        "details" to "Checked in via Dynamic QR 22 mins after 10:00 AM shift start.",
                        "timestampMillis" to (now - 3 * 3600_000L)
                    )
                )
            )

            // 6. Tasks
            val taskSeeds = listOf(
                "tsk_101" to mapOf("taskCode" to "TSK-101", "title" to "Edit AI Workflows Documentary Cut v1", "description" to "Assemble A-roll narration with animated timeline graphics and sound design.", "instructions" to "Keep pacing crisp under 14 minutes. Export 4K H.264 master.", "channelId" to "CH_DOCUMENTARY", "channelName" to "Documentary Channel", "category" to "EDIT", "assignedToIdsCsv" to "usr_editor1", "assignedToNamesCsv" to "Editor 1 (Aarav)", "createdById" to "usr_sandeep", "priority" to "HIGH", "status" to "IN_PROGRESS", "reviewStatus" to "PENDING", "startDateMillis" to (now - dayMs), "deadlineMillis" to (now + 6 * 3600_000L), "expectedMinutes" to 240L, "actualMinutes" to 145L, "delayReason" to "", "managerComments" to "Focus on lower-third fact callouts in chapter 2.", "complexityWeight" to 4L, "attachedFileName" to "SCR-204_Doc_Script.pdf", "submissionNote" to "", "submissionFileName" to ""),
                "tsk_102" to mapOf("taskCode" to "TSK-102", "title" to "Generate 6 Midjourney/AI Visual Prompts for Tech Ep 18", "description" to "Create photorealistic AI image + image-to-video motion prompts for B-roll.", "instructions" to "Deliver prompts ready for Editor 1 in Manoj AI Workspace.", "channelId" to "CH_TECH", "channelName" to "Technology Channel", "category" to "AI_PROMPT", "assignedToIdsCsv" to "usr_manoj", "assignedToNamesCsv" to "Manoj", "createdById" to "usr_parchi", "priority" to "URGENT", "status" to "ASSIGNED", "reviewStatus" to "PENDING", "startDateMillis" to now, "deadlineMillis" to (now + 4 * 3600_000L), "expectedMinutes" to 90L, "actualMinutes" to 0L, "delayReason" to "", "managerComments" to "", "complexityWeight" to 3L, "attachedFileName" to "", "submissionNote" to "", "submissionFileName" to ""),
                "tsk_103" to mapOf("taskCode" to "TSK-103", "title" to "Client Promo Reels — Apex Dental Package", "description" to "Deliver 2 vertical 9:16 Instagram reels and coordinate client sign-off.", "instructions" to "Use Serplive Solutions brand outro and subtitles.", "channelId" to "CH_SERPLIVE", "channelName" to "Serplive Solutions", "category" to "CLIENT_WORK", "assignedToIdsCsv" to "usr_editor2", "assignedToNamesCsv" to "Editor 2 (Rohan)", "createdById" to "usr_sandeep", "priority" to "HIGH", "status" to "OVERDUE", "reviewStatus" to "ACTION_NEEDED", "startDateMillis" to (now - 3 * dayMs), "deadlineMillis" to (now - 5 * 3600_000L), "expectedMinutes" to 180L, "actualMinutes" to 150L, "delayReason" to "Awaiting revised clinic logo PNG from client.", "managerComments" to "Follow up with client today and submit draft by 5 PM.", "complexityWeight" to 3L, "attachedFileName" to "", "submissionNote" to "", "submissionFileName" to ""),
                "tsk_104" to mapOf("taskCode" to "TSK-104", "title" to "Verify Ekadashi Vrat Katha Script & Pronunciation Notes", "description" to "Review Sanskrit shloka cues and mark breathing pauses for Pandit Ji.", "instructions" to "Coordinate studio audio check before 11 AM recording.", "channelId" to "CH_VRAT_KATHA", "channelName" to "Vrat Katha", "category" to "SCRIPT", "assignedToIdsCsv" to "usr_kritika", "assignedToNamesCsv" to "Kritika", "createdById" to "usr_sandeep", "priority" to "MEDIUM", "status" to "SUBMITTED_FOR_REVIEW", "reviewStatus" to "AWAITING_APPROVAL", "startDateMillis" to (now - dayMs), "deadlineMillis" to (now + dayMs), "expectedMinutes" to 75L, "actualMinutes" to 65L, "delayReason" to "", "managerComments" to "", "complexityWeight" to 2L, "attachedFileName" to "", "submissionNote" to "Uploaded verified script v2 with pause markers.", "submissionFileName" to "Ekadashi_Katha_v2.docx"),
                "tsk_105" to mapOf("taskCode" to "TSK-105", "title" to "Anchor & Shoot Tech Tutorial: Local AI Productivity Stack", "description" to "Studio A shoot with Parchi anchoring and live screen overlay markers.", "instructions" to "Confirm teleprompter script SCR-202 is locked before shoot.", "channelId" to "CH_TECH", "channelName" to "Technology Channel", "category" to "SHOOT", "assignedToIdsCsv" to "usr_parchi", "assignedToNamesCsv" to "Parchi", "createdById" to "usr_sandeep", "priority" to "HIGH", "status" to "COMPLETED", "reviewStatus" to "APPROVED", "startDateMillis" to (now - 2 * dayMs), "deadlineMillis" to (now - dayMs), "expectedMinutes" to 120L, "actualMinutes" to 110L, "completedAtMillis" to (now - dayMs - 2 * 3600_000L), "delayReason" to "", "managerComments" to "Great energy and crisp delivery.", "complexityWeight" to 3L, "attachedFileName" to "", "submissionNote" to "", "submissionFileName" to "")
            )
            taskSeeds.forEach { (id, m) ->
                batch.set(userDocRef.collection("tasks").document(id), tsMap(m))
            }

            // 7. Scripts & Versions
            val scriptSeeds = listOf(
                "scr_201" to mapOf("scriptCode" to "SCR-201", "channelId" to "CH_VRAT_KATHA", "channelName" to "Vrat Katha", "videoTitle" to "Devutthana Ekadashi Vrat Katha & Puja Vidhi", "writerId" to "usr_kritika", "writerName" to "Kritika", "anchorName" to "Pandit Ji", "editorName" to "Editor 2 (Rohan)", "deadlineMillis" to (now + dayMs), "status" to "SHOOTING_READY", "currentVersion" to 2L, "contentBody" to "Intro Invocation: Om Namo Bhagavate Vasudevaya...\n\nSection 1: Significance of Kartik Shukla Ekadashi...\nSection 2: Complete Katha Narration with calm 2-second pauses between shlokas.\nClosing Blessing & Channel Subscribe Appeal.", "reviewerComments" to "Approved by Sudeep & Sandeep. Ready for Pandit Ji audio recording.", "highlightedErrors" to "", "factCheckVerified" to true, "attachmentName" to "Ekadashi_Katha_Final.pdf", "createdAtMillis" to now, "updatedAtMillis" to now),
                "scr_202" to mapOf("scriptCode" to "SCR-202", "channelId" to "CH_TECH", "channelName" to "Technology Channel", "videoTitle" to "Top 5 AI Video Production Tools for Creators in 2026", "writerId" to "usr_parchi", "writerName" to "Parchi", "anchorName" to "Parchi", "editorName" to "Editor 1 (Aarav)", "deadlineMillis" to (now + 8 * 3600_000L), "status" to "VERIFICATION", "currentVersion" to 2L, "contentBody" to "HOOK (0:00): Still spending 8 hours editing B-roll manually?\n\nCHAPTER 1: AI Script Structuring\nCHAPTER 2: Keyframe Image-to-Video\nCHAPTER 3: Automated Color & Sound\nOUTRO: Comment 'STUDIO' for prompt pack.", "reviewerComments" to "Verify pricing tier slide in Chapter 2 before final approval.", "highlightedErrors" to "Line 14: Confirm monthly price for Tool #3.", "factCheckVerified" to false, "attachmentName" to "Tech_AI_Tools_v2.docx", "createdAtMillis" to now, "updatedAtMillis" to now),
                "scr_203" to mapOf("scriptCode" to "SCR-203", "channelId" to "CH_ART_CRAFT", "channelName" to "Art and Craft", "videoTitle" to "Handmade Terracotta Mirror Mosaic Wall Hanging", "writerId" to "usr_kritika", "writerName" to "Kritika", "anchorName" to "Art & Craft Teacher", "editorName" to "Editor 2 (Rohan)", "deadlineMillis" to (now + 2 * dayMs), "status" to "APPROVED", "currentVersion" to 1L, "contentBody" to "Tabletop Setup: Overhead 4K camera + side detail cam.\nStep 1: Base primer coat (dry 10 mins).\nStep 2: Geometric chalk grid.\nStep 3: Mirror placement & metallic border finish.", "reviewerComments" to "Material list verified with store inventory.", "highlightedErrors" to "", "factCheckVerified" to true, "attachmentName" to "Terracotta_Mosaic_Guide.pdf", "createdAtMillis" to now, "updatedAtMillis" to now),
                "scr_204" to mapOf("scriptCode" to "SCR-204", "channelId" to "CH_DOCUMENTARY", "channelName" to "Documentary Channel", "videoTitle" to "How Ancient Indian Stepwells Engineered Monsoon Water", "writerId" to "usr_manoj", "writerName" to "Manoj", "anchorName" to "Parchi", "editorName" to "Editor 1 (Aarav)", "deadlineMillis" to (now + 3 * dayMs), "status" to "REVISION_REQUIRED", "currentVersion" to 1L, "contentBody" to "Prologue: Rani ki Vav aerial CGI opener.\nAct 1: Subterranean thermal cooling.\nAct 2: Stone masonry & aquifer recharge.", "reviewerComments" to "Add architectural depth metrics from ASI survey citation.", "highlightedErrors" to "Act 1 timestamp 03:20 needs citation source tag.", "factCheckVerified" to false, "attachmentName" to "Stepwells_Draft_v1.docx", "createdAtMillis" to now, "updatedAtMillis" to now)
            )
            scriptSeeds.forEach { (id, m) ->
                batch.set(userDocRef.collection("scripts").document(id), tsMap(m))
            }

            val verSeeds = listOf(
                "sver_1" to mapOf("scriptDocId" to "scr_201", "versionNumber" to 1L, "contentSnapshot" to "Initial Katha draft without pause cues.", "editedBy" to "Kritika", "changeNote" to "Initial draft", "createdAtMillis" to now),
                "sver_2" to mapOf("scriptDocId" to "scr_201", "versionNumber" to 2L, "contentSnapshot" to "Added shloka pronunciation guide & 2-second studio pause cues.", "editedBy" to "Kritika", "changeNote" to "Added pronunciation cues per review", "createdAtMillis" to now),
                "sver_3" to mapOf("scriptDocId" to "scr_202", "versionNumber" to 1L, "contentSnapshot" to "Outline + 5 tool list.", "editedBy" to "Parchi", "changeNote" to "Initial tech script", "createdAtMillis" to now),
                "sver_4" to mapOf("scriptDocId" to "scr_202", "versionNumber" to 2L, "contentSnapshot" to "Updated hook & added CTA prompt.", "editedBy" to "Parchi", "changeNote" to "Strengthened 15s hook", "createdAtMillis" to now)
            )
            verSeeds.forEach { (id, m) ->
                batch.set(userDocRef.collection("script_versions").document(id), tsMap(m))
            }

            // 8. Calendar & Shooting Schedules
            val calSeeds = listOf(
                "cal_1" to mapOf("channelId" to "CH_VRAT_KATHA", "channelName" to "Vrat Katha", "contentTitle" to "Devutthana Ekadashi Katha", "category" to "Devotional Narration", "scriptWriter" to "Kritika", "anchorName" to "Pandit Ji", "editorName" to "Editor 2 (Rohan)", "shootingDateKey" to todayKey, "shootingTimeSlot" to "11:00 - 12:30", "editingDeadlineKey" to tomorrowKey, "publishingDateKey" to dayAfterKey, "publishingTime" to "06:30 AM", "platform" to "YouTube", "status" to "Shooting Today", "studioRoom" to "Audio Booth"),
                "cal_2" to mapOf("channelId" to "CH_TECH", "channelName" to "Technology Channel", "contentTitle" to "Top 5 AI Video Tools 2026", "category" to "Tech Review", "scriptWriter" to "Parchi", "anchorName" to "Parchi", "editorName" to "Editor 1 (Aarav)", "shootingDateKey" to todayKey, "shootingTimeSlot" to "14:00 - 16:00", "editingDeadlineKey" to dayAfterKey, "publishingDateKey" to dayAfterKey, "publishingTime" to "06:00 PM", "platform" to "YouTube", "status" to "Scheduled", "studioRoom" to "Studio A"),
                "cal_3" to mapOf("channelId" to "CH_ART_CRAFT", "channelName" to "Art and Craft", "contentTitle" to "Terracotta Mirror Mosaic", "category" to "Craft Demo", "scriptWriter" to "Kritika", "anchorName" to "Art & Craft Teacher", "editorName" to "Editor 2 (Rohan)", "shootingDateKey" to tomorrowKey, "shootingTimeSlot" to "11:30 - 13:30", "editingDeadlineKey" to dayAfterKey, "publishingDateKey" to dayAfterKey, "publishingTime" to "04:00 PM", "platform" to "YouTube", "status" to "Prep Ready", "studioRoom" to "Studio B"),
                "cal_4" to mapOf("channelId" to "CH_SERPLIVE", "channelName" to "Serplive Solutions", "contentTitle" to "B2B Performance Funnel Breakdown", "category" to "Agency Education", "scriptWriter" to "Parchi", "anchorName" to "Kritika", "editorName" to "Editor 1 (Aarav)", "shootingDateKey" to tomorrowKey, "shootingTimeSlot" to "15:00 - 16:30", "editingDeadlineKey" to dayAfterKey, "publishingDateKey" to dayAfterKey, "publishingTime" to "07:30 PM", "platform" to "Instagram", "status" to "Scheduled", "studioRoom" to "Studio A")
            )
            calSeeds.forEach { (id, m) ->
                batch.set(userDocRef.collection("calendar_entries").document(id), tsMap(m))
            }

            val shootSeeds = listOf(
                "shoot_1" to mapOf("scheduleMode" to "PANDIT_JI", "channelId" to "CH_VRAT_KATHA", "channelName" to "Vrat Katha", "topicOrKathaName" to "Devutthana Ekadashi Vrat Katha", "scriptName" to "SCR-201", "talentId" to "usr_panditji", "talentName" to "Pandit Ji", "coordinatorName" to "Kritika", "editorName" to "Editor 2 (Rohan)", "shootDateKey" to todayKey, "arrivalTime" to "10:45 AM", "startTime" to "11:00", "expectedEndTime" to "12:30", "actualEndTime" to "", "studioRoom" to "Audio Booth", "specialRequirements" to "Warm water, printed large-font Hindi script, condenser mic pop filter.", "materialsRequired" to "", "materialsAvailable" to true, "purchaseRequired" to "", "recordingStatus" to "SCHEDULED"),
                "shoot_2" to mapOf("scheduleMode" to "ART_CRAFT_TEACHER", "channelId" to "CH_ART_CRAFT", "channelName" to "Art and Craft", "topicOrKathaName" to "Terracotta Mirror Mosaic Wall Art", "scriptName" to "SCR-203", "talentId" to "usr_artteacher", "talentName" to "Art & Craft Teacher", "coordinatorName" to "Kritika", "editorName" to "Editor 2 (Rohan)", "shootDateKey" to tomorrowKey, "arrivalTime" to "11:15 AM", "startTime" to "11:30", "expectedEndTime" to "13:30", "actualEndTime" to "", "studioRoom" to "Studio B", "specialRequirements" to "Overhead macro rig + warm tabletop lights.", "materialsRequired" to "Terracotta plate (2), Acrylic Paints (12), Craft Mirrors (200g), Glue (2)", "materialsAvailable" to false, "purchaseRequired" to "Buy 12-color metallic acrylic set & 200g craft mirrors.", "recordingStatus" to "SCHEDULED"),
                "shoot_3" to mapOf("scheduleMode" to "ANCHOR", "channelId" to "CH_TECH", "channelName" to "Technology Channel", "topicOrKathaName" to "Top 5 AI Video Production Tools", "scriptName" to "SCR-202", "talentId" to "usr_parchi", "talentName" to "Parchi", "coordinatorName" to "Parchi", "editorName" to "Editor 1 (Aarav)", "shootDateKey" to todayKey, "arrivalTime" to "01:45 PM", "startTime" to "14:00", "expectedEndTime" to "16:00", "actualEndTime" to "", "studioRoom" to "Studio A", "specialRequirements" to "Smart casual navy blazer, teleprompter loaded with SCR-202, demo laptop.", "materialsRequired" to "", "materialsAvailable" to true, "purchaseRequired" to "", "recordingStatus" to "SCHEDULED")
            )
            shootSeeds.forEach { (id, m) ->
                batch.set(userDocRef.collection("shooting_schedules").document(id), tsMap(m))
            }

            // 9. AI Assignments, Files, Social Posts, Financial Records, Notifications
            batch.set(
                userDocRef.collection("ai_assignments").document("ai_301"),
                tsMap(mapOf("assignmentCode" to "AI-301", "channelId" to "CH_DOCUMENTARY", "channelName" to "Documentary Channel", "promptType" to "IMAGE_TO_VIDEO_PROMPT", "title" to "Stepwell Ancient Sunlight Shafts Motion Prompts", "brief" to "Generate 4 image-to-video motion prompts for 3D animated archival stills.", "deadlineMillis" to (now + 5 * 3600_000L), "assignedAtMillis" to (now - 3 * 3600_000L), "startedAtMillis" to (now - 2 * 3600_000L), "timeTakenMinutes" to 42L, "generatedPromptText" to "Slow vertical crane descent through sunlit carved sandstone arches, subtle atmospheric dust motes, calm cinematic 24fps.", "attachedAssetName" to "", "status" to "IN_PROGRESS", "reviewerNote" to ""))
            )
            batch.set(
                userDocRef.collection("ai_assignments").document("ai_302"),
                tsMap(mapOf("assignmentCode" to "AI-302", "channelId" to "CH_TECH", "channelName" to "Technology Channel", "promptType" to "IMAGE_PROMPT", "title" to "Cyber-Clean Desk Setup Thumbnail Visuals", "brief" to "High-contrast 16:9 thumbnail background concepts without text overlays.", "deadlineMillis" to (now + dayMs), "assignedAtMillis" to (now - dayMs), "startedAtMillis" to (now - dayMs + 3600_000L), "submittedAtMillis" to (now - dayMs + 2 * 3600_000L), "timeTakenMinutes" to 55L, "generatedPromptText" to "Minimalist obsidian desk, glowing cyan edge light, floating glass UI card silhouette, 85mm shallow depth of field.", "attachedAssetName" to "tech_thumb_prompts_v1.txt", "status" to "APPROVED", "reviewerNote" to "Strong visual framing — approved for Editor 1."))
            )

            val fileSeeds = listOf(
                "file_1" to mapOf("channelId" to "CH_VRAT_KATHA", "channelName" to "Vrat Katha", "projectName" to "Kartik Month Series", "contentTitle" to "Devutthana Ekadashi Katha", "fileType" to "SCRIPT", "fileName" to "Ekadashi_Katha_Final_v2.pdf", "uploadedBy" to "Kritika", "uploadedById" to "usr_kritika", "uploadDateMillis" to now, "version" to "v2.0", "fileSizeLabel" to "1.4 MB", "approvalStatus" to "APPROVED", "contentUri" to ""),
                "file_2" to mapOf("channelId" to "CH_ART_CRAFT", "channelName" to "Art and Craft", "projectName" to "Festive Decor Batch", "contentTitle" to "Terracotta Mirror Mosaic", "fileType" to "SHOOTING_INSTRUCTIONS", "fileName" to "Terracotta_ShotList_&_Materials.pdf", "uploadedBy" to "Kritika", "uploadedById" to "usr_kritika", "uploadDateMillis" to now, "version" to "v1.0", "fileSizeLabel" to "860 KB", "approvalStatus" to "APPROVED", "contentUri" to ""),
                "file_3" to mapOf("channelId" to "CH_DOCUMENTARY", "channelName" to "Documentary Channel", "projectName" to "Heritage Engineering", "contentTitle" to "Ancient Stepwells", "fileType" to "VIDEO_FILE", "fileName" to "Stepwells_RoughCut_v1.mp4", "uploadedBy" to "Editor 1 (Aarav)", "uploadedById" to "usr_editor1", "uploadDateMillis" to now, "version" to "v1.2", "fileSizeLabel" to "412 MB", "approvalStatus" to "PENDING", "contentUri" to ""),
                "file_4" to mapOf("channelId" to "CH_TECH", "channelName" to "Technology Channel", "projectName" to "AI Creators 2026", "contentTitle" to "Top 5 AI Video Tools", "fileType" to "GRAPHICS", "fileName" to "Tech_LowerThirds_Pack.zip", "uploadedBy" to "Editor 1 (Aarav)", "uploadedById" to "usr_editor1", "uploadDateMillis" to now, "version" to "v1.0", "fileSizeLabel" to "28.5 MB", "approvalStatus" to "APPROVED", "contentUri" to "")
            )
            fileSeeds.forEach { (id, m) ->
                batch.set(userDocRef.collection("studio_files").document(id), tsMap(m))
            }

            val postSeeds = listOf(
                "post_1" to mapOf("channelId" to "CH_VRAT_KATHA", "channelName" to "Vrat Katha", "title" to "Tulsi Vivah Complete Katha & Muhurat", "platform" to "YOUTUBE", "videoFileName" to "Tulsi_Vivah_Final_4K.mp4", "thumbnailName" to "Tulsi_Vivah_Thumb.png", "caption" to "Listen to the sacred Tulsi Vivah Katha narrated by Pandit Ji.", "description" to "Complete devotional narration with shloka meanings and puja vidhi timings.", "hashtags" to "#VratKatha #TulsiVivah #Devotional #Serplive", "keywords" to "tulsi vivah katha, vrat katha hindi, pandit ji narration", "scriptCode" to "SCR-198", "publishDateKey" to yesterdayKey, "publishTime" to "06:30 AM", "status" to "PUBLISHED", "publishedUrl" to "https://youtube.com/watch?v=sf360_tulsivivah", "checklistVerified" to true, "viewsCount" to 48200L),
                "post_2" to mapOf("channelId" to "CH_TECH", "channelName" to "Technology Channel", "title" to "Top 5 AI Video Production Tools 2026", "platform" to "YOUTUBE", "videoFileName" to "Tech_AI_Tools_Master.mp4", "thumbnailName" to "AI_Tools_Thumb_v2.png", "caption" to "Cut your video editing workflow in half with these 5 studio-tested AI tools.", "description" to "Parchi breaks down real production benchmarks across scripting, motion graphics & audio.", "hashtags" to "#TechIndia #AIVideoTools #CreatorWorkflow", "keywords" to "ai video tools 2026, creator workflow, tech tutorial", "scriptCode" to "SCR-202", "publishDateKey" to tomorrowKey, "publishTime" to "06:00 PM", "status" to "READY_FOR_PUBLISHING", "publishedUrl" to "", "checklistVerified" to false, "viewsCount" to 0L),
                "post_3" to mapOf("channelId" to "CH_SERPLIVE", "channelName" to "Serplive Solutions", "title" to "3 Meta Ads Hooks Scaling Local Clinics", "platform" to "INSTAGRAM", "videoFileName" to "Serplive_MetaAds_Reel.mp4", "thumbnailName" to "MetaAds_Cover.png", "caption" to "Steal these 3 high-converting video ad hooks from Serplive Solutions.", "description" to "Agency case study breakdown for local healthcare & service brands.", "hashtags" to "#DigitalMarketing #SerpliveSolutions #PerformanceMarketing", "keywords" to "meta ads hooks, digital marketing agency india", "scriptCode" to "SCR-199", "publishDateKey" to todayKey, "publishTime" to "07:30 PM", "status" to "SCHEDULED", "publishedUrl" to "", "checklistVerified" to true, "viewsCount" to 0L)
            )
            postSeeds.forEach { (id, m) ->
                batch.set(userDocRef.collection("social_posts").document(id), tsMap(m))
            }

            val finSeeds = listOf(
                "fin_1" to mapOf("recordType" to "INVESTMENT", "category" to "INITIAL_INVESTMENT", "title" to "18-Month Seed Capital Tranche 1 (Avishek)", "amountInr" to 2500000.0, "monthKey" to lastMonthKey, "recordDateKey" to "$lastMonthKey-01", "enteredBy" to "Sandeep", "notes" to "Initial studio setup, camera gear & 18-month runway pool."),
                "fin_2" to mapOf("recordType" to "EXPENSE", "category" to "EQUIPMENT", "title" to "Sony FX30 Cinema Line + Studio Lighting Rig", "amountInr" to 385000.0, "monthKey" to lastMonthKey, "recordDateKey" to "$lastMonthKey-08", "enteredBy" to "Sandeep", "notes" to "Studio A & Studio B multi-cam setup."),
                "fin_3" to mapOf("recordType" to "EXPENSE", "category" to "OFFICE_RENT", "title" to "Serplive Studio HQ Monthly Lease", "amountInr" to 65000.0, "monthKey" to thisMonthKey, "recordDateKey" to "$thisMonthKey-01", "enteredBy" to "Sandeep", "notes" to "Includes Studio A, Studio B, Audio Booth & Editing Bay."),
                "fin_4" to mapOf("recordType" to "EXPENSE", "category" to "EMPLOYEE_PAYMENTS", "title" to "Production, Anchoring & Editing Payroll", "amountInr" to 215000.0, "monthKey" to thisMonthKey, "recordDateKey" to "$thisMonthKey-05", "enteredBy" to "Sandeep", "notes" to "Monthly team compensation & contributor honorariums."),
                "fin_5" to mapOf("recordType" to "REVENUE", "category" to "AGENCY_REVENUE", "title" to "Serplive Solutions Retainer & Client Video Billing", "amountInr" to 340000.0, "monthKey" to thisMonthKey, "recordDateKey" to "$thisMonthKey-12", "enteredBy" to "Sandeep", "notes" to "3 active agency retainer accounts."),
                "fin_6" to mapOf("recordType" to "REVENUE", "category" to "CHANNEL_REVENUE", "title" to "YouTube AdSense & Brand Integration Payout", "amountInr" to 128000.0, "monthKey" to thisMonthKey, "recordDateKey" to "$thisMonthKey-14", "enteredBy" to "Sandeep", "notes" to "Combined payout across 5 owned channels.")
            )
            finSeeds.forEach { (id, m) ->
                batch.set(userDocRef.collection("financial_records").document(id), tsMap(m))
            }

            val notifSeeds = listOf(
                "ntf_1" to mapOf("recipientUserId" to "ALL", "title" to "Task Overdue Alert: TSK-103", "message" to "Client Promo Reels for Apex Dental missed deadline. Delay reason logged by Editor 2.", "category" to "TASK", "read" to false, "createdAtMillis" to now),
                "ntf_2" to mapOf("recipientUserId" to "usr_panditji", "title" to "Today's Recording Schedule: 11:00 AM", "message" to "Devutthana Ekadashi Vrat Katha in Audio Booth. Arrival at 10:45 AM.", "category" to "SHOOT", "read" to false, "createdAtMillis" to (now - 1800_000L)),
                "ntf_3" to mapOf("recipientUserId" to "usr_manoj", "title" to "Revision Requested on SCR-204", "message" to "Stepwells Documentary script needs ASI citation tag in Act 1.", "category" to "SCRIPT", "read" to false, "createdAtMillis" to (now - 3600_000L)),
                "ntf_4" to mapOf("recipientUserId" to "usr_sandeep", "title" to "Late Arrival Detected: Kritika", "message" to "Checked in 22 mins after shift start via Dynamic QR verification.", "category" to "ATTENDANCE", "read" to false, "createdAtMillis" to (now - 5400_000L))
            )
            notifSeeds.forEach { (id, m) ->
                batch.set(userDocRef.collection("notifications").document(id), tsMap(m))
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }
}
