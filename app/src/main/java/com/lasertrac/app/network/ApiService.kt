package com.lasertrac.app.network

import com.lasertrac.app.network.models.AuthResponse
import com.lasertrac.app.network.models.LoginRequest
import com.lasertrac.app.network.models.RegisterRequest
import com.lasertrac.app.network.models.RegistrationStatusResponse
import com.lasertrac.app.network.models.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    // Appending .php to match the exact filenames on the XAMPP server.
    @POST("register.php")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("login.php")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("users.php")
    suspend fun getUsers(): Response<List<UserResponse>>

    @GET("check_status.php")
    suspend fun checkRegistrationStatus(@Query("email") email: String): Response<RegistrationStatusResponse>
}
