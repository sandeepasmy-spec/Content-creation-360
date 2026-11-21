package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.ChannelEntity
import com.example.data.local.ChannelWorkspaceItemEntity
import com.example.data.model.ScriptStatus
import com.example.data.model.TaskStatus
import com.example.data.model.UserRole
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.StudioMetricCard
import com.example.ui.theme.*
import com.example.viewmodel.StudioFlowUiState

@Composable
fun ChannelsScreen(
    state: StudioFlowUiState,
    onAddWorkspaceItem: (ChannelWorkspaceItemEntity) -> Unit,
    onToggleWorkspaceItem: (ChannelWorkspaceItemEntity) -> Unit
) {
    val user = state.currentUser ?: return
    val role = state.currentRole ?: UserRole.VIDEO_EDITOR
    val allowedIds = user.assignedChannelsCsv.split(",")
    val visibleChannels = if (role.isHigherAuthority) state.channels else state.channels.filter { allowedIds.contains(it.id) }

    var selectedChannelId by remember(visibleChannels) {
        mutableStateOf(visibleChannels.firstOrNull()?.id ?: "CH_ART_CRAFT")
    }
    val selectedChannel = visibleChannels.firstOrNull { it.id == selectedChannelId } ?: visibleChannels.firstOrNull()

    var showAddItemDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        SectionHeader(
            title = "5-Channel Studio Workspaces",
            subtitle = "Isolated dashboards, team assignments, ideas, scripts & analytics"
        )
        Spacer(Modifier.height(10.dp))

        // Channel Switcher Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            visibleChannels.forEach { ch ->
                val isSelected = ch.id == selectedChannel?.id
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedChannelId = ch.id },
                    label = { Text(ch.name) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(ch.accentColorHex), RoundedCornerShape(99.dp))
                        )
                    },
                    modifier = Modifier.testTag("channel_tab_${ch.id}")
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (selectedChannel != null) {
            val chTasks = state.tasks.filter { it.channelId == selectedChannel.id }
            val chScripts = state.scripts.filter { it.channelId == selectedChannel.id }
            val chCalendar = state.calendarEntries.filter { it.channelId == selectedChannel.id }
            val chWorkspace = state.workspaceItems.filter { it.channelId == selectedChannel.id }
            val chTeam = state.users.filter { it.assignedChannelsCsv.split(",").contains(selectedChannel.id) }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    val accent = Color(selectedChannel.accentColorHex)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = NavyDeep
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusBadge(selectedChannel.categoryTag.uppercase(), accent)
                                Text(
                                    "Lead: ${selectedChannel.leadCoordinator}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF93C5FD)
                                )
                            }
                            Text(selectedChannel.name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                            Text(selectedChannel.description, style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StudioMetricCard(
                            title = "Subscribers & Growth",
                            value = selectedChannel.subscribersLabel,
                            subtitle = "+${selectedChannel.growthPercent}% this month",
                            icon = Icons.Default.TrendingUp,
                            accentColor = StatusCompleted,
                            modifier = Modifier.weight(1f)
                        )
                        StudioMetricCard(
                            title = "Production Target",
                            value = "${selectedChannel.videosProduced}/${selectedChannel.monthlyVideoTarget}",
                            subtitle = "${selectedChannel.videosPublished} published live",
                            icon = Icons.Default.VideoLibrary,
                            accentColor = Color(selectedChannel.accentColorHex),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Channel Team Assignments
                item {
                    Surface(shape = RoundedCornerShape(14.dp), color = SurfaceWhite) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Assigned Team (${chTeam.size})", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                chTeam.forEach { member ->
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("${member.fullName} (${UserRole.fromString(member.role).displayName.split(" ").first()})") }
                                    )
                                }
                            }
                        }
                    }
                }

                // Channel-Specific Workspace Board (Ideas, Shopping List, Tech Research, Client Leads, Doc References)
                item {
                    val boardTitle = when (selectedChannel.id) {
                        "CH_ART_CRAFT" -> "Project Ideas & Material Shopping List"
                        "CH_VRAT_KATHA" -> "Vrat Katha Rituals & Audio Prep"
                        "CH_TECH" -> "Tech Research & Verification Tracker"
                        "CH_SERPLIVE" -> "Client Video Projects & Agency Leads"
                        else -> "Documentary Ideas, Research & Fact-Checks"
                    }
                    SectionHeader(
                        title = boardTitle,
                        subtitle = "${chWorkspace.size} tracked items",
                        action = if (!role.isReadOnly) {
                            {
                                TextButton(
                                    onClick = { showAddItemDialog = true },
                                    modifier = Modifier.testTag("add_workspace_item_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add Item")
                                }
                            }
                        } else null
                    )
                }

                items(chWorkspace, key = { it.id }) { item ->
                    Surface(shape = RoundedCornerShape(12.dp), color = SurfaceWhite) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .clickable(enabled = !role.isReadOnly) { onToggleWorkspaceItem(item) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(
                                checked = item.isCompleted,
                                onCheckedChange = if (!role.isReadOnly) {
                                    { onToggleWorkspaceItem(item) }
                                } else null
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text(item.details, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            StatusBadge(
                                label = if (item.isCompleted) "Done" else item.metaBadge,
                                color = if (item.isCompleted) StatusCompleted else StatusPending
                            )
                        }
                    }
                }

                // Channel Tasks, Scripts & Calendar Summary
                item {
                    SectionHeader("Active Scripts (${chScripts.size}) & Tasks (${chTasks.size})")
                }

                items(chScripts) { scr ->
                    Surface(shape = RoundedCornerShape(12.dp), color = SurfaceWhite) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${scr.scriptCode} • ${scr.videoTitle}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("Writer: ${scr.writerName} • Anchor: ${scr.anchorName}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            StatusBadge(ScriptStatus.fromString(scr.status).label, StatusInfo)
                        }
                    }
                }

                items(chTasks) { t ->
                    Surface(shape = RoundedCornerShape(12.dp), color = SurfaceWhite) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${t.taskCode} • ${t.title}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("Assigned: ${t.assignedToNamesCsv}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            val st = TaskStatus.fromString(t.status)
                            StatusBadge(st.label, if (st == TaskStatus.OVERDUE) StatusOverdue else ElectricBlue)
                        }
                    }
                }

                item {
                    Text(
                        "Scheduled Calendar Entries: ${chCalendar.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                }
            }
        }
    }

    if (showAddItemDialog && selectedChannel != null) {
        var title by remember { mutableStateOf("") }
        var details by remember { mutableStateOf("") }
        var metaBadge by remember { mutableStateOf("Active") }
        val defaultType = when (selectedChannel.id) {
            "CH_ART_CRAFT" -> "MATERIAL_SHOPPING"
            "CH_TECH" -> "TECH_RESEARCH"
            "CH_SERPLIVE" -> "CLIENT_LEAD"
            "CH_DOCUMENTARY" -> "DOC_REFERENCE"
            else -> "CRAFT_IDEA"
        }

        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("Add to ${selectedChannel.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title / Material / Client") })
                    OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Details / Quantity / Notes") })
                    OutlinedTextField(value = metaBadge, onValueChange = { metaBadge = it }, label = { Text("Tag (e.g. Purchase Needed, Fact-Checked)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank()) {
                        onAddWorkspaceItem(
                            ChannelWorkspaceItemEntity(
                                channelId = selectedChannel.id,
                                itemType = defaultType,
                                title = title.trim(),
                                details = details.trim(),
                                metaBadge = metaBadge.trim().ifBlank { "Active" },
                                ownerName = user.fullName
                            )
                        )
                        showAddItemDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) { Text("Cancel") }
            }
        )
    }
}
