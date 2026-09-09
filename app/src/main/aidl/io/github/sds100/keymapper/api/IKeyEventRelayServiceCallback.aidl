package io.github.sds100.keymapper.api;

import android.view.KeyEvent;

interface IKeyEventRelayServiceCallback {
    boolean onKeyEvent(in KeyEvent event);
}