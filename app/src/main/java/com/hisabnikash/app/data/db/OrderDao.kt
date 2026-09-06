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

    @Query("SELECT COUNT(*) FROM orders WHERE businessId = :businessId")
    fun observeCount(businessId: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM orders WHERE businessId = :businessId AND status IN ('CONFIRMED','PROCESSING','PACKED','SHIPPED')")
    fun observeProcessingCount(businessId: Long): Flow<Long>

    @Query("SELECT * FROM orders WHERE businessId = :businessId AND status IN ('CONFIRMED','PROCESSING','PACKED','SHIPPED') ORDER BY orderDate DESC LIMIT 100")
    fun observeProcessing(businessId: Long): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE businessId = :businessId AND status = 'DELIVERED' AND codMinor > 0 ORDER BY orderDate DESC LIMIT 200")
    fun observePendingCod(businessId: Long): Flow<List<OrderEntity>>

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
