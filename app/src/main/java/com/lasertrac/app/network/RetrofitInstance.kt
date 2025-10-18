package com.lasertrac.app.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton object to provide a configured Retrofit instance.
 */
object RetrofitInstance {

    // TODO: Replace with your actual base URL. It must end with a '/'.
    private const val BASE_URL = "https://api.example.com/"

    /**
     * A lazy-initialized Retrofit service instance.
     */
    val api: ApiService by lazy {
        // Create a logging interceptor to see request and response logs
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Configure OkHttp client with the logging interceptor
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        // Build the Retrofit instance
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create()) // Use Gson for JSON serialization/deserialization
            .build()
            .create(ApiService::class.java)
    }
}
