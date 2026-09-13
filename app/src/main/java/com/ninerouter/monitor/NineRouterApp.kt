package com.ninerouter.monitor

import android.app.Application
import androidx.work.*
import com.ninerouter.monitor.data.auth.SessionManager
import com.ninerouter.monitor.data.network.NineRouterApiClient
import com.ninerouter.monitor.data.repository.UsageRepository
import com.ninerouter.monitor.worker.UsageRefreshWorker
import java.util.concurrent.TimeUnit

class NineRouterApp : Application() {
    lateinit var sessionManager: SessionManager
        private set
    lateinit var apiClient: NineRouterApiClient
        private set
    lateinit var repository: UsageRepository
        private set

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(this)
        apiClient = NineRouterApiClient()
        repository = UsageRepository(this, apiClient, sessionManager)

        scheduleBackgroundSync()
    }

    private fun scheduleBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicSync = PeriodicWorkRequestBuilder<UsageRefreshWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "NineRouterUsageSync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSync
        )
    }
}
