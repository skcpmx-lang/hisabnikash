package com.hisabnikash.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import com.hisabnikash.app.data.container.AppContainer
import com.hisabnikash.app.data.db.AuditEventEntity
import com.hisabnikash.app.data.repo.MetricsBundle
import com.hisabnikash.app.data.repo.PeriodControl
import com.hisabnikash.app.domain.model.HealthEvaluator
import com.hisabnikash.app.domain.model.HealthIndicator
import com.hisabnikash.app.domain.model.HealthSnapshot
import com.hisabnikash.app.domain.model.HealthVerdict
import com.hisabnikash.app.domain.model.SignalLevel
import com.hisabnikash.app.domain.model.MoneyScale
import com.hisabnikash.app.domain.model.formatDateTime
import com.hisabnikash.app.domain.model.formatMoney
import com.hisabnikash.app.domain.model.formatPercent
import com.hisabnikash.app.ui.components.BarChart
import com.hisabnikash.app.ui.components.ChartPoint
import com.hisabnikash.app.ui.components.EmptyState
import com.hisabnikash.app.ui.components.FilterChips
import com.hisabnikash.app.ui.components.LinkRow
import com.hisabnikash.app.ui.components.MetricCard
import com.hisabnikash.app.ui.components.QuickActionTile
import com.hisabnikash.app.ui.components.HealthGauge
import com.hisabnikash.app.ui.components.SectionHeader
import com.hisabnikash.app.ui.components.StatRow
import com.hisabnikash.app.ui.components.TonalCard
import com.hisabnikash.app.ui.components.orderStatusColor
import com.hisabnikash.app.ui.components.SkeletonCard
import com.hisabnikash.app.ui.nav.Routes
import com.hisabnikash.app.ui.theme.BrandGold
import com.hisabnikash.app.ui.theme.BrandGoldSoft
import com.hisabnikash.app.ui.theme.BrandGreen
import com.hisabnikash.app.ui.theme.BrandGreenSoft
import com.hisabnikash.app.ui.theme.InkFaint
import com.hisabnikash.app.ui.theme.Spacing
import com.hisabnikash.app.ui.theme.SurfaceTint
import com.hisabnikash.app.ui.vm.appViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class PriorityItem(val label: String, val detail: String, val route: String)

data class HomeUiState(
    val loading: Boolean = true,
    val businessId: Long = 0,
    val businessName: String = "",
    val userName: String = "",
    val period: String = "1D",
    val metrics: MetricsBundle = MetricsBundle(),
    val previous: MetricsBundle = MetricsBundle(),
    val chart: List<ChartPoint> = emptyList(),
    val counts: Map<String, Long> = emptyMap(),
    val lowStock: List<com.hisabnikash.app.data.db.ProductEntity> = emptyList(),
    val outOfStock: List<com.hisabnikash.app.data.db.ProductEntity> = emptyList(),
    val processing: List<com.hisabnikash.app.data.db.OrderEntity> = emptyList(),
    val unread: Long = 0,
    val health: HealthVerdict? = null,
    val priorities: List<PriorityItem> = emptyList(),
    val customerCount: Int = 0,
    val productCount: Int = 0,
    val activity: List<AuditEventEntity> = emptyList()
)

class HomeViewModel(container: AppContainer) : ViewModel() {

    private val prefs = container.prefs
    private val workspace = container.workspaceRepository
    private val insights = container.insightsRepository
    private val catalog = container.catalogRepository
    private val orderRepo = container.orderRepository
    private val notifications = container.notificationRepository

    private val periodFlow = MutableStateFlow("1D")

