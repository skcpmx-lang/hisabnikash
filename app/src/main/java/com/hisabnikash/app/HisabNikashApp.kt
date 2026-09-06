package com.hisabnikash.app

import android.app.Application
import com.hisabnikash.app.data.container.AppContainer

/**
 * Application entry point. Owns the app-wide dependency container so that the
 * single source of truth (Room database + preferences) lives for the whole process.
 */
class HisabNikashApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
