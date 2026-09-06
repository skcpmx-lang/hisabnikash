package com.hisabnikash.app.domain.model

import com.hisabnikash.app.data.db.OrderEntity

// ---------------------------------------------------------------------------
// Money
// ---------------------------------------------------------------------------
// All money values are stored as Long minor units (1 unit = 1/100 of the
// base currency). Float/Double are never used for money in this application.

object MoneyScale {
    const val SCALE = 100L
}

fun Long.percentBpsOf(bps: Int): Long =
    if (bps <= 0) 0 else (this * (bps.toLong()) / 10_000L)

// ---------------------------------------------------------------------------
// Enumerations (persisted as name strings)
// ---------------------------------------------------------------------------

enum class OrderStatus(val label: String) {
    DRAFT("Draft"),
    CONFIRMED("Confirmed"),
    PROCESSING("Processing"),
    PACKED("Packed"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    RETURNED("Returned"),
    CANCELLED("Cancelled");

    companion object {
        val RECOGNITION = setOf(DELIVERED)
        val OPEN = setOf(CONFIRMED, PROCESSING, PACKED, SHIPPED)
        val PROCESS = setOf(CONFIRMED, PROCESSING, PACKED, SHIPPED)

        fun from(value: String): OrderStatus =
            entries.firstOrNull { it.name == value } ?: DRAFT
    }
}

enum class CancellationReason(val label: String) {
    CUSTOMER_CANCELLED("Customer cancelled"),
    SELLER_CANCELLED("Seller cancelled"),
    UNREACHABLE("Unreachable"),
    WRONG_ADDRESS("Wrong address"),
    REFUSED("Refused"),
    FAILED_DELIVERY("Failed delivery"),
    COURIER_RETURNED("Courier returned"),
    DUPLICATE("Duplicate"),
    OTHER("Other")
}

enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    BKASH("bKash"),
    NAGAD("Nagad"),
    BANK("Bank"),
    CARD("Card"),
    COD("Cash on Delivery"),
    ADVANCE("Advance"),
    OTHER("Other")
}

enum class AccountType(val label: String) {
    CASH("Cash"),
    BANK("Bank"),
    BKASH("bKash"),
    NAGAD("Nagad"),
    ROCKET("Rocket"),
    CUSTOM("Custom")
}

enum class ExpenseCategory(val label: String) {
    ADVERTISING("Advertising"),
    COURIER("Courier"),
    PACKAGING("Packaging"),
    PURCHASES("Purchases"),
    RENT("Rent"),
    SALARY("Salary"),
    UTILITIES("Utilities"),
    SOFTWARE("Software"),
    OPERATIONS("Operations"),
    OTHER("Other")
}

enum class ReturnStatus(val label: String) {
    REQUESTED("Requested"),
    APPROVED("Approved"),
    PICKUP("Pickup"),
    RECEIVED("Received"),
    REFUND_PENDING("Refund pending"),
    REFUNDED("Refunded"),
    REJECTED("Rejected"),
    COMPLETED("Completed")
}

enum class ReturnResponsibility(val label: String) {
    CUSTOMER("Customer pays"),
    MERCHANT("Merchant pays"),
    POLICY("Courier / policy covers"),
    CUSTOM("Custom")
}

enum class InvoiceStatus(val label: String) {
    DRAFT("Draft"),
    ISSUED("Issued"),
    PARTIALLY_PAID("Partially paid"),
    PAID("Paid"),
    OVERDUE("Overdue"),
    VOID("Void")
}

enum class DeliveryMode(val label: String) {
    CUSTOMER("Customer pays delivery"),
    MERCHANT("Merchant pays / free delivery"),
    INCLUDED("Delivery included in price"),
    PREPAID("Customer prepaid delivery"),
    PARTIAL("Partial subsidy"),
    CUSTOM("Custom")
}

enum class CampaignPlatform(val label: String) {
    META("Meta"),
    GOOGLE("Google"),
    TIKTOK("TikTok"),
    YOUTUBE("YouTube"),
    OTHER("Other")
}

object Defaults {
    val CHANNELS = listOf(
        "Facebook", "Instagram", "TikTok", "Website",
        "WhatsApp", "Marketplace", "Offline", "Other"
    )
    val COURIERS = listOf("Pathao", "Steadfast", "RedX", "Paperfly", "Other")
    val BUSINESS_CATEGORIES = listOf(
        "Facebook Seller", "E-commerce", "F-commerce", "D2C Brand",
        "Fashion", "Electronics", "Beauty & Cosmetics", "Food",
        "Home & Lifestyle", "Wholesale", "Digital Products",
        "Service Business", "Other"
    )
}

