package org.dslul.openboard.inputmethod.latin

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import org.dslul.openboard.inputmethod.keyboard.KeyboardActionListener
import org.dslul.openboard.inputmethod.latin.common.Constants

class KeyboardWrapperView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle), View.OnClickListener {

    var keyboardActionListener: KeyboardActionListener? = null

    private lateinit var stopOneHandedModeBtn: ImageButton
    private lateinit var switchOneHandedModeBtn: ImageButton
    private lateinit var keyboardView: View
    private val iconStopOneHandedModeId: Int
    private val iconSwitchOneHandedModeId: Int

    var oneHandedModeEnabled = false
        set(enabled) {
            field = enabled
            updateViewsVisibility()
            requestLayout()
        }

    var oneHandedGravity = Gravity.NO_GRAVITY
        set(value) {
            field = value
            updateSwitchButtonSide()
            requestLayout()
        }


    override fun onFinishInflate() {
        super.onFinishInflate()
        stopOneHandedModeBtn = findViewById(R.id.btn_stop_one_handed_mode)
        stopOneHandedModeBtn.setImageResource(iconStopOneHandedModeId)
        stopOneHandedModeBtn.visibility = GONE
        switchOneHandedModeBtn = findViewById(R.id.btn_switch_one_handed_mode)
        switchOneHandedModeBtn.setImageResource(iconSwitchOneHandedModeId)
        switchOneHandedModeBtn.visibility = GONE
        keyboardView = findViewById(R.id.keyboard_view)

        stopOneHandedModeBtn.setOnClickListener(this)
        switchOneHandedModeBtn.setOnClickListener(this)
    }

    @SuppressLint("RtlHardcoded")
    fun switchOneHandedModeSide() {
        oneHandedGravity = if (oneHandedGravity == Gravity.LEFT) Gravity.RIGHT else Gravity.LEFT
    }

    private fun updateViewsVisibility() {
        stopOneHandedModeBtn.visibility = if (oneHandedModeEnabled) VISIBLE else GONE
        switchOneHandedModeBtn.visibility = if (oneHandedModeEnabled) VISIBLE else GONE
    }

    @SuppressLint("RtlHardcoded")
    private fun updateSwitchButtonSide() {
        switchOneHandedModeBtn.scaleX = if (oneHandedGravity == Gravity.LEFT) -1f else 1f
    }

    override fun onClick(view: View) {
        if (view === stopOneHandedModeBtn) {
            keyboardActionListener?.onCodeInput(Constants.CODE_STOP_ONE_HANDED_MODE,
                Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE,
                false /* isKeyRepeat */)
        } else if (view === switchOneHandedModeBtn) {
            keyboardActionListener?.onCodeInput(Constants.CODE_SWITCH_ONE_HANDED_MODE,
                Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE,
                false /* isKeyRepeat */)
        }
    }

    @SuppressLint("RtlHardcoded")
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (!oneHandedModeEnabled) {
            super.onLayout(changed, left, top, right, bottom)
            return
        }

        val isLeftGravity = oneHandedGravity == Gravity.LEFT
        val width = right - left
        val spareWidth = width - keyboardView.measuredWidth

        val keyboardLeft = if (isLeftGravity) 0 else spareWidth
        keyboardView.layout(
                keyboardLeft,
                0,
                keyboardLeft + keyboardView.measuredWidth,
                keyboardView.measuredHeight
        )

        val buttonsLeft = if (isLeftGravity) keyboardView.measuredWidth else 0
        stopOneHandedModeBtn.layout(
                buttonsLeft + (spareWidth - stopOneHandedModeBtn.measuredWidth) / 2,
                stopOneHandedModeBtn.measuredHeight / 2,
                buttonsLeft + (spareWidth + stopOneHandedModeBtn.measuredWidth) / 2,
                3 * stopOneHandedModeBtn.measuredHeight / 2
        )
        switchOneHandedModeBtn.layout(
                buttonsLeft + (spareWidth - switchOneHandedModeBtn.measuredWidth) / 2,
                2 * stopOneHandedModeBtn.measuredHeight,
                buttonsLeft + (spareWidth + switchOneHandedModeBtn.measuredWidth) / 2,
                2 * stopOneHandedModeBtn.measuredHeight + switchOneHandedModeBtn.measuredHeight
        )
    }

    init {
        val keyboardAttr = context.obtainStyledAttributes(attrs,
                R.styleable.Keyboard, defStyle, R.style.Keyboard)
        iconStopOneHandedModeId = keyboardAttr.getResourceId(R.styleable.Keyboard_iconStopOneHandedMode, 0)
        iconSwitchOneHandedModeId = keyboardAttr.getResourceId(R.styleable.Keyboard_iconSwitchOneHandedMode, 0)
        keyboardAttr.recycle()

        val themeAttr = context.obtainStyledAttributes(attrs,
                R.styleable.KeyboardTheme, defStyle, 0)
        val keyboardViewStyleId = themeAttr.getResourceId(R.styleable.KeyboardTheme_mainKeyboardViewStyle, 0)
        themeAttr.recycle()
        val styleAttr = context.obtainStyledAttributes(keyboardViewStyleId, intArrayOf(android.R.attr.background))
        setBackgroundResource(styleAttr.getResourceId(0, 0))
        styleAttr.recycle()
    }
}