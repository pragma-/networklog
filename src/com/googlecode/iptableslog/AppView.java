package com.googlecode.iptableslog;

import android.util.Log;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Filterable;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.view.LayoutInflater;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class AppView extends Activity implements IptablesLogListener
{
  public ArrayList<ListItem> listData;
  public ArrayList<ListItem> listDataBuffer;
  public boolean listDataBufferIsDirty = false;
  private CustomAdapter adapter;
  public Sort sortBy = Sort.BYTES;
  public ListItem cachedSearchItem;
  private ListViewUpdater updater;

  public class ListItem {
    protected ApplicationsTracker.AppEntry app;
    protected int totalPackets;
    protected int totalBytes;
    protected String lastTimestamp;
    protected ArrayList<String> uniqueHostsList;
    protected boolean uniqueHostsListNeedsSort = false;
    protected String uniqueHosts;

    @Override
      public String toString() {
        return app.name;
      }
  }

  protected static class SortAppsByBytes implements Comparator<ListItem> {
    public int compare(ListItem o1, ListItem o2) {
      return o1.totalBytes > o2.totalBytes ? -1 : (o1.totalBytes == o2.totalBytes) ? 0 : 1;
    }
  }

  protected static class SortAppsByPackets implements Comparator<ListItem> {
    public int compare(ListItem o1, ListItem o2) {
      return o1.totalPackets > o2.totalPackets ? -1 : (o1.totalPackets == o2.totalPackets) ? 0 : 1;
    }
  }

  protected static class SortAppsByTimestamp implements Comparator<ListItem> {
    public int compare(ListItem o1, ListItem o2) {
      return o2.lastTimestamp.compareTo(o1.lastTimestamp.equals("N/A") ? "0" : o1.lastTimestamp);
    }
  }

  protected static class SortAppsByName implements Comparator<ListItem> {
    public int compare(ListItem o1, ListItem o2) {
      return o1.app.name.compareToIgnoreCase(o2.app.name);
    }
  }

  protected static class SortAppsByUid implements Comparator<ListItem> {
    public int compare(ListItem o1, ListItem o2) {
      return o1.app.uid < o2.app.uid ? -1 : (o1.app.uid == o2.app.uid) ? 0 : 1;
    }
  }

  protected void sortData() {
    Comparator<ListItem> sortMethod;

    switch(sortBy) {
      case UID:
        sortMethod = new SortAppsByUid();
        break;
      case NAME:
        sortMethod = new SortAppsByName();
        break;
      case PACKETS:
        sortMethod = new SortAppsByPackets();
        break;
      case BYTES:
        sortMethod = new SortAppsByBytes();
        break;
      case TIMESTAMP:
        sortMethod = new SortAppsByTimestamp();
        break;
      default:
        return;
    }

    Collections.sort(listData, sortMethod);
    adapter.notifyDataSetChanged();
  }

  protected void getInstalledApps() {
    synchronized(listDataBuffer) {
      listData.clear();
      listDataBuffer.clear();

      for(ApplicationsTracker.AppEntry app : ApplicationsTracker.installedApps) {
        if(IptablesLog.initRunner.running == false) {
          MyLog.d("[AppView] Initialization aborted");
          return;
        }

        ListItem item = new ListItem();
        item.app = app;
        item.lastTimestamp = "N/A";
        item.uniqueHostsList = new ArrayList<String>();
        item.uniqueHostsList.add(0, "N/A");
        item.uniqueHosts = "N/A";
        listData.add(item);
        listDataBuffer.add(item);
      }

      runOnUiThread(new Runnable() {
        public void run() {
          // sort by UID to put list in some kind of order
          // (perhaps by name instead? or another user-preference?)
          Collections.sort(listData, new SortAppsByUid());
          // sort by user preference
          sortData();
        }
      });

      // listDataBuffer must always be sorted by UID for binary search
      Collections.sort(listDataBuffer, new SortAppsByUid());
    }

    sortBy = IptablesLog.settings.getSortBy();
    MyLog.d("Sortby after from settings: " + sortBy);
  }

  protected void loadIcons() {
    if(IptablesLog.data == null) {
      new Thread("IconLoader") {
        public void run() {
          long nextUpdateTime = 0;

          int size;
          synchronized(listDataBuffer) {
            size = listDataBuffer.size();
          }
          for(int i = 0; i < size; i++) {
            ListItem item;
            synchronized(listDataBuffer) {
              item = listDataBuffer.get(i);
            }
            if(item.app.packageName == null)
              continue;

            try {
              MyLog.d("Loading icon for " + item.app.packageName + " (" + item.app.name + ", " + item.app.uid + ")");
              Drawable icon = getPackageManager().getApplicationIcon(item.app.packageName);
              item.app.icon = icon;

              // refresh adapter to display icons once every second while still loading icons
              // (once every second instead of immediately after each icon prevents UI lag)
              // (UI still may lag on lower end devices as loading icons is expensive)
              long currentTime = SystemClock.elapsedRealtime();
              if(currentTime >= nextUpdateTime) {
                nextUpdateTime = currentTime + 2000;
                runOnUiThread(new Runnable() {
                  public void run() {
                    MyLog.d("Updating adapter for icons");
                    // refresh adapter to display icon 
                    adapter.notifyDataSetChanged();
                  }
                });
              }
            } catch (Exception e) {
              Log.d("IptablesLog", "Failure to load icon for " + item.app.packageName + " (" + item.app.name + ", " + item.app.uid + ")", e);
            }
          }

          // refresh adapter to display icons
          runOnUiThread(new Runnable() {
            public void run() {
              adapter.notifyDataSetChanged();
            }
          });
        }
      }.start();
    }
  }

  /** Called when the activity is first created. */
  @Override
    public void onCreate(Bundle savedInstanceState)
    {
      super.onCreate(savedInstanceState);

      MyLog.d("AppView created");

      LinearLayout layout = new LinearLayout(this);
      layout.setOrientation(LinearLayout.VERTICAL);

      TextView tv = new TextView(this);
      tv.setText("Application listing");

      layout.addView(tv);

      if(IptablesLog.data == null) {
        listData = new ArrayList<ListItem>();
        listDataBuffer = new ArrayList<ListItem>();
        cachedSearchItem = new ListItem();
      } else {
        restoreData(IptablesLog.data);
      }

      adapter = new CustomAdapter(this, R.layout.appitem, listData);

      ListView lv = new ListView(this);
      lv.setAdapter(adapter);
      lv.setTextFilterEnabled(true);
      lv.setFastScrollEnabled(true);
      lv.setSmoothScrollbarEnabled(false);
      layout.addView(lv);
      setContentView(layout);

      cachedSearchItem.app = new ApplicationsTracker.AppEntry();
    }

  @Override
    public void onBackPressed() {
      IptablesLog parent = (IptablesLog) getParent();
      parent.confirmExit(this);
    }

  public void restoreData(IptablesLogData data) {
    listData = data.appViewListData;
    listDataBuffer = data.appViewListDataBuffer;
    listDataBufferIsDirty = data.appViewListDataBufferIsDirty;
    sortBy = data.appViewSortBy;
    cachedSearchItem = data.appViewCachedSearchItem;

    if(listData == null)
      listData = new ArrayList<ListItem>();

    if(listDataBuffer == null)
      listDataBuffer = new ArrayList<ListItem>();

    if(cachedSearchItem == null)
      cachedSearchItem = new ListItem();
  }

  public void onNewLogEntry(final IptablesLogTracker.LogEntry entry) {
    MyLog.d("AppView: NewLogEntry: " + entry.uid + " " + entry.src + " " + entry.len);

    synchronized(listDataBuffer) {
      cachedSearchItem.app.uid = entry.uid;

      MyLog.d("Binary searching...");
      int index = Collections.binarySearch(listDataBuffer, cachedSearchItem, new Comparator<ListItem>() {
        public int compare(ListItem o1, ListItem o2) {
          //MyLog.d("Comparing " + o1.app.uid + " " + o1.app.name + " vs " + o2.app.uid + " " + o2.app.name);
          return o1.app.uid < o2.app.uid ? -1 : (o1.app.uid == o2.app.uid) ? 0 : 1;
        }
      });

      MyLog.d("Search done: " + index);

      if(index < 0) {
        MyLog.d("No app entry for " + entry.uid);
        return;
      }

      // binarySearch isn't guaranteed to return the first item of items with the same uid
      while(index > 0) {
        if(listDataBuffer.get(index - 1).app.uid == entry.uid)
          index--;
        else break;
      }

      // generally this will iterate once, but some apps may be grouped under the same uid
      while(true) {
        MyLog.d("while: index = " + index);
        ListItem item = listDataBuffer.get(index);

        if(item.app.uid != entry.uid)
          break;

        listDataBufferIsDirty = true;

        item.totalPackets = entry.packets;
        item.totalBytes = entry.bytes;
        item.lastTimestamp = entry.timestamp;

        String src = entry.src + ":" + entry.spt;
        String dst = entry.dst + ":" + entry.dpt;

        if(!entry.src.equals(IptablesLogTracker.localIpAddr) && !item.uniqueHostsList.contains(src)) {
          item.uniqueHostsList.add(src);
          item.uniqueHostsListNeedsSort = true;
        }

        if(!entry.dst.equals(IptablesLogTracker.localIpAddr) && !item.uniqueHostsList.contains(dst)) {
          item.uniqueHostsList.add(dst);
          item.uniqueHostsListNeedsSort = true;
        }

        index++;
        if(index >= listDataBuffer.size())
          break;
      }
    }
  }

  public void startUpdater() {
    updater = new ListViewUpdater();
    new Thread(updater, "AppViewUpdater").start();
  }

  public void stopUpdater() {
    if(updater != null)
      updater.stop();
  }

  public void attachListener() {
    MyLog.d("Adding AppView listener " + this);
    IptablesLog.logTracker.addListener(this);
  }

  // todo: this is largely duplicated in LogView -- move to its own file
  private class ListViewUpdater implements Runnable {
    boolean running = false;
    Runnable runner = new Runnable() {
      public void run() {
        MyLog.d("AppViewListUpdater enter");
        listData.clear();
        synchronized(listDataBuffer) {
          // todo: find a way so that we don't have to go through every entry
          // in listDataBuffer here
          for(ListItem item : listDataBuffer) {
            if(item.uniqueHostsListNeedsSort) {
              MyLog.d("Updating " + item);
              item.uniqueHostsListNeedsSort = false;

              if(item.uniqueHostsList.get(0).equals("N/A")) {
                item.uniqueHostsList.remove(0);
              }

              Collections.sort(item.uniqueHostsList);

              StringBuilder builder = new StringBuilder();
              Iterator<String> itr = item.uniqueHostsList.iterator();
              while(itr.hasNext()) {
                builder.append("\n  " + itr.next());
              }
              item.uniqueHosts = builder.toString();
            }
            listData.add(item);
          }
        }

        sortData();
        MyLog.d("AppViewListUpdater exit");
      }
    };

    public void stop() {
      running = false;
    }

    public void run() {
      running = true;
      MyLog.d("Starting AppViewUpdater " + this);
      while(running) {
        if(listDataBufferIsDirty == true) {
          runOnUiThread(runner);
          listDataBufferIsDirty = false;
        }

        try { Thread.sleep(1000); } catch (Exception e) { Log.d("IptablesLog", "AppViewListUpdater", e); }
      }
      MyLog.d("Stopped AppView updater " + this);
    }
  }

  private class CustomAdapter extends ArrayAdapter<ListItem> implements Filterable {
    LayoutInflater mInflater = (LayoutInflater) getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

    public CustomAdapter(Context context, int resource, List<ListItem> objects) {
      super(context, resource, objects);
    }

    @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;

        ImageView icon;
        TextView name;
        TextView packets;
        TextView bytes;
        TextView timestamp;
        TextView hosts;

        ListItem item = getItem(position);

        if(convertView == null) {
          convertView = mInflater.inflate(R.layout.appitem, null);
          holder = new ViewHolder(convertView);
          convertView.setTag(holder);
        }

        holder = (ViewHolder) convertView.getTag();
        icon = holder.getIcon();
        icon.setImageDrawable(item.app.icon);

        name = holder.getName();
        name.setText("(" + item.app.uid + ")" + " " + item.app.name);

        packets = holder.getPackets();
        packets.setText("Packets: " + item.totalPackets);

        bytes = holder.getBytes();
        bytes.setText("Bytes: " + item.totalBytes);

        timestamp = holder.getTimestamp();
        timestamp.setText("Timestamp: " + item.lastTimestamp);

        hosts = holder.getUniqueHosts();
        hosts.setText("Addrs: " + item.uniqueHosts);

        return convertView;
      }
  }

  private class ViewHolder {
    private View mView;
    private ImageView mIcon = null;
    private TextView mName = null;
    private TextView mPackets = null;
    private TextView mBytes = null;
    private TextView mTimestamp = null;
    private TextView mUniqueHosts = null;

    public ViewHolder(View view) {
      mView = view;
    }

    public ImageView getIcon() {
      if(mIcon == null) {
        mIcon = (ImageView) mView.findViewById(R.id.appIcon);
      }
      return mIcon;
    }

    public TextView getName() {
      if(mName == null) {
        mName = (TextView) mView.findViewById(R.id.appName);
      }
      return mName;
    }

    public TextView getPackets() {
      if(mPackets == null) {
        mPackets = (TextView) mView.findViewById(R.id.appPackets);
      }
      return mPackets;
    }

    public TextView getBytes() {
      if(mBytes == null) {
        mBytes = (TextView) mView.findViewById(R.id.appBytes);
      }
      return mBytes;
    }

    public TextView getTimestamp() {
      if(mTimestamp == null) {
        mTimestamp = (TextView) mView.findViewById(R.id.appLastTimestamp);
      }
      return mTimestamp;
    }

    public TextView getUniqueHosts() {
      if(mUniqueHosts == null) {
        mUniqueHosts = (TextView) mView.findViewById(R.id.appUniqueHosts);
      }
      return mUniqueHosts;
    }
  }
}
