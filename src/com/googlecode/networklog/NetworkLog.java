package com.googlecode.networklog;

import android.os.Bundle;
import android.content.Intent;
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

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitleProvider;

import java.util.Enumeration;
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.io.File;

public class NetworkLog extends FragmentActivity {
  public static RetainInstanceData data = null;

  public static ViewPager viewPager;
  public final static int PAGE_LOG = 0;
  public final static int PAGE_APP = 1;
  public final static int PAGES    = 2;

  public static LogFragment logFragment;
  public static AppFragment appFragment;

  public static TextView statusText;

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

  class IncomingHandler extends Handler {
    @Override
      public void handleMessage(Message msg) {
        MyLog.d("[client] Received message: " + msg);

        switch(msg.what) {
          case NetworkLogService.MSG_BROADCAST_LOG_ENTRY:
            LogEntry entry = (LogEntry) msg.obj;
            MyLog.d("Received entry: " + entry);
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

      state = NetworkLog.State.LOAD_APPS;
      ApplicationsTracker.getInstalledApps(context, handler);

      if(running == false) {
        return;
      }

      state = NetworkLog.State.LOAD_LIST;
      appFragment.getInstalledApps();

      if(running == false) {
        return;
      }

      appFragment.startUpdater();
      logFragment.startUpdater();

      if(startServiceAtStart && !isServiceRunning(context, "com.googlecode.networklog.NetworkLogService")) {
        handler.post(new Runnable() {
          public void run() {
            startService();
          }
        });
      }

      history.loadEntriesFromFile(context, settings.getHistorySize());

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
            return "Log";
          case PAGE_APP:
            return "Apps";
        }
        return "Unnamed";
      }

    @Override
      public Fragment getItem(int index) {
        Fragment fragment = null;

        switch(index) {
          case PAGE_LOG:
            if(logFragment == null) {
              logFragment = (LogFragment) Fragment.instantiate(context, LogFragment.class.getName());
            }
            fragment = logFragment;
            break;
          case PAGE_APP:
            if(appFragment == null) {
              appFragment = (AppFragment) Fragment.instantiate(context, AppFragment.class.getName());
            }
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
      
      handler = new Handler();
      setContentView(R.layout.main);

      MyLog.d("NetworkLog started");

      loadSettings();
      getLocalIpAddresses();

      if(history == null) {
        history = new HistoryLoader();
      }

      data = (RetainInstanceData) getLastCustomNonConfigurationInstance();

      if(data != null) {
        MyLog.d("Restored run");
        ApplicationsTracker.restoreData(data);
        resolver = data.networkLogResolver;

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
      }

      statusText = (TextView) findViewById(R.id.statusText);

      viewPager = (ViewPager) findViewById(R.id.viewpager);
      MyFragmentPagerAdapter pagerAdapter = new MyFragmentPagerAdapter(this, getSupportFragmentManager());

      viewPager.setAdapter(pagerAdapter);

      TitlePageIndicator titleIndicator = (TitlePageIndicator) findViewById(R.id.titles);
      titleIndicator.setViewPager(viewPager);

      viewPager.setCurrentItem(0);
      viewPager.setCurrentItem(1);

      if(isServiceRunning(this, "com.googlecode.networklog.NetworkLogService")) {
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
        } else {
          appFragment.startUpdater();
          logFragment.startUpdater();
        }

        // all data should be restored at this point, release the object
        data = null;
        MyLog.d("data object released");

        state = NetworkLog.State.RUNNING;
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
        state = NetworkLog.State.EXITING;
        if(stopServiceAtExit) {
          stopService();
        }
      } else {
        // changing configuration

      }

      if(history.dialog_showing == true && history.dialog != null) {
        history.dialog.dismiss();
        history.dialog = null;
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
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.layout.menu, menu);
      return true;
    }

  @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
      MenuItem item;

      item = menu.findItem(R.id.sort);

      if(viewPager.getCurrentItem() == PAGE_APP) {
        item.setVisible(true);

        switch(appFragment.sortBy) {
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
            NetworkLog.settings.setSortBy(Sort.BYTES);
            appFragment.sortBy = Sort.BYTES;
            item = menu.findItem(R.id.sort_by_bytes);
        }

        item.setChecked(true);
      } else {
        item.setVisible(false);
      }

      item = menu.findItem(R.id.service_toggle);

      if(isServiceRunning(this, "com.googlecode.networklog.NetworkLogService")) {
        item.setTitle("Stop logging");
      } else {
        item.setTitle("Start logging");
      }

      return true;
    }

  @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      switch(item.getItemId()) {

        case R.id.filter:
          showFilterDialog();
          break;

        case R.id.service_toggle:
          if(!isServiceRunning(this, "com.googlecode.networklog.NetworkLogService")) {
            startService();
          } else {
            stopService();
          }

          break;

        case R.id.overallgraph:
          startActivity(new Intent(this, OverallAppTimelineGraph.class));
          break;

        case R.id.exit:
          finish();
          break;

        case R.id.settings:
          startActivity(new Intent(this, Preferences.class));
          break;

        case R.id.sort_by_uid:
          appFragment.sortBy = Sort.UID;
          appFragment.sortData();
          appFragment.refreshAdapter();

          NetworkLog.settings.setSortBy(appFragment.sortBy);
          break;

        case R.id.sort_by_name:
          appFragment.sortBy = Sort.NAME;
          appFragment.sortData();
          appFragment.refreshAdapter();

          NetworkLog.settings.setSortBy(appFragment.sortBy);
          break;

        case R.id.sort_by_packets:
          appFragment.sortBy = Sort.PACKETS;
          appFragment.sortData();
          appFragment.refreshAdapter();

          NetworkLog.settings.setSortBy(appFragment.sortBy);
          break;

        case R.id.sort_by_bytes:
          appFragment.sortBy = Sort.BYTES;
          appFragment.sortData();
          appFragment.refreshAdapter();

          NetworkLog.settings.setSortBy(appFragment.sortBy);
          break;

        case R.id.sort_by_timestamp:
          appFragment.sortBy = Sort.TIMESTAMP;
          appFragment.sortData();
          appFragment.refreshAdapter();

          NetworkLog.settings.setSortBy(appFragment.sortBy);
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

    StringBuilder message = new StringBuilder("Are you sure you want to exit?");
    boolean serviceRunning = isServiceRunning(context, "com.googlecode.networklog.NetworkLogService");

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
          finish();
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
    stopService(new Intent(this, NetworkLogService.class));
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

    if(filterTextInclude.length() > 0 || filterTextExclude.length() > 0) {
      sb.append("Filter: ");

      if(filterTextInclude.length() > 0) {
        sb.append("+[" + filterTextInclude + "] ");
      }

      if(filterTextExclude.length() > 0) {
        sb.append("-[" + filterTextExclude + "] ");
      }
    }

    if(!isServiceRunning(context, "com.googlecode.networklog.NetworkLogService")) {
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
              Message msg = Message.obtain(null, NetworkLogService.MSG_UPDATE_NOTIFICATION);
              msg.obj = size;
              service.send(msg);
            } catch(RemoteException e) {
              /* do nothing */
              Log.d("NetworkLog", "RemoteException updating notification", e);
            }
          }
        }
      }
    } catch(Exception e) {
      sb.append(" Bad logfile.");
    }

    statusText.setText(Html.fromHtml("<small>" + sb + "</small>"));
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

        try { Thread.sleep(15000); } catch(Exception e) { Log.d("NetworkLog", "StatusUpdater", e); }
      }
      MyLog.d("Stopped status updater " + this);
    }
  }
}
