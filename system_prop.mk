# system.prop for m86
PRODUCT_PROPERTY_OVERRIDES += \
    ro.build.characteristics=phone \
    ro.bq.gpu_to_cpu_unsupported=1 \
    ro.opengles.version=196609 \
    ro.sf.lcd_density=480 \
    ro.arch=exynos7420

PRODUCT_PROPERTY_OVERRIDES += \
    wifi.interface=wlan0 \
    net.dns1=8.8.8.8 \
    net.dns2=8.8.4.4

# Camera
PRODUCT_PROPERTY_OVERRIDES += \
    camera2.portability.force_api=1

# Dalvik/Art
PRODUCT_PROPERTY_OVERRIDES += \
    ro.sys.fw.dex2oat_thread_count=4 \
    dalvik.vm.image-dex2oat-filter=speed \
    dalvik.vm.dex2oat-filter=speed \
    dalvik.vm.heapstartsize=16m \
    dalvik.vm.heapgrowthlimit=192m \
    dalvik.vm.heapsize=512m \
    dalvik.vm.heaptargetutilization=0.75 \
    dalvik.vm.heapminfree=2m \
    dalvik.vm.heapmaxfree=8m

# Hwui
PRODUCT_PROPERTY_OVERRIDES += \
    ro.hwui.texture_cache_size=96 \
    ro.hwui.layer_cache_size=64 \
    ro.hwui.path_cache_size=16 \
    ro.hwui.texture_cache_flushrate=0.4 \
    ro.hwui.shape_cache_size=4 \
    ro.hwui.gradient_cache_size=2 \
    ro.hwui.drop_shadow_cache_size=6 \
    ro.hwui.r_buffer_cache_size=8 \
    ro.hwui.text_small_cache_width=1024 \
    ro.hwui.text_small_cache_height=1024 \
    ro.hwui.text_large_cache_width=4096 \
    ro.hwui.text_large_cache_height=2048 \
    ro.hwui.fbo_cache_size=16

PRODUCT_PROPERTY_OVERRIDES += \
    ro.sys.sdcardfs=true

# GSM Signal
ro.telephony.ril.config=datacallapn,signalstrength

