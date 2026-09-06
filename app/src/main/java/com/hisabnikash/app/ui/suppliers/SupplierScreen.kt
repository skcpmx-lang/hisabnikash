package com.hisabnikash.app.ui.suppliers

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import com.hisabnikash.app.data.container.AppContainer
import com.hisabnikash.app.data.db.SupplierEntity
import com.hisabnikash.app.domain.model.formatDateTime
import com.hisabnikash.app.domain.model.FormInputSync
import com.hisabnikash.app.domain.model.formatMoney
import com.hisabnikash.app.domain.model.parseMoneyInput
import com.hisabnikash.app.ui.components.AppDropdown
import com.hisabnikash.app.ui.components.AppTextField
import com.hisabnikash.app.ui.components.DropOption
import com.hisabnikash.app.ui.components.EmptyState
import com.hisabnikash.app.ui.components.LabelValueRow
import com.hisabnikash.app.ui.components.MetricCard
import com.hisabnikash.app.ui.components.MoneyField
import com.hisabnikash.app.ui.components.ScreenFrame
import com.hisabnikash.app.ui.components.SectionHeader
import com.hisabnikash.app.ui.components.StatusChip
import com.hisabnikash.app.ui.nav.Routes
import com.hisabnikash.app.ui.theme.BrandGreen
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
// Suppliers list
// ---------------------------------------------------------------------------

data class SuppliersUi(
    val loading: Boolean = true,
    val query: String = "",
    val suppliers: List<SupplierEntity> = emptyList(),
    val purchases: List<com.hisabnikash.app.data.db.PurchaseEntity> = emptyList()
)

class SuppliersViewModel(container: AppContainer) : ViewModel() {

    private val catalog = container.catalogRepository
    private val queryFlow = MutableStateFlow("")

    val state: StateFlow<SuppliersUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(SuppliersUi().copy(loading = false))
            else queryFlow.flatMapLatest { query ->
                combine(
                    catalog.observeSuppliers(id).map { list ->
                        if (query.isBlank()) list
                        else list.filter {
                            it.name.contains(query, true) || (it.phone?.contains(query, true) == true)
                        }
                    },
                    catalog.observePurchases(id)
                ) { suppliers, purchases ->
                    SuppliersUi(loading = false, query = query, suppliers = suppliers, purchases = purchases)
                }
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            SuppliersUi()
        )

    fun setQuery(query: String) { queryFlow.value = query }
}

