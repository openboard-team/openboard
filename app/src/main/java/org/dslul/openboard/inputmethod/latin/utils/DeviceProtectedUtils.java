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
import android.preference.PreferenceManager;
import android.util.Log;

import org.dslul.openboard.inputmethod.latin.LatinIME;

import java.util.Map;
import java.util.Set;

public final class DeviceProtectedUtils {

    static final String TAG = DeviceProtectedUtils.class.getSimpleName();

    @SuppressWarnings("unchecked")
    public static SharedPreferences getSharedPreferences(final Context context) {
        SharedPreferences deviceProtectedPreferences = PreferenceManager.getDefaultSharedPreferences(getDeviceProtectedContext(context));
        if (deviceProtectedPreferences.getAll().isEmpty()) {
            Log.i(TAG, "Device encrypted storage is empty, copying values from credential encrypted storage");
            for (Map.Entry<String, ?> entry : PreferenceManager.getDefaultSharedPreferences(context).getAll().entrySet()) {
                SharedPreferences.Editor deviceProtectedPreferencesEditor = deviceProtectedPreferences.edit();
                try {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        if (entry.getValue() instanceof Boolean)
                            deviceProtectedPreferencesEditor.putBoolean(entry.getKey(), (Boolean) entry.getValue());
                        if (entry.getValue() instanceof Float)
                            deviceProtectedPreferencesEditor.putFloat(entry.getKey(), (Float) entry.getValue());
                        if (entry.getValue() instanceof Integer)
                            deviceProtectedPreferencesEditor.putInt(entry.getKey(), (Integer) entry.getValue());
                        if (entry.getValue() instanceof Long)
                            deviceProtectedPreferencesEditor.putLong(entry.getKey(), (Long) entry.getValue());
                        if (entry.getValue() instanceof String)
                            deviceProtectedPreferencesEditor.putString(entry.getKey(), (String) entry.getValue());
                        if (entry.getValue() instanceof Set)
                            deviceProtectedPreferencesEditor.putStringSet(entry.getKey(), (Set<String>) entry.getValue());
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Unable to copy preference from credential to device encrypted storage", e);
                }
                deviceProtectedPreferencesEditor.apply();
            }
        }
        return deviceProtectedPreferences;
    }

    private static Context getDeviceProtectedContext(final Context context) {
        return context.isDeviceProtectedStorage()
                ? context : context.createDeviceProtectedStorageContext();
    }

    private DeviceProtectedUtils() {
        // This utility class is not publicly instantiable.
    }
}
