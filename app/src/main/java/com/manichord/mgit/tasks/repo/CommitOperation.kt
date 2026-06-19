package com.manichord.mgit.tasks.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.sheimi.android.utils.Profile
import me.sheimi.sgit.MGitApplication
import me.sheimi.sgit.database.models.Repo

object CommitOperation {
    suspend fun execute(
        repo: Repo,
        stageAll: Boolean,
        isAmend: Boolean,
        msg: String,
        authorName: String?,
        authorEmail: String?
    ) = withContext(Dispatchers.IO) {
        doCommit(repo, stageAll, isAmend, msg, authorName, authorEmail)
    }

    suspend fun doCommit(
        repo: Repo,
        stageAll: Boolean,
        isAmend: Boolean,
        msg: String,
        authorName: String?,
        authorEmail: String?
    ) = withContext(Dispatchers.IO) {
        val context = MGitApplication.getContext()
        val config = repo.git.repository.config
        var committerName = config.getString("user", null, "name") ?: ""
        var committerEmail = config.getString("user", null, "email") ?: ""
        if (committerName.isEmpty()) committerName = Profile.getUsername(context)
        if (committerEmail.isEmpty()) committerEmail = Profile.getEmail(context)
        if (committerName.isEmpty() || committerEmail.isEmpty())
            throw Exception("Please set your name and email")
        if (msg.isEmpty())
            throw Exception("Please include a commit message")
        val cc = repo.git.commit()
            .setCommitter(committerName, committerEmail)
            .setAll(stageAll)
            .setAmend(isAmend)
            .setMessage(msg)
        if (authorName != null && authorEmail != null) cc.setAuthor(authorName, authorEmail)
        cc.call()
        repo.updateLatestCommitInfo()
    }
}
