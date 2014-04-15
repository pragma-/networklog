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
  private Context context;

  // Force use of context constructor
  private Settings() {}

  public Settings(Context context) {
    PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
    prefs = PreferenceManager.getDefaultSharedPreferences(context);
    prefs.registerOnSharedPreferenceChangeListener(this);

    MyLog.enabled = getLogcatDebug();
    this.context = context;
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

  public boolean getInvertUploadDownload() {
    return prefs.getBoolean("invert_upload_download", false);
  }

  public boolean getWatchRules() {
    return prefs.getBoolean("watch_rules", false);
  }

  public int getWatchRulesTimeout() {
    return Integer.parseInt(prefs.getString("watch_rules_timeout", "120000"));
  }

  public boolean getBehindFirewall() {
    return prefs.getBoolean("behind_firewall", false);
  }

  public boolean getThroughputBps() {
    return prefs.getBoolean("throughput_bps", true);
  }

  public boolean getRoundValues() {
    return prefs.getBoolean("round_values", true);
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

  public int getUpdateMaxLogEntries() {
    return prefs.getInt("update_max_log_entries", 0);
  }

  public long getMaxLogEntries() {
    int updateMaxLogEntries = getUpdateMaxLogEntries();

    if(updateMaxLogEntries == 0) {
      setUpdateMaxLogEntries(1);

      if(getMaxLogEntries() > 75000) {
        setMaxLogEntries(75000);
      }
    }

    return Long.parseLong(prefs.getString("max_log_entries", "75000"));
  }

  public Sort getPreSortBy() {
    return Sort.forValue(prefs.getString("presort_by", "NAME"));
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

  public int getToastNotificationsPosition() {
    return Integer.parseInt(prefs.getString("notifications_toast_position", "-1"));
  }

  public int getToastNotificationsYOffset() {
    return prefs.getInt("notifications_toast_yoffset", 0);
  }

  public int getToastNotificationsOpacity() {
    return prefs.getInt("notifications_toast_opacity", 119);
  }

  public boolean getToastNotificationsShowAddress() {
    return prefs.getBoolean("notifications_toast_show_address", true);
  }

  public long getGraphInterval() {
    return prefs.getLong("interval", 300000);
  }

  public long getGraphViewsize() {
    return prefs.getLong("viewsize", 1000 * 60 * 60 * 4);
  }

  public int getLogMethod() {
    return Integer.parseInt(prefs.getString("log_method", "0"));
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

  public void setUpdateMaxLogEntries(int value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putInt("update_max_log_entries", value);
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

  public void setToastNotificationsPosition(int value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString("notifications_toast_position", String.valueOf(value));
    editor.commit();
  }

  public void setToastNotificationsYOffset(int value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putInt("notifications_toast_yoffset", value);
    editor.commit();
  }

  public void setToastNotificationsOpacity(int value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putInt("notifications_toast_opacity", value);
    editor.commit();
  }

  public void setToastNotificationsShowAddress(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("notifications_toast_show_address", value);
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

  public void setInvertUploadDownload(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("invert_upload_download", value);
    editor.commit();
  }

  public void setWatchRules(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("watch_rules", value);
    editor.commit();
  }

  public void setWatchRulesTimeout(int value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString("watch_rules_timeout", String.valueOf(value));
    editor.commit();
  }

  public void setBehindFirewall(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("behind_firewall", value);
    editor.commit();
  }

  public void setThroughputBps(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("throughput_bps", value);
    editor.commit();
  }

  public void setRoundValues(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("round_values", value);
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

  public void setLogMethod(int value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString("log_method", String.valueOf(value));
    editor.commit();
  }

  public void setLogFile(String value) {
    String oldValue = prefs.getString("logfile", null);

    if(oldValue != null && !oldValue.equals(value)) {
      // update only if values have changed (mainly to ensure service 
      // doesn't restart for same values)

      SharedPreferences.Editor editor = prefs.edit();
      editor.putString("logfile", value);
      editor.commit();

      if(NetworkLogService.instance != null) {
        // if service is running, restart service so that logfile is opened
        // with new path
        NetworkLog.instance.stopService();
        NetworkLog.instance.startService();
      }
    }
  }

  public void showSampleToast() {
    if(NetworkLog.context != null && NetworkLog.handler != null) {
      String msg = String.format(NetworkLog.context.getResources().getString(R.string.sample_toast_message),
          NetworkLogService.toastYOffset, NetworkLogService.toastOpacity);
      NetworkLogService.showToast(NetworkLog.context, NetworkLog.handler, msg, true);
    }
  }

  @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
      MyLog.d("Shared prefs changed: [" + key + "]");

      if(key.equals("log_method")) {
        int value = Integer.parseInt(prefs.getString(key, "0"));
        MyLog.d("New " + key + " value [" + value + "]");
        if(NetworkLogService.instance != null) {
          NetworkLog.instance.stopService();
          NetworkLog.instance.startService();
        }
      }

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
        String value = prefs.getString(key, "75000");
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
        NetworkLog.appFragment.setPreSortMethod();
        NetworkLog.appFragment.preSortData();
        NetworkLog.appFragment.sortData();
        NetworkLog.appFragment.refreshAdapter();
        return;
      }

      if(key.equals("sort_by")) {
        String value = prefs.getString(key, "BYTES");
        MyLog.d("New " + key + " value [" + value + "]");
        NetworkLog.appFragment.sortBy = Sort.forValue(value);
        NetworkLog.appFragment.setSortMethod();
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
        showSampleToast();
        return;
      }

      if(key.equals("notifications_toast_position")) {
        int value = Integer.parseInt(prefs.getString(key, "-1"));
        MyLog.d("New " + key + " value [" + value + "]");
        NetworkLogService.toastPosition = value;
        showSampleToast();
        return;
      }

      if(key.equals("notifications_toast_yoffset")) {
        int value = prefs.getInt(key, 0);
        MyLog.d("New " + key + " value [" + value + "]");
        NetworkLogService.toastYOffset = value;
        showSampleToast();
        return;
      }

      if(key.equals("notifications_toast_opacity")) {
        int value = prefs.getInt(key, 119);
        MyLog.d("New " + key + " value [" + value + "]");
        NetworkLogService.toastOpacity = value;
        showSampleToast();
        return;
      }

      if(key.equals("notifications_toast_show_address")) {
        boolean value = prefs.getBoolean(key, false);
        MyLog.d("New " + key + " value [" + value + "]");
        NetworkLogService.toastShowAddress = value;
        return;
      }

      if(key.equals("round_values")) {
        boolean value = prefs.getBoolean(key, true);
        MyLog.d("New " + key + " value [" + value + "]");
        NetworkLog.appFragment.roundValues = value;
        NetworkLog.appFragment.refreshAdapter();
        return;
      }

      if(key.equals("invert_upload_download")) {
        boolean value = prefs.getBoolean(key, false);
        MyLog.d("New " + key + " value [" + value + "]");
        NetworkLogService.invertUploadDownload = value;
        return;
      }

      if(key.equals("watch_rules")) {
        boolean value = prefs.getBoolean(key, false);
        MyLog.d("New " + key + " value [" + value + "]");
        NetworkLogService.watchRules = value;
        if(NetworkLogService.instance != null) {
          NetworkLogService.instance.startWatchingRules();
        }
        return;
      }

      if(key.equals("watch_rules_timeout")) {
        int value = Integer.parseInt(prefs.getString(key, "120000"));
        MyLog.d("New " + key + " value [" + value + "]");
        NetworkLogService.watchRulesTimeout = value;
        if(NetworkLogService.rulesWatcher != null) {
          NetworkLogService.rulesWatcher.interrupt();
        }
        return;
      }

      if(key.equals("behind_firewall")) {
        boolean value = prefs.getBoolean(key, false);
        MyLog.d("New " + key + " value [" + value + "]");
        NetworkLogService.behindFirewall = value;
        if(NetworkLogService.instance != null) {
          Iptables.removeRules(context);
          Iptables.addRules(context);
        }
        return;
      }

      if(key.equals("throughput_bps")) {
        boolean value = prefs.getBoolean(key, true);
        MyLog.d("New " + key + " value [" + value + "]");
        NetworkLogService.throughputBps = value;
        ThroughputTracker.updateThroughputBps();
        return;
      }
    }
}
