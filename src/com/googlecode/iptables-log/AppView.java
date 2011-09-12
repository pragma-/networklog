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

public class AppView extends Activity
{
  private ArrayList<ListItem> listData;
  private CustomAdapter adapter;

  private class ListItem {
    protected Drawable mIcon;
    protected int mUid;
    protected String mName;
    protected int totalPackets;
    protected int totalLen;
    protected int lastTimestamp;

    ListItem(Drawable icon, int uid, String name) {
      mIcon = icon;
      mUid = uid;
      mName = name;
    }
  }

  protected class SortAppsByName implements Comparator<ListItem> {
    public int compare(ListItem o1, ListItem o2) {
      return o1.mName.compareToIgnoreCase(o2.mName);
    }
  }

  protected class SortAppsByUid implements Comparator<ListItem> {
    public int compare(ListItem o1, ListItem o2) {
      return o1.mUid < o2.mUid ? -1 : 1;
    }
  }

  protected void getInstalledApps() {
    for(ApplicationsTracker.AppEntry app : ApplicationsTracker.installedApps) {
      String name = app.name;
      Drawable icon = app.icon;
      int uid = app.uid;

      ListItem item = new ListItem(icon, uid, name);
      listData.add(item);
    }

    Collections.sort(listData, new SortAppsByUid());
  }

  @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.layout.appmenu, menu);
      return true;
    }

  @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      switch(item.getItemId()) {
        case R.id.sort_by_uid:
          Collections.sort(listData, new SortAppsByUid());
          adapter.notifyDataSetChanged();
          return true;
        case R.id.sort_by_name:
          Collections.sort(listData, new SortAppsByName());
          adapter.notifyDataSetChanged();
          return true;
        default:
          return super.onOptionsItemSelected(item);
      }
    }

  /** Called when the activity is first created. */
  @Override
    public void onCreate(Bundle savedInstanceState)
    {
      super.onCreate(savedInstanceState);

      LinearLayout layout = new LinearLayout(this);
      layout.setOrientation(LinearLayout.VERTICAL);

      TextView tv = new TextView(this);
      tv.setText("Application listing");

      layout.addView(tv);

      listData = new ArrayList<ListItem>();
      adapter = new CustomAdapter(this, R.layout.appitem, listData);

      ListView lv = new ListView(this);
      lv.setTextFilterEnabled(true);
      lv.setAdapter(adapter);

      layout.addView(lv);

      setContentView(layout);

      getInstalledApps();
      adapter.notifyDataSetChanged();
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

        ListItem item = getItem(position);

        if(convertView == null) {
          convertView = mInflater.inflate(R.layout.appitem, null);
          holder = new ViewHolder(convertView);
          convertView.setTag(holder);
        }

        holder = (ViewHolder) convertView.getTag();
        icon = holder.getIcon();
        icon.setImageDrawable(item.mIcon);

        name = holder.getName();
        name.setText("(" + item.mUid + ")" + " " + item.mName);

        return convertView;
      }
  }

  private class ViewHolder {
    private View mView;
    private ImageView mIcon = null;
    private TextView mName= null;

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
  }
}
