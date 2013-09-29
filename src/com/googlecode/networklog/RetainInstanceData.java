/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class RetainInstanceData {
  /* ApplicationsTracker */
  ArrayList<ApplicationsTracker.AppEntry> applicationsTrackerInstalledApps;
  HashMap<String, ApplicationsTracker.AppEntry> applicationsTrackerUidMap;
  HashMap<String, ApplicationsTracker.AppEntry> applicationsTrackerPackageMap;
  int applicationsTrackerAppCount;

  public void retainApplicationsTrackerData() {
    applicationsTrackerInstalledApps = ApplicationsTracker.installedApps;
    applicationsTrackerUidMap = ApplicationsTracker.uidMap;
    applicationsTrackerPackageMap = ApplicationsTracker.packageMap;
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

  /* FeedbackDialog */
  String feedbackDialogMessage;
  boolean feedbackDialogAttachLogcat;
  int feedbackDialogCursorPosition;

  public void retainFeedbackDialogData() {
    if(NetworkLog.feedbackDialog != null && NetworkLog.feedbackDialog.dialog != null) {
      feedbackDialogMessage = NetworkLog.feedbackDialog.message.getText().toString();
      feedbackDialogAttachLogcat = NetworkLog.feedbackDialog.attachLogcat.isChecked();
      feedbackDialogCursorPosition = NetworkLog.feedbackDialog.message.getSelectionStart();
    }
  }

  /* ClearLog */
  boolean clearLogDialogShowing;
  boolean clearLogProgressDialogShowing;
  int clearLogProgress;
  int clearLogProgressMax;

  public void retainClearLogData() {
    clearLogDialogShowing = NetworkLog.clearLog.dialog != null && NetworkLog.clearLog.dialog.isShowing();
    clearLogProgressDialogShowing = NetworkLog.clearLog.progressDialog != null && NetworkLog.clearLog.progressDialog.isShowing();
    clearLogProgress = NetworkLog.clearLog.progress;
    clearLogProgressMax = NetworkLog.clearLog.progress_max;
  }

  /* ExportDialog */
  boolean exportDialogShowing;
  boolean exportDialogProgressDialogShowing;
  Date exportDialogStartDate;
  Date exportDialogEndDate;
  File exportDialogFile;
  int exportDialogProgress;
  int exportDialogProgressMax;
  ExportDialog.DatePickerMode exportDialogDatePickerMode;

  public void retainExportDialogData() {
    if(NetworkLog.exportDialog != null) {
      if(NetworkLog.exportDialog.dialog != null) {
        exportDialogShowing = true;
        exportDialogStartDate = NetworkLog.exportDialog.startDate;
        exportDialogEndDate = NetworkLog.exportDialog.endDate;
        exportDialogFile = NetworkLog.exportDialog.file;
        exportDialogDatePickerMode = NetworkLog.exportDialog.datePickerMode;
      }

      if(NetworkLog.exportDialog.progressDialog != null && NetworkLog.exportDialog.progressDialog != null) {
        exportDialogProgressDialogShowing = true;
        exportDialogProgress = NetworkLog.exportDialog.progress;
        exportDialogProgressMax = NetworkLog.exportDialog.progress_max;
      }
    }
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
    retainFeedbackDialogData();
    retainClearLogData();
    retainExportDialogData();
  }
}
