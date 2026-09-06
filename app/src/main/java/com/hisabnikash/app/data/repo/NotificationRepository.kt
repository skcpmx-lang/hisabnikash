package com.hisabnikash.app.data.repo

import com.hisabnikash.app.data.db.AppDatabase
import com.hisabnikash.app.data.db.AppNotificationEntity
import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val db: AppDatabase) {

    fun observeNotifications(businessId: Long, filter: String) =
        db.notificationDao().observeFiltered(businessId, filter)

    fun observeUnreadCount(businessId: Long): Flow<Long> =
        db.notificationDao().observeUnreadCount(businessId)

    suspend fun markRead(businessId: Long, id: Long, read: Boolean) =
        db.notificationDao().setRead(businessId, id, read)

    suspend fun markAllRead(businessId: Long) = db.notificationDao().markAllRead(businessId)

    suspend fun insert(notification: AppNotificationEntity) =
        db.notificationDao().insert(notification)

    /**
     * Re-scans business state and inserts useful, non-spammy alerts. Existing
     * unread notifications with the same title are not duplicated.
     */
    suspend fun generateInsights(businessId: Long) {
        val now = System.currentTimeMillis()
        val low = db.productDao().listLowStock(businessId)
        val out = db.productDao().listOutOfStock(businessId)
        val processing = db.orderDao().listProcessing(businessId)
        val pendingCod = db.orderDao().listPendingCod(businessId)
        val returns = db.returnDao().listPending(businessId)
        val settlements = db.courierDao().listPending(businessId)
        val invoices = db.invoiceDao().listOverdue(businessId, now)

        low.take(3).forEach { p ->
            notifyIfNew(
                businessId, "WARNING", "Low stock: ${p.name}",
                "${p.stockQty} units left (threshold ${p.lowStockThreshold}). Restock soon.",
                "PRODUCT", p.id
            )
        }
        out.take(3).forEach { p ->
            notifyIfNew(
                businessId, "CRITICAL", "Out of stock: ${p.name}",
                "This product is sold out. Update stock or create a purchase.",
                "PRODUCT", p.id
            )
        }
        val pendingCount = processing.size
        if (pendingCount > 0) {
            notifyIfNew(
                businessId, "IMPORTANT", "$pendingCount orders need processing",
                "Orders waiting in Confirmed, Processing, Packed or Shipped status.",
                "ORDER", null
            )
        }
        val codPending = pendingCod.sumOf { it.codMinor }
        if (pendingCod.isNotEmpty()) {
            notifyIfNew(
                businessId, "IMPORTANT", "${pendingCod.size} orders carry pending COD",
                "Total ${formatMoney(codPending)} waiting for courier settlement or payment.",
                "ORDER", null
            )
        }
        if (returns.isNotEmpty()) {
            notifyIfNew(
                businessId, "IMPORTANT", "${returns.size} returns need attention",
                "Check the returns list and record refunds or restocking.",
                "RETURN", null
            )
        }
        val outstandingCount = settlements.count { it.pendingMinor > 0 }
        if (outstandingCount > 0) {
            notifyIfNew(
                businessId, "IMPORTANT", "$outstandingCount settlements still pending",
                "Record courier settlements to keep cash and COD accurate.",
                "SETTLEMENT", null
            )
        }
        invoices.forEach { inv ->
            notifyIfNew(
                businessId, "CRITICAL", "Invoice ${inv.invoiceNo} overdue",
                "Due ${formatMoney(inv.totalMinor - inv.paidMinor - inv.advanceMinor)}. Follow up with the customer.",
                "INVOICE", inv.id
            )
        }
    }

    private suspend fun notifyIfNew(
        businessId: Long,
        category: String,
        title: String,
        body: String,
        refType: String,
        refId: Long?
    ) {
        val existing = db.notificationDao().findOpen(businessId, title, System.currentTimeMillis() - 86_400_000L)
        if (existing == null) {
            db.notificationDao().insert(
                AppNotificationEntity(
                    businessId = businessId,
                    category = category,
                    title = title,
                    body = body,
                    refType = refType,
                    refId = refId
                )
            )
        }
    }

    private fun formatMoney(minor: Long): String = com.hisabnikash.app.domain.model.formatMoney(minor)
}
