package com.hisabnikash.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "campaigns",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId"), Index("businessId", "startAt")]
)
data class CampaignEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val platform: String, // META | GOOGLE | TIKTOK | YOUTUBE | OTHER
    val name: String,
    val spendMinor: Long,
    val startAt: Long,
    val endAt: Long? = null,
    val attributedOrders: Long = 0,
    val attributedRevenueMinor: Long = 0,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sales_channels",
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
data class SalesChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId"), Index("businessId", "category"), Index("businessId", "periodStart")]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val category: String,
    val periodStart: Long,
    val periodEnd: Long,
    val budgetMinor: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "notifications",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId"), Index("businessId", "createdAt"), Index("read")]
)
data class AppNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val category: String, // CRITICAL | IMPORTANT | INSIGHT
    val title: String,
    val body: String,
    val refType: String? = null,
    val refId: Long? = null,
    val read: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "audit_events",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId"), Index("businessId", "timestamp"), Index("entityType", "entityId")]
)
data class AuditEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val entityType: String,
    val entityId: Long? = null,
    val action: String,
    val detail: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customer_activities",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId"), Index("customerId"), Index("businessId", "timestamp")]
)
data class CustomerActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val customerId: Long,
    val type: String, // ORDER | PAYMENT | INVOICE | RECEIPT | REFUND | NOTE
    val refId: Long? = null,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
