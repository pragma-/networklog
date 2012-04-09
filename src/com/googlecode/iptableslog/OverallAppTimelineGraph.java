package com.googlecode.iptableslog;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Hashtable;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LineGraphView;
import com.jjoe64.graphview.GraphView.*;

public class OverallAppTimelineGraph extends Activity
{
  protected GraphView graphView;
  protected double interval = IptablesLog.settings.getGraphInterval();
  protected double viewsize = IptablesLog.settings.getGraphViewsize();

  @Override
    protected void onCreate(Bundle savedInstanceState)
    {
      super.onCreate(savedInstanceState);

      // always give data sorted by x values
      graphView = new LineGraphView(this, "Apps Timeline")
      {
        private SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
        @Override
          protected String formatLabel(double value, boolean isValueX)
          {
            if(isValueX)
            {
              // convert from unix timestamp to human time format
              String result = formatter.format(new Date((long) value));
              //MyLog.d("x axis label: " + result);
              return result;
            }
            else
            {
              // y-axis, use default formatter
              String result = String.format("%.2f", value / 1000.0f) + "K";
              //MyLog.d("y axis label: " + result);
              return result;
            }
          }
      };

      buildSeries(interval, viewsize);

      setContentView(graphView);
    }

  @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.layout.graph_menu, menu);
      return true;
    }

  @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
      MenuItem item = null;

      switch((int)interval) {
        case 1:
          item = menu.findItem(R.id.interval_1);
          break;

        case 100:
          item = menu.findItem(R.id.interval_100);
          break;

        case 500:
          item = menu.findItem(R.id.interval_500);
          break;

        case 1000:
          item = menu.findItem(R.id.interval_1000);
          break;

        case 30000:
          item = menu.findItem(R.id.interval_30000);
          break;

        case 60000:
          item = menu.findItem(R.id.interval_60000);
          break;

        case 300000:
          item = menu.findItem(R.id.interval_300000);
          break;

        case 600000:
          item = menu.findItem(R.id.interval_600000);
          break;

        case 900000:
          item = menu.findItem(R.id.interval_900000);
          break;

        case 1800000:
          item = menu.findItem(R.id.interval_1800000);
          break;

        case 3600000:
          item = menu.findItem(R.id.interval_3600000);
          break;

        case 7200000:
          item = menu.findItem(R.id.interval_7200000);
          break;

        case 14400000:
          item = menu.findItem(R.id.interval_14400000);
          break;

        case 28800000:
          item = menu.findItem(R.id.interval_28800000);
          break;

        case 57600000:
          item = menu.findItem(R.id.interval_57600000);
          break;

        case 115200000:
          item = menu.findItem(R.id.interval_115200000);
          break;

        case 230400000:
          item = menu.findItem(R.id.interval_230400000);
          break;
      }

      if(item != null) {
        item.setChecked(true);
      }

      switch((int)viewsize) {
        case 100:
          item = menu.findItem(R.id.viewsize_100);
          break;

        case 500:
          item = menu.findItem(R.id.viewsize_500);
          break;

        case 1000:
          item = menu.findItem(R.id.viewsize_1000);
          break;

        case 30000:
          item = menu.findItem(R.id.viewsize_30000);
          break;

        case 60000:
          item = menu.findItem(R.id.viewsize_60000);
          break;

        case 300000:
          item = menu.findItem(R.id.viewsize_300000);
          break;

        case 600000:
          item = menu.findItem(R.id.viewsize_600000);
          break;

        case 900000:
          item = menu.findItem(R.id.viewsize_900000);
          break;

        case 1800000:
          item = menu.findItem(R.id.viewsize_1800000);
          break;

        case 3600000:
          item = menu.findItem(R.id.viewsize_3600000);
          break;

        case 7200000:
          item = menu.findItem(R.id.viewsize_7200000);
          break;

        case 14400000:
          item = menu.findItem(R.id.viewsize_14400000);
          break;

        case 28800000:
          item = menu.findItem(R.id.viewsize_28800000);
          break;

        case 57600000:
          item = menu.findItem(R.id.viewsize_57600000);
          break;

        case 115200000:
          item = menu.findItem(R.id.viewsize_115200000);
          break;

        case 230400000:
          item = menu.findItem(R.id.viewsize_230400000);
          break;
      }

      if(item != null) {
        item.setChecked(true);
      }

      return true;
    }

  @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      switch(item.getItemId()) {
        case R.id.interval_1:
          interval = 1;
          break;

        case R.id.interval_100:
          interval = 100;
          break;

        case R.id.interval_500:
          interval = 500;
          break;

        case R.id.interval_1000:
          interval = 1000;
          break;

        case R.id.interval_30000:
          interval = 30000;
          break;

        case R.id.interval_60000:
          interval = 60000;
          break;

        case R.id.interval_300000:
          interval = 300000;
          break;

        case R.id.interval_600000:
          interval = 600000;
          break;

        case R.id.interval_900000:
          interval = 900000;
          break;

        case R.id.interval_1800000:
          interval = 1800000;
          break;

        case R.id.interval_3600000:
          interval = 3600000;
          break;

        case R.id.interval_7200000:
          interval = 7200000;
          break;

        case R.id.interval_14400000:
          interval = 14400000;
          break;

        case R.id.interval_28800000:
          interval = 28800000;
          break;

        case R.id.interval_57600000:
          interval = 57600000;
          break;

        case R.id.interval_115200000:
          interval = 115200000;
          break;

        case R.id.interval_230400000:
          interval = 230400000;
          break;

        case R.id.viewsize_100:
          viewsize = 100;
          break;

        case R.id.viewsize_500:
          viewsize = 500;
          break;

        case R.id.viewsize_1000:
          viewsize = 1000;
          break;

        case R.id.viewsize_30000:
          viewsize = 30000;
          break;

        case R.id.viewsize_60000:
          viewsize = 60000;
          break;

        case R.id.viewsize_300000:
          viewsize = 300000;
          break;

        case R.id.viewsize_600000:
          viewsize = 600000;
          break;

        case R.id.viewsize_900000:
          viewsize = 900000;
          break;

        case R.id.viewsize_1800000:
          viewsize = 1800000;
          break;

        case R.id.viewsize_3600000:
          viewsize = 3600000;
          break;

        case R.id.viewsize_7200000:
          viewsize = 7200000;
          break;

        case R.id.viewsize_14400000:
          viewsize = 14400000;
          break;

        case R.id.viewsize_28800000:
          viewsize = 28800000;
          break;

        case R.id.viewsize_57600000:
          viewsize = 57600000;
          break;

        case R.id.viewsize_115200000:
          viewsize = 115200000;
          break;

        case R.id.viewsize_230400000:
          viewsize = 230400000;
          break;

        default:
          return super.onOptionsItemSelected(item);
      }

      IptablesLog.settings.setGraphInterval((long)interval);
      IptablesLog.settings.setGraphViewsize((long)viewsize);
      buildSeries(interval, viewsize);
      return true;
    }

  public void buildSeries(double timeFrameSize, double viewSize) {
    graphView.graphSeries.clear();

    synchronized(IptablesLog.appView.groupDataBuffer) {
      int color = 0;

      Hashtable<String, Boolean> appPlotted = new Hashtable<String, Boolean>();

      for(AppView.GroupItem item : IptablesLog.appView.groupDataBuffer) {
        // don't plot duplicate uids
        if(appPlotted.get(item.app.uidString) == null) {
          appPlotted.put(item.app.uidString, new Boolean(true));

          if(item.packetGraphBuffer.size() > 0) {
            MyLog.d("Starting series for " + item);
            MyLog.d("number of packets: " + item.packetGraphBuffer.size());

            ArrayList<PacketGraphItem> graphData = new ArrayList<PacketGraphItem>();

            double nextTimeFrame = 0;
            double frameLen = 1; // len for this time frame

            for(PacketGraphItem data : item.packetGraphBuffer) {
              MyLog.d("processing: " + data + "; nextTimeFrame: " + nextTimeFrame + "; frameLen: " + frameLen);

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
                MyLog.d("Adding " + data.len + "; frameLen: " + frameLen);

                // get next data
                continue;
              } else {
                // data outside current time frame
                // signifies end of frame
                // plot frame len
                MyLog.d("first plot: (" + nextTimeFrame + ", " + frameLen + ")");
                graphData.add(new PacketGraphItem(nextTimeFrame, frameLen));

                // set up next time frame
                nextTimeFrame += timeFrameSize;
                frameLen = 1;

                // test for gap
                if(data.timestamp > nextTimeFrame) {
                  // data is past this time frame, plot zero here
                  MyLog.d("post zero plot: (" + nextTimeFrame + ", " + frameLen + ")");
                  graphData.add(new PacketGraphItem(nextTimeFrame, frameLen));

                  if((data.timestamp - timeFrameSize) > nextTimeFrame) {
                    MyLog.d("post pre zero plot: (" + nextTimeFrame + ", " + frameLen + ")");
                    graphData.add(new PacketGraphItem(data.timestamp - timeFrameSize, 1));
                  }

                  nextTimeFrame = data.timestamp;
                  frameLen = data.len;

                  MyLog.d("- plotting: (" + nextTimeFrame + ", " + frameLen + ")");
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

            MyLog.d("post plotting: (" + nextTimeFrame + ", " + frameLen + ")");
            graphData.add(new PacketGraphItem(nextTimeFrame, frameLen));

            MyLog.d("post zero plotting: (" + (nextTimeFrame + timeFrameSize) +  ", " + 1 + ")");
            graphData.add(new PacketGraphItem(nextTimeFrame + timeFrameSize, 1.0f));

            MyLog.d("Adding series " + item.app);

            GraphViewData[] seriesData = new GraphViewData[graphData.size()];

            int i = 0;

            for(PacketGraphItem graphItem : graphData)
            {
              seriesData[i] = new GraphViewData(graphItem.timestamp, graphItem.len);
              i++;
            }

            graphView.addSeries(new GraphViewSeries(item.app.toString(), Color.parseColor(getResources().getString(Colors.distinctColor[color])), seriesData));
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
    graphView.setShowLegend(true);
    graphView.setLegendAlign(LegendAlign.BOTTOM);
    graphView.invalidateLabels();
    graphView.invalidate();
  }

}
