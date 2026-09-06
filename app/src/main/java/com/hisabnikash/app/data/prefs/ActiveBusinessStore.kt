package com.hisabnikash.app.data.prefs

import kotlinx.coroutines.flow.Flow

/**
 * The persistence contract for the ONE authoritative "current business"
 * session. Only [WorkspaceRepository] is allowed to make writes against this
 * store, and only after validating the id against the businesses table.
 */
interface ActiveBusinessStore {
    val activeBusinessId: Flow<Long?>

    suspend fun setActiveBusiness(id: Long)

    suspend fun clearActiveBusiness()

    suspend fun setOnboardingDone(done: Boolean)
}
