package com.googlecode.iptableslog;

import android.app.TabActivity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.TabWidget;
import android.widget.TabHost;
import android.content.res.Resources;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.IBinder;
import android.text.Html;

import android.content.Context;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.TextView;
import android.view.View;
import android.view.LayoutInflater;
import android.util.Log;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;

import java.util.Enumeration;
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.io.File;

public class IptablesLog extends TabActivity
{
  public static IptablesLogData data = null;

  public static LogView logView;
  public static AppView appView;

  public static Settings settings;

  public static Handler handler;

  public static Object scriptLock = new Object();

  public static String filterTextInclude;
  public static ArrayList<String> filterTextIncludeList = new ArrayList<String>();
  public static boolean filterUidInclude;
  public static boolean filterNameInclude;
  public static boolean filterAddressInclude;
  public static boolean filterPortInclude;

  public static String filterTextExclude;
  public static ArrayList<String> filterTextExcludeList = new ArrayList<String>();
  public static boolean filterUidExclude;
  public static boolean filterNameExclude;
  public static boolean filterAddressExclude;
  public static boolean filterPortExclude;

  public static NetworkResolver resolver;
  public static boolean resolveHosts;
  public static boolean resolvePorts;

  public static boolean startServiceAtStart;
  public static boolean stopServiceAtExit;

  public static boolean outputPaused;

  public static HistoryLoader history;

  public static StatusUpdater statusUpdater;

  public static ArrayList<String> localIpAddrs;

  public static Messenger service = null;
  public static Messenger messenger = null;
  public static boolean isBound = false;

