package com.hisabnikash.app.ui.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import com.hisabnikash.app.data.container.AppContainer
import com.hisabnikash.app.data.repo.NewOrderInput
import com.hisabnikash.app.data.repo.OrderLine
import com.hisabnikash.app.domain.model.formatMoney
import com.hisabnikash.app.domain.model.parseMoneyInput
import com.hisabnikash.app.ui.components.AppDropdown
import com.hisabnikash.app.ui.components.AppTextField
import com.hisabnikash.app.ui.components.DropOption
import com.hisabnikash.app.ui.components.MoneyField
import com.hisabnikash.app.ui.components.ScreenFrame
import com.hisabnikash.app.ui.components.SectionHeader
import com.hisabnikash.app.ui.theme.InkFaint
import com.hisabnikash.app.ui.nav.Routes
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

data class FormLine(
    val productId: Long? = null,
    val name: String = "",
    val sku: String? = null,
    val qtyText: String = "1",
    val priceText: String = "",
    val costText: String = ""
) {
    val qty: Long get() = qtyText.toLongOrNull()?.coerceAtLeast(1) ?: 1
    val priceMinor: Long get() = parseMoneyInput(priceText) ?: 0
    val costMinor: Long get() = parseMoneyInput(costText) ?: 0
}

data class OrderFormUi(
    val businessId: Long = 0,
    val customerId: Long? = null,
    val channelId: Long? = null,
    val courierId: Long? = null,
    val status: String = "CONFIRMED",
    val dateAt: Long = System.currentTimeMillis(),
    val lines: List<FormLine> = emptyList(),
    val discountText: String = "",
    val deliveryText: String = "",
    val deliveryMode: String = "customer",
    val paymentMethod: String = "COD",
    val advanceText: String = "",
    val courierFeeText: String = "",
    val returnCourierFeeText: String = "",
    val packagingText: String = "",
    val advertisingText: String = "",
    val otherCostText: String = "",
    val trackingNo: String = "",
    val note: String = "",
    val tags: String = "",
    val customers: List<DropOption> = emptyList(),
    val products: List<DropOption> = emptyList(),
    val channels: List<DropOption> = emptyList(),
    val couriers: List<DropOption> = emptyList(),
    val saving: Boolean = false,
    val savedOrderId: Long? = null,
    val error: String? = null
)

class OrderFormViewModel(container: AppContainer) : ViewModel() {

    private val repo = container.orderRepository
    private val catalog = container.catalogRepository
    private val finance = container.financeRepository

    private val form = MutableStateFlow(OrderFormUi())

    val state: StateFlow<OrderFormUi> = container.workspaceRepository.observeActiveBusinessId()
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(form.value)
            else combine(
                form,
                catalog.observeCustomers(id).map { list ->
                    list.map { DropOption("${it.customer.id}", it.customer.name, it.customer.phone) }
                },
                catalog.observeProducts(id).map { list ->
                    list.map {
                        DropOption(
                            "${it.product.id}", it.product.name,
                            "SKU ${it.product.sku ?: "—"} • ${formatMoney(it.product.sellingPriceMinor)}",
                            value = it.product
                        )
                    }
                },
                finance.observeChannels(id),
                container.database.courierDao().observeAll(id)
            ) { f, customers, products, channels, couriers ->
                f.copy(
                    businessId = id,
                    customers = customers,
                    products = products,
                    channels = channels.map { DropOption("${it.id}", it.name) },
                    couriers = couriers.map { DropOption("${it.id}", it.name) }
                )
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            OrderFormUi()
        )

    fun setCustomer(id: Long?) = form.value = form.value.copy(customerId = id)
    fun setChannel(id: Long?) = form.value = form.value.copy(channelId = id)
    fun setCourier(id: Long?) = form.value = form.value.copy(courierId = id)
    fun setStatus(status: String) = form.value = form.value.copy(status = status)
    fun setDeliveryMode(mode: String) = form.value = form.value.copy(deliveryMode = mode)
    fun setPaymentMethod(method: String) = form.value = form.value.copy(paymentMethod = method)
    fun setDiscount(text: String) = form.value = form.value.copy(discountText = text)
    fun setDelivery(text: String) = form.value = form.value.copy(deliveryText = text)
    fun setAdvance(text: String) = form.value = form.value.copy(advanceText = text)
    fun setCourierFee(text: String) = form.value = form.value.copy(courierFeeText = text)
    fun setReturnCourierFee(text: String) = form.value = form.value.copy(returnCourierFeeText = text)
    fun setPackaging(text: String) = form.value = form.value.copy(packagingText = text)
    fun setAdvertising(text: String) = form.value = form.value.copy(advertisingText = text)
    fun setOtherCost(text: String) = form.value = form.value.copy(otherCostText = text)
    fun setTracking(text: String) = form.value = form.value.copy(trackingNo = text)
    fun setNote(text: String) = form.value = form.value.copy(note = text)
    fun setTags(text: String) = form.value = form.value.copy(tags = text)

