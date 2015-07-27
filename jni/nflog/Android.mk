LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := nflog

LOCAL_SRC_FILES := attr.c callback.c nflog.c nlmsg.c socket.c

LOCAL_LDLIBS := -pie -rdynamic
LOCAL_CFLAGS := -fPIE -fvisibility=default

include $(BUILD_EXECUTABLE)
