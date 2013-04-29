/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import android.os.Bundle;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.res.Resources;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.IBinder;
import android.content.Context;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.CheckBox;
import android.view.View;
import android.view.LayoutInflater;
import android.util.Log;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitleProvider;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.app.ActionBar;

import java.util.Enumeration;
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.io.File;

public class NetworkLog extends SherlockFragmentActivity {
  public static final String SCRIPT = "networklog.sh";

  public static RetainInstanceData data = null;

  public static ViewPager viewPager;
  public final static int PAGE_LOG = 0;
  public final static int PAGE_APP = 1;
  public final static int PAGES    = 2;

  public static LogFragment logFragment;
  public static AppFragment appFragment;

  public static ToggleButton loggingButton;
  public static TextView statusText;
  public static Settings settings;
  public static Handler handler;

  public static String filterTextInclude;
  public static ArrayList<String> filterTextIncludeList = new ArrayList<String>();
  public static boolean filterUidInclude;
  public static boolean filterNameInclude;
  public static boolean filterAddressInclude;
  public static boolean filterPortInclude;
  public static boolean filterInterfaceInclude;
  public static boolean filterProtocolInclude;

  public static String filterTextExclude;
  public static ArrayList<String> filterTextExcludeList = new ArrayList<String>();
  public static boolean filterUidExclude;
  public static boolean filterNameExclude;
  public static boolean filterAddressExclude;
  public static boolean filterPortExclude;
  public static boolean filterInterfaceExclude;
  public static boolean filterProtocolExclude;

  public static NetworkResolver resolver;
  public static boolean resolveHosts;
  public static boolean resolvePorts;
  public static boolean resolveCopies;

  public static boolean startServiceAtStart;
  public static boolean stopServiceAtExit;

  public static HistoryLoader history;
  public static FeedbackDialog feedbackDialog;
  public static ClearLog clearLog;
  public static SelectToastApps selectToastApps;

  public static StatusUpdater statusUpdater;

  public static ArrayList<String> localIpAddrs;

  public static Messenger service = null;
  public static Messenger messenger = null;
  public static boolean isBound = false;

  public static Context context;
  public static Menu menu;

