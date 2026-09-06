package com.hisabnikash.app.data.repo

import com.hisabnikash.app.data.db.AppDatabase
import com.hisabnikash.app.data.db.OrderEntity
import com.hisabnikash.app.domain.model.CommerceMath
import com.hisabnikash.app.domain.model.OrderStatus
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

data class Period(val fromAt: Long, val toAt: Long)

object PeriodControl {
    const val DAY_MS = 86_400_000L

    fun resolve(preset: String, now: Long = System.currentTimeMillis()): Period {
        val startOfToday = startOfDay(now)
        return when (preset) {
            "1D" -> Period(startOfToday, now)
            "7D" -> Period(startOfToday - 6 * DAY_MS, now)
            "10D" -> Period(startOfToday - 9 * DAY_MS, now)
            "30D" -> Period(startOfToday - 29 * DAY_MS, now)
            "90D" -> Period(startOfToday - 89 * DAY_MS, now)
            "1Y" -> Period(startOfToday - 364 * DAY_MS, now)
            else -> Period(startOfToday, now)
        }
    }

    fun previous(preset: String, now: Long = System.currentTimeMillis()): Period {
        val current = resolve(preset, now)
        val span = current.toAt - current.fromAt
        val gap = if (preset == "1D") DAY_MS else 0L
        return Period(current.fromAt - span - gap, current.fromAt - gap)
    }

