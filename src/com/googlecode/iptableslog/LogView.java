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
import android.widget.Toast;
import android.util.TypedValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

public class LogView extends Activity
{
  // bound to adapter
  protected ArrayList<ListItem> listData;
  // buffers incoming log entries
  protected ArrayList<ListItem> listDataBuffer;
  // holds all entries, used for filtering
  protected ArrayList<ListItem> listDataUnfiltered;
  protected long maxLogEntries;
  private CustomAdapter adapter;
  private ListViewUpdater updater;
  public TextView statusText;

  protected class ListItem {
    protected Drawable mIcon;
    protected int mUid;
    protected String mUidString;
    protected String mName;
    protected String mNameLowerCase;
    protected String srcAddr;
    protected String srcAddrString;
    protected int srcPort;
    protected String srcPortString;
    protected String dstAddr;
    protected String dstAddrString;
    protected int dstPort;
    protected String dstPortString;
    protected int len;
    protected String timestamp;

    ListItem(Drawable icon, int uid, String name) {
      mIcon = icon;
      mUid = uid;
      mUidString = String.valueOf(uid);
      mName = name;
      mNameLowerCase = name.toLowerCase();
    }

    @Override
      public String toString() {
        return mName;
      }
  }

  public void refreshAdapter() {
    adapter.notifyDataSetChanged();
  }

  public void refreshHosts() {
    synchronized(listData) {
      for(ListItem item : listData) {
        if(IptablesLog.resolveHosts) {
          item.srcAddrString = IptablesLog.resolver.resolveAddress(String.valueOf(item.srcAddr));
          item.dstAddrString = IptablesLog.resolver.resolveAddress(String.valueOf(item.dstAddr));
        } else {
          item.srcAddrString = String.valueOf(item.srcAddr);
          item.dstAddrString = String.valueOf(item.dstAddr);
        }
      }

      adapter.notifyDataSetChanged();
    }
  }

  public void refreshPorts() {
    synchronized(listData) {
      for(ListItem item : listData) {
        if(IptablesLog.resolvePorts) {
          item.srcPortString = IptablesLog.resolver.resolveService(String.valueOf(item.srcPort));
          item.dstPortString = IptablesLog.resolver.resolveService(String.valueOf(item.dstPort));
        } else {
          item.srcPortString = String.valueOf(item.srcPort);
          item.dstPortString = String.valueOf(item.dstPort);
        }
      }

      adapter.notifyDataSetChanged();
    }
  }

  public void refreshIcons() {
    synchronized(listData) {
      for(ListItem item : listData) {
        if(item.mIcon == null) {
          MyLog.d("[LogView] refreshing icon for " + item);
          ApplicationsTracker.AppEntry entry = ApplicationsTracker.installedAppsHash.get(String.valueOf(item.mUid));

          if(entry == null) {
            MyLog.d("[LogView] no app entry found, icon not refreshed");
          } else {
            item.mIcon = entry.icon;
          }
        }
      }

      adapter.notifyDataSetChanged();
    }
  }

  /** Called when the activity is first created. */
  @Override
    public void onCreate(Bundle savedInstanceState)
    {
      super.onCreate(savedInstanceState);

      MyLog.d("LogView created");

      LinearLayout layout = new LinearLayout(this);
      layout.setOrientation(LinearLayout.VERTICAL);

      statusText = new TextView(this);
      layout.addView(statusText);

      if(IptablesLog.data == null) {
        listData = new ArrayList<ListItem>();
        listDataBuffer = new ArrayList<ListItem>();
        listDataUnfiltered = new ArrayList<ListItem>();
      } else {
        restoreData(IptablesLog.data);
      }

      adapter = new CustomAdapter(this, R.layout.logitem, listData);

      ListView listView = new ListView(this);
      listView.setAdapter(adapter);
      listView.setTextFilterEnabled(true);
      listView.setFastScrollEnabled(true);
      listView.setSmoothScrollbarEnabled(false);
      listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
      listView.setStackFromBottom(true);

      listView.setOnItemClickListener(new CustomOnItemClickListener());

      layout.addView(listView);

      setContentView(layout);

      maxLogEntries = IptablesLog.settings.getMaxLogEntries();

      if(IptablesLog.filterTextInclude.length() > 0 || IptablesLog.filterTextExclude.length() > 0) {
        // trigger filtering
        setFilter("");
        adapter.notifyDataSetChanged();
      }
    }

