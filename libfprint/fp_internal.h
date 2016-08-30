/*
 * Internal/private definitions for libfprint
 * Copyright (C) 2007 Daniel Drake <dsd@gentoo.org>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

#ifndef __FPRINT_INTERNAL_H__
#define __FPRINT_INTERNAL_H__

#include <stdint.h>

#include <glib.h>

#include <fprint.h>

#define API_EXPORTED __attribute__((visibility("default")))

#define MIN_ACCEPTABLE_MINUTIAE 11

#define container_of(ptr, type, member) ({                      \
        const typeof( ((type *)0)->member ) *__mptr = (ptr);    \
        (type *)( (char *)__mptr - offsetof(type,member) );})

enum fpi_log_level {
	LOG_LEVEL_DEBUG,
	LOG_LEVEL_INFO,
	LOG_LEVEL_WARNING,
	LOG_LEVEL_ERROR,
};

enum fp_imgdev_action {
    IMG_ACTION_NONE = 0,
    IMG_ACTION_ENROLL,
    IMG_ACTION_VERIFY,
    IMG_ACTION_IDENTIFY,
    IMG_ACTION_CAPTURE,
};

void fpi_log(enum fpi_log_level, const char *component, const char *function,
	const char *format, ...);

#ifndef FP_COMPONENT
#define FP_COMPONENT NULL
#endif

#define ENABLE_LOGGING
#define ENABLE_DEBUG_LOGGING

//#ifdef ENABLE_LOGGING
#define _fpi_log(level, fmt...) fpi_log(level, FP_COMPONENT, __FUNCTION__, fmt)
//#else
//#define _fpi_log(level, fmt...)
//#endif

//#ifdef ENABLE_DEBUG_LOGGING
#define fp_dbg(fmt...) _fpi_log(LOG_LEVEL_DEBUG, fmt)
//#else
//#define fp_dbg(fmt...)
//#endif

#define fp_info(fmt...) _fpi_log(LOG_LEVEL_INFO, fmt)
#define fp_warn(fmt...) _fpi_log(LOG_LEVEL_WARNING, fmt)
#define fp_err(fmt...) _fpi_log(LOG_LEVEL_ERROR, fmt)

struct fp_dev {
	struct fp_driver *drv;
	int udev;
	uint32_t devtype;
	void *priv;

	int nr_enroll_stages;

	/* drivers should not mess with these */
	int __enroll_stage;
};

struct fp_img_dev {
	struct fp_dev *dev;
	int udev;
	enum fp_imgdev_action action;
	int action_state;
	int enroll_stage;
	int action_result;
	void *priv;
};

int fpi_imgdev_capture(struct fp_img_dev *imgdev, int unconditional,
	struct fp_img **image);
int fpi_imgdev_get_img_width(struct fp_img_dev *imgdev);
int fpi_imgdev_get_img_height(struct fp_img_dev *imgdev);

enum fp_driver_type {
	DRIVER_PRIMITIVE = 0,
	DRIVER_IMAGING = 1,
};

struct fp_driver {
	const uint16_t id;
	const char *name;
	const char *full_name;
	enum fp_driver_type type;

	void *priv;

	/* Device operations */
	int (*discover)(uint32_t *devtype);
	int (*init)(struct fp_dev *dev, unsigned long driver_data);
	void (*exit)(struct fp_dev *dev);
	int (*enroll)(struct fp_dev *dev, gboolean initial, int stage,
		struct fp_print_data **print_data, struct fp_img **img);
	int (*verify)(struct fp_dev *dev, struct fp_print_data *data,
		struct fp_img **img);
	int (*identify)(struct fp_dev *dev, struct fp_print_data **print_gallery,
		int *match_offset, struct fp_img **img);
};

enum fp_print_data_type fpi_driver_get_data_type(struct fp_driver *drv);

/* flags for fp_img_driver.flags */
#define FP_IMGDRV_SUPPORTS_UNCONDITIONAL_CAPTURE (1 << 0)

