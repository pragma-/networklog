package com.googlecode.networklog;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

public class RetainInstanceData {
  /* ApplicationsTracker */
  ArrayList<ApplicationsTracker.AppEntry> applicationsTrackerInstalledApps;
  HashMap<String, ApplicationsTracker.AppEntry> applicationsTrackerInstalledAppsHash;
  int applicationsTrackerAppCount;

  public void retainApplicationsTrackerData() {
    applicationsTrackerInstalledApps = ApplicationsTracker.installedApps;
    applicationsTrackerInstalledAppsHash = ApplicationsTracker.installedAppsHash;
    applicationsTrackerAppCount = ApplicationsTracker.appCount;
  }

  /* LogFragment */
  ArrayList<LogFragment.ListItem> logFragmentListData;
  ArrayList<LogFragment.ListItem> logFragmentListDataBuffer;
  ArrayList<LogFragment.ListItem> logFragmentListDataUnfiltered;

  public void retainLogFragmentData() {
    if(NetworkLog.logFragment != null) {
      logFragmentListData = NetworkLog.logFragment.listData;
      logFragmentListDataBuffer = NetworkLog.logFragment.listDataBuffer;
      logFragmentListDataUnfiltered = NetworkLog.logFragment.listDataUnfiltered;
    }
  }

  /* AppFragment */
  ArrayList<AppFragment.GroupItem> appFragmentGroupData;
  ArrayList<AppFragment.GroupItem> appFragmentGroupDataBuffer;
  boolean appFragmentGroupDataBufferIsDirty;
  Sort appFragmentSortBy;
  Sort appFragmentPreSortBy;
  AppFragment.GroupItem appFragmentCachedSearchItem;

  public void retainAppFragmentData() {
    if(NetworkLog.appFragment != null) {
      appFragmentGroupData = NetworkLog.appFragment.groupData;
      appFragmentGroupDataBuffer = NetworkLog.appFragment.groupDataBuffer;
      appFragmentGroupDataBufferIsDirty = NetworkLog.appFragment.groupDataBufferIsDirty;
      appFragmentSortBy = NetworkLog.appFragment.sortBy;
      appFragmentPreSortBy = NetworkLog.appFragment.preSortBy;
      appFragmentCachedSearchItem = NetworkLog.appFragment.cachedSearchItem;
    }
  }

  /* HistoryLoader */
  boolean historyDialogShowing;
  int historyDialogMax;
  int historyDialogProgress;

  public void retainHistoryLoaderData() {
    historyDialogShowing = NetworkLog.history.dialog_showing;
    historyDialogMax = NetworkLog.history.dialog_max;
    historyDialogProgress = NetworkLog.history.dialog_progress;
  }

  /* NetworkLog */
  NetworkLog.State networkLogState;
  NetworkResolver networkLogResolver;

  public void retainRetainInstanceData() {
    networkLogState = NetworkLog.state;
    networkLogResolver = NetworkLog.resolver;
  }

  /* retain data */
  public RetainInstanceData() {
    retainApplicationsTrackerData();
    retainLogFragmentData();
    retainAppFragmentData();
    retainRetainInstanceData();
    retainHistoryLoaderData();
  }
}
