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
import android.widget.Toast;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.widget.AdapterView.AdapterContextMenuInfo;

/* newer API 11 clipboard unsupported on older APIs
import android.content.ClipboardManager;
import android.content.ClipData;
*/

/* use older clipboard API to support older devices */
import android.text.ClipboardManager;

import android.support.v4.app.Fragment;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class LogFragment extends Fragment {
  // bound to adapter
  protected LinkedList<ListItem> listData;
  // buffers incoming log entries
  protected LinkedList<ListItem> listDataBuffer;
  // holds all entries, used for filtering
  protected LinkedList<ListItem> listDataUnfiltered;
  private CustomAdapter adapter;
  private ListViewUpdater updater;
  private NetworkLog parent = null;
  public long maxLogEntries;
  private boolean doNotRefresh = false;
  public boolean needsRefresh = false;

  protected class ListItem {
    protected ApplicationsTracker.AppEntry app;
    protected String in;
    protected String out;
    protected String proto;
    protected String srcAddr;
    protected int srcPort;
    protected String dstAddr;
    protected int dstPort;
    protected int len;
    protected long timestamp;

    ListItem(ApplicationsTracker.AppEntry app) {
      this.app = app;
    }

    @Override
      public String toString() {
        return app.name;
      }
  }

  public void clear() {
    synchronized(listData) {
      synchronized(listDataBuffer) {
        synchronized(listDataUnfiltered) {
          listData.clear();
          listDataBuffer.clear();
          listDataUnfiltered.clear();
          refreshAdapter();
        }
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

    adapter.notifyDataSetChanged();

    if(MyLog.enabled) {
      MyLog.d("Refreshed LogFragment adapter");
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
    public void onCreate(Bundle savedInstanceState)
    {
      super.onCreate(savedInstanceState);
      setRetainInstance(true);

      listData = new LinkedList<ListItem>();
      listDataBuffer = new LinkedList<ListItem>();
      listDataUnfiltered = new LinkedList<ListItem>();

      adapter = new CustomAdapter(getActivity().getApplicationContext(), R.layout.logitem, listData);

      if(NetworkLog.settings == null) {
        NetworkLog activity = (NetworkLog) getActivity();

        if(activity != null) {
          activity.loadSettings();
        }
      }

      try {
        maxLogEntries = NetworkLog.settings.getMaxLogEntries();
      } catch (Exception e) {
        Log.w("NetworkLog", "Exception getting max log entries: " + e.getMessage(), e);
        maxLogEntries = 75000;
      }

      MyLog.d("LogFragment onCreate");
    }

  @Override
    public void onDestroy() {
      super.onDestroy();
      MyLog.d("LogFragment onDestroy");
    }

  @Override
    public void onDestroyView() {
      super.onDestroyView();
      MyLog.d("LogFragment onDestroyView");
    }

  @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
      MyLog.d("[LogFragment] onCreateView");
      LinearLayout layout = new LinearLayout(getActivity().getApplicationContext());
      layout.setOrientation(LinearLayout.VERTICAL);
      ListView listView = new ListView(getActivity().getApplicationContext());
      listView.setAdapter(adapter);
      listView.setTextFilterEnabled(true);
      listView.setFastScrollEnabled(true);
      listView.setSmoothScrollbarEnabled(false);
      listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
      listView.setStackFromBottom(true);
      listView.setOnItemClickListener(new CustomOnItemClickListener());
      layout.addView(listView);
      registerForContextMenu(listView);
      startUpdater();
      return layout;
    }

  @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
      super.onCreateContextMenu(menu, v, menuInfo);
      MenuInflater inflater = getActivity().getMenuInflater();
      inflater.inflate(R.layout.log_context_menu, menu);
    }

  @Override
    public boolean onContextItemSelected(MenuItem item) {
      AdapterContextMenuInfo info;
      ListItem listItem;

      switch(item.getItemId()) {
        case R.id.log_copy_src_ip:
          info = (AdapterContextMenuInfo) item.getMenuInfo();
          listItem = listData.get(info.position);
          copySourceIp(listItem);
          return true;
        case R.id.log_copy_dst_ip:
          info = (AdapterContextMenuInfo) item.getMenuInfo();
          listItem = listData.get(info.position);
          copyDestIp(listItem);
          return true;
        case R.id.log_graph:
          info = (AdapterContextMenuInfo) item.getMenuInfo();
          listItem = listData.get(info.position);
          showGraph(listItem);
          return true;
        default:
          return super.onContextItemSelected(item);
      }
    } 

  @SuppressWarnings("deprecation")
    public void copySourceIp(ListItem item) {
      String srcAddr;
      String srcPort;

      if(NetworkLog.resolveHosts && NetworkLog.resolveCopies) {
        String resolved = NetworkLog.resolver.resolveAddress(item.srcAddr);

        if(resolved != null) {
          srcAddr = resolved;
        } else {
          srcAddr = item.srcAddr;
        }
      } else {
        srcAddr = item.srcAddr;
      }

      if(NetworkLog.resolvePorts && NetworkLog.resolveCopies) {
        srcPort = NetworkLog.resolver.resolveService(String.valueOf(item.srcPort));
      } else {
        srcPort = String.valueOf(item.srcPort);
      }

      ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);

      /* newer API 11 clipboard unsupported on older devices
         ClipData clip = ClipData.newPlainText("NetworkLog Source IP", srcAddr + ":" + srcPort);
         clipboard.setPrimaryClip(clip);
         */

      /* use older deprecated ClipboardManager to support older devices */
      clipboard.setText(srcAddr + ":" + srcPort);
    }

  @SuppressWarnings("deprecation")
    public void copyDestIp(ListItem item) {
      String dstAddr;
      String dstPort;

      if(NetworkLog.resolveHosts && NetworkLog.resolveCopies) {
        String resolved = NetworkLog.resolver.resolveAddress(item.dstAddr);

        if(resolved != null) {
          dstAddr = resolved;
        } else {
          dstAddr = item.dstAddr;
        }
      } else {
        dstAddr = item.dstAddr;
      }

      if(NetworkLog.resolvePorts && NetworkLog.resolveCopies) {
        dstPort = NetworkLog.resolver.resolveService(String.valueOf(item.dstPort));
      } else {
        dstPort = String.valueOf(item.dstPort);
      }

      ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);

      /* newer API 11 clipboard unsupported on older devices
         ClipData clip = ClipData.newPlainText("NetworkLog Dest IP", dstAddr + ":" + dstPort);
         clipboard.setPrimaryClip(clip);
         */

      /* use older deprecated ClipboardManager to support older devices */
      clipboard.setText(dstAddr + ":" + dstPort);
    }

  public void showGraph(ListItem item) {
    startActivity(new Intent(getActivity().getApplicationContext(), AppTimelineGraph.class)
        .putExtra("app_uid", item.app.uid)
        .putExtra("src_addr", item.srcAddr)
        .putExtra("src_port", item.srcPort)
        .putExtra("dst_addr", item.dstAddr)
        .putExtra("dst_port", item.dstPort));
  }

  private class CustomOnItemClickListener implements OnItemClickListener {
    @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        showGraph(listData.get(position));
      }
  }

  public void startUpdater() {
    if(updater != null) {
      updater.stop();
    }

    updater = new ListViewUpdater();
    new Thread(updater, "LogFragmentUpdater").start();
  }

  public void onNewLogEntry(final LogEntry entry) {
    if(listDataBuffer == null) {
      return;
    }

    if(!entry.isValid()) {
      return;
    }

    ApplicationsTracker.AppEntry appEntry = ApplicationsTracker.uidMap.get(entry.uidString);

    if(appEntry == null) {
      if(MyLog.enabled) {
        MyLog.d("LogFragment: No appEntry for uid " + entry.uid);
      }
      return;
    }

    final ListItem item = new ListItem(appEntry);

    item.in = entry.in;
    item.out = entry.out;

    item.srcAddr = entry.src;
    item.srcPort = entry.spt;

    item.dstAddr = entry.dst;
    item.dstPort = entry.dpt;

    item.proto = entry.proto;
    item.len = entry.len;
    item.timestamp = entry.timestamp;

    if(MyLog.enabled) {
      MyLog.d("LogFragment: NewLogEntry: [" + item.app.uidString + "] in=" + item.in + " out=" + item.out + " " + item.srcAddr + ":" + item.srcPort + " --> " + item.dstAddr + ":" + item.dstPort + " proto=" + item.proto + " len=" + item.len);
    }

    synchronized(listDataBuffer) {
      listDataBuffer.add(item);

      while(listDataBuffer.size() > maxLogEntries) {
        if(MyLog.enabled) {
          MyLog.d("Log buffer size reached maxLogEntries limit; truncating");
        }
        listDataBuffer.removeFirst();
      }
    }
  }

  public void clearLogEntriesOlderThan(long timerange) {
    MyLog.d("Clearing logFragment entries older than " + timerange);

    // Add any items in listDataBuffer to listDataUnfiltered
    synchronized(listDataBuffer) {
      synchronized(listDataUnfiltered) {
        for(ListItem item : listDataBuffer) {
          if(MyLog.enabled) {
            MyLog.d("Adding buffer item " + item);
          }
          listDataUnfiltered.add(item);
        }

        listDataBuffer.clear();
      }
    }

    long timestamp = System.currentTimeMillis() - timerange;

    MyLog.d("Setting timestamp " + timestamp);

    // Remove items older than timerange
    synchronized(listDataUnfiltered) {
      Iterator<ListItem> iterator = listDataUnfiltered.iterator();
      while(iterator.hasNext()) {
        ListItem item = iterator.next();

        if(MyLog.enabled) {
          MyLog.d("Checking item " + item.app.uid + " " + item.app.name + " " + item.timestamp);
        }

        if(item.timestamp < timestamp) {
          if(MyLog.enabled) {
            MyLog.d("Removing item");
          }
          iterator.remove();
        } else {
          // remaining entries should be >= timestamp
          break;
        }
      }

      synchronized(listData) {
        listData.clear();
        listData.addAll(listDataUnfiltered);
      }
    }

    NetworkLog.handler.post(new Runnable() {
      public void run() {
        if(NetworkLog.filterTextInclude.length() > 0 || NetworkLog.filterTextExclude.length() > 0) {
          setFilter("");
        } else {
          refreshAdapter();
        }
      }
    });
  }

  public void removeApp(String packageName) {
    if(listData == null || listDataBuffer == null || listDataUnfiltered == null) {
      return;
    }

    synchronized(listDataBuffer) {
      synchronized(listDataUnfiltered) {
        synchronized(listData) {
          ListItem item;
          Iterator<ListItem> iterator;

          iterator = listDataBuffer.iterator();
          while(iterator.hasNext()) {
            item = iterator.next();
            if(item.app.packageName.equals(packageName)) {
              iterator.remove();
            }
          }

          iterator = listDataUnfiltered.iterator();
          while(iterator.hasNext()) {
            item = iterator.next();
            if(item.app.packageName.equals(packageName)) {
              iterator.remove();
            }
          }

          iterator = listData.iterator();
          while(iterator.hasNext()) {
            item = iterator.next();
            if(item.app.packageName.equals(packageName)) {
              iterator.remove();
            }
          }
        }
      }
    }

    if(NetworkLog.filterTextInclude.length() > 0 || NetworkLog.filterTextExclude.length() > 0) {
      setFilter("");
    } else {
      refreshAdapter();
    }
  }

  public void pruneLogEntries() {
    if(listData == null || listDataBuffer == null || listDataUnfiltered == null) {
      return;
    }

    synchronized(listDataBuffer) {
      while(listDataBuffer.size() > maxLogEntries) {
        listDataBuffer.removeFirst();
      }
    }

    synchronized(listDataUnfiltered) {
      while(listDataUnfiltered.size() > maxLogEntries) {
        listDataUnfiltered.removeFirst();
      }
    }

    synchronized(listData) {
      while(listData.size() > maxLogEntries) {
        listData.removeFirst();
      }
    }

    refreshAdapter();
  }

  public void stopUpdater() {
    if(updater != null) {
      updater.stop();
    }
  }

  public boolean appFragmentNeedsRebuild = false;

  Runnable updaterRunner = new Runnable() {
    public void run() {
      if(MyLog.enabled) {
        MyLog.d("LogFragmentUpdater enter");
      }

      if(listDataBuffer == null || listData == null || listDataUnfiltered == null) {
        return;
      }

      int i = 0;
      boolean included = true;
      boolean excluded = false;

      long start = System.currentTimeMillis();

      synchronized(listDataBuffer) {
        synchronized(listData) {
          synchronized(listDataUnfiltered) {
            for(ListItem item : listDataBuffer) {
              if(NetworkLog.filterTextInclude.length() > 0) {
                included = testIncludeFilter(item);
              }

              if(NetworkLog.filterTextExclude.length() > 0) {
                excluded = testExcludeFilter(item);
              }

              if(included == true && excluded == false) {
                listData.add(item);
              }
              listDataUnfiltered.add(item);
              i++;
            }

            listDataBuffer.clear();
          }
        }
      }

      synchronized(listDataUnfiltered) {
        while(listDataUnfiltered.size() > maxLogEntries) {
          listDataUnfiltered.removeFirst();
        }
      }

      synchronized(listData) {
        while(listData.size() > maxLogEntries) {
          listData.removeFirst();
        }
      }

      refreshAdapter();
      long elapsed = System.currentTimeMillis() - start;

      if(MyLog.enabled) {
        MyLog.d("LogFragmentUpdater exit: added " + i + " items -- elapsed: " + elapsed);
      }

      if(appFragmentNeedsRebuild) {
        appFragmentNeedsRebuild = false;
        NetworkLog.appFragment.rebuildLogEntries();
        NetworkLog.appFragment.updaterRunOnce();
      }
    }
  };

  public void updaterRunOnce() {
    NetworkLog.handler.post(updaterRunner);
  }

  // todo: this is largely duplicated in AppFragment -- move to its own file
  private class ListViewUpdater implements Runnable {
    boolean running = false;

    public void stop() {
      running = false;
    }

    public void run() {
      running = true;
      MyLog.d("Starting LogFragmentUpdater " + this);

      while(running) {
        if(needsRefresh == true || (listDataBuffer != null && listDataBuffer.size() > 0)) {
          updaterRunOnce();
          needsRefresh = false;
        }

        try {
          Thread.sleep(1000);
        }
        catch(Exception e) {
          Log.d("NetworkLog", "LogFragmentListUpdater", e);
        }
      }

      if(MyLog.enabled) {
        MyLog.d("Stopped LogFragment updater " + this);
      }
    }
  }

  public void setFilter(CharSequence s) {
    // MyLog.d("[LogFragment] setFilter(" + s + ")");
    if(adapter != null) {
      adapter.getFilter().filter(s);
    }
  }

  String srcAddrResolved;
  String srcPortResolved;
  String dstAddrResolved;
  String dstPortResolved;
  String iface;
  boolean matched;

  public boolean testIncludeFilter(ListItem item) {
    matched = false;
    if(NetworkLog.resolveHosts) {
      srcAddrResolved = NetworkLog.resolver.resolveAddress(item.srcAddr);

      if(srcAddrResolved == null) {
        srcAddrResolved = "";
      }

      dstAddrResolved = NetworkLog.resolver.resolveAddress(item.dstAddr);

      if(dstAddrResolved == null) {
        dstAddrResolved = "";
      }
    } else {
      srcAddrResolved = "";
      dstAddrResolved = "";
    }

    if(NetworkLog.resolvePorts) {
      srcPortResolved = NetworkLog.resolver.resolveService(String.valueOf(item.srcPort));
      dstPortResolved = NetworkLog.resolver.resolveService(String.valueOf(item.dstPort));
    } else {
      srcPortResolved = "";
      dstPortResolved = "";
    }

    if(item.in != null && item.in.length() > 0) {
      iface = item.in;
    } else {
      iface = item.out;
    }

    for(String c : NetworkLog.filterTextIncludeList) {
      if((NetworkLog.filterNameInclude && item.app.nameLowerCase.contains(c))
          || (NetworkLog.filterUidInclude && item.app.uidString.equals(c))
          || (NetworkLog.filterAddressInclude &&
            ((item.srcAddr.contains(c) || StringPool.getLowerCase(srcAddrResolved).contains(c))
             || (item.dstAddr.contains(c) || StringPool.getLowerCase(dstAddrResolved).contains(c))))
          || (NetworkLog.filterPortInclude &&
            ((String.valueOf(item.srcPort).equals(c) || StringPool.getLowerCase(srcPortResolved).equals(c))
             || (String.valueOf(item.dstPort).equals(c) || StringPool.getLowerCase(dstPortResolved).equals(c))))
          || (NetworkLog.filterInterfaceInclude && iface.contains(c))
          || (NetworkLog.filterProtocolInclude &&
            (item.proto.equals(c) || StringPool.getLowerCase(NetworkLog.resolver.resolveProtocol(item.proto)).equals(c))))
      {
        matched = true;
        break;
      }
    }
    return matched;
  }

  boolean testExcludeFilter(ListItem item) {
    matched = false;

    if(NetworkLog.resolveHosts) {
      srcAddrResolved = NetworkLog.resolver.resolveAddress(item.srcAddr);

      if(srcAddrResolved == null) {
        srcAddrResolved = "";
      }

      dstAddrResolved = NetworkLog.resolver.resolveAddress(item.dstAddr);

      if(dstAddrResolved == null) {
        dstAddrResolved = "";
      }
    } else {
      srcAddrResolved = "";
      dstAddrResolved = "";
    }

    if(NetworkLog.resolvePorts) {
      srcPortResolved = NetworkLog.resolver.resolveService(String.valueOf(item.srcPort)); // fixme: get from stringpool
      dstPortResolved = NetworkLog.resolver.resolveService(String.valueOf(item.dstPort)); // fixme: get from stringpool
    } else {
      srcPortResolved = "";
      dstPortResolved = "";
    }

    if(item.in != null && item.in.length() > 0) {
      iface = item.in;
    } else {
      iface = item.out;
    }

    for(String c : NetworkLog.filterTextExcludeList) {
      if((NetworkLog.filterNameExclude && item.app.nameLowerCase.contains(c))
          || (NetworkLog.filterUidExclude && item.app.uidString.equals(c))
          || (NetworkLog.filterAddressExclude && ((item.srcAddr.contains(c) || StringPool.getLowerCase(srcAddrResolved).contains(c)) || (item.dstAddr.contains(c) || StringPool.getLowerCase(dstAddrResolved).contains(c))))
          || (NetworkLog.filterPortExclude && ((String.valueOf(item.srcPort).equals(c) || StringPool.getLowerCase(srcPortResolved).equals(c)) || (String.valueOf(item.dstPort).equals(c) || StringPool.getLowerCase(dstPortResolved).equals(c))))
          || (NetworkLog.filterInterfaceExclude && iface.contains(c))
          || (NetworkLog.filterProtocolExclude &&
            (item.proto.equals(c) || StringPool.getLowerCase(NetworkLog.resolver.resolveProtocol(item.proto)).equals(c))))
      {
        matched = true;
        break;
      }
    }
    return matched;
  }

  private class CustomAdapter extends ArrayAdapter<ListItem> implements Filterable {
    LayoutInflater mInflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    CustomFilter filter;

    public CustomAdapter(Context context, int resource, List<ListItem> objects) {
      super(context, resource, objects);
    }

    private class CustomFilter extends Filter {
      FilterResults results = new FilterResults();

      @Override
        protected FilterResults performFiltering(CharSequence constraint) {
          ArrayList<ListItem> originalItems = new ArrayList<ListItem>(listDataUnfiltered.size());
          ArrayList<ListItem> filteredItems = new ArrayList<ListItem>(listDataUnfiltered.size());
          int[] includedItemsIndex = new int[listDataUnfiltered.size()];
          int includedItemsPos = 0;
          int iteratorPos = -1;
          ListItem item;

          doNotRefresh = true;

          if(MyLog.enabled) {
            MyLog.d("[LogFragment] performFiltering");
          }

          synchronized(listDataUnfiltered) {
            originalItems.addAll(listDataUnfiltered);
          }

          if(NetworkLog.filterTextInclude.length() == 0 && NetworkLog.filterTextExclude.length() == 0) {
            MyLog.d("[LogFragment] no constraint item count: " + originalItems.size());
            results.values = originalItems;
            results.count = originalItems.size();
          } else {
            if(MyLog.enabled) {
              MyLog.d("[LogFragment] item count: " + originalItems.size());
            }

            if(NetworkLog.filterTextIncludeList.size() > 0) {
              Iterator<ListItem> iterator = originalItems.iterator();
              while(iterator.hasNext()) {
                item = iterator.next();
                iteratorPos++;

                if(MyLog.enabled) {
                  MyLog.d("[LogFragment] testing filtered item " + item + "; includes: [" + NetworkLog.filterTextInclude + "]");
                }

                matched = testIncludeFilter(item);

                if(matched) {
                  if(MyLog.enabled) {
                    MyLog.d("[LogFragment] adding filtered item " + item);
                  }
                  includedItemsIndex[includedItemsPos++] = iteratorPos;
                }
              }
            } else {
              int count = originalItems.size();
              for(int i = 0; i < count; i++) {
                includedItemsIndex[includedItemsPos++] = i;
              }
            }

            if(NetworkLog.filterTextExcludeList.size() > 0) {
              for(int i = 0; i < includedItemsPos; i++) {
                item = originalItems.get(includedItemsIndex[i]);

                if(MyLog.enabled) {
                  MyLog.d("[LogFragment] testing filtered item " + item + "; excludes: [" + NetworkLog.filterTextExclude + "]");
                }

                matched = testExcludeFilter(item);

                if(matched) {
                  if(MyLog.enabled) {
                    MyLog.d("[LogFragment] excluding filtered item " + item);
                  }
                } else {
                  filteredItems.add(item);
                }
              }
            } else {
              // no exclusion filter, add all included items to filteredItems
              for(int i = 0; i < includedItemsPos; i++) {
                filteredItems.add(originalItems.get(includedItemsIndex[i]));
              }
            }

            results.values = filteredItems;
            results.count = filteredItems.size();
          }

          if(MyLog.enabled) {
            MyLog.d("[LogFragment] filter returning " + results.count + " items");
          }
          return results;
        }

      @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
          if(MyLog.enabled) {
            MyLog.d("[LogFragment] Publishing filter results");
          }

          synchronized(listData) {
            listData.clear();
            listData.addAll((ArrayList<ListItem>) results.values);
            if(MyLog.enabled) {
              MyLog.d("[LogFilter] listdata size after filter: " + listData.size());
            }
          }

          doNotRefresh = false;
          refreshAdapter();
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
        TextView iface;
        final TextView srcAddr;
        TextView srcPort;
        final TextView dstAddr;
        TextView dstPort;
        TextView len;
        TextView timestamp;

        final ListItem item = getItem(position);

        if(convertView == null) {
          convertView = mInflater.inflate(R.layout.logitem, null);
          holder = new ViewHolder(convertView);
          convertView.setTag(holder);
        }

        holder = (ViewHolder) convertView.getTag();

        icon = holder.getIcon();
        icon.setTag(item.app.packageName);
        icon.setImageDrawable(ApplicationsTracker.loadIcon(getActivity().getApplicationContext(), icon, item.app.packageName));

        name = holder.getName();
        name.setText("(" + item.app.uid + ")" + " " + item.app.name);

        iface = holder.getInterface();

        String proto;
        if(item.proto != null && item.proto.length() > 0) {
          proto = NetworkLog.resolver.resolveProtocol(item.proto) + "/";
        } else {
          proto = "";
        }

        if(item.in != null && item.in.length() != 0) {
          iface.setText(proto + item.in);
        } else {
          iface.setText(proto + item.out);
        }

        srcAddr = holder.getSrcAddr();
        srcAddr.setTag(item.srcAddr);

        if(NetworkLog.resolveHosts) {
          String resolved = NetworkLog.resolver.getResolvedAddress(item.srcAddr);

          if(resolved == null) {
            NetworkResolverUpdater updater = new NetworkResolverUpdater() {
              public void run() {
                String tag = (String) srcAddr.getTag();
                if(tag != null && tag.equals(item.srcAddr)) {
                  srcAddr.setText("SRC: " + resolved);
                }
              }
            };
            resolved = NetworkLog.resolver.resolveAddress(item.srcAddr, updater);
          }

          if(resolved != null) {
            srcAddr.setText("SRC: " + resolved);
          } else {
            srcAddr.setText("SRC: " + item.srcAddr);
          }
        } else {
          srcAddr.setText("SRC: " + item.srcAddr);
        }

        srcPort = holder.getSrcPort();

        if(NetworkLog.resolvePorts) {
          srcPort.setText(NetworkLog.resolver.resolveService(String.valueOf(item.srcPort)));
        } else {
          srcPort.setText(String.valueOf(item.srcPort));
        }

        dstAddr = holder.getDstAddr();
        dstAddr.setTag(item.dstAddr);

        if(NetworkLog.resolveHosts) {
          String resolved = NetworkLog.resolver.getResolvedAddress(item.dstAddr);
          if(resolved == null) {
            NetworkResolverUpdater updater = new NetworkResolverUpdater() {
              public void run() {
                String tag = (String) dstAddr.getTag();
                if(tag != null && tag.equals(item.dstAddr)) {
                  dstAddr.setText("DST: " + resolved);
                }
              }
            };
            resolved = NetworkLog.resolver.resolveAddress(item.dstAddr, updater);
          }

          if(resolved != null) {
            dstAddr.setText("DST: " + resolved);
          } else {
            dstAddr.setText("DST: " + item.dstAddr);
          }
        } else {
          dstAddr.setText("DST: " + item.dstAddr);
        }

        dstPort = holder.getDstPort();

        if(NetworkLog.resolvePorts) {
          dstPort.setText(NetworkLog.resolver.resolveService(String.valueOf(item.dstPort)));
        } else {
          dstPort.setText(String.valueOf(item.dstPort));
        }

        len = holder.getLen();
        len.setText("LEN: " + item.len);

        timestamp = holder.getTimestamp();

        timestamp.setText(Timestamp.getTimestamp(item.timestamp));

        return convertView;
      }
  }

  private class ViewHolder {
    private View mView;
    private ImageView mIcon = null;
    private TextView mName = null;
    private TextView mInterface = null;
    private TextView mSrcAddr = null;
    private TextView mSrcPort = null;
    private TextView mDstAddr = null;
    private TextView mDstPort = null;
    private TextView mLen = null;
    private TextView mTimestamp = null;

    public ViewHolder(View view) {
      mView = view;
    }

    public ImageView getIcon() {
      if(mIcon == null) {
        mIcon = (ImageView) mView.findViewById(R.id.logIcon);
      }

      return mIcon;
    }

    public TextView getName() {
      if(mName == null) {
        mName = (TextView) mView.findViewById(R.id.logName);
      }

      return mName;
    }

    public TextView getInterface() {
      if(mInterface == null) {
        mInterface = (TextView) mView.findViewById(R.id.logInterface);
      }

      return mInterface;
    }

    public TextView getSrcAddr() {
      if(mSrcAddr == null) {
        mSrcAddr = (TextView) mView.findViewById(R.id.srcAddr);
      }

      return mSrcAddr;
    }

    public TextView getSrcPort() {
      if(mSrcPort == null) {
        mSrcPort = (TextView) mView.findViewById(R.id.srcPort);
      }

      return mSrcPort;
    }

    public TextView getDstAddr() {
      if(mDstAddr == null) {
        mDstAddr = (TextView) mView.findViewById(R.id.dstAddr);
      }

      return mDstAddr;
    }

    public TextView getDstPort() {
      if(mDstPort == null) {
        mDstPort = (TextView) mView.findViewById(R.id.dstPort);
      }

      return mDstPort;
    }

    public TextView getLen() {
      if(mLen == null) {
        mLen = (TextView) mView.findViewById(R.id.len);
      }

      return mLen;
    }

    public TextView getTimestamp() {
      if(mTimestamp == null) {
        mTimestamp = (TextView) mView.findViewById(R.id.timestamp);
      }

      return mTimestamp;
    }
  }
}
