package com.googlecode.iptableslog;

import android.app.TabActivity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.TabWidget;
import android.widget.TabHost;
import android.content.res.Resources;

public class IptablesLog extends TabActivity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

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

        IptablesLogTracker.start();
        ApplicationsTracker.getInstalledApps(this);
     }
}
