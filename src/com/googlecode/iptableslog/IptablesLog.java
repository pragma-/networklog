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
import android.content.Context;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.TextView;
import android.view.View;
import android.view.LayoutInflater;

import java.util.ArrayList;

public class IptablesLog extends TabActivity
{
  public static IptablesLogData data = null;

  public static LogView logView;
  public static AppView appView;
  
  public static IptablesLogTracker logTracker;
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

  public static boolean outputPaused;
  
  public static State state;
  public enum State { LOAD_APPS, LOAD_LIST, LOAD_ICONS, RUNNING  };

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

      if(running == false)
        return;

      state = IptablesLog.State.LOAD_LIST;
      appView.getInstalledApps();

      if(running == false)
        return;

      state = IptablesLog.State.LOAD_ICONS;
      appView.loadIcons();

      logTracker = new IptablesLogTracker();

      appView.attachListener();
      appView.startUpdater();

      logView.attachListener();
      logView.startUpdater();

      logTracker.start(data != null);

      state = IptablesLog.State.RUNNING;
      MyLog.d("Init done");
    }
  }

  /** Called when the activity is first created. */
  @Override
    public void onCreate(Bundle savedInstanceState)
    {
      super.onCreate(savedInstanceState);
      MyLog.d("IptablesLog onCreate");

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

      resolveHosts = settings.getResolveHosts();
      resolvePorts = settings.getResolvePorts();

      data = (IptablesLogData) getLastNonConfigurationInstance(); 

      setContentView(R.layout.main);
      handler = new Handler(Looper.getMainLooper());

      if (data != null) {
        MyLog.d("Restored run");
        ApplicationsTracker.restoreData(data);
        resolver = data.iptablesLogResolver;
        outputPaused = data.iptablesLogOutputPaused;
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

      // todo: redesign tabs to be views instead of activities
      //       as this should be less complex and save resources

      // force loading of LogView activity
      tabHost.setCurrentTab(0);
      logView = (LogView) getLocalActivityManager().getCurrentActivity();
      // force loading of AppView activity
      tabHost.setCurrentTab(1);
      appView = (AppView) getLocalActivityManager().getCurrentActivity();

      // display LogView tab by default
      tabHost.setCurrentTab(0);

      if (data == null) {
        initRunner = new InitRunner(this);
        new Thread(initRunner, "Initialization " + initRunner).start();
      } else {
        state = data.iptablesLogState;

        if(state != IptablesLog.State.RUNNING) {
          initRunner = new InitRunner(this);
          new Thread(initRunner, "Initialization " + initRunner).start();
        } else {
          logTracker = new IptablesLogTracker();

          appView.attachListener();
          appView.startUpdater();

          logView.attachListener();
          logView.startUpdater();

          logTracker.start(true);
        }
        // all data should be restored at this point, release the object
        data = null;
        MyLog.d("data object released");

        state = IptablesLog.State.RUNNING;
      }
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

      if(initRunner != null)
        initRunner.stop();
      
      if(logView != null)
        logView.stopUpdater();

      if(appView != null)
        appView.stopUpdater();

      if(logTracker != null)
        logTracker.stop();

      synchronized(ApplicationsTracker.dialogLock) {
        if(ApplicationsTracker.dialog != null) {
          ApplicationsTracker.dialog.dismiss();
          ApplicationsTracker.dialog = null;
        }
      }

      if(data == null) {
        MyLog.d("Shutting down rules");
        Iptables.removeRules();
        IptablesLog.logTracker.kill();
      } else {
        MyLog.d("Not shutting down rules");
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
        case R.id.exit:
          confirmExit(this);
          break;
        case R.id.settings:
          startActivity(new Intent(this, Preferences.class));
          break;
        case R.id.sort_by_uid:
          appView.sortBy = Sort.UID;
          appView.sortData();
          // force adapter refresh if paused
          if(outputPaused)
            appView.refreshAdapter();
          IptablesLog.settings.setSortBy(appView.sortBy);
          break;
        case R.id.sort_by_name:
          appView.sortBy = Sort.NAME;
          appView.sortData();
          // force adapter refresh if paused
          if(outputPaused)
            appView.refreshAdapter();
          IptablesLog.settings.setSortBy(appView.sortBy);
          break;
        case R.id.sort_by_packets:
          appView.sortBy = Sort.PACKETS;
          appView.sortData();
          // force adapter refresh if paused
          if(outputPaused)
            appView.refreshAdapter();
          IptablesLog.settings.setSortBy(appView.sortBy);
          break;
        case R.id.sort_by_bytes:
          appView.sortBy = Sort.BYTES;
          appView.sortData();
          // force adapter refresh if paused
          if(outputPaused)
            appView.refreshAdapter();
          IptablesLog.settings.setSortBy(appView.sortBy);
          break;
        case R.id.sort_by_timestamp:
          appView.sortBy = Sort.TIMESTAMP;
          appView.sortData();
          // force adapter refresh if paused
          if(outputPaused)
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
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle("Confirm exit")
      .setMessage("Are you sure you want to exit?")
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
          appView.resetData();
          logView.resetData();
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
}
