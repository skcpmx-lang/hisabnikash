package com.hisabnikash.app.domain.model

/**
 * Pure persistence rules for products.
 *
 * Products have an OPTIONAL supplier relationship. "Not linked" is a real
 * state and must be stored as NULL — never 0, -1, or any placeholder id.
 * A supplier may only be assigned to a product inside the SAME business, so
 * cross-business relationships are impossible even if a stale id arrives.
 */
data class SupplierReference(val id: Long, val businessId: Long)

object ProductSaveRules {

    /**
     * Normalises a raw supplier id from a form/UI: null, 0 and negative ids
     * all mean "not linked" and resolve to null.
     */
    fun normalizeSupplierId(supplierId: Long?): Long? = supplierId?.takeIf { it > 0 }

    /**
     * Validates the supplier reference for a product before persisting it.
     *
     * @return the id to store (null = not linked), or
     * @throws IllegalArgumentException when the supplier is missing or
     * belongs to a different business. The message is user friendly.
     */
    fun validateSupplier(
        supplierId: Long?,
        businessId: Long,
        supplier: SupplierReference?
    ): Long? {
        val normalized = normalizeSupplierId(supplierId) ?: return null
        if (supplier == null) {
            throw IllegalArgumentException(
                "The selected supplier no longer exists. Choose Not linked or select another supplier."
            )
        }
        if (supplier.businessId != businessId) {
            throw IllegalArgumentException(
                "The selected supplier belongs to a different business and cannot be linked to this product."
            )
        }
        return normalized
    }
}
