package com.manichord.mgit.tasks.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.sheimi.sgit.database.models.Repo
import org.eclipse.jgit.api.CreateBranchCommand

object CheckoutOperation {
    suspend fun execute(repo: Repo, name: String?, newBranch: String?) = withContext(Dispatchers.IO) {
        if (name == null) {
            repo.git.checkout().setName(newBranch).setCreateBranch(true).call()
        } else when {
            Repo.COMMIT_TYPE_REMOTE == Repo.getCommitType(name) -> {
                val branch = if (newBranch.isNullOrEmpty()) Repo.getCommitName(name) else newBranch
                repo.git.checkout().setCreateBranch(true).setName(branch).setStartPoint(name).call()
                repo.git.branchCreate()
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                    .setStartPoint(name).setName(branch).setForce(true).call()
            }
            newBranch.isNullOrEmpty() -> repo.git.checkout().setName(name).call()
            else -> repo.git.checkout().setCreateBranch(true).setName(newBranch).setStartPoint(name).call()
        }
        repo.updateLatestCommitInfo()
    }
}
