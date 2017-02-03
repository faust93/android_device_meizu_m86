/*
 * Copyright (C) 2016 faust93 <monumentum@gmail.com>
 * Based on FP HAL by Shane Francis / Jens Andersen for Sony Kitakami
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

#define LOG_TAG "AOSP FPC HAL"

#include <errno.h>
#include <malloc.h>
#include <string.h>
#include <cutils/log.h>
#include <hardware/hardware.h>
#include <hardware/fingerprint.h>
#include <pthread.h>
#include <endian.h>
#include <stdlib.h>

#include <stdbool.h>

#include "fprint.h"
#include "fp_internal.h"

#define MAX_TEMPLATES 5
#define ENROLL_TIMEOUT 15

int templates_enrolled = 0;
unsigned char tpl_mask = 0;

uint64_t challenge = 0;
uint64_t auth_id = 12345;
uint64_t operation = 0;
bool auth_thread_running = false;

pthread_t thread;
pthread_mutex_t lock;

fingerprint_notify_t callback;
char db_path[255];

struct fp_dev *fp_dev = NULL;
struct fp_print_data *fp_data = NULL;
struct fp_print_data *fp_tpls[MAX_TEMPLATES];


static uint64_t get_64bit_rand() {
    ALOGD("-> %s", __FUNCTION__);
    uint64_t r = (((uint64_t)rand()) << 32) | ((uint64_t)rand());
    return r != 0 ? r : 1;
}

static int fingerprint_free_templates() {
    int i;
     for(i = 0; i != templates_enrolled; i++){
        fp_print_data_free(fp_tpls[i]);
    }
    return i;
}

static int fingerprint_load_templates(struct fp_dev *fp_dev, char *db_path)
{
    struct fp_dscv_print **templates, *tpl;
    struct fp_print_data *fp_tpl_data = NULL;

    if(templates_enrolled > 0)
        fingerprint_free_templates();

    templates_enrolled = 0;
    tpl_mask = 0;
    memset(fp_tpls, 0 , sizeof(*fp_tpls));

    templates = fp_discover_prints(db_path);

    if(templates == NULL)
        return 0;

     for(int i = 0; (struct fp_dscv_print *)templates[i] != NULL; i++){
        tpl = (struct fp_dscv_print *)templates[i];
        ALOGD("%s finger: %d path: %s", __func__, tpl->finger, tpl->path);
        if((fp_print_data_from_dscv_print(tpl, &fp_tpl_data)) !=0 ) {
            ALOGE("Failed to load fingerprint\n");
            return -1;
        }
        tpl_mask |= 1 << (tpl->finger-1);
        fp_tpl_data->id = tpl->finger;
        fp_tpls[i] = fp_tpl_data;
        templates_enrolled++;
     }
    fp_dscv_prints_free(templates);

    ALOGD("%s %d templates loaded, tpl_mask: %d", __func__, templates_enrolled, tpl_mask);

    return 0;
}

void *enroll_thread_loop()
{
    struct fp_print_data *enrolled_print = NULL;

    ALOGD("%s", __func__);

    uint32_t print_count = fp_dev_get_nr_enroll_stages(fp_dev);
    ALOGD("%s : print count is : %u", __func__, print_count);

    int r, status = 1;
    int enroll_timeout = ENROLL_TIMEOUT;

    fp_dev_mode(fp_dev, 1);

    do {
        ALOGD("%s : Looking for Input", __func__);
        usleep(500);
        status = fp_enroll_finger(fp_dev, &enrolled_print);

        switch(status) {
            case FP_ENROLL_FAIL:
            case FP_ENROLL_RETRY:
            case FP_ENROLL_RETRY_TOO_SHORT:
            case FP_ENROLL_RETRY_CENTER_FINGER:
            case FP_ENROLL_RETRY_REMOVE_FINGER:
                ALOGE("Scan failed, please try again.\n");
                fingerprint_msg_t msg;
                msg.type = FINGERPRINT_ACQUIRED;
                msg.data.acquired.acquired_info = FINGERPRINT_ACQUIRED_INSUFFICIENT;
                callback(&msg);
        }
        //image captured
        if (status == FP_ENROLL_PASS || status == FP_ENROLL_COMPLETE) {
            ALOGI("%s : Enroll Step", __func__);
            print_count--;
            if (print_count > 0) {
                ALOGI("%s : Touches Remaining : %d", __func__, print_count);
                    fingerprint_msg_t msg;
                    msg.type = FINGERPRINT_TEMPLATE_ENROLLING;
                    msg.data.enroll.finger.fid = 0; //templates_enrolled + 1;
                    msg.data.enroll.finger.gid = 0;
                    msg.data.enroll.samples_remaining = print_count;
                    msg.data.enroll.msg = 0;
                    callback(&msg);
            } else {
                int bit = 0, print_index = 1; 
                for(int i = 0; i <= 5; i++){
                    if(((tpl_mask) & (1<<(i))) == 0){
                        print_index = i + 1;
                        break;
                    }
                }

                ALOGI("%s : Got print index : %d", __func__, print_index);

                r = fp_print_data_save(enrolled_print, print_index, db_path);
                if (r < 0) {
                    ALOGE("Data save failed, code %d\n", r);
                    fingerprint_msg_t msg;
                    msg.type = FINGERPRINT_ERROR;
                    msg.data.error = FINGERPRINT_ERROR_UNABLE_TO_PROCESS;
                    callback(&msg);
                    break;
                }
                uint32_t print_id = print_index;
                ALOGI("%s : Got print id : %lu auth id: %lu", __func__,(unsigned long) print_id);

                fingerprint_msg_t msg;
                msg.type = FINGERPRINT_TEMPLATE_ENROLLING;
                msg.data.enroll.finger.fid = print_id;
                msg.data.enroll.finger.gid = 0;
                msg.data.enroll.samples_remaining = 0;
                msg.data.enroll.msg = 0;
                callback(&msg);
                status = FP_ENROLL_COMPLETE;

                fingerprint_load_templates(fp_dev, db_path);

                goto out;
            }
        } else {
                enroll_timeout--;
        }

        pthread_mutex_lock(&lock);
        if (!auth_thread_running || !enroll_timeout) {
            pthread_mutex_unlock(&lock);
            goto out;
        }
        pthread_mutex_unlock(&lock);
    } while ( status != FP_ENROLL_COMPLETE);
out:
    // detach to prevent memory leak
    pthread_detach(thread);

    fp_enroll_reset(fp_dev);
    fp_print_data_free(enrolled_print);

    ALOGI("%s : finishing",__func__);

    ALOGD("%s set sensor to nav mode",__func__);
    fp_dev_mode(fp_dev, 0);

    pthread_mutex_lock(&lock);
    auth_thread_running = false;
    pthread_mutex_unlock(&lock);
    return NULL;
}

void *auth_thread_loop()
{
    ALOGD("%s tpls: %d auth_thr: %d", __func__, templates_enrolled, auth_thread_running);
    int match_idx = 0;
    int status = 1;

    pthread_mutex_lock(&lock);
    if(!templates_enrolled || auth_thread_running == false) {
        pthread_mutex_unlock(&lock);
        goto out;
    }
    pthread_mutex_unlock(&lock);

    fp_dev_mode(fp_dev, 1);

    do {
        usleep(1500);
        status = fp_identify_finger(fp_dev, fp_tpls, &match_idx);
        ALOGD("%s : verify thread loop", __func__);

        switch(status) {
            case FP_VERIFY_RETRY_TOO_SHORT:
            case FP_VERIFY_RETRY_CENTER_FINGER:
            case FP_VERIFY_RETRY_REMOVE_FINGER:
            case FP_VERIFY_RETRY:
            case FP_VERIFY_NO_MATCH:
                ALOGE("Auth failed, please try again.\n");
                    fingerprint_msg_t msg = {0};
                    msg.type = FINGERPRINT_ACQUIRED;
                    msg.data.acquired.acquired_info = FINGERPRINT_ACQUIRED_INSUFFICIENT;
                    callback(&msg);
            }

            if(status == FP_VERIFY_MATCH) {
                uint32_t print_id = match_idx+1;
                ALOGI("%s : Got print id : %lu", __func__, print_id);
                hw_auth_token_t hat = {0};
                hat.version = HW_AUTH_TOKEN_VERSION;
                hat.challenge = operation;
                hat.user_id = 0;
                hat.authenticator_id = auth_id;    // secure authenticator ID
                hat.authenticator_type = htobe32(HW_AUTH_FINGERPRINT);  // hw_authenticator_type_t, in network order
                hat.timestamp = time(NULL);           // in network order
                hat.hmac[0] = 1;

                ALOGI("%s : hat->challange %lu",__func__,(unsigned long) hat.challenge);
                ALOGI("%s : hat->user_id %lu",__func__,(unsigned long) hat.user_id);
                ALOGI("%s : hat->authenticator_id %lu",__func__,(unsigned long) hat.authenticator_id);
                ALOGI("%s : hat->authenticator_type %d",__func__, hat.authenticator_type);
                ALOGI("%s : hat->timestamp %lu",__func__,(unsigned long) hat.timestamp);
                ALOGI("%s : hat size %lu",__func__,(unsigned long) sizeof(hw_auth_token_t));

                fingerprint_msg_t msg = {0};
                msg.type = FINGERPRINT_AUTHENTICATED;
                msg.data.authenticated.finger.gid = 0;
                msg.data.authenticated.finger.fid = print_id;
                msg.data.authenticated.hat = hat;
                callback(&msg);
                goto out;
        }

        pthread_mutex_lock(&lock);
        if (!auth_thread_running) {
            pthread_mutex_unlock(&lock);
            goto out;
        }
        pthread_mutex_unlock(&lock);

    } while ( status != FP_VERIFY_MATCH );

out:
    // detach to prevent memory leak
    pthread_detach(thread);

    ALOGI("%s : finishing",__func__);

    ALOGD("%s setting fp sensor to nav mode",__func__);
    fp_dev_mode(fp_dev, 0);

    pthread_mutex_lock(&lock);
    auth_thread_running = false;
    pthread_mutex_unlock(&lock);

    return NULL;
}

static int fingerprint_close(hw_device_t *dev)
{
    ALOGD("%s +",__func__);
    fp_print_data_free(fp_data);
    fp_dev_close(fp_dev);
    fp_exit();
    return 0;
}

static uint64_t fingerprint_pre_enroll(struct fingerprint_device __unused *dev)
{
    challenge = get_64bit_rand(); 
    ALOGI("%s : Challange is : %jd",__func__,challenge);
    return challenge;
}

static int fingerprint_post_enroll(struct fingerprint_device __unused *dev) 
{
    ALOGD("%s",__func__);
    challenge = 0;

    pthread_mutex_lock(&lock);
    auth_thread_running = false;
    pthread_mutex_unlock(&lock);

    return 0;
}

static int fingerprint_enroll(struct fingerprint_device __unused *dev,
                              const hw_auth_token_t __unused *hat,
                              uint32_t __unused gid,
                              uint32_t __unused timeout_sec)
{
    ALOGD("%s",__func__);

    pthread_mutex_lock(&lock);
    bool thread_running = auth_thread_running;
    pthread_mutex_unlock(&lock);

    if (thread_running) {
        ALOGE("%s : Error, thread already running\n", __func__);
        return -1;
    }

    ALOGI("%s : hat->challange %lu",__func__,(unsigned long) hat->challenge);
    ALOGI("%s : hat->user_id %lu",__func__,(unsigned long) hat->user_id);
    ALOGI("%s : hat->authenticator_id %lu",__func__,(unsigned long) hat->authenticator_id);
    ALOGI("%s : hat->authenticator_type %d",__func__,hat->authenticator_type);
    ALOGI("%s : hat->timestamp %lu",__func__,(unsigned long) hat->timestamp);
    ALOGI("%s : hat size %lu",__func__,(unsigned long) sizeof(hw_auth_token_t));

    // from oppo 7r plus
    if (hat->challenge != challenge && !(hat->authenticator_type & HW_AUTH_FINGERPRINT)) {
            return -EPERM;
    }

    pthread_mutex_lock(&lock);
    auth_thread_running = true;
    pthread_mutex_unlock(&lock);

    if(pthread_create(&thread, NULL, enroll_thread_loop, NULL)) {
        ALOGE("%s : Error creating thread\n", __func__);
        auth_thread_running = false;
        return FINGERPRINT_ERROR;
    }

    return 0;

}

static uint64_t fingerprint_get_auth_id(struct fingerprint_device __unused *dev)
{
    uint64_t id = auth_id; //fpc_load_db_id();
    ALOGI("%s : ID : %jd",__func__,id );
    return id;
}

static int fingerprint_cancel(struct fingerprint_device __unused *dev)
{
    ALOGI("%s : +",__func__);

    pthread_mutex_lock(&lock);
    bool thread_running = auth_thread_running;
    pthread_mutex_unlock(&lock);

    ALOGI("%s : check thread running",__func__);
    if (!auth_thread_running) {
        ALOGI("%s : - (thread not running)",__func__);
        return 0;
    }

    pthread_mutex_lock(&lock);
    auth_thread_running = false;
    pthread_mutex_unlock(&lock);

    ALOGI("%s : join running thread",__func__);
    pthread_join(thread, NULL);

    ALOGI("%s : -",__func__);

    fingerprint_msg_t msg;
    msg.type = FINGERPRINT_ERROR;
    msg.data.error = FINGERPRINT_ERROR_CANCELED;
    callback(&msg);

    fp_dev_mode(fp_dev, 0);

    return 0;
}

static int fingerprint_remove(struct fingerprint_device __unused *dev,
                              uint32_t __unused gid, uint32_t __unused fid)
{
    ALOGD("%s : gid: %u fid: %u", __func__, gid, fid);

    fingerprint_msg_t msg;
    msg.type = FINGERPRINT_TEMPLATE_REMOVED;
    msg.data.removed.finger.fid = fid;
    msg.data.removed.finger.gid = gid;
    callback(&msg);

    fp_print_data_delete(fp_dev, fid);
    fingerprint_load_templates(fp_dev, db_path);
    return 0;
}


static int fingerprint_set_active_group(struct fingerprint_device __unused *dev,
                                        uint32_t __unused gid, const char __unused *store_path)
{
    int r;
    sprintf(db_path,"%s",store_path);
    ALOGI("%s : storage path set to : %s gid: %d",__func__, db_path, gid);

    r = fingerprint_load_templates(fp_dev, db_path);
    if (r != 0) {
        ALOGE("Failed to load fingerprint, error %d\n", r);
        return 0;
    }
    return 0;
}

static int fingerprint_enumerate(struct fingerprint_device *dev,
                                 fingerprint_finger_id_t *results,
                                 uint32_t *max_size)
{
    uint32_t print_count = templates_enrolled;

    ALOGD("%s : print count is : %u", __func__, print_count);
    if (*max_size == 0) {
        *max_size = print_count;
    } else {
        for (size_t i = 0; i < *max_size && i < print_count; i++) {
            results[i].fid = fp_tpls[i]->id;
            results[i].gid = 0;
        }
    }

    return print_count;
}

static int fingerprint_authenticate(struct fingerprint_device __unused *dev,
                                    uint64_t operation_id, __unused uint32_t gid)
{

    if (auth_thread_running) {
        ALOGE("%s : Error, thread already running\n", __func__);
        return -1;
    }

    operation = operation_id;

    pthread_mutex_lock(&lock);
    auth_thread_running = true;
    pthread_mutex_unlock(&lock);

    if(pthread_create(&thread, NULL, auth_thread_loop, NULL)) {
        ALOGE("%s : Error creating thread\n", __func__);
        auth_thread_running = false;
        return FINGERPRINT_ERROR;
    }

    return 0;
}

static int set_notify_callback(struct fingerprint_device *dev,
                               fingerprint_notify_t notify)
{
    /* Decorate with locks */
    dev->notify = notify;
    callback = notify;
    return 0;
}

