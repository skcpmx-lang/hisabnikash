package com.hisabnikash.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {

    @Insert
    suspend fun insertBusiness(business: BusinessEntity): Long

    @Update
    suspend fun updateBusiness(business: BusinessEntity)

    @Query("SELECT * FROM businesses WHERE id = :id")
    suspend fun getBusiness(id: Long): BusinessEntity?

    @Query("SELECT * FROM businesses ORDER BY createdAt")
    fun observeBusinesses(): Flow<List<BusinessEntity>>

    @Query("SELECT COUNT(*) FROM businesses")
    suspend fun businessCount(): Int

    @Query("DELETE FROM businesses WHERE id = :id")
    suspend fun deleteBusiness(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: BusinessSettingsEntity)

    @Query("SELECT * FROM business_settings WHERE businessId = :businessId")
    suspend fun getSettings(businessId: Long): BusinessSettingsEntity?

    @Query("SELECT * FROM business_settings WHERE businessId = :businessId")
    fun observeSettings(businessId: Long): Flow<BusinessSettingsEntity?>

    @Query("SELECT * FROM doc_sequences WHERE businessId = :businessId AND docType = :docType")
    suspend fun getSequence(businessId: Long, docType: String): DocSequenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSequence(sequence: DocSequenceEntity)

    @Query("SELECT * FROM doc_sequences WHERE businessId = :businessId")
    fun observeSequences(businessId: Long): Flow<List<DocSequenceEntity>>
}
