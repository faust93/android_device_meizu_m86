/*
 * This file contains device specific hooks.
 * Always enclose hooks to #if MR_DEVICE_HOOKS >= ver
 * with corresponding hook version!
 */

#include <stdio.h>
#include <unistd.h>
#include <sys/stat.h>
#include <stdint.h>
#include <time.h>
#include <string.h>

#include <errno.h>
#include <signal.h>
#include <sys/wait.h>

#include <private/android_filesystem_config.h>

#include <log.h>
#include <util.h>
#include <multirom.h>

#if MR_DEVICE_HOOKS >= 1

int mrom_hook_after_android_mounts(const char *busybox_path, const char *base_path, int type)
{
    return 0;
}
#endif /* MR_DEVICE_HOOKS >= 1 */


#if MR_DEVICE_HOOKS >= 2
void mrom_hook_before_fb_close(void)
{
}
#endif /* MR_DEVICE_HOOKS >= 2 */


#if MR_DEVICE_HOOKS >= 3
void tramp_hook_before_device_init(void)
{
}
#endif /* MR_DEVICE_HOOKS >= 3 */

#if MR_DEVICE_HOOKS >= 4
int mrom_hook_allow_incomplete_fstab(void)
{
    return 0;
}
#endif

#if MR_DEVICE_HOOKS >= 5

void mrom_hook_fixup_bootimg_cmdline(char *bootimg_cmdline, size_t bootimg_cmdline_cap)
{
}

int mrom_hook_has_kexec(void)
{
    // check for fdt blob TODO
//    static const char *checkfile = "/sys/firmware/fdt";
//    if(access(checkfile, R_OK) < 0)
//    {
//        ERROR("%s was not found!\n", checkfile);
//        return 0;
//    }
    return 1;
}
#endif
