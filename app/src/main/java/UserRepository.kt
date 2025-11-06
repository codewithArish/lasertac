package com.lasertrac.app.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.google.gson.Gson
import com.lasertrac.app.db.UserDao
import com.lasertrac.app.db.UserEntity
import com.lasertrac.app.network.AuthResponse
import com.lasertrac.app.network.LoginRequest
import com.lasertrac.app.network.RegisterRequest
import com.lasertrac.app.network.RetrofitInstance
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class UserRepository(private val userDao: UserDao) {

    companion object {
        private const val TAG = "UserRepository"
    }

    // Get all users from local database
    fun getAllLocal(): LiveData<List<UserEntity>> = userDao.getAll()

    // Insert a single user locally
    suspend fun insertLocal(user: UserEntity) {
        try {
            userDao.insertUser(user)
        } catch (e: Exception) {
            Log.e(TAG, "insertLocal error", e)
        }
    }

    // Insert multiple users locally
    suspend fun insertAllLocal(users: List<UserEntity>) {
        try {
            userDao.insertAll(users)
        } catch (e: Exception) {
            Log.e(TAG, "insertAllLocal error", e)
        }
    }

    // Remote register call, bridged from a callback to a suspend function
    suspend fun registerRemote(name: String, email: String, password: String): AuthResponse {
        return suspendCancellableCoroutine { continuation ->
            val call = RetrofitInstance.api.register(RegisterRequest(name = name, email = email, pass = password))
            call.enqueue(object : Callback<AuthResponse> {
                override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.let { continuation.resume(it) }
                            ?: continuation.resumeWithException(Exception("Response body is null"))
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorResponse = try {
                            Gson().fromJson(errorBody, AuthResponse::class.java)
                        } catch (e: Exception) {
                            null
                        }
                        continuation.resumeWithException(
                            Exception(errorResponse?.message ?: "Registration failed: An unknown error occurred")
                        )
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    if (continuation.isCancelled) return
                    continuation.resumeWithException(t)
                }
            })

            continuation.invokeOnCancellation { call.cancel() }
        }
    }

    // Remote login call, bridged from a callback to a suspend function
    suspend fun loginRemote(email: String, password: String): AuthResponse {
        return suspendCancellableCoroutine { continuation ->
            val call = RetrofitInstance.api.login(LoginRequest(email = email, pass = password))
            call.enqueue(object : Callback<AuthResponse> {
                override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.let { continuation.resume(it) }
                            ?: continuation.resumeWithException(Exception("Response body is null"))
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorResponse = try {
                            Gson().fromJson(errorBody, AuthResponse::class.java)
                        } catch (e: Exception) {
                            null
                        }
                        continuation.resumeWithException(
                            Exception(errorResponse?.message ?: "Login failed: An unknown error occurred")
                        )
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    if (continuation.isCancelled) return
                    continuation.resumeWithException(t)
                }
            })

            continuation.invokeOnCancellation { call.cancel() }
        }
    }
}