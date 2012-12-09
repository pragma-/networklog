/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import android.content.Context;
import android.preference.Preference;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.ListPreference;
import android.os.Bundle;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

public class Preferences extends SherlockPreferenceActivity implements OnPreferenceClickListener {
  private PreferenceConfigurationData data = null;
  private AlertDialog warnStartForegroundDialog = null;

  private class PreferenceConfigurationData {
    boolean history_dialog_showing;
    boolean start_foreground_dialog_showing;
    boolean clearlog_dialog_showing;
    boolean clearlog_progress_dialog_showing;

    PreferenceConfigurationData() {
      history_dialog_showing = NetworkLog.history.dialog_showing;
      start_foreground_dialog_showing = (warnStartForegroundDialog == null) ? false : true;
      clearlog_dialog_showing = NetworkLog.clearLog.dialog != null && NetworkLog.clearLog.dialog.isShowing();
      clearlog_progress_dialog_showing = NetworkLog.clearLog.progressDialog != null && NetworkLog.clearLog.progressDialog.isShowing();
    }
  }

  @Override
    public void onDestroy() {
      MyLog.d("Destroying preferences activity");

      if(warnStartForegroundDialog != null) {
        warnStartForegroundDialog.dismiss();
      }

      if(NetworkLog.history.dialog_showing) {
        NetworkLog.history.dialog.dismiss();
        NetworkLog.history.dialog = null;
      }

      if(NetworkLog.clearLog.dialog != null && NetworkLog.clearLog.dialog.isShowing()) {
        NetworkLog.clearLog.dialog.dismiss();
        NetworkLog.clearLog.dialog = null;
      }

      if(NetworkLog.clearLog.progressDialog != null && NetworkLog.clearLog.progressDialog.isShowing()) {
        NetworkLog.clearLog.progressDialog.dismiss();
        NetworkLog.clearLog.progressDialog = null;
      }

      super.onDestroy();
    }

  @Override
    public Object onRetainNonConfigurationInstance() {
      MyLog.d("Saving preference run");
      data = new PreferenceConfigurationData();
      return data;
    }

  @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      MyLog.d("Creating preferences activity");

      addPreferencesFromResource(R.xml.preferences);

      findPreference("filter_dialog").setOnPreferenceClickListener(this);
      findPreference("notifications_statusbar").setOnPreferenceClickListener(this);
      findPreference("notifications_statusbar_apps_dialog").setOnPreferenceClickListener(this);
      findPreference("notifications_toast").setOnPreferenceClickListener(this);
      findPreference("notifications_toast_apps_dialog").setOnPreferenceClickListener(this);
      findPreference("clear_log").setOnPreferenceClickListener(this);

      EditTextPreference logfile = (EditTextPreference) findPreference("logfile");
      logfile.setText(NetworkLog.settings.getLogFile());

      CheckBoxPreference foreground = (CheckBoxPreference) findPreference("start_foreground");
      foreground.setOnPreferenceClickListener(this);
      foreground.setChecked(NetworkLog.settings.getStartForeground());

      String entries[] = getResources().getStringArray(R.array.interval_entries);
      String values[] = getResources().getStringArray(R.array.interval_values);

      final Context context = this;
      OnPreferenceChangeListener changeListener = new OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
          if(preference.getKey().equals("history_size")) {
            NetworkLog.appFragment.clear();
            NetworkLog.logFragment.clear();
            NetworkLog.history.loadEntriesFromFile(context, (String)newValue);
            return true;
          }

          if(preference.getKey().equals("interval_placeholder")) {
            NetworkLog.settings.setGraphInterval(Long.parseLong((String) newValue));
            return true;
          }
          
          if(preference.getKey().equals("viewsize_placeholder")) {
            NetworkLog.settings.setGraphViewsize(Long.parseLong((String) newValue));
            return true;
          }

