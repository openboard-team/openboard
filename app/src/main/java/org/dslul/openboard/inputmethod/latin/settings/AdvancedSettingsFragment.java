/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License.
 */

package org.dslul.openboard.inputmethod.latin.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.LocaleList;
import android.preference.Preference;
import android.util.ArraySet;
import android.util.Log;

import androidx.core.os.LocaleListCompat;

import org.dslul.openboard.inputmethod.dictionarypack.DictionaryPackConstants;
import org.dslul.openboard.inputmethod.latin.AudioAndHapticFeedbackManager;
import org.dslul.openboard.inputmethod.latin.R;
import org.dslul.openboard.inputmethod.latin.SystemBroadcastReceiver;
import org.dslul.openboard.inputmethod.latin.common.LocaleUtils;
import org.dslul.openboard.inputmethod.latin.utils.DialogUtils;
import org.dslul.openboard.inputmethod.latin.utils.DictionaryInfoUtils;
import org.dslul.openboard.inputmethod.latin.utils.ScriptUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * "Advanced" settings sub screen.
 *
 * This settings sub screen handles the following advanced preferences.
 * - Key popup dismiss delay
 * - Keypress vibration duration
 * - Keypress sound volume
 * - Show app icon
 * - Improve keyboard
 * - Debug settings
 */
public final class AdvancedSettingsFragment extends SubScreenFragment {
    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.prefs_screen_advanced);

        final Resources res = getResources();
        final Context context = getActivity();

        // When we are called from the Settings application but we are not already running, some
        // singleton and utility classes may not have been initialized.  We have to call
        // initialization method of these classes here. See {@link LatinIME#onCreate()}.
        AudioAndHapticFeedbackManager.init(context);

        final SharedPreferences prefs = getSharedPreferences();

        if (!Settings.isInternal(prefs)) {
            removePreference(Settings.SCREEN_DEBUG);
        }

        setupKeyLongpressTimeoutSettings();

        final Preference setSecondaryLocale = findPreference("pref_secondary_locale");
        if (setSecondaryLocale != null)
            setSecondaryLocale.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showSecondaryLocaleDialog();
                    return true;
                }
            });
    }


    private void setupKeyLongpressTimeoutSettings() {
        final SharedPreferences prefs = getSharedPreferences();
        final Resources res = getResources();
        final SeekBarDialogPreference pref = (SeekBarDialogPreference)findPreference(
                Settings.PREF_KEY_LONGPRESS_TIMEOUT);
        if (pref == null) {
            return;
        }
        pref.setInterface(new SeekBarDialogPreference.ValueProxy() {
            @Override
            public void writeValue(final int value, final String key) {
                prefs.edit().putInt(key, value).apply();
            }

            @Override
            public void writeDefaultValue(final String key) {
                prefs.edit().remove(key).apply();
            }

            @Override
            public int readValue(final String key) {
                return Settings.readKeyLongpressTimeout(prefs, res);
            }

            @Override
            public int readDefaultValue(final String key) {
                return Settings.readDefaultKeyLongpressTimeout(res);
            }

            @Override
            public String getValueText(final int value) {
                return res.getString(R.string.abbreviation_unit_milliseconds, value);
            }

            @Override
            public void feedbackValue(final int value) {}
        });
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {
        if (key.equals(Settings.PREF_SHOW_SETUP_WIZARD_ICON)) {
            SystemBroadcastReceiver.toggleAppIcon(getActivity());
        }
    }

    private void showSecondaryLocaleDialog() {
        // only latin for now
        final List<String> locales = new ArrayList<String>(getAvailableDictionaryLocalesForScript(ScriptUtils.SCRIPT_LATIN));
        final AlertDialog.Builder builder = new AlertDialog.Builder(
                DialogUtils.getPlatformDialogThemeContext(getActivity()))
                .setTitle(R.string.select_language)
                .setPositiveButton(android.R.string.ok, null);
        if (locales.isEmpty()) {
            builder.setMessage(R.string.no_secondary_locales)
                    .show();
            return;
        }
        locales.add(getResources().getString(R.string.secondary_locale_none));
        final CharSequence[] titles = locales.toArray(new CharSequence[0]);
        for (int i = 0; i < titles.length -1 ; i++) {
            titles[i] = LocaleUtils.constructLocaleFromString(titles[i].toString()).getDisplayLanguage();
        }
        Locale currentSecondaryLocale = Settings.getInstance().getCurrent().mSecondaryLocale;
        int checkedItem;
        if (currentSecondaryLocale == null)
            checkedItem = locales.size() - 1;
        else
            checkedItem = locales.indexOf(currentSecondaryLocale.toString());

        builder.setSingleChoiceItems(titles, checkedItem, (dialogInterface, i) -> {
            String locale = locales.get(i);
            if (locale.equals(getResources().getString(R.string.secondary_locale_none)))
                locale = "";
            getSharedPreferences().edit().putString(Settings.PREF_SECONDARY_LOCALE, locale).apply();
            final Intent newDictBroadcast = new Intent(DictionaryPackConstants.NEW_DICTIONARY_INTENT_ACTION);
            getActivity().sendBroadcast(newDictBroadcast);
        });

        builder.show();
    }

    private Set<String> getAvailableDictionaryLocalesForScript(int script) {
        final Set<String> locales = new HashSet<>();
        // TODO: get from assets
        //   need merged "compress" PR
        //   filter by script!
        final File[] directoryList = DictionaryInfoUtils.getCachedDirectoryList(getActivity());
        for (File directory : directoryList) {
            if (!directory.isDirectory()) continue;
            final String dirLocale =
                    DictionaryInfoUtils.getWordListIdFromFileName(directory.getName());
            final Locale locale = LocaleUtils.constructLocaleFromString(dirLocale);
            if (ScriptUtils.getScriptFromSpellCheckerLocale(locale) != script) continue;
            locales.add(locale.toString());
        }
        return locales;
    }
}
