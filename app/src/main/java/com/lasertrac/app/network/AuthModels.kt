package com.lasertrac.app.network

/**
 * Data class for the login request body.
 */
data class LoginRequest(
    val email: String,
    val pass: String // Assuming the API expects 'pass' instead of 'password'
)

/**
 * Data class for the registration request body.
 */
data class RegisterRequest(
    val email: String,
    val pass: String,
    val name: String
)

/**
 * A generic authentication response for both login and registration.
 * TODO: Adjust the fields to match your API's actual JSON response.
 */
data class AuthResponse(
    val status: String,
    val message: String,
    val user: User? // User data can be null if login fails
)

/**
 * Represents the user data returned by the API on successful authentication.
 */
data class User(
    val id: Int,
    val name: String,
    val email: String,
    val token: String // Often a JWT token is returned for subsequent authenticated requests
)
