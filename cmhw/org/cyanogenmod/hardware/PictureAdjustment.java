/*
 * Copyright (C) 2016 The CyanogenMod Project
 * Copyright (C) 2016 The faust93 at monumentum@gmail.com
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

import android.annotation.TargetApi;
import android.util.Range;

import android.util.Slog;
import org.cyanogenmod.internal.util.FileUtils;

import cyanogenmod.hardware.HSIC;

/**
 * Picture adjustment support
 *
 * Allows tuning of hue, saturation, intensity, and contrast levels
 * of the display
 */
public class PictureAdjustment {

    private static final String TAG = "PictureAdjustment";

    private static final String HUE_FILE = "/sys/devices/13930000.decon_fb/hue";
    private static final String SAT_FILE = "/sys/devices/13930000.decon_fb/sat";
    private static final String AD_FILE = "/sys/devices/13930000.decon_fb/ad";

    private static HSIC hsic = new HSIC(0.0f, 0.0f, 0.0f, 0.0f, 0.0f);

    private static final int[] curColors = new int[] { 0, 0, 0, 0 };

    /**
     * Whether device supports picture adjustment
     *
     * @return boolean Supported devices must return always true
     */
    public static boolean isSupported() {
        FileUtils.writeLine(HUE_FILE, "O 1");
        FileUtils.writeLine(SAT_FILE, "O 1");
        FileUtils.writeLine(AD_FILE, "O 1");
        return true;
    }

    /**
     * This method returns the current picture adjustment values based
     * on the selected DisplayMode.
     *
     * @return the HSIC object or null if not supported
     */
    public static HSIC getHSIC() {
        Slog.e(TAG, "get hsic");
        return hsic;
    }

    /**
     * This method returns the default picture adjustment values.
     *
     * If DisplayModes are available, this may change depending on the
     * selected mode.
     *
     * @return the HSIC object or null if not supported
     */
    public static HSIC getDefaultHSIC() {
        Slog.e(TAG, "get defhsic");
        return hsic;
    }

    /**
     * This method allows to set the picture adjustment
     *
     * @param hsic
     * @return boolean Must be false if feature is not supported or the operation
     * failed; true in any other case.
     */
    public static boolean setHSIC(final HSIC hsic) {

        int h = (int)hsic.getHue();
        int s = (int)((byte)hsic.getSaturation() & 0x000000FF);
        int i = (int)hsic.getIntensity();
        int c = 255 - (int)hsic.getContrast();

        String hue  = String.format("%x",h);
        String sat  = String.format("%x",s);
        String intens  = String.format("%02x",i);
        String cont  = String.format("%02x",c);

        Slog.e(TAG, "hsic hue: " + hue + " sat: " + sat + " int: " + intens + " cont: " + cont);

        if(h != curColors[0]){
            curColors[0] = h;
            FileUtils.writeLine(HUE_FILE, "R " + hue);
            FileUtils.writeLine(HUE_FILE, "G " + hue);
            FileUtils.writeLine(HUE_FILE, "B " + hue);

            FileUtils.writeLine(HUE_FILE, "C " + hue);
            FileUtils.writeLine(HUE_FILE, "M " + hue);
            FileUtils.writeLine(HUE_FILE, "Y " + hue);

        }
        if(s != curColors[1]){
            curColors[1] = s;
            FileUtils.writeLine(SAT_FILE, "T " + sat);
        }

        if(i != curColors[2] || c != curColors[3]){
            curColors[2] = i;
            curColors[3] = c;
            FileUtils.writeLine(AD_FILE, "A " + intens + intens + intens);
            FileUtils.writeLine(AD_FILE, "B " + cont + cont + cont);

        }

        return true;
    }

    /**
     * Get the range available for hue adjustment
     * @return range of floats
     */
    public static Range<Float> getHueRange() {
        return new Range(0.0f, 255.0f);
    }

    /**
     * Get the range available for saturation adjustment
     * @return range of floats
     */
    public static Range<Float> getSaturationRange() {
        return new Range(-127.0f, 127.0f);
    }

    /**
     * Get the range available for intensity adjustment
     * @return range of floats
     */
    public static Range<Float> getIntensityRange() {
        return new Range(0.0f, 255.0f);
    }

    /**
     * Get the range available for contrast adjustment
     * @return range of floats
     */
    public static Range<Float> getContrastRange() {
        return new Range(0.0f, 255.0f);
    }

    /**
     * Get the range available for saturation threshold adjustment
     *
     * This is the threshold where the display becomes fully saturated
     *
     * @return range of floats
     */
    public static Range<Float> getSaturationThresholdRange() {
        return new Range(0.0f, 255.0f);
    }
}
