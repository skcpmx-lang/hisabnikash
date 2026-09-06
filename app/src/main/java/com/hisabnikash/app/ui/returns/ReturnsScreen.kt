package com.hisabnikash.app.ui.returns

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.hisabnikash.app.domain.model.parseMoneyInput
import com.hisabnikash.app.ui.components.AppDropdown
import com.hisabnikash.app.ui.components.AppTextField
import com.hisabnikash.app.ui.components.DropOption
import com.hisabnikash.app.ui.components.EmptyState
import com.hisabnikash.app.ui.components.LabelValueRow
import com.hisabnikash.app.ui.components.MoneyField
import com.hisabnikash.app.ui.components.ScreenFrame
import com.hisabnikash.app.ui.components.SectionHeader
import com.hisabnikash.app.ui.components.StatusChip
import com.hisabnikash.app.ui.nav.Routes
import com.hisabnikash.app.ui.theme.BrandGreen
import com.hisabnikash.app.ui.theme.Error
import com.hisabnikash.app.ui.theme.InkFaint
import com.hisabnikash.app.ui.theme.Warning
import com.hisabnikash.app.ui.vm.appViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Common: order picker state
// ---------------------------------------------------------------------------

data class OrderPickerState(
    val businessId: Long = 0,
    val orders: List<OrderEntity> = emptyList(),
    val selectedOrder: OrderEntity? = null,
    val selectedOrderId: Long? = null
)

internal class OrderPickerFlow(container: AppContainer) {

    private val db = container.database
    private val selectEvent = MutableStateFlow<Long?>(null)

    val state: StateFlow<OrderPickerState> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(OrderPickerState())
            else db.orderDao().observeFiltered(id, "ALL", "").map { orders ->
                OrderPickerState(
                    businessId = id,
                    orders = orders.sortedByDescending { it.orderDate },
                    selectedOrderId = selectEvent.value ?: orders.maxByOrNull { it.orderDate }?.id,
                    selectedOrder = orders.firstOrNull { it.id == (selectEvent.value ?: orders.maxByOrNull { it.orderDate }?.id) }
                )
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            OrderPickerState()
        )

    fun select(id: Long) { selectEvent.value = id }
}

@Composable
private fun OrderPicker(
    picker: OrderPickerFlow,
    onSelect: (Long) -> Unit,
    header: String
) {
    val state by picker.state.collectAsState()
    SectionHeader(header)
    AppDropdown(
        "Order",
        state.orders.map { DropOption("${it.id}", it.orderNo, "${formatDateTime(it.orderDate)} • ${formatMoney(it.totalMinor)}") },
        state.selectedOrderId?.toString(),
        { onSelect(it.id.toLong()) },
        emptyTitle = "No orders yet",
        emptyHint = "Create an order first, then process returns against it."
    )
}

// ---------------------------------------------------------------------------
// Return form
// ---------------------------------------------------------------------------

data class ReturnLine(
    val productId: Long,
    val name: String,
    val qtyText: String = "1",
    val refundText: String = ""
) {
    val qty: Long get() = qtyText.toLongOrNull()?.coerceAtLeast(1) ?: 1
    val refundMinor: Long get() = parseMoneyInput(refundText) ?: 0
}

data class ReturnFormUi(
    val businessId: Long = 0,
    val orders: List<OrderEntity> = emptyList(),
    val selectedOrderId: Long? = null,
    val orderItems: List<com.hisabnikash.app.data.db.OrderItemEntity> = emptyList(),
    val reason: String = "CUSTOMER_RESPONSIBILITY",
    val responsibility: String = "customer",
    val lines: List<ReturnLine> = emptyList(),
    val refundProductText: String = "",
    val refundDeliveryText: String = "",
    val retainedDeliveryText: String = "",
    val forwardCourierText: String = "",
    val returnCourierText: String = "",
    val refundMethod: String? = "CASH",
    val refundAccountId: Long? = null,
    val note: String = "",
    val refundNow: Boolean = false,
    val accounts: List<DropOption> = emptyList(),
    val saving: Boolean = false,
    val error: String? = null
)

