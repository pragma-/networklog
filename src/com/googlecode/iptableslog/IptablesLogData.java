package com.googlecode.iptableslog;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import android.os.Messenger;
import android.content.ServiceConnection;

public class IptablesLogData {
  /* ApplicationsTracker */
  ArrayList<ApplicationsTracker.AppEntry> applicationsTrackerInstalledApps;
  Hashtable<String, ApplicationsTracker.AppEntry> applicationsTrackerInstalledAppsHash;
  int applicationsTrackerAppCount;

  public void gatherApplicationsTrackerData() {
    applicationsTrackerInstalledApps = ApplicationsTracker.installedApps;
    applicationsTrackerInstalledAppsHash = ApplicationsTracker.installedAppsHash;
    applicationsTrackerAppCount = ApplicationsTracker.appCount;
  }

  /* LogView */
  ArrayList<LogView.ListItem> logViewListData;
  ArrayList<LogView.ListItem> logViewListDataBuffer;
  ArrayList<LogView.ListItem> logViewListDataUnfiltered;

  public void gatherLogViewData() {
    if(IptablesLog.logView != null) {
      logViewListData = IptablesLog.logView.listData;
      logViewListDataBuffer = IptablesLog.logView.listDataBuffer;
      logViewListDataUnfiltered = IptablesLog.logView.listDataUnfiltered;
    }
  }

  /* AppView */
  ArrayList<AppView.ListItem> appViewListData;
  ArrayList<AppView.ListItem> appViewListDataBuffer;
  boolean appViewListDataBufferIsDirty;
  Sort appViewSortBy;
  Sort appViewPreSortBy;
  AppView.ListItem appViewCachedSearchItem;

  public void gatherAppViewData() {
    if(IptablesLog.appView != null) {
      appViewListData = IptablesLog.appView.listData;
      appViewListDataBuffer = IptablesLog.appView.listDataBuffer;
      appViewListDataBufferIsDirty = IptablesLog.appView.listDataBufferIsDirty;
      appViewSortBy = IptablesLog.appView.sortBy;
      appViewPreSortBy = IptablesLog.appView.preSortBy;
      appViewCachedSearchItem = IptablesLog.appView.cachedSearchItem;
    }
  }

  /* HistoryLoader */
  boolean historyDialogShowing;
  int historyDialogMax;
  int historyDialogProgress;

  public void gatherHistoryLoaderData() {
    historyDialogShowing = IptablesLog.history.dialog_showing;
    historyDialogMax = IptablesLog.history.dialog_max;
    historyDialogProgress = IptablesLog.history.dialog_progress;
  }

  /* IptablesLog */
  IptablesLog.State iptablesLogState;
  NetworkResolver iptablesLogResolver;
  boolean iptablesLogOutputPaused;

  public void gatherIptablesLogData() {
    iptablesLogState = IptablesLog.state;
    iptablesLogResolver = IptablesLog.resolver;
    iptablesLogOutputPaused = IptablesLog.outputPaused;
  }

  /* gather data */
  public IptablesLogData() {
    gatherApplicationsTrackerData();
    gatherLogViewData();
    gatherAppViewData();
    gatherIptablesLogData();
    gatherHistoryLoaderData();
  }
}