static int fingerprint_open(const hw_module_t* module, const char __unused *id,
                            hw_device_t** device)
{

    int r;
    struct fp_dscv_dev *ddev;
    struct fp_dscv_dev **discovered_devs;
    struct fp_driver *drv;

    ALOGI("%s",__func__);

    if (device == NULL) {
        ALOGE("NULL device on open");
        return -EINVAL;
    }

    if (fp_init() < 0) {
        ALOGE("Could not init libfprint");
        return -EINVAL;
    }

    discovered_devs = fp_discover_devs();
    if (!discovered_devs) {
        ALOGE("Could not discover devices");
        return -EINVAL;
    }

    ddev = discovered_devs[0];
    drv = fp_dscv_dev_get_driver(ddev);
    ALOGD("Found device claimed by %s driver\n", fp_driver_get_full_name(drv));

    fp_dev = fp_dev_open(ddev);
    fp_dscv_devs_free(discovered_devs);
    if (!fp_dev) {
        ALOGE("Could not open device");
        return -EINVAL;
    }

    fingerprint_device_t *dev = malloc(sizeof(fingerprint_device_t));
    memset(dev, 0, sizeof(fingerprint_device_t));

    dev->common.tag = HARDWARE_DEVICE_TAG;
    dev->common.version = FINGERPRINT_MODULE_API_VERSION_2_0;
    dev->common.module = (struct hw_module_t*) module;
    dev->common.close = fingerprint_close;

    dev->pre_enroll = fingerprint_pre_enroll;
    dev->enroll = fingerprint_enroll;
    dev->post_enroll = fingerprint_post_enroll;
    dev->get_authenticator_id = fingerprint_get_auth_id;
    dev->cancel = fingerprint_cancel;
    dev->remove = fingerprint_remove;
    dev->set_active_group = fingerprint_set_active_group;
    dev->enumerate = fingerprint_enumerate;
    dev->authenticate = fingerprint_authenticate;
    dev->set_notify = set_notify_callback;
    dev->notify = NULL;

    operation = 0;
    challenge = get_64bit_rand();

    *device = (hw_device_t*) dev;
    return 0;
}

static struct hw_module_methods_t fingerprint_module_methods = {
    .open = fingerprint_open,
};

fingerprint_module_t HAL_MODULE_INFO_SYM = {
    .common = {
        .tag                = HARDWARE_MODULE_TAG,
        .module_api_version = FINGERPRINT_MODULE_API_VERSION_2_0,
        .hal_api_version    = HARDWARE_HAL_API_VERSION,
        .id                 = FINGERPRINT_HARDWARE_MODULE_ID,
        .name               = "libfprint Fingerprint HAL",
        .author             = "faust93",
        .methods            = &fingerprint_module_methods,
    },
};
