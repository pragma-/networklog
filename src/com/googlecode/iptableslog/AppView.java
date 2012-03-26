package com.googlecode.iptableslog;

import android.util.Log;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Filter;
import android.widget.Filterable;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.view.LayoutInflater;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.Html;
import android.widget.TextView.BufferType;

import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class AppView extends Activity implements IptablesLogListener
{
  // listData bound to adapter
  public ArrayList<ListItem> listData;
  // listDataBuffer used to buffer incoming log entries and to hold original list data for filtering
  public ArrayList<ListItem> listDataBuffer;
  public boolean listDataBufferIsDirty = false;
  private CustomAdapter adapter;
  public Sort preSortBy;
  public Sort sortBy;
  public ListItem cachedSearchItem;
  private ListViewUpdater updater;

  public class HostInfo {
    protected int sentPackets;
    protected int sentBytes;
    protected String sentTimestamp;
    protected int sentPort;
    protected int receivedPackets;
    protected int receivedBytes;
    protected String receivedTimestamp;
    protected int receivedPort;
    protected String resolvedAddress;
  }

  public class ListItem {
    protected ApplicationsTracker.AppEntry app;
    protected int totalPackets;
    protected int totalBytes;
    protected String lastTimestamp;
    protected HashMap<String, HostInfo> uniqueHostsList;
    protected boolean uniqueHostsListNeedsSort = false;
    protected boolean uniqueHostsIsFiltered = false;
    protected String uniqueHosts;
    protected String uniqueHostsUnfiltered;

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
      return o2.lastTimestamp.compareTo(o1.lastTimestamp.equals("") ? "0" : o1.lastTimestamp);
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

  protected void preSortData() {
    Comparator<ListItem> sortMethod;

    switch(preSortBy) {
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

    synchronized(listData) {
      Collections.sort(listData, sortMethod);
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

    synchronized(listData) {
      Collections.sort(listData, sortMethod);
    }
  }

  public void refreshHosts() {
  }

  public void refreshPorts() {
  }

  public void resetData() {
    getInstalledApps();
  }

  protected void getInstalledApps() {
    synchronized(listDataBuffer) {
      synchronized(listData) {
        listData.clear();
        listDataBuffer.clear();

        synchronized(ApplicationsTracker.installedAppsLock) {
          for(ApplicationsTracker.AppEntry app : ApplicationsTracker.installedApps) {
            if(IptablesLog.state != IptablesLog.State.RUNNING && IptablesLog.initRunner.running == false) {
              MyLog.d("[AppView] Initialization aborted");
              return;
            }

            ListItem item = new ListItem();
            item.app = app;
            item.lastTimestamp = "";
            item.uniqueHostsList = new HashMap<String, HostInfo>();
            item.uniqueHosts = "";
            item.uniqueHostsUnfiltered = "";
            listData.add(item);
            listDataBuffer.add(item);
          }
        }

        runOnUiThread(new Runnable() {
          public void run() {
            preSortData();

            // apply filter if there is one set
            //if(IptablesLog.filterText.length() > 0) {
              setFilter(IptablesLog.filterText);
            //}

            adapter.notifyDataSetChanged();
          }
        });

        // listDataBuffer must always be sorted by UID for binary search
        Collections.sort(listDataBuffer, new SortAppsByUid());
      }
    }
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
                    /*
                    preSortData();
                    sortData();
                    setFilter(IptablesLog.filterText);
                    */
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
              preSortData();
              sortData();
              setFilter(IptablesLog.filterText);
              adapter.notifyDataSetChanged();
              IptablesLog.logView.refreshIcons();
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

      sortBy = IptablesLog.settings.getSortBy();
      MyLog.d("Sort-by loaded from settings: " + sortBy);

      preSortBy = IptablesLog.settings.getPreSortBy();
      MyLog.d("Pre-sort-by loaded from settings: " + preSortBy);

      LinearLayout layout = new LinearLayout(this);
      layout.setOrientation(LinearLayout.VERTICAL);

      TextView tv = new TextView(this);
      tv.setText("Application listing");

      layout.addView(tv);

      if(IptablesLog.data == null) {
        listData = new ArrayList<ListItem>();
        listDataBuffer = new ArrayList<ListItem>();
        cachedSearchItem = new ListItem();
        cachedSearchItem.app = new ApplicationsTracker.AppEntry();
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
    preSortBy = data.appViewPreSortBy;
    cachedSearchItem = data.appViewCachedSearchItem;

    if(listData == null)
      listData = new ArrayList<ListItem>();

    if(listDataBuffer == null)
      listDataBuffer = new ArrayList<ListItem>();

    if(cachedSearchItem == null)
      cachedSearchItem = new ListItem();

    if(cachedSearchItem.app == null)
      cachedSearchItem.app = new ApplicationsTracker.AppEntry();

    if(sortBy == null) {
      sortBy = IptablesLog.settings.getSortBy();
      MyLog.d("[restoreData] Sort-by loaded from settings: " + sortBy);
    }

    if(preSortBy == null) {
      preSortBy = IptablesLog.settings.getPreSortBy();
      MyLog.d("[restoreData] Pre-sort-by loaded from settings: " + preSortBy);
    }
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

        String src = entry.src.toLowerCase() + ":" + entry.spt;
        String dst = entry.dst.toLowerCase() + ":" + entry.dpt;

        HostInfo info;

        // todo: make filtering out local IP a user preference
        if(!entry.src.equals(IptablesLogTracker.localIpAddr)) {
          info = item.uniqueHostsList.get(src);

          if(info == null) {
            info = new HostInfo();
          }

          info.receivedPackets++;
          info.receivedBytes += entry.len;
          info.receivedTimestamp = entry.timestamp;
          info.receivedPort = entry.spt;

          item.uniqueHostsList.put(src, info);
          item.uniqueHostsListNeedsSort = true;
        }

        // todo: make filtering out local IP a user preference
        if(!entry.dst.equals(IptablesLogTracker.localIpAddr)) {
          info = item.uniqueHostsList.get(dst);

          if(info == null) {
            info = new HostInfo();
          }

          info.sentPackets++;
          info.sentBytes += entry.len;
          info.sentTimestamp = entry.timestamp;
          info.sentPort = entry.dpt;

          item.uniqueHostsList.put(dst, info);
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

  public static void buildUniqueHosts(ListItem item, boolean applyToUnfiltered) {
    List<String> list = new ArrayList<String>(item.uniqueHostsList.keySet());

    // todo: sort by user preference (bytes, timestamp, address, ports)
    Collections.sort(list);

    StringBuilder builder = new StringBuilder("Addrs:");
    Iterator<String> itr = list.iterator();
    while(itr.hasNext()) {
      String host = itr.next();
      HostInfo info = item.uniqueHostsList.get(host);
      builder.append("<br>&nbsp;&nbsp;");
      builder.append("<u>" + host + "</u>");
      if(info.sentPackets > 0) {
        builder.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;");
        builder.append("<small>Sent:</small> <b>" + info.sentPackets + "</b> <small>packets,</small> <b>" + info.sentBytes + "</b> <small>bytes</small> (" + info.sentTimestamp.substring(info.sentTimestamp.indexOf(' ') + 1, info.sentTimestamp.length()) + ")");
      }
      if(info.receivedPackets > 0) {
        builder.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;");
        builder.append("<small>Recv:</small> <em>" + info.receivedPackets + "</em> <small>packets,</small> <em>" + info.receivedBytes + "</em> <small>bytes</small> (" + info.receivedTimestamp.substring(info.receivedTimestamp.indexOf(' ') + 1, info.receivedTimestamp.length()) + ")");
      }
    }
    item.uniqueHosts = builder.toString();

    if(applyToUnfiltered == true)
      item.uniqueHostsUnfiltered = new String(builder.toString());
  }

  // todo: this is largely duplicated in LogView -- move to its own file
  private class ListViewUpdater implements Runnable {
    boolean running = false;
    Runnable runner = new Runnable() {
      public void run() {
        synchronized(listData) {
          MyLog.d("AppViewListUpdater enter");
          listData.clear();
          synchronized(listDataBuffer) {
            // todo: find a way so that we don't have to go through every entry
            // in listDataBuffer here (maybe use some sort of reference mapping)
            for(ListItem item : listDataBuffer) {
              if(item.uniqueHostsListNeedsSort) {
                MyLog.d("Updating " + item);
                item.uniqueHostsListNeedsSort = false;

                buildUniqueHosts(item, true);
                listData.add(item);
              }
            }
          }

          preSortData();
          sortData();

          // apply filter if there is one set
          //if(IptablesLog.filterText.length() > 0) {
            setFilter(IptablesLog.filterText);
          //}

          adapter.notifyDataSetChanged();
        }

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

  public void setFilter(CharSequence s) {
    MyLog.d("[AppView] setFilter(" + s + ")");
    adapter.getFilter().filter(s);
  }

  private class CustomAdapter extends ArrayAdapter<ListItem> /* implements Filterable */ {
    LayoutInflater mInflater = (LayoutInflater) getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    CustomFilter filter;
    ArrayList<ListItem> originalItems = new ArrayList<ListItem>();

    public CustomAdapter(Context context, int resource, List<ListItem> objects) {
      super(context, resource, objects);
    }

    private class CustomFilter extends Filter {
      @Override
        protected FilterResults performFiltering(CharSequence constraint) {
          // store constraint for edittext/preferences 
          IptablesLog.filterText = constraint;

          constraint = constraint.toString().toLowerCase();

          MyLog.d("[AppView] filter constraint: [" + constraint + "]");

          FilterResults results = new FilterResults();

          synchronized(listDataBuffer) {
            originalItems.clear();
            originalItems.addAll(listDataBuffer);
          }

          if(constraint == null || constraint.length() == 0) {
            MyLog.d("[AppView] no constraint item count: " + originalItems.size());

            // undo uniqueHosts filtering
            for(ListItem item : originalItems) {
              if(item.uniqueHostsIsFiltered) {
                item.uniqueHostsIsFiltered = false;
                buildUniqueHosts(item, false);
              }
            }

            results.values = originalItems;
            results.count = originalItems.size();
          } else {
            ArrayList<ListItem> filteredItems = new ArrayList<ListItem>();
            ArrayList<ListItem> localItems = new ArrayList<ListItem>();
            localItems.addAll(originalItems);
            int count = localItems.size();

            MyLog.d("[AppView] item count: " + count);

            String[] constraints = constraint.toString().split(",");

            for(int i = 0; i < count; i++) {
              ListItem item = localItems.get(i);
              MyLog.d("[AppView] testing filtered item " + item + "; constraint: [" + constraint + "]");

              boolean matched = false;

              for(String c : constraints) {
                c = c.trim();
                if((IptablesLog.filterName && item.app.nameLowerCase.contains(c))
                    || (IptablesLog.filterUid && item.app.uidString.contains(c))
                    || (IptablesLog.filterAddress && item.uniqueHostsUnfiltered.contains(c))
                    || (IptablesLog.filterPort && item.uniqueHostsUnfiltered.contains(":" + c))) 
                {
                  matched = true;
                }
              }

              if(matched) {
                MyLog.d("[AppView] adding filtered item " + item);
                filteredItems.add(item);

                // rebuild unique hosts list with filter applied if constraint matches address or port
                if(IptablesLog.filterAddress || IptablesLog.filterPort) 
                {
                  StringBuilder builder = new StringBuilder("Addrs:");

                  List<String> list = new ArrayList<String>(item.uniqueHostsList.keySet());
                  // todo: sort by user preference (bytes, timestamp, address, ports)
                  Iterator<String> itr = list.iterator();

                  while(itr.hasNext()) {
                    String host = itr.next();
                    matched = false;
                    for(String c : constraints) {
                      c = c.trim();
                      if((IptablesLog.filterAddress && host.contains(c))
                          || (IptablesLog.filterPort && host.contains(":" + c)))
                      {
                        matched = true;
                      }
                    }

                    if(matched) {
                      HostInfo info = item.uniqueHostsList.get(host);
                      builder.append("<br>&nbsp;&nbsp;");
                      builder.append("<u>" + host + "</u>");
                      if(info.sentPackets > 0) {
                        builder.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;");
                        builder.append("<small>Sent:</small> <b>" + info.sentPackets + "</b> <small>packets,</small> <b>" + info.sentBytes + "</b> <small>bytes</small> (" + info.sentTimestamp.substring(info.sentTimestamp.indexOf(' ') + 1, info.sentTimestamp.length()) + ")");
                      }
                      if(info.receivedPackets > 0) {
                        builder.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;");
                        builder.append("<small>Recv:</small> <em>" + info.receivedPackets + "</em> <small>packets,</small> <em>" + info.receivedBytes + "</em> <small>bytes</small> (" + info.receivedTimestamp.substring(info.receivedTimestamp.indexOf(' ') + 1, info.receivedTimestamp.length()) + ")");
                      }
                    }
                  }
                  item.uniqueHosts = builder.toString();
                  item.uniqueHostsIsFiltered = true;
                }
              }
            }

            results.values = filteredItems;
            results.count = filteredItems.size();
          }

          return results;
        }

      @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
          final ArrayList<ListItem> localItems = (ArrayList<ListItem>) results.values;

          synchronized(listData) {
            clear();

            int count = localItems.size();
            for(int i = 0; i < count; i++) {
              add(localItems.get(i));
            }
          }

          preSortData();
          sortData();
          notifyDataSetChanged();
        }
    }

    @Override
      public CustomFilter getFilter() {
        if(filter == null) {
          filter = new CustomFilter();
        }
        return filter;
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

        ListItem item;
        synchronized(listData) {
          item = listData.get(position);
        }

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
        if(item.lastTimestamp.length() > 0)
          timestamp.setText("(" + item.lastTimestamp + ")");
        else
          timestamp.setText("");

        hosts = holder.getUniqueHosts();
        hosts.setText(Html.fromHtml(item.uniqueHosts), BufferType.SPANNABLE);

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