    val state: StateFlow<HomeUiState> = prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(HomeUiState().copy(loading = false, businessId = 0))
            else buildFlow(container, id)
        }
        .stateIn(
            CoroutineScope(Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            HomeUiState()
        )

    /** Reactive state: re-computes whenever any contributing table changes. */
    private fun buildFlow(container: AppContainer, businessId: Long): Flow<HomeUiState> {
        val metricsPair = periodFlow.flatMapLatest { period ->
            val now = System.currentTimeMillis()
            val cur = PeriodControl.resolve(period, now)
            val prev = PeriodControl.previous(period, now)
            combine(
                insights.observeMetrics(businessId, cur.fromAt, cur.toAt),
                insights.observeMetrics(businessId, prev.fromAt, prev.toAt)
            ) { current, previous -> current to previous }
        }
        val chartFlow = periodFlow.flatMapLatest { period ->
            kotlinx.coroutines.flow.flow {
                emit(chartFor(container, businessId, period, System.currentTimeMillis()))
            }
        }
        val stockCounters = combine(
            catalog.observeLowStock(businessId),
            catalog.observeOutOfStock(businessId),
            orderRepo.observeProcessing(businessId),
            notifications.observeUnreadCount(businessId)
        ) { low, out, processing, unread -> LowOut(low, out, processing, unread) }
        val metaCounters = combine(
            catalog.observeCustomers(businessId).map { it.size },
            catalog.observeProducts(businessId).map { it.size },
            container.database.auditDao().observeRecent(businessId)
        ) { customers, products, activity ->
            MetaCounters(customers, products, activity)
        }
        val counters = combine(stockCounters, metaCounters) { stock, meta ->
            CounterBundle(stock, meta)
        }

        return combine(
            metricsPair,
            orderRepo.observeCounts(businessId),
            counters,
            chartFlow,
            workspace.observeActiveWorkspace()
        ) { pair, counts, counter, chart, ws ->
            val (metrics, previous) = pair
            val stock = counter.stock
            val meta = counter.meta
            val priorities = buildList {
                if (stock.processing.isNotEmpty()) add(PriorityItem("${stock.processing.size} orders to process", "Confirmed to Shipped", Routes.MAIN))
                if (stock.low.isNotEmpty()) add(PriorityItem("${stock.low.size} products low on stock", "Restock before they run out", Routes.INVENTORY))
                if (stock.out.isNotEmpty()) add(PriorityItem("${stock.out.size} products out of stock", "Create a purchase to restock", Routes.NEW_PURCHASE))
                if (metrics.codPendingMinor > 0) add(PriorityItem("Pending COD", formatMoney(metrics.codPendingMinor), Routes.SETTLEMENTS))
                if (metrics.payablesMinor > 0) add(PriorityItem("Payables outstanding", formatMoney(metrics.payablesMinor), Routes.PAYABLES))
                if (metrics.receivablesMinor > 0) add(PriorityItem("Receivables outstanding", formatMoney(metrics.receivablesMinor), Routes.RECEIVABLES))
            }
            HomeUiState(
                loading = false,
                businessId = businessId,
                businessName = ws.business?.name ?: "",
                userName = listOfNotNull(ws.settings?.firstName, ws.settings?.lastName).joinToString(" "),
                period = periodFlow.value,
                metrics = metrics,
                previous = previous,
                chart = chart,
                counts = counts,
                lowStock = stock.low,
                outOfStock = stock.out,
                processing = stock.processing,
                unread = stock.unread,
                customerCount = meta.customers,
                productCount = meta.products,
                activity = meta.activity.take(5),
                health = HealthEvaluator.evaluate(
                    HealthSnapshot(
                        revenueMinor = metrics.revenueMinor,
                        previousRevenueMinor = previous.revenueMinor,
                        marginBps = metrics.marginBps,
                        expensesMinor = metrics.expensesMinor,
                        codPendingMinor = metrics.codPendingMinor,
                        payablesMinor = metrics.payablesMinor,
                        availableCashMinor = metrics.availableCashMinor,
                        returnRateBps = metrics.returnRateBps,
                        orderCount = metrics.orderCount,
                        deliveredCount = metrics.deliveredCount,
                        lowStockCount = stock.low.size.toLong(),
                        outOfStockCount = stock.out.size.toLong(),
                        adSpendMinor = metrics.adSpendMinor,
                        roasBps = metrics.roasBps
                    )
                ),
                priorities = priorities
            )
        }
    }

    private suspend fun chartFor(container: AppContainer, businessId: Long, period: String, now: Long): List<ChartPoint> {
        val current = PeriodControl.resolve(period, now)
        return if (period == "1D") {
            container.database.orderDao().allInRange(businessId, current.fromAt, current.toAt)
                .filter { it.status == "DELIVERED" }
                .groupBy { Calendar.getInstance().apply { timeInMillis = it.orderDate }.get(Calendar.HOUR_OF_DAY) }
                .let { byHour ->
                    (0..23).map { hour ->
                        val revenue = byHour[hour]?.sumOf {
                            com.hisabnikash.app.domain.model.CommerceMath.orderRevenue(it)
                        } ?: 0L
                        ChartPoint(hourLabel(hour), revenue / MoneyScale.SCALE.toFloat(), formatMoney(revenue))
                    }
                }
        } else {
            val mode = when (period) {
                "90D" -> "WEEKLY"
                "1Y" -> "MONTHLY"
                else -> "DAILY"
            }
            insights.chartSeries(businessId, current.fromAt, current.toAt, mode).map {
                ChartPoint(
                    seriesLabel(it.time, period),
                    it.revenueMinor / MoneyScale.SCALE.toFloat(),
                    formatMoney(it.revenueMinor)
                )
            }
        }
    }

    private fun seriesLabel(time: Long, period: String): String {
        val date = java.util.Date(time)
        return if (period == "1Y") SimpleDateFormat("MMM", Locale.US).format(date)
        else SimpleDateFormat("d MMM", Locale.US).format(date)
    }

    fun setPeriod(period: String) {
        periodFlow.value = period
    }

    fun refresh(container: AppContainer) {
        // Insights are regenerated on demand after mutations; this gives the
        // user a manual "check now" path as well.
        kotlinx.coroutines.CoroutineScope(Dispatchers.Default).launch {
            prefs.activeBusinessId.first()?.let { notifications.generateInsights(it) }
        }
    }
}

