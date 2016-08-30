# Copyright (C) 2016 faust93 <monumentum@gmail.com>
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

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	core.c \
	data.c \
	img.c \
	imgdev.c \
	fpc1150.c \
	nbis/bozorth3/bozorth3.c \
	nbis/bozorth3/bz_alloc.c \
	nbis/bozorth3/bz_drvrs.c \
	nbis/bozorth3/bz_gbls.c \
	nbis/bozorth3/bz_io.c \
	nbis/bozorth3/bz_sort.c \
	nbis/mindtct/binar.c \
	nbis/mindtct/contour.c \
	nbis/mindtct/dft.c \
	nbis/mindtct/globals.c \
	nbis/mindtct/init.c \
	nbis/mindtct/log.c \
	nbis/mindtct/maps.c \
	nbis/mindtct/minutia.c \
	nbis/mindtct/quality.c \
	nbis/mindtct/ridges.c \
	nbis/mindtct/sort.c \
	nbis/mindtct/block.c \
	nbis/mindtct/detect.c \
	nbis/mindtct/free.c \
	nbis/mindtct/imgutil.c \
	nbis/mindtct/line.c \
	nbis/mindtct/loop.c \
	nbis/mindtct/matchpat.c \
	nbis/mindtct/morph.c \
	nbis/mindtct/remove.c \
	nbis/mindtct/shape.c \
	nbis/mindtct/util.c

LOCAL_C_INCLUDES += $(LOCAL_PATH) $(LOCAL_PATH)/nbis/include

LOCAL_CFLAGS:= -DANDROID_STUB -fno-strict-aliasing

LOCAL_SHARED_LIBRARIES := libglib liblog libdl libutils

LOCAL_MODULE:=libfprint

LOCAL_EXPORT_C_INCLUDE_DIRS := $(LOCAL_PATH)

include $(BUILD_SHARED_LIBRARY)
