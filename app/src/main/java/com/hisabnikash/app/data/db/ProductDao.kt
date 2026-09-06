package com.hisabnikash.app.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class ProductAggregate(
    @Embedded val product: ProductEntity,
    val unitsSold: Long = 0,
    val revenueMinor: Long = 0,
    val cogsMinor: Long = 0,
    val profitMinor: Long = 0,
    val lastSaleAt: Long? = null
)

@Dao
interface ProductDao {

    @Insert
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE id = :id")
    fun observeById(id: Long): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE businessId = :businessId AND id = :id")
    suspend fun getForBusiness(businessId: Long, id: Long): ProductEntity?

    @Query(
        """
        SELECT p.*,
          COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN oi.qty ELSE 0 END), 0) AS unitsSold,
          COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN oi.lineTotalMinor ELSE 0 END), 0) AS revenueMinor,
          COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN oi.qty * oi.unitCostMinor ELSE 0 END), 0) AS cogsMinor,
          MAX(o.orderDate) AS lastSaleAt
        FROM products p
        LEFT JOIN order_items oi ON oi.productId = p.id AND oi.businessId = p.businessId
        LEFT JOIN orders o ON o.id = oi.orderId AND o.businessId = p.businessId
        WHERE p.businessId = :businessId
        GROUP BY p.id
        ORDER BY p.name COLLATE NOCASE
        """
    )
    fun observeAll(businessId: Long): Flow<List<ProductAggregate>>

    @Query(
        """
        SELECT p.*,
          COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN oi.qty ELSE 0 END), 0) AS unitsSold,
          COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN oi.lineTotalMinor ELSE 0 END), 0) AS revenueMinor,
          COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN oi.qty * oi.unitCostMinor ELSE 0 END), 0) AS cogsMinor,
          MAX(o.orderDate) AS lastSaleAt
        FROM products p
        LEFT JOIN order_items oi ON oi.productId = p.id AND oi.businessId = p.businessId
        LEFT JOIN orders o ON o.id = oi.orderId AND o.businessId = p.businessId
        WHERE p.businessId = :businessId
          AND (p.name LIKE '%' || :query || '%' COLLATE NOCASE OR IFNULL(p.sku,'') LIKE '%' || :query || '%' COLLATE NOCASE)
        GROUP BY p.id
        ORDER BY p.name COLLATE NOCASE
        LIMIT 500
        """
    )
    fun search(businessId: Long, query: String): Flow<List<ProductAggregate>>

    @Query("SELECT * FROM products WHERE businessId = :businessId ORDER BY name COLLATE NOCASE")
    suspend fun listAll(businessId: Long): List<ProductEntity>

    @Query("SELECT * FROM products WHERE businessId = :businessId AND status = 'ACTIVE' ORDER BY name COLLATE NOCASE LIMIT 1000")
    suspend fun listActive(businessId: Long): List<ProductEntity>

    @Query("SELECT COUNT(*) FROM products WHERE businessId = :businessId")
    fun observeCount(businessId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(stockQty), 0) FROM products WHERE businessId = :businessId")
    fun observeTotalUnits(businessId: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM products WHERE businessId = :businessId AND stockQty = 0 AND status = 'ACTIVE'")
    fun observeOutOfStockCount(businessId: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM products WHERE businessId = :businessId AND status = 'ACTIVE' AND stockQty > 0 AND stockQty <= lowStockThreshold")
    fun observeLowStockCount(businessId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(p.stockQty * p.purchaseCostMinor), 0) FROM products p WHERE p.businessId = :businessId")
    fun observeInventoryValue(businessId: Long): Flow<Long>

    @Query("SELECT * FROM products WHERE businessId = :businessId AND status = 'ACTIVE' AND stockQty <= lowStockThreshold AND stockQty > 0 ORDER BY stockQty ASC LIMIT 100")
    fun observeLowStock(businessId: Long): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE businessId = :businessId AND status = 'ACTIVE' AND stockQty = 0 ORDER BY name COLLATE NOCASE LIMIT 100")
    fun observeOutOfStock(businessId: Long): Flow<List<ProductEntity>>

    @Query("UPDATE products SET stockQty = :stockQty, updatedAt = :now WHERE id = :id")
    suspend fun updateStock(id: Long, stockQty: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE products SET stockQty = stockQty + :delta, updatedAt = :now WHERE id = :id")
    suspend fun adjustStock(id: Long, delta: Long, now: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Query("DELETE FROM products WHERE businessId = :businessId")
    suspend fun deleteAllForBusiness(businessId: Long)
}

@Dao
interface VariantDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(variants: List<ProductVariantEntity>)

    @Query("SELECT * FROM product_variants WHERE businessId = :businessId AND productId = :productId ORDER BY name")
    fun observeForProduct(businessId: Long, productId: Long): Flow<List<ProductVariantEntity>>

    @Query("DELETE FROM product_variants WHERE productId = :productId")
    suspend fun deleteForProduct(productId: Long)

    @Query("DELETE FROM product_variants WHERE businessId = :businessId")
    suspend fun deleteAllForBusiness(businessId: Long)
}

@Dao
interface InventoryDao {

    @Insert
    suspend fun insert(movement: InventoryMovementEntity): Long

    @Query("SELECT * FROM inventory_movements WHERE businessId = :businessId AND productId = :productId ORDER BY dateAt DESC, id DESC LIMIT 500")
    fun observeForProduct(businessId: Long, productId: Long): Flow<List<InventoryMovementEntity>>

    @Query("SELECT * FROM inventory_movements WHERE businessId = :businessId ORDER BY dateAt DESC, id DESC LIMIT 500")
    fun observeRecent(businessId: Long): Flow<List<InventoryMovementEntity>>

    @Query("SELECT * FROM inventory_movements WHERE businessId = :businessId ORDER BY dateAt DESC, id DESC LIMIT 500")
    suspend fun listRecent(businessId: Long): List<InventoryMovementEntity>

    @Query("SELECT * FROM inventory_movements WHERE businessId = :businessId AND productId = :productId ORDER BY dateAt DESC, id DESC")
    suspend fun listForProduct(businessId: Long, productId: Long): List<InventoryMovementEntity>

    @Query("SELECT * FROM inventory_movements WHERE businessId = :businessId AND refType = :refType AND refId = :refId")
    suspend fun forRef(businessId: Long, refType: String, refId: Long): List<InventoryMovementEntity>
}
