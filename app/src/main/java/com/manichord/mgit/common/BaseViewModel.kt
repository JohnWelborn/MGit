package com.manichord.mgit.common

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class BaseViewModel : ViewModel() {

    private val _error = MutableStateFlow<ErrorState?>(null)
    val error: StateFlow<ErrorState?> = _error.asStateFlow()

    open fun emitError(state: ErrorState) { _error.value = state }
    fun clearError() { _error.value = null }

    data class ErrorState(
        val exception: Throwable? = null,
        val errorRes: Int = 0,
        val titleRes: Int = 0
    )
}
