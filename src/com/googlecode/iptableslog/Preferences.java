package com.googlecode.iptableslog;

import android.content.Context;
import android.preference.PreferenceActivity;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
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
      findPreference("graphs_dialog").setOnPreferenceClickListener(this);
    }

  @Override
    public boolean onPreferenceClick(Preference preference) {
      MyLog.d("Preference [" + preference.getKey() + "] clicked");

      if(preference.getKey().equals("notifications_statusbar") 
          || preference.getKey().equals("notifications_toast")
          || preference.getKey().equals("notifications_statusbar_apps_dialog") 
          || preference.getKey().equals("notifications_toast_apps_dialog")
          || preference.getKey().equals("graphs_dialog")) {
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
