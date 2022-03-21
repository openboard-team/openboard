/*
 * Copyright (C) 2012 The Android Open Source Project
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
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.view.inputmethod.InputMethodSubtype;

import org.dslul.openboard.inputmethod.dictionarypack.DictionaryPackConstants;
import org.dslul.openboard.inputmethod.latin.BuildConfig;
import org.dslul.openboard.inputmethod.latin.R;
import org.dslul.openboard.inputmethod.latin.RichInputMethodManager;
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

public final class SecondaryLocaleSettingsFragment extends SubScreenFragment {
    private RichInputMethodManager mRichImm;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RichInputMethodManager.init(getActivity());
        mRichImm = RichInputMethodManager.getInstance();
        addPreferencesFromResource(R.xml.additional_subtype_settings);
        resetKeyboardLocales();
    }

    private void resetKeyboardLocales() {
        mRichImm.refreshSubtypeCaches();
        getPreferenceScreen().removeAll();
        final Context context = getActivity();
        List<InputMethodSubtype> subtypes = mRichImm.getMyEnabledInputMethodSubtypeList(false);

        for (InputMethodSubtype subtype : subtypes) {
            final Locale secondaryLocale = Settings.getSecondaryLocale(getSharedPreferences(), subtype.getLocale());
            final Preference pref = new Preference(context);
            pref.setTitle(subtype.getDisplayName(context, BuildConfig.APPLICATION_ID, context.getApplicationInfo()));
            if (secondaryLocale != null)
                pref.setSummary(secondaryLocale.getDisplayLanguage(getResources().getConfiguration().locale));

            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showSecondaryLocaleDialog(subtype.getLocale().toLowerCase(Locale.ENGLISH), subtype.isAsciiCapable());
                    return true;
                }
            });
            getPreferenceScreen().addPreference(pref);
        }

    }

    private void showSecondaryLocaleDialog(String mainLocale, boolean asciiCapable) {
        final List<String> locales = new ArrayList<>(getAvailableDictionaryLocales(mainLocale, asciiCapable));
        final AlertDialog.Builder builder = new AlertDialog.Builder(
                DialogUtils.getPlatformDialogThemeContext(getActivity()))
                .setTitle(R.string.language_selection_title)
                .setPositiveButton(android.R.string.ok, null);

        if (locales.isEmpty()) {
            builder.setMessage(R.string.no_secondary_locales)
                    .show();
            return;
        }

        // add "no secondary language" option
        locales.add(getResources().getString(R.string.secondary_locale_none));

        final Locale displayLocale = getResources().getConfiguration().locale;
        final CharSequence[] titles = locales.toArray(new CharSequence[0]);
        for (int i = 0; i < titles.length - 1 ; i++) {
            final Locale loc = LocaleUtils.constructLocaleFromString(titles[i].toString());
            titles[i] = loc.getDisplayLanguage(displayLocale);
        }

        Locale currentSecondaryLocale = Settings.getSecondaryLocale(getSharedPreferences(), mainLocale);
        int checkedItem;
        if (currentSecondaryLocale == null)
            checkedItem = locales.size() - 1;
        else
            checkedItem = locales.indexOf(currentSecondaryLocale.toString());

        builder.setSingleChoiceItems(titles, checkedItem, (dialogInterface, i) -> {
            String locale = locales.get(i);
            if (i == locales.size() - 1)
                locale = "";
            final Set<String> encodedLocales = new HashSet<>();
            boolean updated = false;
            for (String encodedLocale : getSharedPreferences().getStringSet(Settings.PREF_SECONDARY_LOCALES, new HashSet<>())) {
                String[] locs = encodedLocale.split("§");
                if (locs.length == 2 && locs[0].equals(mainLocale)) {
                    if (!locale.isEmpty())
                        encodedLocales.add(mainLocale + "§" + locale);
                    updated = true;
                } else {
                    encodedLocales.add(encodedLocale);
                }
            }
            if (!updated)
                encodedLocales.add(mainLocale + "§" + locale);
            getSharedPreferences().edit().putStringSet(Settings.PREF_SECONDARY_LOCALES, encodedLocales).apply();
            final Intent newDictBroadcast = new Intent(DictionaryPackConstants.NEW_DICTIONARY_INTENT_ACTION);
            getActivity().sendBroadcast(newDictBroadcast);
            resetKeyboardLocales();
        });

        builder.show();
    }

    // get locales with same script as main locale, but different language
    private Set<String> getAvailableDictionaryLocales(String mainLocale, boolean asciiCapable) {
        final Locale mainL = LocaleUtils.constructLocaleFromString(mainLocale);
        final int mainScript;
        if (asciiCapable)
            mainScript = ScriptUtils.SCRIPT_LATIN;
        else
            mainScript = ScriptUtils.getScriptFromSpellCheckerLocale(mainL);
        // TODO: does this really return latin for persian? might need that huge map from somewhere github

        final Set<String> locales = new HashSet<>();

        // TODO: also get locales from assets
        //   need merged "compress" PR
        final File[] directoryList = DictionaryInfoUtils.getCachedDirectoryList(getActivity());
        for (File directory : directoryList) {
            if (!directory.isDirectory()) continue;
            final String dirLocale =
                    DictionaryInfoUtils.getWordListIdFromFileName(directory.getName());
            if (dirLocale.equals(mainLocale)) continue;
            final Locale locale = LocaleUtils.constructLocaleFromString(dirLocale);
            if (locale.getLanguage().equals(mainL.getLanguage())) continue;
            int localeScript = ScriptUtils.getScriptFromSpellCheckerLocale(locale);
            if (localeScript != mainScript) continue;
            locales.add(locale.toString());
        }
        return locales;
    }

}
