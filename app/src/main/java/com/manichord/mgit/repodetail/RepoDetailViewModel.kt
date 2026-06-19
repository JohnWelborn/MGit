package com.manichord.mgit.repodetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.manichord.mgit.common.BaseViewModel
import com.manichord.mgit.tasks.CredentialRequest
import com.manichord.mgit.tasks.Credentials
import com.manichord.mgit.tasks.RepoProgress
import com.manichord.mgit.tasks.repo.AddToStageOperation
import com.manichord.mgit.tasks.repo.CheckoutFileOperation
import com.manichord.mgit.tasks.repo.CheckoutOperation
import com.manichord.mgit.tasks.repo.CherryPickOperation
import com.manichord.mgit.tasks.repo.CommitOperation
import com.manichord.mgit.tasks.repo.DeleteFileOperation
import com.manichord.mgit.tasks.repo.DeleteType
import com.manichord.mgit.tasks.repo.FetchOperation
import com.manichord.mgit.tasks.repo.GetCommitsOperation
import com.manichord.mgit.tasks.repo.MergeOperation
import com.manichord.mgit.tasks.repo.PullOperation
import com.manichord.mgit.tasks.repo.PushOperation
import com.manichord.mgit.tasks.repo.RebaseOperation
import com.manichord.mgit.tasks.repo.ResetCommitOperation
import com.manichord.mgit.tasks.repo.StatusOperation
import com.manichord.mgit.tasks.repo.UpdateIndexOperation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import me.sheimi.sgit.R
import me.sheimi.sgit.database.models.Repo
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit

class RepoDetailViewModel : BaseViewModel() {

    private val _repoChanged = MutableLiveData<Unit>()
    val repoChanged: LiveData<Unit> = _repoChanged

    private val _filesChanged = MutableLiveData<Unit>()
    val filesChanged: LiveData<Unit> = _filesChanged

    private val _toast = MutableLiveData<Int>()
    val toast: LiveData<Int> = _toast

    private val _errorEvent = MutableLiveData<ErrorState?>()
    val errorEvent: LiveData<ErrorState?> = _errorEvent

    private val _checkedOutName = MutableLiveData<String?>()
    val checkedOutName: LiveData<String?> = _checkedOutName

    private val _statusText = MutableLiveData<String>()
    val statusText: LiveData<String> = _statusText

    private val _commits = MutableLiveData<List<RevCommit>>()
    val commits: LiveData<List<RevCommit>> = _commits

    private val _progress = MutableLiveData<RepoProgress?>()
    val progress: LiveData<RepoProgress?> = _progress

    private val _pushResultMsg = MutableLiveData<String?>()
    val pushResultMsg: LiveData<String?> = _pushResultMsg

    private val _credentialRequest = MutableLiveData<CredentialRequest?>()
    val credentialRequest: LiveData<CredentialRequest?> = _credentialRequest

    override fun emitError(state: ErrorState) {
        super.emitError(state)
        _errorEvent.postValue(state)
    }

    fun clearPushResultMsg() { _pushResultMsg.value = null }
    fun clearCredentialRequest() { _credentialRequest.value = null }

    private suspend fun requestCredentials(): Credentials? {
        val deferred = CompletableDeferred<Credentials?>()
        _credentialRequest.postValue(CredentialRequest(deferred))
        return deferred.await()
    }

    private fun launchRemoteOp(
        initMsg: String,
        block: suspend (SendChannel<RepoProgress>, suspend () -> Credentials?) -> Unit,
        onSuccess: () -> Unit = { _repoChanged.value = Unit }
    ) {
        viewModelScope.launch {
            val channel = Channel<RepoProgress>(Channel.CONFLATED)
            _progress.postValue(RepoProgress(initMsg, "", "", 0))
            val progressJob = launch {
                for (p in channel) { _progress.postValue(p) }
            }
            runCatching {
                block(channel, ::requestCredentials)
            }.also {
                channel.close()
                progressJob.join()
                _progress.postValue(null)
            }.onSuccess { onSuccess() }
             .onFailure { emitError(ErrorState(exception = it)) }
        }
    }

    fun fetch(repo: Repo, remotes: Array<String>, initMsg: String) {
        launchRemoteOp(initMsg, { channel, creds ->
            FetchOperation.execute(repo, remotes, channel, creds)
        })
    }

    fun pull(repo: Repo, remote: String, forcePull: Boolean, initMsg: String) {
        launchRemoteOp(initMsg, { channel, creds ->
            PullOperation.execute(repo, remote, forcePull, channel, creds)
        })
    }

