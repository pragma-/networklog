/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import android.os.SystemClock;

public class PacketGraphItem {
  double timestamp;
  double len;

  public PacketGraphItem(double len) {
    this.timestamp = System.currentTimeMillis();
    this.len = len;
  }

  public PacketGraphItem(double timestamp, double len) {
    this.timestamp = timestamp;
    this.len = len;
  }

  public String toString() {
    return "(" + timestamp + ", " + len + ")";
  }
}