class ReturnFormViewModel(container: AppContainer, initialOrderId: Long? = null) : ViewModel() {

    private val db = container.database
    private val finance = container.financeRepository
    private val orders = container.orderRepository
    private val form = MutableStateFlow(ReturnFormUi())
    private val selectEvent = MutableStateFlow(initialOrderId)

    val state: StateFlow<ReturnFormUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(form.value)
            else db.orderDao().observeFiltered(id, "ALL", "").map { orders ->
                val selectedId = selectEvent.value?.takeIf { id -> orders.any { it.id == id } } ?: form.value.selectedOrderId ?: orders.maxByOrNull { it.orderDate }?.id
                form.value.copy(
                    businessId = id,
                    orders = orders.sortedByDescending { it.orderDate },
                    selectedOrderId = selectedId
                )
            }.flatMapLatest { f ->
                val orderId = f.selectedOrderId
                if (orderId == null) flowOf(f.copy(orderItems = emptyList(), lines = emptyList()))
                else db.orderDao().observeItemsFor(orderId).map { items ->
                    f.copy(
                        orderItems = items,
                        lines = if (f.lines.isEmpty()) items.map {
                            ReturnLine(
                                productId = it.productId ?: 0,
                                name = it.name,
                                qtyText = "${it.qty}",
                                refundText = com.hisabnikash.app.domain.model.formatMoneyPlain(it.lineTotalMinor)
                            )
                        } else f.lines
                    )
                }
            }.flatMapLatest { f ->
                combine(
                    flowOf(f),
                    finance.observeAccounts(id).map { list -> list.map { DropOption("${it.id}", it.name) } }
                ) { f2, accounts -> f2.copy(accounts = accounts) }
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            ReturnFormUi()
        )

