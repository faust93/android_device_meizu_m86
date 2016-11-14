/*
 * Copyright (C) 2015 The CyanogenMod Project
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

import cyanogenmod.hardware.DisplayMode;

import org.cyanogenmod.internal.util.FileUtils;

/*
 * Display Modes API
 *
 * A device may implement a list of preset display modes for different
 * viewing intents, such as movies, photos, or extra vibrance. These
 * modes may have multiple components such as gamma correction, white
 * point adjustment, etc, but are activated by a single control point.
 *
 * This API provides support for enumerating and selecting the
 * modes supported by the hardware.
 */

public class DisplayModeControl {

    private static final String CONT_FILE = "/sys/devices/13930000.decon_fb/cont";
    private static final String DEFAULT_PATH = "/data/misc/.displaymodedefault";

    private static final String GAMMA_1 = "0,3,6,9,12,16,19,22,25,29,32,35,39,43,46,50,54,58,62,66,71,75,80,84,89,94,99,103,108,113,118,123,128,133,138,143,148,153,158,162,167,172,176,181,185,190,194,198,202,206,210,213,217,221,224,227,231,234,237,240,243,247,250,253,256,0,3,6,9,12,16,19,22,25,29,32,35,39,43,46,50,54,58,62,66,71,75,80,84,89,94,99,103,108,113,118,123,128,133,138,143,148,153,158,162,167,172,176,181,185,190,194,198,202,206,210,213,217,221,224,227,231,234,237,240,243,247,250,253,256,0,3,6,9,12,16,19,22,25,29,32,35,39,43,46,50,54,58,62,66,71,75,80,84,89,94,99,103,108,113,118,123,128,133,138,143,148,153,158,162,167,172,176,181,185,190,194,198,202,206,210,213,217,221,224,227,231,234,237,240,243,247,250,253,256,";
    private static final String GAMMA_2 = "0,4,9,13,18,22,27,31,36,40,45,49,54,58,63,67,72,76,81,85,90,94,99,103,108,112,117,121,126,130,135,139,144,148,153,158,163,168,173,178,183,188,193,198,203,208,212,216,220,224,227,230,233,236,239,241,243,245,247,249,251,252,253,254,256,0,4,9,13,18,22,27,31,36,40,45,49,54,58,63,67,72,76,81,85,90,94,99,103,108,112,117,121,126,130,135,139,144,148,153,158,163,168,173,178,183,188,193,198,203,208,212,216,220,224,227,230,233,236,239,241,243,245,247,249,251,252,253,254,256,0,4,9,13,18,22,27,31,36,40,45,49,54,58,63,67,72,76,81,85,90,94,99,103,108,112,117,121,126,130,135,139,144,148,153,158,163,168,173,178,183,188,193,198,203,208,212,216,220,224,227,230,233,236,239,241,243,245,247,249,251,252,253,254,256,";
    private static final String GAMMA_3 = "0,2,3,5,6,8,10,12,14,16,18,21,24,27,30,33,37,41,46,50,55,61,66,72,78,84,91,98,104,111,118,125,132,139,146,152,159,166,172,178,184,190,196,201,206,211,215,219,222,226,229,232,234,237,239,241,243,245,247,248,250,251,253,254,255,0,2,3,5,6,8,10,12,14,16,18,21,24,27,30,33,37,41,46,50,55,61,66,72,78,84,91,98,104,111,118,125,132,139,146,152,159,166,172,178,184,190,196,201,206,211,215,219,222,226,229,232,234,237,239,241,243,245,247,248,250,251,253,254,255,0,2,3,5,6,8,10,12,14,16,18,21,24,27,30,33,37,41,46,50,55,61,66,72,78,84,91,98,104,111,118,125,132,139,146,152,159,166,172,178,184,190,196,201,206,211,215,219,222,226,229,232,234,237,239,241,243,245,247,248,250,251,253,254,255,";

    private static int current_mode = 0;

    private static final DisplayMode[] modes = new DisplayMode[4];

    /*
     * All HAF classes should export this boolean.
     * Real implementations must, of course, return true
     */
    public static boolean isSupported() {

        modes[0] = new DisplayMode(0,"Stock Gamma");
        modes[1] = new DisplayMode(1,"Low Gamma");
        modes[2] = new DisplayMode(2,"Mid Gamma");
        modes[3] = new DisplayMode(3,"High Saturated");

        return true; 
    }

    /*
     * Get the list of available modes. A mode has an integer
     * identifier and a string name.
     *
     * It is the responsibility of the upper layers to
     * map the name to a human-readable format or perform translation.
     */
    public static DisplayMode[] getAvailableModes() {
        return modes;
    }

    /*
     * Get the name of the currently selected mode. This can return
     * null if no mode is selected.
     */
    public static DisplayMode getCurrentMode() {
          return getDefaultMode();
//        return modes[current_mode];
    }

    /*
     * Selects a mode from the list of available modes by it's
     * string identifier. Returns true on success, false for
     * failure. It is up to the implementation to determine
     * if this mode is valid.
     */
    public static boolean setMode(DisplayMode mode, boolean makeDefault) {
        if (mode == null) {
                return false;
            }

        String profile;
        current_mode = mode.id;

        FileUtils.writeLine(DEFAULT_PATH, String.valueOf(current_mode));

        if(current_mode == 0){
            FileUtils.writeLine(CONT_FILE, "O 0");
            return true;
        }

        switch(current_mode) {
            case 1:
                profile = GAMMA_1;
                break;
            case 2:
                profile = GAMMA_2;
                break;
            case 3:
                profile = GAMMA_3;
                break;
            default:
                profile = GAMMA_1;
                break;
        }


        FileUtils.writeLine(CONT_FILE, "O 1");
        FileUtils.writeLine(CONT_FILE, profile);

        return true;
    }

    /*
     * Gets the preferred default mode for this device by it's
     * string identifier. Can return null if there is no default.
     */
    public static DisplayMode getDefaultMode() {
        try {
                int mode = Integer.parseInt(FileUtils.readOneLine(DEFAULT_PATH));
                return modes[mode];
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                return modes[1];
            }
    }
}
