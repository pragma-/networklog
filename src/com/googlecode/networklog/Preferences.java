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
import android.util.Log;

import java.util.ArrayList;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.robobunny.SeekBarPreference;

public class Preferences extends SherlockPreferenceActivity implements OnPreferenceClickListener, OnPreferenceChangeListener {
  private InstanceData data = null;
  private AlertDialog warnStartForegroundDialog = null;

  private class InstanceData {
    boolean history_dialog_showing;
    boolean start_foreground_dialog_showing;
    boolean clearlog_dialog_showing;
    boolean clearlog_progress_dialog_showing;
    boolean selectToastApps_dialog_showing;
    ArrayList<SelectToastApps.AppItem> selectToastApps_appData;

    InstanceData() {
      history_dialog_showing = NetworkLog.history.dialog_showing;
      start_foreground_dialog_showing = (warnStartForegroundDialog == null) ? false : true;
      clearlog_dialog_showing = NetworkLog.clearLog.dialog != null && NetworkLog.clearLog.dialog.isShowing();
      clearlog_progress_dialog_showing = NetworkLog.clearLog.progressDialog != null && NetworkLog.clearLog.progressDialog.isShowing();

      if(NetworkLog.selectToastApps != null && NetworkLog.selectToastApps.dialog != null && NetworkLog.selectToastApps.dialog.isShowing()) {
        selectToastApps_dialog_showing = true;
        selectToastApps_appData = NetworkLog.selectToastApps.appData;
      }
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

      if(NetworkLog.selectToastApps != null && NetworkLog.selectToastApps.dialog != null && NetworkLog.selectToastApps.dialog.isShowing()) {
        NetworkLog.selectToastApps.dialog.dismiss();
        NetworkLog.selectToastApps = null;
      }

      super.onDestroy();
    }

  @Override
    public Object onRetainNonConfigurationInstance() {
      MyLog.d("Saving preference run");
      data = new InstanceData();
      return data;
    }

  @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      MyLog.d("Creating preferences activity");

      addPreferencesFromResource(R.xml.preferences);

      findPreference("filter_dialog").setOnPreferenceClickListener(this);
      findPreference("notifications_toast_apps_dialog").setOnPreferenceClickListener(this);
      findPreference("clear_log").setOnPreferenceClickListener(this);
      findPreference("sort_by").setOnPreferenceChangeListener(this);

      EditTextPreference logfile = (EditTextPreference) findPreference("logfile");
      logfile.setText(NetworkLog.settings.getLogFile());

      CheckBoxPreference foreground = (CheckBoxPreference) findPreference("start_foreground");
      foreground.setOnPreferenceClickListener(this);
      foreground.setChecked(NetworkLog.settings.getStartForeground());

      String entries[] = getResources().getStringArray(R.array.interval_entries);
      String values[] = getResources().getStringArray(R.array.interval_values);

      final Context context = this;
      OnPreferenceChangeListener changeListener = new OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, final Object newValue) {
          if(preference.getKey().equals("history_size")) {
            NetworkLog.appFragment.clear();
            NetworkLog.logFragment.clear();
            new Thread(new Runnable() {
              public void run() {
                NetworkLog.history.loadEntriesFromFile(context, (String)newValue);
              }
            }).start();
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

          if(preference.getKey().equals("notifications_toast")) {
            Boolean toast_enabled = (Boolean) newValue;
            ListPreference pref = (ListPreference) findPreference("notifications_toast_position");
            String value = pref.getValue();
            SeekBarPreference sbpref = (SeekBarPreference) findPreference("notifications_toast_yoffset");

            if(toast_enabled && (value.equals("1") || value.equals("2"))) {
              sbpref.setEnabled(true);
            } else {
              sbpref.setEnabled(false);
            }
          }

          if(preference.getKey().equals("notifications_toast_position")) {
            String value = (String) newValue;
            SeekBarPreference sbpref = (SeekBarPreference) findPreference("notifications_toast_yoffset");

            if(value.equals("1") || value.equals("2")) {
              sbpref.setEnabled(true);
            } else {
              sbpref.setEnabled(false);
            }
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

      CheckBoxPreference cbpref = (CheckBoxPreference) findPreference("notifications_toast");
      cbpref.setOnPreferenceChangeListener(changeListener);
      boolean toast_enabled = cbpref.isChecked();

      pref = (ListPreference) findPreference("notifications_toast_position");
      pref.setOnPreferenceChangeListener(changeListener);
      String value = pref.getValue();

      SeekBarPreference sbpref = (SeekBarPreference) findPreference("notifications_toast_yoffset");
      if(toast_enabled && (value.equals("1") || value.equals("2"))) {
        sbpref.setEnabled(true);
      } else {
        sbpref.setEnabled(false);
      }

      data = (InstanceData) getLastNonConfigurationInstance();

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

        if(data.selectToastApps_dialog_showing) {
          NetworkLog.selectToastApps = new SelectToastApps();
          NetworkLog.selectToastApps.showDialog(this, data.selectToastApps_appData);
        }

        MyLog.d("Restored preferences run");
      }
    }

  @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      if(preference.getKey().equals("sort_by")) {
        if(NetworkLog.menu == null) {
          return true;
        }
        String value = (String) newValue;
        com.actionbarsherlock.view.MenuItem item;

        if(value.equals("UID")) {
          item = NetworkLog.menu.findItem(R.id.sort_by_uid);
        } else if(value.equals("NAME")) {
          item = NetworkLog.menu.findItem(R.id.sort_by_name);
        } else if(value.equals("THROUGHPUT")) {
          item = NetworkLog.menu.findItem(R.id.sort_by_throughput);
        } else if(value.equals("PACKETS")) {
          item = NetworkLog.menu.findItem(R.id.sort_by_packets);
        } else if(value.equals("BYTES")) {
          item = NetworkLog.menu.findItem(R.id.sort_by_bytes);
        } else if(value.equals("TIMESTAMP")) {
          item = NetworkLog.menu.findItem(R.id.sort_by_timestamp);
        } else {
          return true;
        }
        item.setChecked(true);
        return true;
      }
      return true;
    }

  @Override
    public boolean onPreferenceClick(Preference preference) {
      MyLog.d("Preference [" + preference.getKey() + "] clicked");

      if(preference.getKey().equals("notifications_toast_apps_dialog")) 
      {
        NetworkLog.selectToastApps = new SelectToastApps();
        NetworkLog.selectToastApps.showDialog(this);
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
      builder.setTitle(getString(R.string.warning))
        .setMessage(getString(R.string.warning_disabling_notification))
        .setCancelable(true)
        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            preference.setChecked(true);
            NetworkLog.settings.setStartForeground(true);
            NetworkLog.toggleServiceForeground(true);
            warnStartForegroundDialog = null;
            dialog.dismiss();
          }
        })
      .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
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
      builder.setTitle(getString(R.string.coming_soon_title))
        .setMessage(getString(R.string.coming_soon_text))
        .setCancelable(true)
        .setNeutralButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            dialog.dismiss();
          }
        });
      AlertDialog alert = builder.create();
      alert.show();
    }
  }
}
