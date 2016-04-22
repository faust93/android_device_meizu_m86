/*
 * Copyright (C) 2014-2015 The CyanogenMod Project
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
#define LOG_TAG "lights"

#include <cutils/log.h>

#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <fcntl.h>
#include <pthread.h>

#include <sys/types.h>

#include <hardware/lights.h>

/******************************************************************************/

static pthread_once_t g_init = PTHREAD_ONCE_INIT;
static pthread_mutex_t g_lock = PTHREAD_MUTEX_INITIALIZER;
static struct light_state_t g_attention;
static struct light_state_t g_notification;
static struct light_state_t g_battery;

#define ALL_MASK	56
#define HOME_MASK	16
#define LEFT_MASK	32
#define RIGHT_MASK	8
#define LR_MASK		40

#define AW_POWER_OFF	0
#define AW_CONST_ON 	1
#define AW_CONST_OFF	2
#define AW_FADE_AUTO	3
#define AW_FADE_ON_STEP	4
#define AW_FADE_OFF_STEP 5
#define AW_FADE_CYCLE	6

#define L_NOTIFICATION  0x01
#define L_BATTERY       0x02
#define L_BUTTONS       0x04
#define L_ATTENTION     0x08
#define L_NONE          0xFF //255

const char *const LCD_FILE
        = "/sys/class/leds/lcd-backlight/brightness";

char const *const LED_MODE
        = "/sys/class/leds/red/brightness";

char const *const LED_OUTN
        = "/sys/class/leds/red/outn";

char const *const LED_FADE
        = "/sys/class/leds/red/fade_parameter";

char const *const LED_GRADE
        = "/sys/class/leds/red/grade_parameter";

char const *const BATTERY_CAPACITY
        = "/sys/class/power_supply/battery/capacity";

char const *const BATTERY_CHARGING_STATUS
        = "/sys/class/power_supply/battery/status";

int btn_state = 0;
int is_charging = 0;

/**
 * device methods
 */

void init_globals(void)
{
    // init the mutex
    pthread_mutex_init(&g_lock, NULL);
}

