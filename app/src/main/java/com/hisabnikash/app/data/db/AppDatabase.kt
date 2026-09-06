package com.hisabnikash.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        BusinessEntity::class,
        BusinessSettingsEntity::class,
        DocSequenceEntity::class,
        CustomerEntity::class,
        ProductEntity::class,
        ProductVariantEntity::class,
        SupplierEntity::class,
        PurchaseEntity::class,
        PurchaseItemEntity::class,
        InventoryMovementEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        OrderStatusHistoryEntity::class,
        PaymentEntity::class,
        AccountEntity::class,
        AccountTransactionEntity::class,
        TransferEntity::class,
        ExpenseEntity::class,
        ReceivableEntity::class,
        PayableEntity::class,
        CourierEntity::class,
        CourierSettlementEntity::class,
        ReturnEntity::class,
        ReturnItemEntity::class,
        ExchangeEntity::class,
        ExchangeItemEntity::class,
        RefundDocumentEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        ReceiptEntity::class,
        CampaignEntity::class,
        SalesChannelEntity::class,
        BudgetEntity::class,
        AppNotificationEntity::class,
        AuditEventEntity::class,
        CustomerActivityEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun customerDao(): CustomerDao
    abstract fun productDao(): ProductDao
    abstract fun variantDao(): VariantDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun supplierDao(): SupplierDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun orderDao(): OrderDao
    abstract fun orderHistoryDao(): OrderHistoryDao
    abstract fun paymentDao(): PaymentDao
    abstract fun accountDao(): AccountDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun transferDao(): TransferDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun receivablePayableDao(): ReceivablePayableDao
    abstract fun courierDao(): CourierDao
    abstract fun returnDao(): ReturnDao
    abstract fun exchangeDao(): ExchangeDao
    abstract fun refundDao(): RefundDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun campaignDao(): CampaignDao
    abstract fun channelDao(): ChannelDao
    abstract fun budgetDao(): BudgetDao
    abstract fun notificationDao(): NotificationDao
    abstract fun auditDao(): AuditDao
    abstract fun activityDao(): ActivityDao
}
