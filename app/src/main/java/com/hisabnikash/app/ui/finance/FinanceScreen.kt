package com.hisabnikash.app.ui.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RequestQuote
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
import com.hisabnikash.app.data.db.BudgetEntity
import com.hisabnikash.app.data.db.CampaignEntity
import com.hisabnikash.app.data.db.CourierEntity
import com.hisabnikash.app.data.db.PayableEntity
import com.hisabnikash.app.data.db.ReceivableEntity
import com.hisabnikash.app.domain.model.formatDate
import com.hisabnikash.app.domain.model.formatDateTime
import com.hisabnikash.app.domain.model.formatMoney
import com.hisabnikash.app.domain.model.parseMoneyInput
import com.hisabnikash.app.ui.components.AppDropdown
import com.hisabnikash.app.ui.components.AppTextField
import com.hisabnikash.app.ui.components.DateField
import com.hisabnikash.app.ui.components.DropOption
import com.hisabnikash.app.ui.components.EmptyState
import com.hisabnikash.app.ui.components.FilterChips
import com.hisabnikash.app.ui.components.LabelValueRow
import com.hisabnikash.app.ui.components.MetricCard
import com.hisabnikash.app.ui.components.MoneyField
import com.hisabnikash.app.ui.components.ScreenFrame
import com.hisabnikash.app.ui.components.SectionHeader
import com.hisabnikash.app.ui.components.StatusChip
import com.hisabnikash.app.ui.nav.Routes
import com.hisabnikash.app.ui.theme.BrandGreen
import com.hisabnikash.app.ui.theme.BrandGold
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
// Receivables
// ---------------------------------------------------------------------------

data class ReceivablesUi(
    val loading: Boolean = true,
    val status: String = "OPEN",
    val items: List<ReceivableEntity> = emptyList(),
    val outstanding: Long = 0,
    val customers: Map<Long, String> = emptyMap()
)

class ReceivablesViewModel(container: AppContainer) : ViewModel() {

    private val finance = container.financeRepository
    private val catalog = container.catalogRepository
    private val statusFlow = MutableStateFlow("OPEN")

    val state: StateFlow<ReceivablesUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(ReceivablesUi().copy(loading = false))
            else statusFlow.flatMapLatest { status ->
                combine(
                    finance.observeReceivables(id, if (status == "OPEN") "ALL" else status),
                    finance.observeReceivablesOutstanding(id),
                    catalog.observeCustomers(id).map { list -> list.associate { it.customer.id to it.customer.name } }
                ) { items, outstanding, customers ->
                    val filtered = if (status == "OPEN") items.filter { it.status in setOf("PENDING", "PARTIAL", "OVERDUE") }
                    else items
                    ReceivablesUi(loading = false, status = status, items = filtered, outstanding = outstanding, customers = customers)
                }
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            ReceivablesUi()
        )

    fun setStatus(status: String) { statusFlow.value = status }
}