          return true;
        }
      };

      ListPreference pref = (ListPreference) findPreference("interval_placeholder");
      pref.setEntries(entries);
      pref.setEntryValues(values);
      pref.setValue(String.valueOf(NetworkLog.settings.getGraphInterval()));
      pref.setOnPreferenceChangeListener(changeListener);

      pref = (ListPreference) findPreference("viewsize_placeholder");
      pref.setEntries(entries);
      pref.setEntryValues(values);
      pref.setValue(String.valueOf(NetworkLog.settings.getGraphViewsize()));
      pref.setOnPreferenceChangeListener(changeListener);

      pref = (ListPreference) findPreference("history_size");
      pref.setOnPreferenceChangeListener(changeListener);

      data = (PreferenceConfigurationData) getLastNonConfigurationInstance();

      if(data != null) {
        MyLog.d("Restoring preferences run");

        if(data.start_foreground_dialog_showing == true) {
          warnStartForegroundDialog = toggleWarnStartForeground(this, foreground);
        }

        if(data.history_dialog_showing) {
          NetworkLog.history.createProgressDialog(this);
        }

        if(data.clearlog_dialog_showing) {
          NetworkLog.clearLog.showClearLogDialog(this);
        }

        if(data.clearlog_progress_dialog_showing) {
          NetworkLog.clearLog.showProgressDialog(this);
        }

        MyLog.d("Restored preferences run");
      }
    }

  @Override
    public boolean onPreferenceClick(Preference preference) {
      MyLog.d("Preference [" + preference.getKey() + "] clicked");

      if(preference.getKey().equals("notifications_statusbar")
          || preference.getKey().equals("notifications_toast")
          || preference.getKey().equals("notifications_statusbar_apps_dialog")
          || preference.getKey().equals("notifications_toast_apps_dialog")) 
      {
        new ComingSoonDialog(this);
        return true;
      }

      if(preference.getKey().equals("filter_dialog")) {
        new FilterDialog(this);
        return true;
      }

      if(preference.getKey().equals("start_foreground")) {
        warnStartForegroundDialog = toggleWarnStartForeground(this, (CheckBoxPreference) preference);
        return true;
      }

      if(preference.getKey().equals("clear_log")) {
        NetworkLog.clearLog.showClearLogDialog(this);
        return true;
      }

      return false;
    }

  public AlertDialog toggleWarnStartForeground(final Context context, final CheckBoxPreference preference) {
    if(NetworkLog.settings.getStartForeground() == false) {
      // don't warn when enabling
      preference.setChecked(true);
      NetworkLog.settings.setStartForeground(true);
      NetworkLog.toggleServiceForeground(true);
      return null;
    } else {
      preference.setChecked(true);

      AlertDialog.Builder builder = new AlertDialog.Builder(context);
      builder.setTitle("Warning")
        .setMessage("Disabling the notification/foreground state will allow Android to kill this service at any time, which may disrupt logging.")
        .setCancelable(true)
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            preference.setChecked(true);
            NetworkLog.settings.setStartForeground(true);
            NetworkLog.toggleServiceForeground(true);
            warnStartForegroundDialog = null;
            dialog.dismiss();
          }
        })
      .setPositiveButton("OK", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          preference.setChecked(false);
          NetworkLog.settings.setStartForeground(false);
          NetworkLog.toggleServiceForeground(false);
          warnStartForegroundDialog = null;
          dialog.dismiss();
        }
      });
      AlertDialog alert = builder.create();
      alert.show();
      return alert;
    }
  }

  public class ComingSoonDialog {
    public ComingSoonDialog(Context context) {
      AlertDialog.Builder builder = new AlertDialog.Builder(context);
      builder.setTitle("Coming soon")
        .setMessage("Sorry, this feature is not yet available.")
        .setCancelable(true)
        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            dialog.dismiss();
          }
        });
      AlertDialog alert = builder.create();
      alert.show();
    }
  }
}
