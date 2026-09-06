package com.hisabnikash.app.ui.documents

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.hisabnikash.app.data.db.InvoiceEntity
import com.hisabnikash.app.data.db.InvoiceItemEntity
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
import com.hisabnikash.app.util.DocumentPrinter
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
// Invoices list
// ---------------------------------------------------------------------------

data class InvoicesUi(
    val loading: Boolean = true,
    val status: String = "ALL",
    val invoices: List<InvoiceEntity> = emptyList()
)

class InvoicesViewModel(container: AppContainer) : ViewModel() {

    private val db = container.database
    private val statusFlow = MutableStateFlow("ALL")

    val state: StateFlow<InvoicesUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(InvoicesUi().copy(loading = false))
            else statusFlow.flatMapLatest { status ->
                db.invoiceDao().observeFiltered(id, if (status == "ALL") "ALL" else status).map { list ->
                    InvoicesUi(loading = false, status = status, invoices = list)
                }
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            InvoicesUi()
        )

    fun setStatus(status: String) { statusFlow.value = status }
}

@Composable
fun InvoicesListRoute(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { InvoicesViewModel(it) }
    val state by vm.state.collectAsState()

    ScreenFrame("Invoices", onBack = { navController.popBackStack() }) {
        FilterChips(listOf("ALL", "DRAFT", "ISSUED", "PARTIALLY_PAID", "PAID", "OVERDUE", "VOID"), state.status, { vm.setStatus(it) })
        if (state.invoices.isEmpty()) {
            EmptyState(
                Icons.Filled.Description,
                "No invoices yet",
                "Invoices are documents — revenue only counts when the payment lands.",
                actionLabel = "New Invoice",
                onAction = { navController.navigate(Routes.NEW_INVOICE) }
            )
        } else {
            state.invoices.forEach { invoice ->
                ElevatedCard(
                    onClick = { navController.navigate(Routes.invoice(invoice.id)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(invoice.invoiceNo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.width(6.dp))
                                StatusChip(invoice.status, invoiceStatusColor(invoice.status))
                            }
                            Text(
                                "${formatDate(invoice.dateAt)} • customer #${invoice.customerId ?: "—"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkFaint
                            )
                        }
                        Text(
                            formatMoney(invoice.totalMinor),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Button(
            onClick = { navController.navigate(Routes.NEW_INVOICE) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) { Icon(Icons.Filled.Add, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("New invoice") }
    }
}

@Composable
fun invoiceStatusColor(status: String) = when (status) {
    "DRAFT" -> InkFaint
    "ISSUED" -> BrandGreen
    "PARTIALLY_PAID" -> Warning
    "PAID" -> BrandGreen
    "OVERDUE" -> Error
    else -> Error
}

// ---------------------------------------------------------------------------
// Invoice form
// ---------------------------------------------------------------------------

data class InvoiceLineDraft(
    val productId: Long? = null,
    val name: String = "",
    val qtyText: String = "1",
    val priceText: String = ""
) {
    val qty: Long get() = qtyText.toLongOrNull()?.coerceAtLeast(1) ?: 1
    val priceMinor: Long get() = parseMoneyInput(priceText) ?: 0
}

data class InvoiceFormUi(
    val businessId: Long = 0,
    val invoiceId: Long = 0,
    val customerId: Long? = null,
    val orderId: Long? = null,
    val dateAt: Long = System.currentTimeMillis(),
    val dueDateAt: Long? = null,
    val lines: List<InvoiceLineDraft> = emptyList(),
    val discountText: String = "",
    val deliveryText: String = "",
    val advanceText: String = "",
    val paymentMethod: String = "COD",
    val note: String = "",
    val terms: String = "",
    val footer: String = "",
    val issuedNow: Boolean = true,
    val customers: List<DropOption> = emptyList(),
    val products: List<DropOption> = emptyList(),
    val saved: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null
)

class InvoiceFormViewModel(container: AppContainer, private val invoiceId: Long) : ViewModel() {

    private val db = container.database
    private val catalog = container.catalogRepository
    private val finance = container.financeRepository
    private val orders = container.orderRepository
    private val workspace = container.workspaceRepository
    private val form = MutableStateFlow(InvoiceFormUi())

    val state: StateFlow<InvoiceFormUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(form.value)
            else combine(
                form,
                catalog.observeCustomers(id).map { list ->
                    list.map { DropOption("${it.customer.id}", it.customer.name, it.customer.phone) }
                },
                catalog.observeProducts(id).map { list ->
                    list.map { DropOption("${it.product.id}", it.product.name, "SKU ${it.product.sku ?: "—"}", it.product) }
                },
                workspace.observeActiveSettings()
            ) { f, customers, products, settings ->
                f.copy(businessId = id, customers = customers, products = products)
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            InvoiceFormUi()
        )

    fun setCustomer(v: Long?) { form.value = form.value.copy(customerId = v) }
    fun setDate(v: Long) { form.value = form.value.copy(dateAt = v) }
    fun setDue(v: Long?) { form.value = form.value.copy(dueDateAt = v) }
    fun setDiscount(v: Long) { form.value = form.value.copy(discountText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }
    fun setDelivery(v: Long) { form.value = form.value.copy(deliveryText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }
    fun setAdvance(v: Long) { form.value = form.value.copy(advanceText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }
    fun setMethod(v: String) { form.value = form.value.copy(paymentMethod = v) }
    fun setNote(v: String) { form.value = form.value.copy(note = v) }
    fun setTerms(v: String) { form.value = form.value.copy(terms = v) }
    fun setFooter(v: String) { form.value = form.value.copy(footer = v) }
    fun setIssuedNow(v: Boolean) { form.value = form.value.copy(issuedNow = v) }

    fun addLine(product: com.hisabnikash.app.data.db.ProductEntity?) {
        val f = form.value
        form.value = f.copy(
            lines = f.lines + InvoiceLineDraft(
                productId = product?.id,
                name = product?.name ?: "Custom line",
                priceText = if ((product?.sellingPriceMinor ?: 0) > 0)
                    com.hisabnikash.app.domain.model.formatMoneyPlain(product!!.sellingPriceMinor) else ""
            )
        )
    }

    fun updateLine(index: Int, line: InvoiceLineDraft) {
        val f = form.value
        val list = f.lines.toMutableList()
        if (index in list.indices) list[index] = line
        form.value = f.copy(lines = list)
    }

    fun removeLine(index: Int) {
        val f = form.value
        form.value = f.copy(lines = f.lines.filterIndexed { i, _ -> i != index })
    }

    fun save(onSaved: () -> Unit) {
        val f = form.value
        if (f.lines.isEmpty()) {
            form.value = f.copy(error = "Add at least one invoice line.")
            return
        }
        form.value = f.copy(saving = true, error = null)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val result = runCatching {
                orders.createInvoice(
                    com.hisabnikash.app.data.repo.InvoiceInput(
                        businessId = f.businessId,
                        customerId = f.customerId,
                        dateAt = f.dateAt,
                        dueDateAt = f.dueDateAt,
                        lines = f.lines.map {
                            com.hisabnikash.app.data.repo.InvoiceLine(
                                productId = it.productId,
                                name = it.name,
                                qty = it.qty,
                                unitPriceMinor = it.priceMinor
                            )
                        },
                        discountMinor = parseMoneyInput(f.discountText) ?: 0,
                        deliveryMinor = parseMoneyInput(f.deliveryText) ?: 0,
                        advanceMinor = parseMoneyInput(f.advanceText) ?: 0,
                        paymentMethod = f.paymentMethod,
                        note = f.note.ifBlank { null },
                        terms = f.terms.ifBlank { null },
                        footer = f.footer.ifBlank { null },
                        issuedNow = f.issuedNow
                    )
                )
            }
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                result.onSuccess { onSaved() }.onFailure { e ->
                    form.value = f.copy(saving = false, error = e.message ?: "Couldn't create invoice.")
                }
            }
        }
    }
}

@Composable
fun InvoiceFormRoute(container: AppContainer, navController: NavHostController, invoiceId: Long?) {
    val vm = appViewModel(container, key = "invoice-form-${invoiceId ?: 0}") { InvoiceFormViewModel(it, invoiceId ?: 0) }
    val state by vm.state.collectAsState()

    ScreenFrame(
        if (invoiceId == null) "New invoice" else "Edit invoice",
        onBack = { navController.popBackStack() }
    ) {
        AppDropdown(
            "Customer",
            state.customers,
            state.customerId?.toString(),
            { vm.setCustomer(it.id.toLong()) },
            placeholder = "Walk-in customer",
            emptyTitle = "No customers yet",
            emptyHint = "You can still create the invoice for a walk-in customer.",
            addLabel = "Add Customer",
            onAdd = { navController.navigate(Routes.NEW_CUSTOMER) }
        )
        DateField("Invoice date", state.dateAt, { vm.setDate(it) })
        DateField("Due date", state.dueDateAt ?: state.dateAt, { vm.setDue(it) })
        SectionHeader("Lines")
        AppDropdown(
            "Add product line",
            state.products,
            null,
            { option -> vm.addLine(option.value as? com.hisabnikash.app.data.db.ProductEntity) },
            placeholder = "Choose a product",
            emptyTitle = "No products yet",
            emptyHint = "Add a product or use a custom line.",
            addLabel = "Add Product",
            onAdd = { navController.navigate(Routes.NEW_PRODUCT) }
        )
        state.lines.forEachIndexed { index, line ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(line.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                            MoneyField("Unit price", line.priceMinor, { vm.updateLine(index, line.copy(priceText = com.hisabnikash.app.domain.model.formatMoneyPlain(it))) })
                        }
                    }
                }
            }
        }
        SectionHeader("Money")
        MoneyField("Discount", parseMoneyInput(state.discountText) ?: 0, { vm.setDiscount(it) })
        MoneyField("Delivery charge", parseMoneyInput(state.deliveryText) ?: 0, { vm.setDelivery(it) })
        MoneyField("Advance received", parseMoneyInput(state.advanceText) ?: 0, { vm.setAdvance(it) })
        AppDropdown(
            "Payment method",
            listOf(
                DropOption("COD", "Cash on Delivery"),
                DropOption("ADVANCE", "Advance"),
                DropOption("BKASH", "bKash"),
                DropOption("NAGAD", "Nagad"),
                DropOption("BANK", "Bank"),
                DropOption("CARD", "Card"),
                DropOption("OTHER", "Other")
            ),
            state.paymentMethod,
            { vm.setMethod(it.id) }
        )
        SectionHeader("Document")
        AppTextField("Note", state.note, { vm.setNote(it) }, singleLine = false, minLines = 2)
        AppTextField("Terms", state.terms, { vm.setTerms(it) }, singleLine = false, minLines = 2)
        AppTextField("Footer", state.footer, { vm.setFooter(it) }, singleLine = false, minLines = 2)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Issue immediately", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            androidx.compose.material3.Switch(checked = state.issuedNow, onCheckedChange = { vm.setIssuedNow(it) })
        }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
        Button(
            onClick = {
                vm.save {
                    navController.navigate(Routes.INVOICES) {
                        popUpTo(Routes.NEW_INVOICE) { inclusive = true }
                    }
                }
            },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)
        ) { Text(if (state.saving) "Saving…" else "Save invoice") }
    }
}

// ---------------------------------------------------------------------------
// Invoice detail
// ---------------------------------------------------------------------------

data class InvoiceDetailUi(
    val businessId: Long = 0,
    val invoice: InvoiceEntity? = null,
    val items: List<InvoiceItemEntity> = emptyList(),
    val business: com.hisabnikash.app.data.db.BusinessEntity? = null,
    val customer: com.hisabnikash.app.data.db.CustomerEntity? = null,
    val settings: com.hisabnikash.app.data.db.BusinessSettingsEntity? = null,
    val receipts: List<com.hisabnikash.app.data.db.ReceiptEntity> = emptyList(),
    val accounts: List<DropOption> = emptyList()
)

class InvoiceDetailViewModel(container: AppContainer, private val invoiceId: Long) : ViewModel() {

    private val db = container.database
    private val finance = container.financeRepository
    private val catalog = container.catalogRepository
    private val workspace = container.workspaceRepository

    val state: StateFlow<InvoiceDetailUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(InvoiceDetailUi())
            else combine(
                db.invoiceDao().observeById(invoiceId),
                db.invoiceDao().observeItemsFor(invoiceId),
                workspace.observeActiveWorkspace(),
                db.receiptDao().observeForInvoice(id, invoiceId),
                finance.observeAccounts(id).map { list -> list.map { DropOption("${it.id}", it.name) } }
            ) { invoice, items, ws, receipts, accounts ->
                InvoiceDetailUi(
                    businessId = id,
                    invoice = invoice,
                    items = items,
                    business = ws.business,
                    customer = null,
                    settings = ws.settings,
                    receipts = receipts,
                    accounts = accounts
                )
            }.flatMapLatest { ui ->
                val cid = ui.invoice?.customerId
                if (cid == null) flowOf(ui)
                else catalog.observeCustomer(cid).map { c -> ui.copy(customer = c) }
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            InvoiceDetailUi()
        )
}

@Composable
fun InvoiceDetailRoute(container: AppContainer, navController: NavHostController, invoiceId: Long) {
    val vm = appViewModel(container, key = "invoice-$invoiceId") { InvoiceDetailViewModel(it, invoiceId) }
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var payAmount by remember { mutableStateOf(0L) }
    var payMethod by remember { mutableStateOf("CASH") }
    var payAccount by remember { mutableStateOf<Long?>(null) }
    var paying by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }

    val invoice = state.invoice

    ScreenFrame(
        title = invoice?.invoiceNo ?: "Invoice",
        onBack = { navController.popBackStack() },
        subtitle = invoice?.let { formatDate(it.dateAt) },
        actions = {
            if (invoice != null) {
                TextButton(onClick = {
                    val file = DocumentPrinter.writePdf(
                        context,
                        DocumentPrinter.buildInvoicePdf(invoice, state.items, state.business, state.customer, state.settings),
                        invoice.invoiceNo
                    )
                    DocumentPrinter.sharePdf(context, file)
                }) {
                    Icon(Icons.Filled.IosShare, contentDescription = "Share PDF")
                    Text("PDF")
                }
                TextButton(onClick = {
                    val file = DocumentPrinter.writePdf(
                        context,
                        DocumentPrinter.buildInvoicePdf(invoice, state.items, state.business, state.customer, state.settings),
                        invoice.invoiceNo
                    )
                    DocumentPrinter.printPdf(context, invoice.invoiceNo, file)
                }) {
                    Icon(Icons.Filled.Print, contentDescription = "Print")
                    Text("Print")
                }
            }
        }
    ) {
        if (invoice == null) {
            Text("Invoice not found.", color = InkFaint, modifier = Modifier.padding(16.dp))
            return@ScreenFrame
        }
        SectionHeader("Document")
        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(invoice.status, invoiceStatusColor(invoice.status))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Customer: ${state.customer?.name ?: "Walk-in"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkFaint
                    )
                }
                Spacer(Modifier.height(6.dp))
                LabelValueRow("Subtotal", formatMoney(invoice.subtotalMinor))
                if (invoice.discountMinor > 0) LabelValueRow("Discount", "-" + formatMoney(invoice.discountMinor))
                if (invoice.deliveryMinor > 0) LabelValueRow("Delivery", formatMoney(invoice.deliveryMinor))
                if (invoice.taxMinor > 0) LabelValueRow("Tax", formatMoney(invoice.taxMinor))
                LabelValueRow("Total", formatMoney(invoice.totalMinor))
                LabelValueRow("Advance", formatMoney(invoice.advanceMinor))
                LabelValueRow("Paid", formatMoney(invoice.paidMinor))
                LabelValueRow("Balance due", formatMoney((invoice.totalMinor - invoice.advanceMinor - invoice.paidMinor).coerceAtLeast(0)))
                invoice.note?.let { LabelValueRow("Note", it) }
            }
        }
        SectionHeader("Items")
        state.items.forEach { item ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${item.qty} × ${formatMoney(item.unitPriceMinor)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkFaint
                        )
                    }
                    Text(formatMoney(item.lineTotalMinor), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
        SectionHeader("Receipts")
        if (state.receipts.isEmpty()) {
            Text("No receipts yet.", color = InkFaint, modifier = Modifier.padding(horizontal = 16.dp))
        } else {
            state.receipts.forEach { receipt ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(receipt.receiptNo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(formatDateTime(receipt.dateAt), style = MaterialTheme.typography.bodySmall, color = InkFaint)
                        }
                        Text(formatMoney(receipt.amountMinor), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BrandGreen)
                    }
                }
            }
        }
        if (invoice.status != "PAID" && invoice.status != "VOID") {
            SectionHeader("Record payment")
            ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                Column(Modifier.padding(16.dp)) {
                    MoneyField("Amount received", payAmount, { payAmount = it })
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
                        payMethod,
                        { payMethod = it.id }
                    )
                    AppDropdown("Into account", state.accounts, payAccount?.toString(), { payAccount = it.id.toLong() }, placeholder = "Optional")
                    actionError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = {
                            val amount = payAmount
                            if (amount <= 0) {
                                actionError = "Enter an amount greater than zero."
                                return@Button
                            }
                            paying = true
                            actionError = null
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                                val result = runCatching {
                                    container.orderRepository.recordPayment(
                                        com.hisabnikash.app.data.repo.PaymentInput(
                                            businessId = state.businessId,
                                            customerId = invoice.customerId,
                                            invoiceId = invoice.id,
                                            accountId = payAccount,
                                            amountMinor = amount,
                                            method = payMethod
                                        )
                                    )
                                }
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                    paying = false
                                    result.onSuccess { payAmount = 0 }.onFailure {
                                        actionError = it.message ?: "Couldn't record payment."
                                    }
                                }
                            }
                        },
                        enabled = !paying,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    ) { Text(if (paying) "Recording…" else "Record payment") }
                }
            }
        }
        if (invoice.status != "VOID") {
            OutlinedButton(
                onClick = {
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                        container.orderRepository.updateInvoiceStatus(invoice.id, if (invoice.status == "DRAFT") "ISSUED" else "VOID")
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            ) { Text(if (invoice.status == "DRAFT") "Issue invoice" else "Void invoice") }
        }
        Spacer(Modifier.height(16.dp))
    }
}
