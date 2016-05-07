/*
 * Copyright (c) 2014 The Android Open Source Project
 * Copyright (c) 2015 Christopher N. Hesse <raymanfx@gmail.com>
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

#include <dirent.h>
#include <errno.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <linux/time.h>
#include <stdbool.h>

#define LOG_TAG "Exynos5433PowerHAL"
/* #define LOG_NDEBUG 0 */
#include <utils/Log.h>

#include <hardware/hardware.h>
#include <hardware/power.h>

#define NSEC_PER_SEC 1000000000
#define USEC_PER_SEC 1000000
#define NSEC_PER_USEC 100


#define POWERHAL_STRINGIFY(s) POWERHAL_TOSTRING(s)
#define POWERHAL_TOSTRING(s) #s

#define BOOSTPULSE_PATH "/sys/devices/system/cpu/cpu0/cpufreq/interactive/boostpulse"

#define BOOST_PULSE_DURATION 400000
#define BOOST_PULSE_DURATION_STR POWERHAL_STRINGIFY(BOOST_PULSE_DURATION)

#define BOOST_CPU0_PATH "/sys/devices/system/cpu/cpu0/cpufreq/interactive/boost"
#define BOOST_CPU4_PATH "/sys/devices/system/cpu/cpu4/cpufreq/interactive/boost"

#define CPU0_MAX_FREQ_PATH "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq"
#define CPU4_MAX_FREQ_PATH "/sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq"

#define LOW_POWER_MAX_FREQ_BIG "800000"
#define NORMAL_MAX_FREQ_BIG "2100000"
#define LOW_POWER_MAX_FREQ_LITTLE "400000"
#define NORMAL_MAX_FREQ_LITTLE "1500000"

#define TOUCHKEY_PATH "/proc/nav_switch"

struct exynos5433_power_module {
    struct power_module base;
    pthread_mutex_t lock;
    int boostpulse_fd;
    int boostpulse_warned;
    const char *touchkey_power_path;
    bool touchkey_blocked;
};

/* POWER_HINT_INTERACTION and POWER_HINT_VSYNC */
static unsigned int vsync_count;
static struct timespec last_touch_boost;
static bool touch_boost;

/* POWER_HINT_LOW_POWER */
static bool low_power_mode = false;

static int sysfs_read(char *path, char *s, int num_bytes)
{
    char errno_str[64];
    int len;
    int ret = 0;
    int fd;

    fd = open(path, O_RDONLY);
    if (fd < 0) {
        strerror_r(errno, errno_str, sizeof(errno_str));
        ALOGE("Error opening %s: %s\n", path, errno_str);

        return -1;
    }

    len = read(fd, s, num_bytes - 1);
    if (len < 0) {
        strerror_r(errno, errno_str, sizeof(errno_str));
        ALOGE("Error reading from %s: %s\n", path, errno_str);

        ret = -1;
    } else {
        s[len] = '\0';
    }

    close(fd);

    return ret;
}

static void sysfs_write(const char *path, char *s)
{
    char errno_str[64];
    int len;
    int fd;

    fd = open(path, O_WRONLY);
    if (fd < 0) {
        strerror_r(errno, errno_str, sizeof(errno_str));
        ALOGE("Error opening %s: %s\n", path, errno_str);
        return;
    }

    len = write(fd, s, strlen(s));
    if (len < 0) {
        strerror_r(errno, errno_str, sizeof(errno_str));
        ALOGE("Error writing to %s: %s\n", path, errno_str);
    }

    close(fd);
}

static void init_touchkey_power_path(struct exynos5433_power_module *exynos5433_pwr)
{
    exynos5433_pwr->touchkey_power_path = TOUCHKEY_PATH;
}

/*
 * This function performs power management setup actions at runtime startup,
 * such as to set default cpufreq parameters.  This is called only by the Power
 * HAL instance loaded by PowerManagerService.
 */
