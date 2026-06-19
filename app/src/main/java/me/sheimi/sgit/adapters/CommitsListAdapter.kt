package me.sheimi.sgit.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.sheimi.android.activities.SheimiFragmentActivity
import me.sheimi.android.utils.BasicFunctions
import me.sheimi.sgit.R
import me.sheimi.sgit.database.models.Repo
import org.eclipse.jgit.revwalk.RevCommit
import java.text.DateFormat

class CommitsListAdapter(
    private val mContext: Context,
    private val mChosenItems: Set<Int>,
    private val mRepo: Repo,
    private val mFile: String?
) : BaseAdapter() {

    private val mCommitDateFormatter: DateFormat =
        android.text.format.DateFormat.getDateFormat(mContext)

    private var mFilter: String? = null
    private var mAll: ArrayList<RevCommit> = ArrayList()
    private var mFiltered: ArrayList<Int>? = null
    private var mPosted: Int = 0
    private var mIsIncomplete: Boolean = false

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var filterJob: Job? = null

    private fun isAccepted(commit: RevCommit, filter: String): Boolean {
        if (commit.id.toString().startsWith("commit ${filter.lowercase()}")) return true
        /* Search raw buffer first (fast path); it may match non-message fields, so
           confirm in the parsed fields only if the raw buffer matches. */
        val raw = String(commit.rawBuffer)
        if (!raw.contains(filter)) return false
        return commit.authorIdent.name.contains(filter)
            || commit.authorIdent.emailAddress.contains(filter)
            || commit.committerIdent.name.contains(filter)
            || commit.committerIdent.emailAddress.contains(filter)
            || commit.fullMessage.contains(filter)
    }

    private fun stopFiltering() {
        filterJob?.cancel()
        filterJob = null
    }

    private fun doFiltering() {
        mFiltered = null
        val currentFilter = mFilter
        if (currentFilter != null) {
            val newFiltered = ArrayList<Int>()
            mFiltered = newFiltered
            mPosted = 0
            mIsIncomplete = true
            filterJob = scope.launch {
                val snapshot = ArrayList(mAll)
                var i = 0
                // Show first results after 100 ms, then throttle to 1 s
                var nextPostNs = System.nanoTime() + 100_000_000L
                while (i < snapshot.size && isActive) {
                    val batchEnd = minOf(i + FILTER_CHUNK_SIZE, snapshot.size)
                    val batchAccepted = withContext(Dispatchers.Default) {
                        val results = ArrayList<Int>()
                        for (j in i until batchEnd) {
                            ensureActive()
                            if (isAccepted(snapshot[j], currentFilter)) results.add(j)
                        }
                        results
                    }
                    newFiltered.addAll(batchAccepted)
                    i = batchEnd
                    if (newFiltered.size != mPosted && System.nanoTime() > nextPostNs) {
                        mPosted = newFiltered.size
                        notifyDataSetChanged()
                        nextPostNs = System.nanoTime() + 1_000_000_000L
                    }
                }
                if (isActive) {
                    mPosted = newFiltered.size
                    mIsIncomplete = false
                    notifyDataSetChanged()
                }
            }
        } else {
            notifyDataSetChanged()
        }
    }

    fun setFilter(query: String?) {
        stopFiltering()
        mFilter = query?.takeIf { it.isNotEmpty() }
        doFiltering()
    }

    override fun getCount(): Int {
        if (mFilter == null) return mAll.size
        if (mIsIncomplete) return mPosted + 1
        return mFiltered?.size ?: 0
    }

    override fun getItemId(position: Int): Long {
        if (mIsIncomplete && position >= mPosted) return -1
        if (mFilter == null) return position.toLong()
        return try { mFiltered!![position].toLong() } catch (_: Exception) { -1L }
    }

    override fun getItem(position: Int): RevCommit? {
        if (mIsIncomplete && position >= mPosted) return null
        return try {
            if (mFilter == null) mAll[position] else mAll[mFiltered!![position]]
        } catch (_: Exception) { null }
    }

    fun isProgressBar(position: Int): Boolean {
        if (mFilter == null) return position >= mAll.size
        if (mIsIncomplete) return position >= mPosted
        return position >= (mFiltered?.size ?: 0)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        if (isProgressBar(position)) {
            return ProgressBar(mContext, null, android.R.attr.progressBarStyleLarge)
        }
        val inflater = LayoutInflater.from(mContext)
        var holder = convertView?.tag as? CommitsListItemHolder
        val view: View
        if (holder == null) {
            view = inflater.inflate(R.layout.listitem_commits, parent, false)
            holder = CommitsListItemHolder(
                commitsIcon = view.findViewById(R.id.commitIcon),
                commitsTitle = view.findViewById(R.id.commitTitle),
                commitsMsg = view.findViewById(R.id.commitMsg),
                commitAuthor = view.findViewById(R.id.commitAuthor),
                commitTime = view.findViewById(R.id.commitTime)
            )
            view.tag = holder
        } else {
            view = convertView!!
        }
        val commit = getItem(position) ?: return view
        val person = commit.authorIdent

        holder.commitsTitle.text = Repo.getCommitDisplayName(commit.name)
        holder.commitAuthor.text = person.name
        holder.commitsMsg.text = commit.shortMessage
        holder.commitTime.text = mCommitDateFormatter.format(person.`when`)
        BasicFunctions.setAvatarImage(holder.commitsIcon, person.emailAddress)

        val colorResId = if (mChosenItems.contains(position)) R.color.pressed_sgit
                         else android.R.color.transparent
        if (mContext is SheimiFragmentActivity) {
            view.setBackgroundColor(mContext.resources.getColor(colorResId))
        }
        return view
    }

    fun clear() {
        stopFiltering()
        mAll = ArrayList()
        mFiltered = if (mFilter == null) null else ArrayList()
    }

    fun setCommits(commits: List<RevCommit>?) {
        if (commits != null) {
            stopFiltering()
            mAll = ArrayList(commits)
            doFiltering()
        }
    }

    fun close() {
        scope.cancel()
    }

    private data class CommitsListItemHolder(
        val commitsIcon: ImageView,
        val commitsTitle: TextView,
        val commitsMsg: TextView,
        val commitAuthor: TextView,
        val commitTime: TextView
    )

    companion object {
        private const val FILTER_CHUNK_SIZE = 100
    }
}
