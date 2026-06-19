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
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.RemoteRefUpdate

object PushOperation {

    suspend fun execute(
        repo: Repo,
        remote: String,
        pushAll: Boolean,
        forcePush: Boolean,
        progressChannel: SendChannel<RepoProgress>,
        requestCredentials: suspend () -> Credentials?
    ): String = withContext(Dispatchers.IO) {
        while (true) {
            try {
                val cmd = repo.git.push()
                    .setPushTags()
                    .setProgressMonitor(CoroutineProgressMonitor(progressChannel))
                    .setTransportConfigCallback(SgitTransportCallback())
                    .setRemote(remote)
                if (pushAll) {
                    cmd.setPushAll()
                } else {
                    cmd.setRefSpecs(RefSpec(repo.branchName))
                }
                if (forcePush) cmd.setForce(true)
                applyCredentials(cmd, repo)

                val result = cmd.call()
                val sb = StringBuilder()
                for (r in result) {
                    for (update in r.remoteUpdates) {
                        sb.append(formatUpdate(update))
                    }
                }
                return@withContext sb.toString()
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
        @Suppress("UNREACHABLE_CODE")
        ""
    }

    private fun formatUpdate(update: RemoteRefUpdate): String = when (update.status) {
        RemoteRefUpdate.Status.AWAITING_REPORT ->
            "[${update.remoteName}] Push process is awaiting update report from remote repository.\n"
        RemoteRefUpdate.Status.NON_EXISTING ->
            "[${update.remoteName}] Remote ref didn't exist.\n"
        RemoteRefUpdate.Status.NOT_ATTEMPTED ->
            "[${update.remoteName}] Push process hasn't yet attempted to update this ref.\n"
        RemoteRefUpdate.Status.OK ->
            "[${update.remoteName}] Success push to remote ref.\n"
        RemoteRefUpdate.Status.REJECTED_NODELETE ->
            "[${update.remoteName}] Remote ref update was rejected, because remote side doesn't support/allow deleting refs.\n"
        RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD ->
            "[${update.remoteName}] Remote ref update was rejected, as it would cause non fast-forward update.\n"
        RemoteRefUpdate.Status.REJECTED_OTHER_REASON -> {
            val reason = update.message
            if (reason.isNullOrEmpty()) "[${update.remoteName}] Remote ref update was rejected.\n"
            else "[${update.remoteName}] Remote ref update was rejected, because $reason.\n"
        }
        RemoteRefUpdate.Status.REJECTED_REMOTE_CHANGED ->
            "[${update.remoteName}] Remote ref update was rejected, because old object id on remote repository wasn't the same as defined expected old object.\n"
        RemoteRefUpdate.Status.UP_TO_DATE ->
            "[${update.remoteName}] remote ref is up to date\n"
        else -> ""
    }
}
