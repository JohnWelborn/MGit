package com.manichord.mgit.tasks.repo

import me.sheimi.sgit.database.models.Repo
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

internal fun applyCredentials(cmd: TransportCommand<*, *>, repo: Repo) {
    val username = repo.username
    val password = repo.password
    if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
        cmd.setCredentialsProvider(UsernamePasswordCredentialsProvider(username, password))
    }
}

internal fun isAuthError(e: Throwable): Boolean {
    val msg = e.message ?: return false
    return msg.contains("Auth fail") || msg.lowercase().contains("auth")
}
