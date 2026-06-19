package com.manichord.mgit.tasks.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.sheimi.sgit.R
import me.sheimi.sgit.database.models.Repo
import org.eclipse.jgit.api.RebaseCommand
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.errors.WrongRepositoryStateException
import timber.log.Timber

object ResetCommitOperation {

    class ResetException(val errorRes: Int, cause: Throwable) : Exception(cause.message, cause)

    suspend fun execute(repo: Repo) = withContext(Dispatchers.IO) {
        repo.git.repository.writeMergeCommitMsg(null)
        repo.git.repository.writeMergeHeads(null)
        try {
            repo.git.rebase().setOperation(RebaseCommand.Operation.ABORT).call()
        } catch (e: WrongRepositoryStateException) {
            Timber.i(e, "Couldn't abort rebase while reset.")
        } catch (e: Exception) {
            throw ResetException(R.string.error_rebase_abort_failed_in_reset, e)
        }
        repo.git.reset().setMode(ResetCommand.ResetType.HARD).call()
    }
}
