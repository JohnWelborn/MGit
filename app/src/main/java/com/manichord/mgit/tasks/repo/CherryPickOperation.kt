package com.manichord.mgit.tasks.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.sheimi.sgit.database.models.Repo

object CherryPickOperation {
    suspend fun execute(repo: Repo, commitStr: String) = withContext(Dispatchers.IO) {
        val commit = repo.git.repository.resolve(commitStr)
        repo.git.cherryPick().include(commit).call()
    }
}
