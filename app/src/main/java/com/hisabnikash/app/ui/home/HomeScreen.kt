package com.hisabnikash.app.ui.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
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
import com.hisabnikash.app.data.repo.MetricsBundle
import com.hisabnikash.app.data.repo.PeriodControl
import com.hisabnikash.app.domain.model.MoneyScale
import com.hisabnikash.app.domain.model.formatMoney
import com.hisabnikash.app.domain.model.formatPercent
import com.hisabnikash.app.ui.components.BarChart
import com.hisabnikash.app.ui.components.ChartPoint
import com.hisabnikash.app.ui.components.EmptyState
import com.hisabnikash.app.ui.components.FilterChips
import com.hisabnikash.app.ui.components.LinkRow
import com.hisabnikash.app.ui.components.MetricCard
import com.hisabnikash.app.ui.components.QuickActionTile
import com.hisabnikash.app.ui.components.SectionHeader
import com.hisabnikash.app.ui.components.SkeletonCard
import com.hisabnikash.app.ui.components.StatusChip
import com.hisabnikash.app.ui.nav.Routes
import com.hisabnikash.app.ui.theme.BrandGold
import com.hisabnikash.app.ui.theme.BrandGreen
import com.hisabnikash.app.ui.theme.InkFaint
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

data class HealthStatus(val score: Int, val label: String, val note: String)

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
    val health: HealthStatus? = null,
    val priorities: List<PriorityItem> = emptyList()
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
        val counters = combine(
            catalog.observeLowStock(businessId),
            catalog.observeOutOfStock(businessId),
            orderRepo.observeProcessing(businessId),
            notifications.observeUnreadCount(businessId)
        ) { low, out, processing, unread -> LowOut(low, out, processing, unread) }

        return combine(
            metricsPair,
            orderRepo.observeCounts(businessId),
            counters,
            chartFlow,
            workspace.observeActiveWorkspace()
        ) { pair, counts, counter, chart, ws ->
            val (metrics, previous) = pair
            val priorities = buildList {
                if (counter.processing.isNotEmpty()) add(PriorityItem("${counter.processing.size} orders to process", "Confirmed to Shipped", Routes.MAIN))
                if (counter.low.isNotEmpty()) add(PriorityItem("${counter.low.size} products low on stock", "Restock before they run out", Routes.INVENTORY))
                if (counter.out.isNotEmpty()) add(PriorityItem("${counter.out.size} products out of stock", "Create a purchase to restock", Routes.NEW_PURCHASE))
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
                lowStock = counter.low,
                outOfStock = counter.out,
                processing = counter.processing,
                unread = counter.unread,
                health = computeHealth(metrics, previous),
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
            insights.dailySeries(businessId, current.fromAt, current.toAt).map {
                ChartPoint(
                    SimpleDateFormat("d MMM", Locale.US).format(java.util.Date(it.time)),
                    it.revenueMinor / MoneyScale.SCALE.toFloat(),
                    formatMoney(it.revenueMinor)
                )
            }
        }
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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(greeting, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        state.userName.ifBlank { "Welcome" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkFaint
                    )
                }
                OutlinedButton(onClick = { navController.navigate(Routes.BUSINESS_SWITCHER) }) {
                    Text(state.businessName.ifBlank { "Business" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Filled.UnfoldMore, contentDescription = "Switch business", modifier = Modifier.size(16.dp))
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

        item { FilterChips(listOf("1D", "7D", "10D", "30D", "90D", "1Y", "Custom"), state.period, { vm.setPeriod(it) }) }

        item {
            SectionHeader("Today's overview")
            Column(Modifier.padding(horizontal = 12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard("Revenue", formatMoney(state.metrics.revenueMinor), Modifier.weight(1f))
                    MetricCard("Net profit", formatMoney(state.metrics.netProfitMinor), Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard("Orders", "${state.metrics.deliveredCount}", Modifier.weight(1f))
                    MetricCard("Expenses", formatMoney(state.metrics.expensesMinor), Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard("Available cash", formatMoney(state.metrics.availableCashMinor), Modifier.weight(1f))
                    MetricCard("COD pending", formatMoney(state.metrics.codPendingMinor), Modifier.weight(1f))
                }
            }
        }

        item {
            SectionHeader(if (state.period == "1D") "Revenue — today by hour" else "Revenue")
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                if (state.metrics.deliveredCount == 0L && state.period == "1D") {
                    EmptyState(
                        Icons.Filled.PointOfSale,
                        "No revenue recorded today",
                        "Delivered orders appear here at the actual transaction time."
                    )
                } else {
                    BarChart(state.chart)
                }
            }
        }

        item {
            SectionHeader("Quick actions")
            Column(Modifier.padding(horizontal = 12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickActionTile("New Order", Icons.Filled.Article, { navController.navigate(Routes.NEW_ORDER) }, Modifier.weight(1f))
                    QuickActionTile("Add Product", Icons.Filled.Inventory2, { navController.navigate(Routes.NEW_PRODUCT) }, Modifier.weight(1f))
                    QuickActionTile("Add Customer", Icons.Filled.Group, { navController.navigate(Routes.NEW_CUSTOMER) }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickActionTile("Record Payment", Icons.Filled.CreditCard, { navController.navigate(Routes.PAYMENT) }, Modifier.weight(1f))
                    QuickActionTile("Add Expense", Icons.Filled.ReceiptLong, { navController.navigate(Routes.EXPENSE) }, Modifier.weight(1f))
                    QuickActionTile("Create Invoice", Icons.Filled.Description, { navController.navigate(Routes.NEW_INVOICE) }, Modifier.weight(1f))
                }
            }
        }

        item {
            SectionHeader("Order funnel")
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        listOf("Draft", "Confirmed", "Processing", "Packed", "Shipped", "Delivered", "Returned", "Cancelled")
                            .forEach { status ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text(
                                        "${state.counts[status.uppercase()] ?: 0}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(status, style = MaterialTheme.typography.labelSmall, color = InkFaint, maxLines = 1)
                                }
                            }
                    }
                    Row(Modifier.padding(top = 8.dp)) {
                        StatusChip("Live database counts", BrandGreen)
                    }
                }
            }
        }

        item {
            SectionHeader("Business pulse")
            Column(Modifier.padding(horizontal = 12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard("Sales", formatMoney(state.metrics.revenueMinor), Modifier.weight(1f),
                        trend = deltaLabel(state.metrics.revenueMinor, state.previous.revenueMinor),
                        trendUp = state.metrics.revenueMinor >= state.previous.revenueMinor)
                    MetricCard("Profit", formatMoney(state.metrics.netProfitMinor), Modifier.weight(1f),
                        trend = deltaLabel(state.metrics.netProfitMinor, state.previous.netProfitMinor),
                        trendUp = state.metrics.netProfitMinor >= state.previous.netProfitMinor)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard("Orders", "${state.metrics.orderCount}", Modifier.weight(1f))
                    MetricCard("AOV", formatMoney(state.metrics.aovMinor), Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard("Returns", "${state.metrics.returnedCount} (${formatPercent(state.metrics.returnRateBps)})", Modifier.weight(1f))
                    MetricCard("Margin", formatPercent(state.metrics.marginBps), Modifier.weight(1f))
                }
            }
        }

        item {
            SectionHeader("Business health")
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(Modifier.padding(16.dp)) {
                    val health = state.health
                    if (health == null) {
                        Text("Not enough data", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Record orders to unlock your business health indicator.",
                            style = MaterialTheme.typography.bodyMedium, color = InkFaint
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${health.score}",
                                style = MaterialTheme.typography.displaySmall,
                                color = BrandGold,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(health.label, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    health.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkFaint,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionHeader("Today's priorities")
            if (state.priorities.isEmpty()) {
                EmptyState(
                    Icons.Filled.SwapHoriz,
                    "All clear",
                    "No urgent items right now. New priorities appear as your business moves."
                )
            } else {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        state.priorities.forEach { item ->
                            LinkRow(item.label, item.detail, { navController.navigate(item.route) })
                        }
                    }
                }
            }
        }

        item {
            SectionHeader("Inventory watch")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                MetricCard("Processing orders", "${state.processing.size}", Modifier.weight(1f))
                MetricCard("Low stock", "${state.lowStock.size}", Modifier.weight(1f))
                MetricCard("Out of stock", "${state.outOfStock.size}", Modifier.weight(1f))
            }
        }

        item {
            Spacer(Modifier.height(20.dp))
            Text(
                "HisabNikash — Your Complete Commerce OS",
                style = MaterialTheme.typography.labelSmall,
                color = InkFaint,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                textAlign = TextAlign.Center
            )
        }
    }
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

private fun computeHealth(current: MetricsBundle, previous: MetricsBundle): HealthStatus {
    val score = computeHealthScore(current, previous)
    val label = when {
        score >= 85 -> "Excellent"
        score >= 65 -> "Good"
        score >= 40 -> "Needs attention"
        else -> "At risk"
    }
    val note = when {
        score >= 85 -> "Revenue and profit are tracking well. Keep the momentum."
        score >= 65 -> "Steady performance with room to improve margins or cash flow."
        score >= 40 -> "Check costs, pending COD and stock levels to protect profit."
        else -> "Several signals are weak. Review expenses and outstanding cash today."
    }
    return HealthStatus(score, label, note)
}

private fun computeHealthScore(current: MetricsBundle, previous: MetricsBundle): Int {
    var score = 70
    if (current.revenueMinor > 0) {
        score += when {
            previous.revenueMinor == 0L -> 5
            current.revenueMinor >= previous.revenueMinor -> 5
            current.revenueMinor >= previous.revenueMinor * 8 / 10 -> 0
            else -> -10
        }
    } else if (previous.revenueMinor > 0) {
        score -= 15
    }
    if (current.marginBps >= 2_500) score += 10
    else if (current.marginBps >= 1_200) score += 5
    else if (current.marginBps in 1..1_199) score -= 5
    if (current.codPendingMinor > 0) score -= 5
    if (current.payablesMinor > current.revenueMinor && current.revenueMinor > 0) score -= 10
    if (current.returnRateBps > 1_500) score -= 5
    return score.coerceIn(0, 100)
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
