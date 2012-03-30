package com.googlecode.iptableslog;

import android.os.SystemClock;

public class PacketGraphItem {
  long timestamp;
  long len;

  public PacketGraphItem(long len) {
    this.timestamp = System.currentTimeMillis();
    this.len = len;
  }

  public PacketGraphItem(long timestamp, long len) {
    this.timestamp = timestamp;
    this.len = len;
  }

  public String toString() {
    return "(" + timestamp + ", " + len + ")";
  }
}
