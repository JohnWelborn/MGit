package com.manichord.mgit.tasks.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.sheimi.sgit.database.models.Repo
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.dircache.DirCacheIterator
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import org.eclipse.jgit.treewalk.FileTreeIterator
import java.io.ByteArrayOutputStream

data class CommitDiffData(
    val entries: List<DiffEntry>,
    val diffs: List<String>,
    val commit: RevCommit?
)

object CommitDiffOperation {
    suspend fun execute(
        repo: Repo,
        oldCommit: String,
        newCommit: String,
        showDescription: Boolean
    ): CommitDiffData = withContext(Dispatchers.IO) {
        val jgitRepo = repo.git.repository
        val diffOutput = ByteArrayOutputStream()
        val formatter = DiffFormatter(diffOutput)
        formatter.setRepository(jgitRepo)

        val oldIter = if (repo.isInitialCommit(newCommit)) EmptyTreeIterator()
                      else getTreeIterator(jgitRepo, oldCommit)
        val newIter = getTreeIterator(jgitRepo, newCommit)
        val entries = formatter.scan(oldIter, newIter)

        val diffs = entries.map { entry ->
            diffOutput.reset()
            formatter.format(entry)
            formatter.flush()
            diffOutput.toString("UTF-8")
        }

        val revCommit = if (showDescription) {
            val id = jgitRepo.resolve(newCommit)
            repo.git.log().add(id).setMaxCount(1).call().firstOrNull()
        } else null

        CommitDiffData(entries, diffs, revCommit)
    }

    private fun getTreeIterator(repo: Repository, commit: String): AbstractTreeIterator {
        if (commit == "dircache") return DirCacheIterator(repo.readDirCache())
        if (commit == "filetree") return FileTreeIterator(repo)
        val treeId = repo.resolve("$commit^{tree}")
            ?: throw NullPointerException("Cannot resolve tree for $commit")
        val treeIter = CanonicalTreeParser()
        treeIter.reset(repo.newObjectReader(), treeId)
        return treeIter
    }
}