static void exynos5433_power_init(struct power_module *module)
{
    struct exynos5433_power_module *exynos5433_pwr = (struct exynos5433_power_module *) module;
    struct stat sb;
    int rc;

    /*
     * You get the values reading the hexdump from the biniary power module or
     * strace
     */
    sysfs_write("/sys/devices/system/cpu/cpu0/cpufreq/interactive/multi_enter_load", "800");
    sysfs_write("/sys/devices/system/cpu/cpu0/cpufreq/interactive/single_enter_load", "200");
    sysfs_write("/sys/devices/system/cpu/cpu0/cpufreq/interactive/param_index", "0");

    sysfs_write("/sys/devices/system/cpu/cpu0/cpufreq/interactive/timer_rate", "20000");
    sysfs_write("/sys/devices/system/cpu/cpu0/cpufreq/interactive/timer_slack", "20000");
    sysfs_write("/sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time", "40000");
    sysfs_write("/sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq", "800000");
    sysfs_write("/sys/devices/system/cpu/cpu0/cpufreq/interactive/go_hispeed_load", "85");
    sysfs_write("/sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads", "75");
    sysfs_write("/sys/devices/system/cpu/cpu0/cpufreq/interactive/above_hispeed_delay", "18000");

    sysfs_write("/sys/devices/system/cpu/cpu0/cpufreq/interactive/boostpulse_duration",
                BOOST_PULSE_DURATION_STR);

    /* The CPU might not be turned on, then it doesn't make sense to configure it. */
    rc = stat("/sys/devices/system/cpu/cpu4/cpufreq/interactive", &sb);
    if (rc < 0) {
        ALOGE("CPU2 is offline, skip init\n");
        goto out;
    }

    sysfs_write("/sys/devices/system/cpu/cpu4/cpufreq/interactive/multi_enter_load", "360");
    sysfs_write("/sys/devices/system/cpu/cpu4/cpufreq/interactive/multi_enter_time", "99000");

    sysfs_write("/sys/devices/system/cpu/cpu4/cpufreq/interactive/multi_exit_load", "240");
    sysfs_write("/sys/devices/system/cpu/cpu4/cpufreq/interactive/multi_exit_time", "299000");

    sysfs_write("/sys/devices/system/cpu/cpu4/cpufreq/interactive/single_enter_load", "95");
    sysfs_write("/sys/devices/system/cpu/cpu4/cpufreq/interactive/single_enter_time", "199000");

    sysfs_write("/sys/devices/system/cpu/cpu4/cpufreq/interactive/single_exit_load", "60");
    /* was emtpy in hex so a value already defined. */
    sysfs_write("/sys/devices/system/cpu/cpu4/cpufreq/interactive/single_exit_time", "99000");

    /* was emtpy in hex so a value already defined. */
    sysfs_write("/sys/devices/system/cpu/cpu4/cpufreq/interactive/param_index", "0");

    sysfs_write("/sys/devices/system/cpu/cpu4/cpufreq/interactive/timer_rate", "20000");
    sysfs_write("/sys/devices/system/cpu/cpu4/cpufreq/interactive/timer_slack", "20000");

    sysfs_write("/sys/devices/system/cpu/cpu4/cpufreq/interactive/min_sample_time", "40000");
    sysfs_write("/sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq", "1000000");

    sysfs_write("/sys/devices/system/cpu/cpu4/cpufreq/interactive/go_hispeed_load", "89");
    sysfs_write("/sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads", "80 1000000:81 1400000:87 1704000:90");

    sysfs_write("/sys/devices/system/cpu/cpu4/cpufreq/interactive/above_hispeed_delay", "59000 1200000:119000 1704000:19000");

    sysfs_write("/sys/devices/system/cpu/cpu4/cpufreq/interactive/boostpulse_duration",
                BOOST_PULSE_DURATION_STR);

out:
    init_touchkey_power_path(exynos5433_pwr);
}

/* This function performs power management actions upon the system entering
 * interactive state (that is, the system is awake and ready for interaction,
 * often with UI devices such as display and touchscreen enabled) or
 * non-interactive state (the system appears asleep, display usually turned
 * off).  The non-interactive state is usually entered after a period of
 * inactivity, in order to conserve battery power during such inactive periods.
 */
