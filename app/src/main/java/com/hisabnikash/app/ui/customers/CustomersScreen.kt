package com.hisabnikash.app.ui.customers

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import com.hisabnikash.app.data.container.AppContainer
import com.hisabnikash.app.data.db.CustomerAggregate
import com.hisabnikash.app.data.db.CustomerEntity
import com.hisabnikash.app.domain.model.formatDateTime
import com.hisabnikash.app.domain.model.formatMoney
import com.hisabnikash.app.domain.model.CommerceMath
import com.hisabnikash.app.ui.components.AppTextField
import com.hisabnikash.app.ui.components.EmptyState
import com.hisabnikash.app.ui.components.LabelValueRow
import com.hisabnikash.app.ui.components.MetricCard
import com.hisabnikash.app.ui.components.ScreenFrame
import com.hisabnikash.app.ui.components.SectionHeader
import com.hisabnikash.app.ui.components.StatusChip
import com.hisabnikash.app.ui.nav.Routes
import com.hisabnikash.app.ui.theme.BrandGreen
import com.hisabnikash.app.ui.theme.InkFaint
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
// Customers tab
// ---------------------------------------------------------------------------

data class CustomersUi(
    val loading: Boolean = true,
    val query: String = "",
    val customers: List<CustomerAggregate> = emptyList(),
    val count: Long = 0,
    val totalOrdersMinor: Long = 0
)

class CustomersViewModel(container: AppContainer) : ViewModel() {

    private val catalog = container.catalogRepository
    private val queryFlow = MutableStateFlow("")

    val state: StateFlow<CustomersUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(CustomersUi().copy(loading = false))
            else queryFlow.flatMapLatest { query ->
                combine(
                    catalog.observeCustomers(id).map { list ->
                        if (query.isBlank()) list
                        else list.filter {
                            it.customer.name.contains(query, true) ||
                                (it.customer.phone?.contains(query, true) == true) ||
                                (it.customer.email?.contains(query, true) == true) ||
                                (it.customer.tags?.contains(query, true) == true)
                        }
                    },
                    catalog.observeCustomers(id).map { list -> list.size.toLong() }
                ) { filtered, count ->
                    CustomersUi(
                        loading = false,
                        query = query,
                        customers = filtered,
                        count = count,
                        totalOrdersMinor = filtered.sumOf { it.deliveredOrdersMinor }
                    )
                }
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            CustomersUi()
        )

    fun setQuery(query: String) { queryFlow.value = query }
}