// ---------------------------------------------------------------------------
// Financial calculations (central money engine)
// ---------------------------------------------------------------------------

object CommerceMath {

    fun orderSubtotal(items: List<Pair<Long, Long>>): Long =
        items.sumOf { (qty, unitPrice) -> qty * unitPrice }

    fun orderTotal(
        subtotalMinor: Long,
        discountMinor: Long,
        deliveryMinor: Long
    ): Long = (subtotalMinor - discountMinor + deliveryMinor).coerceAtLeast(0)

    /**
     * Revenue = subtotal - discount + customer-paid delivery.
     * Courier fee, packaging, advertising and other costs are expenses, not
     * reductions of revenue.
     */
    fun orderRevenue(order: OrderEntity): Long =
        (order.subtotalMinor - order.discountMinor + order.deliveryChargeMinor).coerceAtLeast(0)

    fun orderCogs(items: List<Pair<Long, Long>>): Long =
        items.sumOf { (qty, unitCost) -> qty * unitCost }

    fun orderCosts(order: OrderEntity): Long =
        order.courierFeeMinor + order.returnCourierFeeMinor +
            order.packagingMinor + order.advertisingMinor + order.otherCostMinor

    /**
     * Profit = Revenue - COGS - courier - return courier - packaging -
     * advertising - other costs - absorbed delivery (when merchant pays,
     * delivery charge is 0 so no extra handling is needed).
     */
    fun orderProfit(order: OrderEntity, cogsMinor: Long): Long =
        orderRevenue(order) - cogsMinor - orderCosts(order)

    fun marginPercentBps(netProfitMinor: Long, revenueMinor: Long): Int =
        if (revenueMinor <= 0) 0 else ((netProfitMinor * 10_000) / revenueMinor).toInt()

    fun aov(revenueMinor: Long, orderCount: Long): Long =
        if (orderCount <= 0) 0 else revenueMinor / orderCount

    fun unitsSold(items: List<Pair<Long, Long>>): Long = items.sumOf { it.first }

    fun returnRate(returnedOrders: Long, deliveredOrders: Long): Int =
        if (deliveredOrders <= 0) 0 else ((returnedOrders * 10_000) / deliveredOrders).toInt()

    fun roas(revenueMinor: Long, spendMinor: Long): Int =
        if (spendMinor <= 0) 0 else ((revenueMinor * 10_000) / spendMinor).toInt()

    fun costPerOrder(spendMinor: Long, orders: Long): Long =
        if (orders <= 0) 0 else spendMinor / orders

    fun customerLifetimeValue(revenueMinor: Long, orderCount: Long): Long =
        if (orderCount <= 0) 0 else revenueMinor / orderCount

    fun stockCoverDays(units: Long, dailyUnitsSold: Long): Long =
        if (dailyUnitsSold <= 0) 0 else units / dailyUnitsSold

    fun segmentFor(orderCount: Long, revenueMinor: Long, lastOrderAt: Long?, now: Long): String {
        val dayMs = 86_400_000L
        val sinceLast = lastOrderAt?.let { now - it }
        return when {
            orderCount <= 0 -> "NEW"
            revenueMinor >= 10_000_000L -> "VIP"
            revenueMinor >= 5_000_000L -> "HIGH_VALUE"
            orderCount >= 2 -> "RETURNING"
            sinceLast != null && sinceLast > 180 * dayMs -> "INACTIVE"
            sinceLast != null && sinceLast > 90 * dayMs -> "AT_RISK"
            else -> "NEW"
        }
    }
}

// Domain view models used across screens

data class OrderFinancials(
    val revenueMinor: Long,
    val cogsMinor: Long,
    val costsMinor: Long,
    val profitMinor: Long,
    val totalMinor: Long
)

fun orderFinancials(order: OrderEntity, items: List<Pair<Long, Long>>): OrderFinancials {
    val cogs = CommerceMath.orderCogs(items)
    return OrderFinancials(
        revenueMinor = CommerceMath.orderRevenue(order),
        cogsMinor = cogs,
        costsMinor = CommerceMath.orderCosts(order),
        profitMinor = CommerceMath.orderProfit(order, cogs),
        totalMinor = order.totalMinor
    )
}
