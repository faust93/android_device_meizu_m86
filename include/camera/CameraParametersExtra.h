/*
 * Copyright (C) 2014 The CyanogenMod Project
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

#define CAMERA_PARAMETERS_EXTRA_C \
    const char CameraParameters::PIXEL_FORMAT_YUV420SP_NV21[] = "nv21"; \
    const char CameraParameters::EFFECT_CARTOONIZE[] = "cartoonize"; \
    const char CameraParameters::EFFECT_POINT_RED_YELLOW[] = "point-red-yellow"; \
    const char CameraParameters::EFFECT_POINT_GREEN[] = "point-green"; \
    const char CameraParameters::EFFECT_POINT_BLUE[] = "point-blue"; \
    const char CameraParameters::EFFECT_VINTAGE_COLD[] = "vintage-cold"; \
    const char CameraParameters::EFFECT_VINTAGE_WARM[] = "vintage-warm"; \
    const char CameraParameters::EFFECT_WASHED[] = "washed"; \
    const char CameraParameters::ISO_AUTO[] = "auto"; \
    const char CameraParameters::ISO_NIGHT[] = "night"; \
    const char CameraParameters::ISO_SPORTS[] = "sports"; \
    const char CameraParameters::ISO_6400[] = "6400"; \
    const char CameraParameters::ISO_3200[] = "3200"; \
    const char CameraParameters::ISO_1600[] = "1600"; \
    const char CameraParameters::ISO_800[] = "800"; \
    const char CameraParameters::ISO_400[] = "400"; \
    const char CameraParameters::ISO_200[] = "200"; \
    const char CameraParameters::ISO_100[] = "100"; \
    const char CameraParameters::ISO_80[] = "80"; \
    const char CameraParameters::ISO_50[] = "50"; \
    const char CameraParameters::KEY_SUPPORTED_METERING_MODE[] = "metering-values"; \
    const char CameraParameters::METERING_CENTER[] = "center"; \
    const char CameraParameters::METERING_MATRIX[] = "matrix"; \
    const char CameraParameters::METERING_SPOT[] = "spot"; \
    const char CameraParameters::METERING_OFF[] = "off"; \
    const char CameraParameters::KEY_DYNAMIC_RANGE_CONTROL[] = "dynamic-range-control"; \
    const char CameraParameters::KEY_SUPPORTED_PHASE_AF[] = "phase-af-values"; \
    const char CameraParameters::KEY_PHASE_AF[] = "phase-af"; \
    const char CameraParameters::KEY_SUPPORTED_RT_HDR[] = "rt-hdr-values"; \
    const char CameraParameters::KEY_RT_HDR[] = "rt-hdr";

#define CAMERA_PARAMETERS_EXTRA_H \
    static const char PIXEL_FORMAT_YUV420SP_NV21[]; \
    static const char EFFECT_CARTOONIZE[]; \
    static const char EFFECT_POINT_RED_YELLOW[]; \
    static const char EFFECT_POINT_GREEN[]; \
    static const char EFFECT_POINT_BLUE[]; \
    static const char EFFECT_VINTAGE_COLD[]; \
    static const char EFFECT_VINTAGE_WARM[]; \
    static const char EFFECT_WASHED[]; \
    static const char ISO_AUTO[]; \
    static const char ISO_NIGHT[]; \
    static const char ISO_SPORTS[]; \
    static const char ISO_6400[]; \
    static const char ISO_3200[]; \
    static const char ISO_1600[]; \
    static const char ISO_800[]; \
    static const char ISO_400[]; \
    static const char ISO_200[]; \
    static const char ISO_100[]; \
    static const char ISO_80[]; \
    static const char ISO_50[]; \
    static const char KEY_SUPPORTED_METERING_MODE[]; \
    static const char METERING_CENTER[]; \
    static const char METERING_MATRIX[]; \
    static const char METERING_SPOT[]; \
    static const char METERING_OFF[]; \
    static const char KEY_DYNAMIC_RANGE_CONTROL[]; \
    static const char KEY_SUPPORTED_PHASE_AF[]; \
    static const char KEY_PHASE_AF[]; \
    static const char KEY_SUPPORTED_RT_HDR[]; \
    static const char KEY_RT_HDR[];
