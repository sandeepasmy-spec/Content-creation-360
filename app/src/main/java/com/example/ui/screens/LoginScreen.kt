package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.UserEntity
import com.example.data.model.LoginPortal
import com.example.data.model.UserRole
import com.example.ui.components.AvatarBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun LoginScreen(
    googleEmail: String,
    users: List<UserEntity>,
    isBusy: Boolean,
    authError: String?,
    bannerMessage: String?,
    onSelectPersona: (member: UserEntity, portal: LoginPortal) -> Unit,
    onSignOutGoogle: () -> Unit,
    onClearError: () -> Unit
) {
    var selectedPortal by remember { mutableStateOf(LoginPortal.HIGHER_AUTHORITY) }

    val portalUsers = remember(users, selectedPortal) {
        users.filter { UserRole.fromString(it.role).portal == selectedPortal }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasLight)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_studio_hero),
                    contentDescription = "StudioFlow 360 Command Center",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    NavyDeep.copy(alpha = 0.55f),
                                    NavyDeep.copy(alpha = 0.92f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = StatusCompleted.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "SIGNED IN: ${googleEmail.ifBlank { "Google Workspace" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA7F3D0),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "StudioFlow 360",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White
                    )
                    Text(
                        text = "Choose your Serplive Solutions Portal & Role Workspace",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Access Portal",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    TextButton(
                        onClick = onSignOutGoogle,
                        modifier = Modifier.testTag("switch_google_account_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign Out Google", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Sign Out")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LoginPortal.entries.forEach { portal ->
                        val active = portal == selectedPortal
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedPortal = portal
                                    onClearError()
                                }
                                .testTag("portal_${portal.name.lowercase()}"),
                            color = if (active) NavyDeep else SurfaceWhite,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .border(
                                        1.dp,
                                        if (active) ElectricBlue else BorderSubtle,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = portal.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (active) Color.White else TextPrimary
                                )
                                Text(
                                    text = portal.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (active) Color(0xFF94A3B8) else TextMuted,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }

                if (!authError.isNullOrBlank()) {
                    Surface(color = StatusOverdueSoft, shape = RoundedCornerShape(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = StatusOverdue)
                            Text(authError, style = MaterialTheme.typography.bodySmall, color = StatusOverdue)
                        }
                    }
                }

                if (!bannerMessage.isNullOrBlank()) {
                    Surface(color = StatusCompletedSoft, shape = RoundedCornerShape(10.dp)) {
                        Text(
                            text = bannerMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF065F46),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Surface(shape = RoundedCornerShape(16.dp), color = SurfaceWhite, shadowElevation = 1.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${selectedPortal.label} Role Accounts",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap any role profile below to launch its live Firestore-synced workspace.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )

                        if (portalUsers.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        } else {
                            portalUsers.forEach { member ->
                                val suspended = member.accountStatus != "ACTIVE"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceMuted)
                                        .clickable(enabled = !isBusy && !suspended) {
                                            onSelectPersona(member, selectedPortal)
                                        }
                                        .padding(12.dp)
                                        .testTag("enter_role_${member.username}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    AvatarBadge(member.avatarInitials, member.avatarColorHex, 40)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(member.fullName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        Text(
                                            "${member.designation} • @${member.username}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    if (suspended) {
                                        StatusBadge("SUSPENDED", StatusOverdue)
                                    } else {
                                        Icon(Icons.Default.ArrowForward, contentDescription = "Open Workspace", tint = ElectricBlue)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
