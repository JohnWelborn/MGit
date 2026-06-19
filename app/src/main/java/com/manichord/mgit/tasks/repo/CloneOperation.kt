package com.manichord.mgit.tasks.repo

import com.manichord.mgit.tasks.Credentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.sheimi.android.utils.Profile
import me.sheimi.sgit.database.RepoContract
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.ssh.SgitTransportCallback
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.InvalidRemoteException
import org.eclipse.jgit.api.errors.JGitInternalException
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.errors.NotSupportedException
import org.eclipse.jgit.lib.ProgressMonitor
import java.util.Locale

object CloneOperation {

    suspend fun execute(
        initialRepo: Repo,
        cloneRecursive: Boolean,
        statusName: String,
        requestCredentials: suspend () -> Credentials?
    ) = withContext(Dispatchers.IO) {
        var currentRepo = initialRepo
        while (true) {
            try {
                val cmd = Git.cloneRepository()
                    .setURI(currentRepo.remoteURL)
                    .setCloneAllBranches(true)
                    .setProgressMonitor(CloneProgressMonitor(currentRepo))
                    .setTransportConfigCallback(SgitTransportCallback())
                    .setDirectory(currentRepo.dir)
                    .setCloneSubmodules(cloneRecursive)
                applyCredentials(cmd, currentRepo)
                cmd.call()
                Profile.setLastCloneSuccess()
                currentRepo.updateLatestCommitInfo()
                currentRepo.updateStatus(RepoContract.REPO_STATUS_NULL)
                return@withContext
            } catch (e: TransportException) {
                currentRepo.deleteRepoSync()
                Profile.setLastCloneFailed(currentRepo)
                if (isAuthError(e)) {
                    val creds = requestCredentials() ?: throw e
                    currentRepo = Repo.createRepo(currentRepo.localPath, currentRepo.remoteURL, statusName)
                    currentRepo.username = creds.username
                    currentRepo.password = creds.password
                    if (creds.savePassword) currentRepo.saveCredentials()
                } else {
                    throw e
                }
            } catch (e: JGitInternalException) {
                currentRepo.deleteRepoSync()
                if (e.cause is NotSupportedException) throw InvalidRemoteException(e.message)
                throw e
            } catch (e: GitAPIException) {
                currentRepo.deleteRepoSync()
                throw e
            } catch (e: Throwable) {
                currentRepo.deleteRepoSync()
                throw e
            }
        }
    }

    private class CloneProgressMonitor(private val repo: Repo) : ProgressMonitor {
        private var totalWork = 0
        private var workDone = 0
        private var lastProgress = 0
        private var title: String? = null

        private fun publishProgress() {
            val statusPrefix = if (title != null) "${title} ... " else ""
            val percent = if (totalWork != 0) {
                val p = 100 * workDone / totalWork
                if (p - lastProgress < 1) return
                lastProgress = p
                String.format(Locale.getDefault(), "(%d%%)", p)
            } else "0%"
            repo.updateStatus(statusPrefix + percent)
        }

        override fun start(totalTasks: Int) { publishProgress() }
        override fun beginTask(t: String?, total: Int) {
            totalWork = total; workDone = 0; lastProgress = 0; title = t; publishProgress()
        }
        override fun update(completed: Int) { workDone += completed; publishProgress() }
        override fun endTask() {}
        override fun isCancelled() = false
    }
}
