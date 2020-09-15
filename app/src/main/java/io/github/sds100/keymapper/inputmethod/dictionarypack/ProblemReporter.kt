package io.github.sds100.keymapper.inputmethod.dictionarypack

/**
 * A simple interface to report problems.
 */
interface ProblemReporter {
    fun report(e: Exception?)
}