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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;
import android.widget.Toast;

import org.dslul.openboard.inputmethod.dictionarypack.DictionaryPackConstants;
import org.dslul.openboard.inputmethod.latin.R;
import org.dslul.openboard.inputmethod.latin.common.FileUtils;
import org.dslul.openboard.inputmethod.latin.makedict.DictionaryHeader;
import org.dslul.openboard.inputmethod.latin.userdictionary.UserDictionaryList;
import org.dslul.openboard.inputmethod.latin.userdictionary.UserDictionarySettings;
import org.dslul.openboard.inputmethod.latin.utils.DialogUtils;
import org.dslul.openboard.inputmethod.latin.utils.DictionaryInfoUtils;

import java.io.File;
import java.io.IOException;
import java.util.TreeSet;

/**
 * "Text correction" settings sub screen.
 *
 * This settings sub screen handles the following text correction preferences.
 * - Personal dictionary
 * - Add-on dictionaries
 * - Block offensive words
 * - Auto-correction
 * - Show correction suggestions
 * - Personalized suggestions
 * - Suggest Contact names
 * - Next-word suggestions
 */
public final class CorrectionSettingsFragment extends SubScreenFragment
    implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final boolean DBG_USE_INTERNAL_PERSONAL_DICTIONARY_SETTINGS = false;
    private static final boolean USE_INTERNAL_PERSONAL_DICTIONARY_SETTINGS =
            DBG_USE_INTERNAL_PERSONAL_DICTIONARY_SETTINGS;
    private static final int DICTIONARY_REQUEST_CODE = 96834;
    private static final String DICTIONARY_URL = "https://github.com/openboard-team/openboard/"; // TODO: update once it exists

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.prefs_screen_correction);

        final Context context = getActivity();
        final PackageManager pm = context.getPackageManager();

        final Preference editPersonalDictionary =
                findPreference(Settings.PREF_EDIT_PERSONAL_DICTIONARY);
        final Intent editPersonalDictionaryIntent = editPersonalDictionary.getIntent();
        final ResolveInfo ri = USE_INTERNAL_PERSONAL_DICTIONARY_SETTINGS ? null
                : pm.resolveActivity(
                        editPersonalDictionaryIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (ri == null) {
            overwriteUserDictionaryPreference(editPersonalDictionary);
        }

        // Ideally this would go to a preference screen where extra dictionaries can be managed
        // so user can check which dictionaries exists (internal and added), and also delete them.
        // But for now just adding new ones and replacing is ok.
        final Preference addDictionary = findPreference(Settings.PREF_ADD_DICTIONARY);
        if (addDictionary != null)
            addDictionary.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showAddDictionaryDialog();
                    return true;
                }
            });
    }

    private void overwriteUserDictionaryPreference(final Preference userDictionaryPreference) {
        final Activity activity = getActivity();
        final TreeSet<String> localeList = UserDictionaryList.getUserDictionaryLocalesSet(activity);
        if (null == localeList) {
            // The locale list is null if and only if the user dictionary service is
            // not present or disabled. In this case we need to remove the preference.
            getPreferenceScreen().removePreference(userDictionaryPreference);
        } else if (localeList.size() <= 1) {
            userDictionaryPreference.setFragment(UserDictionarySettings.class.getName());
            // If the size of localeList is 0, we don't set the locale parameter in the
            // extras. This will be interpreted by the UserDictionarySettings class as
            // meaning "the current locale".
            // Note that with the current code for UserDictionaryList#getUserDictionaryLocalesSet()
            // the locale list always has at least one element, since it always includes the current
            // locale explicitly. @see UserDictionaryList.getUserDictionaryLocalesSet().
            if (localeList.size() == 1) {
                final String locale = (String)localeList.toArray()[0];
                userDictionaryPreference.getExtras().putString("locale", locale);
            }
        } else {
            userDictionaryPreference.setFragment(UserDictionaryList.class.getName());
        }
    }

    private void showAddDictionaryDialog() {
        final String link = "<a href='" + DICTIONARY_URL + "'>" +
                getResources().getString(R.string.dictionary_selection_link_text) + "</a>";
        final Spanned message = Html.fromHtml(getResources().getString(R.string.dictionary_selection_message, link));
        final AlertDialog dialog = new AlertDialog.Builder(
                DialogUtils.getPlatformDialogThemeContext(getActivity()))
                .setTitle(R.string.dictionary_selection_title)
                .setMessage(message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.dictionary_selection_load_file, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                                .addCategory(Intent.CATEGORY_OPENABLE)
                                .setType("application/octet-stream");
                        startActivityForResult(intent, DICTIONARY_REQUEST_CODE);
                    }
                })
                .create();
        dialog.show();
        // make links in the HTML text work
        ((TextView) dialog.findViewById(android.R.id.message))
                .setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void onDictionaryFileSelected(int resultCode, Intent resultData) {
        if (resultCode != Activity.RESULT_OK || resultData == null) {
            onDictionaryLoadingError(R.string.dictionary_selection_error);
            return;
        }

        final Uri uri = resultData.getData();
        if (uri == null) {
            onDictionaryLoadingError(R.string.dictionary_selection_error);
            return;
        }

        final File cachedDictionaryFile = new File(getActivity().getCacheDir().getPath() + File.separator + "temp_dict");
        try {
            FileUtils.copyStreamToNewFile(
                    getActivity().getContentResolver().openInputStream(uri),
                    cachedDictionaryFile);
        } catch (IOException e) {
            onDictionaryLoadingError(R.string.dictionary_selection_error);
            return;
        }

        final DictionaryHeader newHeader = DictionaryInfoUtils.getDictionaryFileHeaderOrNull(cachedDictionaryFile, 0, cachedDictionaryFile.length());
        if (newHeader == null) {
            cachedDictionaryFile.delete();
            onDictionaryLoadingError(R.string.dictionary_selection_file_error);
            return;
        }

        final String dictFolder =
                DictionaryInfoUtils.getCacheDirectoryForLocale(newHeader.mLocaleString, getActivity());
        final File dictFile = new File(dictFolder + File.separator + DictionaryInfoUtils.MAIN_DICTIONARY_USER_FILE_NAME);
        if (dictFile.exists()) {
            final DictionaryHeader oldHeader =
                    DictionaryInfoUtils.getDictionaryFileHeaderOrNull(dictFile, 0, dictFile.length());
            if (oldHeader != null
                    && Integer.parseInt(oldHeader.mVersionString) > Integer.parseInt(newHeader.mVersionString)
                    && !shouldReplaceExistingUserDictionary()) {
                cachedDictionaryFile.delete();
                return;
            }
        }

        if (!cachedDictionaryFile.renameTo(dictFile)) {
            cachedDictionaryFile.delete();
            onDictionaryLoadingError(R.string.dictionary_selection_error);
            return;
        }

        // success, now remove internal dictionary file if it exists
        final File internalDictFile = new File(dictFolder + File.separator +
                DictionaryInfoUtils.MAIN_DICTIONARY_INTERNAL_FILE_NAME);
        if (internalDictFile.exists())
            internalDictFile.delete();

        // inform user about success
        final String successMessageForLocale = getResources()
                .getString(R.string.dictionary_selection_load_success, newHeader.mLocaleString);
        Toast.makeText(getActivity(), successMessageForLocale, Toast.LENGTH_SHORT).show();

        // inform LatinIME about new dictionary
        final Intent newDictBroadcast = new Intent(DictionaryPackConstants.NEW_DICTIONARY_INTENT_ACTION);
        getActivity().sendBroadcast(newDictBroadcast);
    }

    private void onDictionaryLoadingError(int resId) {
        // show error message... maybe better as dialog so user definitely notices?
        Toast.makeText(getActivity(), resId, Toast.LENGTH_LONG).show();
    }

    private boolean shouldReplaceExistingUserDictionary() {
        // TODO: show dialog, ask user whether existing file should be replaced
        // return true if yes, no otherwise (set .setCancelable(false) to avoid dismissing without the buttons!)
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == DICTIONARY_REQUEST_CODE)
            onDictionaryFileSelected(resultCode, resultData);
    }

}
