LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := nf-log

LOCAL_SRC_FILES := attr.c callback.c nf-log.c nlmsg.c socket.c

include $(BUILD_EXECUTABLE)
