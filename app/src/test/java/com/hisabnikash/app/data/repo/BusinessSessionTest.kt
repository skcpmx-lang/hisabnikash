package com.hisabnikash.app.data.repo

import com.hisabnikash.app.data.db.BusinessEntity
import com.hisabnikash.app.data.db.BusinessSettingsEntity
import com.hisabnikash.app.data.db.DocSequenceEntity
import com.hisabnikash.app.data.db.WorkspaceDao
import com.hisabnikash.app.data.prefs.ActiveBusinessStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The authoritative-session regression tests for the FK root cause:
 *  - the active business id must be validated against the businesses table,
 *  - stale ids must be healed instead of writing children,
 *  - writes must be refused (not faked) when no business exists,
 *  - a dummy businessId (0 / unknown) must never pass a repository guard.
 */
class BusinessSessionTest {

    private class FakeStore : ActiveBusinessStore {
        private val state = MutableStateFlow<Long?>(null)
        var onboarding = false
            private set
        override val activeBusinessId: Flow<Long?> = state
        override suspend fun setActiveBusiness(id: Long) { state.value = id }
        override suspend fun clearActiveBusiness() { state.value = null }
        override suspend fun setOnboardingDone(done: Boolean) { onboarding = done }
    }

    private class FakeWorkspaceDao : WorkspaceDao {
        private val rows = LinkedHashMap<Long, BusinessEntity>()
        private var nextId = 1L

        fun seed(id: Long, createdAt: Long = id) {
            rows[id] = BusinessEntity(id = id, name = "Business $id", category = "GENERAL", createdAt = createdAt)
        }

        fun seed(id: Long, name: String, createdAt: Long) {
            rows[id] = BusinessEntity(id = id, name = name, category = "GENERAL", createdAt = createdAt)
        }

        val ids: List<Long> get() = rows.keys.toList()

        override suspend fun insertBusiness(business: BusinessEntity): Long {
            val id = nextId++
            rows[id] = business.copy(id = id)
            return id
        }

        override suspend fun updateBusiness(business: BusinessEntity) {
            rows[business.id] = business
        }

        override suspend fun getBusiness(id: Long): BusinessEntity? = rows[id]

        override fun observeBusinesses(): Flow<List<BusinessEntity>> = flowOf(rows.values.toList())

        override suspend fun businessCount(): Int = rows.size

        override suspend fun latestBusiness(): BusinessEntity? =
            rows.values.maxWithOrNull(compareBy<BusinessEntity>({ it.createdAt }, { it.id }))

        override suspend fun deleteBusiness(id: Long) { rows.remove(id) }

        override suspend fun upsertSettings(settings: BusinessSettingsEntity) = Unit
        override suspend fun getSettings(businessId: Long): BusinessSettingsEntity? = null
        override fun observeSettings(businessId: Long): Flow<BusinessSettingsEntity?> = flowOf(null)
        override suspend fun getSequence(businessId: Long, docType: String): DocSequenceEntity? = null
        override suspend fun upsertSequence(sequence: DocSequenceEntity) = Unit
        override fun observeSequences(businessId: Long): Flow<List<DocSequenceEntity>> = flowOf(emptyList())
    }

    private fun session(store: FakeStore, dao: FakeWorkspaceDao) = BusinessSession(store, dao)

    private suspend fun exceptionOf(block: suspend () -> Unit): Throwable =
        runCatching { block() }.exceptionOrNull()!!

    @Test
    fun `valid preferred business is used`() = runTest {
        val store = FakeStore()
        val dao = FakeWorkspaceDao().apply { seed(7L, createdAt = 1); seed(3L, createdAt = 2) }
        store.setActiveBusiness(7L)
        assertEquals(7L, session(store, dao).requireActive())
    }

    @Test
    fun `stale active id heals to most recent existing business`() = runTest {
        val store = FakeStore()
        val dao = FakeWorkspaceDao().apply { seed(1L, createdAt = 1); seed(9L, createdAt = 9) }
        store.setActiveBusiness(999L) // points at a deleted/missing business
        store.setOnboardingDone(true)

        val healed = session(store, dao).recover()

        assertEquals(9L, healed)
        assertEquals(9L, store.activeBusinessId.first())
        // Recovery never re-arms onboarding when a business still exists.
        assertTrue(store.onboarding)
    }

    @Test
    fun `requireActive never writes under a stale business`() = runTest {
        val store = FakeStore()
        val dao = FakeWorkspaceDao().apply { seed(4L) }
        store.setActiveBusiness(404L)

        // Even a direct require (used inside save paths) heals instead of
        // silently writing children under the missing id.
        assertEquals(4L, session(store, dao).requireActive())
    }

    @Test
    fun `no business anywhere refuses the write`() = runTest {
        val store = FakeStore()
        val dao = FakeWorkspaceDao()
        store.setActiveBusiness(42L) // stale AND nothing to fall back to
        val ex = exceptionOf { session(store, dao).requireActive() }
        assertTrue(ex is IllegalStateException)
        assertTrue(ex.message!!.contains("Select or create a business"))
    }

    @Test
    fun `recover with no business clears session and re-arms onboarding`() = runTest {
        val store = FakeStore()
        store.setActiveBusiness(42L)
        store.setOnboardingDone(true)
        val dao = FakeWorkspaceDao()

        assertNull(session(store, dao).recover())
        assertNull(store.activeBusinessId.first())
        assertFalse(store.onboarding)
    }

    @Test
    fun `recover keeps a valid business untouched`() = runTest {
        val store = FakeStore()
        val dao = FakeWorkspaceDao().apply { seed(12L) }
        store.setActiveBusiness(12L)
        store.setOnboardingDone(true)

        assertEquals(12L, session(store, dao).recover())
        assertEquals(12L, store.activeBusinessId.first())
        assertTrue(store.onboarding)
    }

    @Test
    fun `repository guard rejects zero business id`() = runTest {
        val ex = exceptionOf { session(FakeStore(), FakeWorkspaceDao()).requireExists(0L) }
        assertTrue(ex is IllegalStateException)
        assertTrue(ex.message!!.contains("Select or create a business"))
    }

    @Test
    fun `repository guard rejects unknown business id`() = runTest {
        val dao = FakeWorkspaceDao().apply { seed(1L) }
        val ex = exceptionOf { session(FakeStore(), dao).requireExists(12345L) }
        assertTrue(ex is IllegalStateException)
        assertTrue(ex.message!!.contains("no longer exists"))
    }

    @Test
    fun `repository guard accepts a real business id`() = runTest {
        val dao = FakeWorkspaceDao().apply { seed(5L) }
        session(FakeStore(), dao).requireExists(5L) // must not throw
        assertTrue(dao.ids.contains(5L))
    }
}
