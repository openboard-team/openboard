package org.dslul.openboard.inputmethodcommon

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.preference.Preference
import android.preference.PreferenceScreen
import android.provider.Settings
import android.text.TextUtils
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager

/* package private */
internal class InputMethodSettingsImpl : InputMethodSettingsInterface {
    private var mSubtypeEnablerPreference: Preference? = null
    private var mInputMethodSettingsCategoryTitleRes = 0
    private var mInputMethodSettingsCategoryTitle: CharSequence? = null
    private var mSubtypeEnablerTitleRes = 0
    private var mSubtypeEnablerTitle: CharSequence? = null
    private var mSubtypeEnablerIconRes = 0
    private var mSubtypeEnablerIcon: Drawable? = null
    private var mImm: InputMethodManager? = null
    private var mImi: InputMethodInfo? = null
    /**
     * Initialize internal states of this object.
     * @param context the context for this application.
     * @param prefScreen a PreferenceScreen of PreferenceActivity or PreferenceFragment.
     * @return true if this application is an IME and has two or more subtypes, false otherwise.
     */
    fun init(context: Context, prefScreen: PreferenceScreen): Boolean {
        mImm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        mImi = getMyImi(context, mImm)
        if (mImi == null || mImi!!.subtypeCount <= 1) {
            return false
        }
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SUBTYPE_SETTINGS)
        intent.putExtra(Settings.EXTRA_INPUT_METHOD_ID, mImi!!.id)
        intent.flags = (Intent.FLAG_ACTIVITY_NEW_TASK
                or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        mSubtypeEnablerPreference = Preference(context)
        mSubtypeEnablerPreference!!.intent = intent
        prefScreen.addPreference(mSubtypeEnablerPreference)
        updateSubtypeEnabler()
        return true
    }

    /**
     * {@inheritDoc}
     */
    override fun setInputMethodSettingsCategoryTitle(resId: Int) {
        mInputMethodSettingsCategoryTitleRes = resId
        updateSubtypeEnabler()
    }

    /**
     * {@inheritDoc}
     */
    override fun setInputMethodSettingsCategoryTitle(title: CharSequence?) {
        mInputMethodSettingsCategoryTitleRes = 0
        mInputMethodSettingsCategoryTitle = title
        updateSubtypeEnabler()
    }

    /**
     * {@inheritDoc}
     */
    override fun setSubtypeEnablerTitle(resId: Int) {
        mSubtypeEnablerTitleRes = resId
        updateSubtypeEnabler()
    }

    /**
     * {@inheritDoc}
     */
    override fun setSubtypeEnablerTitle(title: CharSequence?) {
        mSubtypeEnablerTitleRes = 0
        mSubtypeEnablerTitle = title
        updateSubtypeEnabler()
    }

    /**
     * {@inheritDoc}
     */
    override fun setSubtypeEnablerIcon(resId: Int) {
        mSubtypeEnablerIconRes = resId
        updateSubtypeEnabler()
    }

    /**
     * {@inheritDoc}
     */
    override fun setSubtypeEnablerIcon(drawable: Drawable?) {
        mSubtypeEnablerIconRes = 0
        mSubtypeEnablerIcon = drawable
        updateSubtypeEnabler()
    }

    fun updateSubtypeEnabler() {
        val pref = mSubtypeEnablerPreference ?: return
        val context = pref.context
        val title: CharSequence?
        title = if (mSubtypeEnablerTitleRes != 0) {
            context.getString(mSubtypeEnablerTitleRes)
        } else {
            mSubtypeEnablerTitle
        }
        pref.title = title
        val intent = pref.intent
        intent?.putExtra(Intent.EXTRA_TITLE, title)
        val summary = getEnabledSubtypesLabel(context, mImm, mImi)
        if (!TextUtils.isEmpty(summary)) {
            pref.summary = summary
        }
        if (mSubtypeEnablerIconRes != 0) {
            pref.setIcon(mSubtypeEnablerIconRes)
        } else {
            pref.icon = mSubtypeEnablerIcon
        }
    }

    companion object {
        private fun getMyImi(context: Context, imm: InputMethodManager?): InputMethodInfo? {
            val imis = imm!!.inputMethodList
            for (i in imis.indices) {
                val imi = imis[i]
                if (imis[i].packageName == context.packageName) {
                    return imi
                }
            }
            return null
        }

        private fun getEnabledSubtypesLabel(
                context: Context?, imm: InputMethodManager?, imi: InputMethodInfo?): String? {
            if (context == null || imm == null || imi == null) return null
            val subtypes = imm.getEnabledInputMethodSubtypeList(imi, true)
            val sb = StringBuilder()
            val N = subtypes.size
            for (i in 0 until N) {
                val subtype = subtypes[i]
                if (sb.length > 0) {
                    sb.append(", ")
                }
                sb.append(subtype.getDisplayName(context, imi.packageName,
                        imi.serviceInfo.applicationInfo))
            }
            return sb.toString()
        }
    }
}