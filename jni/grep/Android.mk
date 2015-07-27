LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := grep

LOCAL_SRC_FILES := getopt32.c grep.c llist.c get_line_from_file.c libbb.c \
                   recursive_action.c xregcomp.c

LOCAL_LDLIBS := -pie -rdynamic
LOCAL_CFLAGS := -fPIE -fvisibility=default

include $(BUILD_EXECUTABLE)
