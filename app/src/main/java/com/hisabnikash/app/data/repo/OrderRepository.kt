package com.hisabnikash.app.data.repo

import androidx.room.withTransaction
import com.hisabnikash.app.data.db.AccountTransactionEntity
import com.hisabnikash.app.data.db.AppDatabase
import com.hisabnikash.app.data.db.AppNotificationEntity
import com.hisabnikash.app.data.db.AuditEventEntity
import com.hisabnikash.app.data.db.CourierSettlementEntity
import com.hisabnikash.app.data.db.CustomerActivityEntity
import com.hisabnikash.app.data.db.ExchangeEntity
import com.hisabnikash.app.data.db.ExchangeItemEntity
import com.hisabnikash.app.data.db.InventoryMovementEntity
import com.hisabnikash.app.data.db.InvoiceEntity
import com.hisabnikash.app.data.db.InvoiceItemEntity
import com.hisabnikash.app.data.db.OrderEntity
import com.hisabnikash.app.data.db.OrderItemEntity
import com.hisabnikash.app.data.db.OrderStatusHistoryEntity
import com.hisabnikash.app.data.db.PaymentEntity
import com.hisabnikash.app.data.db.ReceivableEntity
import com.hisabnikash.app.data.db.ReceiptEntity
import com.hisabnikash.app.data.db.RefundDocumentEntity
import com.hisabnikash.app.data.db.ReturnEntity
import com.hisabnikash.app.data.db.ReturnItemEntity
import com.hisabnikash.app.domain.model.percentBpsOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class OrderLine(
    val productId: Long?,
    val variantId: Long? = null,
    val name: String,
    val sku: String? = null,
    val qty: Long,
    val unitPriceMinor: Long,
    val unitCostMinor: Long,
    val discountMinor: Long = 0
)

data class NewOrderInput(
    val businessId: Long,
    val customerId: Long?,
    val channelId: Long? = null,
    val courierId: Long? = null,
    val status: String,
    val orderDate: Long = System.currentTimeMillis(),
    val lines: List<OrderLine>,
    val discountMinor: Long = 0,
    val deliveryChargeMinor: Long = 0,
    val deliveryMode: String = "customer",
    val paymentMethod: String = "COD",
    val advanceMinor: Long = 0,
    val courierFeeMinor: Long = 0,
    val returnCourierFeeMinor: Long = 0,
    val packagingMinor: Long = 0,
    val advertisingMinor: Long = 0,
    val otherCostMinor: Long = 0,
    val trackingNo: String? = null,
    val note: String? = null,
    val tags: String? = null
)

data class PaymentInput(
    val businessId: Long,
    val customerId: Long?,
    val orderId: Long? = null,
    val invoiceId: Long? = null,
    val receivableId: Long? = null,
    val accountId: Long?,
    val amountMinor: Long,
    val method: String,
    val dateAt: Long = System.currentTimeMillis(),
    val reference: String? = null,
    val note: String? = null
)

data class SettlementInput(
    val businessId: Long,
    val courierId: Long,
    val dateAt: Long = System.currentTimeMillis(),
    val orderId: Long? = null,
    val amountMinor: Long,
    val feeMinor: Long = 0,
    val codMinor: Long = 0,
    val accountId: Long?,
    val trackingNo: String? = null,
    val note: String? = null
)

data class ReturnInput(
    val businessId: Long,
    val orderId: Long,
    val customerId: Long?,
    val reason: String,
    val responsibility: String,
    val items: List<ReturnInputItem>,
    val refundProductMinor: Long = 0,
    val refundDeliveryMinor: Long = 0,
    val deliveryChargeRetainedMinor: Long = 0,
    val forwardCourierMinor: Long = 0,
    val returnCourierMinor: Long = 0,
    val refundMethod: String? = null,
    val refundAccountId: Long? = null,
    val note: String? = null,
    val refundNow: Boolean = false
)

data class ReturnInputItem(
    val productId: Long,
    val qty: Long,
    val restoreStock: Boolean = true,
    val unitRefundMinor: Long = 0
)

data class ExchangeInput(
    val businessId: Long,
    val orderId: Long,
    val items: List<ExchangeInputItem>,
    val priceDiffMinor: Long = 0,
    val extraPaidMinor: Long = 0,
    val refundMinor: Long = 0,
    val courierMinor: Long = 0,
    val note: String? = null
)

data class ExchangeInputItem(
    val originalProductId: Long,
    val originalQty: Long = 1,
    val replacementProductId: Long? = null,
    val replacementQty: Long = 1
)

data class InvoiceInput(
    val businessId: Long,
    val customerId: Long?,
    val orderId: Long? = null,
    val dateAt: Long = System.currentTimeMillis(),
    val dueDateAt: Long? = null,
    val lines: List<InvoiceLine>,
    val discountMinor: Long = 0,
    val deliveryMinor: Long = 0,
    val advanceMinor: Long = 0,
    val paymentMethod: String? = null,
    val note: String? = null,
    val terms: String? = null,
    val footer: String? = null,
    val issuedNow: Boolean = true
)

