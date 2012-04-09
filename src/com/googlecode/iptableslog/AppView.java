package com.googlecode.iptableslog;

import android.util.Log;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ExpandableListView;
import android.widget.BaseExpandableListAdapter;
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
  // groupData bound to adapter
  public ArrayList<GroupItem> groupData;
  // groupDataBuffer used to buffer incoming log entries and to hold original list data for filtering
  public ArrayList<GroupItem> groupDataBuffer;
  public boolean groupDataBufferIsDirty = false;
  private CustomAdapter adapter;
  public Sort preSortBy;
  public Sort sortBy;
  public GroupItem cachedSearchItem;
  private ListViewUpdater updater;
  public TextView statusText;

  public class GroupItem {
    protected ApplicationsTracker.AppEntry app;
    protected long totalPackets;
    protected long totalBytes;
    protected long lastTimestamp;
    protected String lastTimestampString;
    protected HashMap<String, ChildItem> childrenData;
    protected boolean childrenDataNeedsSort = false;
    protected boolean uniqueHostsIsFiltered = false;
    protected boolean uniqueHostsIsDirty = false;
    protected Spanned uniqueHostsSpanned;
    protected String uniqueHosts;
    protected ArrayList<ChildItem> filteredChildItems;
    protected ArrayList<PacketGraphItem> packetGraphBuffer;

    @Override
      public String toString() {
        return "(" + app.uidString + ") " + app.name;
      }
  }

  public class ChildItem {
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

    public String toString() {
      return sentAddressString + ":" + sentPortString + " -> " + receivedAddressString + ":" + receivedPortString;
    }
  }

  public void clear() {
    synchronized(groupData) {
      synchronized(groupDataBuffer) {
        for(GroupItem item : groupDataBuffer) {
          synchronized(item.childrenData) {
            List<String> list = new ArrayList<String>(item.childrenData.keySet());
            Iterator<String> itr = list.iterator();
            boolean has_host = false;

            while(itr.hasNext()) {
              String host = itr.next();
              ChildItem childData = item.childrenData.get(host);
              childData.packetGraphBuffer.clear();
            }

            item.childrenData.clear();
            item.filteredChildItems.clear();
            item.packetGraphBuffer.clear();
          }
        }

        groupDataBuffer.clear();
        groupData.clear();
        groupDataBufferIsDirty = false;
      }
    }

    getInstalledApps();
  }

  protected static class SortAppsByBytes implements Comparator<GroupItem> {
    public int compare(GroupItem o1, GroupItem o2) {
      return o1.totalBytes > o2.totalBytes ? -1 : (o1.totalBytes == o2.totalBytes) ? 0 : 1;
    }
  }

  protected static class SortAppsByPackets implements Comparator<GroupItem> {
    public int compare(GroupItem o1, GroupItem o2) {
      return o1.totalPackets > o2.totalPackets ? -1 : (o1.totalPackets == o2.totalPackets) ? 0 : 1;
    }
  }

  protected static class SortAppsByTimestamp implements Comparator<GroupItem> {
    public int compare(GroupItem o1, GroupItem o2) {
      return o1.lastTimestamp > o2.lastTimestamp ? -1 : (o1.lastTimestamp == o2.lastTimestamp) ? 0 : 1;
    }
  }

  protected static class SortAppsByName implements Comparator<GroupItem> {
    public int compare(GroupItem o1, GroupItem o2) {
      return o1.app.name.compareToIgnoreCase(o2.app.name);
    }
  }

  protected static class SortAppsByUid implements Comparator<GroupItem> {
    public int compare(GroupItem o1, GroupItem o2) {
      return o1.app.uid < o2.app.uid ? -1 : (o1.app.uid == o2.app.uid) ? 0 : 1;
    }
  }

  protected void preSortData() {
    Comparator<GroupItem> sortMethod;

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

    synchronized(groupData) {
      Collections.sort(groupData, sortMethod);
    }
  }

  protected void sortData() {
    Comparator<GroupItem> sortMethod;

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

    synchronized(groupData) {
      Collections.sort(groupData, sortMethod);
    }
  }

  public void refreshAdapter() {
    adapter.notifyDataSetChanged();
  }

  public void refreshHosts() {
    synchronized(groupDataBuffer) {
      for(GroupItem item : groupDataBuffer) {
        //buildUniqueHosts(item);
      }

      adapter.notifyDataSetChanged();
    }
  }

  public void refreshPorts() {
    synchronized(groupDataBuffer) {
      for(GroupItem item : groupDataBuffer) {
        //buildUniqueHosts(item);
      }

      adapter.notifyDataSetChanged();
    }
  }

  protected void getInstalledApps() {
    synchronized(groupDataBuffer) {
      synchronized(groupData) {
        groupData.clear();
        groupDataBuffer.clear();

        synchronized(ApplicationsTracker.installedAppsLock) {
          for(ApplicationsTracker.AppEntry app : ApplicationsTracker.installedApps) {
            if(IptablesLog.state != IptablesLog.State.RUNNING && IptablesLog.initRunner.running == false) {
              MyLog.d("[AppView] Initialization aborted");
              return;
            }

            GroupItem item = new GroupItem();
            item.app = app;
            item.lastTimestamp = 0;
            item.lastTimestampString = "";
            item.childrenData = new HashMap<String, ChildItem>();
            item.filteredChildItems = new ArrayList<ChildItem>();
            item.uniqueHosts = "";
            item.packetGraphBuffer = new ArrayList<PacketGraphItem>();
            groupData.add(item);
            groupDataBuffer.add(item);
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

        // groupDataBuffer must always be sorted by UID for binary search
        Collections.sort(groupDataBuffer, new SortAppsByUid());
      }
    }
  }

  protected void loadIcons() {
    if(IptablesLog.data == null) {
      new Thread("IconLoader") {
        public void run() {
          long nextUpdateTime = 0;

          int size;

          synchronized(groupDataBuffer) {
            size = groupDataBuffer.size();
          }

          for(int i = 0; i < size; i++) {
            GroupItem item;

            synchronized(groupDataBuffer) {
              item = groupDataBuffer.get(i);
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
        groupData = new ArrayList<GroupItem>();
        groupDataBuffer = new ArrayList<GroupItem>();
        cachedSearchItem = new GroupItem();
        cachedSearchItem.app = new ApplicationsTracker.AppEntry();
      } else {
        restoreData(IptablesLog.data);
      }

      adapter = new CustomAdapter();

      ExpandableListView lv = new ExpandableListView(this);
      lv.setAdapter(adapter);
      lv.setTextFilterEnabled(true);
      lv.setFastScrollEnabled(true);
      lv.setSmoothScrollbarEnabled(false);
      lv.setGroupIndicator(null);
      lv.setChildIndicator(null);
      //lv.setDividerHeight(0);
      //lv.setChildDivider(getResources().getDrawable(R.color.transparent));
      layout.addView(lv);
      setContentView(layout);
    }

  private class CustomOnItemClickListener implements OnItemClickListener {
    @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        GroupItem item = groupData.get(position);
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
    groupData = data.appViewGroupData;
    groupDataBuffer = data.appViewGroupDataBuffer;
    groupDataBufferIsDirty = data.appViewGroupDataBufferIsDirty;
    sortBy = data.appViewSortBy;
    preSortBy = data.appViewPreSortBy;
    cachedSearchItem = data.appViewCachedSearchItem;

    if(groupData == null) {
      groupData = new ArrayList<GroupItem>();
    }

    if(groupDataBuffer == null) {
      groupDataBuffer = new ArrayList<GroupItem>();
    }

    if(cachedSearchItem == null) {
      cachedSearchItem = new GroupItem();
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

    synchronized(groupDataBuffer) {
      MyLog.d("Binary searching...");
      index = Collections.binarySearch(groupDataBuffer, cachedSearchItem, new Comparator<GroupItem>() {
        public int compare(GroupItem o1, GroupItem o2) {
          //MyLog.d("Comparing " + o1.app.uid + " " + o1.app.name + " vs " + o2.app.uid + " " + o2.app.name);
          return o1.app.uid < o2.app.uid ? -1 : (o1.app.uid == o2.app.uid) ? 0 : 1;
        }
      });

      // binarySearch isn't guaranteed to return the first item of items with the same uid
      // so find the first item
      while(index > 0) {
        if(groupDataBuffer.get(index - 1).app.uid == uid) {
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

    synchronized(groupDataBuffer) {
      String src = entry.src + ":" + entry.spt;
      String dst = entry.dst + ":" + entry.dpt;

      // generally this will iterate once, but some apps may be grouped under the same uid
      while(true) {
        MyLog.d("finding first index: " + index);
        GroupItem item = groupDataBuffer.get(index);

        if(item.app.uid != entry.uid) {
          break;
        }

        groupDataBufferIsDirty = true;

        PacketGraphItem graphItem = new PacketGraphItem(entry.timestamp, entry.len);

        item.packetGraphBuffer.add(graphItem);
        item.totalPackets++;
        item.totalBytes += entry.len;
        item.lastTimestamp = entry.timestamp;

        ChildItem childData;

        // todo: make filtering out local IP a user preference
        if(!IptablesLog.localIpAddrs.contains(entry.src)) {
          synchronized(item.childrenData) {
            childData = item.childrenData.get(src);

            if(childData == null) {
              childData = new ChildItem();
              childData.packetGraphBuffer = new ArrayList<PacketGraphItem>();
            }

            childData.receivedPackets++;
            childData.receivedBytes += entry.len;
            childData.receivedTimestamp = entry.timestamp;
            childData.receivedTimestampString = "";

            MyLog.d("Added received packet " + entry.src + ":" + entry.spt + " --> " + entry.dst + ":" + entry.dpt + "; total: " + childData.receivedPackets + "; bytes: " + childData.receivedBytes);

            childData.receivedPort = entry.spt;
            childData.receivedPortString = String.valueOf(entry.spt);
            childData.receivedAddress = entry.src;
            childData.receivedAddressString = entry.src;

            childData.sentPort = entry.dpt;
            childData.sentPortString = String.valueOf(entry.dpt);
            childData.sentAddress = entry.dst;
            childData.sentAddressString = entry.dst;

            childData.packetGraphBuffer.add(graphItem);
            MyLog.d("graph receivedbytes " + childData.receivedBytes + " " + childData + " added " + graphItem);

            item.childrenData.put(src, childData);
            item.childrenDataNeedsSort = true;
          }
        }

        // todo: make filtering out local IP a user preference
        if(!IptablesLog.localIpAddrs.contains(entry.dst)) {
          synchronized(item.childrenData) {
            childData = item.childrenData.get(dst);

            if(childData == null) {
              childData = new ChildItem();
              childData.packetGraphBuffer = new ArrayList<PacketGraphItem>();
            }

            childData.sentPackets++;
            childData.sentBytes += entry.len;
            childData.sentTimestamp = entry.timestamp;
            childData.sentTimestampString = "";

            MyLog.d("Added sent packet " + entry.src + ":" + entry.spt + " --> " + entry.dst + ":" + entry.dpt + "; total: " + childData.sentPackets + "; bytes: " + childData.sentBytes);

            childData.sentPort = entry.dpt;
            childData.sentPortString = String.valueOf(entry.dpt);
            childData.sentAddress = entry.dst;
            childData.sentAddressString = entry.dst;

            childData.receivedPort = entry.spt;
            childData.receivedPortString = String.valueOf(entry.spt);
            childData.receivedAddress = entry.src;
            childData.receivedAddressString = entry.src;

            childData.packetGraphBuffer.add(graphItem);
            MyLog.d("graph sentbytes " + childData.sentBytes + " " + childData + " added " + graphItem);

            item.childrenData.put(dst, childData);
            item.childrenDataNeedsSort = true;
          }
        }

        index++;

        if(index >= groupDataBuffer.size()) {
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

  public void buildUniqueHosts(GroupItem item) {
    synchronized(item.childrenData) {
      List<String> list = new ArrayList<String>(item.childrenData.keySet());

      MyLog.d("Building host list for " + item);

      // todo: sort by user preference (bytes, timestamp, address, ports)
      Collections.sort(list);

      StringBuilder builder = new StringBuilder("Addrs:");
      Iterator<String> itr = list.iterator();
      boolean has_host = false;

      while(itr.hasNext()) {
        String host = itr.next();
        ChildItem childData = item.childrenData.get(host);

        MyLog.d("HostchildData entry for " + item + ": " + host);
        MyLog.d("Sent packets: " + childData.sentPackets + "; bytes: " + childData.sentBytes);
        MyLog.d("Received packets: " + childData.receivedPackets + "; bytes: " + childData.receivedBytes);
        MyLog.d("Total: " + (childData.sentBytes + childData.receivedBytes));

        String addressString = null;
        String portString = null;

        if(childData.receivedPackets > 0) {
          MyLog.d("Received address: " + childData.receivedAddress + ":" + childData.receivedPort);

          if(!IptablesLog.localIpAddrs.contains(childData.receivedAddress)) {
            if(IptablesLog.resolveHosts) {
              String resolved = IptablesLog.resolver.resolveAddress(childData.receivedAddress);

              if(resolved != null) {
                childData.receivedAddressString = resolved;
              } else {
                childData.receivedAddressString = childData.receivedAddress;
              }
            } else {
              childData.receivedAddressString = childData.receivedAddress;
            }

            if(IptablesLog.resolvePorts) {
              childData.receivedPortString = IptablesLog.resolver.resolveService(String.valueOf(childData.receivedPort));
            } else {
              childData.receivedPortString = String.valueOf(childData.receivedPort);
            }

            addressString = childData.receivedAddressString;
            portString = childData.receivedPortString;
          }
        }

        if(childData.sentPackets > 0) {
          MyLog.d("Sent address: " + childData.sentAddress + ":" + childData.sentPort);

          if(!IptablesLog.localIpAddrs.contains(childData.sentAddress)) {
            if(IptablesLog.resolveHosts) {
              String resolved = IptablesLog.resolver.resolveAddress(childData.sentAddress);

              if(resolved != null) {
                childData.sentAddressString = resolved;
              } else {
                childData.sentAddressString = childData.sentAddress;
              }
            } else {
              childData.sentAddressString = childData.sentAddress;
            }

            if(IptablesLog.resolvePorts) {
              childData.sentPortString = IptablesLog.resolver.resolveService(String.valueOf(childData.sentPort));
            } else {
              childData.sentPortString = String.valueOf(childData.sentPort);
            }

            addressString = childData.sentAddressString;
            portString = childData.sentPortString;
          }
        }

        if(addressString != null) {
          MyLog.d("Final address: " + addressString + ":" + portString);

          has_host = true;
          builder.append("<br>&nbsp;&nbsp;");
          builder.append("<u>" + addressString + ":" + portString  + "</u>");

          if(childData.sentPackets > 0) {
            if(childData.sentTimestampString.length() == 0) {
              childData.sentTimestampString = Timestamp.getTimestamp(childData.sentTimestamp);
            }

            builder.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;");
            builder.append("<small>Sent:</small> <b>" + childData.sentPackets + "</b> <small>packets,</small> <b>" + childData.sentBytes + "</b> <small>bytes</small> (" + childData.sentTimestampString.substring(childData.sentTimestampString.indexOf(' ') + 1, childData.sentTimestampString.length()) + ")");
          }

          if(childData.receivedPackets > 0) {
            if(childData.receivedTimestampString.length() == 0) {
              childData.receivedTimestampString = Timestamp.getTimestamp(childData.receivedTimestamp);
            }

            builder.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;");
            builder.append("<small>Recv:</small> <em>" + childData.receivedPackets + "</em> <small>packets,</small> <em>" + childData.receivedBytes + "</em> <small>bytes</small> (" + childData.receivedTimestampString.substring(childData.receivedTimestampString.indexOf(' ') + 1, childData.receivedTimestampString.length()) + ")");
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
        synchronized(groupData) {
          MyLog.d("AppViewListUpdater enter");
          groupData.clear();

          synchronized(groupDataBuffer) {
            // todo: find a way so that we don't have to go through every entry
            // in groupDataBuffer here (maybe use some sort of reference mapping)
            for(GroupItem item : groupDataBuffer) {
              if(item.childrenDataNeedsSort) {
                MyLog.d("Updating " + item);
                item.childrenDataNeedsSort = false;

                //buildUniqueHosts(item);
                groupData.add(item);
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
        if(groupDataBufferIsDirty == true) {
          runOnUiThread(runner);
          groupDataBufferIsDirty = false;
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

  private class CustomAdapter extends BaseExpandableListAdapter implements Filterable {
    LayoutInflater mInflater = (LayoutInflater) getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

    CustomFilter filter;

    private class CustomFilter extends Filter {
      ArrayList<GroupItem> originalItems = new ArrayList<GroupItem>();

      @Override
        protected FilterResults performFiltering(CharSequence constraint) {
          FilterResults results = new FilterResults();
          MyLog.d("[AppView] performFiltering");

          synchronized(groupDataBuffer) {
            originalItems.clear();
            originalItems.addAll(groupDataBuffer);
          }

          //  if(IptablesLog.filterTextInclude.length() == 0 && IptablesLog.filterTextExclude.length() == 0) {
          MyLog.d("[AppView] no constraint item count: " + originalItems.size());

          // undo uniqueHosts filtering
          for(GroupItem item : originalItems) {
            if(item.uniqueHostsIsFiltered) {
              item.uniqueHostsIsFiltered = false;
              //buildUniqueHosts(item);
            }
          }

          results.values = originalItems;
          results.count = originalItems.size();
          /*
             } else {
             ArrayList<GroupItem> filteredItems = new ArrayList<GroupItem>();
             ArrayList<GroupItem> localItems = new ArrayList<GroupItem>();
             localItems.addAll(originalItems);
             int count = localItems.size();

             MyLog.d("[AppView] item count: " + count);

             if(IptablesLog.filterTextIncludeList.size() == 0) {
             MyLog.d("[AppView] no include filter, adding all items");

             for(GroupItem item : localItems) {
             filteredItems.add(item);

             synchronized(item.childrenData) {
             List<String> list = new ArrayList<String>(item.childrenData.keySet());
          // todo: sort by user preference
          Collections.sort(list);
          Iterator<String> itr = list.iterator();

          item.filteredChildItems.clear();

          while(itr.hasNext()) {
          String host = itr.next();
          ChildItem childData = item.childrenData.get(host);
          MyLog.d("[AppView] adding filtered host " + childData);
          item.filteredChildItems.add(childData);
          }
             }
             }
             } else {
             if(IptablesLog.filterNameInclude
             || IptablesLog.filterUidInclude
             || IptablesLog.filterAddressInclude
             || IptablesLog.filterPortInclude) {
             for(int i = 0; i < count; i++) {
             GroupItem item = localItems.get(i);
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
          synchronized(item.childrenData) {
          List<String> list = new ArrayList<String>(item.childrenData.keySet());
          // todo: sort by user preference (bytes, timestamp, address, ports)
          Collections.sort(list);
          Iterator<String> itr = list.iterator();

          item.filteredChildItems.clear();

          while(itr.hasNext()) {
          String host = itr.next();
          MyLog.d("[AppView] testing " + host);
          ChildItem childData = item.childrenData.get(host);
          matched = false;

          for(String c : IptablesLog.filterTextIncludeList) {
          if((IptablesLog.filterAddressInclude && ((childData.sentPackets > 0 && childData.sentAddressString.toLowerCase().contains(c))
          || (childData.receivedPackets > 0 && childData.receivedAddressString.toLowerCase().contains(c))))
          || (IptablesLog.filterPortInclude && ((childData.sentPackets > 0 && childData.sentPortString.toLowerCase().equals(c))
                || (childData.receivedPackets > 0 && childData.receivedPortString.toLowerCase().equals(c))))) {
                  matched = true;
                }
          }

        if(matched) {
          if(!item_added) {
            MyLog.d("[AppView] adding filtered item " + item);
            filteredItems.add(item);
            item_added = true;
          }

          MyLog.d("[AppView] adding filtered host " + childData);
          item.filteredChildItems.add(childData);
        }
          }
          }
        } else {
          // no filtering for host/port, matches everything
          MyLog.d("[AppView] no filter for host/port; adding filtered item " + item);
          filteredItems.add(item);

          synchronized(item.childrenData) {
            List<String> list = new ArrayList<String>(item.childrenData.keySet());
            // todo: sort by user preference
            Collections.sort(list);
            Iterator<String> itr = list.iterator();

            item.filteredChildItems.clear();

            while(itr.hasNext()) {
              String host = itr.next();
              ChildItem childData = item.childrenData.get(host);
              MyLog.d("[AppView] adding filtered host " + childData);
              item.filteredChildItems.add(childData);
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
            GroupItem item = filteredItems.get(i);
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

            int hostchildData_count = item.filteredChildItems.size();

            for(int j = hostchildData_count - 1; j >= 0; j--) {
              ChildItem childData = item.filteredChildItems.get(j);

              matched = false;

              for(String c : IptablesLog.filterTextExcludeList) {
                if((IptablesLog.filterAddressExclude && ((childData.sentPackets > 0 && childData.sentAddressString.toLowerCase().contains(c))
                        || (childData.receivedPackets > 0 && childData.receivedAddressString.toLowerCase().contains(c))))
                    || (IptablesLog.filterPortExclude && ((childData.sentPackets > 0 && childData.sentPortString.toLowerCase().equals(c))
                        || (childData.receivedPackets > 0 && childData.receivedPortString.toLowerCase().equals(c))))) {
                  matched = true;
                        }
              }

              if(matched) {
                MyLog.d("[AppView] removing filtered host " + childData);
                item.filteredChildItems.remove(j);
              }
            }

            if(item.filteredChildItems.size() == 0) {
              MyLog.d("[AppView] removed all hosts, removing item from filter results");
              filteredItems.remove(i);
            }
          }
        }

        for(GroupItem item : filteredItems) {
          MyLog.d("[AppView] building addresses for " + item);
          StringBuilder builder = new StringBuilder("Addrs:");
          boolean has_host = false;

          for(ChildItem childData : item.filteredChildItems) {
            MyLog.d("[AppView] adding host " + childData);
            builder.append("<br>&nbsp;&nbsp;");

            String addressString = null;
            String portString = null;

            if(childData.sentPackets > 0 && !IptablesLog.localIpAddrs.contains(childData.sentAddress)) {
              addressString = childData.sentAddressString;
              portString = childData.sentPortString;
            }

            if(childData.receivedPackets > 0 && !IptablesLog.localIpAddrs.contains(childData.receivedAddress)) {
              addressString = childData.receivedAddressString;
              portString = childData.receivedPortString;
            }

            if(addressString != null) {
              has_host = true;
              builder.append("<u>" + addressString + ":" + portString + "</u>");

              if(childData.sentPackets > 0) {
                if(childData.sentTimestampString.length() == 0) {
                  childData.sentTimestampString = Timestamp.getTimestamp(childData.sentTimestamp);
                }

                builder.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;");
                builder.append("<small>Sent:</small> <b>" + childData.sentPackets + "</b> <small>packets,</small> <b>" + childData.sentBytes + "</b> <small>bytes</small> (" + childData.sentTimestampString.substring(childData.sentTimestampString.indexOf(' ') + 1, childData.sentTimestampString.length()) + ")");
              }

              if(childData.receivedPackets > 0) {
                if(childData.receivedTimestampString.length() == 0) {
                  childData.receivedTimestampString = Timestamp.getTimestamp(childData.receivedTimestamp);
                }

                builder.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;");
                builder.append("<small>Recv:</small> <em>" + childData.receivedPackets + "</em> <small>packets,</small> <em>" + childData.receivedBytes + "</em> <small>bytes</small> (" + childData.receivedTimestampString.substring(childData.receivedTimestampString.indexOf(' ') + 1, childData.receivedTimestampString.length()) + ")");
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
        */
          return results;
        }

      @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
          /*
             final ArrayList<GroupItem> localItems = (ArrayList<GroupItem>) results.values;

             if(localItems == null) {
             MyLog.d("[AppView] local items null, wtf");
             return;
             }

             synchronized(groupData) {
             groupData.clear();

             int count = localItems.size();

             for(int i = 0; i < count; i++) {
             groupData.add(localItems.get(i));
             }
             }

             preSortData();
             sortData();

             if(!IptablesLog.outputPaused) {
             notifyDataSetChanged();
             }
             */
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
      public Object getChild(int groupPosition, int childPosition) {
        Set<String> set = (Set<String>) groupData.get(groupPosition).childrenData.keySet();
        return groupData.get(groupPosition).childrenData.get(set.toArray()[childPosition]);
      }

    @Override
      public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
      }

    @Override
      public int getChildrenCount(int groupPosition) {
        return groupData.get(groupPosition).childrenData.size();
      }

    @Override
      public Object getGroup(int groupPosition) {
        return groupData.get(groupPosition);
      }

    @Override
      public int getGroupCount() {
        return groupData.size();
      }

    @Override
      public long getGroupId(int groupPosition) {
        return groupPosition;
      }

    @Override
      public boolean hasStableIds() {
        return true;
      }

    @Override
      public boolean isChildSelectable(int arg0, int arg1) {
        return true;
      }

    @Override
      public View getChildView(int groupPosition, int childPosition,
          boolean isLastChild, View convertView, ViewGroup parent) 
      {
        ChildViewHolder holder = null;

        TextView host;

        TextView sentPackets;
        TextView sentBytes;
        TextView sentTimestamp;

        TextView receivedPackets;
        TextView receivedBytes;
        TextView receivedTimestamp;

        ChildItem item;

        synchronized(groupData) {
          item = (ChildItem) getChild(groupPosition, childPosition);
        }

        if(item == null) {
          MyLog.d("child (" + groupPosition + "," + childPosition + ") not found");
          return null;
        }

        if(convertView == null) {
          convertView = mInflater.inflate(R.layout.hostitem, null);
          holder = new ChildViewHolder(convertView);
          convertView.setTag(holder);
        } else {
          holder = (ChildViewHolder) convertView.getTag();
        }

        host = holder.getHost();
        host.setText("lol");

        sentPackets = holder.getSentPackets();
        sentBytes = holder.getSentBytes();
        sentTimestamp = holder.getSentTimestamp();

        sentPackets.setText(String.valueOf(item.sentPackets));
        sentBytes.setText(String.valueOf(item.sentBytes));
        //sentTimestamp.setText("(" + item.sentTimestampString.substring(item.sentTimestampString.indexOf(' ') + 1, item.sentTimestampString.length()) + ")");

        receivedPackets = holder.getReceivedPackets();
        receivedBytes = holder.getReceivedBytes();
        receivedTimestamp = holder.getReceivedTimestamp();

        receivedPackets.setText(String.valueOf(item.receivedPackets));
        receivedBytes.setText(String.valueOf(item.receivedBytes));
        //receivedTimestamp.setText("(" + item.receivedTimestampString.substring(item.receivedTimestampString.indexOf(' ') + 1, item.receivedTimestampString.length()) + ")");

        return convertView;
      }

    @Override
      public View getGroupView(int groupPosition, boolean isExpanded,
          View convertView, ViewGroup parent)
      {
        GroupViewHolder holder = null;

        ImageView icon;
        TextView name;
        TextView packets;
        TextView bytes;
        TextView timestamp;
        TextView hosts;

        GroupItem item;

        synchronized(groupData) {
          item = groupData.get(groupPosition);
        }

        if(convertView == null) {
          convertView = mInflater.inflate(R.layout.appitem, null);
          holder = new GroupViewHolder(convertView);
          convertView.setTag(holder);
        } else {
          holder = (GroupViewHolder) convertView.getTag();
        }

        icon = holder.getIcon();
        icon.setImageDrawable(item.app.icon);

        name = holder.getName();
        name.setText("(" + item.app.uid + ")" + " " + item.app.name);

        packets = holder.getPackets();
        packets.setText("Packets: " + item.totalPackets);

        bytes = holder.getBytes();
        bytes.setText("Bytes: " + item.totalBytes);

        timestamp = holder.getTimestamp();

        // fixme: remove this soon
        if(item.lastTimestamp != 0) {
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

        return convertView;
      }
  }

  private class GroupViewHolder {
    private View mView;
    private ImageView mIcon = null;
    private TextView mName = null;
    private TextView mPackets = null;
    private TextView mBytes = null;
    private TextView mTimestamp = null;
    private TextView mUniqueHosts = null;

    public GroupViewHolder(View view) {
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
  }

  private class ChildViewHolder {
    private View mView;
    private TextView mHost = null;

    private TextView mSentPackets = null;
    private TextView mSentBytes = null;
    private TextView mSentTimestamp = null;

    private TextView mReceivedPackets = null;
    private TextView mReceivedBytes = null;
    private TextView mReceivedTimestamp = null;

    public ChildViewHolder(View view) {
      mView = view;
    }

    public TextView getHost() {
      if(mHost == null) {
        mHost = (TextView) mView.findViewById(R.id.hostName);
      }

      return mHost;
    }
    
    public TextView getSentPackets() {
      if(mSentPackets == null) {
        mSentPackets = (TextView) mView.findViewById(R.id.sentPackets);
      }

      return mSentPackets;
    }
    
    public TextView getSentBytes() {
      if(mSentBytes == null) {
        mSentBytes = (TextView) mView.findViewById(R.id.sentBytes);
      }

      return mSentBytes;
    }
    
    public TextView getSentTimestamp() {
      if(mSentTimestamp == null) {
        mSentTimestamp = (TextView) mView.findViewById(R.id.sentTimestamp);
      }

      return mSentTimestamp;
    }

    public TextView getReceivedPackets() {
      if(mReceivedPackets == null) {
        mReceivedPackets = (TextView) mView.findViewById(R.id.receivedPackets);
      }

      return mReceivedPackets;
    }
    
    public TextView getReceivedBytes() {
      if(mReceivedBytes == null) {
        mReceivedBytes = (TextView) mView.findViewById(R.id.receivedBytes);
      }

      return mReceivedBytes;
    }
    
    public TextView getReceivedTimestamp() {
      if(mReceivedTimestamp == null) {
        mReceivedTimestamp = (TextView) mView.findViewById(R.id.receivedTimestamp);
      }

      return mReceivedTimestamp;
    }
  }
}
