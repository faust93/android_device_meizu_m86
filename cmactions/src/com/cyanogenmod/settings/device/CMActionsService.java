/*
 * Copyright (c) 2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.settings.device;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import java.util.List;
import java.util.LinkedList;


public class CMActionsService extends IntentService {
    private static final String TAG = "CMActions";
    private final Context mContext;

    public CMActionsService(Context context) {
        super("CMActionService");
        mContext = context;
        Log.d(TAG, "Starting");
        CMActionsSettings cmActionsSettings = new CMActionsSettings(context );
    }

    @Override
    protected void onHandleIntent(Intent intent) {
    }
}