static void exynos5433_power_set_interactive(struct power_module *module, int on)
{
    struct exynos5433_power_module *exynos5433_pwr = (struct exynos5433_power_module *) module;
    struct stat sb;
    char buf[80];
    char touchkey_node[2];
    int touchkey_enabled;
    int rc;

    ALOGV("power_set_interactive: %d\n", on);

    /*
     * Lower maximum frequency when screen is off.  CPU 0 and 1 share a
     * cpufreq policy.
     */
    sysfs_write(CPU0_MAX_FREQ_PATH,
                (!on || low_power_mode) ? LOW_POWER_MAX_FREQ_LITTLE : NORMAL_MAX_FREQ_LITTLE);

    rc = stat(CPU0_MAX_FREQ_PATH, &sb);
    if (rc == 0) {
        sysfs_write(CPU4_MAX_FREQ_PATH,
                    (!on || low_power_mode) ? LOW_POWER_MAX_FREQ_BIG : NORMAL_MAX_FREQ_BIG);
    }

    if (!on) {
        if (sysfs_read(TOUCHKEY_PATH, touchkey_node, sizeof(touchkey_node)) == 0) {
            touchkey_enabled = touchkey_node[0] - '0';
            /*
             * If touchkey_enabled is 0, they keys have been disabled by another component
             * (for example cmhw), which means we don't want them to be enabled when resuming
             * from suspend.
             */
            if (touchkey_enabled == 0) {
                exynos5433_pwr->touchkey_blocked = true;
            } else {
                exynos5433_pwr->touchkey_blocked = false;
                sysfs_write(exynos5433_pwr->touchkey_power_path, "0");
            }
        }
    } else if (!exynos5433_pwr->touchkey_blocked) {
        sysfs_write(exynos5433_pwr->touchkey_power_path, "1");
    }

    ALOGV("power_set_interactive: %d done\n", on);
}

static struct timespec timespec_diff(struct timespec lhs, struct timespec rhs)
{
    struct timespec result;
    if (rhs.tv_nsec > lhs.tv_nsec) {
        result.tv_sec = lhs.tv_sec - rhs.tv_sec - 1;
        result.tv_nsec = NSEC_PER_SEC + lhs.tv_nsec - rhs.tv_nsec;
    } else {
        result.tv_sec = lhs.tv_sec - rhs.tv_sec;
        result.tv_nsec = lhs.tv_nsec - rhs.tv_nsec;
    }
    return result;
}

static int check_boostpulse_on(struct timespec diff)
{
    long boost_ns = (BOOST_PULSE_DURATION * NSEC_PER_USEC) % NSEC_PER_SEC;
    long boost_s = BOOST_PULSE_DURATION / USEC_PER_SEC;

    if (diff.tv_sec == boost_s)
        return (diff.tv_nsec < boost_ns);
    return (diff.tv_sec < boost_s);
}

/* You need to request the powerhal lock before calling this function */
static int boostpulse_open(struct exynos5433_power_module *exynos5433_pwr)
{
    char errno_str[64];

    if (exynos5433_pwr->boostpulse_fd < 0) {
        exynos5433_pwr->boostpulse_fd = open(BOOSTPULSE_PATH, O_WRONLY);
        if (exynos5433_pwr->boostpulse_fd < 0) {
            if (!exynos5433_pwr->boostpulse_warned) {
                strerror_r(errno, errno_str, sizeof(errno_str));
                ALOGE("Error opening %s: %s\n", BOOSTPULSE_PATH, errno_str);
                exynos5433_pwr->boostpulse_warned = 1;
            }
        }
    }

    return exynos5433_pwr->boostpulse_fd;
}

/*
 * This functions is called to pass hints on power requirements, which may
 * result in adjustment of power/performance parameters of the cpufreq governor
 * and other controls.
 */
