package com.lasertrac.app

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.lasertrac.app.network.AuthResponse
import com.lasertrac.app.network.LoginRequest
import com.lasertrac.app.network.RegisterRequest
import com.lasertrac.app.network.RetrofitInstance
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onNavigateToCreateAccount: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val isFormValid by remember { derivedStateOf { email.isNotBlank() && password.isNotBlank() } }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Login", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                isError = errorMessage != null
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                    }
                },
                isError = errorMessage != null
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val response = RetrofitInstance.api.login(LoginRequest(email, password))
                                if (response.isSuccessful) {
                                    val authResponse = response.body()
                                    if (authResponse?.status == "success") {
                                        Log.d("Login", "Login successful: ${authResponse.user}")
                                        onLoginSuccess()
                                    } else {
                                        errorMessage = authResponse?.message ?: "Login failed with unknown server error."
                                    }
                                } else {
                                    val errorBody = response.errorBody()?.string()
                                    val errorResponse = try { Gson().fromJson(errorBody, AuthResponse::class.java) } catch (e: Exception) { null }
                                    errorMessage = errorResponse?.message ?: "An unexpected error occurred."
                                    Log.e("Login", "API Error: $errorBody")
                                }
                            } catch (e: Exception) {
                                errorMessage = "Could not connect to server: ${e.message}"
                                Log.e("Login", "Network/Conversion Error", e)
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isFormValid && !isLoading
                ) {
                    Text("Login")
                }
            }

            TextButton(onClick = { if (!isLoading) onNavigateToCreateAccount() }) {
                Text("Don't have an account? Create one")
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun CreateAccountScreen(onAccountCreated: () -> Unit, onNavigateBackToLogin: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val isFormValid by remember { derivedStateOf { name.isNotBlank() && email.isNotBlank() && password.length >= 6 } }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Create Account", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it }, label = { Text("Password (min 6 chars)") }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                    }
                },
                isError = errorMessage != null
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                             try {
                                val response = RetrofitInstance.api.register(RegisterRequest(email, password, name))
                                if (response.isSuccessful) {
                                    val authResponse = response.body()
                                     if (authResponse?.status == "success") {
                                        Log.d("Register", "Registration successful for: $email")
                                        onAccountCreated()
                                    } else {
                                        errorMessage = authResponse?.message ?: "Registration failed with unknown server error."
                                    }
                                } else {
                                    val errorBody = response.errorBody()?.string()
                                    val errorResponse = try { Gson().fromJson(errorBody, AuthResponse::class.java) } catch (ex: Exception) { null }
                                    errorMessage = errorResponse?.message ?: "An unexpected error occurred."
                                    Log.e("Register", "API Error: $errorBody")
                                }
                            } catch (e: Exception) {
                                errorMessage = "Could not connect to server: ${e.message}"
                                Log.e("Register", "Network/Conversion Error", e)
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isFormValid && !isLoading
                ) {
                    Text("Create Account")
                }
            }
            TextButton(onClick = { if(!isLoading) onNavigateBackToLogin() }) {
                Text("Already have an account? Login")
            }
            errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
