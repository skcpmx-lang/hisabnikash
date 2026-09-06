package com.hisabnikash.app.data.repo

import androidx.room.withTransaction
import com.hisabnikash.app.data.db.AccountEntity
import com.hisabnikash.app.data.db.AppDatabase
import com.hisabnikash.app.data.db.BusinessEntity
import com.hisabnikash.app.data.db.BusinessSettingsEntity
import com.hisabnikash.app.data.db.CourierEntity
import com.hisabnikash.app.data.db.DocSequenceEntity
import com.hisabnikash.app.data.db.SalesChannelEntity
import com.hisabnikash.app.data.prefs.ActiveBusinessStore
import com.hisabnikash.app.domain.model.Defaults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class Workspace(
    val business: BusinessEntity? = null,
    val settings: BusinessSettingsEntity? = null
)

/**
 * Owns business creation, seeding, switching and document numbering.
 */
class WorkspaceRepository(
    private val db: AppDatabase,
    private val prefs: ActiveBusinessStore
) {

    private val workspaceDao = db.workspaceDao()
    private val session = BusinessSession(prefs, workspaceDao)

    val businesses: Flow<List<BusinessEntity>> = workspaceDao.observeBusinesses()

    suspend fun activeBusinessId(): Long? = prefs.activeBusinessId.first()

    fun observeActiveBusinessId(): Flow<Long?> = prefs.activeBusinessId

    fun observeActiveWorkspace(): Flow<Workspace> = combine(
        prefs.activeBusinessId, businesses
    ) { id, list ->
        Workspace(list.firstOrNull { it.id == id })
    }.flatMapLatest { w ->
        if (w.business == null) flowOf(w)
        else workspaceDao.observeSettings(w.business.id).map { w.copy(settings = it) }
    }

    fun observeActiveSettings(): Flow<BusinessSettingsEntity?> =
        prefs.activeBusinessId.flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(null)
            else workspaceDao.observeSettings(id)
        }

    suspend fun createBusiness(
        name: String,
        category: String,
        firstName: String,
        lastName: String,
        address: String?,
        phone: String?,
        email: String?,
        currency: String,
        channels: List<String>,
        initialAccounts: Map<String, Long>,
        invoicePrefix: String,
        logoPath: String?
    ): Long = db.withTransaction {
        val businessId = workspaceDao.insertBusiness(
            BusinessEntity(
                name = name.trim(),
                category = category,
                address = address,
                phone = phone,
                email = email,
                currency = currency,
                logoPath = logoPath
            )
        )
        workspaceDao.upsertSettings(
            BusinessSettingsEntity(
                businessId = businessId,
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                invoicePrefix = invoicePrefix.ifBlank { "INV-" },
                invoiceNextNo = 1
            )
        )
        val usedChannels = channels.map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { Defaults.CHANNELS }
        usedChannels.forEach { name ->
            db.channelDao().insert(SalesChannelEntity(businessId = businessId, name = name))
        }
        val accountInitial = initialAccounts.toMutableMap()
        listOf("Cash", "bKash", "Nagad", "Bank").forEach { name ->
            db.accountDao().insert(
                AccountEntity(
                    businessId = businessId,
                    name = name,
                    type = accountTypeOf(name),
                    openingBalanceMinor = accountInitial.remove(name) ?: 0
                )
            )
        }
        accountInitial.forEach { (name, amount) ->
            db.accountDao().insert(
                AccountEntity(
                    businessId = businessId,
                    name = name,
                    type = "CUSTOM",
                    openingBalanceMinor = amount
                )
            )
        }
        Defaults.COURIERS.forEach { name ->
            db.courierDao().insert(CourierEntity(businessId = businessId, name = name))
        }
        listOf(
            "ORDER" to "ORD-",
            "INVOICE" to invoicePrefix.ifBlank { "INV-" },
            "RECEIPT" to "RCT-",
            "REFUND" to "RFD-",
            "RETURN" to "RTN-",
            "EXCHANGE" to "EXC-"
        ).forEach { (type, prefix) ->
            workspaceDao.upsertSequence(
                DocSequenceEntity(businessId = businessId, docType = type, prefix = prefix, nextNo = 1)
            )
        }
        prefs.setActiveBusiness(businessId)
        prefs.setOnboardingDone(true)
        businessId
    }

    suspend fun updateBusiness(business: BusinessEntity) {
        workspaceDao.updateBusiness(business)
    }

    suspend fun getBusiness(id: Long): BusinessEntity? = workspaceDao.getBusiness(id)

    suspend fun updateSettings(settings: BusinessSettingsEntity) {
        workspaceDao.upsertSettings(settings.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun switchBusiness(id: Long) {
        val business = workspaceDao.getBusiness(id)
            ?: throw IllegalStateException("This business no longer exists. Choose another business.")
        prefs.setActiveBusiness(business.id)
    }

    /**
     * Deletes a business and its cascade children, then repairs the session:
     * the active id is moved to the most recent surviving business, or the
     * session is cleared (and onboarding re-armed) when none remains.
     */
    suspend fun deleteBusiness(id: Long) {
        db.withTransaction { workspaceDao.deleteBusiness(id) }
        val active = prefs.activeBusinessId.first()
        if (active == id) {
            val latest = workspaceDao.latestBusiness()?.id
            if (latest != null) {
                prefs.setActiveBusiness(latest)
            } else {
                prefs.clearActiveBusiness()
                prefs.setOnboardingDone(false)
            }
        }
    }

    /**
     * Repository-level guard: refutes a write when the business row that a
     * child row must reference does not exist. Never emits raw SQLite text.
     */
    suspend fun requireBusinessExists(businessId: Long) = session.requireExists(businessId)

    /**
     * The authoritative business id for every write operation.
     *
     * Resolves from the workspace session ONLY — never from a form's local
     * state. If the session points at a missing/stale business, the workspace
     * is healed to the most recent existing business; if none exists, writes
     * are refused with a domain-level message (no orphan records, no fake ids).
     */
    suspend fun requireActiveBusiness(): Long = session.requireActive()

    /**
     * Startup recovery: validates the workspace session against the database
     * and heals stale IDs. Returns null and clears the session when no
     * business exists at all, so the app routes to business creation.
     */
    suspend fun recoverActiveBusiness(): Long? = session.recover()

    /**
     * Allocates a collision-safe sequential document number per business.
     * Runs inside the caller's transaction; sequence rows are upserted.
     */
    suspend fun nextDocNumber(businessId: Long, docType: String): String =
        db.withTransaction {
            val seq = workspaceDao.getSequence(businessId, docType)
                ?: DocSequenceEntity(businessId, docType, defaultPrefix(docType), 1)
            val number = String.format("%06d", seq.nextNo)
            workspaceDao.upsertSequence(seq.copy(nextNo = seq.nextNo + 1))
            "${seq.prefix}$number"
        }

    private fun defaultPrefix(docType: String) = when (docType) {
        "ORDER" -> "ORD-"
        "INVOICE" -> "INV-"
        "RECEIPT" -> "RCT-"
        "REFUND" -> "RFD-"
        "RETURN" -> "RTN-"
        "EXCHANGE" -> "EXC-"
        else -> "DOC-"
    }

    private fun accountTypeOf(name: String) = when (name.lowercase()) {
        "cash" -> "CASH"
        "bkash" -> "BKASH"
        "nagad" -> "NAGAD"
        "bank" -> "BANK"
        "rocket" -> "ROCKET"
        else -> "CUSTOM"
    }
}
