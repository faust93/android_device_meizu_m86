/*
 * Copyright (C) 2015 The CyanogenMod Project
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

import android.app.ActivityManagerNative;
import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TorchManager;
import android.media.session.MediaSessionLegacyHelper;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManagerGlobal;

import com.android.internal.os.DeviceKeyHandler;
import com.android.internal.util.ArrayUtils;

import android.app.Instrumentation;
import android.content.SharedPreferences;
import android.content.ContextWrapper;

public class KeyHandler implements DeviceKeyHandler {

    private static final String TAG = KeyHandler.class.getSimpleName();
    private static final int GESTURE_REQUEST = 1;

    private static final String KEY_GESTURE_HAPTIC_FEEDBACK =
            "touchscreen_gesture_haptic_feedback";
    private static final String KEY_FPC_TAP =
            "fpc_gesture_tap";
    private static final String KEY_FPC_LEFT =
            "fpc_gesture_left";
    private static final String KEY_FPC_RIGHT =
            "fpc_gesture_right";

    private static final String TOUCHSCREEN_GESTURE_HAPTIC_FEEDBACK =
            "touchscreen_gesture_haptic_feedback";

    private static final String ACTION_DISMISS_KEYGUARD =
            "com.android.keyguard.action.DISMISS_KEYGUARD_SECURELY";

    // Supported scancodes
    private static final int KEY_DOUBLE_TAP = 623;
    private static final int GESTURE_C_SCANCODE = 612;
    private static final int GESTURE_Z_SCANCODE = 622;
    private static final int GESTURE_DOWN_SCANCODE = 610;
    private static final int GESTURE_LTR_SCANCODE = 608;
    private static final int GESTURE_GTR_SCANCODE = 609;
    private static final int MODE_MUTE = 614; //M
    private static final int MODE_DO_NOT_DISTURB = 616; //S
    private static final int MODE_NORMAL = 621; //W

    private static final int GESTURE_FPC_TAP_SCANCODE = 189;
    private static final int GESTURE_FPC_LEFT_SCANCODE = 191;
    private static final int GESTURE_FPC_RIGHT_SCANCODE = 190;

    private static final int GESTURE_WAKELOCK_DURATION = 3000;

    private static final int[] sSupportedGestures = new int[] {
        KEY_DOUBLE_TAP,
        GESTURE_C_SCANCODE,
        GESTURE_Z_SCANCODE,
        GESTURE_DOWN_SCANCODE,
        GESTURE_LTR_SCANCODE,
        GESTURE_GTR_SCANCODE,
        MODE_MUTE,
        MODE_DO_NOT_DISTURB,
        MODE_NORMAL,
        GESTURE_FPC_TAP_SCANCODE,
        GESTURE_FPC_LEFT_SCANCODE,
        GESTURE_FPC_RIGHT_SCANCODE
    };

    private final Context mContext;

    private final PowerManager mPowerManager;
    private KeyguardManager mKeyguardManager;
    private EventHandler mEventHandler;
    private SensorManager mSensorManager;
    private TorchManager mTorchManager;
    private Sensor mProximitySensor;
    private Vibrator mVibrator;
    private WakeLock mProximityWakeLock;
    private WakeLock mGestureWakeLock;
    private int mProximityTimeOut;
    private boolean mProximityWakeSupported;
    private Instrumentation m_Instrumentation;
    private Context cmaContext = null;

    private boolean mNotificationSliderVibrate;

    public KeyHandler(Context context) {
        mContext = context;
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mEventHandler = new EventHandler();
        m_Instrumentation = new Instrumentation();
        mGestureWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "GestureWakeLock");

        try {
            cmaContext = mContext.createPackageContext("com.cyanogenmod.settings.device", Context.CONTEXT_RESTRICTED);
        } catch (Exception e) {
            e.printStackTrace();
        }

        final Resources resources = mContext.getResources();
        mProximityTimeOut = resources.getInteger(
                com.android.internal.R.integer.config_proximityCheckTimeout);
        mProximityWakeSupported = resources.getBoolean(
                com.android.internal.R.bool.config_proximityCheckOnWake);

        if (mProximityWakeSupported) {
            mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            mProximityWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "ProximityWakeLock");
        }

        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator == null || !mVibrator.hasVibrator()) {
            mVibrator = null;
        }
    }

    private void ensureKeyguardManager() {
        if (mKeyguardManager == null) {
            mKeyguardManager =
                    (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        }
    }

    private void ensureTorchManager() {
        if (mTorchManager == null) {
            mTorchManager = (TorchManager) mContext.getSystemService(Context.TORCH_SERVICE);
        }
    }

    private class EventHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            KeyEvent event = (KeyEvent) msg.obj;
            int scanCode = event.getScanCode();
            switch (scanCode) {
            case GESTURE_C_SCANCODE:
                ensureKeyguardManager();
                final String action;
                mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION);
                if (mKeyguardManager.isKeyguardSecure() && mKeyguardManager.isKeyguardLocked()) {
                    action = MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE;
                } else {
                    mContext.sendBroadcastAsUser(new Intent(ACTION_DISMISS_KEYGUARD),
                            UserHandle.CURRENT);
                    action = MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA;
                }
                mPowerManager.wakeUp(SystemClock.uptimeMillis());
                Intent intent = new Intent(action, null);
                startActivitySafely(intent);
                doHapticFeedback();
                break;
            case GESTURE_FPC_LEFT_SCANCODE:
                if(getCMApref(KEY_FPC_LEFT, false)) {
                    m_Instrumentation.sendKeyDownUpSync( KeyEvent.KEYCODE_MENU );
                    doHapticFeedback();
                }
                break;
            case GESTURE_FPC_RIGHT_SCANCODE:
                if(getCMApref(KEY_FPC_RIGHT, false)) {
                    m_Instrumentation.sendKeyDownUpSync( KeyEvent.KEYCODE_APP_SWITCH );
                    doHapticFeedback();
                }
                break;
            case GESTURE_Z_SCANCODE:
                dispatchMediaKeyWithWakeLockToMediaSession(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                doHapticFeedback();
                break;
            case GESTURE_DOWN_SCANCODE:
                ensureTorchManager();
                mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION);
                mTorchManager.toggleTorch();
                doHapticFeedback();
                break;
            case GESTURE_LTR_SCANCODE:
                dispatchMediaKeyWithWakeLockToMediaSession(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                doHapticFeedback();
                break;
            case GESTURE_GTR_SCANCODE:
                dispatchMediaKeyWithWakeLockToMediaSession(KeyEvent.KEYCODE_MEDIA_NEXT);
                doHapticFeedback();
                break;
            case MODE_MUTE:
            case MODE_DO_NOT_DISTURB:
            case MODE_NORMAL:
                int zenMode = Global.ZEN_MODE_OFF;
                if (scanCode == MODE_MUTE) {
                    zenMode = Global.ZEN_MODE_NO_INTERRUPTIONS;
                } else if (scanCode == MODE_DO_NOT_DISTURB) {
                    zenMode = Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS;
                }
                Global.putInt(mContext.getContentResolver(), Global.ZEN_MODE,
                        zenMode);
                if (mNotificationSliderVibrate) {
                    doHapticFeedback();
                }
                mNotificationSliderVibrate = true;
                break;
            }
        }
    }

    public boolean handleKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP) {
            return false;
        }
        boolean isKeySupported = ArrayUtils.contains(sSupportedGestures, event.getScanCode());
        if (isKeySupported && !mEventHandler.hasMessages(GESTURE_REQUEST)) {
            if (event.getScanCode() == KEY_DOUBLE_TAP && !mPowerManager.isScreenOn()) {
                mPowerManager.wakeUpWithProximityCheck(SystemClock.uptimeMillis());
                doHapticFeedback();
                return true;
            }
            Message msg = getMessageForKeyEvent(event);
            boolean defaultProximity = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_proximityCheckOnWakeEnabledByDefault);
            boolean proximityWakeCheckEnabled = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.PROXIMITY_ON_WAKE, defaultProximity ? 1 : 0) == 1;
            if (mProximityWakeSupported && proximityWakeCheckEnabled && mProximitySensor != null) {
                mEventHandler.sendMessageDelayed(msg, mProximityTimeOut);
                processEvent(event);
            } else {
                mEventHandler.sendMessage(msg);
            }
        }
        return isKeySupported;
    }

    private Message getMessageForKeyEvent(KeyEvent keyEvent) {
        Message msg = mEventHandler.obtainMessage(GESTURE_REQUEST);
        msg.obj = keyEvent;
        return msg;
    }

    private void processEvent(final KeyEvent keyEvent) {
        mProximityWakeLock.acquire();
        mSensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                mProximityWakeLock.release();
                mSensorManager.unregisterListener(this);
                if (!mEventHandler.hasMessages(GESTURE_REQUEST)) {
                    // The sensor took to long, ignoring.
                    return;
                }
                mEventHandler.removeMessages(GESTURE_REQUEST);
                if (event.values[0] == mProximitySensor.getMaximumRange()) {
                    Message msg = getMessageForKeyEvent(keyEvent);
                    mEventHandler.sendMessage(msg);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}

        }, mProximitySensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void dispatchMediaKeyWithWakeLockToMediaSession(int keycode) {
        MediaSessionLegacyHelper helper = MediaSessionLegacyHelper.getHelper(mContext);
        if (helper != null) {
            KeyEvent event = new KeyEvent(SystemClock.uptimeMillis(),
                    SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, keycode, 0);
            helper.sendMediaButtonEvent(event, true);
            event = KeyEvent.changeAction(event, KeyEvent.ACTION_UP);
            helper.sendMediaButtonEvent(event, true);
        } else {
            Log.w(TAG, "Unable to send media key event");
        }
    }

    private void startActivitySafely(Intent intent) {
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            UserHandle user = new UserHandle(UserHandle.USER_CURRENT);
            mContext.startActivityAsUser(intent, null, user);
        } catch (ActivityNotFoundException e) {
            // Ignore
        }
    }

    // TODO implement it more graceful way
    private boolean getCMApref(String prefKey, boolean defVal) {
        SharedPreferences cmaPrefs = null;
        cmaPrefs = cmaContext.getSharedPreferences("com.cyanogenmod.settings.device_preferences", Context.MODE_MULTI_PROCESS);
        return cmaPrefs.getBoolean(prefKey, defVal);
    }

    private void doHapticFeedback() {
        if (mVibrator == null) {
            return;
        }
        boolean enabled = getCMApref(TOUCHSCREEN_GESTURE_HAPTIC_FEEDBACK, false);
        if (enabled) {
            mVibrator.vibrate(50);
        }
    }
}