@Composable
fun ReceivablesScreenRoute(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { ReceivablesViewModel(it) }
    val state by vm.state.collectAsState()

    ScreenFrame("Receivables", onBack = { navController.popBackStack() }) {
        MetricCard("Outstanding", formatMoney(state.outstanding), Modifier.fillMaxWidth().padding(horizontal = 12.dp))
        FilterChips(listOf("OPEN", "PARTIAL", "OVERDUE", "PAID"), state.status, { vm.setStatus(it) })
        if (state.items.isEmpty()) {
            EmptyState(
                Icons.Filled.RequestQuote,
                "Nothing due",
                "COD receivables from delivered orders and custom receivables appear here."
            )
        } else {
            state.items.forEach { receivable ->
                val customerName = receivable.customerId?.let { state.customers[it] }
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    customerName ?: "Walk-in / courier",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(6.dp))
                                StatusChip(receivable.status, receivableStatusColor(receivable.status))
                            }
                            Text(
                                "${receivable.sourceType} • created ${formatDateTime(receivable.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkFaint,
                                maxLines = 1
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                formatMoney((receivable.amountMinor - receivable.paidMinor).coerceAtLeast(0)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (receivable.paidMinor >= receivable.amountMinor) BrandGreen else Warning
                            )
                            Text(
                                "of ${formatMoney(receivable.amountMinor)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkFaint
                            )
                        }
                    }
                }
            }
        }
        Button(
            onClick = { navController.navigate(Routes.RECEIVABLE_PAY) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) { Icon(Icons.Filled.Payments, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Record a payment") }
    }
}

@Composable
fun receivableStatusColor(status: String) = when (status) {
    "PAID" -> BrandGreen
    "OVERDUE" -> Error
    else -> Warning
}

// ---------------------------------------------------------------------------
// Payables
// ---------------------------------------------------------------------------

data class PayablesUi(
    val loading: Boolean = true,
    val status: String = "OPEN",
    val items: List<PayableEntity> = emptyList(),
    val outstanding: Long = 0,
    val suppliers: Map<Long, String> = emptyMap()
)

class PayablesViewModel(container: AppContainer) : ViewModel() {

    private val finance = container.financeRepository
    private val catalog = container.catalogRepository
    private val statusFlow = MutableStateFlow("OPEN")

    val state: StateFlow<PayablesUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(PayablesUi().copy(loading = false))
            else statusFlow.flatMapLatest { status ->
                combine(
                    finance.observePayables(id, if (status == "OPEN") "ALL" else status),
                    finance.observePayablesOutstanding(id),
                    catalog.observeSuppliers(id).map { list -> list.associate { it.id to it.name } }
                ) { items, outstanding, suppliers ->
                    val filtered = if (status == "OPEN") items.filter { it.status in setOf("PENDING", "PARTIAL", "OVERDUE") } else items
                    PayablesUi(loading = false, status = status, items = filtered, outstanding = outstanding, suppliers = suppliers)
                }
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            PayablesUi()
        )

    fun setStatus(status: String) { statusFlow.value = status }
}

@Composable
fun PayablesScreenRoute(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { PayablesViewModel(it) }
    val state by vm.state.collectAsState()

    ScreenFrame("Payables", onBack = { navController.popBackStack() }) {
        MetricCard("Outstanding", formatMoney(state.outstanding), Modifier.fillMaxWidth().padding(horizontal = 12.dp))
        FilterChips(listOf("OPEN", "PARTIAL", "OVERDUE", "PAID"), state.status, { vm.setStatus(it) })
        if (state.items.isEmpty()) {
            EmptyState(
                Icons.Filled.Paid,
                "Nothing owed",
                "Purchase payables and custom payables appear here."
            )
        } else {
            state.items.forEach { payable ->
                val supplierName = payable.supplierId?.let { state.suppliers[it] }
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    supplierName ?: "Other payable",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(6.dp))
                                StatusChip(payable.status, receivableStatusColor(payable.status))
                            }
                            Text(
                                "${payable.sourceType} • created ${formatDateTime(payable.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkFaint,
                                maxLines = 1
                            )
                        }
                        Text(
                            formatMoney((payable.amountMinor - payable.paidMinor).coerceAtLeast(0)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Warning
                        )
                    }
                }
            }
        }
        Button(
            onClick = { navController.navigate(Routes.PAYABLE_PAY) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) { Icon(Icons.Filled.Payments, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Record a payment out") }
    }
}

// ---------------------------------------------------------------------------
// Payment forms (receive / pay)
// ---------------------------------------------------------------------------

data class PayForm(
    val businessId: Long = 0,
    val targetId: Long? = null,
    val amountText: String = "",
    val method: String = "CASH",
    val accountId: Long? = null,
    val note: String = "",
    val items: List<DropOption> = emptyList(),
    val accounts: List<DropOption> = emptyList(),
    val customerName: String = "",
    val saving: Boolean = false,
    val error: String? = null
)

class ReceivePaymentViewModel(container: AppContainer, private val receivableId: Long?) : ViewModel() {

    private val finance = container.financeRepository
    private val orders = container.orderRepository
    private val catalog = container.catalogRepository
    private val form = MutableStateFlow(PayForm())

    val state: StateFlow<PayForm> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(form.value)
            else combine(
                form,
                finance.observeReceivables(id, "ALL").map { list ->
                    list.filter { it.status != "PAID" }.map {
                        DropOption(
                            "${it.id}",
                            "Receivable #${it.id} • ${formatMoney(it.amountMinor - it.paidMinor)}",
                            it.sourceType
                        )
                    }
                },
                finance.observeAccounts(id).map { list ->
                    list.map { DropOption("${it.id}", it.name) }
                }
            ) { f, items, accounts ->
                f.copy(
                    businessId = id,
                    items = items,
                    accounts = accounts,
                    targetId = f.targetId ?: receivableId ?: items.firstOrNull()?.id?.toLong(),
                    accountId = f.accountId ?: accounts.firstOrNull()?.id?.toLong()
                )
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            PayForm()
        )

    fun setTarget(id: Long?) { form.value = form.value.copy(targetId = id) }
    fun setAmount(v: Long) { form.value = form.value.copy(amountText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }
    fun setMethod(v: String) { form.value = form.value.copy(method = v) }
    fun setAccount(v: Long?) { form.value = form.value.copy(accountId = v) }
    fun setNote(v: String) { form.value = form.value.copy(note = v) }

    fun save(onSaved: () -> Unit) {
        val f = form.value
        val amount = parseMoneyInput(f.amountText) ?: 0
        if (f.targetId == null) {
            form.value = f.copy(error = "Choose a receivable to pay.")
            return
        }
        if (amount <= 0) {
            form.value = f.copy(error = "Enter an amount greater than zero.")
            return
        }
        form.value = f.copy(saving = true, error = null)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val result = runCatching {
                orders.recordPayment(
                    com.hisabnikash.app.data.repo.PaymentInput(
                        businessId = f.businessId,
                        customerId = null,
                        receivableId = f.targetId,
                        accountId = f.accountId,
                        amountMinor = amount,
                        method = f.method,
                        note = f.note.ifBlank { null }
                    )
                )
            }
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                result.onSuccess { onSaved() }.onFailure { e ->
                    form.value = f.copy(saving = false, error = e.message ?: "Couldn't record payment.")
                }
            }
        }
    }
}

