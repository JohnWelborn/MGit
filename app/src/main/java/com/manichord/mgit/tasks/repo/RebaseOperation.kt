package com.manichord.mgit.tasks.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.sheimi.sgit.database.models.Repo

object RebaseOperation {
    suspend fun execute(repo: Repo, upstream: String) = withContext(Dispatchers.IO) {
        repo.git.rebase().setUpstream(upstream).call()
    }
}
