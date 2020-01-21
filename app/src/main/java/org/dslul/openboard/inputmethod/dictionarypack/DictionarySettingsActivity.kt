package org.dslul.openboard.inputmethod.dictionarypack

import android.annotation.TargetApi
import android.content.Intent
import android.os.Build
import android.preference.PreferenceActivity
import org.dslul.openboard.inputmethod.latin.utils.FragmentUtils

/**
 * Preference screen.
 */
class DictionarySettingsActivity : PreferenceActivity() {

    override fun getIntent(): Intent {
        val modIntent = Intent(super.getIntent())
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, DEFAULT_FRAGMENT)
        modIntent.putExtra(EXTRA_NO_HEADERS, true)
        // Important note : the original intent should contain a String extra with the key
// DictionarySettingsFragment.DICT_SETTINGS_FRAGMENT_CLIENT_ID_ARGUMENT so that the
// fragment can know who the client is.
        return modIntent
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public override fun isValidFragment(fragmentName: String): Boolean {
        return FragmentUtils.isValidFragment(fragmentName)
    }

    companion object {
        private val DEFAULT_FRAGMENT = DictionarySettingsFragment::class.java.name
    }
}