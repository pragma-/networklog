package com.googlecode.iptableslog;

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
