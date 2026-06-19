package com.manichord.mgit.tasks.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.sheimi.android.utils.FsUtils
import me.sheimi.sgit.database.models.Repo

enum class DeleteType { DELETE, REMOVE_CACHED, REMOVE_FORCE }

object DeleteFileOperation {
    suspend fun execute(repo: Repo, filePattern: String, type: DeleteType) = withContext(Dispatchers.IO) {
        when (type) {
            DeleteType.DELETE -> FsUtils.deleteFile(FsUtils.joinPath(repo.dir, filePattern))
            DeleteType.REMOVE_CACHED -> repo.git.rm().setCached(true).addFilepattern(filePattern).call()
            DeleteType.REMOVE_FORCE -> repo.git.rm().addFilepattern(filePattern).call()
        }
    }
}
