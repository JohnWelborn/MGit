package com.manichord.mgit.branchchooser

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manichord.mgit.tasks.repo.CheckoutOperation
import kotlinx.coroutines.launch
import me.sheimi.sgit.database.models.Repo

class BranchChooserViewModel : ViewModel() {

    private val _checkoutDone = MutableLiveData<Boolean>()
    val checkoutDone: LiveData<Boolean> = _checkoutDone

    private val _errorEvent = MutableLiveData<Throwable?>()
    val errorEvent: LiveData<Throwable?> = _errorEvent

    fun checkout(repo: Repo, commitName: String?) {
        viewModelScope.launch {
            runCatching { CheckoutOperation.execute(repo, commitName, null) }
                .onSuccess { _checkoutDone.postValue(true) }
                .onFailure { _errorEvent.postValue(it) }
        }
    }
}
