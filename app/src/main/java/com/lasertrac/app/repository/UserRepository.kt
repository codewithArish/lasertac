package com.lasertrac.app.repository

import android.content.Context
import android.util.Log
import com.lasertrac.app.db.User
import com.lasertrac.app.db.UserDao
import com.lasertrac.app.network.ApiService
import com.lasertrac.app.network.models.AuthResponse
import com.lasertrac.app.network.models.LoginRequest
import com.lasertrac.app.network.models.RegisterRequest
import com.lasertrac.app.security.CredentialsManager
import com.lasertrac.app.utils.NetworkStatusTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException

// Custom exception to specifically signal that verification is needed.
class PendingVerificationException(message: String) : Exception(message)

class UserRepository(
    private val apiService: ApiService,
    private val userDao: UserDao,
    private val context: Context,
    private val networkTracker: NetworkStatusTracker,
    private val credentialsManager: CredentialsManager
) {

    suspend fun login(request: LoginRequest): Result<AuthResponse> = withContext(Dispatchers.IO) {
        if (!networkTracker.isOnline.first()) {
            return@withContext performOfflineLogin(request)
        }

        try {
            val response = apiService.login(request)
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                if (authResponse.status == "success") {
                    // Online login successful, save user data and credentials
                    authResponse.user?.let { userResponse ->
                        val user = User(
                            serverId = userResponse.id,
                            name = userResponse.name,
                            email = userResponse.email,
                            pass = request.pass, // Save password for offline access
                            isSynced = true
                        )
                        userDao.insertUser(user)
                        credentialsManager.saveCredentials(request.email, request.pass)
                    }
                    return@withContext Result.success(authResponse)
                } else {
                    // Server returned an error (e.g., "Account not verified")
                    return@withContext Result.failure(Exception(authResponse.message ?: "Login failed"))
                }
            }
            // Non-2xx server response
            return@withContext Result.failure(IOException("Login failed with server error: ${response.code()}"))
        } catch (e: IOException) {
            Log.e("UserRepository", "Login network exception. Falling back to offline.", e)
            return@withContext performOfflineLogin(request)
        }
    }

    private suspend fun performOfflineLogin(request: LoginRequest): Result<AuthResponse> {
        val user = userDao.getUserByEmail(request.email).first()
        return if (user != null && user.pass == request.pass) {
            Log.i("UserRepository", "Offline login successful for ${request.email}")
            // Create a successful AuthResponse for the ViewModel to consume
            Result.success(AuthResponse("success", "Logged in from offline cache", null))
        } else {
            Result.failure(Exception("Invalid credentials for offline login."))
        }
    }

    suspend fun register(request: RegisterRequest): Result<AuthResponse> = withContext(Dispatchers.IO) {
        if (!networkTracker.isOnline.first()) {
            // If offline, save locally and immediately return the pending verification state
            saveUserForSync(request)
            return@withContext Result.failure(PendingVerificationException("Registration queued. Will sync when online."))
        }

        try {
            val response = apiService.register(request)
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                if (authResponse.status == "success") {
                    // The server has accepted the registration. Now we must wait for verification.
                    // We throw a specific exception that the ViewModel will catch to show the dialog.
                    return@withContext Result.failure(PendingVerificationException(authResponse.message))
                } else {
                    // The server returned a specific error (e.g., "User already exists")
                    return@withContext Result.failure(Exception(authResponse.message))
                }
            }
            return@withContext Result.failure(IOException("Server responded with an error: ${response.code()}"))
        } catch (e: IOException) {
            Log.w("UserRepository", "Online registration failed, queueing user for offline sync.", e)
            saveUserForSync(request)
            return@withContext Result.failure(PendingVerificationException("Registration queued. Will sync when online."))
        }
    }

    private suspend fun saveUserForSync(request: RegisterRequest) {
        val user = User(
            name = request.name,
            email = request.email,
            pass = request.pass, // Save password for the SyncWorker to use
            isSynced = false
        )
        userDao.insertUser(user)
    }
}
