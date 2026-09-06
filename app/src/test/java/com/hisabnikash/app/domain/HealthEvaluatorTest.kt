package com.hisabnikash.app.domain

import com.hisabnikash.app.domain.model.HealthEvaluator
import com.hisabnikash.app.domain.model.HealthSnapshot
import com.hisabnikash.app.domain.model.SignalLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins the deterministic business-health rules. The evaluator must never
 * invent a score for an empty business and must never rank unrelated data.
 */
class HealthEvaluatorTest {

    @Test
    fun `no transaction history means not enough data`() {
        val verdict = HealthEvaluator.evaluate(HealthSnapshot())
        assertNull(verdict)
    }

    @Test
    fun `healthy growth and margin earn a good score`() {
        val verdict = HealthEvaluator.evaluate(
            HealthSnapshot(
                revenueMinor = 200_000_00,
                previousRevenueMinor = 100_000_00,
                marginBps = 3_000,
                orderCount = 5,
                deliveredCount = 4
            )
        )
        assertNotNull(verdict)
        requireNotNull(verdict)
        assertEquals("Excellent", verdict.label)
        assertEquals(SignalLevel.GOOD, verdict.indicators.first { it.key == "momentum" }.level)
        assertEquals(SignalLevel.GOOD, verdict.indicators.first { it.key == "profit" }.level)
    }

    @Test
    fun `declining revenue is flagged as watch`() {
        val verdict = HealthEvaluator.evaluate(
            HealthSnapshot(
                revenueMinor = 50_000_00,
                previousRevenueMinor = 100_000_00,
                marginBps = 2_000,
                orderCount = 5,
                deliveredCount = 4
            )
        )
        requireNotNull(verdict)
        assertEquals(SignalLevel.WATCH, verdict.indicators.first { it.key == "momentum" }.level)
        assertEquals("Declining vs previous period", verdict.trendLabel)
    }

    @Test
    fun `out of stock and high returns pull the score down`() {
        val healthy = HealthEvaluator.evaluate(
            HealthSnapshot(revenueMinor = 100_000_00, marginBps = 2_000, orderCount = 5, deliveredCount = 4)
        )
        val stressed = HealthEvaluator.evaluate(
            HealthSnapshot(
                revenueMinor = 100_000_00,
                marginBps = 2_000,
                orderCount = 5,
                deliveredCount = 4,
                outOfStockCount = 2,
                returnRateBps = 3_000
            )
        )
        requireNotNull(healthy)
        requireNotNull(stressed)
        assertEquals(true, healthy.score > stressed.score)
    }

    @Test
    fun `expenses exceeding revenue is a watch signal`() {
        val verdict = HealthEvaluator.evaluate(
            HealthSnapshot(
                revenueMinor = 100_000_00,
                expensesMinor = 150_000_00,
                marginBps = 1_000,
                orderCount = 5,
                deliveredCount = 4
            )
        )
        requireNotNull(verdict)
        assertEquals(SignalLevel.WATCH, verdict.indicators.first { it.key == "expenses" }.level)
    }

    @Test
    fun `score is always within 0-100`() {
        val verdict = HealthEvaluator.evaluate(
            HealthSnapshot(
                revenueMinor = 5_000_00,
                previousRevenueMinor = 50_000_00,
                marginBps = -5_000,
                expensesMinor = 9_000_00,
                codPendingMinor = 50_000_00,
                payablesMinor = 20_000_00,
                outOfStockCount = 10,
                lowStockCount = 10,
                returnRateBps = 5_000,
                orderCount = 2,
                deliveredCount = 1
            )
        )
        requireNotNull(verdict)
        assertEquals(true, verdict.score in 0..100)
        assertEquals("At risk", verdict.label)
    }
}