private data class LowOut(
    val low: List<com.hisabnikash.app.data.db.ProductEntity>,
    val out: List<com.hisabnikash.app.data.db.ProductEntity>,
    val processing: List<com.hisabnikash.app.data.db.OrderEntity>,
    val unread: Long
)

private data class MetaCounters(
    val customers: Int,
    val products: Int,
    val activity: List<AuditEventEntity>
)

private data class CounterBundle(val stock: LowOut, val meta: MetaCounters)

@Composable
fun HomeTab(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { HomeViewModel(it) }
    val state by vm.state.collectAsState()

    if (state.loading) {
        LazyColumn {
            items(4) { SkeletonCard() }
        }
        return
    }

    val greeting = greetingText()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ------------------------------------------------------------ header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Lg, vertical = Spacing.Md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        greeting,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        state.userName.ifBlank { "Welcome back" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkFaint
                    )
                }
                Surface(
                    onClick = { navController.navigate(Routes.BUSINESS_SWITCHER) },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            state.businessName.ifBlank { "Business" },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.UnfoldMore, contentDescription = "Switch business", modifier = Modifier.size(14.dp), tint = InkFaint)
                    }
                }
                Spacer(Modifier.width(4.dp))
                BadgedBox(
                    badge = {
                        if (state.unread > 0) Badge { Text("$state.unread") }
                    }
                ) {
                    IconButton(onClick = { navController.navigate(Routes.NOTIFICATIONS) }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                    }
                }
                IconButton(onClick = { navController.navigate(Routes.SEARCH) }) {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                }
            }
        }

        // -------------------------------------------------- period selector
        item { FilterChips(listOf("1D", "7D", "10D", "30D", "90D", "1Y"), state.period, { vm.setPeriod(it) }) }

        // ------------------------------------------------------ hero snapshot
        item { DashboardHero(state) }

        // -------------------------------------------------------- performance
        item {
            TonalCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (state.period == "1D") "Revenue — today by hour" else "Revenue trend",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(Spacing.Sm))
                if (state.metrics.deliveredCount == 0L && state.metrics.revenueMinor == 0L) {
                    Text(
                        "No revenue recorded in this period. Delivered orders appear here at their actual transaction time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkFaint,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    BarChart(state.chart, maxLabels = chartMaxLabels(state.period))
                }
            }
        }

        // ------------------------------------------------------ quick actions
        item {
            SectionHeader("Quick actions")
            Column(Modifier.padding(horizontal = Spacing.Md)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Md), modifier = Modifier.fillMaxWidth()) {
                    QuickActionTile("New Order", Icons.Filled.Article, { navController.navigate(Routes.NEW_ORDER) }, Modifier.weight(1f))
                    QuickActionTile("Add Product", Icons.Filled.Inventory2, { navController.navigate(Routes.NEW_PRODUCT) }, Modifier.weight(1f))
                    QuickActionTile("Add Customer", Icons.Filled.Group, { navController.navigate(Routes.NEW_CUSTOMER) }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(Spacing.Md))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Md), modifier = Modifier.fillMaxWidth()) {
                    QuickActionTile("Record Payment", Icons.Filled.CreditCard, { navController.navigate(Routes.PAYMENT) }, Modifier.weight(1f))
                    QuickActionTile("Add Expense", Icons.Filled.ReceiptLong, { navController.navigate(Routes.EXPENSE) }, Modifier.weight(1f))
                    QuickActionTile("Create Invoice", Icons.Filled.Description, { navController.navigate(Routes.NEW_INVOICE) }, Modifier.weight(1f))
                }
            }
        }

        // -------------------------------------------------------- order pipeline
        item {
            SectionHeader("Order pipeline")
            OrderFunnelCard(state.counts) { status ->
                navController.navigate(Routes.ordersFor(status))
            }
        }

        // ------------------------------------------------------ business health
        item {
            SectionHeader("Business health")
            BusinessHealthCard(state.health)
        }

        // ------------------------------------------------------------- operations
        item {
            SectionHeader("Operations")
            TonalCard {
                Text("Today's priorities", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Spacing.Sm))
                if (state.priorities.isEmpty()) {
                    Text(
                        "All clear — no urgent items right now. New priorities appear as your business moves.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkFaint,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                } else {
                    state.priorities.forEach { item ->
                        LinkRow(item.label, item.detail, { navController.navigate(item.route) }, modifier = Modifier.padding(horizontal = 0.dp))
                    }
                }
                Spacer(Modifier.height(Spacing.Sm))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(Spacing.Sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InventoryStat("Processing", "${state.processing.size}")
                    InventoryStat("Low stock", "${state.lowStock.size}")
                    InventoryStat("Out of stock", "${state.outOfStock.size}")
                }
            }
        }

        // ------------------------------------------------------------------ money
        item {
            SectionHeader("Money")
            TonalCard {
                StatRow("Available cash", formatMoney(state.metrics.availableCashMinor))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                StatRow("COD pending", formatMoney(state.metrics.codPendingMinor))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                StatRow("Receivables", formatMoney(state.metrics.receivablesMinor))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                StatRow("Payables", formatMoney(state.metrics.payablesMinor))
            }
        }

        // ------------------------------------------------------------- intelligence
        item {
            SectionHeader("Intelligence")
            TonalCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Customers", style = MaterialTheme.typography.bodyMedium, color = InkFaint, modifier = Modifier.weight(1f))
                    Text("${state.customerCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(Spacing.Sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Products", style = MaterialTheme.typography.bodyMedium, color = InkFaint, modifier = Modifier.weight(1f))
                    Text("${state.productCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (state.metrics.adSpendMinor > 0) {
                    Spacer(Modifier.height(Spacing.Sm))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(Spacing.Sm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Campaign, contentDescription = null, tint = BrandGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Advertising", style = MaterialTheme.typography.bodyMedium, color = InkFaint, modifier = Modifier.weight(1f))
                        Text(
                            "${formatMoney(state.metrics.adSpendMinor)} • ROAS ${formatPercent(state.metrics.roasBps)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // --------------------------------------------------------- recent activity
        item {
            SectionHeader("Recent activity")
            if (state.activity.isEmpty()) {
                Text(
                    "Actions like orders, payments and stock changes appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkFaint,
                    modifier = Modifier.padding(horizontal = Spacing.Lg, vertical = Spacing.Sm)
                )
            } else {
                TonalCard {
                    state.activity.forEachIndexed { index, event ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .background(BrandGreenSoft, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                            }
                            Spacer(Modifier.width(Spacing.Md))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    activityLabel(event),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    event.detail ?: formatDateTime(event.timestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkFaint
                                )
                            }
                            Text(
                                formatDateTime(event.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = InkFaint
                            )
                        }
                        if (index < state.activity.lastIndex) {
                            Spacer(Modifier.height(Spacing.Md))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(Spacing.Md))
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------------- footer
        item {
            Spacer(Modifier.height(Spacing.Xl))
            Text(
                "HisabNikash — Your Complete Commerce OS",
                style = MaterialTheme.typography.labelSmall,
                color = InkFaint,
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Md),
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Executive hero: primary financials first, supporting cash strip beneath. */
@Composable
private fun DashboardHero(state: HomeUiState) {
    val metrics = state.metrics
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Lg, vertical = 6.dp),
        shape = MaterialTheme.shapes.large,
        elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(Spacing.Xxl)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(4.dp)
                        .background(BrandGold, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(Spacing.Sm))
                Text(
                    if (state.period == "1D") "TODAY" else state.period,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandGold,
                )
                Spacer(Modifier.weight(1f))
                val delta = deltaLabel(metrics.revenueMinor, state.previous.revenueMinor)
                Text(delta, style = MaterialTheme.typography.labelSmall, color = InkFaint)
            }
            Spacer(Modifier.height(Spacing.Md))
            Text(
                formatMoney(metrics.revenueMinor),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Revenue",
                style = MaterialTheme.typography.bodyMedium,
                color = InkFaint
            )
            Spacer(Modifier.height(Spacing.Xxl))
            Row {
                HeroStat("Net profit", formatMoney(metrics.netProfitMinor), Modifier.weight(1f), trailing = true)
                Box(Modifier.width(1.dp).height(36.dp).background(MaterialTheme.colorScheme.outlineVariant))
                HeroStat("Orders", "${metrics.deliveredCount}", Modifier.weight(1f), trailing = true)
            }
            Spacer(Modifier.height(Spacing.Lg))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(Spacing.Lg))
            Row {
                HeroStat("Available cash", formatMoney(metrics.availableCashMinor), Modifier.weight(1f))
                HeroStat("COD pending", formatMoney(metrics.codPendingMinor), Modifier.weight(1f))
                HeroStat("Receivables", formatMoney(metrics.receivablesMinor), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trailing: Boolean = false
) {
    Column(
        modifier = modifier.padding(horizontal = Spacing.Sm),
        horizontalAlignment = if (trailing) Alignment.Start else Alignment.Start
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = InkFaint)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.InventoryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = InkFaint)
    }
}

private fun activityLabel(event: AuditEventEntity): String {
    val type = event.entityType.lowercase().replaceFirstChar { it.uppercase() }
    val action = event.action.substringBefore(':')
        .lowercase()
        .split('_')
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    return if (action.isBlank()) type else "$type $action"
}

private fun greetingText(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

private fun deltaLabel(current: Long, previous: Long): String {
    if (previous == 0L) return "New this period"
    val delta = (current - previous) * 100 / previous
    return if (delta >= 0) "+$delta% vs previous" else "$delta% vs previous"
}

// ---------------------------------------------------------------------------
// Order funnel — full labels, live counts, tap-to-filter
// ---------------------------------------------------------------------------

private val PIPELINE_STATUSES = listOf("DRAFT", "CONFIRMED", "PROCESSING", "PACKED", "SHIPPED", "DELIVERED")
private val EXCEPTION_STATUSES = listOf("RETURNED", "CANCELLED")

@Composable
private fun OrderFunnelCard(counts: Map<String, Long>, onStage: (String) -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.ScreenMargin, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(Spacing.Lg)) {
            Text(
                "Sales pipeline",
                style = MaterialTheme.typography.labelMedium,
                color = InkFaint
            )
            Spacer(Modifier.height(Spacing.Sm))
            // Scrollable rail: every stage keeps its FULL label at a readable
            // size; nothing is truncated, squeezed or ellipsized.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                PIPELINE_STATUSES.forEachIndexed { index, status ->
                    FunnelStage(
                        status = status,
                        count = counts[status] ?: 0,
                        onClick = { onStage(status) }
                    )
                    if (index < PIPELINE_STATUSES.lastIndex) {
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = InkFaint,
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.CenterVertically)
                                .padding(horizontal = 2.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(Spacing.Md))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(Spacing.Md))
            Text(
                "Exception states",
                style = MaterialTheme.typography.labelMedium,
                color = InkFaint
            )
            Spacer(Modifier.height(Spacing.Sm))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Sm)
            ) {
                EXCEPTION_STATUSES.forEach { status ->
                    Surface(
                        onClick = { onStage(status) },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, orderStatusColor(status).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .background(orderStatusColor(status), CircleShape)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "${status.replaceFirstChar { it.uppercase() }} (${counts[status] ?: 0})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FunnelStage(status: String, count: Long, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        // No fixed width: the stage grows to fit its content and the rail
        // scrolls horizontally, so a stage can never truncate its label.
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(orderStatusColor(status), CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "$count",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                status.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                // softWrap=false keeps the status on ONE line; the surface
                // width follows the measured text, so nothing ever wraps,
                // clips or ellipsizes — even at 150% font scale.
                softWrap = false
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Business health — transparent, data-driven internal indicator
// ---------------------------------------------------------------------------

@Composable
private fun BusinessHealthCard(health: HealthVerdict?) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.ScreenMargin, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(Spacing.Lg)) {
            if (health == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .background(BrandGoldSoft, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.MonitorHeart,
                            contentDescription = null,
                            tint = BrandGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(Spacing.Md))
                    Column {
                        Text(
                            "NOT ENOUGH DATA",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = BrandGold
                        )
                        Text(
                            "No score yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkFaint
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.Md))
                Text(
                    "Business health is computed from your own transactions. Record at least one order, expense or stock movement and the indicator will appear — it is never guessed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkFaint
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HealthGauge(score = health.score, size = 120.dp)
                    Spacer(Modifier.width(Spacing.Xl))
                    Column(Modifier.weight(1f)) {
                        Text(health.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            health.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkFaint,
                            maxLines = 3
                        )
                        health.trendLabel?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.Lg))
                health.indicators.chunked(2).forEach { rowIndicators ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Sm)
                    ) {
                        rowIndicators.forEach { indicator ->
                            HealthIndicatorTile(indicator, Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(Spacing.Sm))
                }
                Spacer(Modifier.height(Spacing.Xs))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(Spacing.Sm))
                Text(
                    "Internal business-health indicator based on your current data. Not a credit, bank or financial rating.",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkFaint
                )
            }
        }
    }
}

@Composable
private fun HealthIndicatorTile(indicator: HealthIndicator, modifier: Modifier = Modifier) {
    val dotColor = when (indicator.level) {
        SignalLevel.GOOD -> BrandGreen
        SignalLevel.FAIR -> BrandGold
        SignalLevel.WATCH -> MaterialTheme.colorScheme.error
    }
    Column(
        modifier = modifier
            .background(SurfaceTint, MaterialTheme.shapes.small)
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).background(dotColor, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(
                indicator.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            indicator.value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun chartMaxLabels(period: String): Int = when (period) {
    "1D" -> 8
    "7D" -> 7
    "10D" -> 7
    "30D" -> 8
    "90D" -> 8
    "1Y" -> 8
    else -> 8
}

private fun hourLabel(hour: Int): String {
    val suffix = if (hour < 12) "am" else "pm"
    val display = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "$display$suffix"
}
