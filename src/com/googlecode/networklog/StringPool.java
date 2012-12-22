/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.Collections;

public class StringPool {
  final static int maxPoolSize = 1024;
  final public static ConcurrentHashMap<String, String> pool = new ConcurrentHashMap<String, String>(maxPoolSize);
  final public static ConcurrentHashMap<String, String> lowercasePool = new ConcurrentHashMap<String, String>(maxPoolSize);
  final public static ArrayList<String> charPool = new ArrayList<String>(maxPoolSize);
  static int poolSize = 0;
  static int lowercasePoolSize = 0;
  static int charPoolSize = 0;

  public static void clearCharPool() {
    charPool.clear();
    charPoolSize = 0;
  }

  public static String get(char[] chars, int pos, int length) {
    int min = 0;
    int max = charPool.size() - 1;
    int mid;
    String string;
    int charLength = pos + length;
    int stringLength;
    int difference;
    boolean found = false;
    int i, j;

    if(max >= 0) {
      // binary search to find matching string
      while(max >= min) {
        mid = (max + min) / 2;
        string = charPool.get(mid);
        stringLength = string.length();
        i = pos;
        j = 0;
        found = true;

        while(i < charLength && j < stringLength) {
          difference = chars[i++] - string.charAt(j++);

          if(difference > 0) {
            min = mid + 1;
            found = false;
            break;
          } else if(difference < 0) {
            max = mid - 1;
            found = false;
            break;
          }
        }

        if(found == true) {
          // may have found match
          if(length != stringLength) {
            // lengths do not match, so strings cannot match
            if(length > stringLength) {
              min = mid + 1;
            } else {
              max = mid - 1;
            }
            continue;
          }
          // found match
          return string;
        }
      }
    }

    // no match found
    string = new String(chars, pos, length);

    if(MyLog.enabled) {
      MyLog.d("charPool creating new string: [" + string + "]");
    }

    charPool.add(string);
    Collections.sort(charPool);
    charPoolSize++;

    if(MyLog.enabled) {
      MyLog.d("[StringPool] new charPool addition [" + string + "]; pool size: " + charPoolSize);
    }

    if (charPoolSize >= maxPoolSize) {
      // clear pool to free memory and allow pool to rebuild
      MyLog.d("[StringPool] Clearing charPool");
      charPool.clear();
      charPoolSize = 0;
    }

    return string;
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
