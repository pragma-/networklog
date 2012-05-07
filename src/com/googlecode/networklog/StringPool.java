package com.googlecode.networklog;

import java.util.concurrent.ConcurrentHashMap;

public class StringPool {
  final public static ConcurrentHashMap<String, String> pool = new ConcurrentHashMap<String, String>(1024);

  public static String get(String string) {
    if (pool.size() > NetworkLog.settings.getMaxLogEntries()) {
      // clear pool to free memory and allow pool to rebuild
      MyLog.d("[StringPool] Clearing pool");
      pool.clear();
    }

    String result = pool.get(string);

    if(result == null) {
      String newString = new String(string); // decouple string from substring(), etc
      pool.put(newString, newString);
      MyLog.d("[StringPool] new addition [" + newString + "]; pool size: " + pool.size());
      return newString;
    } else {
      return result;
    }
  }
}
