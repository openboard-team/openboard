package org.dslul.openboard.inputmethod.dictionarypack

import android.view.View
import java.util.*

/**
 * Helper class to maintain the interface state of word list preferences.
 *
 * This is necessary because the views are created on-demand by calling code. There are many
 * situations where views are renewed with little relation with user interaction. For example,
 * when scrolling, the view is reused so it doesn't keep its state, which means we need to keep
 * it separately. Also whenever the underlying dictionary list undergoes a change (for example,
 * update the metadata, or finish downloading) the whole list has to be thrown out and recreated
 * in case some dictionaries appeared, disappeared, changed states etc.
 */
class DictionaryListInterfaceState {
    internal class State {
        var mOpen = false
        var mStatus: Int = MetadataDbHelper.Companion.STATUS_UNKNOWN
    }

    private val mWordlistToState = HashMap<String, State>()
    private val mViewCache = ArrayList<View>()
    fun isOpen(wordlistId: String?): Boolean {
        val state = mWordlistToState[wordlistId] ?: return false
        return state.mOpen
    }

    fun getStatus(wordlistId: String?): Int {
        val state = mWordlistToState[wordlistId] ?: return MetadataDbHelper.Companion.STATUS_UNKNOWN
        return state.mStatus
    }

    fun setOpen(wordlistId: String, status: Int) {
        val newState: State
        val state = mWordlistToState[wordlistId]
        newState = state ?: State()
        newState.mOpen = true
        newState.mStatus = status
        mWordlistToState[wordlistId] = newState
    }

    fun closeAll() {
        for (state in mWordlistToState.values) {
            state.mOpen = false
        }
    }

    fun findFirstOrphanedView(): View? {
        for (v in mViewCache) {
            if (null == v.parent) return v
        }
        return null
    }

    fun addToCacheAndReturnView(view: View): View {
        mViewCache.add(view)
        return view
    }

    fun removeFromCache(view: View?) {
        mViewCache.remove(view)
    }
}