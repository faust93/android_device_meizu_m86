# Copyright (C) 2012 The Android Open Source Project
# Copyright (C) 2013 The CyanogenMod Project
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

""" Custom OTA commands """

import common
import re
import os

def FullOTA_InstallEnd(info):
  info.script.Mount("/system")
  info.script.AppendExtra('ui_print("updateing dtb...");')
  info.script.AppendExtra('package_extract_file("dtb_cm", "/dev/block/platform/15570000.ufs/by-name/dtb");')
  info.script.Unmount("/system")

