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

/* status_t Parcel::writeString16 */

#include <binder/Parcel.h>

namespace android {

// ---------------------------------------------------------------------------

extern "C" {

    /* status_t Parcel::writeString16(const char16_t* str, size_t len) */
    status_t _ZN7android6Parcel13writeString16EPKtm(Parcel* thiz, unsigned short const* str, unsigned long len) {
	return thiz->writeString16((const char16_t*)str, (size_t)len);
    }

//    status_t __attribute__((weak)) _ZN7android5Fence4waitEj (unsigned int timeout);
//
//    status_t _ZN7android5Fence4waitEi (int timeout) {
//        return _ZN7android5Fence4waitEj((unsigned int)(timeout));
//    }
}

// ---------------------------------------------------------------------------

}; // namespace android