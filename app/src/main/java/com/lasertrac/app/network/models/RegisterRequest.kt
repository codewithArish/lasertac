package com.lasertrac.app.network.models

data class RegisterRequest(
    val name: String,
    val email: String,
    val pass: String
)