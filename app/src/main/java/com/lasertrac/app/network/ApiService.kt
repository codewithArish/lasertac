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
     * Example GET request. Kept for demonstration.
     */
    @GET("example/endpoint")
    suspend fun getSampleData(): Response<SampleApiResponse>

    /**
     * Performs a login request.
     * This now returns a Response object to allow for explicit error handling.
     */
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    /**
     * Performs a user registration request.
     * This now returns a Response object.
     */
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

}