@Composable
fun ReceivePaymentRoute(container: AppContainer, navController: NavHostController, receivableId: Long?) {
    val vm = appViewModel(container, key = "receive-pay-${receivableId ?: 0}") {
        ReceivePaymentViewModel(it, receivableId)
    }
    val state by vm.state.collectAsState()

    ScreenFrame("Record payment", onBack = { navController.popBackStack() }) {
        if (state.items.isNotEmpty()) {
            AppDropdown("Receivable", state.items, state.targetId?.toString(), { vm.setTarget(it.id.toLong()) })
        }
        MoneyField("Amount received", parseMoneyInput(state.amountText) ?: 0, { vm.setAmount(it) })
        AppDropdown(
            "Method",
            listOf(
                DropOption("CASH", "Cash"),
                DropOption("BKASH", "bKash"),
                DropOption("NAGAD", "Nagad"),
                DropOption("BANK", "Bank"),
                DropOption("COD", "COD settlement"),
                DropOption("CARD", "Card"),
                DropOption("OTHER", "Other")
            ),
            state.method,
            { vm.setMethod(it.id) }
        )
        AppDropdown("Into account", state.accounts, state.accountId?.toString(), { vm.setAccount(it.id.toLong()) })
        AppTextField("Note", state.note, { vm.setNote(it) }, singleLine = false, minLines = 2)
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
        Button(
            onClick = { vm.save { navController.popBackStack() } },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
        ) { Text(if (state.saving) "Recording…" else "Record payment") }
    }
}

class PayOutViewModel(container: AppContainer, private val payableId: Long?) : ViewModel() {

    private val finance = container.financeRepository
    private val orders = container.orderRepository
    private val form = MutableStateFlow(PayForm())

    val state: StateFlow<PayForm> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(form.value)
            else combine(
                form,
                finance.observePayables(id, "ALL").map { list ->
                    list.filter { it.status != "PAID" }.map {
                        DropOption(
                            "${it.id}",
                            "Payable #${it.id} • ${formatMoney(it.amountMinor - it.paidMinor)}",
                            it.sourceType
                        )
                    }
                },
                finance.observeAccounts(id).map { list ->
                    list.map { DropOption("${it.id}", it.name) }
                }
            ) { f, items, accounts ->
                f.copy(
                    businessId = id,
                    items = items,
                    accounts = accounts,
                    targetId = f.targetId ?: payableId ?: items.firstOrNull()?.id?.toLong(),
                    accountId = f.accountId ?: accounts.firstOrNull()?.id?.toLong()
                )
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            PayForm()
        )

    fun setTarget(id: Long?) { form.value = form.value.copy(targetId = id) }
    fun setAmount(v: Long) { form.value = form.value.copy(amountText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }
    fun setMethod(v: String) { form.value = form.value.copy(method = v) }
    fun setAccount(v: Long?) { form.value = form.value.copy(accountId = v) }
    fun setNote(v: String) { form.value = form.value.copy(note = v) }

    fun save(onSaved: () -> Unit) {
        val f = form.value
        val amount = parseMoneyInput(f.amountText) ?: 0
        if (f.targetId == null) {
            form.value = f.copy(error = "Choose a payable.")
            return
        }
        if (amount <= 0) {
            form.value = f.copy(error = "Enter an amount greater than zero.")
            return
        }
        form.value = f.copy(saving = true, error = null)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val result = runCatching {
                orders.recordPayablePayment(
                    businessId = f.businessId,
                    payableId = f.targetId,
                    accountId = f.accountId,
                    amountMinor = amount,
                    method = f.method,
                    note = f.note.ifBlank { null }
                )
            }
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                result.onSuccess { onSaved() }.onFailure { e ->
                    form.value = f.copy(saving = false, error = e.message ?: "Couldn't record payment.")
                }
            }
        }
    }
}

