package io.github.sds100.keymapper.inputmethod.dictionarypack

/**
 * Exception thrown when the metadata for the dictionary does not comply to a known format.
 */
class BadFormatException : Exception {
    constructor() : super()
    constructor(message: String?) : super(message)
}