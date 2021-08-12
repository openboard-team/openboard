/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dslul.openboard.inputmethod.latin.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.RequiresApi;

public final class DeviceProtectedUtils {

    static final String TAG = DeviceProtectedUtils.class.getSimpleName();

    public static SharedPreferences getSharedPreferences(final Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return PreferenceManager.getDefaultSharedPreferences(context);
        }
        Context deviceProtectedContext = getDeviceProtectedContext(context);
        SharedPreferences deviceProtectedPreferences = PreferenceManager.getDefaultSharedPreferences(deviceProtectedContext);
        if (deviceProtectedPreferences.getAll().isEmpty()) {
            Log.i(TAG, "Device encrypted storage is empty, copying values from credential encrypted storage");
            deviceProtectedContext.moveSharedPreferencesFrom(context, PreferenceManager.getDefaultSharedPreferencesName(context));
        }
        return deviceProtectedPreferences;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static Context getDeviceProtectedContext(final Context context) {
        return context.isDeviceProtectedStorage()
                ? context : context.createDeviceProtectedStorageContext();
    }

    private DeviceProtectedUtils() {
        // This utility class is not publicly instantiable.
    }
}
