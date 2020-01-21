package org.dslul.openboard.inputmethod.compat

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.SuggestionSpan
import org.dslul.openboard.inputmethod.annotations.UsedForTesting
import org.dslul.openboard.inputmethod.latin.SuggestedWords
import org.dslul.openboard.inputmethod.latin.SuggestedWords.SuggestedWordInfo
import org.dslul.openboard.inputmethod.latin.common.LocaleUtils
import org.dslul.openboard.inputmethod.latin.define.DebugFlags
import java.util.*

object SuggestionSpanUtils {
    // Note that SuggestionSpan.FLAG_AUTO_CORRECTION has been introduced
// in API level 15 (Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1).
    private val FIELD_FLAG_AUTO_CORRECTION = CompatUtils.getField(
            SuggestionSpan::class.java, "FLAG_AUTO_CORRECTION")
    private val OBJ_FLAG_AUTO_CORRECTION: Int? = CompatUtils.getFieldValue(
            null /* receiver */, null /* defaultValue */, FIELD_FLAG_AUTO_CORRECTION) as Int

    @kotlin.jvm.JvmStatic
    @UsedForTesting
    fun getTextWithAutoCorrectionIndicatorUnderline(
            context: Context?, text: String,locale: Locale?): CharSequence {
        if (TextUtils.isEmpty(text) || OBJ_FLAG_AUTO_CORRECTION == null) {
            return text
        }
        val spannable: Spannable = SpannableString(text)
        val suggestionSpan = SuggestionSpan(context, locale, arrayOf(), OBJ_FLAG_AUTO_CORRECTION, null)
        spannable.setSpan(suggestionSpan, 0, text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE or Spanned.SPAN_COMPOSING)
        return spannable
    }

    @UsedForTesting
    fun getTextWithSuggestionSpan(context: Context?,
                                  pickedWord: String, suggestedWords: SuggestedWords, locale: Locale?): CharSequence {
        if (TextUtils.isEmpty(pickedWord) || suggestedWords.isEmpty
                || suggestedWords.isPrediction || suggestedWords.isPunctuationSuggestions) {
            return pickedWord
        }
        val suggestionsList = ArrayList<String>()
        for (i in 0 until suggestedWords.size()) {
            if (suggestionsList.size >= SuggestionSpan.SUGGESTIONS_MAX_SIZE) {
                break
            }
            val info = suggestedWords.getInfo(i)
            if (info.isKindOf(SuggestedWordInfo.KIND_PREDICTION)) {
                continue
            }
            val word = suggestedWords.getWord(i)
            if (!TextUtils.equals(pickedWord, word)) {
                suggestionsList.add(word.toString())
            }
        }
        val suggestionSpan = SuggestionSpan(context, locale,
                suggestionsList.toTypedArray(), 0 /* flags */, null)
        val spannable: Spannable = SpannableString(pickedWord)
        spannable.setSpan(suggestionSpan, 0, pickedWord.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannable
    }

    /**
     * Returns first [Locale] found in the given array of [SuggestionSpan].
     * @param suggestionSpans the array of [SuggestionSpan] to be examined.
     * @return the first [Locale] found in `suggestionSpans`. `null` when not
     * found.
     */
    @UsedForTesting
    fun findFirstLocaleFromSuggestionSpans(
            suggestionSpans: Array<SuggestionSpan>): Locale? {
        for (suggestionSpan in suggestionSpans) {
            val localeString = suggestionSpan.locale
            if (TextUtils.isEmpty(localeString)) {
                continue
            }
            return LocaleUtils.constructLocaleFromString(localeString)
        }
        return null
    }

    init {
        if (DebugFlags.DEBUG_ENABLED) {
            if (OBJ_FLAG_AUTO_CORRECTION == null) {
                throw RuntimeException("Field is accidentially null.")
            }
        }
    }
}