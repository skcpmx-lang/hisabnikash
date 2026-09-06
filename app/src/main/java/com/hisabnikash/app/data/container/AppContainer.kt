package com.hisabnikash.app.data.container

import android.content.Context
import androidx.room.Room
import com.hisabnikash.app.data.db.AppDatabase

/**
 * Simple manual DI container. Keeps the process-wide singletons in one place
 * without introducing a framework.
 */
class AppContainer(context: Context) {

    val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "hisabnikash.db"
    ).fallbackToDestructiveMigration()
        .build()
}
