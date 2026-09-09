package org.dslul.openboard.inputmethod.latin

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.DeadObjectException
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import io.github.sds100.keymapper.api.IKeyEventRelayService
import io.github.sds100.keymapper.api.IKeyEventRelayServiceCallback

/**
 * This handles connecting to the Key Mapper relay service and exposes an interface
 * so other parts of the app can get a reference to the service even when it isn't
 * bound yet. This class mirrors the implementation in the Key Mapper GUI Keyboard app
 * (https://github.com/keymapperorg/KeyMapperOpenBoard) so both keyboards stay
 * interoperable with Key Mapper.
 */
class KeyEventRelayServiceWrapperImpl(
    context: Context,
    private val servicePackageName: String,
    private val id: String,
    private val callback: IKeyEventRelayServiceCallback,
) : KeyEventRelayServiceWrapper {
    companion object {
        /**
         * This is used to listen to when the key event relay service is restarted in Key Mapper.
         */
        const val ACTION_REBIND_RELAY_SERVICE = "io.github.sds100.keymapper.ACTION_REBIND_RELAY_SERVICE"
    }

    private val ctx: Context = context.applicationContext

    private val keyEventRelayServiceLock: Any = Any()
    private var keyEventRelayService: IKeyEventRelayService? = null

    private val serviceConnection: ServiceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                synchronized(keyEventRelayServiceLock) {
                    keyEventRelayService = IKeyEventRelayService.Stub.asInterface(service)
                    keyEventRelayService?.registerCallback(callback, id)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                synchronized(keyEventRelayServiceLock) {
                    // Do not unregister the callback in onServiceDisconnected
                    // because the connection is already broken at that point and it
                    // will fail.

                    Log.d(LatinIME.TAG, "Key event relay service stopped: $servicePackageName")

                    keyEventRelayService = null
                }
            }
        }

    private val broadcastReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                context ?: return
                intent ?: return

                when (intent.action) {
                    ACTION_REBIND_RELAY_SERVICE -> {
                        bind()
                    }
                }
            }
        }

    fun onCreate() {
        val intentFilter =
            IntentFilter().apply {
                addAction(ACTION_REBIND_RELAY_SERVICE)
            }

        ctx.registerReceiver(broadcastReceiver, intentFilter)
        bind()
    }

    fun onDestroy() {
        ctx.unregisterReceiver(broadcastReceiver)
        unbind()
    }

    override fun sendKeyEvent(
        event: KeyEvent,
        targetPackageName: String,
        callbackId: String,
    ): Boolean {
        if (keyEventRelayService == null) {
            return false
        }

        try {
            return keyEventRelayService!!.sendKeyEvent(event, targetPackageName, callbackId)
        } catch (e: DeadObjectException) {
            keyEventRelayService = null
            return false
        }
    }

    override fun sendMotionEvent(
        event: MotionEvent,
        targetPackageName: String,
        callbackId: String,
    ): Boolean {
        if (keyEventRelayService == null) {
            return false
        }

        try {
            return keyEventRelayService!!.sendMotionEvent(event, targetPackageName, callbackId)
        } catch (e: DeadObjectException) {
            keyEventRelayService = null
            return false
        }
    }

    private fun bind() {
        try {
            val relayServiceIntent = Intent()
            val component =
                ComponentName(servicePackageName, "io.github.sds100.keymapper.api.KeyEventRelayService")
            relayServiceIntent.setComponent(component)
            val isSuccess = ctx.bindService(relayServiceIntent, serviceConnection, 0)

            if (isSuccess) {
                Log.d(LatinIME.TAG, "Bind $servicePackageName")
            } else {
                Log.d(LatinIME.TAG, "Bind unsuccessful $servicePackageName")
                ctx.unbindService(serviceConnection)
            }
        } catch (e: SecurityException) {
            Log.e(LatinIME.TAG, e.toString())

            // Docs say to unbind if there is a security exception.
            ctx.unbindService(serviceConnection)
        }
    }

    private fun unbind() {
        Log.d(LatinIME.TAG, "Unbind $servicePackageName")
        // Unregister the callback if this input method is unbinding
        // from the relay service. This should not happen in onServiceDisconnected
        // because the connection is already broken at that point and it
        // will fail.
        try {
            keyEventRelayService?.unregisterCallback(id)
            ctx.unbindService(serviceConnection)
        } catch (e: RemoteException) {
            // do nothing
        } catch (e: IllegalArgumentException) {
            // an exception is thrown if you unbind from a service
            // while there is no registered connection.
        }
    }
}

interface KeyEventRelayServiceWrapper {
    fun sendKeyEvent(
        event: KeyEvent,
        targetPackageName: String,
        callbackId: String,
    ): Boolean

    fun sendMotionEvent(
        event: MotionEvent,
        targetPackageName: String,
        callbackId: String,
    ): Boolean
}
