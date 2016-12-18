/*
 * Copyright (C) 2008 The Android Open Source Project
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


#define LOG_NDEBUG 0

#include <cutils/log.h>

#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <fcntl.h>
#include <pthread.h>

#include <sys/ioctl.h>
#include <sys/types.h>

#include <hardware/lights.h>

/******************************************************************************/

#define MAX_PATH_SIZE 80

#define MODE_DEFAULT  0x00
#define MODE_BREATH   0x01
#define MODE_BLINK    0x02
#define MODE_MY       0x04

#define L_NOTIFICATION  0x01
#define L_BATTERY       0x02
#define L_ATTENTION     0x03
#define L_NONE          0xFF //255

static pthread_once_t g_init = PTHREAD_ONCE_INIT;
static pthread_mutex_t g_lock = PTHREAD_MUTEX_INITIALIZER;
static struct light_state_t g_notification;
static struct light_state_t g_battery;
static int g_attention = 0;

char const*const LED_FILE
        = "/sys/devices/gpioi2c0.14/i2c-12/12-0030/leds/m86_led/brightness";

char const*const LED_MODE_FILE
        = "/sys/devices/gpioi2c0.14/i2c-12/12-0030/led_pattern";

char const*const LED_MY_PATTERN_FILE
        = "/sys/devices/gpioi2c0.14/i2c-12/12-0030/my_pattern";

char const*const LCD_FILE
        = "/sys/devices/virtual/backlight/pwm-backlight.0/brightness";

char const*const BATTERY_STATUS
        = "/sys/class/power_supply/bq2753x-0/status";
/**
 * device methods
 */

void init_globals(void)
{
    // init the mutex
    pthread_mutex_init(&g_lock, NULL);
}

static int
write_str(char const* path, char *value)
{
    int fd;
    static int already_warned = 0;

    fd = open(path, O_RDWR);
    if (fd >= 0) {
        char buffer[64];
        int bytes = sprintf(buffer, "%s\n", value);
        int amt = write(fd, buffer, bytes);
        close(fd);
        return amt == -1 ? -errno : 0;
    } else {
        if (already_warned == 0) {
            ALOGD("write_str failed to open %s\n", path);
            already_warned = 1;
        }
        return -errno;
    }
}

static int
write_int(char const* path, int value)
{
    int fd;
    static int already_warned = 0;

    fd = open(path, O_RDWR);
    if (fd >= 0) {
        char buffer[20];
        int bytes = sprintf(buffer, "%d\n", value);
        int amt = write(fd, buffer, bytes);
        close(fd);
        return amt == -1 ? -errno : 0;
    } else {
        if (already_warned == 0) {
            ALOGE("write_int failed to open %s\n", path);
            already_warned = 1;
        }
        return -errno;
    }
}

static int
is_avail(char const* path)
{
    int fd = open(path, O_RDWR);
    if (fd >= 0) {
        close(fd);
        return 1;
    } else {
        return 0;
    }
}

static int
is_lit(struct light_state_t const* state)
{
    return state->color & 0x00ffffff;
}

static int
rgb_to_brightness(struct light_state_t const* state)
{
    int color = state->color & 0x00ffffff;
    return ((77*((color>>16)&0x00ff))
            + (150*((color>>8)&0x00ff)) + (29*(color&0x00ff))) >> 8;
}

static int
set_light_backlight(struct light_device_t* dev,
        struct light_state_t const* state)
{
    int err = 0;
    int brightness = rgb_to_brightness(state);
    pthread_mutex_lock(&g_lock);
    err = write_int(LCD_FILE, brightness);
    pthread_mutex_unlock(&g_lock);
    return err;
}

