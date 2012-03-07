package com.googlecode.iptableslog;

import android.app.TabActivity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.TabWidget;
import android.widget.TabHost;
import android.content.res.Resources;
import android.util.Log;

public class IptablesLog extends TabActivity
{
  public static IptablesLogData data = null;

  /** Called when the activity is first created. */
  @Override
    public void onCreate(Bundle savedInstanceState)
    {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);

      data = (IptablesLogData) getLastNonConfigurationInstance(); 
      
      if (data == null) {
        MyLog.d("Fresh run");
        ApplicationsTracker.getInstalledApps(this);
      } else {
        MyLog.d("Restored run");
        ApplicationsTracker.restoreData(data);
      }

      Resources res = getResources();
      TabHost tabHost = getTabHost();
      TabHost.TabSpec spec;
      Intent intent;

      intent = new Intent().setClass(this, LogView.class);
      spec = tabHost.newTabSpec("log").setIndicator("Log", 
          res.getDrawable(R.drawable.tab_logview)).setContent(intent);
      tabHost.addTab(spec);

      intent = new Intent().setClass(this, AppView.class);
      spec = tabHost.newTabSpec("apps").setIndicator("Apps", 
          res.getDrawable(R.drawable.tab_appview)).setContent(intent);
      tabHost.addTab(spec);

      tabHost.setCurrentTab(0);
      tabHost.setCurrentTab(1);
      tabHost.setCurrentTab(0);

      if(data != null) {
        MyLog.d("Restoring data");
        MyLog.d("apptracker data: " + data.applicationsTrackerInstalledApps.size());
        MyLog.d("appview data: " + data.appViewListData.size());
        LogView.restoreData(data);
        AppView.restoreData(data);
        IptablesLogTracker.restoreData(data);

        AppView.updateAdapter();
      }

      
      IptablesLogTracker.start(data != null);

      // all data should be restored at this point, release the object
      data = null;
      MyLog.d("data object released");
    }

  @Override
    public void onDestroy()
    {
      super.onDestroy();
      MyLog.d("onDestroy called");
      if(data == null) {
        MyLog.d("Shutting down rules and logger");
        Iptables.removeRules();
        IptablesLogTracker.stop();
      } else {
        MyLog.d("Found data, not shutting down rules and logger");
      }
    }

  @Override
    public Object onRetainNonConfigurationInstance() {
      MyLog.d("Saving run");
      data = new IptablesLogData();
      return data;
    }
}
