package org.dslul.openboard.inputmethod.compat

import android.os.Build
import android.os.Build.VERSION_CODES
import android.text.TextUtils
import android.util.Log
import android.view.inputmethod.InputMethodSubtype
import org.dslul.openboard.inputmethod.annotations.UsedForTesting
import org.dslul.openboard.inputmethod.latin.RichInputMethodSubtype
import org.dslul.openboard.inputmethod.latin.common.Constants
import org.dslul.openboard.inputmethod.latin.common.LocaleUtils
import java.util.*

object InputMethodSubtypeCompatUtils {
    private val TAG = InputMethodSubtypeCompatUtils::class.java.simpleName
    // Note that InputMethodSubtype(int nameId, int iconId, String locale, String mode,
// String extraValue, boolean isAuxiliary, boolean overridesImplicitlyEnabledSubtype, int id)
// has been introduced in API level 17 (Build.VERSION_CODE.JELLY_BEAN_MR1).
    private val CONSTRUCTOR_INPUT_METHOD_SUBTYPE = CompatUtils.getConstructor(InputMethodSubtype::class.java,
            Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java, String::class.java, String::class.java, Boolean::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType, Int::class.javaPrimitiveType)

    @kotlin.jvm.JvmStatic
    fun newInputMethodSubtype(nameId: Int, iconId: Int, locale: String?,
                              mode: String?, extraValue: String?, isAuxiliary: Boolean,
                              overridesImplicitlyEnabledSubtype: Boolean, id: Int): InputMethodSubtype? {
        return if (CONSTRUCTOR_INPUT_METHOD_SUBTYPE == null) {
            InputMethodSubtype(nameId, iconId, locale, mode, extraValue, isAuxiliary,
                    overridesImplicitlyEnabledSubtype)
        } else CompatUtils.newInstance(CONSTRUCTOR_INPUT_METHOD_SUBTYPE,
                nameId, iconId, locale, mode, extraValue, isAuxiliary,
                overridesImplicitlyEnabledSubtype, id) as InputMethodSubtype
    }

    // Note that InputMethodSubtype.getLanguageTag() is expected to be available in Android N+.
    private val GET_LANGUAGE_TAG = CompatUtils.getMethod(InputMethodSubtype::class.java, "getLanguageTag")

    @kotlin.jvm.JvmStatic
    fun getLocaleObject(subtype: InputMethodSubtype): Locale { // Locale.forLanguageTag() is available only in Android L and later.
        if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            val languageTag = CompatUtils.invoke(subtype, null, GET_LANGUAGE_TAG) as String
            if (!TextUtils.isEmpty(languageTag)) {
                return Locale.forLanguageTag(languageTag)
            }
        }
        return LocaleUtils.constructLocaleFromString(subtype.locale)
    }

    init {
        if (CONSTRUCTOR_INPUT_METHOD_SUBTYPE == null) {
            Log.w(TAG, "Warning!!! Constructor is not defined.")
        }
    }
}