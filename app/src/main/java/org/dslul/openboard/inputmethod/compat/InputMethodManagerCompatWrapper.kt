package org.dslul.openboard.inputmethod.compat

import android.content.Context
import android.os.IBinder
import android.view.inputmethod.InputMethodManager

class InputMethodManagerCompatWrapper(context: Context) {
    @kotlin.jvm.JvmField
    val mImm: InputMethodManager
    fun switchToNextInputMethod(token: IBinder?, onlyCurrentIme: Boolean): Boolean {
        return CompatUtils.invoke(mImm, false /* defaultValue */,
                METHOD_switchToNextInputMethod, token, onlyCurrentIme) as Boolean
    }

    fun shouldOfferSwitchingToNextInputMethod(token: IBinder?): Boolean {
        return CompatUtils.invoke(mImm, false /* defaultValue */,
                METHOD_shouldOfferSwitchingToNextInputMethod, token) as Boolean
    }

    companion object {
        // Note that InputMethodManager.switchToNextInputMethod() has been introduced
// in API level 16 (Build.VERSION_CODES.JELLY_BEAN).
        private val METHOD_switchToNextInputMethod = CompatUtils.getMethod(
                InputMethodManager::class.java, "switchToNextInputMethod", IBinder::class.java, Boolean::class.javaPrimitiveType)
        // Note that InputMethodManager.shouldOfferSwitchingToNextInputMethod() has been introduced
// in API level 19 (Build.VERSION_CODES.KITKAT).
        private val METHOD_shouldOfferSwitchingToNextInputMethod = CompatUtils.getMethod(InputMethodManager::class.java,
                "shouldOfferSwitchingToNextInputMethod", IBinder::class.java)
    }

    init {
        mImm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }
}