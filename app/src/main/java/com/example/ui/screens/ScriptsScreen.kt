package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.ChannelEntity
import com.example.data.local.ScriptEntity
import com.example.data.model.ScriptStatus
import com.example.data.model.UserRole
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.GeofenceHelper
import com.example.viewmodel.StudioFlowUiState

@Composable
fun ScriptsScreen(
    state: StudioFlowUiState,
    onSaveScript: (
        existing: ScriptEntity?,
        channel: ChannelEntity,
        title: String,
        anchor: String,
        editor: String,
        body: String,
        attachment: String,
        changeNote: String,
        status: ScriptStatus
    ) -> Unit,
    onReviewScript: (
        script: ScriptEntity,
        nextStatus: ScriptStatus,
        comments: String,
        highlightedErrors: String,
        factChecked: Boolean
    ) -> Unit
) {
    val role = state.currentRole ?: UserRole.VIDEO_EDITOR
    val canWrite = !role.isReadOnly && !role.isExternal
    val canVerify = role.canVerifyScripts

    var selectedChannelFilter by remember { mutableStateOf("ALL") }
    var editingScript by remember { mutableStateOf<ScriptEntity?>(null) }
    var showCreateEditor by remember { mutableStateOf(false) }
    var reviewingScript by remember { mutableStateOf<ScriptEntity?>(null) }
    var historyScript by remember { mutableStateOf<ScriptEntity?>(null) }

    val filteredScripts = state.scripts.filter {
        selectedChannelFilter == "ALL" || it.channelId == selectedChannelFilter
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        SectionHeader(
            title = "Script Portal & Verification",
            subtitle = "Idea -> Writing -> Submitted -> Verification -> Revision / Approved -> Shooting Ready",
            action = if (canWrite) {
                {
                    Button(
                        onClick = { editingScript = null; showCreateEditor = true },
                        modifier = Modifier.testTag("new_script_button")
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = "New Script", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New Script")
                    }
                }
            } else null
        )

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selectedChannelFilter == "ALL", onClick = { selectedChannelFilter = "ALL" }, label = { Text("All Channels") })
            state.channels.forEach { ch ->
                FilterChip(selected = selectedChannelFilter == ch.id, onClick = { selectedChannelFilter = ch.id }, label = { Text(ch.name) })
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(filteredScripts, key = { it.id }) { script ->
                val st = ScriptStatus.fromString(script.status)
                val badgeColor = when (st) {
                    ScriptStatus.APPROVED, ScriptStatus.SHOOTING_READY -> StatusCompleted
                    ScriptStatus.REVISION_REQUIRED -> StatusOverdue
                    ScriptStatus.VERIFICATION, ScriptStatus.SCRIPT_SUBMITTED -> StatusPending
                    else -> StatusInfo
                }
                val versionsCount = state.scriptVersions.count { it.scriptId == script.id }

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
                            Text(
                                "${script.scriptCode} • ${script.channelName} • v${script.currentVersion}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricBlue
                            )
                            StatusBadge(st.label, badgeColor)
                        }
                        Text(script.videoTitle, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            "Writer: ${script.writerName} • Anchor: ${script.anchorName} • Editor: ${script.editorName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            script.contentBody.take(150) + if (script.contentBody.length > 150) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )

                        if (script.highlightedErrors.isNotBlank()) {
                            Surface(color = StatusOverdueSoft, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    "Flagged Errors: ${script.highlightedErrors}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusOverdue,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                        if (script.reviewerComments.isNotBlank()) {
                            Text(
                                "Review Notes: ${script.reviewerComments}",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusInfo
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { historyScript = script }) {
                                Icon(Icons.Default.History, contentDescription = "History", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Versions ($versionsCount)")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (canWrite) {
                                    OutlinedButton(onClick = {
                                        editingScript = script
                                        showCreateEditor = true
                                    }) { Text("Edit") }
                                }
                                if (canVerify) {
                                    Button(onClick = { reviewingScript = script }) {
                                        Text("Verify")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateEditor) {
        val existing = editingScript
        var selectedChannel by remember {
            mutableStateOf(state.channels.firstOrNull { it.id == existing?.channelId } ?: state.channels.first())
        }
        var title by remember { mutableStateOf(existing?.videoTitle ?: "") }
        var anchor by remember { mutableStateOf(existing?.anchorName ?: "Parchi") }
        var editor by remember { mutableStateOf(existing?.editorName ?: "Editor 1 (Aarav)") }
        var body by remember { mutableStateOf(existing?.contentBody ?: "") }
        var attachment by remember { mutableStateOf(existing?.attachmentName ?: "") }
        var changeNote by remember { mutableStateOf("") }
        var nextStatus by remember { mutableStateOf(ScriptStatus.SCRIPT_SUBMITTED) }

        AlertDialog(
            onDismissRequest = { showCreateEditor = false },
            title = { Text(if (existing == null) "Create New Script" else "Edit ${existing.scriptCode} (v${existing.currentVersion + 1})") },
            text = {
                Column(modifier = Modifier.heightIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.channels.forEach { ch ->
                            FilterChip(selected = selectedChannel.id == ch.id, onClick = { selectedChannel = ch }, label = { Text(ch.name) })
                        }
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Video Title") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = anchor, onValueChange = { anchor = it }, label = { Text("Anchor") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = editor, onValueChange = { editor = it }, label = { Text("Editor") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("Full Script Body") },
                        minLines = 4,
                        maxLines = 6
                    )
                    OutlinedTextField(value = attachment, onValueChange = { attachment = it }, label = { Text("Script Attachment File Name") })
                    OutlinedTextField(value = changeNote, onValueChange = { changeNote = it }, label = { Text("Version Change Note") })
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(ScriptStatus.IDEA, ScriptStatus.SCRIPT_WRITING, ScriptStatus.SCRIPT_SUBMITTED).forEach { st ->
                            FilterChip(selected = nextStatus == st, onClick = { nextStatus = st }, label = { Text(st.label) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSaveScript(existing, selectedChannel, title, anchor, editor, body, attachment, changeNote, nextStatus)
                    showCreateEditor = false
                }) { Text("Save Script") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateEditor = false }) { Text("Cancel") }
            }
        )
    }

    reviewingScript?.let { script ->
        var comments by remember { mutableStateOf(script.reviewerComments) }
        var errors by remember { mutableStateOf(script.highlightedErrors) }
        var factChecked by remember { mutableStateOf(script.factCheckVerified) }
        var chosenStatus by remember { mutableStateOf(ScriptStatus.APPROVED) }

        AlertDialog(
            onDismissRequest = { reviewingScript = null },
            title = { Text("Verify ${script.scriptCode}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(script.videoTitle, style = MaterialTheme.typography.titleSmall)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            ScriptStatus.VERIFICATION,
                            ScriptStatus.REVISION_REQUIRED,
                            ScriptStatus.APPROVED,
                            ScriptStatus.SHOOTING_READY
                        ).forEach { st ->
                            FilterChip(selected = chosenStatus == st, onClick = { chosenStatus = st }, label = { Text(st.label) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = factChecked, onCheckedChange = { factChecked = it })
                        Text("Fact-check & brand compliance verified", style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedTextField(value = errors, onValueChange = { errors = it }, label = { Text("Highlight Errors / Corrections Needed") })
                    OutlinedTextField(value = comments, onValueChange = { comments = it }, label = { Text("Reviewer Comments") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onReviewScript(script, chosenStatus, comments, errors, factChecked)
                    reviewingScript = null
                }) { Text("Submit Review") }
            },
            dismissButton = {
                TextButton(onClick = { reviewingScript = null }) { Text("Cancel") }
            }
        )
    }

    historyScript?.let { script ->
        val versions = state.scriptVersions.filter { it.scriptId == script.id }
        AlertDialog(
            onDismissRequest = { historyScript = null },
            title = { Text("${script.scriptCode} Version History") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (versions.isEmpty()) {
                        Text("v${script.currentVersion} — Current live version.")
                    } else {
                        versions.forEach { v ->
                            Surface(color = SurfaceMuted, shape = RoundedCornerShape(10.dp)) {
                                Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                                    Text("v${v.versionNumber} • By ${v.editedBy} (${GeofenceHelper.formatDateTime(v.createdAtMillis)})", style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
                                    Text(v.changeNote, style = MaterialTheme.typography.titleSmall)
                                    Text(v.contentSnapshot.take(120), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { historyScript = null }) { Text("Close") }
            }
        )
    }
}