    fun push(repo: Repo, remote: String, pushAll: Boolean, forcePush: Boolean, initMsg: String) {
        launchRemoteOp(initMsg, { channel, creds ->
            val resultMsg = PushOperation.execute(repo, remote, pushAll, forcePush, channel, creds)
            _pushResultMsg.postValue(resultMsg)
        })
    }

    fun addToStage(repo: Repo, filePattern: String) {
        viewModelScope.launch {
            runCatching { AddToStageOperation.execute(repo, filePattern) }
                .onSuccess { _toast.value = R.string.success_add_to_stage }
                .onFailure { emitError(ErrorState(exception = it)) }
        }
    }

    fun checkoutFile(repo: Repo, path: String) {
        viewModelScope.launch {
            runCatching { CheckoutFileOperation.execute(repo, path) }
                .onSuccess { _toast.value = R.string.success_checkout_file }
                .onFailure { emitError(ErrorState(exception = it)) }
        }
    }

    fun deleteFile(repo: Repo, filePattern: String, type: DeleteType) {
        viewModelScope.launch {
            runCatching { DeleteFileOperation.execute(repo, filePattern, type) }
                .onSuccess { _filesChanged.value = Unit }
                .onFailure { emitError(ErrorState(exception = it)) }
        }
    }

    fun rebase(repo: Repo, upstream: String) {
        viewModelScope.launch {
            runCatching { RebaseOperation.execute(repo, upstream) }
                .onSuccess { _repoChanged.value = Unit; _toast.value = R.string.success_rebase }
                .onFailure { emitError(ErrorState(exception = it)) }
        }
    }

    fun cherryPick(repo: Repo, commitStr: String) {
        viewModelScope.launch {
            runCatching { CherryPickOperation.execute(repo, commitStr) }
                .onSuccess { _repoChanged.value = Unit; _toast.value = R.string.success_cherry_pick }
                .onFailure { emitError(ErrorState(exception = it)) }
        }
    }

    fun resetCommit(repo: Repo) {
        viewModelScope.launch {
            runCatching { ResetCommitOperation.execute(repo) }
                .onSuccess { _repoChanged.value = Unit; _toast.value = R.string.success_reset }
                .onFailure { e ->
                    val errorRes = (e as? ResetCommitOperation.ResetException)?.errorRes ?: 0
                    emitError(ErrorState(exception = e, errorRes = errorRes))
                }
        }
    }

    fun updateIndex(repo: Repo, path: String, newMode: Int) {
        viewModelScope.launch {
            runCatching { UpdateIndexOperation.execute(repo, path, newMode) }
                .onFailure { emitError(ErrorState(exception = it)) }
        }
    }

    fun loadStatus(repo: Repo) {
        viewModelScope.launch {
            runCatching { StatusOperation.execute(repo) }
                .onSuccess { _statusText.postValue(it) }
                .onFailure { emitError(ErrorState(exception = it)) }
        }
    }

    fun loadCommits(repo: Repo, file: String?) {
        viewModelScope.launch {
            runCatching { GetCommitsOperation.execute(repo, file) }
                .onSuccess { _commits.postValue(it) }
                .onFailure { emitError(ErrorState(exception = it)) }
        }
    }

    fun checkout(repo: Repo, commitName: String?, newBranch: String?, displayName: String) {
        viewModelScope.launch {
            runCatching { CheckoutOperation.execute(repo, commitName, newBranch) }
                .onSuccess { _checkedOutName.value = displayName }
                .onFailure { emitError(ErrorState(exception = it)) }
        }
    }

    fun merge(repo: Repo, commit: Ref, ffMode: MergeCommand.FastForwardMode, autoCommit: Boolean) {
        viewModelScope.launch {
            runCatching { MergeOperation.execute(repo, commit, ffMode, autoCommit) }
                .onSuccess { _repoChanged.value = Unit }
                .onFailure { emitError(ErrorState(exception = it)) }
        }
    }

    fun commit(repo: Repo, msg: String, isAmend: Boolean, stageAll: Boolean, authorName: String?, authorEmail: String?) {
        viewModelScope.launch {
            runCatching { CommitOperation.execute(repo, stageAll, isAmend, msg, authorName, authorEmail) }
                .onSuccess { _repoChanged.value = Unit; _toast.value = R.string.success_commit }
                .onFailure { emitError(ErrorState(exception = it)) }
        }
    }
}
