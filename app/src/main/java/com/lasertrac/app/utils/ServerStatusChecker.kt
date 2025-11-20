package com.lasertrac.app.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class ServerStatusChecker(private val host: String, private val port: Int) {

    private val _isServerOnline = MutableStateFlow(false)
    val isServerOnline: StateFlow<Boolean> = _isServerOnline.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var monitoringJob: Job? = null

    fun start() {
        if (monitoringJob?.isActive == true) return // Already running
        monitoringJob = scope.launch {
            while (isActive) {
                val isReachable = isServerReachable()
                if (_isServerOnline.value != isReachable) {
                    _isServerOnline.value = isReachable
                    Log.d("ServerStatusChecker", "Server is now ${if (isReachable) "ONLINE" else "OFFLINE"}")
                }
                delay(5000) // Check every 5 seconds
            }
        }
    }

    fun stop() {
        monitoringJob?.cancel()
    }

    private fun isServerReachable(): Boolean {
        return try {
            // Try to open a socket connection to the server. This is a lightweight way to check reachability.
            Socket().use { it.connect(InetSocketAddress(host, port), 1500) }
            true
        } catch (e: IOException) {
            false // Could not connect
        }
    }
}
