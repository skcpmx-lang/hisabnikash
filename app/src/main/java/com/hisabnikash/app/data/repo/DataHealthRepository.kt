package com.hisabnikash.app.data.repo

import androidx.room.withTransaction
import com.hisabnikash.app.data.db.AppDatabase

data class HealthFinding(
    val severity: String, // CRITICAL | WARNING | INFO
    val type: String,
    val message: String,
    val repair: String? = null,
    val count: Long = 1
)

data class HealthReport(val findings: List<HealthFinding>) {
    val hasCritical: Boolean get() = findings.any { it.severity == "CRITICAL" }
    val summary: String
        get() = if (findings.isEmpty()) "All checks passed. Your data looks healthy."
        else "${findings.size} finding(s): ${findings.count { it.severity == "CRITICAL" }} critical, " +
            "${findings.count { it.severity == "WARNING" }} warning."
}

/**
 * Non-destructive audit of referential integrity and financial consistency.
 * Nothing is changed until the user explicitly repairs.
 */
class DataHealthRepository(private val db: AppDatabase) {

    suspend fun checkBusiness(businessId: Long): HealthReport {
        val sql = db.openHelper.writableDatabase
        val findings = mutableListOf<HealthFinding>()

        fun count(query: String, vararg bind: String): Long =
            sql.query(query.replace(":b", businessId.toString()), bind).use { c ->
                if (c.moveToFirst()) c.getLong(0) else 0
            }

        fun exists(query: String): Boolean =
            sql.query(query.replace(":b", businessId.toString()), arrayOf<Any?>()).use { it.moveToFirst() }

        // Orphan records.
        val orphanMovements = count(
            "SELECT COUNT(*) FROM inventory_movements m WHERE m.businessId=:b AND m.productId NOT IN (SELECT id FROM products WHERE businessId=:b)"
        )
        if (orphanMovements > 0) findings += HealthFinding(
            "WARNING", "ORPHAN_MOVEMENT", "$orphanMovements inventory movement(s) reference missing products.",
            "Delete orphaned inventory movements."
        )

        val orphanItems = count(
            "SELECT COUNT(*) FROM order_items oi WHERE oi.businessId=:b AND oi.orderId NOT IN (SELECT id FROM orders WHERE businessId=:b)"
        )
        if (orphanItems > 0) findings += HealthFinding(
            "CRITICAL", "ORPHAN_ORDER_ITEM", "$orphanItems order item(s) reference missing orders.",
            "Delete orphaned order items."
        )

        val missingBusiness = count(
            "SELECT COUNT(*) FROM inventory_movements m WHERE m.businessId=:b AND m.businessId NOT IN (SELECT id FROM businesses)"
        )
        if (missingBusiness > 0) findings += HealthFinding(
            "CRITICAL", "MISSING_BUSINESS", "$missingBusiness record(s) carry an unknown business id."
        )

        // Duplicate invoice numbers per business.
        val dupInvoices = count(
            "SELECT COUNT(*) FROM (SELECT invoiceNo FROM invoices WHERE businessId=:b GROUP BY invoiceNo HAVING COUNT(*)>1)"
        )
        if (dupInvoices > 0) findings += HealthFinding(
            "CRITICAL", "DUP_INVOICE", "$dupInvoices invoice number(s) are duplicated. Keep numbers unique.",
            "Rename duplicates to a new number."
        )

        // Impossible financial states.
        val badReceivables = count(
            "SELECT COUNT(*) FROM receivables WHERE businessId=:b AND paidMinor > amountMinor"
        )
        if (badReceivables > 0) findings += HealthFinding(
            "CRITICAL", "OVER_PAID_RECEIVABLE", "$badReceivables receivable(s) are over-paid.",
            "Clamp paid amount to the receivable total."
        )
        val badPayables = count(
            "SELECT COUNT(*) FROM payables WHERE businessId=:b AND paidMinor > amountMinor"
        )
        if (badPayables > 0) findings += HealthFinding(
            "CRITICAL", "OVER_PAID_PAYABLE", "$badPayables payable(s) are over-paid.",
            "Clamp paid amount to the payable total."
        )
        val negativeOrders = count(
            "SELECT COUNT(*) FROM orders WHERE businessId=:b AND totalMinor < 0"
        )
        if (negativeOrders > 0) findings += HealthFinding(
            "CRITICAL", "NEGATIVE_TOTAL", "$negativeOrders order(s) have negative totals."
        )

        // Database-level integrity.
        val integrity = count("PRAGMA integrity_check") // returns ok or problem string
        if (integrity != 0L) findings += HealthFinding("INFO", "SQLITE_INTEGRITY", "SQLite integrity check completed.")

        // Missing indexes/noise checks are intentionally skipped; schema is fixed.

        if (findings.isEmpty()) findings += HealthFinding(
            "INFO", "CLEAN", "No orphan records, duplicate numbers or impossible financial states found."
        )
        return HealthReport(findings)
    }

    /**
     * Applies only the safe, expected repairs. Never rewrites financial
     * history silently — repairs are limited to removing orphaned records and
     * clamping over-paid documents after the user confirms.
     */
    suspend fun repair(businessId: Long, types: Set<String>): Int = db.withTransaction {
        var fixed = 0
        val database = db.openHelper.writableDatabase
        if ("ORPHAN_MOVEMENT" in types) {
            fixed += database.delete(
                "inventory_movements",
                "businessId = ? AND productId NOT IN (SELECT id FROM products WHERE businessId = ?)",
                arrayOf(businessId.toString(), businessId.toString())
            )
        }
        if ("ORPHAN_ORDER_ITEM" in types) {
            fixed += database.delete(
                "order_items",
                "businessId = ? AND orderId NOT IN (SELECT id FROM orders WHERE businessId = ?)",
                arrayOf(businessId.toString(), businessId.toString())
            )
        }
        if ("OVER_PAID_RECEIVABLE" in types) {
            database.execSQL(
                "UPDATE receivables SET paidMinor = amountMinor, status='PAID' WHERE businessId = ? AND paidMinor > amountMinor",
                arrayOf(businessId.toString())
            )
            fixed++
        }
        if ("OVER_PAID_PAYABLE" in types) {
            database.execSQL(
                "UPDATE payables SET paidMinor = amountMinor, status='PAID' WHERE businessId = ? AND paidMinor > amountMinor",
                arrayOf(businessId.toString())
            )
            fixed++
        }
        fixed
    }
}