  private class CustomOnItemClickListener implements OnItemClickListener {
    @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ListItem item = listData.get(position);
        startActivity(new Intent(getApplicationContext(), AppTimelineGraph.class)
            .putExtra("app_uid", item.mUidString)
            .putExtra("src_addr", item.srcAddr)
            .putExtra("src_port", item.srcPort)
            .putExtra("dst_addr", item.dstAddr)
            .putExtra("dst_port", item.dstPort));
      }
  }

  public void startUpdater() {
    updater = new ListViewUpdater();
    new Thread(updater, "LogViewUpdater").start();
  }

  public void restoreData(IptablesLogData data) {
    listData = data.logViewListData;
    listDataBuffer = data.logViewListDataBuffer;
    listDataUnfiltered = data.logViewListDataUnfiltered;
  }

  @Override
    public void onBackPressed() {
      IptablesLog parent = (IptablesLog) getParent();
      parent.confirmExit(this);
    }

  public void onNewLogEntry(final IptablesLogService.LogEntry entry) {
    ApplicationsTracker.AppEntry appEntry = ApplicationsTracker.installedAppsHash.get(String.valueOf(entry.uid));

    if(appEntry == null) {
      MyLog.d("LogView: No appEntry for uid " + entry.uid);
      return;
    }

    final ListItem item = new ListItem(appEntry.icon, appEntry.uid, appEntry.name);

    item.srcAddr = entry.src;
    item.srcPort = entry.spt;

    if(IptablesLog.resolveHosts) {
      item.srcAddrString = IptablesLog.resolver.resolveAddress(entry.src);
    }
    else {
      item.srcAddrString = entry.src;
    }

    if(IptablesLog.resolvePorts) {
      item.srcPortString = IptablesLog.resolver.resolveService(String.valueOf(entry.spt));
    }
    else {
      item.srcPortString = String.valueOf(entry.spt);
    }

    item.dstAddr = entry.dst;
    item.dstPort = entry.dpt;

    if(IptablesLog.resolveHosts) {
      item.dstAddrString = IptablesLog.resolver.resolveAddress(entry.dst);
    }
    else {
      item.dstAddrString = entry.dst;
    }

    if(IptablesLog.resolvePorts) {
      item.dstPortString = IptablesLog.resolver.resolveService(String.valueOf(entry.dpt));
    }
    else {
      item.dstPortString = String.valueOf(entry.dpt);
    }

    item.len = entry.len;
    item.timestamp = entry.timestampString;

    MyLog.d("LogView: Add item: " + item.srcAddr + " " + item.srcPort + " " + item.dstAddr + " " + item.dstPort + " " + item.len);

    synchronized(listDataBuffer) {
      listDataBuffer.add(item);

      while(listDataBuffer.size() > maxLogEntries) {
        listDataBuffer.remove(0);
      }
    }
  }

  public void resetData() {
    synchronized(listDataBuffer) {
      listDataBuffer.clear();
    }

    synchronized(listData) {
      listData.clear();
    }

    adapter.notifyDataSetChanged();
  }

  public void pruneLogEntries() {
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
    }

    if(!IptablesLog.outputPaused) {
      adapter.notifyDataSetChanged();
    }
  }

  public void stopUpdater() {
    if(updater != null) {
      updater.stop();
    }
  }

  // todo: this is largely duplicated in AppView -- move to its own file
  private class ListViewUpdater implements Runnable {
    boolean running = false;
    Runnable runner = new Runnable() {
      public void run() {
        MyLog.d("LogViewUpdater enter");
        int i = 0;

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

        if(IptablesLog.filterTextInclude.length() > 0 || IptablesLog.filterTextExclude.length() > 0)
          // trigger filtering
        {
          setFilter("");
        }

        if(!IptablesLog.outputPaused) {
          adapter.notifyDataSetChanged();
        }

        MyLog.d("LogViewUpdater exit: added " + i + " items");
      }
    };

    public void stop() {
      running = false;
    }

    public void run() {
      running = true;
      MyLog.d("Starting LogView updater " + this);

      while(running) {
        if(listDataBuffer.size() > 0) {
          runOnUiThread(runner);
        }

        try {
          Thread.sleep(2500);
        }
        catch(Exception e) {
          Log.d("IptablesLog", "LogViewListUpdater", e);
        }
      }

      MyLog.d("Stopped LogView updater " + this);
    }
  }

  public void setFilter(CharSequence s) {
    // MyLog.d("[LogView] setFilter(" + s + ")");
    adapter.getFilter().filter(s);
  }

  private class CustomAdapter extends ArrayAdapter<ListItem> implements Filterable {
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

          synchronized(listDataUnfiltered) {
            originalItems.clear();
            originalItems.addAll(listDataUnfiltered);
          }

          if(IptablesLog.filterTextInclude.length() == 0 && IptablesLog.filterTextExclude.length() == 0) {
            // MyLog.d("[LogView] no constraint item count: " + originalItems.size());
            results.values = originalItems;
            results.count = originalItems.size();
          } else {
            ArrayList<ListItem> filteredItems = new ArrayList<ListItem>();
            ArrayList<ListItem> localItems = new ArrayList<ListItem>();
            localItems.addAll(originalItems);
            int count = localItems.size();

            MyLog.d("[LogView] item count: " + count);

            if(IptablesLog.filterTextIncludeList.size() == 0) {
              filteredItems.addAll(localItems);
            } else {
              for(int i = 0; i < count; i++) {
                ListItem item = localItems.get(i);
                // MyLog.d("[LogView] testing filtered item " + item + "; includes: [" + IptablesLog.filterTextInclude + "]");

                boolean matched = false;

                for(String c : IptablesLog.filterTextIncludeList) {
                  if((IptablesLog.filterNameInclude && item.mNameLowerCase.contains(c))
                      || (IptablesLog.filterUidInclude && item.mUidString.equals(c))
                      || (IptablesLog.filterAddressInclude && (item.srcAddrString.contains(c) || item.dstAddrString.contains(c)))
                      || (IptablesLog.filterPortInclude && (item.srcPortString.toLowerCase().equals(c) || item.dstPortString.toLowerCase().equals(c))))
                  {
                    matched = true;
                  }
                }

                if(matched) {
                  // MyLog.d("[LogView] adding filtered item " + item);
                  filteredItems.add(item);
                }
              }
            }

            if(IptablesLog.filterTextExcludeList.size() > 0) {
              count = filteredItems.size();

              for(int i = count - 1; i >= 0; i--) {
                ListItem item = filteredItems.get(i);
                // MyLog.d("[LogView] testing filtered item " + item + "; excludes: [" + IptablesLog.filterTextExclude + "]");

                boolean matched = false;

                for(String c : IptablesLog.filterTextExcludeList) {
                  if((IptablesLog.filterNameExclude && item.mNameLowerCase.contains(c))
                      || (IptablesLog.filterUidExclude && item.mUidString.contains(c))
                      || (IptablesLog.filterAddressExclude && (item.srcAddrString.contains(c) || item.dstAddrString.contains(c)))
                      || (IptablesLog.filterPortExclude && (item.srcPortString.toLowerCase().equals(c) || item.dstPortString.toLowerCase().equals(c))))
                  {
                    matched = true;
                  }
                }

                if(matched) {
                  // MyLog.d("[LogView] removing filtered item " + item);
                  filteredItems.remove(i);
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

            if(!IptablesLog.outputPaused) {
              notifyDataSetChanged();
            }
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
        icon.setImageDrawable(item.mIcon);

        name = holder.getName();
        name.setText("(" + item.mUid + ")" + " " + item.mName);

        srcAddr = holder.getSrcAddr();
        srcAddr.setText("SRC: " + item.srcAddrString);

        srcPort = holder.getSrcPort();
        srcPort.setText(item.srcPortString);

        dstAddr = holder.getDstAddr();
        dstAddr.setText("DST: " + item.dstAddrString);

        dstPort = holder.getDstPort();
        dstPort.setText(item.dstPortString);

        len = holder.getLen();
        len.setText("LEN: " + item.len);

        timestamp = holder.getTimestamp();
        timestamp.setText(item.timestamp);

        return convertView;
      }
  }

  private class ViewHolder {
    private View mView;
    private ImageView mIcon = null;
    private TextView mName = null;
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