    fun addLine(productId: Long?, priceMinor: Long, costMinor: Long, name: String, sku: String?) {
        val current = form.value
        form.value = current.copy(
            lines = current.lines + FormLine(
                productId = productId,
                name = if (name.isBlank()) "Item" else name,
                sku = sku,
                priceText = if (priceMinor > 0) com.hisabnikash.app.domain.model.formatMoneyPlain(priceMinor) else "",
                costText = if (costMinor > 0) com.hisabnikash.app.domain.model.formatMoneyPlain(costMinor) else ""
            )
        )
    }

    fun updateLine(index: Int, line: FormLine) {
        val current = form.value
        val lines = current.lines.toMutableList()
        if (index in lines.indices) lines[index] = line
        form.value = current.copy(lines = lines)
    }

    fun removeLine(index: Int) {
        val current = form.value
        form.value = current.copy(lines = current.lines.filterIndexed { i, _ -> i != index })
    }

    fun save(navController: NavHostController) {
        val f = form.value
        if (f.lines.isEmpty()) {
            form.value = f.copy(error = "Add at least one product line.")
            return
        }
        form.value = f.copy(saving = true, error = null)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val result = runCatching {
                repo.createOrder(
                    NewOrderInput(
                        businessId = f.businessId,
                        customerId = f.customerId,
                        channelId = f.channelId,
                        courierId = f.courierId,
                        status = f.status,
                        orderDate = f.dateAt,
                        lines = f.lines.map {
                            OrderLine(
                                productId = it.productId,
                                name = it.name,
                                sku = it.sku,
                                qty = it.qty,
                                unitPriceMinor = it.priceMinor,
                                unitCostMinor = it.costMinor
                            )
                        },
                        discountMinor = parseMoneyInput(f.discountText) ?: 0,
                        deliveryChargeMinor = parseMoneyInput(f.deliveryText) ?: 0,
                        deliveryMode = f.deliveryMode,
                        paymentMethod = f.paymentMethod,
                        advanceMinor = parseMoneyInput(f.advanceText) ?: 0,
                        courierFeeMinor = parseMoneyInput(f.courierFeeText) ?: 0,
                        returnCourierFeeMinor = parseMoneyInput(f.returnCourierFeeText) ?: 0,
                        packagingMinor = parseMoneyInput(f.packagingText) ?: 0,
                        advertisingMinor = parseMoneyInput(f.advertisingText) ?: 0,
                        otherCostMinor = parseMoneyInput(f.otherCostText) ?: 0,
                        trackingNo = f.trackingNo.ifBlank { null },
                        note = f.note.ifBlank { null },
                        tags = f.tags.ifBlank { null }
                    )
                )
            }
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                result.onSuccess {
                    navController.navigate(Routes.order(it)) {
                        popUpTo(Routes.NEW_ORDER) { inclusive = true }
                    }
                }.onFailure { e ->
                    form.value = f.copy(
                        saving = false,
                        error = "Couldn't save the order. ${e.message ?: "Check the required fields and try again."}"
                    )
                }
            }
        }
    }
}