struct fp_img_driver {
	struct fp_driver driver;
	uint16_t flags;
	int img_width;
	int img_height;
	unsigned int enlarge_factor;
	int bz3_threshold;

	/* Device operations */
	int (*init)(struct fp_img_dev *dev, unsigned long driver_data);
	void (*exit)(struct fp_img_dev *dev);
	int (*await_finger_on)(struct fp_img_dev *dev);
	int (*await_finger_off)(struct fp_img_dev *dev);
	int (*capture)(struct fp_img_dev *dev, gboolean unconditional,
		struct fp_img **image);
	int (*mode)(struct fp_img_dev *dev, gboolean sensor_mode);
};

/* Drivers */
extern struct fp_img_driver fpc1150_driver;

void fpi_img_driver_setup(struct fp_img_driver *idriver);

#define fpi_driver_to_img_driver(drv) \
	container_of((drv), struct fp_img_driver, driver)

struct fp_dscv_dev {
	int  udev;
	struct fp_driver *drv;
	unsigned long driver_data;
	uint32_t devtype;
};

struct fp_dscv_print {
	uint16_t driver_id;
	uint32_t devtype;
	enum fp_finger finger;
	char *path;
};

enum fp_print_data_type {
	PRINT_DATA_RAW = 0, /* memset-imposed default */
	PRINT_DATA_NBIS_MINUTIAE,
};

struct fp_print_data_item {
	size_t length;
	unsigned char data[0];
};

struct fp_print_data {
	uint16_t driver_id;
	uint32_t devtype;
	enum fp_print_data_type type;
	GSList *prints;
        enum fp_finger id;
};

struct fpi_print_data_fp1 {
	char prefix[3];
	uint16_t driver_id;
	uint32_t devtype;
	unsigned char data_type;
	unsigned char data[0];
} __attribute__((__packed__));

struct fpi_print_data_item_fp1 {
	uint32_t length;
	unsigned char data[0];
} __attribute__((__packed__));

void fpi_data_exit(void);

struct fp_print_data *fpi_print_data_new(struct fp_dev *dev);
struct fp_print_data_item *fpi_print_data_item_new(size_t length);

gboolean fpi_print_data_compatible(uint16_t driver_id1, uint32_t devtype1,
	enum fp_print_data_type type1, uint16_t driver_id2, uint32_t devtype2,
	enum fp_print_data_type type2);

struct fp_minutiae {
	int alloc;
	int num;
	struct fp_minutia **list;
};

/* bit values for fp_img.flags */
#define FP_IMG_V_FLIPPED 		(1<<0)
#define FP_IMG_H_FLIPPED 		(1<<1)
#define FP_IMG_COLORS_INVERTED	(1<<2)
#define FP_IMG_BINARIZED_FORM	(1<<3)
#define FP_IMG_PARTIAL		(1<<4)

#define FP_IMG_STANDARDIZATION_FLAGS (FP_IMG_V_FLIPPED | FP_IMG_H_FLIPPED \
	| FP_IMG_COLORS_INVERTED)

struct fp_img {
	int width;
	int height;
	size_t length;
	uint16_t flags;
	struct fp_minutiae *minutiae;
	unsigned char *binarized;
	unsigned char data[0];
};

struct fp_img *fpi_img_new(size_t length);
struct fp_img *fpi_img_new_for_imgdev(struct fp_img_dev *dev);
struct fp_img *fpi_img_resize(struct fp_img *img, size_t newsize);
gboolean fpi_img_is_sane(struct fp_img *img);
int fpi_img_detect_minutiae(struct fp_img *img);
int fpi_img_to_print_data(struct fp_img_dev *imgdev, struct fp_img *img,
	struct fp_print_data *print);
int fpi_img_to_print_data_new(struct fp_img_dev *imgdev, struct fp_img *img,
	struct fp_print_data **ret);
int fpi_img_compare_print_data(struct fp_print_data *enrolled_print,
	struct fp_print_data *new_print);
int fpi_img_compare_print_data_to_gallery(struct fp_print_data *print,
	struct fp_print_data **gallery, int match_threshold, int *match_offset);

#endif