@Composable
fun SuppliersScreenRoute(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { SuppliersViewModel(it) }
    val state by vm.state.collectAsState()

    ScreenFrame("Suppliers", onBack = { navController.popBackStack() }) {
        OutlinedTextField(
            value = state.query,
            onValueChange = { vm.setQuery(it) },
            placeholder = { Text("Search suppliers") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        )
        if (state.suppliers.isEmpty()) {
            EmptyState(
                Icons.Filled.LocalShipping,
                if (state.query.isBlank()) "No suppliers yet" else "No matches",
                if (state.query.isBlank())
                    "Add suppliers to link purchases, track payables and keep product costs accurate."
                else "Try a different name or phone.",
                actionLabel = if (state.query.isBlank()) "Add Supplier" else null,
                onAction = { navController.navigate(Routes.NEW_SUPPLIER) }
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.suppliers, key = { it.id }) { supplier ->
                    val purchases = state.purchases.filter { it.supplierId == supplier.id }
                    ElevatedCard(
                        onClick = { navController.navigate(Routes.supplier(supplier.id)) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(supplier.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    supplier.phone ?: supplier.email ?: "No contact",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkFaint
                                )
                                Text(
                                    "${purchases.size} purchases • ${formatMoney(purchases.sumOf { it.totalMinor })} bought",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkFaint
                                )
                            }
                            Text(
                                formatMoney(purchases.sumOf { it.totalMinor - it.paidMinor }),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (purchases.sumOf { it.totalMinor - it.paidMinor } > 0) Warning else BrandGreen
                            )
                        }
                    }
                }
            }
        }
        Button(
            onClick = { navController.navigate(Routes.NEW_SUPPLIER) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) { Icon(Icons.Filled.Add, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Add supplier") }
    }
}

// ---------------------------------------------------------------------------
// Supplier form
// ---------------------------------------------------------------------------

private data class SupplierFields(
    val name: String,
    val phone: String,
    val email: String,
    val address: String,
    val notes: String
)

data class SupplierForm(
    val businessId: Long = 0,
    val supplierId: Long = 0,
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val notes: String = "",
    val saving: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null
)

class SupplierFormViewModel(container: AppContainer, private val supplierId: Long) : ViewModel() {

    private val catalog = container.catalogRepository
    private val form = MutableStateFlow(SupplierForm())

    val state: StateFlow<SupplierForm> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(form.value)
            else
                // Same fix as the customer form: the form value is combined so
                // every keystroke re-emits (the old one-shot flowOf held the
                // initial empty value and typed text never appeared).
                combine(form, flowOf(id)) { f, businessId -> f.copy(businessId = businessId) }
                    .flatMapLatest { f ->
                        if (supplierId > 0 && !f.loaded) {
                            catalog.observeSupplier(supplierId).map { supplier ->
                                val (loaded, fields) = FormInputSync.mergeLoaded(
                                    f.loaded,
                                    SupplierFields(f.name, f.phone, f.email, f.address, f.notes),
                                    supplier?.let {
                                        SupplierFields(it.name, it.phone.orEmpty(), it.email.orEmpty(), it.address.orEmpty(), it.notes.orEmpty())
                                    },
                                    f.name.isBlank() && f.phone.isBlank() &&
                                    f.email.isBlank() && f.address.isBlank() && f.notes.isBlank(),
                                )
                                f.copy(
                                    businessId = id,
                                    supplierId = supplierId,
                                    loaded = loaded,
                                    name = fields.name,
                                    phone = fields.phone,
                                    email = fields.email,
                                    address = fields.address,
                                    notes = fields.notes
                                )
                            }
                        } else flowOf(f)
                    }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            SupplierForm()
        )

    fun setName(v: String) { form.value = form.value.copy(name = v) }
    fun setPhone(v: String) { form.value = form.value.copy(phone = v) }
    fun setEmail(v: String) { form.value = form.value.copy(email = v) }
    fun setAddress(v: String) { form.value = form.value.copy(address = v) }
    fun setNotes(v: String) { form.value = form.value.copy(notes = v) }

    fun save(onSaved: (Long) -> Unit) {
        val f = form.value
        if (f.name.isBlank()) {
            form.value = f.copy(error = "Supplier name is required.")
            return
        }
        form.value = f.copy(saving = true, error = null)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val result = runCatching {
                catalog.saveSupplier(
                    SupplierEntity(
                        id = f.supplierId,
                        businessId = f.businessId,
                        name = f.name.trim(),
                        phone = f.phone.ifBlank { null },
                        email = f.email.ifBlank { null },
                        address = f.address.ifBlank { null },
                        notes = f.notes.ifBlank { null }
                    )
                )
            }
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                result.onSuccess(onSaved).onFailure { e ->
                    val msg = e.message.orEmpty()
                    form.value = f.copy(
                        saving = false,
                        error = if (msg.contains("FOREIGN KEY", ignoreCase = true) ||
                            msg.contains("SQLITE_CONSTRAINT", ignoreCase = true)
                        ) {
                            "This supplier could not be saved because one of its references no longer exists. Refresh and try again."
                        } else {
                            "This supplier could not be saved. Please check the details and try again."
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SupplierFormRoute(container: AppContainer, navController: NavHostController, supplierId: Long?) {
    val vm = appViewModel(container, key = "supplier-form-${supplierId ?: 0}") {
        SupplierFormViewModel(it, supplierId ?: 0)
    }
    val state by vm.state.collectAsState()

    ScreenFrame(
        if (supplierId == null) "New supplier" else "Edit supplier",
        onBack = { navController.popBackStack() }
    ) {
        if (supplierId != null && !state.loaded) {
            Text("Loading supplier…", color = InkFaint, modifier = Modifier.padding(16.dp))
            return@ScreenFrame
        }
        AppTextField("Supplier name", state.name, { vm.setName(it) }, placeholder = "e.g. Bengal Textiles")
        AppTextField("Phone", state.phone, { vm.setPhone(it) }, placeholder = "01XXXXXXXXX")
        AppTextField("Email", state.email, { vm.setEmail(it) }, placeholder = "sales@supplier.com")
        AppTextField("Address", state.address, { vm.setAddress(it) }, singleLine = false, minLines = 2)
        AppTextField("Notes", state.notes, { vm.setNotes(it) }, singleLine = false, minLines = 2)
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
        Button(
            onClick = {
                vm.save { id ->
                    navController.navigate(Routes.supplier(id)) {
                        popUpTo(Routes.NEW_SUPPLIER) { inclusive = true }
                    }
                }
            },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
        ) { Text(if (state.saving) "Saving…" else "Save supplier") }
    }
}

// ---------------------------------------------------------------------------
// Supplier detail
// ---------------------------------------------------------------------------

data class SupplierDetailUi(
    val businessId: Long = 0,
    val supplier: SupplierEntity? = null,
    val purchases: List<com.hisabnikash.app.data.db.PurchaseEntity> = emptyList(),
    val payables: List<com.hisabnikash.app.data.db.PayableEntity> = emptyList()
)

class SupplierDetailViewModel(container: AppContainer, private val supplierId: Long) : ViewModel() {

    private val catalog = container.catalogRepository
    private val finance = container.financeRepository

    val state: StateFlow<SupplierDetailUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(SupplierDetailUi())
            else combine(
                catalog.observeSupplier(supplierId),
                catalog.observePurchasesForSupplier(id, supplierId),
                finance.observePayablesForSupplier(id, supplierId)
            ) { supplier, purchases, payables ->
                SupplierDetailUi(businessId = id, supplier = supplier, purchases = purchases, payables = payables)
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            SupplierDetailUi()
        )
}

@Composable
fun SupplierDetailRoute(container: AppContainer, navController: NavHostController, supplierId: Long) {
    val vm = appViewModel(container, key = "supplier-$supplierId") { SupplierDetailViewModel(it, supplierId) }
    val state by vm.state.collectAsState()
    val supplier = state.supplier
    val purchases = state.purchases
    val payables = state.payables

    ScreenFrame(
        title = supplier?.name ?: "Supplier",
        onBack = { navController.popBackStack() },
        subtitle = supplier?.phone ?: "Supplier profile",
        actions = {
            if (supplier != null) {
                TextButton(onClick = { navController.navigate(Routes.supplierEdit(supplier.id)) }) { Text("Edit") }
            }
        }
    ) {
        if (supplier == null) {
            Text("Supplier not found.", color = InkFaint, modifier = Modifier.padding(16.dp))
            return@ScreenFrame
        }
        SectionHeader("Summary")
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            MetricCard("Purchases", "${purchases.size}", Modifier.weight(1f))
            MetricCard("Bought", formatMoney(purchases.sumOf { it.totalMinor }), Modifier.weight(1f))
            MetricCard("Due", formatMoney(purchases.sumOf { it.totalMinor - it.paidMinor }), Modifier.weight(1f))
        }
        SectionHeader("Profile")
        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            Column(Modifier.padding(16.dp)) {
                LabelValueRow("Phone", supplier.phone ?: "—")
                LabelValueRow("Email", supplier.email ?: "—")
                LabelValueRow("Address", supplier.address ?: "—")
                LabelValueRow("Notes", supplier.notes ?: "—")
            }
        }
        SectionHeader("Purchases")
        if (purchases.isEmpty()) {
            Text("No purchases yet.", color = InkFaint, modifier = Modifier.padding(horizontal = 16.dp))
        } else {
            purchases.forEach { purchase ->
                ElevatedCard(
                    onClick = { navController.navigate(Routes.purchase(purchase.id)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Purchase #${purchase.id}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                formatDateTime(purchase.dateAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = InkFaint
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatMoney(purchase.totalMinor), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                if (purchase.totalMinor - purchase.paidMinor > 0) "Due ${formatMoney(purchase.totalMinor - purchase.paidMinor)}"
                                else "Paid",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (purchase.totalMinor - purchase.paidMinor > 0) Warning else BrandGreen
                            )
                        }
                    }
                }
            }
        }
        SectionHeader("Payables")
        payables.forEach { payable ->
            ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp), shape = MaterialTheme.shapes.small) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${payable.sourceType} • ${payable.status}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        formatMoney(payable.amountMinor - payable.paidMinor),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Warning
                    )
                }
            }
        }
        Button(
            onClick = { navController.navigate(Routes.NEW_PURCHASE) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
        ) { Text("New purchase") }
    }
}

