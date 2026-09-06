package com.hisabnikash.app.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class SupplierAggregate(
    @Embedded val supplier: SupplierEntity,
    val purchaseCount: Long = 0,
    val purchasedMinor: Long = 0,
    val payableMinor: Long = 0,
    val lastPriceMinor: Long = 0,
    val lastPurchaseAt: Long? = null
)

@Dao
interface SupplierDao {

    @Insert
    suspend fun insert(supplier: SupplierEntity): Long

    @Update
    suspend fun update(supplier: SupplierEntity)

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun getById(id: Long): SupplierEntity?

    @Query("SELECT * FROM suppliers WHERE id = :id")
    fun observeById(id: Long): Flow<SupplierEntity?>

    @Query("SELECT * FROM suppliers WHERE businessId = :businessId ORDER BY name COLLATE NOCASE")
    suspend fun listAll(businessId: Long): List<SupplierEntity>

    @Query("SELECT * FROM suppliers WHERE businessId = :businessId ORDER BY name COLLATE NOCASE")
    fun observeAll(businessId: Long): Flow<List<SupplierEntity>>

    @Query(
        """
        SELECT s.*,
          COUNT(p.id) AS purchaseCount,
          COALESCE(SUM(p.totalMinor), 0) AS purchasedMinor,
          COALESCE(SUM(CASE WHEN p.totalMinor > p.paidMinor THEN p.totalMinor - p.paidMinor ELSE 0 END), 0) AS payableMinor,
          MAX(p.dateAt) AS lastPurchaseAt,
          (SELECT COALESCE(MAX(pi.unitCostMinor), 0) FROM purchase_items pi JOIN purchases pp ON pp.id = pi.purchaseId WHERE pp.supplierId = s.id) AS lastPriceMinor
        FROM suppliers s
        LEFT JOIN purchases p ON p.supplierId = s.id AND p.businessId = s.businessId
        WHERE s.businessId = :businessId
        GROUP BY s.id
        ORDER BY s.name COLLATE NOCASE
        """
    )
    fun observeWithAggregates(businessId: Long): Flow<List<SupplierAggregate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(suppliers: List<SupplierEntity>)

    @Query("DELETE FROM suppliers WHERE businessId = :businessId")
    suspend fun deleteAllForBusiness(businessId: Long)
}

@Dao
interface PurchaseDao {

    @Insert
    suspend fun insert(purchase: PurchaseEntity): Long

    @Update
    suspend fun update(purchase: PurchaseEntity)

    @Insert
    suspend fun insertItems(items: List<PurchaseItemEntity>)

    @Query("SELECT * FROM purchases WHERE id = :id")
    suspend fun getById(id: Long): PurchaseEntity?

    @Query("SELECT * FROM purchases WHERE id = :id")
    fun observeById(id: Long): Flow<PurchaseEntity?>

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId ORDER BY id")
    suspend fun itemsFor(purchaseId: Long): List<PurchaseItemEntity>

    @Query("SELECT * FROM purchases WHERE businessId = :businessId ORDER BY dateAt DESC, id DESC")
    fun observeAll(businessId: Long): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases WHERE businessId = :businessId AND supplierId = :supplierId ORDER BY dateAt DESC, id DESC")
    fun observeForSupplier(businessId: Long, supplierId: Long): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchase_items WHERE businessId = :businessId AND productId = :productId ORDER BY id DESC LIMIT 50")
    fun observeForProduct(businessId: Long, productId: Long): Flow<List<PurchaseItemEntity>>

    @Query("SELECT * FROM purchase_items WHERE businessId = :businessId AND productId = :productId ORDER BY id DESC LIMIT 200")
    suspend fun listForProduct(businessId: Long, productId: Long): List<PurchaseItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(purchases: List<PurchaseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllItems(items: List<PurchaseItemEntity>)

    @Query("DELETE FROM purchases WHERE businessId = :businessId")
    suspend fun deleteAllForBusiness(businessId: Long)

    @Query("DELETE FROM purchase_items WHERE businessId = :businessId")
    suspend fun deleteAllItemsForBusiness(businessId: Long)
}
