package com.hisabnikash.app.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "orders",
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
        Index("businessId", "orderDate"),
        Index("businessId", "status"),
        Index("customerId"),
        Index("channelId"),
        Index("courierId"),
        Index("orderNo")
    ]
)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val orderNo: String,
    val customerId: Long?,
    val channelId: Long? = null,
    val courierId: Long? = null,
    val status: String, // DRAFT | CONFIRMED | PROCESSING | PACKED | SHIPPED | DELIVERED | RETURNED | CANCELLED
    val orderDate: Long,
    val subtotalMinor: Long = 0,
    val discountMinor: Long = 0,
    val deliveryChargeMinor: Long = 0,
    val deliveryMode: String = "customer", // customer | merchant | included | prepaid | partial | custom
    val paymentMethod: String = "COD", // COD | ADVANCE | CARD | BANK | BKASH | NAGAD | CASH | OTHER
    val advanceMinor: Long = 0,
    val codMinor: Long = 0,
    val courierFeeMinor: Long = 0,
    val returnCourierFeeMinor: Long = 0,
    val packagingMinor: Long = 0,
    val advertisingMinor: Long = 0,
    val otherCostMinor: Long = 0,
    val trackingNo: String? = null,
    val note: String? = null,
    val tags: String? = null,
    val totalMinor: Long = 0,
    val cancellationReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class OrderWithItems(
    @Embedded val order: OrderEntity,
    @Relation(parentColumn = "id", entityColumn = "orderId")
    val items: List<OrderItemEntity>
)

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId"), Index("orderId"), Index("productId")]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val orderId: Long,
    val productId: Long?,
    val variantId: Long? = null,
    val name: String,
    val sku: String? = null,
    val qty: Long,
    val unitPriceMinor: Long,
    val unitCostMinor: Long,
    val discountMinor: Long = 0,
    val lineTotalMinor: Long
)

@Entity(
    tableName = "order_status_history",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId"), Index("orderId"), Index("businessId", "timestamp")]
)
data class OrderStatusHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val orderId: Long,
    val fromStatus: String?,
    val toStatus: String,
    val reason: String? = null,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "payments",
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
        Index("businessId", "dateAt"),
        Index("customerId"),
        Index("orderId"),
        Index("invoiceId"),
        Index("receivableId")
    ]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val customerId: Long?,
    val orderId: Long? = null,
    val invoiceId: Long? = null,
    val receivableId: Long? = null,
    val accountId: Long?,
    val amountMinor: Long,
    val method: String, // CASH | BKASH | NAGAD | BANK | CARD | COD | OTHER
    val direction: String, // IN | OUT
    val dateAt: Long,
    val reference: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "couriers",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId"), Index("businessId", "name")]
)
data class CourierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val name: String,
    val forwardFeeMinor: Long = 0,
    val returnFeeMinor: Long = 0,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "courier_settlements",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId"), Index("courierId"), Index("businessId", "dateAt")]
)
data class CourierSettlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val courierId: Long,
    val dateAt: Long,
    val orderId: Long? = null,
    val amountMinor: Long, // cash settled into account
    val feeMinor: Long = 0,
    val codMinor: Long = 0,
    val pendingMinor: Long = 0,
    val accountId: Long?,
    val trackingNo: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
