package com.hisabnikash.app.domain

import com.hisabnikash.app.domain.model.ProductSaveRules
import com.hisabnikash.app.domain.model.SupplierReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Pins the product supplier rules:
 *  - "Not linked" is NULL, never 0/-1/placeholder
 *  - only an existing supplier of the SAME business may be linked
 *  - cross-business and missing suppliers are rejected with clear errors
 */
class ProductSaveRulesTest {

    @Test
    fun `not linked resolves to null`() {
        assertNull(ProductSaveRules.validateSupplier(null, 1L, null))
    }

    @Test
    fun `zero and negative ids mean not linked`() {
        assertNull(ProductSaveRules.normalizeSupplierId(0L))
        assertNull(ProductSaveRules.normalizeSupplierId(-1L))
        assertNull(ProductSaveRules.validateSupplier(0L, 1L, null))
    }

    @Test
    fun `existing supplier of same business is accepted`() {
        val supplier = SupplierReference(id = 42L, businessId = 7L)
        assertEquals(42L, ProductSaveRules.validateSupplier(42L, 7L, supplier))
    }

    @Test
    fun `missing supplier is rejected with friendly message`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ProductSaveRules.validateSupplier(99L, 7L, null)
        }
        assertEquals(
            "The selected supplier no longer exists. Choose Not linked or select another supplier.",
            error.message
        )
    }

    @Test
    fun `supplier from another business cannot be assigned`() {
        val supplier = SupplierReference(id = 42L, businessId = 999L)
        val error = assertThrows(IllegalArgumentException::class.java) {
            ProductSaveRules.validateSupplier(42L, 7L, supplier)
        }
        assertEquals(
            "The selected supplier belongs to a different business and cannot be linked to this product.",
            error.message
        )
    }
}