@Composable
fun PayOutRoute(container: AppContainer, navController: NavHostController, payableId: Long?) {
    val vm = appViewModel(container, key = "payout-${payableId ?: 0}") { PayOutViewModel(it, payableId) }
    val state by vm.state.collectAsState()

    ScreenFrame("Pay payable", onBack = { navController.popBackStack() }) {
        if (state.items.isNotEmpty()) {
            AppDropdown("Payable", state.items, state.targetId?.toString(), { vm.setTarget(it.id.toLong()) })
        }
        MoneyField("Amount paid", parseMoneyInput(state.amountText) ?: 0, { vm.setAmount(it) })
        AppDropdown(
            "Method",
            listOf(
                DropOption("CASH", "Cash"),
                DropOption("BKASH", "bKash"),
                DropOption("NAGAD", "Nagad"),
                DropOption("BANK", "Bank"),
                DropOption("CARD", "Card"),
                DropOption("OTHER", "Other")
            ),
            state.method,
            { vm.setMethod(it.id) }
        )
        AppDropdown("From account", state.accounts, state.accountId?.toString(), { vm.setAccount(it.id.toLong()) })
        AppTextField("Note", state.note, { vm.setNote(it) }, singleLine = false, minLines = 2)
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
        Button(
            onClick = { vm.save { navController.popBackStack() } },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)
        ) { Text(if (state.saving) "Recording…" else "Make payment") }
    }
}

// ---------------------------------------------------------------------------
// Courier settlements
// ---------------------------------------------------------------------------

data class SettlementsUi(
    val loading: Boolean = true,
    val settlements: List<com.hisabnikash.app.data.db.CourierSettlementEntity> = emptyList(),
    val couriers: Map<Long, String> = emptyMap()
)

class SettlementsViewModel(container: AppContainer) : ViewModel() {

    private val db = container.database
    private val couriers = container.database.courierDao()

    val state: StateFlow<SettlementsUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(SettlementsUi().copy(loading = false))
            else combine(
                couriers.observeSettlements(id),
                couriers.observeAll(id)
            ) { settlements, courierList ->
                SettlementsUi(loading = false, settlements = settlements, couriers = courierList.associate { it.id to it.name })
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            SettlementsUi()
        )
}

@Composable
fun SettlementsScreenRoute(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { SettlementsViewModel(it) }
    val state by vm.state.collectAsState()

    ScreenFrame("Courier settlements", onBack = { navController.popBackStack() }) {
        if (state.settlements.isEmpty()) {
            EmptyState(
                Icons.Filled.LocalShipping,
                "No settlements yet",
                "Record COD cash and fees received from couriers.",
                actionLabel = "Settle cash",
                onAction = { navController.navigate(Routes.SETTLEMENT) }
            )
        } else {
            state.settlements.forEach { settlement ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                state.couriers[settlement.courierId] ?: "Courier #${settlement.courierId}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                formatMoney(settlement.amountMinor),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreen
                            )
                        }
                        LabelValueRow("COD total", formatMoney(settlement.codMinor))
                        LabelValueRow("Fees", formatMoney(settlement.feeMinor))
                        LabelValueRow("Pending", formatMoney(settlement.pendingMinor))
                        LabelValueRow("Date", formatDateTime(settlement.dateAt))
                    }
                }
            }
        }
        Button(
            onClick = { navController.navigate(Routes.SETTLEMENT) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) { Text("Record settlement") }
    }
}

data class SettlementForm(
    val businessId: Long = 0,
    val courierId: Long? = null,
    val codText: String = "",
    val cashText: String = "",
    val feeText: String = "",
    val accountId: Long? = null,
    val note: String = "",
    val couriers: List<DropOption> = emptyList(),
    val accounts: List<DropOption> = emptyList(),
    val saving: Boolean = false,
    val error: String? = null
)

class SettlementFormViewModel(container: AppContainer) : ViewModel() {

    private val orders = container.orderRepository
    private val finance = container.financeRepository
    private val form = MutableStateFlow(SettlementForm())

    val state: StateFlow<SettlementForm> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(form.value)
            else combine(
                form,
                container.database.courierDao().observeAll(id).map { list ->
                    list.map { DropOption("${it.id}", it.name) }
                },
                finance.observeAccounts(id).map { list ->
                    list.map { DropOption("${it.id}", it.name) }
                }
            ) { f, couriers, accounts ->
                f.copy(
                    businessId = id,
                    couriers = couriers,
                    accounts = accounts,
                    courierId = f.courierId ?: couriers.firstOrNull()?.id?.toLong(),
                    accountId = f.accountId ?: accounts.firstOrNull()?.id?.toLong()
                )
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            SettlementForm()
        )

