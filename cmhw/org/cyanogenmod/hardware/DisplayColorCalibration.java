/*
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2016 faust93 at monumentum@gmail.com
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

package org.cyanogenmod.hardware;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Slog;

import org.cyanogenmod.internal.util.FileUtils;

public class DisplayColorCalibration {

    private static final String TAG = "DisplayColorCalibration";

    private static final String COLOR_FILE = "/sys/devices/13930000.decon_fb/scr";

    private static final int MIN = 0;
    private static final int MAX = 255;

    private static final int[] sCurColors = new int[] { MAX, MAX, MAX };


    public static boolean isSupported() {
        FileUtils.writeLine(COLOR_FILE, "O 1");
        return true;
    }

    public static int getMaxValue()  {
        return MAX;
    }

    public static int getMinValue()  {
        return MIN;
    }

    public static int getDefValue() {
        return getMaxValue();
    }

    public static String getCurColors()  {

        return String.format("%d %d %d", sCurColors[0],
                sCurColors[1], sCurColors[2]);
    }

    public static boolean setColors(String colors) {

        String r,g,b;
        String[] rgb = colors.split(" ");

        sCurColors[0] = Integer.parseInt(rgb[0]);
        sCurColors[1] = Integer.parseInt(rgb[1]);
        sCurColors[2] = Integer.parseInt(rgb[2]);

        r = String.format("%02x",sCurColors[0]);
        g = String.format("%02x",sCurColors[1]);
        b = String.format("%02x",sCurColors[2]);

//        Slog.e(TAG, "Colors: " + colors);
//        Slog.e(TAG, "Colors RGB: " + r + g + b);

        FileUtils.writeLine(COLOR_FILE, "R " + r + "0000");
        FileUtils.writeLine(COLOR_FILE, "G " + "00" + g + "00");
        FileUtils.writeLine(COLOR_FILE, "B " + "0000" + b);
        FileUtils.writeLine(COLOR_FILE, "W " + r + g + b);

        return true;
    }

}
