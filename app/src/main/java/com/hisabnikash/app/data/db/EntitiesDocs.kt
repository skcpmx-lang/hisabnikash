package com.hisabnikash.app.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

// An invoice is a document only. It never creates revenue, payments or orders.
@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("businessId"),
        Index("invoiceNo"),
        Index("customerId"),
        Index("orderId"),
        Index("businessId", "dateAt"),
        Index("status")
    ]
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val invoiceNo: String,
    val customerId: Long?,
    val orderId: Long? = null,
    val dateAt: Long,
    val dueDateAt: Long? = null,
    val subtotalMinor: Long = 0,
    val discountMinor: Long = 0,
    val deliveryMinor: Long = 0,
    val taxMinor: Long = 0,
    val totalMinor: Long = 0,
    val advanceMinor: Long = 0,
    val paidMinor: Long = 0,
    val status: String, // DRAFT | ISSUED | PARTIALLY_PAID | PAID | OVERDUE | VOID
    val paymentMethod: String? = null,
    val note: String? = null,
    val terms: String? = null,
    val footer: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class InvoiceWithItems(
    @Embedded val invoice: InvoiceEntity,
    @Relation(parentColumn = "id", entityColumn = "invoiceId")
    val items: List<InvoiceItemEntity>
)

@Entity(
    tableName = "invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId"), Index("invoiceId")]
)
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val invoiceId: Long,
    val productId: Long? = null,
    val name: String,
    val sku: String? = null,
    val qty: Long,
    val unitPriceMinor: Long,
    val lineTotalMinor: Long
)

// A receipt is a document that references a payment. The payment row is the
// financial fact; creating a receipt without a payment is impossible.
@Entity(
    tableName = "receipts",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("businessId"),
        Index("receiptNo"),
        Index("customerId"),
        Index("orderId"),
        Index("invoiceId"),
        Index("businessId", "dateAt")
    ]
)
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val receiptNo: String,
    val dateAt: Long,
    val customerId: Long?,
    val orderId: Long? = null,
    val invoiceId: Long? = null,
    val accountId: Long?,
    val amountMinor: Long,
    val method: String,
    val reference: String? = null,
    val remainingMinor: Long = 0,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
