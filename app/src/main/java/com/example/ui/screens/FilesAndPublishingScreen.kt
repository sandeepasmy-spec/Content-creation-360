package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.data.local.SocialPostEntity
import com.example.data.local.StudioFileEntity
import com.example.data.model.PublishingStatus
import com.example.data.model.SocialPlatform
import com.example.data.model.UserRole
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.GeofenceHelper
import com.example.viewmodel.StudioFlowUiState

@Composable
fun FilesAndPublishingScreen(
    state: StudioFlowUiState,
    onUploadFile: (StudioFileEntity) -> Unit,
    onApproveFile: (StudioFileEntity, String) -> Unit,
    onSaveSocialPost: (SocialPostEntity) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val user = state.currentUser ?: return
    val role = state.currentRole ?: UserRole.VIDEO_EDITOR
    val canEdit = !role.isReadOnly

    var activeTab by remember { mutableStateOf(0) } // 0 = Files Portal, 1 = Social Media Publishing
    var selectedChannelFilter by remember { mutableStateOf("ALL") }
    var showUploadDialog by remember { mutableStateOf(false) }
    var editingPost by remember { mutableStateOf<SocialPostEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        TabRow(selectedTabIndex = activeTab) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("File Portal") })
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Social Publishing") })
        }
        Spacer(Modifier.height(10.dp))

        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selectedChannelFilter == "ALL", onClick = { selectedChannelFilter = "ALL" }, label = { Text("All Channels") })
            state.channels.forEach { ch ->
                FilterChip(selected = selectedChannelFilter == ch.id, onClick = { selectedChannelFilter = ch.id }, label = { Text(ch.name) })
            }
        }
        Spacer(Modifier.height(10.dp))

        if (activeTab == 0) {
            val files = state.studioFiles.filter {
                val chMatch = selectedChannelFilter == "ALL" || it.channelId == selectedChannelFilter
                val extMatch = if (role.isExternal) user.assignedChannelsCsv.split(",").contains(it.channelId) && it.approvalStatus == "APPROVED" else true
                chMatch && extMatch
            }

            SectionHeader(
                title = "Studio File & Asset Portal",
                subtitle = "Organized: Channel -> Project -> Content Title -> File Type",
                action = if (canEdit && !role.isExternal) {
                    {
                        Button(
                            onClick = { showUploadDialog = true },
                            modifier = Modifier.testTag("upload_file_button")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Upload", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Upload")
                        }
                    }
                } else null
            )

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(files, key = { it.id }) { file ->
                    val statusColor = when (file.approvalStatus) {
                        "APPROVED" -> StatusCompleted
                        "REVISION" -> StatusOverdue
                        else -> StatusPending
                    }
                    Surface(shape = RoundedCornerShape(14.dp), color = SurfaceWhite) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${file.channelName} -> ${file.projectName} -> ${file.contentTitle}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricBlue,
                                    modifier = Modifier.weight(1f)
                                )
                                StatusBadge(file.approvalStatus, statusColor)
                            }
                            Text(file.fileName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                "${file.fileType} • ${file.version} • ${file.fileSizeLabel} • By ${file.uploadedBy} (${GeofenceHelper.formatDateShort(file.uploadDateMillis)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = {
                                    onShowMessage("Prepared '${file.fileName}' (${file.fileSizeLabel}) for local viewing/download.")
                                }) {
                                    Icon(Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Download")
                                }
                                if (role.canVerifyScripts && file.approvalStatus != "APPROVED") {
                                    Button(onClick = { onApproveFile(file, "APPROVED") }) {
                                        Text("Approve")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Social Media Content Publishing (YouTube, Facebook, Instagram)
            val clipboard = LocalClipboardManager.current
            val posts = state.socialPosts.filter { selectedChannelFilter == "ALL" || it.channelId == selectedChannelFilter }

            SectionHeader(
                title = "Social Media Publishing Hub",
                subtitle = "YouTube, Facebook & Instagram • Checklist & Verified Link Gate",
                action = if (canEdit && !role.isExternal) {
                    {
                        Button(onClick = {
                            val ch = state.channels.first()
                            editingPost = SocialPostEntity(
                                channelId = ch.id,
                                channelName = ch.name,
                                title = "",
                                platform = SocialPlatform.YOUTUBE.name,
                                videoFileName = "",
                                thumbnailName = "",
                                caption = "",
                                description = "",
                                hashtags = "",
                                keywords = "",
                                scriptCode = "SCR-NEW",
                                publishDateKey = GeofenceHelper.todayDateKey(),
                                publishTime = "06:00 PM",
                                status = PublishingStatus.READY_FOR_PUBLISHING.name
                            )
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "New Post", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("New Post")
                        }
                    }
                } else null
            )

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(posts, key = { it.id }) { post ->
                    val st = PublishingStatus.fromString(post.status)
                    val stColor = when (st) {
                        PublishingStatus.PUBLISHED, PublishingStatus.PERFORMANCE_TRACKING -> StatusCompleted
                        PublishingStatus.SCHEDULED -> ElectricBlue
                        PublishingStatus.READY_FOR_PUBLISHING -> StatusPending
                    }
                    Surface(shape = RoundedCornerShape(14.dp), color = SurfaceWhite) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${post.channelName} • ${post.platform}", style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
                                StatusBadge(st.label, stColor)
                            }
                            Text(post.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("Video: ${post.videoFileName} • Thumb: ${post.thumbnailName}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("Caption: ${post.caption}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            Text("Hashtags: ${post.hashtags}", style = MaterialTheme.typography.labelSmall, color = StatusInfo)
                            Text("Schedule: ${post.publishDateKey} at ${post.publishTime}", style = MaterialTheme.typography.bodySmall, color = TextMuted)

                            if (post.publishedUrl.isNotBlank()) {
                                Text("Live URL: ${post.publishedUrl}", style = MaterialTheme.typography.labelSmall, color = StatusCompleted)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(onClick = {
                                    val copyBlob = "${post.title}\n\n${post.caption}\n\n${post.description}\n\n${post.hashtags}"
                                    clipboard.setText(AnnotatedString(copyBlob))
                                    onShowMessage("Copied caption, description & hashtags to clipboard.")
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Copy Metadata")
                                }
                                if (canEdit) {
                                    Button(onClick = { editingPost = post }) {
                                        Text(if (post.publishedUrl.isBlank()) "Publish / Paste URL" else "Edit")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUploadDialog) {
        val context = LocalContext.current
        var ch by remember { mutableStateOf(state.channels.first()) }
        var project by remember { mutableStateOf("Monthly Batch") }
        var contentTitle by remember { mutableStateOf("") }
        var fileType by remember { mutableStateOf("VIDEO_FILE") }
        var fileName by remember { mutableStateOf("") }
        var version by remember { mutableStateOf("v1.0") }
        var contentUri by remember { mutableStateOf("") }

        val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            uri?.let {
                contentUri = it.toString()
                fileName = it.lastPathSegment ?: "studio_media_asset"
            }
        }
        val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                contentUri = it.toString()
                val cursor = context.contentResolver.query(it, null, null, null, null)
                val name = cursor?.use { c ->
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (c.moveToFirst() && idx >= 0) c.getString(idx) else null
                }
                fileName = name ?: it.lastPathSegment ?: "studio_document"
            }
        }

        AlertDialog(
            onDismissRequest = { showUploadDialog = false },
            title = { Text("Upload Studio Deliverable") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.channels.forEach { c ->
                            FilterChip(selected = ch.id == c.id, onClick = { ch = c }, label = { Text(c.name) })
                        }
                    }
                    OutlinedTextField(value = project, onValueChange = { project = it }, label = { Text("Project Folder") })
                    OutlinedTextField(value = contentTitle, onValueChange = { contentTitle = it }, label = { Text("Content Title") })
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("SCRIPT", "REFERENCE_IMAGE", "VIDEO_FILE", "GRAPHICS", "SHOOTING_INSTRUCTIONS", "THUMBNAIL").forEach { ft ->
                            FilterChip(selected = fileType == ft, onClick = { fileType = ft }, label = { Text(ft) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Photo/Video") }
                        OutlinedButton(
                            onClick = { docPicker.launch(arrayOf("*/*")) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Document") }
                    }
                    OutlinedTextField(value = fileName, onValueChange = { fileName = it }, label = { Text("File Name") })
                    OutlinedTextField(value = version, onValueChange = { version = it }, label = { Text("Version") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (fileName.isNotBlank() && contentTitle.isNotBlank()) {
                        onUploadFile(
                            StudioFileEntity(
                                channelId = ch.id,
                                channelName = ch.name,
                                projectName = project.trim(),
                                contentTitle = contentTitle.trim(),
                                fileType = fileType,
                                fileName = fileName.trim(),
                                uploadedBy = user.fullName,
                                uploadedById = user.id,
                                version = version,
                                fileSizeLabel = "Ready",
                                contentUri = contentUri
                            )
                        )
                        showUploadDialog = false
                    }
                }) { Text("Save File") }
            },
            dismissButton = {
                TextButton(onClick = { showUploadDialog = false }) { Text("Cancel") }
            }
        )
    }

    editingPost?.let { post ->
        var ch by remember { mutableStateOf(state.channels.firstOrNull { it.id == post.channelId } ?: state.channels.first()) }
        var platform by remember { mutableStateOf(SocialPlatform.fromString(post.platform)) }
        var title by remember { mutableStateOf(post.title) }
        var videoFile by remember { mutableStateOf(post.videoFileName) }
        var thumb by remember { mutableStateOf(post.thumbnailName) }
        var caption by remember { mutableStateOf(post.caption) }
        var hashtags by remember { mutableStateOf(post.hashtags) }
        var checklistVerified by remember { mutableStateOf(post.checklistVerified) }
        var publishedUrl by remember { mutableStateOf(post.publishedUrl) }
        var targetStatus by remember { mutableStateOf(PublishingStatus.fromString(post.status)) }

        AlertDialog(
            onDismissRequest = { editingPost = null },
            title = { Text("Publishing Checklist & Confirmation") },
            text = {
                Column(modifier = Modifier.heightIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.channels.forEach { c ->
                            FilterChip(selected = ch.id == c.id, onClick = { ch = c }, label = { Text(c.name) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SocialPlatform.entries.forEach { p ->
                            FilterChip(selected = platform == p, onClick = { platform = p }, label = { Text(p.label) })
                        }
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Video / Post Title") })
                    OutlinedTextField(value = videoFile, onValueChange = { videoFile = it }, label = { Text("Final Video File") })
                    OutlinedTextField(value = thumb, onValueChange = { thumb = it }, label = { Text("Thumbnail File") })
                    OutlinedTextField(value = caption, onValueChange = { caption = it }, label = { Text("Caption / Description") })
                    OutlinedTextField(value = hashtags, onValueChange = { hashtags = it }, label = { Text("Hashtags & Keywords") })

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = checklistVerified, onCheckedChange = { checklistVerified = it })
                        Text("Manual upload checklist verified (Audio, Thumbnail, Tags)", style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedTextField(
                        value = publishedUrl,
                        onValueChange = { publishedUrl = it },
                        label = { Text("Confirmed Platform URL (Required to mark Published)") }
                    )
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PublishingStatus.entries.forEach { st ->
                            FilterChip(selected = targetStatus == st, onClick = { targetStatus = st }, label = { Text(st.label) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val safeStatus = if ((targetStatus == PublishingStatus.PUBLISHED || targetStatus == PublishingStatus.PERFORMANCE_TRACKING) &&
                        (!checklistVerified || publishedUrl.isBlank())
                    ) {
                        onShowMessage("Cannot mark Published until manual checklist is checked and confirmed URL is entered.")
                        PublishingStatus.SCHEDULED
                    } else {
                        targetStatus
                    }
                    onSaveSocialPost(
                        post.copy(
                            channelId = ch.id,
                            channelName = ch.name,
                            platform = platform.name,
                            title = title.trim(),
                            videoFileName = videoFile.trim(),
                            thumbnailName = thumb.trim(),
                            caption = caption.trim(),
                            hashtags = hashtags.trim(),
                            checklistVerified = checklistVerified,
                            publishedUrl = publishedUrl.trim(),
                            status = safeStatus.name
                        )
                    )
                    editingPost = null
                }) { Text("Save Post") }
            },
            dismissButton = {
                TextButton(onClick = { editingPost = null }) { Text("Cancel") }
            }
        )
    }
}