    private fun startOfDay(now: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

data class MetricsBundle(
    val revenueMinor: Long = 0,
    val netProfitMinor: Long = 0,
    val cogsMinor: Long = 0,
    val couponDeliveryMinor: Long = 0,
    val courierMinor: Long = 0,
    val packagingMinor: Long = 0,
    val advertisingMinor: Long = 0,
    val otherCostsMinor: Long = 0,
    val refundsMinor: Long = 0,
    val expensesMinor: Long = 0,
    val orderCount: Long = 0,
    val deliveredCount: Long = 0,
    val returnedCount: Long = 0,
    val cancelledCount: Long = 0,
    val unitsSold: Long = 0,
    val codPendingMinor: Long = 0,
    val availableCashMinor: Long = 0,
    val receivablesMinor: Long = 0,
    val payablesMinor: Long = 0,
    val cashInMinor: Long = 0,
    val cashOutMinor: Long = 0,
    val transferInMinor: Long = 0,
    val transferOutMinor: Long = 0,
    val aovMinor: Long = 0,
    val marginBps: Int = 0,
    val returnRateBps: Int = 0,
    val roasBps: Int = 0,
    val adSpendMinor: Long = 0,
    val adRevenueMinor: Long = 0
)

/**
 * Central aggregation engine. Every screen (dashboard, analytics, reports,
 * profit explorer, cash flow) reads its numbers through this single path so
 * figures always agree.
 */
class InsightsRepository(private val db: AppDatabase) {

    fun observeOrderCounts(businessId: Long) = db.orderDao().observeCountsByStatus(businessId)

    fun observeDeliveredOrders(businessId: Long, fromAt: Long, toAt: Long) =
        db.orderDao().observeDeliveredInRange(businessId, fromAt, toAt)

    suspend fun metrics(businessId: Long, fromAt: Long, toAt: Long): MetricsBundle {
        val orders = db.orderDao().allInRange(businessId, fromAt, toAt)
        val delivered = orders.filter { it.status == OrderStatus.DELIVERED.name }
        val refunds = db.refundDao().inRange(businessId, fromAt, toAt)
        val expenses = db.expenseDao().inRange(businessId, fromAt, toAt)
        val ledger = db.ledgerDao().inRange(businessId, fromAt, toAt)
        val cogs = db.orderDao().cogsInRange(businessId, fromAt, toAt)
        val units = db.orderDao().unitsInRange(businessId, fromAt, toAt)
        val codOpen = db.receivablePayableDao().openCodReceivables(businessId)

        val revenue = delivered.sumOf { CommerceMath.orderRevenue(it) } - refunds.sumOf { it.amountMinor }
        val orderCosts = delivered.sumOf { CommerceMath.orderCosts(it) }
        val profit = revenue - cogs - orderCosts - refunds.sumOf { it.amountMinor }

        val courier = delivered.sumOf { it.courierFeeMinor + it.returnCourierFeeMinor }
        val packaging = delivered.sumOf { it.packagingMinor }
        val advertising = delivered.sumOf { it.advertisingMinor }
        val other = delivered.sumOf { it.otherCostMinor }

        val cashIn = ledger.filter { it.direction == "IN" && it.category != "TRANSFER" }.sumOf { it.amountMinor }
        val cashOut = ledger.filter { it.direction == "OUT" && it.category != "TRANSFER" }.sumOf { it.amountMinor }
        val transfers = ledger.filter { it.category == "TRANSFER" }
        val transferIn = transfers.filter { it.direction == "IN" }.sumOf { it.amountMinor }
        val transferOut = transfers.filter { it.direction == "OUT" }.sumOf { it.amountMinor }

        val opening = db.accountDao().totalOpening(businessId)
        val netAllTime = db.ledgerDao().netAllTime(businessId)
        val codPending = codOpen.sumOf { (it.amountMinor - it.paidMinor).coerceAtLeast(0) }

        val campaigns = db.campaignDao().allInRange(businessId, fromAt, toAt)

        val aov = if (delivered.isEmpty()) 0 else revenue / delivered.size
        val margin = CommerceMath.marginPercentBps(profit, revenue)
        val returnRate = CommerceMath.returnRate(
            orders.count { it.status == "RETURNED" },
            delivered.size.toLong()
        )
        val adSpend = campaigns.sumOf { it.spendMinor }
        val roas = CommerceMath.roas(campaigns.sumOf { it.attributedRevenueMinor }, adSpend)

        return MetricsBundle(
            revenueMinor = revenue.coerceAtLeast(0),
            netProfitMinor = profit,
            cogsMinor = cogs,
            courierMinor = courier,
            packagingMinor = packaging,
            advertisingMinor = advertising,
            otherCostsMinor = other,
            refundsMinor = refunds.sumOf { it.amountMinor },
            expensesMinor = expenses.sumOf { it.amountMinor },
            orderCount = orders.size.toLong(),
            deliveredCount = delivered.size.toLong(),
            returnedCount = orders.count { it.status == "RETURNED" }.toLong(),
            cancelledCount = orders.count { it.status == "CANCELLED" }.toLong(),
            unitsSold = units,
            codPendingMinor = codPending,
            availableCashMinor = opening + netAllTime,
            receivablesMinor = codPending,
            payablesMinor = db.receivablePayableDao().outstandingPayablesMinor(businessId),
            cashInMinor = cashIn,
            cashOutMinor = cashOut,
            transferInMinor = transferIn,
            transferOutMinor = transferOut,
            aovMinor = aov,
            marginBps = margin,
            returnRateBps = returnRate,
            roasBps = roas,
            adSpendMinor = adSpend,
            adRevenueMinor = campaigns.sumOf { it.attributedRevenueMinor }
        )
    }

    /** Same bundle with un-recognised orders excluded (orders not yet delivered). */
    suspend fun deliveredOrderCount(businessId: Long, fromAt: Long, toAt: Long): Long =
        db.orderDao().countWithStatusInRange(businessId, "DELIVERED", fromAt, toAt)

    suspend fun byChannel(businessId: Long, fromAt: Long, toAt: Long) =
        db.orderDao().byChannel(businessId, fromAt, toAt)

    suspend fun productSales(businessId: Long, fromAt: Long, toAt: Long) =
        db.orderDao().productSalesInRange(businessId, fromAt, toAt)

    suspend fun expensesByCategory(businessId: Long, fromAt: Long, toAt: Long) =
        db.expenseDao().inRange(businessId, fromAt, toAt).groupBy { it.category }
            .map { (category, list) -> category to list.sumOf { it.amountMinor } }
            .sortedByDescending { it.second }

    suspend fun dailySeries(businessId: Long, fromAt: Long, toAt: Long): List<DailyPoint> {
        val delivered = db.orderDao().allInRange(businessId, fromAt, toAt)
            .filter { it.status == OrderStatus.DELIVERED.name }
        val refunds = db.refundDao().inRange(businessId, fromAt, toAt)
        val dayMs = PeriodControl.DAY_MS
        val startDay = fromAt - (fromAt % dayMs)
        val map = LinkedHashMap<Long, MutableLongs>()
        var day = startDay
        while (day <= toAt) {
            map[day] = MutableLongs()
            day += dayMs
        }
        delivered.forEach { o ->
            val bucket = o.orderDate - (o.orderDate % dayMs)
            map[bucket]?.apply { revenue += CommerceMath.orderRevenue(o) }
        }
        refunds.forEach { r ->
            val bucket = r.dateAt - (r.dateAt % dayMs)
            map[bucket]?.apply { refunds += r.amountMinor }
        }
        return map.map { (time, v) ->
            DailyPoint(
                time = time,
                revenueMinor = v.revenue - v.refunds,
                ordersCount = 0
            )
        }
    }

    suspend fun intradayBuckets(businessId: Long, fromAt: Long, toAt: Long): List<DailyPoint> {
        val delivered = db.orderDao().allInRange(businessId, fromAt, toAt)
            .filter { it.status == OrderStatus.DELIVERED.name }
        val buckets = LinkedHashMap<Int, MutableLongs>()
        for (hour in 0..23) buckets[hour] = MutableLongs()
        delivered.forEach { o ->
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = o.orderDate }
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            buckets[hour]?.apply { revenue += CommerceMath.orderRevenue(o); orders += 1 }
        }
        return buckets.map { (hour, v) ->
            DailyPoint(
                time = hour * 86_400_000L,
                revenueMinor = v.revenue,
                ordersCount = v.orders
            )
        }
    }

    private class MutableLongs {
        var revenue: Long = 0
        var refunds: Long = 0
        var orders: Long = 0
    }
}

data class DailyPoint(
    val time: Long,
    val revenueMinor: Long,
    val ordersCount: Long
) {
    val hour: Int get() = (time / 86_400_000L).toInt()
}
