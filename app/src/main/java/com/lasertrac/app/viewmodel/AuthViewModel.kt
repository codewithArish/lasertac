package com.lasertrac.app.viewmodel

import android.app.Application
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lasertrac.app.HybridAuthApp
import com.lasertrac.app.db.AppDatabase
import com.lasertrac.app.network.RetrofitInstance
import com.lasertrac.app.network.models.LoginRequest
import com.lasertrac.app.network.models.RegisterRequest
import com.lasertrac.app.network.models.RegistrationStatusResponse
import com.lasertrac.app.repository.PendingVerificationException
import com.lasertrac.app.repository.UserRepository
import com.lasertrac.app.security.CredentialsManager
import com.lasertrac.app.utils.NetworkStatusTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository
    private val credentialsManager: CredentialsManager
    private val retrofitInstance: RetrofitInstance

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _isUserLoggedIn = MutableStateFlow(false)
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    val isOnline: StateFlow<Boolean> = HybridAuthApp.serverStatusChecker.isServerOnline

    init {
        val appDatabase = AppDatabase.getDatabase(application)
        credentialsManager = CredentialsManager(application)
        retrofitInstance = RetrofitInstance(application.applicationContext)
        userRepository = UserRepository(
            apiService = retrofitInstance.api,
            userDao = appDatabase.userDao(),
            context = application.applicationContext,
            networkTracker = NetworkStatusTracker(application),
            credentialsManager = credentialsManager
        )
        checkIfUserIsLoggedIn()
    }

    private fun checkIfUserIsLoggedIn() {
        _isUserLoggedIn.value = credentialsManager.isUserLoggedIn()
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val deviceId = Settings.Secure.getString(getApplication<Application>().contentResolver, Settings.Secure.ANDROID_ID)
            Log.d("AuthViewModel", "Device ID: $deviceId")
            val result = userRepository.login(LoginRequest(email, pass), deviceId)
            result.fold(
                onSuccess = {
                    credentialsManager.saveCredentials(email, pass, deviceId)
                    // The user is saved to the DB by the repository, so we just update the state
                    _loginState.value = LoginState.Success(it.message)
                    _isUserLoggedIn.value = true
                },
                onFailure = { 
                    _loginState.value = LoginState.Error(it.message ?: "An unknown error occurred")
                } 
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            credentialsManager.clearCredentials()
            _isUserLoggedIn.value = false
        }
    }

    fun register(name: String, email: String, pass: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val result = userRepository.register(RegisterRequest(name, email, pass))
            result.fold(
                onSuccess = { 
                    // This case should ideally not be hit with the current logic
                },
                onFailure = { error ->
                    if (error is PendingVerificationException) {
                        _loginState.value = LoginState.Success(error.message ?: "Verification required.")
                    } else {
                        _loginState.value = LoginState.Error(error.message ?: "An unknown error occurred")
                    }
                }
            )
        }
    }

    fun checkRegistrationStatus(email: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val response = retrofitInstance.api.checkRegistrationStatus(email)
                if (response.isSuccessful && response.body() != null) {
                    _loginState.value = LoginState.Success(response.body()!!.message)
                } else {
                    _loginState.value = LoginState.Error("Error checking status: ${response.message()}")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }
}
