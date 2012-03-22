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

  public boolean getResolveAddresses() {
    return prefs.getBoolean("resolve_hosts", false);
  }

  public boolean getResolvePorts() {
    return prefs.getBoolean("resolve_ports", false);
  }

  public String getFilterText() {
    return prefs.getString("filter_text", "");
  }

  public int getFilterOptions() {
    return prefs.getInt("filter_options", 0);
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

  public void setResolveAddresses(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("resolve_hosts", value);
    editor.commit();
  }

  public void setResolvePorts(boolean value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean("resolve_ports", value);
    editor.commit();
  }

  public void setFilterText(String value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString("filter_text", value);
    editor.commit();
  }

  public void setFilterOptions(int value) {
    SharedPreferences.Editor editor = prefs.edit();
    editor.putInt("filter_options", value);
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

  @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
      MyLog.d("Prefs changed: [" + key + "]");

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
