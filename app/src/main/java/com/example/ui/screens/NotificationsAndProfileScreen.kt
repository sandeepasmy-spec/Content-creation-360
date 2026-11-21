package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.UserEntity
import com.example.data.model.UserRole
import com.example.ui.components.AvatarBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.GeofenceHelper
import com.example.viewmodel.StudioFlowUiState

@Composable
fun NotificationsScreen(
    state: StudioFlowUiState,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit
) {
    val user = state.currentUser ?: return
    val myNotifs = state.notifications.filter { it.recipientUserId == "ALL" || it.recipientUserId == user.id }
    val unread = myNotifs.count { !it.isRead }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        SectionHeader(
            title = "Real-Time Notifications",
            subtitle = "$unread unread alerts across tasks, scripts, shoots & attendance",
            action = if (unread > 0) {
                {
                    TextButton(onClick = onMarkAllRead) { Text("Mark All Read") }
                }
            } else null
        )
        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (myNotifs.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.NotificationsNone,
                        title = "All caught up",
                        description = "No studio notifications at the moment."
                    )
                }
            } else {
                items(myNotifs, key = { it.docId.ifBlank { it.id.toString() } }) { notif ->
                    val catColor = when (notif.category) {
                        "TASK" -> StatusPending
                        "SCRIPT" -> StatusInfo
                        "ATTENDANCE" -> StatusOverdue
                        "SHOOT" -> ChannelVratColor
                        else -> ElectricBlue
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (notif.isRead) SurfaceWhite else ElectricBlueSoft.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (notif.docId.isNotBlank()) onMarkRead(notif.docId) }
                    ) {
                        Column(
                            modifier = Modifier
                                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusBadge(notif.category, catColor)
                                Text(
                                    GeofenceHelper.formatDateTime(notif.createdAtMillis),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                            Text(notif.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(notif.message, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileAndSecurityScreen(
    state: StudioFlowUiState,
    onSwitchRole: () -> Unit,
    onToggleUserStatus: (UserEntity) -> Unit,
    onLogout: () -> Unit
) {
    val user = state.currentUser ?: return
    val role = state.currentRole ?: UserRole.VIDEO_EDITOR

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Surface(shape = RoundedCornerShape(16.dp), color = SurfaceWhite) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AvatarBadge(user.avatarInitials, user.avatarColorHex, 56)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(user.fullName, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text("${user.designation} • ${user.department}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("@${user.username} • ${role.portal.label}", style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
                        if (state.googleEmail.isNotBlank()) {
                            Text("Google: ${state.googleEmail}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                    StatusBadge(user.accountStatus, StatusCompleted)
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onSwitchRole,
                    modifier = Modifier.weight(1f).testTag("switch_role_button")
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Switch Role / Portal", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Switch Role / Portal")
                }
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOverdue),
                    modifier = Modifier.weight(1f).testTag("logout_button")
                ) {
                    Icon(Icons.Default.Logout, contentDescription = "Sign Out", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sign Out")
                }
            }
        }

        item {
            SectionHeader("Login History", "${state.loginHistory.size} recent sessions")
        }

        items(state.loginHistory) { log ->
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
                        Text("${log.portal} • ${log.deviceInfo}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(GeofenceHelper.formatDateTime(log.timestamp), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    StatusBadge(
                        if (log.success) "Success" else "Denied",
                        if (log.success) StatusCompleted else StatusOverdue
                    )
                }
            }
        }

        // Sandeep Admin Team Account Control
        if (role == UserRole.SUPER_ADMIN) {
            item {
                SectionHeader("Team Account Directory (Super Admin)", "Manage active/suspended account status")
            }
            items(state.users.filter { it.id != user.id }, key = { it.id }) { target ->
                Surface(shape = RoundedCornerShape(12.dp), color = SurfaceWhite) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AvatarBadge(target.avatarInitials, target.avatarColorHex, 36)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(target.fullName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text("${target.designation} • @${target.username}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        TextButton(onClick = { onToggleUserStatus(target) }) {
                            Text(
                                if (target.accountStatus == "ACTIVE") "Suspend" else "Activate",
                                color = if (target.accountStatus == "ACTIVE") StatusOverdue else StatusCompleted
                            )
                        }
                    }
                }
            }
        }
    }
}
