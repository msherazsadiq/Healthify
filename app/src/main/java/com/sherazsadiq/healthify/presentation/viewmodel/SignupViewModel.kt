package com.sherazsadiq.healthify.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherazsadiq.healthify.data.FirebaseRepository
import com.sherazsadiq.healthify.utils.ResultState
import kotlinx.coroutines.launch

class SignupViewModel : ViewModel() {

    private val firebaseRepository = FirebaseRepository()

    private val _signupResult = MutableLiveData<ResultState<Boolean>>()
    val signupResult: LiveData<ResultState<Boolean>> = _signupResult

    fun signup(email: String, password: String) {
        _signupResult.value = ResultState.Loading

        viewModelScope.launch {
            val result = firebaseRepository.signup(email, password)
            _signupResult.value = result
        }
    }
}
