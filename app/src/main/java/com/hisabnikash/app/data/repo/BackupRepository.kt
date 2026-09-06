package com.hisabnikash.app.data.repo

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.withTransaction
import com.hisabnikash.app.data.db.AppDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Local JSON backup/restore. Backup reads raw table rows scoped to a business;
 * restore validates the bundle before touching the database and requires an
 * explicit confirmation (replaceExisting) to avoid silent overwrites.
 */
class BackupRepository(private val db: AppDatabase) {

    /**
     * Insertion order respects foreign keys. Deletion order is the reverse.
     */
    private val tableOrder = listOf(
        "businesses",
        "business_settings",
        "doc_sequences",
        "sales_channels",
        "accounts",
        "couriers",
        "customers",
        "suppliers",
        "products",
        "product_variants",
        "account_transactions",
        "transfers",
        "expenses",
        "receivables",
        "payables",
        "orders",
        "order_items",
        "order_status_history",
        "payments",
        "courier_settlements",
        "purchases",
        "purchase_items",
        "inventory_movements",
        "returns",
        "return_items",
        "exchanges",
        "exchange_items",
        "refund_documents",
        "invoices",
        "invoice_items",
        "receipts",
        "campaigns",
        "budgets",
        "notifications",
        "audit_events",
        "customer_activities"
    )

    suspend fun exportToFile(file: File, businessId: Long): Int {
        val database = db.openHelper.writableDatabase
        val root = JSONObject()
        root.put("format", "hisabnikash-backup")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("businessId", businessId)
        val tables = JSONObject()
        var rows = 0
        db.withTransaction {
            tableOrder.forEach { table ->
                val array = JSONArray()
                val where = if (table == "businesses") "id = $businessId" else "businessId = $businessId"
                database.query("SELECT * FROM $table WHERE $where").use { cursor ->
                    val columnNames = cursor.columnNames
                    while (cursor.moveToNext()) {
                        val row = JSONObject()
                        columnNames.forEachIndexed { index, name ->
                            when (cursor.getType(index)) {
                                android.database.Cursor.FIELD_TYPE_NULL -> row.put(name, JSONObject.NULL)
                                android.database.Cursor.FIELD_TYPE_INTEGER -> row.put(name, cursor.getLong(index))
                                android.database.Cursor.FIELD_TYPE_FLOAT -> row.put(name, cursor.getDouble(index))
                                else -> row.put(name, cursor.getString(index))
                            }
                        }
                        array.put(row)
                        rows++
                    }
                }
                tables.put(table, array)
            }
        }
        root.put("tables", tables)
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }
        return rows
    }

    suspend fun readBackupInfo(file: File): BackupInfo? = runCatching {
        val root = JSONObject(file.readText(Charsets.UTF_8))
        if (root.optString("format") != "hisabnikash-backup") return null
        val tables = root.optJSONObject("tables") ?: return null
        val count = tables.keys().asSequence().sumOf { key ->
            tables.optJSONArray(key)?.length() ?: 0
        }
        BackupInfo(
            businessId = root.optLong("businessId"),
            exportedAt = root.optLong("exportedAt"),
            rowCount = count,
            version = root.optInt("version"),
            tables = tables.keys().asSequence().toList()
        )
    }.getOrNull()

    /**
     * Restores a backup. Must only be called after the user confirms, because
     * replaceExisting deletes the current business data first. The restore is
     * atomic: any failure rolls everything back.
     */
    suspend fun restoreFromFile(file: File, replaceExisting: Boolean): RestoreResult {
        val info = readBackupInfo(file) ?: return RestoreResult(false, "Invalid or unsupported backup file.")
        val root = JSONObject(file.readText(Charsets.UTF_8))
        val businessId = info.businessId
        val database = db.openHelper.writableDatabase
        return db.withTransaction {
            if (replaceExisting) {
                tableOrder.asReversed().forEach { table ->
                    if (tableAllowsDelete(table)) {
                        database.delete(table, "businessId = ?", arrayOf(businessId.toString()))
                    }
                }
            }
            var inserted = 0
            tableOrder.forEach { table ->
                val array = root.optJSONObject("tables")?.optJSONArray(table) ?: return@forEach
                for (i in 0 until array.length()) {
                    val row = array.getJSONObject(i)
                    val cv = ContentValues()
                    row.keys().forEach { key ->
                        val value = row.opt(key)
                        when (value) {
                            is JSONObject.NULL -> cv.putNull(key)
                            is Number -> cv.put(key, (value as Number).toLong())
                            else -> cv.put(key, value.toString())
                        }
                    }
                    // Preserve original ids where present so relations survive.
                    database.insert(table, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, cv)
                    inserted++
                }
            }
            // Foreign key integrity is enforced by SQLite during insert; a
            // failed parent insert aborts the whole transaction.
            RestoreResult(true, "Restored $inserted rows from backup.", inserted)
        }
    }

    private fun tableAllowsDelete(table: String): Boolean = true
}

data class BackupInfo(
    val businessId: Long,
    val exportedAt: Long,
    val rowCount: Int,
    val version: Int,
    val tables: List<String>
)

data class RestoreResult(
    val success: Boolean,
    val message: String,
    val rowsRestored: Int = 0
)
