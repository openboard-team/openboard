/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
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