    fun selectOrder(id: Long) { selectEvent.value = id }
    fun setReason(v: String) { form.value = form.value.copy(reason = v) }
    fun setResponsibility(v: String) { form.value = form.value.copy(responsibility = v) }
    fun setRefundProduct(v: Long) { form.value = form.value.copy(refundProductText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }
    fun setRefundDelivery(v: Long) { form.value = form.value.copy(refundDeliveryText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }
    fun setRetained(v: Long) { form.value = form.value.copy(retainedDeliveryText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }
    fun setForwardCourier(v: Long) { form.value = form.value.copy(forwardCourierText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }
    fun setReturnCourier(v: Long) { form.value = form.value.copy(returnCourierText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }
    fun setRefundMethod(v: String?) { form.value = form.value.copy(refundMethod = v) }
    fun setRefundAccount(v: Long?) { form.value = form.value.copy(refundAccountId = v) }
    fun setNote(v: String) { form.value = form.value.copy(note = v) }
    fun setRefundNow(v: Boolean) { form.value = form.value.copy(refundNow = v) }

    fun updateLine(index: Int, line: ReturnLine) {
        val f = form.value
        val list = f.lines.toMutableList()
        if (index in list.indices) list[index] = line
        form.value = f.copy(lines = list)
    }

    fun save(onSaved: () -> Unit) {
        val f = form.value
        if (f.selectedOrderId == null) {
            form.value = f.copy(error = "Choose an order.")
            return
        }
        if (f.lines.isEmpty()) {
            form.value = f.copy(error = "Add at least one return item.")
            return
        }
        form.value = f.copy(saving = true, error = null)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val result = runCatching {
                orders.createReturn(
                    com.hisabnikash.app.data.repo.ReturnInput(
                        businessId = f.businessId,
                        orderId = f.selectedOrderId,
                        customerId = f.orders.firstOrNull { it.id == f.selectedOrderId }?.customerId,
                        reason = f.reason,
                        responsibility = f.responsibility,
                        items = f.lines.map {
                            com.hisabnikash.app.data.repo.ReturnInputItem(
                                productId = it.productId,
                                qty = it.qty,
                                unitRefundMinor = it.refundMinor
                            )
                        },
                        refundProductMinor = parseMoneyInput(f.refundProductText) ?: 0,
                        refundDeliveryMinor = parseMoneyInput(f.refundDeliveryText) ?: 0,
                        deliveryChargeRetainedMinor = parseMoneyInput(f.retainedDeliveryText) ?: 0,
                        forwardCourierMinor = parseMoneyInput(f.forwardCourierText) ?: 0,
                        returnCourierMinor = parseMoneyInput(f.returnCourierText) ?: 0,
                        refundMethod = f.refundMethod,
                        refundAccountId = f.refundAccountId,
                        note = f.note.ifBlank { null },
                        refundNow = f.refundNow
                    )
                )
            }
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                result.onSuccess { onSaved() }.onFailure { e ->
                    form.value = f.copy(saving = false, error = e.message ?: "Couldn't create return.")
                }
            }
        }
    }
}

@Composable
fun ReturnFormRoute(container: AppContainer, navController: NavHostController, orderId: Long? = null) {
    val vm = appViewModel(container, key = orderId?.let { "return-form-$it" }) { ReturnFormViewModel(it, orderId) }
    val state by vm.state.collectAsState()

    ScreenFrame("New return", onBack = { navController.popBackStack() }) {
        AppDropdown(
            "Order",
            state.orders.map { DropOption("${it.id}", it.orderNo, "${formatDateTime(it.orderDate)} • ${formatMoney(it.totalMinor)}") },
            state.selectedOrderId?.toString(),
            { vm.selectOrder(it.id.toLong()) },
            emptyTitle = "No orders yet",
            emptyHint = "Create an order first, then process a return against it."
        )
        AppDropdown(
            "Reason",
            listOf(
                DropOption("CUSTOMER_RESPONSIBILITY", "Customer responsibility"),
                DropOption("MERCHANT_RESPONSIBILITY", "Merchant responsibility"),
                DropOption("POLICY_RESPONSIBILITY", "Policy"),
                DropOption("CUSTOM_RESPONSIBILITY", "Custom")
            ),
            state.reason,
            { vm.setReason(it.id) }
        )
        AppDropdown(
            "Who bears the cost",
            listOf(
                DropOption("customer", "Customer"),
                DropOption("merchant", "Merchant"),
                DropOption("policy", "Store policy"),
                DropOption("custom", "Custom")
            ),
            state.responsibility,
            { vm.setResponsibility(it.id) }
        )
        SectionHeader("Items returned")
        state.lines.forEachIndexed { index, line ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(line.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            AppTextField(
                                "Qty returned",
                                line.qtyText,
                                { vm.updateLine(index, line.copy(qtyText = it.filter(Char::isDigit))) },
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            MoneyField("Unit refund", line.refundMinor, { vm.updateLine(index, line.copy(refundText = com.hisabnikash.app.domain.model.formatMoneyPlain(it))) })
                        }
                    }
                }
            }
        }
        SectionHeader("Refund & costs")
        MoneyField("Product refund", parseMoneyInput(state.refundProductText) ?: 0, { vm.setRefundProduct(it) })
        MoneyField("Delivery refund", parseMoneyInput(state.refundDeliveryText) ?: 0, { vm.setRefundDelivery(it) })
        MoneyField("Delivery charge retained", parseMoneyInput(state.retainedDeliveryText) ?: 0, { vm.setRetained(it) })
        MoneyField("Forward courier cost", parseMoneyInput(state.forwardCourierText) ?: 0, { vm.setForwardCourier(it) })
        MoneyField("Return courier cost", parseMoneyInput(state.returnCourierText) ?: 0, { vm.setReturnCourier(it) })
        AppDropdown(
            "Refund method",
            listOf(
                DropOption("CASH", "Cash"),
                DropOption("BKASH", "bKash"),
                DropOption("NAGAD", "Nagad"),
                DropOption("BANK", "Bank"),
                DropOption("OTHER", "Other")
            ),
            state.refundMethod,
            { vm.setRefundMethod(it.id) }
        )
        AppDropdown("Refund from account", state.accounts, state.refundAccountId?.toString(), { vm.setRefundAccount(it.id.toLong()) }, placeholder = "Optional")
        AppTextField("Note", state.note, { vm.setNote(it) }, singleLine = false, minLines = 2)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Refund immediately", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            androidx.compose.material3.Switch(checked = state.refundNow, onCheckedChange = { vm.setRefundNow(it) })
        }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
        Button(
            onClick = { vm.save { navController.popBackStack() } },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)
        ) { Text(if (state.saving) "Saving…" else "Create return") }
    }
}

