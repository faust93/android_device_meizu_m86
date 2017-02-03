#include <stdlib.h>

// These are paths to folders in /sys which contain "uevent" file
// need to init this device.
// MultiROM needs to init framebuffer, mmc blocks, input devices,
// some ADB-related stuff and USB drives, if OTG is supported
// You can use * at the end to init this folder and all its subfolders
const char *mr_init_devices[] =
{
    "/sys/class/graphics/fb0",

    "/sys/block/mmcblk0",
    "/sys/devices/15560000.dwmmc2",
    "/sys/devices/15560000.dwmmc2/mmc_host/mmc0",
    "/sys/devices/15560000.dwmmc2/mmc_host/mmc0/mmc0:59b4",
    "/sys/devices/15560000.dwmmc2/mmc_host/mmc0/mmc0:59b4/block/mmcblk0",
    "/sys/devices/15560000.dwmmc2/mmc_host/mmc0/mmc0:59b4/block/mmcblk0/*",
    "/dev/block/platform/15570000.ufs/by-name/*",
    "/sys/devices/15570000.ufs",
    "/sys/devices/15570000.ufs/host0",
    "/sys/devices/15570000.ufs/host0/target0:0:0/0:0:0:0",
    "/sys/devices/15570000.ufs/host0/target0:0:0/0:0:0:0/block/sda",
    "/sys/devices/15570000.ufs/host0/target0:0:0/0:0:0:0/block/sda/*",
    "/sys/devices/15570000.ufs/host0/target0:0:0/0:0:0:0/block/sda/sda26",
    "/sys/devices/15570000.ufs/host0/target0:0:0/0:0:0:0/block/sda/sda43",
    "/sys/devices/15570000.ufs/host0/target0:0:0/0:0:0:0/block/sda/sda41",
    "/sys/devices/15570000.ufs/host0/target0:0:0/0:0:0:0/block/sda/sda44",
    "/sys/devices/15570000.ufs/host0/target0:0:0/0:0:0:0/block/sda/sda22",
    "/sys/devices/15570000.ufs/host0/target0:0:0/0:0:0:0/block/sda/sda30",
    "/sys/devices/15570000.ufs/host0/target0:0:0/0:0:0:0/block/sda/sda21",

    "/sys/bus/mmc",
    "/sys/bus/mmc/drivers/mmcblk",
    "/sys/module/mmc_core",
    "/sys/module/mmcblk",
    "/sys/module/block",
    "/sys/module/ufshcd",
    "/sys/module/ufshcd_pltfrm",
    "/sys/module/decon",

    "/sys/devices/gpio_keys.42/input*",
    "/sys/devices/virtual/input*",
    "/sys/devices/virtual/misc/uinput",
    "/sys/devices/13690000.hsi2c/i2c-10/10-0044/input*",
    "/sys/devices/13690000.hsi2c/i2c-10/10-001e/input*",
    "/sys/devices/13660000.hsi2c/i2c-4/4-0049/input*",
    "/sys/devices/13660000.hsi2c/i2c-4/4-0049/input/input7",

    // for adb
    "/sys/devices/virtual/tty/ptmx",
    "/sys/devices/virtual/misc/android_adb",
    "/sys/devices/virtual/android_usb/android0/f_adb",

    "/sys/devices/platform/nop_usb_xceiv*",

    // Encryption
    "/sys/devices/virtual/misc/device-mapper",
    "/sys/devices/virtual/misc/ion",
    "/sys/devices/virtual/mobicore/mobicore",

    NULL
};
