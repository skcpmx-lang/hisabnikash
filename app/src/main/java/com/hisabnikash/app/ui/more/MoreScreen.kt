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
import androidx.compose.material.icons.filled.Coin
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import com.hisabnikash.app.data.container.AppContainer
import com.hisabnikash.app.data.repo.PeriodControl
import com.hisabnikash.app.domain.model.formatMoney
import com.hisabnikash.app.ui.components.AppTextField
import com.hisabnikash.app.ui.components.BarChart
import com.hisabnikash.app.ui.components.ChartPoint
import com.hisabnikash.app.ui.components.FilterChips
import com.hisabnikash.app.ui.components.MetricCard
import com.hisabnikash.app.ui.components.ScreenFrame
import com.hisabnikash.app.ui.components.SectionHeader
import com.hisabnikash.app.ui.nav.Routes
import com.hisabnikash.app.ui.theme.BrandGreen
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
        "Workspace",
        listOf(
            MoreLink("Business switcher", "Switch or add a business", Icons.Filled.Business, Routes.BUSINESS_SWITCHER),
            MoreLink("Settings", "Profile, invoicing and defaults", Icons.Filled.Settings, Routes.SETTINGS),
            MoreLink("App lock", "PIN and biometric protection", Icons.Filled.Security, Routes.SECURITY)
        )
    ),
    MoreSection(
        "Inventory & catalog",
        listOf(
            MoreLink("Inventory movements", "Every stock change in one log", Icons.Filled.Inventory2, Routes.INVENTORY),
            MoreLink("Suppliers", "Suppliers, purchases and payables", Icons.Filled.Shop, Routes.SUPPLIERS),
            MoreLink("Sales channels", "Facebook, Instagram, store…", Icons.Filled.SmartToy, Routes.CHANNELS),
            MoreLink("Couriers", "Fees, defaults and settlements", Icons.Filled.LocalShipping, Routes.COURIERS)
        )
    ),
    MoreSection(
        "Money",
        listOf(
            MoreLink("Accounts", "Cash, bank and wallets", Icons.Filled.AccountBalanceWallet, Routes.ACCOUNTS),
            MoreLink("Transactions", "The full money ledger", Icons.Filled.Payments, Routes.TRANSACTIONS),
            MoreLink("Transfers", "Move money between accounts", Icons.Filled.SwapHoriz, Routes.TRANSFERS),
            MoreLink("Expenses", "Costs by category", Icons.Filled.ReceiptLong, Routes.EXPENSES),
            MoreLink("Receivables", "What customers owe", Icons.Filled.RequestQuote, Routes.RECEIVABLES),
            MoreLink("Payables", "What you owe suppliers", Icons.Filled.Coin, Routes.PAYABLES),
            MoreLink("Courier settlements", "COD cash and fees", Icons.Filled.LocalShipping, Routes.SETTLEMENTS)
        )
    ),
    MoreSection(
        "Documents",
        listOf(
            MoreLink("Invoices", "Create, share and track", Icons.Filled.Description, Routes.INVOICES),
            MoreLink("Receipts", "Proof for every payment", Icons.Filled.ReceiptLong, Routes.RECEIPTS),
            MoreLink("Returns", "Stock back and refunds", Icons.Filled.Reply, Routes.RETURNS),
            MoreLink("Exchanges", "Swaps and price differences", Icons.Filled.SwipeLeft, Routes.EXCHANGES),
            MoreLink("Refunds", "Money back records", Icons.Filled.Replay, Routes.REFUNDS)
        )
    ),
    MoreSection(
        "Growth",
        listOf(
            MoreLink("Analytics", "Revenue, profit and product mix", Icons.Filled.BarChart, Routes.ANALYTICS),
            MoreLink("Campaigns", "Ad spend and ROAS", Icons.Filled.Campaign, Routes.CAMPAIGNS),
            MoreLink("Budgets", "Spending limits", Icons.Filled.Book, Routes.BUDGETS)
        )
    ),
    MoreSection(
        "Data & help",
        listOf(
            MoreLink("Backup & restore", "JSON export and import", Icons.Filled.Save, Routes.BACKUP),
            MoreLink("Data health", "Check and repair records", Icons.Filled.HealthAndSafety, Routes.DATA_HEALTH),
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

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.OutlinedButton(onClick = { navController.navigate(Routes.ANALYTICS) }, modifier = Modifier.weight(1f)) {
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
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column {
                    section.links.forEachIndexed { index, link ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = { navController.navigate(link.route) })
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(link.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(link.label, style = MaterialTheme.typography.titleSmall)
                                Text(link.subtitle, style = MaterialTheme.typography.bodySmall, color = InkFaint)
                            }
                            androidx.compose.material3.Icon(
                                Icons.Filled.SmartToy,
                                contentDescription = null,
                                tint = InkFaint
                            )
                        }
                        if (index < section.links.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
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
    val period: String = "30D",
    val metrics: com.hisabnikash.app.data.repo.MetricsBundle = com.hisabnikash.app.data.repo.MetricsBundle(),
    val chart: List<ChartPoint> = emptyList(),
    val topProducts: List<String> = emptyList(),
    val channels: List<String> = emptyList(),
    val expenses: List<String> = emptyList()
)

class AnalyticsViewModel(container: AppContainer) : ViewModel() {

    private val insights = container.insightsRepository
    private val periodFlow = MutableStateFlow("30D")

    val state: StateFlow<AnalyticsUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(AnalyticsUi().copy(loading = false))
            else periodFlow.flatMapLatest { period ->
                kotlinx.coroutines.flow.flow {
                    val now = System.currentTimeMillis()
                    val range = PeriodControl.resolve(period, now)
                    val metrics = insights.metrics(id, range.fromAt, range.toAt)
                    val daily = insights.dailySeries(id, range.fromAt, range.toAt)
                    val products = insights.productSales(id, range.fromAt, range.toAt)
                        .sortedByDescending { it.revenueMinor }
                        .take(6)
                    val channels = insights.byChannel(id, range.fromAt, range.toAt)
                        .sortedByDescending { it.orderCount }
                    val expenses = insights.expensesByCategory(id, range.fromAt, range.toAt)
                        .take(6)
                    emit(
                        AnalyticsUi(
                            loading = false,
                            period = period,
                            metrics = metrics,
                            chart = daily.map {
                                ChartPoint(
                                    com.hisabnikash.app.domain.model.formatDay(it.time),
                                    it.revenueMinor / com.hisabnikash.app.domain.model.MoneyScale.SCALE.toFloat(),
                                    formatMoney(it.revenueMinor)
                                )
                            },
                            topProducts = products.map { "${it.name}: ${formatMoney(it.revenueMinor)}" },
                            channels = channels.map { "${it.name ?: "Unassigned"}: ${it.orderCount} orders" },
                            expenses = expenses.map { "${it.first}: ${formatMoney(it.second)}" }
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
}

@Composable
fun AnalyticsScreenRoute(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { AnalyticsViewModel(it) }
    val state by vm.state.collectAsState()

    ScreenFrame("Analytics", onBack = { navController.popBackStack() }) {
        FilterChips(listOf("7D", "30D", "90D", "1Y"), state.period, { vm.setPeriod(it) })
        val m = state.metrics
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            MetricCard("Revenue", formatMoney(m.revenueMinor), Modifier.weight(1f))
            MetricCard("Profit", formatMoney(m.netProfitMinor), Modifier.weight(1f))
            MetricCard("Orders", "${m.deliveredCount}", Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            MetricCard("AOV", formatMoney(m.aovMinor), Modifier.weight(1f))
            MetricCard("Margin", com.hisabnikash.app.domain.model.formatPercent(m.marginBps), Modifier.weight(1f))
            MetricCard("ROAS", com.hisabnikash.app.domain.model.formatPercent(m.roasBps), Modifier.weight(1f))
        }
        SectionHeader("Revenue trend")
        BarChart(state.chart)
        SectionHeader("Top products")
        if (state.topProducts.isEmpty()) Text("No sales in this period.", color = InkFaint, modifier = Modifier.padding(horizontal = 16.dp))
        else state.topProducts.forEach {
            Text("• $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp))
        }
        SectionHeader("By channel")
        if (state.channels.isEmpty()) Text("No channel data.", color = InkFaint, modifier = Modifier.padding(horizontal = 16.dp))
        else state.channels.forEach {
            Text("• $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp))
        }
        SectionHeader("Expenses by category")
        if (state.expenses.isEmpty()) Text("No expenses in this period.", color = InkFaint, modifier = Modifier.padding(horizontal = 16.dp))
        else state.expenses.forEach {
            Text("• $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp))
        }
        Spacer(Modifier.height(20.dp))
    }
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
                    container.prefs.activeBusinessId.first()?.let { id ->
                        container.financeRepository.addChannel(id, name)
                    }
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
