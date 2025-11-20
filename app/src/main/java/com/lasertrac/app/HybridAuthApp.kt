package com.lasertrac.app

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lasertrac.app.utils.ServerStatusChecker
import com.lasertrac.app.worker.SyncRegistrationWorker
import java.util.concurrent.TimeUnit

class HybridAuthApp : Application() {

    companion object {
        lateinit var serverStatusChecker: ServerStatusChecker
            private set
    }

    override fun onCreate() {
        super.onCreate()
        serverStatusChecker = ServerStatusChecker("10.0.2.2", 80)
        serverStatusChecker.start()

        setupRecurringSync()
    }

    private fun setupRecurringSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncRegistrationWorker>(
            15, TimeUnit.MINUTES
        )
        .setConstraints(constraints)
        .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "sync_registrations",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    override fun onTerminate() {
        super.onTerminate()
        serverStatusChecker.stop()
    }
}
