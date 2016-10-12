LOCAL_PATH := $(call my-dir)

# GLGPS
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    libmmcompat.c \
    icu53.c

LOCAL_SHARED_LIBRARIES := libicuuc libicui18n
LOCAL_MODULE := libmmcompat
LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)

# camera

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
     gui/SensorManager.cpp \
     ui/GraphicBuffer.cpp \
     ui/GraphicBufferAllocator.cpp \
     ui/GraphicBufferMapper.cpp

LOCAL_SHARED_LIBRARIES := liblog libcutils libhardware libui libgui libbinder libutils libsync
LOCAL_MODULE := libshim_camera
LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)

include $(LOCAL_PATH)/skia/Android.mk
