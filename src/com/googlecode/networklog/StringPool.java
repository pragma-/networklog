/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import java.util.concurrent.ConcurrentHashMap;

public class StringPool {
  final public static ConcurrentHashMap<String, String> pool = new ConcurrentHashMap<String, String>(1024);
  final public static ConcurrentHashMap<String, String> lowercasePool = new ConcurrentHashMap<String, String>(1024);
  static long size = 0;

  public static String get(String string) {
    if(string == null) {
      return "";
    }

    String result = pool.get(string);

    if(result == null) {
      String newString = new String(string); // decouple string from substring(), etc
      
      pool.put(newString, newString);
      size++;

      if(MyLog.enabled) {
        MyLog.d("[StringPool] new addition [" + newString + "]; pool size: " + size);
      }

      if (size > NetworkLog.settings.getMaxLogEntries()) {
        // clear pool to free memory and allow pool to rebuild
        MyLog.d("[StringPool] Clearing pool");
        pool.clear();
        size = 0;
      }

      return newString;
    } else {
      return result;
    }
  }

  public static String getLowerCase(String string) {
    if(string == null) {
      return "";
    }

    String result = lowercasePool.get(string);

    if(result == null) {
      String newString = new String(string.toLowerCase());

      lowercasePool.put(string, newString);
      return newString;
    } else {
      return result;
    }
  }
}