// ---------------------------------------------------------------------------
// Exchange form
// ---------------------------------------------------------------------------

data class ExchangeLine(
    val originalProductId: Long,
    val originalName: String,
    val originalQtyText: String = "1",
    val replacementProductId: Long? = null,
    val replacementQtyText: String = "1"
) {
    val originalQty: Long get() = originalQtyText.toLongOrNull()?.coerceAtLeast(1) ?: 1
    val replacementQty: Long get() = replacementQtyText.toLongOrNull()?.coerceAtLeast(1) ?: 1
}

data class ExchangeFormUi(
    val businessId: Long = 0,
    val orders: List<OrderEntity> = emptyList(),
    val selectedOrderId: Long? = null,
    val orderItems: List<com.hisabnikash.app.data.db.OrderItemEntity> = emptyList(),
    val products: List<DropOption> = emptyList(),
    val lines: List<ExchangeLine> = emptyList(),
    val priceDiffText: String = "",
    val extraPaidText: String = "",
    val refundText: String = "",
    val courierText: String = "",
    val note: String = "",
    val saving: Boolean = false,
    val error: String? = null
)

class ExchangeFormViewModel(container: AppContainer, initialOrderId: Long? = null) : ViewModel() {

    private val db = container.database
    private val orders = container.orderRepository
    private val catalog = container.catalogRepository
    private val form = MutableStateFlow(ExchangeFormUi())
    private val selectEvent = MutableStateFlow(initialOrderId)

    val state: StateFlow<ExchangeFormUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(form.value)
            else combine(
                db.orderDao().observeFiltered(id, "ALL", ""),
                catalog.observeProducts(id).map { list ->
                    list.map {
                        DropOption("${it.product.id}", it.product.name, "In stock ${it.product.stockQty}")
                    }
                }
            ) { orders, products ->
                val selectedId = selectEvent.value?.takeIf { id -> orders.any { it.id == id } } ?: form.value.selectedOrderId ?: orders.maxByOrNull { it.orderDate }?.id
                form.value.copy(businessId = id, orders = orders.sortedByDescending { it.orderDate }, selectedOrderId = selectedId, products = products)
            }.flatMapLatest { f ->
                val orderId = f.selectedOrderId
                if (orderId == null) flowOf(f.copy(orderItems = emptyList(), lines = emptyList()))
                else db.orderDao().observeItemsFor(orderId).map { items ->
                    f.copy(
                        orderItems = items,
                        lines = if (f.lines.isEmpty()) items.map {
                            ExchangeLine(originalProductId = it.productId ?: 0, originalName = it.name, originalQtyText = "${it.qty}")
                        } else f.lines
                    )
                }
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            ExchangeFormUi()
        )

