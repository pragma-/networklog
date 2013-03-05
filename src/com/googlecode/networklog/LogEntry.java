/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

public class LogEntry {
  int uid;
  String uidString;
  String in;
  String out;
  String proto;
  String src;
  String dst;
  int len;
  int spt;
  int dpt;
  long timestamp;

  public boolean isValid() {
    if(StringUtils.contains(in, "{}:=")) {
      return false;
    }

    if(StringUtils.contains(out, "{}:=")) {
      return false;
    }

    if(StringUtils.contains(proto, "{}:=")) {
      return false;
    }

    if(StringUtils.contains(src, "{}:=")) {
      return false;
    }

    if(StringUtils.contains(dst, "{}:=")) {
      return false;
    }

    return true;
  }
}
