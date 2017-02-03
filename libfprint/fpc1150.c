/*
 * Fingerprint FPC1150 driver for libfprint
 * Copyright (C) 2016 faust93 <monumentum@gmail.com>
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

#include <errno.h>
#include <string.h>
#include <unistd.h>

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/types.h>
#include <fp_internal.h>

#include <linux/ioctl.h>

#define FP_COMPONENT "fpc1150"

/* bozorth score matching threshold */
#define BZ3_THRESHOLD 35

/* amount of enroll stages for each template */
#define ENROLL_STAGES 35

/* raw sensor image 90ccw rotated */
#define RAW_IMAGE_WIDTH		416
#define RAW_IMAGE_HEIGTH	80
#define RAW_IMAGE_SIZE		(RAW_IMAGE_WIDTH * RAW_IMAGE_HEIGTH)

/* assembled image */
#define IMAGE_WIDTH		416
#define IMAGE_HEIGTH	        160
#define IMAGE_SIZE		(IMAGE_WIDTH * IMAGE_HEIGTH)

#define NAV_MODE 0
#define CAP_MODE 1

/* sysfs paths */
#define FPC_DEV "/dev/fpc1020"
#define FP_NAV_SWITCH "/proc/nav_switch"
#define FP_CAP_MODE "/sys/devices/14d70000.spi/spi_master/spi4/spi4.0/setup/capture_mode"
#define FP_CAP_COUNT "/sys/devices/14d70000.spi/spi_master/spi4/spi4.0/setup/capture_count"
#define FP_PXL_CTRL "/sys/devices/14d70000.spi/spi_master/spi4/spi4.0/setup/pxl_ctrl"

/* sensor mode */
int current_mode = -1;

void contrast(unsigned char *src, int width, int height, int contrast)
{
    int x,y;
    double fgray, a, b;

    a = (double)((127.0 + contrast) / 127.0);
    b = (double)(-contrast);

    for (y = 0; y < height; y++)
    for (x = 0; x < width; x++)
    {
        fgray = (double)src[x+y*width];
        fgray = b + a*fgray;
        if (fgray < 0.0)    fgray = 0.0;
        if (fgray > 255.0)  fgray = 255.0;
        src[x+y*width] = (uint8_t)fgray;
    }
}

void luminosity(unsigned char *src, int w, int h, int luminosity)
{
    int x,y;
    double fgray, a, b;

    if (luminosity>0)
    {
        a = (255.0 - abs(luminosity)) / 255.0;
        b = (double)luminosity;
    }
    else
    {
        a = (255.0 - abs(luminosity)) / 255.0;
        b = 0.0;
    }
    for (y = 0; y < h; y++)
    for (x = 0; x < w; x++)
    {
        fgray = (double)src[x+y*w];
        fgray = b + a*fgray;
        if (fgray < 0.0)    fgray = 0.0;
        if (fgray > 255.0)  fgray = 255.0;
        src[x+y*w] = (uint8_t)fgray;
    }
}


static inline void rot90ccw(unsigned char *src, register unsigned char *dst,
                            int width, int height)
{
    unsigned char *endp;
    register unsigned char *base;
    int j, size = width * height;
    endp = src + size;
    dst = dst + size - 1;
    for (base = endp - width; base < endp; base++) {
        src = base;
        for (j = 0; j < height; j++, src -= width)
            *dst-- = *src;
    }
}

static inline void flip_v(unsigned char *src, int width, int height)
{
    int y, x, middle = height / 2;
    for(y = 0; y < middle; ++y)
    {
     unsigned char* idx1 = src + width * y;
     unsigned char* idx2 = src + width * (height - y - 1);
        for(x = 0; x < width; ++x)
        {
            uint32_t pixel =*idx1;
            *idx1=*idx2;
            *idx2=pixel;
            ++idx2;
            ++idx1;
        }
    }
}

int write_sysfs(char *sys_node, char *val)
{
    int fd,ret;

    fd = open(sys_node, O_WRONLY);
        if (fd < 0) {
            fp_dbg("Failed opening %s node: %d", sys_node, fd);
            return -1;
        }

    ret = write(fd, val, strlen(val));
        if (ret <= 0) {
            fp_dbg("Failed writing %s node: %d", sys_node, ret);
            return -1;
        }
    close(fd);
    return ret;
}


int reset()
{
    int fn,ret = 0;
    int attempts = 3;

#define FPC_HW_RESET    _IOW('K', 0, int)

    if((fn=open(FPC_DEV, O_RDONLY)) < -1) {
                fp_dbg("Error, can't open %s\n", FPC_DEV);
		return -1;
	}

    if (ioctl(fn, FPC_HW_RESET) < 0) {
        ret = -errno;
        fp_dbg("Failed resetting FPC1020: %d", ret);
    }

    close(fn);
    return ret;
}

static void capture_mode()
{
        reset();
	write_sysfs(FP_NAV_SWITCH, "0");
	write_sysfs(FP_CAP_MODE, "1");
	write_sysfs(FP_CAP_COUNT, "2");
	write_sysfs(FP_PXL_CTRL, "0");
        fp_dbg("cap mode..\n");
}

