package com.hisabnikash.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ---------------------------------------------------------------------------
// Workspace
// ---------------------------------------------------------------------------

@Entity(
    tableName = "businesses",
    indices = [Index("createdAt")]
)
data class BusinessEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val logoPath: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val currency: String = "BDT",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "business_settings",
    primaryKeys = ["businessId"],
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BusinessSettingsEntity(
    val businessId: Long,
    val firstName: String = "",
    val lastName: String = "",
    val invoicePrefix: String = "INV-",
    val invoiceNextNo: Long = 1,
    val invoiceFooter: String = "Thank you for your business.",
    val invoiceTerms: String = "Payment is due on the due date unless otherwise agreed.",
    val taxEnabled: Boolean = false,
    val taxRateBps: Int = 0, // basis points, e.g. 1500 = 15%
    val taxAppliedToDelivery: Boolean = false,
    val deliveryRefundPolicy: String = "merchant", // merchant | customer | policy | custom
    val deliveryRefundPercent: Int = 0,
    val lowStockThresholdDefault: Long = 5,
    val defaultCourierId: Long? = null,
    val defaultAccountId: Long? = null,
    val defaultChannelId: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "doc_sequences",
    primaryKeys = ["businessId", "docType"],
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId")]
)
data class DocSequenceEntity(
    val businessId: Long,
    val docType: String, // ORDER | INVOICE | RECEIPT | REFUND | RETURN | EXCHANGE
    val prefix: String,
    val nextNo: Long = 1
)

// ---------------------------------------------------------------------------
// Finance / accounts
// ---------------------------------------------------------------------------

@Entity(
    tableName = "accounts",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId"), Index("businessId", "type")]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val name: String,
    val type: String, // CASH | BANK | BKASH | NAGAD | ROCKET | CUSTOM
    val openingBalanceMinor: Long = 0,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "account_transactions",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("businessId"),
        Index("accountId"),
        Index("businessId", "dateAt"),
        Index("refType", "refId")
    ]
)
data class AccountTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val accountId: Long,
    val direction: String, // IN | OUT
    val amountMinor: Long,
    val dateAt: Long,
    val category: String,
    val refType: String, // PAYMENT | EXPENSE | TRANSFER_IN | TRANSFER_OUT | SETTLEMENT | REFUND | PURCHASE | OPENING | ADJUSTMENT
    val refId: Long? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transfers",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId"), Index("businessId", "dateAt")]
)
data class TransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val fromAccountId: Long,
    val toAccountId: Long,
    val amountMinor: Long,
    val dateAt: Long,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId"), Index("businessId", "dateAt"), Index("category")]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val category: String, // ADVERTISING | COURIER | PACKAGING | PURCHASES | RENT | SALARY | UTILITIES | SOFTWARE | OPERATIONS | OTHER
    val accountId: Long? = null,
    val amountMinor: Long,
    val dateAt: Long,
    val vendor: String? = null,
    val description: String? = null,
    val note: String? = null,
    val attachmentPath: String? = null,
    val recurring: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// ---------------------------------------------------------------------------
// Receivables / payables
// ---------------------------------------------------------------------------

@Entity(
    tableName = "receivables",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId"), Index("businessId", "customerId"), Index("status")]
)
data class ReceivableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val customerId: Long?,
    val sourceType: String, // COD | INVOICE | CUSTOM
    val sourceId: Long? = null,
    val amountMinor: Long,
    val paidMinor: Long = 0,
    val dueDateAt: Long? = null,
    val status: String, // PENDING | PARTIAL | PAID | OVERDUE
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "payables",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessId"), Index("businessId", "supplierId"), Index("status")]
)
data class PayableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val supplierId: Long?,
    val sourceType: String, // PURCHASE | EXPENSE | CUSTOM
    val sourceId: Long? = null,
    val amountMinor: Long,
    val paidMinor: Long = 0,
    val dueDateAt: Long? = null,
    val status: String, // PENDING | PARTIAL | PAID | OVERDUE
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
