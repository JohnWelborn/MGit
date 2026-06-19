package com.manichord.mgit.repodetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manichord.mgit.tasks.repo.CommitDiffData
import com.manichord.mgit.tasks.repo.CommitDiffOperation
import kotlinx.coroutines.launch
import me.sheimi.sgit.database.models.Repo

class CommitDiffViewModel : ViewModel() {

    private val _diffResult = MutableLiveData<CommitDiffData>()
    val diffResult: LiveData<CommitDiffData> = _diffResult

    private val _errorEvent = MutableLiveData<Throwable?>()
    val errorEvent: LiveData<Throwable?> = _errorEvent

    fun loadDiff(repo: Repo, oldCommit: String, newCommit: String, showDescription: Boolean) {
        viewModelScope.launch {
            runCatching { CommitDiffOperation.execute(repo, oldCommit, newCommit, showDescription) }
                .onSuccess { _diffResult.postValue(it) }
                .onFailure { _errorEvent.postValue(it) }
        }
    }
}