static void exynos5433_power_hint(struct power_module *module,
                                  power_hint_t hint,
                                  void *data)
{
    struct exynos5433_power_module *exynos5433_pwr = (struct exynos5433_power_module *) module;
    char errno_str[64];
    int len;

    switch (hint) {
        case POWER_HINT_INTERACTION:

            ALOGV("%s: POWER_HINT_INTERACTION", __func__);

            pthread_mutex_lock(&exynos5433_pwr->lock);
            if (boostpulse_open(exynos5433_pwr) >= 0) {

                len = write(exynos5433_pwr->boostpulse_fd, "1", 1);
                if (len < 0) {
                    strerror_r(errno, errno_str, sizeof(errno_str));
                    ALOGE("Error writing to %s: %s\n", BOOSTPULSE_PATH, errno_str);
                } else {
                    clock_gettime(CLOCK_MONOTONIC, &last_touch_boost);
                    touch_boost = true;
                }

            }
            pthread_mutex_unlock(&exynos5433_pwr->lock);

            break;

        case POWER_HINT_VSYNC: {
            struct timespec now, diff;

            ALOGV("%s: POWER_HINT_VSYNC", __func__);

            pthread_mutex_lock(&exynos5433_pwr->lock);
            if (data) {
                if (vsync_count < UINT_MAX)
                    vsync_count++;
            } else {
                if (vsync_count)
                    vsync_count--;
                if (vsync_count == 0 && touch_boost) {
                    touch_boost = false;

                    clock_gettime(CLOCK_MONOTONIC, &now);
                    diff = timespec_diff(now, last_touch_boost);

                    if (check_boostpulse_on(diff)) {
                        struct stat sb;
                        int rc;

                        sysfs_write(BOOST_CPU0_PATH, "0");
                        rc = stat(CPU4_MAX_FREQ_PATH, &sb);
                        if (rc == 0) {
                            sysfs_write(BOOST_CPU4_PATH, "0");
                        }
                    }
                }
            }
            pthread_mutex_unlock(&exynos5433_pwr->lock);
            break;
        }
        case POWER_HINT_LOW_POWER: {
            int rc;
            struct stat sb;

            ALOGV("%s: POWER_HINT_LOW_POWER", __func__);

            pthread_mutex_lock(&exynos5433_pwr->lock);

            if (data) {
                sysfs_write(CPU0_MAX_FREQ_PATH, LOW_POWER_MAX_FREQ_LITTLE);
                rc = stat(CPU4_MAX_FREQ_PATH, &sb);
                if (rc == 0) {
                    sysfs_write(CPU4_MAX_FREQ_PATH, LOW_POWER_MAX_FREQ_BIG);
                }
            } else {
                sysfs_write(CPU0_MAX_FREQ_PATH, NORMAL_MAX_FREQ_LITTLE);
                rc = stat(CPU4_MAX_FREQ_PATH, &sb);
                if (rc == 0) {
                    sysfs_write(CPU4_MAX_FREQ_PATH, NORMAL_MAX_FREQ_BIG);
                }
            }
            low_power_mode = data;

            pthread_mutex_unlock(&exynos5433_pwr->lock);
            break;
        }
        default:
            break;
    }
}

static struct hw_module_methods_t power_module_methods = {
    .open = NULL,
};

struct exynos5433_power_module HAL_MODULE_INFO_SYM = {
    base: {
        common: {
            tag: HARDWARE_MODULE_TAG,
            module_api_version: POWER_MODULE_API_VERSION_0_2,
            hal_api_version: HARDWARE_HAL_API_VERSION,
            id: POWER_HARDWARE_MODULE_ID,
            name: "EXYNOS7420 Power HAL",
            author: "The Android Open Source Project",
            methods: &power_module_methods,
        },

        init: exynos5433_power_init,
        setInteractive: exynos5433_power_set_interactive,
        powerHint: exynos5433_power_hint,
    },

    lock: PTHREAD_MUTEX_INITIALIZER,
    boostpulse_fd: -1,
    boostpulse_warned: 0,
};
