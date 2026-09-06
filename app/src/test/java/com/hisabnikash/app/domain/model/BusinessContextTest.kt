package com.hisabnikash.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regression tests for the business-workspace rule that prevents the shared
 * foreign-key failure: the id a write uses must be a REAL business id from
 * the workspace session, verified against the businesses table, never a
 * form-local placeholder (0 / -1), never stale.
 */
class BusinessContextTest {

    @Test
    fun `preferred real id wins over latest`() {
        assertEquals(7L, BusinessContext.resolveActive(preferredId = 7L, preferredExists = true, latestId = 3L))
    }

    @Test
    fun `stale preferred falls back to latest real business`() {
        // Session remembered a business that no longer exists (deleted or
        // restored DB): the write must move to the most recent existing one.
        assertEquals(3L, BusinessContext.resolveActive(preferredId = 99L, preferredExists = false, latestId = 3L))
    }

    @Test
    fun `stale preferred with no fallback refuses the write`() {
        assertNull(BusinessContext.resolveActive(preferredId = 99L, preferredExists = false, latestId = null))
    }

    @Test
    fun `zero placeholder id is never used even if latest exists`() {
        // The historical bug: forms defaulted businessId = 0 and that value
        // reached the database. The session resolver must treat it as invalid.
        assertEquals(5L, BusinessContext.resolveActive(preferredId = 0L, preferredExists = false, latestId = 5L))
        assertNull(BusinessContext.resolveActive(preferredId = 0L, preferredExists = false, latestId = null))
    }

    @Test
    fun `negative id is never used`() {
        assertNull(BusinessContext.resolveActive(preferredId = -1L, preferredExists = false, latestId = null))
    }

    @Test
    fun `no business anywhere refuses the write`() {
        assertNull(BusinessContext.resolveActive(preferredId = null, preferredExists = false, latestId = null))
    }
}
