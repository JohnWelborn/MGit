package com.manichord.mgit.tasks

sealed class RepoTaskResult {
    object Success : RepoTaskResult()
    data class Error(val exception: Throwable?, val errorRes: Int = 0) : RepoTaskResult()
    object Cancelled : RepoTaskResult()
}

data class RepoProgress(
    val title: String,
    val leftHint: String,
    val rightHint: String,
    val percent: Int
)
