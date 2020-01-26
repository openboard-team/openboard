package org.dslul.openboard.inputmethod.compat

import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.UserManager
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import java.lang.reflect.Method

/**
 * A temporary solution until `UserManagerCompat.isUserUnlocked()` in the support-v4 library
 * becomes publicly available.
 */
object UserManagerCompatUtils {
    private var METHOD_isUserUnlocked: Method? = null
    const val LOCK_STATE_UNKNOWN = 0
    const val LOCK_STATE_UNLOCKED = 1
    const val LOCK_STATE_LOCKED = 2
    /**
     * Check if the calling user is running in an "unlocked" state. A user is unlocked only after
     * they've entered their credentials (such as a lock pattern or PIN), and credential-encrypted
     * private app data storage is available.
     * @param context context from which [UserManager] should be obtained.
     * @return One of [LockState].
     */
    @RequiresApi(VERSION_CODES.M)
    @kotlin.jvm.JvmStatic
    @LockState
    fun getUserLockState(context: Context): Int {
        if (METHOD_isUserUnlocked == null) {
            return LOCK_STATE_UNKNOWN
        }
        val userManager = context.getSystemService(UserManager::class.java)
                ?: return LOCK_STATE_UNKNOWN
        val result = CompatUtils.invoke(userManager, null, METHOD_isUserUnlocked) as Boolean
        return if (result) LOCK_STATE_UNLOCKED else LOCK_STATE_LOCKED
    }

    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @IntDef(LOCK_STATE_UNKNOWN, LOCK_STATE_UNLOCKED, LOCK_STATE_LOCKED)
    annotation class LockState

    init { // We do not try to search the method in Android M and prior.
        METHOD_isUserUnlocked = if (Build.VERSION.SDK_INT <= VERSION_CODES.M) {
            null
        } else {
            CompatUtils.getMethod(UserManager::class.java, "isUserUnlocked")
        }
    }
}