static int
write_int(char const* path, int value)
{
    int fd;
    static int already_warned = 0;

    fd = open(path, O_RDWR);
    if (fd >= 0) {
        char buffer[20];
        int bytes = snprintf(buffer, sizeof(buffer), "%d\n", value);
        ssize_t amt = write(fd, buffer, (size_t)bytes);
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
write_str(char const* path, char *value)
{
    int fd;
    static int already_warned = 0;

    fd = open(path, O_RDWR);
    if (fd >= 0) {
        char buffer[PAGE_SIZE];
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
read_int(char const* path, int *value)
{
    int fd;
    static int already_warned = 0;

    fd = open(path, O_RDONLY);
    if (fd >= 0) {
        char buffer[20];
        int amt = read(fd, buffer, 20);
        sscanf(buffer, "%d\n", value);
        close(fd);
        return amt == -1 ? -errno : 0;
    } else {
        if (already_warned == 0) {
            ALOGD("read_int failed to open %s\n", path);
            already_warned = 1;
        }
        return -errno;
    }
}

static int
is_lit(struct light_state_t const* state)
{
    return state->color & 0x00ffffff;
}

static int
rgb_to_brightness(const struct light_state_t *state)
{
    int color = state->color & 0x00ffffff;
    return ((77 * ((color >> 16) & 0xff))
            + (150 * ((color >> 8) & 0xff))
            + (29 * (color & 0xff))) >> 8;
}

static int
set_speaker_light_locked(int event,
        struct light_state_t const* state)
{
    int onMS, offMS;
    unsigned int colorRGB;
    char buffer[25];
    int brightness;

    if (state == NULL) {
	ALOGV("disabling buttons backlight\n");
        write_int(LED_OUTN, 0);
	write_int(LED_MODE, 0);
        return 0;
    }

    brightness = rgb_to_brightness(state);
    ALOGD("set_speaker_light_locked mode=%d brightnsS=%d\n", state->flashMode, brightness);

    write_int(LED_OUTN, 0);
    write_int(LED_MODE, 0);
    write_int(LED_OUTN, HOME_MASK);

    if(state->flashMode == LIGHT_FLASH_TIMED) {
            onMS = state->flashOnMS;
            offMS = state->flashOffMS;
    } else {
            if(event == L_BATTERY && brightness != 0){  //battery charging
                char charging_status[15];
                FILE* fp = fopen(BATTERY_CHARGING_STATUS, "rb");
                fgets(charging_status, 14, fp);
                fclose(fp);
                if (strstr(charging_status, "Charging") != NULL) {
                    onMS = 500;
                    offMS = 2000;
                    is_charging = 1;
                } else {
                    return 0;
                    }
            } else {
                if(brightness == 28 || brightness == 0)    // battery charged or disconnected
                    is_charging = 0;
	    return 0;
            }
    }

    ALOGD("set_speaker_light_locked mode %d,  onMS=%d, offMS=%d\n",
            state->flashMode, onMS, offMS);

    switch(onMS){
	case 5000:
	    onMS = 5;
	    break;
	case 2000:
	    onMS = 4;
	    break;
	case 1000:
	    onMS = 3;
	    break;
	case 500:
	    onMS = 2;
	    break;
	case 250:
	    onMS = 1;
	    break;
	default:
	    onMS = 1;
    }

    switch(offMS){
	case 5000:
	    offMS = 5;
	    break;
	case 2000:
	    offMS = 4;
	    break;
	case 1000:
	    offMS = 3;
	    break;
	case 500:
	    offMS = 2;
	    break;
	case 250:
	    offMS = 1;
	    break;
	case 1:
	    offMS = 0;
	    break;
	default:
	    offMS = 0;
    }

    snprintf(buffer, sizeof(buffer), "%d %d %d\n", offMS, onMS, onMS);
    write_str(LED_FADE, buffer);
    write_str(LED_GRADE, "0 200");
    write_int(LED_MODE, AW_FADE_AUTO);

    return 0;
}

static void
handle_speaker_light_locked(struct light_device_t *dev)
{
    if (is_lit(&g_attention)) {
        set_speaker_light_locked(L_ATTENTION, &g_attention);
    } else if (is_lit(&g_notification)) {
        set_speaker_light_locked(L_NOTIFICATION, &g_notification);
    } else {
        set_speaker_light_locked(L_BATTERY, &g_battery);
    }
}

static int
set_light_backlight(struct light_device_t *dev,
        const struct light_state_t *state)
{
    int err = 0;
    int brightness = rgb_to_brightness(state);

    pthread_mutex_lock(&g_lock);

    err = write_int(LCD_FILE, brightness);

    pthread_mutex_unlock(&g_lock);

    return err;
}

static int
set_light_buttons(struct light_device_t *dev,
        const struct light_state_t *state)
{
    int lcd_on, err = 0;
    int brightness = rgb_to_brightness(state);
    char buffer[25];

    if(brightness > 200)
	brightness = 200;

    ALOGD("set_speaker_light_buttons brightness: %d", brightness);

//    read_int(LCD_FILE, &lcd_on);

    pthread_mutex_lock(&g_lock);

//    write_int(LED_OUTN, 0);
//    write_int(LED_MODE, 0);

    if(brightness == 0 && is_charging == 1){    // buttons on & charging
        write_int(LED_OUTN, 0);
        write_int(LED_MODE, 0);

        write_int(LED_OUTN, HOME_MASK);
        write_str(LED_FADE, "4 2 2");
        write_str(LED_GRADE, "0 200");
        write_int(LED_MODE, AW_FADE_AUTO);
        btn_state = 0;
    } else if(brightness != 0 && !btn_state) {                // turn buttons on
        write_int(LED_OUTN, ALL_MASK);
        write_str(LED_FADE, "1");
        write_int(LED_GRADE, brightness);
        write_int(LED_MODE, AW_CONST_ON);
        btn_state = 1;
    } else if(brightness == 0 && btn_state){
        write_int(LED_OUTN, ALL_MASK);
        write_str(LED_FADE, "1");
        write_int(LED_GRADE, brightness);
        write_int(LED_MODE, AW_POWER_OFF);
        btn_state = 0;
    }

    pthread_mutex_unlock(&g_lock);

    return err;
}

static int
set_light_attention(struct light_device_t *dev,
        const struct light_state_t *state)
{
    pthread_mutex_lock(&g_lock);

    ALOGD("set_light_attention");

    g_attention = *state;
    handle_speaker_light_locked(dev);

    pthread_mutex_unlock(&g_lock);

    return 0;
}

static int
set_light_notifications(struct light_device_t *dev,
        const struct light_state_t *state)
{
    pthread_mutex_lock(&g_lock);

    ALOGD("set_light_notifications");

    g_notification = *state;
    handle_speaker_light_locked(dev);

    pthread_mutex_unlock(&g_lock);

    return 0;
}

static int
set_light_battery(struct light_device_t *dev,
        const struct light_state_t *state)
{
    pthread_mutex_lock(&g_lock);

    ALOGD("set_light_battery");

    g_battery = *state;
    handle_speaker_light_locked(dev);

    pthread_mutex_unlock(&g_lock);

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
static int open_lights(const struct hw_module_t *module, const char *name,
        struct hw_device_t **device)
{
    int (*set_light)(struct light_device_t *dev,
            const struct light_state_t *state);

    if (0 == strcmp(LIGHT_ID_BACKLIGHT, name))
        set_light = set_light_backlight;
    else if (0 == strcmp(LIGHT_ID_BUTTONS, name))
        set_light = set_light_buttons;
    else if (0 == strcmp(LIGHT_ID_NOTIFICATIONS, name))
        set_light = set_light_notifications;
    else if (0 == strcmp(LIGHT_ID_ATTENTION, name))
        set_light = set_light_attention;
    else if (0 == strcmp(LIGHT_ID_BATTERY, name))
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
    .name = "NX512J Lights Module v1.1",
    .author = "faust93 at monumentum@gmail.com",
    .methods = &lights_module_methods,
};
