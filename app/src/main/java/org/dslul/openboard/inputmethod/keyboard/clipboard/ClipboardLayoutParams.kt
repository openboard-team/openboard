package org.dslul.openboard.inputmethod.keyboard.clipboard

import android.content.res.Resources
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.utils.ResourceUtils

class ClipboardLayoutParams(res: Resources) {

    private val keyVerticalGap: Int
    private val keyHorizontalGap: Int
    private val topPadding: Int
    private val bottomPadding: Int
    private val listHeight: Int
    private val actionBarHeight: Int

    companion object {
        private const val DEFAULT_KEYBOARD_ROWS = 4
    }

    init {
        val defaultKeyboardHeight = ResourceUtils.getDefaultKeyboardHeight(res)
        val suggestionStripHeight = res.getDimensionPixelSize(R.dimen.config_suggestions_strip_height)
        val defaultKeyboardWidth = ResourceUtils.getDefaultKeyboardWidth(res)

        keyVerticalGap = res.getFraction(R.fraction.config_key_vertical_gap_holo,
                defaultKeyboardHeight, defaultKeyboardHeight).toInt()
        bottomPadding = res.getFraction(R.fraction.config_keyboard_bottom_padding_holo,
                defaultKeyboardHeight, defaultKeyboardHeight).toInt()
        topPadding = res.getFraction(R.fraction.config_keyboard_top_padding_holo,
                defaultKeyboardHeight, defaultKeyboardHeight).toInt()
        keyHorizontalGap = res.getFraction(R.fraction.config_key_horizontal_gap_holo,
                defaultKeyboardWidth, defaultKeyboardWidth).toInt()

        actionBarHeight = (defaultKeyboardHeight - bottomPadding - topPadding) / DEFAULT_KEYBOARD_ROWS - keyVerticalGap / 2
        listHeight = defaultKeyboardHeight + suggestionStripHeight - actionBarHeight - bottomPadding
    }

    fun setListProperties(recycler: RecyclerView) {
        (recycler.layoutParams as FrameLayout.LayoutParams).apply {
            height = listHeight
            recycler.layoutParams = this
        }
    }

    fun setActionBarProperties(layout: FrameLayout) {
        (layout.layoutParams as LinearLayout.LayoutParams).apply {
            height = actionBarHeight
            layout.layoutParams = this
        }
    }

    fun setItemProperties(view: View) {
        (view.layoutParams as RecyclerView.LayoutParams).apply {
            topMargin = keyHorizontalGap / 2
            bottomMargin = keyVerticalGap / 2
            marginStart = keyHorizontalGap / 2
            marginEnd = keyHorizontalGap / 2
            view.layoutParams = this
        }
    }

    val actionBarContentHeight
        get() = actionBarHeight
}