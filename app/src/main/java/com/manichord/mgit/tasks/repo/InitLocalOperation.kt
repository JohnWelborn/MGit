package com.manichord.mgit.tasks.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.sheimi.sgit.database.models.Repo
import org.eclipse.jgit.api.Git

object InitLocalOperation {
    suspend fun execute(repo: Repo) = withContext(Dispatchers.IO) {
        Git.init().setDirectory(repo.dir).call()
    }
}