data class InvoiceLine(
    val productId: Long? = null,
    val name: String,
    val sku: String? = null,
    val qty: Long,
    val unitPriceMinor: Long
)

class OrderRepository(private val db: AppDatabase, private val workspace: WorkspaceRepository) {

    // ------------------------------------------------------------------ reads

    fun observeOrders(businessId: Long, status: String, query: String): Flow<List<OrderEntity>> =
        db.orderDao().observeFiltered(businessId, status, query)

    fun observeOrder(id: Long): Flow<OrderEntity?> = db.orderDao().observeById(id)

    fun observeOrderWithItems(id: Long) = db.orderDao().observeWithItems(id)

    suspend fun orderWithItems(id: Long) = db.orderDao().getWithItems(id)

    fun observeOrderItems(orderId: Long) = db.orderDao().observeItemsFor(orderId)

    fun observeStatusHistory(orderId: Long) = db.orderHistoryDao().observeForOrder(orderId)

    fun observeCountsByStatus(businessId: Long) = db.orderDao().observeCountsByStatus(businessId)

    fun observeCounts(businessId: Long) =
        db.orderDao().observeCountsByStatus(businessId)
            .map { counts -> counts.associate { it.status to it.count } }

    fun observeProcessing(businessId: Long) = db.orderDao().observeProcessing(businessId)

    fun observePendingCod(businessId: Long) = db.orderDao().observePendingCod(businessId)

    fun observePaymentsForOrder(businessId: Long, orderId: Long) =
        db.paymentDao().observeForOrder(businessId, orderId)

    // ---------------------------------------------------------------- create

