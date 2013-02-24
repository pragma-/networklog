/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import android.util.Log;

import java.util.HashMap;

public class ThroughputTracker {
  static class ThroughputData {
    ApplicationsTracker.AppEntry app;
    long upload;
    long download;
  }

  public static HashMap<String, ThroughputData> throughputMap = new HashMap<String, ThroughputData>();

  public static void updateEntry(LogEntry entry) {
    ApplicationsTracker.AppEntry appEntry = ApplicationsTracker.uidMap.get(entry.uidString);

    if(appEntry == null) {
      Log.w("NetworkLog", "[ThroughputTracker] No app entry for uid " + entry.uidString);
      return;
    }

    synchronized(throughputMap) {
      ThroughputData throughput = throughputMap.get(appEntry.packageName);

      if(throughput == null) {
        throughput = new ThroughputData();
        throughput.app = appEntry;
        throughputMap.put(appEntry.packageName, throughput);
      }

      if(entry.in != null && entry.in.length() > 0) {
        throughput.download += entry.len;
      } else {
        throughput.upload += entry.len;
      }
    }
  }

  static class ThroughputUpdater implements Runnable {
    boolean running = false;
    long totalUpload;
    long totalDownload;

    public void stop() {
      running = false;
    }

    public void run() {
      String uploadSpeed;
      String downloadSpeed;
      boolean isDirty = false;
      running = true;

      while(running) {
        synchronized(throughputMap) {
          if(!throughputMap.isEmpty()) {
            isDirty = true;
            for(ThroughputData entry : throughputMap.values()) {
              uploadSpeed = StringUtils.formatToBytes(entry.upload * 8) + "bps";
              downloadSpeed = StringUtils.formatToBytes(entry.download * 8) + "bps";

              if(MyLog.enabled) {
                MyLog.d(entry.app.name + " throughput: " + uploadSpeed + "/" + downloadSpeed);
              }

              totalUpload += entry.upload;
              totalDownload += entry.download;
            }

            uploadSpeed = StringUtils.formatToBytes(totalUpload * 8) + "bps";
            downloadSpeed = StringUtils.formatToBytes(totalDownload * 8) + "bps";

            final String throughput = uploadSpeed + "/" + downloadSpeed;

            if(MyLog.enabled) {
              MyLog.d("Throughput: " + throughput);
            }

            NetworkLogService.instance.updateThroughputString(throughput);

            throughputMap.clear();
            totalUpload = 0;
            totalDownload = 0;
          } else if(isDirty) {
            isDirty = false;
            NetworkLogService.instance.updateThroughputString("0bps/0bps");
          }
        }

        try { Thread.sleep(1000); } catch (Exception e) { Log.d("NetworkLog", "ThroughputUpdater", e); }
      }
    }
  }

  static ThroughputUpdater updater;

  public static void startUpdater() {
    if(updater != null) {
      stopUpdater();
    }

    updater = new ThroughputUpdater();
    new Thread(updater, "ThroughputUpdater").start();
  }

  public static void stopUpdater() {
    if(updater == null) {
      return;
    }

    updater.stop();
  }
}
