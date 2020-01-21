package org.dslul.openboard.inputmethodcommon

import android.graphics.drawable.Drawable

/**
 * InputMethodSettingsInterface is the interface for adding IME related preferences to
 * PreferenceActivity or PreferenceFragment.
 */
interface InputMethodSettingsInterface {
    /**
     * Sets the title for the input method settings category with a resource ID.
     * @param resId The resource ID of the title.
     */
    fun setInputMethodSettingsCategoryTitle(resId: Int)

    /**
     * Sets the title for the input method settings category with a CharSequence.
     * @param title The title for this preference.
     */
    fun setInputMethodSettingsCategoryTitle(title: CharSequence?)

    /**
     * Sets the title for the input method enabler preference for launching subtype enabler with a
     * resource ID.
     * @param resId The resource ID of the title.
     */
    fun setSubtypeEnablerTitle(resId: Int)

    /**
     * Sets the title for the input method enabler preference for launching subtype enabler with a
     * CharSequence.
     * @param title The title for this preference.
     */
    fun setSubtypeEnablerTitle(title: CharSequence?)

    /**
     * Sets the icon for the preference for launching subtype enabler with a resource ID.
     * @param resId The resource id of an optional icon for the preference.
     */
    fun setSubtypeEnablerIcon(resId: Int)

    /**
     * Sets the icon for the Preference for launching subtype enabler with a Drawable.
     * @param drawable The drawable of an optional icon for the preference.
     */
    fun setSubtypeEnablerIcon(drawable: Drawable?)
}