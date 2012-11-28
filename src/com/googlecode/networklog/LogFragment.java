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
import android.content.ClipboardManager;
import android.content.ClipData;

import android.support.v4.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

public class LogFragment extends Fragment {
  // bound to adapter
  protected ArrayList<ListItem> listData;
  // buffers incoming log entries
  protected ArrayList<ListItem> listDataBuffer;
  // holds all entries, used for filtering
  protected ArrayList<ListItem> listDataUnfiltered;
  private CustomAdapter adapter;
  private ListViewUpdater updater;
  private NetworkLog parent = null;

  protected class ListItem {
    protected Drawable mIcon;
    protected int mUid;
    protected String mUidString;
    protected String in;
    protected String out;
    protected String mName;
    protected String mNameLowerCase;
    protected String srcAddr;
    protected int srcPort;
    protected String dstAddr;
    protected int dstPort;
    protected int len;
    protected long timestamp;

    ListItem(Drawable icon, int uid, String name) {
      mIcon = icon;
      mUid = uid;
      mUidString = null;
      mName = name;
      mNameLowerCase = null;
    }

    @Override
      public String toString() {
        return mName;
      }
  }

  public void clear() {
    synchronized(listData) {
      synchronized(listDataBuffer) {
        synchronized(listDataUnfiltered) {
          listData.clear();
          listDataBuffer.clear();
          listDataUnfiltered.clear();
          adapter.notifyDataSetChanged();
        }
      }
    }
  }

