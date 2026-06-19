package com.manichord.mgit.tasks.repo

import com.manichord.mgit.common.get
import com.manichord.mgit.exceptions.NoSuchIndexPathException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.sheimi.sgit.R
import me.sheimi.sgit.database.models.Repo
import org.eclipse.jgit.lib.FileMode

object UpdateIndexOperation {

    fun calculateNewMode(executable: Boolean): Int =
        if (executable) 0b111101101 else 0b110100100

    suspend fun execute(repo: Repo, path: String, newMode: Int) = withContext(Dispatchers.IO) {
        val dircache = repo.git.repository.lockDirCache()
        try {
            val entry = dircache[path]
                ?: throw NoSuchIndexPathException(path)
            val oldMode = entry.fileMode
            entry.fileMode = FileMode.fromBits(newMode or (oldMode.bits or 0b111111111 xor 0b111111111))
        } finally {
            dircache.unlock()
        }
    }
}
