/*
 * Copyright (C) 2015 The CyanogenMod Project
 * Copyright (C) 2016 faust93 adaptation for Meizu PRO5 FTS Driver
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

package com.cyanogenmod.settings.device;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;

import android.util.Log;

import com.cyanogenmod.settings.device.utils.FileUtils;

import java.lang.Integer;

public class CMActionsSettings {
    private static final String TAG = "CMActions";

    // Preference keys
    public static final String TOUCHSCREEN_GESTURE_CONTROL_KEY = "touchscreen_gesture_control";
    public static final String TOUCHSCREEN_DOUBLETAP_KEY = "touchscreen_gesture_dtap";
    public static final String TOUCHSCREEN_C_GESTURE_KEY = "touchscreen_gesture_c";
    public static final String TOUCHSCREEN_Z_GESTURE_KEY = "touchscreen_gesture_z";
    public static final String TOUCHSCREEN_LTR_GESTURE_KEY = "touchscreen_gesture_ltr";
    public static final String TOUCHSCREEN_GTR_GESTURE_KEY = "touchscreen_gesture_gtr";
    public static final String TOUCHSCREEN_S_GESTURE_KEY = "touchscreen_gesture_s";
    public static final String TOUCHSCREEN_W_GESTURE_KEY = "touchscreen_gesture_w";
    public static final String TOUCHSCREEN_M_GESTURE_KEY = "touchscreen_gesture_m";
    public static final String TOUCHSCREEN_YDOWN_GESTURE_KEY = "touchscreen_gesture_ydown";

    // Proc nodes
    public static final String TOUCHSCREEN_GESTURE_MODE_NODE = "/sys/devices/13660000.hsi2c/i2c-4/4-0049/gesture_control";

    // Key Masks
    public static final int KEY_MASK_DTP_CONTROL = 0x200;
    public static final int KEY_MASK_GESTURE_DTP = 0x1000000;

    public static final int KEY_MASK_SWIPE_CONTROL = 0x400;
    public static final int KEY_MASK_GESTURE_YDOWN = 0x4000000;
    public static final int KEY_MASK_GESTURE_LTR = 0x1000000;
    public static final int KEY_MASK_GESTURE_GTR = 0x2000000;

    public static final int KEY_MASK_GESTURE_CONTROL = 0x300;
    public static final int KEY_MASK_GESTURE_C = 0x2000000;
    public static final int KEY_MASK_GESTURE_E = 0x4000000;
    public static final int KEY_MASK_GESTURE_S = 0x14000000;
    public static final int KEY_MASK_GESTURE_V = 0x1000000;
    public static final int KEY_MASK_GESTURE_W = 0x8000000;
    public static final int KEY_MASK_GESTURE_Z = 0x40000000;
    public static final int KEY_MASK_GESTURE_O = 0x80000000;
    public static final int KEY_MASK_GESTURE_M = 0x10000000;

    public static final int DISABLE_ALL_MASK = 0x100;
    public static final int ENABLE_ALL_MASK = 0x1000100;

    private static boolean mIsGestureEnabled;
    private static boolean mIsGesture_DTP_Enabled;
    private static boolean mIsGesture_C_Enabled;
    private static boolean mIsGesture_E_Enabled;
    private static boolean mIsGesture_S_Enabled;
    private static boolean mIsGesture_V_Enabled;
    private static boolean mIsGesture_O_Enabled;
    private static boolean mIsGesture_M_Enabled;
    private static boolean mIsGesture_W_Enabled;
    private static boolean mIsGesture_Z_Enabled;
    private static boolean mIsGesture_YDOWN_Enabled;
    private static boolean mIsGesture_LTR_Enabled;
    private static boolean mIsGesture_GTR_Enabled;

    private final Context mContext;

    public CMActionsSettings(Context context ) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        loadPreferences(sharedPrefs);
        sharedPrefs.registerOnSharedPreferenceChangeListener(mPrefListener);
        mContext = context;
    }

    public static boolean areGesturesEnabled() {
        Log.d(TAG,"Are gestures enabled:" +mIsGestureEnabled);
        return mIsGestureEnabled;
    }

    public static void loadPreferences(SharedPreferences sharedPreferences) {
        mIsGestureEnabled = sharedPreferences.getBoolean(TOUCHSCREEN_GESTURE_CONTROL_KEY, false);
        mIsGesture_DTP_Enabled = sharedPreferences.getBoolean(TOUCHSCREEN_DOUBLETAP_KEY, false);
        mIsGesture_C_Enabled = sharedPreferences.getBoolean(TOUCHSCREEN_C_GESTURE_KEY, false);
        mIsGesture_S_Enabled = sharedPreferences.getBoolean(TOUCHSCREEN_S_GESTURE_KEY, false);
        mIsGesture_M_Enabled = sharedPreferences.getBoolean(TOUCHSCREEN_M_GESTURE_KEY, false);
        mIsGesture_W_Enabled = sharedPreferences.getBoolean(TOUCHSCREEN_W_GESTURE_KEY, false);
        mIsGesture_Z_Enabled = sharedPreferences.getBoolean(TOUCHSCREEN_Z_GESTURE_KEY, false);
        mIsGesture_YDOWN_Enabled = sharedPreferences.getBoolean(TOUCHSCREEN_YDOWN_GESTURE_KEY, false);
        mIsGesture_LTR_Enabled = sharedPreferences.getBoolean(TOUCHSCREEN_LTR_GESTURE_KEY, false);
        mIsGesture_GTR_Enabled = sharedPreferences.getBoolean(TOUCHSCREEN_GTR_GESTURE_KEY, false);

        updateGestureMode();
    }

    private SharedPreferences.OnSharedPreferenceChangeListener mPrefListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    boolean updated = true;

                    if (TOUCHSCREEN_GESTURE_CONTROL_KEY.equals(key)) {
                        mIsGestureEnabled = sharedPreferences.getBoolean(TOUCHSCREEN_GESTURE_CONTROL_KEY, false);
                        TouchscreenGestureSettings.gestureCat.setEnabled(areGesturesEnabled());
                    } else if (TOUCHSCREEN_DOUBLETAP_KEY.equals(key)) {
                        mIsGesture_DTP_Enabled = sharedPreferences.getBoolean(TOUCHSCREEN_DOUBLETAP_KEY, false);
                    } else if (TOUCHSCREEN_C_GESTURE_KEY.equals(key)) {
                        mIsGesture_C_Enabled = sharedPreferences.getBoolean(TOUCHSCREEN_C_GESTURE_KEY, false);
                    } else if (TOUCHSCREEN_S_GESTURE_KEY.equals(key)) {
                        mIsGesture_S_Enabled = sharedPreferences.getBoolean(TOUCHSCREEN_S_GESTURE_KEY, false);
                    } else if (TOUCHSCREEN_W_GESTURE_KEY.equals(key)) {
                        mIsGesture_W_Enabled = sharedPreferences.getBoolean(TOUCHSCREEN_W_GESTURE_KEY, false);
                    } else if (TOUCHSCREEN_M_GESTURE_KEY.equals(key)) {
                        mIsGesture_M_Enabled = sharedPreferences.getBoolean(TOUCHSCREEN_M_GESTURE_KEY, false);
                    } else if (TOUCHSCREEN_Z_GESTURE_KEY.equals(key)) {
                        mIsGesture_Z_Enabled = sharedPreferences.getBoolean(TOUCHSCREEN_Z_GESTURE_KEY, false);
                    } else if (TOUCHSCREEN_YDOWN_GESTURE_KEY.equals(key)) {
                        mIsGesture_YDOWN_Enabled = sharedPreferences.getBoolean(TOUCHSCREEN_YDOWN_GESTURE_KEY, false);
                    } else if (TOUCHSCREEN_LTR_GESTURE_KEY.equals(key)) {
                        mIsGesture_LTR_Enabled = sharedPreferences.getBoolean(TOUCHSCREEN_LTR_GESTURE_KEY, false);
                    } else if (TOUCHSCREEN_GTR_GESTURE_KEY.equals(key)) {
                        mIsGesture_GTR_Enabled = sharedPreferences.getBoolean(TOUCHSCREEN_GTR_GESTURE_KEY, false);
                    } else {
                        updated = false;
                    }
                    if (updated) {
                        updateGestureMode();
                    }
                }
            };

    /* Use bitwise logic to set gesture_mode in kernel driver.
       Check each if each key is enabled with & operator and KEY_MASK,
       if enabled toggle the appropriate bit with ^ XOR operator */
    public static void updateGestureMode() {
        int gesture_mode = 0;

        if (mIsGestureEnabled) {
            FileUtils.writeAsByte(TOUCHSCREEN_GESTURE_MODE_NODE, ENABLE_ALL_MASK);
            /* TAP_CTR masking */
            gesture_mode = (gesture_mode ^ KEY_MASK_DTP_CONTROL);
            if (mIsGesture_DTP_Enabled)
                gesture_mode = (gesture_mode ^ KEY_MASK_GESTURE_DTP);
            Log.d(TAG, "Gesture mode DoubleTap: " + gesture_mode);
            FileUtils.writeAsByte(TOUCHSCREEN_GESTURE_MODE_NODE, gesture_mode);
            gesture_mode = 0;
            /* SWIPE_CTR masking */
            gesture_mode = (gesture_mode ^ KEY_MASK_SWIPE_CONTROL);
            if (mIsGesture_YDOWN_Enabled)
                gesture_mode = (gesture_mode ^ KEY_MASK_GESTURE_YDOWN);
            if (mIsGesture_LTR_Enabled)
                gesture_mode = (gesture_mode ^ KEY_MASK_GESTURE_LTR);
            if (mIsGesture_GTR_Enabled)
                gesture_mode = (gesture_mode ^ KEY_MASK_GESTURE_GTR);
            Log.d(TAG, "Gesture mode Swipe: " + gesture_mode);
            FileUtils.writeAsByte(TOUCHSCREEN_GESTURE_MODE_NODE, gesture_mode);
            gesture_mode = 0;
            /* UNICODE_CTR masking */
            gesture_mode = (gesture_mode ^ KEY_MASK_GESTURE_CONTROL);
            if (((gesture_mode & KEY_MASK_GESTURE_C) == 1) != mIsGesture_C_Enabled)
                gesture_mode = (gesture_mode ^ KEY_MASK_GESTURE_C);
            if (((gesture_mode & KEY_MASK_GESTURE_S) == 1) != mIsGesture_S_Enabled)
                    gesture_mode = (gesture_mode ^ KEY_MASK_GESTURE_S);
            if (((gesture_mode & KEY_MASK_GESTURE_W) == 1) != mIsGesture_W_Enabled)
                    gesture_mode = (gesture_mode ^ KEY_MASK_GESTURE_W);
            if (((gesture_mode & KEY_MASK_GESTURE_M) == 1) != mIsGesture_M_Enabled)
                    gesture_mode = (gesture_mode ^ KEY_MASK_GESTURE_M);
            if (((gesture_mode & KEY_MASK_GESTURE_Z) == 1) != mIsGesture_Z_Enabled)
                    gesture_mode = (gesture_mode ^ KEY_MASK_GESTURE_Z);
        } else {
            gesture_mode = DISABLE_ALL_MASK;
        }
        Log.d(TAG, "finished gesture mode: " + gesture_mode);
        FileUtils.writeAsByte(TOUCHSCREEN_GESTURE_MODE_NODE, gesture_mode);
    }

}
