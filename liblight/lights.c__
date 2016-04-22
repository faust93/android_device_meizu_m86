/*
 * Copyright (C) 2011 The CyanogenMod Project
 * Copyright (C) 2012 marc1706
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "lights"

#include <cutils/log.h>

#include <stdint.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <fcntl.h>
#include <pthread.h>
#include <time.h>


#include <sys/ioctl.h>
#include <sys/types.h>

#include <hardware/lights.h>

static pthread_once_t g_init = PTHREAD_ONCE_INIT;
static pthread_mutex_t g_lock = PTHREAD_MUTEX_INITIALIZER;
static struct light_state_t g_notification;
static struct light_state_t g_battery;
static int g_backlight = 255;

// battery state checker vars
#define BATT_NONE		1
#define BATT_FULL		5
#define BATT_CHARGING	9

static pthread_t t_battery_checker = 0;
static int battery_thread_check = 1;
static int last_battery_state = 0;
static int force_led_amber = 0;
static int battery_thread_running = 0;

// sysfs files
char const*const AMBER_LED_FILE = "/sys/class/leds/amber/brightness";
char const*const GREEN_LED_FILE = "/sys/class/leds/green/brightness";

char const*const BUTTON_FILE = "/sys/class/leds/button-backlight/brightness";

char const*const AMBER_BLINK_FILE = "/sys/class/leds/amber/blink";
char const*const GREEN_BLINK_FILE = "/sys/class/leds/green/blink";

char const*const LCD_BACKLIGHT_FILE = "/sys/class/leds/lcd-backlight/brightness";

char const*const BATTERY_STATUS_FILE = "/sys/class/power_supply/battery/status";

enum {
	LED_AMBER,
	LED_GREEN,
	LED_BLUE,
	LED_BLANK,
};

/**
 * Aux method, write int to file
 */
static int write_int (const char* path, int value) {
	int fd;
	static int already_warned = 0;

	fd = open(path, O_RDWR);
	if (fd < 0) {
		if (already_warned == 0) {
			ALOGE("write_int failed to open %s\n", path);
			already_warned = 1;
		}
		return -errno;
	}

	char buffer[20];
	int bytes = snprintf(buffer, sizeof(buffer), "%d\n",value);
	int written = write (fd, buffer, bytes);
	close (fd);

	return written == -1 ? -errno : 0;

}

void init_globals (void) {
	pthread_mutex_init (&g_lock, NULL);
}

static int is_lit (struct light_state_t const* state) {
	return state->color & 0x00ffffff;
}


static void set_speaker_light_locked (struct light_device_t *dev, struct light_state_t *state) {
	unsigned int colorRGB = state->color & 0xFFFFFF;
	unsigned int color = LED_BLANK;

	if (colorRGB & 0xFF)
		color = LED_BLUE;
	if ((colorRGB >> 8)&0xFF)
		color = LED_GREEN;
	if ((colorRGB >> 16)&0xFF)
		color = LED_AMBER;
		
	/*
	* this is needed in order to make sure that we don't show the green led
	* at first while having a battery level between 90% and 100%
	* -- marc1706
	*/
	if (state->flashMode == LIGHT_FLASH_NONE && color == LED_GREEN &&
		force_led_amber) {
		color = LED_AMBER;
		force_led_amber = 0;
	}

	int amber = (colorRGB >> 16)&0xFF;
	int green = (colorRGB >> 8)&0xFF;
	int blue = (colorRGB)&0xFF;

	switch (state->flashMode) {
		case LIGHT_FLASH_TIMED:
			switch (color) {
				case LED_AMBER:
					write_int (AMBER_BLINK_FILE, 2);
					write_int (GREEN_LED_FILE, 0);
					break;
				case LED_GREEN:
					write_int (GREEN_BLINK_FILE, 3);
					write_int (AMBER_LED_FILE, 0);
					break;
				case LED_BLUE:
					write_int (GREEN_BLINK_FILE, 3);
					write_int (AMBER_LED_FILE, 0);
					break;
				case LED_BLANK:
					write_int (AMBER_BLINK_FILE, 0);
					write_int (GREEN_BLINK_FILE, 0);
					break;
				default:
					ALOGE("set_led_state colorRGB=%08X, unknown color\n",
							colorRGB);
					break;
			}
			break;
		case LIGHT_FLASH_NONE:
			switch (color) {
				case LED_AMBER:
					write_int (AMBER_LED_FILE, 1);
					write_int (GREEN_LED_FILE, 0);
					break;
				case LED_GREEN:
					write_int (AMBER_LED_FILE, 0);
					write_int (GREEN_LED_FILE, 1);
					break;
				case LED_BLUE:
					write_int (AMBER_LED_FILE, 0);
					write_int (GREEN_LED_FILE, 1);
					break;
				case LED_BLANK:
					write_int (AMBER_LED_FILE, 0);
					write_int (GREEN_LED_FILE, 0);
					break;

			}
			break;
		default:
			ALOGE("set_led_state colorRGB=%08X, unknown mode %d\n",
					colorRGB, state->flashMode);
	}

}

