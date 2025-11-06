package com.lasertrac.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.lasertrac.app.data.local.AppDatabase
import com.lasertrac.app.network.RetrofitInstance
import com.lasertrac.app.network.models.LoginRequest
import com.lasertrac.app.network.models.RegisterRequest
import com.lasertrac.app.repository.UserRepository
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository

    private val _authResult = MutableLiveData<Result<String>>()
    val authResult: LiveData<Result<String>> = _authResult

    init {
        val userDao = AppDatabase.getDatabase(application).userDao()
        userRepository = UserRepository(RetrofitInstance.api, userDao, application)
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            val result = userRepository.login(LoginRequest(email, pass))
            result.fold(
                onSuccess = { _authResult.postValue(Result.success(it.message)) },
                onFailure = { _authResult.postValue(Result.failure(it)) }
            )
        }
    }

    fun register(name: String, email: String, pass: String) {
        viewModelScope.launch {
            val result = userRepository.register(RegisterRequest(name, email, pass))
            result.fold(
                onSuccess = { _authResult.postValue(Result.success(it.message)) },
                onFailure = { _authResult.postValue(Result.failure(it)) }
            )
        }
    }
}