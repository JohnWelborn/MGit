package com.manichord.mgit.tasks.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.sheimi.sgit.database.models.Repo

object StatusOperation {
    suspend fun execute(repo: Repo): String = withContext(Dispatchers.IO) {
        val status = repo.git.status().call()
        if (!status.hasUncommittedChanges() && status.isClean) {
            return@withContext "Nothing to commit, working directory clean"
        }
        buildString {
            appendStatusSet("Added files:", status.added)
            appendStatusSet("Changed files:", status.changed)
            appendStatusSet("Removed files:", status.removed)
            appendStatusSet("Missing files:", status.missing)
            appendStatusSet("Modified files:", status.modified)
            appendStatusSet("Conflicting files:", status.conflicting)
            appendStatusSet("Untracked files:", status.untracked)
        }
    }

    private fun StringBuilder.appendStatusSet(type: String, files: Set<String>) {
        if (files.isEmpty()) return
        append(type).append("\n\n")
        files.forEach { append('\t').append(it).append('\n') }
        append("\n")
    }
}
