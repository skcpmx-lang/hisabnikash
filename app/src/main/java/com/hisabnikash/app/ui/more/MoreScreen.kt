package com.hisabnikash.app.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwipeLeft
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import com.hisabnikash.app.data.container.AppContainer
import com.hisabnikash.app.data.repo.Period
import com.hisabnikash.app.data.repo.PeriodControl
import com.hisabnikash.app.domain.model.formatMoney
import com.hisabnikash.app.ui.components.AppTextField
import com.hisabnikash.app.ui.components.BarChart
import com.hisabnikash.app.ui.components.ChartPoint
import com.hisabnikash.app.ui.components.FilterChips
import com.hisabnikash.app.ui.components.EmptyState
import com.hisabnikash.app.ui.components.MetricCard
import com.hisabnikash.app.ui.components.StatRow
import com.hisabnikash.app.ui.components.TonalCard
import com.hisabnikash.app.ui.components.ScreenFrame
import com.hisabnikash.app.ui.components.SectionHeader
import com.hisabnikash.app.ui.nav.Routes
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import com.hisabnikash.app.ui.theme.BrandGreen
import com.hisabnikash.app.ui.theme.Spacing
import com.hisabnikash.app.ui.theme.InkFaint
import com.hisabnikash.app.ui.theme.Warning
import com.hisabnikash.app.ui.vm.appViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class MoreLink(val label: String, val subtitle: String, val icon: ImageVector, val route: String)

private data class MoreSection(val title: String, val links: List<MoreLink>)

private val sections = listOf(
    MoreSection(
        "Money",
        listOf(
            MoreLink("Accounts", "Cash, bank and wallets", Icons.Filled.AccountBalanceWallet, Routes.ACCOUNTS),
            MoreLink("Transactions", "The full money ledger", Icons.Filled.Payments, Routes.TRANSACTIONS),
            MoreLink("Transfers", "Move money between accounts", Icons.Filled.SwapHoriz, Routes.TRANSFERS),
            MoreLink("Expenses", "Costs by category", Icons.Filled.ReceiptLong, Routes.EXPENSES),
            MoreLink("Receivables", "What customers owe", Icons.Filled.RequestQuote, Routes.RECEIVABLES),
            MoreLink("Payables", "What you owe suppliers", Icons.Filled.Paid, Routes.PAYABLES),
            MoreLink("Courier settlements", "COD cash and fees", Icons.Filled.LocalShipping, Routes.SETTLEMENTS),
            MoreLink("Refunds", "Money back records", Icons.Filled.Replay, Routes.REFUNDS)
        )
    ),
    MoreSection(
        "Stock",
        listOf(
            MoreLink("Inventory movements", "Every stock change in one log", Icons.Filled.Inventory2, Routes.INVENTORY),
            MoreLink("Suppliers", "Suppliers, purchases and payables", Icons.Filled.Shop, Routes.SUPPLIERS)
        )
    ),
    MoreSection(
        "Delivery",
        listOf(
            MoreLink("Couriers", "Fees, defaults and settlements", Icons.Filled.LocalShipping, Routes.COURIERS),
            MoreLink("Returns", "Stock back and refunds", Icons.Filled.Reply, Routes.RETURNS),
            MoreLink("Exchanges", "Swaps and price differences", Icons.Filled.SwipeLeft, Routes.EXCHANGES)
        )
    ),
    MoreSection(
        "Growth",
        listOf(
            MoreLink("Campaigns", "Ad spend and ROAS", Icons.Filled.Campaign, Routes.CAMPAIGNS),
            MoreLink("Budgets", "Spending limits", Icons.Filled.Book, Routes.BUDGETS),
            MoreLink("Sales channels", "Facebook, Instagram, store…", Icons.Filled.SmartToy, Routes.CHANNELS)
        )
    ),
    MoreSection(
        "Reports",
        listOf(
            MoreLink("Analytics", "Revenue, profit and product mix", Icons.Filled.BarChart, Routes.ANALYTICS)
        )
    ),
    MoreSection(
        "Documents",
        listOf(
            MoreLink("Invoices", "Create, share and track", Icons.Filled.Description, Routes.INVOICES),
            MoreLink("Receipts", "Proof for every payment", Icons.Filled.ReceiptLong, Routes.RECEIPTS)
        )
    ),
    MoreSection(
        "Tools",
        listOf(
            MoreLink("Backup & restore", "JSON export and import", Icons.Filled.Save, Routes.BACKUP),
            MoreLink("Data health", "Check and repair records", Icons.Filled.HealthAndSafety, Routes.DATA_HEALTH)
        )
    ),
    MoreSection(
        "Settings",
        listOf(
            MoreLink("Business switcher", "Switch or add a business", Icons.Filled.Business, Routes.BUSINESS_SWITCHER),
            MoreLink("Settings", "Profile, invoicing and defaults", Icons.Filled.Settings, Routes.SETTINGS),
            MoreLink("App lock", "PIN and biometric protection", Icons.Filled.Security, Routes.SECURITY)
        )
    ),
    MoreSection(
        "Support",
        listOf(
            MoreLink("Support", "How money is counted", Icons.Filled.SupportAgent, Routes.SUPPORT),
            MoreLink("About", "Version and privacy", Icons.Filled.HealthAndSafety, Routes.ABOUT)
        )
    )
)

