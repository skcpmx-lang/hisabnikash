package com.hisabnikash.app.ui.orders

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import com.hisabnikash.app.data.container.AppContainer
import com.hisabnikash.app.data.db.OrderStatusHistoryEntity
import com.hisabnikash.app.data.db.OrderWithItems
import com.hisabnikash.app.data.db.PaymentEntity
import com.hisabnikash.app.domain.model.CommerceMath
import com.hisabnikash.app.domain.model.formatDateTime
import com.hisabnikash.app.domain.model.formatMoney
import com.hisabnikash.app.domain.model.parseMoneyInput
import com.hisabnikash.app.ui.components.AppDropdown
import com.hisabnikash.app.ui.components.AppTextField
import com.hisabnikash.app.ui.components.DropOption
import com.hisabnikash.app.ui.components.LabelValueRow
import com.hisabnikash.app.ui.components.MetricCard
import com.hisabnikash.app.ui.components.MoneyField
import com.hisabnikash.app.ui.components.ScreenFrame
import com.hisabnikash.app.ui.components.SectionHeader
import com.hisabnikash.app.ui.components.StatusChip
import com.hisabnikash.app.ui.components.orderStatusColor
import com.hisabnikash.app.ui.nav.Routes
import com.hisabnikash.app.ui.theme.BrandGreen
import com.hisabnikash.app.ui.theme.Error
import com.hisabnikash.app.ui.theme.InkFaint
import com.hisabnikash.app.ui.vm.appViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OrderDetailUi(
    val loading: Boolean = true,
    val businessId: Long = 0,
    val order: OrderWithItems? = null,
    val history: List<OrderStatusHistoryEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
    val customerName: String? = null,
    val channelName: String? = null,
    val courierName: String? = null,
    val accounts: List<DropOption> = emptyList()
)

class OrderDetailViewModel(container: AppContainer, private val orderId: Long) : ViewModel() {

    private val orders = container.orderRepository
    private val catalog = container.catalogRepository
    private val finance = container.financeRepository

    val state: StateFlow<OrderDetailUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(OrderDetailUi().copy(loading = false))
            else {
                val orderFlow = orders.observeOrderWithItems(orderId)
                val historyFlow = orders.observeStatusHistory(orderId)
                val paymentsFlow = orders.observePaymentsForOrder(id, orderId)
                val customersFlow = catalog.observeCustomers(id).map { list ->
                    list.associate { it.customer.id to it.customer.name }
                }
                val channelsFlow = finance.observeChannels(id).map { list ->
                    list.associate { it.id to it.name }
                }
                val couriersFlow = container.database.courierDao().observeAll(id).map { list ->
                    list.associate { it.id to it.name }
                }
                val accountsFlow = finance.observeAccounts(id).map { list ->
                    list.map { DropOption("${it.id}", it.name) }
                }
                val left = combine(orderFlow, historyFlow, paymentsFlow, customersFlow, channelsFlow) {
                        order, history, payments, customers, channels ->
                    OrderDetailLeft(order, history, payments, customers, channels)
                }
                val right = combine(couriersFlow, accountsFlow) { couriers, accounts ->
                    OrderDetailRight(couriers, accounts)
                }
                combine(left, right) { l, r ->
                    OrderDetailUi(
                        loading = false,
                        businessId = id,
                        order = l.order,
                        history = l.history,
                        payments = l.payments,
                        customerName = l.order?.order?.customerId?.let { l.customers[it] },
                        channelName = l.order?.order?.channelId?.let { l.channels[it] },
                        courierName = l.order?.order?.courierId?.let { r.couriers[it] },
                        accounts = r.accounts
                    )
                }
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            OrderDetailUi()
        )
}

