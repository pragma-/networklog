/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import android.content.Context;

import java.io.File;

public class SelectBlockedApps extends AppsSelector
{
  public SelectBlockedApps() {
    name = "blocked apps";  // TODO: use string resource
  }

  public File getSaveFile(Context context) {
    return new File(context.getDir("data", Context.MODE_PRIVATE), "blockedapps.txt");
  }

  public void negativeButton() {
    NetworkLog.selectBlockedApps = null;
  }

  public void positiveButton() {
    NetworkLogService.blockedApps = apps;
    NetworkLog.selectBlockedApps = null;
  }
}
