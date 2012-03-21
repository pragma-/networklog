package com.googlecode.iptableslog;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.app.ProgressDialog;
import android.os.Handler;

import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;

public class ApplicationsTracker {
  public static ArrayList<AppEntry> installedApps;
  public static Hashtable<String, AppEntry> installedAppsHash;
  public static ProgressDialog dialog;
  public static int appCount;
  public static Object dialogLock = new Object();

  public static class AppEntry {
    String name;
    String packageName;
    Drawable icon;
    int uid;
  }

  public static void restoreData(IptablesLogData data) {
    installedApps = data.applicationsTrackerInstalledApps;
    installedAppsHash = data.applicationsTrackerInstalledAppsHash;
  }

  public static void getInstalledApps(final Context context, final Handler handler) {
    MyLog.d("Loading installed apps");
    if(IptablesLog.data == null) {
      installedApps = new ArrayList<AppEntry>();
      installedAppsHash = new Hashtable<String, AppEntry>();
    } else {
      restoreData(IptablesLog.data);
      installedApps.clear();
      installedAppsHash.clear();
    }

    MyLog.d("Get package manager");
    List<ApplicationInfo> apps = new ArrayList<ApplicationInfo>();
    PackageManager pm = context.getPackageManager();

    MyLog.d("Getting apps");
    apps = pm.getInstalledApplications(0);

    appCount = apps.size();
    handler.post(new Runnable() {
      public void run() {
        MyLog.d("Showing progress dialog; size: " + appCount);
        synchronized(dialogLock) {
          dialog = new ProgressDialog(context);
          dialog.setIndeterminate(false);
          dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
          dialog.setMax(appCount);
          dialog.setCancelable(false);
          dialog.setTitle("");
          dialog.setMessage("Loading apps");
          dialog.show();
        }
      }
    });

    int count = 0;
    for(final ApplicationInfo app : apps) {
      MyLog.d("Processing app " + app);

      if(IptablesLog.initRunner.running == false) {
        MyLog.d("[ApplicationsTracker] Initialization aborted");
        return;
      }

      final int progress = ++count;
      handler.post(new Runnable() {
        public void run() {
          synchronized(dialogLock) {
            if(dialog != null) {
              MyLog.d("Updating dialog progress: " + progress + " " + app.processName);
              dialog.setProgress(progress);
            }
          }
        }
      });

      String name = app.loadLabel(pm).toString();
      MyLog.d("Label: " + name);
      int uid = app.uid;
      String sUid = Integer.toString(uid);

      AppEntry entryHash = installedAppsHash.get(sUid);

      AppEntry entry = new AppEntry();
      entry.name = name;
      entry.icon = null;
      entry.uid = uid;
      entry.packageName = new String(app.packageName);

      installedApps.add(entry);

      if(entryHash != null) {
        entryHash.name.concat("; " + name);
      } else {
        installedAppsHash.put(sUid, entry);
      } 
    }

    AppEntry entry = new AppEntry();
    entry.name = "Kernel";
    entry.icon = context.getResources().getDrawable(R.drawable.linux_icon);
    entry.packageName = null;
    entry.uid = -1;

    installedApps.add(entry);
    installedAppsHash.put("-1", entry);

    AppEntry entryHash = installedAppsHash.get("0");
    if(entryHash == null) {
      entry = new AppEntry();
      entry.name = "Root";
      entry.icon = context.getResources().getDrawable(R.drawable.root_icon);
      entry.packageName = null;
      entry.uid = 0;

      installedApps.add(entry);
      installedAppsHash.put("0", entry);
    }

    handler.post(new Runnable() {
      public void run() {
        MyLog.d("Dismissing dialog");
        synchronized(dialogLock) {
          if(dialog != null) {
            dialog.dismiss();
            dialog = null;
          }
        }
      }
    });
    MyLog.d("Done getting installed apps");
  }
}
