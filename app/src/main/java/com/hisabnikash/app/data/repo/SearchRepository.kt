package com.hisabnikash.app.data.repo

import com.hisabnikash.app.data.db.AppDatabase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class SearchSection(
    val title: String,
    val items: List<SearchResult>
)

data class SearchResult(
    val key: String,
    val title: String,
    val subtitle: String? = null,
    val trailing: String? = null,
    val route: String,
    val routeArg: String? = null
)

/**
 * Indexed local search across orders, products, customers, suppliers,
 * expenses, transactions, invoices, receipts and couriers.
 */
class SearchRepository(private val db: AppDatabase) {

    suspend fun search(businessId: Long, rawQuery: String): List<SearchSection> = coroutineScope {
        val q = rawQuery.trim()
        if (q.isEmpty()) return@coroutineScope emptyList()
        val lower = q.lowercase()

        // Smart queries first.
        if (lower == "today" || lower.contains("orders today")) {
            val today = PeriodControl.resolve("1D")
            val orders = db.orderDao().allInRange(businessId, today.fromAt, today.toAt).take(20)
            return@coroutineScope listOf(
                SearchSection("Today", orders.map { it.toResult() })
            )
        }
        if (lower.contains("low stock")) {
            val items = db.productDao().listLowStock(businessId)
            return@coroutineScope listOf(
                SearchSection("Low stock", items.map {
                    SearchResult("product-${it.id}", it.name, "SKU ${it.sku ?: "—"}",
                        "${it.stockQty} left", "product/${it.id}", it.id.toString())
                })
            )
        }
        if (lower.contains("pending cod")) {
            val items = db.orderDao().listPendingCod(businessId)
            return@coroutineScope listOf(
                SearchSection("Pending COD", items.map { it.toResult() })
            )
        }
        if (lower.contains("unpaid")) {
            val items = db.receivablePayableDao().openReceivables(businessId)
            return@coroutineScope listOf(
                SearchSection("Unpaid customers / receivables", items.map {
                    SearchResult("receivable-${it.id}", "Receivable #${it.id}",
                        it.note, com.hisabnikash.app.domain.model.formatMoney(it.amountMinor - it.paidMinor),
                        "receivables", it.id.toString())
                })
            )
        }

        val customersDeferred = async { db.customerDao().searchOnce(businessId, q) }
        val productsDeferred = async { db.productDao().searchOnce(businessId, q) }
        val suppliersDeferred = async { db.supplierDao().search(businessId, q) }
        val ordersDeferred = async { db.orderDao().searchByNumber(businessId, q) }
        val expensesDeferred = async { db.expenseDao().search(businessId, q) }
        val transactionsDeferred = async { db.ledgerDao().search(businessId, q) }
        val invoicesDeferred = async { db.invoiceDao().search(businessId, q) }
        val receiptsDeferred = async { db.receiptDao().search(businessId, q) }

        val sections = mutableListOf<SearchSection>()

        val customers = customersDeferred.await()
        if (customers.isNotEmpty()) sections += SearchSection("Customers", customers.map {
            SearchResult("customer-${it.customer.id}", it.customer.name,
                it.customer.phone ?: it.customer.email, null, "customer/${it.customer.id}", it.customer.id.toString())
        })
        val products = productsDeferred.await()
        if (products.isNotEmpty()) sections += SearchSection("Products", products.map {
            SearchResult("product-${it.product.id}", it.product.name,
                "SKU ${it.product.sku ?: "—"}", null, "product/${it.product.id}", it.product.id.toString())
        })
        val suppliers = suppliersDeferred.await()
        if (suppliers.isNotEmpty()) sections += SearchSection("Suppliers", suppliers.map {
            SearchResult("supplier-${it.id}", it.name, it.phone, null, "supplier/${it.id}", it.id.toString())
        })
        val orders = ordersDeferred.await()
        if (orders.isNotEmpty()) sections += SearchSection("Orders", orders.map { it.toResult() })
        val expenses = expensesDeferred.await()
        if (expenses.isNotEmpty()) sections += SearchSection("Expenses", expenses.map {
            SearchResult("expense-${it.id}", it.description ?: it.category,
                it.vendor, com.hisabnikash.app.domain.model.formatMoney(it.amountMinor), "expenses", null)
        })
        val transactions = transactionsDeferred.await()
        if (transactions.isNotEmpty()) sections += SearchSection("Transactions", transactions.map {
            SearchResult("tx-${it.id}", it.note ?: it.category,
                it.direction, com.hisabnikash.app.domain.model.formatMoney(it.amountMinor), "transactions", null)
        })
        val invoices = invoicesDeferred.await()
        if (invoices.isNotEmpty()) sections += SearchSection("Invoices", invoices.map {
            SearchResult("invoice-${it.id}", it.invoiceNo, "Status ${it.status}",
                com.hisabnikash.app.domain.model.formatMoney(it.totalMinor), "invoice/${it.id}", it.id.toString())
        })
        val receipts = receiptsDeferred.await()
        if (receipts.isNotEmpty()) sections += SearchSection("Receipts", receipts.map {
            SearchResult("receipt-${it.id}", it.receiptNo, it.method,
                com.hisabnikash.app.domain.model.formatMoney(it.amountMinor), "receipts", null)
        })
        sections
    }

    private fun com.hisabnikash.app.data.db.OrderEntity.toResult() = SearchResult(
        key = "order-$id",
        title = orderNo,
        subtitle = "Status ${status}",
        trailing = com.hisabnikash.app.domain.model.formatMoney(totalMinor),
        route = "order/$id",
        routeArg = id.toString()
    )
}
