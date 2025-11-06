package com.lasertrac.app.network.models

data class AuthResponse(
    val status: String,
    val message: String,
    val user: UserResponse?
)
