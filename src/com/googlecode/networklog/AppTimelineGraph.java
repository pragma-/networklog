package com.googlecode.networklog;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView.OnItemClickListener;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.LayoutInflater;
import android.util.Log;
import android.util.AttributeSet;
import android.graphics.drawable.shapes.Shape;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.ShapeDrawable;

import java.lang.Runnable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import com.jjoe64.graphview.GraphView.*;

public class AppTimelineGraph extends Activity
{
  private MyGraphView graphView;
  private CustomAdapter adapter;
  private double interval = NetworkLog.settings.getGraphInterval();
  private double viewsize = NetworkLog.settings.getGraphViewsize();
  private ArrayList<ListItem> listData = new ArrayList<ListItem>();
  private Spinner intervalSpinner;
  private Spinner viewsizeSpinner;
  private String[] intervalValues;
  private String app_uid = null;
  private String src_addr;
  private String src_port;
  private String dst_addr;
  private String dst_port;

  private class ListItem {
    Drawable mIcon;
    int mHashCode;
    String mName;
    boolean mEnabled;
    double size;
  }

  @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      requestWindowFeature(Window.FEATURE_NO_TITLE);
      setContentView(R.layout.graph_main);

      Bundle extras = getIntent().getExtras();

      if(extras != null) {
        app_uid = extras.getString("app_uid");
        src_addr = extras.getString("src_addr");
        src_port = extras.getString("src_port");
        dst_addr = extras.getString("dst_addr");
        dst_port = extras.getString("dst_port");
      }

      if(app_uid == null) {
        // alert dialog
        finish();
      }

      int index = NetworkLog.appFragment.getItemByAppUid(Integer.parseInt(app_uid));

      if(index < 0) {
        // alert dialog
        finish();
      }

      AppFragment.GroupItem item = NetworkLog.appFragment.groupDataBuffer.get(index);

      intervalValues = getResources().getStringArray(R.array.interval_values);
      
      graphView = (MyGraphView) findViewById(R.id.graph);
      graphView.setTitle(item.toString() + " Timeline");
      graphView.setEnableMultiLineXLabel(true);
      graphView.setLegendSorter(new Runnable() {
        public void run() {
          sortLegend();
        }
      });

      ListView listView = (ListView) findViewById(R.id.graph_legend);
      adapter = new CustomAdapter(this, R.layout.graph_legend_item, listData);
      listView.setAdapter(adapter);
      listView.setOnItemClickListener(new CustomOnItemClickListener());
      listView.setFastScrollEnabled(true);

      MyOnItemSelectedListener listener = new MyOnItemSelectedListener();

