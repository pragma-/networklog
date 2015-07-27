#!/bin/bash

build_package() {
  ndk-build -B V=1 APP_BUILD_SCRIPT=Android.mk NDK_APPLICATION_MK=Application.mk -C jni/$1

  rm res/raw/${1}_armv5.zip
  rm res/raw/${1}_armv7.zip
  rm res/raw/${1}_x86.zip
  rm res/raw/${1}_mips.zip

  zip -9 res/raw/${1}_armv5.zip libs/armeabi/$1
  zip -9 res/raw/${1}_armv7.zip libs/armeabi-v7a/$1
  zip -9 res/raw/${1}_x86.zip libs/x86/$1
  zip -9 res/raw/${1}_mips.zip libs/mips/$1
}

clean() {
  rm -rf obj/
  rm -rf libs/armeabi/
  rm -rf libs/armeabi-v7a/
  rm -rf libs/x86/
  rm -rf libs/mips/
}

clean

build_package grep
build_package nflog
build_package run_pie

cp src/com/googlecode/networklog/SysUtils.java .
perl update-jni-md5.pl
diff SysUtils.java src/com/googlecode/networklog/SysUtils.java

clean
