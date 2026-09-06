package com.hisabnikash.app.domain

import com.hisabnikash.app.data.repo.PeriodControl
import com.hisabnikash.app.domain.model.CommerceMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for deterministic customer segmentation and period math.
 *
 * These pin the boundaries of the rules so a future refactor cannot silently
 * make "AT_RISK" or "INACTIVE" unreachable again.
 */
class CustomerSegmentTest {

    private val day = 86_400_000L
    private val now = 1_700_000_000_000L // fixed instant for determinism

    @Test
    fun `single order 100 days old is at risk`() {
        val segment = CommerceMath.segmentFor(
            orderCount = 1,
            revenueMinor = 100_000,
            lastOrderAt = now - 100 * day,
            now = now
        )
        assertEquals("AT_RISK", segment)
    }

    @Test
    fun `single order 200 days old is inactive`() {
        val segment = CommerceMath.segmentFor(
            orderCount = 1,
            revenueMinor = 100_000,
            lastOrderAt = now - 200 * day,
            now = now
        )
        assertEquals("INACTIVE", segment)
    }

    @Test
    fun `repeat customer is returning even after long gap`() {
        val segment = CommerceMath.segmentFor(
            orderCount = 2,
            revenueMinor = 100_000,
            lastOrderAt = now - 200 * day,
            now = now
        )
        assertEquals("RETURNING", segment)
    }

    @Test
    fun `high spend outranks recency-based segments`() {
        val segment = CommerceMath.segmentFor(
            orderCount = 1,
            revenueMinor = 6_000_000,
            lastOrderAt = now - 300 * day,
            now = now
        )
        assertEquals("HIGH_VALUE", segment)
    }

    @Test
    fun `one day preset covers today only`() {
        val range = PeriodControl.resolve("1D", now)
        assertTrue(range.toAt - range.fromAt <= day)
        assertTrue(range.toAt <= now)
    }

    @Test
    fun `ten day preset covers ten days`() {
        val range = PeriodControl.resolve("10D", now)
        // Calendar-day based: allow a small DST slack only in the gap check.
        assertTrue(range.toAt >= now)
        assertTrue(range.fromAt <= now - 8 * day)
        assertTrue(range.fromAt >= now - 11 * day)
    }
}
