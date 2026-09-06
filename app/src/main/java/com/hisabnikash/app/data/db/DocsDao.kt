package com.hisabnikash.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

    @Insert
    suspend fun insert(invoice: InvoiceEntity): Long

    @Update
    suspend fun update(invoice: InvoiceEntity)

    @Insert
    suspend fun insertItems(items: List<InvoiceItemEntity>)

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getById(id: Long): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE id = :id")
    fun observeById(id: Long): Flow<InvoiceEntity?>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId ORDER BY id")
    suspend fun itemsFor(invoiceId: Long): List<InvoiceItemEntity>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId ORDER BY id")
    fun observeItemsFor(invoiceId: Long): Flow<List<InvoiceItemEntity>>

    @Query(
        """
        SELECT * FROM invoices WHERE businessId = :businessId AND (:status = 'ALL' OR status = :status)
        ORDER BY dateAt DESC, id DESC LIMIT 1000
        """
    )
    fun observeFiltered(businessId: Long, status: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE businessId = :businessId AND invoiceNo LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY dateAt DESC LIMIT 200")
    suspend fun search(businessId: Long, query: String): List<InvoiceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(invoices: List<InvoiceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllItems(items: List<InvoiceItemEntity>)

    @Query("DELETE FROM invoices WHERE businessId = :businessId")
    suspend fun deleteAll(businessId: Long)

    @Query("DELETE FROM invoice_items WHERE businessId = :businessId")
    suspend fun deleteAllItems(businessId: Long)
}

@Dao
interface ReceiptDao {

    @Insert
    suspend fun insert(receipt: ReceiptEntity): Long

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getById(id: Long): ReceiptEntity?

    @Query("SELECT * FROM receipts WHERE id = :id")
    fun observeById(id: Long): Flow<ReceiptEntity?>

    @Query("SELECT * FROM receipts WHERE businessId = :businessId ORDER BY dateAt DESC, id DESC LIMIT 1000")
    fun observeAll(businessId: Long): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE businessId = :businessId AND customerId = :customerId ORDER BY dateAt DESC LIMIT 300")
    fun observeForCustomer(businessId: Long, customerId: Long): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE businessId = :businessId AND invoiceId = :invoiceId ORDER BY dateAt DESC LIMIT 200")
    fun observeForInvoice(businessId: Long, invoiceId: Long): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE businessId = :businessId AND receiptNo LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY dateAt DESC LIMIT 200")
    suspend fun search(businessId: Long, query: String): List<ReceiptEntity>

    @Query("UPDATE receipts SET remainingMinor = :remaining WHERE id = :id")
    suspend fun updateRemaining(id: Long, remaining: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(receipts: List<ReceiptEntity>)

    @Query("DELETE FROM receipts WHERE businessId = :businessId")
    suspend fun deleteAll(businessId: Long)
}