static void nav_mode()
{
        reset();
	write_sysfs(FP_NAV_SWITCH, "1");
	write_sysfs(FP_CAP_MODE, "7");
	write_sysfs(FP_CAP_COUNT, "1");
	write_sysfs(FP_PXL_CTRL, "30");
        fp_dbg("nav mode..\n");
}


int get_raw_img(gchar *image, int size)
{
    int fn,ret;
    int attempts = 3;

    if((fn=open(FPC_DEV, O_RDONLY)) < -1) {
                fp_dbg("Error, can't open %s\n", FPC_DEV);
		return -1;
	}
gri_again:
	if(read(fn,image,size) != size) {
    	    fp_dbg("%s: retry %d\n",__func__,attempts);
	    attempts--;
	    if(attempts!=0)
		goto gri_again;
	    else
		return -1;
	}
    close(fn);

    return 0;
}

int get_raw_img_nonblock(gchar *image, int size)
{
    int fn,ret;
    int cycles = 10;

    if((fn=open(FPC_DEV, O_RDONLY | O_NONBLOCK)) < -1) {
                fp_dbg("Error, can't open %s\n", FPC_DEV);
            return -1;
        }

    do {
        usleep(100000);
        ret = read(fn,image,size);
        if(ret == size)
            break;
        cycles--;
        if(!cycles){
            close(fn);
            return FP_TIMEOUT;
            }
        } while(ret == -1 || !ret );

    close(fn);

    return 0;
}

/* non blocking wait for finger down */
int wait_finger()
{
    int fn,ret;
    int cycles = 10;
    char buf[6];

    fp_dbg("waiting for finger..\n");

    write_sysfs(FP_CAP_MODE, "7");
    if((fn=open(FPC_DEV, O_RDONLY | O_NONBLOCK)) < -1) {
                fp_dbg("Error, can't open %s\n", FPC_DEV);
                return -1;
    }
    do {
        ret = read(fn,buf,4);
        usleep(150000);
        if(ret == 4)
            break;
        cycles--;
        if(!cycles){
            close(fn);
            return FP_TIMEOUT;
            }
        } while(ret == -1 || !ret );

    write_sysfs(FP_CAP_MODE, "1");
    close(fn);

    return 0;
}

int wait_finger_up()
{
    int fn,ret = 0;
    int cycles = 3;
    char buf[6];

    fp_dbg("waiting for finger up..\n");

    write_sysfs(FP_CAP_MODE, "8");
    if((fn=open(FPC_DEV, O_RDONLY)) < -1) {
                fp_dbg("Error, can't open %s\n", FPC_DEV);
                return -1;
    }

    do {
        ret = read(fn,buf,4);
        usleep(10000);
        if(ret == 4)
            break;
        cycles--;
        if(!cycles){
            close(fn);
            return FP_TIMEOUT;
            }
        } while(ret == -1 || !ret );


    write_sysfs(FP_CAP_MODE, "7");
    fp_dbg("waiting for finger up done..\n");

    return ret;
}

static int mode(struct fp_img_dev *dev, int sensor_mode)
{

    if(sensor_mode != current_mode){
        current_mode = sensor_mode;

        switch(sensor_mode){
            case NAV_MODE:
//             wait_finger_up();
             nav_mode();
             break;
            case CAP_MODE:
             capture_mode();
             break;
        }
    }
    return 0;
}

static gint capture(struct fp_img_dev *dev, gboolean unconditional, struct fp_img **ret)
{
	struct fp_img *img = NULL;
	gchar *image, *image_tmp;

	int fn, r = 0;

	if(!unconditional){
    	    if((r = wait_finger())!=0)
		return r;
	}

        image  = g_malloc0(IMAGE_SIZE);
        image_tmp  = g_malloc0(IMAGE_SIZE);

        if(unconditional == 1)
            r = get_raw_img(image,RAW_IMAGE_SIZE);
        else
            r = get_raw_img_nonblock(image,RAW_IMAGE_SIZE);

        if(r == FP_TIMEOUT && r < 0){
            fp_dbg("no data acquired\n");
            goto out;
        }

        img = fpi_img_new_for_imgdev(dev);

        rot90ccw(image, image_tmp, RAW_IMAGE_HEIGTH, RAW_IMAGE_WIDTH);
        memcpy(img->data, image_tmp, RAW_IMAGE_SIZE);
        flip_v(image_tmp, RAW_IMAGE_WIDTH, RAW_IMAGE_HEIGTH);
        memcpy(img->data + RAW_IMAGE_SIZE, image_tmp, RAW_IMAGE_SIZE);

        *ret = img;
out:
        g_free(image);
        g_free(image_tmp);
        return r;
}


static gint dev_init(struct fp_img_dev *dev, unsigned long driver_data)
{
	dev->dev->nr_enroll_stages = ENROLL_STAGES;
	return 0;
}

static
void dev_exit(struct fp_img_dev *dev)
{
    nav_mode();
}


struct fp_img_driver fpc1150_driver = {
	.driver = {
		.id = 1,
		.name = FP_COMPONENT,
		.full_name = "Fingerprint FPC1150",
	},
	.img_height = IMAGE_HEIGTH,
	.img_width = IMAGE_WIDTH,
	.bz3_threshold = BZ3_THRESHOLD,

	.init = dev_init,
	.exit = dev_exit,
	.capture = capture,
	.mode = mode,
};
