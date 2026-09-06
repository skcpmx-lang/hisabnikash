package com.hisabnikash.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(campaign: CampaignEntity): Long

    @Query("SELECT * FROM campaigns WHERE id = :id")
    suspend fun getById(id: Long): CampaignEntity?

    @Query("SELECT * FROM campaigns WHERE id = :id")
    fun observeById(id: Long): Flow<CampaignEntity?>

    @Query("SELECT * FROM campaigns WHERE businessId = :businessId ORDER BY startAt DESC, id DESC")
    fun observeAll(businessId: Long): Flow<List<CampaignEntity>>

    @Query("SELECT * FROM campaigns WHERE businessId = :businessId AND startAt BETWEEN :fromAt AND :toAt ORDER BY startAt ASC")
    suspend fun allInRange(businessId: Long, fromAt: Long, toAt: Long): List<CampaignEntity>

    @Query("SELECT * FROM campaigns WHERE businessId = :businessId AND startAt BETWEEN :fromAt AND :toAt ORDER BY startAt ASC")
    fun observeInRange(businessId: Long, fromAt: Long, toAt: Long): Flow<List<CampaignEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(campaigns: List<CampaignEntity>)

    @Query("DELETE FROM campaigns WHERE businessId = :businessId")
    suspend fun deleteAll(businessId: Long)
}

@Dao
interface ChannelDao {

    @Insert
    suspend fun insert(channel: SalesChannelEntity): Long

    @Query("SELECT * FROM sales_channels WHERE businessId = :businessId ORDER BY createdAt")
    suspend fun listAll(businessId: Long): List<SalesChannelEntity>

    @Query("SELECT * FROM sales_channels WHERE businessId = :businessId ORDER BY createdAt")
    fun observeAll(businessId: Long): Flow<List<SalesChannelEntity>>

    @Query("SELECT * FROM sales_channels WHERE businessId = :businessId AND name = :name COLLATE NOCASE")
    suspend fun findByName(businessId: Long, name: String): SalesChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<SalesChannelEntity>)

    @Query("DELETE FROM sales_channels WHERE businessId = :businessId")
    suspend fun deleteAll(businessId: Long)
}

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity): Long

    @Query("SELECT * FROM budgets WHERE businessId = :businessId AND (:category = 'ALL' OR category = :category) ORDER BY periodStart DESC, id DESC")
    fun observeFiltered(businessId: Long, category: String): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<BudgetEntity>)

    @Query("DELETE FROM budgets WHERE businessId = :businessId")
    suspend fun deleteAll(businessId: Long)
}

@Dao
interface NotificationDao {

    @Insert
    suspend fun insert(notification: AppNotificationEntity): Long

    @Query("SELECT * FROM notifications WHERE id = :id")
    suspend fun getById(id: Long): AppNotificationEntity?

    @Query(
        """
        SELECT * FROM notifications
        WHERE businessId = :businessId AND (:filter = 'ALL' OR (:filter = 'UNREAD' AND read = 0) OR (category = :filter))
        ORDER BY createdAt DESC, id DESC LIMIT 500
        """
    )
    fun observeFiltered(businessId: Long, filter: String): Flow<List<AppNotificationEntity>>

    @Query("UPDATE notifications SET read = :read WHERE businessId = :businessId AND id = :id")
    suspend fun setRead(businessId: Long, id: Long, read: Boolean)

    @Query("UPDATE notifications SET read = 1 WHERE businessId = :businessId")
    suspend fun markAllRead(businessId: Long)

    @Query("SELECT COUNT(*) FROM notifications WHERE businessId = :businessId AND read = 0")
    fun observeUnreadCount(businessId: Long): Flow<Long>

    @Query("SELECT * FROM notifications WHERE businessId = :businessId AND title = :title AND read = 0 AND createdAt >= :since LIMIT 1")
    suspend fun findOpen(businessId: Long, title: String, since: Long): AppNotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<AppNotificationEntity>)

    @Query("DELETE FROM notifications WHERE businessId = :businessId")
    suspend fun deleteAll(businessId: Long)
}

@Dao
interface AuditDao {

    @Insert
    suspend fun insert(event: AuditEventEntity)

    @Query("SELECT * FROM audit_events WHERE businessId = :businessId ORDER BY timestamp DESC, id DESC LIMIT 300")
    fun observeRecent(businessId: Long): Flow<List<AuditEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<AuditEventEntity>)

    @Query("DELETE FROM audit_events WHERE businessId = :businessId")
    suspend fun deleteAll(businessId: Long)
}

@Dao
interface ActivityDao {

    @Insert
    suspend fun insert(activity: CustomerActivityEntity)

    @Query("SELECT * FROM customer_activities WHERE customerId = :customerId ORDER BY timestamp DESC, id DESC LIMIT 300")
    fun observeForCustomer(customerId: Long): Flow<List<CustomerActivityEntity>>

    @Query("SELECT * FROM customer_activities WHERE businessId = :businessId ORDER BY timestamp DESC, id DESC LIMIT 300")
    fun observeRecent(businessId: Long): Flow<List<CustomerActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(activities: List<CustomerActivityEntity>)

    @Query("DELETE FROM customer_activities WHERE businessId = :businessId")
    suspend fun deleteAll(businessId: Long)
}
