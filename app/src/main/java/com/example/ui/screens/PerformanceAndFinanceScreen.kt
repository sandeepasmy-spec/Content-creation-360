package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.FinancialRecordEntity
import com.example.data.model.UserRole
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.GeofenceHelper
import com.example.viewmodel.StudioFlowUiState
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PerformanceScreen(state: StudioFlowUiState) {
    val user = state.currentUser ?: return
    val role = state.currentRole ?: UserRole.VIDEO_EDITOR
    val visiblePerf = if (role.isHigherAuthority) {
        state.performanceSummaries
    } else {
        state.performanceSummaries.filter { it.userId == user.id }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            SectionHeader(
                title = "Employee Performance & Efficiency",
                subtitle = "Explainable formula: On-Time (35%) • Low Revisions (25%) • Complexity (20%) • Attendance (20%)"
            )
        }

        if (visiblePerf.size >= 2) {
            item {
                BarChartCard(
                    title = "Monthly Team Efficiency Comparison",
                    subtitle = "Composite performance score out of 100",
                    items = visiblePerf.map {
                        ChartBarItem(
                            label = it.fullName.split(" ").first(),
                            value = it.explainableScore.toFloat(),
                            max = 100f,
                            color = if (it.explainableScore >= 80) StatusCompleted else StatusPending
                        )
                    }
                )
            }
        }

        items(visiblePerf, key = { it.userId }) { p ->
            val scoreColor = when {
                p.explainableScore >= 85 -> StatusCompleted
                p.explainableScore >= 70 -> ElectricBlue
                else -> StatusPending
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.fullName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(p.designation, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        StatusBadge("${p.explainableScore}/100", scoreColor)
                    }

                    LinearProgressIndicator(
                        progress = { p.explainableScore / 100f },
                        modifier = Modifier.fillMaxWidth().height(7.dp),
                        color = scoreColor,
                        trackColor = SurfaceMuted
                    )

                    Text(p.scoreBreakdown, style = MaterialTheme.typography.labelSmall, color = StatusInfo)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Assigned: ${p.totalAssigned} • Completed: ${p.completed}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("On-Time: ${p.onTime} • Delayed: ${p.delayed}", style = MaterialTheme.typography.bodySmall, color = if (p.delayed > 0) StatusOverdue else StatusCompleted)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Revisions: ${p.revisionsCount} • Awaiting: ${p.awaitingApproval}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text("Avg Time: ${p.avgCompletionMinutes}m • Att: ${p.attendancePercent}%", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
fun FinanceAndInvestorScreen(
    state: StudioFlowUiState,
    onAddFinancialRecord: (FinancialRecordEntity) -> Unit
) {
    val user = state.currentUser ?: return
    val role = state.currentRole ?: UserRole.VIDEO_EDITOR

    // Strict RBAC guard: Employees & External Contributors never access Financial data
    if (!role.canViewFinance) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            EmptyStateCard(
                icon = Icons.Default.Lock,
                title = "Restricted Executive Module",
                description = "Financial & investor ledgers are accessible only to Sandeep, Sudeep, and Avishek."
            )
        }
        return
    }

    val inrFmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    val totalInvested = state.financialRecords.filter { it.recordType == "INVESTMENT" }.sumOf { it.amountInr }
    val totalExpense = state.financialRecords.filter { it.recordType == "EXPENSE" }.sumOf { it.amountInr }
    val totalRevenue = state.financialRecords.filter { it.recordType == "REVENUE" }.sumOf { it.amountInr }
    val netRunway = totalInvested + totalRevenue - totalExpense
    val netProfitLoss = totalRevenue - totalExpense

    var showAddDialog by remember { mutableStateOf(false) }

    val expenseByCategory = state.financialRecords
        .filter { it.recordType == "EXPENSE" }
        .groupBy { it.category }
        .map { (cat, list) -> cat to list.sumOf { it.amountInr }.toFloat() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            SectionHeader(
                title = "Financial & 18-Month Investor Ledger",
                subtitle = if (role.canEditFinance) "Sandeep Admin Mode • Full Edit Access" else "Read-Only Investor & Strategy View",
                action = if (role.canEditFinance) {
                    {
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.testTag("add_finance_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Entry", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Record")
                        }
                    }
                } else null
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudioMetricCard(
                        title = "18-Mo Capital Pool",
                        value = inrFmt.format(totalInvested),
                        subtitle = "Avishek Seed Capital",
                        icon = Icons.Default.Savings,
                        accentColor = ElectricBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StudioMetricCard(
                        title = "Remaining Runway",
                        value = inrFmt.format(netRunway),
                        subtitle = "Capital + Rev - Exp",
                        icon = Icons.Default.AccountBalance,
                        accentColor = StatusCompleted,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudioMetricCard(
                        title = "Total Expenditure",
                        value = inrFmt.format(totalExpense),
                        subtitle = "Rent, Payroll, Gear, Ops",
                        icon = Icons.Default.ReceiptLong,
                        accentColor = StatusPending,
                        modifier = Modifier.weight(1f)
                    )
                    StudioMetricCard(
                        title = "Operating P&L",
                        value = inrFmt.format(netProfitLoss),
                        subtitle = "Revenue: ${inrFmt.format(totalRevenue)}",
                        icon = Icons.Default.Insights,
                        accentColor = if (netProfitLoss >= 0) StatusCompleted else StatusOverdue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (expenseByCategory.isNotEmpty()) {
            val palette = listOf(ElectricBlue, StatusPending, StatusInfo, ChannelArtColor, StatusCompleted, StatusOverdue)
            item {
                DoughnutChartCard(
                    title = "Expenditure Breakdown",
                    centerLabel = inrFmt.format(totalExpense),
                    slices = expenseByCategory.mapIndexed { idx, (cat, amt) ->
                        DoughnutSlice(cat.replace("_", " "), amt, palette[idx % palette.size])
                    }
                )
            }
        }

        item {
            SectionHeader("Transactions & Capital Ledger", "${state.financialRecords.size} entries")
        }

        items(state.financialRecords, key = { it.id }) { rec ->
            val typeColor = when (rec.recordType) {
                "INVESTMENT" -> ElectricBlue
                "REVENUE" -> StatusCompleted
                else -> StatusOverdue
            }
            Surface(shape = RoundedCornerShape(14.dp), color = SurfaceWhite) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(rec.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("${rec.category} • ${rec.recordDateKey} • By ${rec.enteredBy}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        if (rec.notes.isNotBlank()) {
                            Text(rec.notes, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(inrFmt.format(rec.amountInr), style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily), color = typeColor)
                        StatusBadge(rec.recordType, typeColor)
                    }
                }
            }
        }
    }

    if (showAddDialog && role.canEditFinance) {
        var recType by remember { mutableStateOf("EXPENSE") }
        var category by remember { mutableStateOf("PRODUCTION") }
        var title by remember { mutableStateOf("") }
        var amountStr by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Financial Record") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("INVESTMENT", "EXPENSE", "REVENUE").forEach { t ->
                            FilterChip(selected = recType == t, onClick = { recType = t }, label = { Text(t) })
                        }
                    }
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "OFFICE_RENT",
                            "EMPLOYEE_PAYMENTS",
                            "EQUIPMENT",
                            "PRODUCTION",
                            "SOFTWARE",
                            "MARKETING",
                            "INITIAL_INVESTMENT",
                            "AGENCY_REVENUE",
                            "CHANNEL_REVENUE"
                        ).forEach { cat ->
                            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                        }
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Description") })
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Amount (INR)") }
                    )
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0.0) {
                        val today = GeofenceHelper.todayDateKey()
                        onAddFinancialRecord(
                            FinancialRecordEntity(
                                recordType = recType,
                                category = category,
                                title = title.trim(),
                                amountInr = amt,
                                monthKey = today.substring(0, 7),
                                recordDateKey = today,
                                enteredBy = user.fullName,
                                notes = notes.trim()
                            )
                        )
                        showAddDialog = false
                    }
                }) { Text("Save Entry") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}