@Composable
fun OrderFormRoute(container: AppContainer, navController: NavHostController, orderId: Long?) {
    val vm = appViewModel(container) { OrderFormViewModel(it) }
    val state by vm.state.collectAsState()

    ScreenFrame("New order", onBack = { navController.popBackStack() }) {
        AppDropdown(
            "Customer",
            state.customers,
            state.customerId?.toString(),
            { vm.setCustomer(it.value as? Long ?: it.id.toLong()) },
            placeholder = "Walk-in customer",
            emptyTitle = "No customers yet",
            emptyHint = "Add a customer first or create the order for a walk-in customer.",
            addLabel = "Add Customer",
            onAdd = { navController.navigate(Routes.NEW_CUSTOMER) }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.weight(1f)) {
                AppDropdown(
                    "Channel",
                    state.channels,
                    state.channelId?.toString(),
                    { vm.setChannel(it.value as? Long ?: it.id.toLong()) },
                    placeholder = "Select channel",
                    emptyTitle = "No channels yet",
                    emptyHint = "Channels are set up when your business is created."
                )
            }
            Box(Modifier.weight(1f)) {
                AppDropdown(
                    "Courier",
                    state.couriers,
                    state.courierId?.toString(),
                    { vm.setCourier(it.value as? Long ?: it.id.toLong()) },
                    placeholder = "Not assigned",
                    emptyTitle = "No couriers yet",
                    emptyHint = "Default couriers are added when your business is created.",
                    addLabel = "Add Courier",
                    onAdd = { navController.navigate(Routes.COURIER_EDIT) }
                )
            }
        }
        AppDropdown(
            "Status",
            listOf(
                DropOption("DRAFT", "Draft"),
                DropOption("CONFIRMED", "Confirmed"),
                DropOption("PROCESSING", "Processing"),
                DropOption("PACKED", "Packed"),
                DropOption("SHIPPED", "Shipped"),
                DropOption("DELIVERED", "Delivered")
            ),
            state.status,
            { vm.setStatus(it.id) }
        )
        AppDropdown(
            "Payment method",
            listOf(
                DropOption("COD", "Cash on Delivery"),
                DropOption("ADVANCE", "Advance"),
                DropOption("BKASH", "bKash"),
                DropOption("NAGAD", "Nagad"),
                DropOption("CASH", "Cash"),
                DropOption("BANK", "Bank"),
                DropOption("CARD", "Card"),
                DropOption("OTHER", "Other")
            ),
            state.paymentMethod,
            { vm.setPaymentMethod(it.id) }
        )

        SectionHeader("Products")
        state.lines.forEachIndexed { index, line ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            line.name,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        IconButton(onClick = { vm.removeLine(index) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove line")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            AppTextField(
                                "Qty",
                                line.qtyText,
                                { vm.updateLine(index, line.copy(qtyText = it.filter(Char::isDigit))) },
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            MoneyField("Price", line.priceMinor, { vm.updateLine(index, line.copy(priceText = com.hisabnikash.app.domain.model.formatMoneyPlain(it))) })
                        }
                        Column(Modifier.weight(1f)) {
                            MoneyField("Cost", line.costMinor, { vm.updateLine(index, line.copy(costText = com.hisabnikash.app.domain.model.formatMoneyPlain(it))) })
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { vm.addLine(null, 0, 0, "Custom item", null) },
                modifier = Modifier.padding(horizontal = 16.dp)
            ) { Icon(Icons.Filled.Add, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Custom item") }
        }
        // Product dropdown to add stock items
        val productOptions = state.products
        AppDropdown(
            "Add product from catalog",
            productOptions,
            null,
            { option ->
                val product = option.value as? com.hisabnikash.app.data.db.ProductEntity
                vm.addLine(
                    option.id.toLong(),
                    product?.sellingPriceMinor ?: 0,
                    product?.purchaseCostMinor ?: 0,
                    option.label,
                    product?.sku
                )
            },
            placeholder = "Choose a product",
            emptyTitle = "No products yet",
            emptyHint = "Add a product first, or use a custom item line.",
            addLabel = "Add Product",
            onAdd = { navController.navigate(Routes.NEW_PRODUCT) }
        )

        SectionHeader("Money")
        MoneyField("Discount", parseMoneyInput(state.discountText) ?: 0, { vm.setDiscount(com.hisabnikash.app.domain.model.formatMoneyPlain(it)) })
        MoneyField("Customer delivery charge", parseMoneyInput(state.deliveryText) ?: 0, { vm.setDelivery(com.hisabnikash.app.domain.model.formatMoneyPlain(it)) })
        MoneyField("Advance received", parseMoneyInput(state.advanceText) ?: 0, { vm.setAdvance(com.hisabnikash.app.domain.model.formatMoneyPlain(it)) })
        SectionHeader("Costs")
        MoneyField("Courier fee", parseMoneyInput(state.courierFeeText) ?: 0, { vm.setCourierFee(com.hisabnikash.app.domain.model.formatMoneyPlain(it)) })
        MoneyField("Return courier fee", parseMoneyInput(state.returnCourierFeeText) ?: 0, { vm.setReturnCourierFee(com.hisabnikash.app.domain.model.formatMoneyPlain(it)) })
        MoneyField("Packaging", parseMoneyInput(state.packagingText) ?: 0, { vm.setPackaging(com.hisabnikash.app.domain.model.formatMoneyPlain(it)) })
        MoneyField("Advertising", parseMoneyInput(state.advertisingText) ?: 0, { vm.setAdvertising(com.hisabnikash.app.domain.model.formatMoneyPlain(it)) })
        MoneyField("Other costs", parseMoneyInput(state.otherCostText) ?: 0, { vm.setOtherCost(com.hisabnikash.app.domain.model.formatMoneyPlain(it)) })

        SectionHeader("Delivery")
        AppDropdown(
            "Delivery economics",
            listOf(
                DropOption("customer", "Customer pays delivery"),
                DropOption("merchant", "Merchant pays / free delivery"),
                DropOption("included", "Included in price"),
                DropOption("prepaid", "Customer prepaid delivery"),
                DropOption("partial", "Partial subsidy"),
                DropOption("custom", "Custom")
            ),
            state.deliveryMode,
            { vm.setDeliveryMode(it.id) }
        )
        AppTextField("Tracking number", state.trackingNo, { vm.setTracking(it) }, placeholder = "e.g. PA-1234567890")
        AppTextField("Notes", state.note, { vm.setNote(it) }, singleLine = false, minLines = 2)
        AppTextField("Tags", state.tags, { vm.setTags(it) }, placeholder = "gift, urgent, holi")

        Spacer(Modifier.height(10.dp))
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
        }
        Button(
            onClick = { vm.save(navController) },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(if (state.saving) "Saving…" else "Save order")
        }
    }
}