      intervalSpinner = (Spinner) findViewById(R.id.intervalSpinner);
      intervalSpinner.setOnItemSelectedListener(listener);
      ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
          this, R.array.interval_entries, android.R.layout.simple_spinner_item);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      intervalSpinner.setAdapter(adapter);

      viewsizeSpinner = (Spinner) findViewById(R.id.viewsizeSpinner);
      viewsizeSpinner.setOnItemSelectedListener(listener);
      adapter = ArrayAdapter.createFromResource(
          this, R.array.interval_entries, android.R.layout.simple_spinner_item);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      viewsizeSpinner.setAdapter(adapter);

      int length = intervalValues.length;
      String intervalString = String.valueOf((int)interval);
      String viewsizeString = String.valueOf((int)viewsize);

      for(int i = 0; i < length; i++) {
        if(intervalString.equals(intervalValues[i])) {
          intervalSpinner.setSelection(i);
        }
      }

      for(int i = 0; i < length; i++) {
        if(viewsizeString.equals(intervalValues[i])) {
          viewsizeSpinner.setSelection(i);
        }
      }

      buildLegend(this);
      buildSeries(interval, viewsize);
    }

  public class MyOnItemSelectedListener implements OnItemSelectedListener {
    @Override
      public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        if(parent == intervalSpinner) {
          interval = Double.parseDouble(intervalValues[pos]);
          MyLog.d("Setting interval " + pos + ", " + interval);
          NetworkLog.settings.setGraphInterval((long)interval);
        } else {
          viewsize = Double.parseDouble(intervalValues[pos]);
          MyLog.d("Setting viewsize " + pos + ", " + viewsize);
          NetworkLog.settings.setGraphViewsize((long)viewsize);
        }
        buildSeries(interval, viewsize);
      }

    @Override
      public void onNothingSelected(AdapterView parent) {
        // do nothing
      }
  }

  public boolean seriesEnabled(int hashCode) {
    for(ListItem item : listData) {
      if(item.mHashCode == hashCode) {
        return item.mEnabled;
      }
    }
    return true;
  }

  public void buildLegend(Context context) {
    int index = NetworkLog.appFragment.getItemByAppUid(Integer.parseInt(app_uid));

    if(index < 0)
    {
      // alert dialog
      finish();
    }

    AppFragment.GroupItem item = NetworkLog.appFragment.groupDataBuffer.get(index);

    MyLog.d("Starting graph for " + item);

    synchronized(item.childrenData) {
      ArrayList<String> list = new ArrayList<String>(item.childrenData.keySet());
      Collections.sort(list);
      Iterator<String> itr = list.iterator();
      int color = 0;

      float density = context.getResources().getDisplayMetrics().density;
      Shape rect = new RectShape();

      while(itr.hasNext())
      {
        String host = itr.next();
        AppFragment.ChildItem info = item.childrenData.get(host);

        if(info.packetGraphBuffer.size() > 0)
        {
          String label = host;

          if(info.sentPackets > 0 && info.out != null && info.out.length() != 0) {
            String sentAddressString;
            String sentPortString;

            if(NetworkLog.resolveHosts) {
              sentAddressString = NetworkLog.resolver.resolveAddress(info.sentAddress);

              if(sentAddressString == null) {
                sentAddressString = info.sentAddress;
              }
            } else {
              sentAddressString = info.sentAddress;
            }

            if(NetworkLog.resolvePorts) {
              sentPortString = NetworkLog.resolver.resolveService(String.valueOf(info.sentPort));
            } else {
              sentPortString = String.valueOf(info.sentPort);
            }

            label = sentAddressString + ":" + sentPortString;
          }
          else if(info.receivedPackets > 0 && info.in != null && info.in.length() != 0) {
            String receivedAddressString;
            String receivedPortString;

            if(NetworkLog.resolveHosts) {
              receivedAddressString = NetworkLog.resolver.resolveAddress(info.receivedAddress);

              if(receivedAddressString == null) {
                receivedAddressString = info.receivedAddress;
              }
            } else {
              receivedAddressString = info.receivedAddress;
            }

            if(NetworkLog.resolvePorts) {
              receivedPortString = NetworkLog.resolver.resolveService(String.valueOf(info.receivedPort));
            } else {
              receivedPortString = String.valueOf(info.receivedPort);
            }

            label = receivedAddressString + ":" + receivedPortString;
          }

          MyLog.d("Building legend for " + label);

          ShapeDrawable shape = new ShapeDrawable(rect);
          shape.getPaint().setColor(Color.parseColor(getResources().getString(Colors.distinctColor[color])));
          shape.setIntrinsicWidth((int)(18 * (density + 0.5)));
          shape.setIntrinsicHeight((int)(18 * (density + 0.5)));

          int hashCode = label.hashCode();
          ListItem legend = new ListItem();

          legend.mIcon = shape;
          legend.mHashCode = hashCode;
          legend.mName = label;
          legend.mEnabled = true;

          listData.add(legend);

          color++;

          if(color >= Colors.distinctColor.length)
          {
            color = 0;
          }
        }
      }
    }
  }

  private Comparator<ListItem> legendSorter = new Comparator<ListItem>() {
    public int compare(ListItem o1, ListItem o2) {
      return o1.size > o2.size ? -1 : (o1.size == o2.size) ? 0 : 1;
    }
  };

  HashMap<Integer, Double> legendMap = new HashMap<Integer, Double>();

  public void sortLegend() {

    for(GraphViewSeries series : graphView.graphSeries) {
      legendMap.put(series.id, series.size);
    }

    // FIXME: update hashcode when resolving host addr
    Double size;
    synchronized(listData) {
      for(ListItem item : listData) {
        size = legendMap.get(item.mHashCode);
        item.size = size == null ? 0 : size;
      }

      Collections.sort(listData, legendSorter);

      adapter.notifyDataSetChanged();
    }
  }

  public void buildSeries(double timeFrameSize, double viewSize) {
    graphView.graphSeries.clear();

    int index = NetworkLog.appFragment.getItemByAppUid(Integer.parseInt(app_uid));

    if(index < 0)
    {
      // alert dialog
      finish();
    }

    AppFragment.GroupItem item = NetworkLog.appFragment.groupDataBuffer.get(index);

    MyLog.d("Starting graph for " + item);

    synchronized(item.childrenData) {
      ArrayList<String> list = new ArrayList<String>(item.childrenData.keySet());
      Collections.sort(list);
      Iterator<String> itr = list.iterator();
      int color = 0;

      while(itr.hasNext())
      {
        String host = itr.next();
        MyLog.d("Graphing " + host);
        AppFragment.ChildItem info = item.childrenData.get(host);

        if(info.packetGraphBuffer.size() > 0)
        {
          MyLog.d("number of packets: " + info.packetGraphBuffer.size());
          ArrayList<PacketGraphItem> graphData = new ArrayList<PacketGraphItem>();

          double nextTimeFrame = 0;
          double frameLen = 1; // len for this time frame

          for(PacketGraphItem data : info.packetGraphBuffer)
          {
            //MyLog.d("processing: " + data + "; nextTimeFrame: " + nextTimeFrame + "; frameLen: " + frameLen);

            if(nextTimeFrame == 0) {
              // first  plot
              graphData.add(new PacketGraphItem(data.timestamp, data.len));

              // set up first time frame
              nextTimeFrame = data.timestamp + timeFrameSize;
              frameLen = data.len;

              // get next data
              continue;
            }

            if(data.timestamp <= nextTimeFrame) {
              // data within current time frame, add to frame len
              frameLen += data.len;
              // MyLog.d("Adding " + data.len + "; frameLen: " + frameLen);

              // get next data
              continue;
            } else {
              // data outside current time frame
              // signifies end of frame
              // plot frame len
              // MyLog.d("first plot: (" + nextTimeFrame + ", " + frameLen + ")");
              graphData.add(new PacketGraphItem(nextTimeFrame, frameLen));

              // set up next time frame
              nextTimeFrame += timeFrameSize;
              frameLen = 1;

              // test for gap
              if(data.timestamp > nextTimeFrame) {
                // data is past this time frame, plot zero here
                // MyLog.d("post zero plot: (" + nextTimeFrame + ", " + frameLen + ")");
                graphData.add(new PacketGraphItem(nextTimeFrame, frameLen));

                if((data.timestamp - timeFrameSize) > nextTimeFrame) {
                  // MyLog.d("post pre zero plot: (" + nextTimeFrame + ", " + frameLen + ")");
                  graphData.add(new PacketGraphItem(data.timestamp - timeFrameSize, 1));
                }

                nextTimeFrame = data.timestamp;
                frameLen = data.len;

                // MyLog.d("- plotting: (" + nextTimeFrame + ", " + frameLen + ")");
                graphData.add(new PacketGraphItem(nextTimeFrame, frameLen));

                nextTimeFrame += timeFrameSize;
                frameLen = 1;
                continue;
              } else {
                // data is within this frame, add len
                frameLen = data.len;
              }
            }
          }

          // MyLog.d("post plotting: (" + nextTimeFrame + ", " + frameLen + ")");
          graphData.add(new PacketGraphItem(nextTimeFrame, frameLen));

          // MyLog.d("post zero plotting: (" + (nextTimeFrame + timeFrameSize) +  ", " + 1 + ")");
          graphData.add(new PacketGraphItem(nextTimeFrame + timeFrameSize, 1));

          // MyLog.d("Adding series " + info);

          GraphViewData[] seriesData = new GraphViewData[graphData.size()];

          int i = 0;

          for(PacketGraphItem graphItem : graphData)
          {
            seriesData[i] = new GraphViewData(graphItem.timestamp, graphItem.len);
            i++;
          }

          String label = host;

          if(info.sentPackets > 0 && info.out != null && info.out.length() != 0) {
            String sentAddressString;
            String sentPortString;

            if(NetworkLog.resolveHosts) {
              sentAddressString = NetworkLog.resolver.resolveAddress(info.sentAddress);

              if(sentAddressString == null) {
                sentAddressString = info.sentAddress;
              }

            } else {
              sentAddressString = info.sentAddress;
            }

            if(NetworkLog.resolvePorts) {
              sentPortString = NetworkLog.resolver.resolveService(String.valueOf(info.sentPort));
            } else {
              sentPortString = String.valueOf(info.sentPort);
            }

            label = sentAddressString + ":" + sentPortString;
          }
          else if(info.receivedPackets > 0 && info.in != null && info.in.length() != 0) {
            String receivedAddressString;
            String receivedPortString;

            if(NetworkLog.resolveHosts) {
              receivedAddressString = NetworkLog.resolver.resolveAddress(info.receivedAddress);

              if(receivedAddressString == null) {
                receivedAddressString = info.receivedAddress;
              }

            } else {
              receivedAddressString = info.receivedAddress;
            }

            if(NetworkLog.resolvePorts) {
              receivedPortString = NetworkLog.resolver.resolveService(String.valueOf(info.receivedPort));
            } else {
              receivedPortString = String.valueOf(info.receivedPort);
            }

            label = receivedAddressString + ":" + receivedPortString;
          }

          int hashCode = label.hashCode();

          graphView.addSeries(new GraphViewSeries(hashCode, label, Color.parseColor(getResources().getString(Colors.distinctColor[color])), seriesData));

          boolean enabled = true;
          for(ListItem legend : listData) {
            if(legend.mHashCode == hashCode) {
              enabled = legend.mEnabled;
              break;
            }
          }
          graphView.setSeriesEnabled(hashCode, enabled);

          color++;

          if(color >= Colors.distinctColor.length)
          {
            color = 0;
          }
        }
      }
    }

    double minX = graphView.getMinX(true);
    double maxX = graphView.getMaxX(true);

    double viewStart = maxX - viewSize;

    if(viewStart < minX)
    {
      viewStart = minX;
    }

    if(viewStart + viewSize > maxX)
    {
      viewSize = maxX - viewStart;
    }

    graphView.setViewPort(viewStart, viewSize);
    graphView.setScrollable(true);
    graphView.setScalable(true);
    graphView.setShowLegend(false);
    graphView.invalidateLabels();
    graphView.invalidate();
  }

  private class CustomOnItemClickListener implements OnItemClickListener {
    @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ListItem item = listData.get(position);
        item.mEnabled = !item.mEnabled;

        CheckedTextView ctv = (CheckedTextView) view.findViewById(R.id.legendName);
        ctv.setChecked(item.mEnabled);

        graphView.setSeriesEnabled(item.mHashCode, item.mEnabled);
        graphView.invalidateLabels();
        graphView.invalidate();
      }
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
        CheckedTextView name;

        ListItem item = getItem(position);

        if(convertView == null) {
          convertView = mInflater.inflate(R.layout.graph_legend_item, null);
          holder = new ViewHolder(convertView);
          convertView.setTag(holder);
        } else {
          holder = (ViewHolder) convertView.getTag();
        }

        icon = holder.getIcon();
        icon.setImageDrawable(item.mIcon);

        name = holder.getName();
        name.setText(item.mName);
        name.setChecked(item.mEnabled);

        return convertView;
      }
  }

  private class ViewHolder {
    private View mView;
    private ImageView mIcon;
    private CheckedTextView mName;

    public ViewHolder(View view) {
      mView = view;
    }

    public ImageView getIcon() {
      if(mIcon == null) {
        mIcon = (ImageView) mView.findViewById(R.id.legendIcon);
      }

      return mIcon;
    }

    public CheckedTextView getName() {
      if(mName == null) {
        mName = (CheckedTextView) mView.findViewById(R.id.legendName);
      }

      return mName;
    }
  }
}