    fun selectOrder(id: Long) { selectEvent.value = id }
    fun setPriceDiff(v: Long) { form.value = form.value.copy(priceDiffText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }
    fun setExtraPaid(v: Long) { form.value = form.value.copy(extraPaidText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }
    fun setRefund(v: Long) { form.value = form.value.copy(refundText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }
    fun setCourier(v: Long) { form.value = form.value.copy(courierText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }
    fun setNote(v: String) { form.value = form.value.copy(note = v) }

    fun updateLine(index: Int, line: ExchangeLine) {
        val f = form.value
        val list = f.lines.toMutableList()
        if (index in list.indices) list[index] = line
        form.value = f.copy(lines = list)
    }

    fun save(onSaved: () -> Unit) {
        val f = form.value
        if (f.selectedOrderId == null) {
            form.value = f.copy(error = "Choose an order.")
            return
        }
        if (f.lines.isEmpty()) {
            form.value = f.copy(error = "Add at least one exchange item.")
            return
        }
        form.value = f.copy(saving = true, error = null)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val result = runCatching {
                orders.createExchange(
                    com.hisabnikash.app.data.repo.ExchangeInput(
                        businessId = f.businessId,
                        orderId = f.selectedOrderId,
                        items = f.lines.map {
                            com.hisabnikash.app.data.repo.ExchangeInputItem(
                                originalProductId = it.originalProductId,
                                originalQty = it.originalQty,
                                replacementProductId = it.replacementProductId,
                                replacementQty = it.replacementQty
                            )
                        },
                        priceDiffMinor = parseMoneyInput(f.priceDiffText) ?: 0,
                        extraPaidMinor = parseMoneyInput(f.extraPaidText) ?: 0,
                        refundMinor = parseMoneyInput(f.refundText) ?: 0,
                        courierMinor = parseMoneyInput(f.courierText) ?: 0,
                        note = f.note.ifBlank { null }
                    )
                )
            }
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                result.onSuccess { onSaved() }.onFailure { e ->
                    form.value = f.copy(saving = false, error = e.message ?: "Couldn't create exchange.")
                }
            }
        }
    }
}

@Composable
fun ExchangeFormRoute(container: AppContainer, navController: NavHostController, orderId: Long? = null) {
    val vm = appViewModel(container, key = orderId?.let { "exchange-form-$it" }) { ExchangeFormViewModel(it, orderId) }
    val state by vm.state.collectAsState()

    ScreenFrame("New exchange", onBack = { navController.popBackStack() }) {
        AppDropdown(
            "Order",
            state.orders.map { DropOption("${it.id}", it.orderNo, "${formatDateTime(it.orderDate)} • ${formatMoney(it.totalMinor)}") },
            state.selectedOrderId?.toString(),
            { vm.selectOrder(it.id.toLong()) },
            emptyTitle = "No orders yet",
            emptyHint = "Create an order first, then process an exchange."
        )
        SectionHeader("Swap items")
        state.lines.forEachIndexed { index, line ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(line.originalName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    AppTextField(
                        "Qty returned",
                        line.originalQtyText,
                        { vm.updateLine(index, line.copy(originalQtyText = it.filter(Char::isDigit))) },
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                    AppDropdown(
                        "Replace with",
                        state.products,
                        line.replacementProductId?.toString(),
                        { vm.updateLine(index, line.copy(replacementProductId = it.id.toLong())) },
                        placeholder = "Same product",
                        emptyTitle = "No products",
                        emptyHint = "Add products to the catalog first."
                    )
                    AppTextField(
                        "Replacement qty",
                        line.replacementQtyText,
                        { vm.updateLine(index, line.copy(replacementQtyText = it.filter(Char::isDigit))) },
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                }
            }
        }
        SectionHeader("Money")
        MoneyField("Price difference", parseMoneyInput(state.priceDiffText) ?: 0, { vm.setPriceDiff(it) })
        MoneyField("Extra paid by customer", parseMoneyInput(state.extraPaidText) ?: 0, { vm.setExtraPaid(it) })
        MoneyField("Refund to customer", parseMoneyInput(state.refundText) ?: 0, { vm.setRefund(it) })
        MoneyField("Courier cost", parseMoneyInput(state.courierText) ?: 0, { vm.setCourier(it) })
        AppTextField("Note", state.note, { vm.setNote(it) }, singleLine = false, minLines = 2)
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
        Button(
            onClick = { vm.save { navController.popBackStack() } },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)
        ) { Text(if (state.saving) "Saving…" else "Create exchange") }
    }
}

// ---------------------------------------------------------------------------
// Refund form + list
// ---------------------------------------------------------------------------

data class RefundFormUi(
    val businessId: Long = 0,
    val orders: List<OrderEntity> = emptyList(),
    val selectedOrderId: Long? = null,
    val customerId: Long? = null,
    val amountText: String = "",
    val method: String = "CASH",
    val accountId: Long? = null,
    val reason: String = "",
    val note: String = "",
    val accounts: List<DropOption> = emptyList(),
    val saving: Boolean = false,
    val error: String? = null
)

class RefundFormViewModel(container: AppContainer, initialOrderId: Long? = null) : ViewModel() {

    private val db = container.database
    private val finance = container.financeRepository
    private val orders = container.orderRepository
    private val form = MutableStateFlow(RefundFormUi())
    private val selectEvent = MutableStateFlow(initialOrderId)

    val state: StateFlow<RefundFormUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(form.value)
            else combine(
                db.orderDao().observeFiltered(id, "ALL", ""),
                finance.observeAccounts(id).map { list -> list.map { DropOption("${it.id}", it.name) } }
            ) { orders, accounts ->
                val selectedId = selectEvent.value?.takeIf { id -> orders.any { it.id == id } } ?: form.value.selectedOrderId ?: orders.maxByOrNull { it.orderDate }?.id
                form.value.copy(
                    businessId = id,
                    orders = orders.sortedByDescending { it.orderDate },
                    selectedOrderId = selectedId,
                    accounts = accounts,
                    customerId = orders.firstOrNull { it.id == selectedId }?.customerId,
                    accountId = form.value.accountId ?: accounts.firstOrNull()?.id?.toLong()
                )
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            RefundFormUi()
        )

    fun selectOrder(id: Long) { selectEvent.value = id }
    fun setAmount(v: Long) { form.value = form.value.copy(amountText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }
    fun setMethod(v: String) { form.value = form.value.copy(method = v) }
    fun setAccount(v: Long?) { form.value = form.value.copy(accountId = v) }
    fun setReason(v: String) { form.value = form.value.copy(reason = v) }
    fun setNote(v: String) { form.value = form.value.copy(note = v) }

    fun save(onSaved: () -> Unit) {
        val f = form.value
        val amount = parseMoneyInput(f.amountText) ?: 0
        if (amount <= 0) {
            form.value = f.copy(error = "Enter a refund amount greater than zero.")
            return
        }
        form.value = f.copy(saving = true, error = null)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val result = runCatching {
                orders.createRefund(
                    businessId = f.businessId,
                    customerId = f.customerId,
                    orderId = f.selectedOrderId,
                    amountMinor = amount,
                    method = f.method,
                    accountId = f.accountId,
                    reason = f.reason.ifBlank { null },
                    note = f.note.ifBlank { null }
                )
            }
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                result.onSuccess { onSaved() }.onFailure { e ->
                    form.value = f.copy(saving = false, error = e.message ?: "Couldn't create refund.")
                }
            }
        }
    }
}

@Composable
fun RefundFormRoute(container: AppContainer, navController: NavHostController, orderId: Long? = null) {
    val vm = appViewModel(container, key = orderId?.let { "refund-form-$it" }) { RefundFormViewModel(it, orderId) }
    val state by vm.state.collectAsState()

    ScreenFrame("New refund", onBack = { navController.popBackStack() }) {
        AppDropdown(
            "Order",
            state.orders.map { DropOption("${it.id}", it.orderNo, "${formatDateTime(it.orderDate)} • ${formatMoney(it.totalMinor)}") },
            state.selectedOrderId?.toString(),
            { vm.selectOrder(it.id.toLong()) },
            emptyTitle = "No orders yet",
            emptyHint = "Create an order first, then record a refund."
        )
        MoneyField("Refund amount", parseMoneyInput(state.amountText) ?: 0, { vm.setAmount(it) })
        AppDropdown(
            "Method",
            listOf(
                DropOption("CASH", "Cash"),
                DropOption("BKASH", "bKash"),
                DropOption("NAGAD", "Nagad"),
                DropOption("BANK", "Bank"),
                DropOption("OTHER", "Other")
            ),
            state.method,
            { vm.setMethod(it.id) }
        )
        AppDropdown("From account", state.accounts, state.accountId?.toString(), { vm.setAccount(it.id.toLong()) })
        AppTextField("Reason", state.reason, { vm.setReason(it) }, placeholder = "e.g. wrong item delivered")
        AppTextField("Note", state.note, { vm.setNote(it) }, singleLine = false, minLines = 2)
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
        Button(
            onClick = { vm.save { navController.popBackStack() } },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)
        ) { Text(if (state.saving) "Saving…" else "Create refund") }
    }
}

