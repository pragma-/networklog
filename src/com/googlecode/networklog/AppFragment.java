/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import android.util.Log;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
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
import android.os.Parcelable;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.net.Uri;

/* newer API 11 clipboard unsupported on older APIs
import android.content.ClipboardManager;
import android.content.ClipData;
*/

/* use older clipboard API to support older devices */
import android.text.ClipboardManager;

import android.support.v4.app.Fragment;

import java.lang.StringBuilder;
import java.util.Arrays;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class AppFragment extends Fragment {
  // groupData bound to adapter, and filtered
  public ArrayList<GroupItem> groupData;
  // groupDataBuffer used to buffer incoming log entries and to hold original list data for filtering
  public ArrayList<GroupItem> groupDataBuffer;
  public boolean groupDataBufferIsDirty = false;
  public boolean needsRefresh = false;
  private ExpandableListView listView;
  private CustomAdapter adapter;
  public Sort preSortBy;
  public Sort sortBy;
  public boolean roundValues;
  public GroupItem cachedSearchItem;
  private ListViewUpdater updater;
  // remember last index return by getItemByAppUid to optimize-out call to binarySearch
  int lastGetItemByAppUidIndex = -1;
  private NetworkLog parent = null;
  private boolean gotInstalledApps = false;
  private boolean doNotRefresh = false;

  public class GroupItem {
    protected ApplicationsTracker.AppEntry app;
    protected long uploadThroughput;
    protected long downloadThroughput;
    protected long totalThroughput;
    protected String throughputString;
    protected long totalPackets;
    protected long totalBytes;
    protected long lastTimestamp;
    // childrenData bound to adapter, holds original list of children
    protected HashMap<String, ChildItem> childrenData;
    // holds filtered list of children
    // used in place of childrenData in getView, if non-empty
    protected HashMap<String, ChildItem> childrenDataFiltered;
    // holds sorted keys into childrenData
    protected String[] childrenDataSorted;
    protected boolean childrenNeedSort = false;
    protected boolean childrenAreFiltered = false;
    protected boolean childrenAreDirty = false;
    protected boolean isExpanded = false;

    @Override
      public String toString() {
        return "(" + app.uidString + ") " + app.name;
      }
  }

  public class ChildItem {
    protected String proto; // protocol (udp, tcp, igmp, icmp, etc)
    protected int sentPackets;
    protected int sentBytes;
    protected long sentTimestamp;
    protected int sentPort;
    protected String sentAddress;
    protected String out; // interface (rmnet, wifi, etc)

    protected int receivedPackets;
    protected int receivedBytes;
    protected long receivedTimestamp;
    protected int receivedPort;
    protected String receivedAddress;
    protected String in; // interface (rmnet, wifi, etc)

    public String toString() {
      // todo: resolver here
      return sentAddress + ":" + sentPort + " -> " + receivedAddress + ":" + receivedPort;
    }
  }

  public void clear() {
    synchronized(groupData) {
      synchronized(groupDataBuffer) {
        for(GroupItem item : groupDataBuffer) {
          synchronized(item.childrenData) {
            List<String> list = new ArrayList<String>(item.childrenData.keySet());
            Iterator<String> itr = list.iterator();

            while(itr.hasNext()) {
              String host = itr.next();
              ChildItem childData = item.childrenData.get(host);
            }

            item.childrenData.clear();
            item.childrenDataFiltered.clear();
            item.childrenDataSorted = null;
            item.childrenAreFiltered = false;
          }
        }

        groupDataBuffer.clear();
        groupData.clear();
        groupDataBufferIsDirty = false;
      }
    }

    getInstalledApps(false);
    lastGetItemByAppUidIndex = -1;
  }

  protected static class SortAppsByBytes implements Comparator<GroupItem> {
    public int compare(GroupItem o1, GroupItem o2) {
      return o1.totalBytes > o2.totalBytes ? -1 : (o1.totalBytes == o2.totalBytes) ? 0 : 1;
    }
  }

  protected static class SortAppsByThroughput implements Comparator<GroupItem> {
    public int compare(GroupItem o1, GroupItem o2) {
      return o1.totalThroughput > o2.totalThroughput ? -1 : (o1.totalThroughput == o2.totalThroughput) ? 0 : 1;
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

  protected static class SortChildrenByBytes implements Comparator<String> {
    GroupItem item;

    public SortChildrenByBytes(GroupItem item) {
      this.item = item;
    }

    public int compare(String o1, String o2) {
      ChildItem c1;
      ChildItem c2;

      if(item.childrenAreFiltered) {
        c1 = item.childrenDataFiltered.get(o1);
        c2 = item.childrenDataFiltered.get(o2);
      } else {
        c1 = item.childrenData.get(o1);
        c2 = item.childrenData.get(o2);
      }

      long totalBytes1 = c1.sentBytes + c1.receivedBytes;
      long totalBytes2 = c2.sentBytes + c2.receivedBytes;

      return totalBytes1 > totalBytes2 ? -1 : (totalBytes1 == totalBytes2) ? 0 : 1;
    }
  }

  protected static class SortChildrenByPackets implements Comparator<String> {
    GroupItem item;

    public SortChildrenByPackets(GroupItem item) {
      this.item = item;
    }

    public int compare(String o1, String o2) {
      ChildItem c1;
      ChildItem c2;

      if(item.childrenAreFiltered) {
        c1 = item.childrenDataFiltered.get(o1);
        c2 = item.childrenDataFiltered.get(o2);
      } else {
        c1 = item.childrenData.get(o1);
        c2 = item.childrenData.get(o2);
      }

      long totalPackets1 = c1.sentPackets + c1.receivedPackets;
      long totalPackets2 = c2.sentPackets + c2.receivedPackets;

      return totalPackets1 > totalPackets2 ? -1 : (totalPackets1 == totalPackets2) ? 0 : 1;
    }
  }

  protected static class SortChildrenByTimestamp implements Comparator<String> {
    GroupItem item;

    public SortChildrenByTimestamp(GroupItem item) {
      this.item = item;
    }

    public int compare(String o1, String o2) {
      ChildItem c1;
      ChildItem c2;

      if(item.childrenAreFiltered) {
        c1 = item.childrenDataFiltered.get(o1);
        c2 = item.childrenDataFiltered.get(o2);
      } else {
        c1 = item.childrenData.get(o1);
        c2 = item.childrenData.get(o2);
      }

      long timestamp1 = c1.sentTimestamp;
      long timestamp2 = c2.sentTimestamp;

      if(c1.receivedTimestamp > timestamp1) {
        timestamp1 = c1.receivedTimestamp;
      }

      if(c2.receivedTimestamp > timestamp2) {
        timestamp2 = c2.receivedTimestamp;
      }

      return timestamp1 > timestamp2 ? -1 : (timestamp1 == timestamp2) ? 0 : 1;
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
    Comparator<GroupItem> preSortMethod = null;
    Comparator<GroupItem> sortMethod;
    switch(sortBy) {
      case UID:
        sortMethod = new SortAppsByUid();
        break;
      case NAME:
        sortMethod = new SortAppsByName();
        break;
      case THROUGHPUT:
        preSortMethod = new SortAppsByTimestamp();
        sortMethod = new SortAppsByThroughput();
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
      if(preSortMethod != null) {
        Collections.sort(groupData, preSortMethod);
      }

      Collections.sort(groupData, sortMethod);
    }
  }

  public void sortChildren() {
    synchronized(groupData) {
      for(GroupItem item : groupData) {
        item.childrenNeedSort = true;
      }
    }
  }

  public void setDoNotRefresh(boolean value) {
    doNotRefresh = value;
  }

  public void refreshAdapter() {
    if(doNotRefresh) {
      return;
    }

    if(listView == null) {
      return;
    }

    int index = listView.getFirstVisiblePosition();
    View v = listView.getChildAt(0);
    int top = (v == null) ? 0 : v.getTop();

    adapter.notifyDataSetChanged();

    if(MyLog.enabled && MyLog.level >= 5) {
      MyLog.d(5, "Refreshed AppFragment adapter");
    }

    listView.setSelectionFromTop(index, top);

    int size = adapter.getGroupCount();
    for(int i = 0; i < size; i++) {
      if(((GroupItem)adapter.getGroup(i)).isExpanded == true) {
        listView.expandGroup(i);
      } else {
        listView.collapseGroup(i);
      }
    }
  }

  public void addApp(ApplicationsTracker.AppEntry app) {
    if(groupDataBuffer == null) {
      return;
    }

    synchronized(groupDataBuffer) {
      GroupItem item = null;

      for(GroupItem i : groupDataBuffer) {
        if(i.app.packageName.equals(app.packageName)) {
          item = i;
          break;
        }
      }

      if(item == null) {
        item = new GroupItem();
        item.app = app;
        item.lastTimestamp = 0;
        item.childrenData = new HashMap<String, ChildItem>();
        item.childrenDataFiltered = new HashMap<String, ChildItem>();

        if(NetworkLogService.throughputBps) {
          item.throughputString = "0bps/0bps";
        } else {
          item.throughputString = "0B/0B";
        }

        groupData.add(item);
        groupDataBuffer.add(item);
      } else {
        item.app = app;
      }

      // groupDataBuffer must always be sorted by UID for binary search
      Collections.sort(groupDataBuffer, new SortAppsByUid());
      lastGetItemByAppUidIndex = -1;
    }

    if(NetworkLog.filterTextInclude.length() > 0 || NetworkLog.filterTextExclude.length() > 0) {
      setFilter("");
    } else {
      refreshAdapter();
    }
  }

  public void removeApp(String packageName) {
    if(groupData == null || groupDataBuffer == null) {
      return;
    }

    synchronized(groupData) {
      GroupItem item;
      Iterator<GroupItem> iterator = groupData.iterator();
      while(iterator.hasNext()) {
        item = iterator.next();
        if(item.app.packageName.equals(packageName)) {
          item.childrenData.clear();
          item.childrenDataFiltered.clear();
          iterator.remove();
        }
      }
    }

    synchronized(groupDataBuffer) {
      GroupItem item;
      Iterator<GroupItem> iterator = groupDataBuffer.iterator();
      while(iterator.hasNext()) {
        item = iterator.next();
        if(item.app.packageName.equals(packageName)) {
          item.childrenData.clear();
          item.childrenDataFiltered.clear();
          iterator.remove();
        }
      }
    }

    lastGetItemByAppUidIndex = -1;

    if(NetworkLog.filterTextInclude.length() > 0 || NetworkLog.filterTextExclude.length() > 0) {
      setFilter("");
    } else {
      refreshAdapter();
    }
  }

  protected void getInstalledApps(final boolean refresh) {
    synchronized(groupDataBuffer) {
      synchronized(groupData) {
        groupData.clear();
        groupDataBuffer.clear();

        synchronized(ApplicationsTracker.installedAppsLock) {
          for(ApplicationsTracker.AppEntry app : ApplicationsTracker.installedApps) {
            if(NetworkLog.state != NetworkLog.State.RUNNING && NetworkLog.initRunner.running == false) {
              MyLog.d("[AppFragment] Initialization aborted");
              return;
            }

            GroupItem item = new GroupItem();
            item.app = app;
            item.lastTimestamp = 0;
            item.childrenData = new HashMap<String, ChildItem>();
            item.childrenDataFiltered = new HashMap<String, ChildItem>();
            if(NetworkLogService.throughputBps) {
              item.throughputString = "0bps/0bps";
            } else {
              item.throughputString = "0B/0B";
            }
            groupData.add(item);
            groupDataBuffer.add(item);
          }
        }

        if(refresh == true) {
          Activity activity = getActivity();

          if(activity != null) {
            activity.runOnUiThread(new Runnable() {
              public void run() {
                preSortData();
                if(NetworkLog.filterTextInclude.length() > 0 || NetworkLog.filterTextExclude.length() > 0) {
                  setFilter("");
                } else {
                  refreshAdapter();
                }
              }
            });
          }
        }

        // groupDataBuffer must always be sorted by UID for binary search
        Collections.sort(groupDataBuffer, new SortAppsByUid());
      }
    }
  }

  @Override
    public void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      setUserVisibleHint(true);
    }

  public void setParent(NetworkLog parent) {
    this.parent = parent;
  }

  @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
      super.setUserVisibleHint(isVisibleToUser);

      if (this.isVisible() && !isVisibleToUser) {
        if(parent != null) {
          parent.invalidateOptionsMenu();
        }
      }
    }

  /** Called when the activity is first created. */
  @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      MyLog.d("AppFragment onCreate");

      setRetainInstance(true);

      sortBy = NetworkLog.settings.getSortBy();
      preSortBy = NetworkLog.settings.getPreSortBy();
      roundValues = NetworkLog.settings.getRoundValues();

      groupData = new ArrayList<GroupItem>();
      groupDataBuffer = new ArrayList<GroupItem>();
      cachedSearchItem = new GroupItem();
      cachedSearchItem.app = new ApplicationsTracker.AppEntry();

      adapter = new CustomAdapter();
    }

  @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
      Context context = getActivity().getApplicationContext();

      MyLog.d("[AppFragment] onCreateView");

      if(NetworkLog.settings == null) {
        NetworkLog activity = (NetworkLog) getActivity();

        if(activity != null) {
          activity.loadSettings();
        }
      }

      LinearLayout layout = new LinearLayout(context);
      layout.setOrientation(LinearLayout.VERTICAL);

      TextView tv = new TextView(context);
      tv.setText(getString(R.string.app_instructions));
      layout.addView(tv);

      listView = new ExpandableListView(context);
      listView.setAdapter(adapter);
      listView.setTextFilterEnabled(true);
      listView.setFastScrollEnabled(true);
      listView.setSmoothScrollbarEnabled(false);
      listView.setGroupIndicator(null);
      listView.setChildIndicator(null);
      listView.setDividerHeight(0);
      listView.setChildDivider(getResources().getDrawable(R.color.transparent));
      layout.addView(listView);

      listView.setOnGroupExpandListener(new OnGroupExpandListener() {
        @Override
        public void onGroupExpand(int groupPosition) {
          ((GroupItem)adapter.getGroup(groupPosition)).isExpanded = true;
        }
      });

      listView.setOnGroupCollapseListener(new OnGroupCollapseListener() {
        @Override
        public void onGroupCollapse(int groupPosition) {
          ((GroupItem)adapter.getGroup(groupPosition)).isExpanded = false;
        }
      });

      listView.setOnChildClickListener(new OnChildClickListener() {
        @Override
        public boolean onChildClick(ExpandableListView parent, View v, 
          int groupPosition, int childPosition, long id)
        {
          GroupItem group = (GroupItem) adapter.getGroup(groupPosition);
          ChildItem child = (ChildItem) adapter.getChild(groupPosition, childPosition);

          getActivity().startActivity(new Intent(getActivity().getApplicationContext(), AppTimelineGraph.class)
            .putExtra("app_uid", group.app.uid)
            .putExtra("src_addr", child.receivedAddress)
            .putExtra("src_port", child.receivedPort)
            .putExtra("dst_addr", child.sentAddress)
            .putExtra("dst_port", child.sentPort));

          return true;
        }
      });

      registerForContextMenu(listView);

      if(gotInstalledApps == false) {
        getInstalledApps(true);
        gotInstalledApps = true;
      }

      startUpdater();

      return layout;
    }

  @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
      super.onCreateContextMenu(menu, v, menuInfo);

      ExpandableListView.ExpandableListContextMenuInfo info =
        (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;

      int type = ExpandableListView.getPackedPositionType(info.packedPosition);
      int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
      int child = ExpandableListView.getPackedPositionChild(info.packedPosition);

      MenuInflater inflater = getActivity().getMenuInflater();
      inflater.inflate(R.layout.app_context_menu, menu);

      if (type != ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
        menu.findItem(R.id.app_copy_ip).setVisible(false);
        menu.findItem(R.id.app_whois_ip).setVisible(false);
      }

      GroupItem groupItem = (GroupItem) adapter.getGroup(group);

      if(NetworkLogService.toastBlockedApps.get(groupItem.app.packageName) != null) {
        menu.findItem(R.id.app_toggle_app_notifications).setTitle(R.string.enable_notifications);
      } else {
        menu.findItem(R.id.app_toggle_app_notifications).setTitle(R.string.disable_notifications);
      }

      if(NetworkLogService.blockedApps.get(groupItem.app.packageName) != null) {
        menu.findItem(R.id.app_toggle_app_logging).setTitle(R.string.unblock_app);
      } else {
        menu.findItem(R.id.app_toggle_app_logging).setTitle(R.string.block_app);
      }
    }

  @Override
    public boolean onContextItemSelected(MenuItem item) {
      if(!(item.getMenuInfo() instanceof ExpandableListContextMenuInfo))
        return super.onContextItemSelected(item);

      ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
      int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
      int childPos = ExpandableListView.getPackedPositionChild(info.packedPosition);
      ChildItem childItem;
      GroupItem groupItem;

      switch(item.getItemId()) {
        case R.id.app_copy_ip:
          childItem = (ChildItem) adapter.getChild(groupPos, childPos);
          copyIpAddress(childItem);
          return true;
        case R.id.app_whois_ip:
          childItem = (ChildItem) adapter.getChild(groupPos, childPos);
          whoisIpAddress(childItem);
          return true;
        case R.id.app_graph:
          groupItem = (GroupItem) adapter.getGroup(groupPos);
          showGraph(groupItem.app.uid);
          return true;
        case R.id.app_toggle_app_notifications:
          groupItem = (GroupItem) adapter.getGroup(groupPos);
          if(NetworkLogService.toastBlockedApps.remove(groupItem.app.packageName) == null) {
            NetworkLogService.toastBlockedApps.put(groupItem.app.packageName, groupItem.app.packageName);
          }
          new SelectToastApps().saveBlockedApps(NetworkLog.context, NetworkLogService.toastBlockedApps);
          return true;
        case R.id.app_toggle_app_logging:
          groupItem = (GroupItem) adapter.getGroup(groupPos);
          if(NetworkLogService.blockedApps.remove(groupItem.app.packageName) == null) {
            NetworkLogService.blockedApps.put(groupItem.app.packageName, groupItem.app.packageName);
          }
          new SelectBlockedApps().saveBlockedApps(NetworkLog.context, NetworkLogService.blockedApps);
          return true;
        default:
          return super.onContextItemSelected(item);
      }
    }

  @SuppressWarnings("deprecation")
  void copyIpAddress(ChildItem childItem) {
    String hostString = "";

    if(childItem.sentPackets > 0 && childItem.out != null) {
      String sentAddressString;
      String sentPortString;

      if(NetworkLog.resolveHosts && NetworkLog.resolveCopies) {
        sentAddressString = NetworkLog.resolver.resolveAddress(childItem.sentAddress);
        if(sentAddressString == null) {
          sentAddressString = childItem.sentAddress;
        }
      } else {
        sentAddressString = childItem.sentAddress;
      }

      if(NetworkLog.resolvePorts && NetworkLog.resolveCopies) {
        sentPortString = NetworkLog.resolver.resolveService(String.valueOf(childItem.sentPort));
      } else {
        sentPortString = String.valueOf(childItem.sentPort);
      }
      hostString = sentAddressString + ":" + sentPortString;
    }
    else if(childItem.receivedPackets > 0 && childItem.in != null) {
      String receivedAddressString;
      String receivedPortString;

      if(NetworkLog.resolveHosts && NetworkLog.resolveCopies) {
        receivedAddressString = NetworkLog.resolver.resolveAddress(childItem.receivedAddress);
        if(receivedAddressString == null) {
          receivedAddressString = childItem.receivedAddress;
        }
      } else {
        receivedAddressString = childItem.receivedAddress;
      }

      if(NetworkLog.resolvePorts && NetworkLog.resolveCopies) {
        receivedPortString = NetworkLog.resolver.resolveService(String.valueOf(childItem.receivedPort));
      } else {
        receivedPortString = String.valueOf(childItem.receivedPort);
      }
      hostString = receivedAddressString + ":" + receivedPortString;
    }

    ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);

    /* newer API 11 clipboard unsupported on older devices
    ClipData clip = ClipData.newPlainText("NetworkLog IP Address", hostString);
    clipboard.setPrimaryClip(clip);
    */

    /* use older deprecated ClipboardManager to support older devices */
    clipboard.setText(hostString);
  }

  void whoisIpAddress(ChildItem childItem) {
    String hostString = "";

    if(childItem.sentPackets > 0 && childItem.out != null) {
      String sentAddressString;

      if(NetworkLog.resolveHosts) {
        sentAddressString = NetworkLog.resolver.resolveAddress(childItem.sentAddress);
        if(sentAddressString == null) {
          sentAddressString = childItem.sentAddress;
        }
      } else {
        sentAddressString = childItem.sentAddress;
      }
      hostString = sentAddressString;
    }
    else if(childItem.receivedPackets > 0 && childItem.in != null) {
      String receivedAddressString;

      if(NetworkLog.resolveHosts) {
        receivedAddressString = NetworkLog.resolver.resolveAddress(childItem.receivedAddress);
        if(receivedAddressString == null) {
          receivedAddressString = childItem.receivedAddress;
        }
      } else {
        receivedAddressString = childItem.receivedAddress;
      }
      hostString = receivedAddressString;
    }
    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.whois.com/whois/" + hostString)));
  }

  void showGraph(int appuid) {
    getActivity().startActivity(new Intent(getActivity().getApplicationContext(), AppTimelineGraph.class)
        .putExtra("app_uid", appuid));
  }

  Comparator comparator = new Comparator<GroupItem>() {
    public int compare(GroupItem o1, GroupItem o2) {
      return o1.app.uid < o2.app.uid ? -1 : (o1.app.uid == o2.app.uid) ? 0 : 1;
    }
  };

  public int getItemByAppUid(int uid) {
    if(groupDataBuffer == null) {
      return -1;
    }

    synchronized(groupDataBuffer) {
      // check to see if we need to search for index
      // (more often than not, the last index is still the active index being requested)
      if(lastGetItemByAppUidIndex < 0 || groupDataBuffer.get(lastGetItemByAppUidIndex).app.uid != uid) {
        cachedSearchItem.app.uid = uid;
        lastGetItemByAppUidIndex = Collections.binarySearch(groupDataBuffer, cachedSearchItem, comparator);
      }

      // binarySearch isn't guaranteed to return the first item of items with the same uid
      // so find the first item
      while(lastGetItemByAppUidIndex > 0) {
        if(groupDataBuffer.get(lastGetItemByAppUidIndex - 1).app.uid == uid) {
          lastGetItemByAppUidIndex--;
        }
        else {
          break;
        }
      }
    }

    return lastGetItemByAppUidIndex;
  }

  public void updateAppThroughputBps() {
    if(groupDataBuffer == null) {
      return;
    }

    synchronized(groupDataBuffer) {
      for(GroupItem item : groupDataBuffer) {
        if(NetworkLogService.throughputBps) {
          item.uploadThroughput *= Byte.SIZE;
          item.downloadThroughput *= Byte.SIZE;
          item.totalThroughput *= Byte.SIZE;
        } else {
          item.uploadThroughput /= Byte.SIZE;
          item.downloadThroughput /= Byte.SIZE;
          item.totalThroughput /= Byte.SIZE;
        }

        if(NetworkLogService.invertUploadDownload) {
          item.throughputString = StringUtils.formatToBytes(item.downloadThroughput) + (NetworkLogService.throughputBps ? "bps/" : "B/") + StringUtils.formatToBytes(item.uploadThroughput) + (NetworkLogService.throughputBps ? "bps" : "B");
        } else {
          item.throughputString = StringUtils.formatToBytes(item.uploadThroughput) + (NetworkLogService.throughputBps ? "bps/" : "B/") + StringUtils.formatToBytes(item.downloadThroughput) + (NetworkLogService.throughputBps ? "bps" : "B");
        }
      }

      refreshAdapter();
    }
  }

  public void updateAppThroughput(int uid, long upload, long download) {
    if(groupDataBuffer == null) {
      return;
    }

    synchronized(groupDataBuffer) {
      int index = getItemByAppUid(uid);

      if(index < 0) {
        MyLog.d("updateAppThroughput: No app entry for " + uid);
        return;
      }

      GroupItem item;
      while(true) {
        item = groupDataBuffer.get(index);

        if(item.app.uid != uid) {
          break;
        }

        groupDataBufferIsDirty = true;

        item.uploadThroughput = upload;
        item.downloadThroughput = download;
        item.totalThroughput = upload + download;

        if(NetworkLogService.invertUploadDownload) {
          item.throughputString = StringUtils.formatToBytes(download) + (NetworkLogService.throughputBps ? "bps/" : "B/") + StringUtils.formatToBytes(upload) + (NetworkLogService.throughputBps ? "bps" : "B");
        } else {
          item.throughputString = StringUtils.formatToBytes(upload) + (NetworkLogService.throughputBps ? "bps/" : "B/") + StringUtils.formatToBytes(download) + (NetworkLogService.throughputBps ? "bps" : "B");
        }

        index++;
        if(index >= groupDataBuffer.size()) {
          break;
        }
      }
    }
  }

  public void rebuildLogEntries() {
    Log.d("NetworkLog", "AppFragment rebuilding entries start");
    long start = System.currentTimeMillis();
    stopUpdater();
    synchronized(groupDataBuffer) {
      clear();

      synchronized(NetworkLog.logFragment.listDataUnfiltered) {
        Iterator<LogFragment.ListItem> iterator = NetworkLog.logFragment.listDataUnfiltered.iterator();
        LogEntry entry = new LogEntry();
        while(iterator.hasNext()) {
          LogFragment.ListItem item = iterator.next();

          entry.uid = item.app.uid;
          entry.in = item.in;
          entry.out = item.out;
          entry.proto = item.proto;
          entry.src = item.srcAddr;
          entry.dst = item.dstAddr;
          entry.len = item.len;
          entry.spt = item.srcPort;
          entry.dpt = item.dstPort;
          entry.timestamp = item.timestamp;

          onNewLogEntry(entry);
        }
      }
      groupDataBufferIsDirty = true;
    }
    startUpdater();
    long elapsed = System.currentTimeMillis() - start;
    Log.d("NetworkLog", "AppFragment rebuilding entries end -- elapsed: " + elapsed);
  }

  // cache objects to prevent unnecessary allocations
  private CharArray charBuffer = new CharArray(256);
  private String srcKey;
  private String dstKey;
  private GroupItem newLogItem;
  private ChildItem newLogChild;

  public void onNewLogEntry(final LogEntry entry) {
    if(MyLog.enabled && MyLog.level >= 6) {
      MyLog.d(6, "AppFragment: NewLogEntry: [" + entry.uid + "] in=" + entry.in + " out=" + entry.out + " " + entry.src + ":" + entry.spt + " --> " + entry.dst + ":" + entry.dpt + " [" + entry.len + "]");
    }

    if(groupDataBuffer == null) {
      return;
    }

    if(!entry.isValid()) {
      return;
    }

    int index = getItemByAppUid(entry.uid);

    if(index < 0) {
      MyLog.d("No app entry for uid " + entry.uid);
      return;
    }

    synchronized(groupDataBuffer) {
      try {
        charBuffer.reset();
        charBuffer.append(entry.src).append(':').append(entry.spt).append(':').append(entry.proto).append(':');

        if(entry.in != null && entry.in.length() > 0) {
          charBuffer.append(entry.in);
        } else {
          charBuffer.append(entry.out);
        }

        srcKey = StringPool.get(charBuffer);

        charBuffer.reset();
        charBuffer.append(entry.dst).append(':').append(entry.dpt).append(':').append(entry.proto).append(':');

        if(entry.in != null && entry.in.length() > 0) {
          charBuffer.append(entry.in);
        } else {
          charBuffer.append(entry.out);
        }

        dstKey = StringPool.get(charBuffer);
      } catch (ArrayIndexOutOfBoundsException e) {
        Log.e("NetworkLog", "[AppFragment.onNewEntry] charBuffer too long, skipping entry", e);
        return;
      }

      // generally this will iterate once, but some apps may be grouped under the same uid
      while(true) {
        newLogItem = groupDataBuffer.get(index);

        if(newLogItem.app.uid != entry.uid) {
          break;
        }

        groupDataBufferIsDirty = true;

        newLogItem.totalPackets++;
        newLogItem.totalBytes += entry.len;
        newLogItem.lastTimestamp = entry.timestamp;

        if(entry.in != null && entry.in.length() != 0) {
          synchronized(newLogItem.childrenData) {
            newLogChild = newLogItem.childrenData.get(srcKey);

            if(newLogChild == null) {
              newLogChild = new ChildItem();
              newLogItem.childrenData.put(srcKey, newLogChild);
            }

            newLogChild.in = entry.in;
            newLogChild.out = null;
            newLogChild.proto = entry.proto;
            newLogChild.receivedPackets++;
            newLogChild.receivedBytes += entry.len;
            newLogChild.receivedTimestamp = entry.timestamp;

            if(MyLog.enabled && MyLog.level >= 8) {
              MyLog.d(8, "Added received packet index=" + index + " in=" + entry.in + " out=" + entry.out + " proto=" + entry.proto + " " + entry.src + ":" + entry.spt + " --> " + entry.dst + ":" + entry.dpt + "; total: " + newLogChild.receivedPackets + "; bytes: " + newLogChild.receivedBytes);
            }

            newLogChild.receivedPort = entry.spt;
            newLogChild.receivedAddress = entry.src;
            newLogChild.sentPort = entry.dpt;
            newLogChild.sentAddress = entry.dst;

            newLogItem.childrenNeedSort = true;
          }
        }

        if(entry.out != null && entry.out.length() != 0) {
          synchronized(newLogItem.childrenData) {
            newLogChild = newLogItem.childrenData.get(dstKey);

            if(newLogChild == null) {
              newLogChild = new ChildItem();
              newLogItem.childrenData.put(dstKey, newLogChild);
            }

            newLogChild.in = null;
            newLogChild.out = entry.out;
            newLogChild.proto = entry.proto;
            newLogChild.sentPackets++;
            newLogChild.sentBytes += entry.len;
            newLogChild.sentTimestamp = entry.timestamp;

            if(MyLog.enabled && MyLog.level >= 8) {
              MyLog.d(8, "Added sent packet index=" + index + " in=" + entry.in + " out=" + entry.out + " " + entry.src + ":" + entry.spt + " --> " + entry.dst + ":" + entry.dpt + "; total: " + newLogChild.sentPackets + "; bytes: " + newLogChild.sentBytes);
            }

            newLogChild.receivedPort = entry.spt;
            newLogChild.receivedAddress = entry.src;
            newLogChild.sentPort = entry.dpt;
            newLogChild.sentAddress = entry.dst;

            newLogItem.childrenNeedSort = true;
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
    if(updater != null) {
      updater.stop();
    }

    updater = new ListViewUpdater();
    new Thread(updater, "AppFragmentUpdater").start();
  }

  public void stopUpdater() {
    if(updater != null) {
      updater.stop();
    }
  }

  Runnable updaterRunner = new Runnable() {
    public void run() {
      if(groupData == null) {
        return;
      }

      synchronized(groupData) {
        if(MyLog.enabled && MyLog.level >= 4) {
          MyLog.d(4, "AppFragmentListUpdater enter");
        }

        if(groupDataBufferIsDirty) {
          preSortData();
          sortData();
        }

        if(groupDataBufferIsDirty && (NetworkLog.filterTextInclude.length() > 0 || NetworkLog.filterTextExclude.length() > 0)) {
          setFilter("");
        } else {
          refreshAdapter();
        }
      }

      groupDataBufferIsDirty = false;
      needsRefresh = false;

      if(MyLog.enabled && MyLog.level >= 4) {
        MyLog.d(4, "AppFragmentListUpdater exit");
      }
    }
  };

  public void updaterRunOnce() {
    NetworkLog.handler.post(updaterRunner);
  }

  // todo: this is largely duplicated in LogFragment -- move to its own file
  private class ListViewUpdater implements Runnable {
    boolean running = false;

    public void stop() {
      running = false;
    }

    public void run() {
      running = true;
      MyLog.d("Starting AppFragmentUpdater " + this);

      while(running) {
        if(groupDataBufferIsDirty == true || needsRefresh == true) {
          updaterRunOnce();
        }

        try {
          Thread.sleep(1000);
        } catch(Exception e) {
          Log.d("NetworkLog", "AppFragmentListUpdater", e);
        }
      }

      MyLog.d("Stopped AppFragment updater " + this);
    }
  }

  public void setFilter(CharSequence s) {
    if(MyLog.enabled) {
      MyLog.d("[AppFragment] setFilter(" + s + ")");
    }
    if(adapter != null) {
      adapter.getFilter().filter(s);
    }
  }

  private class CustomAdapter extends BaseExpandableListAdapter implements Filterable {
    LayoutInflater mInflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

    CustomFilter filter;

    private class CustomFilter extends Filter {
      FilterResults results = new FilterResults();

      @Override
        protected FilterResults performFiltering(CharSequence constraint) {
          ArrayList<GroupItem> originalItems = new ArrayList<GroupItem>(groupDataBuffer.size());
          ArrayList<GroupItem> filteredItems = new ArrayList<GroupItem>(groupDataBuffer.size());
          String host;
          String iface;
          ChildItem childData;
          boolean matched;
          String sentAddressResolved;
          String sentPortResolved;
          String receivedAddressResolved;
          String receivedPortResolved;

          doNotRefresh = true;

          if(MyLog.enabled) {
            MyLog.d("[AppFragment] performFiltering");
          }

          Log.d("NetworkLog", "[appFragment] performing filtering");

          synchronized(groupDataBuffer) {
            originalItems.addAll(groupDataBuffer);
          }

          if(NetworkLog.filterTextInclude.length() == 0 && NetworkLog.filterTextExclude.length() == 0) {
            if(MyLog.enabled) {
              MyLog.d("[AppFragment] no constraint item count: " + originalItems.size());
            }

            // undo uniqueHosts filtering
            // fixme: perhaps an array of indices into which items are filtered?
            for(GroupItem item : originalItems) {
              if(item.childrenAreFiltered) {
                item.childrenAreFiltered = false;
                item.childrenDataFiltered.clear();
                item.childrenNeedSort = true;
              }
            }

            results.values = originalItems;
            results.count = originalItems.size();
          } else {
            int count = originalItems.size();

            if(MyLog.enabled) {
              MyLog.d("[AppFragment] item count: " + count);
            }

            if(NetworkLog.filterTextIncludeList.size() == 0) {
              if(MyLog.enabled) {
                MyLog.d("[AppFragment] no include filter, adding all items");
              }

              for(GroupItem item : originalItems) {
                filteredItems.add(item);

                synchronized(item.childrenData) {
                  item.childrenDataFiltered.clear();
                  List<String> list = new ArrayList<String>(item.childrenData.keySet());
                  Iterator<String> itr = list.iterator();
                  while(itr.hasNext()) {
                    host = itr.next();
                    childData = item.childrenData.get(host);
                    if(MyLog.enabled) {
                      MyLog.d("[AppFragment] adding filtered host " + childData);
                    }
                    item.childrenDataFiltered.put(host, childData);
                    item.childrenAreFiltered = true;
                  }
                }
              }
            } else {
              GroupItem item;
              for(int i = 0; i < count; i++) {
                item = originalItems.get(i);
                // MyLog.d("[AppFragment] testing filtered item " + item + "; includes: [" + NetworkLog.filterTextInclude + "]");

                boolean item_added = false;
                matched = false;

                if(NetworkLog.filterNameInclude || NetworkLog.filterUidInclude) {
                  for(String c : NetworkLog.filterTextIncludeList) {
                    if((NetworkLog.filterNameInclude && item.app.nameLowerCase.contains(c))
                        || (NetworkLog.filterUidInclude && item.app.uidString.equals(c))) {
                      matched = true;
                        }
                  }
                } else {
                  matched = true;
                }

                if(matched) {
                  // test filter against address/port/iface/proto
                  if(NetworkLog.filterAddressInclude || NetworkLog.filterPortInclude 
                      || NetworkLog.filterInterfaceInclude || NetworkLog.filterProtocolInclude) {
                    synchronized(item.childrenData) {
                      item.childrenDataFiltered.clear();
                      List<String> list = new ArrayList<String>(item.childrenData.keySet());
                      // todo: sort by user preference (bytes, timestamp, address, ports)
                      Collections.sort(list);
                      Iterator<String> itr = list.iterator();
                      while(itr.hasNext()) {
                        host = itr.next();
                        // MyLog.d("[AppFragment] testing " + host);

                        childData = item.childrenData.get(host);

                        matched = false;

                        if(NetworkLog.resolveHosts) {
                          sentAddressResolved = NetworkLog.resolver.resolveAddress(childData.sentAddress);

                          if(sentAddressResolved == null) {
                            sentAddressResolved = "";
                          }

                          receivedAddressResolved = NetworkLog.resolver.resolveAddress(childData.receivedAddress);

                          if(receivedAddressResolved == null) {
                            receivedAddressResolved = "";
                          }
                        } else {
                          sentAddressResolved = "";
                          receivedAddressResolved = "";
                        }

                        if(NetworkLog.resolvePorts) {
                          sentPortResolved = NetworkLog.resolver.resolveService(String.valueOf(childData.sentPort));
                          receivedPortResolved = NetworkLog.resolver.resolveService(String.valueOf(childData.receivedPort));
                        } else {
                          sentPortResolved = "";
                          receivedPortResolved = "";
                        }

                        if(childData.in != null && childData.in.length() > 0) {
                          iface = childData.in;
                        } else {
                          iface = childData.out;
                        }

                        for(String c : NetworkLog.filterTextIncludeList) {
                          if((NetworkLog.filterAddressInclude && 
                                ((childData.sentPackets > 0 && (childData.sentAddress.contains(c) || StringPool.getLowerCase(sentAddressResolved).contains(c)))
                                  || (childData.receivedPackets > 0 && (childData.receivedAddress.contains(c) || StringPool.getLowerCase(receivedAddressResolved).contains(c)))))
                              || (NetworkLog.filterPortInclude && 
                                ((childData.sentPackets > 0 && (String.valueOf(childData.sentPort).equals(c) || StringPool.getLowerCase(sentPortResolved).equals(c)))
                                  || (childData.receivedPackets > 0 && (String.valueOf(childData.receivedPort).equals(c) || StringPool.getLowerCase(receivedPortResolved).equals(c)))))
                              || (NetworkLog.filterInterfaceInclude && iface.contains(c))
                              || (NetworkLog.filterProtocolInclude && StringPool.getLowerCase(NetworkLog.resolver.resolveProtocol(childData.proto)).equals(c))) {
                            matched = true;
                            break;
                                  }
                        }

                        if(matched) {
                          if(!item_added) {
                            // MyLog.d("[AppFragment] adding filtered item " + item);
                            filteredItems.add(item);
                            item_added = true;
                          }

                          // MyLog.d("[AppFragment] adding filtered host " + childData);
                          item.childrenDataFiltered.put(host, childData);
                          item.childrenAreFiltered = true;
                          item.childrenNeedSort = true;
                        }
                      }
                    }
                  } else {
                    // no filtering for host/port, matches everything
                    // MyLog.d("[AppFragment] no filter for host/port; adding filtered item " + item);
                    filteredItems.add(item);

                    synchronized(item.childrenData) {
                      List<String> list = new ArrayList<String>(item.childrenData.keySet());
                      // todo: sort by user preference
                      Collections.sort(list);
                      item.childrenDataFiltered.clear();
                      Iterator<String> itr = list.iterator();
                      while(itr.hasNext()) {
                        host = itr.next();
                        childData = item.childrenData.get(host);
                        // MyLog.d("[AppFragment] adding filtered host " + childData);
                        item.childrenDataFiltered.put(host, childData);
                        item.childrenAreFiltered = true;
                        item.childrenNeedSort = true;
                      }
                    }
                  }
                }
              }
            }

            if(NetworkLog.filterTextExcludeList.size() > 0) {
              count = filteredItems.size();

              GroupItem item;
              for(int i = count - 1; i >= 0; i--) {
                item = filteredItems.get(i);
                // MyLog.d("[AppFragment] testing filtered item: " + i + " " + item + "; excludes: [" + NetworkLog.filterTextExclude + "]");

                matched = false;

                if(NetworkLog.filterNameExclude || NetworkLog.filterUidExclude) {
                  for(String c : NetworkLog.filterTextExcludeList) {
                    if((NetworkLog.filterNameExclude && item.app.nameLowerCase.contains(c))
                        || NetworkLog.filterUidExclude && item.app.uidString.equals(c)) 
                    {
                      matched = true;
                    }
                  }
                } else {
                  matched = false;
                }

                if(matched) {
                  // MyLog.d("[AppFragment] removing filtered item: " + item);
                  filteredItems.remove(i);
                  continue;
                }

                if(NetworkLog.filterAddressExclude || NetworkLog.filterPortExclude
                    || NetworkLog.filterInterfaceExclude || NetworkLog.filterProtocolExclude) {
                  List<String> list = new ArrayList<String>(item.childrenDataFiltered.keySet());
                  Iterator<String> itr = list.iterator();
                  while(itr.hasNext()) {
                    host = itr.next();
                    childData = item.childrenDataFiltered.get(host);

                    matched = false;

                    if(NetworkLog.resolveHosts) {
                      sentAddressResolved = NetworkLog.resolver.resolveAddress(childData.sentAddress);

                      if(sentAddressResolved == null) {
                        sentAddressResolved = "";
                      }

                      receivedAddressResolved = NetworkLog.resolver.resolveAddress(childData.receivedAddress);

                      if(receivedAddressResolved == null) {
                        receivedAddressResolved = "";
                      }
                    } else {
                      sentAddressResolved = "";
                      receivedAddressResolved = "";
                    }

                    if(NetworkLog.resolvePorts) {
                      sentPortResolved = NetworkLog.resolver.resolveService(String.valueOf(childData.sentPort));
                      receivedPortResolved = NetworkLog.resolver.resolveService(String.valueOf(childData.receivedPort));
                    } else {
                      sentPortResolved = "";
                      receivedPortResolved = "";
                    }

                    if(childData.in != null && childData.in.length() > 0) {
                      iface = childData.in;
                    } else {
                      iface = childData.out;
                    }

                    for(String c : NetworkLog.filterTextExcludeList) {
                      if((NetworkLog.filterAddressExclude && 
                            ((childData.sentPackets > 0 && (childData.sentAddress.contains(c) || StringPool.getLowerCase(sentAddressResolved).contains(c)))
                             || (childData.receivedPackets > 0 && (childData.receivedAddress.contains(c) || StringPool.getLowerCase(receivedAddressResolved).contains(c)))))
                          || (NetworkLog.filterPortExclude && 
                            ((childData.sentPackets > 0 && (String.valueOf(childData.sentPort).equals(c) || StringPool.getLowerCase(sentPortResolved).equals(c)))
                             || (childData.receivedPackets > 0 && (String.valueOf(childData.receivedPort).equals(c) || StringPool.getLowerCase(receivedPortResolved).equals(c)))))
                          || (NetworkLog.filterInterfaceExclude && iface.contains(c))
                          || (NetworkLog.filterProtocolExclude && StringPool.getLowerCase(NetworkLog.resolver.resolveProtocol(childData.proto)).equals(c))) {
                        matched = true;
                        break;
                          }
                    }

                    if(matched) {
                      // MyLog.d("[AppFragment] removing filtered host [" + host + "] " + childData);
                      item.childrenDataFiltered.remove(host);
                    }
                  }

                  if(item.childrenDataFiltered.size() == 0 && matched) {
                    // MyLog.d("[AppFragment] removed all hosts, removing item from filter results");
                    filteredItems.remove(i);
                  }
                }
              }
            }

            results.values = filteredItems;
            results.count = filteredItems.size();
          }

          if(MyLog.enabled) {
            MyLog.d("[AppFragment] filter returning " + results.count + " results");
          }
          Log.d("NetworkLog", "[AppFragment] filter returning " + results.count + " results");
          return results;
        }

      @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
          if(MyLog.enabled) {
            MyLog.d("[AppFragment] Publishing filter results");
          }
            Log.d("NetworkLog", "[AppFragment] Publishing filter results");
          
          synchronized(groupData) {
            groupData.clear();
            groupData.addAll((ArrayList<GroupItem>) results.values);

            preSortData();
            sortData();
          }
          doNotRefresh = false;
          refreshAdapter();
          Log.d("NetworkLog", "[AppFragment] Published");
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
        GroupItem groupItem = groupData.get(groupPosition);
        HashMap<String, ChildItem> children;

        if(groupItem.childrenAreFiltered == false) {
          children = groupItem.childrenData;
        } else {
          children = groupItem.childrenDataFiltered;
        }

        if(groupItem.childrenNeedSort == true || groupItem.childrenDataSorted == null) {
          groupItem.childrenNeedSort = false;

          if(groupItem.childrenDataSorted == null || groupItem.childrenDataSorted.length < children.size()) {
            groupItem.childrenDataSorted = new String[children.size()];
          }

          children.keySet().toArray(groupItem.childrenDataSorted);

          Comparator<String> sortMethod;
          switch(sortBy) {
            case UID:
              sortMethod = new SortChildrenByBytes(groupItem);
              break;
            case NAME:
              sortMethod = new SortChildrenByBytes(groupItem);
              break;
            case PACKETS:
              sortMethod = new SortChildrenByPackets(groupItem);
              break;
            case BYTES:
              sortMethod = new SortChildrenByBytes(groupItem);
              break;
            case TIMESTAMP:
              sortMethod = new SortChildrenByTimestamp(groupItem);
              break;
            default:
              sortMethod = new SortChildrenByBytes(groupItem);
          }

          Arrays.sort(groupItem.childrenDataSorted, sortMethod);
        }

        return children.get(groupItem.childrenDataSorted[childPosition]);
      }

    @Override
      public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
      }

    @Override
      public int getChildrenCount(int groupPosition) {
        GroupItem groupItem = groupData.get(groupPosition);

        if(groupItem.childrenAreFiltered == false) {
          return groupItem.childrenData.size();
        } else {
          return groupItem.childrenDataFiltered.size();
        }
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
      public View getGroupView(int groupPosition, boolean isExpanded,
          View convertView, ViewGroup parent)
      {
        GroupViewHolder holder = null;

        ImageView icon;
        TextView name;
        TextView throughput;
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

        if(groupPosition == 0) {
          holder.getDivider().setVisibility(View.GONE);
        } else {
          holder.getDivider().setVisibility(View.VISIBLE);
        }

        icon = holder.getIcon();
        icon.setTag(item.app.packageName);
        icon.setImageDrawable(ApplicationsTracker.loadIcon(getActivity().getApplicationContext(), icon, item.app.packageName));

        name = holder.getName();

        name.setText("(" + item.app.uid + ")" + " " + item.app.name);

        throughput = holder.getThroughput();
        throughput.setText(getString(R.string.app_throughput) + item.throughputString);

        packets = holder.getPackets();
        if(roundValues) {
          packets.setText(getString(R.string.app_packets) + StringUtils.formatToThousands(item.totalPackets));
        } else {
          packets.setText(getString(R.string.app_packets) + item.totalPackets);
        }

        bytes = holder.getBytes();
        if(roundValues) {
          bytes.setText(getString(R.string.app_bytes) + StringUtils.formatToBytes(item.totalBytes));
        } else {
          bytes.setText(getString(R.string.app_bytes) + item.totalBytes);
        }

        timestamp = holder.getTimestamp();

        if(item.lastTimestamp != 0) {
          timestamp.setText("(" + Timestamp.getTimestamp(item.lastTimestamp) + ")");
          timestamp.setVisibility(View.VISIBLE);
        } else {
          timestamp.setVisibility(View.GONE);
        }

        return convertView;
      }

    @Override
      public View getChildView(int groupPosition, int childPosition,
          boolean isLastChild, View convertView, ViewGroup parent) 
      {
        ChildViewHolder holder = null;

        final TextView host;

        TextView sentPackets;
        TextView sentBytes;
        TextView sentTimestamp;

        TextView receivedPackets;
        TextView receivedBytes;
        TextView receivedTimestamp;

        final ChildItem item;

        synchronized(groupData) {
          item = (ChildItem) getChild(groupPosition, childPosition);
        }

        if(item == null) {
          if(MyLog.enabled) {
            MyLog.d("child (" + groupPosition + "," + childPosition + ") not found");
          }
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
        host.setPaintFlags(host.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);

        String hostString;
        final String iface;

        if(item.sentPackets > 0 && item.out != null && item.out.length() > 0) {
          host.setTag(item.sentAddress);

          String sentAddressString;
          final String sentPortString;

          if(NetworkLog.resolvePorts) {
            sentPortString = NetworkLog.resolver.resolveService(String.valueOf(item.sentPort));
          } else {
            sentPortString = String.valueOf(item.sentPort);
          }

          if(item.proto != null && item.proto.length() > 0) {
            iface = NetworkLog.resolver.resolveProtocol(item.proto) + "/" + item.out;
          } else {
            iface = item.out;
          }

          if(NetworkLog.resolveHosts) {
            sentAddressString = NetworkLog.resolver.getResolvedAddress(item.sentAddress);

            if(sentAddressString == null) {
              NetworkResolverUpdater updater = new NetworkResolverUpdater() {
                public void run() {
                  String tag = (String) host.getTag();
                  if(tag != null && tag.equals(item.sentAddress)) {
                    host.setText(resolved + ":" + sentPortString + " (" + iface + ")");
                  }
                }
              };

              sentAddressString = NetworkLog.resolver.resolveAddress(item.sentAddress, updater);

              if(sentAddressString == null) {
                sentAddressString = item.sentAddress;
              }
            }
          } else {
            sentAddressString = item.sentAddress;
          }

          hostString = sentAddressString + ":" + sentPortString + " (" + iface + ")";
        } else {
          host.setTag(item.receivedAddress);

          String receivedAddressString;
          final String receivedPortString;

          if(NetworkLog.resolvePorts) {
            receivedPortString = NetworkLog.resolver.resolveService(String.valueOf(item.receivedPort));
          } else {
            receivedPortString = String.valueOf(item.receivedPort);
          }

          if(item.proto != null && item.proto.length() > 0) {
            iface = NetworkLog.resolver.resolveProtocol(item.proto) + "/" + item.in;
          } else {
            iface = item.in;
          }

          if(NetworkLog.resolveHosts) {
            receivedAddressString = NetworkLog.resolver.getResolvedAddress(item.receivedAddress);

            if(receivedAddressString == null) {
              NetworkResolverUpdater updater = new NetworkResolverUpdater() {
                public void run() {
                  String tag = (String) host.getTag();
                  if(tag != null && tag.equals(item.receivedAddress)) {
                    host.setText(resolved + ":" + receivedPortString + " (" + iface + ")");
                  }
                }
              };

              receivedAddressString = NetworkLog.resolver.resolveAddress(item.receivedAddress, updater);

              if(receivedAddressString == null) {
                receivedAddressString = item.receivedAddress;
              }
            }
          } else {
            receivedAddressString = item.receivedAddress;
          }

          hostString = receivedAddressString + ":" + receivedPortString + " (" + iface + ")";
        }

        host.setText(hostString);

        sentPackets = holder.getSentPackets();
        sentBytes = holder.getSentBytes();
        sentTimestamp = holder.getSentTimestamp();

        if(item.sentPackets > 0) {
          if(roundValues) {
            sentPackets.setText(StringUtils.formatToThousands(item.sentPackets));
            sentBytes.setText(StringUtils.formatToBytes(item.sentBytes));
          } else {
            sentPackets.setText(String.valueOf(item.sentPackets));
            sentBytes.setText(String.valueOf(item.sentBytes));
          }

          String timestampString = Timestamp.getTimestamp(item.sentTimestamp);
          sentTimestamp.setText("(" + timestampString.substring(timestampString.indexOf('-') + 1, timestampString.indexOf('.')) + ")");

          sentPackets.setVisibility(View.VISIBLE);
          sentBytes.setVisibility(View.VISIBLE);
          sentTimestamp.setVisibility(View.VISIBLE);
          holder.getSentLabel().setVisibility(View.VISIBLE);
          holder.getSentPacketsLabel().setVisibility(View.VISIBLE);
          holder.getSentBytesLabel().setVisibility(View.VISIBLE);
        } else {
          sentPackets.setVisibility(View.GONE);
          sentBytes.setVisibility(View.GONE);
          sentTimestamp.setVisibility(View.GONE);
          holder.getSentLabel().setVisibility(View.GONE);
          holder.getSentPacketsLabel().setVisibility(View.GONE);
          holder.getSentBytesLabel().setVisibility(View.GONE);
        }

        receivedPackets = holder.getReceivedPackets();
        receivedBytes = holder.getReceivedBytes();
        receivedTimestamp = holder.getReceivedTimestamp();

        if(item.receivedPackets > 0) {
          if(roundValues) {
            receivedPackets.setText(StringUtils.formatToThousands(item.receivedPackets));
            receivedBytes.setText(StringUtils.formatToBytes(item.receivedBytes));
          } else {
            receivedPackets.setText(String.valueOf(item.receivedPackets));
            receivedBytes.setText(String.valueOf(item.receivedBytes));
          }

          String timestampString = Timestamp.getTimestamp(item.receivedTimestamp);
          receivedTimestamp.setText("(" + timestampString.substring(timestampString.indexOf('-') + 1, timestampString.indexOf('.')) + ")");
          receivedPackets.setVisibility(View.VISIBLE);
          receivedBytes.setVisibility(View.VISIBLE);
          receivedTimestamp.setVisibility(View.VISIBLE);
          holder.getReceivedLabel().setVisibility(View.VISIBLE);
          holder.getReceivedPacketsLabel().setVisibility(View.VISIBLE);
          holder.getReceivedBytesLabel().setVisibility(View.VISIBLE);
        } else {
          receivedPackets.setVisibility(View.GONE);
          receivedBytes.setVisibility(View.GONE);
          receivedTimestamp.setVisibility(View.GONE);
          holder.getReceivedLabel().setVisibility(View.GONE);
          holder.getReceivedPacketsLabel().setVisibility(View.GONE);
          holder.getReceivedBytesLabel().setVisibility(View.GONE);
        }

        return convertView;
      }
  }

  private class GroupViewHolder {
    private View mView;
    private ImageView mDivider = null;
    private ImageView mIcon = null;
    private TextView mName = null;
    private TextView mThroughput = null;
    private TextView mPackets = null;
    private TextView mBytes = null;
    private TextView mTimestamp = null;
    private TextView mUniqueHosts = null;

    public GroupViewHolder(View view) {
      mView = view;
    }

    public ImageView getDivider() {
      if(mDivider == null) {
        mDivider = (ImageView) mView.findViewById(R.id.appDivider);
      }

      return mDivider;
    }

    public ImageView getIcon() {
      if(mIcon == null) {
        mIcon = (ImageView) mView.findViewById(R.id.appIconx);
      }

      return mIcon;
    }

    public TextView getName() {
      if(mName == null) {
        mName = (TextView) mView.findViewById(R.id.appName);
      }

      return mName;
    }

    public TextView getThroughput() {
      if(mThroughput == null) {
        mThroughput = (TextView) mView.findViewById(R.id.appThroughput);
      }

      return mThroughput;
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

    private TextView mSentLabel = null;
    private TextView mSentPackets = null;
    private TextView mSentPacketsLabel = null;
    private TextView mSentBytes = null;
    private TextView mSentBytesLabel = null;
    private TextView mSentTimestamp = null;

    private TextView mReceivedLabel = null;
    private TextView mReceivedPackets = null;
    private TextView mReceivedPacketsLabel = null;
    private TextView mReceivedBytes = null;
    private TextView mReceivedBytesLabel = null;
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
    
    public TextView getSentLabel() {
      if(mSentLabel == null) {
        mSentLabel = (TextView) mView.findViewById(R.id.sentLabel);
      }

      return mSentLabel;
    }

    public TextView getSentPacketsLabel() {
      if(mSentPacketsLabel == null) {
        mSentPacketsLabel = (TextView) mView.findViewById(R.id.sentPacketsLabel);
      }

      return mSentPacketsLabel;
    }

    public TextView getSentBytesLabel() {
      if(mSentBytesLabel == null) {
        mSentBytesLabel = (TextView) mView.findViewById(R.id.sentBytesLabel);
      }

      return mSentBytesLabel;
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

    public TextView getReceivedLabel() {
      if(mReceivedLabel == null) {
        mReceivedLabel = (TextView) mView.findViewById(R.id.receivedLabel);
      }

      return mReceivedLabel;
    }

    public TextView getReceivedPacketsLabel() {
      if(mReceivedPacketsLabel == null) {
        mReceivedPacketsLabel = (TextView) mView.findViewById(R.id.receivedPacketsLabel);
      }

      return mReceivedPacketsLabel;
    }

    public TextView getReceivedBytesLabel() {
      if(mReceivedBytesLabel == null) {
        mReceivedBytesLabel = (TextView) mView.findViewById(R.id.receivedBytesLabel);
      }

      return mReceivedBytesLabel;
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
