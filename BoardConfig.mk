#
# Copyright (C) 2015 The CyanogenMod Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

TARGET_OTA_ASSERT_DEVICE := m86,PRO5

M86_PATH := device/meizu/m86

BOARD_VENDOR := meizu

# Include path
TARGET_SPECIFIC_HEADER_PATH := $(M86_PATH)/include

# Architecture
TARGET_ARCH := arm64
TARGET_ARCH_VARIANT := armv8-a
TARGET_CPU_ABI := arm64-v8a
TARGET_CPU_ABI2 :=
TARGET_CPU_VARIANT := generic

TARGET_2ND_ARCH := arm
TARGET_2ND_ARCH_VARIANT := armv7-a-neon
TARGET_2ND_CPU_ABI := armeabi-v7a
TARGET_2ND_CPU_ABI2 := armeabi
TARGET_2ND_CPU_VARIANT := cortex-a53.a57

ENABLE_CPUSETS := true
ENABLE_SCHEDBOOST := true

BOARD_USES_WFD_SERVICE := true

# Binder
TARGET_USES_64_BIT_BINDER := true

# Bootloader
TARGET_BOOTLOADER_BOARD_NAME := PRO5
TARGET_NO_BOOTLOADER := true

# Charger
BOARD_BATTERY_DEVICE_NAME := battery
BOARD_CHARGER_ENABLE_SUSPEND := true
BOARD_CHARGING_MODE_BOOTING_LPM := /sys/class/power_supply/battery/batt_lp_charging
CHARGING_ENABLED_PATH := "/sys/class/power_supply/battery/batt_lp_charging"

# Graphics
USE_OPENGL_RENDERER := true
NUM_FRAMEBUFFER_SURFACE_BUFFERS := 3

# Mixer
BOARD_USE_BGRA_8888 := true

# Include an expanded selection of fonts
EXTENDED_FONT_FOOTPRINT := true

#TARGET_RELEASETOOLS_EXTENSIONS := $(M86_PATH)

#WITH_DEXPREOPT := true

# Kernel
TARGET_KERNEL_ARCH := arm64
TARGET_KERNEL_HEADER_ARCH := arm64
TARGET_KERNEL_CROSS_COMPILE_PREFIX := aarch64-linux-android-
BOARD_KERNEL_BASE := 0x40078000
BOARD_KERNEL_CMDLINE :=
BOARD_KERNEL_PAGESIZE := 4096
#BOARD_MKBOOTIMG_ARGS := --base $(BOARD_KERNEL_BASE) --ramdisk_offset 0x01f88000 --tags_offset 0xfff88100 --pagesize $(BOARD_KERNEL_PAGESIZE)
BOARD_MKBOOTIMG_ARGS := --base $(BOARD_KERNEL_BASE) --ramdisk_offset 0x01f88000 --pagesize $(BOARD_KERNEL_PAGESIZE)
TARGET_KERNEL_CONFIG := cm_pro5_defconfig
TARGET_KERNEL_SOURCE := kernel/meizu/m576
TARGET_USES_UNCOMPRESSED_KERNEL := true
#BOARD_KERNEL_SEPARATED_DT := true

# Lights
TARGET_PROVIDES_LIBLIGHT := true

# NFC
BOARD_NFC_HAL_SUFFIX := default
BOARD_NFC_CHIPSET := pn547
TARGET_USES_NQ_NFC := true

# Partitions
TARGET_USERIMAGES_USE_EXT4 := true
BOARD_FLASH_BLOCK_SIZE := 131072
BOARD_BOOTIMAGE_PARTITION_SIZE := 25161728
BOARD_RECOVERYIMAGE_PARTITION_SIZE := 33550336
BOARD_SYSTEMIMAGE_PARTITION_SIZE := 2684350464
BOARD_USERDATAIMAGE_PARTITION_SIZE := 27241979904

# Platform
TARGET_BOARD_PLATFORM := exynos5
#TARGET_SLSI_VARIANT := blobs
#TARGET_SLSI_VARIANT := cm
#TARGET_SOC := exynos7420

BOARD_HAVE_OPENSOURCE_IMMVIBE := true

BOARD_USES_CYANOGEN_HARDWARE := true
BOARD_HARDWARE_CLASS += $(M86_PATH)/cmhw
#BOARD_HARDWARE_CLASS += hardware/samsung/cmhw

# Radio
#BOARD_MODEM_TYPE := ss333
#BOARD_PROVIDES_LIBRIL := true
BOARD_PROVIDES_RILD := true
BOARD_RIL_CLASS := ../../../device/meizu/m86/ril/m86RIL.java

# Recovery
TARGET_RECOVERY_FSTAB := $(M86_PATH)/rootdir/etc/fstab.m86
BOARD_HAS_NO_SELECT_BUTTON := true
BOARD_HAS_LARGE_FILESYSTEM := true
BOARD_HAS_NO_MISC_PARTITION := true
BOARD_USE_CUSTOM_RECOVERY_FONT := \"roboto_15x24.h\"
BOARD_SUPPRESS_SECURE_ERASE := true
BOARD_HAS_DOWNLOAD_MODE := true
DEVICE_RESOLUTION := 1080x1920
TARGET_RECOVERY_PIXEL_FORMAT := RGBA_8888
TARGET_USERIMAGES_USE_EXT4 := true