// ---------------------------------------------------------------------------
// Purchase form
// ---------------------------------------------------------------------------

data class PurchaseLineDraft(
    val productId: Long = 0,
    val name: String = "",
    val qtyText: String = "1",
    val costText: String = ""
) {
    val qty: Long get() = qtyText.toLongOrNull()?.coerceAtLeast(1) ?: 1
    val costMinor: Long get() = parseMoneyInput(costText) ?: 0
}

data class PurchaseForm(
    val businessId: Long = 0,
    val supplierId: Long? = null,
    val supplierName: String = "",
    val dateAt: Long = System.currentTimeMillis(),
    val lines: List<PurchaseLineDraft> = emptyList(),
    val paidText: String = "",
    val accountId: Long? = null,
    val notes: String = "",
    val suppliers: List<DropOption> = emptyList(),
    val products: List<DropOption> = emptyList(),
    val accounts: List<DropOption> = emptyList(),
    val saving: Boolean = false,
    val error: String? = null
)

class PurchaseFormViewModel(container: AppContainer) : ViewModel() {

    private val catalog = container.catalogRepository
    private val finance = container.financeRepository
    private val form = MutableStateFlow(PurchaseForm())

    val state: StateFlow<PurchaseForm> = container.workspaceRepository.observeActiveBusinessId()
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(form.value)
            else combine(
                form,
                catalog.observeSuppliers(id).map { list ->
                    list.map { DropOption("${it.id}", it.name, it.phone) }
                },
                catalog.observeProducts(id).map { list ->
                    list.map {
                        DropOption(
                            "${it.product.id}", it.product.name,
                            "Cost ${formatMoney(it.product.purchaseCostMinor)} • ${it.product.stockQty} in stock",
                            value = it.product
                        )
                    }
                },
                finance.observeAccounts(id).map { list ->
                    list.map { DropOption("${it.id}", it.name, it.type) }
                }
            ) { f, suppliers, products, accounts ->
                val defaultAccount = accounts.firstOrNull { it.subtitle == "CASH" } ?: accounts.firstOrNull()
                f.copy(
                    businessId = id,
                    suppliers = suppliers,
                    products = products,
                    accounts = accounts,
                    supplierName = suppliers.firstOrNull { it.id == f.supplierId?.toString() }?.label ?: "",
                    accountId = f.accountId ?: defaultAccount?.id?.toLong()
                )
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            PurchaseForm()
        )

