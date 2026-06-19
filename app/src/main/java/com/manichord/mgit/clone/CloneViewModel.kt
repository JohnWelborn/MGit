package com.manichord.mgit.clone

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.manichord.mgit.tasks.CredentialRequest
import com.manichord.mgit.tasks.Credentials
import com.manichord.mgit.tasks.repo.CloneOperation
import com.manichord.mgit.tasks.repo.InitLocalOperation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.sheimi.sgit.MGitApplication
import me.sheimi.sgit.R
import me.sheimi.sgit.database.RepoContract
import me.sheimi.sgit.database.models.Repo
import timber.log.Timber

class CloneViewModel(application: Application) : AndroidViewModel(application) {

    var remoteUrl: String = ""
        set(value) {
            field = value
            localRepoName.value = stripGitExtension(stripUrlFromRepo(remoteUrl))
        }

    val localRepoName: MutableLiveData<String> = MutableLiveData()
    var cloneRecursively: Boolean = false
    val initLocal: MutableLiveData<Boolean> = MutableLiveData()

    var remoteUrlError: MutableLiveData<String?> = MutableLiveData()
    var localRepoNameError: MutableLiveData<String?> = MutableLiveData()

    val visible: MutableLiveData<Boolean> = MutableLiveData()

    val credentialRequest: MutableLiveData<CredentialRequest?> = MutableLiveData()
    val cloneError: MutableLiveData<Throwable?> = MutableLiveData()

    init {
        visible.value = false
        initLocal.value = false
    }

    fun show(show: Boolean) {
        visible.value = show
    }

    fun clearCredentialRequest() { credentialRequest.value = null }
    fun clearCloneError() { cloneError.value = null }

    private suspend fun requestCredentials(): Credentials? {
        val deferred = CompletableDeferred<Credentials?>()
        credentialRequest.postValue(CredentialRequest(deferred))
        return deferred.await()
    }

    fun cloneRepo() {
        if (initLocal.value == true) {
            Timber.d("INIT LOCAL %s", localRepoName.value)
            initLocalRepo()
        } else {
            Timber.d("CLONE REPO %s %s [%b]", localRepoName.value, remoteUrl, cloneRecursively)
            val repo = Repo.createRepo(localRepoName.value, remoteUrl, "")
            show(false)
            remoteUrl = ""
            startClone(repo, cloneRecursively, "")
        }
    }

    fun startClone(repo: Repo, cloneRecursive: Boolean, statusName: String) {
        viewModelScope.launch {
            runCatching {
                CloneOperation.execute(repo, cloneRecursive, statusName, ::requestCredentials)
            }.onFailure { e ->
                Timber.e(e, "Clone failed")
                cloneError.postValue(e)
            }
        }
    }

    fun validate(): Boolean {
        return if (initLocal.value == true) {
            validateLocalName(localRepoName.value as String)
        } else validateRemoteUrl(remoteUrl) && validateLocalName(localRepoName.value as String)
    }

    fun initLocalRepo() {
        val repo = Repo.createRepo(localRepoName.value, "local repository", "")
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    InitLocalOperation.execute(repo)
                    repo.updateLatestCommitInfo()
                    repo.updateStatus(RepoContract.REPO_STATUS_NULL)
                }
            }.onFailure { e ->
                Timber.e(e, "Failed to init local repo")
                withContext(Dispatchers.IO) { repo.deleteRepoSync() }
            }
        }
    }

    private fun stripUrlFromRepo(remoteUrl: String): String {
        val lastSlash = remoteUrl.lastIndexOf("/")
        return if (lastSlash != -1) remoteUrl.substring(lastSlash + 1) else remoteUrl
    }

    private fun stripGitExtension(remoteUrl: String): String {
        val extension = remoteUrl.indexOf(".git")
        return if (extension != -1) remoteUrl.substring(0, extension) else remoteUrl
    }

    private fun validateRemoteUrl(remoteUrl: String): Boolean {
        remoteUrlError.value = null
        if (remoteUrl.isBlank()) {
            remoteUrlError.value = getApplication<MGitApplication>().getString(R.string.alert_remoteurl_required)
            return false
        }
        return true
    }

    private fun validateLocalName(localName: String): Boolean {
        localRepoNameError.value = null
        if (localName.isBlank()) {
            localRepoNameError.value = getApplication<MGitApplication>().getString(R.string.alert_localpath_required)
            return false
        }
        if (localName.contains("/")) {
            localRepoNameError.value = getApplication<MGitApplication>().getString(R.string.alert_localpath_format)
            return false
        }
        val prefsHelper = (getApplication<MGitApplication>()).prefenceHelper
        val file = Repo.getDir(prefsHelper, localName)
        if (file.exists()) {
            localRepoNameError.value = getApplication<MGitApplication>().getString(R.string.alert_localpath_repo_exists)
            return false
        }
        return true
    }
}
