package com.hisabnikash.app.data.repo

import com.hisabnikash.app.data.db.BusinessEntity
import com.hisabnikash.app.data.db.WorkspaceDao
import com.hisabnikash.app.data.prefs.ActiveBusinessStore
import com.hisabnikash.app.domain.model.BusinessContext
import kotlinx.coroutines.flow.first

/**
 * The ONE authoritative current-business session.
 *
 * All writes obtain their business id through [requireActive] (or the
 * repository-level [requireExists] guard). Nothing else may decide which
 * business a record belongs to — form state and navigation arguments are
 * display-only. The session is validated against the `businesses` table on
 * every use, and stale ids are healed to the most recent existing business.
 */
class BusinessSession(
    private val store: ActiveBusinessStore,
    private val workspaceDao: WorkspaceDao
) {

    /**
     * @throws IllegalStateException with a human-readable message when no
     * valid business exists, so no child record is ever written under a
     * missing/stale parent.
     */
    suspend fun requireActive(): Long =
        resolveActive() ?: throw IllegalStateException(
            "Select or create a business before saving."
        )

    /**
     * Startup/repair entry point. Returns the business id that should be
     * active, healing the store when the remembered id is stale; returns null
     * (and clears the session) when no business exists at all.
     */
    suspend fun recover(): Long? {
        val resolved = resolveActive()
        if (resolved == null) {
            store.clearActiveBusiness()
            store.setOnboardingDone(false)
            return null
        }
        if (resolved != store.activeBusinessId.first()) {
            store.setActiveBusiness(resolved)
        }
        return resolved
    }

    /** Repository-level guard against writes under a missing business row. */
    suspend fun requireExists(businessId: Long) {
        if (businessId <= 0) throw IllegalStateException(
            "Select or create a business before saving."
        )
        if (workspaceDao.getBusiness(businessId) == null) throw IllegalStateException(
            "The active business no longer exists. Switch business and try again."
        )
    }

    suspend fun businessExists(id: Long): Boolean =
        id > 0 && workspaceDao.getBusiness(id) != null

    suspend fun latestBusiness(): BusinessEntity? = workspaceDao.latestBusiness()

    private suspend fun resolveActive(): Long? {
        val preferred = store.activeBusinessId.first()
        val exists = preferred?.let { workspaceDao.getBusiness(it) != null } == true
        val latest = workspaceDao.latestBusiness()?.id
        return BusinessContext.resolveActive(preferred, exists, latest)
    }
}
