package com.googlecode.iptableslog;

import android.util.Log; 

import java.util.ArrayList;
import java.util.Hashtable;

public class IptablesLogData {
  /* ApplicationsTracker */
  ArrayList<ApplicationsTracker.AppEntry> applicationsTrackerInstalledApps;
  Hashtable<String, ApplicationsTracker.AppEntry> applicationsTrackerInstalledAppsHash;

  public void gatherApplicationsTrackerData() {
    applicationsTrackerInstalledApps = ApplicationsTracker.installedApps;
    applicationsTrackerInstalledAppsHash = ApplicationsTracker.installedAppsHash;
  }

  /* LogView */
  ArrayList<LogView.ListItem> logViewListData;

  public void gatherLogViewData() {
    logViewListData = LogView.listData;
  }

  /* AppView */
  ArrayList<AppView.ListItem> appViewListData;
  AppView.Sort appViewSortBy;

  public void gatherAppViewData() {
    appViewListData = AppView.listData;
    appViewSortBy = AppView.sortBy;
  }

  /* Iptables */
  ShellCommand iptablesLogTrackerCommand;

  public void gatherIptablesLogTrackerData() {
    iptablesLogTrackerCommand = IptablesLogTracker.command;
  }

  public IptablesLogData() {
    gatherApplicationsTrackerData();
    gatherLogViewData();
    gatherAppViewData();
    gatherIptablesLogTrackerData();
  }
}
