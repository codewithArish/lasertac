package com.lasertrac.app.network.models

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val user: UserResponse?
)