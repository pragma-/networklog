/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

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
