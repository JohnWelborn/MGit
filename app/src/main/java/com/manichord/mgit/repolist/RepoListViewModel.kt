package com.manichord.mgit.repolist

import com.manichord.mgit.common.BaseViewModel
import com.manichord.mgit.tasks.RepoProgress
import com.manichord.mgit.tasks.repo.PullOperation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import me.sheimi.sgit.database.models.Repo

class RepoListViewModel : BaseViewModel() {

    fun pullAll(repos: List<Repo>) {
        for (repo in repos) {
            viewModelScope.launch {
                runCatching {
                    val channel = Channel<RepoProgress>(Channel.CONFLATED)
                    PullOperation.execute(repo, "origin", false, channel) { null }
                    channel.close()
                }.onFailure { emitError(ErrorState(exception = it)) }
            }
        }
    }
}
