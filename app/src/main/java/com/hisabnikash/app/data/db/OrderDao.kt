package com.hisabnikash.app.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Insert
    suspend fun insert(order: OrderEntity): Long

    @Update
    suspend fun update(order: OrderEntity)

    @Insert
    suspend fun insertItems(items: List<OrderItemEntity>)

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteItemsFor(orderId: Long)

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getById(id: Long): OrderEntity?

    @Query("SELECT * FROM orders WHERE id = :id")
    fun observeById(id: Long): Flow<OrderEntity?>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId ORDER BY id")
    suspend fun itemsFor(orderId: Long): List<OrderItemEntity>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId ORDER BY id")
    fun observeItemsFor(orderId: Long): Flow<List<OrderItemEntity>>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getWithItems(id: Long): OrderWithItems?

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :id")
    fun observeWithItems(id: Long): Flow<OrderWithItems?>

    @Query(
        """
        SELECT * FROM orders
        WHERE businessId = :businessId
          AND (:status = 'ALL' OR status = :status)
          AND (:query = ''
               OR orderNo LIKE '%' || :query || '%' COLLATE NOCASE
               OR EXISTS(SELECT 1 FROM order_items oi
                         WHERE oi.orderId = orders.id AND oi.name LIKE '%' || :query || '%' COLLATE NOCASE))
        ORDER BY orderDate DESC, id DESC
        LIMIT 1000
        """
    )
    fun observeFiltered(businessId: Long, status: String, query: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE businessId = :businessId ORDER BY orderDate DESC, id DESC LIMIT 1000")
    fun observeAll(businessId: Long): Flow<List<OrderEntity>>

    @Query(
        """
        SELECT status, COUNT(*) AS count FROM orders
        WHERE businessId = :businessId GROUP BY status
        """
    )
    fun observeCountsByStatus(businessId: Long): Flow<List<StatusCount>>

    data class StatusCount(val status: String, val count: Long)

    @Query(
        """
        SELECT * FROM orders WHERE businessId = :businessId
        AND orderDate BETWEEN :fromAt AND :toAt AND status IN ('CONFIRMED','PROCESSING','PACKED','SHIPPED','DELIVERED','RETURNED')
        ORDER BY orderDate ASC
        """
    )
    suspend fun inRange(businessId: Long, fromAt: Long, toAt: Long): List<OrderEntity>

    @Query(
        """
        SELECT * FROM orders WHERE businessId = :businessId
        AND orderDate BETWEEN :fromAt AND :toAt AND status = 'DELIVERED'
        ORDER BY orderDate ASC
        """
    )
    fun observeDeliveredInRange(businessId: Long, fromAt: Long, toAt: Long): Flow<List<OrderEntity>>

    @Query(
        """
        SELECT * FROM orders WHERE businessId = :businessId
        AND orderDate BETWEEN :fromAt AND :toAt
        ORDER BY orderDate ASC
        """
    )
    suspend fun allInRange(businessId: Long, fromAt: Long, toAt: Long): List<OrderEntity>

    @Query(
        """
        SELECT * FROM orders WHERE businessId = :businessId
        AND orderDate BETWEEN :fromAt AND :toAt
        ORDER BY orderDate ASC
        """
    )
    fun observeAllInRange(businessId: Long, fromAt: Long, toAt: Long): Flow<List<OrderEntity>>

    @Query("SELECT COUNT(*) FROM orders WHERE businessId = :businessId")
    fun observeCount(businessId: Long): Flow<Long>

    @Query(
        """
        SELECT COALESCE(SUM(oi.qty * oi.unitCostMinor), 0) FROM order_items oi
        JOIN orders o ON o.id = oi.orderId
        WHERE o.businessId = :businessId AND o.status = 'DELIVERED'
          AND o.orderDate BETWEEN :fromAt AND :toAt
        """
    )
    suspend fun cogsInRange(businessId: Long, fromAt: Long, toAt: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(oi.qty * oi.unitCostMinor), 0) FROM order_items oi
        JOIN orders o ON o.id = oi.orderId
        WHERE o.businessId = :businessId AND o.status = 'DELIVERED'
          AND o.orderDate BETWEEN :fromAt AND :toAt
        """
    )
    fun observeCogsInRange(businessId: Long, fromAt: Long, toAt: Long): Flow<Long>

    @Query(
        """
        SELECT COALESCE(SUM(oi.qty), 0) FROM order_items oi
        JOIN orders o ON o.id = oi.orderId
        WHERE o.businessId = :businessId AND o.status = 'DELIVERED'
          AND o.orderDate BETWEEN :fromAt AND :toAt
        """
    )
    suspend fun unitsInRange(businessId: Long, fromAt: Long, toAt: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(oi.qty), 0) FROM order_items oi
        JOIN orders o ON o.id = oi.orderId
        WHERE o.businessId = :businessId AND o.status = 'DELIVERED'
          AND o.orderDate BETWEEN :fromAt AND :toAt
        """
    )
    fun observeUnitsInRange(businessId: Long, fromAt: Long, toAt: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM orders WHERE businessId = :businessId AND status = :status AND orderDate BETWEEN :fromAt AND :toAt")
    suspend fun countWithStatusInRange(businessId: Long, status: String, fromAt: Long, toAt: Long): Long

    @Query(
        """
        SELECT c.name AS name, COUNT(o.id) AS orderCount,
               COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN o.totalMinor ELSE 0 END), 0) AS revenueMinor,
               COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN o.subtotalMinor - o.discountMinor ELSE 0 END), 0) AS productOnlyMinor,
               COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN (o.subtotalMinor - o.discountMinor) - o.courierFeeMinor - o.packagingMinor - o.advertisingMinor - o.otherCostMinor - o.returnCourierFeeMinor ELSE 0 END), 0) AS contributionMinor
        FROM orders o
        LEFT JOIN sales_channels c ON c.id = o.channelId
        WHERE o.businessId = :businessId AND o.orderDate BETWEEN :fromAt AND :toAt
        GROUP BY o.channelId
        ORDER BY revenueMinor DESC
        """
    )
    suspend fun byChannel(businessId: Long, fromAt: Long, toAt: Long): List<ChannelMetric>

    data class ChannelMetric(
        val name: String?,
        val orderCount: Long,
        val revenueMinor: Long,
        val productOnlyMinor: Long,
        val contributionMinor: Long
    )

    @Query(
        """
        SELECT p.id AS productId, p.name AS name, p.sku AS sku, p.category AS category,
               p.sellingPriceMinor AS sellingPriceMinor, p.purchaseCostMinor AS purchaseCostMinor, p.stockQty AS stockQty,
               p.lowStockThreshold AS lowStockThreshold,
               COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN oi.qty ELSE 0 END), 0) AS unitsSold,
               COALESCE(SUM(CASE WHEN o.status IN ('DELIVERED','RETURNED','CANCELLED') THEN oi.qty ELSE 0 END), 0) AS reservedUnits,
               COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN oi.lineTotalMinor ELSE 0 END), 0) AS revenueMinor,
               COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN oi.qty * oi.unitCostMinor ELSE 0 END), 0) AS cogsMinor,
               MAX(CASE WHEN o.status = 'DELIVERED' THEN o.orderDate END) AS lastSaleAt
        FROM products p
        LEFT JOIN order_items oi ON oi.productId = p.id AND oi.businessId = p.businessId
        LEFT JOIN orders o ON o.id = oi.orderId AND o.businessId = p.businessId
        WHERE p.businessId = :businessId AND o.orderDate BETWEEN :fromAt AND :toAt
        GROUP BY p.id
        ORDER BY unitsSold DESC
        """
    )
    suspend fun productSalesInRange(businessId: Long, fromAt: Long, toAt: Long): List<ProductSalesMetric>

    data class ProductSalesMetric(
        val productId: Long,
        val name: String,
        val sku: String?,
        val category: String?,
        val sellingPriceMinor: Long,
        val purchaseCostMinor: Long,
        val stockQty: Long,
        val lowStockThreshold: Long,
        val unitsSold: Long,
        val reservedUnits: Long,
        val revenueMinor: Long,
        val cogsMinor: Long,
        val lastSaleAt: Long?
    )

    @Query("SELECT COUNT(*) FROM orders WHERE businessId = :businessId AND status IN ('CONFIRMED','PROCESSING','PACKED','SHIPPED')")
    fun observeProcessingCount(businessId: Long): Flow<Long>

    @Query("SELECT * FROM orders WHERE businessId = :businessId AND status IN ('CONFIRMED','PROCESSING','PACKED','SHIPPED') ORDER BY orderDate DESC LIMIT 100")
    fun observeProcessing(businessId: Long): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE businessId = :businessId AND status IN ('CONFIRMED','PROCESSING','PACKED','SHIPPED') ORDER BY orderDate DESC LIMIT 100")
    suspend fun listProcessing(businessId: Long): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE businessId = :businessId AND status = 'DELIVERED' AND codMinor > 0 ORDER BY orderDate DESC LIMIT 200")
    fun observePendingCod(businessId: Long): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE businessId = :businessId AND status = 'DELIVERED' AND codMinor > 0 ORDER BY orderDate DESC LIMIT 200")
    suspend fun listPendingCod(businessId: Long): List<OrderEntity>

    @Query(
        """
        SELECT * FROM orders WHERE businessId = :businessId
        AND (orderNo LIKE '%' || :query || '%' COLLATE NOCASE OR trackingNo LIKE '%' || :query || '%' COLLATE NOCASE)
        ORDER BY orderDate DESC LIMIT 200
        """
    )
    suspend fun searchByNumber(businessId: Long, query: String): List<OrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orders: List<OrderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllItems(items: List<OrderItemEntity>)

    @Query("DELETE FROM orders WHERE businessId = :businessId")
    suspend fun deleteAllForBusiness(businessId: Long)

    @Query("DELETE FROM order_items WHERE businessId = :businessId")
    suspend fun deleteAllItemsForBusiness(businessId: Long)
}