    fun setCourier(id: Long?) { form.value = form.value.copy(courierId = id) }
    fun setCod(v: Long) { form.value = form.value.copy(codText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }
    fun setCash(v: Long) { form.value = form.value.copy(cashText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }
    fun setFee(v: Long) { form.value = form.value.copy(feeText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }
    fun setAccount(id: Long?) { form.value = form.value.copy(accountId = id) }
    fun setNote(v: String) { form.value = form.value.copy(note = v) }

    fun save(onSaved: () -> Unit) {
        val f = form.value
        if (f.courierId == null) {
            form.value = f.copy(error = "Choose a courier.")
            return
        }
        form.value = f.copy(saving = true, error = null)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val result = runCatching {
                orders.recordSettlement(
                    com.hisabnikash.app.data.repo.SettlementInput(
                        businessId = f.businessId,
                        courierId = f.courierId,
                        amountMinor = parseMoneyInput(f.cashText) ?: 0,
                        feeMinor = parseMoneyInput(f.feeText) ?: 0,
                        codMinor = parseMoneyInput(f.codText) ?: 0,
                        accountId = f.accountId,
                        note = f.note.ifBlank { null }
                    )
                )
            }
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                result.onSuccess { onSaved() }.onFailure { e ->
                    form.value = f.copy(saving = false, error = e.message ?: "Couldn't record settlement.")
                }
            }
        }
    }
}

@Composable
fun SettlementFormRoute(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { SettlementFormViewModel(it) }
    val state by vm.state.collectAsState()

    ScreenFrame("Record settlement", onBack = { navController.popBackStack() }) {
        AppDropdown("Courier", state.couriers, state.courierId?.toString(), { vm.setCourier(it.id.toLong()) })
        MoneyField("COD total", parseMoneyInput(state.codText) ?: 0, { vm.setCod(it) })
        MoneyField("Cash received", parseMoneyInput(state.cashText) ?: 0, { vm.setCash(it) })
        MoneyField("Courier fees", parseMoneyInput(state.feeText) ?: 0, { vm.setFee(it) })
        AppDropdown("Cash into account", state.accounts, state.accountId?.toString(), { vm.setAccount(it.id.toLong()) })
        AppTextField("Note", state.note, { vm.setNote(it) }, singleLine = false, minLines = 2)
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
        Button(
            onClick = { vm.save { navController.popBackStack() } },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)
        ) { Text(if (state.saving) "Saving…" else "Save settlement") }
    }
}

// ---------------------------------------------------------------------------
// Couriers
// ---------------------------------------------------------------------------

data class CourierForm(
    val businessId: Long = 0,
    val courierId: Long = 0,
    val name: String = "",
    val forwardText: String = "",
    val returnText: String = "",
    val note: String = "",
    val loaded: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null
)

class CourierFormViewModel(container: AppContainer, private val courierId: Long) : ViewModel() {

    private val db = container.database
    private val form = MutableStateFlow(CourierForm())

    val state: StateFlow<CourierForm> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(form.value)
            else if (courierId > 0 && !form.value.loaded) {
                db.courierDao().observeById(courierId).map { courier ->
                    form.value.copy(
                        businessId = id,
                        courierId = courierId,
                        loaded = true,
                        name = courier?.name ?: "",
                        forwardText = if ((courier?.forwardFeeMinor ?: 0) > 0)
                            com.hisabnikash.app.domain.model.formatMoneyPlain(courier!!.forwardFeeMinor) else "",
                        returnText = if ((courier?.returnFeeMinor ?: 0) > 0)
                            com.hisabnikash.app.domain.model.formatMoneyPlain(courier!!.returnFeeMinor) else "",
                        note = courier?.note ?: ""
                    )
                }
            } else flowOf(form.value.copy(businessId = id))
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            CourierForm()
        )

    fun setName(v: String) { form.value = form.value.copy(name = v) }
    fun setForward(v: Long) { form.value = form.value.copy(forwardText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }
    fun setReturn(v: Long) { form.value = form.value.copy(returnText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }
    fun setNote(v: String) { form.value = form.value.copy(note = v) }

    fun save(onSaved: () -> Unit) {
        val f = form.value
        if (f.name.isBlank()) {
            form.value = f.copy(error = "Courier name is required.")
            return
        }
        form.value = f.copy(saving = true, error = null)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val result = runCatching {
                val courier = CourierEntity(
                    id = f.courierId,
                    businessId = f.businessId,
                    name = f.name.trim(),
                    forwardFeeMinor = parseMoneyInput(f.forwardText) ?: 0,
                    returnFeeMinor = parseMoneyInput(f.returnText) ?: 0,
                    note = f.note.ifBlank { null }
                )
                if (courier.id == 0L) db.courierDao().insert(courier) else db.courierDao().update(courier)
            }
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                result.onSuccess { onSaved() }.onFailure { e ->
                    form.value = f.copy(saving = false, error = e.message ?: "Couldn't save courier.")
                }
            }
        }
    }
}

