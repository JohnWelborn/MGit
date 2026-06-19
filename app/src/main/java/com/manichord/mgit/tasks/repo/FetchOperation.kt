package com.manichord.mgit.tasks.repo

import com.manichord.mgit.tasks.Credentials
import com.manichord.mgit.tasks.CoroutineProgressMonitor
import com.manichord.mgit.tasks.RepoProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.withContext
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.ssh.SgitTransportCallback
import org.eclipse.jgit.api.errors.TransportException

object FetchOperation {

    suspend fun execute(
        repo: Repo,
        remotes: Array<String>,
        progressChannel: SendChannel<RepoProgress>,
        requestCredentials: suspend () -> Credentials?
    ) = withContext(Dispatchers.IO) {
        for (remote in remotes) {
            fetchRemote(repo, remote, progressChannel, requestCredentials)
        }
        repo.updateLatestCommitInfo()
    }

    private suspend fun fetchRemote(
        repo: Repo,
        remote: String,
        progressChannel: SendChannel<RepoProgress>,
        requestCredentials: suspend () -> Credentials?
    ) {
        while (true) {
            try {
                val cmd = repo.git.fetch()
                    .setProgressMonitor(CoroutineProgressMonitor(progressChannel))
                    .setTransportConfigCallback(SgitTransportCallback())
                    .setRemoveDeletedRefs(true)
                    .setRemote(remote)
                applyCredentials(cmd, repo)
                cmd.call()
                return
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
    }
}
