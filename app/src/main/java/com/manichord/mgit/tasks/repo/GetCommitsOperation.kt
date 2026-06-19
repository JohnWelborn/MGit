package com.manichord.mgit.tasks.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.sheimi.sgit.database.models.Repo
import org.eclipse.jgit.revwalk.RevCommit

object GetCommitsOperation {
    suspend fun execute(repo: Repo, file: String? = null): List<RevCommit> = withContext(Dispatchers.IO) {
        val cmd = repo.git.log()
        if (file != null) cmd.addPath(file)
        cmd.call().toList()
    }
}
