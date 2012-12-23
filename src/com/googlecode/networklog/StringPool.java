/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import java.util.concurrent.ConcurrentHashMap;

public class StringPool {
  final static int maxPoolSize = 1024;
  final public static ConcurrentHashMap<String, String> pool = new ConcurrentHashMap<String, String>(maxPoolSize);
  final public static ConcurrentHashMap<String, String> lowercasePool = new ConcurrentHashMap<String, String>(maxPoolSize);
  final public static CharArrayStringAATree charPool = new CharArrayStringAATree();
  static int poolSize = 0;
  static int lowercasePoolSize = 0;
  static CharArray charBuffer = new CharArray(256);

  public static void clearCharPool() {
    charPool.clear();
  }

  public static String get(char[] chars, int pos, int length) {
    charBuffer.reset();
    charBuffer.append(chars, pos, length);

    if (charPool.size + 1 >= maxPoolSize) {
      // clear pool to free memory and allow pool to rebuild
      MyLog.d("[StringPool] Clearing charPool");
      charPool.clear();
    }

    return charPool.insert(charBuffer);
  }

  public static String get(String string) {
    if(string == null) {
      return "";
    }

    String result = pool.get(string);

    if(result == null) {
      String newString = new String(string); // decouple string from substring(), etc
      
      pool.put(newString, newString);
      poolSize++;

      if(MyLog.enabled) {
        MyLog.d("[StringPool] new addition [" + newString + "]; pool size: " + poolSize);
      }

      if (poolSize >= maxPoolSize) {
        // clear pool to free memory and allow pool to rebuild
        MyLog.d("[StringPool] Clearing pool");
        pool.clear();
        poolSize = 0;
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
      lowercasePoolSize++;

      if(MyLog.enabled) {
        MyLog.d("[StringPool] new lowercase addition [" + newString + "]; pool size: " + lowercasePoolSize);
      }

      if (lowercasePoolSize >= maxPoolSize) {
        // clear pool to free memory and allow pool to rebuild
        MyLog.d("[StringPool] Clearing lowercase pool");
        lowercasePool.clear();
        lowercasePoolSize = 0;
      }

      return newString;
    } else {
      return result;
    }
  }
}
