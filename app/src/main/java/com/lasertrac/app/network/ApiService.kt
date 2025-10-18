package com.lasertrac.app.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Defines the API endpoints for your application's network requests.
 */
interface ApiService {

    /**
     * Example GET request. Replace 'example/endpoint' with your actual endpoint.
     */
    @GET("example/endpoint")
    suspend fun getSampleData(): Response<SampleApiResponse> // Assuming SampleApiResponse is still needed

    /**
     * Performs a login request.
     * TODO: Replace "login" with your actual login endpoint path.
     */
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    /**
     * Performs a user registration request.
     * TODO: Replace "register" with your actual registration endpoint path.
     */
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

}
