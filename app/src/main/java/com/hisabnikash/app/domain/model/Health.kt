package com.hisabnikash.app.domain.model

/**
 * Deterministic business-health evaluation.
 *
 * This is an INTERNAL operating indicator, explicitly not a credit, bank or
 * financial rating: it is derived only from this business's own transaction
 * history and uses a fixed, documented rule set so the score is reproducible.
 *
 * Scoring rules (baseline 50, clamped to 0..100):
 *  - sales momentum   +10 grew / +5 new or steady / -10 declined
 *  - profitability    +15 >=25% / +10 >=12% / +5 >=1% / -10 negative
 *  - expense pressure +5 under control / -10 exceeds revenue / -5 no revenue yet
 *  - cash exposure    +10 no COD & payables / 0 moderate / -10 heavy
 *  - inventory        +5 no alerts / -5 low / -15 out of stock
 *  - returns          +5 <15% / -5 15-30% / -10 >30%
 *  - ad efficiency    +5 when ads run and ROAS >= 200%
 *
 * Labels: 88+ Excellent, 70+ Good, 50+ Needs attention, below At risk.
 * With no transaction history at all the evaluator returns null — the UI
 * shows "Not enough data" instead of inventing a score.
 */

enum class SignalLevel { GOOD, FAIR, WATCH }

data class HealthIndicator(
    val key: String,
    val label: String,
    val value: String,
    val level: SignalLevel
)

data class HealthVerdict(
    val score: Int,
    val label: String,
    val note: String,
    val trendLabel: String?,
    val indicators: List<HealthIndicator>
)

data class HealthSnapshot(
    val revenueMinor: Long = 0,
    val previousRevenueMinor: Long = 0,
    val marginBps: Int = 0,
    val expensesMinor: Long = 0,
    val codPendingMinor: Long = 0,
    val payablesMinor: Long = 0,
    val availableCashMinor: Long = 0,
    val returnRateBps: Int = 0,
    val orderCount: Long = 0,
    val deliveredCount: Long = 0,
    val lowStockCount: Long = 0,
    val outOfStockCount: Long = 0,
    val adSpendMinor: Long = 0,
    val roasBps: Int = 0
)

object HealthEvaluator {

    const val MIN_ORDER_HISTORY = 3L

    /** Returns null when there is not enough real data for a meaningful read. */
    fun evaluate(s: HealthSnapshot): HealthVerdict? {
        val hasHistory = s.orderCount > 0 || s.deliveredCount > 0 ||
            s.expensesMinor > 0 || s.codPendingMinor > 0
        if (!hasHistory) return null

        var score = 50

        // 1. Sales momentum vs the previous like-for-like period.
        val momentum = momentum(s.revenueMinor, s.previousRevenueMinor)
        score += when (momentum) {
            SignalLevel.GOOD -> 10
            SignalLevel.FAIR -> 5
            else -> if (s.previousRevenueMinor > 0) -10 else 0
        }

        // 2. Profitability.
        score += when {
            s.marginBps >= 2_500 -> 15
            s.marginBps >= 1_200 -> 10
            s.marginBps >= 100 -> 5
            s.marginBps < 0 -> -10
            else -> 0
        }

        // 3. Expense pressure: expenses under control is a positive sign.
        score += when {
            s.revenueMinor > 0 && s.expensesMinor > s.revenueMinor -> -10
            s.revenueMinor == 0L && s.expensesMinor > 0 -> -5
            else -> 5
        }

        // 4. Cash exposure: COD and payables relative to revenue.
        val exposure = s.codPendingMinor + s.payablesMinor
        score += when {
            s.codPendingMinor == 0L && s.payablesMinor == 0L -> 10
            s.revenueMinor > 0 && exposure > s.revenueMinor / 2 -> -10
            s.codPendingMinor > 0 -> -5
            else -> 0
        }

        // 5. Inventory health.
        score += when {
            s.outOfStockCount > 0 -> -15
            s.lowStockCount > 0 -> -5
            else -> 5
        }

        // 6. Return rate.
        score += when {
            s.returnRateBps >= 3_000 -> -10
            s.returnRateBps >= 1_500 -> -5
            else -> 5
        }

        // 7. Advertising efficiency (only when money is actually spent).
        if (s.adSpendMinor > 0 && s.roasBps >= 200) score += 5

        val clamped = score.coerceIn(0, 100)
        val label = when {
            clamped >= 88 -> "Excellent"
            clamped >= 70 -> "Good"
            clamped >= 50 -> "Needs attention"
            else -> "At risk"
        }
        val note = when {
            clamped >= 88 -> "Revenue and profit are tracking well. Keep the momentum."
            clamped >= 70 -> "Steady performance with room to improve margins or cash flow."
            clamped >= 50 -> "Check costs, pending COD and stock levels to protect profit."
            else -> "Several signals are weak. Review expenses and outstanding cash today."
        }
        val indicators = listOf(
            HealthIndicator("momentum", "Sales momentum", momentumValue(s), momentum),
            HealthIndicator("profit", "Profitability", "${formatPercent(s.marginBps)} margin", levelFor(s.marginBps, 1_200, 100)),
            HealthIndicator("cash", "Cash position", cashValue(s), cashLevel(s, exposure)),
            HealthIndicator("inventory", "Inventory", inventoryValue(s), inventoryLevel(s)),
            HealthIndicator("returns", "Returns", "${formatPercent(s.returnRateBps)} rate", rateLevel(s.returnRateBps)),
            HealthIndicator("expenses", "Expense pressure", expenseValue(s), expenseLevel(s))
        )
        return HealthVerdict(clamped, label, note, momentumTrend(s, momentum), indicators)
    }