/*
* check battery level and change LED if necessary
* Copyright (C) 2012 Marc Alexander - marc1706
*
* @param (int) ret: return 1 if charging, 0 if full
*/
static int check_battery_level(int ret) {
	char str[20];
	int batt, battery_state;

	memset(&str[0], 0, sizeof(str));

	batt = open(BATTERY_STATUS_FILE,O_RDONLY);
	read(batt, str, 20);
	close(batt);
	
	battery_state = 0;
	battery_state = sprintf(str, "%s", str);
	
	if (last_battery_state != BATT_CHARGING && last_battery_state != BATT_FULL) {
		last_battery_state = BATT_NONE;
		ALOGE("%s: Incorrect last battery_state! Resetting last battery state!",
			__func__);
	}
	
	if (battery_state != BATT_CHARGING && battery_state != BATT_FULL)
		ALOGE("%s: Incorrect battery_state!", __func__);
	
	// did the battery state change?
	if (battery_state != last_battery_state) {
		last_battery_state = battery_state;

		// battery & notification led should show
		if (is_lit (&g_battery) && is_lit (&g_notification)) {
			if (battery_state == BATT_CHARGING) {
				// Charging -- fast blink amber
				write_int (AMBER_BLINK_FILE, 2);
				write_int (GREEN_LED_FILE, 0);
				if (ret)
					ret = 1;
			} else {
				// Charging -- fast blink green
				write_int (AMBER_LED_FILE, 0);
				write_int (GREEN_BLINK_FILE, 3);
				// cancel thread if we reached full level
				battery_thread_check = 0;
				if (ret)
					ret = 1;
			}
		} else if (!(is_lit (&g_battery) && is_lit (&g_notification))) {
			if (battery_state == BATT_CHARGING) {
				// Charging
				write_int (AMBER_LED_FILE, 1);
				write_int (GREEN_LED_FILE, 0);
				if (ret)
					ret = 1;
			} else if (battery_state == BATT_FULL) {
				// Full
				write_int (AMBER_LED_FILE, 0);
				write_int (GREEN_LED_FILE, 1);
				// cancel thread if we reached full level
				battery_thread_check = 0;
				if (ret)
					ret = 0;
			}
		}
		ALOGI("%s: state=%u", __func__, battery_state);
	} else if (ret) {
		if (battery_state == BATT_CHARGING)
			ret = 1;
		else
			ret = 0;
	}
	
	return ret;
}

/*
* thread routing for checking the battery state
* Copyright (C) 2012 Marc Alexander - marc1706
*/
void *battery_level_check(void *arg) {
	struct timespec wait;
	wait.tv_nsec = 1;
	wait.tv_sec = 5;
	
	struct timespec start_wait;
	start_wait.tv_nsec = 500000000;
	start_wait.tv_sec = 0;
	
	// wait for last thread to finish
	while (battery_thread_running)
		nanosleep(&start_wait, NULL);
		
	battery_thread_running = 1;
	battery_thread_check = 1;

	while (battery_thread_check) {
		check_battery_level(0);
		nanosleep(&wait, NULL);
	}
	
	ALOGI("%s: done with thread", __func__);
	battery_thread_running = 0;
	
	return NULL;
}

/*
* start thread for checking battery state
* Copyright (C) 2012 Marc Alexander - marc1706
*/
void start_battery_thread() {
	ALOGI("%s: start thread", __func__);
	if (t_battery_checker == 0)
		pthread_create(&t_battery_checker, NULL, battery_level_check, NULL);
}

static void set_speaker_light_locked_dual (struct light_device_t *dev, struct light_state_t *bstate, struct light_state_t *nstate) {

	unsigned int bcolorRGB = bstate->color & 0xFFFFFF;
	unsigned int bcolor = LED_BLANK;
	int is_charging = check_battery_level(1);

	if ((bcolorRGB >> 8)&0xFF) bcolor = LED_GREEN;
	if ((bcolorRGB >> 16)&0xFF) bcolor = LED_AMBER;
	
	if (bcolor == LED_GREEN && is_charging) {
		write_int (AMBER_BLINK_FILE, 2);
		write_int (GREEN_LED_FILE, 0);
		start_battery_thread();
	} else if (bcolor == LED_AMBER) {
		write_int (AMBER_BLINK_FILE, 2);
		write_int (GREEN_LED_FILE, 0);
	} else if (bcolor == LED_GREEN) {
		write_int (AMBER_LED_FILE, 0);
		write_int (GREEN_BLINK_FILE, 3);
	} else {
		ALOGE("set_led_state (dual) unexpected color: bcolorRGB=%08x\n", bcolorRGB);
	}
}

