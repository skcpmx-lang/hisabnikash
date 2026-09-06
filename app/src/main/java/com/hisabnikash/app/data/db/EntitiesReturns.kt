package com.hisabnikash.app.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "returns",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId"), Index("orderId"), Index("businessId", "createdAt")]
)
data class ReturnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val returnNo: String,
    val orderId: Long,
    val customerId: Long?,
    val status: String, // REQUESTED | APPROVED | PICKUP | RECEIVED | REFUND_PENDING | REFUNDED | REJECTED | COMPLETED
    val reason: String,
    val responsibility: String, // customer | merchant | policy | custom
    val refundProductMinor: Long = 0,
    val refundDeliveryMinor: Long = 0,
    val deliveryChargeRetainedMinor: Long = 0,
    val forwardCourierMinor: Long = 0,
    val returnCourierMinor: Long = 0,
    val refundMethod: String? = null,
    val refundAccountId: Long? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ReturnWithItems(
    @Embedded val returnOrder: ReturnEntity,
    @Relation(parentColumn = "id", entityColumn = "returnId")
    val items: List<ReturnItemEntity>
)

@Entity(
    tableName = "return_items",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ReturnEntity::class,
            parentColumns = ["id"],
            childColumns = ["returnId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId"), Index("returnId"), Index("productId")]
)
data class ReturnItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val returnId: Long,
    val productId: Long,
    val qty: Long,
    val restoreStock: Boolean = true,
    val unitRefundMinor: Long = 0
)

@Entity(
    tableName = "exchanges",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId"), Index("orderId"), Index("businessId", "createdAt")]
)
data class ExchangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val exchangeNo: String,
    val orderId: Long,
    val status: String, // REQUESTED | APPROVED | SHIPPED | COMPLETED | REJECTED
    val priceDiffMinor: Long = 0,
    val extraPaidMinor: Long = 0,
    val refundMinor: Long = 0,
    val deliveryDiffMinor: Long = 0,
    val courierMinor: Long = 0,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "exchange_items",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExchangeEntity::class,
            parentColumns = ["id"],
            childColumns = ["exchangeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId"), Index("exchangeId"), Index("originalProductId"), Index("replacementProductId")]
)
data class ExchangeItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val exchangeId: Long,
    val originalProductId: Long,
    val originalQty: Long = 1,
    val replacementProductId: Long? = null,
    val replacementQty: Long = 1
)

// RefundDocument is both the document and the financial fact. The linked
// ledger transaction (OUT) is created in the same database transaction.
@Entity(
    tableName = "refund_documents",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId"), Index("orderId"), Index("customerId"), Index("businessId", "dateAt")]
)
data class RefundDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val refundNo: String,
    val dateAt: Long,
    val customerId: Long?,
    val orderId: Long? = null,
    val returnId: Long? = null,
    val amountMinor: Long,
    val method: String,
    val accountId: Long?,
    val reason: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
