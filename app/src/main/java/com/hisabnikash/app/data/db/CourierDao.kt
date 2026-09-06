package com.hisabnikash.app.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class CourierStats(
    @Embedded val courier: CourierEntity,
    val deliveredCount: Long = 0,
    val returnedCount: Long = 0,
    val totalFeeMinor: Long = 0,
    val settledMinor: Long = 0,
    val pendingCodMinor: Long = 0
)

@Dao
interface CourierDao {

    @Insert
    suspend fun insert(courier: CourierEntity): Long

    @Update
    suspend fun update(courier: CourierEntity)

    @Query("SELECT * FROM couriers WHERE id = :id")
    suspend fun getById(id: Long): CourierEntity?

    @Query("SELECT * FROM couriers WHERE businessId = :businessId ORDER BY createdAt")
    suspend fun listAll(businessId: Long): List<CourierEntity>

    @Query("SELECT * FROM couriers WHERE businessId = :businessId ORDER BY createdAt")
    fun observeAll(businessId: Long): Flow<List<CourierEntity>>

    @Query(
        """
        SELECT c.*,
          COALESCE((SELECT COUNT(*) FROM orders o WHERE o.courierId = c.id AND o.status = 'DELIVERED'), 0) AS deliveredCount,
          COALESCE((SELECT COUNT(*) FROM orders o WHERE o.courierId = c.id AND o.status = 'RETURNED'), 0) AS returnedCount,
          COALESCE((SELECT SUM(o.courierFeeMinor + o.returnCourierFeeMinor) FROM orders o WHERE o.courierId = c.id AND o.status IN ('DELIVERED','RETURNED')), 0) AS totalFeeMinor,
          COALESCE((SELECT SUM(s.amountMinor) FROM courier_settlements s WHERE s.courierId = c.id), 0) AS settledMinor,
          COALESCE((SELECT SUM(o.codMinor) FROM orders o WHERE o.courierId = c.id AND o.status = 'DELIVERED'), 0) AS pendingCodMinor
        FROM couriers c
        WHERE c.businessId = :businessId
        ORDER BY c.createdAt
        """
    )
    fun observeStats(businessId: Long): Flow<List<CourierStats>>

    @Insert
    suspend fun insertSettlement(settlement: CourierSettlementEntity): Long

    @Query("SELECT * FROM courier_settlements WHERE id = :id")
    suspend fun getSettlement(id: Long): CourierSettlementEntity?

    @Query("SELECT * FROM courier_settlements WHERE businessId = :businessId ORDER BY dateAt DESC, id DESC LIMIT 1000")
    fun observeSettlements(businessId: Long): Flow<List<CourierSettlementEntity>>

    @Query("SELECT * FROM courier_settlements WHERE businessId = :businessId AND courierId = :courierId ORDER BY dateAt DESC LIMIT 300")
    fun observeForCourier(businessId: Long, courierId: Long): Flow<List<CourierSettlementEntity>>

    @Query("SELECT * FROM courier_settlements WHERE businessId = :businessId AND pendingMinor > 0 ORDER BY dateAt DESC LIMIT 100")
    suspend fun listPending(businessId: Long): List<CourierSettlementEntity>

    @Query(
        """
        SELECT
          COALESCE(SUM(CASE WHEN codMinor <= amountMinor + feeMinor THEN amountMinor ELSE 0 END), 0) AS settledMinor,
          COALESCE(SUM(feeMinor), 0) AS feesMinor,
          COALESCE(SUM(codMinor), 0) AS codTotalMinor
        FROM courier_settlements WHERE businessId = :businessId
        """
    )
    suspend fun settlementTotals(businessId: Long): SettlementTotals

    data class SettlementTotals(val settledMinor: Long, val feesMinor: Long, val codTotalMinor: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(couriers: List<CourierEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSettlements(settlements: List<CourierSettlementEntity>)

    @Query("DELETE FROM couriers WHERE businessId = :businessId")
    suspend fun deleteAll(businessId: Long)

    @Query("DELETE FROM courier_settlements WHERE businessId = :businessId")
    suspend fun deleteAllSettlements(businessId: Long)
}
