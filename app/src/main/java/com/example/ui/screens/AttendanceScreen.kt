package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.AttendanceEntity
import com.example.data.model.UserRole
import com.example.ui.components.QrCodeMatrixCanvas
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.StudioMetricCard
import com.example.ui.theme.*
import com.example.util.GeofenceHelper
import com.example.viewmodel.StudioFlowUiState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun AttendanceScreen(
    state: StudioFlowUiState,
    onCheckIn: (method: String, lat: Double, lng: Double, wifiSsid: String, qrToken: String?) -> Unit,
    onCheckOut: (lat: Double, lng: Double) -> Unit,
    onRequestCorrection: (reason: String) -> Unit,
    onResolveCorrection: (record: AttendanceEntity, approve: Boolean) -> Unit,
    onUpdateOfficeConfig: (officeName: String, lat: Double, lng: Double, radius: Float, wifiSsid: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = state.currentUser ?: return
    val role = state.currentRole ?: UserRole.VIDEO_EDITOR
    val config = state.officeConfig
    val todayKey = GeofenceHelper.todayDateKey()
    val todayRecord = state.attendance.firstOrNull { it.userId == user.id && it.dateKey == todayKey }

    var selectedMethod by remember { mutableStateOf("GPS_GEOFENCE") }
    var qrInput by remember { mutableStateOf("") }
    var showCorrectionDialog by remember { mutableStateOf(false) }
    var showAdminConfigDialog by remember { mutableStateOf(false) }
    var lastGpsStatus by remember { mutableStateOf("Location is only requested when marking attendance.") }
    var pendingAction by remember { mutableStateOf<String?>(null) }

    val activeQrToken = remember(config?.qrUpdatedAt) {
        GeofenceHelper.generateDynamicQrToken()
    }

    fun executeLocationVerifiedAction(action: String) {
        scope.launch {
            lastGpsStatus = "Verifying office GPS coordinates..."
            val loc = GeofenceHelper.getSingleCurrentLocation(context)
            val wifiSsid = GeofenceHelper.getCurrentWifiSsid(context)
            if (loc == null) {
                lastGpsStatus = "Could not acquire GPS fix. Using fallback coordinate check."
                // Fallback to 0,0 which intentionally fails geofence unless calibrated
                when (action) {
                    "CHECK_IN" -> onCheckIn(selectedMethod, 0.0, 0.0, wifiSsid, qrInput)
                    "CHECK_OUT" -> onCheckOut(0.0, 0.0)
                    "CALIBRATE" -> lastGpsStatus = "GPS unavailable on this device/emulator."
                }
            } else {
                val dist = if (config != null) {
                    GeofenceHelper.calculateDistanceMeters(loc.latitude, loc.longitude, config.officeLat, config.officeLng)
                } else 0f
                lastGpsStatus = "Verified GPS: ${"%.4f".format(loc.latitude)}, ${"%.4f".format(loc.longitude)} (${dist.roundToInt()}m from HQ) • Wi-Fi: $wifiSsid"
                when (action) {
                    "CHECK_IN" -> onCheckIn(selectedMethod, loc.latitude, loc.longitude, wifiSsid, qrInput)
                    "CHECK_OUT" -> onCheckOut(loc.latitude, loc.longitude)
                    "CALIBRATE" -> if (config != null) {
                        onUpdateOfficeConfig(config.officeName, loc.latitude, loc.longitude, config.geofenceRadiusMeters, config.authorizedWifiSsid)
                    }
                }
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted && pendingAction != null) {
            executeLocationVerifiedAction(pendingAction!!)
        } else {
            lastGpsStatus = "Location permission denied. Office geofence verification requires one-time location access."
        }
        pendingAction = null
    }

    fun requestAndRunAttendance(action: String) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            executeLocationVerifiedAction(action)
        } else {
            pendingAction = action
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    val myRecords = state.attendance.filter { it.userId == user.id }
    val pendingCorrections = state.attendance.filter { it.correctionRequested }
    val visibleRecords = if (role.isHigherAuthority) state.attendance else myRecords

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            SectionHeader(
                title = "Office Geofenced Attendance",
                subtitle = "Zero background tracking • GPS, Office Wi-Fi & Rotating Dynamic QR",
                action = if (role == UserRole.SUPER_ADMIN) {
                    {
                        OutlinedButton(
                            onClick = { showAdminConfigDialog = true },
                            modifier = Modifier.testTag("office_config_button")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Office Config", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Office HQ")
                        }
                    }
                } else null
            )
        }

        // Office HQ Summary Card
        if (config != null) {
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = NavyDeep) {
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
                            Text(config.officeName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            StatusBadge("${config.geofenceRadiusMeters.roundToInt()}m Radius", ElectricBlueBright)
                        }
                        Text(
                            "HQ: ${"%.4f".format(config.officeLat)}, ${"%.4f".format(config.officeLng)} • Wi-Fi: ${config.authorizedWifiSsid}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF93C5FD)
                        )
                        Text(lastGpsStatus, style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
                    }
                }
            }
        }

        // Verification Method & Check-In / Check-Out Panel (for non-read-only employees)
        if (!role.isReadOnly && !role.isExternal) {
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = SurfaceWhite) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Verification Method", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "GPS_GEOFENCE" to "GPS Geofence",
                                "WIFI_SSID" to "Office Wi-Fi",
                                "DYNAMIC_QR" to "Dynamic QR"
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = selectedMethod == key,
                                    onClick = { selectedMethod = key },
                                    label = { Text(label) }
                                )
                            }
                        }

                        if (selectedMethod == "DYNAMIC_QR") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                QrCodeMatrixCanvas(token = activeQrToken)
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Active Office QR Token", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                    Text(activeQrToken, style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
                                    OutlinedTextField(
                                        value = qrInput,
                                        onValueChange = { qrInput = it },
                                        label = { Text("Enter / Scan Token") },
                                        singleLine = true
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { requestAndRunAttendance("CHECK_IN") },
                                enabled = todayRecord?.checkInTimeMillis == null && !state.isBusy,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("check_in_button")
                            ) {
                                Icon(Icons.Default.Login, contentDescription = "Check In", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Check In")
                            }
                            OutlinedButton(
                                onClick = { requestAndRunAttendance("CHECK_OUT") },
                                enabled = todayRecord?.checkInTimeMillis != null && todayRecord.checkOutTimeMillis == null && !state.isBusy,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("check_out_button")
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = "Check Out", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Check Out")
                            }
                        }

                        TextButton(
                            onClick = { showCorrectionDialog = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Request Manual Attendance Correction")
                        }
                    }
                }
            }
        }

        // Monthly Stats Summary
        item {
            val presentDays = myRecords.count { it.status == "PRESENT" || it.status == "LATE" }
            val lateCount = myRecords.count { it.lateArrival }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StudioMetricCard(
                    title = "My Days Logged",
                    value = "$presentDays",
                    subtitle = "$lateCount late arrivals",
                    icon = Icons.Default.EventAvailable,
                    accentColor = StatusCompleted,
                    modifier = Modifier.weight(1f)
                )
                StudioMetricCard(
                    title = "Today's Status",
                    value = todayRecord?.status ?: "NOT MARKED",
                    subtitle = if (todayRecord?.workingMinutes ?: 0 > 0) "${todayRecord?.workingMinutes} mins worked" else "Shift 10:00-19:00",
                    icon = Icons.Default.AccessTime,
                    accentColor = ElectricBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Manager Pending Correction Requests (Sandeep only)
        if (role == UserRole.SUPER_ADMIN && pendingCorrections.isNotEmpty()) {
            item {
                SectionHeader("Pending Attendance Corrections", "${pendingCorrections.size} awaiting approval")
            }
            items(pendingCorrections) { rec ->
                Surface(shape = RoundedCornerShape(14.dp), color = StatusPendingSoft) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("${rec.userName} • ${rec.dateKey}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("Reason: ${rec.correctionReason}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onResolveCorrection(rec, true) }) { Text("Approve") }
                            OutlinedButton(onClick = { onResolveCorrection(rec, false) }) { Text("Reject") }
                        }
                    }
                }
            }
        }

        item {
            SectionHeader("Daily Attendance Records", "${visibleRecords.size} entries")
        }

        items(visibleRecords) { rec ->
            val stColor = when (rec.status) {
                "PRESENT" -> StatusCompleted
                "LATE", "CORRECTION_PENDING" -> StatusPending
                else -> StatusOverdue
            }
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
                        Text("${rec.userName} • ${rec.dateKey}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(
                            "In: ${GeofenceHelper.formatTime(rec.checkInTimeMillis)} • Out: ${GeofenceHelper.formatTime(rec.checkOutTimeMillis)} • ${rec.verificationMethod}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    StatusBadge(rec.status, stColor)
                }
            }
        }

        if (role.isHigherAuthority) {
            item {
                SectionHeader("Attendance Audit Log", "Immutable verification & change history")
            }
            items(state.attendanceAudits.take(8)) { log ->
                Surface(shape = RoundedCornerShape(12.dp), color = SurfaceMuted) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text("${log.action} • ${log.targetUserName}", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                        Text("${log.details} (By ${log.performedBy})", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        }
    }

    if (showCorrectionDialog) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCorrectionDialog = false },
            title = { Text("Request Attendance Correction") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (e.g. Outdoor client shoot)") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    onRequestCorrection(reason)
                    showCorrectionDialog = false
                }) { Text("Submit Request") }
            },
            dismissButton = {
                TextButton(onClick = { showCorrectionDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAdminConfigDialog && config != null) {
        var officeName by remember { mutableStateOf(config.officeName) }
        var latText by remember { mutableStateOf(config.officeLat.toString()) }
        var lngText by remember { mutableStateOf(config.officeLng.toString()) }
        var radiusText by remember { mutableStateOf(config.geofenceRadiusMeters.roundToInt().toString()) }
        var wifiSsid by remember { mutableStateOf(config.authorizedWifiSsid) }

        AlertDialog(
            onDismissRequest = { showAdminConfigDialog = false },
            title = { Text("Configure Office Geofence") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = officeName, onValueChange = { officeName = it }, label = { Text("Office Name") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = latText, onValueChange = { latText = it }, label = { Text("Latitude") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = lngText, onValueChange = { lngText = it }, label = { Text("Longitude") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = radiusText, onValueChange = { radiusText = it }, label = { Text("Geofence Radius (meters)") })
                    OutlinedTextField(value = wifiSsid, onValueChange = { wifiSsid = it }, label = { Text("Authorized Office Wi-Fi SSID") })
                    OutlinedButton(onClick = {
                        showAdminConfigDialog = false
                        requestAndRunAttendance("CALIBRATE")
                    }) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Use Current GPS", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Calibrate HQ to My Current GPS")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateOfficeConfig(
                        officeName,
                        latText.toDoubleOrNull() ?: config.officeLat,
                        lngText.toDoubleOrNull() ?: config.officeLng,
                        radiusText.toFloatOrNull() ?: config.geofenceRadiusMeters,
                        wifiSsid
                    )
                    showAdminConfigDialog = false
                }) { Text("Save Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showAdminConfigDialog = false }) { Text("Cancel") }
            }
        )
    }
}