    fun setSupplier(id: Long?) { form.value = form.value.copy(supplierId = id) }
    fun setPaid(text: String) { form.value = form.value.copy(paidText = text) }
    fun setAccount(id: Long?) { form.value = form.value.copy(accountId = id) }
    fun setNotes(text: String) { form.value = form.value.copy(notes = text) }

    fun addLine(product: com.hisabnikash.app.data.db.ProductEntity?) {
        val f = form.value
        if (product == null) {
            form.value = f.copy(error = "Choose a product from the catalog to purchase stock.")
            return
        }
        form.value = f.copy(
            error = null,
            lines = f.lines + PurchaseLineDraft(
                productId = product.id,
                name = product.name,
                costText = if (product.purchaseCostMinor > 0)
                    com.hisabnikash.app.domain.model.formatMoneyPlain(product.purchaseCostMinor) else ""
            )
        )
    }

    fun updateLine(index: Int, line: PurchaseLineDraft) {
        val f = form.value
        val list = f.lines.toMutableList()
        if (index in list.indices) list[index] = line
        form.value = f.copy(lines = list)
    }

    fun removeLine(index: Int) {
        val f = form.value
        form.value = f.copy(lines = f.lines.filterIndexed { i, _ -> i != index })
    }

    fun save(onSaved: (Long) -> Unit) {
        val f = form.value
        if (f.supplierId == null) {
            form.value = f.copy(error = "Choose a supplier.")
            return
        }
        if (f.lines.isEmpty()) {
            form.value = f.copy(error = "Add at least one line.")
            return
        }
        form.value = f.copy(saving = true, error = null)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val result = runCatching {
                catalog.createPurchase(
                    com.hisabnikash.app.data.repo.NewPurchaseInput(
                        businessId = f.businessId,
                        supplierId = f.supplierId,
                        dateAt = f.dateAt,
                        lines = f.lines.map {
                            com.hisabnikash.app.data.repo.PurchaseLine(
                                productId = it.productId,
                                qty = it.qty,
                                unitCostMinor = it.costMinor
                            )
                        },
                        paidMinor = parseMoneyInput(f.paidText) ?: 0,
                        accountId = f.accountId,
                        notes = f.notes.ifBlank { null }
                    )
                )
            }
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                result.onSuccess(onSaved).onFailure { e ->
                    form.value = f.copy(saving = false, error = "Couldn't create purchase: ${e.message}")
                }
            }
        }
    }
}

