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

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

public class LogView extends Activity implements IptablesLogListener
{
  // bound to adapter
  protected ArrayList<ListItem> listData;
  // buffers incoming log entries
  protected ArrayList<ListItem> listDataBuffer;
  // holds all entries, used for filtering
  protected ArrayList<ListItem> listDataUnfiltered;
  protected long maxLogEntries;
  private ListView listView;
  private CustomAdapter adapter;
  private ListViewUpdater updater;

  protected class ListItem {
    protected Drawable mIcon;
    protected int mUid;
    protected String mUidString;
    protected String mName;
    protected String mNameLowerCase;
    protected String srcAddr;
    protected int srcPort;
    protected String srcPortString;
    protected String dstAddr;
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

  /** Called when the activity is first created. */
  @Override
    public void onCreate(Bundle savedInstanceState)
    {
      super.onCreate(savedInstanceState);

      MyLog.d("LogView created");

      LinearLayout layout = new LinearLayout(this);
      layout.setOrientation(LinearLayout.VERTICAL);

      TextView tv = new TextView(this);
      tv.setText("Iptables Log");

      layout.addView(tv);

      if(IptablesLog.data == null) {
        listData = new ArrayList<ListItem>();
        listDataBuffer = new ArrayList<ListItem>();
        listDataUnfiltered = new ArrayList<ListItem>();
      } else {
        restoreData(IptablesLog.data);
      }

      adapter = new CustomAdapter(this, R.layout.logitem, listData);

      listView = new ListView(this);
      listView.setAdapter(adapter);
      listView.setTextFilterEnabled(true);
      listView.setFastScrollEnabled(true);
      listView.setSmoothScrollbarEnabled(false);
      listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
      listView.setStackFromBottom(true);

      layout.addView(listView);

      setContentView(layout);

      maxLogEntries = IptablesLog.settings.getMaxLogEntries();

      if(IptablesLog.filterText.length() > 0) {
        setFilter(IptablesLog.filterText);
        adapter.notifyDataSetChanged();
      }
    }

  public void startUpdater() {
    updater = new ListViewUpdater();
    new Thread(updater, "LogViewUpdater").start();
  }

  public void attachListener() {
    MyLog.d("Adding LogView listener " + this);
    IptablesLog.logTracker.addListener(this);
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

  public void onNewLogEntry(final IptablesLogTracker.LogEntry entry) {
    ApplicationsTracker.AppEntry appEntry = ApplicationsTracker.installedAppsHash.get(String.valueOf(entry.uid));

    if(appEntry == null) {
      MyLog.d("LogView: No appEntry for uid " + entry.uid);
      return;
    }

    final ListItem item = new ListItem(appEntry.icon, appEntry.uid, appEntry.name);

    item.srcAddr = entry.src;
    item.srcPort = entry.spt;
    item.srcPortString = String.valueOf(entry.spt);
    item.dstAddr = entry.dst;
    item.dstPort = entry.dpt;
    item.dstPortString = String.valueOf(entry.dpt);
    item.len = entry.len;
    item.timestamp = entry.timestamp;

    MyLog.d("LogView: Add item: " + item.srcAddr + " " + item.srcPort + " " + item.dstAddr + " " + item.dstPort + " " + item.len);

    synchronized(listDataBuffer) {
      listDataBuffer.add(item);

      while(listDataBuffer.size() > maxLogEntries)
        listDataBuffer.remove(0);
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
      while(listDataBuffer.size() > maxLogEntries)
        listDataBuffer.remove(0);
    }

    synchronized(listDataUnfiltered) {
      while(listDataUnfiltered.size() > maxLogEntries)
        listDataUnfiltered.remove(0);
    }

    synchronized(listData) {
      while(listData.size() > maxLogEntries)
        listData.remove(0);
    }

    adapter.notifyDataSetChanged();
  }

  public void stopUpdater() {
    if(updater != null)
      updater.stop();
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
          while(listDataUnfiltered.size() > maxLogEntries)
            listDataUnfiltered.remove(0);
        }

        synchronized(listData) {
          while(listData.size() > maxLogEntries)
            listData.remove(0);
        }

        if(IptablesLog.filterText.length() > 0)
          setFilter(IptablesLog.filterText);

        adapter.notifyDataSetChanged();
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
        
        try { Thread.sleep(750); } catch (Exception e) { Log.d("IptablesLog", "LogViewListUpdater", e); }
      }
      MyLog.d("Stopped LogView updater " + this);
    }
  }

  public void setFilter(CharSequence s) {
    MyLog.d("[LogView] setFilter(" + s + ")");
    adapter.getFilter().filter(s);
  }

  private class CustomAdapter extends ArrayAdapter<ListItem> {
    LayoutInflater mInflater = (LayoutInflater) getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    CustomFilter filter;
    ArrayList<ListItem> originalItems = new ArrayList<ListItem>();

    public CustomAdapter(Context context, int resource, List<ListItem> objects) {
      super(context, resource, objects);
    }

    private class CustomFilter extends Filter {
      @Override
        protected FilterResults performFiltering(CharSequence constraint) {
          // store constraint for filter dialog/preferences
          IptablesLog.filterText = constraint;

          constraint = constraint.toString().toLowerCase();

          MyLog.d("[LogView] filter constraint: [" + constraint + "]");

          FilterResults results = new FilterResults();

          synchronized(listDataUnfiltered) {
            originalItems.clear();
            originalItems.addAll(listDataUnfiltered);
          }

          if(constraint == null || constraint.length() == 0) {
            MyLog.d("[LogView] no constraint item count: " + originalItems.size());
            results.values = originalItems;
            results.count = originalItems.size();
          } else {
            ArrayList<ListItem> filteredItems = new ArrayList<ListItem>();
            ArrayList<ListItem> localItems = new ArrayList<ListItem>();
            localItems.addAll(originalItems);
            int count = localItems.size();

            MyLog.d("[LogView] item count: " + count);

            for(int i = 0; i < count; i++) {
              ListItem item = localItems.get(i);
              MyLog.d("[LogView] testing filtered item " + item + "; constraint: [" + constraint + "]");

              if((IptablesLog.filterName && item.mNameLowerCase.contains(constraint))
                || (IptablesLog.filterUid && item.mUidString.contains(constraint))
                || (IptablesLog.filterAddress && (item.srcAddr.contains(constraint) || item.dstAddr.contains(constraint)))
                || (IptablesLog.filterPort && (item.srcPortString.equals(constraint) || item.dstPortString.equals(constraint))))
              {
                MyLog.d("[LogView] adding filtered item " + item);
                filteredItems.add(item);
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
            for(int i = 0; i < count; i++)
              add(localItems.get(i));

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
        srcAddr.setText("SRC: " + item.srcAddr);

        srcPort = holder.getSrcPort();
        srcPort.setText(Integer.toString(item.srcPort));

        dstAddr = holder.getDstAddr();
        dstAddr.setText("DST: " + item.dstAddr);

        dstPort = holder.getDstPort();
        dstPort.setText(Integer.toString(item.dstPort));

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
