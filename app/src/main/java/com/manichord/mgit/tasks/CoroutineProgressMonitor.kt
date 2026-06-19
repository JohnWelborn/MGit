package com.manichord.mgit.tasks

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.trySendBlocking
import org.eclipse.jgit.lib.ProgressMonitor

class CoroutineProgressMonitor(
    private val channel: SendChannel<RepoProgress>,
    private val isCancelledCheck: () -> Boolean = { false }
) : ProgressMonitor {

    private var totalWork = 0
    private var workDone = 0
    private var lastPercent = -1
    private var title = ""

    override fun start(totalTasks: Int) {}

    override fun beginTask(t: String?, total: Int) {
        title = t ?: ""
        totalWork = total
        workDone = 0
        lastPercent = -1
        emit()
    }

    override fun update(completed: Int) {
        workDone += completed
        val pct = if (totalWork > 0) (100 * workDone / totalWork) else 0
        if (pct > lastPercent) { lastPercent = pct; emit() }
    }

    override fun endTask() {}
    override fun isCancelled() = isCancelledCheck()

    private fun emit() {
        val shown = workDone.coerceAtMost(totalWork)
        val pct = if (totalWork > 0) (100 * shown / totalWork) else 0
        channel.trySendBlocking(RepoProgress(title, "$pct%", "$shown/$totalWork", pct))
    }
}
