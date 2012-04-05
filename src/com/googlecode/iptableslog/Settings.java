package com.googlecode.iptableslog;

import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.Context;

public class Settings implements OnSharedPreferenceChangeListener {
  private SharedPreferences prefs;

  private Settings() {}

  public Settings(Context context) {
    PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
    prefs = PreferenceManager.getDefaultSharedPreferences(context);
    prefs.registerOnSharedPreferenceChangeListener(this);
  }

  public String getLogFile() {
    return prefs.getString("logfile", "/sdcard/iptableslog.txt");
  }

  public String getLogFileMaxSize() {
    return prefs.getString("logfile_maxsize", "12000000");
  }

  public boolean getStartServiceAtBoot() {
    return prefs.getBoolean("startServiceAtBoot", false);
  }

  public boolean getStartServiceAtStart() {
    return prefs.getBoolean("startServiceAtStart", true);
  }

  public boolean getStopServiceAtExit() {
    return prefs.getBoolean("stopServiceAtExit", false);
  }

  public boolean getResolveHosts() {
    return prefs.getBoolean("resolve_hosts", false);
  }

  public boolean getResolvePorts() {
    return prefs.getBoolean("resolve_ports", false);
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

  public long getMaxLogEntries() {
    return Long.parseLong(prefs.getString("max_log_entries", "15000"));
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

  public long getGraphInterval() {
    return prefs.getLong("interval", 1000);
  }

  public long getGraphViewsize() {
    return prefs.getLong("viewsize", 1000 * 60 * 15);
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

  public void setLogFile(String value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString("logfile", value);
    editor.commit();
  }

  public void setLogFileMaxSize(String value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString("logfile_maxsize", value);
    editor.commit();
  }

  @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
      MyLog.d("Shared prefs changed: [" + key + "]");

      if(key.equals("logfile")) {
        String value = prefs.getString(key, "/sdcard/iptableslog.txt");
        MyLog.d("New " + key + " value [" + value + "]");

        // update service

        return;
      }

      if(key.equals("logfile_maxsize")) {
        String value = prefs.getString(key, "12000000");
        MyLog.d("New " + key + " value [" + value + "]");

        // update service

        return;
      }

      if(key.equals("startServiceAtBoot")) {
        boolean value = prefs.getBoolean(key, false);
        MyLog.d("New " + key + " value [" + value + "]");
        if(value == true) {
          // add boot listeners
        } else {
          // remove boot listeners
        }
        return;
      }

      if(key.equals("startServiceAtStart")) {
        boolean value = prefs.getBoolean(key, true);
        MyLog.d("New " + key + " value [" + value + "]");
        IptablesLog.startServiceAtStart = value;
        return;
      }

      if(key.equals("stopServiceAtExit")) {
        boolean value = prefs.getBoolean(key, false);
        MyLog.d("New " + key + " value [" + value + "]");
        IptablesLog.stopServiceAtExit = value;
        return;
      }

      if(key.equals("resolve_hosts")) {
        boolean value = prefs.getBoolean(key, false);
        MyLog.d("New " + key + " value [" + value + "]");
        IptablesLog.resolveHosts = value;
        IptablesLog.logView.refreshHosts();
        IptablesLog.appView.refreshHosts();
        return;
      }

      if(key.equals("resolve_ports")) {
        boolean value = prefs.getBoolean(key, false);
        MyLog.d("New " + key + " value [" + value + "]");
        IptablesLog.resolvePorts = value;
        IptablesLog.logView.refreshPorts();
        IptablesLog.appView.refreshPorts();
        return;
      }

      if(key.equals("max_log_entries")) {
        String value = prefs.getString(key, "15000");
        MyLog.d("New " + key + " value [" + value + "]");
        IptablesLog.logView.maxLogEntries = Long.parseLong(value);
        IptablesLog.logView.pruneLogEntries();
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
        IptablesLog.appView.preSortBy = Sort.forValue(value);
        IptablesLog.appView.preSortData();
        IptablesLog.appView.sortData();
        return;
      }

      if(key.equals("sort_by")) {
        String value = prefs.getString(key, "BYTES");
        MyLog.d("New " + key + " value [" + value + "]");
        IptablesLog.appView.sortBy = Sort.forValue(value);
        IptablesLog.appView.preSortData();
        IptablesLog.appView.sortData();
        return;
      }
    }
}
