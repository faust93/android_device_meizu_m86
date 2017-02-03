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

import android.app.ActionBar;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.view.Menu;
import android.view.MenuItem;
import android.preference.SwitchPreference;

public class TouchscreenGestureSettings extends PreferenceActivity {
    public static final String CATEGORY_GESTURES = "category_gestures";
    public static final String FPC_GESTURE_LEFT = "fpc_gesture_left";
    public static final String FPC_GESTURE_RIGHT = "fpc_gesture_right";
    public static final String FPC_GESTURE_RIGHT_MENU = "fpc_gesture_right_menu";
    public static final String FPC_GESTURE_LEFT_OH = "fpc_gesture_left_oh";
    public static final String FPC_GESTURE_RIGHT_OH = "fpc_gesture_right_oh";

    protected static PreferenceCategory gestureCat;

    protected static SwitchPreference fpc_gesture_left;
    protected static SwitchPreference fpc_gesture_right;
    protected static SwitchPreference fpc_gesture_right_menu;
    protected static SwitchPreference fpc_gesture_left_oh;
    protected static SwitchPreference fpc_gesture_right_oh;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.touchscreen_panel);

        fpc_gesture_left = (SwitchPreference) findPreference(FPC_GESTURE_LEFT);
        fpc_gesture_right = (SwitchPreference) findPreference(FPC_GESTURE_RIGHT);
        fpc_gesture_right_menu = (SwitchPreference) findPreference(FPC_GESTURE_RIGHT_MENU);
        fpc_gesture_left_oh = (SwitchPreference) findPreference(FPC_GESTURE_LEFT_OH);
        fpc_gesture_right_oh = (SwitchPreference) findPreference(FPC_GESTURE_RIGHT_OH);

        gestureCat = (PreferenceCategory) findPreference(CATEGORY_GESTURES);

        if (gestureCat != null) {
            gestureCat.setEnabled(CMActionsSettings.areGesturesEnabled());
        }
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        gestureCat = (PreferenceCategory) findPreference(CATEGORY_GESTURES);
        if (gestureCat != null) {
            gestureCat.setEnabled(CMActionsSettings.areGesturesEnabled());
        }
            getListView().setPadding(0, 0, 0, 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }
}
