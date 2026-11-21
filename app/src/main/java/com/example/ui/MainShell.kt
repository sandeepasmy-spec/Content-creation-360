package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserRole
import com.example.ui.components.AvatarBadge
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.StudioFlowUiState
import com.example.viewmodel.StudioFlowViewModel
import kotlinx.coroutines.launch

data class NavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(
    state: StudioFlowUiState,
    viewModel: StudioFlowViewModel,
    onSignOutGoogle: () -> Unit
) {
    val user = state.currentUser ?: return
    val role = state.currentRole ?: UserRole.VIDEO_EDITOR
    var currentRoute by remember(user.id) { mutableStateOf("dashboard") }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.bannerMessage) {
        val msg = state.bannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    // Strict Role-Based Navigation Destinations
    val destinations = remember(role) {
        buildList {
            add(NavDestination("dashboard", "Dashboard", Icons.Default.Dashboard))
            if (!role.isExternal) {
                add(NavDestination("channels", "Channels (5)", Icons.Default.Tv))
                add(NavDestination("tasks", "Tasks", Icons.Default.TaskAlt))
                if (role != UserRole.INVESTOR) {
                    add(NavDestination("attendance", "Attendance", Icons.Default.LocationOn))
                }
                add(NavDestination("calendar_shooting", "Calendar & Shoot", Icons.Default.CalendarMonth))
                add(NavDestination("scripts", "Scripts", Icons.Default.Description))
                if (role == UserRole.AI_PARTNER || role.isHigherAuthority || role == UserRole.COORDINATOR_ANCHOR) {
                    add(NavDestination("ai_studio", "AI Studio", Icons.Default.AutoAwesome))
                }
                add(NavDestination("files_publishing", "Files & Publish", Icons.Default.CloudUpload))
                add(NavDestination("performance", "Performance", Icons.Default.Speed))
                if (role.canViewFinance) {
                    add(NavDestination("finance", "Finance & ROI", Icons.Default.AccountBalance))
                }
            }
            add(NavDestination("notifications", "Alerts", Icons.Default.Notifications))
            add(NavDestination("profile", "Profile", Icons.Default.Person))
        }
    }

    // Support back button returning to dashboard from secondary screens
    BackHandler(enabled = currentRoute != "dashboard") {
        currentRoute = "dashboard"
    }

    val unreadNotifs = state.notifications.count {
        !it.isRead && (it.recipientUserId == "ALL" || it.recipientUserId == user.id)
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasLight)
    ) {
        val isWideScreen = maxWidth >= 840.dp

        @Composable
        fun NavSidebarContent(onSelect: (String) -> Unit) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(248.dp)
                    .background(NavyDeep)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(14.dp)
            ) {
                // Brand Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElectricBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Logo", tint = Color.White)
                    }
                    Column {
                        Text("StudioFlow 360", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("Serplive Solutions", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = NavyBorder)
                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    destinations.forEach { dest ->
                        val active = currentRoute == dest.route
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) ElectricBlue else Color.Transparent)
                                .clickable { onSelect(dest.route) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("nav_${dest.route}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                dest.icon,
                                contentDescription = dest.label,
                                tint = if (active) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = dest.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) Color.White else Color(0xFFCBD5E1),
                                modifier = Modifier.weight(1f)
                            )
                            if (dest.route == "notifications" && unreadNotifs > 0) {
                                Badge(containerColor = StatusOverdue) {
                                    Text("$unreadNotifs", color = Color.White)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = NavyBorder)
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect("profile") }
                        .padding(8.dp)
                ) {
                    AvatarBadge(user.avatarInitials, user.avatarColorHex, 34)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.fullName, style = MaterialTheme.typography.labelLarge, color = Color.White, maxLines = 1)
                        Text(role.displayName, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8), maxLines = 1)
                    }
                }
            }
        }

        @Composable
        fun ScreenContentArea(modifier: Modifier = Modifier) {
            Box(modifier = modifier.fillMaxSize()) {
                when (currentRoute) {
                    "dashboard" -> {
                        when {
                            role.isHigherAuthority -> HigherAuthorityDashboard(
                                state = state,
                                onDateFilterChange = viewModel::setDateFilter,
                                onNavigateModule = { currentRoute = it },
                                onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                            )
                            role.isExternal -> ExternalContributorDashboard(state = state)
                            else -> EmployeeDashboard(
                                state = state,
                                onUpdateTask = { task, status, note, file, delay ->
                                    viewModel.updateTaskStatus(task, status, note, file, delay)
                                },
                                onNavigateModule = { currentRoute = it }
                            )
                        }
                    }
                    "channels" -> ChannelsScreen(
                        state = state,
                        onAddWorkspaceItem = viewModel::addChannelWorkspaceItem,
                        onToggleWorkspaceItem = viewModel::toggleChannelWorkspaceItem
                    )
                    "tasks" -> TasksScreen(
                        state = state,
                        onCreateTask = viewModel::createTask,
                        onUpdateTaskStatus = viewModel::updateTaskStatus
                    )
                    "attendance" -> AttendanceScreen(
                        state = state,
                        onCheckIn = viewModel::markAttendanceCheckIn,
                        onCheckOut = viewModel::markAttendanceCheckOut,
                        onRequestCorrection = viewModel::requestAttendanceCorrection,
                        onResolveCorrection = viewModel::resolveAttendanceCorrection,
                        onUpdateOfficeConfig = viewModel::updateOfficeConfig
                    )
                    "calendar_shooting" -> CalendarAndShootingScreen(
                        state = state,
                        onAddCalendarEntry = viewModel::addCalendarEntry,
                        onRescheduleCalendarEntry = viewModel::rescheduleCalendarEntry,
                        onAddShootingSchedule = viewModel::addShootingSchedule,
                        onUpdateShootingStatus = viewModel::updateShootingStatus
                    )
                    "scripts" -> ScriptsScreen(
                        state = state,
                        onSaveScript = viewModel::saveScript,
                        onReviewScript = viewModel::reviewScript
                    )
                    "ai_studio" -> ManojAiWorkspaceScreen(
                        state = state,
                        onGenerateSamplePrompt = viewModel::generateAiSamplePrompt,
                        onStartAssignment = viewModel::startAiAssignment,
                        onSubmitAssignment = viewModel::submitAiAssignment,
                        onCreateAssignment = viewModel::createAiAssignment
                    )
                    "files_publishing" -> FilesAndPublishingScreen(
                        state = state,
                        onUploadFile = viewModel::uploadStudioFile,
                        onApproveFile = viewModel::approveStudioFile,
                        onSaveSocialPost = viewModel::saveSocialPost,
                        onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                    )
                    "performance" -> PerformanceScreen(state = state)
                    "finance" -> FinanceAndInvestorScreen(
                        state = state,
                        onAddFinancialRecord = viewModel::addFinancialRecord
                    )
                    "notifications" -> NotificationsScreen(
                        state = state,
                        onMarkRead = viewModel::markNotificationRead,
                        onMarkAllRead = viewModel::markAllNotificationsRead
                    )
                    "profile" -> ProfileAndSecurityScreen(
                        state = state,
                        onSwitchRole = viewModel::switchRolePortal,
                        onToggleUserStatus = viewModel::toggleUserStatus,
                        onLogout = onSignOutGoogle
                    )
                }
            }
        }

        if (isWideScreen) {
            // Desktop / Tablet Split Layout with Persistent Dark Navy Sidebar
            Row(modifier = Modifier.fillMaxSize()) {
                NavSidebarContent(onSelect = { currentRoute = it })
                Scaffold(
                    modifier = Modifier.weight(1f),
                    containerColor = CanvasLight,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite),
                            title = {
                                Text(
                                    text = destinations.firstOrNull { it.route == currentRoute }?.label ?: "StudioFlow 360",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            actions = {
                                IconButton(onClick = { currentRoute = "notifications" }) {
                                    BadgedBox(
                                        badge = {
                                            if (unreadNotifs > 0) {
                                                Badge { Text("$unreadNotifs") }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                                    }
                                }
                                TextButton(onClick = { currentRoute = "profile" }) {
                                    AvatarBadge(user.avatarInitials, user.avatarColorHex, 28)
                                    Spacer(Modifier.width(6.dp))
                                    Text(user.fullName)
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    ScreenContentArea(modifier = Modifier.padding(innerPadding))
                }
            }
        } else {
            // Mobile Layout: Collapsible Dark Navy Drawer + Top Bar + Touch-Friendly Bottom Bar
            val bottomDestinations = remember(destinations) {
                destinations.filter {
                    it.route in listOf("dashboard", "channels", "tasks", "attendance", "scripts", "ai_studio", "finance", "notifications", "profile")
                }.take(5)
            }

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    NavSidebarContent(
                        onSelect = { route ->
                            currentRoute = route
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            ) {
                Scaffold(
                    containerColor = CanvasLight,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite),
                            navigationIcon = {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("drawer_menu_button")
                                ) {
                                    Icon(Icons.Default.Menu, contentDescription = "Open Navigation")
                                }
                            },
                            title = {
                                Column {
                                    Text(
                                        text = destinations.firstOrNull { it.route == currentRoute }?.label ?: "StudioFlow 360",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "${user.fullName} • ${role.displayName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { currentRoute = "notifications" }) {
                                    BadgedBox(
                                        badge = {
                                            if (unreadNotifs > 0) {
                                                Badge { Text("$unreadNotifs") }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                                    }
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = SurfaceWhite,
                            tonalElevation = 3.dp
                        ) {
                            bottomDestinations.forEach { dest ->
                                NavigationBarItem(
                                    selected = currentRoute == dest.route,
                                    onClick = { currentRoute = dest.route },
                                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                                    label = { Text(dest.label.split(" ").first(), maxLines = 1) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    ScreenContentArea(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
