package org.dslul.openboard.inputmethodcommon

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.preference.PreferenceActivity

/**
 * This is a helper class for an IME's settings preference activity. It's recommended for every
 * IME to have its own settings preference activity which inherits this class.
 */
abstract class InputMethodSettingsActivity : PreferenceActivity(), InputMethodSettingsInterface {
    private val mSettings = InputMethodSettingsImpl()
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceScreen = preferenceManager.createPreferenceScreen(this)
        mSettings.init(this, preferenceScreen)
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
    public override fun onResume() {
        super.onResume()
        mSettings.updateSubtypeEnabler()
    }
}