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
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView.OnItemClickListener;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
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

public class OverallAppTimelineGraph extends Activity
{
  private MyGraphView graphView;
  private CustomAdapter adapter;
  private double interval = NetworkLog.settings.getGraphInterval();
  private double viewsize = NetworkLog.settings.getGraphViewsize();
  private ArrayList<ListItem> listData = new ArrayList<ListItem>();
  private Spinner intervalSpinner;
  private Spinner viewsizeSpinner;
  private String[] intervalValues;

  private class ListItem {
    Drawable mIcon;
    int mUid;
    String mName;
    boolean mEnabled;
    double size;
  }

  @Override
    protected void onCreate(Bundle savedInstanceState)
    {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.graph_main);

      intervalValues = getResources().getStringArray(R.array.interval_values);

      graphView = (MyGraphView) findViewById(R.id.graph);
      graphView.setTitle("Apps Timeline");
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

  public boolean seriesEnabled(int uid) {
    for(ListItem item : listData) {
      if(item.mUid == uid) {
        return item.mEnabled;
      }
    }
    return true;
  }

  public void buildLegend(Context context) {
    synchronized(NetworkLog.appFragment.groupDataBuffer) {
      int color = 0;

      Hashtable<String, String> appPlotted = new Hashtable<String, String>();
      float density = context.getResources().getDisplayMetrics().density;
      Shape rect = new RectShape();

      for(AppFragment.GroupItem item : NetworkLog.appFragment.groupDataBuffer) {
        // don't plot duplicate uids
        if(appPlotted.get(item.app.uidString) == null) {
          appPlotted.put(item.app.uidString, item.app.uidString);

          if(item.packetGraphBuffer.size() > 0) {
            MyLog.d("Building legend for " + item);

            ShapeDrawable shape = new ShapeDrawable(rect);
            shape.getPaint().setColor(Color.parseColor(getResources().getString(Colors.distinctColor[color])));
            shape.setIntrinsicWidth((int)(18 * (density + 0.5)));
            shape.setIntrinsicHeight((int)(18 * (density + 0.5)));

            ListItem legend = new ListItem();

            legend.mIcon = shape;
            legend.mUid = item.app.uid;
            legend.mName = item.app.name;
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
  }

  private Comparator<ListItem> legendSorter = new Comparator<ListItem>() {
    public int compare(ListItem o1, ListItem o2) {
      return o1.size > o2.size ? -1 : (o1.size == o2.size) ? 0 : 1;
    }
  };

  private HashMap<Integer, Double> legendMap = new HashMap<Integer, Double>();
  
  public void sortLegend() {
    for(GraphViewSeries series : graphView.graphSeries) {
      legendMap.put(series.id, series.size);
    }

    synchronized(listData) {
      for(ListItem item : listData) {
        item.size = legendMap.get(item.mUid);
      }

      Collections.sort(listData, legendSorter);

      adapter.notifyDataSetChanged();
    }
  }

  public void buildSeries(double timeFrameSize, double viewSize) {
    graphView.graphSeries.clear();

    synchronized(NetworkLog.appFragment.groupDataBuffer) {
      int color = 0;

      Hashtable<String, String> appPlotted = new Hashtable<String, String>();

      for(AppFragment.GroupItem item : NetworkLog.appFragment.groupDataBuffer) {
        // don't plot duplicate uids
        if(appPlotted.get(item.app.uidString) == null) {
          appPlotted.put(item.app.uidString, item.app.uidString);

          if(item.packetGraphBuffer.size() > 0) {
            MyLog.d("Starting series for " + item);
            MyLog.d("number of packets: " + item.packetGraphBuffer.size());

            ArrayList<PacketGraphItem> graphData = new ArrayList<PacketGraphItem>();

            double nextTimeFrame = 0;
            double frameLen = 1; // len for this time frame

            for(PacketGraphItem data : item.packetGraphBuffer) {
              // MyLog.d("processing: " + data + "; nextTimeFrame: " + nextTimeFrame + "; frameLen: " + frameLen);

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
            graphData.add(new PacketGraphItem(nextTimeFrame + timeFrameSize, 1.0f));

            // MyLog.d("Adding series " + item.app);

            GraphViewData[] seriesData = new GraphViewData[graphData.size()];

            int i = 0;

            for(PacketGraphItem graphItem : graphData)
            {
              seriesData[i] = new GraphViewData(graphItem.timestamp, graphItem.len);
              i++;
            }

            graphView.addSeries(new GraphViewSeries(item.app.uid, item.app.toString(), Color.parseColor(getResources().getString(Colors.distinctColor[color])), seriesData));

            boolean enabled = true;
            for(ListItem legend : listData) {
              if(legend.mUid == item.app.uid) {
                enabled = legend.mEnabled;
                break;
              }
            }
            graphView.setSeriesEnabled(item.app.uid, enabled);

            color++;

            if(color >= Colors.distinctColor.length)
            {
              color = 0;
            }
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

        graphView.setSeriesEnabled(item.mUid, item.mEnabled);
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
        name.setText("(" + item.mUid + ") " + item.mName);
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