@Composable
fun PurchaseFormRoute(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { PurchaseFormViewModel(it) }
    val state by vm.state.collectAsState()

    ScreenFrame("New purchase", onBack = { navController.popBackStack() }) {
        AppDropdown(
            "Supplier",
            state.suppliers,
            state.supplierId?.toString(),
            { vm.setSupplier(it.id.toLong()) },
            emptyTitle = "No suppliers yet",
            emptyHint = "Add a supplier before recording purchases.",
            addLabel = "Add Supplier",
            onAdd = { navController.navigate(Routes.NEW_SUPPLIER) }
        )
        AppDropdown(
            "Pay from account",
            state.accounts,
            state.accountId?.toString(),
            { vm.setAccount(it.id.toLong()) },
            placeholder = "No payment now"
        )
        SectionHeader("Items")
        AppDropdown(
            "Add product",
            state.products,
            null,
            { option -> vm.addLine(option.value as? com.hisabnikash.app.data.db.ProductEntity) },
            placeholder = "Choose a product",
            emptyTitle = "No products yet",
            emptyHint = "Add a product first, then purchase stock for it.",
            addLabel = "Add Product",
            onAdd = { navController.navigate(Routes.NEW_PRODUCT) }
        )
        state.lines.forEachIndexed { index, line ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(line.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1)
                        IconButton(onClick = { vm.removeLine(index) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove line")
                        }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            AppTextField(
                                "Qty",
                                line.qtyText,
                                { vm.updateLine(index, line.copy(qtyText = it.filter(Char::isDigit))) },
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            MoneyField("Unit cost", line.costMinor, {
                                vm.updateLine(index, line.copy(costText = com.hisabnikash.app.domain.model.formatMoneyPlain(it)))
                            })
                        }
                    }
                }
            }
        }
        SectionHeader("Payment")
        MoneyField("Amount paid now", parseMoneyInput(state.paidText) ?: 0, { vm.setPaid(com.hisabnikash.app.domain.model.formatMoneyPlain(it)) })
        AppTextField("Notes", state.notes, { vm.setNotes(it) }, singleLine = false, minLines = 2)
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
        Button(
            onClick = {
                vm.save { id ->
                    navController.navigate(Routes.purchase(id)) {
                        popUpTo(Routes.NEW_PURCHASE) { inclusive = true }
                    }
                }
            },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
        ) { Text(if (state.saving) "Saving…" else "Save purchase") }
    }
}

// ---------------------------------------------------------------------------
// Purchase detail
// ---------------------------------------------------------------------------

@Composable
fun PurchaseDetailRoute(container: AppContainer, navController: NavHostController, purchaseId: Long) {
    val detail by androidx.compose.runtime.produceState<com.hisabnikash.app.data.repo.PurchaseDetail?>(
        null, purchaseId
    ) {
        value = container.catalogRepository.getPurchaseWithItems(purchaseId)
    }
    val purchase = detail?.purchase
    val items = detail?.items ?: emptyList()

    ScreenFrame(
        title = if (purchase != null) "Purchase #${purchase.id}" else "Purchase",
        onBack = { navController.popBackStack() },
        subtitle = purchase?.let { formatDateTime(it.dateAt) }
    ) {
        val p = purchase
        if (p == null) {
            Text("Purchase not found.", color = InkFaint, modifier = Modifier.padding(16.dp))
            return@ScreenFrame
        }
        SectionHeader("Summary")
        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            Column(Modifier.padding(16.dp)) {
                LabelValueRow("Total", formatMoney(p.totalMinor))
                LabelValueRow("Paid", formatMoney(p.paidMinor))
                LabelValueRow("Due", formatMoney((p.totalMinor - p.paidMinor).coerceAtLeast(0)))
                LabelValueRow("Notes", p.notes ?: "—")
            }
        }
        SectionHeader("Items")
        items.forEach { item ->
            ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp), shape = MaterialTheme.shapes.small) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Product #${item.productId}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${item.qty} × ${formatMoney(item.unitCostMinor)}",
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
    }
}