/**
 * The More tab is a single fully-scrolling page so every module is reachable
 * without hidden menus.
 */
@Composable
fun MoreTab(container: AppContainer, navController: NavHostController) {
    val business by container.workspaceRepository.observeActiveWorkspace().collectAsState(
        initial = com.hisabnikash.app.data.repo.Workspace()
    )
    val unread by container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(0L)
            else container.notificationRepository.observeUnreadCount(id)
        }
        .collectAsState(initial = 0L)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("More", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${business.business?.name ?: "Your business"} • ${business.business?.category?.replaceFirstChar { it.uppercase() } ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkFaint
                )
            }
            androidx.compose.material3.BadgedBox(
                badge = {
                    if (unread > 0) androidx.compose.material3.Badge { Text("$unread") }
                }
            ) {
                androidx.compose.material3.IconButton(onClick = { navController.navigate(Routes.NOTIFICATIONS) }) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                }
            }
            androidx.compose.material3.IconButton(onClick = { navController.navigate(Routes.SEARCH) }) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { navController.navigate(Routes.ANALYTICS) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.BarChart, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Analytics")
            }
            androidx.compose.material3.OutlinedButton(onClick = { navController.navigate(Routes.BUSINESS_SWITCHER) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Business, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Switch")
            }
        }

        sections.forEach { section ->
            SectionHeader(section.title)
            TonalCard {
                section.links.forEachIndexed { index, link ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = { navController.navigate(link.route) })
                            .padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.shapes.small
                                )
                                .border(
                                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    MaterialTheme.shapes.small
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                link.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(Spacing.Md))
                        Column(Modifier.weight(1f)) {
                            Text(link.label, style = MaterialTheme.typography.titleSmall)
                            Text(link.subtitle, style = MaterialTheme.typography.bodySmall, color = InkFaint, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                        androidx.compose.material3.Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = InkFaint
                        )
                    }
                    if (index < section.links.lastIndex) {
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ---------------------------------------------------------------------------
// Analytics
// ---------------------------------------------------------------------------

data class AnalyticsUi(
    val loading: Boolean = true,
    val period: String = "1D",
    val customFromAt: Long? = null,
    val customToAt: Long? = null,
    val metrics: com.hisabnikash.app.data.repo.MetricsBundle = com.hisabnikash.app.data.repo.MetricsBundle(),
    val chart: List<ChartPoint> = emptyList(),
    val topProducts: List<String> = emptyList(),
    val channels: List<String> = emptyList(),
    val expenses: List<String> = emptyList(),
    val customersServed: Int = 0,
    val repeatCustomers: Int = 0,
    val courierOrders: Long = 0,
    val courierSpendMinor: Long = 0
)

class AnalyticsViewModel(container: AppContainer) : ViewModel() {

    private val insights = container.insightsRepository
    private val periodFlow = MutableStateFlow("1D")
    private val customRange = MutableStateFlow<Period?>(null)

    val state: StateFlow<AnalyticsUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(AnalyticsUi().copy(loading = false))
            else combine(periodFlow, customRange) { period, custom -> period to custom }
                .flatMapLatest { (period, custom) ->
                    kotlinx.coroutines.flow.flow {
                        val now = System.currentTimeMillis()
                        val range = if (period == "CUSTOM") {
                            custom ?: PeriodControl.resolve("30D", now)
                        } else {
                            PeriodControl.resolve(period, now)
                        }
                        val metrics = insights.metrics(id, range.fromAt, range.toAt)
                        val products = insights.productSales(id, range.fromAt, range.toAt)
                            .sortedByDescending { it.revenueMinor }
                            .take(6)
                        val channels = insights.byChannel(id, range.fromAt, range.toAt)
                            .sortedByDescending { it.orderCount }
                        val expenses = insights.expensesByCategory(id, range.fromAt, range.toAt)
                            .take(6)
                        val allOrders = container.database.orderDao().allInRange(id, range.fromAt, range.toAt)
                        val served = allOrders.mapNotNull { it.customerId }
                            .filter { it > 0 }
                            .distinct()
                            .size
                        val repeatCustomers = allOrders.mapNotNull { it.customerId }
                            .filter { it > 0 }
                            .groupingBy { it }
                            .eachCount()
                            .values.count { it > 1 }
                        val chart = if (period == "1D") {
                            insights.intradayBuckets(id, range.fromAt, range.toAt).map {
                                val label = java.text.SimpleDateFormat("HH:00", java.util.Locale.US)
                                    .format(java.util.Date(it.time))
                                ChartPoint(
                                    label,
                                    it.revenueMinor / com.hisabnikash.app.domain.model.MoneyScale.SCALE.toFloat(),
                                    formatMoney(it.revenueMinor)
                                )
                            }
                        } else {
                            val mode = when (period) {
                                "90D" -> "WEEKLY"
                                "1Y" -> "MONTHLY"
                                else -> "DAILY"
                            }
                            insights.chartSeries(id, range.fromAt, range.toAt, mode).map {
                                val label = if (period == "1Y") {
                                    java.text.SimpleDateFormat("MMM", java.util.Locale.US).format(java.util.Date(it.time))
                                } else {
                                    java.text.SimpleDateFormat("d MMM", java.util.Locale.US).format(java.util.Date(it.time))
                                }
                                ChartPoint(
                                    label,
                                    it.revenueMinor / com.hisabnikash.app.domain.model.MoneyScale.SCALE.toFloat(),
                                    formatMoney(it.revenueMinor)
                                )
                            }
                        }
                        emit(
                            AnalyticsUi(
                                loading = false,
                                period = period,
                                customFromAt = if (period == "CUSTOM") custom?.fromAt else null,
                                customToAt = if (period == "CUSTOM") custom?.toAt else null,
                                metrics = metrics,
                                chart = chart,
                                topProducts = products.map { "${it.name}: ${formatMoney(it.revenueMinor)}" },
                                channels = channels.map { "${it.name ?: "Unassigned"}: ${it.orderCount} orders" },
                                expenses = expenses.map { "${it.first}: ${formatMoney(it.second)}" },
                                customersServed = served,
                                repeatCustomers = repeatCustomers,
                                courierOrders = metrics.deliveredCount,
                                courierSpendMinor = metrics.courierMinor
                            )
                        )
                    }
                }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            AnalyticsUi()
        )

    fun setPeriod(period: String) { periodFlow.value = period }

    fun setCustomRange(fromAt: Long, toAt: Long) {
        customRange.value = Period(fromAt, toAt)
        periodFlow.value = "CUSTOM"
    }
}

@Composable
fun AnalyticsScreenRoute(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { AnalyticsViewModel(it) }
    val state by vm.state.collectAsState()
    var showPicker by remember { mutableStateOf(false) }

    ScreenFrame("Analytics", onBack = { navController.popBackStack() }) {
        val periodLabel = if (state.period == "CUSTOM") "Custom" else state.period
        FilterChips(
            listOf("1D", "7D", "10D", "30D", "90D", "1Y", "Custom"),
            periodLabel,
            { picked ->
                if (picked == "Custom") showPicker = true else vm.setPeriod(picked)
            }
        )
        if (state.period == "CUSTOM" && state.customFromAt != null && state.customToAt != null) {
            Text(
                "Custom range: ${com.hisabnikash.app.domain.model.formatDay(state.customFromAt!!)} – ${com.hisabnikash.app.domain.model.formatDay(state.customToAt!!)}",
                style = MaterialTheme.typography.bodySmall,
                color = InkFaint,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }
        if (showPicker) {
            CustomRangePicker(
                initialFromAt = state.customFromAt,
                initialToAt = state.customToAt,
                onDismiss = { showPicker = false },
                onApply = { from, to ->
                    showPicker = false
                    vm.setCustomRange(from, to)
                }
            )
        }
        val m = state.metrics
        // ------------------------------------------------ KPI strip (hero first)
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            MetricCard("Revenue", formatMoney(m.revenueMinor), Modifier.weight(1f), icon = Icons.Filled.Paid)
            MetricCard("Profit", formatMoney(m.netProfitMinor), Modifier.weight(1f), icon = Icons.Filled.AccountBalanceWallet)
            MetricCard("Orders", "${m.deliveredCount}", Modifier.weight(1f), icon = Icons.Filled.BarChart)
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            MetricCard("AOV", formatMoney(m.aovMinor), Modifier.weight(1f))
            MetricCard("Margin", com.hisabnikash.app.domain.model.formatPercent(m.marginBps), Modifier.weight(1f))
            MetricCard("ROAS", com.hisabnikash.app.domain.model.formatPercent(m.roasBps), Modifier.weight(1f))
        }

        // ------------------------------------------------- revenue & profit chart
        SectionHeader(if (state.period == "1D") "Revenue — today by hour" else "Revenue trend")
        TonalCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(BrandGreen, RoundedCornerShape(4.dp)))
                Spacer(Modifier.width(6.dp))
                Text("Revenue", style = MaterialTheme.typography.labelMedium, color = InkFaint)
                Spacer(Modifier.weight(1f))
                Text(
                    "${state.chart.count { it.value > 0f }} active ${if (state.period == "1D") "hours" else "days"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkFaint
                )
            }
            Spacer(Modifier.height(Spacing.Sm))
            if (m.deliveredCount == 0L && m.revenueMinor == 0L) {
                EmptyState(
                    icon = Icons.Filled.BarChart,
                    title = "No revenue yet",
                    message = "Delivered orders in ${if (state.period == "1D") "today" else "this period"} appear here at their transaction time.",
                    modifier = Modifier.padding(0.dp)
                )
            } else {
                BarChart(state.chart, maxLabels = when (state.period) {
                    "7D" -> 7
                    "10D" -> 7
                    else -> 8
                })
            }
        }

        // -------------------------------------------------------------- summary
        SectionHeader("Summary")
        TonalCard {
            StatRow("Units sold", "${m.unitsSold}")
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatRow("COGS", formatMoney(m.cogsMinor))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatRow("Courier & delivery", formatMoney(m.courierMinor + m.packagingMinor))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatRow("Refunds", formatMoney(m.refundsMinor))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatRow("Expenses", formatMoney(m.expensesMinor))
            if (m.advertisingMinor > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                StatRow("Advertising", formatMoney(m.advertisingMinor))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatRow("Orders created", "${m.orderCount}")
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatRow("Returned / cancelled", "${m.returnedCount} / ${m.cancelledCount}")
        }

        // ------------------------------------------------------------ top products
        SectionHeader("Top products")
        if (state.topProducts.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Inventory2,
                title = "No product sales",
                message = "Delivered orders with items will rank your best sellers here.",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            TonalCard {
                state.topProducts.forEachIndexed { index, row ->
                    val parts = row.split(": ")
                    val amount = parts.lastOrNull() ?: ""
                    val name = if (parts.size > 1) parts.dropLast(1).joinToString(": ") else row
                    RankingRow(index + 1, name, amount)
                    if (index < state.topProducts.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        // --------------------------------------------------------------- channels
        SectionHeader("By channel")
        if (state.channels.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Shop,
                title = "No channel activity",
                message = "Orders assigned to a channel are broken down here.",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            TonalCard {
                state.channels.forEachIndexed { index, row ->
                    val parts = row.split(": ")
                    val value = parts.lastOrNull() ?: ""
                    val name = if (parts.size > 1) parts.dropLast(1).joinToString(": ") else row
                    RankLine(name, value, icon = Icons.Filled.Shop)
                    if (index < state.channels.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        // ---------------------------------------------------------------- expenses
        SectionHeader("Expenses by category")
        if (state.expenses.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.ReceiptLong,
                title = "No expenses",
                message = "Expenses recorded in this period appear here by category.",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            TonalCard {
                state.expenses.forEachIndexed { index, row ->
                    val parts = row.split(": ")
                    val value = parts.lastOrNull() ?: ""
                    val name = if (parts.size > 1) parts.dropLast(1).joinToString(": ") else row
                    RankLine(name, value, icon = Icons.Filled.ReceiptLong, accent = false)
                    if (index < state.expenses.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        // --------------------------------------------------- customer & courier insights
        SectionHeader("Customers")
        TonalCard {
            StatRow("Customers served", "${state.customersServed}")
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatRow("Repeat customers", "${state.repeatCustomers}")
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatRow(
                "Repeat rate",
                if (state.customersServed > 0) {
                    com.hisabnikash.app.domain.model.formatPercent((state.repeatCustomers * 10_000 / state.customersServed))
                } else "-"
            )
        }

        SectionHeader("Courier & delivery")
        TonalCard {
            StatRow("Delivered orders", "${state.courierOrders}")
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatRow("Courier spend", formatMoney(state.courierSpendMinor))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatRow(
                "Cost per delivery",
                if (state.courierOrders > 0) formatMoney(state.courierSpendMinor / state.courierOrders) else "-"
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun RankingRow(rank: Int, name: String, amount: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(
                    if (rank <= 3) BrandGreen else MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$rank",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (rank <= 3) MaterialTheme.colorScheme.primary else InkFaint
            )
        }
        Spacer(Modifier.width(Spacing.Md))
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(8.dp))
        Text(amount, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RankLine(name: String, value: String, icon: ImageVector, accent: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(Spacing.Md))
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Material date-range picker bound to a local-timezone day range.
 *
 * `DateRangePicker` works in UTC day boundaries, so the selected days are
 * converted to the device timezone before they are stored and queried. The
 * resulting range is inclusive: [fromAt] at 00:00 of the first day through
 * 23:59:59.999 of the last day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRangePicker(
    initialFromAt: Long?,
    initialToAt: Long?,
    onDismiss: () -> Unit,
    onApply: (fromAt: Long, toAt: Long) -> Unit
) {
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialFromAt?.let(::toUtcDayStart),
        initialSelectedEndDateMillis = initialToAt?.let(::toUtcDayStart)
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = pickerState.selectedStartDateMillis
                    val end = pickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        val range = toLocalRange(start, end)
                        onApply(range.first, range.second)
                    }
                },
                enabled = pickerState.selectedStartDateMillis != null &&
                    pickerState.selectedEndDateMillis != null
            ) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DateRangePicker(
            state = pickerState,
            modifier = Modifier.weight(1f),
            showModeToggle = false
        )
    }
}

private fun toUtcDayStart(localMillis: Long): Long {
    val date = Instant.ofEpochMilli(localMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private fun toLocalRange(startUtc: Long, endUtc: Long): Pair<Long, Long> {
    val zone = ZoneId.systemDefault()
    val startDay = Instant.ofEpochMilli(startUtc).atZone(ZoneOffset.UTC).toLocalDate()
    val endDay = Instant.ofEpochMilli(endUtc).atZone(ZoneOffset.UTC).toLocalDate()
    val fromAt = startDay.atStartOfDay(zone).toInstant().toEpochMilli()
    val toAt = endDay.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1L
    return fromAt to toAt
}

// ---------------------------------------------------------------------------
// Inventory movements
// ---------------------------------------------------------------------------

data class InventoryUi(
    val loading: Boolean = true,
    val valueMinor: Long = 0,
    val totalUnits: Long = 0,
    val lowCount: Long = 0,
    val outCount: Long = 0,
    val movements: List<com.hisabnikash.app.data.db.InventoryMovementEntity> = emptyList()
)

class InventoryViewModel(container: AppContainer) : ViewModel() {

    private val catalog = container.catalogRepository

    val state: StateFlow<InventoryUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(InventoryUi().copy(loading = false))
            else combine(
                catalog.observeInventoryValue(id),
                catalog.observeTotalUnits(id),
                catalog.observeLowStock(id),
                catalog.observeOutOfStock(id),
                catalog.observeMovements(id)
            ) { value, units, low, out, movements ->
                InventoryUi(
                    loading = false,
                    valueMinor = value,
                    totalUnits = units,
                    lowCount = low.size.toLong(),
                    outCount = out.size.toLong(),
                    movements = movements
                )
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            InventoryUi()
        )
}

@Composable
fun InventoryScreenRoute(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { InventoryViewModel(it) }
    val state by vm.state.collectAsState()

    ScreenFrame("Inventory", onBack = { navController.popBackStack() }) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            MetricCard("Value", formatMoney(state.valueMinor), Modifier.weight(1f))
            MetricCard("Units", "${state.totalUnits}", Modifier.weight(1f))
            MetricCard("Low/out", "${state.lowCount}/${state.outCount}", Modifier.weight(1f))
        }
        SectionHeader("Movements")
        if (state.movements.isEmpty()) {
            Text(
                "Purchases, sales and adjustments appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkFaint,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            state.movements.forEach { m ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${m.type.replaceFirstChar { it.uppercase() }} • product #${m.productId}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                m.reason ?: com.hisabnikash.app.domain.model.formatDateTime(m.dateAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = InkFaint
                            )
                        }
                        Text(
                            (if (m.qty >= 0) "+" else "") + m.qty,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (m.qty >= 0) BrandGreen else Warning
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ---------------------------------------------------------------------------
// Channels
// ---------------------------------------------------------------------------

@Composable
fun ChannelsScreenRoute(container: AppContainer, navController: NavHostController) {
    val channels by container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(emptyList())
            else container.financeRepository.observeChannels(id)
        }
        .collectAsState(initial = emptyList())
    var newName by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    ScreenFrame("Sales channels", onBack = { navController.popBackStack() }) {
        channels.forEach { channel ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(channel.name, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
        SectionHeader("Add channel")
        com.hisabnikash.app.ui.components.AppTextField("Channel name", newName, { newName = it }, placeholder = "e.g. TikTok Shop")
        Button(
            onClick = {
                val name = newName.trim()
                if (name.isEmpty()) return@Button
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                    val id = container.workspaceRepository.requireActiveBusiness()
                    container.financeRepository.addChannel(id, name)
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        newName = ""
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) { Text("Add channel") }
        Spacer(Modifier.height(16.dp))
    }
}

// ---------------------------------------------------------------------------
// Receipts
// ---------------------------------------------------------------------------

@Composable
fun ReceiptsListRoute(container: AppContainer, navController: NavHostController) {
    val receipts by container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(emptyList())
            else container.database.receiptDao().observeAll(id)
        }
        .collectAsState(initial = emptyList())

    ScreenFrame("Receipts", onBack = { navController.popBackStack() }) {
        if (receipts.isEmpty()) {
            Text(
                "Every payment creates a receipt automatically. Receipts can never exist without the money movement behind them.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkFaint,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else {
            receipts.forEach { receipt ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(receipt.receiptNo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(formatMoney(receipt.amountMinor), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BrandGreen)
                        }
                        Text(
                            "${com.hisabnikash.app.domain.model.formatDateTime(receipt.dateAt)} • ${receipt.method} • " +
                                if (receipt.remainingMinor > 0) "remaining ${formatMoney(receipt.remainingMinor)}" else "settled",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkFaint
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
