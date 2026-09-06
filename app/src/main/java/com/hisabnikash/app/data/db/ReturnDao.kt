package com.hisabnikash.app.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class ReturnWithRelation(
    @Embedded val returnRow: ReturnEntity,
    val items: List<ReturnItemEntity>
)

@Dao
interface ReturnDao {

    @Insert
    suspend fun insert(returnOrder: ReturnEntity): Long

    @Update
    suspend fun update(returnOrder: ReturnEntity)

    @Insert
    suspend fun insertItems(items: List<ReturnItemEntity>)

    @Query("SELECT * FROM returns WHERE id = :id")
    suspend fun getById(id: Long): ReturnEntity?

    @Query("SELECT * FROM returns WHERE id = :id")
    fun observeById(id: Long): Flow<ReturnEntity?>

    @Query(
        """
        SELECT * FROM returns WHERE businessId = :businessId AND (:status = 'ALL' OR status = :status)
        ORDER BY createdAt DESC, id DESC LIMIT 1000
        """
    )
    fun observeFiltered(businessId: Long, status: String): Flow<List<ReturnEntity>>

    @Query("SELECT * FROM returns WHERE businessId = :businessId AND orderId = :orderId ORDER BY createdAt DESC LIMIT 100")
    fun observeForOrder(businessId: Long, orderId: Long): Flow<List<ReturnEntity>>

    @Query("SELECT * FROM returns WHERE businessId = :businessId AND customerId = :customerId ORDER BY createdAt DESC LIMIT 200")
    fun observeForCustomer(businessId: Long, customerId: Long): Flow<List<ReturnEntity>>

    @Query("SELECT * FROM return_items WHERE returnId = :returnId ORDER BY id")
    suspend fun itemsFor(returnId: Long): List<ReturnItemEntity>

    @Query("SELECT * FROM return_items WHERE businessId = :businessId AND productId = :productId ORDER BY id DESC")
    suspend fun listForProduct(businessId: Long, productId: Long): List<ReturnItemEntity>

    @Query("SELECT * FROM returns WHERE businessId = :businessId AND customerId = :customerId")
    suspend fun listForCustomer(businessId: Long, customerId: Long): List<ReturnEntity>

    @Query("SELECT COUNT(*) FROM returns WHERE businessId = :businessId")
    fun observeCount(businessId: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM returns WHERE businessId = :businessId AND status NOT IN ('REJECTED','COMPLETED')")
    fun observePendingCount(businessId: Long): Flow<Long>

    @Query("SELECT * FROM returns WHERE businessId = :businessId AND status NOT IN ('REJECTED','COMPLETED') ORDER BY createdAt DESC LIMIT 100")
    suspend fun listPending(businessId: Long): List<ReturnEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(returns: List<ReturnEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllItems(items: List<ReturnItemEntity>)

    @Query("DELETE FROM returns WHERE businessId = :businessId")
    suspend fun deleteAll(businessId: Long)

    @Query("DELETE FROM return_items WHERE businessId = :businessId")
    suspend fun deleteAllItems(businessId: Long)
}

@Dao
interface ExchangeDao {

    @Insert
    suspend fun insert(exchange: ExchangeEntity): Long

    @Update
    suspend fun update(exchange: ExchangeEntity)

    @Query("SELECT * FROM exchanges WHERE id = :id")
    suspend fun getById(id: Long): ExchangeEntity?

    @Query("SELECT * FROM exchanges WHERE id = :id")
    fun observeById(id: Long): Flow<ExchangeEntity?>

    @Query("SELECT * FROM exchanges WHERE businessId = :businessId ORDER BY createdAt DESC, id DESC LIMIT 500")
    fun observeAll(businessId: Long): Flow<List<ExchangeEntity>>

    @Query("SELECT * FROM exchange_items WHERE exchangeId = :exchangeId ORDER BY id")
    suspend fun itemsFor(exchangeId: Long): List<ExchangeItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exchanges: List<ExchangeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllItems(items: List<ExchangeItemEntity>)

    @Query("DELETE FROM exchanges WHERE businessId = :businessId")
    suspend fun deleteAll(businessId: Long)

    @Query("DELETE FROM exchange_items WHERE businessId = :businessId")
    suspend fun deleteAllItems(businessId: Long)
}

@Dao
interface RefundDao {

    @Insert
    suspend fun insert(refund: RefundDocumentEntity): Long

    @Query("SELECT * FROM refund_documents WHERE id = :id")
    suspend fun getById(id: Long): RefundDocumentEntity?

    @Query("SELECT * FROM refund_documents WHERE id = :id")
    fun observeById(id: Long): Flow<RefundDocumentEntity?>

    @Query("SELECT * FROM refund_documents WHERE businessId = :businessId ORDER BY dateAt DESC, id DESC LIMIT 1000")
    fun observeAll(businessId: Long): Flow<List<RefundDocumentEntity>>

    @Query("SELECT * FROM refund_documents WHERE businessId = :businessId AND orderId = :orderId ORDER BY dateAt DESC LIMIT 200")
    fun observeForOrder(businessId: Long, orderId: Long): Flow<List<RefundDocumentEntity>>

    @Query("SELECT * FROM refund_documents WHERE businessId = :businessId AND customerId = :customerId ORDER BY dateAt DESC LIMIT 200")
    fun observeForCustomer(businessId: Long, customerId: Long): Flow<List<RefundDocumentEntity>>

    @Query(
        "SELECT COALESCE(SUM(amountMinor), 0) FROM refund_documents WHERE businessId = :businessId AND dateAt BETWEEN :fromAt AND :toAt"
    )
    suspend fun totalInRange(businessId: Long, fromAt: Long, toAt: Long): Long

    @Query(
        "SELECT * FROM refund_documents WHERE businessId = :businessId AND dateAt BETWEEN :fromAt AND :toAt ORDER BY dateAt ASC"
    )
    suspend fun inRange(businessId: Long, fromAt: Long, toAt: Long): List<RefundDocumentEntity>

    @Query(
        "SELECT * FROM refund_documents WHERE businessId = :businessId AND dateAt BETWEEN :fromAt AND :toAt ORDER BY dateAt ASC"
    )
    fun observeInRange(businessId: Long, fromAt: Long, toAt: Long): Flow<List<RefundDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(refunds: List<RefundDocumentEntity>)

    @Query("DELETE FROM refund_documents WHERE businessId = :businessId")
    suspend fun deleteAll(businessId: Long)
}
