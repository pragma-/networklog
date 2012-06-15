/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.lang.StringBuilder;

public class Timestamp {
  static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
  static final SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);

  public static String getTimestamp(long timestamp) {
    return format.format(timestamp);
  }

  public static String getTimestamp() {
    return format.format(new Date());
  }
}
