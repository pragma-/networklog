package com.googlecode.networklog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;

public class BootCompletedReceiver extends BroadcastReceiver {
  @Override
    public void onReceive(Context context, Intent intent) {
      MyLog.d("Received broadcast: " + intent.getAction());

      if ("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE".equals(intent.getAction())) {
        SharedPreferences prefs = context.getSharedPreferences("com.googlecode.networklog_preferences", Context.MODE_PRIVATE);

        if(prefs.getBoolean("startServiceAtBoot", false) == true) {
          MyLog.d("Starting service at boot");
          Intent i = new Intent(context, NetworkLogService.class);

          i.putExtra("logfile", prefs.getString("logfile", "/sdcard/networklog.txt"));
          i.putExtra("logfile_maxsize", prefs.getString("logfile_maxsize", "12000000"));

          context.startService(i);
        } else {
          MyLog.d("Not starting service at boot");
        }
      }
    }
}
