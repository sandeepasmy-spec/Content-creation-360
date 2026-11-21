package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AiAssignmentEntity
import com.example.data.local.ChannelEntity
import com.example.data.model.UserRole
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.StudioMetricCard
import com.example.ui.theme.*
import com.example.util.GeofenceHelper
import com.example.viewmodel.StudioFlowUiState

@Composable
fun ManojAiWorkspaceScreen(
    state: StudioFlowUiState,
    onGenerateSamplePrompt: (channelName: String, promptType: String, brief: String) -> Unit,
    onStartAssignment: (AiAssignmentEntity) -> Unit,
    onSubmitAssignment: (AiAssignmentEntity, promptText: String, attachedAsset: String) -> Unit,
    onCreateAssignment: (ChannelEntity, promptType: String, title: String, brief: String) -> Unit
) {
    val role = state.currentRole ?: UserRole.AI_PARTNER
    val canAssign = role.canAssignTasks

    var selectedChannel by remember { mutableStateOf(state.channels.firstOrNull()) }
    var promptType by remember { mutableStateOf("SCRIPT_PROMPT") }
    var briefInput by remember { mutableStateOf("") }
    var activeSubmission by remember { mutableStateOf<AiAssignmentEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val completedCount = state.aiAssignments.count { it.status == "SUBMITTED" || it.status == "APPROVED" }
    val avgMinutes = state.aiAssignments.filter { it.timeTakenMinutes > 0 }.let { list ->
        if (list.isEmpty()) 0 else list.map { it.timeTakenMinutes }.average().toInt()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Surface(shape = RoundedCornerShape(18.dp), color = NavyDeep) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_ai_studio),
                        contentDescription = "AI Prompt Studio Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(NavyDeep.copy(alpha = 0.45f), NavyDeep.copy(alpha = 0.92f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StatusBadge("MANOJ • AI CONTENT PROMPT STUDIO", ElectricBlueBright)
                        Text("Script, Image & Image-to-Video Prompts", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Text(state.aiNoticeBanner, style = MaterialTheme.typography.bodySmall, color = Color(0xFF93C5FD))
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StudioMetricCard(
                    title = "Monthly Prompts Delivered",
                    value = "$completedCount / ${state.aiAssignments.size}",
                    subtitle = "Across 5 studio channels",
                    icon = Icons.Default.AutoAwesome,
                    accentColor = StatusInfo,
                    modifier = Modifier.weight(1f)
                )
                StudioMetricCard(
                    title = "Avg Delivery Time",
                    value = "$avgMinutes mins",
                    subtitle = "Tracked start -> submit",
                    icon = Icons.Default.Timer,
                    accentColor = StatusCompleted,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Interactive Prompt Builder
        item {
            Surface(shape = RoundedCornerShape(16.dp), color = SurfaceWhite) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Quick Prompt Workbench", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.channels.forEach { ch ->
                            FilterChip(
                                selected = selectedChannel?.id == ch.id,
                                onClick = { selectedChannel = ch },
                                label = { Text(ch.name) }
                            )
                        }
                    }
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "SCRIPT_PROMPT" to "Script Prompt",
                            "IMAGE_PROMPT" to "AI Image",
                            "IMAGE_TO_VIDEO_PROMPT" to "Image-to-Video",
                            "CONTENT_PLAN" to "Content Plan"
                        ).forEach { (k, lbl) ->
                            FilterChip(selected = promptType == k, onClick = { promptType = k }, label = { Text(lbl) })
                        }
                    }
                    OutlinedTextField(
                        value = briefInput,
                        onValueChange = { briefInput = it },
                        label = { Text("Topic / Visual Brief") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onGenerateSamplePrompt(
                                selectedChannel?.name ?: "Technology Channel",
                                promptType,
                                briefInput
                            )
                        },
                        enabled = !state.isBusy,
                        modifier = Modifier.fillMaxWidth().testTag("generate_ai_prompt_button")
                    ) {
                        if (state.isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Generating with Gemini 3.8 Flash...")
                        } else {
                            Icon(Icons.Default.Bolt, contentDescription = "Generate", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Generate with Gemini 3.8 Flash")
                        }
                    }

                    if (!state.aiOutputPreview.isNullOrBlank()) {
                        Surface(color = StatusInfoSoft, shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    state.aiNoticeBanner,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusInfo
                                )
                                Text(state.aiOutputPreview, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Assigned AI Prompt Tasks",
                subtitle = "Track assigned, started, submitted & approved deliverables",
                action = if (canAssign) {
                    {
                        Button(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Assign", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Assign Prompt")
                        }
                    }
                } else null
            )
        }

        items(state.aiAssignments, key = { it.id }) { item ->
            val stColor = when (item.status) {
                "APPROVED", "SUBMITTED" -> StatusCompleted
                "REVISION_REQUIRED" -> StatusOverdue
                "IN_PROGRESS" -> StatusInfo
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
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${item.assignmentCode} • ${item.channelName} • ${item.promptType}", style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
                        StatusBadge(item.status, stColor)
                    }
                    Text(item.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(item.brief, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    if (item.generatedPromptText.isNotBlank()) {
                        Surface(color = SurfaceMuted, shape = RoundedCornerShape(8.dp)) {
                            Text(item.generatedPromptText, style = MaterialTheme.typography.bodySmall, color = TextPrimary, modifier = Modifier.padding(8.dp))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Due: ${GeofenceHelper.formatDateTime(item.deadlineMillis)} • Time: ${item.timeTakenMinutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (item.status == "ASSIGNED") {
                                OutlinedButton(onClick = { onStartAssignment(item) }) { Text("Start Timer") }
                            }
                            if (item.status != "APPROVED" && !role.isReadOnly) {
                                Button(onClick = { activeSubmission = item }) { Text("Submit Prompt") }
                            }
                        }
                    }
                }
            }
        }
    }

    activeSubmission?.let { item ->
        var promptText by remember { mutableStateOf(item.generatedPromptText.ifBlank { state.aiOutputPreview ?: "" }) }
        var assetName by remember { mutableStateOf(item.attachedAssetName.ifBlank { "${item.assignmentCode}_prompts.txt" }) }
        AlertDialog(
            onDismissRequest = { activeSubmission = null },
            title = { Text("Submit ${item.assignmentCode}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        label = { Text("Final Prompt / Script Output") },
                        minLines = 4
                    )
                    OutlinedTextField(
                        value = assetName,
                        onValueChange = { assetName = it },
                        label = { Text("Attached Prompt / Asset File") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSubmitAssignment(item, promptText, assetName)
                    activeSubmission = null
                }) { Text("Submit Work") }
            },
            dismissButton = {
                TextButton(onClick = { activeSubmission = null }) { Text("Cancel") }
            }
        )
    }

    if (showCreateDialog) {
        var ch by remember { mutableStateOf(state.channels.first()) }
        var pType by remember { mutableStateOf("SCRIPT_PROMPT") }
        var title by remember { mutableStateOf("") }
        var brief by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Assign Prompt Task to Manoj") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.channels.forEach { c ->
                            FilterChip(selected = ch.id == c.id, onClick = { ch = c }, label = { Text(c.name) })
                        }
                    }
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("SCRIPT_PROMPT", "IMAGE_PROMPT", "IMAGE_TO_VIDEO_PROMPT", "CONTENT_PLAN").forEach { pt ->
                            FilterChip(selected = pType == pt, onClick = { pType = pt }, label = { Text(pt) })
                        }
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Assignment Title") })
                    OutlinedTextField(value = brief, onValueChange = { brief = it }, label = { Text("Prompt Brief") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onCreateAssignment(ch, pType, title, brief)
                    showCreateDialog = false
                }) { Text("Assign") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}