@Composable
fun RefundsListRoute(container: AppContainer, navController: NavHostController) {
    val refunds by container.workspaceRepository.observeActiveBusinessId()
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(emptyList())
            else container.database.refundDao().observeAll(id)
        }
        .collectAsState(initial = emptyList())

    ScreenFrame("Refunds", onBack = { navController.popBackStack() }) {
        if (refunds.isEmpty()) {
            EmptyState(
                Icons.Filled.Replay,
                "No refunds yet",
                "Refund documents appear here with their exact ledger impact.",
                actionLabel = "New Refund",
                onAction = { navController.navigate(Routes.NEW_REFUND) }
            )
        } else {
            refunds.forEach { refund ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(refund.refundNo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            StatusChip("Refunded", Error)
                        }
                        LabelValueRow("Amount", formatMoney(refund.amountMinor))
                        LabelValueRow("Method", refund.method)
                        LabelValueRow("Date", formatDateTime(refund.dateAt))
                        refund.reason?.let { LabelValueRow("Reason", it) }
                    }
                }
            }
        }
        Button(
            onClick = { navController.navigate(Routes.NEW_REFUND) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) { Text("New refund") }
    }
}

@Composable
fun ReturnsListRoute(container: AppContainer, navController: NavHostController) {
    val returns by container.workspaceRepository.observeActiveBusinessId()
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(emptyList())
            else container.database.returnDao().observeFiltered(id, "ALL")
        }
        .collectAsState(initial = emptyList())

    ScreenFrame("Returns", onBack = { navController.popBackStack() }) {
        if (returns.isEmpty()) {
            EmptyState(
                Icons.Filled.Reply,
                "No returns yet",
                "Returns restore stock and show refunds honestly.",
                actionLabel = "New Return",
                onAction = { navController.navigate(Routes.NEW_RETURN) }
            )
        } else {
            returns.forEach { returnOrder ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(returnOrder.returnNo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            StatusChip(returnOrder.status, if (returnOrder.status == "COMPLETED") BrandGreen else Warning)
                        }
                        LabelValueRow("Order", "#${returnOrder.orderId}")
                        LabelValueRow("Reason", returnOrder.reason.replaceFirstChar { it.uppercase() })
                        LabelValueRow("Product refund", formatMoney(returnOrder.refundProductMinor))
                        LabelValueRow("Date", formatDateTime(returnOrder.createdAt))
                    }
                }
            }
        }
        Button(
            onClick = { navController.navigate(Routes.NEW_RETURN) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) { Text("New return") }
    }
}

