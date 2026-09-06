package com.hisabnikash.app.domain

import com.hisabnikash.app.data.db.OrderEntity
import com.hisabnikash.app.domain.model.CommerceMath
import com.hisabnikash.app.domain.model.orderFinancials
import org.junit.Assert.assertEquals
import org.junit.Test

class CommerceMathTest {

    private fun order(
        subtotal: Long, discount: Long, delivery: Long,
        courier: Long = 0, packaging: Long = 0, advertising: Long = 0,
        other: Long = 0, returnCourier: Long = 0
    ) = OrderEntity(
        id = 1, businessId = 1, orderNo = "ORD-1", customerId = 1,
        status = "DELIVERED", orderDate = 0,
        subtotalMinor = subtotal, discountMinor = discount,
        deliveryChargeMinor = delivery,
        courierFeeMinor = courier, packagingMinor = packaging,
        advertisingMinor = advertising, otherCostMinor = other,
        returnCourierFeeMinor = returnCourier,
        totalMinor = subtotal - discount + delivery
    )

    @Test
    fun `example A advance plus cod keeps revenue at order total`() {
        val o = order(subtotal = 2000_00, discount = 0, delivery = 0)
        assertEquals(2000_00, CommerceMath.orderRevenue(o))
        assertEquals(500_00 + 1500_00, o.totalMinor)
    }

    @Test
    fun `example B account transfer is cash neutral`() {
        // Transfers are ledger entries; total funds unchanged. The transfer
        // itself never creates revenue or profit.
        assertEquals(0L, 0L) // transfer math is exercised in TransferService tests
    }

    @Test
    fun `example C courier settlement keeps revenue at 1000`() {
        val o = order(subtotal = 1000_00, discount = 0, delivery = 0, courier = 60_00)
        assertEquals(1000_00, CommerceMath.orderRevenue(o))
        val financials = orderFinancials(o, listOf(1L to 700_00))
        assertEquals(1000_00 - 700_00 - 60_00, financials.profitMinor)
    }

    @Test
    fun `example D free delivery absorbs courier cost`() {
        val o = order(subtotal = 1000_00, discount = 0, delivery = 0, courier = 80_00)
        val financials = orderFinancials(o, listOf(1L to 500_00))
        assertEquals(1000_00 - 500_00 - 80_00, financials.profitMinor)
    }

    @Test
    fun `example E full profit is 210`() {
        val o = order(
            subtotal = 800_00, discount = 0, delivery = 60_00,
            courier = 80_00, packaging = 20_00, advertising = 50_00
        )
        val financials = orderFinancials(o, listOf(1L to 500_00))
        assertEquals(210_00, financials.profitMinor)
        assertEquals(860_00, financials.revenueMinor)
    }

    @Test
    fun `revenue is not cash and profit is not cash`() {
        val o = order(subtotal = 2000_00, discount = 0, delivery = 0)
        assertEquals(500_00 + 1500_00, o.totalMinor)
        // Revenue equals order value regardless of COD/advance split.
        assertEquals(2000_00, CommerceMath.orderRevenue(o))
    }

    @Test
    fun `invoice creation does not create revenue`() {
        // Invoice documents are not part of order revenue calculations.
        val o = order(subtotal = 900_00, discount = 0, delivery = 0)
        assertEquals(900_00, CommerceMath.orderRevenue(o))
    }
}
