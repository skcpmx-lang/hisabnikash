package com.hisabnikash.app.data.container

import android.content.Context
import androidx.room.Room
import com.hisabnikash.app.data.db.AppDatabase
import com.hisabnikash.app.data.prefs.AppPreferences
import com.hisabnikash.app.data.repo.BackupRepository
import com.hisabnikash.app.data.repo.CatalogRepository
import com.hisabnikash.app.data.repo.DataHealthRepository
import com.hisabnikash.app.data.repo.FinanceRepository
import com.hisabnikash.app.data.repo.InsightsRepository
import com.hisabnikash.app.data.repo.NotificationRepository
import com.hisabnikash.app.data.repo.OrderRepository
import com.hisabnikash.app.data.repo.SearchRepository
import com.hisabnikash.app.data.repo.SecurityRepository
import com.hisabnikash.app.data.repo.WorkspaceRepository

/**
 * Simple manual DI container. Keeps the process-wide singletons in one place
 * without introducing a framework.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "hisabnikash.db"
    ).fallbackToDestructiveMigration()
        .build()

    val prefs: AppPreferences = AppPreferences(appContext)

    val workspaceRepository: WorkspaceRepository = WorkspaceRepository(database, prefs)
    val catalogRepository: CatalogRepository = CatalogRepository(database, workspaceRepository)
    val financeRepository: FinanceRepository = FinanceRepository(database)
    val orderRepository: OrderRepository = OrderRepository(database, workspaceRepository)
    val insightsRepository: InsightsRepository = InsightsRepository(database)
    val notificationRepository: NotificationRepository = NotificationRepository(database)
    val searchRepository: SearchRepository = SearchRepository(database)
    val securityRepository: SecurityRepository = SecurityRepository(appContext, prefs)
    val backupRepository: BackupRepository = BackupRepository(database)
    val dataHealthRepository: DataHealthRepository = DataHealthRepository(database)
}