@Composable
fun ExchangesListRoute(container: AppContainer, navController: NavHostController) {
    val exchanges by container.workspaceRepository.observeActiveBusinessId()
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(emptyList())
            else container.database.exchangeDao().observeAll(id)
        }
        .collectAsState(initial = emptyList())

    ScreenFrame("Exchanges", onBack = { navController.popBackStack() }) {
        if (exchanges.isEmpty()) {
            EmptyState(
                Icons.Filled.CurrencyExchange,
                "No exchanges yet",
                "Track swaps, price differences and refunds in one place.",
                actionLabel = "New Exchange",
                onAction = { navController.navigate(Routes.NEW_EXCHANGE) }
            )
        } else {
            exchanges.forEach { exchange ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(exchange.exchangeNo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            StatusChip(exchange.status, if (exchange.status == "COMPLETED") BrandGreen else Warning)
                        }
                        LabelValueRow("Order", "#${exchange.orderId}")
                        LabelValueRow("Price difference", formatMoney(exchange.priceDiffMinor))
                        LabelValueRow("Refund", formatMoney(exchange.refundMinor))
                        LabelValueRow("Date", formatDateTime(exchange.createdAt))
                    }
                }
            }
        }
        Button(
            onClick = { navController.navigate(Routes.NEW_EXCHANGE) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) { Text("New exchange") }
    }
}
