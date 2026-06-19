package com.manichord.mgit.tasks.repo

import com.manichord.mgit.tasks.Credentials
import com.manichord.mgit.tasks.CoroutineProgressMonitor
import com.manichord.mgit.tasks.RepoProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.withContext
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.ssh.SgitTransportCallback
import org.eclipse.jgit.api.RebaseCommand
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.errors.TransportException

object PullOperation {

    suspend fun execute(
        repo: Repo,
        remote: String,
        forcePull: Boolean,
        progressChannel: SendChannel<RepoProgress>,
        requestCredentials: suspend () -> Credentials?
    ) = withContext(Dispatchers.IO) {
        val git = repo.git

        val branch: String?
        if (forcePull) {
            val fullBranch = git.repository.fullBranch
            require(fullBranch.startsWith("refs/heads/")) { "Not on a branch" }
            branch = fullBranch.removePrefix("refs/heads/")
            git.repository.writeMergeCommitMsg(null)
            git.repository.writeMergeHeads(null)
            try { git.rebase().setOperation(RebaseCommand.Operation.ABORT).call() } catch (_: Exception) {}
            git.reset().setMode(ResetCommand.ResetType.HARD).setRef("HEAD").call()
        } else {
            branch = null
        }

        while (true) {
            try {
                val cmd = git.pull()
                    .setRemote(remote)
                    .setProgressMonitor(CoroutineProgressMonitor(progressChannel))
                    .setTransportConfigCallback(SgitTransportCallback())
                applyCredentials(cmd, repo)
                cmd.call()
                break
            } catch (e: TransportException) {
                if (isAuthError(e)) {
                    val creds = requestCredentials() ?: throw e
                    repo.username = creds.username
                    repo.password = creds.password
                    if (creds.savePassword) repo.saveCredentials()
                } else {
                    throw e
                }
            }
        }

        if (forcePull && branch != null) {
            git.reset().setMode(ResetCommand.ResetType.HARD).setRef("$remote/$branch").call()
        }

        repo.updateLatestCommitInfo()
    }
}
