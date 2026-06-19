package com.manichord.mgit.tasks.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.sheimi.sgit.database.models.Repo

object CheckoutFileOperation {
    suspend fun execute(repo: Repo, path: String) = withContext(Dispatchers.IO) {
        repo.git.checkout().addPath(path).call()
    }
}
