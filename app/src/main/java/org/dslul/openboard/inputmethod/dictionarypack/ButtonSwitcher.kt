package org.dslul.openboard.inputmethod.dictionarypack

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewPropertyAnimator
import android.widget.Button
import android.widget.FrameLayout
import org.dslul.openboard.inputmethod.latin.R

/**
 * A view that handles buttons inside it according to a status.
 */
class ButtonSwitcher : FrameLayout {
    // One of the above
    private var mStatus = NOT_INITIALIZED
    private var mAnimateToStatus = NOT_INITIALIZED
    private var mInstallButton: Button? = null
    private var mCancelButton: Button? = null
    private var mDeleteButton: Button? = null
    private var mInterfaceState: DictionaryListInterfaceState? = null
    private var mOnClickListener: OnClickListener? = null

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context!!, attrs, defStyle)

    fun reset(interfaceState: DictionaryListInterfaceState?) {
        mStatus = NOT_INITIALIZED
        mAnimateToStatus = NOT_INITIALIZED
        mInterfaceState = interfaceState
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int,
                          bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        mInstallButton = findViewById<View>(R.id.dict_install_button) as Button
        mCancelButton = findViewById<View>(R.id.dict_cancel_button) as Button
        mDeleteButton = findViewById<View>(R.id.dict_delete_button) as Button
        setInternalOnClickListener(mOnClickListener)
        setButtonPositionWithoutAnimation(mStatus)
        if (mAnimateToStatus != NOT_INITIALIZED) { // We have been asked to animate before we were ready, so we took a note of it.
// We are now ready: launch the animation.
            animateButtonPosition(mStatus, mAnimateToStatus)
            mStatus = mAnimateToStatus
            mAnimateToStatus = NOT_INITIALIZED
        }
    }

    private fun getButton(status: Int): Button? {
        return when (status) {
            STATUS_INSTALL -> mInstallButton
            STATUS_CANCEL -> mCancelButton
            STATUS_DELETE -> mDeleteButton
            else -> null
        }
    }

    fun setStatusAndUpdateVisuals(status: Int) {
        if (mStatus == NOT_INITIALIZED) {
            setButtonPositionWithoutAnimation(status)
            mStatus = status
        } else {
            if (null == mInstallButton) { // We may come here before we have been layout. In this case we don't know our
// size yet so we can't start animations so we need to remember what animation to
// start once layout has gone through.
                mAnimateToStatus = status
            } else {
                animateButtonPosition(mStatus, status)
                mStatus = status
            }
        }
    }

    private fun setButtonPositionWithoutAnimation(status: Int) { // This may be called by setStatus() before the layout has come yet.
        if (null == mInstallButton) return
        val width = width
        // Set to out of the screen if that's not the currently displayed status
        mInstallButton!!.translationX = if (STATUS_INSTALL == status) 0F else width.toFloat()
        mCancelButton!!.translationX = if (STATUS_CANCEL == status) 0F else width.toFloat()
        mDeleteButton!!.translationX = if (STATUS_DELETE == status) 0F else width.toFloat()
    }

    // The helper method for {@link AnimatorListenerAdapter}.
    fun animateButtonIfStatusIsEqual(newButton: View, newStatus: Int) {
        if (newStatus != mStatus) return
        animateButton(newButton, ANIMATION_IN)
    }

    private fun animateButtonPosition(oldStatus: Int, newStatus: Int) {
        val oldButton: View? = getButton(oldStatus)
        val newButton: View? = getButton(newStatus)
        if (null != oldButton && null != newButton) { // Transition between two buttons : animate out, then in
            animateButton(oldButton, ANIMATION_OUT).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    animateButtonIfStatusIsEqual(newButton, newStatus)
                }
            })
        } else oldButton?.let { animateButton(it, ANIMATION_OUT) }
                ?: newButton?.let { animateButton(it, ANIMATION_IN) }
    }

    fun setInternalOnClickListener(listener: OnClickListener?) {
        mOnClickListener = listener
        if (null != mInstallButton) { // Already laid out : do it now
            mInstallButton!!.setOnClickListener(mOnClickListener)
            mCancelButton!!.setOnClickListener(mOnClickListener)
            mDeleteButton!!.setOnClickListener(mOnClickListener)
        }
    }

    private fun animateButton(button: View, direction: Int): ViewPropertyAnimator {
        val outerX = width.toFloat()
        val innerX = button.x - button.translationX
        mInterfaceState!!.removeFromCache(parent as View)
        if (ANIMATION_IN == direction) {
            button.isClickable = true
            return button.animate().translationX(0f)
        }
        button.isClickable = false
        return button.animate().translationX(outerX - innerX)
    }

    companion object {
        const val NOT_INITIALIZED = -1
        const val STATUS_NO_BUTTON = 0
        const val STATUS_INSTALL = 1
        const val STATUS_CANCEL = 2
        const val STATUS_DELETE = 3
        // Animation directions
        const val ANIMATION_IN = 1
        const val ANIMATION_OUT = 2
    }
}