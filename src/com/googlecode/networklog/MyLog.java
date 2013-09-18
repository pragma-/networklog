/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import android.util.Log;

public class MyLog {
  public static boolean enabled = true;
  public static int level = 0;
  public static String tag = "NetworkLog";

  public static void d(String msg) {
    d(0, tag, msg);
  }

  public static void d(String tag, String msg) {
    d(0, tag, msg);
  }

  public static void d(int level, String msg) {
    d(level, tag, msg);
  }

  public static void d(int level, String tag, String msg) {
    if(!enabled || level > MyLog.level) {
      return;
    }

    for(String line : msg.split("\n")) {
      Log.d(tag, line);
    }
  }
}
