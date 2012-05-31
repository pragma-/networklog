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
    retainRetainInstanceData();
    retainHistoryLoaderData();
  }
}
