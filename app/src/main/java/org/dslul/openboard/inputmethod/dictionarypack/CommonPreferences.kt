package org.dslul.openboard.inputmethod.dictionarypack

import android.content.Context
import android.content.SharedPreferences

object CommonPreferences {
    private const val COMMON_PREFERENCES_NAME = "LatinImeDictPrefs"
    fun getCommonPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(COMMON_PREFERENCES_NAME, 0)
    }

    fun enable(pref: SharedPreferences?, id: String?) {
        val editor = pref!!.edit()
        editor.putBoolean(id, true)
        editor.apply()
    }

    fun disable(pref: SharedPreferences?, id: String?) {
        val editor = pref!!.edit()
        editor.putBoolean(id, false)
        editor.apply()
    }
}