  public static ServiceConnection connection = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder serv) {
      service = new Messenger(serv);
      isBound = true;

      MyLog.d("Attached to service; setting isBound true");

      // Register with service
      try {
        Message msg = Message.obtain(null, NetworkLogService.MSG_REGISTER_CLIENT);
        msg.replyTo = messenger;
        service.send(msg);
      } catch(RemoteException e) {
        /* do nothing */
        Log.d("NetworkLog", "RemoteException registering with service", e);
      }
    }

    public void onServiceDisconnected(ComponentName className) {
      MyLog.d("Detached from service; setting isBound false");
      service = null;
      isBound = false;
    }
  };

  private LogEntry entry;

  class IncomingHandler extends Handler {
    @Override
      public void handleMessage(Message msg) {
        MyLog.d("[client] Received message: " + msg);

        switch(msg.what) {
          case NetworkLogService.MSG_BROADCAST_LOG_ENTRY:
            entry = (LogEntry) msg.obj;
            if(MyLog.enabled) {
              MyLog.d("Received entry: " + entry);
            }
            logFragment.onNewLogEntry(entry);
            appFragment.onNewLogEntry(entry);
            break;

          default:
            super.handleMessage(msg);
        }
      }
  }

  void doBindService() {
    MyLog.d("doBindService");
    if(isBound) {
      MyLog.d("Already bound to service; unbinding...");
      doUnbindService();
    }

    messenger = new Messenger(new IncomingHandler());
    MyLog.d("Created messenger: " + messenger);

    MyLog.d("Binding connection to service: " + connection);
    bindService(new Intent(this, NetworkLogService.class), connection, 0);
    MyLog.d("doBindService done");
  }

  void doUnbindService() {
    MyLog.d("doUnbindService");
    if(isBound) {
      if(service != null) {
        try {
          MyLog.d("Unregistering from service; service: " + service + "; messenger: " + messenger);
          Message msg = Message.obtain(null, NetworkLogService.MSG_UNREGISTER_CLIENT);
          msg.replyTo = messenger;
          service.send(msg);
        } catch(RemoteException e) {
          /* do nothing */
          Log.d("NetworkLog", "RemoteException unregistering with service", e);
        }

        try {
          MyLog.d("Unbinding connection from service:" + connection);
          unbindService(connection);
        } catch(Exception e) {
          Log.d("NetworkLog", "Ignored unbind exception:", e);
        } finally {
          MyLog.d("Setting isBound false");
          isBound = false;
        }

        MyLog.d("doUnbindService done");
      }
    }
  }

  public static State state;
  public enum State { LOAD_APPS, RUNNING, EXITING  };

  public static InitRunner initRunner;
  public class InitRunner implements Runnable
  {
    private Context context;
    public boolean running = false;

    public InitRunner(Context context) {
      this.context = context;
    }

    public void stop() {
      running = false;
    }

    public void run() {
      MyLog.d("Init begin");
      running = true;

      Looper.myLooper().prepare();

      state = NetworkLog.State.LOAD_APPS;
      ApplicationsTracker.getInstalledApps(context, handler);

      if(running == false) {
        return;
      }

      history.loadEntriesFromFile(context, settings.getHistorySize());

      if(startServiceAtStart && !isServiceRunning(context, NetworkLogService.class.getName())) {
        handler.post(new Runnable() {
          public void run() {
            startService();
          }
        });
      }

      state = NetworkLog.State.RUNNING;
      MyLog.d("Init done");
    }
  }

  public void loadSettings() {
    if(settings == null) {
      settings = new Settings(this);
    }

    filterTextInclude = settings.getFilterTextInclude();
    FilterUtils.buildList(filterTextInclude, filterTextIncludeList);
    filterUidInclude = settings.getFilterUidInclude();
    filterNameInclude = settings.getFilterNameInclude();
    filterAddressInclude = settings.getFilterAddressInclude();
    filterPortInclude = settings.getFilterPortInclude();
    filterInterfaceInclude = settings.getFilterInterfaceInclude();
    filterProtocolInclude = settings.getFilterProtocolInclude();

    filterTextExclude = settings.getFilterTextExclude();
    FilterUtils.buildList(filterTextExclude, filterTextExcludeList);
    filterUidExclude = settings.getFilterUidExclude();
    filterNameExclude = settings.getFilterNameExclude();
    filterAddressExclude = settings.getFilterAddressExclude();
    filterPortExclude = settings.getFilterPortExclude();
    filterInterfaceExclude = settings.getFilterInterfaceExclude();
    filterProtocolExclude = settings.getFilterProtocolExclude();

    startServiceAtStart = settings.getStartServiceAtStart();
    stopServiceAtExit = settings.getStopServiceAtExit();

    resolveHosts = settings.getResolveHosts();
    resolvePorts = settings.getResolvePorts();
    resolveCopies = settings.getResolveCopies();

    startServiceAtStart = settings.getStartServiceAtStart();
    stopServiceAtExit = settings.getStopServiceAtExit();

    NetworkLogService.throughputBps = settings.getThroughputBps();
    NetworkLogService.toastEnabled = settings.getToastNotifications();
    NetworkLogService.toastDuration = settings.getToastNotificationsDuration();
    NetworkLogService.toastPosition = settings.getToastNotificationsPosition();
    NetworkLogService.toastYOffset = settings.getToastNotificationsYOffset();
    NetworkLogService.toastOpacity = settings.getToastNotificationsOpacity();
    NetworkLogService.toastShowAddress = settings.getToastNotificationsShowAddress();
    NetworkLogService.toastBlockedApps = SelectToastApps.loadBlockedApps(this);
  }

  private static class MyFragmentPagerAdapter extends FragmentPagerAdapter implements TitleProvider {
    Context context;

    public MyFragmentPagerAdapter(Context context, FragmentManager fm) {
      super(fm);
      this.context = context;
    }

    @Override
      public String getTitle(int index) {
        switch(index) {
          case PAGE_LOG:
            return context.getResources().getString(R.string.tab_log);
          case PAGE_APP:
            return context.getResources().getString(R.string.tab_apps);
        }
        return "Unnamed";
      }

    @Override
      public Fragment getItem(int index) {
        Fragment fragment = null;

        switch(index) {
          case PAGE_LOG:
            fragment = logFragment;
            break;
          case PAGE_APP:
            fragment = appFragment;
            break;
        }

        return fragment;
      }

    @Override
      public int getCount() {
        return PAGES;
      }
  }

  /** Called when the activity is first created. */
  @Override
    public void onCreate(Bundle savedInstanceState)
    {
      super.onCreate(savedInstanceState);
      MyLog.d("NetworkLog started");

      context = this;

      loadSettings();
      getLocalIpAddresses();

      data = (RetainInstanceData) getLastCustomNonConfigurationInstance();

      if(data != null) {
        MyLog.d("Restored run");
        ApplicationsTracker.restoreData(data);
        resolver = data.networkLogResolver;

        // restore history loading progress dialog
        history.dialog_showing = data.historyDialogShowing;
        history.dialog_max = data.historyDialogMax;
        history.dialog_progress = data.historyDialogProgress;

        if(history.dialog_showing) {
          history.createProgressDialog(this);
        }

        if(data.feedbackDialogMessage != null) {
          feedbackDialog = new FeedbackDialog(this);
          feedbackDialog.setMessage(data.feedbackDialogMessage);
          feedbackDialog.setAttachLogcat(data.feedbackDialogAttachLogcat);
          feedbackDialog.setCursorPosition(data.feedbackDialogCursorPosition);
          feedbackDialog.show();
        }

        if(data.clearLogDialogShowing) {
          clearLog.showClearLogDialog(this);
        }

        clearLog.progress = data.clearLogProgress;
        clearLog.progress_max = data.clearLogProgressMax;

        if(data.clearLogProgressDialogShowing) {
          clearLog.showProgressDialog(this);
        }
      } else {
        MyLog.d("Fresh run");
        resolver = new NetworkResolver();

        logFragment = (LogFragment) Fragment.instantiate(this, LogFragment.class.getName());
        appFragment = (AppFragment) Fragment.instantiate(this, AppFragment.class.getName());
      }

      logFragment.setParent(this);
      appFragment.setParent(this);

      handler = new Handler();

      setContentView(R.layout.main);

      ActionBar actionBar = getSupportActionBar();
      actionBar.setCustomView(R.layout.actionbar_top);
      actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_CUSTOM);

      if(history == null) {
        history = new HistoryLoader();
      }

      if(clearLog == null) {
        clearLog = new ClearLog();
      }

      statusText = (TextView) findViewById(R.id.statusText);

      viewPager = (ViewPager) findViewById(R.id.viewpager);
      MyFragmentPagerAdapter pagerAdapter = new MyFragmentPagerAdapter(this, getSupportFragmentManager());

      viewPager.setAdapter(pagerAdapter);

      TitlePageIndicator titleIndicator = (TitlePageIndicator) findViewById(R.id.titles);
      titleIndicator.setViewPager(viewPager);

      viewPager.setCurrentItem(1);

      if(isServiceRunning(this, NetworkLogService.class.getName())) {
        doBindService();
      }

      if(data == null) {
        initRunner = new InitRunner(this);
        new Thread(initRunner, "Initialization " + initRunner).start();
      } else {
        state = data.networkLogState;

        if(state != NetworkLog.State.RUNNING) {
          initRunner = new InitRunner(this);
          new Thread(initRunner, "Initialization " + initRunner).start();
        }

        // all data should be restored at this point, release the object
        data = null;
        MyLog.d("data object released");

        state = NetworkLog.State.RUNNING;
      }
      statusUpdater = new StatusUpdater();
      new Thread(statusUpdater, "StatusUpdater").start();
      ThroughputTracker.updateThroughput(0, 0);
    }

  @Override
    public void onResume()
    {
      super.onResume();
      MyLog.d("onResume()");
    }

  @Override
    public void onPause()
    {
      super.onPause();
      MyLog.d("onPause()");
    }

  @Override
    public void onDestroy()
    {
      super.onDestroy();
      MyLog.d("NetworkLog onDestroy");

      if(data == null) {
        // exiting
        state = NetworkLog.State.EXITING;
        if(stopServiceAtExit) {
          stopService();
          ApplicationsTracker.stopWatchingPackages();
        } else if(NetworkLogService.instance == null) {
          ApplicationsTracker.stopWatchingPackages();
        }
      } else {
        // changing configuration
      }

      if(history.dialog_showing == true && history.dialog != null) {
        history.dialog.dismiss();
        history.dialog = null;
      }

      if(feedbackDialog != null && feedbackDialog.dialog != null && feedbackDialog.dialog.isShowing()) {
        feedbackDialog.dialog.dismiss();
        feedbackDialog.dialog = null;
        feedbackDialog = null;
      }

      if(clearLog.dialog != null && clearLog.dialog.isShowing()) {
        clearLog.dialog.dismiss();
        clearLog.dialog = null;
      }

      if(clearLog.progressDialog != null && clearLog.progressDialog.isShowing()) {
        clearLog.progressDialog.dismiss();
        clearLog.progressDialog = null;
      }

      if(initRunner != null) {
        initRunner.stop();
      }

      if(logFragment != null) {
        logFragment.stopUpdater();
      }

      if(appFragment != null) {
        appFragment.stopUpdater();
      }

      if(statusUpdater != null) {
        statusUpdater.stop();
      }

      synchronized(ApplicationsTracker.dialogLock) {
        if(ApplicationsTracker.dialog != null) {
          ApplicationsTracker.dialog.dismiss();
          ApplicationsTracker.dialog = null;
        }
      }

      if(isBound) {
        doUnbindService();
      }
    }

  @Override
    public Object onRetainCustomNonConfigurationInstance() {
      MyLog.d("Saving run");
      data = new RetainInstanceData();
      return data;
    }

  @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getSupportMenuInflater();
      inflater.inflate(R.layout.menu, menu);
      return true;
    }

  @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
      this.menu = menu;
      MenuItem item = menu.findItem(R.id.sort);

      if(viewPager.getCurrentItem() == PAGE_APP) {
        item.setVisible(true);

        Sort sortBy;
        if(appFragment == null || appFragment.sortBy == null) {
          sortBy = NetworkLog.settings.getSortBy();
        } else {
          sortBy = appFragment.sortBy;
        }

        switch(sortBy) {
          case UID:
            item = menu.findItem(R.id.sort_by_uid);
            break;
          case NAME:
            item = menu.findItem(R.id.sort_by_name);
            break;
          case THROUGHPUT:
            item = menu.findItem(R.id.sort_by_throughput);
            break;
          case PACKETS:
            item = menu.findItem(R.id.sort_by_packets);
            break;
          case BYTES:
            item = menu.findItem(R.id.sort_by_bytes);
            break;
          case TIMESTAMP:
            item = menu.findItem(R.id.sort_by_timestamp);
            break;
          default:
            NetworkLog.settings.setSortBy(Sort.BYTES);
            item = menu.findItem(R.id.sort_by_bytes);
        }

        item.setChecked(true);
      } else {
        item.setVisible(false);
      }

      loggingButton = (ToggleButton) findViewById(R.id.actionbar_service_toggle);

      if(isServiceRunning(this, NetworkLogService.class.getName())) {
        loggingButton.setChecked(true);
      } else {
        loggingButton.setChecked(false);
      }

      return true;
    }

  public void serviceToggle(View view) {
    loggingButton = (ToggleButton)view;

    if(!isServiceRunning(this, NetworkLogService.class.getName())) {
      startService();
      loggingButton.setChecked(true);
    } else {
      stopService();
      loggingButton.setChecked(false);
    }
  }

  @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      switch(item.getItemId()) {
        case R.id.filter:
          showFilterDialog();
          break;
        case R.id.overallgraph:
          startActivity(new Intent(this, OverallAppTimelineGraph.class));
          break;
        case R.id.exit:
          finish();
          break;
        case R.id.feedback:
          feedbackDialog = new FeedbackDialog(this);
          feedbackDialog.show();
          break;
        case R.id.clearlog:
          clearLog.showClearLogDialog(this);
          break;
        case R.id.settings:
          startActivity(new Intent(this, Preferences.class));
          break;
        case R.id.sort_by_uid:
          NetworkLog.settings.setSortBy(Sort.UID);
          item.setChecked(true);
          break;
        case R.id.sort_by_name:
          NetworkLog.settings.setSortBy(Sort.NAME);
          item.setChecked(true);
          break;
        case R.id.sort_by_throughput:
          NetworkLog.settings.setSortBy(Sort.THROUGHPUT);
          item.setChecked(true);
          break;
        case R.id.sort_by_packets:
          NetworkLog.settings.setSortBy(Sort.PACKETS);
          item.setChecked(true);
          break;
        case R.id.sort_by_bytes:
          NetworkLog.settings.setSortBy(Sort.BYTES);
          item.setChecked(true);
          break;
        case R.id.sort_by_timestamp:
          NetworkLog.settings.setSortBy(Sort.TIMESTAMP);
          item.setChecked(true);
          break;
        default:
          return super.onOptionsItemSelected(item);
      }

      return true;
    }

  @Override
    public void onBackPressed() {
      confirmExit();
    }

  public void showFilterDialog() {
    new FilterDialog(this);
  }

  public void confirmExit() {
    Context context = this;

    if(settings.getConfirmExit() == false) {
      finish();
      return;
    }

    StringBuilder message = new StringBuilder(getString(R.string.confirm_exit_text));
    boolean serviceRunning = isServiceRunning(context, NetworkLogService.class.getName());

    if(serviceRunning) {
      message.append("\n\n");
      if(stopServiceAtExit) {
        message.append(getString(R.string.logging_will_stop));
      } else {
        message.append(getString(R.string.logging_will_continue));
      }
    }

    View checkBoxView = View.inflate(this, R.layout.confirm_exit_checkbox, null);
    final CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.confirm_exit_checkbox);
    checkBox.setChecked(false);

    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(getString(R.string.confirm_exit_title))
      .setMessage(message.toString())
      .setCancelable(true)
      .setView(checkBoxView)
      .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          settings.setConfirmExit(!checkBox.isChecked());
          finish();
        }
      })
    .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        dialog.cancel();
      }
    });
    AlertDialog alert = builder.create();
    alert.show();
  }

  public static void getLocalIpAddresses() {
    MyLog.d("getLocalIpAddresses");
    localIpAddrs = new ArrayList<String>();

    try {
      for(Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
        NetworkInterface intf = en.nextElement();
        MyLog.d("Network interface found: " + intf.toString());

        for(Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
          InetAddress inetAddress = enumIpAddr.nextElement();
          MyLog.d("InetAddress: " + inetAddress.toString());

          if(!inetAddress.isLoopbackAddress()) {
            MyLog.d("Adding local IP address: [" + inetAddress.getHostAddress().toString() + "]");
            localIpAddrs.add(inetAddress.getHostAddress().toString());
          }
        }
      }
    } catch(SocketException ex) {
      Log.e("NetworkLog", ex.toString());
    }
  }

  public void startService() {
    MyLog.d("Starting service...");

    Intent intent = new Intent(this, NetworkLogService.class);

    intent.putExtra("logfile", settings.getLogFile());

    startService(intent);
    doBindService();
    updateStatusText();
  }

  public void stopService() {
    MyLog.d("Stopping service...");
    doUnbindService();
    stopService(new Intent(this, NetworkLogService.class));
    updateStatusText();
  }

  public static boolean isServiceRunning(Context context, String serviceName) {
    ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);

    for(RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
      if(MyLog.enabled) {
        MyLog.d("Service: " + service.service.getClassName() + "; " + service.pid + "; " + service.clientCount + "; " + service.foreground + "; " + service.process);
      }

      if(serviceName.equals(service.service.getClassName())) {
        return true;
      }
    }

    return false;
  }

  public static void toggleServiceForeground(Boolean value) {
    MyLog.d("toggleServiceForeground " + value);

    if(isBound && service != null) {
      try {
        Message msg = Message.obtain(null, NetworkLogService.MSG_TOGGLE_FOREGROUND);
        msg.obj = value;
        service.send(msg);
      } catch(RemoteException e) {
        /* do nothing */
        Log.d("NetworkLog", "RemoteException toggling foreground", e);
      }
    }
  }

  static Runnable updateStatusRunner = new Runnable() {
    public void run() {
      updateStatusText();
    }
  };

  public static void updateStatus(int icon) {
    if(handler != null) {
      handler.post(updateStatusRunner);
    }
  }

  public static void updateStatusText() {
    if(context == null || statusText == null) {
      return;
    }

    StringBuilder sb = new StringBuilder();
    Resources res = context.getResources();

    if(filterTextInclude.length() > 0 || filterTextExclude.length() > 0) {
      sb.append(res.getString(R.string.filter_applied));

      if(filterTextInclude.length() > 0) {
        sb.append("+[" + filterTextInclude + "] ");
      }

      if(filterTextExclude.length() > 0) {
        sb.append("-[" + filterTextExclude + "] ");
      }
    }

    boolean serviceRunning = NetworkLogService.instance != null;

    if(!serviceRunning) {
      sb.append(res.getString(R.string.logging_inactive));
    }

    if(NetworkLogService.logfileString.length() > 0) {
      sb.append(context.getResources().getString(R.string.logfile_size));
      sb.append(NetworkLogService.logfileString);
    }

    if(serviceRunning && ThroughputTracker.throughputString.length() > 0) {
      sb.append(context.getResources().getString(R.string.throughput));
      sb.append(ThroughputTracker.throughputString);
    }

    statusText.setText(sb);
  }

  public static void updateNotificationText(String text) {
    if(isBound) {
      if(service != null) {
        try {
          Message msg = Message.obtain(null, NetworkLogService.MSG_UPDATE_NOTIFICATION);
          msg.obj = text;
          service.send(msg);
        } catch(RemoteException e) {
          /* do nothing */
          Log.d("NetworkLog", "RemoteException updating notification", e);
        }
      }
    }
  }

  class StatusUpdater implements Runnable {
    boolean running = false;
    Runnable runner = new Runnable() {
      public void run() {
        MyLog.d("Updating statusText");
        NetworkLogService.updateLogfileString();
        updateStatusText();
      }
    };

    public void stop() {
      running = false;
    }

    public void run() {
      running = true;
      MyLog.d("Starting status updater " + this);

      while(running) {
        runOnUiThread(runner);

        try { Thread.sleep(15000); } catch(Exception e) { Log.d("NetworkLog", "StatusUpdater", e); }
      }
      MyLog.d("Stopped status updater " + this);
    }
  }
}
