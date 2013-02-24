/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import android.util.Log;

import java.util.HashMap;

public class ThroughputTracker {
  public static String throughputString = "";

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

            updateThroughput(totalUpload * 8, totalDownload * 8);

            throughputMap.clear();
            totalUpload = 0;
            totalDownload = 0;
          } else if(isDirty) {
            isDirty = false;
            updateThroughput(0, 0);
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

    updateThroughput(0, 0);
    updater = new ThroughputUpdater();
    new Thread(updater, "ThroughputUpdater").start();
  }

  public static void stopUpdater() {
    if(updater == null) {
      return;
    }

    updateThroughput(-1, -1);
    updater.stop();
  }

  public static void updateThroughput(long upload, long download) {
    if(NetworkLogService.instance == null || upload == -1 || download == -1) {
      throughputString = "";
    } else {
      throughputString = StringUtils.formatToBytes(upload) + "bps/" + StringUtils.formatToBytes(download) + "bps";

      if(MyLog.enabled) {
        MyLog.d("Throughput: " + throughputString);
      }
    }

    int icon;
    if(upload > 0 && download > 0) {
      icon = R.drawable.up1_down1;
    } else if(upload > 0 && download == 0) {
      icon = R.drawable.up1_down0;
    } else if(upload == 0 && download > 0) {
      icon = R.drawable.up0_down1;
    } else {
      icon = R.drawable.up0_down0;
    }

    NetworkLogService.updateNotification(icon);
    NetworkLog.updateStatus(icon);
  }
}