  public void refreshAdapter() {
    adapter.notifyDataSetChanged();
    MyLog.d("Refreshed LogFragment adapter");
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

      listData = new ArrayList<ListItem>();
      listDataBuffer = new ArrayList<ListItem>();
      MyLog.d("Created new listDataBuffer " + listDataBuffer.toString());
      listDataUnfiltered = new ArrayList<ListItem>();

      adapter = new CustomAdapter(getActivity().getApplicationContext(), R.layout.logitem, listData);

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

    if(NetworkLog.settings == null) {
      NetworkLog activity = (NetworkLog) getActivity();

      if(activity != null) {
        activity.loadSettings();
      }
    }

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

    if(NetworkLog.filterTextInclude.length() > 0 || NetworkLog.filterTextExclude.length() > 0) {
      // trigger filtering
      setFilter("");
      adapter.notifyDataSetChanged();
    }
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
    ClipData clip = ClipData.newPlainText("NetworkLog Source IP", srcAddr + ":" + srcPort);
    clipboard.setPrimaryClip(clip);
  }

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
    ClipData clip = ClipData.newPlainText("NetworkLog Dest IP", dstAddr + ":" + dstPort);
    clipboard.setPrimaryClip(clip);
  }

  public void showGraph(ListItem item) {
    if(item.mUidString == null) {
      item.mUidString = String.valueOf(item.mUid);
    }

    startActivity(new Intent(getActivity().getApplicationContext(), AppTimelineGraph.class)
        .putExtra("app_uid", item.mUidString)
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
    updater = new ListViewUpdater();
    new Thread(updater, "LogFragmentUpdater").start();
  }

  public void onNewLogEntry(final LogEntry entry) {
    ApplicationsTracker.AppEntry appEntry = ApplicationsTracker.installedAppsHash.get(String.valueOf(entry.uid));

    if(appEntry == null) {
      MyLog.d("LogFragment: No appEntry for uid " + entry.uid);
      return;
    }

    final ListItem item = new ListItem(appEntry.icon, appEntry.uid, appEntry.name);

    if(entry.in != null) {
      item.in = entry.in;
    } else {
      item.in = null;
    }

    if(entry.out != null) {
      item.out = entry.out;
    } else {
      item.out = null;
    }

    item.srcAddr = entry.src;
    item.srcPort = entry.spt;

    item.dstAddr = entry.dst;
    item.dstPort = entry.dpt;

    item.len = entry.len;
    item.timestamp = entry.timestamp;

    if(MyLog.enabled) {
      if(item.mUidString == null) {
        item.mUidString = String.valueOf(item.mUid);
      }
      MyLog.d("LogFragment: NewLogEntry: [" + item.mUidString + "] in=" + item.in + " out=" + item.out + " " + item.srcAddr + ":" + item.srcPort + " --> " + item.dstAddr + ":" + item.dstPort + " [" + item.len + "]");
    }

    synchronized(listDataBuffer) {
      listDataBuffer.add(item);

      while(listDataBuffer.size() > NetworkLog.settings.getMaxLogEntries()) {
        listDataBuffer.remove(0);
      }
    }
  }

  public void pruneLogEntries() {
    long maxLogEntries = NetworkLog.settings.getMaxLogEntries();

    synchronized(listDataBuffer) {
      while(listDataBuffer.size() > maxLogEntries) {
        listDataBuffer.remove(0);
      }
    }

    synchronized(listDataUnfiltered) {
      while(listDataUnfiltered.size() > maxLogEntries) {
        listDataUnfiltered.remove(0);
      }
    }

    synchronized(listData) {
      while(listData.size() > maxLogEntries) {
        listData.remove(0);
      }
      adapter.notifyDataSetChanged();
    }
  }

  public void stopUpdater() {
    if(updater != null) {
      updater.stop();
    }
  }

  // todo: this is largely duplicated in AppFragment -- move to its own file
  private class ListViewUpdater implements Runnable {
    boolean running = false;
    Runnable runner = new Runnable() {
      public void run() {
        MyLog.d("LogFragmentUpdater enter");
        int i = 0;
        long maxLogEntries = NetworkLog.settings.getMaxLogEntries();

        synchronized(listDataBuffer) {
          synchronized(listData) {
            synchronized(listDataUnfiltered) {
              for(ListItem item : listDataBuffer) {
                listData.add(item);
                listDataUnfiltered.add(item);
                i++;
              }

              listDataBuffer.clear();
            }
          }
        }

        synchronized(listDataUnfiltered) {
          while(listDataUnfiltered.size() > maxLogEntries) {
            listDataUnfiltered.remove(0);
          }
        }

        synchronized(listData) {
          while(listData.size() > maxLogEntries) {
            listData.remove(0);
          }
        }

        if(NetworkLog.filterTextInclude.length() > 0 || NetworkLog.filterTextExclude.length() > 0)
          // trigger filtering
        {
          setFilter("");
        }

        adapter.notifyDataSetChanged();

        MyLog.d("LogFragmentUpdater exit: added " + i + " items");
      }
    };

    public void stop() {
      running = false;
    }

    public void run() {
      running = true;
      MyLog.d("Starting LogFragmentUpdater " + this);

      while(running) {
        if(listDataBuffer.size() > 0) {
          Activity activity = getActivity();
          if(activity != null) {
            activity.runOnUiThread(runner);
          }
        }

        try {
          Thread.sleep(1000);
        }
        catch(Exception e) {
          Log.d("NetworkLog", "LogFragmentListUpdater", e);
        }
      }

      MyLog.d("Stopped LogFragment updater " + this);
    }
  }

  public void setFilter(CharSequence s) {
    // MyLog.d("[LogFragment] setFilter(" + s + ")");
    adapter.getFilter().filter(s);
  }

  private class CustomAdapter extends ArrayAdapter<ListItem> implements Filterable {
    LayoutInflater mInflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    CustomFilter filter;
    ArrayList<ListItem> originalItems = new ArrayList<ListItem>();

    public CustomAdapter(Context context, int resource, List<ListItem> objects) {
      super(context, resource, objects);
    }

    private class CustomFilter extends Filter {
      FilterResults results = new FilterResults();
      ArrayList<ListItem> filteredItems = new ArrayList<ListItem>();
      ArrayList<ListItem> localItems = new ArrayList<ListItem>();

      @Override
        protected FilterResults performFiltering(CharSequence constraint) {
          MyLog.d("[LogFragment] performFiltering");

          synchronized(listDataUnfiltered) {
            originalItems.clear();
            originalItems.addAll(listDataUnfiltered);
          }

          if(NetworkLog.filterTextInclude.length() == 0 && NetworkLog.filterTextExclude.length() == 0) {
            // MyLog.d("[LogFragment] no constraint item count: " + originalItems.size());
            results.values = originalItems;
            results.count = originalItems.size();
          } else {
            localItems.clear();
            filteredItems.clear();
            localItems.addAll(originalItems);
            int count = localItems.size();

            // MyLog.d("[LogFragment] item count: " + count);

            if(NetworkLog.filterTextIncludeList.size() == 0) {
              filteredItems.addAll(localItems);
            } else {
              for(int i = 0; i < count; i++) {
                ListItem item = localItems.get(i);
                // MyLog.d("[LogFragment] testing filtered item " + item + "; includes: [" + NetworkLog.filterTextInclude + "]");

                boolean matched = false;

                String srcAddrResolved;
                String srcPortResolved;
                String dstAddrResolved;
                String dstPortResolved;

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

                if(item.mUidString == null) {
                  item.mUidString = String.valueOf(item.mUid);
                }

                if(item.mNameLowerCase == null) {
                  item.mNameLowerCase = StringPool.getLowerCase(item.mName);
                }

                for(String c : NetworkLog.filterTextIncludeList) {
                  if((NetworkLog.filterNameInclude && item.mNameLowerCase.contains(c))
                      || (NetworkLog.filterUidInclude && item.mUidString.equals(c))
                      || (NetworkLog.filterAddressInclude && 
                        ((item.srcAddr.contains(c) || StringPool.getLowerCase(srcAddrResolved).contains(c)) 
                         || (item.dstAddr.contains(c) || StringPool.getLowerCase(dstAddrResolved).contains(c))))
                      || (NetworkLog.filterPortInclude && 
                        ((StringPool.getLowerCase(String.valueOf(item.srcPort)).equals(c) || StringPool.getLowerCase(srcPortResolved).equals(c))
                         || (StringPool.getLowerCase(String.valueOf(item.dstPort)).equals(c) || StringPool.getLowerCase(dstPortResolved).equals(c)))))
                  {
                    matched = true;
                  }
                }

                if(matched) {
                   // MyLog.d("[LogFragment] adding filtered item " + item);
                  filteredItems.add(item);
                }
              }
            }

            if(NetworkLog.filterTextExcludeList.size() > 0) {
              count = filteredItems.size();

              for(int i = count - 1; i >= 0; i--) {
                ListItem item = filteredItems.get(i);
                // MyLog.d("[LogFragment] testing filtered item " + item + "; excludes: [" + NetworkLog.filterTextExclude + "]");

                boolean matched = false;

                String srcAddrResolved;
                String srcPortResolved;
                String dstAddrResolved;
                String dstPortResolved;

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

                if(item.mUidString == null) {
                  item.mUidString = String.valueOf(item.mUid);
                }

                if(item.mNameLowerCase == null) {
                  item.mNameLowerCase = StringPool.getLowerCase(item.mName);
                }

                for(String c : NetworkLog.filterTextExcludeList) {
                  if((NetworkLog.filterNameExclude && item.mNameLowerCase.contains(c))
                      || (NetworkLog.filterUidExclude && item.mUidString.contains(c))
                      || (NetworkLog.filterAddressExclude && ((item.srcAddr.contains(c) || StringPool.getLowerCase(srcAddrResolved).contains(c)) || (item.dstAddr.contains(c) || StringPool.getLowerCase(dstAddrResolved).contains(c))))
                      || (NetworkLog.filterPortExclude && ((String.valueOf(item.srcPort).equals(c) || StringPool.getLowerCase(srcPortResolved).equals(c)) || (String.valueOf(item.dstPort).equals(c) || StringPool.getLowerCase(dstPortResolved).equals(c)))))
                  {
                    matched = true;
                  }
                }

                if(matched) {
                  // MyLog.d("[LogFragment] removing filtered item " + item);
                  filteredItems.remove(i);
                }
              }
            }

            results.values = filteredItems;
            results.count = filteredItems.size();
          }

          MyLog.d("[LogFragment] filter returning " + results.count + " items");
          return results;
        }

      @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
          MyLog.d("[LogFragment] Publishing filter results");

          synchronized(listData) {
            listData.clear();
            listData.addAll((ArrayList<ListItem>) results.values);
            MyLog.d("[LogFilter] listdata size after filter: " + listData.size());
            refreshAdapter();
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
        TextView iface;
        TextView srcAddr;
        TextView srcPort;
        TextView dstAddr;
        TextView dstPort;
        TextView len;
        TextView timestamp;

        ListItem item = getItem(position);

        if(convertView == null) {
          convertView = mInflater.inflate(R.layout.logitem, null);
          holder = new ViewHolder(convertView);
          convertView.setTag(holder);
        }

        holder = (ViewHolder) convertView.getTag();
        icon = holder.getIcon();

        if(item.mIcon == null) {
          if(item.mUidString == null) {
            item.mUidString = String.valueOf(item.mUid);
          }
          item.mIcon = ApplicationsTracker.loadIcon(getActivity().getApplicationContext(), ApplicationsTracker.installedAppsHash.get(item.mUidString).packageName);
        }

        icon.setImageDrawable(item.mIcon);

        name = holder.getName();
        name.setText("(" + item.mUid + ")" + " " + item.mName);

        iface = holder.getInterface();

        if(item.in != null && item.in.length() != 0) {
          iface.setText(item.in);
        } else {
          iface.setText(item.out);
        }

        srcAddr = holder.getSrcAddr();

        if(NetworkLog.resolveHosts) {
          String resolved = NetworkLog.resolver.resolveAddress(item.srcAddr);

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

        if(NetworkLog.resolveHosts) {
          String resolved = NetworkLog.resolver.resolveAddress(item.dstAddr);

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
