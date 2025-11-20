package com.lasertrac.app.viewmodel

import android.app.Application
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository
    private val credentialsManager: CredentialsManager
    private val retrofitInstance: RetrofitInstance

    // Holds the result for a SUCCESSFUL login only.
    private val _loginSuccess = MutableLiveData<Result<String>?>()
    val loginSuccess: LiveData<Result<String>?> = _loginSuccess

    // Holds the result for a registration that is PENDING verification.
    private val _registrationPending = MutableLiveData<Result<String>?>()
    val registrationPending: LiveData<Result<String>?> = _registrationPending

    // Holds the result of a status check.
    private val _registrationStatus = MutableLiveData<Result<RegistrationStatusResponse>?>()
    val registrationStatus: LiveData<Result<RegistrationStatusResponse>?> = _registrationStatus

    private val _isUserLoggedIn = MutableLiveData<Boolean>()
    val isUserLoggedIn: LiveData<Boolean> = _isUserLoggedIn

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
    }

    fun checkIfUserIsLoggedIn() {
        _isUserLoggedIn.value = credentialsManager.isUserLoggedIn()
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            val deviceId = Settings.Secure.getString(getApplication<Application>().contentResolver, Settings.Secure.ANDROID_ID)
            Log.d("AuthViewModel", "Device ID: $deviceId")
            val result = userRepository.login(LoginRequest(email, pass), deviceId)
            result.fold(
                onSuccess = {
                    credentialsManager.saveCredentials(email, pass, deviceId)
                    _loginSuccess.postValue(Result.success(it.message))
                },
                onFailure = { _loginSuccess.postValue(Result.failure(it)) } // Post failure to the same LiveData
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            credentialsManager.clearCredentials()
            _isUserLoggedIn.postValue(false)
        }
    }

    fun register(name: String, email: String, pass: String) {
        viewModelScope.launch {
            val result = userRepository.register(RegisterRequest(name, email, pass))
            // This logic is now much cleaner
            result.fold(
                onSuccess = { 
                    // This will never be called due to the new repository logic, but is here for completeness.
                },
                onFailure = { error ->
                    if (error is PendingVerificationException) {
                        // If it's our specific exception, notify the pending LiveData
                        _registrationPending.postValue(Result.success(error.message ?: "Verification required."))
                    } else {
                        // For all other errors (e.g., "User exists"), notify the login LiveData as a failure
                        _loginSuccess.postValue(Result.failure(error))
                    }
                }
            )
        }
    }

    fun checkRegistrationStatus(email: String) {
        viewModelScope.launch {
            try {
                val response = retrofitInstance.api.checkRegistrationStatus(email)
                if (response.isSuccessful && response.body() != null) {
                    _registrationStatus.postValue(Result.success(response.body()!!))
                } else {
                    _registrationStatus.postValue(Result.failure(Exception("Error checking status: ${response.message()}")))
                }
            } catch (e: Exception) {
                _registrationStatus.postValue(Result.failure(e))
            }
        }
    }

    fun resetAllEvents() {
        _loginSuccess.value = null
        _registrationPending.value = null
        _registrationStatus.value = null
    }
}