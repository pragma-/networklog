package com.googlecode.networklog;

import android.content.Context;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.shapes.Shape;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.ShapeDrawable;
import android.util.Log;

import java.lang.Runnable;
import java.util.ArrayList;
import java.util.Hashtable;

import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.GraphViewSeries;

public class OverallAppTimelineGraph extends GraphActivity
{
  @Override
    protected void onCreate(Bundle savedInstanceState)
    {
      super.onCreate(savedInstanceState);
      graphView.setTitle("Apps Timeline");
      if(instanceData == null) {
        buildLegend(this);
      }
      buildSeries(interval, viewsize);
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

            LegendItem legend = new LegendItem();

            legend.mIcon = shape;
            legend.mHashCode = String.valueOf(item.app.uid).hashCode();
            legend.mName = item.app.name;
            legend.mEnabled = true;

            legendData.add(legend);

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

            int hashCode = String.valueOf(item.app.uid).hashCode();

            graphView.addSeries(new GraphViewSeries(hashCode, item.app.toString(), Color.parseColor(getResources().getString(Colors.distinctColor[color])), seriesData));

            boolean enabled = true;
            for(LegendItem legend : legendData) {
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
    }

    double minX = graphView.getMinX(true);
    double maxX = graphView.getMaxX(true);

    double viewStart = maxX - viewSize;

    if(instanceData != null) {
      viewStart = instanceData.viewportStart;
      viewSize = instanceData.viewsize;
    }

    if(viewStart < minX)
    {
      viewStart = minX;
    }

    if(viewStart + viewSize > maxX)
    {
      viewSize = maxX - viewStart;
    }


    graphView.setViewPort(viewStart, viewSize);
    graphView.invalidateLabels();
    graphView.invalidate();
  }
}
