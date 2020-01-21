package org.dslul.openboard.inputmethod.dictionarypack

/**
 * A class to group constants for dictionary pack usage.
 *
 * This class only defines constants. It should not make any references to outside code as far as
 * possible, as it's used to separate cleanly the keyboard code from the dictionary pack code; this
 * is needed in particular to cleanly compile regression tests.
 */
object DictionaryPackConstants {
    /**
     * The root domain for the dictionary pack, upon which authorities and actions will append
     * their own distinctive strings.
     */
    private const val DICTIONARY_DOMAIN = "org.dslul.openboard.inputmethod.dictionarypack.aosp"
    /**
     * Authority for the ContentProvider protocol.
     */
// TODO: find some way to factorize this string with the one in the resources
    const val AUTHORITY = DICTIONARY_DOMAIN
    /**
     * The action of the intent for publishing that new dictionary data is available.
     */
// TODO: make this different across different packages. A suggested course of action is
// to use the package name inside this string.
// NOTE: The appended string should be uppercase like all other actions, but it's not for
// historical reasons.
    const val NEW_DICTIONARY_INTENT_ACTION = "$DICTIONARY_DOMAIN.newdict"
    /**
     * The action of the intent sent by the dictionary pack to ask for a client to make
     * itself known. This is used when the settings activity is brought up for a client the
     * dictionary pack does not know about.
     */
    const val UNKNOWN_DICTIONARY_PROVIDER_CLIENT = (DICTIONARY_DOMAIN
            + ".UNKNOWN_CLIENT")
    // In the above intents, the name of the string extra that contains the name of the client
// we want information about.
    const val DICTIONARY_PROVIDER_CLIENT_EXTRA = "client"
    /**
     * The action of the intent to tell the dictionary provider to update now.
     */
    const val UPDATE_NOW_INTENT_ACTION = (DICTIONARY_DOMAIN
            + ".UPDATE_NOW")
    /**
     * The intent action to inform the dictionary provider to initialize the db
     * and update now.
     */
    const val INIT_AND_UPDATE_NOW_INTENT_ACTION = (DICTIONARY_DOMAIN
            + ".INIT_AND_UPDATE_NOW")
}