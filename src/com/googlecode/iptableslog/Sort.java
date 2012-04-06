package com.googlecode.iptableslog;

import java.util.HashMap;

public enum Sort
{
  UID, NAME, PACKETS, BYTES, TIMESTAMP;

  static final HashMap<String, Sort> sortMap = new HashMap<String, Sort>();

  static {
    for (Sort s : Sort.values()) {
      sortMap.put(s.toString(), s);
    }
  }

  public static Sort forValue(String value) {
    Sort result = sortMap.get(value);
    MyLog.d("Sort result: [" + (result == null ? "(null)" : result) + "]");
    return sortMap.get(value);
  }
}
