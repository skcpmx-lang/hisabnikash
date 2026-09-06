package com.hisabnikash.app.data.repo

import androidx.room.withTransaction
import com.hisabnikash.app.data.db.AccountTransactionEntity
import com.hisabnikash.app.data.db.AppDatabase
import com.hisabnikash.app.data.db.CustomerEntity
import com.hisabnikash.app.data.db.InventoryMovementEntity
import com.hisabnikash.app.data.db.ProductEntity
import com.hisabnikash.app.data.db.ProductVariantEntity
import com.hisabnikash.app.data.db.PurchaseEntity
import com.hisabnikash.app.data.db.PurchaseItemEntity
import com.hisabnikash.app.data.db.SupplierEntity
import kotlinx.coroutines.flow.Flow
import java.io.File

data class PurchaseLine(
    val productId: Long,
    val qty: Long,
    val unitCostMinor: Long
)

data class NewPurchaseInput(
    val businessId: Long,
    val supplierId: Long,
    val dateAt: Long = System.currentTimeMillis(),
    val lines: List<PurchaseLine>,
    val paidMinor: Long = 0,
    val accountId: Long? = null,
    val notes: String? = null
)

data class StockAdjustmentInput(
    val businessId: Long,
    val productId: Long,
    val newStockQty: Long,
    val reason: String,
    val notes: String? = null
)

class CatalogRepository(private val db: AppDatabase, private val workspace: WorkspaceRepository) {

    // -------------------------------------------------------------- customers

    fun observeCustomers(businessId: Long) = db.customerDao().observeAll(businessId)

    fun searchCustomers(businessId: Long, query: String) = db.customerDao().search(businessId, query)

    fun observeCustomer(id: Long) = db.customerDao().observeById(id)

    suspend fun getCustomer(id: Long) = db.customerDao().getById(id)

    suspend fun saveCustomer(customer: CustomerEntity): Long =
        if (customer.id == 0L) db.customerDao().insert(customer)
        else {
            db.customerDao().update(customer.copy(updatedAt = System.currentTimeMillis()))
            customer.id
        }

    // --------------------------------------------------------------- products

    fun observeProducts(businessId: Long) = db.productDao().observeAll(businessId)

    fun searchProducts(businessId: Long, query: String) = db.productDao().search(businessId, query)

    fun observeProduct(id: Long) = db.productDao().observeById(id)

    suspend fun getProduct(id: Long) = db.productDao().getById(id)

    suspend fun listActiveProducts(businessId: Long) = db.productDao().listActive(businessId)

    suspend fun saveProduct(product: ProductEntity, variants: List<ProductVariantEntity>): Long =
        db.withTransaction {
            val id = if (product.id == 0L) db.productDao().insert(product)
            else {
                db.productDao().update(product.copy(updatedAt = System.currentTimeMillis()))
                product.id
            }
            db.variantDao().deleteForProduct(id)
            if (variants.isNotEmpty()) {
                db.variantDao().insertAll(
                    variants.map {
                        it.copy(id = 0, businessId = product.businessId, productId = id)
                    }
                )
            }
            audit(product.businessId, "PRODUCT", id, if (product.id == 0L) "CREATE" else "UPDATE", product.name)
            id
        }

    fun observeVariants(businessId: Long, productId: Long) =
        db.variantDao().observeForProduct(businessId, productId)

    fun observeInventoryValue(businessId: Long) = db.productDao().observeInventoryValue(businessId)

    fun observeTotalUnits(businessId: Long) = db.productDao().observeTotalUnits(businessId)

    fun observeLowStock(businessId: Long) = db.productDao().observeLowStock(businessId)

    fun observeOutOfStock(businessId: Long) = db.productDao().observeOutOfStock(businessId)

    fun observeMovements(businessId: Long) = db.inventoryDao().observeRecent(businessId)

    fun observeMovementsForProduct(businessId: Long, productId: Long) =
        db.inventoryDao().observeForProduct(businessId, productId)

    suspend fun adjustStock(input: StockAdjustmentInput) = db.withTransaction {
        require(input.reason.isNotBlank()) { "A meaningful reason is required for stock adjustment." }
        val product = db.productDao().getById(input.productId) ?: return@withTransaction
        val delta = input.newStockQty - product.stockQty
        if (delta == 0L) return@withTransaction
        db.productDao().updateStock(input.productId, input.newStockQty)
        db.inventoryDao().insert(
            InventoryMovementEntity(
                businessId = input.businessId,
                productId = input.productId,
                type = "ADJUSTMENT",
                qty = delta,
                dateAt = System.currentTimeMillis(),
                reason = input.reason,
                notes = input.notes,
                balanceAfter = input.newStockQty
            )
        )
        audit(input.businessId, "PRODUCT", input.productId, "STOCK_ADJUST", "$delta (${input.reason})")
    }