    suspend fun createOrder(input: NewOrderInput): Long = db.withTransaction {
        requireOrderContext(input.businessId, input.customerId, input.channelId, input.courierId, input.lines.mapNotNull { it.productId })
        val subtotal = input.lines.sumOf { it.qty * it.unitPriceMinor - it.discountMinor }.coerceAtLeast(0)
        val total = (subtotal - input.discountMinor + input.deliveryChargeMinor).coerceAtLeast(0)
        val cod = if (input.paymentMethod == "COD") {
            (total - input.advanceMinor).coerceAtLeast(0)
        } else 0
        val advance = if (input.paymentMethod == "COD") input.advanceMinor.coerceAtMost(total) else total

        val orderNo = workspace.nextDocNumber(input.businessId, "ORDER")
        val orderId = db.orderDao().insert(
            OrderEntity(
                businessId = input.businessId,
                orderNo = orderNo,
                customerId = input.customerId,
                channelId = input.channelId,
                courierId = input.courierId,
                status = input.status,
                orderDate = input.orderDate,
                subtotalMinor = subtotal,
                discountMinor = input.discountMinor,
                deliveryChargeMinor = input.deliveryChargeMinor,
                deliveryMode = input.deliveryMode,
                paymentMethod = input.paymentMethod,
                advanceMinor = advance,
                codMinor = cod,
                courierFeeMinor = input.courierFeeMinor,
                returnCourierFeeMinor = input.returnCourierFeeMinor,
                packagingMinor = input.packagingMinor,
                advertisingMinor = input.advertisingMinor,
                otherCostMinor = input.otherCostMinor,
                trackingNo = input.trackingNo,
                note = input.note,
                tags = input.tags,
                totalMinor = total
            )
        )
        db.orderDao().insertItems(
            input.lines.map {
                OrderItemEntity(
                    businessId = input.businessId,
                    orderId = orderId,
                    productId = it.productId,
                    variantId = it.variantId,
                    name = it.name,
                    sku = it.sku,
                    qty = it.qty,
                    unitPriceMinor = it.unitPriceMinor,
                    unitCostMinor = it.unitCostMinor,
                    discountMinor = it.discountMinor,
                    lineTotalMinor = (it.qty * it.unitPriceMinor - it.discountMinor).coerceAtLeast(0)
                )
            }
        )
        db.orderHistoryDao().insert(
            OrderStatusHistoryEntity(
                businessId = input.businessId,
                orderId = orderId,
                fromStatus = null,
                toStatus = input.status
            )
        )
        if (input.status != "DRAFT") {
            reserveStock(input.businessId, orderId, input.lines, "SALE")
        }
        if (input.status == "DELIVERED") {
            ensureCodReceivable(input.businessId, orderId, cod)
        }
        input.customerId?.let { customerId ->
            db.activityDao().insert(
                CustomerActivityEntity(
                    businessId = input.businessId,
                    customerId = customerId,
                    type = "ORDER",
                    refId = orderId,
                    text = "Order $orderNo created (${input.status.lowercase()})",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        db.notificationDao().insert(
            AppNotificationEntity(
                businessId = input.businessId,
                category = "IMPORTANT",
                title = "New order $orderNo",
                body = "${totalLabel(total)} order created. New order recorded with status ${input.status}.",
                refType = "ORDER",
                refId = orderId
            )
        )
        audit(input.businessId, "ORDER", orderId, "CREATE", orderNo)
        orderId
    }

    suspend fun updateOrder(input: NewOrderInput, orderId: Long) = db.withTransaction {
        val existing = db.orderDao().getById(orderId) ?: return@withTransaction
        val subtotal = input.lines.sumOf { it.qty * it.unitPriceMinor - it.discountMinor }.coerceAtLeast(0)
        val total = (subtotal - input.discountMinor + input.deliveryChargeMinor).coerceAtLeast(0)
        val cod = if (input.paymentMethod == "COD") (total - input.advanceMinor).coerceAtLeast(0) else 0
        val advance = if (input.paymentMethod == "COD") input.advanceMinor.coerceAtMost(total) else total

        if (existing.status != "DRAFT") {
            // Restore previously reserved stock before re-reserving.
            releaseStock(input.businessId, orderId, "SALE_REVERSAL")
        }
        db.orderDao().update(
            existing.copy(
                customerId = input.customerId,
                channelId = input.channelId,
                courierId = input.courierId,
                status = input.status,
                orderDate = input.orderDate,
                subtotalMinor = subtotal,
                discountMinor = input.discountMinor,
                deliveryChargeMinor = input.deliveryChargeMinor,
                deliveryMode = input.deliveryMode,
                paymentMethod = input.paymentMethod,
                advanceMinor = advance,
                codMinor = cod,
                courierFeeMinor = input.courierFeeMinor,
                returnCourierFeeMinor = input.returnCourierFeeMinor,
                packagingMinor = input.packagingMinor,
                advertisingMinor = input.advertisingMinor,
                otherCostMinor = input.otherCostMinor,
                trackingNo = input.trackingNo,
                note = input.note,
                tags = input.tags,
                totalMinor = total,
                updatedAt = System.currentTimeMillis()
            )
        )
        db.orderDao().deleteItemsFor(orderId)
        db.orderDao().insertItems(
            input.lines.map {
                OrderItemEntity(
                    businessId = input.businessId,
                    orderId = orderId,
                    productId = it.productId,
                    variantId = it.variantId,
                    name = it.name,
                    sku = it.sku,
                    qty = it.qty,
                    unitPriceMinor = it.unitPriceMinor,
                    unitCostMinor = it.unitCostMinor,
                    discountMinor = it.discountMinor,
                    lineTotalMinor = (it.qty * it.unitPriceMinor - it.discountMinor).coerceAtLeast(0)
                )
            }
        )
        if (input.status != "DRAFT") {
            reserveStock(input.businessId, orderId, input.lines, "SALE")
        }
        if (input.status == "DELIVERED") ensureCodReceivable(input.businessId, orderId, cod)
        audit(input.businessId, "ORDER", orderId, "UPDATE", existing.orderNo)
    }

    // ---------------------------------------------------------------- status

    suspend fun updateStatus(orderId: Long, toStatus: String, reason: String? = null) = db.withTransaction {
        val existing = db.orderDao().getById(orderId) ?: return@withTransaction
        if (existing.status == toStatus) return@withTransaction

        if (toStatus == "CANCELLED") {
            releaseStock(existing.businessId, orderId, "SALE_REVERSAL")
            db.orderDao().update(
                existing.copy(
                    status = toStatus,
                    cancellationReason = reason,
                    codMinor = 0,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            db.orderDao().update(
                existing.copy(status = toStatus, updatedAt = System.currentTimeMillis())
            )
        }
        if (toStatus == "DELIVERED") {
            ensureCodReceivable(existing.businessId, orderId, existing.codMinor)
            db.notificationDao().insert(
                AppNotificationEntity(
                    businessId = existing.businessId,
                    category = "IMPORTANT",
                    title = "Order ${existing.orderNo} delivered",
                    body = if (existing.codMinor > 0)
                        "COD of ${formatMoney(existing.codMinor)} is now receivable."
                    else "Delivery recorded; revenue and profit updated.",
                    refType = "ORDER",
                    refId = orderId
                )
            )
            existing.customerId?.let { customerId ->
                db.activityDao().insert(
                    CustomerActivityEntity(
                        businessId = existing.businessId,
                        customerId = customerId,
                        type = "ORDER",
                        refId = orderId,
                        text = "Order ${existing.orderNo} delivered",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
        if (toStatus == "CANCELLED") {
            db.notificationDao().insert(
                AppNotificationEntity(
                    businessId = existing.businessId,
                    category = "IMPORTANT",
                    title = "Order ${existing.orderNo} cancelled",
                    body = reason?.let { "Reason: $it." } ?: "The order was cancelled and stock was restored.",
                    refType = "ORDER",
                    refId = orderId
                )
            )
        }
        db.orderHistoryDao().insert(
            OrderStatusHistoryEntity(
                businessId = existing.businessId,
                orderId = orderId,
                fromStatus = existing.status,
                toStatus = toStatus,
                reason = reason
            )
        )
        audit(existing.businessId, "ORDER", orderId, "STATUS:${existing.status}->$toStatus", existing.orderNo)
    }

    // -------------------------------------------------------------- payments

    /**
     * Records one payment and creates its receipt document in the same
     * transaction. There is no way to create a receipt without a payment, so
     * receipts can never double-count money.
     */
    suspend fun recordPayment(input: PaymentInput): ReceiptEntity = db.withTransaction {
        workspace.requireBusinessExists(input.businessId)
        input.customerId?.let { id ->
            val customer = db.customerDao().getById(id)
            if (customer != null && customer.businessId != input.businessId) {
                throw IllegalStateException(
                    "This payment's customer belongs to a different business. Switch business and try again."
                )
            }
        }
        input.accountId?.let { id ->
            val account = db.accountDao().getById(id)
            if (account != null && account.businessId != input.businessId) {
                throw IllegalStateException(
                    "This payment's account belongs to a different business. Switch business and try again."
                )
            }
        }
        val receiptNo = workspace.nextDocNumber(input.businessId, "RECEIPT")
        val paymentId = db.paymentDao().insert(
            PaymentEntity(
                businessId = input.businessId,
                customerId = input.customerId,
                orderId = input.orderId,
                invoiceId = input.invoiceId,
                receivableId = input.receivableId,
                accountId = input.accountId,
                amountMinor = input.amountMinor,
                method = input.method,
                direction = "IN",
                dateAt = input.dateAt,
                reference = input.reference,
                note = input.note
            )
        )
        input.accountId?.let { accountId ->
            db.ledgerDao().insert(
                AccountTransactionEntity(
                    businessId = input.businessId,
                    accountId = accountId,
                    direction = "IN",
                    amountMinor = input.amountMinor,
                    dateAt = input.dateAt,
                    category = if (input.method == "COD") "COD" else "PAYMENT",
                    refType = "PAYMENT",
                    refId = paymentId,
                    note = input.reference ?: "Payment recorded"
                )
            )
        }
        var remaining = 0L
        input.receivableId?.let { receivableId ->
            val receivable = db.receivablePayableDao().getReceivable(receivableId)
            if (receivable != null) {
                val newPaid = (receivable.paidMinor + input.amountMinor).coerceAtMost(receivable.amountMinor)
                remaining = (receivable.amountMinor - newPaid).coerceAtLeast(0)
                db.receivablePayableDao().updateReceivable(
                    receivable.copy(
                        paidMinor = newPaid,
                        status = when {
                            newPaid >= receivable.amountMinor -> "PAID"
                            newPaid > 0 -> "PARTIAL"
                            else -> receivable.status
                        },
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
        input.invoiceId?.let { invoiceId ->
            updateInvoiceAfterPayment(input.businessId, invoiceId, input.amountMinor)
        }
        var remainingOnInvoice = 0L
        input.invoiceId?.let { invoiceId ->
            val invoice = db.invoiceDao().getById(invoiceId)
            if (invoice != null && invoice.status != "DRAFT" && invoice.status != "VOID") {
                remainingOnInvoice = (invoice.totalMinor - invoice.advanceMinor - invoice.paidMinor).coerceAtLeast(0)
            }
        }
        val receipt = ReceiptEntity(
            businessId = input.businessId,
            receiptNo = receiptNo,
            dateAt = input.dateAt,
            customerId = input.customerId,
            orderId = input.orderId,
            invoiceId = input.invoiceId,
            accountId = input.accountId,
            amountMinor = input.amountMinor,
            method = input.method,
            reference = input.reference,
            remainingMinor = remaining.coerceAtLeast(remainingOnInvoice),
            note = input.note
        )
        val receiptId = db.receiptDao().insert(receipt)
        input.customerId?.let { customerId ->
            db.activityDao().insert(
                CustomerActivityEntity(
                    businessId = input.businessId,
                    customerId = customerId,
                    type = "PAYMENT",
                    refId = receiptId,
                    text = "Payment of ${formatMoney(input.amountMinor)} received ($receiptNo)",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        audit(input.businessId, "PAYMENT", paymentId, "CREATE", text = receiptNo)
        receipt.copy(id = receiptId)
    }

    /**
     * Records a supplier/other money-out payment against a payable.
     */
    suspend fun recordPayablePayment(
        businessId: Long,
        payableId: Long,
        accountId: Long?,
        amountMinor: Long,
        method: String,
        dateAt: Long = System.currentTimeMillis(),
        note: String? = null
    ) = db.withTransaction {
        val payable = db.receivablePayableDao().getPayable(payableId) ?: return@withTransaction
        val paymentId = db.paymentDao().insert(
            PaymentEntity(
                businessId = businessId,
                customerId = null,
                accountId = accountId,
                amountMinor = amountMinor,
                method = method,
                direction = "OUT",
                dateAt = dateAt,
                reference = "Payable #$payableId",
                note = note
            )
        )
        accountId?.let { accId ->
            db.ledgerDao().insert(
                AccountTransactionEntity(
                    businessId = businessId,
                    accountId = accId,
                    direction = "OUT",
                    amountMinor = amountMinor,
                    dateAt = dateAt,
                    category = "PAYABLE",
                    refType = "PAYMENT",
                    refId = paymentId,
                    note = note ?: "Payable payment"
                )
            )
        }
        val newPaid = (payable.paidMinor + amountMinor).coerceAtMost(payable.amountMinor)
        db.receivablePayableDao().updatePayable(
            payable.copy(
                paidMinor = newPaid,
                status = when {
                    newPaid >= payable.amountMinor -> "PAID"
                    newPaid > 0 -> "PARTIAL"
                    else -> payable.status
                },
                updatedAt = System.currentTimeMillis()
            )
        )
        audit(businessId, "PAYABLE", payableId, "PAY", text = "Paid ${formatMoney(amountMinor)}")
    }

    // ------------------------------------------------------------ settlements

    /** Records a courier settlement, posts cash and fees to the ledger and
     *  pays down the oldest COD receivables it covers. */
    suspend fun recordSettlement(input: SettlementInput): Long = db.withTransaction {
        val settlement = CourierSettlementEntity(
            businessId = input.businessId,
            courierId = input.courierId,
            dateAt = input.dateAt,
            orderId = input.orderId,
            amountMinor = input.amountMinor,
            feeMinor = input.feeMinor,
            codMinor = input.codMinor,
            pendingMinor = (input.codMinor - input.amountMinor - input.feeMinor).coerceAtLeast(0),
            accountId = input.accountId,
            trackingNo = input.trackingNo,
            note = input.note
        )
        val id = db.courierDao().insertSettlement(settlement)
        input.accountId?.let { accountId ->
            if (input.amountMinor > 0) {
                db.ledgerDao().insert(
                    AccountTransactionEntity(
                        businessId = input.businessId,
                        accountId = accountId,
                        direction = "IN",
                        amountMinor = input.amountMinor,
                        dateAt = input.dateAt,
                        category = "SETTLEMENT",
                        refType = "SETTLEMENT",
                        refId = id,
                        note = "Courier settlement received"
                    )
                )
            }
            if (input.feeMinor > 0) {
                db.ledgerDao().insert(
                    AccountTransactionEntity(
                        businessId = input.businessId,
                        accountId = accountId,
                        direction = "OUT",
                        amountMinor = input.feeMinor,
                        dateAt = input.dateAt,
                        category = "COURIER",
                        refType = "SETTLEMENT_FEE",
                        refId = id,
                        note = "Courier fees"
                    )
                )
            }
        }
        // Apply settled COD against oldest COD receivables.
        var toApply = input.codMinor
        if (toApply > 0) {
            val open = db.receivablePayableDao().openCodReceivables(input.businessId)
            for (r in open) {
                if (toApply <= 0) break
                val remaining = (r.amountMinor - r.paidMinor).coerceAtLeast(0)
                if (remaining <= 0) continue
                val apply = minOf(remaining, toApply)
                val newPaid = r.paidMinor + apply
                db.receivablePayableDao().updateReceivable(
                    r.copy(
                        paidMinor = newPaid,
                        status = if (newPaid >= r.amountMinor) "PAID" else "PARTIAL",
                        updatedAt = System.currentTimeMillis()
                    )
                )
                toApply -= apply
            }
        }
        audit(input.businessId, "SETTLEMENT", id, "CREATE", text = "Courier settlement ${formatMoney(input.amountMinor)}")
        id
    }

    // ----------------------------------------------------------------- returns

    suspend fun createReturn(input: ReturnInput): Long = db.withTransaction {
        val returnNo = workspace.nextDocNumber(input.businessId, "RETURN")
        val returnId = db.returnDao().insert(
            ReturnEntity(
                businessId = input.businessId,
                returnNo = returnNo,
                orderId = input.orderId,
                customerId = input.customerId,
                status = if (input.refundNow) "REFUNDED" else "RECEIVED",
                reason = input.reason,
                responsibility = input.responsibility,
                refundProductMinor = input.refundProductMinor,
                refundDeliveryMinor = input.refundDeliveryMinor,
                deliveryChargeRetainedMinor = input.deliveryChargeRetainedMinor,
                forwardCourierMinor = input.forwardCourierMinor,
                returnCourierMinor = input.returnCourierMinor,
                refundMethod = input.refundMethod,
                refundAccountId = input.refundAccountId,
                note = input.note
            )
        )
        db.returnDao().insertItems(
            input.items.map {
                ReturnItemEntity(
                    businessId = input.businessId,
                    returnId = returnId,
                    productId = it.productId,
                    qty = it.qty,
                    restoreStock = it.restoreStock,
                    unitRefundMinor = it.unitRefundMinor
                )
            }
        )
        input.items.forEach { item ->
            if (item.restoreStock) {
                db.productDao().adjustStock(item.productId, item.qty)
                val newBalance = (db.productDao().getById(item.productId)?.stockQty ?: 0)
                db.inventoryDao().insert(
                    InventoryMovementEntity(
                        businessId = input.businessId,
                        productId = item.productId,
                        type = "RETURN_IN",
                        qty = item.qty,
                        dateAt = System.currentTimeMillis(),
                        refType = "RETURN",
                        refId = returnId,
                        reason = input.reason,
                        balanceAfter = newBalance
                    )
                )
            }
        }
        db.orderDao().update(existingOrder(input.orderId).copy(status = "RETURNED", updatedAt = System.currentTimeMillis()))
        if (input.refundNow) {
            val totalRefund = input.refundProductMinor + input.refundDeliveryMinor
            if (totalRefund > 0) {
                createRefundInternal(
                    businessId = input.businessId,
                    customerId = input.customerId,
                    orderId = input.orderId,
                    returnId = returnId,
                    amountMinor = totalRefund,
                    method = input.refundMethod ?: "OTHER",
                    accountId = input.refundAccountId,
                    reason = input.reason,
                    note = input.note
                )
            }
        }
        audit(input.businessId, "RETURN", returnId, "CREATE", text = returnNo)
        returnId
    }

    suspend fun updateReturnStatus(returnId: Long, status: String) = db.withTransaction {
        val current = db.returnDao().getById(returnId) ?: return@withTransaction
        db.returnDao().update(current.copy(status = status, updatedAt = System.currentTimeMillis()))
        audit(current.businessId, "RETURN", returnId, "STATUS:$status", current.returnNo)
    }

    private suspend fun existingOrder(orderId: Long) = db.orderDao().getById(orderId)!!

    // --------------------------------------------------------------- exchange

    suspend fun createExchange(input: ExchangeInput): Long = db.withTransaction {
        val exchangeNo = workspace.nextDocNumber(input.businessId, "EXCHANGE")
        val exchangeId = db.exchangeDao().insert(
            ExchangeEntity(
                businessId = input.businessId,
                exchangeNo = exchangeNo,
                orderId = input.orderId,
                status = "COMPLETED",
                priceDiffMinor = input.priceDiffMinor,
                extraPaidMinor = input.extraPaidMinor,
                refundMinor = input.refundMinor,
                courierMinor = input.courierMinor,
                note = input.note
            )
        )
        db.exchangeDao().insertAllItems(
            input.items.map {
                ExchangeItemEntity(
                    businessId = input.businessId,
                    exchangeId = exchangeId,
                    originalProductId = it.originalProductId,
                    originalQty = it.originalQty,
                    replacementProductId = it.replacementProductId,
                    replacementQty = it.replacementQty
                )
            }
        )
        input.items.forEach { item ->
            db.productDao().adjustStock(item.originalProductId, -item.originalQty)
            db.inventoryDao().insert(
                InventoryMovementEntity(
                    businessId = input.businessId,
                    productId = item.originalProductId,
                    type = "EXCHANGE_OUT",
                    qty = -item.originalQty,
                    dateAt = System.currentTimeMillis(),
                    refType = "EXCHANGE",
                    refId = exchangeId,
                    reason = "Exchange $exchangeNo"
                )
            )
            item.replacementProductId?.let { replacement ->
                db.productDao().adjustStock(replacement, item.replacementQty)
                db.inventoryDao().insert(
                    InventoryMovementEntity(
                        businessId = input.businessId,
                        productId = replacement,
                        type = "EXCHANGE_IN",
                        qty = item.replacementQty,
                        dateAt = System.currentTimeMillis(),
                        refType = "EXCHANGE",
                        refId = exchangeId,
                        reason = "Exchange $exchangeNo"
                    )
                )
            }
        }
        if (input.extraPaidMinor > 0) {
            val account = db.accountDao().listAll(input.businessId).firstOrNull { it.type == "CASH" }
            db.paymentDao().insert(
                PaymentEntity(
                    businessId = input.businessId,
                    customerId = null,
                    orderId = input.orderId,
                    accountId = account?.id,
                    amountMinor = input.extraPaidMinor,
                    method = "CASH",
                    direction = "IN",
                    dateAt = System.currentTimeMillis(),
                    reference = exchangeNo
                )
            )
            account?.let {
                db.ledgerDao().insert(
                    AccountTransactionEntity(
                        businessId = input.businessId,
                        accountId = it.id,
                        direction = "IN",
                        amountMinor = input.extraPaidMinor,
                        dateAt = System.currentTimeMillis(),
                        category = "EXCHANGE",
                        refType = "EXCHANGE",
                        refId = exchangeId,
                        note = "Exchange price difference"
                    )
                )
            }
        }
        if (input.refundMinor > 0) {
            createRefundInternal(
                businessId = input.businessId,
                customerId = null,
                orderId = input.orderId,
                returnId = null,
                amountMinor = input.refundMinor,
                method = "CASH",
                accountId = null,
                reason = "Exchange refund",
                note = exchangeNo
            )
        }
        audit(input.businessId, "EXCHANGE", exchangeId, "CREATE", text = exchangeNo)
        exchangeId
    }

    // ----------------------------------------------------------------- refund

    suspend fun createRefund(
        businessId: Long,
        customerId: Long?,
        orderId: Long?,
        amountMinor: Long,
        method: String,
        accountId: Long?,
        reason: String?,
        note: String?
    ): Long = db.withTransaction {
        createRefundInternal(businessId, customerId, orderId, null, amountMinor, method, accountId, reason, note)
    }

    private suspend fun createRefundInternal(
        businessId: Long,
        customerId: Long?,
        orderId: Long?,
        returnId: Long?,
        amountMinor: Long,
        method: String,
        accountId: Long?,
        reason: String?,
        note: String?
    ): Long {
        val refundNo = workspace.nextDocNumber(businessId, "REFUND")
        val refundId = db.refundDao().insert(
            RefundDocumentEntity(
                businessId = businessId,
                refundNo = refundNo,
                dateAt = System.currentTimeMillis(),
                customerId = customerId,
                orderId = orderId,
                returnId = returnId,
                amountMinor = amountMinor,
                method = method,
                accountId = accountId,
                reason = reason,
                note = note
            )
        )
        accountId?.let {
            db.ledgerDao().insert(
                AccountTransactionEntity(
                    businessId = businessId,
                    accountId = it,
                    direction = "OUT",
                    amountMinor = amountMinor,
                    dateAt = System.currentTimeMillis(),
                    category = "REFUND",
                    refType = "REFUND",
                    refId = refundId,
                    note = reason ?: "Refund $refundNo"
                )
            )
        }
        customerId?.let {
            db.activityDao().insert(
                CustomerActivityEntity(
                    businessId = businessId,
                    customerId = it,
                    type = "REFUND",
                    refId = refundId,
                    text = "Refund $refundNo of ${formatMoney(amountMinor)}",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        audit(businessId, "REFUND", refundId, "CREATE", text = refundNo)
        return refundId
    }

    // ---------------------------------------------------------------- invoices

    suspend fun createInvoice(input: InvoiceInput): Long = db.withTransaction {
        val settings = db.workspaceDao().getSettings(input.businessId)
        val subtotal = input.lines.sumOf { it.qty * it.unitPriceMinor }.coerceAtLeast(0)
        val tax = if (settings?.taxEnabled == true && settings.taxRateBps > 0) {
            val taxable = if (settings.taxAppliedToDelivery) subtotal + input.deliveryMinor else subtotal
            taxable.percentBpsOf(settings.taxRateBps)
        } else 0
        val total = (subtotal - input.discountMinor + input.deliveryMinor + tax).coerceAtLeast(0)
        val invoiceNo = workspace.nextDocNumber(input.businessId, "INVOICE")
        val invoiceId = db.invoiceDao().insert(
            InvoiceEntity(
                businessId = input.businessId,
                invoiceNo = invoiceNo,
                customerId = input.customerId,
                orderId = input.orderId,
                dateAt = input.dateAt,
                dueDateAt = input.dueDateAt,
                subtotalMinor = subtotal,
                discountMinor = input.discountMinor,
                deliveryMinor = input.deliveryMinor,
                taxMinor = tax,
                totalMinor = total,
                advanceMinor = input.advanceMinor.coerceAtMost(total),
                paidMinor = input.advanceMinor.coerceAtMost(total),
                status = when {
                    !input.issuedNow -> "DRAFT"
                    input.advanceMinor >= total -> "PAID"
                    input.advanceMinor > 0 -> "PARTIALLY_PAID"
                    else -> "ISSUED"
                },
                paymentMethod = input.paymentMethod,
                note = input.note,
                terms = input.terms ?: settings?.invoiceTerms,
                footer = input.footer ?: settings?.invoiceFooter,
                updatedAt = System.currentTimeMillis()
            )
        )
        db.invoiceDao().insertItems(
            input.lines.map {
                InvoiceItemEntity(
                    businessId = input.businessId,
                    invoiceId = invoiceId,
                    productId = it.productId,
                    name = it.name,
                    sku = it.sku,
                    qty = it.qty,
                    unitPriceMinor = it.unitPriceMinor,
                    lineTotalMinor = it.qty * it.unitPriceMinor
                )
            }
        )
        // An invoice is a document. It never creates revenue, payments or
        // ledger entries unless an advance is explicitly received.
        if (input.advanceMinor > 0) {
            recordPayment(
                PaymentInput(
                    businessId = input.businessId,
                    customerId = input.customerId,
                    invoiceId = invoiceId,
                    accountId = settings?.defaultAccountId,
                    amountMinor = input.advanceMinor,
                    method = input.paymentMethod ?: "OTHER",
                    dateAt = input.dateAt,
                    reference = "Advance on $invoiceNo"
                )
            )
        }
        input.customerId?.let { customerId ->
            db.activityDao().insert(
                CustomerActivityEntity(
                    businessId = input.businessId,
                    customerId = customerId,
                    type = "INVOICE",
                    refId = invoiceId,
                    text = "Invoice $invoiceNo issued",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        audit(input.businessId, "INVOICE", invoiceId, "CREATE", text = invoiceNo)
        invoiceId
    }

    suspend fun updateInvoiceStatus(invoiceId: Long, status: String) = db.withTransaction {
        val invoice = db.invoiceDao().getById(invoiceId) ?: return@withTransaction
        db.invoiceDao().update(invoice.copy(status = status, updatedAt = System.currentTimeMillis()))
        if (status == "VOID") {
            audit(invoice.businessId, "INVOICE", invoiceId, "VOID", invoice.invoiceNo)
        } else {
            audit(invoice.businessId, "INVOICE", invoiceId, "STATUS:$status", invoice.invoiceNo)
        }
    }

    private suspend fun updateInvoiceAfterPayment(businessId: Long, invoiceId: Long, amount: Long) {
        val invoice = db.invoiceDao().getById(invoiceId) ?: return
        if (invoice.status == "DRAFT" || invoice.status == "VOID") return
        val newPaid = (invoice.paidMinor + amount).coerceAtMost(invoice.totalMinor)
        val status = when {
            newPaid >= invoice.totalMinor -> "PAID"
            newPaid > invoice.advanceMinor -> "PARTIALLY_PAID"
            else -> invoice.status
        }
        db.invoiceDao().update(
            invoice.copy(paidMinor = newPaid, status = status, updatedAt = System.currentTimeMillis())
        )
    }

    // ------------------------------------------------------------- stock util

    private suspend fun reserveStock(businessId: Long, orderId: Long, lines: List<OrderLine>, type: String) {
        lines.forEach { line ->
            if (line.productId != null && line.qty > 0) {
                db.productDao().adjustStock(line.productId, -line.qty)
                val balance = db.productDao().getById(line.productId)?.stockQty ?: 0
                db.inventoryDao().insert(
                    InventoryMovementEntity(
                        businessId = businessId,
                        productId = line.productId,
                        type = type,
                        qty = -line.qty,
                        dateAt = System.currentTimeMillis(),
                        refType = "ORDER",
                        refId = orderId,
                        reason = "Order reservation",
                        balanceAfter = balance
                    )
                )
            }
        }
    }

    private suspend fun releaseStock(businessId: Long, orderId: Long, type: String) {
        val items = db.orderDao().itemsFor(orderId)
        items.forEach { item ->
            if (item.productId != null && item.qty > 0) {
                db.productDao().adjustStock(item.productId, item.qty)
                db.inventoryDao().insert(
                    InventoryMovementEntity(
                        businessId = businessId,
                        productId = item.productId,
                        type = type,
                        qty = item.qty,
                        dateAt = System.currentTimeMillis(),
                        refType = "ORDER",
                        refId = orderId,
                        reason = "Order release"
                    )
                )
            }
        }
    }

    private suspend fun ensureCodReceivable(businessId: Long, orderId: Long, codMinor: Long) {
        if (codMinor <= 0) return
        val existing = db.receivablePayableDao().findReceivable(businessId, "COD", orderId)
        if (existing == null) {
            val order = db.orderDao().getById(orderId)
            db.receivablePayableDao().insertReceivable(
                ReceivableEntity(
                    businessId = businessId,
                    customerId = order?.customerId,
                    sourceType = "COD",
                    sourceId = orderId,
                    amountMinor = codMinor,
                    paidMinor = 0,
                    status = "PENDING",
                    note = "COD for ${order?.orderNo}"
                )
            )
        }
    }

    private suspend fun audit(businessId: Long, type: String, id: Long?, action: String, text: String? = null) {
        db.auditDao().insert(
            AuditEventEntity(
                businessId = businessId,
                entityType = type,
                entityId = id,
                action = action,
                detail = text
            )
        )
    }

    private fun totalLabel(minor: Long): String = formatMoney(minor)

    /**
     * Cross-business guard for order creation: every referenced parent row
     * must belong to the same business as the order itself.
     */
    private suspend fun requireOrderContext(
        businessId: Long,
        customerId: Long?,
        channelId: Long?,
        courierId: Long?,
        productIds: List<Long>
    ) {
        workspace.requireBusinessExists(businessId)
        customerId?.let { id ->
            val customer = db.customerDao().getById(id)
            if (customer != null && customer.businessId != businessId) {
                throw IllegalStateException(
                    "The selected customer belongs to a different business. Switch business and try again."
                )
            }
        }
        channelId?.let { id ->
            val channels = db.channelDao().listAll(businessId)
            if (channels.none { it.id == id }) {
                throw IllegalStateException(
                    "The selected sales channel no longer exists or belongs to a different business. Refresh and try again."
                )
            }
        }
        courierId?.let { id ->
            val courier = db.courierDao().getById(id)
            if (courier != null && courier.businessId != businessId) {
                throw IllegalStateException(
                    "The selected courier belongs to a different business. Switch business and try again."
                )
            }
        }
        productIds.forEach { id ->
            val product = db.productDao().getById(id)
            if (product != null && product.businessId != businessId) {
                throw IllegalStateException(
                    "A product on this order belongs to a different business. Switch business and try again."
                )
            }
        }
    }

}

private fun formatMoney(minor: Long): String = com.hisabnikash.app.domain.model.formatMoney(minor)