@Composable
fun CourierFormRoute(container: AppContainer, navController: NavHostController, courierId: Long?) {
    val vm = appViewModel(container, key = "courier-${courierId ?: 0}") { CourierFormViewModel(it, courierId ?: 0) }
    val state by vm.state.collectAsState()

    ScreenFrame(
        if (courierId == null) "New courier" else "Edit courier",
        onBack = { navController.popBackStack() }
    ) {
        AppTextField("Courier name", state.name, { vm.setName(it) }, placeholder = "e.g. Pathao, Steadfast")
        MoneyField("Typical forward fee", parseMoneyInput(state.forwardText) ?: 0, { vm.setForward(it) })
        MoneyField("Typical return fee", parseMoneyInput(state.returnText) ?: 0, { vm.setReturn(it) })
        AppTextField("Note", state.note, { vm.setNote(it) }, singleLine = false, minLines = 2)
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
        Button(
            onClick = { vm.save { navController.popBackStack() } },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
        ) { Text(if (state.saving) "Saving…" else "Save courier") }
    }
}

data class CouriersUi(
    val loading: Boolean = true,
    val couriers: List<CourierEntity> = emptyList()
)

class CouriersViewModel(container: AppContainer) : ViewModel() {

    private val db = container.database

    val state: StateFlow<CouriersUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(CouriersUi().copy(loading = false))
            else db.courierDao().observeAll(id).map { list -> CouriersUi(loading = false, couriers = list) }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            CouriersUi()
        )
}

@Composable
fun CouriersScreenRoute(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { CouriersViewModel(it) }
    val state by vm.state.collectAsState()
    val couriers = state.couriers

    ScreenFrame("Couriers", onBack = { navController.popBackStack() }) {
        if (couriers.isEmpty()) {
            EmptyState(
                Icons.Filled.LocalShipping,
                "No couriers",
                "Add couriers to assign deliveries and settle COD.",
                actionLabel = "Add Courier",
                onAction = { navController.navigate(Routes.COURIER_EDIT) }
            )
        } else {
            couriers.forEach { courier ->
                ElevatedCard(
                    onClick = { navController.navigate(Routes.courierEdit(courier.id)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(courier.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Forward ${formatMoney(courier.forwardFeeMinor)} • Return ${formatMoney(courier.returnFeeMinor)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkFaint
                            )
                        }
                        StatusChip("Default", BrandGold)
                    }
                }
            }
        }
        Button(
            onClick = { navController.navigate(Routes.COURIER_EDIT) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) { Text("Add courier") }
    }
}

// ---------------------------------------------------------------------------
// Campaigns
// ---------------------------------------------------------------------------

data class CampaignsUi(
    val loading: Boolean = true,
    val campaigns: List<CampaignEntity> = emptyList()
)

class CampaignsViewModel(container: AppContainer) : ViewModel() {

    private val finance = container.financeRepository

    val state: StateFlow<CampaignsUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(CampaignsUi().copy(loading = false))
            else finance.observeCampaigns(id).map { list -> CampaignsUi(loading = false, campaigns = list) }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            CampaignsUi()
        )
}

@Composable
fun CampaignsScreenRoute(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { CampaignsViewModel(it) }
    val state by vm.state.collectAsState()

    ScreenFrame("Campaigns", onBack = { navController.popBackStack() }) {
        if (state.campaigns.isEmpty()) {
            EmptyState(
                Icons.Filled.Campaign,
                "No campaigns yet",
                "Track ad spend and attributed revenue so ROAS stays real.",
                actionLabel = "New Campaign",
                onAction = { navController.navigate(Routes.NEW_CAMPAIGN) }
            )
        } else {
            state.campaigns.forEach { campaign ->
                val spend = campaign.spendMinor
                val revenue = campaign.attributedRevenueMinor
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                campaign.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            StatusChip(campaign.platform, BrandGold)
                        }
                        Spacer(Modifier.height(4.dp))
                        LabelValueRow("Spend", formatMoney(spend))
                        LabelValueRow("Attributed revenue", formatMoney(revenue))
                        LabelValueRow(
                            "ROAS",
                            if (spend > 0) com.hisabnikash.app.domain.model.formatPercent(
                                (revenue * 10_000 / spend).toInt()
                            ) else "—"
                        )
                        LabelValueRow("Period", "${formatDate(campaign.startAt)} → ${campaign.endAt?.let { formatDate(it) } ?: "open"}")
                    }
                }
            }
        }
        Button(
            onClick = { navController.navigate(Routes.NEW_CAMPAIGN) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) { Text("New campaign") }
    }
}