    // --------------------------------------------------------------- suppliers

    fun observeSuppliers(businessId: Long) = db.supplierDao().observeAll(businessId)

    fun observeSuppliersWithAggregates(businessId: Long) = db.supplierDao().observeWithAggregates(businessId)

    fun observeSupplier(id: Long) = db.supplierDao().observeById(id)

    suspend fun listSuppliers(businessId: Long) = db.supplierDao().listAll(businessId)

    suspend fun saveSupplier(supplier: SupplierEntity): Long =
        if (supplier.id == 0L) db.supplierDao().insert(supplier)
        else {
            db.supplierDao().update(supplier.copy(updatedAt = System.currentTimeMillis()))
            supplier.id
        }

    // ---------------------------------------------------------------- purchase

    suspend fun createPurchase(input: NewPurchaseInput): Long = db.withTransaction {
        val total = input.lines.sumOf { it.qty * it.unitCostMinor }.coerceAtLeast(0)
        val purchaseId = db.purchaseDao().insert(
            PurchaseEntity(
                businessId = input.businessId,
                supplierId = input.supplierId,
                dateAt = input.dateAt,
                totalMinor = total,
                paidMinor = input.paidMinor.coerceAtMost(total),
                accountId = input.accountId,
                notes = input.notes,
                updatedAt = System.currentTimeMillis()
            )
        )
        db.purchaseDao().insertItems(
            input.lines.map {
                PurchaseItemEntity(
                    businessId = input.businessId,
                    purchaseId = purchaseId,
                    productId = it.productId,
                    qty = it.qty,
                    unitCostMinor = it.unitCostMinor,
                    lineTotalMinor = it.qty * it.unitCostMinor
                )
            }
        )
        input.lines.forEach { line ->
            db.productDao().adjustStock(line.productId, line.qty)
            val balance = db.productDao().getById(line.productId)?.stockQty ?: 0
            db.inventoryDao().insert(
                InventoryMovementEntity(
                    businessId = input.businessId,
                    productId = line.productId,
                    type = "PURCHASE",
                    qty = line.qty,
                    dateAt = input.dateAt,
                    refType = "PURCHASE",
                    refId = purchaseId,
                    reason = "Purchase stock in",
                    unitCostMinor = line.unitCostMinor,
                    balanceAfter = balance
                )
            )
        }
        if (input.paidMinor > 0) {
            input.accountId?.let { accountId ->
                db.ledgerDao().insert(
                    AccountTransactionEntity(
                        businessId = input.businessId,
                        accountId = accountId,
                        direction = "OUT",
                        amountMinor = input.paidMinor,
                        dateAt = input.dateAt,
                        category = "PURCHASE",
                        refType = "PURCHASE",
                        refId = purchaseId,
                        note = "Purchase payment"
                    )
                )
            }
        }
        if (input.paidMinor < total) {
            db.receivablePayableDao().insertPayable(
                com.hisabnikash.app.data.db.PayableEntity(
                    businessId = input.businessId,
                    supplierId = input.supplierId,
                    sourceType = "PURCHASE",
                    sourceId = purchaseId,
                    amountMinor = total,
                    paidMinor = input.paidMinor.coerceAtMost(total),
                    status = if (input.paidMinor > 0) "PARTIAL" else "PENDING",
                    note = "Purchase payable"
                )
            )
        }
        audit(input.businessId, "PURCHASE", purchaseId, "CREATE", "Purchase ${input.lines.sumOf { it.qty }} units")
        purchaseId
    }

    suspend fun getPurchaseWithItems(purchaseId: Long) = PurchaseDetail(
        purchase = db.purchaseDao().getById(purchaseId),
        items = db.purchaseDao().itemsFor(purchaseId)
    )

    fun observePurchases(businessId: Long) = db.purchaseDao().observeAll(businessId)

    fun observePurchase(id: Long) = db.purchaseDao().observeById(id)

    fun observePurchasesForSupplier(businessId: Long, supplierId: Long) =
        db.purchaseDao().observeForSupplier(businessId, supplierId)

    fun observePurchaseItemsForProduct(businessId: Long, productId: Long) =
        db.purchaseDao().observeForProduct(businessId, productId)

    // ------------------------------------------------------------------- misc

    private suspend fun audit(businessId: Long, type: String, id: Long?, action: String, text: String? = null) {
        db.auditDao().insert(
            com.hisabnikash.app.data.db.AuditEventEntity(
                businessId = businessId,
                entityType = type,
                entityId = id,
                action = action,
                detail = text
            )
        )
    }
}

data class PurchaseDetail(
    val purchase: PurchaseEntity?,
    val items: List<PurchaseItemEntity>
)