  public static ServiceConnection connection = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder serv) {
      service = new Messenger(serv);
      isBound = true;

      MyLog.d("Attached to service; setting isBound true");

      // Register with service
      try {
        Message msg = Message.obtain(null, IptablesLogService.MSG_REGISTER_CLIENT);
        msg.replyTo = messenger;
        service.send(msg);
      } catch(RemoteException e) {
        /* do nothing */
        Log.d("IptablesLog", "RemoteException registering with service", e);
      }
    }

    public void onServiceDisconnected(ComponentName className) {
      MyLog.d("Detached from service; setting isBound false");
      service = null;
      isBound = false;
    }
  };

  class IncomingHandler extends Handler {
    @Override
      public void handleMessage(Message msg) {
        MyLog.d("[client] Received message: " + msg);

        switch(msg.what) {
          case IptablesLogService.MSG_BROADCAST_LOG_ENTRY:
            LogEntry entry = (LogEntry) msg.obj;
            MyLog.d("Received entry: " + entry);
            logView.onNewLogEntry(entry);
            appView.onNewLogEntry(entry);
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
    //bindService(new Intent(this, IptablesLogService.class), connection, 0);
    bindService(new Intent(this, IptablesLogService.class), connection, 0);
    MyLog.d("doBindService done");
  }

  void doUnbindService() {
    MyLog.d("doUnbindService");
    if(isBound) {
      if(service != null) {
        try {
          MyLog.d("Unregistering from service; service: " + service + "; messenger: " + messenger);
          Message msg = Message.obtain(null, IptablesLogService.MSG_UNREGISTER_CLIENT);
          msg.replyTo = messenger;
          service.send(msg);
        } catch(RemoteException e) {
          /* do nothing */
          Log.d("IptablesLog", "RemoteException unregistering with service", e);
        }

        try {
          MyLog.d("Unbinding connection from service:" + connection);
          unbindService(connection);
        } catch(Exception e) {
          Log.d("IptablesLog", "Ignored unbind exception:", e);
        } finally {
          MyLog.d("Setting isBound false");
          isBound = false;
        }

        MyLog.d("doUnbindService done");
      }
    }
  }

  public static State state;
  public enum State { LOAD_APPS, LOAD_LIST, RUNNING, EXITING  };

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

      state = IptablesLog.State.LOAD_APPS;
      ApplicationsTracker.getInstalledApps(context, handler);

      if(running == false) {
        return;
      }

      state = IptablesLog.State.LOAD_LIST;
      appView.getInstalledApps();

      if(running == false) {
        return;
      }

      appView.startUpdater();
      logView.startUpdater();

      if(startServiceAtStart && !isServiceRunning(context, "com.googlecode.iptableslog.IptablesLogService")) {
        startService();
      }

      history.loadEntriesFromFile(context, settings.getHistorySize());

      state = IptablesLog.State.RUNNING;
      MyLog.d("Init done");
    }
  }

  /** Called when the activity is first created. */
  @Override
    public void onCreate(Bundle savedInstanceState)
    {
      super.onCreate(savedInstanceState);
      MyLog.d("IptablesLog started");
      history = new HistoryLoader();
      handler = new Handler();

      setContentView(R.layout.main);

      settings = new Settings(this);

      MyLog.enabled = settings.getLogcatDebug();

      filterTextInclude = settings.getFilterTextInclude();
      FilterUtils.buildList(filterTextInclude, filterTextIncludeList);
      filterUidInclude = settings.getFilterUidInclude();
      filterNameInclude = settings.getFilterNameInclude();
      filterAddressInclude = settings.getFilterAddressInclude();
      filterPortInclude = settings.getFilterPortInclude();

      filterTextExclude = settings.getFilterTextExclude();
      FilterUtils.buildList(filterTextExclude, filterTextExcludeList);
      filterUidExclude = settings.getFilterUidExclude();
      filterNameExclude = settings.getFilterNameExclude();
      filterAddressExclude = settings.getFilterAddressExclude();
      filterPortExclude = settings.getFilterPortExclude();

      startServiceAtStart = settings.getStartServiceAtStart();
      stopServiceAtExit = settings.getStopServiceAtExit();

      resolveHosts = settings.getResolveHosts();
      resolvePorts = settings.getResolvePorts();

      startServiceAtStart = settings.getStartServiceAtStart();
      stopServiceAtExit = settings.getStopServiceAtExit();

      getLocalIpAddresses();

      data = (IptablesLogData) getLastNonConfigurationInstance();

      if(data != null) {
        MyLog.d("Restored run");
        ApplicationsTracker.restoreData(data);
        resolver = data.iptablesLogResolver;
        outputPaused = data.iptablesLogOutputPaused;

        // restore history loading progress dialog
        history.dialog_showing = data.historyDialogShowing;
        history.dialog_max = data.historyDialogMax;
        history.dialog_progress = data.historyDialogProgress;

        if(history.dialog_showing && history.dialog == null) {
          history.createProgressDialog(this);
        }
      } else {
        MyLog.d("Fresh run");

        resolver = new NetworkResolver();
        outputPaused = false;
      }

      Resources res = getResources();
      TabHost tabHost = getTabHost();
      TabHost.TabSpec spec;
      Intent intent;

      tabHost.getTabWidget().setDividerDrawable(R.drawable.tab_divider);

      View tab = LayoutInflater.from(this).inflate(R.layout.tabview, null);
      ((TextView)tab.findViewById(R.id.tabtext)).setText("Log");
      intent = new Intent().setClass(this, LogView.class);
      spec = tabHost.newTabSpec("log").setIndicator(tab).setContent(intent);
      tabHost.addTab(spec);

      tab = LayoutInflater.from(this).inflate(R.layout.tabview, null);
      ((TextView)tab.findViewById(R.id.tabtext)).setText("Apps");
      intent = new Intent().setClass(this, AppView.class);
      spec = tabHost.newTabSpec("apps").setIndicator(tab).setContent(intent);
      tabHost.addTab(spec);

      // force loading of LogView activity
      tabHost.setCurrentTab(0);
      logView = (LogView) getLocalActivityManager().getCurrentActivity();
      // force loading of AppView activity
      tabHost.setCurrentTab(1);
      appView = (AppView) getLocalActivityManager().getCurrentActivity();

      // display LogView tab by default
      tabHost.setCurrentTab(0);

      if(isServiceRunning(this, "com.googlecode.iptableslog.IptablesLogService")) {
        doBindService();
      }

      if(data == null) {
        initRunner = new InitRunner(this);
        new Thread(initRunner, "Initialization " + initRunner).start();
      } else {
        state = data.iptablesLogState;

        if(state != IptablesLog.State.RUNNING) {
          initRunner = new InitRunner(this);
          new Thread(initRunner, "Initialization " + initRunner).start();
        } else {
          appView.startUpdater();
          logView.startUpdater();
        }

        // all data should be restored at this point, release the object
        data = null;
        MyLog.d("data object released");

        state = IptablesLog.State.RUNNING;
      }
      statusUpdater = new StatusUpdater();
      new Thread(statusUpdater, "StatusUpdater").start();
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
      MyLog.d("onDestroy called");

      if(data == null) {
        // exiting
        state = IptablesLog.State.EXITING;
        if(stopServiceAtExit) {
          stopService();
        }
      } else {
        // changing configuration

      }

      if(history.dialog_showing == true) {
        history.dialog.dismiss();
        history.dialog = null;
      }

      if(initRunner != null) {
        initRunner.stop();
      }

      if(logView != null) {
        logView.stopUpdater();
      }

      if(appView != null) {
        appView.stopUpdater();
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
    public Object onRetainNonConfigurationInstance() {
      MyLog.d("Saving run");
      data = new IptablesLogData();
      return data;
    }

  @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.layout.menu, menu);
      return true;
    }

  @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
      MenuItem item;

      item = menu.findItem(R.id.sort);

      if(getLocalActivityManager().getCurrentActivity() instanceof AppView) {
        item.setVisible(true);

        switch(appView.sortBy) {
          case UID:
            item = menu.findItem(R.id.sort_by_uid);
            break;

          case NAME:
            item = menu.findItem(R.id.sort_by_name);
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
            IptablesLog.settings.setSortBy(Sort.BYTES);
            appView.sortBy = Sort.BYTES;
            item = menu.findItem(R.id.sort_by_bytes);
        }

        item.setChecked(true);
      } else {
        item.setVisible(false);
      }

      item = menu.findItem(R.id.service_toggle);

      if(isServiceRunning(this, "com.googlecode.iptableslog.IptablesLogService")) {
        item.setTitle("Stop logging");
      } else {
        item.setTitle("Start logging");
      }

      /*
         item = menu.findItem(R.id.pause);

         if(outputPaused)
         item.setTitle("Resume Output");
         else
         item.setTitle("Pause Output");
         */

      return true;
    }

  @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      switch(item.getItemId()) {
        case R.id.filter:
          showFilterDialog();
          break;

          /*
             case R.id.pause:
             outputPaused = !outputPaused;

             if(outputPaused)
             item.setTitle("Resume Output");
             else {
             item.setTitle("Pause Output");
             logView.refreshAdapter();
             appView.refreshAdapter();
             }
             break;
             */
        case R.id.service_toggle:
          if(!isServiceRunning(this, "com.googlecode.iptableslog.IptablesLogService")) {
            startService();
          } else {
            stopService();
          }

          break;

        case R.id.overallgraph:
          startActivity(new Intent(this, OverallAppTimelineGraph.class));
          break;

        case R.id.exit:
          confirmExit(this);
          break;

        case R.id.settings:
          startActivity(new Intent(this, Preferences.class));
          break;

        case R.id.sort_by_uid:
          appView.sortBy = Sort.UID;
          appView.sortData();
          appView.refreshAdapter();

          IptablesLog.settings.setSortBy(appView.sortBy);
          break;

        case R.id.sort_by_name:
          appView.sortBy = Sort.NAME;
          appView.sortData();
          appView.refreshAdapter();

          IptablesLog.settings.setSortBy(appView.sortBy);
          break;

        case R.id.sort_by_packets:
          appView.sortBy = Sort.PACKETS;
          appView.sortData();
          appView.refreshAdapter();

          IptablesLog.settings.setSortBy(appView.sortBy);
          break;

        case R.id.sort_by_bytes:
          appView.sortBy = Sort.BYTES;
          appView.sortData();
          appView.refreshAdapter();

          IptablesLog.settings.setSortBy(appView.sortBy);
          break;

        case R.id.sort_by_timestamp:
          appView.sortBy = Sort.TIMESTAMP;
          appView.sortData();
          appView.refreshAdapter();

          IptablesLog.settings.setSortBy(appView.sortBy);
          break;

        default:
          return super.onOptionsItemSelected(item);
      }

      return true;
    }

  @Override
    public void onBackPressed() {
      confirmExit(this);
    }

  public void showFilterDialog() {
    Context context = getLocalActivityManager().getCurrentActivity();
    new FilterDialog(context);
  }

  public void confirmExit(Context context) {
    StringBuilder message = new StringBuilder("Are you sure you want to exit?");
    boolean serviceRunning = isServiceRunning(context, "com.googlecode.iptableslog.IptablesLogService");

    if(serviceRunning) {
      if(stopServiceAtExit) {
        message.append("\n\nLogging will be stopped.");
      } else {
        message.append("\n\nLogging will continue in background service.");
      }
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle("Confirm exit")
      .setMessage(message.toString())
      .setCancelable(true)
      .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          IptablesLog.this.finish();
        }
      })
    .setNegativeButton("No", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        dialog.cancel();
      }
    });
    AlertDialog alert = builder.create();
    alert.show();
  }

  public void confirmReset(Context context) {
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle("Confirm data reset")
      .setMessage("Are you sure you want to reset data?")
      .setCancelable(true)
      .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          appView.clear();
          logView.clear();
        }
      })
    .setNegativeButton("No", new DialogInterface.OnClickListener() {
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
        MyLog.d(intf.toString());

        for(Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
          InetAddress inetAddress = enumIpAddr.nextElement();
          MyLog.d(inetAddress.toString());

          if(!inetAddress.isLoopbackAddress()) {
            MyLog.d("Adding local IP address: [" + inetAddress.getHostAddress().toString() + "]");
            localIpAddrs.add(inetAddress.getHostAddress().toString());
          }
        }
      }
    } catch(SocketException ex) {
      Log.e("IptablesLog", ex.toString());
    }
  }

  public void startService() {
    MyLog.d("Starting service...");

    Intent intent = new Intent(this, IptablesLogService.class);

    intent.putExtra("logfile", settings.getLogFile());
    intent.putExtra("logfile_maxsize", settings.getLogFileMaxSize());

    startService(intent);
    doBindService();
    handler.post(new Runnable() {
      public void run() {
        updateStatusText(getApplicationContext());
      }
    });
  }

  public void stopService() {
    MyLog.d("Stopping service...");
    doUnbindService();
    stopService(new Intent(this, IptablesLogService.class));
    updateStatusText(this);
  }

  public static boolean isServiceRunning(Context context, String serviceName) {
    ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);

    for(RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
      MyLog.d("Service: " + service.service.getClassName() + "; " + service.pid + "; " + service.uid + "; " + service.clientCount + "; " + service.foreground + "; " + service.process);

      if(serviceName.equals(service.service.getClassName())) {
        return true;
      }
    }

    return false;
  }

  public static void updateStatusText(Context context) {
    StringBuilder sb = new StringBuilder();

    if(isServiceRunning(context, "com.googlecode.iptableslog.IptablesLogService")) {
      if(filterTextInclude.length() > 0 || filterTextExclude.length() > 0) {
        sb.append("Filter: ");

        if(filterTextInclude.length() > 0) {
          sb.append("+[" + filterTextInclude + "] ");
        }

        if(filterTextExclude.length() > 0) {
          sb.append("-[" + filterTextExclude + "]");
        }
      }
    } else {
      sb.append("Logging not active.");
    }

    try {
      File logfile = new File(settings.getLogFile());
      long length = logfile.length();

      if(length > 0) {
        String size;

        if(length > 1000000) {
          size = String.format("%.2f", (length / 1000000.0)) + "MB";
        } else if(length > 1000) {
          size = String.format("%.2f", (length / 1000.0)) + "KB";
        } else {
          size = String.valueOf(length);
        }

        sb.append(" Logfile size: " + size);

        if(isBound) {
          if(service != null) {
            try {
              Message msg = Message.obtain(null, IptablesLogService.MSG_UPDATE_NOTIFICATION);
              msg.obj = size;
              service.send(msg);
            } catch(RemoteException e) {
              /* do nothing */
              Log.d("IptablesLog", "RemoteException updating notification", e);
            }
          }
        }
      }
    } catch(Exception e) {
      sb.append(" Bad logfile.");
    }

    appView.statusText.setText(Html.fromHtml("<small>" + sb + "</small>"));
    logView.statusText.setText(Html.fromHtml("<small>" + sb + "</small>"));
  }

  class StatusUpdater implements Runnable {
    boolean running = false;
    Runnable runner = new Runnable() {
      public void run() {
        MyLog.d("Updating statusText");
        updateStatusText(getApplicationContext());
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

        try { Thread.sleep(15000); } catch(Exception e) { Log.d("IptablesLog", "StatusUpdater", e); }
      }
      MyLog.d("Stopped status updater " + this);
    }
  }
}
