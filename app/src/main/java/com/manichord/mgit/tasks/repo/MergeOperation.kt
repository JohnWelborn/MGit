package com.manichord.mgit.tasks.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.sheimi.sgit.database.models.Repo
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.lib.Ref

object MergeOperation {
    suspend fun execute(
        repo: Repo,
        commit: Ref,
        ffMode: MergeCommand.FastForwardMode,
        autoCommit: Boolean
    ) = withContext(Dispatchers.IO) {
        repo.git.merge().include(commit).setFastForward(ffMode).call()
        if (autoCommit) {
            val b1 = repo.branchName
            val b2 = commit.name
            val msg = if (b1 == null) "Merge branch '${Repo.getCommitDisplayName(b2)}'"
                      else "Merge branch '${Repo.getCommitDisplayName(b2)}' into ${Repo.getCommitDisplayName(b1)}"
            CommitOperation.doCommit(repo, false, false, msg, null, null)
        }
        repo.updateLatestCommitInfo()
    }
}
