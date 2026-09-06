package com.hisabnikash.app.ui.products

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.hisabnikash.app.data.container.AppContainer
import com.hisabnikash.app.data.db.ProductAggregate
import com.hisabnikash.app.data.db.ProductEntity
import com.hisabnikash.app.data.db.ProductVariantEntity
import com.hisabnikash.app.domain.model.formatDateTime
import com.hisabnikash.app.domain.model.formatMoney
import com.hisabnikash.app.domain.model.parseMoneyInput
import com.hisabnikash.app.ui.components.AppDropdown
import com.hisabnikash.app.ui.components.AppTextField
import com.hisabnikash.app.ui.components.DropOption
import com.hisabnikash.app.ui.components.EmptyState
import com.hisabnikash.app.ui.components.FilterChips
import com.hisabnikash.app.ui.components.LabelValueRow
import com.hisabnikash.app.ui.components.MetricCard
import com.hisabnikash.app.ui.components.MoneyField
import com.hisabnikash.app.ui.components.ScreenFrame
import com.hisabnikash.app.ui.components.SectionHeader
import com.hisabnikash.app.ui.components.StatusChip
import com.hisabnikash.app.ui.components.TonalCard
import com.hisabnikash.app.ui.theme.BrandGreenSoft
import com.hisabnikash.app.ui.theme.InkFaint
import com.hisabnikash.app.ui.theme.Spacing
import com.hisabnikash.app.ui.nav.Routes
import com.hisabnikash.app.ui.theme.BrandGreen
import com.hisabnikash.app.ui.theme.Warning
import com.hisabnikash.app.ui.theme.Error
import java.io.File
import com.hisabnikash.app.ui.vm.appViewModel
import kotlinx.coroutines.flow.Flow
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
// Products tab
// ---------------------------------------------------------------------------

data class ProductsUi(
    val loading: Boolean = true,
    val filter: String = "ALL",
    val query: String = "",
    val products: List<ProductAggregate> = emptyList(),
    val inventoryValueMinor: Long = 0,
    val totalUnits: Long = 0,
    val lowCount: Long = 0,
    val outCount: Long = 0,
    val productCount: Long = 0
)

class ProductsViewModel(container: AppContainer) : ViewModel() {

    private val catalog = container.catalogRepository
    private val filterFlow = MutableStateFlow("ALL")
    private val queryFlow = MutableStateFlow("")

    val state: StateFlow<ProductsUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(ProductsUi().copy(loading = false))
            else combine(filterFlow, queryFlow) { f, q -> f to q }
                .flatMapLatest { (filter, query) ->
                    val productsFlow = catalog.observeProducts(id)
                    val left = combine(
                        productsFlow,
                        catalog.observeInventoryValue(id),
                        catalog.observeTotalUnits(id),
                        catalog.observeLowStock(id),
                        catalog.observeOutOfStock(id)
                    ) { products, value, units, low, out ->
                        ProductsData(products, value, units, low.size.toLong(), out.size.toLong())
                    }
                    combine(left, productsFlow) { data, full -> data to full.size.toLong() }
                        .map { (data, count) ->
                            val filtered = data.products.filter { agg ->
                                when (filter) {
                                    "LOW" -> agg.product.stockQty > 0 && agg.product.stockQty <= agg.product.lowStockThreshold
                                    "OUT" -> agg.product.stockQty <= 0
                                    "ACTIVE" -> agg.product.status == "ACTIVE"
                                    else -> true
                                }
                            }.filter { agg ->
                                query.isBlank() ||
                                    agg.product.name.contains(query, true) ||
                                    (agg.product.sku?.contains(query, true) == true) ||
                                    (agg.product.category?.contains(query, true) == true)
                            }
                            ProductsUi(
                                loading = false,
                                filter = filter,
                                query = query,
                                products = filtered,
                                inventoryValueMinor = data.inventoryValueMinor,
                                totalUnits = data.totalUnits,
                                lowCount = data.lowCount,
                                outCount = data.outCount,
                                productCount = count
                            )
                        }
                }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            ProductsUi()
        )

    fun setFilter(filter: String) { filterFlow.value = filter }
    fun setQuery(query: String) { queryFlow.value = query }
}