    private fun momentum(current: Long, previous: Long): SignalLevel = when {
        previous == 0L -> SignalLevel.FAIR
        current >= previous -> SignalLevel.GOOD
        current >= previous * 8 / 10 -> SignalLevel.FAIR
        else -> SignalLevel.WATCH
    }

    private fun momentumValue(s: HealthSnapshot): String = when {
        s.previousRevenueMinor == 0L && s.revenueMinor == 0L -> "No sales yet"
        s.previousRevenueMinor == 0L -> "New sales this period"
        else -> relativeChange(s.revenueMinor, s.previousRevenueMinor)
    }

    private fun momentumTrend(s: HealthSnapshot, level: SignalLevel): String? = when (level) {
        SignalLevel.GOOD -> "Improving vs previous period"
        SignalLevel.FAIR -> "Holding steady vs previous period"
        SignalLevel.WATCH -> "Declining vs previous period"
    }

    private fun levelFor(bps: Int, goodFrom: Int, fairFrom: Int): SignalLevel = when {
        bps >= goodFrom -> SignalLevel.GOOD
        bps >= fairFrom -> SignalLevel.FAIR
        else -> SignalLevel.WATCH
    }

    private fun cashValue(s: HealthSnapshot): String =
        "${formatMoney(s.availableCashMinor)} available, ${formatMoney(s.codPendingMinor)} COD"

    private fun cashLevel(s: HealthSnapshot, exposure: Long): SignalLevel = when {
        s.codPendingMinor == 0L && s.payablesMinor == 0L -> SignalLevel.GOOD
        s.revenueMinor > 0 && exposure > s.revenueMinor / 2 -> SignalLevel.WATCH
        else -> SignalLevel.FAIR
    }

    private fun inventoryValue(s: HealthSnapshot): String = when {
        s.outOfStockCount > 0 && s.lowStockCount > 0 ->
            "${s.outOfStockCount} out, ${s.lowStockCount} low"
        s.outOfStockCount > 0 -> "${s.outOfStockCount} out of stock"
        s.lowStockCount > 0 -> "${s.lowStockCount} low on stock"
        else -> "No stock alerts"
    }

    private fun inventoryLevel(s: HealthSnapshot): SignalLevel = when {
        s.outOfStockCount > 0 -> SignalLevel.WATCH
        s.lowStockCount > 0 -> SignalLevel.FAIR
        else -> SignalLevel.GOOD
    }

    private fun rateLevel(rateBps: Int): SignalLevel = when {
        rateBps >= 3_000 -> SignalLevel.WATCH
        rateBps >= 1_500 -> SignalLevel.FAIR
        else -> SignalLevel.GOOD
    }

    private fun expenseValue(s: HealthSnapshot): String = when {
        s.revenueMinor == 0L && s.expensesMinor == 0L -> "No expenses recorded"
        s.revenueMinor == 0L -> formatMoney(s.expensesMinor) + " spent this period"
        else -> "${formatPercent((s.expensesMinor * 10_000 / s.revenueMinor).toInt())} of revenue spent"
    }

    private fun expenseLevel(s: HealthSnapshot): SignalLevel = when {
        s.revenueMinor > 0 && s.expensesMinor > s.revenueMinor -> SignalLevel.WATCH
        s.revenueMinor == 0L && s.expensesMinor > 0 -> SignalLevel.FAIR
        else -> SignalLevel.GOOD
    }

    private fun relativeChange(current: Long, previous: Long): String {
        if (previous == 0L) return "New sales"
        val delta = (current - previous) * 100 / previous
        return if (delta >= 0) "+$delta% vs previous" else "$delta% vs previous"
    }
}

