package org.dslul.openboard.inputmethodcommon

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.preference.PreferenceFragment

/**
 * This is a helper class for an IME's settings preference fragment. It's recommended for every
 * IME to have its own settings preference fragment which inherits this class.
 */
abstract class InputMethodSettingsFragment : PreferenceFragment(), InputMethodSettingsInterface {
    private val mSettings = InputMethodSettingsImpl()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context: Context = activity
        preferenceScreen = preferenceManager.createPreferenceScreen(context)
        mSettings.init(context, preferenceScreen)
    }

    /**
     * {@inheritDoc}
     */
    override fun setInputMethodSettingsCategoryTitle(resId: Int) {
        mSettings.setInputMethodSettingsCategoryTitle(resId)
    }

    /**
     * {@inheritDoc}
     */
    override fun setInputMethodSettingsCategoryTitle(title: CharSequence?) {
        mSettings.setInputMethodSettingsCategoryTitle(title)
    }

    /**
     * {@inheritDoc}
     */
    override fun setSubtypeEnablerTitle(resId: Int) {
        mSettings.setSubtypeEnablerTitle(resId)
    }

    /**
     * {@inheritDoc}
     */
    override fun setSubtypeEnablerTitle(title: CharSequence?) {
        mSettings.setSubtypeEnablerTitle(title)
    }

    /**
     * {@inheritDoc}
     */
    override fun setSubtypeEnablerIcon(resId: Int) {
        mSettings.setSubtypeEnablerIcon(resId)
    }

    /**
     * {@inheritDoc}
     */
    override fun setSubtypeEnablerIcon(drawable: Drawable?) {
        mSettings.setSubtypeEnablerIcon(drawable)
    }

    /**
     * {@inheritDoc}
     */
    override fun onResume() {
        super.onResume()
        mSettings.updateSubtypeEnabler()
    }
}