@Dao
interface OrderHistoryDao {

    @Insert
    suspend fun insert(history: OrderStatusHistoryEntity)

    @Query("SELECT * FROM order_status_history WHERE orderId = :orderId ORDER BY timestamp DESC, id DESC")
    fun observeForOrder(orderId: Long): Flow<List<OrderStatusHistoryEntity>>

    @Query("SELECT * FROM order_status_history WHERE businessId = :businessId ORDER BY timestamp DESC, id DESC LIMIT 300")
    fun observeRecent(businessId: Long): Flow<List<OrderStatusHistoryEntity>>
}

@Dao
interface PaymentDao {

    @Insert
    suspend fun insert(payment: PaymentEntity): Long

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun getById(id: Long): PaymentEntity?

    @Query("SELECT * FROM payments WHERE businessId = :businessId ORDER BY dateAt DESC, id DESC LIMIT 500")
    fun observeAll(businessId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE businessId = :businessId AND orderId = :orderId ORDER BY dateAt DESC")
    fun observeForOrder(businessId: Long, orderId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE businessId = :businessId AND customerId = :customerId ORDER BY dateAt DESC LIMIT 300")
    fun observeForCustomer(businessId: Long, customerId: Long): Flow<List<PaymentEntity>>

    @Query(
        "SELECT * FROM payments WHERE businessId = :businessId AND dateAt BETWEEN :fromAt AND :toAt ORDER BY dateAt DESC"
    )
    suspend fun inRange(businessId: Long, fromAt: Long, toAt: Long): List<PaymentEntity>

}
