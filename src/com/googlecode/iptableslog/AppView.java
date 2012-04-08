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
import android.content.Intent;
import android.view.LayoutInflater;
import android.graphics.drawable.Drawable;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.os.SystemClock;
import android.text.Html;
import android.text.Spanned;
import android.widget.TextView.BufferType;
import android.util.TypedValue;

import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class AppView extends Activity {
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
  public TextView statusText;

  public class HostInfo {
    protected int sentPackets;
    protected int sentBytes;
    protected long sentTimestamp;
    protected String sentTimestampString;
    protected int sentPort;
    protected String sentPortString;
    protected String sentAddress;
    protected String sentAddressString;

    protected int receivedPackets;
    protected int receivedBytes;
    protected long receivedTimestamp;
    protected String receivedTimestampString;
    protected int receivedPort;
    protected String receivedPortString;
    protected String receivedAddress;
    protected String receivedAddressString;

    protected ArrayList<PacketGraphItem> packetGraphBuffer;
    // protected ArrayList<PacketGraphItem> packetGraphData;

    public String toString() {
      return sentAddressString + ":" + sentPortString + " -> " + receivedAddressString + ":" + receivedPortString;
    }
  }

  public class ListItem {
    protected ApplicationsTracker.AppEntry app;
    protected long totalPackets;
    protected long totalBytes;
    protected long lastTimestamp;
    protected String lastTimestampString;
    protected HashMap<String, HostInfo> uniqueHostsList;
    protected boolean uniqueHostsListNeedsSort = false;
    protected boolean uniqueHostsIsFiltered = false;
    protected boolean uniqueHostsIsDirty = false;
    protected Spanned uniqueHostsSpanned;
    protected String uniqueHosts;
    protected ArrayList<HostInfo> filteredHostInfos;
    protected ArrayList<PacketGraphItem> packetGraphBuffer;

    @Override
      public String toString() {
        return "(" + app.uidString + ") " + app.name;
      }
  }

  public void clear() {
    synchronized(listData) {
      synchronized(listDataBuffer) {
        for(ListItem item : listDataBuffer) {
          synchronized(item.uniqueHostsList) {
            List<String> list = new ArrayList<String>(item.uniqueHostsList.keySet());
            Iterator<String> itr = list.iterator();
            boolean has_host = false;

            while(itr.hasNext()) {
              String host = itr.next();
              HostInfo info = item.uniqueHostsList.get(host);
              info.packetGraphBuffer.clear();
            }

            item.uniqueHostsList.clear();
            item.filteredHostInfos.clear();
            item.packetGraphBuffer.clear();
          }
        }

        listDataBuffer.clear();
        listData.clear();
        listDataBufferIsDirty = false;
      }
    }

    getInstalledApps();
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
      return o1.lastTimestamp > o2.lastTimestamp ? -1 : (o1.lastTimestamp == o2.lastTimestamp) ? 0 : 1;
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

  public void refreshAdapter() {
    adapter.notifyDataSetChanged();
  }

  public void refreshHosts() {
    synchronized(listDataBuffer) {
      for(ListItem item : listDataBuffer) {
        buildUniqueHosts(item);
      }

      adapter.notifyDataSetChanged();
    }
  }

  public void refreshPorts() {
    synchronized(listDataBuffer) {
      for(ListItem item : listDataBuffer) {
        buildUniqueHosts(item);
      }

      adapter.notifyDataSetChanged();
    }
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
            item.lastTimestamp = 0;
            item.lastTimestampString = "";
            item.uniqueHostsList = new HashMap<String, HostInfo>();
            item.filteredHostInfos = new ArrayList<HostInfo>();
            item.uniqueHosts = "";
            item.packetGraphBuffer = new ArrayList<PacketGraphItem>();
            listData.add(item);
            listDataBuffer.add(item);
          }
        }

        runOnUiThread(new Runnable() {
          public void run() {
            preSortData();

            // apply filter if there is one set
            //if(IptablesLog.filterText.length() > 0) {
            setFilter("");
            //}

            if(!IptablesLog.outputPaused) {
              adapter.notifyDataSetChanged();
            }
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

            if(item.app.packageName == null) {
              continue;
            }

            try {
              MyLog.d("Loading icon for " + item.app.packageName + " (" + item.app.name + ", " + item.app.uid + ")");
              Drawable icon = getPackageManager().getApplicationIcon(item.app.packageName);
              item.app.icon = icon;

              // refresh adapter to display icons once every second while still loading icons
              // (once few seconds instead of immediately after each icon prevents UI lag)
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
                    if(!IptablesLog.outputPaused) {
                      adapter.notifyDataSetChanged();
                    }
                  }
                });
              }
            } catch(Exception e) {
              Log.d("IptablesLog", "Failure to load icon for " + item.app.packageName + " (" + item.app.name + ", " + item.app.uid + ")", e);
            }
          }

          // refresh adapter to display icons
          runOnUiThread(new Runnable() {
            public void run() {
              preSortData();
              sortData();
              setFilter("");

              if(!IptablesLog.outputPaused) {
                adapter.notifyDataSetChanged();
              }

              IptablesLog.logView.refreshIcons();
            }
          });
        }
      } .start();
    }
  }

  /** Called when the activity is first created. */
  @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      MyLog.d("AppView created");

      sortBy = IptablesLog.settings.getSortBy();
      MyLog.d("Sort-by loaded from settings: " + sortBy);

      preSortBy = IptablesLog.settings.getPreSortBy();
      MyLog.d("Pre-sort-by loaded from settings: " + preSortBy);

      LinearLayout layout = new LinearLayout(this);
      layout.setOrientation(LinearLayout.VERTICAL);

      statusText = new TextView(this);
      layout.addView(statusText);

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
      lv.setOnItemClickListener(new CustomOnItemClickListener());
      layout.addView(lv);
      setContentView(layout);
    }

  private class CustomOnItemClickListener implements OnItemClickListener {
    @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ListItem item = listData.get(position);
        startActivity(new Intent(getApplicationContext(), AppTimelineGraph.class)
            .putExtra("app_uid", item.app.uidString));
      }
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

    if(listData == null) {
      listData = new ArrayList<ListItem>();
    }

    if(listDataBuffer == null) {
      listDataBuffer = new ArrayList<ListItem>();
    }

    if(cachedSearchItem == null) {
      cachedSearchItem = new ListItem();
    }

    if(cachedSearchItem.app == null) {
      cachedSearchItem.app = new ApplicationsTracker.AppEntry();
    }

    if(sortBy == null) {
      sortBy = IptablesLog.settings.getSortBy();
      MyLog.d("[restoreData] Sort-by loaded from settings: " + sortBy);
    }

    if(preSortBy == null) {
      preSortBy = IptablesLog.settings.getPreSortBy();
      MyLog.d("[restoreData] Pre-sort-by loaded from settings: " + preSortBy);
    }
  }

  public int getItemByAppUid(int uid) {
    cachedSearchItem.app.uid = uid;

    int index;

    synchronized(listDataBuffer) {
      MyLog.d("Binary searching...");
      index = Collections.binarySearch(listDataBuffer, cachedSearchItem, new Comparator<ListItem>() {
        public int compare(ListItem o1, ListItem o2) {
          //MyLog.d("Comparing " + o1.app.uid + " " + o1.app.name + " vs " + o2.app.uid + " " + o2.app.name);
          return o1.app.uid < o2.app.uid ? -1 : (o1.app.uid == o2.app.uid) ? 0 : 1;
        }
      });

      // binarySearch isn't guaranteed to return the first item of items with the same uid
      // so find the first item
      while(index > 0) {
        if(listDataBuffer.get(index - 1).app.uid == uid) {
          index--;
        }
        else {
          break;
        }
      }
    }

    MyLog.d("Search done, first: " + index);
    return index;
  }

  public void onNewLogEntry(final LogEntry entry) {
    MyLog.d("AppView: NewLogEntry: [" + entry.uid + "] " + entry.src + ":" + entry.spt + " --> " + entry.dst + ":" + entry.dpt + " [" + entry.len + "]");

    int index = getItemByAppUid(entry.uid);

    if(index < 0) {
      MyLog.d("No app entry");
      return;
    }

    synchronized(listDataBuffer) {
      String src = entry.src + ":" + entry.spt;
      String dst = entry.dst + ":" + entry.dpt;

      // generally this will iterate once, but some apps may be grouped under the same uid
      while(true) {
        MyLog.d("finding first index: " + index);
        ListItem item = listDataBuffer.get(index);

        if(item.app.uid != entry.uid) {
          break;
        }

        listDataBufferIsDirty = true;

        PacketGraphItem graphItem = new PacketGraphItem(entry.timestamp, entry.len);

        item.packetGraphBuffer.add(graphItem);
        item.totalPackets++;
        item.totalBytes += entry.len;
        item.lastTimestamp = entry.timestamp;

        HostInfo info;

        // todo: make filtering out local IP a user preference
        if(!IptablesLog.localIpAddrs.contains(entry.src)) {
          synchronized(item.uniqueHostsList) {
            info = item.uniqueHostsList.get(src);

            if(info == null) {
              info = new HostInfo();
              info.packetGraphBuffer = new ArrayList<PacketGraphItem>();
            }

            info.receivedPackets++;
            info.receivedBytes += entry.len;
            info.receivedTimestamp = entry.timestamp;
            info.receivedTimestampString = "";

            MyLog.d("Added received packet " + entry.src + ":" + entry.spt + " --> " + entry.dst + ":" + entry.dpt + "; total: " + info.receivedPackets + "; bytes: " + info.receivedBytes);

            info.receivedPort = entry.spt;
            info.receivedPortString = String.valueOf(entry.spt);
            info.receivedAddress = entry.src;
            info.receivedAddressString = entry.src;

            info.sentPort = entry.dpt;
            info.sentPortString = String.valueOf(entry.dpt);
            info.sentAddress = entry.dst;
            info.sentAddressString = entry.dst;

            info.packetGraphBuffer.add(graphItem);
            MyLog.d("graph receivedbytes " + info.receivedBytes + " " + info + " added " + graphItem);

            item.uniqueHostsList.put(src, info);
            item.uniqueHostsListNeedsSort = true;
          }
        }

        // todo: make filtering out local IP a user preference
        if(!IptablesLog.localIpAddrs.contains(entry.dst)) {
          synchronized(item.uniqueHostsList) {
            info = item.uniqueHostsList.get(dst);

            if(info == null) {
              info = new HostInfo();
              info.packetGraphBuffer = new ArrayList<PacketGraphItem>();
            }

            info.sentPackets++;
            info.sentBytes += entry.len;
            info.sentTimestamp = entry.timestamp;
            info.sentTimestampString = "";

            MyLog.d("Added sent packet " + entry.src + ":" + entry.spt + " --> " + entry.dst + ":" + entry.dpt + "; total: " + info.sentPackets + "; bytes: " + info.sentBytes);

            info.sentPort = entry.dpt;
            info.sentPortString = String.valueOf(entry.dpt);
            info.sentAddress = entry.dst;
            info.sentAddressString = entry.dst;

            info.receivedPort = entry.spt;
            info.receivedPortString = String.valueOf(entry.spt);
            info.receivedAddress = entry.src;
            info.receivedAddressString = entry.src;

            info.packetGraphBuffer.add(graphItem);
            MyLog.d("graph sentbytes " + info.sentBytes + " " + info + " added " + graphItem);

            item.uniqueHostsList.put(dst, info);
            item.uniqueHostsListNeedsSort = true;
          }
        }

        index++;

        if(index >= listDataBuffer.size()) {
          break;
        }
      }
    }
  }

  public void startUpdater() {
    updater = new ListViewUpdater();
    new Thread(updater, "AppViewUpdater").start();
  }

  public void stopUpdater() {
    if(updater != null) {
      updater.stop();
    }
  }

  public void buildUniqueHosts(ListItem item) {
    synchronized(item.uniqueHostsList) {
      List<String> list = new ArrayList<String>(item.uniqueHostsList.keySet());

      MyLog.d("Building host list for " + item);

      // todo: sort by user preference (bytes, timestamp, address, ports)
      Collections.sort(list);

      StringBuilder builder = new StringBuilder("Addrs:");
      Iterator<String> itr = list.iterator();
      boolean has_host = false;

      while(itr.hasNext()) {
        String host = itr.next();
        HostInfo info = item.uniqueHostsList.get(host);

        MyLog.d("Hostinfo entry for " + item + ": " + host);
        MyLog.d("Sent packets: " + info.sentPackets + "; bytes: " + info.sentBytes);
        MyLog.d("Received packets: " + info.receivedPackets + "; bytes: " + info.receivedBytes);
        MyLog.d("Total: " + (info.sentBytes + info.receivedBytes));

        String addressString = null;
        String portString = null;

        if(info.receivedPackets > 0) {
          MyLog.d("Received address: " + info.receivedAddress + ":" + info.receivedPort);

          if(!IptablesLog.localIpAddrs.contains(info.receivedAddress)) {
            if(IptablesLog.resolveHosts) {
              info.receivedAddressString = IptablesLog.resolver.resolveAddress(info.receivedAddress);
            } else {
              info.receivedAddressString = info.receivedAddress;
            }

            if(IptablesLog.resolvePorts) {
              info.receivedPortString = IptablesLog.resolver.resolveService(String.valueOf(info.receivedPort));
            } else {
              info.receivedPortString = String.valueOf(info.receivedPort);
            }

            addressString = info.receivedAddressString;
            portString = info.receivedPortString;
          }
        }

        if(info.sentPackets > 0) {
          MyLog.d("Sent address: " + info.sentAddress + ":" + info.sentPort);

          if(!IptablesLog.localIpAddrs.contains(info.sentAddress)) {
            if(IptablesLog.resolveHosts) {
              info.sentAddressString = IptablesLog.resolver.resolveAddress(info.sentAddress);
            } else {
              info.sentAddressString = info.sentAddress;
            }

            if(IptablesLog.resolvePorts) {
              info.sentPortString = IptablesLog.resolver.resolveService(String.valueOf(info.sentPort));
            } else {
              info.sentPortString = String.valueOf(info.sentPort);
            }

            addressString = info.sentAddressString;
            portString = info.sentPortString;
          }
        }

        if(addressString != null) {
          MyLog.d("Final address: " + addressString + ":" + portString);

          has_host = true;
          builder.append("<br>&nbsp;&nbsp;");
          builder.append("<u>" + addressString + ":" + portString  + "</u>");

          if(info.sentPackets > 0) {
            if(info.sentTimestampString.length() == 0) {
              info.sentTimestampString = Timestamp.getTimestamp(info.sentTimestamp);
            }

            builder.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;");
            builder.append("<small>Sent:</small> <b>" + info.sentPackets + "</b> <small>packets,</small> <b>" + info.sentBytes + "</b> <small>bytes</small> (" + info.sentTimestampString.substring(info.sentTimestampString.indexOf(' ') + 1, info.sentTimestampString.length()) + ")");
          }

          if(info.receivedPackets > 0) {
            if(info.receivedTimestampString.length() == 0) {
              info.receivedTimestampString = Timestamp.getTimestamp(info.receivedTimestamp);
            }

            builder.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;");
            builder.append("<small>Recv:</small> <em>" + info.receivedPackets + "</em> <small>packets,</small> <em>" + info.receivedBytes + "</em> <small>bytes</small> (" + info.receivedTimestampString.substring(info.receivedTimestampString.indexOf(' ') + 1, info.receivedTimestampString.length()) + ")");
          }
        }
      }

      if(has_host) {
        item.uniqueHosts = builder.toString();
        item.uniqueHostsIsDirty = true;
      }
    }
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

                buildUniqueHosts(item);
                listData.add(item);
              }
            }
          }

          preSortData();
          sortData();

          // apply filter if there is one set
          //if(IptablesLog.filterText.length() > 0) {
          setFilter("");
          //}

          if(!IptablesLog.outputPaused) {
            adapter.notifyDataSetChanged();
          }
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

        try {
          Thread.sleep(5000);
        } catch(Exception e) {
          Log.d("IptablesLog", "AppViewListUpdater", e);
        }
      }

      MyLog.d("Stopped AppView updater " + this);
    }
  }

  public void setFilter(CharSequence s) {
    MyLog.d("[AppView] setFilter(" + s + ")");
    adapter.getFilter().filter(s);
  }

  private class CustomAdapter extends ArrayAdapter<ListItem> { /* implements Filterable */
    LayoutInflater mInflater = (LayoutInflater) getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    CustomFilter filter;
    ArrayList<ListItem> originalItems = new ArrayList<ListItem>();

    public CustomAdapter(Context context, int resource, List<ListItem> objects) {
      super(context, resource, objects);
    }

    private class CustomFilter extends Filter {
      @Override
        protected FilterResults performFiltering(CharSequence constraint) {
          FilterResults results = new FilterResults();
          MyLog.d("[AppView] performFiltering");

          synchronized(listDataBuffer) {
            originalItems.clear();
            originalItems.addAll(listDataBuffer);
          }

          if(IptablesLog.filterTextInclude.length() == 0 && IptablesLog.filterTextExclude.length() == 0) {
            MyLog.d("[AppView] no constraint item count: " + originalItems.size());

            // undo uniqueHosts filtering
            for(ListItem item : originalItems) {
              if(item.uniqueHostsIsFiltered) {
                item.uniqueHostsIsFiltered = false;
                buildUniqueHosts(item);
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

            if(IptablesLog.filterTextIncludeList.size() == 0) {
              MyLog.d("[AppView] no include filter, adding all items");

              for(ListItem item : localItems) {
                filteredItems.add(item);

                synchronized(item.uniqueHostsList) {
                  List<String> list = new ArrayList<String>(item.uniqueHostsList.keySet());
                  // todo: sort by user preference
                  Collections.sort(list);
                  Iterator<String> itr = list.iterator();

                  item.filteredHostInfos.clear();

                  while(itr.hasNext()) {
                    String host = itr.next();
                    HostInfo info = item.uniqueHostsList.get(host);
                    MyLog.d("[AppView] adding filtered host " + info);
                    item.filteredHostInfos.add(info);
                  }
                }
              }
            } else {
              if(IptablesLog.filterNameInclude
                  || IptablesLog.filterUidInclude
                  || IptablesLog.filterAddressInclude
                  || IptablesLog.filterPortInclude) {
                for(int i = 0; i < count; i++) {
                  ListItem item = localItems.get(i);
                  MyLog.d("[AppView] testing filtered item " + item + "; includes: [" + IptablesLog.filterTextInclude + "]");

                  boolean item_added = false;
                  boolean matched = true;

                  for(String c : IptablesLog.filterTextIncludeList) {
                    if((IptablesLog.filterNameInclude && !item.app.nameLowerCase.contains(c))
                        || (IptablesLog.filterUidInclude && !item.app.uidString.equals(c))) {
                      matched = false;
                      break;
                        }
                  }

                  if(matched) {
                    // test filter against address/port
                    if(IptablesLog.filterAddressInclude || IptablesLog.filterPortInclude) {
                      synchronized(item.uniqueHostsList) {
                        List<String> list = new ArrayList<String>(item.uniqueHostsList.keySet());
                        // todo: sort by user preference (bytes, timestamp, address, ports)
                        Collections.sort(list);
                        Iterator<String> itr = list.iterator();

                        item.filteredHostInfos.clear();

                        while(itr.hasNext()) {
                          String host = itr.next();
                          MyLog.d("[AppView] testing " + host);
                          HostInfo info = item.uniqueHostsList.get(host);
                          matched = false;

                          for(String c : IptablesLog.filterTextIncludeList) {
                            if((IptablesLog.filterAddressInclude && ((info.sentPackets > 0 && info.sentAddressString.toLowerCase().contains(c))
                                    || (info.receivedPackets > 0 && info.receivedAddressString.toLowerCase().contains(c))))
                                || (IptablesLog.filterPortInclude && ((info.sentPackets > 0 && info.sentPortString.toLowerCase().equals(c))
                                    || (info.receivedPackets > 0 && info.receivedPortString.toLowerCase().equals(c))))) {
                              matched = true;
                                    }
                          }

                          if(matched) {
                            if(!item_added) {
                              MyLog.d("[AppView] adding filtered item " + item);
                              filteredItems.add(item);
                              item_added = true;
                            }

                            MyLog.d("[AppView] adding filtered host " + info);
                            item.filteredHostInfos.add(info);
                          }
                        }
                      }
                    } else {
                      // no filtering for host/port, matches everything
                      MyLog.d("[AppView] no filter for host/port; adding filtered item " + item);
                      filteredItems.add(item);

                      synchronized(item.uniqueHostsList) {
                        List<String> list = new ArrayList<String>(item.uniqueHostsList.keySet());
                        // todo: sort by user preference
                        Collections.sort(list);
                        Iterator<String> itr = list.iterator();

                        item.filteredHostInfos.clear();

                        while(itr.hasNext()) {
                          String host = itr.next();
                          HostInfo info = item.uniqueHostsList.get(host);
                          MyLog.d("[AppView] adding filtered host " + info);
                          item.filteredHostInfos.add(info);
                        }
                      }
                    }
                  }
                }
                  }
            }

            if(IptablesLog.filterTextExcludeList.size() > 0) {
              count = filteredItems.size();

              for(int i = count - 1; i >= 0; i--) {
                ListItem item = filteredItems.get(i);
                MyLog.d("[AppView] testing filtered item: " + i + " " + item + "; excludes: [" + IptablesLog.filterTextExclude + "]");

                boolean matched = false;

                for(String c : IptablesLog.filterTextExcludeList) {
                  if((IptablesLog.filterNameExclude && item.app.nameLowerCase.contains(c))
                      || IptablesLog.filterUidExclude && item.app.uidString.equals(c)) {
                    matched = true;
                      }
                }

                if(matched) {
                  MyLog.d("[AppView] removing filtered item: " + item);
                  filteredItems.remove(i);
                  continue;
                }

                int hostinfo_count = item.filteredHostInfos.size();

                for(int j = hostinfo_count - 1; j >= 0; j--) {
                  HostInfo info = item.filteredHostInfos.get(j);

                  matched = false;

                  for(String c : IptablesLog.filterTextExcludeList) {
                    if((IptablesLog.filterAddressExclude && ((info.sentPackets > 0 && info.sentAddressString.toLowerCase().contains(c))
                            || (info.receivedPackets > 0 && info.receivedAddressString.toLowerCase().contains(c))))
                        || (IptablesLog.filterPortExclude && ((info.sentPackets > 0 && info.sentPortString.toLowerCase().equals(c))
                            || (info.receivedPackets > 0 && info.receivedPortString.toLowerCase().equals(c))))) {
                      matched = true;
                            }
                  }

                  if(matched) {
                    MyLog.d("[AppView] removing filtered host " + info);
                    item.filteredHostInfos.remove(j);
                  }
                }

                if(item.filteredHostInfos.size() == 0) {
                  MyLog.d("[AppView] removed all hosts, removing item from filter results");
                  filteredItems.remove(i);
                }
              }
            }

            for(ListItem item : filteredItems) {
              MyLog.d("[AppView] building addresses for " + item);
              StringBuilder builder = new StringBuilder("Addrs:");
              boolean has_host = false;

              for(HostInfo info : item.filteredHostInfos) {
                MyLog.d("[AppView] adding host " + info);
                builder.append("<br>&nbsp;&nbsp;");

                String addressString = null;
                String portString = null;

                if(info.sentPackets > 0 && !IptablesLog.localIpAddrs.contains(info.sentAddress)) {
                  addressString = info.sentAddressString;
                  portString = info.sentPortString;
                }

                if(info.receivedPackets > 0 && !IptablesLog.localIpAddrs.contains(info.receivedAddress)) {
                  addressString = info.receivedAddressString;
                  portString = info.receivedPortString;
                }

                if(addressString != null) {
                  has_host = true;
                  builder.append("<u>" + addressString + ":" + portString + "</u>");

                  if(info.sentPackets > 0) {
                    if(info.sentTimestampString.length() == 0) {
                      info.sentTimestampString = Timestamp.getTimestamp(info.sentTimestamp);
                    }

                    builder.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;");
                    builder.append("<small>Sent:</small> <b>" + info.sentPackets + "</b> <small>packets,</small> <b>" + info.sentBytes + "</b> <small>bytes</small> (" + info.sentTimestampString.substring(info.sentTimestampString.indexOf(' ') + 1, info.sentTimestampString.length()) + ")");
                  }

                  if(info.receivedPackets > 0) {
                    if(info.receivedTimestampString.length() == 0) {
                      info.receivedTimestampString = Timestamp.getTimestamp(info.receivedTimestamp);
                    }

                    builder.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;");
                    builder.append("<small>Recv:</small> <em>" + info.receivedPackets + "</em> <small>packets,</small> <em>" + info.receivedBytes + "</em> <small>bytes</small> (" + info.receivedTimestampString.substring(info.receivedTimestampString.indexOf(' ') + 1, info.receivedTimestampString.length()) + ")");
                  }
                }
              }

              if(has_host) {
                item.uniqueHosts = builder.toString();
                item.uniqueHostsIsDirty = true;
                item.uniqueHostsIsFiltered = true;
              }
            }

            results.values = filteredItems;
            results.count = filteredItems.size();
          }

          MyLog.d("returning " + results.count + " results");
          return results;
        }

      @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
          final ArrayList<ListItem> localItems = (ArrayList<ListItem>) results.values;

          if(localItems == null) {
            MyLog.d("[AppView] local items null, wtf");
            return;
          }

          synchronized(listData) {
            listData.clear();

            int count = localItems.size();

            for(int i = 0; i < count; i++) {
              listData.add(localItems.get(i));
            }
          }

          preSortData();
          sortData();

          if(!IptablesLog.outputPaused) {
            notifyDataSetChanged();
          }
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

        if(item.lastTimestampString.length() == 0 && item.lastTimestamp != 0) {
          MyLog.d("[appview] Setting timestamp for " + item);
          item.lastTimestampString = Timestamp.getTimestamp(item.lastTimestamp);
        }

        if(item.lastTimestampString.length() > 0) {
          timestamp.setText("(" + item.lastTimestampString + ")");
          timestamp.setVisibility(View.VISIBLE);
        }
        else {
          timestamp.setText("");
          timestamp.setVisibility(View.GONE);
        }

        hosts = holder.getUniqueHosts();

        if(item.uniqueHostsIsDirty == true) {
          item.uniqueHostsSpanned = Html.fromHtml(item.uniqueHosts);
          item.uniqueHostsIsDirty = false;
        }

        if(item.uniqueHosts.length() == 0) {
          hosts.setVisibility(View.GONE);
        } else {
          hosts.setText(item.uniqueHostsSpanned);
          hosts.setVisibility(View.VISIBLE);
        }

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