@Composable
fun ProductsTab(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { ProductsViewModel(it) }
    val state by vm.state.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Routes.NEW_PRODUCT) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Product") }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Products",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            ) {
                MetricCard("Products", "${state.productCount}", Modifier.weight(1f))
                MetricCard("Units", "${state.totalUnits}", Modifier.weight(1f))
                MetricCard("Value", formatMoney(state.inventoryValueMinor), Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            AppTextField(
                "Search",
                state.query,
                { vm.setQuery(it) },
                placeholder = "Name, SKU or category"
            )
            FilterChips(
                listOf("ALL", "ACTIVE", "LOW", "OUT"),
                state.filter,
                { vm.setFilter(it) }
            )
            if (state.products.isEmpty()) {
                EmptyState(
                    Icons.Filled.Inventory2,
                    if (state.query.isBlank()) "No products yet" else "No matches found",
                    if (state.query.isBlank())
                        "Add your products with stock and cost so orders can be priced automatically."
                    else "Try a different name, SKU or category.",
                    actionLabel = if (state.query.isBlank()) "Add Product" else null,
                    onAction = { navController.navigate(Routes.NEW_PRODUCT) }
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.products, key = { it.product.id }) { agg ->
                        ProductRow(
                            agg,
                            onClick = { navController.navigate(Routes.product(agg.product.id)) },
                            store = container.productImageStore
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductRow(
    agg: ProductAggregate,
    onClick: () -> Unit,
    store: com.hisabnikash.app.data.media.ProductImageStore
) {
    val product = agg.product
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductThumbnail(product, store, Modifier)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        product.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    when {
                        product.stockQty <= 0 -> StatusChip("Out", Error)
                        product.stockQty <= product.lowStockThreshold -> StatusChip("Low", Warning)
                        else -> StatusChip("In stock", BrandGreen)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    listOfNotNull(product.sku?.let { "SKU $it" }, product.category).joinToString(" • ").ifBlank { "No SKU" },
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint,
                    maxLines = 1
                )
                Text(
                    "${product.stockQty} in stock • ${agg.unitsSold} sold",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint,
                    maxLines = 1
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatMoney(product.sellingPriceMinor),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    formatMoney(agg.profitMinor),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (agg.profitMinor >= 0) BrandGreen else Error
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Product form
// ---------------------------------------------------------------------------

data class VariantDraft(
    val id: Long = 0,
    val name: String = "",
    val sku: String = "",
    val priceText: String = "",
    val costText: String = "",
    val stockText: String = "0"
)

data class ProductForm(
    val businessId: Long = 0,
    val productId: Long = 0,
    val name: String = "",
    val sku: String = "",
    val category: String = "",
    val priceText: String = "",
    val costText: String = "",
    val stockText: String = "0",
    val thresholdText: String = "5",
    val supplierId: Long? = null,
    val description: String = "",
    val notes: String = "",
    val status: String = "ACTIVE",
    val imagePath: String? = null,
    val variants: List<VariantDraft> = emptyList(),
    val suppliers: List<DropOption> = emptyList(),
    val saving: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null
)

class ProductFormViewModel(private val container: AppContainer, private val productId: Long) : ViewModel() {

    private val catalog = container.catalogRepository
    private val form = MutableStateFlow(ProductForm())

    val state: StateFlow<ProductForm> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(form.value)
            else combine(
                form,
                catalog.observeSuppliers(id).map { list ->
                    list.map { DropOption("${it.id}", it.name, it.phone) }
                }
            ) { f, suppliers -> f.copy(businessId = id, suppliers = suppliers) }
                .flatMapLatest { f ->
                    if (productId > 0 && !f.loaded) {
                        catalog.observeProduct(productId).map { product ->
                            f.copy(loaded = true, productId = productId).withProduct(product)
                        }
                    } else flowOf(f)
                }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            ProductForm()
        )

    private fun ProductForm.withProduct(product: ProductEntity?): ProductForm {
        if (product == null || product.businessId != businessId) return this
        return copy(
            name = product.name,
            sku = product.sku ?: "",
            category = product.category ?: "",
            priceText = com.hisabnikash.app.domain.model.formatMoneyPlain(product.sellingPriceMinor),
            costText = com.hisabnikash.app.domain.model.formatMoneyPlain(product.purchaseCostMinor),
            stockText = "${product.stockQty}",
            thresholdText = "${product.lowStockThreshold}",
            supplierId = product.supplierId,
            description = product.description ?: "",
            notes = product.notes ?: "",
            status = product.status,
            imagePath = product.imagePath
        )
    }

    fun setName(v: String) = { form.value = form.value.copy(name = v) }()
    fun setSku(v: String) = { form.value = form.value.copy(sku = v) }()
    fun setCategory(v: String) = { form.value = form.value.copy(category = v) }()
    fun setPrice(v: Long) = { form.value = form.value.copy(priceText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }()
    fun setCost(v: Long) = { form.value = form.value.copy(costText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }()
    fun setStock(v: String) = { form.value = form.value.copy(stockText = v.filter(Char::isDigit)) }()
    fun setThreshold(v: String) = { form.value = form.value.copy(thresholdText = v.filter(Char::isDigit)) }()
    fun setSupplier(v: Long?) = { form.value = form.value.copy(supplierId = v) }()
    fun setDescription(v: String) = { form.value = form.value.copy(description = v) }()
    fun setNotes(v: String) = { form.value = form.value.copy(notes = v) }()
    fun setStatus(v: String) = { form.value = form.value.copy(status = v) }()

    fun setImagePath(path: String?) {
        val old = form.value.imagePath
        if (old != null && old != path) container.productImageStore.delete(old)
        form.value = form.value.copy(imagePath = path)
    }

    fun addVariant() {
        val f = form.value
        form.value = f.copy(variants = f.variants + VariantDraft())
    }

    fun updateVariant(index: Int, draft: VariantDraft) {
        val f = form.value
        val list = f.variants.toMutableList()
        if (index in list.indices) list[index] = draft
        form.value = f.copy(variants = list)
    }

    fun removeVariant(index: Int) {
        val f = form.value
        form.value = f.copy(variants = f.variants.filterIndexed { i, _ -> i != index })
    }

    fun save(onSaved: (Long) -> Unit) {
        val f = form.value
        if (f.name.isBlank()) {
            form.value = f.copy(error = "Product name is required.")
            return
        }
        form.value = f.copy(saving = true, error = null)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val result = runCatching {
                catalog.saveProduct(
                    ProductEntity(
                        id = f.productId,
                        businessId = f.businessId,
                        name = f.name.trim(),
                        sku = f.sku.ifBlank { null },
                        category = f.category.ifBlank { null },
                        sellingPriceMinor = parseMoneyInput(f.priceText) ?: 0,
                        purchaseCostMinor = parseMoneyInput(f.costText) ?: 0,
                        stockQty = f.stockText.toLongOrNull() ?: 0,
                        lowStockThreshold = f.thresholdText.toLongOrNull() ?: 5,
                        supplierId = f.supplierId,
                        description = f.description.ifBlank { null },
                        notes = f.notes.ifBlank { null },
                        status = f.status,
                        imagePath = f.imagePath
                    ),
                    f.variants.map {
                        ProductVariantEntity(
                            businessId = f.businessId,
                            productId = f.productId,
                            name = it.name.ifBlank { "Variant" },
                            sku = it.sku.ifBlank { null },
                            priceMinor = parseMoneyInput(it.priceText),
                            costMinor = parseMoneyInput(it.costText),
                            stockQty = it.stockText.toLongOrNull() ?: 0
                        )
                    }
                )
            }
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                result.onSuccess(onSaved).onFailure { e ->
                    form.value = f.copy(saving = false, error = "Couldn't save product: ${e.message}")
                }
            }
        }
    }
}

@Composable
fun ProductFormRoute(container: AppContainer, navController: NavHostController, productId: Long?) {
    val vm = appViewModel(container, key = "product-form-$productId") {
        ProductFormViewModel(it, productId ?: 0)
    }
    val state by vm.state.collectAsState()

    ScreenFrame(
        if (productId == null) "New product" else "Edit product",
        onBack = { navController.popBackStack() }
    ) {
        if (productId != null && !state.loaded) {
            Text("Loading product…", color = InkFaint, modifier = Modifier.padding(16.dp))
            return@ScreenFrame
        }
        AppTextField("Product name", state.name, { vm.setName(it) }, placeholder = "e.g. Cotton Kurti")
        Spacer(Modifier.height(2.dp))

        SectionHeader("Photo")
        if (productId == null || state.loaded) {
            ProductImageSection(
                imagePath = state.imagePath,
                onImage = { vm.setImagePath(it) },
                store = container.productImageStore
            )
        }

        SectionHeader("Info")
        TonalCard {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) { AppTextField("SKU", state.sku, { vm.setSku(it) }, placeholder = "SKU-001") }
                Column(Modifier.weight(1f)) { AppTextField("Category", state.category, { vm.setCategory(it) }, placeholder = "Clothing") }
            }
            Spacer(Modifier.height(Spacing.Sm))
            AppDropdown(
                "Status",
                listOf(
                    DropOption("ACTIVE", "Active — sellable"),
                    DropOption("INACTIVE", "Inactive — hidden from new orders")
                ),
                state.status,
                { vm.setStatus(it.id) }
            )
        }

        SectionHeader("Pricing")
        TonalCard {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) { MoneyField("Selling price", parseMoneyInput(state.priceText) ?: 0, { vm.setPrice(it) }) }
                Column(Modifier.weight(1f)) { MoneyField("Purchase cost", parseMoneyInput(state.costText) ?: 0, { vm.setCost(it) }) }
            }
        }

        SectionHeader("Inventory")
        TonalCard {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    AppTextField("Stock on hand", state.stockText, { vm.setStock(it) }, keyboardType = KeyboardType.Number)
                }
                Column(Modifier.weight(1f)) {
                    AppTextField("Low-stock alert at", state.thresholdText, { vm.setThreshold(it) }, keyboardType = KeyboardType.Number)
                }
            }
        }

        SectionHeader("Supplier")
        TonalCard {
            AppDropdown(
                "Default supplier",
                state.suppliers,
                state.supplierId?.toString(),
                { vm.setSupplier(it.id.toLong()) },
                placeholder = "Not linked",
                emptyTitle = "No suppliers yet",
                emptyHint = "Link a supplier later from Purchases.",
                addLabel = "Add Supplier",
                onAdd = { navController.navigate(Routes.NEW_SUPPLIER) }
            )
        }

        SectionHeader("Variants (optional)")
        TonalCard {
            if (state.variants.isEmpty()) {
                Text(
                    "Add size, colour or model options that can carry their own price and stock.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            state.variants.forEachIndexed { index, variant ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Variant ${index + 1}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            IconButton(onClick = { vm.removeVariant(index) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove variant")
                            }
                        }
                        AppTextField("Name", variant.name, { vm.updateVariant(index, variant.copy(name = it)) }, placeholder = "e.g. Medium / Black")
                        Row(Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                AppTextField("SKU", variant.sku, { vm.updateVariant(index, variant.copy(sku = it)) })
                            }
                            Column(Modifier.weight(1f)) {
                                AppTextField("Stock", variant.stockText, { vm.updateVariant(index, variant.copy(stockText = it.filter(Char::isDigit))) }, keyboardType = KeyboardType.Number)
                            }
                        }
                        Row(Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                MoneyField("Price", parseMoneyInput(variant.priceText) ?: 0, { vm.updateVariant(index, variant.copy(priceText = com.hisabnikash.app.domain.model.formatMoneyPlain(it))) })
                            }
                            Column(Modifier.weight(1f)) {
                                MoneyField("Cost", parseMoneyInput(variant.costText) ?: 0, { vm.updateVariant(index, variant.copy(costText = com.hisabnikash.app.domain.model.formatMoneyPlain(it))) })
                            }
                        }
                    }
                }
            }
            TextButton(onClick = { vm.addVariant() }, modifier = Modifier.align(Alignment.Start)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Add variant")
            }
        }

        SectionHeader("Details")
        TonalCard {
            AppTextField("Description", state.description, { vm.setDescription(it) }, singleLine = false, minLines = 2)
            Spacer(Modifier.height(Spacing.Sm))
            AppTextField("Internal notes", state.notes, { vm.setNotes(it) }, singleLine = false, minLines = 2)
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
        Button(
            onClick = {
                vm.save { id ->
                    navController.navigate(Routes.product(id)) {
                        popUpTo(Routes.NEW_PRODUCT) { inclusive = true }
                    }
                }
            },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
        ) { Text(if (state.saving) "Saving…" else "Save product") }
    }
}