static void handle_speaker_battery_locked (struct light_device_t *dev) {
	unsigned int colorRGB = g_battery.color & 0xFFFFFF;
	int ret = 0;
	
	// reset threads first
	t_battery_checker = 0;
	battery_thread_check = 0;

	if (is_lit (&g_battery) && is_lit (&g_notification)) {
		set_speaker_light_locked_dual (dev, &g_battery, &g_notification);
	} else if (is_lit (&g_battery)) {
		// check if battery is below 100% and we are trying to show the green led
		if ((colorRGB >> 8)&0xFF) {
			ret = check_battery_level(1);
			if (ret) {
				force_led_amber = 1;
				start_battery_thread();
			}
				
			ALOGV("%s: changing color from LED_GREEN to LED_AMBER", __func__);
		}
		set_speaker_light_locked (dev, &g_battery);
	} else {
		set_speaker_light_locked (dev, &g_notification);
	}
	ALOGV("%s: g_battery=%d , g_notification=%d", __func__, is_lit (&g_battery),
		is_lit (&g_notification));
}

static int set_light_buttons (struct light_device_t* dev,
		struct light_state_t const* state) {
	int err = 0;
	int on = is_lit (state);
	pthread_mutex_lock (&g_lock);
	err = write_int (BUTTON_FILE, on?255:0);
	pthread_mutex_unlock (&g_lock);

	return err;
}

static int rgb_to_brightness(struct light_state_t const* state)
{
	int color = state->color & 0x00ffffff;
	return ((77*((color>>16)&0x00ff))
			+ (150*((color>>8)&0x00ff)) + (29*(color&0x00ff))) >> 8;
}

static int set_light_backlight(struct light_device_t* dev,
		struct light_state_t const* state) {
	int err = 0;
	int brightness = rgb_to_brightness(state);
	ALOGV("%s: brightness=%d color=0x%08x",
		__func__,brightness, state->color);
	pthread_mutex_lock(&g_lock);
	g_backlight = brightness;
	err = write_int(LCD_BACKLIGHT_FILE, brightness);
	pthread_mutex_unlock(&g_lock);
	return err;
}

static int set_light_battery (struct light_device_t* dev,
		struct light_state_t const* state) {
	pthread_mutex_lock (&g_lock);
	g_battery = *state;
	handle_speaker_battery_locked(dev);
	pthread_mutex_unlock (&g_lock);

	return 0;
}

static int set_light_attention (struct light_device_t* dev,
		struct light_state_t const* state) {
	/* bravo has no attention, bad bravo */
	/* dito on the leo */
	return 0;
}

static int set_light_notifications (struct light_device_t* dev,
		struct light_state_t const* state) {
	pthread_mutex_lock (&g_lock);
	g_notification = *state;
	handle_speaker_battery_locked (dev);
	pthread_mutex_unlock (&g_lock);
	return 0;
}

/* Close the lights device */
static int close_lights (struct light_device_t *dev) {
	if (dev)
		free (dev);

	return 0;
}


/******************************************************************************/

/**
 * module methods
 */

/* Open a new instance of a lights device using name */
static int open_lights (const struct hw_module_t* module, char const* name,
		struct hw_device_t** device) {
	int (*set_light)(struct light_device_t* dev,
			struct light_state_t const* state);

	if (0 == strcmp(LIGHT_ID_BACKLIGHT, name)) {
			set_light = set_light_backlight;
	}
	else if (0 == strcmp(LIGHT_ID_BUTTONS, name)) {
		set_light = set_light_buttons;
	}
	else if (0 == strcmp(LIGHT_ID_BATTERY, name)) {
		set_light = set_light_battery;
	}
	else if (0 == strcmp(LIGHT_ID_ATTENTION, name)) {
		set_light = set_light_attention;
	}
	else if (0 == strcmp(LIGHT_ID_NOTIFICATIONS, name))  {
		set_light = set_light_notifications;
	}
	else {
		return -EINVAL;
	}

	pthread_once (&g_init, init_globals);
	struct light_device_t *dev = calloc(1, sizeof(struct light_device_t));

	dev->common.tag = HARDWARE_DEVICE_TAG;
	dev->common.version = 0;
	dev->common.module = (struct hw_module_t*)module;
	dev->common.close = (int (*)(struct hw_device_t*))close_lights;
	dev->set_light = set_light;

	*device = (struct hw_device_t*) dev;
	return 0;
}


static struct hw_module_methods_t lights_module_methods = {
	.open = open_lights,
};

/*
 * The lights Module
 */
struct hw_module_t HAL_MODULE_INFO_SYM = {
	.tag = HARDWARE_MODULE_TAG,
	.version_major = 1,
	.version_minor = 1,
	.id = LIGHTS_HARDWARE_MODULE_ID,
	.name = "HTC leo lights module",
	.author = "Marc Alexander",
	.methods = &lights_module_methods,
};
