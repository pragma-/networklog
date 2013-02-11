/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.shapes.Shape;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.ShapeDrawable;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.GraphViewSeries;

public class OverallAppTimelineGraph extends GraphActivity
{
  @Override
    protected void onCreate(Bundle savedInstanceState)
    {
      super.onCreate(savedInstanceState);
      graphView.setTitle("Apps Timeline");
      buildSeries(interval, viewsize);
    }

  public void buildSeries(double timeFrameSize, double viewSize) {
    if(instanceData != null) {
      graphView.graphSeries = instanceData.graphSeries;
    } else {
      HashMap<Integer, ArrayList<PacketGraphItem>> appMap = new HashMap<Integer, ArrayList<PacketGraphItem>>();
      HashMap<Integer, String> uidNameMap = new HashMap<Integer, String>();
      ArrayList<PacketGraphItem> packetList;

      graphView.graphSeries.clear();

      if(NetworkLog.logFragment == null || NetworkLog.logFragment.listData == null || NetworkLog.logFragment.listData.size() == 0) {
        SysUtils.showError(this, "No data", "There is no graph to show because there are no network log entries.");
        finish();
      }

      synchronized(NetworkLog.logFragment.listData) {
        for(LogFragment.ListItem item : NetworkLog.logFragment.listData) {
          packetList = appMap.get(item.app.uid);

          if(packetList == null) {
            packetList = new ArrayList<PacketGraphItem>();
            appMap.put(item.app.uid, packetList);
            uidNameMap.put(item.app.uid, "(" + item.app.uid + ") " + item.app.name);
          }

          packetList.add(new PacketGraphItem(item.timestamp, item.len));
        }
      }

      if(appMap.size() == 0) {
        SysUtils.showError(this, "No data", "There is no graph to show because there are no network log entries.");
        finish();
      }

      int color = 0;
      float density = getResources().getDisplayMetrics().density;
      Shape rect = new RectShape();
      int intrinsicLength = (int)(18 * (density + 0.5));
      int uid;
      for(Map.Entry<Integer, ArrayList<PacketGraphItem>> entry : appMap.entrySet()) {
        uid = entry.getKey();
        packetList = entry.getValue();

        if(MyLog.enabled) {
          MyLog.d("number of packets for " + uid + ": " + packetList.size());
        }
        ArrayList<PacketGraphItem> graphData = new ArrayList<PacketGraphItem>();

        double nextTimeFrame = 0;
        double frameLen = 1; // len for this time frame

        for(PacketGraphItem data : packetList) {
          if(nextTimeFrame == 0) {
            // first  plot
            graphData.add(new PacketGraphItem(data.timestamp - 1, 1));
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
            // get next data
            continue;
          } else {
            // data outside current time frame
            // signifies end of frame
            // plot frame len
            graphData.add(new PacketGraphItem(nextTimeFrame, frameLen));

            // set up next time frame
            nextTimeFrame += timeFrameSize;
            frameLen = 1;

            // test for gap
            if(data.timestamp > nextTimeFrame) {
              // data is past this time frame, plot zero here
              graphData.add(new PacketGraphItem(nextTimeFrame, frameLen));

              if((data.timestamp - timeFrameSize) > nextTimeFrame) {
                graphData.add(new PacketGraphItem(data.timestamp - timeFrameSize, 1));
              }

              nextTimeFrame = data.timestamp;
              frameLen = data.len;

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

        graphData.add(new PacketGraphItem(nextTimeFrame, frameLen));
        graphData.add(new PacketGraphItem(nextTimeFrame + timeFrameSize, 1));

        GraphViewData[] seriesData = new GraphViewData[graphData.size()];

        int i = 0;

        for(PacketGraphItem graphItem : graphData) {
          seriesData[i] = new GraphViewData(graphItem.timestamp, graphItem.len);
          i++;
        }

        int hashCode = String.valueOf(uid).hashCode();
        String name = uidNameMap.get(uid);

        graphView.addSeries(new GraphViewSeries(hashCode, name, Color.parseColor(getResources().getString(Colors.distinctColor[color])), seriesData));

        boolean enabled = true;
        boolean exists = false;
        for(LegendItem legend : legendData) {
          if(legend.mHashCode == hashCode) {
            enabled = legend.mEnabled;
            exists = true;
            break;
          }
        }

        if(exists == false) {
          ShapeDrawable shape = new ShapeDrawable(rect);
          shape.getPaint().setColor(Color.parseColor(getResources().getString(Colors.distinctColor[color])));
          shape.setIntrinsicWidth(intrinsicLength);
          shape.setIntrinsicHeight(intrinsicLength);

          LegendItem legend = new LegendItem();

          legend.mIcon = shape;
          legend.mHashCode = hashCode;
          legend.mName = name;
          legend.mEnabled = true;

          legendData.add(legend);
        }

        graphView.setSeriesEnabled(hashCode, enabled);

        color++;

        if(color >= Colors.distinctColor.length) {
          color = 0;
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

    if(viewStart < minX) {
      viewStart = minX;
    }

    if(viewStart + viewSize > maxX) {
      viewSize = maxX - viewStart;
    }

    graphView.setViewPort(viewStart, viewSize);
    graphView.invalidateLabels();
    graphView.invalidate();
  }
}
