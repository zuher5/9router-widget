package com.ninerouter.monitor.worker

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ninerouter.monitor.NineRouterApp
import com.ninerouter.monitor.widget.NineRouterMediumWidget
import com.ninerouter.monitor.widget.NineRouterSmallWidget

class UsageRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? NineRouterApp ?: return Result.failure()
        val repo = app.repository

        return try {
            val result = repo.fetchUsageStats("7d")
            if (result.isSuccess) {
                // Update Glance widgets dengan data terbaru dari cache
                NineRouterSmallWidget().updateAll(applicationContext)
                NineRouterMediumWidget().updateAll(applicationContext)
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
