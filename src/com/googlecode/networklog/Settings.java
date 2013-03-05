/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import android.os.Environment;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.Context;
import android.util.Log;

import java.io.File;

public class Settings implements OnSharedPreferenceChangeListener {
  private SharedPreferences prefs;

  // Force use of context constructor
  private Settings() {}

  public Settings(Context context) {
    PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
    prefs = PreferenceManager.getDefaultSharedPreferences(context);
    prefs.registerOnSharedPreferenceChangeListener(this);

    MyLog.enabled = getLogcatDebug();
  }

  public String getHistorySize() {
    return prefs.getString("history_size", "14400000");
  }

  public String getClearLogTimerange() {
    return prefs.getString("clearlog_timerange", "0");
  }

  public String getLogFile() {
    String logfile = prefs.getString("logfile", null);

    if(logfile == null) {
      logfile = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "networklog.txt";
      Log.d("NetworkLog", "Set default logfile path: " + logfile);
    }

    return logfile;
  }

  public boolean getStartForeground() {
    return prefs.getBoolean("start_foreground", true);
  }

  public boolean getStartServiceAtBoot() {
    return prefs.getBoolean("startServiceAtBoot", false);
  }

  public boolean getStartServiceAtStart() {
    return prefs.getBoolean("startServiceAtStart", false);
  }

  public boolean getStopServiceAtExit() {
    return prefs.getBoolean("stopServiceAtExit", false);
  }

  public boolean getResolveHosts() {
    return prefs.getBoolean("resolve_hosts", false);
  }

  public boolean getResolvePorts() {
    return prefs.getBoolean("resolve_ports", true);
  }

  public boolean getResolveCopies() {
    return !prefs.getBoolean("copy_original_addrs", false);
  }

  public boolean getConfirmExit() {
    return prefs.getBoolean("confirm_exit", true);
  }

  public String getFilterTextInclude() {
    return prefs.getString("filter_text_include", "");
  }

  public boolean getFilterUidInclude() {
    return prefs.getBoolean("filter_by_uid_include", false);
  }

  public boolean getFilterNameInclude() {
    return prefs.getBoolean("filter_by_name_include", false);
  }

  public boolean getFilterAddressInclude() {
    return prefs.getBoolean("filter_by_address_include", false);
  }

  public boolean getFilterPortInclude() {
    return prefs.getBoolean("filter_by_port_include", false);
  }

  public boolean getFilterInterfaceInclude() {
    return prefs.getBoolean("filter_by_interface_include", false);
  }

  public boolean getFilterProtocolInclude() {
    return prefs.getBoolean("filter_by_protocol_include", false);
  }

  public String getFilterTextExclude() {
    return prefs.getString("filter_text_exclude", "");
  }

  public boolean getFilterUidExclude() {
    return prefs.getBoolean("filter_by_uid_exclude", false);
  }

  public boolean getFilterNameExclude() {
    return prefs.getBoolean("filter_by_name_exclude", false);
  }

  public boolean getFilterAddressExclude() {
    return prefs.getBoolean("filter_by_address_exclude", false);
  }

  public boolean getFilterPortExclude() {
    return prefs.getBoolean("filter_by_port_exclude", false);
  }

  public boolean getFilterInterfaceExclude() {
    return prefs.getBoolean("filter_by_interface_exclude", false);
  }

  public boolean getFilterProtocolExclude() {
    return prefs.getBoolean("filter_by_protocol_exclude", false);
  }

  public long getMaxLogEntries() {
    return Long.parseLong(prefs.getString("max_log_entries", "150000"));
  }

  public Sort getPreSortBy() {
    return Sort.forValue(prefs.getString("presort_by", "BYTES"));
  }

  public Sort getSortBy() {
    return Sort.forValue(prefs.getString("sort_by", "BYTES"));
  }

  public boolean getLogcatDebug() {
    return prefs.getBoolean("logcat_debug", false);
  }

  public boolean getStatusbarNotifications() {
    return prefs.getBoolean("notifications_statusbar", false);
  }

  public boolean getToastNotifications() {
    return prefs.getBoolean("notifications_toast", false);
  }

  public int getToastNotificationsDuration() {
    return Integer.parseInt(prefs.getString("notifications_toast_duration", "3500"));
  }

  public long getGraphInterval() {
    return prefs.getLong("interval", 300000);
  }

  public long getGraphViewsize() {
    return prefs.getLong("viewsize", 1000 * 60 * 60 * 4);
  }

  public void setResolveHosts(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("resolve_hosts", value);
    editor.commit();
  }

  public void setResolvePorts(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("resolve_ports", value);
    editor.commit();
  }

  public void setResolveCopies(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("copy_original_addrs", value);
    editor.commit();
  }

  public void setConfirmExit(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("confirm_exit", value);
    editor.commit();
  }

  public void setFilterTextInclude(String value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString("filter_text_include", value);
    editor.commit();
  }

  public void setFilterUidInclude(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("filter_by_uid_include", value);
    editor.commit();
  }

  public void setFilterNameInclude(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("filter_by_name_include", value);
    editor.commit();
  }

  public void setFilterAddressInclude(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("filter_by_address_include", value);
    editor.commit();
  }

  public void setFilterPortInclude(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("filter_by_port_include", value);
    editor.commit();
  }

  public void setFilterInterfaceInclude(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("filter_by_interface_include", value);
    editor.commit();
  }

  public void setFilterProtocolInclude(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("filter_by_protocol_include", value);
    editor.commit();
  }

  public void setFilterTextExclude(String value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString("filter_text_exclude", value);
    editor.commit();
  }

  public void setFilterUidExclude(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("filter_by_uid_exclude", value);
    editor.commit();
  }

