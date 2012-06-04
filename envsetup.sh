#!/bin/sh

# Just some convenient functions and aliases for some common tasks
# Usage: $ source envsetup.sh

APP="NetworkLog-release.apk"

function clean() { ant clean; }
function debug_build() { ant debug; }
function release_build() { ant release; }
function install_device() { adb -d install -r bin/$APP; }
function install_emulator() { adb -e install -r bin/$APP; }
function logcat() { adb logcat -c && adb logcat -v time | tee log-`date +%Y%m%d-%H:%M.%N`; }
function meminfo() { adb shell dumpsys meminfo; }
function procrank_sample() { adb shell procrank | grep networklog | tee procrank-`date +%Y%m%d-%H:%M.%N`; }
function clean_build_install_and_logcat() { clean && release_build && install_device && logcat; }

function check_status()
{
  adb shell iptables -L -v | head -n 25
  echo -----------------------------------------------------
  adb shell ps busybox_g1
}

function jdb_debug()
{
  adb forward tcp:8700 jdwp:$(adb jdwp | tail -1)
  jdb -sourcepath ./src -connect com.sun.jdi.SocketAttach:hostname=localhost,port=8700
}

function push() 
{
  git push github master
  git push bb master
  git push gc master
}

alias b=clean_build_install_and_logcat
alias c=clean
alias id=install_device
alias ie=install_emulator
alias lc=logcat
alias mi=meminfo
alias procrank=procrank_sample
alias v=check_status
alias jdb=jdb_debug
