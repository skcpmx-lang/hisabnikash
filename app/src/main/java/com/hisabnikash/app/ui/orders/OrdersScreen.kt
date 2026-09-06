package com.hisabnikash.app.ui.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import com.hisabnikash.app.data.container.AppContainer
import com.hisabnikash.app.data.db.OrderEntity
import com.hisabnikash.app.domain.model.formatDateTime
import com.hisabnikash.app.domain.model.formatMoney
import com.hisabnikash.app.ui.components.AppTextField
import com.hisabnikash.app.ui.components.EmptyState
import com.hisabnikash.app.ui.components.FilterChips
import com.hisabnikash.app.ui.components.ScreenFrame
import com.hisabnikash.app.ui.components.StatusChip
import com.hisabnikash.app.ui.components.orderStatusColor
import com.hisabnikash.app.ui.nav.Routes
import com.hisabnikash.app.ui.theme.InkFaint
import com.hisabnikash.app.ui.vm.appViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map

data class OrdersUi(
    val loading: Boolean = true,
    val status: String = "ALL",
    val query: String = "",
    val orders: List<OrderEntity> = emptyList(),
    val customerNames: Map<Long, String> = emptyMap(),
    val channels: Map<Long, String> = emptyMap()
)

class OrdersListViewModel(container: AppContainer, initialStatus: String = "ALL") : ViewModel() {

    private val repo = container.orderRepository
    private val catalog = container.catalogRepository
    private val finance = container.financeRepository

    private val statusFlow = kotlinx.coroutines.flow.MutableStateFlow(initialStatus)
    private val queryFlow = kotlinx.coroutines.flow.MutableStateFlow("")

    val state: StateFlow<OrdersUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(OrdersUi().copy(loading = false))
            else combine(statusFlow, queryFlow) { s, q -> s to q }
                .flatMapLatest { (status, query) ->
                    combine(
                        repo.observeOrders(id, status, query),
                        catalog.observeCustomers(id).map { list ->
                            list.associate { it.customer.id to it.customer.name }
                        },
                        finance.observeChannels(id)
                    ) { orders, names, channels ->
                        OrdersUi(
                            loading = false,
                            status = status,
                            query = query,
                            orders = orders,
                            customerNames = names,
                            channels = channels.associate { it.id to it.name }
                        )
                    }
                }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            OrdersUi()
        )

    fun setStatus(status: String) {
        statusFlow.value = status
    }

    fun setQuery(query: String) {
        queryFlow.value = query
    }
}

private val ORDER_STATUSES = listOf(
    "ALL", "DRAFT", "CONFIRMED", "PROCESSING", "PACKED", "SHIPPED",
    "DELIVERED", "RETURNED", "CANCELLED"
)

@Composable
private fun OrdersBody(
    state: OrdersUi,
    navController: NavHostController,
    onQuery: (String) -> Unit,
    onStatus: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    AppTextField(
        "Search",
        query,
        { query = it; onQuery(it) },
        placeholder = "Order number or product"
    )
    FilterChips(ORDER_STATUSES, state.status, onStatus)
    if (state.orders.isEmpty()) {
        EmptyState(
            Icons.Filled.Article,
            if (state.query.isBlank()) "No orders yet" else "No matches found",
            if (state.query.isBlank())
                "Add your first order to start tracking sales and profit."
            else "Try a different search term or status.",
            actionLabel = if (state.query.isBlank()) "Create Order" else null,
            onAction = { navController.navigate(Routes.NEW_ORDER) }
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(state.orders, key = { it.id }) { order ->
                OrderRow(
                    order,
                    customerName = order.customerId?.let { state.customerNames[it] },
                    channelName = order.channelId?.let { state.channels[it] },
                    onClick = { navController.navigate(Routes.order(order.id)) }
                )
            }
        }
    }
}

@Composable
fun OrdersTab(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { OrdersListViewModel(it) }
    val state by vm.state.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Routes.NEW_ORDER) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New Order") }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Orders",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
            }
            OrdersBody(state, navController, vm::setQuery, vm::setStatus)
        }
    }
}

/**
 * Full-screen orders list opened from the dashboard funnel with an initial
 * status already applied. Back returns to the dashboard.
 */
@Composable
fun OrdersByStatusRoute(container: AppContainer, navController: NavHostController, status: String) {
    val vm = appViewModel(container, key = "orders-status-$status") {
        OrdersListViewModel(it, initialStatus = status)
    }
    val state by vm.state.collectAsState()

    ScreenFrame(
        title = "${status.replaceFirstChar { it.uppercase() }} orders",
        onBack = { navController.popBackStack() },
        scroll = false
    ) {
        OrdersBody(state, navController, vm::setQuery, vm::setStatus)
    }
}

@Composable
fun OrderRow(
    order: OrderEntity,
    customerName: String?,
    channelName: String?,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        order.orderNo,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusChip(order.status.replaceFirstChar { it.uppercase() }, orderStatusColor(order.status))
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    customerName ?: "Walk-in customer",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    listOfNotNull(formatDateTime(order.orderDate), channelName).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint,
                    maxLines = 1
                )
            }
            Text(
                formatMoney(order.totalMinor),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