  public void setFilterNameExclude(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("filter_by_name_exclude", value);
    editor.commit();
  }

  public void setFilterAddressExclude(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("filter_by_address_exclude", value);
    editor.commit();
  }

  public void setFilterPortExclude(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("filter_by_port_exclude", value);
    editor.commit();
  }

  public void setFilterInterfaceExclude(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("filter_by_interface_exclude", value);
    editor.commit();
  }

  public void setFilterProtocolExclude(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("filter_by_protocol_exclude", value);
    editor.commit();
  }

  public void setMaxLogEntries(long value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString("max_log_entries", String.valueOf(value));
    editor.commit();
  }

  public void setPreSortBy(Sort value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString("presort_by", value.toString());
    editor.commit();
  }

  public void setSortBy(Sort value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString("sort_by", value.toString());
    editor.commit();
  }

  public void setLogcatDebug(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("logcat_debug", value);
    editor.commit();
  }

  public void setStatusbarNotifications(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("notifications_statusbar", value);
    editor.commit();
  }

  public void setToastNotifications(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("notifications_toast", value);
    editor.commit();
  }

  public void setToastNotificationsDuration(int value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString("notifications_toast_duration", String.valueOf(value));
    editor.commit();
  }

  public void setGraphInterval(long value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putLong("interval", value);
    editor.commit();
  }

  public void setGraphViewsize(long value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putLong("viewsize", value);
    editor.commit();
  }

  public void setStartForeground(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("start_foreground", value);
    editor.commit();
  }

  public void setStartServiceAtBoot(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("startServiceAtBoot", value);
    editor.commit();
  }

  public void setStartServiceAtStart(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("startServiceAtStart", value);
    editor.commit();
  }

  public void setStopServiceAtExit(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("stopServiceAtExit", value);
    editor.commit();
  }

  public void setHistorySize(String value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString("history_size", value);
    editor.commit();
   }

  public void setClearLogTimerange(String value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString("clearlog_timerange", value);
    editor.commit();
   }

  public void setLogFile(String value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString("logfile", value);
    editor.commit();
  }

  @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
      MyLog.d("Shared prefs changed: [" + key + "]");

      if(key.equals("logfile")) {
        String value = prefs.getString(key, null);
        MyLog.d("New " + key + " value [" + value + "]");
        // update service
        return;
      }

      if(key.equals("startServiceAtBoot")) {
        boolean value = prefs.getBoolean(key, false);
        MyLog.d("New " + key + " value [" + value + "]");
      }

      if(key.equals("startServiceAtStart")) {
        boolean value = prefs.getBoolean(key, false);
        MyLog.d("New " + key + " value [" + value + "]");
        NetworkLog.startServiceAtStart = value;
        return;
      }

      if(key.equals("stopServiceAtExit")) {
        boolean value = prefs.getBoolean(key, false);
        MyLog.d("New " + key + " value [" + value + "]");
        NetworkLog.stopServiceAtExit = value;
        return;
      }

      if(key.equals("resolve_hosts")) {
        boolean value = prefs.getBoolean(key, false);
        MyLog.d("New " + key + " value [" + value + "]");
        NetworkLog.resolveHosts = value;
        NetworkLog.logFragment.refreshAdapter();
        NetworkLog.appFragment.refreshAdapter();
        return;
      }

      if(key.equals("resolve_ports")) {
        boolean value = prefs.getBoolean(key, true);
        MyLog.d("New " + key + " value [" + value + "]");
        NetworkLog.resolvePorts = value;
        NetworkLog.logFragment.refreshAdapter();
        NetworkLog.appFragment.refreshAdapter();
        return;
      }

      if(key.equals("copy_original_addrs")) {
        boolean value = prefs.getBoolean(key, false);
        MyLog.d("New " + key + " value [" + value + "]");
        NetworkLog.resolveCopies = !value;
        return;
      }

      if(key.equals("max_log_entries")) {
        String value = prefs.getString(key, "150000");
        MyLog.d("New " + key + " value [" + value + "]");
        NetworkLog.logFragment.maxLogEntries = Long.parseLong(value);
        NetworkLog.logFragment.pruneLogEntries();
        return;
      }

      if(key.equals("logcat_debug")) {
        boolean value = prefs.getBoolean(key, false);
        MyLog.d("New " + key + " value [" + value + "]");
        MyLog.enabled = value;
        return;
      }

      if(key.equals("presort_by")) {
        String value = prefs.getString(key, "BYTES");
        MyLog.d("New " + key + " value [" + value + "]");
        NetworkLog.appFragment.preSortBy = Sort.forValue(value);
        NetworkLog.appFragment.preSortData();
        NetworkLog.appFragment.sortData();
        NetworkLog.appFragment.refreshAdapter();
        return;
      }

      if(key.equals("sort_by")) {
        String value = prefs.getString(key, "BYTES");
        MyLog.d("New " + key + " value [" + value + "]");
        NetworkLog.appFragment.sortBy = Sort.forValue(value);
        NetworkLog.appFragment.preSortData();
        NetworkLog.appFragment.sortData();
        NetworkLog.appFragment.sortChildren();
        NetworkLog.appFragment.refreshAdapter();
        return;
      }

      if(key.equals("notifications_toast")) {
        boolean value = prefs.getBoolean(key, false);
        MyLog.d("New " + key + " value [" + value + "]");
        NetworkLogService.toastEnabled = value;
        return;
      }

      if(key.equals("notifications_toast_duration")) {
        int value = Integer.parseInt(prefs.getString(key, "3500"));
        MyLog.d("New " + key + " value [" + value + "]");
        NetworkLogService.toastDuration = value;
        return;
      }
    }
}
