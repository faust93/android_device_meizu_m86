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

# For 64 bit
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit some common CM stuff.
$(call inherit-product, vendor/cm/config/common_full_phone.mk)

# Inherit from m86 device
$(call inherit-product, device/meizu/m86/device.mk)

# Enhanced NFC
$(call inherit-product, vendor/cm/config/nfc_enhanced.mk)

# Set those variables here to overwrite the inherited values.
PRODUCT_BRAND := Meizu
PRODUCT_MANUFACTURER := Meizu
PRODUCT_MODEL := PRO5

TARGET_VENDOR_PRODUCT_NAME := meizu_PRO5
TARGET_VENDOR_DEVICE_NAME := PRO5

PRODUCT_BUILD_PROP_OVERRIDES += \
        TARGET_DEVICE=PRO5 \
        PRODUCT_NAME=meizu_PRO5 \
        PRODUCT_MODEL="PRO 5" \
        BUILD_FINGERPRINT=Meizu/meizu_PRO5/PRO5:5.1/LMY47D/m86.Flyme_OS_5.1460049852:user/release-keys \
        PRIVATE_BUILD_DESC="meizu_PRO5-user 5.1 LMY47D m86.Flyme_OS_5.1460049852 release-keys"

