package org.dslul.openboard.inputmethod.compat

import android.view.textservice.TextInfo
import org.dslul.openboard.inputmethod.annotations.UsedForTesting

object TextInfoCompatUtils {
    // Note that TextInfo.getCharSequence() is supposed to be available in API level 21 and later.
    private val TEXT_INFO_GET_CHAR_SEQUENCE = CompatUtils.getMethod(TextInfo::class.java, "getCharSequence")
    private val TEXT_INFO_CONSTRUCTOR_FOR_CHAR_SEQUENCE = CompatUtils.getConstructor(TextInfo::class.java, CharSequence::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)

    @get:UsedForTesting
    val isCharSequenceSupported: Boolean
        get() = TEXT_INFO_GET_CHAR_SEQUENCE != null &&
                TEXT_INFO_CONSTRUCTOR_FOR_CHAR_SEQUENCE != null

    @kotlin.jvm.JvmStatic
    @UsedForTesting
    fun newInstance(charSequence: CharSequence, start: Int, end: Int, cookie: Int,
                    sequenceNumber: Int): TextInfo? {
        return if (TEXT_INFO_CONSTRUCTOR_FOR_CHAR_SEQUENCE != null) {
            CompatUtils.newInstance(TEXT_INFO_CONSTRUCTOR_FOR_CHAR_SEQUENCE,
                    charSequence, start, end, cookie, sequenceNumber) as TextInfo
        } else TextInfo(charSequence.subSequence(start, end).toString(), cookie,
                sequenceNumber)
    }

    /**
     * Returns the result of [TextInfo.getCharSequence] when available. Otherwise returns
     * the result of [TextInfo.getText] as fall back.
     * @param textInfo the instance for which [TextInfo.getCharSequence] or
     * [TextInfo.getText] is called.
     * @return the result of [TextInfo.getCharSequence] when available. Otherwise returns
     * the result of [TextInfo.getText] as fall back. If `textInfo` is `null`,
     * returns `null`.
     */
    @kotlin.jvm.JvmStatic
    @UsedForTesting
    fun getCharSequenceOrString(textInfo: TextInfo?): CharSequence? {
        val defaultValue: CharSequence? = textInfo?.text
        return CompatUtils.invoke(textInfo, defaultValue!!,
                TEXT_INFO_GET_CHAR_SEQUENCE) as CharSequence
    }
}