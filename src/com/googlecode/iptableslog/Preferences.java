package com.googlecode.iptableslog;

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
  @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      addPreferencesFromResource(R.xml.preferences);

      findPreference("filter_dialog").setOnPreferenceClickListener(this);
      findPreference("notifications_statusbar").setOnPreferenceClickListener(this);
      findPreference("notifications_statusbar_apps_dialog").setOnPreferenceClickListener(this);
      findPreference("notifications_toast").setOnPreferenceClickListener(this);
      findPreference("notifications_toast_apps_dialog").setOnPreferenceClickListener(this);


      CharSequence entries[] = {
        "1 ms",
        "100 ms",
        "500 ms",
        "1 second",
        "30 seconds",
        "1 minute",
        "5 minutes",
        "10 minutes",
        "15 minutes",
        "30 minutes",
        "1 hour",
        "2 hours",
        "4 hours",
        "8 hours",
        "16 hours",
        "24 hours",
        "48 hours"
      };

      CharSequence values[] = {
        "1",
        "100",
        "500",
        "1000",
        "30000",
        "60000",
        "300000",
        "600000",
        "900000",
        "1800000",
        "3600000",
        "7200000",
        "14400000",
        "28800000",
        "57600000",
        "115200000",
        "230400000",
      };

      ListPreference pref = (ListPreference) findPreference("interval_placeholder");

      pref.setEntries(entries);
      pref.setEntryValues(values);
      pref.setValue(String.valueOf(IptablesLog.settings.getGraphInterval()));

      pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
          MyLog.d("[interval] pref: " + preference + "; new value: " + newValue);
          IptablesLog.settings.setGraphInterval(Long.parseLong((String) newValue));
          return true;
        }
      });

      pref = (ListPreference) findPreference("viewsize_placeholder");

      pref.setEntries(entries);
      pref.setEntryValues(values);
      pref.setValue(String.valueOf(IptablesLog.settings.getGraphViewsize()));

      pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
          MyLog.d("[viewsize] pref: " + preference + "; new value: " + newValue);
          IptablesLog.settings.setGraphViewsize(Long.parseLong((String) newValue));
          return true;
        }
      });
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
