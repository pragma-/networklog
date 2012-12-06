/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

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

  /* ClearLog */
  boolean clearLogDialogShowing;

  public void retainClearLogData() {
    clearLogDialogShowing = NetworkLog.clearLog.dialog != null && NetworkLog.clearLog.dialog.isShowing();
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
    retainClearLogData();
  }
}