static int
set_light_locked(struct light_state_t const* state, int type)
{
    int blink, mode = 0;
    int onMS, offMS;
    char buffer[50];
    unsigned int colorRGB, brightness;

    switch (state->flashMode) {
        case LIGHT_FLASH_TIMED:
        case LIGHT_FLASH_HARDWARE:
            onMS = state->flashOnMS;
            offMS = state->flashOffMS;
            break;
        case LIGHT_FLASH_NONE:
        default:
            onMS = 0;
            offMS = 0;
            break;
    }

    brightness = rgb_to_brightness(state);

    if(type == L_BATTERY && brightness != 0){  //battery charging

        char charging_status[15];
        FILE* fp = fopen(BATTERY_STATUS, "rb");
        fgets(charging_status, 14, fp);
        fclose(fp);

        if (strstr(charging_status, "Charging") != NULL) {
                onMS = 2500;
                offMS = 2500;
            } else {
                onMS = 0;
            }
    }

    colorRGB = state->color;

    if (onMS > 0) {
        blink = 1;
        mode = MODE_MY;
        if(onMS!=1){
            onMS = onMS / 60;
            offMS = offMS / 60;
        }
    } else {
        blink = 0;
        mode = MODE_DEFAULT;
    }

#if 1
    ALOGD("set_speaker_light_locked mode %d, colorRGB=%08X, onMS=%d, offMS=%d mode:%d\n",
            state->flashMode, colorRGB, onMS, offMS, mode);
#endif
    if(onMS == 1)
        snprintf(buffer, sizeof(buffer), "2,0x01,0x7f,");
    else
        snprintf(buffer, sizeof(buffer), "8,0x%x,0x7f,0x60,0x00,0x%x,0xff,0x60,0x00,", onMS, offMS);

    write_str(LED_MY_PATTERN_FILE, buffer);
    write_int(LED_MODE_FILE, mode);

    return 0;
}

static int
set_light_notifications(struct light_device_t* dev,
        struct light_state_t const* state)
{
    pthread_mutex_lock(&g_lock);
    set_light_locked(state, L_NOTIFICATION);
    pthread_mutex_unlock(&g_lock);
    return 0;
}

static int
set_light_attention(struct light_device_t* dev,
        struct light_state_t const* state)
{
    ALOGD("light_attention mode %d,onMS=%d, offMS=%d\n",state->flashMode,state->flashOnMS,state->flashOffMS);

    pthread_mutex_lock(&g_lock);
    set_light_locked(state, L_ATTENTION);
    pthread_mutex_unlock(&g_lock);
    return 0;
}

static int set_light_battery(struct light_device_t* dev,
        struct light_state_t const* state)
{
    pthread_mutex_lock(&g_lock);

    struct light_state_t* finalState =
        (struct light_state_t*) malloc(sizeof(struct light_state_t));

    ALOGD("set_light_battery");

    memcpy(finalState, state, sizeof(struct light_state_t));
    set_light_locked(finalState, L_BATTERY);
    pthread_mutex_unlock(&g_lock);

    free(finalState);

    return 0;
}

/** Close the lights device */
static int
close_lights(struct light_device_t *dev)
{
    if (dev) {
        free(dev);
    }
    return 0;
}


/******************************************************************************/

/**
 * module methods
 */

/** Open a new instance of a lights device using name */
static int open_lights(const struct hw_module_t* module, char const* name,
        struct hw_device_t** device)
{
    int (*set_light)(struct light_device_t* dev,
            struct light_state_t const* state);

    if (0 == strcmp(LIGHT_ID_BACKLIGHT, name))
        set_light = set_light_backlight;
    else if (0 == strcmp(LIGHT_ID_NOTIFICATIONS, name))
        set_light = set_light_notifications;
    else if (0 == strcmp(LIGHT_ID_ATTENTION, name))
        set_light = set_light_attention;
    else if (!strcmp(LIGHT_ID_BATTERY, name))
        set_light = set_light_battery;
    else
        return -EINVAL;

    pthread_once(&g_init, init_globals);

    struct light_device_t *dev = malloc(sizeof(struct light_device_t));
    memset(dev, 0, sizeof(*dev));

    dev->common.tag = HARDWARE_DEVICE_TAG;
    dev->common.version = 0;
    dev->common.module = (struct hw_module_t*)module;
    dev->common.close = (int (*)(struct hw_device_t*))close_lights;
    dev->set_light = set_light;

    *device = (struct hw_device_t*)dev;
    return 0;
}

static struct hw_module_methods_t lights_module_methods = {
    .open =  open_lights,
};

/*
 * The lights Module
 */
struct hw_module_t HAL_MODULE_INFO_SYM = {
    .tag = HARDWARE_MODULE_TAG,
    .version_major = 1,
    .version_minor = 0,
    .id = LIGHTS_HARDWARE_MODULE_ID,
    .name = "Lights for Meizu PRO5",
    .author = "faust93 at monumentum@gmail.com",
    .methods = &lights_module_methods,
};
