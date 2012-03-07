package com.googlecode.iptableslog;

import android.util.Log;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
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
  protected static ArrayList<ListItem> listData;
  private ListView listView;
  private CustomAdapter adapter;
  private int sortBy = 0;

  protected class ListItem {
    protected Drawable mIcon;
    protected int mUid;
    protected String mName;
    protected String srcAddr;
    protected int srcPort;
    protected String dstAddr;
    protected int dstPort;
    protected int len;
    protected String timestamp;

    ListItem(Drawable icon, int uid, String name) {
      mIcon = icon;
      mUid = uid;
      mName = name;
    }
  }

  @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.layout.logmenu, menu);
      return true;
    }

  @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      switch(item.getItemId()) {
        default:
          return super.onOptionsItemSelected(item);
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
      } else {
        restoreData(IptablesLog.data);
      }

      adapter = new CustomAdapter(this, R.layout.logitem, listData);

      listView = new ListView(this);
      listView.setTextFilterEnabled(true);
      listView.setAdapter(adapter);

      layout.addView(listView);

      setContentView(layout);

      IptablesLogTracker.addListener(this);
    }

  public void restoreData(IptablesLogData data) {
    listData = data.logViewListData;
  }

  public void onNewLogEntry(IptablesLogTracker.LogEntry entry) {
    ApplicationsTracker.AppEntry appEntry = ApplicationsTracker.installedAppsHash.get(entry.uid);

    if(appEntry == null) {
      MyLog.d("LogView: No appEntry for uid " + entry.uid);
      return;
    }

    final ListItem item = new ListItem(appEntry.icon, appEntry.uid, appEntry.name);

    item.srcAddr = entry.src;
    item.srcPort = entry.spt;
    item.dstAddr = entry.dst;
    item.dstPort = entry.dpt;
    item.len = entry.len;
    item.timestamp = entry.timestamp;

    runOnUiThread(new Runnable() {
      public void run() {
        MyLog.d("LogView: Add item: " + item.srcAddr + " " + item.srcPort + " " + item.dstAddr + " " + item.dstPort + " " + item.len);

        listData.add(item);

        while(listData.size() >= 100) {
          listData.remove(0);
        }

        adapter.notifyDataSetChanged();
        listView.smoothScrollToPosition(adapter.getCount());
      }
    });
  }

  private class CustomAdapter extends ArrayAdapter<ListItem> {
    LayoutInflater mInflater = (LayoutInflater) getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

    public CustomAdapter(Context context, int resource, List<ListItem> objects) {
      super(context, resource, objects);
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
