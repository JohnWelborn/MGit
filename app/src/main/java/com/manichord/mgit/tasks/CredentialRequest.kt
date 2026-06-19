package com.manichord.mgit.tasks

import kotlinx.coroutines.CompletableDeferred

data class Credentials(val username: String, val password: String, val savePassword: Boolean)

class CredentialRequest(private val deferred: CompletableDeferred<Credentials?>) {
    fun resolve(creds: Credentials?) = deferred.complete(creds)
}
