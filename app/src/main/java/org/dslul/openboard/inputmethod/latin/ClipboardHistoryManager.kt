package org.dslul.openboard.inputmethod.latin

import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import org.dslul.openboard.inputmethod.compat.ClipboardManagerCompat
import org.dslul.openboard.inputmethod.latin.utils.JsonUtils
import java.io.File
import java.lang.Exception
import java.util.*

class ClipboardHistoryManager(
        private val latinIME: LatinIME
) : ClipboardManager.OnPrimaryClipChangedListener {

    private lateinit var pinnedHistoryClipsFile: File
    private lateinit var clipboardManager: ClipboardManager
    private val historyEntries: MutableList<ClipboardHistoryEntry>
    private var onHistoryChangeListener: OnHistoryChangeListener? = null

    fun onCreate() {
        pinnedHistoryClipsFile = File(latinIME.filesDir, PINNED_CLIPS_DATA_FILE_NAME)
        clipboardManager = latinIME.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        fetchPrimaryClip()
        clipboardManager.addPrimaryClipChangedListener(this)
        startLoadPinnedClipsFromDisk()
    }

    fun onPinnedClipsAvailable(pinnedClips: List<ClipboardHistoryEntry>) {
        historyEntries.addAll(pinnedClips)
        sortHistoryEntries()
        if (onHistoryChangeListener != null) {
            pinnedClips.forEach {
                onHistoryChangeListener?.onClipboardHistoryEntryAdded(historyEntries.indexOf(it))
            }
        }
    }

    fun onDestroy() {
        clipboardManager.removePrimaryClipChangedListener(this)
    }

    override fun onPrimaryClipChanged() = fetchPrimaryClip()

    private fun fetchPrimaryClip() {
        val clipData = clipboardManager.primaryClip ?: return
        if (clipData.itemCount == 0) return
        clipData.getItemAt(0)?.let { clipItem ->
            // Starting from API 30, onPrimaryClipChanged() can be called multiple times
            // for the same clip. We can identify clips with their timestamps since API 26.
            // We use that to prevent unwanted duplicates.
            val id = ClipboardManagerCompat.getClipTimestamp(clipData)?.also { stamp ->
                if (historyEntries.any { it.id == stamp }) return
            } ?: System.currentTimeMillis()

            val content = clipItem.coerceToText(latinIME)
            if (TextUtils.isEmpty(content)) return

            val entry = ClipboardHistoryEntry(id, content)
            historyEntries.add(entry)
            sortHistoryEntries()
            val at = historyEntries.indexOf(entry)
            onHistoryChangeListener?.onClipboardHistoryEntryAdded(at)
        }
    }

    fun toggleClipPinned(clipId: Long) {
        val from = historyEntries.indexOfFirst { it.id == clipId }
        val historyEntry = historyEntries[from].apply {
            id = System.currentTimeMillis()
            isPinned = !isPinned
        }
        sortHistoryEntries()
        val to = historyEntries.indexOf(historyEntry)
        onHistoryChangeListener?.onClipboardHistoryEntryMoved(from, to)
        startSavePinnedClipsToDisk()
    }

    fun clearHistory() {
        ClipboardManagerCompat.clearPrimaryClip(clipboardManager)
        val pos = historyEntries.indexOfFirst { !it.isPinned }
        val count = historyEntries.count { !it.isPinned }
        historyEntries.removeAll { !it.isPinned }
        if (onHistoryChangeListener != null) {
            onHistoryChangeListener?.onClipboardHistoryEntriesRemoved(pos, count)
        }
    }

    private fun sortHistoryEntries() {
        historyEntries.sort()
    }

    fun getHistorySize() = historyEntries.size

    fun getHistoryEntry(position: Int) = historyEntries[position]

    fun getHistoryEntryContent(id: Long) = historyEntries.first { it.id == id }

    fun setHistoryChangeListener(l: OnHistoryChangeListener?) {
        onHistoryChangeListener = l
    }

    private fun startLoadPinnedClipsFromDisk() {
        object : Thread("$TAG-load") {
            override fun run() {
                loadFromDisk()
            }
        }.start()
    }

    private fun loadFromDisk() {
        // Debugging
        if (pinnedHistoryClipsFile.exists() && !pinnedHistoryClipsFile.canRead()) {
            Log.w(TAG, "Attempt to read pinned clips file $pinnedHistoryClipsFile without permission")
        }
        var list = emptyList<ClipboardHistoryEntry>()
        try {
            if (pinnedHistoryClipsFile.exists()) {
                val bytes = Base64.decode(pinnedHistoryClipsFile.readText(), Base64.DEFAULT)
                list = JsonUtils.jsonBytesToHistoryEntryList(bytes)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't retrieve $pinnedHistoryClipsFile content", e)
        }
        latinIME.mHandler.postUpdateClipboardPinnedClips(list)
    }

    private fun startSavePinnedClipsToDisk() {
        val localCopy = historyEntries.filter { it.isPinned }.map { it.copy() }
        object : Thread("$TAG-save") {
            override fun run() {
                saveToDisk(localCopy)
            }
        }.start()
    }

    private fun saveToDisk(list: List<ClipboardHistoryEntry>) {
        // Debugging
        if (pinnedHistoryClipsFile.exists() && !pinnedHistoryClipsFile.canWrite()) {
            Log.w(TAG, "Attempt to write pinned clips file $pinnedHistoryClipsFile without permission")
        }
        try {
            pinnedHistoryClipsFile.createNewFile()
            val jsonStr = JsonUtils.historyEntryListToJsonStr(list)
            if (!TextUtils.isEmpty(jsonStr)) {
                val rawText = Base64.encodeToString(jsonStr.encodeToByteArray(), Base64.DEFAULT)
                pinnedHistoryClipsFile.writeText(rawText)
            } else {
                pinnedHistoryClipsFile.writeText("")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't write to $pinnedHistoryClipsFile", e)
        }
    }

    interface OnHistoryChangeListener {
        fun onClipboardHistoryEntryAdded(at: Int)
        fun onClipboardHistoryEntriesRemoved(pos: Int, count: Int)
        fun onClipboardHistoryEntryMoved(from: Int, to: Int)
    }

    companion object {
        const val PINNED_CLIPS_DATA_FILE_NAME = "pinned_clips.data"
        const val TAG = "ClipboardHistoryManager"
    }

    init {
        historyEntries = LinkedList()
    }
}