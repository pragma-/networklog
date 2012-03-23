package com.googlecode.iptableslog;

import android.content.Context;
import android.preference.PreferenceActivity;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.os.Bundle;

public class Preferences extends PreferenceActivity implements OnPreferenceClickListener {
  @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      addPreferencesFromResource(R.xml.preferences);

      Preference filter = findPreference("filter_dialog");
      filter.setOnPreferenceClickListener(this);
    }

  @Override
    public boolean onPreferenceClick(Preference preference) {
      MyLog.d("Preference [" + preference.getKey() + "] clicked");

      if(preference.getKey().equals("filter_dialog")) {
        new FilterDialog(this);
        return true;
      }

      return false;
    }
}
