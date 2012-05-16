package com.googlecode.networklog;

import android.content.Context;
import android.preference.PreferenceActivity;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.ListPreference;
import android.os.Bundle;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class Preferences extends PreferenceActivity implements OnPreferenceClickListener {
  private PreferenceConfigurationData data = null;


  private class PreferenceConfigurationData {
    boolean history_dialog_showing;
    int history_dialog_max;
    int history_dialog_progress;

    PreferenceConfigurationData() {
      history_dialog_showing = NetworkLog.history.dialog_showing;
      history_dialog_max = NetworkLog.history.dialog_max;
      history_dialog_progress = NetworkLog.history.dialog_progress;
    }
  }

  @Override
    public void onDestroy() {
      super.onDestroy();
      MyLog.d("Destroying preferences activity");

      if(NetworkLog.history.dialog_showing) {
        NetworkLog.history.dialog.dismiss();
        NetworkLog.history.dialog = null;
      }
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

      String entries[] = getResources().getStringArray(R.array.interval_entries);
      String values[] = getResources().getStringArray(R.array.interval_values);

      final Context context = this;
      OnPreferenceChangeListener changeListener = new OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
          if(preference.getKey().equals("logfile_trim")) {

            return true;
          }

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
        MyLog.d("Restored preferences run");

        if(data.history_dialog_showing && NetworkLog.history.dialog == null) {
          NetworkLog.history.createProgressDialog(this);
        }
      }
    }

  @Override
    public boolean onPreferenceClick(Preference preference) {
      MyLog.d("Preference [" + preference.getKey() + "] clicked");

      if(preference.getKey().equals("notifications_statusbar")
          || preference.getKey().equals("notifications_toast")
          || preference.getKey().equals("notifications_statusbar_apps_dialog")
          || preference.getKey().equals("notifications_toast_apps_dialog")) {
        new ComingSoonDialog(this);
        return true;
          }

      if(preference.getKey().equals("filter_dialog")) {
        new FilterDialog(this);
        return true;
      }

      return false;
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
