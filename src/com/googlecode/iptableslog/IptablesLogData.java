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
  ArrayList<LogView.ListItem> logViewListDataBuffer;

  public void gatherLogViewData() {
    logViewListData = LogView.listData;
    logViewListDataBuffer = LogView.listDataBuffer;
  }

  /* AppView */
  ArrayList<AppView.ListItem> appViewListData;
  ArrayList<AppView.ListItem> appViewListDataBuffer;
  boolean appViewListDataBufferIsDirty;
  AppView.Sort appViewSortBy;
  AppView.ListItem appViewCachedSearchItem;

  public void gatherAppViewData() {
    appViewListData = AppView.listData;
    appViewListDataBuffer = AppView.listDataBuffer;
    appViewListDataBufferIsDirty = AppView.listDataBufferIsDirty;
    appViewSortBy = AppView.sortBy;
    appViewCachedSearchItem = AppView.cachedSearchItem;
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
