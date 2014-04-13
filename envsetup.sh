#!/bin/sh

# Just some convenient functions and aliases for some common tasks
# Usage: $ source envsetup.sh

APP="NetworkLog-release.apk"

function clean() { ant clean; }
function debug_build() { ant debug; }
function release_build() { ant release; }
function install_device() { adb -d install -r bin/$APP; }
function install_emulator() { adb -e install -r bin/$APP; }
function logcat_networklog() { adb $1 logcat -c && adb $1 logcat -v time | tee log-`date +%Y%m%d-%H:%M.%N` | grep NetworkLog; }
function logcat_all() { adb $1 logcat -c && adb $1 logcat -v time | tee log-`date +%Y%m%d-%H:%M.%N`; }
function meminfo() { adb shell dumpsys meminfo; }
function procrank_sample() { adb shell procrank | grep networklog | tee procrank-`date +%Y%m%d-%H:%M.%N`; }
function build_install_and_logcat() { release_build && install_device && logcat_all; }
function build_install_emulator_and_logcat() { release_build && install_emulator && logcat_all -e; }

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

function push_tags()
{
  git push github master --tags
  git push bb master --tags
  git push gc master --tags
  git push gitorious master --tags
}

function push() 
{
  git push github master
  git push bb master
  git push gc master
  git push gitorious master
}

alias b=build_install_and_logcat
alias be=build_install_emulator_and_logcat
alias c=clean
alias id=install_device
alias ie=install_emulator
alias lc=logcat_networklog
alias lca=logcat_all
alias lce='logcat_all -e'
alias mi=meminfo
alias procrank=procrank_sample
alias v=check_status
alias j=jdb_debug
alias pt=push_tags
alias cl='rm log-*'
alias vl='logs=(log-*); vim "${logs[@]: -1}"'
