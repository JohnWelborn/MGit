package com.manichord.mgit.tasks.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.sheimi.sgit.database.models.Repo

object AddToStageOperation {
    suspend fun execute(repo: Repo, filePattern: String) = withContext(Dispatchers.IO) {
        repo.git.add().addFilepattern(filePattern).call()
    }
}
