package org.dslul.openboard.inputmethod.dictionarypack

import android.util.Log

/**
 * A very simple problem reporter.
 */
internal class LogProblemReporter(private val TAG: String) : ProblemReporter {
    override fun report(e: Exception?) {
        Log.e(TAG, "Reporting problem", e)
    }

}