@Composable
fun OrderDetailRoute(container: AppContainer, navController: NavHostController, orderId: Long) {
    val vm = appViewModel(container, key = "order-$orderId") { OrderDetailViewModel(it, orderId) }
    val state by vm.state.collectAsState()
    var updating by remember { mutableStateOf(false) }
    var updateError by remember { mutableStateOf<String?>(null) }
    var cancelReason by remember { mutableStateOf("") }
    var payAmount by remember { mutableStateOf<Long>(0) }
    var payMethod by remember { mutableStateOf("CASH") }
    var payAccount by remember { mutableStateOf<Long?>(null) }
    var paying by remember { mutableStateOf(false) }

    val order = state.order?.order

    fun changeStatus(toStatus: String, reason: String? = null) {
        updating = true
        updateError = null
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val result = runCatching { container.orderRepository.updateStatus(orderId, toStatus, reason) }
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                updating = false
                result.onFailure { updateError = it.message ?: "Couldn't update order." }
            }
        }
    }

    ScreenFrame(
        title = order?.orderNo ?: "Order",
        onBack = { navController.popBackStack() },
        subtitle = order?.let { formatDateTime(it.orderDate) }
    ) {
        if (order == null) {
            Text("Order not found.", color = InkFaint, modifier = Modifier.padding(16.dp))
            return@ScreenFrame
        }
        val total = order.totalMinor
        val revenue = CommerceMath.orderRevenue(order)
        val cogs = state.order?.items?.sumOf { it.qty * it.unitCostMinor } ?: 0
        val costs = CommerceMath.orderCosts(order)
        val profit = revenue - cogs - costs
        val items = state.order?.items ?: emptyList()
        val context = LocalContext.current
        val orderSummary = buildString {
            appendLine("${order.orderNo} — ${order.status}")
            appendLine("Date: ${formatDateTime(order.orderDate)}")
            appendLine("Customer: ${state.customerName ?: "Walk-in customer"}")
            appendLine("Channel: ${state.channelName ?: "—"}")
            appendLine("Courier: ${state.courierName ?: "—"}")
            order.trackingNo?.let { appendLine("Tracking: $it") }
            appendLine("")
            items.forEach { item ->
                appendLine("${item.qty} × ${item.name} — ${formatMoney(item.lineTotalMinor)}")
            }
            appendLine("")
            appendLine("Subtotal: ${formatMoney(order.subtotalMinor)}")
            if (order.discountMinor > 0) appendLine("Discount: -${formatMoney(order.discountMinor)}")
            if (order.deliveryChargeMinor > 0) appendLine("Delivery: ${formatMoney(order.deliveryChargeMinor)}")
            appendLine("Total: ${formatMoney(total)}")
            appendLine("Payment: ${order.paymentMethod} (advance ${formatMoney(order.advanceMinor)}${if (order.codMinor > 0) ", COD ${formatMoney(order.codMinor)}" else ""})")
        }

        SectionHeader("Share")
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, order.orderNo)
                        putExtra(Intent.EXTRA_TEXT, orderSummary)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share order"))
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Share")
            }
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(order.orderNo, orderSummary))
                    Toast.makeText(context, "Order ${order.orderNo} copied", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Copy")
            }
        }

        SectionHeader("Order")
        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(order.status.replaceFirstChar { it.uppercase() }, orderStatusColor(order.status))
                    Spacer(Modifier.width(8.dp))
                    Text("Payment: ${order.paymentMethod}", style = MaterialTheme.typography.labelMedium, color = InkFaint)
                }
                Spacer(Modifier.height(6.dp))
                LabelValueRow("Customer", state.customerName ?: "Walk-in customer")
                LabelValueRow("Channel", state.channelName ?: "—")
                LabelValueRow("Courier", state.courierName ?: "—")
                LabelValueRow("Tracking", order.trackingNo ?: "—")
                LabelValueRow("Tags", order.tags ?: "—")
                order.note?.let { LabelValueRow("Note", it) }
            }
        }

        SectionHeader("Financials")
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            MetricCard("Revenue", formatMoney(revenue), Modifier.weight(1f))
            MetricCard("COGS", formatMoney(cogs), Modifier.weight(1f))
            MetricCard("Profit", formatMoney(profit), Modifier.weight(1f))
        }
        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            Column(Modifier.padding(16.dp)) {
                LabelValueRow("Subtotal", formatMoney(order.subtotalMinor))
                LabelValueRow("Discount", formatMoney(order.discountMinor))
                LabelValueRow("Delivery charge", formatMoney(order.deliveryChargeMinor))
                LabelValueRow("Customer total", formatMoney(total))
                LabelValueRow("Advance received", formatMoney(order.advanceMinor))
                LabelValueRow("COD remaining", formatMoney((order.codMinor).coerceAtLeast(0)))
                LabelValueRow("Courier fee", formatMoney(order.courierFeeMinor))
                LabelValueRow("Return courier fee", formatMoney(order.returnCourierFeeMinor))
                LabelValueRow("Packaging", formatMoney(order.packagingMinor))
                LabelValueRow("Advertising", formatMoney(order.advertisingMinor))
                LabelValueRow("Other costs", formatMoney(order.otherCostMinor))
            }
        }

        SectionHeader("Items")
        items.forEach { item ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${item.qty} × ${formatMoney(item.unitPriceMinor)} (cost ${formatMoney(item.unitCostMinor)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkFaint
                        )
                    }
                    Text(
                        formatMoney(item.lineTotalMinor),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        SectionHeader("Update status")
        if (updating) {
            Text("Updating…", color = InkFaint, modifier = Modifier.padding(horizontal = 16.dp))
        } else {
            val next = when (order.status) {
                "DRAFT" -> "CONFIRMED" to "Confirm order"
                "CONFIRMED" -> "PROCESSING" to "Start processing"
                "PROCESSING" -> "PACKED" to "Mark packed"
                "PACKED" -> "SHIPPED" to "Mark shipped"
                "SHIPPED" -> "DELIVERED" to "Delivered"
                else -> null
            }
            if (next != null) {
                Button(
                    onClick = { changeStatus(next.first) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(next.second)
                }
            }
            OutlinedButton(
                onClick = { changeStatus("RETURNED", cancelReason.ifBlank { "Returned from delivery" }) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                enabled = order.status != "RETURNED" && order.status != "CANCELLED"
            ) {
                Icon(Icons.Filled.Replay, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Mark returned")
            }
            if (order.status != "CANCELLED" && order.status != "DELIVERED") {
                AppTextField(
                    "Return/cancel reason",
                    cancelReason,
                    { cancelReason = it },
                    placeholder = "Optional — e.g. customer refused parcel"
                )
                OutlinedButton(
                    onClick = { changeStatus("CANCELLED", cancelReason.ifBlank { "Cancelled" }) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text("Cancel order")
                }
            }
            updateError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        SectionHeader("Money actions")
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { navController.navigate(Routes.returnForOrder(order.id)) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Reply, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Return")
            }
            OutlinedButton(onClick = { navController.navigate(Routes.exchangeForOrder(order.id)) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.CurrencyExchange, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Exchange")
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { navController.navigate(Routes.refundForOrder(order.id)) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Replay, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Refund")
            }
            OutlinedButton(onClick = { navController.navigate(Routes.NEW_INVOICE) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Description, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Invoice")
            }
        }

        SectionHeader("Record payment")
        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            Column(Modifier.padding(16.dp)) {
                MoneyField(
                    "Amount received",
                    payAmount,
                    { payAmount = it },
                    enabled = order.status != "CANCELLED"
                )
                AppDropdown(
                    "Method",
                    listOf(
                        DropOption("CASH", "Cash"),
                        DropOption("BKASH", "bKash"),
                        DropOption("NAGAD", "Nagad"),
                        DropOption("BANK", "Bank"),
                        DropOption("COD", "COD"),
                        DropOption("CARD", "Card"),
                        DropOption("OTHER", "Other")
                    ),
                    payMethod,
                    { payMethod = it.id }
                )
                AppDropdown("Into account", state.accounts, payAccount?.toString(), { payAccount = it.id.toLong() }, placeholder = "Account optional")
                Button(
                    onClick = {
                        val amount = payAmount
                        if (amount <= 0) {
                            updateError = "Enter an amount greater than zero."
                            return@Button
                        }
                        paying = true
                        updateError = null
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                            val result = runCatching {
                                container.orderRepository.recordPayment(
                                    com.hisabnikash.app.data.repo.PaymentInput(
                                        businessId = container.workspaceRepository.requireActiveBusiness(),
                                        customerId = order.customerId,
                                        orderId = order.id,
                                        accountId = payAccount,
                                        amountMinor = amount,
                                        method = payMethod
                                    )
                                )
                            }
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                paying = false
                                result.onSuccess {
                                    payAmount = 0
                                    updateError = null
                                }.onFailure { updateError = it.message ?: "Couldn't record payment." }
                            }
                        }
                    },
                    enabled = !paying,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                ) { Text(if (paying) "Recording…" else "Record payment") }
            }
        }

        SectionHeader("Payments")
        if (state.payments.isEmpty()) {
            Text(
                "No payments recorded yet.",
                color = InkFaint,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            state.payments.forEach { payment ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(payment.method, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(formatDateTime(payment.dateAt), style = MaterialTheme.typography.bodySmall, color = InkFaint)
                        }
                        Text(
                            formatMoney(payment.amountMinor),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BrandGreen
                        )
                    }
                }
            }
        }

        SectionHeader("Status history")
        if (state.history.isEmpty()) {
            Text("No history yet.", color = InkFaint, modifier = Modifier.padding(horizontal = 16.dp))
        } else {
            state.history.reversed().forEach { h ->
                Text(
                    "${h.fromStatus ?: "—"} → ${h.toStatus}" + (h.reason?.let { " • $it" } ?: "") + " • ${formatDateTime(h.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

private data class OrderDetailLeft(
    val order: OrderWithItems?,
    val history: List<OrderStatusHistoryEntity>,
    val payments: List<PaymentEntity>,
    val customers: Map<Long, String>,
    val channels: Map<Long, String>
)

private data class OrderDetailRight(
    val couriers: Map<Long, String>,
    val accounts: List<DropOption>
)
