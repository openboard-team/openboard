package io.github.sds100.keymapper.inputmethod.compat

import android.os.Build
import android.os.Build.VERSION_CODES
import android.text.TextUtils
import android.view.inputmethod.InputMethodSubtype
import io.github.sds100.keymapper.inputmethod.latin.common.LocaleUtils
import java.util.*

object InputMethodSubtypeCompatUtils {
    // Note that InputMethodSubtype.getLanguageTag() is expected to be available in Android N+.
    private val GET_LANGUAGE_TAG = CompatUtils.getMethod(InputMethodSubtype::class.java, "getLanguageTag")

    @kotlin.jvm.JvmStatic
    fun getLocaleObject(subtype: InputMethodSubtype): Locale { // Locale.forLanguageTag() is available only in Android L and later.
        if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            val languageTag = CompatUtils.invoke(subtype, null, GET_LANGUAGE_TAG) as String?
            if (!TextUtils.isEmpty(languageTag)) {
                return Locale.forLanguageTag(languageTag)
            }
        }
        return LocaleUtils.constructLocaleFromString(subtype.locale)
    }

}