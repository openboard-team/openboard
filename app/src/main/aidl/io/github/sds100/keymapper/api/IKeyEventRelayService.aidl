package io.github.sds100.keymapper.api;

import android.view.KeyEvent;
import android.view.MotionEvent;
import io.github.sds100.keymapper.api.IKeyEventRelayServiceCallback;

interface IKeyEventRelayService {
    /**
     * Send a key event to the target package that is registered with
     * a callback.
     *
     * callbackID parameter was added in Key Mapper 2.8.
     */
    boolean sendKeyEvent(in KeyEvent event, in String targetPackageName, in String callbackId);

    /**
     * Register a callback to receive key events from this relay service. The service
     * checks the process uid of the caller to this method and only permits certain applications
     * from connecting.
     *
     * id parameter was added in Key Mapper 2.8.
     */
    void registerCallback(IKeyEventRelayServiceCallback client, String id);

    /**
     * Unregister all the callbacks associated with the calling package from this relay service.
     *
     * callbackID parameter was added in Key Mapper 2.8.
     */
    void unregisterCallback(String callbackId);

    /**
     * Send a motion event to the target package that is registered with
     * a callback.
     */
    boolean sendMotionEvent(in MotionEvent event, in String targetPackageName, in String callbackId);
}