# TWRP Recovery
TW_THEME := portrait_hdpi
TW_INCLUDE_CRYPTO := true
TW_FLASH_FROM_STORAGE := true
TW_INTERNAL_STORAGE_PATH := "/data/media/0"
TW_INTERNAL_STORAGE_MOUNT_POINT := "data"
TW_EXTERNAL_STORAGE_PATH := "/external_sd"
TW_EXTERNAL_STORAGE_MOUNT_POINT := "external_sd"
TW_DEFAULT_EXTERNAL_STORAGE := true
TW_BRIGHTNESS_PATH := /sys/devices/virtual/backlight/pwm-backlight.0/brightness
TW_SCREEN_BLANK_ON_BOOT := true

# Renderscript
BOARD_OVERRIDE_RS_CPU_VARIANT_32 := cortex-a53
BOARD_OVERRIDE_RS_CPU_VARIANT_64 := cortex-a57

# Sensors
TARGET_NO_SENSOR_PERMISSION_CHECK := true

BOARD_GLOBAL_CFLAGS += -DMETADATA_CAMERA_SOURCE

# We have legacy camera
TARGET_HAS_LEGACY_CAMERA_HAL1 := true

# WEBGL
ENABLE_WEBGL := true

BOARD_USES_SCALER := true
BOARD_USES_HWC_SERVICES := true

TARGET_NEEDS_TEXT_RELOCATIONS := true

##
MAX_VIRTUAL_DISPLAY_DIMENSION := 1
MAX_EGL_CACHE_KEY_SIZE := 12*1024
MAX_EGL_CACHE_SIZE := 2048*1024
#TARGET_RUNNING_WITHOUT_SYNC_FRAMEWORK := true
TARGET_HAS_HH_VSYNC_ISSUE := true
VSYNC_EVENT_PHASE_OFFSET_NS := 7500000
SF_VSYNC_EVENT_PHASE_OFFSET_NS := 5000000
TARGET_FORCE_HWC_FOR_VIRTUAL_DISPLAYS := true

#BOARD_NEEDS_MEMORYHEAPION := true
#COMMON_GLOBAL_CFLAGS += -DEXYNOS5_ENHANCEMENTS
#COMMON_GLOBAL_CFLAGS += -DUSE_NATIVE_SEC_NV12TILED
#BOARD_USE_SAMSUNG_CAMERAFORMAT_NV21 := true

# for further investigation about custom params
##COMMON_GLOBAL_CFLAGS += -DSAMSUNG_CAMERA_HARDWARE

# Wifi
BOARD_WLAN_DEVICE                := bcmdhd
WPA_SUPPLICANT_VERSION           := VER_0_8_X
BOARD_WPA_SUPPLICANT_DRIVER      := NL80211
BOARD_WPA_SUPPLICANT_PRIVATE_LIB := lib_driver_cmd_bcmdhd
BOARD_HOSTAPD_DRIVER             := NL80211
BOARD_HOSTAPD_PRIVATE_LIB        := lib_driver_cmd_bcmdhd
WIFI_DRIVER_FW_PATH_PARAM        := "/sys/module/bcmdhd/parameters/firmware_path"
WIFI_DRIVER_FW_PATH_STA          := "/system/vendor/firmware/fw_bcmdhd.bin"
WIFI_DRIVER_FW_PATH_AP           := "/system/vendor/firmware/fw_bcmdhd_apsta.bin"
WIFI_BAND                        := 802_11_ABG

# Bluetooth
BOARD_BLUETOOTH_BDROID_BUILDCFG_INCLUDE_DIR := $(M86_PATH)/bluetooth
BOARD_BLUEDROID_VENDOR_CONF := $(M86_PATH)/bluetooth/libbt_vndcfg.txt
BOARD_HAVE_BLUETOOTH := true
BOARD_HAVE_BLUETOOTH_BCM := true

# SELinux
BOARD_SEPOLICY_DIRS := \
	device/meizu/m86/sepolicy

# Multirom
TARGET_RECOVERY_IS_MULTIROM := true
MR_NO_KEXEC := enabled
MR_CONTINUOUS_FB_UPDATE := true
MR_INPUT_TYPE := type_b
MR_INIT_DEVICES := $(M86_PATH)/multirom/mr_init_devices.c
MR_DPI := xhdpi
MR_DPI_FONT := 340
MR_USE_MROM_FSTAB := true
#MR_FSTAB := $(PLATFORM_PATH)/multirom/mrom.fstab
MR_FSTAB := $(M86_PATH)/twrp.fstab
MR_KEXEC_MEM_MIN := 0x0
MR_KEXEC_DTB := true
#MR_DEVICE_HOOKS := $(PLATFORM_PATH)/multirom/mr_hooks.c
#MR_DEVICE_HOOKS_VER := 5
#MR_INFOS := $(PLATFORM_PATH)/multirom/mrom_infos
MR_DEVICE_SPECIFIC_VERSION := e
MR_DEVICE_VARIANTS := m86
MR_PIXEL_FORMAT := "RGBA_8888"
MR_DTB_DEV := "/dev/block/sda25"

# inherit from the proprietary version
-include vendor/meizu/m86/BoardConfigVendor.mk
