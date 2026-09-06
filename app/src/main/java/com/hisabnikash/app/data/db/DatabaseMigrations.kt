package com.hisabnikash.app.data.db

import androidx.room.migration.Migration

/**
 * Central registry of every schema migration shipped by the app.
 *
 * Room is configured without `fallbackToDestructiveMigration()`, so upgrading
 * an existing install can never silently wipe user data. Every future schema
 * change MUST bump [AppDatabase.version] and add its `Migration` here. The
 * exported schema JSON under `app/schemas/` (kotlin 1.0.0 baseline) is the
 * reference for writing and testing each migration — keep a copy per shipped
 * version so upgrades remain verifiable.
 */
object DatabaseMigrations {

    /** All migrations, from version 1 (initial release) to the latest schema. */
    val ALL: Array<Migration> = emptyArray()
}
