package com.googlecode.networklog;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

public class NetworkLogData {
  /* ApplicationsTracker */
  ArrayList<ApplicationsTracker.AppEntry> applicationsTrackerInstalledApps;
  HashMap<String, ApplicationsTracker.AppEntry> applicationsTrackerInstalledAppsHash;
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
    if(NetworkLog.logView != null) {
      logViewListData = NetworkLog.logView.listData;
      logViewListDataBuffer = NetworkLog.logView.listDataBuffer;
      logViewListDataUnfiltered = NetworkLog.logView.listDataUnfiltered;
    }
  }

  /* AppView */
  ArrayList<AppView.GroupItem> appViewGroupData;
  ArrayList<AppView.GroupItem> appViewGroupDataBuffer;
  boolean appViewGroupDataBufferIsDirty;
  Sort appViewSortBy;
  Sort appViewPreSortBy;
  AppView.GroupItem appViewCachedSearchItem;

  public void gatherAppViewData() {
    if(NetworkLog.appView != null) {
      appViewGroupData = NetworkLog.appView.groupData;
      appViewGroupDataBuffer = NetworkLog.appView.groupDataBuffer;
      appViewGroupDataBufferIsDirty = NetworkLog.appView.groupDataBufferIsDirty;
      appViewSortBy = NetworkLog.appView.sortBy;
      appViewPreSortBy = NetworkLog.appView.preSortBy;
      appViewCachedSearchItem = NetworkLog.appView.cachedSearchItem;
    }
  }

  /* HistoryLoader */
  boolean historyDialogShowing;
  int historyDialogMax;
  int historyDialogProgress;

  public void gatherHistoryLoaderData() {
    historyDialogShowing = NetworkLog.history.dialog_showing;
    historyDialogMax = NetworkLog.history.dialog_max;
    historyDialogProgress = NetworkLog.history.dialog_progress;
  }

  /* NetworkLog */
  NetworkLog.State iptablesLogState;
  NetworkResolver iptablesLogResolver;
  boolean iptablesLogOutputPaused;

  public void gatherNetworkLogData() {
    iptablesLogState = NetworkLog.state;
    iptablesLogResolver = NetworkLog.resolver;
    iptablesLogOutputPaused = NetworkLog.outputPaused;
  }

  /* gather data */
  public NetworkLogData() {
    gatherApplicationsTrackerData();
    gatherLogViewData();
    gatherAppViewData();
    gatherNetworkLogData();
    gatherHistoryLoaderData();
  }
}