data class CampaignForm(
    val businessId: Long = 0,
    val platform: String = "META",
    val name: String = "",
    val spendText: String = "",
    val startAt: Long = System.currentTimeMillis(),
    val endAt: Long? = null,
    val attributedOrdersText: String = "0",
    val attributedRevenueText: String = "",
    val note: String = "",
    val saving: Boolean = false,
    val error: String? = null
)

class CampaignFormViewModel(container: AppContainer) : ViewModel() {

    private val finance = container.financeRepository
    private val form = MutableStateFlow(CampaignForm())

    val state: StateFlow<CampaignForm> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null) flowOf(form.value) else flowOf(form.value.copy(businessId = id))
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            CampaignForm()
        )

    fun setPlatform(v: String) { form.value = form.value.copy(platform = v) }
    fun setName(v: String) { form.value = form.value.copy(name = v) }
    fun setSpend(v: Long) { form.value = form.value.copy(spendText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }
    fun setStart(v: Long) { form.value = form.value.copy(startAt = v) }
    fun setEnd(v: Long?) { form.value = form.value.copy(endAt = v) }
    fun setAttributedOrders(v: String) { form.value = form.value.copy(attributedOrdersText = v.filter(Char::isDigit)) }
    fun setAttributedRevenue(v: Long) { form.value = form.value.copy(attributedRevenueText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }
    fun setNote(v: String) { form.value = form.value.copy(note = v) }

    fun save(onSaved: () -> Unit) {
        val f = form.value
        if (f.name.isBlank()) {
            form.value = f.copy(error = "Campaign name is required.")
            return
        }
        form.value = f.copy(saving = true, error = null)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val result = runCatching {
                finance.saveCampaign(
                    CampaignEntity(
                        businessId = f.businessId,
                        platform = f.platform,
                        name = f.name.trim(),
                        spendMinor = parseMoneyInput(f.spendText) ?: 0,
                        startAt = f.startAt,
                        endAt = f.endAt,
                        attributedOrders = f.attributedOrdersText.toLongOrNull() ?: 0,
                        attributedRevenueMinor = parseMoneyInput(f.attributedRevenueText) ?: 0,
                        note = f.note.ifBlank { null }
                    )
                )
            }
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                result.onSuccess { onSaved() }.onFailure { e ->
                    form.value = f.copy(saving = false, error = e.message ?: "Couldn't save campaign.")
                }
            }
        }
    }
}

@Composable
fun CampaignFormRoute(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { CampaignFormViewModel(it) }
    val state by vm.state.collectAsState()

    ScreenFrame("New campaign", onBack = { navController.popBackStack() }) {
        AppDropdown(
            "Platform",
            listOf(
                DropOption("META", "Facebook / Instagram"),
                DropOption("GOOGLE", "Google"),
                DropOption("TIKTOK", "TikTok"),
                DropOption("YOUTUBE", "YouTube"),
                DropOption("OTHER", "Other")
            ),
            state.platform,
            { vm.setPlatform(it.id) }
        )
        AppTextField("Campaign name", state.name, { vm.setName(it) }, placeholder = "e.g. Eid Sale Booster")
        MoneyField("Ad spend", parseMoneyInput(state.spendText) ?: 0, { vm.setSpend(it) })
        DateField("Start date", state.startAt, { vm.setStart(it) })
        DateField("End date", state.endAt ?: state.startAt, { vm.setEnd(it) })
        AppTextField(
            "Attributed orders",
            state.attributedOrdersText,
            { vm.setAttributedOrders(it) },
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
        )
        MoneyField("Attributed revenue", parseMoneyInput(state.attributedRevenueText) ?: 0, { vm.setAttributedRevenue(it) })
        AppTextField("Note", state.note, { vm.setNote(it) }, singleLine = false, minLines = 2)
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
        Button(
            onClick = { vm.save { navController.popBackStack() } },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)
        ) { Text(if (state.saving) "Saving…" else "Save campaign") }
    }
}

// ---------------------------------------------------------------------------
// Budgets
// ---------------------------------------------------------------------------

data class BudgetsUi(
    val loading: Boolean = true,
    val category: String = "ALL",
    val budgets: List<BudgetEntity> = emptyList()
)