// ---------------------------------------------------------------------------
// Product image
// ---------------------------------------------------------------------------

/** Compact list thumbnail: real photo when set, neutral icon otherwise. */
@Composable
private fun ProductThumbnail(
    product: ProductEntity,
    store: com.hisabnikash.app.data.media.ProductImageStore,
    modifier: Modifier
) {
    val image = store.file(product.imagePath)
    if (image != null) {
        AsyncImage(
            model = image,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
        )
    } else {
        Box(
            modifier = modifier
                .size(48.dp)
                .background(BrandGreenSoft, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Professional product-image workflow: gallery (photo picker — no permission
 * needed on Android 13+ and gracefully falling back on older versions),
 * camera capture, replace, remove and preview. Images are imported into
 * app-internal storage by [com.hisabnikash.app.data.media.ProductImageStore]
 * and rendered through Coil, which downsamples to the view size.
 */
@Composable
private fun ProductImageSection(
    imagePath: String?,
    onImage: (String?) -> Unit,
    store: com.hisabnikash.app.data.media.ProductImageStore
) {
    val context = LocalContext.current
    var cameraTarget by remember { mutableStateOf<java.io.File?>(null) }

    val gallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val stored = store.import(uri)
            if (stored != null) onImage(stored)
        }
    }
    val camera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val file = cameraTarget
        cameraTarget = null
        if (success && file != null) {
            val stored = store.importCamera(file)
            if (stored != null) onImage(stored)
        } else {
            file?.delete()
        }
    }

    TonalCard {
        Column {
            if (imagePath == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(BrandGreenSoft, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.AddAPhoto,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.width(Spacing.Lg))
                    Column(Modifier.weight(1f)) {
                        Text("Add product photo", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Photos stay on this device and help you recognise products fast.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkFaint
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.Lg))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Sm)) {
                    OutlinedButton(
                        onClick = {
                            gallery.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add photo")
                    }
                    OutlinedButton(
                        onClick = {
                            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
                            val file = File(dir, "camera_${System.currentTimeMillis()}.jpg")
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            cameraTarget = file
                            camera.launch(uri)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Take photo")
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = store.file(imagePath),
                        contentDescription = "Product photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(Modifier.width(Spacing.Lg))
                    Column(Modifier.weight(1f)) {
                        Text("Photo added", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Shown in product lists and on the product page.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkFaint
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.Lg))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Sm)) {
                    OutlinedButton(
                        onClick = {
                            gallery.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Replace")
                    }
                    OutlinedButton(
                        onClick = { onImage(null) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Remove")
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Product detail
// ---------------------------------------------------------------------------

data class ProductDetailUi(
    val loading: Boolean = true,
    val businessId: Long = 0,
    val aggregate: ProductAggregate? = null,
    val variants: List<ProductVariantEntity> = emptyList(),
    val movements: List<com.hisabnikash.app.data.db.InventoryMovementEntity> = emptyList()
)

class ProductDetailViewModel(container: AppContainer, private val productId: Long) : ViewModel() {

    private val catalog = container.catalogRepository

    val state: StateFlow<ProductDetailUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null) flowOf(ProductDetailUi().copy(loading = false))
            else combine(
                catalog.observeProducts(id).map { list -> list.firstOrNull { it.product.id == productId } },
                catalog.observeMovementsForProduct(id, productId)
            ) { product, movements ->
                ProductDetailUi(
                    loading = false,
                    businessId = id,
                    aggregate = product,
                    movements = movements
                )
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            ProductDetailUi()
        )
}

@Composable
fun ProductDetailRoute(container: AppContainer, navController: NavHostController, productId: Long) {
    val vm = appViewModel(container, key = "product-detail-$productId") {
        ProductDetailViewModel(it, productId)
    }
    val state by vm.state.collectAsState()
    val catalog = container.catalogRepository
    var newStock by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var adjusting by remember { mutableStateOf(false) }
    var adjustError by remember { mutableStateOf<String?>(null) }

    val product = state.aggregate?.product

    ScreenFrame(
        title = product?.name ?: "Product",
        onBack = { navController.popBackStack() },
        subtitle = product?.sku ?: "Product details",
        actions = {
            if (product != null) {
                TextButton(onClick = { navController.navigate(Routes.productEdit(product.id)) }) { Text("Edit") }
            }
        }
    ) {
        if (product == null) {
            Text("Product not found.", color = InkFaint, modifier = Modifier.padding(16.dp))
            return@ScreenFrame
        }
        container.productImageStore.file(product.imagePath)?.let { image ->
            AsyncImage(
                model = image,
                contentDescription = "Photo of ${product.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.ScreenMargin, vertical = 6.dp)
                    .height(180.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
        }
        SectionHeader("Performance")
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            MetricCard("Sold", "${state.aggregate?.unitsSold ?: 0}", Modifier.weight(1f))
            MetricCard("Revenue", formatMoney(state.aggregate?.revenueMinor ?: 0), Modifier.weight(1f))
            MetricCard("Profit", formatMoney(state.aggregate?.profitMinor ?: 0), Modifier.weight(1f))
        }
        SectionHeader("Stock")
        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            Column(Modifier.padding(16.dp)) {
                LabelValueRow("On hand", "${product.stockQty}")
                LabelValueRow("Low-stock alert at", "${product.lowStockThreshold}")
                LabelValueRow("Selling price", formatMoney(product.sellingPriceMinor))
                LabelValueRow("Purchase cost", formatMoney(product.purchaseCostMinor))
                LabelValueRow("SKU", product.sku ?: "—")
                LabelValueRow("Category", product.category ?: "—")
                LabelValueRow("Status", product.status.replaceFirstChar { it.uppercase() })
            }
        }
        SectionHeader("Adjust stock")
        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            Column(Modifier.padding(16.dp)) {
                AppTextField(
                    "New stock quantity",
                    if (newStock.isBlank()) "${product.stockQty}" else newStock,
                    { newStock = it.filter(Char::isDigit) },
                    keyboardType = KeyboardType.Number
                )
                AppTextField("Reason", reason, { reason = it }, placeholder = "e.g. Damaged, cycle count, received stock")
                adjustError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = {
                        val qty = newStock.toLongOrNull() ?: product.stockQty
                        if (reason.isBlank()) {
                            adjustError = "A reason is required so the stock log is trustworthy."
                            return@Button
                        }
                        adjusting = true
                        adjustError = null
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                            val result = runCatching {
                                catalog.adjustStock(
                                    com.hisabnikash.app.data.repo.StockAdjustmentInput(
                                        businessId = state.businessId,
                                        productId = product.id,
                                        newStockQty = qty,
                                        reason = reason.trim(),
                                        notes = null
                                    )
                                )
                            }
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                result.onSuccess {
                                    adjusting = false
                                    newStock = ""
                                    reason = ""
                                }.onFailure { e ->
                                    adjusting = false
                                    adjustError = e.message ?: "Couldn't adjust stock."
                                }
                            }
                        }
                    },
                    enabled = !adjusting
                ) { Text(if (adjusting) "Adjusting…" else "Apply adjustment") }
            }
        }
        if (state.variants.isNotEmpty()) {
            SectionHeader("Variants")
            state.variants.forEach { variant ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(variant.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "SKU ${variant.sku ?: "—"} • ${variant.stockQty} in stock",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkFaint
                            )
                        }
                        Text(
                            formatMoney(variant.priceMinor ?: product.sellingPriceMinor),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        SectionHeader("Stock log")
        if (state.movements.isEmpty()) {
            Text(
                "No stock movements yet. Purchases, sales and adjustments appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkFaint,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            state.movements.forEach { movement ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                movement.type.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                formatDateTime(movement.dateAt) + (movement.reason?.let { " • $it" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = InkFaint,
                                maxLines = 2
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                (if (movement.qty >= 0) "+" else "") + movement.qty,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (movement.qty >= 0) BrandGreen else Error
                            )
                            Text("Balance ${movement.balanceAfter}", style = MaterialTheme.typography.labelSmall, color = InkFaint)
                        }
                    }
                }
            }
        }
    }
}

private data class ProductsData(
    val products: List<com.hisabnikash.app.data.db.ProductAggregate>,
    val inventoryValueMinor: Long,
    val totalUnits: Long,
    val lowCount: Long,
    val outCount: Long
)
