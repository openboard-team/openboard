package org.dslul.openboard.inputmethod.latin.settings

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.Preference
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import org.dslul.openboard.inputmethod.dictionarypack.DictionaryPackConstants
import org.dslul.openboard.inputmethod.latin.BinaryDictionaryGetter
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.common.FileUtils
import org.dslul.openboard.inputmethod.latin.common.LocaleUtils
import org.dslul.openboard.inputmethod.latin.utils.DialogUtils
import org.dslul.openboard.inputmethod.latin.utils.DictionaryInfoUtils
import java.io.File
import java.io.IOException
import java.util.*

class DictionarySettingsFragment : SubScreenFragment() {

    // dict for which dialog is currently open (if any)
    private var currentDictLocale: Locale? = null
    private var currentDictState: Int? = null

    private val cachedDictionaryFile by lazy { File(activity.cacheDir.path + File.separator + "temp_dict") }
    private val currentDictExistsForUser get() = currentDictState == DICT_INTERNAL_AND_USER || currentDictState == DICT_USER_ONLY
    private val currentDictExistsInternal get() = currentDictState == DICT_INTERNAL_AND_USER || currentDictState == DICT_INTERNAL_ONLY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.additional_subtype_settings)
        reloadDictionaries()
        // + button to add dictionary
        setHasOptionsMenu(true)
        activity.actionBar?.setTitle(R.string.dictionary_settings_category)
    }

    // shows existing dictionaries as preferences
    private fun reloadDictionaries() {
        val screen = preferenceScreen ?: return
        screen.removeAll()
        val userDicts = mutableSetOf<Locale>()
        val internalDicts = mutableSetOf<Locale>()
        // get available dictionaries
        // cached (internal in use and user dicts)
        DictionaryInfoUtils.getCachedDirectoryList(activity)?.forEach { dir ->
            if (!dir.isDirectory)
                return@forEach
            dir.list()?.forEach {
                if (it.endsWith(USER_DICTIONARY_SUFFIX))
                    userDicts.add(dir.name.toLocale())
                else if (it.startsWith(DictionaryInfoUtils.MAIN_DICT_PREFIX))
                    internalDicts.add(dir.name.toLocale())
            }
        }
        // internal only
        BinaryDictionaryGetter.getAssetsDictionaryList(activity)?.forEach { dictFile ->
            BinaryDictionaryGetter.extractLocaleFromAssetsDictionaryFile(dictFile)?.let {
                internalDicts.add(it.toLocale())
            }
        }

        // first show user-added dictionaries
        userDicts.sortedBy { it.displayName() }.forEach { dict ->
            val pref = Preference(activity).apply {
                title = dict.displayName()
                setSummary(R.string.user_dictionary_summary)
                setOnPreferenceClickListener {
                    // open dialog for update or delete / reset
                    currentDictLocale = dict
                    currentDictState = if (internalDicts.contains(dict)) DICT_INTERNAL_AND_USER else DICT_USER_ONLY
                    showUpdateDialog()
                    true
                }
            }
            screen.addPreference(pref)
        }

        // TODO: only show if language is actually used?
        internalDicts.sortedBy { it.displayName() }.forEach { dict ->
            if (userDicts.contains(dict)) return@forEach // don't show a second time
            val pref = Preference(activity).apply {
                title = dict.displayName()
                setSummary(R.string.internal_dictionary_summary)
                setOnPreferenceClickListener {
                    // open dialog for update, maybe disabling if i can make it work?
                    currentDictLocale = dict
                    currentDictState = DICT_INTERNAL_ONLY
                    showUpdateDialog()
                    true
                }
            }
            screen.addPreference(pref)
        }
    }

    private fun showUpdateDialog() {
        // -1: adding new dict, don't know where it may exist
        // 0: user only -> offer delete
        // 1: internal only -> only update (and maybe later: disable)
        // 2: user and internal -> offer reset to internal

        if (currentDictState == null) return
        if (currentDictLocale == null && currentDictState != DICT_NEW)
            return

        val link = "<a href='$DICTIONARY_URL'>" +
                resources.getString(R.string.dictionary_link_text) + "</a>"
        val message = if (currentDictState == DICT_NEW)
            Html.fromHtml(resources.getString(R.string.add_new_dictionary, link))
        else
            Html.fromHtml(resources.getString(R.string.update_dictionary, link))
        val title = if (currentDictState == DICT_NEW) R.string.add_new_dictionary_title
            else R.string.dictionary_settings_category
        val updateButtonTitle = if (currentDictExistsForUser) R.string.update_dictionary_button
        else R.string.user_dict_settings_add_menu_title

        val builder = AlertDialog.Builder(DialogUtils.getPlatformDialogThemeContext(activity))
            .setNegativeButton(R.string.cancel, null)
            .setMessage(message)
            .setTitle(title)
            .setPositiveButton(updateButtonTitle) { _, _ ->
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/octet-stream")
                startActivityForResult(intent, DICTIONARY_REQUEST_CODE)
            }

        // allow removing dictionaries
        if (currentDictExistsForUser) {
            builder.setNeutralButton(if (currentDictExistsInternal) R.string.reset_dictionary else R.string.delete_dict) { _, _ ->
                AlertDialog.Builder(DialogUtils.getPlatformDialogThemeContext(activity))
                    .setTitle(R.string.remove_dictionary_title)
                    .setMessage(resources.getString(R.string.remove_dictionary_message, currentDictLocale?.displayName()))
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.delete_dict) { _,_ ->
                        currentDictLocale?.getUserDictFilenames()?.let { files ->
                            var parent: File? = null
                            files.forEach {
                                val f = File(it)
                                parent = f.parentFile
                                f.delete()
                            }
                            if (parent?.list()?.isEmpty() == true)
                                parent?.delete()
                        }
                        reloadDictionaries()
                    }
                    .show()
            }
        }

        val dialog = builder.create()
        dialog.show()
        // make links in the HTML text work
        (dialog.findViewById<View>(android.R.id.message) as TextView).movementMethod =
            LinkMovementMethod.getInstance()
    }

    // copied from CustomInputStyleSettingsFragment
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.add_style, menu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val value = TypedValue()
            activity.theme.resolveAttribute(android.R.attr.colorForeground, value, true)
            menu.findItem(R.id.action_add_style).icon?.setTint(value.data)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.action_add_style) {
            currentDictLocale = null
            currentDictState = DICT_NEW
            showUpdateDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == DICTIONARY_REQUEST_CODE) onDictionaryFileSelected(resultCode, resultData)
    }

    private fun onDictionaryFileSelected(resultCode: Int, resultData: Intent?) {
        if (resultCode != Activity.RESULT_OK || resultData == null) {
            onDictionaryLoadingError(R.string.dictionary_load_error.resString())
            return
        }
        val uri = resultData.data ?: return onDictionaryLoadingError(R.string.dictionary_load_error.resString())

        cachedDictionaryFile.delete()
        try {
            FileUtils.copyStreamToNewFile(
                activity.contentResolver.openInputStream(uri),
                cachedDictionaryFile
            )
        } catch (e: IOException) {
            onDictionaryLoadingError(R.string.dictionary_load_error.resString())
            return
        }

        val newHeader = DictionaryInfoUtils.getDictionaryFileHeaderOrNull(cachedDictionaryFile, 0, cachedDictionaryFile.length())
        if (newHeader == null) {
            cachedDictionaryFile.delete()
            onDictionaryLoadingError(R.string.dictionary_file_error.resString())
            return
        }
        val locale = newHeader.mLocaleString.toLocale()
        if (currentDictLocale != null && locale != currentDictLocale) {
            cachedDictionaryFile.delete()
            onDictionaryLoadingError(resources.getString(R.string.dictionary_file_wrong_locale, locale.displayName(), currentDictLocale?.displayName()))
            return
        }
        // idString is content of 'dictionary' key, in format <type>:<locale>
        val dictionaryType = newHeader.mIdString.substringBefore(":")

        val userDictFile = File(locale.getUserDictFilename(dictionaryType))
        // ask for user confirmation if it would be a version downgrade or if user pressed add new,
        //  but we already have a user dictionary for the same locale
        val shouldAskMessageId = if (userDictFile.exists()) {
            val oldHeader = DictionaryInfoUtils.getDictionaryFileHeaderOrNull(userDictFile, 0, userDictFile.length())
            if (oldHeader != null && oldHeader.mVersionString.toInt() > newHeader.mVersionString.toInt())
                R.string.overwrite_old_dicitonary_messsage
            else if (currentDictState == DICT_NEW && currentDictLocale == null)
                R.string.replace_dictionary_message
            else 0
        } else 0
        if (shouldAskMessageId != 0)
            showConfirmReplaceDialog(locale, dictionaryType, shouldAskMessageId)
        else
            moveCachedFileToDictionaries(locale, dictionaryType)
    }

    private fun showConfirmReplaceDialog(locale: Locale, dictionaryType: String, messageId: Int) {
        AlertDialog.Builder(DialogUtils.getPlatformDialogThemeContext(activity))
            .setTitle(R.string.replace_dictionary)
            .setMessage(resources.getString(messageId, locale.displayName()))
            .setCancelable(false)
            .setNegativeButton(R.string.cancel, ) { _,_ ->
                cachedDictionaryFile.delete()
            }
            .setPositiveButton(R.string.replace_dictionary) { _,_ ->
                moveCachedFileToDictionaries(locale, dictionaryType)
            }
            .show()
    }

    private fun moveCachedFileToDictionaries(locale: Locale, dictionaryType: String) {
        val dictFile = File(locale.getUserDictFilename(dictionaryType))
        if (!cachedDictionaryFile.renameTo(dictFile)) {
            cachedDictionaryFile.delete()
            onDictionaryLoadingError(R.string.dictionary_load_error.resString())
            return
        }

        // success, now remove internal dictionary file if a main dictionary was added
        if (dictionaryType == DictionaryInfoUtils.DEFAULT_MAIN_DICT)
            File(locale.getInternalDictFilename()).delete()

        // inform user about success
        val successMessageForLocale = resources
            .getString(R.string.dictionary_load_success, locale.displayName())
        Toast.makeText(activity, successMessageForLocale, Toast.LENGTH_LONG).show()

        // inform LatinIME about new dictionary
        val newDictBroadcast = Intent(DictionaryPackConstants.NEW_DICTIONARY_INTENT_ACTION)
        activity.sendBroadcast(newDictBroadcast)
        reloadDictionaries()
    }

    private fun onDictionaryLoadingError(message: String) {
        AlertDialog.Builder(DialogUtils.getPlatformDialogThemeContext(activity))
            .setNegativeButton(android.R.string.ok, null)
            .setMessage(message)
            .setTitle("loading error")
            .show()
    }

    private fun Locale.getUserDictFilename(dictionaryType: String) =
        DictionaryInfoUtils.getCacheDirectoryForLocale(this.toString(), activity) + File.separator + dictionaryType + "_" + USER_DICTIONARY_SUFFIX

    private fun Locale.getUserDictFilenames(): List<String> {
        val dicts = mutableListOf<String>()
        val p = DictionaryInfoUtils.getCacheDirectoryForLocale(this.toString(), activity)
        DictionaryInfoUtils.getCachedDirectoryList(activity)?.forEach { dir ->
            if (!dir.isDirectory)
                return@forEach
            dir.list()?.forEach {
                if (it.endsWith(USER_DICTIONARY_SUFFIX))
                    dicts.add(p + File.separator + it)
            }
        }
        return dicts
    }

    private fun Locale.getInternalDictFilename() =
        DictionaryInfoUtils.getCacheDirectoryForLocale(this.toString(), activity) + File.separator + DictionaryInfoUtils.getMainDictFilename(this.toString())

    private fun String.displayName() = LocaleUtils.constructLocaleFromString(this).displayName()

    private fun String.toLocale() = LocaleUtils.constructLocaleFromString(this)

    private fun Locale.displayName() = getDisplayName(resources.configuration.locale)

    private fun Int.resString() = resources.getString(this)

    companion object {
        private const val DICTIONARY_REQUEST_CODE = 96834
        private const val DICTIONARY_URL =
            "https://github.com/Helium314/openboard/tree/new/dictionaries/dict"
        private const val USER_DICTIONARY_SUFFIX = "user.dict"

        private const val DICT_INTERNAL_AND_USER = 2
        private const val DICT_INTERNAL_ONLY = 1
        private const val DICT_USER_ONLY = 0
        private const val DICT_NEW = -1

    }
}