class BudgetsViewModel(container: AppContainer) : ViewModel() {

    private val finance = container.financeRepository
    private val categoryFlow = MutableStateFlow("ALL")

    val state: StateFlow<BudgetsUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(BudgetsUi().copy(loading = false))
            else categoryFlow.flatMapLatest { category ->
                finance.observeBudgets(id, category).map { list ->
                    BudgetsUi(loading = false, category = category, budgets = list)
                }
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            BudgetsUi()
        )

    fun setCategory(category: String) { categoryFlow.value = category }
}

@Composable
fun BudgetsScreenRoute(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { BudgetsViewModel(it) }
    val state by vm.state.collectAsState()

    ScreenFrame("Budgets", onBack = { navController.popBackStack() }) {
        FilterChips(
            listOf("ALL", "ADVERTISING", "COURIER", "PACKAGING", "OPERATIONS", "OTHER"),
            state.category,
            { vm.setCategory(it) }
        )
        if (state.budgets.isEmpty()) {
            EmptyState(
                Icons.Filled.Campaign,
                "No budgets yet",
                "Set monthly spending limits for categories.",
                actionLabel = "New Budget",
                onAction = { navController.navigate(Routes.NEW_BUDGET) }
            )
        } else {
            state.budgets.forEach { budget ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(Modifier.padding(14.dp)) {
                        LabelValueRow("Category", budget.category)
                        LabelValueRow("Limit", formatMoney(budget.budgetMinor))
                        LabelValueRow("Period", "${formatDate(budget.periodStart)} → ${formatDate(budget.periodEnd)}")
                    }
                }
            }
        }
        Button(
            onClick = { navController.navigate(Routes.NEW_BUDGET) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) { Text("New budget") }
    }
}

data class BudgetForm(
    val businessId: Long = 0,
    val category: String = "ADVERTISING",
    val periodStart: Long = startOfMonth(),
    val periodEnd: Long = endOfMonth(),
    val budgetText: String = "",
    val saving: Boolean = false,
    val error: String? = null
)

private fun startOfMonth(): Long {
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun endOfMonth(): Long {
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.DAY_OF_MONTH, cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
    cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
    cal.set(java.util.Calendar.MINUTE, 59)
    return cal.timeInMillis
}

class BudgetFormViewModel(container: AppContainer) : ViewModel() {

    private val finance = container.financeRepository
    private val form = MutableStateFlow(BudgetForm())

    val state: StateFlow<BudgetForm> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null) flowOf(form.value) else flowOf(form.value.copy(businessId = id))
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            BudgetForm()
        )

    fun setCategory(v: String) { form.value = form.value.copy(category = v) }
    fun setStart(v: Long) { form.value = form.value.copy(periodStart = v) }
    fun setEnd(v: Long) { form.value = form.value.copy(periodEnd = v) }
    fun setBudget(v: Long) { form.value = form.value.copy(budgetText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }

    fun save(onSaved: () -> Unit) {
        val f = form.value
        val amount = parseMoneyInput(f.budgetText) ?: 0
        if (amount <= 0) {
            form.value = f.copy(error = "Enter a budget greater than zero.")
            return
        }
        form.value = f.copy(saving = true, error = null)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val result = runCatching {
                finance.saveBudget(
                    BudgetEntity(
                        businessId = f.businessId,
                        category = f.category,
                        periodStart = f.periodStart,
                        periodEnd = f.periodEnd,
                        budgetMinor = amount
                    )
                )
            }
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                result.onSuccess { onSaved() }.onFailure { e ->
                    form.value = f.copy(saving = false, error = e.message ?: "Couldn't save budget.")
                }
            }
        }
    }
}

@Composable
fun BudgetFormRoute(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { BudgetFormViewModel(it) }
    val state by vm.state.collectAsState()

    ScreenFrame("New budget", onBack = { navController.popBackStack() }) {
        AppDropdown(
            "Category",
            listOf(
                DropOption("ADVERTISING", "Advertising"),
                DropOption("COURIER", "Courier"),
                DropOption("PACKAGING", "Packaging"),
                DropOption("OPERATIONS", "Operations"),
                DropOption("OTHER", "Other")
            ),
            state.category,
            { vm.setCategory(it.id) }
        )
        DateField("From", state.periodStart, { vm.setStart(it) })
        DateField("To", state.periodEnd, { vm.setEnd(it) })
        MoneyField("Budget amount", parseMoneyInput(state.budgetText) ?: 0, { vm.setBudget(it) })
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
        Button(
            onClick = { vm.save { navController.popBackStack() } },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)
        ) { Text(if (state.saving) "Saving…" else "Save budget") }
    }
}
