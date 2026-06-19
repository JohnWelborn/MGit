package com.manichord.mgit.tasks.repo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.sheimi.sgit.database.RepoContract
import me.sheimi.sgit.database.models.Repo
import timber.log.Timber

class InitLocalViewModel : ViewModel() {

    fun startInitLocal(repo: Repo) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    InitLocalOperation.execute(repo)
                    repo.updateLatestCommitInfo()
                    repo.updateStatus(RepoContract.REPO_STATUS_NULL)
                }
            }.onFailure { e ->
                Timber.e(e, "Failed to init local repo")
                withContext(Dispatchers.IO) { repo.deleteRepoSync() }
            }
        }
    }
}