@Composable
fun CustomersTab(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { CustomersViewModel(it) }
    val state by vm.state.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Routes.NEW_CUSTOMER) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Customer") }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Customers",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            OutlinedTextField(
                value = state.query,
                onValueChange = { vm.setQuery(it) },
                placeholder = { Text("Search name, phone, email or tag") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            if (state.customers.isEmpty()) {
                EmptyState(
                    Icons.Filled.Group,
                    if (state.query.isBlank()) "No customers yet" else "No matches found",
                    if (state.query.isBlank())
                        "Build a customer book with contact details, order history and outstanding dues."
                    else "Try a different name, phone, email or tag.",
                    actionLabel = if (state.query.isBlank()) "Add Customer" else null,
                    onAction = { navController.navigate(Routes.NEW_CUSTOMER) }
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.customers, key = { it.customer.id }) { agg ->
                        CustomerRow(agg, onClick = { navController.navigate(Routes.customer(agg.customer.id)) })
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerRow(agg: CustomerAggregate, onClick: () -> Unit) {
    val customer = agg.customer
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    customer.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    (customer.phone ?: customer.email ?: "No contact").ifBlank { "No contact" },
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint,
                    maxLines = 1
                )
                Text(
                    "${agg.orderCount} orders • ${formatMoney(agg.totalOrdersMinor)} lifetime",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint,
                    maxLines = 1
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (agg.codOutstandingMinor > 0) {
                    StatusChip("Dues", BrandGoldColor)
                } else {
                    StatusChip("${agg.orderCount}", BrandGreen)
                }
                Text(
                    formatMoney(agg.codOutstandingMinor),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (agg.codOutstandingMinor > 0) BrandGoldColor else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private val BrandGoldColor = com.hisabnikash.app.ui.theme.BrandGold
private val ErrorColor = com.hisabnikash.app.ui.theme.Error

// ---------------------------------------------------------------------------
// Customer form
// ---------------------------------------------------------------------------

data class CustomerForm(
    val businessId: Long = 0,
    val customerId: Long = 0,
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val tags: String = "",
    val notes: String = "",
    val saving: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null
)

class CustomerFormViewModel(container: AppContainer, private val customerId: Long) : ViewModel() {

    private val catalog = container.catalogRepository
    private val form = MutableStateFlow(CustomerForm())

    val state: StateFlow<CustomerForm> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(form.value)
            else if (customerId > 0 && !form.value.loaded) {
                catalog.observeCustomer(customerId).map { customer ->
                    form.value.copy(
                        businessId = id,
                        customerId = customerId,
                        loaded = true,
                        name = customer?.name ?: "",
                        phone = customer?.phone ?: "",
                        email = customer?.email ?: "",
                        address = customer?.address ?: "",
                        tags = customer?.tags ?: "",
                        notes = customer?.notes ?: ""
                    )
                }
            } else flowOf(form.value.copy(businessId = id))
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            CustomerForm()
        )

    fun setName(v: String) { form.value = form.value.copy(name = v) }
    fun setPhone(v: String) { form.value = form.value.copy(phone = v) }
    fun setEmail(v: String) { form.value = form.value.copy(email = v) }
    fun setAddress(v: String) { form.value = form.value.copy(address = v) }
    fun setTags(v: String) { form.value = form.value.copy(tags = v) }
    fun setNotes(v: String) { form.value = form.value.copy(notes = v) }

    fun save(onSaved: (Long) -> Unit) {
        val f = form.value
        if (f.name.isBlank()) {
            form.value = f.copy(error = "Customer name is required.")
            return
        }
        form.value = f.copy(saving = true, error = null)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val result = runCatching {
                catalog.saveCustomer(
                    CustomerEntity(
                        id = f.customerId,
                        businessId = f.businessId,
                        name = f.name.trim(),
                        phone = f.phone.ifBlank { null },
                        email = f.email.ifBlank { null },
                        address = f.address.ifBlank { null },
                        tags = f.tags.ifBlank { null },
                        notes = f.notes.ifBlank { null }
                    )
                )
            }
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                result.onSuccess(onSaved).onFailure { e ->
                    form.value = f.copy(saving = false, error = "Couldn't save customer: ${e.message}")
                }
            }
        }
    }
}

@Composable
fun CustomerFormRoute(container: AppContainer, navController: NavHostController, customerId: Long?) {
    val vm = appViewModel(container, key = "customer-form-${customerId ?: 0}") {
        CustomerFormViewModel(it, customerId ?: 0)
    }
    val state by vm.state.collectAsState()

    ScreenFrame(
        if (customerId == null) "New customer" else "Edit customer",
        onBack = { navController.popBackStack() }
    ) {
        if (customerId != null && !state.loaded) {
            Text("Loading customer…", color = InkFaint, modifier = Modifier.padding(16.dp))
            return@ScreenFrame
        }
        AppTextField("Full name", state.name, { vm.setName(it) }, placeholder = "e.g. Nusrat Jahan")
        AppTextField("Phone", state.phone, { vm.setPhone(it) }, placeholder = "01XXXXXXXXX", keyboardType = KeyboardType.Phone)
        AppTextField("Email", state.email, { vm.setEmail(it) }, placeholder = "nusrat@example.com", keyboardType = KeyboardType.Email)
        AppTextField("Address", state.address, { vm.setAddress(it) }, singleLine = false, minLines = 2)
        AppTextField("Tags", state.tags, { vm.setTags(it) }, placeholder = "wholesale, repeat, holi")
        AppTextField("Notes", state.notes, { vm.setNotes(it) }, singleLine = false, minLines = 2)

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
        Button(
            onClick = {
                vm.save { id ->
                    navController.navigate(Routes.customer(id)) {
                        popUpTo(Routes.NEW_CUSTOMER) { inclusive = true }
                    }
                }
            },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
        ) { Text(if (state.saving) "Saving…" else "Save customer") }
    }
}

// ---------------------------------------------------------------------------
// Customer detail (360)
// ---------------------------------------------------------------------------

data class Customer360Ui(
    val loading: Boolean = true,
    val businessId: Long = 0,
    val customer: CustomerEntity? = null,
    val orders: List<com.hisabnikash.app.data.db.OrderEntity> = emptyList(),
    val payments: List<com.hisabnikash.app.data.db.PaymentEntity> = emptyList(),
    val receivables: List<com.hisabnikash.app.data.db.ReceivableEntity> = emptyList()
)

class Customer360ViewModel(container: AppContainer, private val customerId: Long) : ViewModel() {

    private val catalog = container.catalogRepository
    private val db = container.database

    val state: StateFlow<Customer360Ui> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(Customer360Ui().copy(loading = false))
            else combine(
                catalog.observeCustomer(customerId),
                db.orderDao().observeFiltered(id, "ALL", ""),
                db.paymentDao().observeForCustomer(id, customerId),
                db.receivablePayableDao().observeReceivablesForCustomer(id, customerId)
            ) { customer, orders, payments, receivables ->
                Customer360Ui(
                    loading = false,
                    businessId = id,
                    customer = customer,
                    orders = orders.filter { it.customerId == customerId },
                    payments = payments,
                    receivables = receivables
                )
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            Customer360Ui()
        )
}

@Composable
fun CustomerDetailRoute(container: AppContainer, navController: NavHostController, customerId: Long) {
    val vm = appViewModel(container, key = "customer-$customerId") { Customer360ViewModel(it, customerId) }
    val state by vm.state.collectAsState()

    val customer = state.customer

    ScreenFrame(
        title = customer?.name ?: "Customer",
        onBack = { navController.popBackStack() },
        subtitle = customer?.phone ?: "Customer profile",
        actions = {
            if (customer != null) {
                TextButton(onClick = { navController.navigate(Routes.customerEdit(customer.id)) }) { Text("Edit") }
            }
        }
    ) {
        if (customer == null) {
            Text("Customer not found.", color = InkFaint, modifier = Modifier.padding(16.dp))
            return@ScreenFrame
        }

        val deliveredTotal = state.orders.filter { it.status == "DELIVERED" }.sumOf { it.totalMinor }
        val returned = state.orders.count { it.status == "RETURNED" }
        val outstanding = state.receivables.sumOf { (it.amountMinor - it.paidMinor).coerceAtLeast(0) }

        SectionHeader("Relationship")
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            MetricCard("Orders", "${state.orders.size}", Modifier.weight(1f))
            MetricCard("Lifetime", formatMoney(deliveredTotal), Modifier.weight(1f))
            MetricCard("Dues", formatMoney(outstanding), Modifier.weight(1f))
        }
        SectionHeader("Profile")
        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            Column(Modifier.padding(16.dp)) {
                LabelValueRow("Phone", customer.phone ?: "—")
                LabelValueRow("Email", customer.email ?: "—")
                LabelValueRow("Address", customer.address ?: "—")
                LabelValueRow("Tags", customer.tags ?: "—")
                LabelValueRow(
                    "Segment",
                    CommerceMath.segmentFor(
                        state.orders.size.toLong(),
                        deliveredTotal,
                        state.orders.maxOfOrNull { it.orderDate },
                        System.currentTimeMillis()
                    )
                )
                LabelValueRow("Returns", "$returned")
            }
        }
        SectionHeader("Orders")
        if (state.orders.isEmpty()) {
            Text("No orders yet.", color = InkFaint, modifier = Modifier.padding(horizontal = 16.dp))
        } else {
            state.orders.take(15).forEach { order ->
                ElevatedCard(
                    onClick = { navController.navigate(Routes.order(order.id)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(order.orderNo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${formatDateTime(order.orderDate)} • ${order.status.replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkFaint
                            )
                        }
                        Text(
                            formatMoney(order.totalMinor),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        SectionHeader("Receivables")
        if (state.receivables.isEmpty()) {
            Text("Nothing outstanding.", color = InkFaint, modifier = Modifier.padding(horizontal = 16.dp))
        } else {
            state.receivables.forEach { r ->
                statusRow(
                    "Receivable #${r.id} • ${r.sourceType}",
                    formatMoney(r.amountMinor - r.paidMinor),
                    r.paidMinor >= r.amountMinor
                )
            }
        }
        SectionHeader("Payments")
        if (state.payments.isEmpty()) {
            Text("No payments recorded.", color = InkFaint, modifier = Modifier.padding(horizontal = 16.dp))
        } else {
            state.payments.forEach { p ->
                statusRow(
                    "${p.method} • ${formatDateTime(p.dateAt)}",
                    (if (p.direction == "IN") "+" else "-") + formatMoney(p.amountMinor),
                    p.direction == "IN"
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { navController.navigate(Routes.NEW_ORDER) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) { Text("New order for this customer") }
    }
}

@Composable
private fun statusRow(label: String, value: String, positive: Boolean) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
        shape = MaterialTheme.shapes.small
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (positive) BrandGreen else ErrorColor
            )
        }
    }
}
