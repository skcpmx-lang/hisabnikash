package com.hisabnikash.app.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class CustomerAggregate(
    @Embedded val customer: CustomerEntity,
    val orderCount: Long = 0,
    val totalOrdersMinor: Long = 0,        // all non-cancelled orders
    val deliveredOrdersMinor: Long = 0,    // delivered order totals
    val refundsMinor: Long = 0,            // refund documents
    val codOutstandingMinor: Long = 0,
    val lastOrderAt: Long? = null,
    val returnCount: Long = 0
)

@Dao
interface CustomerDao {

    @Insert
    suspend fun insert(customer: CustomerEntity): Long

    @Update
    suspend fun update(customer: CustomerEntity)

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE id = :id")
    fun observeById(id: Long): Flow<CustomerEntity?>

    @Query(
        """
        SELECT c.*,
          COALESCE(SUM(CASE WHEN o.status != 'CANCELLED' THEN 1 ELSE 0 END), 0) AS orderCount,
          COALESCE(SUM(CASE WHEN o.status != 'CANCELLED' THEN o.totalMinor ELSE 0 END), 0) AS totalOrdersMinor,
          COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN o.totalMinor ELSE 0 END), 0) AS deliveredOrdersMinor,
          COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN o.codMinor ELSE 0 END), 0) AS codOutstandingMinor,
          MAX(o.orderDate) AS lastOrderAt,
          (SELECT COUNT(*) FROM returns r WHERE r.customerId = c.id AND r.businessId = :businessId) AS returnCount,
          (SELECT COALESCE(SUM(r.amountMinor), 0) FROM refund_documents r WHERE r.customerId = c.id AND r.businessId = :businessId) AS refundsMinor
        FROM customers c
        LEFT JOIN orders o ON o.customerId = c.id AND o.businessId = c.businessId
        WHERE c.businessId = :businessId
        GROUP BY c.id
        ORDER BY c.name COLLATE NOCASE
        """
    )
    fun observeAll(businessId: Long): Flow<List<CustomerAggregate>>

    @Query(
        """
        SELECT c.*,
          COALESCE(SUM(CASE WHEN o.status != 'CANCELLED' THEN 1 ELSE 0 END), 0) AS orderCount,
          COALESCE(SUM(CASE WHEN o.status != 'CANCELLED' THEN o.totalMinor ELSE 0 END), 0) AS totalOrdersMinor,
          COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN o.totalMinor ELSE 0 END), 0) AS deliveredOrdersMinor,
          COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN o.codMinor ELSE 0 END), 0) AS codOutstandingMinor,
          MAX(o.orderDate) AS lastOrderAt,
          (SELECT COUNT(*) FROM returns r WHERE r.customerId = c.id AND r.businessId = :businessId) AS returnCount,
          (SELECT COALESCE(SUM(r.amountMinor), 0) FROM refund_documents r WHERE r.customerId = c.id AND r.businessId = :businessId) AS refundsMinor
        FROM customers c
        LEFT JOIN orders o ON o.customerId = c.id AND o.businessId = c.businessId
        WHERE c.businessId = :businessId
          AND (c.name LIKE '%' || :query || '%' COLLATE NOCASE OR c.phone LIKE '%' || :query || '%' OR c.email LIKE '%' || :query || '%')
        GROUP BY c.id
        ORDER BY c.name COLLATE NOCASE
        LIMIT 500
        """
    )
    fun search(businessId: Long, query: String): Flow<List<CustomerAggregate>>

    @Query("SELECT * FROM customers WHERE businessId = :businessId ORDER BY name COLLATE NOCASE LIMIT 1000")
    suspend fun listAll(businessId: Long): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE businessId = :businessId AND id = :id")
    suspend fun getForBusiness(businessId: Long, id: Long): CustomerEntity?

    @Query("SELECT COUNT(*) FROM customers WHERE businessId = :businessId")
    fun observeCount(businessId: Long): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)

    @Query("DELETE FROM customers WHERE businessId = :businessId")
    suspend fun deleteAllForBusiness(businessId: Long)
}
